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
