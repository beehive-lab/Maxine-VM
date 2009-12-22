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

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.cps.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.template.source.*;

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

    /**
     * Testing with unresolved, resolved, and initialized class constant.
     */
    @Override
    protected Class[] templateSources() {
        return TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES;
    }

}
