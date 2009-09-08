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

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the compiler for the
 * sequence aload_0, getfield, ireturn, so as to compare with what
 * a template-based JIT could do.
 *
 * @author Laurent Daynes
 */
public class JITTest_unresolvedGetField extends CompilerTestCase<CPSTargetMethod> {

    private int intField;

    static class UnresolvedAtCompileTime {
        int intUnresolvedField;
    }

    public int perform_visitor(UnresolvedAtCompileTime b) {
        final int i = b.intUnresolvedField;
        return i;
    }

    public void perform_visitor2(UnresolvedAtCompileTime b) {
        @SuppressWarnings("unused")
        final int i = b.intUnresolvedField;
    }

    public void test_visitor() {
        final TargetMethod method = compileMethod("perform_visitor", SignatureDescriptor.create(int.class, UnresolvedAtCompileTime.class));
        disassemble(method);
    }
}


