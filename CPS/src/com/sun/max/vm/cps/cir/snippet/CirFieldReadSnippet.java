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

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.object.host.*;
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
                    final Value value = MaxineVM.isHosted() ? HostTupleAccess.readValue(tuple.asObject(), fieldActor) : fieldActor.readValue(tuple.asReference());
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
