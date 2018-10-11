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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.SafepointPoll.State;
import com.sun.max.vm.thread.*;

/**
 * Access to a block of thread local storage.
 *
 * @see VmThreadLocal
 */
public final class TeleThreadLocalsBlock extends AbstractVmHolder implements TeleVMCache, MaxThreadLocalsBlock {

    private static final int TRACE_VALUE = 2;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    /**
     * Description of the memory region occupied by a {@linkplain MaxThreadLocalsBlock thread locals block} in the VM.
     * <br>
     * This region has no parent; it is allocated from the OS.
     * <br>
     * This region's children are
     * the {@linkplain MaxThreadLocalsArea thread locals areas} it contains.
     */
    private final class ThreadLocalsBlockMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxThreadLocalsBlock> {

        private final TeleThreadLocalsBlock teleThreadLocalsBlock;

        private ThreadLocalsBlockMemoryRegion(MaxVM vm, TeleThreadLocalsBlock owner, String regionName, Address start, long nBytes) {
            super(vm, regionName, start, nBytes);
            this.teleThreadLocalsBlock = owner;
        }

        public MaxEntityMemoryRegion<? extends MaxEntity> parent() {
            // Thread local memory blocks are allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion<? extends MaxEntity>> children() {
            if (threadLocalsBlockMemoryRegion == null) {
                return new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity>>(0);
            }
            final List<MaxEntityMemoryRegion<? extends MaxEntity>> regions =
                new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity>>(areas.size());
            for (TeleThreadLocalsArea teleThreadLocalsArea : areas.values()) {
                if (teleThreadLocalsArea != null) {
                    regions.add(teleThreadLocalsArea.memoryRegion());
                }
            }
            return regions;
        }

        public MaxThreadLocalsBlock owner() {
            return teleThreadLocalsBlock;
        }

    }

    private final String entityName;
    private final String entityDescription;
    private final TeleNativeThread teleNativeThread;

    /**
     * The region of VM memory occupied by this block, null if this is a dummy
     * for which there are no locals (as for a native thread).
     * <p>
     * Don't null this field out when the thread is known to have died;
     * we'll need to keep track of it to update update information about memory allocations.
     */
    private final ThreadLocalsBlockMemoryRegion threadLocalsBlockMemoryRegion;

    /**
     * The thread locals areas for each state; null if no actual thread locals allocated.
     */
    private final Map<SafepointPoll.State, TeleThreadLocalsArea> areas;
    private final int offsetToTTLA;

    /**
     * Control to prevent infinite recursion due to cycle in call path.
     */
    private boolean updatingCache = false;

    /**
     * The VM thread object pointed to by the most recently read value of a particular thread local variable.
     */
    private TeleVmThread teleVmThread = null;

    /**
     * Creates an accessor for thread local information in the ordinary case.
     *
     * @param teleNativeThread the thread owning the thread local information
     * @param regionName descriptive name for this thread locals block in the VM
     * @param start starting location of the memory associated with this entity in the VM.
     * @param nBytes length of the memory associated with this entity in the VM.
     */
    public TeleThreadLocalsBlock(TeleNativeThread teleNativeThread, String regionName, Address start, long nBytes) {
        super(teleNativeThread.vm());
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleNativeThread = teleNativeThread;
        this.entityName = regionName;
        this.threadLocalsBlockMemoryRegion = new ThreadLocalsBlockMemoryRegion(teleNativeThread.vm(), this, regionName, start, nBytes);
        teleNativeThread.vm().addressSpace().add(threadLocalsBlockMemoryRegion);
        this.areas = new EnumMap<SafepointPoll.State, TeleThreadLocalsArea>(SafepointPoll.State.class);
        this.offsetToTTLA = Platform.platform().pageSize - Word.size();
        this.entityDescription = "VM thread-local variables owned by thread " + teleNativeThread.entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    /**
     * Creates an accessor for thread local information in the special case where there is actually no thread local storage
     * identified. This might happen if the thread is non-Java, or isn't far enough along in its creation sequence for
     * the storage to be known.
     *
     * @param teleNativeThread the thread owning the thread local information
     * @param name a descriptive name for the area, in the absence of one associated with a memory region
     */
    public TeleThreadLocalsBlock(TeleNativeThread teleNativeThread, String name) {
        super(teleNativeThread.vm());
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleNativeThread = teleNativeThread;
        this.entityName = name;
        this.threadLocalsBlockMemoryRegion = null;
        this.areas = null;
        this.offsetToTTLA = Platform.platform().pageSize - Word.size();
        this.entityDescription = "VM thread-local variables owned by thread " + teleNativeThread.entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    public void updateCache(long epoch) {
        if (threadLocalsBlockMemoryRegion != null) {
            // This gets called redundantly from several places; be sure it only gets done once per epoch.
            if (epoch > lastUpdateEpoch) {
                assert vm().lockHeldByCurrentThread();
                if (updatingCache) {
                    return;
                }
                updatingCache = true;
                updateTracer.begin();
                for (TeleThreadLocalsArea teleThreadLocalsArea : areas.values()) {
                    if (teleThreadLocalsArea != null) {
                        teleThreadLocalsArea.updateCache(epoch);
                    }
                }
                final TeleThreadLocalsArea enabledThreadLocalsArea = areas.get(SafepointPoll.State.ENABLED);
                if (enabledThreadLocalsArea != null) {
                    final Word threadLocalValue = enabledThreadLocalsArea.getWord(VmThreadLocal.VM_THREAD);
                    if (threadLocalValue.isNotZero()) {
                        teleVmThread = (TeleVmThread) objects().findObjectAt(threadLocalValue.asAddress());
                    }
                }
                updatingCache = false;
                lastUpdateEpoch = epoch;
                updateTracer.end(null);
            } else {
                Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
            }
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxThreadLocalsBlock> memoryRegion() {
        return threadLocalsBlockMemoryRegion;
    }

    public boolean contains(Address address) {
        return threadLocalsBlockMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents this.
        return null;
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public TeleThreadLocalsArea tlaFor(State state) {
        if (threadLocalsBlockMemoryRegion != null) {
            updateCache(vm().teleProcess().epoch());
            return areas.get(state);
        }
        return null;
    }

    public MaxThreadLocalsArea findTLA(Address address) {
        if (threadLocalsBlockMemoryRegion != null) {
            for (SafepointPoll.State state : SafepointPoll.State.CONSTANTS) {
                final TeleThreadLocalsArea tla = tlaFor(state);
                if (tla.memoryRegion().contains(address)) {
                    return tla;
                }
            }
        }
        return null;
    }

    /**
     * Gets the value of the thread local variable holding a reference to the VM thread corresponding to the native
     * thread.
     *
     * @return access to the VM thread corresponding to this thread, if any
     */
    TeleVmThread teleVmThread() {
        return teleVmThread;
    }

    /**
     * Update any state related to this thread locals area, based on possibly more information having been acquired.
     *
     * @param threadLocalsRegion the memory region containing the thread locals block
     * @param tlaSize the size in bytes of each Thread Locals Area in the region.
     */
    void updateAfterGather(TeleFixedMemoryRegion threadLocalsRegion, int tlaSize) {
        if (threadLocalsRegion != null) {
            for (SafepointPoll.State safepointState : SafepointPoll.State.CONSTANTS) {
                final Pointer tlaStartPointer = getThreadLocalsAreaStart(threadLocalsRegion, tlaSize, safepointState);
                // Only create a new TeleThreadLocalsArea if the start address has changed which
                // should only happen once going from 0 to a non-zero value.
                final TeleThreadLocalsArea area = areas.get(safepointState);
                if (area == null || !area.memoryRegion().start().equals(tlaStartPointer)) {
                    areas.put(safepointState, new TeleThreadLocalsArea(vm(), thread(), safepointState, tlaStartPointer));
                }
            }
            updateCache(vm().teleProcess().epoch());
        }
    }

    /**
     * Removes any state associated with the thread, typically because the thread has died.
     */
    void clear() {
        if (threadLocalsBlockMemoryRegion != null) {
            vm().addressSpace().remove(threadLocalsBlockMemoryRegion);
            areas.clear();
            teleVmThread = null;
            lastUpdateEpoch = vm().teleProcess().epoch();
        }
    }

    /**
     * Gets the address of one of the three thread locals areas inside a given thread locals region.
     *
     * @param threadLocalsRegion the VM memory region containing the thread locals block
     * @param tlaSize the size of a thread locals area within the region
     * @param safepointState denotes which of the three thread locals areas is being requested
     * @return the address of the thread locals areas in {@code threadLocalsRegion} corresponding to {@code state}
     * @see VmThreadLocal
     */
    private Pointer getThreadLocalsAreaStart(TeleFixedMemoryRegion threadLocalsRegion, int tlaSize, SafepointPoll.State safepointState) {
        if (threadLocalsRegion != null) {
            return threadLocalsRegion.start().plus(offsetToTTLA).plus(tlaSize * safepointState.ordinal()).asPointer();
        }
        return null;
    }

}
