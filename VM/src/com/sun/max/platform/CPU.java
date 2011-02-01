/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
