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

import static com.sun.max.asm.gen.cisc.x86.OperandCode.*;
import static com.sun.max.util.HexByte.*;

import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public class AMD64TemplateCreator extends X86TemplateCreator<AMD64Template> {

    public AMD64TemplateCreator() {
        super(AMD64Assembly.ASSEMBLY, WordWidth.BITS_64);
    }

    @Override
    protected AMD64Template createTemplate(X86InstructionDescription instructionDescription, int serial, InstructionAssessment instructionFamily, X86TemplateContext context) {
        return new AMD64Template(instructionDescription, serial, instructionFamily, context);
    }

    public AMD64Template createInlineBytesTemplate() {
        final Object[] specification = {_00, ".BYTE", Ib};
        final AMD64Template template = new AMD64Template(new X86InstructionDescription(new ArraySequence<Object>(specification)), 0, null, null);
        X86InstructionDescriptionVisitor.Static.visitInstructionDescription(template, template.instructionDescription());
        return template;
    }

    public AMD64Template createSwitchLabelTemplate() {
        final Object[] specification = {_00, ".SWITCH_CASE", Iw, Jv};
        final X86TemplateContext context = new X86TemplateContext();
        context.setOperandSizeAttribute(WordWidth.BITS_32);
        final AMD64Template template = new AMD64Template(new X86InstructionDescription(new ArraySequence<Object>(specification)), 0, null, context);
        X86InstructionDescriptionVisitor.Static.visitInstructionDescription(template, template.instructionDescription().setDefaultOperandSize(WordWidth.BITS_32));
        return template;
    }
}
