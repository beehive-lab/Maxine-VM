/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.reference.legacy;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.tele.*;

// TODO (mlvdv) This mechanism is only relevant to inspection of VM's with relocating
// GC.  It should be pushed down into the heap-specific support, implementations of {@link TeleHeapScheme}

/**
 * Access to and management of a special array in VM memory
 * that holds GC roots on behalf of references held in the Inspector.
 * <p>
 * <strong>Assumption:</strong> the special array, once allocated, does not move.
 * <p>
 * Remote references held by the inspector are implemented as a
 * handle that identifies an entry in this table, which contains actual
 * memory addresses in the VM.  In order to track object movement
 * across GC invocations, a mirror of this table is maintained in the
 * VM, where it can be updated by the GC.
 *
 * @see InspectableHeapInfo
 * @see VmObjectAccess
 */
public final class TeleRoots extends AbstractVmHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;
    private static final int DETAILED_TRACE_VALUE = 2;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final VmReferenceManager teleReferenceManager;

    // TODO (mlvdv) this properly belongs to implementations of {@link TeleHeapScheme}
    // that support relocating collectors and should be moved.
    /**
     * Memory location of the specially allocated memory in the VM to be
     * used for the Inspector root table.
     */
    private Address rootsRegionStart = Pointer.zero();

    // TODO (mlvdv) this properly belongs to implementations of {@link TeleHeapScheme}
    // that support relocating collectors and should be moved.
    /**
     * Gets the raw location of the tele roots table in VM memory.  This is a specially allocated
     * region of memory that is assumed will not move.
     * <br>
     * It is equivalent to the starting location of the roots region, but must be
     * accessed this way instead to avoid a circularity.  It is used before
     * more abstract objects such as {@link TeleFixedMemoryRegion}s can be created.
     *
     * @return location of the specially allocated VM memory region where teleRoots are stored.
     * @see InspectableHeapInfo
     */
    private Address rootsRegionStart() {
        if (rootsRegionStart.isZero()) {
            // The address of the tele roots field must be known before we can create any instances of relocatable
            // references, since those references must be registered in the VM's root table using this address.
            rootsRegionStart = fields().InspectableHeapInfo_rootsPointer.readWord(vm()).asAddress();
            Trace.line(TRACE_VALUE, "rootsRegionStart=" + rootsRegionStart.to0xHexString());
        }
        return rootsRegionStart;
    }

    private final Address[] cachedRoots = new Address[InspectableHeapInfo.MAX_NUMBER_OF_ROOTS];
    private final BitSet usedIndices = new BitSet();

    /**
     * Set of pending tele root handles to be cleared. Asynchronous unregistration is required
     * given that roots can be unregistered by a call from {@link MutableTeleReference#finalize()}
     * on the finalization thread. Clearing the entry in the remote root table requires writing
     * to the VM which may require acquiring certain locks. All too often acquiring these
     * locks on the finalizer thread leads to some kind of deadlock.
     */
    private final BitSet unregistrationQueue = new BitSet();

    public TeleRoots(TeleVM vm, VmReferenceManager teleReferenceManager) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleReferenceManager = teleReferenceManager;
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updating the <strong>root cache</strong> involves copying the VM's copy
     * of the Inspector root table, possibly modified by GC since the last update,
     * into the local cache of the root table.
     */
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            final int numberOfIndices = usedIndices.length();
            for (int i = 0; i < numberOfIndices; i++) {
                Trace.line(DETAILED_TRACE_VALUE, tracePrefix() + "copying from VM(" + i + ")");
                WordArray.set(cachedRoots, i, memory().getWord(rootsRegionStart(), 0, i).asAddress());
            }
            lastUpdateEpoch = epoch;
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + " redundant udpate epoch=" + epoch + ": " + this);
        }
    }

    public int registeredRootCount() {
        int count = 0;
        for (Address address : cachedRoots) {
            if (address != null && !address.equals(Address.zero())) {
                count++;
            }
        }
        return count;
    }

    // TODO (mlvdv) Why isn't this done with every refresh (but probably shouldn't be done during GC)?
    // At present this only happens when a GC has just completed.
    /**
     * Clears the entries in the VM's Tele root table that were submitted by {@link #unregister(int)}.
     */
    public void flushUnregisteredRoots() {
        synchronized (unregistrationQueue) {
            for (int index = unregistrationQueue.nextSetBit(0); index >= 0; index = unregistrationQueue.nextSetBit(index + 1)) {
                WordArray.set(cachedRoots, index, Address.zero());
                usedIndices.clear(index);
                //teleRootsReference().setWord(0, index, Word.zero());
                Trace.line(DETAILED_TRACE_VALUE, tracePrefix() + "Unegistering(" + index + ")");
                memory().setWord(rootsRegionStart(), 0, index, Word.zero());
            }
            usedIndices.andNot(unregistrationQueue);
            unregistrationQueue.clear();
        }
    }

    /**
     * Register a VM memory location in the VM's Inspector root table, both the local copy
     * and the one in VM memory.
     */
    int register(Address rawReference) {
        final int index = usedIndices.nextClearBit(0);
        usedIndices.set(index);
        // Local copy of root table
        WordArray.set(cachedRoots, index, rawReference);
        // Remote root table
        //teleRootsReference().setWord(0, index, rawReference);
        Trace.line(DETAILED_TRACE_VALUE, tracePrefix() + "Registering(" + index + "," + rawReference.toHexString() + ")");
        memory().setWord(rootsRegionStart(), 0, index, rawReference);
        return index;
    }

    /**
     * Notifies this object that an entry should be cleared in the VM's Tele root table.
     * The clearing does not actually occur until {@link #flushUnregisteredRoots()} is called.
     */
    void unregister(int index) {
        synchronized (unregistrationQueue) {
            Trace.line(DETAILED_TRACE_VALUE, tracePrefix() + "Enqueuing unregistration(" + index + ")");
            unregistrationQueue.set(index);
        }
    }

    /**
     * The remote location bits currently at a position in the Inspector root table.
     */
    Address getRawReference(int index) {
        return WordArray.get(cachedRoots, index).asAddress();
    }

}
