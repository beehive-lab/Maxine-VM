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
package com.sun.max.ins.memory;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Representation for a region of VM memory constrained to contain
 * only whole words, word aligned.
 *
 * @author Michael Van De Vanter
 */
public class MemoryWordRegion extends InspectorMemoryRegion {

    public final int wordCount;
    private final Size wordSize;

    /**
     * Creates a memory region containing only aligned, whole words.
     *
     * @param start address at beginning of region, must be word aligned
     * @param wordCount number of words to include in the region
     * @param wordSize size of word in target platform.
     */
    public MemoryWordRegion(MaxVM vm, Address start, int wordCount, Size wordSize) {
        super(vm, null, start, wordSize.times(wordCount));
        this.wordCount = wordCount;
        this.wordSize = wordSize;
        ProgramError.check(start.isAligned(wordSize.toInt()));
    }

    /**
     * Address of the specified word in the region.
     *
     * @param index index of word, 0 is at start
     * @return the address of the specified word
     */
    public Address getAddressAt(int index) {
        assert index >= 0 && index < wordCount;
        return start().plus(wordSize.times(index));
    }

    /**
     * @return Index of word at a specified address in the region, -1 if not in region.
     */
    public int indexAt(Address address) {
        assert address != null;
        if (contains(address)) {
            return address.minus(start()).dividedBy(wordSize).toInt();
        }
        return -1;
    }

}
