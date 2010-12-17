/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.c1x.value.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * Records debug info at the current code location.
 *
 * @author Doug Simon
 */
public final class Infopoint extends Instruction {

    public final FrameState state;

    /**
     * {@link Bytecodes#HERE}, {@link Bytecodes#INFO} or {@link Bytecodes#SAFEPOINT}.
     */
    public final int opcode;

    /**
     * Creates a new Infopoint instance.
     * @param state the debug info at this instruction
     */
    public Infopoint(int opcode, FrameState state) {
        super(opcode == HERE ? CiKind.Long : CiKind.Void);
        assert opcode == HERE || opcode == INFO || opcode == SAFEPOINT : Bytecodes.nameOf(opcode);
        this.opcode = opcode;
        this.state = state;
        setFlag(Flag.LiveSideEffect); // ensure this instruction is not eliminated
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitInfopoint(this);
    }

    @Override
    public FrameState stateBefore() {
        return state;
    }
}
