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
