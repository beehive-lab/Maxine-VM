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
/*VCSID=9e0ef7a3-f9bd-4183-b563-4d1bfcf907da*/
package com.sun.max.platform;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;

/**
 * Aggregation of all processor-related info relevant to VM configuration.
 *
 * @author Bernd Mathiske
 */
public class ProcessorKind {

    private final ProcessorModel _processorModel;
    private final InstructionSet _instructionSet;
    private final DataModel _dataModel;

    public ProcessorKind(ProcessorModel processorModel, InstructionSet instructionSet, DataModel dataModel) {
        _processorModel = processorModel;
        _instructionSet = instructionSet;
        _dataModel = dataModel;
    }

    @INLINE
    public final ProcessorModel processorModel() {
        return _processorModel;
    }

    @INLINE
    public final InstructionSet instructionSet() {
        return _instructionSet;
    }

    @INLINE
    public final DataModel dataModel() {
        return _dataModel;
    }

    public static ProcessorKind defaultForInstructionSet(InstructionSet instructionSet) {
        final ProcessorModel processorModel = ProcessorModel.defaultForInstructionSet(instructionSet);
        return new ProcessorKind(processorModel, instructionSet, processorModel.defaultDataModel());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ProcessorKind)) {
            return false;
        }
        final ProcessorKind processorKind = (ProcessorKind) other;
        return _processorModel.equals(processorKind._processorModel) && _instructionSet.equals(processorKind._instructionSet) && _dataModel.equals(processorKind._dataModel);
    }

    @Override
    public String toString() {
        return _processorModel.toString().toLowerCase() + ", isa=" + _instructionSet + ", " + _dataModel;
    }

}
