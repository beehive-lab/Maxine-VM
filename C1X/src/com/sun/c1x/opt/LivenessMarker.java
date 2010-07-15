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

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * The {@code LivenessMarker} class walks over an IR graph and marks instructions
 * whose values are live, either because they are needed to compute the method's result,
 * may produce a side-effect, or are needed for deoptimization.
 *
 * @author Ben L. Titzer
 */
public class LivenessMarker {

    final IR ir;

    final InstructionMarker deoptMarker = new InstructionMarker(Value.Flag.LiveDeopt);
    final InstructionMarker valueMarker = new InstructionMarker(Value.Flag.LiveValue);

    int count;

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
            }
        });

        // propagate liveness flags to inputs of instructions
        valueMarker.markAll();
        deoptMarker.markAll();
    }

    public int liveCount() {
        return count;
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
                    } else {
                        C1XMetrics.DeadCodeEliminated++;
                    }
                    prev = i;
                }
            }
        });
        // clear all marks on all instructions
        valueMarker.clearAll();
        deoptMarker.clearAll();
    }

    private static class Link {
        final Value value;
        Link next;

        Link(Value v) {
            this.value = v;
        }
    }

    private class InstructionMarker implements ValueClosure {
        final Value.Flag reason;
        Link head;
        Link tail;

        public InstructionMarker(Value.Flag reason) {
            this.reason = reason;
        }

        public Value apply(Value i) {
            if (!i.checkFlag(reason) && !i.isDeadPhi()) {
                // set the flag and add to the queue
                setFlag(i, reason);
                if (head == null) {
                    head = tail = new Link(i);
                } else {
                    tail.next = new Link(i);
                    tail = tail.next;
                }
            }
            return i;
        }

        private void markAll() {
            Link cursor = head;
            while (cursor != null) {
                markInputs(cursor.value);
                cursor = cursor.next;
            }
        }

        private void clearAll() {
            Link cursor = head;
            while (cursor != null) {
                cursor.value.clearLive();
                cursor = cursor.next;
            }
        }

        private void markInputs(Value i) {
            if (!i.isDeadPhi()) {
                i.inputValuesDo(this);
                if (i instanceof Phi) {
                    // phis are special
                    Phi phi = (Phi) i;
                    int max = phi.inputCount();
                    for (int j = 0; j < max; j++) {
                        apply(phi.inputAt(j));
                    }
                }
            }
        }
    }

    private void markRootInstr(Instruction i) {
        NewFrameState stateBefore = i.stateBefore();
        if (stateBefore != null) {
            // stateBefore != null implies that this instruction may have side effects
            stateBefore.valuesDo(deoptMarker);
            i.inputValuesDo(valueMarker);
            setFlag(i, Value.Flag.LiveSideEffect);
        } else if (i.checkFlag(Value.Flag.LiveStore)) {
            // instruction is a store that cannot be eliminated
            i.inputValuesDo(valueMarker);
            setFlag(i, Value.Flag.LiveSideEffect);
        }
        if (i instanceof BlockEnd) {
            // input values to block ends are control dependencies
            i.inputValuesDo(valueMarker);
            setFlag(i, Value.Flag.LiveControl);
        }
    }

    void setFlag(Value i, Value.Flag flag) {
        if (!i.isLive()) {
            count++;
        }
        i.setFlag(flag);
    }
}
