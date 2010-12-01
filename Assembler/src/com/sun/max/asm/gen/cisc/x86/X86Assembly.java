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
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class X86Assembly<Template_Type extends X86Template> extends Assembly<Template_Type> {

    public X86Assembly(ISA isa, Class<Template_Type> templateType) {
        super(isa, templateType);
    }

    @Override
    public BitRangeOrder bitRangeEndianness() {
        return BitRangeOrder.DESCENDING;
    }

    /**
     * Whether to support 16 bit addressing.
     */
    private static boolean are16BitAddressesSupported;

    public static boolean are16BitAddressesSupported() {
        return are16BitAddressesSupported;
    }

    public static void support16BitAddresses() {
        are16BitAddressesSupported = true;
    }

    /**
     * Whether to support 16 bit addressing.
     */
    private static boolean are16BitOffsetsSupported;

    public static boolean are16BitOffsetsSupported() {
        return are16BitOffsetsSupported;
    }

    public static void support16BitOffsets() {
        are16BitOffsetsSupported = true;
    }

    private static <Template_Type extends X86Template> boolean parametersMatching(Template_Type original, Template_Type candidate, Class argumentType) {
        int i = 0;
        int j = 0;
        while (i < original.parameters().size()) {
            final Class originalType = original.parameters().get(i).type();
            Class candidateType = candidate.parameters().get(j).type();
            if (originalType == argumentType) {
                if (candidateType != byte.class) {
                    return false;
                }
                j++;
                candidateType = candidate.parameters().get(j).type();
            }
            if (originalType != candidateType) {
                return false;
            }
            i++;
            j++;
        }
        return true;
    }

    public static <Template_Type extends X86Template> Template_Type getModVariantTemplate(Iterable<Template_Type> templates, Template_Type original, Class argumentType) {
        for (Template_Type candidate : templates) {
            if (candidate.opcode1() == original.opcode1() && candidate.opcode2() == original.opcode2() &&
                    candidate.instructionSelectionPrefix() == original.instructionSelectionPrefix() &&
                    candidate.modRMGroupOpcode() == original.modRMGroupOpcode() &&
                    candidate.addressSizeAttribute() == original.addressSizeAttribute() &&
                    candidate.operandSizeAttribute() == original.operandSizeAttribute() &&
                    parametersMatching(original, candidate, argumentType)) {
                return candidate;
            }
        }
        throw ProgramError.unexpected("could not find mod variant for: " + original);
    }

}
