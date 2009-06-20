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
import com.sun.max.vm.compiler.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cir.builtin.*;

/**
 * Like CIR replication, except that builtins with a foldable variant get substituted for the latter.
 *
 * @author Bernd Mathiske
 */
public class CirBuiltinVariantOptimization extends CirDeflation {

    public static enum Variant {
        FOLDABLE, FOLDABLE_WHEN_NOT_ZERO;
    }

    private final Variant variant;

    CirBuiltinVariantOptimization(CirOptimizer optimizer, CirNode node, Variant variant) {
        super(optimizer, node);
        this.variant = variant;
    }

    public static void apply(CirGenerator cirGenerator, CirNode node, Variant variant, CirMethod cirMethod) {
        final CirOptimizer optimizer = new CirOptimizer(cirGenerator, cirMethod, node, CirInliningPolicy.NONE);
        optimizer.notifyBeforeTransformation(node, TransformationType.BUILTIN_VARIANT_OPTIMIZATION);
        final CirBuiltinVariantOptimization optimization = new CirBuiltinVariantOptimization(optimizer, node, variant);
        optimization.reduceCalls();
        optimizer.notifyAfterTransformation(node, TransformationType.BUILTIN_VARIANT_OPTIMIZATION);
    }

    @Override
    protected boolean updateCall(CirCall call) {
        boolean result = super.updateCall(call);
        if (call.procedure() instanceof CirBuiltin) {
            final CirBuiltin cirBuiltin = (CirBuiltin) call.procedure();
            switch (variant) {
                case FOLDABLE: {
                    if (cirBuiltin != cirBuiltin.foldableVariant()) {
                        call.setProcedure(cirBuiltin.foldableVariant());
                        result = true;
                    }
                    break;
                }
                case FOLDABLE_WHEN_NOT_ZERO: {
                    if (cirBuiltin != cirBuiltin.foldableWhenNotZeroVariant()) {
                        call.setProcedure(cirBuiltin.foldableWhenNotZeroVariant());
                        result = true;
                    }
                    break;
                }
            }
        }
        return result;
    }

}
