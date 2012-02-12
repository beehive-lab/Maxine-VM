/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: (4)=true; (8)=true; (10)=true; (100)=true;
 */
/**
 */
package jtt.max;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.debug.*;

public final class ImmortalHeap_allocation {
    private ImmortalHeap_allocation() {
    }

    @INLINE
    public static void resetImmortalHeap(ImmortalMemoryRegion immortalMemoryRegion, Pointer value) {
        immortalMemoryRegion.mark.set(value);
    }

    @NO_SAFEPOINT_POLLS("immortal heap allocation and reset must be atomic")
    public static boolean test(int size) {
        ImmortalMemoryRegion immortalMemoryRegion = ImmortalHeap.getImmortalHeap();
        Pointer oldMark = immortalMemoryRegion.mark();
        ImmortalHeap.allocate(Size.fromInt(size), true);
        if (DebugHeap.isTagging()) {
            size += Word.size();
        }
        if (immortalMemoryRegion.mark().equals(oldMark.plus(Size.fromInt(size).wordAligned()))) {
            resetImmortalHeap(immortalMemoryRegion, oldMark);
            return true;
        }
        resetImmortalHeap(immortalMemoryRegion, oldMark);
        return false;
    }

}
