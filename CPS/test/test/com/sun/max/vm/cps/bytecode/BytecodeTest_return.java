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

public abstract class BytecodeTest_return<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_return(String name) {
        super(name);
    }

    private void perform_vreturn() {
    }

    public void test_vreturn() {
        final Method_Type method = compileMethod("perform_vreturn", SignatureDescriptor.create(void.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void vreturn() {
                confirmPresence();
            }
        };
        final Object result = executeWithReceiver(method);
        assertTrue(result == VoidValue.VOID);
    }

    private static final int INT_RESULT = 317;

    private int perform_ireturn() {
        return INT_RESULT;
    }

    public void test_ireturn() {
        final Method_Type method = compileMethod("perform_ireturn", SignatureDescriptor.create(int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void ireturn() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertEquals(result.asInt(), INT_RESULT);
    }

    private static final float FLOAT_RESULT = (float) 422.012;

    private float perform_freturn() {
        return FLOAT_RESULT;
    }

    public void test_freturn() {
        final Method_Type method = compileMethod("perform_freturn", SignatureDescriptor.create(float.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void freturn() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertEquals(result.asFloat(), FLOAT_RESULT);
    }

    private static final long LONG_RESULT = 0x1234567812345678L;

    private long perform_lreturn() {
        return LONG_RESULT;
    }

    public void test_lreturn() {
        final Method_Type method = compileMethod("perform_lreturn", SignatureDescriptor.create(long.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void lreturn() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertEquals(result.asLong(), LONG_RESULT);
    }

    private static final double DOUBLE_RESULT = -170.578;

    private double perform_dreturn() {
        return DOUBLE_RESULT;
    }

    public void test_dreturn() {
        final Method_Type method = compileMethod("perform_dreturn", SignatureDescriptor.create(double.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void dreturn() {
                confirmPresence();
            }
        };
        final Value result = executeWithReceiver(method);
        assertEquals(result.asDouble(), DOUBLE_RESULT);
    }

    private Object perform_areturn(Object result) {
        return result;
    }

    public void test_areturn() {
        final Method_Type method = compileMethod("perform_areturn", SignatureDescriptor.create(Object.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void areturn() {
                confirmPresence();
            }
        };
        final Object expectedResult = new Object();
        final Value result = executeWithReceiver(method, ReferenceValue.from(expectedResult));
        assertTrue(result.asObject() == expectedResult);
    }
}
