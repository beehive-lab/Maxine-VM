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
package com.sun.max.vm.cps.tir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.tir.pipeline.*;

public class TirTrace {
    private TirTree tree;
    private TraceAnchor anchor;
    private List<TirInstruction> instructions = new ArrayList<TirInstruction>();
    private TirState tailState;

    public TirTrace(TirTree tree, TraceAnchor anchor) {
        this.tree = tree;
        this.anchor = anchor;
    }

    public List<TirInstruction> instructions() {
        return instructions;
    }

    public void append(List<TirInstruction> instructionList) {
        for (TirInstruction instruction : instructionList) {
            append(instruction);
        }
    }

    public void append(TirInstruction instruction) {
        instructions.add(instruction);
    }

    public void send(TirMessageSink sink) {
        sink.receive(new TirMessage.TirTraceBegin(this));
        sendInstructions(sink);
        sink.receive(new TirMessage.TirTraceEnd());
    }

    public void sendInstructions(TirMessageSink sink) {
        for (int i = instructions.size() - 1; i >= 0; i--) {
            sink.receive(instructions.get(i));
        }
    }

    public TirState tailState() {
        return tailState;
    }

    public void complete(TirState tailState) {
        ProgramError.check(tree.entryState().matches(tailState), "Tail state should match entry state!");
        ProgramError.check(Utils.indexOfIdentical(tree.traces(), this) != -1, "This trace should already be in the tree!");
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
