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
import com.sun.max.program.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.compiler.tir.TirMessage.*;


public class TirPipelineFilter extends TirInstructionFilter {
    private TirTree tree;
    private TirTrace trace;
    private TirPipelineOrder order;

    public TirTree tree() {
        return tree;
    }

    public TirTrace trace() {
        return trace;
    }

    public TirPipelineFilter(TirPipelineOrder order, TirMessageSink receiver) {
        super(receiver);
        this.order = order;
    }

    @Override
    public final void visit(TirTreeBegin treeBegin) {
        ProgramError.check(treeBegin.order() == order);
        tree = treeBegin.tree();
        beginTree();
        super.visit(treeBegin);
    }

    @Override
    public final void visit(TirTraceBegin traceBegin) {
        trace = traceBegin.trace();
        beginTrace();
        super.visit(traceBegin);
    }

    @Override
    public final void visit(TirTraceEnd traceEnd) {
        endTrace();
        trace = null;
        super.visit(traceEnd);
    }

    @Override
    public final void visit(TirTreeEnd treeEnd) {
        endTree();
        tree = null;
        super.visit(treeEnd);
    }

    protected void beginTrace() {

    }

    protected void beginTree() {

    }

    protected void endTree() {

    }

    protected void endTrace() {

    }

    protected boolean isInvariant(TirInstruction instruction) {
        if (instruction instanceof TirLocal) {
            return true;
        } else if (Sequence.Static.containsIdentical(tree().prologue(), instruction)) {
            return true;
        } else {
            return false;
        }
    }
}
