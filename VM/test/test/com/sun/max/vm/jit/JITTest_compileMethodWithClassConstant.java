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
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.template.source.*;
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
        return MaxineVM.usingTarget(new Function<Boolean>() {
            public Boolean call() {
                final TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(classname);
                final ClassActor classActor = ClassActor.fromJava(getClass());
                return !typeDescriptor.isResolvableWithoutClassLoading(classActor, classActor.constantPool().classLoader());
            }
        });
    }

    public void test_ldc_unresolved_class() {
        assert classIsUnresolved("test.com.sun.max.vm.jit.UnresolvedAtTestTime");
        compileMethod("perform_ldc_unresolved_class");
    }

    /**
     * Testing with unresolved, resolved, and initialised class constant.
     */
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, ResolvedBytecodeTemplateSource.class, InitializedBytecodeTemplateSource.class, InstrumentedBytecodeSource.class };
    }

}
