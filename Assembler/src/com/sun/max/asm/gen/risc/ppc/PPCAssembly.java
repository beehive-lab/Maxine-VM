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
/*VCSID=a7e10cc2-2086-4554-88f8-c6ffcee187bf*/
package com.sun.max.asm.gen.risc.ppc;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;

/**
 * @author Doug Simon
 */
public final class PPCAssembly extends RiscAssembly<PPCTemplate> {

    private static final boolean GENERATING_DEPRECATED_INSTRUCTIONS = true;
    private static final boolean GENERATING_64BIT_INSTRUCTIONS = true;
    private static final boolean GENERATING_POWER5_INSTRUCTIONS = false;

    private PPCAssembly() {
        super(InstructionSet.PPC, PPCTemplate.class);
    }

    public boolean generatingDeprecatedInstructions() {
        return GENERATING_DEPRECATED_INSTRUCTIONS;
    }

    public boolean generating64BitInstructions() {
        return GENERATING_64BIT_INSTRUCTIONS;
    }

    public boolean generatingPower5Instructions() {
        return GENERATING_POWER5_INSTRUCTIONS;
    }

    /**
     * The existence of this method documents a bug in the Apple version of GNU 'as' where bit 11
     * in an mtcrf instruction is encoded as 1 but specified as 0 in the architecture manual.
     * 
     * This will have to be a non-constant method should another non-broken external assembler
     * be used for testing.
     */
    public boolean isExternalMTCRFEncodingBroken() {
        return true;
    }

    @Override
    public BitRangeOrder bitRangeEndianness() {
        return BitRangeOrder.ASCENDING;
    }

    @Override
    protected Sequence<PPCTemplate> createTemplates() {
        final PPCTemplateCreator creator = new PPCTemplateCreator();
        creator.createTemplates(new RawInstructions(creator));
        creator.createTemplates(new SyntheticInstructions(creator));
        return creator.templates();
    }

    @Override
    protected PPCTemplate createInlineByteTemplate() {
        final PPCTemplateCreator creator = new PPCTemplateCreator();
        return creator.createInlineByteTemplate();
    }

    public static final PPCAssembly ASSEMBLY = new PPCAssembly();
}
