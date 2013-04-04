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
 * @Runs: (1)=true; (10)=true;
 */
/**
 */
package jtt.max;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

public final class ImmortalHeap_gc {
    private ImmortalHeap_gc() {
    }

    public static boolean test(int nrObjects) {
        String[] strings;
        ImmortalMemoryRegion immortalMemoryRegion = ImmortalHeap.getImmortalHeap();
        Pointer oldMark = immortalMemoryRegion.mark();

        try {
            Heap.enableImmortalMemoryAllocation();
            strings = new String[nrObjects];
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }

        if (immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }

        oldMark = immortalMemoryRegion.mark();

        for (int i = 0; i < nrObjects; i++) {
            strings[i] = new String("" + i);
        }

        if (!immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }

        System.gc();

        if (!immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }

        String expected = "";
        String result = "";
        for (int i = 0; i < nrObjects; i++) {
            expected += i;
            result += strings[i];
        }

        if (expected.equals(result)) {
            return true;
        }

        return false;
    }

}
