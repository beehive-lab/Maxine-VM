/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.program.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirGuard extends TirInstruction {
    private TraceAnchor anchor;
    private TirTrace trace;
    private final TirState state;
    private final TirInstruction operand0;
    private final TirInstruction operand1;
    private final ValueComparator valueComparator;
    private final Class throwable;

    public TirGuard(TirInstruction operand0, TirInstruction opearnd1, ValueComparator valueComparator, TirState state, TirTrace trace, Class throwable) {
        this.operand0 = operand0;
        this.operand1 = opearnd1;
        this.valueComparator = valueComparator;
        this.state = state;
        this.throwable = throwable;
        this.trace = trace;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void visitOperands(TirInstructionVisitor visitor) {
        operand0.accept(visitor);
        operand1.accept(visitor);
    }

    @Override
    public String toString() {
        return "GUARD " + valueComparator.toString();
    }

    @Override
    public Kind kind() {
        ProgramError.check(operand0.kind() == operand1.kind());
        return operand0.kind();
    }

    public TirState state() {
        return state;
    }

    public TirTrace trace() {
        return trace;
    }

    public TirInstruction operand0() {
        return operand0;
    }

    public TirInstruction operand1() {
        return operand1;
    }

    public Class throwable() {
        return throwable;
    }

    public ValueComparator valueComparator() {
        return valueComparator;
    }

    public TraceAnchor anchor() {
        return anchor;
    }

    public void setAnchor(TraceAnchor anchor) {
        this.anchor = anchor;
    }

    public BytecodeLocation location() {
        return state.last().location();
    }

    @Override
    public boolean isLiveIfUnused() {
        return true;
    }
}
