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
package com.sun.max.asm.gen.cisc.ia32;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.ia32.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;

/**
 * Run this program to generate the IA32RawAssembler and IA32LabelAssembler classes.
 * 
 * @author Bernd Mathiske
 */
public class IA32AssemblerGenerator extends X86AssemblerGenerator<IA32Template> {

    public IA32AssemblerGenerator() {
        super(IA32Assembly.ASSEMBLY, WordWidth.BITS_32);
    }

    public static void main(String[] programArguments) {
        final IA32AssemblerGenerator generator = new IA32AssemblerGenerator();
        generator._options.parseArguments(programArguments);
        generator.generate();
    }

    @Override
    protected void printModVariants(IndentWriter stream, IA32Template template) {
        if (template.modCase() != X86TemplateContext.ModCase.MOD_0 || template.parameters().length() == 0) {
            return;
        }
        switch (template.rmCase()) {
            case NORMAL: {
                switch (template.addressSizeAttribute()) {
                    case BITS_16:
                        printModVariant(stream, template, IA32IndirectRegister16.BP_INDIRECT);
                        break;
                    default:
                        printModVariant(stream, template, IA32IndirectRegister32.EBP_INDIRECT);
                        break;
                }
                break;
            }
            case SIB: {
                switch (template.sibBaseCase()) {
                    case GENERAL_REGISTER:
                        printModVariant(stream, template, IA32BaseRegister32.EBP_BASE);
                        break;
                    default:
                        break;
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void printSibVariants(IndentWriter stream, IA32Template template) {
        if (template.modCase() != null && template.modCase() != X86TemplateContext.ModCase.MOD_3 &&
                                          template.rmCase() == X86TemplateContext.RMCase.NORMAL &&
                                          template.addressSizeAttribute() == WordWidth.BITS_32 &&
                                          template.parameters().length() > 0) {
            printSibVariant(stream, template, IA32IndirectRegister32.ESP_INDIRECT);
        }
    }

    @Override
    protected String generateExampleInstruction(IA32Template template, IndexedSequence<Argument> arguments, IndexedSequence<DisassembledLabel> labels) {
        final byte[] bytes = {};
        final IA32DisassembledInstruction dis = new IA32DisassembledInstruction(0, 0, bytes, template, arguments);
        return dis.toString(labels);
    }
}
