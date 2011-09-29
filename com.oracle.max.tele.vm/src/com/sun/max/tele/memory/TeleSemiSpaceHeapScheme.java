/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.memory;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.runtime.*;

/**
 * Implementation details about the heap in the VM, specialized
 * for the semi-space implementation.
 * <p>
 * Assumes forwarding pointer stored in the "Hub" field of objects.
 *
 * @see SemiSpaceHeapScheme
 */
final class TeleSemiSpaceHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme{

    private static final List<MaxCodeLocation> EMPTY_METHOD_LIST = Collections.emptyList();

    TeleSemiSpaceHeapScheme(TeleVM vm) {
        super(vm);
    }

    public Class heapSchemeClass() {
        return SemiSpaceHeapScheme.class;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        return EMPTY_METHOD_LIST;
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryStatus status() {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }
                if (heap().isInGC()) {
                    // Don't quibble if we're in a GC, as long as the address is in either the To or From regions.
                    return MaxMemoryStatus.LIVE;
                }
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) {
                    // When not in GC, everything in from-space is dead
                    return MaxMemoryStatus.DEAD;
                }
                if (!heapRegion.memoryRegion().containsInAllocated(address)) {
                    // everything in to-space after the global allocation mark is dead
                    return MaxMemoryStatus.FREE;
                }
                for (TeleNativeThread teleNativeThread : vm().teleProcess().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                    TeleThreadLocalsArea teleThreadLocalsArea = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
                    if (teleThreadLocalsArea != null) {
                        Word tlabDisabledWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME);
                        Word tlabMarkWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME);
                        Word tlabTopWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME);
                        if (!tlabDisabledWord.isZero() && !tlabMarkWord.isZero() && !tlabTopWord.isZero()) {
                            if (address.greaterEqual(tlabMarkWord.asAddress()) && tlabTopWord.asAddress().greaterThan(address)) {
                                return MaxMemoryStatus.FREE;
                            }
                        }
                    }
                }
                // Everything else should be live.
                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                // Provide text to be displayed in display cell
                return "";
            }

            public String shortDescription() {
                // More information could be added here
                return vm().heapScheme().name();
            }

            public Address address() {
                return address;
            }
            public TeleObject tele() {
                return null;
            }
        };
    }

    public int gcForwardingPointerOffset() {
        return Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();
    }

    @Override
    public boolean isObjectForwarded(Pointer origin) {
        if (!origin.isZero()) {
            Pointer possibleForwardingPointer = vm().dataAccess().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                return true;
            }
        }
        return false;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return (!pointer.isZero()) && pointer.and(1).toLong() == 1;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return isForwardingPointer(pointer) ? pointer.minus(1) : pointer;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        if (!origin.isZero()) {
            Pointer possibleForwardingPointer = vm().dataAccess().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                final Pointer newCell = getTrueLocationFromPointer(possibleForwardingPointer);
                if (!newCell.isZero()) {
                    return Layout.generalLayout().cellToOrigin(newCell);
                }
            }
        }
        return origin;
    }
}
