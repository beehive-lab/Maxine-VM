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
package com.sun.max.vm.compiler.cir.optimize;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;

/**
 * Removes the effect of checkcast byte codes from a specific position in CIR.
 *
 * This is used to implement {@link UnsafeLoophole}.
 * When we use this feature, javac silently injects checkcast bytecodes
 * even though our generic typing does not seem to explicitly request them at the source code level.
 * Here need to filter these unwanted bytecodes out
 * to effectively produce no-operation type casts.
 * 
 * @author Bernd Mathiske
 */
public final class CirCheckcastPruning extends CirVisitor {

    private CirCheckcastPruning() {
    }

    /**
     * Prune the application of {@link CheckCast} in the subsequent CirCall, if any.
     * 
     * @param value the continuation to be pruned
     */
    public static void apply(CirValue value) {
        final CirCheckcastPruning removal = new CirCheckcastPruning();
        value.acceptVisitor(removal);
    }

    @Override
    public void visitCall(CirCall call) {
        final CirValue procedure = call.procedure();
        if (procedure instanceof CirClosure) {
            procedure.acceptVisitor(this);
            return;
        }
        if (procedure != CirSnippet.get(Snippet.CheckCast.SNIPPET)) {
            if (procedure == CirSnippet.get(ResolutionSnippet.ResolveClass.SNIPPET)) {
                final CirValue normalContinuation = CirMethod.getNormalContinuation(call.arguments());
                normalContinuation.acceptVisitor(this);
            }
            return;
        }
        final CirValue[] arguments = call.arguments();
        call.setProcedure(CirMethod.getNormalContinuation(arguments), call.bytecodeLocation());
        call.setArguments();
        call.setJavaFrameDescriptor(null);
    }

    @Override
    public void visitClosure(CirClosure closure) {
        closure.body().acceptVisitor(this);
    }
}
