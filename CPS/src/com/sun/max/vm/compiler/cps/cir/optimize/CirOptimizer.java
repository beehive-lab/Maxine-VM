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
package com.sun.max.vm.compiler.cps.cir.optimize;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cps.cir.transform.*;

/**
 * The context and driver for optimizing a CIR graph for a given method.
 *
 * @author Bernd Mathiske
 */
public class CirOptimizer {

    private final CirGenerator cirGenerator;
    private final CirMethod cirMethod;
    private final CirNode node;
    private final CirInliningPolicy inliningPolicy;

    public CirGenerator cirGenerator() {
        return cirGenerator;
    }

    public CirNode node() {
        return node;
    }

    private static final boolean OBSERVE_COMPLETE_IR = true;

    public void notifyBeforeTransformation(CirNode n, Transformation transformation) {
        cirGenerator.notifyBeforeTransformation(cirMethod, OBSERVE_COMPLETE_IR ? node : n, transformation);
    }

    public void notifyAfterTransformation(CirNode n, Transformation transformation) {
        cirGenerator.notifyAfterTransformation(cirMethod, OBSERVE_COMPLETE_IR ? node : n, transformation);
    }

    public void notifyBeforeTransformation(CirNode n, TransformationType transformationType) {
        cirGenerator.notifyBeforeTransformation(cirMethod, OBSERVE_COMPLETE_IR ? node : n, transformationType);
    }

    public void notifyAfterTransformation(CirNode n, TransformationType transformationType) {
        cirGenerator.notifyAfterTransformation(cirMethod, OBSERVE_COMPLETE_IR ? node : n, transformationType);
    }

    public CirInliningPolicy inliningPolicy() {
        return inliningPolicy;
    }

    public CirMethod cirMethod() {
        return cirMethod;
    }

    public ClassMethodActor classMethodActor() {
        return cirMethod.classMethodActor();
    }

    public CirOptimizer(CirGenerator cirGenerator, CirMethod cirMethod, CirNode node, CirInliningPolicy inliningPolicy) {
        this.cirGenerator = cirGenerator;
        this.node = node;
        this.inliningPolicy = inliningPolicy;
        this.cirMethod = cirMethod;
    }

    public static void apply(CirGenerator cirGenerator, CirMethod cirMethod, CirNode node, CirInliningPolicy inliningPolicy) {
        final CirOptimizer optimizer = new CirOptimizer(cirGenerator, cirMethod, node, inliningPolicy);
        boolean inflated = true;
        int count = 0;
        do {
            int deflations = 0;
            while (CirDeflation.apply(optimizer, node)) {
                deflations++;
                // do nothing.
            }
            final Iterable<CirBlock> blocks = CirBlockUpdating.apply(node);
            CirBlockParameterMerging.apply(optimizer, node, blocks);
            if (CirConstantBlockArgumentsPropagation.apply(optimizer, node, blocks)) {
                continue;
            }
            int inflations = 0;
            while (CirInlining.apply(optimizer, node)) {
                inflations++;
            }
            inflated = inflations != 0;
            count++;
        } while (inflated);
    }

    public boolean hasNoInlining() {
        return classMethodActor().getAnnotation(NO_INLINING.class) != null ||
        classMethodActor().holder().getAnnotation(NO_INLINING.class) != null;
    }
}
