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
package com.sun.c1x.opt;

import com.sun.c1x.*;
import com.oracle.max.criutils.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.c1x.value.FrameState.PhiProcedure;
import com.sun.cri.ci.*;

/**
 * This class removes simple diamonds where control flow joins and splits again immediately.
 * This is quite special-cased for this scenario:
 *
 * boolean foo() {
 *   return abc < def;
 * }
 * void bar () {
 *   if (foo) {
 *     ...
 *   }
 */
public class DiamondEliminator implements BlockClosure {
    final IR ir;
    private boolean eliminated;

    public DiamondEliminator(IR ir) {
        this.ir = ir;
        ir.startBlock.iteratePreOrder(this);
        if (eliminated) {
            new BlockMerger(ir);
        }
    }

    public void apply(BlockBegin block) {
        // check that we have two predecessors and that block ends with an If (which implies two successors)
        if (!(block.numberOfPreds() == 2 && block.end() instanceof If)) {
            return;
        }
        If curIf = (If) block.end();
        BlockBegin leftPred = block.predAt(0);
        BlockBegin rightPred = block.predAt(1);

        // check that we compare a phi function with a constant
        if (!(curIf.x() instanceof Phi
             && leftPred.end() instanceof Goto && rightPred.end() instanceof Goto
             && curIf.y() instanceof Constant
             && curIf.x().kind == CiKind.Int && curIf.y().kind == CiKind.Int)) {
            return;
        }
        final Phi ifPhi = (Phi) curIf.x();
        Constant ifCompare = (Constant) curIf.y();

        // check that phi function belongs to our block, and block contains nothing but the constant (optional) and the if.
        if (!(ifPhi.block() == block
              && (block.next() == curIf || (block.next() == ifCompare && ifCompare.next() == curIf))
              && (curIf.condition() == Condition.EQ || curIf.condition() == Condition.NE)
              && ifPhi.inputAt(0) instanceof Constant && ifPhi.inputAt(1) instanceof Constant
             )) {
            return;
        }

        // check that there is only one phi function in this block, and that if can be statically decided
        boolean onlyPhi = block.stateBefore().forEachPhi(block, new PhiProcedure() {
            public boolean doPhi(Phi phi) {
                return phi == ifPhi;
            }
        });
        int compare = ifCompare.value.asInt();
        int left = ((Constant) ifPhi.inputAt(0)).value.asInt();
        int right = ((Constant) ifPhi.inputAt(1)).value.asInt();

        if (!(onlyPhi && left != right && (left == compare || right == compare))) {
            return;
        }

        UsageChecker usageChecker = new UsageChecker();
        if (usageChecker.findUsage(ir, ifPhi, block, curIf)) {
            return;
        }

        BlockBegin leftSux;
        BlockBegin rightSux;
        if ((curIf.condition() == Condition.EQ && left == compare) || (curIf.condition() == Condition.NE && right == compare)) {
            leftSux = block.suxAt(0);
            rightSux = block.suxAt(1);
        } else {
            leftSux = block.suxAt(1);
            rightSux = block.suxAt(0);
        }
        if (leftSux == rightSux) {
            // corner case where the block substitution below would fail.
            return;
        }

        if (C1XOptions.PrintHIR) {
            TTY.println("Eliminating Block B" + block.blockID + ", phi " + ifPhi.id() + ", connecting blocks B" + leftPred.blockID + "->" + leftSux.blockID + " and " + rightPred.blockID + "->" + rightSux.blockID);
        }

        // connect leftPred with leftSux, and rightPred with rightSux. This eliminates the curIf and the ifPhi
        Util.replaceAllInList(block, leftSux, leftPred.end().successors());
        Util.replaceAllInList(block, rightSux, rightPred.end().successors());
        Util.replaceAllInList(block, leftPred, leftSux.predecessors());
        Util.replaceAllInList(block, rightPred, rightSux.predecessors());

        leftPred.end().setStateAfter(block.end().stateAfter().copy());
        rightPred.end().setStateAfter(block.end().stateAfter().copy());

        eliminated = true;
    }

    private final class UsageChecker implements BlockClosure, ValueClosure {
        public Value search;
        public BlockBegin ignoreBlock;
        public Value ignore;
        public boolean found;

        private BlockBegin curBlock;
        private Value curValue;

        public boolean findUsage(IR ir, Value search, BlockBegin ignoreBlock, Value ignore) {
            this.search = search;
            this.ignoreBlock = ignoreBlock;
            this.ignore = ignore;
            ir.startBlock.iterateAnyOrder(this, false);
            return found;
        }

        public void apply(BlockBegin block) {
            if (block != ignoreBlock) {
                curBlock = block;
                curValue = null;

                block.stateBefore().forEachPhi(block, new PhiProcedure() {
                    public boolean doPhi(Phi phi) {
                        for (int i = 0; i < phi.inputCount(); i++) {
                            if (phi.inputAt(i) == search) {
                                found = true;
                            }
                        }
                        return true;
                    }
                });

                if (block.exceptionHandlerStates() != null) {
                    for (FrameState s : block.exceptionHandlerStates()) {
                        s.valuesDo(this);
                    }
                }
                for (Instruction n = block; n != null; n = n.next()) {
                    curValue = n;
                    if (n != ignore) {
                        n.allValuesDo(this);
                    }
                }
            }
        }

        public Value apply(Value v) {
            if (v == search) {
                found = true;
            }
            return v;
        }
    }
}
