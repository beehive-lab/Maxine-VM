/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

