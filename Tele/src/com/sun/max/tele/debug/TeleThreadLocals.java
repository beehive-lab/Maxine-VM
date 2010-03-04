/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Safepoint.*;
import com.sun.max.vm.thread.*;

/**
 * Access to thread local storage.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleThreadLocals extends AbstractTeleVMHolder implements MaxThreadLocals {

    /**
     * Creates an accessor for thread local information, including cases where there is actually
     * no thread local storage identified.  This might happen if the thread is non-Java, or isn't
     * far enough along in its creation sequence for the storage to be known.
     *
     * @param teleNativeThread the thread owning the thread local information
     * @param memoryRegion the memory region, if any, holding thread local information
     * @return access to thread local information
     */
    static TeleThreadLocals create(TeleNativeThread teleNativeThread, MemoryRegion memoryRegion) {
        return (memoryRegion == null) ?
                        new NullThreadLocals(teleNativeThread) :
                            new ThreadLocals(teleNativeThread, memoryRegion);
    }

    private final TeleNativeThread teleNativeThread;

    protected TeleThreadLocals(TeleNativeThread teleNativeThread) {
        super(teleNativeThread.teleVM());
        this.teleNativeThread = teleNativeThread;
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public abstract TeleThreadLocalsArea threadLocalsAreaFor(Safepoint.State state);

    public abstract TeleThreadLocalsMemoryRegion memoryRegion();

    /**
    * Gets the value of the thread local variable holding a reference to the VM thread corresponding to the native thread.
    *
    * @return access to the VM thread corresponding to this thread, if any
    */
    abstract TeleVmThread teleVmThread();

    /**
     * Update any state related to this thread locals area, based on possibly more information having been acquired.
     *
     * @param threadLocalsRegion the memory region containing the thread locals block
     * @param tlaSize the size in bytes of each Thread Locals Area in the region.
     */
    abstract void updateAfterGather(MemoryRegion threadLocalsRegion, int tlaSize);

    /**
     * Removes any state associated with the thread, typically because the thread has died.
     */
    abstract void clear();

    /**
     * Update any state that may have changed since the last process execution step.
     */
    abstract void refresh();

    /**
     * Access to information about the "thread locals block" of storage in the VM for a thread.
     *
     * @author Michael Van De Vanter
     */
    protected static final class ThreadLocals extends TeleThreadLocals {

        private static final int TRACE_LEVEL = 2;

        private final TeleThreadLocalsMemoryRegion memoryRegion;
        private final Map<Safepoint.State, TeleThreadLocalsArea> areas;
        private long lastRefreshedEpoch = -1L;
        private TeleVmThread teleVmThread;

        final int offsetToTriggeredThreadLocals;

        public ThreadLocals(TeleNativeThread teleNativeThread, MemoryRegion memoryRegion) {
            super(teleNativeThread);
            assert teleNativeThread != null;
            assert memoryRegion != null;
            this.memoryRegion = new TeleThreadLocalsMemoryRegion(teleNativeThread, memoryRegion);
            this.areas = new EnumMap<Safepoint.State, TeleThreadLocalsArea>(Safepoint.State.class);
            offsetToTriggeredThreadLocals = Platform.target().pageSize - Word.size();
        }

        @Override
        public TeleThreadLocalsMemoryRegion memoryRegion() {
            return memoryRegion;
        }

        @Override
        public TeleThreadLocalsArea threadLocalsAreaFor(State state) {
            refresh();
            return areas.get(state);
        }

        @Override
        public TeleVmThread teleVmThread() {
            refresh();
            final TeleThreadLocalsArea enabledThreadLocalsArea = areas.get(Safepoint.State.ENABLED);
            if (enabledThreadLocalsArea != null) {
                final Word threadLocalValue = enabledThreadLocalsArea.getWord(VmThreadLocal.VM_THREAD);
                if (!threadLocalValue.isZero()) {
                    if (teleVM().tryLock()) {
                        try {
                            final Reference vmThreadReference = teleVM().wordToReference(threadLocalValue);
                            teleVmThread = (TeleVmThread) teleVM().makeTeleObject(vmThreadReference);
                        } finally {
                            teleVM().unlock();
                        }
                    }
                }
            }
            return teleVmThread;
        }

        @Override
        public void updateAfterGather(MemoryRegion threadLocalsRegion, int threadLocalsAreaSize) {
            if (threadLocalsRegion != null) {
                for (Safepoint.State safepointState : Safepoint.State.CONSTANTS) {
                    final Pointer tlaStartPointer = getThreadLocalsAreaStart(threadLocalsRegion, threadLocalsAreaSize, safepointState);
                    // Only create a new TeleThreadLocalsArea if the start address has changed which
                    // should only happen once going from 0 to a non-zero value.
                    final TeleThreadLocalsArea area = areas.get(safepointState);
                    if (area == null || !area.memoryRegion().start().equals(tlaStartPointer)) {
                        areas.put(safepointState, new TeleThreadLocalsArea(thread(), safepointState, tlaStartPointer));
                    }
                }
                refresh();
            }
        }

        /**
         * Gets the address of one of the three thread locals areas inside a given thread locals region.
         *
         * @param threadLocalsRegion the VM memory region containing the thread locals block
         * @param threadLocalsAreaSize the size of a thread locals area within the region
         * @param safepointState denotes which of the three thread locals areas is being requested
         * @return the address of the thread locals areas in {@code threadLocalsRegion} corresponding to {@code state}
         * @see VmThreadLocal
         */
        private Pointer getThreadLocalsAreaStart(MemoryRegion threadLocalsRegion, int threadLocalsAreaSize, Safepoint.State safepointState) {
            return threadLocalsRegion.start().plus(offsetToTriggeredThreadLocals).plus(threadLocalsAreaSize * safepointState.ordinal()).asPointer();
        }

        @Override
        public void refresh() {
            final long processEpoch = teleVM().teleProcess().epoch();
            if (lastRefreshedEpoch < processEpoch) {
                if (teleVM().tryLock()) {
                    try {
                        Trace.line(TRACE_LEVEL, tracePrefix() + "refreshThreadLocals (epoch=" + processEpoch + ") for " + this);
                        final DataAccess dataAccess = teleVM().teleProcess().dataAccess();
                        for (TeleThreadLocalsArea teleThreadLocalsArea : areas.values()) {
                            if (teleThreadLocalsArea != null) {
                                teleThreadLocalsArea.refresh(dataAccess);
                            }
                        }
                        lastRefreshedEpoch = processEpoch;
                    } finally {
                        teleVM().unlock();
                    }
                }
            }
        }

        @Override
        public void clear() {
            areas.clear();
            teleVmThread = null;
            lastRefreshedEpoch = teleVM().teleProcess().epoch();
        }
    }

    /**
     * Dummy access to the "thread locals block" of storage in the VM for a thread when that block doesn't exist.
     *
     * @author Michael Van De Vanter
     */
    protected static final class NullThreadLocals extends TeleThreadLocals {

        private static final int TRACE_LEVEL = 2;


        public NullThreadLocals(TeleNativeThread teleNativeThread) {
            super(teleNativeThread);
        }

        @Override
        public TeleThreadLocalsMemoryRegion memoryRegion() {
            return null;
        }

        @Override
        public TeleThreadLocalsArea threadLocalsAreaFor(State state) {
            return null;
        }

        @Override
        public TeleVmThread teleVmThread() {
            return null;
        }

        @Override
        public void updateAfterGather(MemoryRegion threadLocalsRegion, int tlaSize) {
        }

        @Override
        public void clear() {
        }

        @Override
        public void refresh() {
        }
    }

}
