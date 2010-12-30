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
