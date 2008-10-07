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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods performing field access to resolved and unresolved class.
 * This suite of tests exercises the part of the template-based JIT that (a) selects an appropriate template
 * based on the initialization state of the refered class (unresolved, loaded, initialized), and customize
 * the template appropriately (i.e., replace ResolutionGuard of the template with appropriate ResolutionGuard,
 * replace reference literals to static tuple with static tuplee obtained from the constant pool of the
 * compiled method, modified field offsets with appropriate offset values, etc.).
 **
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMethodWithFieldAccess extends JitCompilerTestCase {
    static class IntFieldHolder {
        int _a;
        int _b;
        int f() {
            return 2 * _a + _b;
        }
    }

    static class FloatFieldHolder {
        float _a;
        float _b;
        float f() {
            return 2.5F * _a + _b;
        }
    }

    static class LongFieldHolder {
        long _a;
        long _b;

        long f() {
            return 2 * _a + _b;
        }
    }

    ///////////////////////////// INT

    @SuppressWarnings("unused")
    void performOneIntFieldAccess(IntFieldHolder holder) {
        final int a = holder._a;
    }

    @SuppressWarnings("unused")
    void performTwoIntFieldAccess(IntFieldHolder holder) {
        final int a = holder._a;
        final int b = holder._b;
    }

    @SuppressWarnings("unused")
    void performOneIntFieldAccess(UnresolvedAtTestTime holder) {
        final int a = holder._intField;
    }

    @SuppressWarnings("unused")
    void performTwoIntFieldAccess(UnresolvedAtTestTime holder) {
        final int a =  holder._intField;
        final int b =  holder._intField2;
    }

    ///////////////////////////// FLOAT

    @SuppressWarnings("unused")
    void performOneFloatFieldAccess(FloatFieldHolder holder) {
        final float a = holder._a;
    }

    @SuppressWarnings("unused")
    void performTwoFloatFieldAccess(FloatFieldHolder holder) {
        final float a = holder._a;
        final float b = holder._b;
    }

    @SuppressWarnings("unused")
    void performOneFloatFieldAccess(UnresolvedAtTestTime holder) {
        final float a = holder._floatField;
    }

    @SuppressWarnings("unused")
    void performTwoFloatFieldAccess(UnresolvedAtTestTime holder) {
        final float a =  holder._floatField;
        final float b =  holder._floatField2;
    }

    ///////////////////////////// LONG

    @SuppressWarnings("unused")
    void performOneLongFieldAccess(LongFieldHolder holder) {
        final long a = holder._a;
    }

    @SuppressWarnings("unused")
    void performTwoLongFieldAccess(LongFieldHolder holder) {
        final long a = holder._a;
        final long b = holder._b;
    }

    @SuppressWarnings("unused")
    void performOneLongFieldAccess(UnresolvedAtTestTime holder) {
        final long a = holder._longField;
    }

    @SuppressWarnings("unused")
    void performTwoLongFieldAccess(UnresolvedAtTestTime holder) {
        final long a =  holder._longField;
        final long b =  holder._longField2;
    }

    ///////////////////////////////

    void preload(final String className) {
        MaxineVM.usingTarget(new Runnable() {
            public void run() {
                Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, className);
            }
        });
    }

    void compileMethod(String methodName, String fieldHolderClassName) {
        Trace.line(1, "COMPILING " + methodName);
        compileMethod(methodName, SignatureDescriptor.create("(" + JavaTypeDescriptor.getDescriptorForJavaString(fieldHolderClassName) + ")V"));
    }

    void compileOwnFieldAccess(Class <?> fieldHolderClass, Kind resultKind) {
        compileMethod(fieldHolderClass, "f",   SignatureDescriptor.create("()" + resultKind.character()));
    }

    void compileResolvedFieldAccess(String methodName, Class <?> fieldHolderClass) {
        final String fieldHolderClassName = fieldHolderClass.getName();
        preload(fieldHolderClassName);
        compileMethod(methodName, fieldHolderClassName);
    }

    public void test_compileIntFieldAccess() {
        compileOwnFieldAccess(IntFieldHolder.class, Kind.INT);
    }

    public void test_compileFloatFieldAccess() {
        compileOwnFieldAccess(FloatFieldHolder.class, Kind.FLOAT);
    }

    public void test_compileLongFieldAccess() {
        compileOwnFieldAccess(LongFieldHolder.class, Kind.LONG);
    }

    ///////////////////////////// INT

    public void test_compileResolvedOneIntFieldAccess() {
        compileResolvedFieldAccess("performOneIntFieldAccess", IntFieldHolder.class);
    }

    public void test_compileResolvedTwoIntFieldAccess() {
        compileResolvedFieldAccess("performTwoIntFieldAccess", IntFieldHolder.class);
    }

    public void test_compileUnresolvedOneIntFieldAccess() {
        compileMethod("performOneIntFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    public void test_compileUnresolvedTwoIntFieldAccess() {
        compileMethod("performTwoIntFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    ///////////////////////////// FLOAT

    public void test_compileResolvedOneFloatFieldAccess() {
        compileResolvedFieldAccess("performOneFloatFieldAccess", FloatFieldHolder.class);
    }

    public void test_compileResolvedTwoFloatFieldAccess() {
        compileResolvedFieldAccess("performTwoFloatFieldAccess", FloatFieldHolder.class);
    }

    public void test_compileUnresolvedOneFloatFieldAccess() {
        compileMethod("performOneFloatFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    public void test_compileUnresolvedTwoFloatFieldAccess() {
        compileMethod("performTwoFloatFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    ///////////////////////////// LONG
    public void test_compileResolvedOneLongFieldAccess() {
        compileResolvedFieldAccess("performOneLongFieldAccess", LongFieldHolder.class);
    }

    public void test_compileResolvedTwoLongFieldAccess() {
        compileResolvedFieldAccess("performTwoLongFieldAccess", LongFieldHolder.class);
    }

    public void test_compileUnresolvedOneLongFieldAccess() {
        compileMethod("performOneLongFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    public void test_compileUnresolvedTwoLongFieldAccess() {
        compileMethod("performTwoLongFieldAccess", UNRESOLVED_CLASS_NAME);
    }

    /**
     * Testing with unresolved, resolved, and initialized class constant.
     */
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, ResolvedFieldAccessTemplateSource.class, InitializedStaticFieldAccessTemplateSource.class, InstrumentedBytecodeSource.class};
    }

}
