/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.heap;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.ms.*;
import com.sun.max.vm.reference.*;

/**
 * Implementation details about the heap in the VM,
 * specialized for the mark-sweep implementation.
 */
public final class RemoteMSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    public RemoteMSHeapScheme(TeleVM vm) {
        super(vm);
    }

    public Class heapSchemeClass() {
        return MSHeapScheme.class;
    }

    public void initialize(long epoch) {
    }

    public List<VmHeapRegion> heapRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryStatus status() {
                // TODO (mlvdv) ensure the location is in one of the regions being managed.
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }

                // Unclear what the semantics of this should be during GC.
                // We should be able to tell past the marking phase if an address point to a live object.
                // But what about during the marking phase ? The only thing that can be told is that
                // what was dead before marking begin should still be dead during marking.

                // TODO (ld) This requires the inspector to know intimately about the heap structures.
                // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
                // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
                // all reusable free space as such (instead of the chunk of free list as is done now). in any case.

                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                return "";
            }

            public String shortDescription() {
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

    public boolean isObjectOrigin(Address origin) throws TeleError {
        return false;
    }

    public RemoteReference makeReference(Address origin) throws TeleError {
        return null;
    }

    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
    }


    /**
     * Surrogate object for the scheme instance in the VM.
     */
    public static class TeleGenMSHeapScheme extends TeleHeapScheme {

        private TeleFreeHeapSpaceManager freeHeapSpaceManager;


        public TeleGenMSHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

        @Override
        protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
            if (!super.updateObjectCache(epoch, statsPrinter)) {
                return false;
            }
            // TODO (mlvdv) do these ever change once set?
            if (freeHeapSpaceManager == null) {
                final Reference freeHeapSpaceManagerRef = fields().MSHeapScheme_objectSpace.readReference(reference());
                freeHeapSpaceManager = (TeleFreeHeapSpaceManager) objects().makeTeleObject(freeHeapSpaceManagerRef);
            }

            return true;
        }

    }
}
