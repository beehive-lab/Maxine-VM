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

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * An abstraction over memory watchpoints.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleWatchpoint extends RuntimeMemoryRegion implements VMTriggerEventHandler, MaxWatchpoint {

    /**
     * Distinguishes among uses for watchpoints,
     * independently of how the location is specified.
     */
    private enum WatchpointKind {
        /**
         * A breakpoint created on behalf of a client external to the {@link TeleVM}.  SUch
         * a watchpoint is presumed to be managed completely by the client:  creation/deletion,
         * enable/disable etc.  Only client watchpoints are visible to the client in ordinary use.
         */
        CLIENT,

        /**
         * A breakpoint created by one of the services int he {@link TeleVM}, generally in order
         * to catch certain events in the VM so that state can be synchronized for
         * some purpose.  Presumed to be managed completely by the service using it.  These
         * are generally not visible to clients.
         * <br>
         * Not relocatable.
         */
        SYSTEM;
    }

    private static final int TRACE_VALUE = 1;

    private final WatchpointKind kind;

    /**
     * Watchpoints factory.
     */
    private final Factory factory;

    /**
     * Is this watchpoint still alive (not yet disposed)?
     * This is true from the creation of the watchpoint until it is disposed, at which event
     * it becomes permanently false.
     */
    private boolean alive = true;

    /**
     * Watchpoint configuration flags.
     */
    private boolean after;
    private boolean trapOnRead = false;
    private boolean trapOnWrite = false;
    private boolean trapOnExec = false;
    private boolean isEnabledDuringGC = false;

    /**
     * Stores old data of fields covered by watchpoint.
     */
    private byte[] memoryCache;

    /**
     * The relocation algorithm for relocatable watchpoints.
     * true = eager, false = lazy
     */
    private boolean eagerRelocationUpdate = false;

    /**
     * Used for watchpoints set on object fields.
     * Memory offset in byte from object start address.
     */
    protected long teleObjectStartAddressOffset = 0;

    private VMTriggerEventHandler triggerEventHandler = VMTriggerEventHandler.Static.ALWAYS_TRUE;

    private TeleWatchpoint(WatchpointKind kind, Factory factory, String description, Address start, Size size, boolean after) {
        super(start, size);
        setDescription(description);
        this.kind = kind;
        this.factory = factory;
        this.after = after;
    }

    private TeleWatchpoint(WatchpointKind kind, Factory factory, String description, MemoryRegion memoryRegion, boolean after) {
        super(memoryRegion);
        setDescription(description);
        this.kind = kind;
        this.factory = factory;
        this.after = after;
    }

    public String getDescription() {
        return description();
    }

    @Override
    public boolean equals(Object o) {
        // For the purposes of the collection, define ordering and equality in terms of start location only.
        if (o instanceof TeleWatchpoint) {
            final TeleWatchpoint teleWatchpoint = (TeleWatchpoint) o;
            return start().equals(teleWatchpoint.start());
        }
        return false;
    }

    public boolean isTrapOnRead() {
        return trapOnRead;
    }

    public boolean setTrapOnRead(boolean read) {
        ProgramError.check(alive, "Attempt to set flag on disabled watchpoint");
        this.trapOnRead = read;
        if (factory.isInGC() && !isEnabledDuringGC) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    public boolean isTrapOnWrite() {
        return trapOnWrite;
    }

    public boolean setTrapOnWrite(boolean write) {
        ProgramError.check(alive, "Attempt to set flag on disabled watchpoint");
        this.trapOnWrite = write;
        if (factory.isInGC() && !isEnabledDuringGC) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    public boolean isTrapOnExec() {
        return trapOnExec;
    }

    public boolean setTrapOnExec(boolean exec) {
        ProgramError.check(alive, "Attempt to set flag on disabled watchpoint");
        this.trapOnExec = exec;
        if (factory.isInGC() && !isEnabledDuringGC) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    public boolean isEnabledDuringGC() {
        return isEnabledDuringGC;
    }

    public void setEnabledDuringGC(boolean isEnabledDuringGC) {
        ProgramError.check(alive, "Attempt to set flag on disabled watchpoint");
        this.isEnabledDuringGC = isEnabledDuringGC;
        if (factory.isInGC()) {
            setActive(isEnabledDuringGC);
        }
    }

    public boolean isEnabled() {
        return alive && (trapOnRead || trapOnWrite || trapOnExec);
    }

    public boolean dispose() {
        return factory.removeWatchpoint(this);
    }

    public TeleObject getTeleObject() {
        return null;
    }

    private long getTeleObjectStartAddressOffset() {
        return teleObjectStartAddressOffset;
    }

    public void setEagerRelocationUpdate(boolean eagerRelocationUpdate) {
        this.eagerRelocationUpdate = eagerRelocationUpdate;
        if (getTeleObject() != null) {
            TeleWatchpoint cardTableWatchpoint = null;
            final TeleVM teleVM = factory.teleProcess.teleVM();
            int index = InspectableHeapInfo.getCardTableIndex(start, teleVM.teleHeapRegionsArray());
            int objectReferences = teleVM.readCardTableEntry(index);
            Address cardTableAddress = teleVM.getCardTableAddress(index);
            if (eagerRelocationUpdate) {
                if (objectReferences > 0) {
                    cardTableWatchpoint = (TeleWatchpoint) factory.findSystemWatchpoint(cardTableAddress);
                    cardTableWatchpoint.setActive(false);
                } else {
                    try {
                        cardTableWatchpoint = factory.createCardTableWatchpoint(cardTableAddress);
                    } catch (TooManyWatchpointsException e) {
                        ProgramError.unexpected("Can't create card table watchpoitn");
                    }
                }
                objectReferences++;
                teleVM.writeCardTableEntry(index, objectReferences);
                cardTableWatchpoint.setActive(true);
            } else {
                cardTableWatchpoint = (TeleWatchpoint) factory.findSystemWatchpoint(cardTableAddress);
                cardTableWatchpoint.setActive(false);
                objectReferences--;
                teleVM.writeCardTableEntry(index, objectReferences);
                if (objectReferences > 0) {
                    cardTableWatchpoint.setActive(true);
                } else {
                    cardTableWatchpoint.dispose();
                }
            }
        }
    }

    public boolean isEagerRelocationUpdateSet() {
        return eagerRelocationUpdate;
    }

    /**
     * Future usage: e.g. for conditional Watchpoints
     * @param teleProcess
     */
    protected void updateMemoryCache(TeleProcess teleProcess) {
        if (memoryCache == null || memoryCache.length != size.toInt()) {
            memoryCache = new byte[size.toInt()];
        }
        try {
            memoryCache = teleProcess.dataAccess().readFully(start, size.toInt());
        } catch (DataIOError e) {
            // Must be a watchpoint in an address space that doesn't (yet?) exist in the VM process.
            memoryCache = null;
        }
    }

    private boolean setActive(boolean active) {
        if (active) {
            return factory.activateWatchpoint(this);
        } else {
            return factory.deactivateWatchpoint(this);
        }
    }

    /**
     * Assigns to this watchpoint a  handler for events triggered by this watchpoint.  A null handler
     * is equivalent to there being no handling action and a return of true (VM execution should halt).
     *
     * @param triggerEventHandler handler for VM execution events triggered by this watchpoint.
     */
    protected void setTriggerEventHandler(VMTriggerEventHandler triggerEventHandler) {
        this.triggerEventHandler =
            (triggerEventHandler == null) ? VMTriggerEventHandler.Static.ALWAYS_TRUE : triggerEventHandler;
    }

    public final boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
        assert teleNativeThread.state() == TeleNativeThread.ThreadState.WATCHPOINT;
        if (factory.teleProcess.teleVM().isInGC()) {
            factory.setInGC(true);
            if (!isEnabledDuringGC()) {
                return false;
            }
        }
        return triggerEventHandler.handleTriggerEvent(teleNativeThread);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{").append(kind.toString());
        sb.append(", ").append(isEnabled() ? "enabled " : "disabled ");
        sb.append(", 0x").append(start().toHexString());
        sb.append(", size=").append(size().toString());
        sb.append(", \"").append(getDescription()).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * A watchpoint for a specified, fixed memory region.
     */
    private static final class TeleRegionWatchpoint extends TeleWatchpoint {

        public TeleRegionWatchpoint(WatchpointKind kind, Factory factory, String description, MemoryRegion memoryRegion, boolean after) {
            super(kind, factory, description, memoryRegion.start(), memoryRegion.size(), after);
        }
    }

    /**
     * A watchpoint for a whole object.
     */
    private static final class TeleObjectWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;

        public TeleObjectWatchpoint(WatchpointKind kind, Factory factory, String description, TeleObject teleObject, boolean after) {
            super(kind, factory, description, teleObject.getCurrentMemoryRegion(), after);
            this.teleObject = teleObject;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }
    }

    /**
     * A watchpoint for the memory holding an object's field.
     */
    private static final class TeleFieldWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;

        public TeleFieldWatchpoint(WatchpointKind kind, Factory factory, String description, TeleObject teleObject, FieldActor fieldActor, boolean after) {
            super(kind, factory, description, teleObject.getCurrentMemoryRegion(fieldActor), after);
            this.teleObject = teleObject;
            teleObjectStartAddressOffset = teleObject.getCurrentMemoryRegion(fieldActor).start().minus(teleObject.getCurrentMemoryRegion().start()).toLong();
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }
    }

    /**
     *A watchpoint for the memory holding an array element.
     */
    private static final class TeleArrayElementWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;
        private final int index;

        public TeleArrayElementWatchpoint(WatchpointKind kind, Factory factory, String description, TeleObject teleObject, Kind elementKind, Offset arrayOffsetFromOrigin, int index, boolean after) {
            super(kind, factory, description, teleObject.getCurrentOrigin().plus(arrayOffsetFromOrigin.plus(index * elementKind.width.numberOfBytes)), Size.fromInt(elementKind.width.numberOfBytes), after);
            this.teleObject = teleObject;
            this.index = index;
            teleObjectStartAddressOffset = teleObject.getCurrentOrigin().plus(arrayOffsetFromOrigin.plus(index * elementKind.width.numberOfBytes)).minus(teleObject.getCurrentMemoryRegion().start()).toLong();
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }
    }

    /**
     * A watchpoint for the memory holding an object's header field.
     */
    private static final class TeleHeaderWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;
        private final HeaderField headerField;

        public TeleHeaderWatchpoint(WatchpointKind kind, Factory factory, String description, TeleObject teleObject, HeaderField headerField, boolean after) {
            super(kind, factory, description, teleObject.getCurrentMemoryRegion(headerField), after);
            this.teleObject = teleObject;
            this.headerField = headerField;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }
    }

    /**
     * A watchpoint for the memory holding a {@linkplain VmThreadLocal thread local variable}.
     * @see VmThreadLocal
     */
    private static final class TeleVmThreadLocalWatchpoint extends TeleWatchpoint {

        private final TeleThreadLocalValues teleThreadLocalValues;

        public TeleVmThreadLocalWatchpoint(WatchpointKind kind, Factory factory, String description, TeleThreadLocalValues teleThreadLocalValues, int index, boolean after) {
            super(kind, factory,  description, teleThreadLocalValues.getMemoryRegion(index), after);
            this.teleThreadLocalValues = teleThreadLocalValues;
        }
    }

    /**
     * A factory for creating and managing process watchpoints.
     * <br>
     * <b>Implementation Restriction</b>: currently limited to one set at a time.
     * <br>
     * Overlapping watchpoints are not permitted.
     *
     * @author Michael Van De Vanter
     */
    public static final class Factory extends Observable {

        private final TeleProcess teleProcess;

        private final Comparator<TeleWatchpoint> watchpointComparitor = new Comparator<TeleWatchpoint>() {

            public int compare(TeleWatchpoint o1, TeleWatchpoint o2) {
                // For the purposes of the collection, define equality and comparison to be based
                // exclusively on starting address.
                return o1.start().compareTo(o2.start());
            }
        };

        // This implementation is not thread-safe; this factory must take care of that.
        // Keep the set ordered by start address only, implemented by the comparator and equals().
        // An additional constraint imposed by this factory is that no regions overlap,
        // either in part or whole, with others in the set.
        private final TreeSet<TeleWatchpoint> clientWatchpoints = new TreeSet<TeleWatchpoint>(watchpointComparitor);

        // A thread-safe, immutable collection of the current watchpoint list.
        // This list will be trapOnRead many, many more times than it will change.
        private volatile IterableWithLength<MaxWatchpoint> clientWatchpointsCache;

        // Watchpoints used for internal purposes, for example for GC and relocation services
        private final TreeSet<TeleWatchpoint> systemWatchpoints = new TreeSet<TeleWatchpoint>(watchpointComparitor);
        private volatile IterableWithLength<MaxWatchpoint> systemWatchpointsCache;

        // Is VM known to be in GC?
        private boolean inGC = false;

        /**
         * The number of watchpoints that are currently relocatable.
         */
        private int relocatableWatchpointsCounter = 0;

        // End of GC watchpoint; used in lazy watchpoint relocation algorithm
        private TeleWatchpoint endOfGCWatchpoint;

        public Factory(TeleProcess teleProcess) {
            this.teleProcess = teleProcess;
            updateCaches();
            teleProcess.teleVM().addVMStateObserver(new TeleVMStateObserver() {

                public void upate(MaxVMState maxVMState) {
                    if (maxVMState.processState() == ProcessState.TERMINATED) {
                        clientWatchpoints.clear();
                        systemWatchpoints.clear();
                        updateCaches();
                        setChanged();
                        notifyObservers();
                    }
                }
            });
        }

        public void initFactory() {
            try {
                endOfGCWatchpoint = createGCEndWatchpoint();
            } catch (TooManyWatchpointsException e) {
                ProgramError.unexpected("Can't create GC (system) watchpoint");
            }
        }

        /**
         * Updates the watchpoint caches of memory contents.
         */
        public void updateWatchpointMemoryCaches() {
            for (TeleWatchpoint teleWatchpoint : clientWatchpoints) {
                teleWatchpoint.updateMemoryCache(teleProcess);
            }
        }

        /**
         * Creates a new, active watchpoint that covers a given memory region in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param memoryRegion the region of memory in the VM to be watched.
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createRegionWatchpoint(String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, description, memoryRegion, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new, active watchpoint that covers an entire heap object's memory in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createObjectWatchpoint(String description, TeleObject teleObject, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleObjectWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new, active watchpoint that covers a heap object's field in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param fieldActor description of a field in object of that type
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createFieldWatchpoint(String description, TeleObject teleObject, FieldActor fieldActor, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleFieldWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, fieldActor, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new, active watchpoint that covers an element in an array in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM that contains the array
         * @param elementKind the type category of the array elements
         * @param arrayOffsetFromOrigin location relative to the object's origin of element 0 in the array
         * @param index index of the element to watch
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createArrayElementWatchpoint(String description, TeleObject teleObject, Kind elementKind, Offset arrayOffsetFromOrigin, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleArrayElementWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, elementKind, arrayOffsetFromOrigin, index, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new, active watchpoint that covers a field in an object's header in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param headerField a field in the object's header
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createHeaderWatchpoint(String description, TeleObject teleObject, HeaderField headerField, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleHeaderWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, headerField, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new, active watchpoint that covers a thread local variable in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleThreadLocalValues a set of thread local values
         * @param index identifies the particular thread local variable
         * @param after before or after watchpoint
         * @param read trapOnRead watchpoint
         * @param write trapOnWrite watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint createVmThreadLocalWatchpoint(String description, TeleThreadLocalValues teleThreadLocalValues, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint = new TeleVmThreadLocalWatchpoint(WatchpointKind.CLIENT, this, description, teleThreadLocalValues, index, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addClientWatchpoint(teleWatchpoint);
        }

        /**
         * Find an existing client watchpoint set in the VM.
         *
         * @param address a memory address in the VM
         * @return the watchpoint whose memory region includes the address, null if none.
         */
        public synchronized TeleWatchpoint findClientWatchpoint(Address address) {
            for (TeleWatchpoint teleWatchpoint : clientWatchpoints) {
                if (teleWatchpoint.contains(address)) {
                    return teleWatchpoint;
                }
            }
            return null;
        }

        /**
         * Find existing client memory watchpoints in the VM by location.
         * <br>
         * thread-safe
         *
         * @param memoryRegion a memory region in the VM
         * @return all watchpoints whose memory regions overlap the specified region, empty sequence if none.
         */
        public Sequence<MaxWatchpoint> findClientWatchpoints(MemoryRegion memoryRegion) {
            DeterministicSet<MaxWatchpoint> watchpoints = DeterministicSet.Static.empty(MaxWatchpoint.class);
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                if (maxWatchpoint.overlaps(memoryRegion)) {
                    if (watchpoints.isEmpty()) {
                        watchpoints = new DeterministicSet.Singleton<MaxWatchpoint>(maxWatchpoint);
                    } else if (watchpoints.length() == 1) {
                        GrowableDeterministicSet<MaxWatchpoint> newSet = new LinkedIdentityHashSet<MaxWatchpoint>(watchpoints.first());
                        newSet.add(maxWatchpoint);
                        watchpoints = newSet;
                    } else {
                        final GrowableDeterministicSet<MaxWatchpoint> growableSet = (GrowableDeterministicSet<MaxWatchpoint>) watchpoints;
                        growableSet.add(maxWatchpoint);
                    }
                }
            }
            return watchpoints;
        }

        /**
         * @return all watchpoints currently set in the VM; thread-safe.
         */
        public IterableWithLength<MaxWatchpoint> clientWatchpoints() {
            // Hand out the cached, thread-safe summary
            return clientWatchpointsCache;
        }

        /**
         * Creates a new, inactive system watchpoint. This watchpoint is not shown in the list of current watchpoints.
         * This watchpoint has to be explicitly activated.
         *
         * @param description
         * @param memoryRegion
         * @param after
         * @param read
         * @param write
         * @param exec
         * @param gc
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException
         *
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized TeleWatchpoint createSystemWatchpoint(String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc) throws TooManyWatchpointsException {
            final TeleWatchpoint teleWatchpoint = new TeleRegionWatchpoint(WatchpointKind.SYSTEM, this, description, memoryRegion, after);
            teleWatchpoint.setTrapOnRead(read);
            teleWatchpoint.setTrapOnWrite(write);
            teleWatchpoint.setTrapOnExec(exec);
            teleWatchpoint.setEnabledDuringGC(gc);
            return addSystemWatchpoint(teleWatchpoint);
        }

        /**
         * Find an system watchpoint set at a particular location.
         *
         * @param address a location in VM memory
         * @return a system watchpoint, null if none exists at the address.
         */
        private MaxWatchpoint findSystemWatchpoint(Address address) {
            for (MaxWatchpoint maxWatchpoint : systemWatchpointsCache) {
                if (maxWatchpoint.contains(address)) {
                    return maxWatchpoint;
                }
            }
            return null;
        }

        /**
         * Creates a system memory watchpoint that triggers when each GC concludes.
         * <br>
         * When triggered, handler does any lazy watchpoint relocations.
         *
         * @return a newly created system watchpoint, set on the counter that gets incremented at the conclusion of each GC.
         * @throws TooManyWatchpointsException
         */
        private TeleWatchpoint createGCEndWatchpoint() throws TooManyWatchpointsException {
            FixedMemoryRegion memoryRegion = new FixedMemoryRegion(teleProcess.teleVM().rootEpochAddress(), Size.fromInt(Pointer.size()), "Root epoch counter address");
            final TeleWatchpoint teleWatchpoint = createSystemWatchpoint("End of GC", memoryRegion, true, false, true, false, true);
            teleWatchpoint.setTriggerEventHandler(new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    try {
                        lazyUpdateRelocatableWatchpoints();
                    } catch (TooManyWatchpointsException exception) {
                        ProgramError.unexpected("Handling watchpoint trigger event: " + exception);
                    } catch (DuplicateWatchpointException exception) {
                        ProgramError.unexpected("Handling watchpoint trigger event: " + exception);
                    }
                    setInGC(false);
                    return false;
                }
            });
            return teleWatchpoint;
        }


        /**
         * Creates a system memory watchpoint on a location in the card table.
         *
         * @param cardTableAddress
         * @return a newly created system watchpoint
         * @throws TooManyWatchpointsException
         */
        private TeleWatchpoint createCardTableWatchpoint(Address cardTableAddress) throws TooManyWatchpointsException {
            final TeleVM teleVM = teleProcess.teleVM();
            FixedMemoryRegion memoryRegion = new FixedMemoryRegion(cardTableAddress, Size.fromInt(Word.size()), "Card table entry");
            TeleWatchpoint cardTableWatchpoint = createSystemWatchpoint("Card table watchpoint", memoryRegion, true, true, false, false, true);
            cardTableWatchpoint.setTriggerEventHandler(new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    if (teleVM.isInGC()) {
                        // Handle watchpoint triggered in card table
                        Address objectOldAddress = teleVM.getObjectOldAddress();
                        try {
                            return !relocateCardTableWatchpoint(objectOldAddress, teleVM.getObjectNewAddress());
                        } catch (TooManyWatchpointsException exception) {
                            ProgramError.unexpected("Handling watchpoint trigger event: " + exception);
                        } catch (DuplicateWatchpointException exception) {
                            ProgramError.unexpected("Handling watchpoint trigger event: " + exception);
                        }
                    }
                    return false;
                }
            });
            return cardTableWatchpoint;
        }

        /**
         * @return total number of existing watchpoints of all kinds.[
         */
        private int watchpointCount() {
            return clientWatchpoints.size() + systemWatchpoints.size();
        }

        private boolean isInGC() {
            return inGC;
        }

        private void updateCaches() {
            clientWatchpointsCache = new VectorSequence<MaxWatchpoint>(clientWatchpoints);
            systemWatchpointsCache = new VectorSequence<MaxWatchpoint>(systemWatchpoints);
        }

        /**
         * Adds a watchpoint to the list of current client watchpoints, and activates this watchpoint.
         *
         * @param teleWatchpoint the new client watchpoint, presumed not to have been added before.
         * @return the watchpoint, null if failed to create for some reason
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private TeleWatchpoint addClientWatchpoint(TeleWatchpoint teleWatchpoint)  throws TooManyWatchpointsException, DuplicateWatchpointException {
            assert teleWatchpoint.kind == WatchpointKind.CLIENT;
            teleWatchpoint.alive = false;
            if (watchpointCount() >= teleProcess.maximumWatchpointCount()) {
                throw new TooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    teleProcess.maximumWatchpointCount() + ") exceeded");
            }
            if (!clientWatchpoints.add(teleWatchpoint)) {
                // An existing watchpoint starts at the same location
                throw new DuplicateWatchpointException("Watchpoint already exists at location: " + teleWatchpoint.start().toHexString());
            }
            // Check for possible overlaps with predecessor or successor (according to start location)
            final TeleWatchpoint lowerWatchpoint = clientWatchpoints.lower(teleWatchpoint);
            final TeleWatchpoint higherWatchpoint = clientWatchpoints.higher(teleWatchpoint);
            if ((lowerWatchpoint != null && lowerWatchpoint.overlaps(teleWatchpoint)) ||
                            (higherWatchpoint != null && higherWatchpoint.overlaps(teleWatchpoint))) {
                clientWatchpoints.remove(teleWatchpoint);
                throw new DuplicateWatchpointException("Watchpoint already exists that overlaps with start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            }
            if (teleWatchpoint.isEnabledDuringGC() || !isInGC()) {
                if (!teleWatchpoint.setActive(true)) {
                    clientWatchpoints.remove(teleWatchpoint);
                    return null;
                }
            } else {
                Trace.line(TRACE_VALUE, "Watchpoint deactivated during GC " + teleWatchpoint.toString());
            }

            Trace.line(TRACE_VALUE, "Created watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            teleWatchpoint.alive = true;

            if (teleWatchpoint.getTeleObject() != null) {
                relocatableWatchpointsCounter++;
                if (relocatableWatchpointsCounter == 1) {
                    endOfGCWatchpoint.setActive(true);
                }
            }

            updateCaches();
            setChanged();
            notifyObservers();
            return teleWatchpoint;
        }

        /**
         * Add a system watchpoint, assumed to be newly created.
         * <br>Does <strong>not</strong> activate the watchpoint.
         * <br>Does <strong>not</strong> check for overlap with existing watchpoints.
         * <br>Does <strong>not</strong> notify observers of change.
         *
         * @param teleWatchpoint
         * @return the watchpoint
         * @throws TooManyWatchpointsException
         */
        private TeleWatchpoint addSystemWatchpoint(TeleWatchpoint teleWatchpoint) throws TooManyWatchpointsException {
            if (watchpointCount() >= teleProcess.maximumWatchpointCount()) {
                throw new TooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    teleProcess.maximumWatchpointCount() + ") exceeded");
            }
            systemWatchpoints.add(teleWatchpoint);
            updateCaches();
            return teleWatchpoint;
        }

        /**
         * Removes an alive memory watchpoint from the VM.
         * <br>
         * Notifies observers if a client watchpoint.
         *
         * @param teleWatchpoint an existing watchpoint in the VM
         * @return true if successful; false if watchpoint is not alive in VM.
         */
        private synchronized boolean removeWatchpoint(TeleWatchpoint teleWatchpoint) {
            ProgramError.check(teleWatchpoint.alive, "Attempt to delete an already deleted watchpoint ");
            switch(teleWatchpoint.kind) {
                case CLIENT: {
                    if (clientWatchpoints.remove(teleWatchpoint)) {
                        if (teleWatchpoint.setActive(false)) {
                            Trace.line(TRACE_VALUE, "Removed watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                            teleWatchpoint.alive = false;
                            if (teleWatchpoint.getTeleObject() != null) {
                                relocatableWatchpointsCounter--;
                                if (relocatableWatchpointsCounter == 0) {
                                    endOfGCWatchpoint.setActive(false);
                                }
                            }
                            updateCaches();
                            setChanged();
                            notifyObservers();
                            return true;
                        } else {
                            // Can't deactivate for some reason, so put back in the alive collection.
                            clientWatchpoints.add(teleWatchpoint);
                        }
                    }
                    break;
                }
                case SYSTEM: {
                    if (systemWatchpoints.remove(teleWatchpoint)) {
                        if (teleWatchpoint.setActive(false)) {
                            Trace.line(TRACE_VALUE, "Removed system watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                            teleWatchpoint.alive = false;
                            updateCaches();
                            return true;
                        } else {
                            // Can't deactivate for some reason, so put back in the alive collection.
                            systemWatchpoints.add(teleWatchpoint);
                        }
                    }
                    break;
                }
                default:
                    ProgramError.unknownCase();
            }
            Trace.line(TRACE_VALUE, "Failed to remove watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            return false;
        }

        /**
         * Activates a watchpoint.
         * @param teleWatchpoint
         * @return true if succeeded
         */
        private synchronized boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (!teleProcess.activateWatchpoint(teleWatchpoint)) {
                Trace.line(TRACE_VALUE, "Failed to activated watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            Trace.line(TRACE_VALUE, "Watchpoint activated " + teleWatchpoint.start().toHexString());
            return true;
        }

        /**
         * Deactivates a watchpoint, but keeps it in the watchpoints list.
         * @param teleWatchpoint
         * @return true if succeeded
         */
        private synchronized boolean deactivateWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (!teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                Trace.line(TRACE_VALUE, "Failed to deactivate watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            Trace.line(TRACE_VALUE, "Watchpoint deactivated " + teleWatchpoint.start().toHexString());
            return true;
        }

        /**
         * Resets an already set watchpoint.
         *
         * @param teleWatchpoint
         * @return true if reset was successful
         */
        private synchronized boolean resetWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (!teleWatchpoint.setActive(false)) {
                Trace.line(TRACE_VALUE, "Failed to reset watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            if (!teleWatchpoint.setActive(true)) {
                Trace.line(TRACE_VALUE, "Failed to reset and install watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            Trace.line(TRACE_VALUE, "Watchpoint reset " + teleWatchpoint.start().toHexString());
            teleWatchpoint.alive = true;
            updateCaches();
            setChanged();
            notifyObservers();
            return true;
        }

        /**
         * Relocates a watchpoint to the new address of the object.
         *
         * @param teleWatchpoint
         * @return true when relocation of watchpoint succeeded
         *
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized boolean relocateWatchpoint(TeleWatchpoint teleWatchpoint) throws TooManyWatchpointsException, DuplicateWatchpointException {
            ProgramError.check(teleWatchpoint.alive, "Attempt to relocate an already deleted watchpoint ");
            TeleObject teleObject = teleWatchpoint.getTeleObject();
            if (teleObject != null) {
                if (teleObject.isLive()) {
                    Address newAddress = teleObject.getCurrentMemoryRegion().start().plus(teleWatchpoint.getTeleObjectStartAddressOffset());

                    if (teleWatchpoint.dispose()) {
                        teleWatchpoint.setStart(newAddress);
                        if (addClientWatchpoint(teleWatchpoint) != null) {
                            return true;
                        } else {
                            // Can't store relocated watchpoint in watchpoints list.
                            Trace.line(TRACE_VALUE, "Failed to store relocated watchpoint in watchpoints list start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                        }
                    }
                    Trace.line(TRACE_VALUE, "Failed to relocate watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                } else if (teleObject.isObsolete()) {
                    Address newAddress = teleObject.getForwardedMemoryRegion().start().plus(teleWatchpoint.getTeleObjectStartAddressOffset());

                    if (teleWatchpoint.dispose()) {
                        teleWatchpoint.setStart(newAddress);
                        if (addClientWatchpoint(teleWatchpoint) != null) {
                            return true;
                        } else {
                            // Can't store relocated watchpoint in watchpoints list.
                            Trace.line(TRACE_VALUE, "Failed to store relocated watchpoint in watchpoints list start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                        }
                    }
                    Trace.line(TRACE_VALUE, "Failed to relocate watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());

                } else {
                    if (teleWatchpoint.dispose()) {
                        TeleWatchpoint teleRegionWatchpoint = createRegionWatchpoint("RegionWatchpoint - GC removed corresponding Object", new FixedMemoryRegion(teleWatchpoint.start(), teleWatchpoint.size(), "Old memory location of watched object"),
                            teleWatchpoint.after, teleWatchpoint.trapOnRead, teleWatchpoint.trapOnWrite, teleWatchpoint.trapOnExec, teleWatchpoint.isEnabledDuringGC);
                        if (teleRegionWatchpoint != null) {
                            return true;
                        } else {
                            Trace.line(TRACE_VALUE, "Failed to create RegionWatchpoint for outdated TeleWatchpoint=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                        }
                    }
                    Trace.line(TRACE_VALUE, "Failed to handle outdated watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                }
            }
            return false;
        }

        /**
         * Relocates a watchpoint to its new given address.
         * @param teleWatchpoint
         * @param newAddress
         * @return true if relocation was successful
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized boolean relocateWatchpoint(TeleWatchpoint teleWatchpoint, Address newAddress) throws TooManyWatchpointsException, DuplicateWatchpointException {
            ProgramError.check(teleWatchpoint.alive, "Attempt to relocate an already deleted watchpoint ");
            TeleObject teleObject = teleWatchpoint.getTeleObject();
            if (teleObject != null) {
                if (teleObject.isLive()) {
                    if (teleWatchpoint.dispose()) {
                        Address newWatchpointAddress = newAddress.plus(teleWatchpoint.getTeleObjectStartAddressOffset());
                        teleWatchpoint.setStart(newWatchpointAddress);
                        if (addClientWatchpoint(teleWatchpoint) != null) {
                            return true;
                        } else {
                            // Can't store relocated watchpoint in watchpoints list.
                            Trace.line(TRACE_VALUE, "Failed to store relocated watchpoint in watchpoints list start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                        }
                    }
                    Trace.line(TRACE_VALUE, "Failed to relocate watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                }
            }
            return false;
        }

        /**
         * Relocates a card table watchpoint and its corresponding watchpoint.
         * @param oldAddress address of watched memory area (from-space)
         * @param newAddress address of new memory area (to-space)
         * @return true if relocation was successful
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized boolean relocateCardTableWatchpoint(Address oldAddress, Address newAddress) throws TooManyWatchpointsException, DuplicateWatchpointException {
            TeleWatchpoint cardTableWatchpoint;
            final int oldIndex = InspectableHeapInfo.getCardTableIndex(oldAddress, teleProcess.teleVM().teleHeapRegionsArray());
            final int newIndex = InspectableHeapInfo.getCardTableIndex(newAddress, teleProcess.teleVM().teleHeapRegionsArray());
            if (oldIndex == newIndex) {
                return false;
            }

            int oldObjectReferences = teleProcess.teleVM().readCardTableEntry(oldIndex);
            final Address oldCardTableAddress = teleProcess.teleVM().getCardTableAddress(oldIndex);
            cardTableWatchpoint = (TeleWatchpoint) findSystemWatchpoint(oldCardTableAddress);
            if (cardTableWatchpoint == null) {
                return false;
            }
            oldObjectReferences--;
            if (oldObjectReferences == 0) {
                cardTableWatchpoint.dispose();
            } else {
                cardTableWatchpoint.setActive(false);
            }
            teleProcess.teleVM().writeCardTableEntry(oldIndex, oldObjectReferences);
            if (oldObjectReferences > 0) {
                cardTableWatchpoint.setActive(true);
            }

            int newObjectReferences = teleProcess.teleVM().readCardTableEntry(newIndex);
            final Address newCardTableAddress = teleProcess.teleVM().getCardTableAddress(newIndex);
            if (newObjectReferences == 0) {
                cardTableWatchpoint = createCardTableWatchpoint(newCardTableAddress);
            } else {
                cardTableWatchpoint = (TeleWatchpoint) findSystemWatchpoint(newCardTableAddress);
                cardTableWatchpoint.setActive(false);
            }
            newObjectReferences++;
            teleProcess.teleVM().writeCardTableEntry(newIndex, newObjectReferences);
            cardTableWatchpoint.setActive(true);

            // finally move watchpoint
            updateRelocatableWatchpoint(oldAddress, newAddress);

            return true;
        }

        /**
         * Notifies the factory of the VM GC state.
         *
         * @param inGC whether the VM is currently in GC.
         */
        private void setInGC(boolean inGC) {
            if (inGC != this.inGC) {
                this.inGC = inGC;
                for (TeleWatchpoint teleWatchpoint : clientWatchpoints) {
                    if (!teleWatchpoint.isEnabledDuringGC()) {
                        teleWatchpoint.setActive(!inGC);
                    }
                }
            }
        }

        /**
         * Updates all relocatable watchpoint in a lazy way.
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private void lazyUpdateRelocatableWatchpoints() throws TooManyWatchpointsException, DuplicateWatchpointException {
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                relocateWatchpoint((TeleWatchpoint) maxWatchpoint);
            }
        }

        /**
         * Updates a single relocatable watchpoint.
         * @param oldAddress
         * @param newAddress
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private void updateRelocatableWatchpoint(Address oldAddress, Address newAddress) throws TooManyWatchpointsException, DuplicateWatchpointException {
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                if (maxWatchpoint.start().equals(oldAddress)) {
                    relocateWatchpoint((TeleWatchpoint) maxWatchpoint, newAddress);
                }
            }
        }

        /**
         * Update relocatable watchpoints of a given memory range.
         * @param start
         * @param end
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private void updateRelocatableWatchpoints(Address start, Address end) throws TooManyWatchpointsException, DuplicateWatchpointException {
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                if (maxWatchpoint.start().greaterEqual(start) && end.greaterEqual(maxWatchpoint.start())) {
                    relocateWatchpoint((TeleWatchpoint) maxWatchpoint);
                }
            }
        }

        public void writeSummaryToStream(PrintStream printStream) {
            printStream.println("Watchpoints :");
            for (TeleWatchpoint teleWatchpoint : clientWatchpoints) {
                printStream.println("  " + teleWatchpoint.toString());

            }
            for (TeleWatchpoint teleWatchpoint : systemWatchpoints) {
                printStream.println("  " + teleWatchpoint.toString());
            }
        }

    }

    public static class TooManyWatchpointsException extends MaxException {
        TooManyWatchpointsException(String message) {
            super(message);
        }
    }

    public static class DuplicateWatchpointException extends MaxException {
        DuplicateWatchpointException(String message) {
            super(message);
        }
    }

}
