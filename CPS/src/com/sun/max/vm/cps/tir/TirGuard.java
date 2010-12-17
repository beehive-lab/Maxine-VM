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

package com.sun.max.vm.cps.tir;

import com.sun.max.program.*;
import com.sun.max.vm.bytecode.*;
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
