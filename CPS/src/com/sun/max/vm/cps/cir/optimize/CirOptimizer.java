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

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.transform.*;

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

    @HOSTED_ONLY
    public boolean hasNoInlining() {
        return classMethodActor().getAnnotation(NO_INLINING.class) != null ||
        classMethodActor().holder().getAnnotation(NO_INLINING.class) != null;
    }
}
