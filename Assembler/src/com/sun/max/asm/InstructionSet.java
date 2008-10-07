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
/*VCSID=4d596222-ac0c-425e-b10e-3dc044d57095*/
package com.sun.max.asm;


/**
 * Instruction Set Architecture monikers.
 * 
 * @author Bernd Mathiske
 */
public enum InstructionSet {

    AMD64(Category.CISC, RelativeAddressing.FROM_INSTRUCTION_END),
    ARM(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_END),
    IA32(Category.CISC, RelativeAddressing.FROM_INSTRUCTION_END),
    PPC(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_START),
    SPARC(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_START);

    public enum Category {
        CISC, RISC;
    }

    private final Category _category;

    public Category category() {
        return _category;
    }

    public enum RelativeAddressing {
        FROM_INSTRUCTION_START, FROM_INSTRUCTION_END;
    }

    private final RelativeAddressing _relativeAddressing;

    public RelativeAddressing relativeAddressing() {
        return _relativeAddressing;
    }

    private InstructionSet(Category category, RelativeAddressing relativeAddressing) {
        _category = category;
        _relativeAddressing = relativeAddressing;
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Determines if call instructions in this instruction set push the return address on the stack.
     */
    public boolean callsPushReturnAddressOnStack() {
        return _category == Category.CISC;
    }
}
