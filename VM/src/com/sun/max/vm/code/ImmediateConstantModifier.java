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

public class ImmediateConstantModifier extends ConstantModifier {

    public ImmediateConstantModifier(int position, int size, Value value) {
        super(position, size, value);
    }
    /**
     * @param code copy of the original target method's code annotated with this constant modifier
     * @param byteValue new constant value to replace the original one
     */
    public void fix(byte[] code, byte byteValue) throws AssemblyException {
        fix(code, 0, byteValue);
    }

    public WordWidth constantWidth() throws AssemblyException {
        switch (kind().asEnum) {
            case BYTE:
                return WordWidth.BITS_8;
            case SHORT:
                return WordWidth.BITS_16;
            case INT:
                return WordWidth.BITS_32;
            case LONG:
                return WordWidth.BITS_64;
            default:
                throw new AssemblyException("Can't fix immediate operand of type " + kind() + " with byte value");
        }
    }
    /**
     * @param codeRegion byte buffer containing a copy of the original code annotated with this displacement modifier
     * @param offsetToCode offset to the first byte of a copy of the original target code annotated with this displacement modifier
     * @param byteValue new constant value to replace the original one
     */
    public void fix(byte[] codeRegion, int offsetToCode, byte byteValue) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        editor.fixImmediateOperand(constantWidth(), byteValue);
    }

    /**
     * @param code copy of the original target method's code annotated with this constant modifier
     * @param byteValue new constant value to replace the original one
     */
    public void fix(byte[] code, int intValue) throws AssemblyException {
        fix(code, 0, intValue);
    }

    /**
     * @param codeRegion byte buffer containing a copy of the original code annotated with this displacement modifier
     * @param offsetToCode offset to the first byte of a copy of the original target code annotated with this displacement modifier
     * @param intValue new constant value to replace the original one
     */
    public void fix(byte[] codeRegion, int offsetToCode, int intValue) throws AssemblyException {
        if (WordWidth.signedEffective(intValue).greaterThan(constantWidth())) {
            throw new AssemblyException("Can't fix immediate operand of effective width " + constantWidth() + " with value " + intValue);
        }
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        editor.fixImmediateOperand(constantWidth(), intValue);
    }

    /**
     * @param code copy of the original target method's code annotated with this constant modifier
     * @param longValue new constant value to replace the original one
     */
    public void fix(byte[] code, long longValue) throws AssemblyException {
        fix(code, 0, longValue);
    }
    /**
     * @param codeRegion byte buffer containing a copy of the original code annotated with this displacement modifier
     * @param offsetToCode offset to the first byte of a copy of the original target code annotated with this displacement modifier
     * @param byteValue new constant value to replace the original one
     */
    public void fix(byte[] codeRegion, int offsetToCode, long longValue) throws AssemblyException {
        if (WordWidth.signedEffective(longValue).greaterThan(constantWidth())) {
            throw new AssemblyException("Can't fix immediate operand of effective width " + constantWidth() + " with value " + longValue);
        }
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        editor.fixImmediateOperand(longValue);
    }

    public int getImmediateConstant(byte[] codeRegion) throws AssemblyException {
        return getImmediateConstant(codeRegion, 0);
    }

    public int getImmediateConstant(byte[] codeRegion, int offsetToCode) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size());
        return editor.getIntImmediate(kind().width);
    }
}
