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
 * @Runs: (10)=true; (20)=true;
 */
/**
 */
package jtt.max;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

public final class ImmortalHeap_switching {
    private ImmortalHeap_switching() {
    }

    // Ensure the allocations aren't optimized away!
    public static Object obj1;
    public static Object obj2;
    public static Object obj3;

    public static boolean test(int size) {
        ImmortalMemoryRegion immortalMemoryRegion = ImmortalHeap.getImmortalHeap();
        Pointer oldMark = immortalMemoryRegion.mark();
        obj1 = new Object();
        if (!immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }
        try {
            Heap.enableImmortalMemoryAllocation();
            obj2 = new Object();
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
        if (immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }
        oldMark = immortalMemoryRegion.mark();
        obj3 = new Object();
        return immortalMemoryRegion.mark().equals(oldMark);
    }
}
