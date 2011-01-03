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
