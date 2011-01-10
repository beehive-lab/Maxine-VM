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
import com.sun.max.lang.*;

/**
 * A displacement modifier is a helper for modifying target code.
 * The modifier is specific to an annotated target code and can be used to
 * modify copies of that target code to adapt the code to a different value of a displacement in a memory-addressing
 *  instruction.
 * Currently used by the template-based JIT.
 *
 * @author Laurent Daynes
 */
public class DisplacementModifier extends InstructionModifier {
    /**
     * Width of the displacement.
     */
    private final WordWidth displacementWidth;

    /**
     * Value of the displacement.
     */
    private final int displacementValue;

    /**
     * Whether and index register is being used. Mostly used to ease validation of the instruction (see AssemblyInstructionEditor).
     * TODO: this information is currently specific to x86, so it needs to be moved to an x86 specific subclass.
     */
    private final  boolean useIndexRegister;

    public DisplacementModifier(int position, int length, WordWidth displacementWidth, int displacementValue, boolean useIndexRegister) {
        super(position, length);
        this.displacementWidth = displacementWidth;
        this.displacementValue = displacementValue;
        this.useIndexRegister = useIndexRegister;
    }

    public WordWidth displacementWidth() {
        return displacementWidth;
    }

    public int displacementValue() {
        return displacementValue;
    }

    public boolean useIndexRegister() {
        return useIndexRegister;
    }

    /**
     * @param code copy of the original target method's code annotated with this displacement modifier
     * @param newDisplacement new displacement to replace the original one
     */
    public void fix(byte[] code, byte disp8) throws AssemblyException {
        fix(code, 0, disp8);
    }

    /**
     * @param code copy of the original code annotated with this displacement modifier
     * @param newDisplacement new displacement to replace the original one
     */
    public void fix(byte[] code, int disp32) throws AssemblyException {
        fix(code, 0, disp32);
    }

    /**
     * @param code byte buffer containing a copy of the original code annotated with this displacement modifier
     * @param position position of the original code annotated with this displacement modifier
     * @param newDisplacement new displacement to replace the original one
     */
    public void fix(byte[] code, int position, byte disp8) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(code, position + startPosition(), size);
        editor.fixDisplacement(displacementWidth, useIndexRegister, disp8);
    }

    /**
     * @param codeRegion byte buffer containing a copy of the original code annotated with this displacement modifier
     * @param offsetToCode offset to the first byte of a copy of the original target code annotated with this displacement modifier
     * @param newDisplacement new displacement to replace the original one
     */
    public void fix(byte[] codeRegion, int offsetToCode, int disp32) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size);
        if (displacementWidth == WordWidth.BITS_8) {
            if (disp32 <= Byte.MAX_VALUE && disp32 >= Byte.MIN_VALUE) {
                editor.fixDisplacement(displacementWidth, useIndexRegister,  (byte) disp32);
                return;
            }
        }
        editor.fixDisplacement(displacementWidth, useIndexRegister, disp32);
    }

}
