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
package com.sun.max.vm.cps.cir.snippet;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;

/**
 * Checks whether a method has been optimized such that it does not contain any calls except to builtins.
 *
 * This is for instance necessary for bootstrapping most snippets (which are essentially just methods).
 *
 * @author Bernd Mathiske
 */
public final class CirBuiltinCheck extends CirVisitor {

    private CirBuiltinCheck(Snippet snippet) {
        this.snippet = snippet;
    }

    private final Snippet snippet;

    public static void apply(CirClosure closure, Snippet snippet) {
        CirVisitingTraversal.apply(closure, new CirBuiltinCheck(snippet));
    }

    @Override
    public void visitMethod(CirMethod method) {
        ProgramError.unexpected(errorMessagePrefix() + "method found: " + method);
    }

    @Override
    public void visitBlock(CirBlock block) {
        ProgramError.unexpected(errorMessagePrefix() + "block found: " + block.traceToString(false) + "_" + block.id());
    }

    private String errorMessagePrefix() {
        return "checking for CIR reduction of " + snippet + " to mere builtins - ";
    }

}
