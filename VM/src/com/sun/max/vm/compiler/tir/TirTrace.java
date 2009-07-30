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
package com.sun.max.vm.compiler.tir;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.state.*;


public class TirTrace {
    private TirTree tree;
    private TraceAnchor anchor;
    private AppendableIndexedSequence<TirInstruction> instructions = new ArrayListSequence<TirInstruction>();
    private TirState tailState;

    public TirTrace(TirTree tree, TraceAnchor anchor) {
        this.tree = tree;
        this.anchor = anchor;
    }

    public Sequence<TirInstruction> instructions() {
        return instructions;
    }

    public void append(Sequence<TirInstruction> instructionList) {
        for (TirInstruction instruction : instructionList) {
            append(instruction);
        }
    }

    public void append(TirInstruction instruction) {
        instructions.append(instruction);
    }

    public void send(TirMessageSink sink) {
        sink.receive(new TirMessage.TirTraceBegin(this));
        sendInstructions(sink);
        sink.receive(new TirMessage.TirTraceEnd());
    }

    public void sendInstructions(TirMessageSink sink) {
        for (int i = instructions.length() - 1; i >= 0; i--) {
            sink.receive(instructions.get(i));
        }
    }

    public TirState tailState() {
        return tailState;
    }

    public void complete(TirState tailState) {
        ProgramError.check(tree.entryState().matches(tailState), "Tail state should match entry state!");
        ProgramError.check(Sequence.Static.containsIdentical(tree.traces(), this), "This trace should already be in the tree!");
        this.tailState = tailState;

        // Inspect locals that were modified on this trace.
        tree.entryState().compare(tailState, new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction entry, TirInstruction tail) {
                final TirLocal local = (TirLocal) entry;
                local.complete(tail);
            }
        });

        // Inspect all guard exit states for cases where a local is being written. This local needs to be flagged
        // as read although it may not be used along a trace. This usually happens when a local is modified along
        // a trace but a previous guard instruction exists that may need to write back the previous local value.
        //
        // Alternately we could just flag all uses of locals as read, but this creates unnecessary write-backs.

        for (TirTrace trace : tree.traces()) {
            for (TirInstruction instruction : trace.instructions()) {
                if (instruction instanceof TirGuard) {
                    final TirGuard guard = (TirGuard) instruction;
                    guard.state().compare(tree.entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
                        @Override
                        public void visit(TirInstruction exit, TirInstruction entry) {
                            if (entry instanceof TirLocal) {
                                final TirLocal local = (TirLocal) entry;
                                if (local.flags().isWritten() == false) {
                                    return;
                                }
                            }

                            if (exit instanceof TirLocal) {
                                final TirLocal local = (TirLocal) exit;
                                local.flags().setRead(true);
                            }
                        }
                    });
                }
            }
        }
    }

    public TraceAnchor anchor() {
        return anchor;
    }

    public TirTree tree() {
        return tree;
    }
}
