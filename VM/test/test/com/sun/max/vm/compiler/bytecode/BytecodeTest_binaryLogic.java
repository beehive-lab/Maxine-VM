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
/*VCSID=49204054-12ff-4e65-a2e0-aa3a015d3b6f*/
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_binaryLogic<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_binaryLogic(String name) {
        super(name);
    }

    private int perform_iand(int a, int b) {
        return a & b;
    }

    public void test_iand() {
        final Method_Type method = compileMethod("perform_iand", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void iand() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(0x3030), IntValue.from(0x2107));
        assertTrue(result.asInt() == 0x2000);
    }

    private long perform_land(long a, long b) {
        return a & b;
    }

    public void test_land() {
        final Method_Type method = compileMethod("perform_land", SignatureDescriptor.create(long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void land() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(0x101010), LongValue.from(0x022333));
        assertTrue(result.asLong() == 0x000010);
    }

    private int perform_ior(int a, int b) {
        return a | b;
    }

    public void test_ior() {
        final Method_Type method = compileMethod("perform_ior", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ior() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(0x3030), IntValue.from(0x2107));
        assertTrue(result.asInt() == 0x3137);
    }

    private long perform_lor(long a, long b) {
        return a | b;
    }

    public void test_lor() {
        final Method_Type method = compileMethod("perform_lor", SignatureDescriptor.create(long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lor() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(0x101010), LongValue.from(0x022333));
        assertTrue(result.asLong() == 0x123333);
    }

    private int perform_ixor(int a, int b) {
        return a ^ b;
    }

    public void test_ixor() {
        final Method_Type method = compileMethod("perform_ixor", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ixor() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(0x3030), IntValue.from(0x2107));
        assertTrue(result.asInt() == 4407);
    }

    private long perform_lxor(long a, long b) {
        return a ^ b;
    }

    public void test_lxor() {
        final Method_Type method = compileMethod("perform_lxor", SignatureDescriptor.create(long.class, long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lxor() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(0x101010), LongValue.from(0x022333));
        assertTrue(result.asLong() == 1192739);
    }

}
