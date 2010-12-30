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

import com.sun.max.vm.cps.ir.*;
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
