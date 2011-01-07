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

import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the compiler for the
 * sequence aload_0, getfield, ireturn, so as to compare with what
 * a template-based JIT could do.
 *
 * @author Laurent Daynes
 */
public class JITTest_resolvedGetField extends CompilerTestCase<CPSTargetMethod> {

    private int intField;

    static class ResolvedAtCompileTime {
        int intField;
    }

    public int perform_not_equal(int a, int b) {
        return (a != b) ? 1 : 0;
    }

    // Used to debug conditional branches
    public void test_not_equal() {
        compileMethod("perform_not_equal", SignatureDescriptor.create(int.class, int.class, int.class));
    }

    public Object perform_use_null_constant(Object o, int a) {
        return (a != 0) ? o : null;
    }

    public void test_use_null_constant() {
        compileMethod("perform_use_null_constant", SignatureDescriptor.create(Object.class, Object.class, int.class));
    }

    public int perform_visitor(ResolvedAtCompileTime resolvedAtCompileTime) {
        return resolvedAtCompileTime.intField;
    }

    public void test_visitor() throws ClassNotFoundException {
        // Make sure the class whose field is being accessed is loaded in the target first (we want a resolved symbol at compiled time).
        HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.loadClass(ResolvedAtCompileTime.class.getName());
        // Now compile the method we're interested in
        compileMethod("perform_visitor", SignatureDescriptor.create(int.class, ResolvedAtCompileTime.class));
    }

    public void perform_increment() {
        int i = intField;
        i += 5;
        intField = i;
    }

    public void test_increment() {
        compileMethod("perform_increment", SignatureDescriptor.create(void.class));
    }

    public int foo(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        return (i1 + i2 * i3 + i4 - i5 * i6) << i7;
    }

    public void perform_invocation(int a) {
        int b = a * 53;
        b = foo(b, a, 67, 86, 77, 88, 99);
    }

    public void test_invocation() {
        compileMethod("perform_invocation", SignatureDescriptor.create(void.class, int.class));
        compileMethod("foo", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class));
    }
}
