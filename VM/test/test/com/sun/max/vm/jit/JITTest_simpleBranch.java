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

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.generate.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the optimizing compiler.
 *
 * @author Laurent Daynes
 */
public class JITTest_simpleBranch extends CompilerTestCase<TargetMethod> {

    public void perform_unresolved_invokespecial(UnresolvedAtCompileTime o) {
        new UnresolvedAtCompileTime();
    }

    public void perform_unresolved_invokevirtual(UnresolvedAtCompileTime o) {
        o.parameterlessMethod();
    }

    public void perform_unresolved_invokeinterface(UnresolvedAtCompileTimeInterface o) {
        o.parameterlessUnresolvedInterfaceMethod();
    }

    public void perform_unresolved_invokestatic(UnresolvedAtCompileTime o) {
        UnresolvedAtCompileTime.parameterlessUnresolvedStaticMethod();
    }

    public void perform_resolved_invokespecial(ResolvedAtCompileTime o) {
        new ResolvedAtCompileTime();
    }

    public void perform_resolved_invokevirtual(ResolvedAtCompileTime o) {
        o.parameterlessMethod();
    }

    public void perform_resolved_invokeinterface(ResolvedAtCompileTimeInterface o) {
        o.parameterlessResolvedInterfaceMethod();
    }

    public void perform_resolved_invokestatic(ResolvedAtCompileTime o) {
        ResolvedAtCompileTime.parameterlessResolvedStaticMethod();
    }

    /**
     * public void perform_ifne(int i) {
     *    if (i == 0) {
     *        return 1;
     *    }
     *    return i << 1;
     * }.
     */
    public void test_ifne() {
        new TestBytecodeAssembler(false, "test_ifne", SignatureDescriptor.create("(I)I")) {
            @Override
            public void generateCode() {
                final int i = 1;
                final Label label = newLabel();
                iload(i);
                ifne(label);
                iconst(1);
                ireturn();
                label.bind();
                iload(i);
                iconst(1);
                ishl();
                ireturn();
            }
        }.compile(getClass());
    }

    /**
     * public void perform_ifne2(int i) {
     *    int c = i;
     *    while (c != 0) {
     *        c = c >> 1;
     *    }
     *    return c;
     * }.
     */
    public void test_ifne2() {
        new TestBytecodeAssembler(false, "perform_ifne2", SignatureDescriptor.create("(I)I")) {
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
                iload(c);
                ireturn();
            }
        }.compile(getClass());
    }



    private void do_unresolved_invoke(final String testName) {
        do_invoke("unresolved_" + testName, UnresolvedAtCompileTime.class);
    }

    private void do_resolved_invoke(final String testName) {
        do_invoke("resolved_" + testName, ResolvedAtCompileTime.class);
    }

    private void do_invoke(final String testName, Class argumentType) {
        final TargetMethod method = compileMethod("perform_" + testName, SignatureDescriptor.create(void.class, argumentType));
        new BytecodeConfirmation(method.classMethodActor()) {
            private void confirmFor(String bytecodeName) {
                if (testName.equals(bytecodeName)) {
                    confirmPresence();
                }
            }
            @Override
            public void invokevirtual(int index) {
                confirmFor("invokevirtual");
            }
            @Override
            public void invokespecial(int index) {
                confirmFor("invokespecial");
            }
            @Override
            public void invokestatic(int index) {
                confirmFor("invokestatic");
            }
            @Override
            public void invokeinterface(int index, int count) {
                confirmFor("invokeinterface");
            }
            @Override
            public void vreturn() {
                confirmPresence();
            }
        };
        Trace.line(1, method.classMethodActor().name());
    }

    public void test_unresolved_invokespecial() {
        do_unresolved_invoke("invokespecial");
    }

    public void test_unresolved_invokevirtual() {
        do_unresolved_invoke("invokevirtual");
    }

    public void test_unresolved_invokestatic() {
        do_resolved_invoke("invokestatic");
    }

    public void test_unresolved_invokeinterface() {
        do_invoke("unresolved_invokeinterface", UnresolvedAtCompileTimeInterface.class);
    }

    public void test_resolved_invokespecial() {
        do_resolved_invoke("invokespecial");
    }

    public void test_resolved_invokevirtual() {
        do_resolved_invoke("invokevirtual");
    }

    public void test_resolved_invokestatic() {
        do_resolved_invoke("invokestatic");
    }

    public void test_resolved_invokeinterface() {
        do_invoke("resolved_invokeinterface", ResolvedAtCompileTimeInterface.class);
    }

    int _i0;
    int _i1;
    int _i2;
    int _i3;
    int _i;

    public void perform_tableswitch(int i) {
        switch(i) {
            case 0:
                _i0 += i;
                break;
            case 1:
                _i1 += i;
                break;
            case 2:
                _i2 += i;
                break;
            case 3:
                _i3 += i;
                break;
            default:
                _i += i;
                break;
        }
    }

    public void test_tableswitch() {
        final TargetMethod method = compileMethod("perform_tableswitch", SignatureDescriptor.create(void.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases)  {
                bytecodeScanner().skipBytes(numberOfCases * 4);
                confirmPresence();
            }
        };
    }
}
