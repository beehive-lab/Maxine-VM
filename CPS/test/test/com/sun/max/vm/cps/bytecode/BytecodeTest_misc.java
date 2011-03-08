/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.create.*;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_misc<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_misc(String name) {
        super(name);
    }

    public void test_nop() {
        final String className = getClass().getName() + "_test_nop";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(0, 1);
        code.nop();
        code.bipush(1);
        code.nop();
        code.pop();
        code.nop();
        code.vreturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_nop", SignatureDescriptor.create(void.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_nop", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void nop() {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result, VoidValue.VOID);
    }

    private int perform_iinc(int a) {
        int b = a;
        b += 3;
        return b;
    }

    public void test_iinc() {
        final Method_Type method = compileMethod("perform_iinc", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            public void iinc() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(4));
        assertTrue(result.asInt() == 7);
    }

    public void test_goto() {
        final String className = getClass().getName() + "_test_goto";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(0, 1);
        code.goto_(6); // #0
        code.bipush(0); // #3
        code.ireturn(); // #5
        code.bipush(1); // #6
        code.ireturn(); // #8
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_goto", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_goto", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void goto_(int offset) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), 1);
    }

    public void test_goto_w() {
        final String className = getClass().getName() + "_test_goto_w";

        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(0, 1);
        code.goto_w(8); // #0
        code.bipush(0); // #5
        code.ireturn(); // #7
        code.bipush(1); // #8
        code.ireturn(); // #10
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_goto_w", SignatureDescriptor.create(int.class), code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_goto_w", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            public void goto_w_(int offset) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertEquals(result.asInt(), 1);
    }

    private int perform_arraylength(int[] array) {
        return array.length;
    }

    public void test_arraylength() {
        final Method_Type method = compileMethod("perform_arraylength", SignatureDescriptor.create(int.class, int[].class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void arraylength() {
                confirmPresence();
            }
        };
        final int[] array = new int[5];
        final Value result = executeWithReceiver(method, ReferenceValue.from(array));
        assertTrue(result.asInt() == 5);
    }

    private int perform_checkcast1(Object x) {
        final Integer n = (Integer) x;
        return n.intValue();
    }

    /**
     * Tests casting of non-array, non-null value.
     */
    public void test_checkcast1() {
        final Method_Type method = compileMethod("perform_checkcast1", SignatureDescriptor.create(int.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void checkcast(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, ReferenceValue.from(new Integer(345)));
        assertTrue(result.asInt() == 345);
        executeWithReceiverAndExpectedException(method, ClassCastException.class, ReferenceValue.from(new Float(345)));
    }

    private boolean perform_checkcast2(Object x) {
        final String s = (String) x;
        return s == null;
    }

    /**
     * Tests that casting of null to any type works.
     */
    public void test_checkcast2() {
        final Method_Type method = compileMethod("perform_checkcast2", SignatureDescriptor.create(boolean.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void checkcast(int index) {
                confirmPresence();
            }
        };
        assertTrue(perform_checkcast2(null));
        final Value result = executeWithReceiver(method, ReferenceValue.from(null));
        assertTrue(result.asBoolean());
    }

    private boolean perform_checkcast3(Object x) {
        final int[] a = (int[]) x;
        return a[3] == 3;
    }

    /**
     * Tests casting of a primitive array type.
     */
    public void test_checkcast3() {
        final Method_Type method = compileMethod("perform_checkcast3", SignatureDescriptor.create(boolean.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void checkcast(int index) {
                confirmPresence();
            }
        };
        final int[] arg = new int[]{0, 1, 2, 3};
        assertTrue(perform_checkcast3(arg));
        final Value result = executeWithReceiver(method, ReferenceValue.from(arg));
        assertTrue(result.asBoolean());
    }

    private boolean perform_checkcast4(Object x) {
        final String[] a = (String[]) x;
        return a[3].equals("3");
    }

    /**
     * Tests casting of a primitive array type.
     */
    public void test_checkcast4() {
        final Method_Type method = compileMethod("perform_checkcast4", SignatureDescriptor.create(boolean.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void checkcast(int index) {
                confirmPresence();
            }
        };
        final String[] arg = new String[]{"0", "1", "2", "3"};
        assertTrue(perform_checkcast4(arg));
        final Value result = executeWithReceiver(method, ReferenceValue.from(arg));
        assertTrue(result.asBoolean());
    }

    private boolean perform_instanceof(Object x) {
        return x instanceof Integer;
    }

    public void test_instanceof() {
        final Method_Type method = compileMethod("perform_instanceof", SignatureDescriptor.create(boolean.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void instanceof_(int index) {
                confirmPresence();
            }
        };
        Value result = executeWithReceiver(method, ReferenceValue.from(new Integer(345)));
        assertTrue(result.asBoolean());
        result = executeWithReceiver(method, ReferenceValue.from(new Float(345)));
        assertFalse(result.asBoolean());
        assertFalse(perform_instanceof(null));
        result = executeWithReceiver(method, ReferenceValue.from(null));
        assertFalse(result.asBoolean());
    }
}
