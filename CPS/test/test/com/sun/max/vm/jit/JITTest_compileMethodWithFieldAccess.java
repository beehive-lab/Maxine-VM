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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.hosted.*;
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
        int a;
        int b;
        int f() {
            return 2 * a + b;
        }
    }

    static class FloatFieldHolder {
        float a;
        float b;
        float f() {
            return 2.5F * a + b;
        }
    }

    static class LongFieldHolder {
        long a;
        long b;

        long f() {
            return 2 * a + b;
        }
    }

    ///////////////////////////// INT

    @SuppressWarnings("unused")
    void performOneIntFieldAccess(IntFieldHolder holder) {
        final int a = holder.a;
    }

    @SuppressWarnings("unused")
    void performTwoIntFieldAccess(IntFieldHolder holder) {
        final int a = holder.a;
        final int b = holder.b;
    }

    @SuppressWarnings("unused")
    void performOneIntFieldAccess(UnresolvedAtTestTime holder) {
        final int a = holder.intField;
    }

    @SuppressWarnings("unused")
    void performTwoIntFieldAccess(UnresolvedAtTestTime holder) {
        final int a =  holder.intField;
        final int b =  holder.intField2;
    }

    ///////////////////////////// FLOAT

    @SuppressWarnings("unused")
    void performOneFloatFieldAccess(FloatFieldHolder holder) {
        final float a = holder.a;
    }

    @SuppressWarnings("unused")
    void performTwoFloatFieldAccess(FloatFieldHolder holder) {
        final float a = holder.a;
        final float b = holder.b;
    }

    @SuppressWarnings("unused")
    void performOneFloatFieldAccess(UnresolvedAtTestTime holder) {
        final float a = holder.floatField;
    }

    @SuppressWarnings("unused")
    void performTwoFloatFieldAccess(UnresolvedAtTestTime holder) {
        final float a =  holder.floatField;
        final float b =  holder.floatField2;
    }

    ///////////////////////////// LONG

    @SuppressWarnings("unused")
    void performOneLongFieldAccess(LongFieldHolder holder) {
        final long a = holder.a;
    }

    @SuppressWarnings("unused")
    void performTwoLongFieldAccess(LongFieldHolder holder) {
        final long a = holder.a;
        final long b = holder.b;
    }

    @SuppressWarnings("unused")
    void performOneLongFieldAccess(UnresolvedAtTestTime holder) {
        final long a = holder.longField;
    }

    @SuppressWarnings("unused")
    void performTwoLongFieldAccess(UnresolvedAtTestTime holder) {
        final long a =  holder.longField;
        final long b =  holder.longField2;
    }

    ///////////////////////////////

    void preload(final String className) {
        Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className);
    }

    void compileMethod(String methodName, String fieldHolderClassName) {
        Trace.line(1, "COMPILING " + methodName);
        compileMethod(methodName, SignatureDescriptor.create("(" + JavaTypeDescriptor.getDescriptorForJavaString(fieldHolderClassName) + ")V"));
    }

    void compileOwnFieldAccess(Class <?> fieldHolderClass, Kind resultKind) {
        compileMethod(fieldHolderClass, "f",   SignatureDescriptor.create("()" + resultKind.character));
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
}
