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
/*VCSID=7f204dde-a229-4abc-bb03-9eaf09601f8c*/
package com.sun.max.asm.gen.risc;

import com.sun.max.collect.*;

/**
 * This class provides a mechanism for making modifications to a set of RISC instruction descriptions.
 *
 * @author Bernd Mathiske
 */
public class RiscInstructionDescriptionModifier {

    private final Sequence<RiscInstructionDescription> _instructionDescriptions;

    public RiscInstructionDescriptionModifier(Sequence<RiscInstructionDescription> instructionDescriptions) {
        _instructionDescriptions = instructionDescriptions;
    }

    /**
     * Replaces a specification in the set of instruction descriptions.
     * 
     * @param before  the specification to be replaced (matched with {@link Object#equals})
     * @param after   the replacement value
     */
    public RiscInstructionDescriptionModifier replace(Object before, Object after) {
        for (RiscInstructionDescription instructionDescription : _instructionDescriptions) {
            final MutableSequence<Object> specifications = instructionDescription.specifications();
            for (int i = 0; i < specifications.length(); i++) {
                if (specifications.get(i).equals(before)) {
                    specifications.set(i, after);
                }
            }
        }
        return this;
    }


    public RiscInstructionDescriptionModifier swap(Object a, Object b) {
        for (RiscInstructionDescription instructionDescription : _instructionDescriptions) {
            final MutableSequence<Object> specifications = instructionDescription.specifications();
            final int aIndex = Sequence.Static.indexOfIdentical(specifications, a);
            final int bIndex = Sequence.Static.indexOfIdentical(specifications, b);
            if (aIndex != -1 && bIndex != -1) {
                specifications.set(aIndex, b);
                specifications.set(bIndex, a);
            }
        }
        return this;
    }

    public RiscInstructionDescriptionModifier setExternalName(String externalName) {
        for (RiscInstructionDescription instructionDescription : _instructionDescriptions) {
            instructionDescription.setExternalName(externalName);
        }
        return this;
    }
}
