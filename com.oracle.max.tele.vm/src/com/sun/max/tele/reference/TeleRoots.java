/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.reference;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.tele.*;

/**
 * Access to and management of a special array in the VM
 * that holds GC roots on behalf of references held in the Inspector.
 * <br>
 * Remote references held by the inspector are implemented as a
 * handle that identifies an entry in this table, which contains actual
 * memory addresses in the VM.  In order to track object movement
 * across GC invocations, a mirror of this table is maintained in the
 * VM, where it can be updated by the GC.
 *
 * @see InspectableHeapInfo
 * @see TeleHeap
 */
public final class TeleRoots extends AbstractTeleVMHolder implements TeleVMCache {


    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final TeleReferenceScheme teleReferenceScheme;

    private final Address[] cachedRoots = new Address[InspectableHeapInfo.MAX_NUMBER_OF_ROOTS];
    private final BitSet usedIndices = new BitSet();

    /**
     * Queue of pending tele roots to be cleared. Asynchronous unregistration is required
     * given that roots can be unregistered by a call from {@link MutableTeleReference#finalize()}
     * on the finalization thread. Clearing the entry in the remote root table requires writing
     * to the VM which may require acquiring certain locks. All too often acquiring these
     * locks on the finalizer thread leads to some kind of deadlock.
     */
    private final BitSet unregistrationQueue = new BitSet();

    TeleRoots(TeleReferenceScheme teleReferenceScheme) {
        super(teleReferenceScheme.vm());
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleReferenceScheme = teleReferenceScheme;
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            // Flush local cache; copy remote contents of Inspectors' root table into Inspector's local cache.
            final int numberOfIndices = usedIndices.length();
            for (int i = 0; i < numberOfIndices; i++) {
                WordArray.set(cachedRoots, i, teleRootsReference().getWord(0, i).asAddress());
            }
            lastUpdateEpoch = epoch;
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + " redundant udpate epoch=" + epoch + ": " + this);
        }
    }


    /**
     * Clears the entries in the VM's Tele root table that were submitted by {@link #unregister(int)}.
     */
    public void flushUnregisteredRoots() {
        synchronized (unregistrationQueue) {
            for (int index = unregistrationQueue.nextSetBit(0); index >= 0; index = unregistrationQueue.nextSetBit(index + 1)) {
                WordArray.set(cachedRoots, index, Address.zero());
                usedIndices.clear(index);
                teleRootsReference().setWord(0, index, Word.zero());
            }
            usedIndices.andNot(unregistrationQueue);
            unregistrationQueue.clear();
        }
    }

    private RemoteTeleReference teleRootsReference() {
        return teleReferenceScheme.createTemporaryRemoteTeleReference(vm().dataAccess().readWord(heap().teleRootsPointer()).asAddress());
    }

    /**
     * Register a VM location in the VM's Inspector root table.
     */
    int register(Address rawReference) {
        final int index = usedIndices.nextClearBit(0);
        usedIndices.set(index);
        // Local copy of root table
        WordArray.set(cachedRoots, index, rawReference);
        // Remote root table
        teleRootsReference().setWord(0, index, rawReference);
        return index;
    }

    /**
     * Notifies this object that an entry should be cleared in the VM's Tele root table.
     * The clearing does not actually occur until {@link #flushUnregisteredRoots()} is called.
     */
    void unregister(int index) {
        synchronized (unregistrationQueue) {
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
