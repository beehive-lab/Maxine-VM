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
