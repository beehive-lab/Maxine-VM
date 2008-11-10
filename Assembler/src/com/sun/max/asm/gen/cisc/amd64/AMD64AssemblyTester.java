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
package com.sun.max.asm.gen.cisc.amd64;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.amd64.complete.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public class AMD64AssemblyTester extends X86AssemblyTester<AMD64Template, AMD64DisassembledInstruction> {

    public AMD64AssemblyTester(EnumSet<AssemblyTestComponent> components) {
        super(AMD64Assembly.ASSEMBLY, WordWidth.BITS_64, components);
    }

    @Override
    protected String assemblerCommand() {
        return System.getProperty("os.name").equals("Mac OS X") ? "as -arch x86_64" : "gas -64";
    }

    @Override
    protected Assembler createTestAssembler() {
        return new AMD64Assembler(0L);
    }

    @Override
    protected AMD64Disassembler createTestDisassembler() {
        return new AMD64Disassembler(0L, null);
    }

    @Override
    protected boolean isLegalArgumentList(AMD64Template template, IndexedSequence<Argument> arguments) {
        final WordWidth externalCodeSizeAttribute = template.externalCodeSizeAttribute();
        for (Argument argument : arguments) {
            if (argument instanceof AMD64GeneralRegister8) {
                final AMD64GeneralRegister8 generalRegister8 = (AMD64GeneralRegister8) argument;
                if (generalRegister8.isHighByte()) {
                    if (template.hasRexPrefix(arguments)) {
                        return false;
                    }
                } else if (generalRegister8.value() >= 4 && externalCodeSizeAttribute != null && externalCodeSizeAttribute.lessThan(WordWidth.BITS_64)) {
                    return false;
                }
            } else if (externalCodeSizeAttribute != null && externalCodeSizeAttribute.lessThan(WordWidth.BITS_64)) {
                // exclude cases that gas does not support (but that otherwise seem plausible):
                if (argument instanceof GeneralRegister) {
                    final GeneralRegister generalRegister = (GeneralRegister) argument;
                    if ((generalRegister.value() >= 8) || (generalRegister.width() == WordWidth.BITS_64)) {
                        return false;
                    }
                } else if (argument instanceof AMD64XMMRegister) {
                    final AMD64XMMRegister xmmRegister = (AMD64XMMRegister) argument;
                    if (xmmRegister.value() >= 8) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
