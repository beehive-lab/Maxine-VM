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
