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

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirMessage.*;

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
        } else if (Utils.indexOfIdentical(tree().prologue(), instruction) != -1) {
            return true;
        } else {
            return false;
        }
    }
}
