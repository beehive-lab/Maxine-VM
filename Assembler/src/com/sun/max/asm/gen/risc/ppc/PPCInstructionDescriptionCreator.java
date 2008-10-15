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
package com.sun.max.asm.gen.risc.ppc;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;

/**
 * 
 * 
 * @author Doug Simon
 */
public abstract class PPCInstructionDescriptionCreator extends RiscInstructionDescriptionCreator {

    protected PPCInstructionDescriptionCreator(PPCTemplateCreator templateCreator) {
        super(PPCAssembly.ASSEMBLY, templateCreator);
    }

    @Override
    public PPCAssembly assembly() {
        final Assembly assembly = super.assembly();
        return (PPCAssembly) assembly;
    }

    protected RiscInstructionDescription define64(Object... specifications) {
        return (assembly().generating64BitInstructions()) ? define(specifications) : null;
    }

    protected RiscInstructionDescription defineP5(Object... specifications) {
        return (assembly().generatingPower5Instructions()) ? define(specifications) : null;
    }

    protected RiscInstructionDescriptionModifier synthesize64(String name, String templateName, Object... patterns) {
        return (assembly().generating64BitInstructions()) ? synthesize(name, templateName, patterns) : new RiscInstructionDescriptionModifier(Sequence.Static.empty(RiscInstructionDescription.class));
    }
}
