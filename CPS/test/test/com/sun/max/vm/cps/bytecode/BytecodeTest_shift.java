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

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_shift<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_shift(String name) {
        super(name);
    }

    private int perform_ishl(int a, int b) {
        return a << b;
    }

    public void test_ishl() {
        final Method_Type method = compileMethod("perform_ishl", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ishl() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(20), IntValue.from(3));
        assertTrue(result.asInt() == 20 * 8);
    }

    private long perform_lshl(long a, int b) {
        return a << b;
    }

    public void test_lshl() {
        final Method_Type method = compileMethod("perform_lshl", SignatureDescriptor.create(long.class, long.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lshl() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(23), IntValue.from(2));
        assertTrue(result.asLong() == 23 * 4);
    }

    private int perform_ishr(int a, int b) {
        return a >> b;
    }

    public void test_ishr() {
        final Method_Type method = compileMethod("perform_ishr", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ishr() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(200), IntValue.from(3));
        assertTrue(result.asInt() == 200 / 8);
    }

    private long perform_lshr(long a, int b) {
        return a >> b;
    }

    public void test_lshr() {
        final Method_Type method = compileMethod("perform_lshr", SignatureDescriptor.create(long.class, long.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lshr() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(2673), IntValue.from(2));
        assertTrue(result.asLong() == 2673 / 4);
    }

    private int perform_iushr(int a, int b) {
        return a >>> b;
    }

    public void test_iushr() {
        final Method_Type method = compileMethod("perform_iushr", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void iushr() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(200), IntValue.from(3));
        assertTrue(result.asInt() == 200 / 8);
    }

    private long perform_lushr(long a, int b) {
        return a >>> b;
    }

    public void test_lushr() {
        final Method_Type method = compileMethod("perform_lushr", SignatureDescriptor.create(long.class, long.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lushr() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(2673), IntValue.from(2));
        assertTrue(result.asLong() == 2673 / 4);
    }
}
