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
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Testing JIT-compilation of dynamically bound method invocation (i.e., invocation of methods which requires
 * dynamic dispatch (e.g., virtual methods, interfaces).
 * Such methods are invoked with the invokevirtual or invokeinterface bytecode.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileDynamicMethodInvocation extends JitCompilerTestCase {
    int intField;

    int getInt() {
        return intField;
    }

    int compute1(int i) {
        return intField - i;
    }

    int compute2(int i1, int i2) {
        return i2 * (intField - i1);
    }

    void setInt(int i) {
        intField = i;
    }

    void setInt(int i1, int i2) {
        intField = i1 + i2;
    }

    @SuppressWarnings("unused")
    void performResolvedVirtualCall(JITTest_compileDynamicMethodInvocation o) {
        final int i = o.getInt();
    }

    void performResolvedVirtualCall1(JITTest_compileDynamicMethodInvocation o) {
        o.setInt(5);
    }

    void performResolvedVirtualCall2(JITTest_compileDynamicMethodInvocation o) {
        o.setInt(5, 2);
    }

    @SuppressWarnings("unused")
    void performResolvedVirtualCall3(JITTest_compileDynamicMethodInvocation o) {
        final int i = o.compute1(5);
    }

    @SuppressWarnings("unused")
    void performResolvedVirtualCall4(JITTest_compileDynamicMethodInvocation o) {
        final int i = o.compute2(5, 2);
    }

    void performUnresolvedVirtualCall(UnresolvedAtTestTime o) {
        o.getInt();
    }

    void performUnresolvedVirtualCallWithParams(UnresolvedAtTestTime o) {
        o.updateInt(1, 2);
    }

    void performUnresolvedInterfaceCall(UnresolvedInterface o) {
        o.parameterlessUnresolvedInterfaceMethod();
    }

    void performUnresolvedInterfaceCallWithParams(UnresolvedInterface o) {
        o.unresolvedInterfaceMethod(1, 2);
    }

    void compileMethod(String methodName, final int bytecode, Class parameterType) {
        final TargetMethod targetMethod = compileMethod(methodName, SignatureDescriptor.create(void.class, parameterType));
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void invokevirtual(int index) {
                if (bytecode == Bytecodes.INVOKEVIRTUAL) {
                    confirmPresence();
                }
            }

            @Override
            public void invokeinterface(int index, int count) {
                if (bytecode == Bytecodes.INVOKEINTERFACE) {
                    confirmPresence();
                }
            }
        };
    }

    public void test_compileResolvedVirtualCall() {
        compileMethod("performResolvedVirtualCall", Bytecodes.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall1", Bytecodes.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall2", Bytecodes.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall3", Bytecodes.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall4", Bytecodes.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
    }

    public void test_compileUnresolvedVirtualCall() {
        compileMethod("performUnresolvedVirtualCall", Bytecodes.INVOKEVIRTUAL, UnresolvedAtTestTime.class);
    }

    public void test_compileUnresolvedVirtualCallWithParams() {
        compileMethod("performUnresolvedVirtualCallWithParams", Bytecodes.INVOKEVIRTUAL, UnresolvedAtTestTime.class);
    }

    public void test_compileUnresolvedInterfaceCall() {
        compileMethod("performUnresolvedInterfaceCall", Bytecodes.INVOKEINTERFACE, UnresolvedInterface.class);
    }

    public void test_compileUnresolvedInterfaceCallWithParams() {
        compileMethod("performUnresolvedInterfaceCallWithParams", Bytecodes.INVOKEINTERFACE, UnresolvedInterface.class);
    }
}
