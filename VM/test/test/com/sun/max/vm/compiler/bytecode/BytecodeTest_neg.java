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

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_neg<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_neg(String name) {
        super(name);
    }

    private int perform_ineg(int a) {
        return -a;
    }

    public void test_ineg() {
        final Method_Type method = compileMethod("perform_ineg", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ineg() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, IntValue.from(3));
        assertTrue(result.asInt() == -3);
    }

    private long perform_lneg(long a) {
        return -a;
    }

    public void test_lneg() {
        final Method_Type method = compileMethod("perform_lneg", SignatureDescriptor.create(long.class, long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void lneg() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method, LongValue.from(3));
        assertTrue(result.asLong() == -3L);
    }

    private float perform_fneg(float a) {
        return -a;
    }

    public void test_fneg() {
        final Method_Type method = compileMethod("perform_fneg", SignatureDescriptor.create(float.class, float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void fneg() {
                confirmPresence();
            }
        };
        Value result = executeWithReceiver(method, FloatValue.from(-1.2345f));
        assertTrue(result.asFloat() == 1.2345f);

        result = executeWithReceiver(method, FloatValue.from(3.2f));
        assertTrue(result.asFloat() == -3.2f);
    }

    private double perform_dneg(double a) {
        return -a;
    }

    public void test_dneg() {
        final Method_Type method = compileMethod("perform_dneg", SignatureDescriptor.create(double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dneg() {
                confirmPresence();
            }
        };
        Value result = executeWithReceiver(method, DoubleValue.from(-1.2345));
        assertTrue(result.asDouble() == 1.2345);

        result = executeWithReceiver(method, DoubleValue.from(3.2345));
        assertTrue(result.asDouble() == -3.2345);
    }

    final double dabs(double d) {
        return (d >= 0) ? d : -d;
    }

    public void test_dabs() {
        final Method_Type method = compileMethod("dabs", SignatureDescriptor.create(double.class, double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void dneg() {
                confirmPresence();
            }
        };
        Value result = executeWithReceiver(method, DoubleValue.from(-1.2345));
        assertTrue(result.asDouble() == 1.2345);

        result = executeWithReceiver(method, DoubleValue.from(3.2345));
        assertTrue(result.asDouble() == 3.2345);
    }
}
