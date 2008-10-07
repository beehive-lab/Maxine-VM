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
/*VCSID=eb623de7-8787-431f-91ef-6537aba7c191*/
package com.sun.max.asm.gen.cisc.ia32;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32Assembly extends X86Assembly<IA32Template> {

    private IA32Assembly() {
        super(InstructionSet.IA32, IA32Template.class);
    }

    @Override
    protected Sequence<IA32Template> createTemplates() {
        final IA32TemplateCreator creator = new IA32TemplateCreator();
        creator.createTemplates(new OneByteOpcodeMap());
        creator.createTemplates(new TwoByteOpcodeMap());
        creator.createTemplates(new FloatingPointOpcodeMap(this));
        return creator.templates();
    }

    @Override
    protected IA32Template createInlineByteTemplate() {
        final IA32TemplateCreator creator = new IA32TemplateCreator();
        return creator.createInlineBytesTemplate();
    }

    public static final IA32Assembly ASSEMBLY = new IA32Assembly();

}
