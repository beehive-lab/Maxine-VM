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
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_putstatic<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_putstatic(String name) {
        super(name);
    }

    private static byte _byteField = 0;

    private static void perform_putstatic_byte() {
        _byteField = 111;

    }

    public void test_putstatic_byte() {
        final Method_Type method = compileMethod("perform_putstatic_byte", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_byteField == 111);
    }

    private static boolean _booleanField = false;

    private static void perform_putstatic_boolean() {
        _booleanField = true;
    }

    public void test_putstatic_boolean() {
        final Method_Type method = compileMethod("perform_putstatic_boolean", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_booleanField);
    }

    private static short _shortField = 0;

    private static void perform_putstatic_short() {
        _shortField = 222;
    }

    public void test_putstatic_short() {
        final Method_Type method = compileMethod("perform_putstatic_short", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_shortField == 222);
    }

    private static char _charField = 0;

    private static void perform_putstatic_char() {
        _charField = 333;
    }

    public void test_putstatic_char() {
        final Method_Type method = compileMethod("perform_putstatic_char", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_charField == 333);
    }

    private static int _intField = 0;

    private static void perform_putstatic_int() {
        _intField = 44;
    }

    public void test_putstatic_int() {
        final Method_Type method = compileMethod("perform_putstatic_int", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_intField == 44);
    }

    private static float _floatField = 0;

    private static void perform_putstatic_float() {
        _floatField = 55;
    }

    public void test_putstatic_float() {
        final Method_Type method = compileMethod("perform_putstatic_float", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_floatField == 55);
    }

    private static long _longField = 0;

    private static void perform_putstatic_long() {
        _longField = 66;
    }

    public void test_putstatic_long() {
        final Method_Type method = compileMethod("perform_putstatic_long", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_longField == 66);
    }

    private static double _doubleField = 0;

    private static void perform_putstatic_double() {
        _doubleField = 77;
    }

    public void test_putstatic_double() {
        final Method_Type method = compileMethod("perform_putstatic_double", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_doubleField == 77);
    }

    private static Word _wordField;

    private static void perform_putstatic_word() {
        _wordField = Offset.fromInt(88);
    }

    public void test_putstatic_word() {
        _wordField = Offset.zero();
        final Method_Type method = compileMethod("perform_putstatic_word", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_wordField.asOffset().toInt() == 88);
    }

    private static Object _referenceField = null;

    private static void perform_putstatic_reference() {
        _referenceField = Kind.REFERENCE;
    }

    public void test_putstatic_reference() {
        final Method_Type method = compileMethod("perform_putstatic_reference", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(_referenceField == Kind.REFERENCE);
    }

}
