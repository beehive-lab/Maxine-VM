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
/*VCSID=5f57debe-a9c0-4399-b564-afe463f07aaa*/
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

    public int perform_ifne(int i) {
        if (i  == 0) {
            return 1;
        }
        return i << 1;
    }

    public int perform_ifne2(int i) {
        int c = i;
        while (c != 0) {
            c = c >> 1;
        }
        return c;
    }

    private void do_ifne(String testName) {
        Trace.on(1);
        final TargetMethod method = compileMethod("perform_" + testName, SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {

            @Override
            public void iload_1() {
                confirmPresence();
            }
            @Override
            public void ifne(int index) {
                confirmPresence();
            }
            @Override
            public void ireturn() {
                confirmPresence();
            }
        };
        disassemble(method);
    }

    public void test_ifne() {
        do_ifne("ifne");
    }

    public void test_ifne2() {
        do_ifne("ifne2");
    }

    private void do_unresolved_invoke(final String testName) {
        do_invoke("unresolved_" + testName, UnresolvedAtCompileTime.class);
    }

    private void do_resolved_invoke(final String testName) {
        do_invoke("resolved_" + testName, ResolvedAtCompileTime.class);
    }

    private void do_invoke(final String testName, Class argumentType) {
        Trace.on(1);
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
        Trace.on(1);
        Trace.line(1, method.classMethodActor().name());
        disassemble(method);
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
                getBytecodeScanner().skipBytes(numberOfCases * 4);
                confirmPresence();
            }
        };
        disassemble(method);
    }
}

