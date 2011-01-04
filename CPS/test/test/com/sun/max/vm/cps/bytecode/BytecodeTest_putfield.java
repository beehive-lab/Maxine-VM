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

import com.sun.max.unsafe.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_putfield<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_putfield(String name) {
        super(name);
    }

    private byte byteField = 0;

    private void perform_putfield_byte() {
        byteField = 111;
    }

    public void test_putfield_byte() {
        final Method_Type method = compileMethod("perform_putfield_byte", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(byteField == 111);
    }

    private boolean booleanField = false;

    private void perform_putfield_boolean() {
        booleanField = true;
    }

    public void test_putfield_boolean() {
        final Method_Type method = compileMethod("perform_putfield_boolean", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(booleanField);
    }

    private short shortField = 0;

    private void perform_putfield_short() {
        shortField = 222;
    }

    public void test_putfield_short() {
        final Method_Type method = compileMethod("perform_putfield_short", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(shortField == 222);
    }

    private char charField = 0;

    private void perform_putfield_char() {
        charField = 333;
    }

    public void test_putfield_char() {
        final Method_Type method = compileMethod("perform_putfield_char", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(charField == 333);
    }

    private int intField = 0;

    private void perform_putfield_int() {
        intField = 44;
    }

    public void test_putfield_int() {
        final Method_Type method = compileMethod("perform_putfield_int", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(intField == 44);
    }

    private float floatField = 0;

    private void perform_putfield_float() {
        floatField = 55;
    }

    public void test_putfield_float() {
        final Method_Type method = compileMethod("perform_putfield_float", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(floatField == 55);
    }

    private long longField = 0;

    private void perform_putfield_long() {
        longField = 66;
    }

    public void test_putfield_long() {
        final Method_Type method = compileMethod("perform_putfield_long", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(longField == 66);
    }

    private double doubleField = 0;

    private void perform_putfield_double() {
        doubleField = 77;
    }

    public void test_putfield_double() {
        final Method_Type method = compileMethod("perform_putfield_double", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(doubleField == 77);
    }

    private Word wordField;

    private void perform_putfield_word() {
        wordField = Offset.fromInt(88);
    }

    public void test_putfield_word() {
        wordField = Offset.zero();
        final Method_Type method = compileMethod("perform_putfield_word", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(wordField.asOffset().toInt() == 88);
    }

    private Object referenceField = null;

    private void perform_putfield_reference() {
        referenceField = Kind.REFERENCE;
    }

    public void test_putfield_reference() {
        final Method_Type method = compileMethod("perform_putfield_reference", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void putfield(int index) {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(referenceField == Kind.REFERENCE);
    }

}
