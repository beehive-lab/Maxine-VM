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

import java.lang.reflect.*;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.actor.member.*;

/**
 * Testing the JIT-compiler with methods including reference to resolved or unresolved class and bytecode instruction requiring
 * a class initialization barrier.
 * This suite of tests exercises the part of the template-based JIT that (a) selects an appropriate template
 * based on the initialization state of the refered class (unresolved, loaded, initialized), and customize
 * the template appropriately (i.e., replace ResolutionGuard of the template with appropriate ResolutionGuard,
 * replace reference literals to actors of the template with actors of the obtained from the constant pool of the
 * compiled method, etc.).
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMethodWithCIB extends JitCompilerTestCase {

    public void perform_resolved_new_op() {
        @SuppressWarnings("unused")
        final  JITTest_compileMethodWithCIB o = new JITTest_compileMethodWithCIB();
    }

    public void perform_unresolved_new_op() {
        @SuppressWarnings("unused")
        final UnresolvedAtTestTime o = new UnresolvedAtTestTime();
    }

    private void compileConstructor(Class<?> javaClass) throws NoSuchMethodException {
        final Constructor javaConstructor = javaClass.getConstructor();
        final ClassMethodActor classMethodActor = (ClassMethodActor) MethodActor.fromJavaConstructor(javaConstructor);
        compileMethod(classMethodActor);
    }

    public void test_resolved_constructor() throws NoSuchMethodException {
        compileConstructor(Throwable.class);
    }
    public void test_resolved_new_op() {
        compileMethod("perform_resolved_new_op");
    }

    public void test_unresolved_new_op() {
        compileMethod("perform_unresolved_new_op");
    }
}
