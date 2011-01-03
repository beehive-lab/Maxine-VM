/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.ir.*;
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), IntValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5), IntValue.from(x));
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

            final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), LongValue.from(x));
            assertTrue(result == VoidValue.VOID);
            assertTrue(array[index] == x);
            executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(-1), LongValue.from(x));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), FloatValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(7), FloatValue.from(x));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), DoubleValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5), DoubleValue.from(x));
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
        result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ReferenceValue.NULL);
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == null);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5), ReferenceValue.from(x));
        executeWithReceiverAndExpectedException(method, ArrayStoreException.class, ReferenceValue.from(array), IntValue.from(index), ReferenceValue.from(new Object()));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ByteValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(-2), ByteValue.from(x));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), BooleanValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5), BooleanValue.from(x));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), CharValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(10), CharValue.from(x));
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
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(index), ShortValue.from(x));
        assertTrue(result == VoidValue.VOID);
        assertTrue(array[index] == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5), ShortValue.from(x));
    }
}
