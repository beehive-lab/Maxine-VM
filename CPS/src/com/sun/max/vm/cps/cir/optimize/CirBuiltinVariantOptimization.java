/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.builtin.*;

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
