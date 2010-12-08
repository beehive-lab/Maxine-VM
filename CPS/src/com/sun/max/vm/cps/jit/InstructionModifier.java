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
package com.sun.max.vm.cps.jit;

import com.sun.max.asm.*;
import com.sun.max.vm.asm.amd64.*;

/**
 * Instruction modifiers are helpers that abstract modification to target code. Modifiers are customized for a specific
 * modification, e.g., change a displacement in a memory-addressing instruction, changing an immediate value in an
 * instruction setting a register, change a label in a branch instruction, etc.
 * <p>
 * Modifiers are currently used for template-based JIT compilation, where in a template is copied into a
 * code buffer, then the copy is modified to adjust the template.
 *
 * @author Laurent Daynes
 */
public class InstructionModifier {

    /**
     * Position of the instruction.
     */
    public final int startPosition;

    /**
     * Size of the instruction.
     */
    public final int size;

    public InstructionModifier(int position, int size) {
        startPosition = position;
        this.size = size;
    }

    /**
     * Gets the position of the first byte of the instruction that depends on the resolved link.
     */
    public final int startPosition() {
        return startPosition;
    }

    public final int endPosition() {
        return startPosition + size;
    }

    // FIXME: there a platform-dependence here. This need to go via a VM configuration for a factory of instruction editor!
    protected AssemblyInstructionEditor createAssemblyInstructionEditor(byte[] code, int position, int sz) {
        return new AMD64InstructionEditor(code, position, sz);
    }
}

