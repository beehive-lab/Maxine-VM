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
/*VCSID=f465272c-3f8f-4369-9213-aac478417eeb*/
package com.sun.max.vm.compiler.tir.pipeline;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.compiler.tir.TirMessage.*;


public class TirReverse extends TirInstructionFilter {
    public TirReverse(TirMessageSink receiver) {
        super(receiver);
    }

    private VariableSequence<TirMessage> _prolog = new ArrayListSequence<TirMessage>();
    private VariableSequence<TirMessage> _instructions = _prolog;
    private VariableSequence<Pair<TirTraceBegin, VariableSequence<TirMessage>>> _traces =
        new ArrayListSequence<Pair<TirTraceBegin, VariableSequence<TirMessage>>>();

    private TirTreeBegin _treeBegin;
    private TirTraceBegin _traceBegin;

    @Override
    public void visit(TirTreeBegin treeBegin) {
        _treeBegin = treeBegin;
    }

    @Override
    public void visit(TirTraceBegin traceBegin) {
        _traceBegin = traceBegin;
        _instructions = new ArrayListSequence<TirMessage>();
    }

    @Override
    public void visit(TirTraceEnd traceEnd) {
        final Pair<TirTraceBegin, VariableSequence<TirMessage>> trace = new Pair<TirTraceBegin, VariableSequence<TirMessage>>(_traceBegin, _instructions);
        _traces.append(trace);
        _instructions = _prolog;
    }

    @Override
    public void visit(TirTreeEnd treeEnd) {
        final TirPipelineOrder order = _treeBegin.order().reverse();
        final TirTreeBegin treeBegin = new TirTreeBegin(_treeBegin.tree(), order);
        forward(treeBegin);
        if (order == TirPipelineOrder.FORWARD) {
            while (_instructions.isEmpty() == false) {
                forward(_prolog.removeLast());
            }
        }
        while (_traces.isEmpty() == false) {
            final Pair<TirTraceBegin, VariableSequence<TirMessage>> trace = _traces.removeLast();
            forward(trace.first());
            while (trace.second().isEmpty() == false) {
                forward(trace.second().removeLast());
            }
            forward(new TirMessage.TirTraceEnd());
        }
        if (order == TirPipelineOrder.REVERSE) {
            while (_instructions.isEmpty() == false) {
                forward(_prolog.removeLast());
            }
        }
        forward(new TirMessage.TirTreeEnd());
    }

    @Override
    public void visit(TirInstruction instruction) {
        _instructions.append(instruction);
    }
}
