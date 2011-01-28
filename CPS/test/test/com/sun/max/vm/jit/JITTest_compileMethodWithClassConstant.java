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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;
/**
 * Testing the JIT-compiler with methods including reference to resolved and unresolved class.
 * This suite of tests exercises the part of the template-based JIT that (a) selects an appropriate template
 * based on the initialisation state of the referred class (unresolved, loaded, initialised), and customise
 * the template appropriately (i.e., replace ResolutionGuard of the template with appropriate ResolutionGuard,
 * replace reference literals to actors of the template with actors of the obtained from the constant pool of the
 * compiled method, etc.).
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMethodWithClassConstant extends JitCompilerTestCase {

    public void perform_ldc_resolved_class() {
        @SuppressWarnings("unused")
        final Class c = JITTest_compileMethodWithClassConstant.class;
    }

    public void perform_ldc_unresolved_class() {
        @SuppressWarnings("unused")
       final Class c = UnresolvedAtTestTime.class;
    }

    public void perform_new_resolved_objarray_op() {
        @SuppressWarnings("unused")
        final  JITTest_compileMethodWithClassConstant[] a = new JITTest_compileMethodWithClassConstant[3];
    }

    public void perform_new_unresolved_objarray_op() {
        @SuppressWarnings("unused")
        final  UnresolvedAtTestTime[] a = new UnresolvedAtTestTime[3];
    }

    public boolean perform_resolved_instance_of_op(Object o) {
        return o instanceof JITTest_compileMethodWithClassConstant;
    }

    public boolean perform_unresolved_instance_of_op(Object o) {
        return o instanceof UnresolvedAtTestTime;
    }

    public void perform_resolved_typecast(Object o) {
        @SuppressWarnings("unused")
        final JITTest_compileMethodWithClassConstant a = (JITTest_compileMethodWithClassConstant) o;
    }

    public void perform_unresolved_typecast(Object o) {
        @SuppressWarnings("unused")
        final UnresolvedAtTestTime a = (UnresolvedAtTestTime) o;
    }

    public void test_new_resolved_objarray_op() {
        compileMethod("perform_new_resolved_objarray_op");
    }

    public void test_new_unresolved_objarray_op() {
        compileMethod("perform_new_unresolved_objarray_op");
    }

    private void compile(String methodName, String signature) {
        compileMethod(methodName, SignatureDescriptor.create(signature));
    }

    public void test_unresolved_instance_of_op() {
        compile("perform_resolved_instance_of_op", "(Ljava/lang/Object;)Z");
    }

    public void test_unresolved_typecast() {
        compile("perform_unresolved_typecast", "(Ljava/lang/Object;)V");
    }

    public void test_resolved_typecast() {
        compile("perform_resolved_typecast", "(Ljava/lang/Object;)V");
    }

    public void test_resolved_instance_of_op() {
        compile("perform_resolved_instance_of_op", "(Ljava/lang/Object;)Z");
    }

    public void test_ldc_resolved_class() {
        compileMethod("perform_ldc_resolved_class");
    }

    private boolean classIsUnresolved(final String classname) {
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(classname);
        final ClassActor classActor = ClassActor.fromJava(getClass());
        return !typeDescriptor.isResolvableWithoutClassLoading(classActor.constantPool().classLoader());
    }

    public void test_ldc_unresolved_class() {
        assert classIsUnresolved("test.com.sun.max.vm.jit.UnresolvedAtTestTime");
        compileMethod("perform_ldc_unresolved_class");
    }
}
