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
package com.sun.max.vm.cps.cir.snippet;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateMultiReferenceArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreatePrimitiveArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateReferenceArray;
import com.sun.max.vm.compiler.snippet.MonitorSnippet.MonitorEnter;
import com.sun.max.vm.compiler.snippet.MonitorSnippet.MonitorExit;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallEpilogue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallEpilogueForC;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologueForC;
import com.sun.max.vm.compiler.snippet.Snippet.MakeClassInitialized;
import com.sun.max.vm.compiler.snippet.Snippet.MakeHolderInitialized;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;

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
        final int numberOfSnippets = Snippet.snippets().size();
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
