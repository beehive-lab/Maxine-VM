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
