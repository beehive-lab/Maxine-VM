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
 * Testing the JIT-compiler with methods using bytecode instructions with explicit bytecode operand (e.g., bipush, sipush).
 *
 * @author Laurent Daynes
 */
public class JITTest_compileConstantOperandPush extends JitCompilerTestCase {

    void perform_bipush(int a) {
        @SuppressWarnings("unused")
        final int r = 111 * a;
    }

    void perform_sipush(int a) {
        @SuppressWarnings("unused")
        final int r = 1111 * a;
    }

    public void test_bipush() {
        compilePushMethod("perform_bipush");
    }

    public void test_sipush() {
        compilePushMethod("perform_sipush");
    }

    private void compilePushMethod(String methodName) {
        compileMethod(methodName, SignatureDescriptor.create("(I)V"));
    }

}
