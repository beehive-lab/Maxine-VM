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


/**
 * Represents the target machine for a compiler, including the CPU architecture, the size of pointers and references,
 * alignment of stacks, caches, etc.
 *
 * @author Ben L. Titzer
 */
public class CiTarget {
    public final CiArchitecture arch;

    public final int pageSize;
    public final boolean isMP;
    private final int[] spillSlotsPerKindMap;
    
    /**
     * Specifies if this target supports encoding objects inline in the machine code.
     */
    public final boolean inlineObjects;

    /**
     * The spill slot size for values that occupy 1 {@linkplain CiKind#sizeInSlots() Java slot}.
     */
    public final int spillSlotSize;

    public final int wordSize;
    public final int referenceSize;
    public final int stackAlignment;
    public final int cacheAlignment;
    public final int codeAlignment;
    public final int heapAlignment;

    /**
     * Specifies how {@code long} and {@code double} constants are to be stored
     * in {@linkplain CiDebugInfo.Frame frames}. This is useful for VMs such as HotSpot
     * where convention the interpreter uses is that the second local
     * holds the first raw word of the native long or double representation.
     * This is actually reasonable, since locals and stack arrays
     * grow downwards in all implementations.
     * If, on some machine, the interpreter's Java locals or stack
     * were to grow upwards, the embedded doubles would be word-swapped.)
     */
    public final boolean debugInfoDoubleWordsInSecondSlot;
    
    public CiTarget(CiArchitecture arch,
             boolean isMP,
             int spillSlotSize,
             int wordSize,
             int referenceSize,
             int stackAlignment,
             int pageSize,
             int cacheAlignment,
             int heapAlignment,
             int codeAlignment,
             boolean inlineObjects, boolean debugInfoDoubleWordsInSecondSlot) {
        this.arch = arch;
        this.pageSize = pageSize;
        this.isMP = isMP;
        this.spillSlotSize = spillSlotSize;
        this.wordSize = wordSize;
        this.referenceSize = referenceSize;
        this.stackAlignment = stackAlignment;
        this.cacheAlignment = cacheAlignment;
        this.codeAlignment = codeAlignment;
        this.heapAlignment = heapAlignment;
        this.inlineObjects = inlineObjects;
        this.spillSlotsPerKindMap = new int[CiKind.values().length];
        this.debugInfoDoubleWordsInSecondSlot = debugInfoDoubleWordsInSecondSlot;

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
        return kind.sizeInBytes(referenceSize, wordSize);
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
