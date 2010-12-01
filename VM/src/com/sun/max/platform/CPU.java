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

import com.sun.max.lang.*;
import com.sun.max.memory.*;

/**
 * A specific processor model.
 *
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public enum CPU {

    /* Generic default cores: */
    // TODO: Obtain the cache alignment at runtime, e.g. by using sysconf(_SC_LEVEL2_CACHE_LINESIZE)
    AMD64(ISA.AMD64, new DataModel(WordWidth.BITS_64, Endianness.LITTLE, 64), MemoryModel.AMD64),
    ARM32(ISA.ARM, new DataModel(WordWidth.BITS_32, Endianness.LITTLE, 64), MemoryModel.SequentialConsistency),
    IA32(ISA.IA32, new DataModel(WordWidth.BITS_32, Endianness.LITTLE, 64), MemoryModel.RelaxedMemoryOrder),
    PPC(ISA.PPC, new DataModel(WordWidth.BITS_32, Endianness.BIG, 64), MemoryModel.RelaxedMemoryOrder),
    SPARC(ISA.SPARC, new DataModel(WordWidth.BITS_32, Endianness.BIG, 64), MemoryModel.TotalStoreOrder),
    SPARCV9(ISA.SPARC, new DataModel(WordWidth.BITS_64, Endianness.BIG, 64), MemoryModel.TotalStoreOrder);

    public final ISA isa;

    public final DataModel defaultDataModel;

    public final MemoryModel memoryModel;

    private CPU(ISA isa, DataModel defaultDataModel, MemoryModel memoryModel) {
        this.isa = isa;
        this.defaultDataModel = defaultDataModel;
        this.memoryModel = memoryModel;
    }

    public static CPU defaultForInstructionSet(ISA isa) {
        if (isa.equals(ISA.SPARC)) {
            return SPARCV9;
        }
        return valueOf(isa.name());
    }

}
