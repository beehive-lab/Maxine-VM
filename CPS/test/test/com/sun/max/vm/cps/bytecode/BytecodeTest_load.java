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

public abstract class BytecodeTest_load<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_load(String name) {
        super(name);
    }

    private static int perform_iload(int a0, int a1, int a2, int a3, int a4) {
        return a0 + a1 + a2 + a3 + a4;
    }

    public void test_iload() {
        final Method_Type method = compileMethod("perform_iload", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void iload_0() {
                confirmPresence();
            }

            @Override public void iload_1() {
                confirmPresence();
            }

            @Override public void iload_2() {
                confirmPresence();
            }

            @Override public void iload_3() {
                confirmPresence();
            }

            @Override public void iload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(10), IntValue.from(200), IntValue.from(3000), IntValue.from(40000), IntValue.from(500000));
        assertTrue(result.asInt() == 543210);
    }

    private static float perform_fload(float a0, float a1, float a2, float a3, float a4) {
        return a0 + a1 + a2 + a3 + a4;
    }

    public void test_fload() {
        final Method_Type method = compileMethod("perform_fload", SignatureDescriptor.create(float.class, float.class, float.class, float.class, float.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void fload_0() {
                confirmPresence();
            }

            @Override public void fload_1() {
                confirmPresence();
            }

            @Override public void fload_2() {
                confirmPresence();
            }

            @Override public void fload_3() {
                confirmPresence();
            }

            @Override public void fload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(90), FloatValue.from(800), FloatValue.from(7000), FloatValue.from(60000), FloatValue.from(500000));
        assertTrue(result.asFloat() == 567890);
    }

    private static long perform_lload_even(long a0, long a2, long a4) {
        return a0 + a2 + a4;
    }

    public void test_lload_even() {
        final Method_Type method = compileMethod("perform_lload_even", SignatureDescriptor.create(long.class, long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lload_0() {
                confirmPresence();
            }

            @Override public void lload_2() {
                confirmPresence();
            }

            @Override public void lload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(50), LongValue.from(600), LongValue.from(7000));
        assertTrue(result.asLong() == 7650);
    }

    private static long perform_lload_odd(int unused, long a1, long a3, long a5) {
        return a1 + a3 + a5;
    }

    public void test_lload_odd() {
        final Method_Type method = compileMethod("perform_lload_odd", SignatureDescriptor.create(long.class, int.class, long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lload_1() {
                confirmPresence();
            }

            @Override public void lload_3() {
                confirmPresence();
            }

            @Override public void lload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(0), LongValue.from(30), LongValue.from(400), LongValue.from(5000));
        assertTrue(result.asLong() == 5430);
    }

    private static double perform_dload_even(double a0, double a2, double a4) {
        return a0 + a2 + a4;
    }

    public void test_dload_even() {
        final Method_Type method = compileMethod("perform_dload_even", SignatureDescriptor.create(double.class, double.class, double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dload_0() {
                confirmPresence();
            }

            @Override public void dload_2() {
                confirmPresence();
            }

            @Override public void dload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(10), DoubleValue.from(200), DoubleValue.from(3000));
        assertTrue(result.asDouble() == 3210);
    }

    private static double perform_dload_odd(int unused, double a1, double a3, double a5) {
        return a1 + a3 + a5;
    }

    public void test_dload_odd() {
        final Method_Type method = compileMethod("perform_dload_odd", SignatureDescriptor.create(double.class, int.class, double.class, double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dload_1() {
                confirmPresence();
            }

            @Override public void dload_3() {
                confirmPresence();
            }

            @Override public void dload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, IntValue.from(0), DoubleValue.from(30), DoubleValue.from(500), DoubleValue.from(7000));
        assertTrue(result.asDouble() == 7530);
    }

    private static Integer perform_aload(Integer a0, Integer a1, Integer a2, Integer a3, Integer a4) {
        return a0 + a1 + a2 + a3 + a4;
    }

    public void test_aload() {
        final Method_Type method = compileMethod("perform_aload",
                        SignatureDescriptor.create(Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void aload_0() {
                confirmPresence();
            }

            @Override public void aload_1() {
                confirmPresence();
            }

            @Override public void aload_2() {
                confirmPresence();
            }

            @Override public void aload_3() {
                confirmPresence();
            }

            @Override public void aload(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method, ReferenceValue.from(70), ReferenceValue.from(100), ReferenceValue.from(2000), ReferenceValue.from(40000),
                        ReferenceValue.from(500000));
        assertTrue(result.asObject().equals(542170));
    }
}
