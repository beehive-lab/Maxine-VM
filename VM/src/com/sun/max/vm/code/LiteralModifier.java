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
package com.sun.max.vm.code;

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.vm.value.*;

/**
 * A literal modifier is a helper for modifying target code.
 * The modifier is specific to an annotated target code and can be used to
 * modify copies of that target code to adapt the code to a use a different literal.
 * A literal is a constant value that cannot fit in an immediate operand of an instruction.
 * Instead, the constant value is loaded in the register from a specific memory location.
 * 
 * The LiteralModifier is used to modify the displacement relative to the base register used as
 * the base for literals.
 * 
 * Currently used by the template-based JIT compiler.
 *
 * @author Laurent Daynes
 */
public class LiteralModifier extends ConstantModifier {

    public LiteralModifier(int position, int size, Value value) {
        super(position, size, value);
    }

    /**
     * Replace the bytes encoding the offset to the literal in an  instruction whose source
     * operands is a literal.
     *
     * @param code bytes buffer containing a copy of the original code annotated with this displacement modifier
     * @param disp32 offset to the literal replacing the original one
     */
    public void fix(byte[] codeRegion,  int disp32) throws AssemblyException {
        fix(codeRegion, 0, disp32);
    }

    /**
     * Replace the bytes encoding the offset to the literal in an  instruction whose source
     * operands is a literal. The instruction to modify is within a buffer containing a copy of the
     * original code annotated with this displacement modifier
     * 
     * @param code bytes buffer containing a copy of the original code annotated with this displacement modifier
     * @param offsetToCode offset to the first byte of a copy of the original target code annotated with this displacement modifier
     * @param disp32 offset to the literal replacing the original one
     */
    public void fix(byte[] codeRegion, int offsetToCode, int disp32) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        editor.fixDisplacement(WordWidth.BITS_32, false, disp32);
    }

    public int getOffsetToLiteral(byte[] codeRegion) throws AssemblyException {
        return getOffsetToLiteral(codeRegion, 0);
    }

    public int getOffsetToLiteral(byte[] codeRegion, int offsetToCode) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        return editor.getIntDisplacement(WordWidth.BITS_32);
    }
}
