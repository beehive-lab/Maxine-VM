/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.tir.pipeline;

import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.tir.*;

/**
 * Implements a dead code elimination pipeline filter. It maintains liveness information as it visits instructions
 * in reverse tree order.
 *
 * @author Michael Bebenita
 */
public class TirDeadCodeElimination extends TirPipelineFilter {
    public TirDeadCodeElimination(TirMessageSink receiver) {
        super(TirPipelineOrder.REVERSE, receiver);
    }

    private LinkedIdentityHashSet<TirInstruction> live = new LinkedIdentityHashSet<TirInstruction>();

    /**
     * Ignore dead instructions.
     */
    @Override
    protected boolean filter(TirInstruction instruction) {
        return isLive(instruction);
    }

    private TirInstructionVisitor operandVisitor = new TirInstructionAdapter() {
        @Override
        public void visit(TirInstruction operand) {
            live.add(operand);
        }
    };

    /**
     * Adds the instruction's operands and removes itself from the live set.
     */
    @Override
    public void visit(TirInstruction instruction) {
        instruction.visitOperands(operandVisitor);
        live.remove(instruction);
        forward(instruction);
    }

    private boolean isLive(TirInstruction instruction) {
        return instruction.isLiveIfUnused() || live.contains(instruction);
    }

    @Override
    protected void beginTrace() {
        tree().entryState().compare(trace().tailState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction entry, TirInstruction tail) {
                final TirLocal local = (TirLocal) entry;
                if (local.flags().isRead() && local != tail) {
                    live.add(tail);
                }
            }
        });
    }
}
