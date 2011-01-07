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

import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods performing static field access to initialized and uninitialized class.
 * This suite of tests exercises the part of the template-based JIT that (a) selects an appropriate template
 * based on the initialization state of the refered class (unresolved, loaded, initialized), and customize
 * the template appropriately (i.e., replace ResolutionGuard of the template with appropriate ResolutionGuard,
 * replace reference literals to static tuple with static tuple obtained from the constant pool of the
 * compiled method, modified field offsets with appropriate offset values, etc.).
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMethodWithStaticFieldAccess extends JitCompilerTestCase {
    static byte staticByteField;
    static boolean staticBooleanField;
    static char staticCharField;
    static short staticShortField;
    static int staticIntField;
    static float staticFloatField;
    static long staticLongField;
    static double staticDoubleField;
    static JITTest_compileMethodWithStaticFieldAccess staticObjectField;

    String methodNameFor(String typeName, boolean resolved) {
        return "use" + (resolved ? "Resolved" : "Unresolved") + "Static" + typeName + "Field";
    }

    String methodNameForKind(Kind kind, boolean resolved) {
        final String kindName = kind.name.toString();
        return  methodNameFor(kindName.substring(0, 1).toUpperCase() +  kindName.substring(1).toLowerCase(), resolved);
    }

    /////////////////////////////////////////////// RESOLVED & INITIALIZED CLASS
    void useResolvedStaticByteField() {
        staticByteField++;
    }

    void useResolvedStaticBooleanField() {
        staticBooleanField ^= true;
    }

    void useResolvedStaticShortField() {
        staticShortField++;
    }

    void useResolvedStaticIntField() {
        staticIntField++;
    }

    void useResolvedStaticLongField() {
        staticLongField++;
    }

    void useResolvedStaticFloatField() {
        staticFloatField *= 0.15F;
    }

    void useResolvedStaticDoubleField() {
        staticDoubleField *= 0.125D;
    }

    @SuppressWarnings("unused")
    void useResolvedStaticObjectField() {
        final Object o = staticObjectField;
        staticObjectField =  null;
    }

    public void test_useResolvedStaticByteField() {
        compileMethod(methodNameForKind(Kind.BYTE, true));
    }

    public void test_useResolvedStaticBooleanField() {
        compileMethod(methodNameForKind(Kind.BOOLEAN, true));
    }

    public void test_useResolvedStaticShortField() {
        compileMethod(methodNameForKind(Kind.SHORT, true));
    }

    public void test_useResolvedStaticIntField() {
        compileMethod(methodNameForKind(Kind.INT, true));
    }

    public void test_useResolvedStaticLongField() {
        compileMethod(methodNameForKind(Kind.LONG, true));
    }

    public void test_useResolvedStaticFloatField() {
        compileMethod(methodNameForKind(Kind.FLOAT, true));
    }

    public void test_useResolvedStaticDoubleField() {
        compileMethod(methodNameForKind(Kind.DOUBLE, true));
    }

    public void test_useResolvedStaticObjectField() {
        compileMethod(methodNameFor("Object", true));
    }

    /////////////////////////////////////////////// UNRESOLVED & UNINITIALIZED CLASS
    void useUnresolvedStaticByteField()  {
        UnresolvedAtTestTime.staticByteField++;
    }

    void useUnresolvedStaticBooleanField() {
        UnresolvedAtTestTime.staticBooleanField ^= true;
    }

    void useUnresolvedStaticShortField()  {
        UnresolvedAtTestTime.staticShortField++;
    }

    void useUnresolvedStaticIntField()  {
        UnresolvedAtTestTime.staticIntField++;
    }

    void useUnresolvedStaticLongField() {
        UnresolvedAtTestTime.staticLongField++;
    }

    void useUnresolvedStaticFloatField() {
        UnresolvedAtTestTime.staticFloatField *= 0.15F;
    }

    void useUnresolvedStaticDoubleField() {
        UnresolvedAtTestTime.staticDoubleField *= 0.125D;
    }

    @SuppressWarnings("unused")
    void useUnresolvedStaticObjectField() {
        final Object o = UnresolvedAtTestTime.staticObjectField;
        UnresolvedAtTestTime.staticObjectField =  null;
    }

    public void test_useUnresolvedStaticByteField() {
        compileMethod(methodNameForKind(Kind.BYTE, false));
    }

    public void test_useUnresolvedStaticBooleanField() {
        compileMethod(methodNameForKind(Kind.BOOLEAN, false));
    }

    public void test_useUnresolvedStaticShortField() {
        compileMethod(methodNameForKind(Kind.SHORT, false));
    }

    public void test_useUnresolvedStaticIntField() {
        compileMethod(methodNameForKind(Kind.INT, false));
    }

    public void test_useUnresolvedStaticLongField() {
        compileMethod(methodNameForKind(Kind.LONG, false));
    }

    public void test_useUnresolvedStaticFloatField() {
        compileMethod(methodNameForKind(Kind.FLOAT, false));
    }

    public void test_useUnresolvedStaticDoubleField() {
        compileMethod(methodNameForKind(Kind.DOUBLE, false));
    }

    public void test_useUnresolvedStaticObjectField() {
        compileMethod(methodNameFor("Object", false));
    }
}
