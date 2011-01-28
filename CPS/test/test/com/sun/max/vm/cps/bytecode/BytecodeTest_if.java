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
