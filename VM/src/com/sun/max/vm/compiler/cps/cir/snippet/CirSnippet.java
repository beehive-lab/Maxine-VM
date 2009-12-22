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
package com.sun.max.vm.compiler.cps.cir.snippet;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.builtin.*;
import com.sun.max.vm.compiler.cps.cir.optimize.*;
import com.sun.max.vm.compiler.cps.cir.transform.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.*;
import com.sun.max.vm.compiler.snippet.MonitorSnippet.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.trampoline.template.*;

/**
 * IR subroutines derived by translating Java methods.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 */
public class CirSnippet extends CirMethod {

    public final Snippet snippet;
    public final boolean nonFoldable;

    @HOSTED_ONLY
    protected CirSnippet(Snippet snippet) {
        super(snippet.executable);
        this.snippet = snippet;

        if ((snippet instanceof ArraySetSnippet) ||
            (snippet instanceof FieldWriteSnippet) ||
            (snippet instanceof TemplateBasedITableTrampoline) ||
            (snippet instanceof TemplateBasedVTableTrampoline) ||
            (snippet instanceof MonitorEnter) ||
            (snippet instanceof MonitorExit) ||
            (snippet instanceof NativeCallEpilogue) ||
            (snippet instanceof NativeCallEpilogueForC) ||
            (snippet instanceof NativeCallPrologue) ||
            (snippet instanceof NativeCallPrologueForC) ||
            (snippet instanceof Snippet.CreateArithmeticException) ||
            (snippet instanceof CreateMultiReferenceArray) ||
            (snippet instanceof CreatePrimitiveArray) ||
            (snippet instanceof CreateReferenceArray) ||
            (snippet instanceof CreateTupleOrHybrid) ||
            (snippet instanceof Snippet.RaiseThrowable) ||
            (snippet instanceof MakeClassInitialized) ||
            (snippet instanceof MakeHolderInitialized) ||
            (snippet instanceof StaticTrampoline) ||
            (snippet instanceof TupleOffsetSnippet)) {
            nonFoldable = true;
        } else {
            nonFoldable = false;
        }
    }

    @Override
    public MethodActor foldingMethodActor() {
        return snippet.executable;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitSnippet(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitSnippet(this, scope);
    }

    @HOSTED_ONLY
    private static void register(CirSnippet[] cirSnippets, CirSnippet cirSnippet) {
        int serial = cirSnippet.snippet.serial();
        assert cirSnippets[serial] == null : "serial=" + serial + ", snippet=" + cirSnippet;
        cirSnippets[serial] = cirSnippet;
    }

    @HOSTED_ONLY
    private static CirSnippet[] createCirSnippets() {
        final int numberOfSnippets = Snippet.snippets().length();
        final CirSnippet[] snippets = new CirSnippet[numberOfSnippets];

        register(snippets, new CirCheckCast());
        register(snippets, new CirLinkNativeMethod());
        register(snippets, new CirMakeEntrypoint());
        register(snippets, new CirSelectInterfaceMethod());
        register(snippets, new CirSelectVirtualMethod());

        for (int i = 0; i < numberOfSnippets; i++) {
            if (snippets[i] == null) {
                Snippet snippet = Snippet.snippets().get(i);
                if (snippet instanceof FieldReadSnippet) {
                    FieldReadSnippet fieldReadSnippet = (FieldReadSnippet) snippet;
                    TupleOffsetSnippet tupleOffsetSnippet = fieldReadSnippet.tupleOffsetSnippet();
                    CirSnippet cirTupleOffsetSnippet = new CirSnippet(tupleOffsetSnippet);

                    register(snippets, new CirFieldReadSnippet(fieldReadSnippet, cirTupleOffsetSnippet));
                    register(snippets, cirTupleOffsetSnippet);
                } else if (snippet instanceof TupleOffsetSnippet) {
                    // Handled above
                    continue;
                } else if (snippet instanceof ResolutionSnippet) {
                    register(snippets, new CirResolutionSnippet((ResolutionSnippet) snippet));
                } else {
                    register(snippets, new CirSnippet(snippet));
                }
            }
        }

        for (int i = 0; i < numberOfSnippets; i++) {
            assert snippets[i] != null;
        }

        return snippets;
    }

    private static final CirSnippet[] cirSnippets = createCirSnippets();

    public static CirSnippet get(Snippet snippet) {
        return cirSnippets[snippet.serial()];
    }

    @Override
    public String toString() {
        return "<CirSnippet: " + name() + ">";
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return !nonFoldable && CirValue.areConstant(arguments);
    }

    protected CirProcedure builtinOrMethod(ClassMethodActor classMethodActor, CirGenerator cirGenerator) {
        if (classMethodActor.isBuiltin()) {
            return CirBuiltin.get(Builtin.get(classMethodActor));
        }
        return cirGenerator.createIrMethod(classMethodActor);
    }
}
