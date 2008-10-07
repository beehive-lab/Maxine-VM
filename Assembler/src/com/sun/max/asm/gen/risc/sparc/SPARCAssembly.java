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
/*VCSID=4141c816-6402-47a8-b6da-c1d3131bb9bb*/
package com.sun.max.asm.gen.risc.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public final class SPARCAssembly extends RiscAssembly<SPARCTemplate> {

    private static final boolean GENERATING_DEPRECATED_INSTRUCTIONS = true;
    private static final boolean GENERATING_V9_INSTRUCTIONS = true;

    private SPARCAssembly() {
        super(InstructionSet.SPARC, SPARCTemplate.class);
    }

    public boolean generatingDeprecatedInstructions() {
        return GENERATING_DEPRECATED_INSTRUCTIONS;
    }

    public boolean generatingV9Instructions() {
        return GENERATING_V9_INSTRUCTIONS;
    }

    @Override
    public BitRangeOrder bitRangeEndianness() {
        return BitRangeOrder.DESCENDING;
    }

    @Override
    protected Sequence<SPARCTemplate> createTemplates() {
        final SPARCTemplateCreator creator = new SPARCTemplateCreator();
        creator.createTemplates(new MemoryAccess(creator));
        creator.createTemplates(new MemorySynchronization(creator));
        creator.createTemplates(new IntegerArithmetic(creator));
        creator.createTemplates(new ControlTransfer(creator));
        creator.createTemplates(new ConditionalMove(creator));
        creator.createTemplates(new RegisterWindowManagement(creator));
        creator.createTemplates(new StateRegisterAccess(creator));
        creator.createTemplates(new PrivilegedRegisterAccess(creator));
        creator.createTemplates(new FloatingPointOperate(creator));
        creator.createTemplates(new ImplementationDependent(creator));
        creator.createTemplates(new SyntheticInstructions(creator));
        return creator.templates();
    }

    @Override
    protected SPARCTemplate createInlineByteTemplate() {
        final SPARCTemplateCreator creator = new SPARCTemplateCreator();
        return creator.createInlineByteTemplate();
    }

    public static final SPARCAssembly ASSEMBLY = new SPARCAssembly();
}
