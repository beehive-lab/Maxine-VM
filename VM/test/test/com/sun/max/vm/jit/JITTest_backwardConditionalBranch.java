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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.bytecode.*;
import test.com.sun.max.vm.compiler.cps.*;

import com.sun.max.vm.compiler.cps.target.*;
import com.sun.max.vm.template.source.*;
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

    /**
     * Testing with unresolved and branch bytecode sources.
     */
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, BranchBytecodeSource.class, InstrumentedBytecodeSource.class};
    }
}
