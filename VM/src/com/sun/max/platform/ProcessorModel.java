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
/*VCSID=17f90c19-45c4-4c6a-bc0e-0eb2b2194740*/
package com.sun.max.platform;

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;

/**
 * A specific processor model.
 *
 * @author Bernd Mathiske
 */
public enum ProcessorModel {

    /* Generic default cores: */
    AMD64(InstructionSet.AMD64, new DataModel(WordWidth.BITS_64, Endianness.LITTLE, Alignment.BYTES_8), MemoryModel.AMD64),
    ARM32(InstructionSet.ARM, new DataModel(WordWidth.BITS_32, Endianness.LITTLE, Alignment.BYTES_4), MemoryModel.SequentialConsistency),
    IA32(InstructionSet.IA32, new DataModel(WordWidth.BITS_32, Endianness.LITTLE, Alignment.BYTES_4), MemoryModel.RelaxedMemoryOrder),
    PPC(InstructionSet.PPC, new DataModel(WordWidth.BITS_32, Endianness.BIG, Alignment.BYTES_4), MemoryModel.RelaxedMemoryOrder),
    SPARC(InstructionSet.SPARC, new DataModel(WordWidth.BITS_32, Endianness.BIG, Alignment.BYTES_4), MemoryModel.TotalStoreOrder),
    SPARCV9(InstructionSet.SPARC, new DataModel(WordWidth.BITS_64, Endianness.BIG, Alignment.BYTES_8), MemoryModel.TotalStoreOrder);

    private final InstructionSet _instructionSet;

    public InstructionSet instructionSet() {
        return _instructionSet;
    }

    private final DataModel _defaultDataModel;

    public DataModel defaultDataModel() {
        return _defaultDataModel;
    }

    private final MemoryModel _memoryModel;

    public MemoryModel memoryModel() {
        return _memoryModel;
    }

    private ProcessorModel(InstructionSet instructionSet, DataModel defaultDataModel, MemoryModel memoryModel) {
        _instructionSet = instructionSet;
        _defaultDataModel = defaultDataModel;
        _memoryModel = memoryModel;
    }

    public static ProcessorModel defaultForInstructionSet(InstructionSet instructionSet) {
        if (instructionSet.equals(InstructionSet.SPARC)) {
            return SPARCV9;
        }
        return valueOf(instructionSet.name());
    }

}
