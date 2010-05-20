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
package com.sun.c1x.lir;

import com.sun.c1x.asm.*;
import com.sun.cri.ci.*;

/**
 * The {@code LIRLabel} class definition.
 *
 * @author Marcelo Cintra
 */
public class LIRLabel extends LIROp0 {

    private Label label;

    /**
     * Constructs a LIRLabel instruction.
     * @param label the label
     */
    public LIRLabel(Label label) {
        super(LIROpcode.Label, CiValue.IllegalValue, null);
        assert label != null;
        this.label = label;
    }

    /**
     * Gets the label associated to this instruction.
     * @return the label
     */
    public Label label() {
        return label;
    }

    /**
     * Emits target assembly code for this LIRLabel instruction.
     * @param masm the LIRAssembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitOpLabel(this);
    }

    /**
     * Prints this instruction to a LogStream.
     */
    @Override
    public String operationString(OperandFormatter operandFmt) {
        return label.isBound() ? String.valueOf(label.position()) : "?";
    }
}
