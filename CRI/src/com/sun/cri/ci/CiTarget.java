/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;


/**
 * Represents the target machine for a compiler, including the CPU architecture, the size of pointers and references,
 * alignment of stacks, caches, etc.
 *
 * @author Ben L. Titzer
 */
public class CiTarget {
    public final CiArchitecture arch;

    /**
     * The OS page size.
     */
    public final int pageSize;
    
    /**
     * Specifies if this is a multi-processor system.
     */
    public final boolean isMP;
    
    /**
     * The number of {@link #spillSlotSize spill slots} required per kind.
     */
    private final int[] spillSlotsPerKindMap;
    
    /**
     * Specifies if this target supports encoding objects inline in the machine code.
     */
    public final boolean inlineObjects;

    /**
     * The spill slot size for values that occupy 1 {@linkplain CiKind#sizeInSlots() Java slot}.
     */
    public final int spillSlotSize;

    /**
     * The machine word size on this target.
     */
    public final int wordSize;

    /**
     * The stack alignment requirement of the platform. For example,
     * from Appendix D of <a href="http://www.intel.com/Assets/PDF/manual/248966.pdf">Intel 64 and IA-32 Architectures Optimization Reference Manual</a>:
     * <pre>
     *     "It is important to ensure that the stack frame is aligned to a
     *      16-byte boundary upon function entry to keep local __m128 data,
     *      parameters, and XMM register spill locations aligned throughout
     *      a function invocation."
     * </pre>
     */
    public final int stackAlignment;
    
    /**
     * @see http://docs.sun.com/app/docs/doc/806-0477/6j9r2e2b9?a=view
     */
    public final int stackBias;
    
    /**
     * The cache alignment.
     */
    public final int cacheAlignment;

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
             int stackAlignment,
             int pageSize,
             int cacheAlignment,
             boolean inlineObjects,
             boolean debugInfoDoubleWordsInSecondSlot) {
        this.arch = arch;
        this.pageSize = pageSize;
        this.isMP = isMP;
        this.spillSlotSize = spillSlotSize;
        this.wordSize = arch.wordSize;
        this.stackAlignment = stackAlignment;
        this.stackBias = 0; // TODO: configure with param once SPARC port exists
        this.cacheAlignment = cacheAlignment;
        this.inlineObjects = inlineObjects;
        this.spillSlotsPerKindMap = new int[CiKind.values().length];
        this.debugInfoDoubleWordsInSecondSlot = debugInfoDoubleWordsInSecondSlot;

        for (CiKind k : CiKind.values()) {
            // initialize the number of spill slots required for each kind
            int size = k.sizeInBytes(arch.wordSize);
            int slots = 0;
            while (slots * spillSlotSize < size) {
                slots++;
            }
            spillSlotsPerKindMap[k.ordinal()] = slots;
        }
    }

    /**
     * Gets the size in bytes of the specified kind for this target.
     * 
     * @param kind the kind for which to get the size
     * @return the size in bytes of {@code kind}
     */
    public int sizeInBytes(CiKind kind) {
        return kind.sizeInBytes(wordSize);
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
