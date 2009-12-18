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

public abstract class BytecodeTest_ifcmp<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_ifcmp(String name) {
        super(name);
    }

    private static int perform_if_icmpeq(int a, int b) {
        int n = 0;
        if (a == b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a != b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmpeq() {
        final Method_Type method = compileMethod("perform_if_icmpeq", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmpeq(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(1), IntValue.from(2));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_icmpne(int a, int b) {
        int n = 0;
        if (a != b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a == b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmpne() {
        final Method_Type method = compileMethod("perform_if_icmpne", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmpne(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(1), IntValue.from(2));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_icmplt(int a, int b) {
        int n = 0;
        if (a < b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a >= b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmplt() {
        final Method_Type method = compileMethod("perform_if_icmplt", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmplt(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(1), IntValue.from(2));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_icmpge(int a, int b) {
        int n = 0;
        if (a >= b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a < b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmpge() {
        final Method_Type method = compileMethod("perform_if_icmpge", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmpge(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(1), IntValue.from(2));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_icmpgt(int a, int b) {
        int n = 0;
        if (a > b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a <= b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmpgt() {
        final Method_Type method = compileMethod("perform_if_icmpgt", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmpgt(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(2), IntValue.from(1));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_icmple(int a, int b) {
        int n = 0;
        if (a <= b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a > b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_icmple() {
        final Method_Type method = compileMethod("perform_if_icmple", SignatureDescriptor.create(int.class, int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_icmple(int offset) {
                confirmPresence();
            }
        };
        Value result = execute(method, IntValue.from(2), IntValue.from(2));
        assertTrue(result.asInt() == 2);
        result = execute(method, IntValue.from(2), IntValue.from(1));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_acmpeq(Object a, Object b) {
        int n = 0;
        if (a == b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a != b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_acmpeq() {
        final Method_Type method = compileMethod("perform_if_acmpeq", SignatureDescriptor.create(int.class, Object.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_acmpeq(int offset) {
                confirmPresence();
            }
        };
        final ReferenceValue a = ReferenceValue.from(this);
        Value result = execute(method, a, a);
        assertTrue(result.asInt() == 2);
        result = execute(method, a, ReferenceValue.from(method));
        assertTrue(result.asInt() == -2);
    }

    private static int perform_if_acmpne(Object a, Object b) {
        int n = 0;
        if (a != b) {
            n += 1;
        } else {
            n -= 1;
        }
        if (a == b) {
            n -= 1;
        } else {
            n += 1;
        }
        return n;
    }

    public void test_if_acmpne() {
        final Method_Type method = compileMethod("perform_if_acmpne", SignatureDescriptor.create(int.class, Object.class, Object.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override public void if_acmpne(int offset) {
                confirmPresence();
            }
        };
        final ReferenceValue a = ReferenceValue.from(this);
        Value result = execute(method, a, ReferenceValue.from(method));
        assertTrue(result.asInt() == 2);
        result = execute(method, a, a);
        assertTrue(result.asInt() == -2);
    }
}
