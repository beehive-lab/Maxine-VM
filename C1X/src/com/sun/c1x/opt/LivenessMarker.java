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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * The <code>LivenessMarker</code> class walks over an IR graph and marks instructions
 * whose values are live, either because they are needed to compute the method's result,
 * may produce a side-effect, or are needed for deoptimization.
 *
 * @author Ben L. Titzer
 */
public class LivenessMarker {

    final IR ir;

    final Queue<Value> valueQueue = new LinkedList<Value>();
    final Queue<Value> deoptQueue = new LinkedList<Value>();

    final InstrMarker deoptMarker = new InstrMarker(Value.Flag.LiveDeopt, deoptQueue);
    final InstrMarker valueMarker = new InstrMarker(Value.Flag.LiveValue, valueQueue);

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

                Value x;
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

    private void markInputs(Value i, ValueClosure marker) {
        if (!i.isDeadPhi()) {
            i.inputValuesDo(marker);
            if (i instanceof Phi) {
                // phis are special
                Phi phi = (Phi) i;
                int max = phi.operandCount();
                for (int j = 0; j < max; j++) {
                    Value x = phi.operandAt(j);
                    marker.apply(x);
                }
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

    private class InstrMarker implements ValueClosure {
        final Value.Flag reason;
        final Queue<Value> queue;

        public InstrMarker(Value.Flag reason, Queue<Value> queue) {
            this.reason = reason;
            this.queue = queue;
        }

        public Value apply(Value i) {
            markLive(i);
            return i;
        }

        final void markLive(Value i) {
            if (!i.checkFlag(reason) && !i.isDeadPhi()) {
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
            i.setFlag(Value.Flag.LiveSideEffect);
        } else if (i.checkFlag(Value.Flag.LiveStore)) {
            // instruction is a store that cannot be eliminated
            i.inputValuesDo(valueMarker);
            i.setFlag(Value.Flag.LiveSideEffect);
        }
        if (i instanceof BlockEnd) {
            // input values to block ends are control dependencies
            i.inputValuesDo(valueMarker);
            i.setFlag(Value.Flag.LiveControl);
        }
    }
}
