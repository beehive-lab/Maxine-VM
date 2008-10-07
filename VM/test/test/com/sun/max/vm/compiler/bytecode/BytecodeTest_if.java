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
/*VCSID=876cc03e-350d-4727-a62a-9b972d249fcb*/
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_if<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_if(String name) {
        super(name);
    }

    private static int perform_ifeq(int a) {
        int n = 0;
        if (a == 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a != 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifeq() {
        final Method_Type method = compileMethod("perform_ifeq", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifeq(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(7));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_ifne(int a) {
        int n = 0;
        if (a != 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a == 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifne() {
        final Method_Type method = compileMethod("perform_ifne", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifne(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(5));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_iflt(int a) {
        int n = 0;
        if (a < 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a >= 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_iflt() {
        final Method_Type method = compileMethod("perform_iflt", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void iflt(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(-1));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_ifge(int a) {
        int n = 0;
        if (a >= 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a < 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifge() {
        final Method_Type method = compileMethod("perform_ifge", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifge(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(-1));
        assertTrue(result.asInt() == -2);
        result = execute(method, IntValue.from(1));
        assertTrue(result.asInt() == 2);
    }

    private static int perform_ifgt(int a) {
        int n = 0;
        if (a > 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a <= 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifgt() {
        final Method_Type method = compileMethod("perform_ifgt", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifgt(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(1));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_ifle(int a) {
        int n = 0;
        if (a <= 0) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a > 0) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifle() {
        final Method_Type method = compileMethod("perform_ifle", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifle(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(0));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(1));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_ifnull(Object a) {
        int n = 0;
        if (a == null) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a != null) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifnull() {
        final Method_Type method = compileMethod("perform_ifnull", SignatureDescriptor.create(int.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifnull(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, ReferenceValue.NULL);
        assertTrue(result.asInt() == 2);
        result = execute(method, ReferenceValue.from(this));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_ifnonnull(Object a) {
        int n = 0;
        if (a != null) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a == null) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_ifnonnull() {
        final Method_Type method = compileMethod("perform_ifnonnull", SignatureDescriptor.create(int.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override public void ifnonnull(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, ReferenceValue.from(this));
        assertTrue(result.asInt() == 2);
        result = execute(method, ReferenceValue.NULL);
        assertTrue(result.asInt() == -2);
    }

}
