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
package com.sun.max.asm.dis.ia32;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.x86.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.ia32.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * Instantiate this class to disassemble IA32 instruction streams.
 *
 * @author Bernd Mathiske
 */
public class IA32Disassembler extends X86Disassembler {

    public IA32Disassembler(int startAddress, InlineDataDecoder inlineDataDecoder) {
        super(new Immediate32Argument(startAddress), IA32Assembly.ASSEMBLY, inlineDataDecoder);
    }

    @Override
    protected boolean isRexPrefix(HexByte opcode) {
        return false;
    }

    @Override
    protected Assembler createAssembler(int position) {
        return new IA32Assembler((int) startAddress().asLong() + position);
    }

    private static Map<X86InstructionHeader, AppendableSequence<X86Template>> headerToTemplates = X86InstructionHeader.createMapping(IA32Assembly.ASSEMBLY, WordWidth.BITS_32);

    @Override
    protected Map<X86InstructionHeader, AppendableSequence<X86Template>> headerToTemplates() {
        return headerToTemplates;
    }
}
