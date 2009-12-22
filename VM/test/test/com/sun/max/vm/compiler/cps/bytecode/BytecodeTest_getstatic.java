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
package test.com.sun.max.vm.compiler.cps.bytecode;

import test.com.sun.max.vm.compiler.cps.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_getstatic<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_getstatic(String name) {
        super(name);
    }

    private static byte byteField = 111;

    private static byte perform_getstatic_byte() {
        return byteField;
    }

    public void test_getstatic_byte() {
        final Method_Type method = compileMethod("perform_getstatic_byte", SignatureDescriptor.create(byte.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asByte() == byteField);
        assertTrue(result.asByte() == 111);
    }

    private static boolean booleanField = true;

    private static boolean perform_getstatic_boolean() {
        return booleanField;
    }

    public void test_getstatic_boolean() {
        final Method_Type method = compileMethod("perform_getstatic_boolean", SignatureDescriptor.create(boolean.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asBoolean() == booleanField);
        assertTrue(result.asBoolean());
    }

    private static short shortField = 333;

    private static short perform_getstatic_short() {
        return shortField;
    }

    public void test_getstatic_short() {
        final Method_Type method = compileMethod("perform_getstatic_short", SignatureDescriptor.create(short.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asShort() == shortField);
        assertTrue(result.asShort() == 333);
    }

    private static char charField = 444;

    private static char perform_getstatic_char() {
        return charField;
    }

    public void test_getstatic_char() {
        final Method_Type method = compileMethod("perform_getstatic_char", SignatureDescriptor.create(char.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asChar() == charField);
        assertTrue(result.asChar() == 444);
    }

    private static int intField = 55;

    private static int perform_getstatic_int() {
        return intField;
    }

    public void test_getstatic_int() {
        final Method_Type method = compileMethod("perform_getstatic_int", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asInt() == intField);
        assertTrue(result.asInt() == 55);
    }

    private static float floatField = 66;

    private static float perform_getstatic_float() {
        return floatField;
    }

    public void test_getstatic_float() {
        final Method_Type method = compileMethod("perform_getstatic_float", SignatureDescriptor.create(float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asFloat() == floatField);
        assertTrue(result.asFloat() == 66);
    }

    private static long longField = 77L;

    private static long perform_getstatic_long() {
        return longField;
    }

    public void test_getstatic_long() {
        final Method_Type method = compileMethod("perform_getstatic_long", SignatureDescriptor.create(long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asLong() == longField);
        assertTrue(result.asLong() == 77L);
    }

    private static double doubleField = 77;

    private static double perform_getstatic_double() {
        return doubleField;
    }

    public void test_getstatic_double() {
        final Method_Type method = compileMethod("perform_getstatic_double", SignatureDescriptor.create(double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asDouble() == doubleField);
        assertTrue(result.asDouble() == 77);
    }

    protected static Word wordField;

    private static Word perform_getstatic_word() {
        return wordField;
    }

    public void test_getstatic_word() {
        wordField = Offset.fromInt(88);
        final Method_Type method = compileMethod("perform_getstatic_word", SignatureDescriptor.create(Word.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asWord().equals(wordField));
        assertTrue(result.asWord().asOffset().toInt() == 88);
    }

    private static Object referenceField = BytecodeTest_getstatic.class;

    private static Object perform_getstatic_reference() {
        return referenceField;
    }

    public void test_getstatic_reference() {
        final Method_Type method = compileMethod("perform_getstatic_reference", SignatureDescriptor.create(Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void getstatic(int index) {
                confirmPresence();
            }
        };
        final Value result = execute(method);
        assertTrue(result.asObject() == referenceField);
        assertTrue(result.asObject() == BytecodeTest_getstatic.class);
    }

}
