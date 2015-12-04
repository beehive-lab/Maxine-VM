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
	//System/out.println("GOT ImmortalMemoryRegion");
        Pointer oldMark = immortalMemoryRegion.mark();
	//System/out.println("DONE MARK");
        try {
            Heap.enableImmortalMemoryAllocation();
	    //System/out.println("DONE ENABLEALLOC");
            strings = new String[nrObjects];
	    //System/out.println("DONE ALLOC");
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
	//System/out.println("DONE DISABLE");
        if (immortalMemoryRegion.mark().equals(oldMark)) {
	    //System/out.println("DONE and EQUALS");
            return false;
        }
	//System/out.println("DONE and NEQ");
        oldMark = immortalMemoryRegion.mark();
	//System/out.println("DONE MARK2");

        for (int i = 0; i < nrObjects; i++) {
            strings[i] = new String("" + i);
        }
	//System/out.println("DONE STRINGS");
        if (!immortalMemoryRegion.mark().equals(oldMark)) {
	    //System/out.println("DONE and EQUALS2");
            return false;
        }
	//System/out.println("BEFORE GC");
        System.gc();
	//System/out.println("DONE GC");

        if (!immortalMemoryRegion.mark().equals(oldMark)) {
	    //System/out.println("DONE and EQAULS3");
            return false;
        }

        String expected = "";
        String result = "";
        for (int i = 0; i < nrObjects; i++) {
            expected += i;
            result += strings[i];
        }
	//System/out.println("DONE STRINGS");
        if (expected.equals(result)) {
	    //System/out.println("DONE EQUALS3");
            return true;
        }
	//System/out.println("DONE false");

        return false;
    }

}
