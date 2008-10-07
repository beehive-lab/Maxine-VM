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
/*VCSID=d5ee3d28-562c-4fd2-8663-63b49a4248d7*/
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.prototype.*;
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

    private TestInterface _interfaceInstance;

    private double perform_invokeinterface_2(int a, double b) {
        return _interfaceInstance.interface_2(a, b);
    }

    public void test_invokeinterface_2() {
        _interfaceInstance = new TestInterface() {
            public double interface_2(int a, double b) {
                return a - b;
            }
        };
        PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(TestInterface.class));
        PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(_interfaceInstance.getClass()));
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
