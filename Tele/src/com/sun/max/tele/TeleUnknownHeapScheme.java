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
package com.sun.max.tele;

import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Implementation details about the heap in the VM, specialized
 * for situations where there is no specialization code available for the heap scheme.
 *<br>
 * Assume that any address in a region known to be a heap region is live, and that
 * anything else is not.
 *
 * @author Michael Van De Vanter
 */
public final class TeleUnknownHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme{

    protected TeleUnknownHeapScheme(TeleVM vm) {
        super(vm);
    }

    public Class heapSchemeClass() {
        return null;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        return Collections.emptyList();
    }

    public Offset gcForwardingPointerOffset() {
        // Don't know anything about how this GC works.
        return null;
    }

    public boolean isInLiveMemory(Address address) {
        if (heap().isInGC()) {
            return true;
        }
        for (MaxHeapRegion heapRegion : heap().heapRegions()) {
            if (heapRegion.memoryRegion().contains(address)) {
                return true;
            }
        }
        return false;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return false;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return pointer;
    }

    public Pointer getForwardedOrigin(Pointer pointer) {
        return pointer;
    }

    public boolean isObjectForwarded(Pointer origin) {
        return false;
    }

}
