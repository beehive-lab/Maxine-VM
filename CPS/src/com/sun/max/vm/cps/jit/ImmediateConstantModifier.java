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
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size);
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
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size);
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
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size);
        editor.fixImmediateOperand(longValue);
    }

    public int getImmediateConstant(byte[] codeRegion) throws AssemblyException {
        return getImmediateConstant(codeRegion, 0);
    }

    public int getImmediateConstant(byte[] codeRegion, int offsetToCode) throws AssemblyException {
        final AssemblyInstructionEditor editor =  createAssemblyInstructionEditor(codeRegion, offsetToCode + startPosition(), size);
        return editor.getIntImmediate(kind().width);
    }
}
