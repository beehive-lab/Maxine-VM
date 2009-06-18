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
package com.sun.max.asm.gen;

import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * Wraps mere object arrays into instruction descriptions.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class InstructionDescriptionCreator<InstructionDescription_Type extends InstructionDescription> {

    private final Assembly assembly;

    protected InstructionDescriptionCreator(Assembly assembly) {
        this.assembly = assembly;
    }

    public Assembly assembly() {
        return assembly;
    }

    protected abstract InstructionDescription_Type createInstructionDescription(MutableSequence<Object> specifications);

    protected InstructionDescription_Type defineInstructionDescription(MutableSequence<Object> specifications) {
        final InstructionDescription_Type instructionDescription = createInstructionDescription(specifications);
        instructionDescriptions.append(instructionDescription);
        instructionDescription.setArchitectureManualSection(currentArchitectureManualSection);
        return instructionDescription;
    }

    private final AppendableSequence<InstructionDescription_Type> instructionDescriptions = new LinkSequence<InstructionDescription_Type>();

    protected InstructionDescription_Type define(Object... specifications) {
        return defineInstructionDescription(new ArraySequence<Object>(Arrays.flatten(specifications)));
    }

    private String currentArchitectureManualSection;

    /**
     * Sets the name of the architecture manual section for which instruction descriptions are
     * currently being {@link #define defined}.
     */
    public void setCurrentArchitectureManualSection(String section) {
        currentArchitectureManualSection = section;
    }

    public Sequence<InstructionDescription_Type> instructionDescriptions() {
        return instructionDescriptions;
    }
}
