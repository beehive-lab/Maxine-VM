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
/*VCSID=06632ceb-5464-4fce-8f3e-afefcc479c58*/
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.*;

import com.sun.max.program.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.source.*;
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

    void compileMethod(String methodName, final Bytecode bytecode, Class returnType) {
        Trace.line(1, "\n\n ** Compiling " + methodName);
        final TargetMethod targetMethod = compileMethod(methodName, SignatureDescriptor.create(returnType));
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void invokespecial(int index) {
                if (bytecode == Bytecode.INVOKESPECIAL) {
                    confirmPresence();
                }
            }

            @Override
            public void invokestatic(int index) {
                if (bytecode == Bytecode.INVOKESTATIC) {
                    confirmPresence();
                }
            }
        };
    }


    public void test_compileResolvedInit() {
        compileMethod("performResolvedInit", Bytecode.INVOKESPECIAL, Object.class);
    }

    public void test_compileUnresolvedInit() {
        compileMethod("performUnresolvedInit", Bytecode.INVOKESPECIAL, Object.class);
    }

    public void test_compileResolvedSuperInvoke() {
        compileMethod("performResolvedSuperInvoke", Bytecode.INVOKESPECIAL, TargetMethod.class);
    }

    public void test_compileResolvedStaticInvoke() {
        compileMethod("performResolvedStaticMethod0", Bytecode.INVOKESTATIC, void.class);
        compileMethod("performResolvedStaticMethod1", Bytecode.INVOKESTATIC, void.class);
    }

    public void test_compileUnesolvedStaticInvoke() {
        compileMethod("performUnresolvedStaticMethod0", Bytecode.INVOKESTATIC, void.class);
        compileMethod("performUnresolvedStaticMethod1", Bytecode.INVOKESTATIC, void.class);
        compileMethod("performUnresolvedStaticMethodReturningInt", Bytecode.INVOKESTATIC, int.class);
    }

    public void test_compileResolvedStaticInvokeWithResult() {
        compileMethod("performResolvedStaticMethodReturningChar", Bytecode.INVOKESTATIC, char.class);
        compileMethod("performResolvedStaticMethodReturningInt", Bytecode.INVOKESTATIC, int.class);
        compileMethod("performResolvedStaticMethodReturningFloat", Bytecode.INVOKESTATIC, float.class);
        compileMethod("performResolvedStaticMethodReturningLong", Bytecode.INVOKESTATIC, long.class);
        compileMethod("performResolvedStaticMethodReturningDouble", Bytecode.INVOKESTATIC, double.class);
        compileMethod("performResolvedStaticMethodReturningObject", Bytecode.INVOKESTATIC, Object.class);
    }

    public JITTest_compileStaticMethodInvocation(String name) {
        super(name);
    }
    /**
     * Testing with unresolved, resolved, and initialized class constant.
     */
    @Override
    protected Class[] templateSources() {
        return TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES;
    }

}
