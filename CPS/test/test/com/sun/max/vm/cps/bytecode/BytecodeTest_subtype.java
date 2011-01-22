/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.cps.bytecode;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import test.com.sun.max.vm.bytecode.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Performs various tests to ensure that compilation of the type testing bytecodes (i.e. instanceof, aastore, checkcast)
 * matches the semantics of the host on which the tests are run (which is presumed to implement the required VM
 * semantics correctly). The tests explore a predefined set of types and instances of those types.
 *
 * @author Doug Simon
 */
public abstract class BytecodeTest_subtype<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_subtype(String name) {
        super(name);
    }

    static interface I1 {}
    static interface I2 {}
    static interface I3 extends I1, I2 {}
    static class CI1 implements I1 {}
    static class CI2 implements I2 {}
    static class CI3 implements I3 {}
    static class SCI1 extends CI1 {}
    static class SCI2 extends SCI1 implements I2 {}
    static class SCI3 extends SCI2 implements I3 {}

    static class CC implements Cloneable {}
    static class SC implements Cloneable {}
    static class SCC implements Cloneable {}
    static class SubCC extends CC {}
    static class SubSC extends SC {}
    static class SubSCC extends SCC {}

    interface K extends Cloneable {}
    interface K1 {}
    interface K2 {}
    interface K0 extends K1, K2 {}

    static class C1 {}
    static class C2 extends C1 {}
    static class C3 extends C2 {}
    static class C4 extends C3 {}
    static class C5 extends C4 {}
    static class C6 extends C5 {}
    static class C7 extends C6 {}
    static class C8 extends C7 {}
    static class C9 extends C8 {}
    static class C10 extends C9 {}

    private static Class[] klasses;
    private static Map<Class, Object> instances;

    public static final int MAX_ARRAY_DIMENSION_TESTED = 3;

    private static Object arrayOf(Class componentType, int dimensions) {
        final int[] dimensionLengths = new int[dimensions];
        dimensionLengths[0] = 1;
        return Array.newInstance(componentType, dimensionLengths);
    }

    private static void gatherSuperTypes(Class c, Set<Class> superTypes) {
        for (Class iface : c.getInterfaces()) {
            superTypes.add(iface);
            gatherSuperTypes(iface, superTypes);
        }
        final Class superClass = c.getSuperclass();
        if (superClass != null) {
            superTypes.add(superClass);
            gatherSuperTypes(superClass, superTypes);
        }
    }

    @Override
    public void setUp() {
        if (instances != null) {
            return;
        }
        instances = new HashMap<Class, Object>();

        final Set<Class> classes = new LinkedHashSet<Class>(Arrays.asList(new Class[]{
            Object.class,
            String.class,
            Exception.class,
            Cloneable.class,
            IllegalStateException.class,
            SocketException.class,
            IOException.class,
            LinkedHashMap.class,
            Hashtable.class,
            ArrayList.class,
            VerifyError.class
        }));

        for (Kind kind : Kind.VALUES) {
            if (kind != Kind.WORD) {
                classes.add(kind.javaClass);
            }
        }

        classes.addAll(Arrays.asList(BytecodeTest_subtype.class.getDeclaredClasses()));

        // Expand to get complete type hierarchy
        final Set<Class> hierarchy = new LinkedHashSet<Class>(classes);
        for (Class c : classes) {
            gatherSuperTypes(c, hierarchy);
        }

        final Set<Class> allClasses = new LinkedHashSet<Class>(hierarchy);
        for (Class c : hierarchy) {
            for (int i = 1; i <= MAX_ARRAY_DIMENSION_TESTED; ++i) {
                final Object array = arrayOf(c, i);
                final Class<?> arrayClass = array.getClass();
                allClasses.add(arrayClass);
                instances.put(arrayClass, array);
            }

            if (c == int.class || c == String.class) {
                final Object array = arrayOf(c, 255);
                final Class<?> arrayClass = array.getClass();
                allClasses.add(arrayClass);
                instances.put(arrayClass, array);
            }

            if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                try {
                    instances.put(c, c.newInstance());
                } catch (Exception e) {
                }
            }
        }

        assert !instances.containsKey(null);
        instances.put(null, null);

        klasses = allClasses.toArray(new Class[classes.size()]);
    }

    private static final Pattern ARRAY_DIMENSIONS = Pattern.compile("(.*?)((?:\\[\\])+)(.*)");
    private static final int TRACE_LEVEL = 1;

    /**
     * Replaces contiguous sequences of "[]" in a given string with "$n" where n is the number of "[]" in the sequence.
     * That is, a method name that includes one or more array type names is transformed to be a legal Java method name.
     */
    private static String sanitizeMethodName(String methodName) {
        String buf = methodName;
        final Matcher matcher = ARRAY_DIMENSIONS.matcher(buf);
        while (matcher.matches()) {
            buf = matcher.group(1) + "$" + (matcher.group(2).length() / 2) + matcher.group(3);
            matcher.reset(buf);
        }
        return buf;
    }

    private int testCount;

    private String methodName(String testDescription) {
        return "perform" + (++testCount) + "_" + testDescription;
    }

    private void instanceof_test(final Class type, Iterable<Object> objects) {
        final String targetTypeName = type.getSimpleName();
        final String methodName = methodName("perform_instanceof_" + targetTypeName);
        final Method_Type method = compile(new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(boolean.class, Object.class)) {
            @Override
            public void generateCode() {
                aload(0);
                instanceof_(PoolConstantFactory.createClassConstant(type));
                ireturn();
            }
        }, getClass());
        for (Object object : objects) {
            final String objectTypeName = object == null ? "null" : object.getClass().getSimpleName();
            final Value result = execute(method, ReferenceValue.from(object));
            if (type.isInstance(object)) {
                assertTrue(objectTypeName + " should be an instance of " + targetTypeName, result.asBoolean());
            } else {
                assertFalse(objectTypeName + " should not be an instance of " + targetTypeName, result.asBoolean());
            }
        }
    }

    private void arraystoreTest(Object array, Iterable<Object> values) {
        final String arrayTypeName = array.getClass().getSimpleName();
        final String methodName = methodName("perform_aastore_" + arrayTypeName);
        final Method_Type method = compile(new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(void.class, array.getClass(), Object.class)) {
            @Override
            public void generateCode() {
                aload(0); // array
                iconst(0); // index
                aload(1); // value
                aastore();
                vreturn();
            }
        }, getClass());
        for (Object value : values) {
            final Class valueType = value == null ? Object.class : value.getClass();
            final String valueTypeName = value == null ? "null" : valueType.getSimpleName();
            final ClassActor arrayClassActor = ClassActor.fromJava(array.getClass());
            final ClassActor componentClassActor = arrayClassActor.componentClassActor();
            final boolean expectArrayStoreException = componentClassActor.kind.isWord || (value != null && !componentClassActor.isAssignableFrom(ClassActor.fromJava(value.getClass())));
            try {
                executeWithException(method, ReferenceValue.from(array), ReferenceValue.from(value));
                if (expectArrayStoreException) {
                    fail(valueTypeName + " should not be storeable to " + arrayTypeName);
                }
            } catch (InvocationTargetException e) {
                if (!expectArrayStoreException) {
                    System.err.println(valueTypeName + " should be storeable to " + arrayTypeName);
                    e.printStackTrace();
                    fail(valueTypeName + " should be storeable to " + arrayTypeName);
                }
                assertTrue(e.getTargetException() instanceof ArrayStoreException);
            }
        }
    }

    private void checkcast_test(final Class targetType, Iterable<Object> objects) {
        final String targetTypeName = targetType.getSimpleName();
        final String methodName = methodName("perform_checkcast_" + targetTypeName);
        final Method_Type method = compile(new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(void.class, Object.class)) {
            @Override
            public void generateCode() {
                aload(0); // object
                checkcast(PoolConstantFactory.createClassConstant(targetType));
                pop();
                vreturn();
            }
        }, getClass());
        for (Object object : objects) {
            final String objectTypeName = object == null ? "null" : object.getClass().getSimpleName();
            final boolean expectedClassCastException = object != null && !targetType.isInstance(object);
            try {
                executeWithException(method, ReferenceValue.from(object));
                if (expectedClassCastException) {
                    fail(objectTypeName + " should not be castable to " + targetTypeName);
                }
            } catch (InvocationTargetException e) {
                if (!expectedClassCastException) {
                    e.printStackTrace();
                    fail(objectTypeName + " should be castable to " + targetTypeName);
                }
                assertTrue(e.getTargetException() instanceof ClassCastException);
            }
        }
    }

    /**
     * Stub testing takes too long when compiling so many classes which prevents the auto-tests from completing in a timely manner.
     */
    @Override
    protected boolean shouldTestStubs() {
        return false;
    }

    public void test_arraystore() {
        for (Class type : klasses) {
            if (type.isArray() && Kind.fromJava(type.getComponentType()) == Kind.fromJava(Object.class)) {
                Trace.line(TRACE_LEVEL, "aastore " + type.getName());
                final Object array = instances.get(type);
                arraystoreTest(array, instances.values());
            }
        }
    }

    public void test_isAssignableFrom() {
        for (Class<?> fromType : klasses) {
            Trace.line(TRACE_LEVEL, "isAssignableFrom " + fromType.getName());
            final ClassActor fromTypeActor = ClassActor.fromJava(fromType);
            for (Class<?> toType : klasses) {
                final ClassActor toTypeActor = ClassActor.fromJava(toType);
                final boolean apiAnswer = toType.isAssignableFrom(fromType);
                final boolean actorAnswer = toTypeActor.isAssignableFrom(fromTypeActor);
                if (fromTypeActor.kind.isWord) {
                    if (toTypeActor.kind != Kind.WORD) {
                        if (toType != Accessor.class && toType != Boxed.class && actorAnswer == true) {
                            toTypeActor.isAssignableFrom(fromTypeActor);
                            fail(fromType + " should not be assignable to non-word type " + toType);
                        }
                    } else {
                        assertTrue(apiAnswer == actorAnswer);
                    }
                } else {
                    if (apiAnswer != actorAnswer) {
                        fail(toType + " should " + (apiAnswer ? "" : "not ") + "be assignable from " + fromType);
                    }
                }
            }
        }
    }

    public void test_instanceof() {
        for (Class<?> type : klasses) {
            Trace.line(TRACE_LEVEL, "instanceof " + type.getName());
            instanceof_test(type, instances.values());
        }
    }

    public void test_checkcast() {
        for (Class<?> type : klasses) {
            if (!type.isPrimitive()) {
                Trace.line(TRACE_LEVEL, "checkcast " + type.getName());
                checkcast_test(type, instances.values());
            }
        }
    }

}
