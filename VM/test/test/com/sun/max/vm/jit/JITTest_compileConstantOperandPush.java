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
/*VCSID=ed515bbb-38f9-4108-b91f-6ceb024523b3*/
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;


/**
 * Testing the JIT-compiler with methods using bytecode instructions with explicit bytecode operand (e.g., bipush, sipush).
 *
 * @author Laurent Daynes
 */
public class JITTest_compileConstantOperandPush extends JitCompilerTestCase {
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, InstrumentedBytecodeSource.class};
    }

    void perform_bipush(int a) {
        @SuppressWarnings("unused")
        final int r = 111 * a;
    }

    void perform_sipush(int a) {
        @SuppressWarnings("unused")
        final int r = 1111 * a;
    }

    public void test_bipush() {
        compilePushMethod("perform_bipush");
    }

    public void test_sipush() {
        compilePushMethod("perform_sipush");
    }

    private void compilePushMethod(String methodName) {
        compileMethod(methodName, SignatureDescriptor.create("(I)V"));
    }

}
