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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cir.transform.*;

/**
 * The context and driver for optimizing a CIR graph for a given method.
 *
 * @author Bernd Mathiske
 */
public class CirOptimizer {

    private final CirGenerator _cirGenerator;
    private final CirMethod _cirMethod;
    private final CirNode _node;
    private final CirInliningPolicy _inliningPolicy;

    public CirGenerator cirGenerator() {
        return _cirGenerator;
    }

    public CirNode node() {
        return _node;
    }

    private static final boolean OBSERVE_COMPLETE_IR = true;

    public void notifyBeforeTransformation(CirNode node, Transformation transformation) {
        _cirGenerator.notifyBeforeTransformation(_cirMethod, OBSERVE_COMPLETE_IR ? _node : node, transformation);
    }

    public void notifyAfterTransformation(CirNode node, Transformation transformation) {
        _cirGenerator.notifyAfterTransformation(_cirMethod, OBSERVE_COMPLETE_IR ? _node : node, transformation);
    }

    public CirInliningPolicy inliningPolicy() {
        return _inliningPolicy;
    }

    public CirMethod cirMethod() {
        return _cirMethod;
    }

    public ClassMethodActor classMethodActor() {
        return _cirMethod.classMethodActor();
    }

    public CirOptimizer(CirGenerator cirGenerator, CirMethod cirMethod, CirNode node, CirInliningPolicy inliningPolicy) {
        _cirGenerator = cirGenerator;
        _node = node;
        _inliningPolicy = inliningPolicy;
        _cirMethod = cirMethod;
    }

    public static void apply(CirGenerator cirGenerator, CirMethod cirMethod, CirNode node, CirInliningPolicy inliningPolicy) {
        final CirOptimizer optimizer = new CirOptimizer(cirGenerator, cirMethod, node, inliningPolicy);
        boolean inflated = true;
        do {
            while (CirDeflation.apply(optimizer, node)) {
                // do nothing.
            }
            final Iterable<CirBlock> blocks = CirBlockUpdating.apply(node);
            CirBlockParameterMerging.apply(blocks);
            if (CirConstantBlockArgumentsPropagation.apply(blocks)) {
                continue;
            }
            inflated = false;
            while (CirInlining.apply(optimizer, node)) {
                inflated = true;
            }
        } while (inflated);
    }

}
