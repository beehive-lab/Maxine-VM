/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.heap.gcx.mse.*;

/**
 * Implementation details about the heap in the VM, specialized for the region-based mark-sweep implementation.
 *
 */
public class TeleMSEHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme {

    /**
     * @param teleVM
     */
    public TeleMSEHeapScheme(TeleVM teleVM) {
        super(teleVM);
    }

    @Override
    public Class heapSchemeClass() {
        return MSEHeapScheme.class;
    }

    public int gcForwardingPointerOffset() {
        // MS is a non-moving collector. Doesn't do any forwarding.
        return -1;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        // MS is a non-moving collector. Doesn't do any forwarding.
        return origin;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return pointer;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return false;
    }

    public boolean isInLiveMemory(Address address) {
        if (heap().isInGC()) {
            // Unclear what the semantics of this should be during GC.
            // We should be able to tell past the marking phase if an address point to a live object.
            // But what about during the marking phase ? The only thing that can be told is that
            // what was dead before marking begin should still be dead during marking.
            return true;
        }
        // TODO (ld)
        // This requires the inspector to know intimately about the heap structures.
        // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
        // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
        // all reusable free space as such (instead of the chunk of free list as is done now). in any case.
        return true;
    }

    public boolean isObjectForwarded(Pointer origin) {
        return false;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        // TODO (ld)
        return Collections.emptyList();
    }

}
