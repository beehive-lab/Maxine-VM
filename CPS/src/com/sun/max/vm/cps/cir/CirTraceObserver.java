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
package com.sun.max.vm.cps.cir;

import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each CIR transformation.
 * This class deals with the fact that the CIR graph is not attached to the enclosing {@link CirMethod} instance
 * until the CIR compilation has completed.
 *
 * <p>
 * To enable CIR tracing during compilation while bootstrapping or in the target, pass the following
 * system property:
 * <p>
 * <pre>
 *     -Dmax.ir.observers=com.sun.max.vm.compiler.cir.CirTraceObserver
 * </pre>
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class CirTraceObserver extends IrTraceObserver {

    public CirTraceObserver() {
        super(CirMethod.class);
    }

    public enum TransformationType {
        TEMPLATE_CHECKING("Checking template CIR", 3),
        BUILTIN_VARIANT_OPTIMIZATION("Builtin Variant Optimization", 5),
        OPTIMIZATION("Optimizing", 3),
        SNIPPET_OPTIMIZATION("Snippet Optimization", 2),
        INITIAL_CIR_CREATION("Initial CIR", 2),
        HCIR_FREE_VARIABLE_CAPTURING("HCIR Free Variable Capturing", 2),
        ALPHA_CONVERSION("Alpha conversion", 2),
        JAVA_LOCALS_PRUNING("Pruning Java locals", 2),
        COPY_PROPAGATION("Copy propagation", 2),
        HCIR_TO_LCIR("HCIR -> LCIR", 2),
        FOLDING("Folding", 3),
        REDUCING("Reducing", 4),
        DEFLATION("Deflation", 3),
        BETA_REDUCTION("Beta Reduction", 4),
        BLOCK_INLINING("Block Inlining", 3),
        BLOCK_PARAMETER_MERGING("Block Parameter Merging", 4),
        CONSTANT_BLOCK_ARGUMENT_PROPAGATION("Constant Block Argument Propagation", 4),
        METHOD_INLINING("Method Inlining", 3),
        INLINING("Inlining", 3),
        LCIR_FREE_VARIABLE_CAPTURING("LCIR Free Variable Capturing", 2),
        WRAPPER_APPLICATION("Applying wrapper", 3);

        TransformationType(String description, int traceLevel) {
            this.description = description;
            this.traceLevel = traceLevel;
        }

        private final int traceLevel;
        private final String description;

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Describes the application of a particular {@linkplain TransformationType type} of transformation.
     * The description may optionally include more detail about the transformation being applied.
     * For example, when {@linkplain TransformationType#METHOD_INLINING inlining} a method, the name of
     * the method being inlined could be part of the description.
     */
    public static class Transformation {
        final TransformationType type;
        final String detail;

        public Transformation(TransformationType type) {
            this(type, null);
        }

        public Transformation(TransformationType type, String detail) {
            this.type = type;
            this.detail = detail;
        }

        @Override
        public String toString() {
            if (detail == null) {
                return type.toString();
            }
            return type + " -- " + detail;
        }
    }

    private static final ThreadLocal<String> lastTrace = new ThreadLocal<String>();

    private String trace(CirNode node) {
        final String trace = node.traceToString(false);
        final String theLastTrace = lastTrace.get();
        if (trace.equals(theLastTrace)) {
            return "[no change from last CIR trace]";
        }
        lastTrace.set(trace);
        return trace;
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod) {
            final int transformTraceLevel = transformTraceLevel(transform);
            if (hasLevel(transformTraceLevel)) {
                if (irMethod instanceof CirMethod) {
                    out.println(traceString(irMethod, "before transformation: " + transform));
                    final CirMethod cirMethod = (CirMethod) irMethod;
                    if (context == null) {
                        out.println(trace(cirMethod));
                    } else {
                        final CirNode cirNode = (CirNode) context;
                        out.println(trace(cirNode));
                    }
                    out.flush();
                }
            }
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod) {
            final int transformTraceLevel = transformTraceLevel(transform);
            if (hasLevel(transformTraceLevel)) {
                if (irMethod instanceof CirMethod) {
                    out.println(traceString(irMethod, "after transformation: " + transform));
                    final CirMethod cirMethod = (CirMethod) irMethod;
                    if (context == null) {
                        out.println(trace(cirMethod));
                    } else {
                        final CirNode cirNode = (CirNode) context;
                        out.println(trace(cirNode));
                    }
                    out.flush();
                }
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        if (transform instanceof Transformation) {
            return ((Transformation) transform).type.traceLevel;
        }
        return ((TransformationType) transform).traceLevel;
    }
}
