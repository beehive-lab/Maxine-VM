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

/**
 * The tests are divided in two cases for each type of field: whether the field is resolved or not.
 * To test the case of unresolved field, we arrange for the method whose compilation is being tested to be passed
 * an argument that is an instance of the UnresolvedClassUnderTest such that a field of the argument is read,
 * and the UnresolvedClassUnderTest is not loaded in the prototype VM. So there are two test cases per
 * type of field: test_getfield_XXX and test_unresolved_getfield_XXX.
 */
public abstract class BytecodeTest_getfield<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_getfield(String name) {
        super(name);
    }

    private byte byteField = 111;

    /**
     * Testing for a byte field when the field is resolved at compile-time
     * (so the compiler will not issue a guarded field load).
     */
    private byte perform_getfield_byte() {
        return byteField;
    }

    /**
     * Testing for a byte field when the field is resolved at compile-time
     * (so the compiler will issue a guarded field load).
     */
    private byte perform_getfield_byte(UnresolvedClassUnderTest u) {
        return u.byteField;
    }

    private void do_getfield_byte(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asByte() == byteField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asByte() == u.byteField);
        }
        assertTrue(result.asByte() == 111);
    }
    public void test_resolved_getfield_byte() {
        final Method_Type method = compileMethod("perform_getfield_byte", SignatureDescriptor.create(byte.class));
        do_getfield_byte(method, null);
    }

    public void test_unresolved_getfield_byte() {
        // NOTE: using UnresolvedClassUnderTest.class here cause the loading of the UnresolvedClassUnderTest in the host VM, not in the Target.
        // So we do have the desired effect with respect to the test: testing the compilation of a "unresolved" field.
        final Method_Type method = compileMethod("perform_getfield_byte", SignatureDescriptor.create(byte.class, UnresolvedClassUnderTest.class));
        do_getfield_byte(method, new UnresolvedClassUnderTest());
    }

    private boolean booleanField = true;

    private boolean perform_getfield_boolean() {
        return booleanField;
    }
    private boolean perform_getfield_boolean(UnresolvedClassUnderTest u) {
        return u.booleanField;
    }

    public void test_resolved_getfield_boolean() {
        final Method_Type method = compileMethod("perform_getfield_boolean", SignatureDescriptor.create(boolean.class));
        do_getfield_boolean(method, null);
    }

    public void test_unresolved_getfield_boolean() {
        final Method_Type method = compileMethod("perform_getfield_boolean", SignatureDescriptor.create(boolean.class, UnresolvedClassUnderTest.class));
        do_getfield_boolean(method, new UnresolvedClassUnderTest());
    }

    private void do_getfield_boolean(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asBoolean() == booleanField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asBoolean() == u.booleanField);
        }
        assertTrue(result.asBoolean());
    }

    private short shortField = 333;

    private short perform_getfield_short() {
        return shortField;
    }

    private short perform_getfield_short(UnresolvedClassUnderTest u) {
        return u.shortField;
    }

    public void test_resolved_getfield_short() {
        final Method_Type method = compileMethod("perform_getfield_short", SignatureDescriptor.create(short.class));
        do_getfield_short(method, null);
    }

    public void test_unresolved_getfield_short() {
        final Method_Type method = compileMethod("perform_getfield_short", SignatureDescriptor.create(short.class, UnresolvedClassUnderTest.class));
        do_getfield_short(method, new UnresolvedClassUnderTest());
    }

    private void do_getfield_short(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asShort() == shortField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asShort() == u.shortField);
        }
        assertTrue(result.asShort() == 333);
    }

    private char charField = 444;

    private char perform_getfield_char() {
        return charField;
    }

    private char perform_getfield_char(UnresolvedClassUnderTest u) {
        return u.charField;
    }

    public void test_resolved_getfield_char() {
        final Method_Type method = compileMethod("perform_getfield_char", SignatureDescriptor.create(char.class));
        do_getfield_char(method, null);
    }
    public void test_getfield_char() {
        final Method_Type method = compileMethod("perform_getfield_char", SignatureDescriptor.create(char.class, UnresolvedClassUnderTest.class));
        do_getfield_char(method, new UnresolvedClassUnderTest());
    }
    private void do_getfield_char(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asChar() == charField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asChar() == charField);
        }
        assertTrue(result.asChar() == 444);
    }

    private int intField = 55;

    private int perform_getfield_int() {
        return intField;
    }
    private int perform_getfield_int(UnresolvedClassUnderTest u) {
        return u.intField;
    }

    public void test_resolved_getfield_int() {
        final Method_Type method = compileMethod("perform_getfield_int", SignatureDescriptor.create(int.class));
        do_getfield_int(method, null);
    }
    public void test_getfield_int() {
        final Method_Type method = compileMethod("perform_getfield_int", SignatureDescriptor.create(int.class, UnresolvedClassUnderTest.class));
        do_getfield_int(method, new UnresolvedClassUnderTest());
    }
    private void do_getfield_int(Method_Type method, UnresolvedClassUnderTest u) {

        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asInt() == intField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asInt() == u.intField);
        }
        assertTrue(result.asInt() == 55);
    }

    private float floatField = 6.6F;

    private float perform_getfield_float() {
        return floatField;
    }
    private float perform_getfield_float(UnresolvedClassUnderTest u) {
        return u.floatField;
    }
    public void test_resolved_getfield_float() {
        final Method_Type method = compileMethod("perform_getfield_float", SignatureDescriptor.create(float.class));
        do_getfield_float(method, null);
    }
    public void test_unresolved_getfield_float() {
        final Method_Type method = compileMethod("perform_getfield_float", SignatureDescriptor.create(float.class, UnresolvedClassUnderTest.class));
        do_getfield_float(method, new UnresolvedClassUnderTest());
    }
    private void do_getfield_float(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asFloat() == floatField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asFloat() == u.floatField);
        }
        assertTrue(result.asFloat() == 6.6F);
    }

    private long longField = 77L;

    private long perform_getfield_long() {
        return longField;
    }
    private long perform_getfield_long(UnresolvedClassUnderTest u) {
        return u.longField;
    }
    public void test_resolved_getfield_long() {
        final Method_Type method = compileMethod("perform_getfield_long", SignatureDescriptor.create(long.class));
        do_getfield_long(method, null);
    }
    public void test_unresolved_getfield_long() {
        final Method_Type method = compileMethod("perform_getfield_long", SignatureDescriptor.create(long.class, UnresolvedClassUnderTest.class));
        do_getfield_long(method, new UnresolvedClassUnderTest());
    }

    private void do_getfield_long(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asLong() == longField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asLong() == u.longField);
        }
        assertTrue(result.asLong() == 77L);
    }

    private double doubleField = 8.8;

    private double perform_getfield_double() {
        return doubleField;
    }
    private double perform_getfield_double(UnresolvedClassUnderTest u) {
        return doubleField;
    }

    public void test_resolved_getfield_double() {
        final Method_Type method = compileMethod("perform_getfield_double", SignatureDescriptor.create(double.class));
        do_getfield_double(method, null);
    }
    public void test_unresolved_getfield_double() {
        final Method_Type method = compileMethod("perform_getfield_double", SignatureDescriptor.create(double.class, UnresolvedClassUnderTest.class));
        do_getfield_double(method, new UnresolvedClassUnderTest());
    }
    private void do_getfield_double(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asDouble() == doubleField);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asDouble() == u.doubleField);
        }
        assertTrue(result.asDouble() == 8.8);
    }

    private Word wordField;

    private Word perform_getfield_word() {
        return wordField;
    }

    private Word perform_getfield_word(UnresolvedClassUnderTest u) {
        return u.wordField;
    }
    public void test_unresolved_getfield_word() {
        final Method_Type method = compileMethod("perform_getfield_word", SignatureDescriptor.create(Word.class));
        do_getfield_word(method, null);
    }
    public void test_resolved_getfield_word() {
        final Method_Type method = compileMethod("perform_getfield_word", SignatureDescriptor.create(Word.class, UnresolvedClassUnderTest.class));
        final UnresolvedClassUnderTest u = new UnresolvedClassUnderTest();
        u.wordField = Offset.fromInt(88);
        do_getfield_word(method, u);
    }

    private void do_getfield_word(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        wordField = Offset.fromInt(88);
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asWord().equals(wordField));
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asWord().equals(u.wordField));
        }
        assertTrue(result.asWord().asOffset().toInt() == 88);
    }

    private Object referenceField = this;

    private Object perform_getfield_reference() {
        return referenceField;
    }

    private Object perform_getfield_reference(UnresolvedClassUnderTest u) {
        return u.referenceField;
    }

    public void test_resolved_getfield_reference() {
        final Method_Type method = compileMethod("perform_getfield_reference", SignatureDescriptor.create(Object.class));
        do_getfield_reference(method, null);
    }
    public void test_unresolved_getfield_reference() {
        final Method_Type method = compileMethod("perform_getfield_reference", SignatureDescriptor.create(Object.class, UnresolvedClassUnderTest.class));
        do_getfield_reference(method, new UnresolvedClassUnderTest());
    }
    private void do_getfield_reference(Method_Type method, UnresolvedClassUnderTest u) {
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getfield(int index) {
                confirmPresence();
            }
        };
        Value result = null;
        if (u == null) {
            result = executeWithReceiver(method);
            assertTrue(result.asObject() == referenceField);
            assertTrue(result.asObject() == this);
        } else {
            result = executeWithReceiver(method, ReferenceValue.from(u));
            assertTrue(result.asObject() == u.referenceField);
            assertTrue(result.asObject() == u);
        }
    }
}

