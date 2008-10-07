/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=bbc6dd75-1be2-4b28-92fb-65ad32cff48c*/
package test.com.sun.max.vm.compiler.bytecode;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.*;
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

    private static Class[] _classes;
    private static Map<Class, Object> _instances;

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
        if (_instances != null) {
            return;
        }
        _instances = new HashMap<Class, Object>();

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
                classes.add(kind.toJava());
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
                _instances.put(arrayClass, array);
            }

            if (c == int.class || c == String.class) {
                final Object array = arrayOf(c, 255);
                final Class<?> arrayClass = array.getClass();
                allClasses.add(arrayClass);
                _instances.put(arrayClass, array);
            }

            if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                try {
                    _instances.put(c, c.newInstance());
                } catch (Exception e) {
                }
            }
        }

        assert !_instances.containsKey(null);
        _instances.put(null, null);

        _classes = allClasses.toArray(new Class[classes.size()]);
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

    private int _testCount;

    private String methodName(String testDescription) {
        return "perform" + (++_testCount) + "_" + testDescription;
    }

    private void instanceof_test(final Class type, Iterable<Object> objects) {
        final String targetTypeName = type.getSimpleName();
        final String methodName = methodName("perform_instanceof_" + targetTypeName);
        final Method_Type method = new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(boolean.class, Object.class)) {
            @Override
            public void generateCode() {
                aload(0);
                instanceof_(PoolConstantFactory.createClassConstant(type));
                ireturn();
            }
        }.compile(getClass());
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
        final Method_Type method = new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(void.class, array.getClass(), Object.class)) {
            @Override
            public void generateCode() {
                aload(0); // array
                iconst(0); // index
                aload(1); // value
                aastore();
                vreturn();
            }
        }.compile(getClass());
        for (Object value : values) {
            final Class valueType = value == null ? Object.class : value.getClass();
            final String valueTypeName = value == null ? "null" : valueType.getSimpleName();
            final ClassActor arrayClassActor = ClassActor.fromJava(array.getClass());
            final ClassActor componentClassActor = arrayClassActor.componentClassActor();
            final boolean expectArrayStoreException = componentClassActor.kind() == Kind.WORD || (value != null && !componentClassActor.isAssignableFrom(ClassActor.fromJava(value.getClass())));
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
        final Method_Type method = new TestBytecodeAssembler(true, sanitizeMethodName(methodName), SignatureDescriptor.create(void.class, Object.class)) {
            @Override
            public void generateCode() {
                aload(0); // object
                checkcast(PoolConstantFactory.createClassConstant(targetType));
                pop();
                vreturn();
            }
        }.compile(getClass());
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
        for (Class type : _classes) {
            if (type.isArray() && Kind.fromJava(type.getComponentType()) == Kind.fromJava(Object.class)) {
                Trace.line(TRACE_LEVEL, "aastore " + type.getName());
                final Object array = _instances.get(type);
                arraystoreTest(array, _instances.values());
            }
        }
    }

    public void test_isAssignableFrom() {
        for (Class<?> fromType : _classes) {
            Trace.line(TRACE_LEVEL, "isAssignableFrom " + fromType.getName());
            final ClassActor fromTypeActor = ClassActor.fromJava(fromType);
            for (Class<?> toType : _classes) {
                final ClassActor toTypeActor = ClassActor.fromJava(toType);
                final boolean apiAnswer = toType.isAssignableFrom(fromType);
                final boolean actorAnswer = toTypeActor.isAssignableFrom(fromTypeActor);
                if (fromTypeActor.kind() == Kind.WORD) {
                    if (toTypeActor.kind() != Kind.WORD) {
                        if (toType != Accessor.class && toType != UnsafeBox.class && actorAnswer == true) {
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
        for (Class<?> type : _classes) {
            Trace.line(TRACE_LEVEL, "instanceof " + type.getName());
            instanceof_test(type, _instances.values());
        }
    }

    public void test_checkcast() {
        for (Class<?> type : _classes) {
            if (!type.isPrimitive()) {
                Trace.line(TRACE_LEVEL, "checkcast " + type.getName());
                checkcast_test(type, _instances.values());
            }
        }
    }

}
