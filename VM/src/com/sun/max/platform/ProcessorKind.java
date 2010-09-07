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
package com.sun.max.platform;

import com.sun.max.asm.*;
import com.sun.max.lang.*;

/**
 * Aggregation of all processor-related info relevant to VM configuration.
 *
 * @author Bernd Mathiske
 */
public class ProcessorKind {

    public final ProcessorModel processorModel;
    public final InstructionSet instructionSet;
    public final DataModel dataModel;

    public ProcessorKind(ProcessorModel processorModel, InstructionSet instructionSet, DataModel dataModel) {
        this.processorModel = processorModel;
        this.instructionSet = instructionSet;
        this.dataModel = dataModel;
    }

    public static ProcessorKind defaultForInstructionSet(InstructionSet instructionSet) {
        final ProcessorModel processorModel = ProcessorModel.defaultForInstructionSet(instructionSet);
        return new ProcessorKind(processorModel, instructionSet, processorModel.defaultDataModel);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ProcessorKind)) {
            return false;
        }
        final ProcessorKind pk = (ProcessorKind) other;
        return processorModel.equals(pk.processorModel) && instructionSet.equals(pk.instructionSet) && dataModel.equals(pk.dataModel);
    }

    @Override
    public String toString() {
        return processorModel.toString().toLowerCase() + ", isa=" + instructionSet + ", " + dataModel;
    }

}
