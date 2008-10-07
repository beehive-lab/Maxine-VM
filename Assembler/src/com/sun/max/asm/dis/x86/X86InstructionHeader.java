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
/*VCSID=068c4659-7486-49e8-9943-b208327b1361*/
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

    protected boolean _hasAddressSizePrefix;
    protected HexByte _rexPrefix;
    protected HexByte _instructionSelectionPrefix;
    protected HexByte _opcode1;
    protected HexByte _opcode2;

    X86InstructionHeader() {
    }

    private X86InstructionHeader(WordWidth addressWidth, X86Template template) {
        _hasAddressSizePrefix = template.addressSizeAttribute() != addressWidth;
        _instructionSelectionPrefix = template.instructionSelectionPrefix();
        _opcode1 = template.opcode1();
        _opcode2 = template.opcode2();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof X86InstructionHeader) {
            final X86InstructionHeader header = (X86InstructionHeader) other;
            return _hasAddressSizePrefix == header._hasAddressSizePrefix &&
                _instructionSelectionPrefix == header._instructionSelectionPrefix && _opcode1 == header._opcode1 && _opcode2 == header._opcode2;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = _hasAddressSizePrefix ? -1 : 1;
        if (_instructionSelectionPrefix != null) {
            result *= _instructionSelectionPrefix.ordinal();
        }
        if (_opcode1 != null) {
            result *= _opcode1.ordinal();
        }
        if (_opcode2 != null) {
            result ^= _opcode2.ordinal();
        }
        if (_instructionSelectionPrefix != null) {
            result += _instructionSelectionPrefix.ordinal() * 1024;
        }
        if (_opcode2 != null) {
            result += _opcode2.ordinal() * 256;
        }
        if (_opcode1 != null) {
            result += _opcode1.ordinal();
        }
        return result;
    }

    public static <Template_Type extends X86Template> Map<X86InstructionHeader, AppendableSequence<Template_Type>> createMapping(Assembly<Template_Type> assembly, WordWidth addressWidth) {
        final Map<X86InstructionHeader, AppendableSequence<Template_Type>> result = new HashMap<X86InstructionHeader, AppendableSequence<Template_Type>>();
        for (Template_Type template : assembly.templates()) {
            X86InstructionHeader header = new X86InstructionHeader(addressWidth, template);
            AppendableSequence<Template_Type> matchingTemplates = result.get(header);
            if (matchingTemplates == null) {
                matchingTemplates = new LinkSequence<Template_Type>();
                result.put(header, matchingTemplates);
            }
            matchingTemplates.append(template);
            for (X86Parameter parameter : template.parameters()) {
                switch (parameter.place()) {
                    case OPCODE1_REXB:
                    case OPCODE1:
                        for (int i = 0; i < 8; i++) {
                            header = new X86InstructionHeader(addressWidth, template);
                            header._opcode1 = HexByte.VALUES.get(header._opcode1.ordinal() + i);
                            result.put(header, matchingTemplates);
                        }
                        break;
                    case OPCODE2_REXB:
                    case OPCODE2:
                        for (int i = 0; i < 8; i++) {
                            header = new X86InstructionHeader(addressWidth, template);
                            header._opcode2 = HexByte.VALUES.get(header._opcode2.ordinal() + i);
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
