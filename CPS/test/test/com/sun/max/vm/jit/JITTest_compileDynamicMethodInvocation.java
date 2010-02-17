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

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.max.vm.bytecode.*;
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

    void compileMethod(String methodName, final Bytecode bytecode, Class parameterType) {
        final TargetMethod targetMethod = compileMethod(methodName, SignatureDescriptor.create(void.class, parameterType));
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void invokevirtual(int index) {
                if (bytecode == Bytecode.INVOKEVIRTUAL) {
                    confirmPresence();
                }
            }

            @Override
            public void invokeinterface(int index, int count) {
                if (bytecode == Bytecode.INVOKEINTERFACE) {
                    confirmPresence();
                }
            }
        };
    }

    public void test_compileResolvedVirtualCall() {
        compileMethod("performResolvedVirtualCall", Bytecode.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall1", Bytecode.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall2", Bytecode.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall3", Bytecode.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
        compileMethod("performResolvedVirtualCall4", Bytecode.INVOKEVIRTUAL, JITTest_compileDynamicMethodInvocation.class);
    }

    public void test_compileUnresolvedVirtualCall() {
        compileMethod("performUnresolvedVirtualCall", Bytecode.INVOKEVIRTUAL, UnresolvedAtTestTime.class);
    }

    public void test_compileUnresolvedVirtualCallWithParams() {
        compileMethod("performUnresolvedVirtualCallWithParams", Bytecode.INVOKEVIRTUAL, UnresolvedAtTestTime.class);
    }

    public void test_compileUnresolvedInterfaceCall() {
        compileMethod("performUnresolvedInterfaceCall", Bytecode.INVOKEINTERFACE, UnresolvedInterface.class);
    }

    public void test_compileUnresolvedInterfaceCallWithParams() {
        compileMethod("performUnresolvedInterfaceCallWithParams", Bytecode.INVOKEINTERFACE, UnresolvedInterface.class);
    }
}
