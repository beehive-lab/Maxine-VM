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
package com.sun.max.asm.gen.risc;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.risc.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class RiscAssembly extends Assembly<RiscTemplate> {

    protected RiscAssembly(InstructionSet instructionSet, Class<RiscTemplate> templateType) {
        super(instructionSet, templateType);
    }

    private List<SpecificityGroup> specificityGroups;

    private void initialize() {
        final IntHashMap<IntHashMap<OpcodeMaskGroup>> specificityTable = new IntHashMap<IntHashMap<OpcodeMaskGroup>>();
        for (RiscTemplate template : templates()) {
            if (!template.isRedundant()) {
                IntHashMap<OpcodeMaskGroup> opcodeMaskGroups = specificityTable.get(template.specificity());
                if (opcodeMaskGroups == null) {
                    opcodeMaskGroups = new IntHashMap<OpcodeMaskGroup>();
                    specificityTable.put(template.specificity(), opcodeMaskGroups);
                }
                final int opcodeMask = template.opcodeMask();
                OpcodeMaskGroup opcodeMaskGroup = opcodeMaskGroups.get(opcodeMask);
                if (opcodeMaskGroup == null) {
                    opcodeMaskGroup = new OpcodeMaskGroup(opcodeMask);
                    opcodeMaskGroups.put(opcodeMask, opcodeMaskGroup);
                }
                opcodeMaskGroup.add(template);
            }
        }
        specificityGroups = new LinkedList<SpecificityGroup>();
        for (int specificity = 33; specificity >= 0; specificity--) {
            final IntHashMap<OpcodeMaskGroup> opcodeGroupTable = specificityTable.get(specificity);
            if (opcodeGroupTable != null) {
                final List<OpcodeMaskGroup> opcodeMaskGroups = opcodeGroupTable.toList();
                final SpecificityGroup specificityGroup = new SpecificityGroup(specificity, opcodeMaskGroups);
                specificityGroups.add(specificityGroup);
            }
        }
    }

    public void printSpecificityGroups(PrintStream out) {
        for (SpecificityGroup specificityGroup : specificityGroups) {
            out.println("Specificity group " + specificityGroup.specificity());
            for (OpcodeMaskGroup opcodeMaskGroup : specificityGroup.opcodeMaskGroups()) {
                out.println("  Opcode mask group " + Integer.toBinaryString(opcodeMaskGroup.mask()));
                for (RiscTemplate template : opcodeMaskGroup.templates()) {
                    out.println("    " + template);
                }
            }
        }
    }

    public List<SpecificityGroup> specificityGroups() {
        if (specificityGroups == null) {
            initialize();
        }
        return specificityGroups;
    }

}
