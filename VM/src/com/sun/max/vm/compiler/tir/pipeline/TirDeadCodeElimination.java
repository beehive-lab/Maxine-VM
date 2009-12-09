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
package com.sun.max.vm.compiler.tir.pipeline;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.hotpath.state.*;

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

    private VariableDeterministicSet<TirInstruction> live = new LinkedIdentityHashSet<TirInstruction>();

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
