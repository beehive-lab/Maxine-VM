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
/*VCSID=79e9a9f3-7468-4107-9d40-b171a2a70e51*/
package test.com.sun.max.vm.jdk;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class JdkTest_System<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public JdkTest_System(String name) {
        super(name);
    }

    private static final int LENGTH = 10;

    public void test_sameNullsArrayCopy() throws Exception {
        final Method_Type method = compileMethod(JDK_java_lang_System.class, "arraycopy");
        final Object[] array = new Array[LENGTH];
        for (int length = 0; length < LENGTH; length++) {
            for (int i = 0; i < length; i++) {
                execute(method, ReferenceValue.from(array), IntValue.from(i), ReferenceValue.from(array), IntValue.from(i + 1), IntValue.from(length - i));
            }
            for (int i = 0; i < length; i++) {
                execute(method, ReferenceValue.from(array), IntValue.from(i + 1), ReferenceValue.from(array), IntValue.from(i), IntValue.from(length - i));
            }
        }
    }

    private static Object createEmptyArray(Kind kind) {
        return Array.newInstance(kind.toJava(), LENGTH);
    }

    private static Object createFullArray(Kind kind) {
        final Object array = createEmptyArray(kind);
        for (int i = 0; i < Array.getLength(array); i++) {
            if (kind == Kind.REFERENCE) {
                final Object value = ReferenceValue.from(new Integer(i));
                Array.set(array, i, value);
            } else {
                final Object value = kind.convert(IntValue.from(i)).asBoxedJavaValue();
                Array.set(array, i, value);
            }
        }
        return array;
    }

    private boolean areArraysEqual(Kind kind, Object array1, Object array2) {
        for (int i = 0; i < LENGTH; i++) {
            final Value value1 = kind.asValue(Array.get(array1, i));
            final Value value2 = kind.asValue(Array.get(array2, i));
            if (!value1.equals(value2)) {
                return false;
            }
        }
        return true;
    }

    private void performArrayCopyWithException(Method_Type method, Kind kind, int fromIndex, int toIndex, int length) throws Exception {
        final Object fromArray = createFullArray(kind);
        final Object toArray = createEmptyArray(kind);

        executeWithException(method, ReferenceValue.from(fromArray), IntValue.from(fromIndex), ReferenceValue.from(toArray), IntValue.from(toIndex), IntValue.from(length));

        final Object checkArray = createEmptyArray(kind);
        System.arraycopy(fromArray, fromIndex, checkArray, toIndex, length);
        assertTrue(areArraysEqual(kind, checkArray, toArray));
    }

    private void performArrayCopyWithIndexOutOfBoundsException(Method_Type method, Kind kind, int fromIndex, int toIndex, int length) throws Exception {
        try {
            performArrayCopyWithException(method, kind, fromIndex, toIndex, length);
        } catch (InvocationTargetException invocationTargetException) {
            if (!IndexOutOfBoundsException.class.isInstance(invocationTargetException.getTargetException())) {
                fail();
            }
        }
    }

    private void performArrayCopy(Method_Type method, Kind kind, int fromIndex, int toIndex, int length) throws Exception {
        try {
            performArrayCopyWithException(method, kind, fromIndex, toIndex, length);
        } catch (InvocationTargetException invocationTargetException) {
            fail();
        }
    }

    private void performArrayCopyForKind(Method_Type method, Kind kind) throws Exception {
        performArrayCopyWithIndexOutOfBoundsException(method, kind, -1, 0, 3);
        performArrayCopyWithIndexOutOfBoundsException(method, kind, 0, -1, 3);
        performArrayCopyWithIndexOutOfBoundsException(method, kind, 0, 0, -1);
        performArrayCopyWithIndexOutOfBoundsException(method, kind, 3, 0, LENGTH - 2);
        performArrayCopyWithIndexOutOfBoundsException(method, kind, 0, 3, LENGTH - 2);
        performArrayCopy(method, kind, 0, 0, LENGTH);
        performArrayCopy(method, kind, 3, 4, 5);
        performArrayCopy(method, kind, 3, 4, 0);
        performArrayCopy(method, kind, 3, 4, 1);
    }

    public void test_arrayCopyForKinds() throws Exception {
        final Method_Type method = compileMethod(JDK_java_lang_System.class, "arraycopy");
        for (Kind kind : Kind.EXTENDED_PRIMITIVE_VALUES) {
            performArrayCopyForKind(method, kind);
        }
    }

}
