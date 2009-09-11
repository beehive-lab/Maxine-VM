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
package com.sun.max.asm.dis.x86;

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * Info about the first few bytes of an x86 instruction,
 * narrowing the set of possible instructions to probe by the disassembler.
 *
 * @author Bernd Mathiske
 */
public class X86InstructionHeader {

    protected boolean hasAddressSizePrefix;
    protected HexByte rexPrefix;
    protected HexByte instructionSelectionPrefix;
    protected HexByte opcode1;
    protected HexByte opcode2;

    X86InstructionHeader() {
    }

    private X86InstructionHeader(WordWidth addressWidth, X86Template template) {
        hasAddressSizePrefix = template.addressSizeAttribute() != addressWidth;
        instructionSelectionPrefix = template.instructionSelectionPrefix();
        opcode1 = template.opcode1();
        opcode2 = template.opcode2();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof X86InstructionHeader) {
            final X86InstructionHeader header = (X86InstructionHeader) other;
            return hasAddressSizePrefix == header.hasAddressSizePrefix &&
                instructionSelectionPrefix == header.instructionSelectionPrefix && opcode1 == header.opcode1 && opcode2 == header.opcode2;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Instruction header: rexPrefix=%s, instructionSelectionPrefix=%s, opcode1=%s, opcode2=%s, hasAddressSizePrefix=%b", rexPrefix, instructionSelectionPrefix, opcode1, opcode2, hasAddressSizePrefix);
    }

    @Override
    public int hashCode() {
        int result = hasAddressSizePrefix ? -1 : 1;
        if (instructionSelectionPrefix != null) {
            result *= instructionSelectionPrefix.ordinal();
        }
        if (opcode1 != null) {
            result *= opcode1.ordinal();
        }
        if (opcode2 != null) {
            result ^= opcode2.ordinal();
        }
        if (instructionSelectionPrefix != null) {
            result += instructionSelectionPrefix.ordinal() * 1024;
        }
        if (opcode2 != null) {
            result += opcode2.ordinal() * 256;
        }
        if (opcode1 != null) {
            result += opcode1.ordinal();
        }
        return result;
    }

    public static Map<X86InstructionHeader, AppendableSequence<X86Template>> createMapping(Assembly<? extends X86Template> assembly, WordWidth addressWidth) {
        final Map<X86InstructionHeader, AppendableSequence<X86Template>> result = new HashMap<X86InstructionHeader, AppendableSequence<X86Template>>();
        for (X86Template template : assembly.templates()) {
            X86InstructionHeader header = new X86InstructionHeader(addressWidth, template);
            AppendableSequence<X86Template> matchingTemplates = result.get(header);
            if (matchingTemplates == null) {
                matchingTemplates = new LinkSequence<X86Template>();
                result.put(header, matchingTemplates);
            }
            matchingTemplates.append(template);
            for (X86Parameter parameter : template.parameters()) {
                switch (parameter.place()) {
                    case OPCODE1_REXB:
                    case OPCODE1:
                        for (int i = 0; i < 8; i++) {
                            header = new X86InstructionHeader(addressWidth, template);
                            header.opcode1 = HexByte.VALUES.get(header.opcode1.ordinal() + i);
                            result.put(header, matchingTemplates);
                        }
                        break;
                    case OPCODE2_REXB:
                    case OPCODE2:
                        for (int i = 0; i < 8; i++) {
                            header = new X86InstructionHeader(addressWidth, template);
                            header.opcode2 = HexByte.VALUES.get(header.opcode2.ordinal() + i);
                            result.put(header, matchingTemplates);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return result;
    }

}
