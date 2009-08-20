/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.opt;

import com.sun.c1x.ir.*;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.graph.IR;
import com.sun.c1x.C1XMetrics;

import java.util.Queue;
import java.util.LinkedList;

/**
 * The <code>LivenessMarker</code> class walks over an IR graph and marks instructions
 * whose values are live, either because they are needed to compute the method's result,
 * may produce a side-effect, or are needed for deoptimization.
 *
 * @author Ben L. Titzer
 */
public class LivenessMarker {

    final IR ir;

    final Queue<Instruction> valueQueue = new LinkedList<Instruction>();
    final Queue<Instruction> deoptQueue = new LinkedList<Instruction>();

    final InstrMarker deoptMarker = new InstrMarker(Instruction.Flag.LiveValue, deoptQueue);
    final InstrMarker valueMarker = new InstrMarker(Instruction.Flag.LiveDeopt, valueQueue);

    boolean removeDeadCode;
    boolean clearLiveMarks;

    /**
     * Creates a new liveness marking instance and marks live instructions.
     * @param ir the IR to mark
     */
    public LivenessMarker(IR ir) {
        this.ir = ir;
        markRoots();
    }

    private void markRoots() {
        // first pass: mark root instructions and their inputs
        ir.startBlock.iteratePreOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                Instruction i = block;
                while ((i = i.next()) != null) {
                    // visit all instructions first, marking control dependent and side-effects
                    markRootInstr(i);
                }

                Instruction x;
                // process queue of instructions which are used for their values
                while ((x = valueQueue.poll()) != null) {
                    markInputs(x, valueMarker);
                }
                // process queue of instructions which are used to generate deoptimization code
                while ((x = deoptQueue.poll()) != null) {
                    markInputs(x, deoptMarker);
                }
            }
        });
    }

    private void markInputs(Instruction i, InstructionClosure marker) {
        i.inputValuesDo(marker);
        if (i instanceof Phi) {
            // phis are special
            Phi phi = (Phi) i;
            int max = phi.operandCount();
            for (int j = 0; j < max; j++) {
                Instruction x = phi.operandAt(j);
                marker.apply(x);
            }
        }
    }

    public void removeDeadCode() {
        // second pass: remove dead instructions from blocks
        ir.startBlock.iteratePreOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                Instruction prev = block;
                Instruction i = block;
                while ((i = i.next()) != null) {
                    if (i.isLive()) {
                        prev.resetNext(i); // skip any previous dead instructions
                        i.clearLive();     // clear the live marks for later DCE passes
                    } else {
                        C1XMetrics.DeadCodeEliminated++;
                    }
                    prev = i;
                }
            }
        });
    }

    private class InstrMarker implements InstructionClosure {
        final Instruction.Flag reason;
        final Queue<Instruction> queue;

        public InstrMarker(Instruction.Flag reason, Queue<Instruction> queue) {
            this.reason = reason;
            this.queue = queue;
        }

        public Instruction apply(Instruction i) {
            markLive(i);
            return i;
        }

        final void markLive(Instruction i) {
            if (!i.checkFlag(reason)) {
                i.setFlag(reason);
                queue.offer(i);
            }
        }
    }

    private void markRootInstr(Instruction i) {
        ValueStack stateBefore = i.stateBefore();
        if (stateBefore != null) {
            // state before != null implies that this instruction may have side effects
            stateBefore.valuesDo(deoptMarker);
            i.inputValuesDo(valueMarker);
            i.setFlag(Instruction.Flag.LiveSideEffect);
        }
        if (i instanceof BlockEnd) {
            // input values to block ends are control dependencies
            i.inputValuesDo(valueMarker);
            i.setFlag(Instruction.Flag.LiveControl);
        }
    }
}
