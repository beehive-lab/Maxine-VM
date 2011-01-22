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

import java.util.*;

import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirMessage.*;

public class TirReverse extends TirInstructionFilter {
    public TirReverse(TirMessageSink receiver) {
        super(receiver);
    }

    private ArrayList<TirMessage> prolog = new ArrayList<TirMessage>();
    private ArrayList<TirMessage> instructions = prolog;
    private ArrayList<Pair<TirTraceBegin, ArrayList<TirMessage>>> traces =
        new ArrayList<Pair<TirTraceBegin, ArrayList<TirMessage>>>();

    private TirTreeBegin treeBegin;
    private TirTraceBegin traceBegin;

    @Override
    public void visit(TirTreeBegin treeBegin) {
        this.treeBegin = treeBegin;
    }

    @Override
    public void visit(TirTraceBegin traceBegin) {
        this.traceBegin = traceBegin;
        instructions = new ArrayList<TirMessage>();
    }

    @Override
    public void visit(TirTraceEnd traceEnd) {
        final Pair<TirTraceBegin, ArrayList<TirMessage>> trace = new Pair<TirTraceBegin, ArrayList<TirMessage>>(traceBegin, instructions);
        traces.add(trace);
        instructions = prolog;
    }

    @Override
    public void visit(TirTreeEnd treeEnd) {
        final TirPipelineOrder order = this.treeBegin.order().reverse();
        final TirTreeBegin treeBegin = new TirTreeBegin(this.treeBegin.tree(), order);
        forward(treeBegin);
        if (order == TirPipelineOrder.FORWARD) {
            while (instructions.isEmpty() == false) {
                forward(prolog.remove(prolog.size()));
            }
        }
        while (traces.isEmpty() == false) {
            final Pair<TirTraceBegin, ArrayList<TirMessage>> trace = traces.remove(traces.size() - 1);
            forward(trace.first());
            while (trace.second().isEmpty() == false) {
                ArrayList<TirMessage> second = trace.second();
                forward(second.remove(second.size() - 1));
            }
            forward(new TirMessage.TirTraceEnd());
        }
        if (order == TirPipelineOrder.REVERSE) {
            while (instructions.isEmpty() == false) {
                forward(prolog.remove(prolog.size() - 1));
            }
        }
        forward(new TirMessage.TirTreeEnd());
    }

    @Override
    public void visit(TirInstruction instruction) {
        instructions.add(instruction);
    }
}
