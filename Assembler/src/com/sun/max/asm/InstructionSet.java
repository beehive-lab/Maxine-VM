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
package com.sun.max.asm;

/**
 * Instruction Set Architecture monikers.
 *
 * @author Bernd Mathiske
 */
public enum InstructionSet {

    AMD64(Category.CISC, RelativeAddressing.FROM_INSTRUCTION_END, 0),
    ARM(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_END, 0),
    IA32(Category.CISC, RelativeAddressing.FROM_INSTRUCTION_END, 0),
    PPC(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_START, 0),
    SPARC(Category.RISC, RelativeAddressing.FROM_INSTRUCTION_START, 8);

    public enum Category {
        CISC, RISC;
    }

    private final Category category;

    public Category category() {
        return category;
    }

    public enum RelativeAddressing {
        FROM_INSTRUCTION_START, FROM_INSTRUCTION_END;
    }

    private final RelativeAddressing relativeAddressing;

    public RelativeAddressing relativeAddressing() {
        return relativeAddressing;
    }

    private final int offsetToReturnPC;

    /**
     * @return offset to the return address of the caller from the caller's saved PC.
     */
    public int offsetToReturnPC() {
        return offsetToReturnPC;
    }

    private InstructionSet(Category category, RelativeAddressing relativeAddressing, int offsetToReturnPC) {
        this.category = category;
        this.relativeAddressing = relativeAddressing;
        this.offsetToReturnPC = offsetToReturnPC;
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Determines if call instructions in this instruction set push the return address on the stack.
     */
    public boolean callsPushReturnAddressOnStack() {
        return category == Category.CISC;
    }

}
