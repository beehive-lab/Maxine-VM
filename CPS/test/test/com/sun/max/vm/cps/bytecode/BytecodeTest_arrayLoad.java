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

public abstract class BytecodeTest_arrayLoad<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_arrayLoad(String name) {
        super(name);
    }

    private int perform_iaload(int[] array, int index) {
        return array[index];
    }

    public void test_iaload() {
        final Method_Type method = compileMethod("perform_iaload", SignatureDescriptor.create(int.class, int[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void iaload() {
                confirmPresence();
            }
        };
        final int[] array = new int[3];
        array[1] = 123;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(1));
        assertTrue(result.asInt() == 123);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(3));
    }

    private long perform_laload(long[] array, int index) {
        return array[index];
    }

    public void test_laload() {
        final Method_Type method = compileMethod("perform_laload", SignatureDescriptor.create(long.class, long[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void laload() {
                confirmPresence();
            }
        };
        final long[] array = new long[3];
        array[1] = 234;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(1));
        assertTrue(result.asLong() == 234);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(3));
    }

    private float perform_faload(float[] array, int index) {
        return array[index];
    }

    public void test_faload() {
        final Method_Type method = compileMethod("perform_faload", SignatureDescriptor.create(float.class, float[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void faload() {
                confirmPresence();
            }
        };
        final float[] array = new float[3];
        array[2] = 3234;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(2));
        assertTrue(result.asFloat() == 3234);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(-1));
    }

    private double perform_daload(double[] array, int index) {
        return array[index];
    }

    public void test_daload() {
        final Method_Type method = compileMethod("perform_daload", SignatureDescriptor.create(double.class, double[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void daload() {
                confirmPresence();
            }
        };
        final double[] array = new double[5];
        final double x = 54353.232;
        array[1] = x;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(1));
        assertTrue(result.asDouble() == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(50));
    }

    private Object perform_aaload(Object[] array, int index) {
        return array[index];
    }

    public void test_aaload() {
        //Trace.on(3);
        final Method_Type method = compileMethod("perform_aaload", SignatureDescriptor.create(Object.class, Object[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void aaload() {
                confirmPresence();
            }
        };
        final Object[] array = new Object[5];
        array[0] = this;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(0));
        assertTrue(result.asObject() == this);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5));
    }

    private byte perform_baload_byte(byte[] array, int index) {
        return array[index];
    }

    public void test_baload_byte() {
        final Method_Type method = compileMethod("perform_baload_byte", SignatureDescriptor.create(byte.class, byte[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void baload() {
                confirmPresence();
            }
        };
        final byte[] array = new byte[5];
        final byte x = 112;
        array[2] = x;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(2));
        assertTrue(result.asByte() == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(7));
    }

    private boolean perform_baload_boolean(boolean[] array, int index) {
        return array[index];
    }

    public void test_baload_boolean() {
        final Method_Type method = compileMethod("perform_baload_boolean", SignatureDescriptor.create(boolean.class, boolean[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void baload() {
                confirmPresence();
            }
        };
        final boolean[] array = new boolean[5];
        array[2] = true;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(2));
        assertTrue(result.asBoolean());
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(7));
    }

    private char perform_caload(char[] array, int index) {
        return array[index];
    }

    public void test_caload() {
        final Method_Type method = compileMethod("perform_caload", SignatureDescriptor.create(char.class, char[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void caload() {
                confirmPresence();
            }
        };
        final char[] array = new char[5];
        final char x = 'x';
        array[3] = x;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(3));
        assertTrue(result.asChar() == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(5));
    }

    private short perform_saload(short[] array, int index) {
        return array[index];
    }

    public void test_saload() {
        final Method_Type method = compileMethod("perform_saload", SignatureDescriptor.create(short.class, short[].class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void saload() {
                confirmPresence();
            }
        };
        final short[] array = new short[100];
        final short x = 32;
        array[55] = x;
        final Value result = executeWithReceiver(method, ReferenceValue.from(array), IntValue.from(55));
        assertTrue(result.asShort() == x);
        executeWithReceiverAndExpectedException(method, ArrayIndexOutOfBoundsException.class, ReferenceValue.from(array), IntValue.from(101));
    }
}
