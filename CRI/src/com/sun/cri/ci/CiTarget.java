/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ci;

import com.sun.cri.ci.CiRegister.*;
import com.sun.cri.ri.*;

/**
 * This class represents the target machine for a compiler, including
 * the CPU architecture, the size of pointers and references, alignment
 * of stacks, caches, etc.
 *
 * @author Ben L. Titzer
 */
public class CiTarget {
    public final CiArchitecture arch;

    public final AllocationSpec allocationSpec;
    public final CiRegister stackPointerRegister;
    public final CiRegister scratchRegister;
    public final RiRegisterConfig registerConfig;
    public final int pageSize;
    public final boolean isMP;
    private final int[] spillSlotsPerKindMap;
    
    /**
     * The spill slot size for values that occupy 1 {@linkplain CiKind#sizeInSlots() Java slot}.
     */
    public final int spillSlotSize;

    public int referenceSize;
    public int stackAlignment;
    public int cacheAlignment;
    public int codeAlignment;
    public int heapAlignment;

    public CiTarget(CiArchitecture arch, RiRegisterConfig registerConfig, int pageSize, boolean isMP) {
        this.arch = arch;
        this.registerConfig = registerConfig;
        this.referenceSize = arch.wordSize;
        this.stackAlignment = arch.wordSize;
        this.cacheAlignment = arch.wordSize;
        this.heapAlignment = arch.wordSize;
        this.spillSlotSize = arch.wordSize;
        this.codeAlignment = 16;

        this.stackPointerRegister = registerConfig.getStackPointerRegister();
        this.scratchRegister = registerConfig.getScratchRegister();

        this.pageSize = pageSize;
        this.isMP = isMP;
        this.allocationSpec = new AllocationSpec(registerConfig.getAllocatableRegisters(), registerConfig.getRegisterReferenceMapOrder(), registerConfig.getCallerSaveRegisters());
        this.spillSlotsPerKindMap = new int[CiKind.values().length];

        for (CiKind k : CiKind.values()) {
            // initialize the number of spill slots required for each kind
            int size = k.sizeInBytes(referenceSize, arch.wordSize);
            int slots = 0;
            while (slots * spillSlotSize < size) {
                slots++;
            }
            spillSlotsPerKindMap[k.ordinal()] = slots;
        }
    }

    /**
     * Gets the size in bytes of the specified kind for this target.
     * @param kind the kind for which to get the size
     * @return the size in bytes of {@code kind}
     */
    public int sizeInBytes(CiKind kind) {
        return kind.sizeInBytes(referenceSize, arch.wordSize);
    }

    /**
     * Gets the number of spill slots for a specified kind in this target.
     * @param kind the kind for which to get the spill slot count
     * @return the number of spill slots for {@code kind}
     */
    public int spillSlots(CiKind kind) {
        return spillSlotsPerKindMap[kind.ordinal()];
    }
    
    /**
     * Aligns the given frame size (without return instruction pointer) to the stack
     * alignment size and return the aligned size (without return instruction pointer).
     * @param frameSize the initial frame size to be aligned
     * @return the aligned frame size
     */
    public int alignFrameSize(int frameSize) {
        int x = frameSize + arch.returnAddressSize + (stackAlignment - 1);
        return (x / stackAlignment) * stackAlignment - arch.returnAddressSize;
    }
}
