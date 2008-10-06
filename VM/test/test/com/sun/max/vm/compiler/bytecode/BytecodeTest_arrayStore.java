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
/*VCSID=550d68ee-a774-4de6-bd82-ed1ca5011561*/
package test.com.sun.max.vm.compiler.bytecode;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_arrayStore<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_arrayStore(String name) {
        super(name);
    }

    private void perform_iastore(int[] array, int index, int value) {
        array[index] = value;
    }

    public void test_iastore() {
        final Method_Type method = compileMethod("perform_iastore", SignatureDescriptor.create(void.class, int[].class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void iastore() {
                confirmPresence();
            }
        };
        final int[] array = new int[5];
        final int index = 1;
        final int x = 123;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), IntValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(5), IntValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getTargetException() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_lastore(long[] array, int index, long value) {
        array[index] = value;
    }

    public void test_lastore() {
       // Trace.on(3);
        try {
            final Method_Type method = compileMethod("perform_lastore", SignatureDescriptor.create(void.class, long[].class, int.class, long.class));
            new BytecodeConfirmation(method.classMethodActor()) {
                @Override
                public void lastore() {
                    confirmPresence();
                }
            };
            final long[] array = new long[5];
            final int index = 1;
            final long x = 123;

            Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), LongValue.from(x));
            assertTrue(result == VoidValue.VOID);
            assertTrue(array[index] == x);
            try {
                result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(-1), LongValue.from(x));
                fail();
            } catch (InvocationTargetException invocationTargetException) {
                assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
            }
        } finally {
           // Trace.on(1);
        }
    }

    private void perform_fastore(float[] array, int index, float value) {
        array[index] = value;
    }

    public void test_fastore() {
        final Method_Type method = compileMethod("perform_fastore", SignatureDescriptor.create(void.class, float[].class, int.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void fastore() {
                confirmPresence();
            }
        };
        final float[] array = new float[5];
        final int index = 0;
        final float x = 23434;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), FloatValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(7), FloatValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_dastore(double[] array, int index, double value) {
        array[index] = value;
    }

    public void test_dastore() {
        final Method_Type method = compileMethod("perform_dastore", SignatureDescriptor.create(void.class, double[].class, int.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void dastore() {
                confirmPresence();
            }
        };
        final double[] array = new double[5];
        final int index = 4;
        final double x = 12123.8245;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), DoubleValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(5), DoubleValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_aastore(Object[] array, int index, Object value) {
        array[index] = value;
    }

    public void test_aastore() {
        final Method_Type method = compileMethod("perform_aastore", SignatureDescriptor.create(void.class, Object[].class, int.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void aastore() {
                confirmPresence();
            }
        };
        final Object[] array = new String[5];
        final int index = 4;
        final Object x = "x";
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ReferenceValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(5), ReferenceValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(index), ReferenceValue.from(new Object()));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayStoreException);
        }
    }

    private void perform_bastore_byte(byte[] array, int index, byte value) {
        array[index] = value;
    }

    public void test_bastore_byte() {
        final Method_Type method = compileMethod("perform_bastore_byte", SignatureDescriptor.create(void.class, byte[].class, int.class, byte.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void bastore() {
                confirmPresence();
            }
        };
        final byte[] array = new byte[5];
        final int index = 3;
        final byte x = 55;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ByteValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(-2), ByteValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_bastore_boolean(boolean[] array, int index, boolean value) {
        array[index] = value;
    }

    public void test_bastore_boolean() {
        final Method_Type method = compileMethod("perform_bastore_boolean", SignatureDescriptor.create(void.class, boolean[].class, int.class, boolean.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void bastore() {
                confirmPresence();
            }
        };
        final boolean[] array = new boolean[5];
        final int index = 2;
        final boolean x = true;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), BooleanValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(5), BooleanValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_castore(char[] array, int index, char value) {
        array[index] = value;
    }

    public void test_castore() {
        final Method_Type method = compileMethod("perform_castore", SignatureDescriptor.create(void.class, char[].class, int.class, char.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void castore() {
                confirmPresence();
            }
        };
        final char[] array = new char[5];
        final int index = 1;
        final char x = 'x';
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), CharValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(10), CharValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }

    private void perform_sastore(short[] array, int index, short value) {
        array[index] = value;
    }

    public void test_sastore() {
        final Method_Type method = compileMethod("perform_sastore", SignatureDescriptor.create(void.class, short[].class, int.class, short.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void sastore() {
                confirmPresence();
            }
        };
        final short[] array = new short[5];
        final int index = 1;
        final short x = 22;
        Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ShortValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        try {
            result = executeWithReceiverAndException(method, ReferenceValue.from(array), IntValue.from(5), ShortValue.from(x));
            fail();
        } catch (InvocationTargetException invocationTargetException) {
            assertTrue(invocationTargetException.getCause() instanceof ArrayIndexOutOfBoundsException);
        }
    }
}
