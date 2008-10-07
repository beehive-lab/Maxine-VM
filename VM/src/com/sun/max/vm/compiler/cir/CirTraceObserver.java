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
/*VCSID=b8fe850b-6316-4f1c-b8c4-36d677bad2b2*/
package com.sun.max.vm.compiler.cir;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

/**
 * Extends {@link IrTraceObserver} to show traces of each CIR transformation.
 * This class deals with the fact that the CIR graph is not attached to the enclosing {@link CirMethod} instance
 * until the CIR compilation has completed.
 *
 * <p>
 * To enable CIR tracing during compilation at prototyping time or in the target, pass the following
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
    }

    public enum Transformation {
        TEMPLATE_CHECKING("Checking template CIR", 3),
        BUILTIN_VARIANT_OPTIMIZATION("Builtin Variant Optimization", 5),
        OPTIMIZATION("Optimizing", 3),
        SNIPPET_OPTIMIZATION("Snippet Optimization", 2),
        INITIAL_CIR_CREATION("Initial CIR", 2),
        HCIR_FREE_VARIABLE_CAPTURING("HCIR Free Variable Capturing", 2),
        ALPHA_CONVERSION("Alpha conversion", 2),
        JAVA_LOCALS_PRUNING("Pruning Java locals", 2),
        HCIR_SPLITTING("Split HCIR", 2),
        HCIR_TO_LCIR("HCIR -> LCIR", 2),
        FOLDING("Folding", 4),
        REDUCING("Reducing", 4),
        DEFLATION("Deflation", 4),
        BETA_REDUCTION("Beta Reduction", 4),
        BLOCK_UPDATING("Block Updating", 4),
        METHOD_UPDATING("Method Updating", 4),
        INLINING("Inlining", 4),
        LCIR_FREE_VARIABLE_CAPTURING("LCIR Free Variable Capturing", 2),
        WRAPPER_APPLICATION("Applying wrapper", 3);


        Transformation(String description, int traceLevel) {
            _description = description;
            _traceLevel = traceLevel;
        }

        private final int _traceLevel;
        private final String _description;

        @Override
        public String toString() {
            return _description;
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        final int transformTraceLevel = transformTraceLevel(transform);
        if (hasLevel(transformTraceLevel)) {
            if (irMethod instanceof CirMethod) {
                _out.println(traceString(irMethod, "before transformation: " + transform));
                final CirMethod cirMethod = (CirMethod) irMethod;
                if (cirMethod.isGenerated() || context == null) {
                    _out.println(irMethod.traceToString());
                } else {
                    final CirNode cirNode = (CirNode) context;
                    _out.println(cirNode.traceToString(false));
                }
            }
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        final int transformTraceLevel = transformTraceLevel(transform);
        if (hasLevel(transformTraceLevel)) {
            if (irMethod instanceof CirMethod) {
                _out.println(traceString(irMethod, "after transformation: " + transform));
                final CirMethod cirMethod = (CirMethod) irMethod;
                if (cirMethod.isGenerated() || context == null) {
                    _out.println(irMethod.traceToString());
                } else {
                    final CirNode cirNode = (CirNode) context;
                    _out.println(cirNode.traceToString(false));
                }
            }
        }
    }

    @Override
    protected int transformTraceLevel(Object transform) {
        return ((Transformation) transform)._traceLevel;
    }
}
