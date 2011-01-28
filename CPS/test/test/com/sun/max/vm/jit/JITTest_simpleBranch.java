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
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the optimizing compiler.
 *
 * @author Laurent Daynes
 */
public class JITTest_simpleBranch extends CompilerTestCase<CPSTargetMethod> {

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
        compile(new TestBytecodeAssembler(false, "test_ifne", SignatureDescriptor.create("(I)I")) {
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
        }, getClass());
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
        compile(new TestBytecodeAssembler(false, "perform_ifne2", SignatureDescriptor.create("(I)I")) {
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
        }, getClass());
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
        Trace.line(1, method.classMethodActor().name);
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

    int i0;
    int i1;
    int i2;
    int i3;
    int iDefault;

    public void perform_tableswitch(int i) {
        switch(i) {
            case 0:
                i0 += i;
                break;
            case 1:
                i1 += i;
                break;
            case 2:
                i2 += i;
                break;
            case 3:
                i3 += i;
                break;
            default:
                iDefault += i;
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
