/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import static com.sun.max.vm.heap.gcx.HeapFreeChunk.*;

/**
 * Helper class to specially mark objects identified as dark-matter.
 * This is useful only in debug mode, to help the Inspector.
 *
 */
public final class DarkMatterDebugHelper {
    // FIXME: this is designed to not interfere with misc word bit use for thin/biased lock word and hash code.
    // Management of misc word usage must be revisited so it can be centralized so anyone can check what
    // bits are being reserved and whether there is any conflicting use.
    // The DARK MATTER flag set all bits to zero (so it looks like an unlocked, un-hashed header), except for  the bits used for counting recursive lock requests,
    // namely, the highest 5 bits.
    // This is an unused pattern across all current locking implementation, which allow to unambiguously distinguish live objects from dark-matter.
    public static final Word DARK_MATTER = Word.allOnes().asAddress().unsignedShiftedRight(5);

    @INLINE
    private static void setDarkMatter(Pointer origin) {
        Layout.writeMisc(origin, DARK_MATTER);
    }

    public static void setDarkMatter(Address cell) {
        setDarkMatter(Layout.cellToOrigin(cell.asPointer()));
    }

    /**
     * Format all objects in the specified range as dark matter.
     * @param firstDarkMatterWord
     * @param darkMatterSpread
     */
    public static void setDarkMatter(Address firstDarkMatterWord, Size darkMatterSpread) {
        Address cell = firstDarkMatterWord.asPointer();
        final Address end = cell.plus(darkMatterSpread);
        while (cell.lessThan(end)) {
            final Pointer origin = Layout.cellToOrigin(cell.asPointer());
            setDarkMatter(origin);
            if (Layout.getHub(origin) == heapFreeChunkHub()) {
                cell = cell.plus(toHeapFreeChunk(origin).size);
            } else {
                cell = cell.plus(Layout.size(origin));
            }
        }
    }
}
