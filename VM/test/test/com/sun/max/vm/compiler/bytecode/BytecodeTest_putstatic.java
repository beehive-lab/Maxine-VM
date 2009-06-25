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

    private static byte byteField = 0;

    private static void perform_putstatic_byte() {
        byteField = 111;

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
        assertTrue(byteField == 111);
    }

    private static boolean booleanField = false;

    private static void perform_putstatic_boolean() {
        booleanField = true;
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
        assertTrue(booleanField);
    }

    private static short shortField = 0;

    private static void perform_putstatic_short() {
        shortField = 222;
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
        assertTrue(shortField == 222);
    }

    private static char charField = 0;

    private static void perform_putstatic_char() {
        charField = 333;
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
        assertTrue(charField == 333);
    }

    private static int intField = 0;

    private static void perform_putstatic_int() {
        intField = 44;
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
        assertTrue(intField == 44);
    }

    private static float floatField = 0;

    private static void perform_putstatic_float() {
        floatField = 55;
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
        assertTrue(floatField == 55);
    }

    private static long longField = 0;

    private static void perform_putstatic_long() {
        longField = 66;
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
        assertTrue(longField == 66);
    }

    private static double doubleField = 0;

    private static void perform_putstatic_double() {
        doubleField = 77;
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
        assertTrue(doubleField == 77);
    }

    private static Word wordField;

    private static void perform_putstatic_word() {
        wordField = Offset.fromInt(88);
    }

    public void test_putstatic_word() {
        wordField = Offset.zero();
        final Method_Type method = compileMethod("perform_putstatic_word", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void putstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result == VoidValue.VOID);
        assertTrue(wordField.asOffset().toInt() == 88);
    }

    private static Object referenceField = null;

    private static void perform_putstatic_reference() {
        referenceField = Kind.REFERENCE;
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
        assertTrue(referenceField == Kind.REFERENCE);
    }

}
