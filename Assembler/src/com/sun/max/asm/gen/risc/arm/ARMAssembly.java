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
/*VCSID=b9d1845a-173c-479a-a474-af24fab319c3*/

package com.sun.max.asm.gen.risc.arm;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;


/**
 *
 * @author Sumeet Panchal
 */

public final class ARMAssembly extends RiscAssembly<ARMTemplate> {

    public static final ARMAssembly ASSEMBLY = new ARMAssembly();

    private ARMAssembly() {
        super(InstructionSet.ARM, ARMTemplate.class);
    }

    @Override
    public BitRangeOrder bitRangeEndianness() {
        return BitRangeOrder.DESCENDING;
    }

    @Override
    protected Sequence<ARMTemplate> createTemplates() {
        final ARMTemplateCreator creator = new ARMTemplateCreator();
        creator.createTemplates(new RawInstructions(creator));
        return creator.templates();
    }

    @Override
    protected ARMTemplate createInlineByteTemplate() {
        final ARMTemplateCreator creator = new ARMTemplateCreator();
        return creator.createInlineByteTemplate();
    }
}
