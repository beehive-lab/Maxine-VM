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
