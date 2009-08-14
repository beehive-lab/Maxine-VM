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
 * @author Paul Caprioli
 */
public enum InstructionSet {

    AMD64(Category.CISC, 0, false, 0),
    ARM(Category.RISC, 4, false, 0),
    IA32(Category.CISC, 0, false, 0),
    PPC(Category.RISC, 4, true, 0),
    SPARC(Category.RISC, 4, true, 8);

    public enum Category {
        CISC, RISC;
    }

    public final Category category;

    /** True if PC-relative control transfer instructions contain an offset relative to the start of the instruction.
     *  False if PC-relative control transfer instructions contain an offset relative to the end of the instruction.
     */
    public final boolean relativeBranchFromStart;

    /** Offset to the return address of the caller from the caller's saved PC.
     *  For example, a SPARC call instruction saves its address in %o7. The return address is typically 2 instructions after
     *  (to account for the call instruction itself and the delay slot).
     */
    public final int offsetToReturnPC;

    /** Width of an instruction (in bytes).
     *  If instructions are of variable width, this field is zero.
     */
    public final int instructionWidth;

    private InstructionSet(Category category, int instructionWidth, boolean relativeBranchFromStart, int offsetToReturnPC) {
        this.category = category;
        this.instructionWidth = instructionWidth;
        this.relativeBranchFromStart = relativeBranchFromStart;
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
