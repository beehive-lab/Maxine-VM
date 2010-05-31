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

import java.util.*;

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

    protected abstract InstructionDescription_Type createInstructionDescription(List<Object> specifications);

    protected InstructionDescription_Type defineInstructionDescription(List<Object> specifications) {
        final InstructionDescription_Type instructionDescription = createInstructionDescription(specifications);
        instructionDescriptions.add(instructionDescription);
        instructionDescription.setArchitectureManualSection(currentArchitectureManualSection);
        return instructionDescription;
    }

    private final List<InstructionDescription_Type> instructionDescriptions = new LinkedList<InstructionDescription_Type>();

    private static void deepCopy(Object[] src, List<Object> dst) {
        for (Object object : src) {
            if (object instanceof Object[]) {
                deepCopy((Object[]) object, dst);
            } else {
                dst.add(object);
            }
        }
    }

    protected InstructionDescription_Type define(Object... specifications) {
        List<Object> specList = new ArrayList<Object>(specifications.length * 2);
        deepCopy(specifications, specList);
        return defineInstructionDescription(specList);
    }

    private String currentArchitectureManualSection;

    /**
     * Sets the name of the architecture manual section for which instruction descriptions are
     * currently being {@link #define defined}.
     */
    public void setCurrentArchitectureManualSection(String section) {
        currentArchitectureManualSection = section;
    }

    public List<InstructionDescription_Type> instructionDescriptions() {
        return instructionDescriptions;
    }
}
