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

public abstract class BytecodeTest_store<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_store(String name) {
        super(name);
    }

    // CheckStyle: stop parameter assignment check

    private static int perform_istore(int a0, int a1, int a2, int a3, int a4) {
        a0 = a0 + a4;
        a1 = a0 + a1;
        a2 = a1 + a2;
        a3 = a2 + a3;
        a4 = a3;
        return a4;
    }

    private static float perform_fstore(float a0, float a1, float a2, float a3, float a4) {
        a0 = a0 + a4;
        a1 = a0 + a1;
        a2 = a1 + a2;
        a3 = a2 + a3;
        a4 = a3;
        return a4;
    }

    private static long perform_lstore_even(long a0, long a2, long a4) {
        a0 = a0 + a4;
        a2 = a0 + a2;
        a4 = a2;
        return a4;
    }

    private static long perform_lstore_odd(int unused, long a1, long a3, long a5) {
        a1 = a1 + a5;
        a3 = a1 + a3;
        a5 = a3;
        return a5;
    }

    private static double perform_dstore_even(double a0, double a2, double a4) {
        a0 = a0 + a4;
        a2 = a0 + a2;
        a4 = a2;
        return a4;
    }

    private static double perform_dstore_odd(int unused, double a1, double a3, double a5) {
        a1 = a1 + a5;
        a3 = a1 + a3;
        a5 = a3;
        return a5;
    }

    private static Integer perform_astore(Integer a0, Integer a1, Integer a2, Integer a3, Integer a4) {
        final int dummy = a0;
        a0 = a1;
        a1 = a2;
        a2 = a3;
        a3 = a4;
        a4 = dummy;
        return a0 + a1 + a2 + a3 + a4;
    }

    // CheckStyle: resume parameter assignment check

    public void test_istore() {
        final Method_Type method = compileMethod("perform_istore", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void istore_0() {
                confirmPresence();
            }

            @Override
            public void istore_1() {
                confirmPresence();
            }

            @Override
            public void istore_2() {
                confirmPresence();
            }

            @Override
            public void istore_3() {
                confirmPresence();
            }

            @Override
            public void istore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(10), IntValue.from(200), IntValue.from(3000), IntValue.from(40000), IntValue.from(500000));
        assertTrue(result.asInt() == 543210);
    }

    public void test_fstore() {
        final Method_Type method = compileMethod("perform_fstore", SignatureDescriptor.create(float.class, float.class, float.class, float.class, float.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void fstore_0() {
                confirmPresence();
            }

            @Override
            public void fstore_1() {
                confirmPresence();
            }

            @Override
            public void fstore_2() {
                confirmPresence();
            }

            @Override
            public void fstore_3() {
                confirmPresence();
            }

            @Override
            public void fstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(90), FloatValue.from(800), FloatValue.from(7000), FloatValue.from(60000), FloatValue.from(500000));
        assertTrue(result.asFloat() == 567890);
    }

    public void test_lstore_even() {
        final Method_Type method = compileMethod("perform_lstore_even", SignatureDescriptor.create(long.class, long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void lstore_0() {
                confirmPresence();
            }

            @Override
            public void lstore_2() {
                confirmPresence();
            }

            @Override
            public void lstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(50), LongValue.from(600), LongValue.from(7000));
        assertTrue(result.asLong() == 7650);
    }

    public void test_lstore_odd() {
        final Method_Type method = compileMethod("perform_lstore_odd", SignatureDescriptor.create(long.class, int.class, long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void lstore_1() {
                confirmPresence();
            }

            @Override
            public void lstore_3() {
                confirmPresence();
            }

            @Override
            public void lstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(0), LongValue.from(30), LongValue.from(400), LongValue.from(5000));
        assertTrue(result.asLong() == 5430);
    }

    public void test_dstore_even() {
        final Method_Type method = compileMethod("perform_dstore_even", SignatureDescriptor.create(double.class, double.class, double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void dstore_0() {
                confirmPresence();
            }

            @Override
            public void dstore_2() {
                confirmPresence();
            }

            @Override
            public void dstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(10), DoubleValue.from(200), DoubleValue.from(3000));
        assertTrue(result.asDouble() == 3210);
    }

    public void test_dstore_odd() {
        final Method_Type method = compileMethod("perform_dstore_odd", SignatureDescriptor.create(double.class, int.class, double.class, double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void dstore_1() {
                confirmPresence();
            }

            @Override
            public void dstore_3() {
                confirmPresence();
            }

            @Override
            public void dstore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(0), DoubleValue.from(30), DoubleValue.from(500), DoubleValue.from(7000));
        assertTrue(result.asDouble() == 7530);
    }

    public void test_astore() {
        final Method_Type method = compileMethod("perform_astore",
                        SignatureDescriptor.create(Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void astore_0() {
                confirmPresence();
            }

            @Override
            public void astore_1() {
                confirmPresence();
            }

            @Override
            public void astore_2() {
                confirmPresence();
            }

            @Override
            public void astore_3() {
                confirmPresence();
            }

            @Override
            public void astore(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, ReferenceValue.from(70), ReferenceValue.from(100), ReferenceValue.from(2000), ReferenceValue.from(40000),
                        ReferenceValue.from(500000));
        assertTrue(result.asObject().equals(542170));
    }
}
