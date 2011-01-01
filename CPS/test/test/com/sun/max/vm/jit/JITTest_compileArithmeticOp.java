/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods doing arithmetic operation only (int, float, double, etc.).
 * This exercises the customization of template with immediate and literal pool constant to numeric value,
 *  immediate operand, and typed load/store from/to the evaluation stack and local variables.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileArithmeticOp extends JitCompilerTestCase {

    public JITTest_compileArithmeticOp() {
    }

    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant operands
     * (cause the jit to modify code with immediate int constant dependency).
     */
    public void perform_int_op() {
        int i = 10;             // will exercise a bipush
        i = 2 * i + 1;          // will exercise iconst2 & iconst1
    }

    public void perform_idiv() {
        int i = 100;
        i = i / 9;
    }

    public void perform_iinc() {
        int i = 23;
        i--;
        i++;
        i -= 3;
        i += 5;
    }

    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant pool literal
     * (cause the jit to modify code with literal float constant dependency).
     */
    public void perform_floating_point_op() {
        float f = 3335.20F;   // will exercise ldc and fstore1
        f = 0.275F * f + 1f;    // will exercise ldc and fconst1, fload1
    }

    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant pool literal
     * (cause the jit to modify code with literal constant dependency).
     */
    public void perform_double_op() {
        double d = 33300003435.20D;  // will exercise ldc2_w and dstore1
        d = 0.275F * d + 1D;                   // will exercise ldc2_w, dconst1, dload1, etc.
    }

    public void test_int_op() {
        compileMethod("perform_int_op", SignatureDescriptor.VOID);
    }

    public void test_idiv() {
        compileMethod("perform_idiv", SignatureDescriptor.VOID);
    }

    public void test_iinc() {
        compileMethod("perform_iinc", SignatureDescriptor.VOID);
    }

    public void test_floating_point_op() {
        compileMethod("perform_floating_point_op", SignatureDescriptor.VOID);
    }

    public void test_double_op() {
        compileMethod("perform_double_op", SignatureDescriptor.VOID);
    }

    public JITTest_compileArithmeticOp(String name) {
        super(name);
    }
}
