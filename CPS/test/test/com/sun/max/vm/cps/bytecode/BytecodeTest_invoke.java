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

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_invoke<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_invoke(String name) {
        super(name);
    }

    private int perform_invokevirtual_0(Integer number) {
        return number.intValue();
    }

    public void test_invokevirtual_0() {
        final Method_Type method = compileMethod("perform_invokevirtual_0", SignatureDescriptor.create(int.class, Integer.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void invokevirtual(int index) {
                confirmPresence();
            }
        };
        final int expectedResult = 1234;
        final Value result = executeWithReceiver(method, ReferenceValue.from(expectedResult));
        assertTrue(result.asInt() == expectedResult);
    }

    double virtual_2(double b, int a) {
        return b - a;
    }

    private double perform_invokevirtual_2(int a, double b) {
        return virtual_2(b, a);
    }

    public void test_invokevirtual_2() {
        final Method_Type method = compileMethod("perform_invokevirtual_2", SignatureDescriptor.create(double.class, int.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void invokevirtual(int index) {
                confirmPresence();
            }
        };
        final int a = 1234;
        final double b = 123123.12312;
        final Value result = executeWithReceiver(method, IntValue.from(a), DoubleValue.from(b));
        assertTrue(result.asDouble() == b - a);
    }

    private double special_2(double b, int a) {
        return b - a;
    }

    private double perform_invokespecial_2(int a, double b) {
        return special_2(b, a);
    }

    public void test_invokespecial_2() {
        final Method_Type method = compileMethod("perform_invokespecial_2", SignatureDescriptor.create(double.class, int.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void invokespecial(int index) {
                confirmPresence();
            }
        };
        final int a = 1234;
        final double b = 123123.12312;
        final Value result = executeWithReceiver(method, IntValue.from(a), DoubleValue.from(b));
        assertTrue(result.asDouble() == b - a);
    }

    private static int static_0() {
        return 12345;
    }

    private int perform_invokestatic_0() {
        return static_0();
    }

    public void test_invokestatic_0() {
        final Method_Type method = compileMethod("perform_invokestatic_0", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void invokestatic(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result.asInt() == 12345);
    }

    private static int static_1(int a) {
        return a - 12345;
    }

    private int perform_invokestatic_1(int a) {
        return static_1(a);
    }

    public void test_invokestatic_1() {
        final Method_Type method = compileMethod("perform_invokestatic_1", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void invokestatic(int index) {
                confirmPresence();
            }
        };
        final int a = 123123;
        final Value result = executeWithReceiver(method, IntValue.from(a));
        assertTrue(result.asInt() == a - 12345);
    }

    private static double static_2(int a, double b) {
        return b * (a - 12345);
    }

    private double perform_invokestatic_2(int a, double b) {
        return static_2(a, b);
    }

    public void test_invokestatic_2() {
        final Method_Type method = compileMethod("perform_invokestatic_2", SignatureDescriptor.create(double.class, int.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void invokestatic(int index) {
                confirmPresence();
            }
        };
        final int a = 123123;
        final double b = 1212.3244;
        final Value result = executeWithReceiver(method, IntValue.from(a), DoubleValue.from(b));
        assertTrue(result.asDouble() == b * (a - 12345));
    }

    private TestInterface interfaceInstance;

    private double perform_invokeinterface_2(int a, double b) {
        return interfaceInstance.interface_2(a, b);
    }

    public void test_invokeinterface_2() {
        interfaceInstance = new TestInterface() {
            public double interface_2(int a, double b) {
                return a - b;
            }
        };
        HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(TestInterface.class));
        HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(interfaceInstance.getClass()));
        final Method_Type method = compileMethod("perform_invokeinterface_2", SignatureDescriptor.create(double.class, int.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            protected void invokeinterface(int index, int count) {
                confirmPresence();
            }
        };
        final int a = 12312;
        final double b = 3323.8765;
        final Value result = executeWithReceiver(method, IntValue.from(a), DoubleValue.from(b));
        assertTrue(result.asDouble() == a - b);
    }
}
