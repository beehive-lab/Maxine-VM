/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;

public class LIRPhiMapping {
    private final LIRBlock block;

    private final CiValue[][] inputs;
    private final CiVariable[] results;

    public LIRPhiMapping(LIRBlock block, LIRGenerator gen) {
        this.block = block;

        assert block.firstNode() instanceof MergeNode : "phi functions are only present at control flow merges";
        MergeNode mergeNode = (MergeNode) block.firstNode();
        List<PhiNode> phis = mergeNode.phis().snapshot();

        int numPhis = phis.size();
        int numPreds = block.numberOfPreds();

        results = new CiVariable[numPhis];
        for (int i = 0; i < numPhis; i++) {
            CiVariable opd = gen.newVariable(phis.get(i).kind());
            gen.setResult(phis.get(i), opd);
            results[i] = opd;
        }

        inputs = new CiValue[numPreds][numPhis];
        for (int i = 0; i < numPreds; i++) {
            assert i == mergeNode.phiPredecessorIndex((FixedNode) block.predAt(i).lastNode()) : "block predecessors and node predecessors must have same order";
            for (int j = 0; j < numPhis; j++) {
                inputs[i][j] = gen.operand(phis.get(j).valueAt(i));
            }
        }
    }

    public CiVariable[] results() {
        return results;
    }

    public CiValue[] inputs(LIRBlock pred) {
        assert pred.numberOfSux() == 1 && pred.suxAt(0) == block;
        return inputs[block.getPredecessors().indexOf(pred)];
    }
}
