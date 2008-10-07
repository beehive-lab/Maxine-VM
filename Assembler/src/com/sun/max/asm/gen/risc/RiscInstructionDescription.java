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
/*VCSID=ebbd66fb-3dfb-474b-88e0-f020954a5147*/
package com.sun.max.asm.gen.risc;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * 
 *
 * @author Bernd Mathiske
 */
public final class RiscInstructionDescription extends InstructionDescription {

    RiscInstructionDescription(MutableSequence<Object> specifications) {
        super(specifications);

        int bits = 0;
        int mask = 0;
        for (Object specification : specifications) {
            final RiscField field;
            if (specification instanceof RiscField) {
                field = (RiscField) specification;
                if (field instanceof InputOperandField) {
                    // Cannot recover the value of these fields from an assembled instruction
                    // with support for a simultaneous equation solver
                    beNotDisassemblable();
                }
            } else if (specification instanceof RiscConstant) {
                field = ((RiscConstant) specification).field();
            } else {
                continue;
            }
            bits += field.bitRange().encodedWidth();
            final int fieldMask = field.bitRange().instructionMask();
            if ((fieldMask & mask) != 0) {
                ProgramError.unexpected("RISC instruction field defines bits also defined by another field: " + field.name() + "[" + field.bitRange() + "]");
            }
            mask |= fieldMask;
        }

        if (bits != 32) {
            ProgramError.unexpected("RISC instruction description describes " + bits + " instruction field bits: " + specifications);
        }
    }

    private boolean _synthetic;

    public InstructionDescription beSynthetic() {
        _synthetic = true;
        return this;
    }

    @Override
    public boolean isSynthetic() {
        return _synthetic;
    }

}
