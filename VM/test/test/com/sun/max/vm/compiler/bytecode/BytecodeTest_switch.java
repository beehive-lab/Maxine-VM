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
/*VCSID=7bfbdbbb-cb65-434e-9316-5ee26f22b452*/
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.BytecodeConfirmation.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_switch<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_switch(String name) {
        super(name);
    }

    private static int perform_tableswitch_1(int a) {
        int b = a;
        switch (b) {
            case 1:
                b = 10;
                break;
            default:
                b = -1;
                break;
        }
        return b;
    }

    public void test_tableswitch_1() {
        final Method_Type method = compileMethod("perform_tableswitch_1", SignatureDescriptor.create(int.class, int.class));
        try {
            new BytecodeConfirmation(method.classMethodActor()) {

                @Override
                public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
                    getBytecodeScanner().skipBytes(numberOfCases * 4);
                    confirmPresence();
                }
            };
            Value result = execute(method, IntValue.from(1));
            assertTrue(result.asInt() == 10);
            result = execute(method, IntValue.from(0));
            assertTrue(result.asInt() == -1);
        } catch (BytecodeAbsent e) {
            // The above code may compile as a lookupswitch or even nested if-then-else statements
        }
    }

    private static int perform_tableswitch_3(int a) {
        int b = a;
        switch (b) {
            case 1:
                b = 10;
                break;
            case 2:
                b = 20;
                break;
            case 3:
                b = 30;
                break;
        }
        return b;
    }

    public void test_tableswitch_3() {
        final Method_Type method = compileMethod("perform_tableswitch_3", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
                getBytecodeScanner().skipBytes(numberOfCases * 4);
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(1));
        assertTrue(result.asInt() == 10);
        result = execute(method, IntValue.from(2));
        assertTrue(result.asInt() == 20);
        result = execute(method, IntValue.from(3));
        assertTrue(result.asInt() == 30);
        result = execute(method, IntValue.from(7));
        assertTrue(result.asInt() == 7);
    }

    private static int perform_lookupswitch_2(int a) {
        int b = a;
        switch (b) {
            case 1:
                b = 10;
                break;
            case 1000:
                b = 20;
                break;
        }
        return b;
    }

    public void test_lookupswitch_2_1() {
        final Method_Type method = compileMethod("perform_lookupswitch_2", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void lookupswitch(int defaultOffset, int numberOfCases) {
                getBytecodeScanner().skipBytes(numberOfCases * 8);
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(1));
        assertTrue(result.asInt() == 10);
        result = execute(method, IntValue.from(1000));
        assertTrue(result.asInt() == 20);
        result = execute(method, IntValue.from(7));
        assertTrue(result.asInt() == 7);
    }

    private static int perform_lookupswitch_3(int a) {
        int b = a;
        switch (b) {
            case 'X':
                b = 10;
                break;
            case -1:
                b = 20;
                break;
            default:
                b = 30;
        }
        return b;
    }

    public void test_lookupswitch_3() {
        final Method_Type method = compileMethod("perform_lookupswitch_3", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void lookupswitch(int defaultOffset, int numberOfCases) {
                getBytecodeScanner().skipBytes(numberOfCases * 8);
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from('X'));
        assertTrue(result.asInt() == 10);
        result = execute(method, IntValue.from(-1));
        assertTrue(result.asInt() == 20);
        result = execute(method, IntValue.from(4));
        assertTrue(result.asInt() == 30);
    }
}
