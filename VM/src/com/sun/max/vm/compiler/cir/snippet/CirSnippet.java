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
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * IR subroutines derived by translating Java methods.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 */
public class CirSnippet extends CirMethod {

    private final Snippet _snippet;

    public Snippet snippet() {
        return _snippet;
    }

    protected CirSnippet(Snippet snippet) {
        super((ClassMethodActor) snippet.foldingMethodActor());
        _snippet = snippet;
    }

    @Override
    public MethodActor foldingMethodActor() {
        return _snippet.foldingMethodActor();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitSnippet(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitSnippet(this, scope);
    }

    private static CirSnippet[] createCirSnippets() {
        final int numberOfSnippets = Snippet.snippets().length();
        final CirSnippet[] cirSnippets = new CirSnippet[numberOfSnippets];
        for (int i = 0; i < numberOfSnippets; i++) {
            cirSnippets[i] = new CirSnippet(Snippet.snippets().get(i));
        }
        return cirSnippets;
    }

    private static CirSnippet[] _cirSnippets = createCirSnippets();
    /**
     * Used by subclass constructors to update the above array
     * to consistently contain entries of the respective subclass.
     */
    protected final void register() {
        _cirSnippets[_snippet.serial()] = this;
    }

    public static CirSnippet get(Snippet snippet) {
        return _cirSnippets[snippet.serial()];
    }

    @Override
    public String toString() {
        return "<CirSnippet: " + name() + ">";
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final CirValue[] a = Arrays.subArray(arguments, 0, arguments.length - 2); // exclude the continuations
        return _snippet.isFoldable(a);
    }
}
