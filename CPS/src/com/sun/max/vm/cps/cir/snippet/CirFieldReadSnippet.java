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
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.value.*;

/**
 * Snippets that implement field read access operations given a field actor.
 *
 * @author Bernd Mathiske
 */
public final class CirFieldReadSnippet extends CirSnippet {

    private final CirSnippet cirTupleOffsetSnippet;

    @HOSTED_ONLY
    protected CirFieldReadSnippet(FieldReadSnippet fieldReadSnippet, CirSnippet cirTupleOffsetSnippet) {
        super(fieldReadSnippet);
        this.cirTupleOffsetSnippet = cirTupleOffsetSnippet;
    }

    private enum FieldReadParameter {
        tuple, fieldActor, normalContinuation, exceptionContinuation
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return true;
    }

    private enum TupleOffsetParameter {
        tuple, offset, normalContinuation, exceptionContinuation
    }

    private CirValue[] getTupleOffsetArguments(CirValue[] fieldReadArguments, FieldActor fieldActor) {
        // ATTENTION: this requires that the two parameter lists are aligned! See enums above.
        final CirValue[] tupleOffsetArguments = fieldReadArguments.clone();
        tupleOffsetArguments[TupleOffsetParameter.offset.ordinal()] = CirConstant.fromInt(fieldActor.offset());
        return tupleOffsetArguments;
    }

    /**
     * Replaces all builtins with their respective foldable variant.
     * The beta reduction dance below shields the continuations from this transformation.
     */
    private CirCall foldWithConstantField(CirOptimizer cirOptimizer, CirValue[] arguments, FieldActor fieldActor, CirBuiltinVariantOptimization.Variant variant) {
        final CirValue[] a = getTupleOffsetArguments(arguments, fieldActor);

        final CirClosure closure = cirTupleOffsetSnippet.copyClosure();

        final CirGenerator cirGenerator = cirOptimizer.cirGenerator();

        if (MaxineVM.isHosted()) {
            // If snippet compilation has not been completed, snippets are still in unoptimized form.
            // Thus builtins can hide behind higher level calls.
            // So here we first need to compile the tuple offset snippet down to builtin calls to reveal all its builtins.
            // Only then we can replace these builtins with their respective foldable variants.
            if (!cirGenerator.compilerScheme().areSnippetsCompiled() && cirGenerator.compilerScheme().optimizing()) {
                CirOptimizer.apply(cirGenerator, cirTupleOffsetSnippet, closure, CirInliningPolicy.STATIC);
            }
        }

        final CirCall call = CirBetaReduction.applyMultiple(closure, a[TupleOffsetParameter.tuple.ordinal()], a[TupleOffsetParameter.offset.ordinal()]); // omitting the exception arguments
        CirBuiltinVariantOptimization.apply(cirGenerator, call, variant, cirOptimizer.cirMethod());
        assert call == closure.body();
        closure.setBody(call);
        return CirBetaReduction.applyMultiple(closure, a); // now filling in the exception arguments
    }

    private CirCall foldWithMutableField(CirValue[] arguments, FieldActor fieldActor) {
        final CirValue[] a = getTupleOffsetArguments(arguments, fieldActor);
        return new CirCall(cirTupleOffsetSnippet, a);
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        if (isConstantArgument(arguments, FieldReadParameter.fieldActor)) {
            final FieldActor fieldActor = (FieldActor) getConstantArgumentValue(arguments, FieldReadParameter.fieldActor).asObject();
            if (fieldActor.isConstant()) {
                if (isConstantArgument(arguments, FieldReadParameter.tuple)) {
                    return super.fold(cirOptimizer, arguments);
                }
                return foldWithConstantField(cirOptimizer, arguments, fieldActor, CirBuiltinVariantOptimization.Variant.FOLDABLE);
            }
            if (fieldActor.isConstantWhenNotZero()) {
                if (isConstantArgument(arguments, FieldReadParameter.tuple)) {
                    final Value tuple = getConstantArgumentValue(arguments, FieldReadParameter.tuple);
                    final Value value = fieldActor.getValue(tuple.asObject());
                    if (!value.isZero()) {
                        return super.fold(cirOptimizer, arguments);
                    }
                }
                return foldWithConstantField(cirOptimizer, arguments, fieldActor, CirBuiltinVariantOptimization.Variant.FOLDABLE_WHEN_NOT_ZERO);
            }
            return foldWithMutableField(arguments, fieldActor);
        }
        return inline(cirOptimizer, arguments, NO_JAVA_FRAME_DESCRIPTOR);
    }
}
