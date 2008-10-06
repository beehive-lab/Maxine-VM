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
/*VCSID=7e9bf7a5-d1aa-49f3-9b81-1eb16940caf9*/

package com.sun.max.vm.compiler.tir;

import com.sun.max.program.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirGuard extends TirInstruction {
    private TraceAnchor _anchor;
    private TirTrace _trace;
    private final TirState _state;
    private final TirInstruction _operand0;
    private final TirInstruction _operand1;
    private final ValueComparator _valueComparator;
    private final Class<? extends Throwable> _throwable;

    public TirGuard(TirInstruction operand0, TirInstruction opearnd1, ValueComparator valueComparator, TirState state, TirTrace trace, Class<? extends Throwable> throwable) {
        _operand0 = operand0;
        _operand1 = opearnd1;
        _valueComparator = valueComparator;
        _state = state;
        _throwable = throwable;
        _trace = trace;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void visitOperands(TirInstructionVisitor visitor) {
        _operand0.accept(visitor);
        _operand1.accept(visitor);
    }

    @Override
    public String toString() {
        return "GUARD " + _valueComparator.toString();
    }

    @Override
    public Kind kind() {
        ProgramError.check(_operand0.kind() == _operand1.kind());
        return _operand0.kind();
    }

    public TirState state() {
        return _state;
    }

    public TirTrace trace() {
        return _trace;
    }

    public TirInstruction operand0() {
        return _operand0;
    }

    public TirInstruction operand1() {
        return _operand1;
    }

    public Class<? extends Throwable> thorwable() {
        return _throwable;
    }

    public ValueComparator valueComparator() {
        return _valueComparator;
    }

    public TraceAnchor anchor() {
        return _anchor;
    }

    public void setAnchor(TraceAnchor anchor) {
        _anchor = anchor;
    }

    public BytecodeLocation location() {
        return _state.last().location();
    }

    @Override
    public boolean isLiveIfUnused() {
        return true;
    }
}
