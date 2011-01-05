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

import test.com.sun.max.vm.bytecode.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods performing conditional backward loop. This tests the JIT support for the if<cond>, ifnull, ifnonnull,
 * ifacmp<cond> ificmp<cond> bytecodes, and simple loop control flows.
 *
 * @author Laurent Daynes
 */
public abstract class JITTest_backwardConditionalBranch extends JitCompilerTestCase {

    /**
     * public void perform_forward_ifne(int i) {
     *     if (i == 0) {
     *         return;
     *     }
     *     int n = i;
     *     n = n << n;
     * }.
     */
    public void test_perform_forward_ifne() {
        final CPSTargetMethod targetMethod = compile(new TestBytecodeAssembler(false, "perform_forward_ifne", SignatureDescriptor.create("(I)V")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final int n = allocateLocal(Kind.INT);
                final Label label = newLabel();
                iload(i);
                ifne(label);
                vreturn();
                label.bind();
                iload(i);
                istore(n);
                iload(n);
                iload(n);
                ishl();
                istore(n);
                vreturn();
            }
        }, getClass());
        traceCompiledMethod(targetMethod);
    }

    /**
     * public void perform_backward_ifne(int i) {
     *     int c = i;
     *     while (c != 0) {
     *         c = c >> 1;
     *     }
     * }.
     */
    public void test_perform_backward_ifne() {
        final CPSTargetMethod targetMethod = compile(new TestBytecodeAssembler(false, "perform_backward_ifne", SignatureDescriptor.create("(I)V")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final int c = allocateLocal(Kind.INT);
                final Label loopTest = newLabel();
                final Label loopHead = newLabel();
                iload(i);
                istore(c);
                goto_(loopTest);
                loopHead.bind();
                iload(c);
                iconst(1);
                ishr();
                istore(c);
                loopTest.bind();
                iload(c);
                ifne(loopHead);
                vreturn();
            }
        }, getClass());
        traceCompiledMethod(targetMethod);
    }

    /**
     * public void perform_backward_ifgt(int i) {
     *     int c = i;
     *     while (c > 0) {
     *         c = c >> 1;
     *     }
     * }.
     */
    public void test_perform_backward_ifgt() {
        final CPSTargetMethod targetMethod = compile(new TestBytecodeAssembler(false, "perform_backward_ifgt", SignatureDescriptor.create("(I)V")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final int c = allocateLocal(Kind.INT);
                final Label loopTest = newLabel();
                final Label loopHead = newLabel();
                iload(i);
                istore(c);
                goto_(loopTest);
                loopHead.bind();
                iload(c);
                iconst(1);
                ishr();
                istore(c);
                loopTest.bind();
                iload(c);
                ifgt(loopHead);
                vreturn();
            }
        }, getClass());
        traceCompiledMethod(targetMethod);
    }

    /**
     * public void perform_backward_iflt(int i) {
     *     int c = -100;
     *     while (c < 0) {
     *         c += i;
     *     }
     * }.
     */
    public void test_perform_backward_iflt() {
        final CPSTargetMethod targetMethod = compile(new TestBytecodeAssembler(false, "perform_backward_iflt", SignatureDescriptor.create("(I)V")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final int c = allocateLocal(Kind.INT);
                final Label loopTest = newLabel();
                final Label loopHead = newLabel();
                iconst(-100);
                istore(c);
                goto_(loopTest);
                loopHead.bind();
                iload(c);
                iload(i);
                iadd();
                istore(c);
                loopTest.bind();
                iload(c);
                iflt(loopHead);
                vreturn();
            }
        }, getClass());
        traceCompiledMethod(targetMethod);
    }

    /**
     * public void perform_forward_if_icmpne(int i) {
     *     int c = 256;
     *     if (c == i) {
     *         return;
     *     }
     *     int n = i;
     *     n = n << 2;
     * }.
     */
    public void test_perform_forward_if_icmpne() {
        final CPSTargetMethod targetMethod = compile(new TestBytecodeAssembler(false, "perform_forward_if_icmpne", SignatureDescriptor.create("(I)V")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final int c = allocateLocal(Kind.INT);
                final int n = allocateLocal(Kind.INT);
                final Label label = newLabel();
                iconst(256);
                istore(c);
                iload(c);
                iload(i);
                if_icmpne(label);
                vreturn();
                label.bind();
                iload(i);
                istore(n);
                iload(n);
                iconst(2);
                ishl();
                istore(n);
                vreturn();
            }
        }, getClass());
        traceCompiledMethod(targetMethod);
    }

    private void traceCompiledMethod(final CPSTargetMethod targetMethod) {
        traceBundleAndDisassemble(targetMethod);
    }
}
