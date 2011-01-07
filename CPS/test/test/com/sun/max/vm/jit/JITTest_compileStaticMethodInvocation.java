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
 * Testing JIT-compilation of statically bound method invocation (i.e., invocation of methods that
 * whose address is known statically (e.g., private methods, constructors, static methods of a class).
 * Such methods are invoked with the invokestatic or invokespecial bytecode.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileStaticMethodInvocation extends JitCompilerTestCase {
    public JITTest_compileStaticMethodInvocation() {
    }

    Object performResolvedInit() {
        return new JITTest_compileStaticMethodInvocation();
    }

    Object performUnresolvedInit() {
        return new UnresolvedClassUnderTest();
    }

    int performUnresolvedStaticMethodReturningInt() {
        return UnresolvedAtTestTime.staticGetInt();
    }

    TargetMethod performResolvedSuperInvoke() {
        return super.compileMethod("performResolvedSuperInvoke");
    }

    public static void resolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < 1000; i++) {
            j = j * 2 + 1;
        }
    }
    public static void resolvedStaticMethod(int k) {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < k; i++) {
            j = j * 2 + (k - i);
        }
    }

    public static void resolvedStaticMethod2(int k, int l) {
        // Do enough stuff here to avoid being inlined.
        int j = l;
        for (int i = 0; i < k; i++) {
            j = j * 2 + (k - i);
        }
    }

    public static int resolvedStaticGetInt() {
        return 11;
    }

    public static char resolvedStaticGetChar() {
        return '1';
    }

    public static float resolvedStaticGetFloat() {
        return 11.11F;
    }

    public static long resolvedStaticGetLong() {
        return 93475983245L;
    }

    public static double resolvedStaticGetDouble() {
        return 93475983245.23333D;
    }

    public static Object resolvedStaticGetObject() {
        return new Object();
    }

    void performUnresolvedStaticMethod0() {
        UnresolvedClassUnderTest.unresolvedStaticMethod();
    }

    void performUnresolvedStaticMethod1() {
        UnresolvedClassUnderTest.unresolvedStaticMethod(35);
    }

    void performResolvedStaticMethod0() {
        resolvedStaticMethod();
    }

    void performResolvedStaticMethod1() {
        resolvedStaticMethod(35);
    }

    char performResolvedStaticMethodReturningChar() {
        return resolvedStaticGetChar();
    }

    int performResolvedStaticMethodReturningInt() {
        return resolvedStaticGetInt();
    }

    float performResolvedStaticMethodReturningFloat() {
        return resolvedStaticGetFloat();
    }

    long performResolvedStaticMethodReturningLong() {
        return resolvedStaticGetLong();
    }

    double performResolvedStaticMethodReturningDouble() {
        return resolvedStaticGetDouble();
    }

    Object performResolvedStaticMethodReturningObject() {
        return resolvedStaticGetObject();
    }

    void compileMethod(String methodName, final int bytecode, Class returnType) {
        final TargetMethod targetMethod = compileMethod(methodName, SignatureDescriptor.create(returnType));
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void invokespecial(int index) {
                if (bytecode == Bytecodes.INVOKESPECIAL) {
                    confirmPresence();
                }
            }

            @Override
            public void invokestatic(int index) {
                if (bytecode == Bytecodes.INVOKESTATIC) {
                    confirmPresence();
                }
            }
        };
    }

    public void test_compileResolvedInit() {
        compileMethod("performResolvedInit", Bytecodes.INVOKESPECIAL, Object.class);
    }

    public void test_compileUnresolvedInit() {
        compileMethod("performUnresolvedInit", Bytecodes.INVOKESPECIAL, Object.class);
    }

    public void test_compileResolvedSuperInvoke() {
        compileMethod("performResolvedSuperInvoke", Bytecodes.INVOKESPECIAL, TargetMethod.class);
    }

    public void test_compileResolvedStaticInvoke() {
        compileMethod("performResolvedStaticMethod0", Bytecodes.INVOKESTATIC, void.class);
        compileMethod("performResolvedStaticMethod1", Bytecodes.INVOKESTATIC, void.class);
    }

    public void test_compileUnesolvedStaticInvoke() {
        compileMethod("performUnresolvedStaticMethod0", Bytecodes.INVOKESTATIC, void.class);
        compileMethod("performUnresolvedStaticMethod1", Bytecodes.INVOKESTATIC, void.class);
        compileMethod("performUnresolvedStaticMethodReturningInt", Bytecodes.INVOKESTATIC, int.class);
    }

    public void test_compileResolvedStaticInvokeWithResult() {
        compileMethod("performResolvedStaticMethodReturningChar", Bytecodes.INVOKESTATIC, char.class);
        compileMethod("performResolvedStaticMethodReturningInt", Bytecodes.INVOKESTATIC, int.class);
        compileMethod("performResolvedStaticMethodReturningFloat", Bytecodes.INVOKESTATIC, float.class);
        compileMethod("performResolvedStaticMethodReturningLong", Bytecodes.INVOKESTATIC, long.class);
        compileMethod("performResolvedStaticMethodReturningDouble", Bytecodes.INVOKESTATIC, double.class);
        compileMethod("performResolvedStaticMethodReturningObject", Bytecodes.INVOKESTATIC, Object.class);
    }

    public JITTest_compileStaticMethodInvocation(String name) {
        super(name);
    }
}
