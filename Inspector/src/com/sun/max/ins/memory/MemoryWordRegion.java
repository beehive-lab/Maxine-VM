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
package com.sun.max.ins.memory;

import com.sun.max.ins.util.*;
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
        InspectorError.check(start.isAligned(wordSize.toInt()));
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
