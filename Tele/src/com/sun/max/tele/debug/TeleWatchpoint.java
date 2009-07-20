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

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleWatchpoint extends RuntimeMemoryRegion implements MaxWatchpoint {

    private static final int TRACE_VALUE = 1;

    private final Factory factory;

    /**
     * Is this watchpoint still functional?
     * True from the creation of the watchpoint until it is disposed, at which event
     * it becomes permanently false.
     */
    private boolean active = true;

    // configuration flags
    private boolean after;
    private boolean read;
    private boolean write;
    private boolean exec;
    private boolean gc;


    private byte[] teleWatchpointCache;

    /**
     * Creates a watchpoint with memory location not yet configured.
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @see RuntimeMemoryRegion#setStart(Address)
     * @see RuntimeMemoryRegion#setSize(Size)
     */
    private TeleWatchpoint(Factory factory, String description, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
        this(factory, description, Address.zero(), Size.zero(), after, read, write, exec, gc);
    }

    private TeleWatchpoint(Factory factory, String description, Address start, Size size, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
        super(start, size);
        setDescription(description);
        this.factory = factory;
        this.after = after;
        this.read = read;
        this.write = write;
        this.exec = exec;
        this.gc = gc;
    }

    private TeleWatchpoint(Factory factory, String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
        super(memoryRegion);
        setDescription(description);
        this.factory = factory;
        this.after = after;
        this.read = read;
        this.write = write;
        this.exec = exec;
        this.gc = gc;
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

    /**
     * @return whether this watchpoint is configured to trap when the watched memory is read
     */
    public boolean isRead() {
        return read;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#setRead(boolean)
     */
    public boolean setRead(boolean read) {
        ProgramError.check(active, "Attempt to set flag on disabled watchpoint");
        this.read = read;
        if (factory.ongoingGC && !gc) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    /**
     * @return whether this watchpoint is configured to trap when the watched memory is written to
     */
    public boolean isWrite() {
        return write;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#setWrite(boolean)
     */
    public boolean setWrite(boolean write) {
        ProgramError.check(active, "Attempt to set flag on disabled watchpoint");
        this.write = write;
        if (factory.ongoingGC && !gc) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    /**
     * @return whether this watchpoint is configured to trap when the watched memory is executed from.
     */
    public boolean isExec() {
        return exec;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#setExec(boolean)
     */
    public boolean setExec(boolean exec) {
        ProgramError.check(active, "Attempt to set flag on disabled watchpoint");
        this.exec = exec;
        if (factory.ongoingGC && !gc) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    public boolean isGC() {
        return gc;
    }

    public void setGC(boolean gc) {
        ProgramError.check(active, "Attempt to set flag on disabled watchpoint");
        this.gc = gc;
        if (factory.ongoingGC && !gc) {
            disable();
        } else if (factory.ongoingGC && gc) {
            reenable();
        }
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#isActive()
     */
    public boolean isActive() {
        return active;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#remove()
     */
    public boolean dispose() {
        return  factory.removeWatchpoint(this);
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#getTeleObject()
     */
    public TeleObject getTeleObject() {
        return null;
    }

    protected void updateTeleWatchpointCache(TeleProcess teleProcess) {
        if (teleWatchpointCache == null || teleWatchpointCache.length != size.toInt()) {
            teleWatchpointCache = new byte[size.toInt()];
        }
        teleWatchpointCache = teleProcess.dataAccess().readFully(start, size.toInt());
    }

    public boolean disable() {
        return factory.deactivateWatchpoint(this);
    }

    public boolean reenable() {
        return factory.activateWatchpoint(this);
    }

    /**
     * A watchpoint for a specified, fixed memory region.
     *
     */
    private static final class TeleRegionWatchpoint extends TeleWatchpoint {

        public TeleRegionWatchpoint(Factory factory, String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, memoryRegion.start(), memoryRegion.size(), after, read, write, exec, gc);
        }

        @Override
        public String toString() {
            return "TeleRegionWatchpoint" + super.toString();
        }
    }

    /**
     * A watchpoint for a whole object.
     */
    private static final class TeleObjectWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;

        public TeleObjectWatchpoint(Factory factory, String description, TeleObject teleObject, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, teleObject.getCurrentMemoryRegion(), after, read, write, exec, gc);
            this.teleObject = teleObject;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }

        @Override
        public String toString() {
            return "TeleObjectWatchpoint@" + super.toString();
        }
    }

    /**
     * A watchpoint for the memory holding an object's field.
     */
    private static final class TeleFieldWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;

        public TeleFieldWatchpoint(Factory factory, String description, TeleObject teleObject, FieldActor fieldActor, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, teleObject.getCurrentMemoryRegion(fieldActor), after, read, write, exec, gc);
            this.teleObject = teleObject;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }

        @Override
        public String toString() {
            return "TeleFieldWatchpoint@" + super.toString();
        }
    }

    /**
     *A watchpoint for the memory holding an array element.
     */
    private static final class TeleArrayElementWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;
        private final int index;

        public TeleArrayElementWatchpoint(Factory factory, String description, TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, teleObject.getCurrentOrigin().plus(arrayOffsetFromOrigin + (index * elementKind.width.numberOfBytes)), Size.fromInt(elementKind.width.numberOfBytes), after, read, write, exec, gc);
            this.teleObject = teleObject;
            this.index = index;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }

        @Override
        public String toString() {
            return "TeleArrayElementWatchpoint@" + super.toString();
        }
    }

    /**
     * A watchpoint for the memory holding an object's header field.
     */
    private static final class TeleHeaderWatchpoint extends TeleWatchpoint {

        private final TeleObject teleObject;
        private final HeaderField headerField;

        public TeleHeaderWatchpoint(Factory factory, String description, TeleObject teleObject, HeaderField headerField, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, teleObject.getCurrentMemoryRegion(headerField), after, read, write, exec, gc);
            this.teleObject = teleObject;
            this.headerField = headerField;
        }

        @Override
        public TeleObject getTeleObject() {
            return teleObject;
        }

        @Override
        public String toString() {
            return "TeleHeaderWatchpoint@" + super.toString();
        }
    }

    /**
     * A watchpoint for the memory holding a {@linkplain VmThreadLocal thread local variable}.
     * @see VmThreadLocal
     */
    private static final class TeleVmThreadLocalWatchpoint extends TeleWatchpoint {

        private final TeleThreadLocalValues teleThreadLocalValues;

        public TeleVmThreadLocalWatchpoint(Factory factory, String description, TeleThreadLocalValues teleThreadLocalValues, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description,  teleThreadLocalValues.getMemoryRegion(index), after, read, write, exec, gc);
            this.teleThreadLocalValues = teleThreadLocalValues;
        }


        @Override
        public String toString() {
            return "TeleVmThreadWatchpoint@" + super.toString();
        }
    }

    /**
     * A factory for creating and managing process watchpoints.
     * <br>
     * <b>Implementation Restriction</b>: currently limited to one set at a time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Factory extends Observable implements Comparator<TeleWatchpoint> {

        private final TeleProcess teleProcess;

        // This implementation is not thread-safe; this factory must take care of that.
        // Keep the set ordered by start address only, implemented by the comparator and equals().
        // An additional constraint imposed by this factory is that no regions overlap,
        // either in part or whole, with others in the set.
        private TreeSet<TeleWatchpoint> watchpoints = new TreeSet<TeleWatchpoint>(this);

        // A thread-safe, immutable collection of the current watchpoint list.
        // This list will be read many, many more times than it will change.
        private volatile IterableWithLength<MaxWatchpoint> watchpointsCache;

        private boolean ongoingGC = false;

        private int relocatableWatchpointsCounter = 0;

        private Address triggeredWatchpointAddress;
        private String triggeredWatchpointCode;

        public Factory(TeleProcess teleProcess) {
            this.teleProcess = teleProcess;
            updateCache();
            teleProcess.teleVM().addVMStateObserver(new TeleVMStateObserver() {

                public void upate(MaxVMState maxVMState) {
                    if (maxVMState.processState() == ProcessState.TERMINATED) {
                        watchpoints.clear();
                        updateCache();
                        setChanged();
                        notifyObservers();
                    }
                }
            });
        }

        private void updateCache() {
            watchpointsCache = new VectorSequence<MaxWatchpoint>(watchpoints);
        }

        /**
         * Does the bookkeeping of set releocatable watchpoints in our system.
         * @param watchpoint
         * @return watchpoint if creation was successful
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private TeleWatchpoint setRelocatableWatchpoint(TeleWatchpoint watchpoint) throws TooManyWatchpointsException, DuplicateWatchpointException {
            TeleWatchpoint result = null;
            try {
                result = addWatchpoint(watchpoint);
            } catch (TooManyWatchpointsException e) {
                throw e;
            } catch (DuplicateWatchpointException e) {
                throw e;
            }

            if (result != null) {
                relocatableWatchpointsCounter++;
            }
            return result;
        }

        /**
         * Creates a new watchpoint that covers a given memory region in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param memoryRegion the region of memory in the VM to be watched.
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setRegionWatchpoint(String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleRegionWatchpoint(this, description, memoryRegion, after, read, write, exec, gc);
            return addWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new watchpoint that covers an entire heap object's memory in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setObjectWatchpoint(String description, TeleObject teleObject, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleObjectWatchpoint(this, description, teleObject, after, read, write, exec, gc);
            return setRelocatableWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new watchpoint that covers a heap object's field in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param fieldActor description of a field in object of that type
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setFieldWatchpoint(String description, TeleObject teleObject, FieldActor fieldActor, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleFieldWatchpoint(this, description, teleObject, fieldActor, after, read, write, exec, gc);
            return setRelocatableWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new watchpoint that covers an element in an array in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM that contains the array
         * @param elementKind the type category of the array elements
         * @param arrayOffsetFromOrigin location relative to the object's origin of element 0 in the array
         * @param index index of the element to watch
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setArrayElementWatchpoint(String description, TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleArrayElementWatchpoint(this, description, teleObject, elementKind, arrayOffsetFromOrigin, index, after, read, after, exec, gc);
            return setRelocatableWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new watchpoint that covers a field in an object's header in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param headerField a field in the object's header
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setHeaderWatchpoint(String description, TeleObject teleObject, HeaderField headerField, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleHeaderWatchpoint(this, description, teleObject, headerField, after, read, write, exec, gc);
            return setRelocatableWatchpoint(teleWatchpoint);
        }

        /**
         * Creates a new watchpoint that covers a thread local variable in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleThreadLocalValues a set of thread local values
         * @param index identifies the particular thread local variable
         * @param after before or after watchpoint
         * @param read read watchpoint
         * @param write write watchpoint
         * @param exec execute watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setVmThreadLocalWatchpoint(String description, TeleThreadLocalValues teleThreadLocalValues, int index, boolean after, boolean read, boolean write, boolean exec, boolean gc)
            throws TooManyWatchpointsException, DuplicateWatchpointException {
            final TeleWatchpoint teleWatchpoint =
                new TeleVmThreadLocalWatchpoint(this, description, teleThreadLocalValues, index, after, read, write, exec, gc);
            return addWatchpoint(teleWatchpoint);
        }

        private TeleWatchpoint addWatchpoint(TeleWatchpoint teleWatchpoint)  throws TooManyWatchpointsException, DuplicateWatchpointException {
            teleWatchpoint.active = false;
            if (watchpoints.size() >= teleProcess.maximumWatchpointCount()) {
                throw new TooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    teleProcess.maximumWatchpointCount() + ") exceeded");
            }
            if (!watchpoints.add(teleWatchpoint)) {
                // An existing watchpoint starts at the same location
                throw new DuplicateWatchpointException("Watchpoint already exists at location: " + teleWatchpoint.start().toHexString());
            }
            // Check for possible overlaps with predecessor or successor (according to start location)
            final TeleWatchpoint lowerWatchpoint = watchpoints.lower(teleWatchpoint);
            final TeleWatchpoint higherWatchpoint = watchpoints.higher(teleWatchpoint);
            if ((lowerWatchpoint != null && lowerWatchpoint.overlaps(teleWatchpoint)) ||
                            (higherWatchpoint != null && higherWatchpoint.overlaps(teleWatchpoint))) {
                watchpoints.remove(teleWatchpoint);
                throw new DuplicateWatchpointException("Watchpoint already exists that overlaps with start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            }
            if (teleWatchpoint.isGC() || !ongoingGC) {
                if (!teleProcess.activateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Failed to create watchpoint " + teleWatchpoint.toString());
                    watchpoints.remove(teleWatchpoint);
                    return null;
                }
            } else {
                Trace.line(TRACE_VALUE, "Watchpoint deactivated during GC" + teleWatchpoint.toString());
            }

            if (relocatableWatchpointsCounter == 1) {
                final Pointer endOfGCField = teleProcess.teleVM().fields().InspectableHeapInfo_rootEpoch.staticTupleReference(teleProcess.teleVM()).toOrigin().plus(teleProcess.teleVM().fields().InspectableHeapInfo_rootEpoch.fieldActor().offset());
                teleProcess.teleVM().setRegionWatchpoint("GC End", new FixedMemoryRegion(endOfGCField, Size.fromInt(Pointer.size()), ""), true, false, true, false, true); //TODO: make it invisible
            }

            Trace.line(TRACE_VALUE, "Created watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            teleWatchpoint.active = true;
            updateCache();
            setChanged();
            notifyObservers();
            return teleWatchpoint;
        }

        /**
         * Resets an already set watchpoint.
         *
         * @param teleWatchpoint
         * @return true if reset was successful
         */
        private synchronized boolean resetWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                if (!teleProcess.activateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Failed to reset and install watchpoint at " + teleWatchpoint.start().toHexString());
                    return false;
                }
            } else {
                Trace.line(TRACE_VALUE, "Failed to reset watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }

            Trace.line(TRACE_VALUE, "Watchpoint reseted " + teleWatchpoint.start().toHexString());
            teleWatchpoint.active = true;
            updateCache();
            setChanged();
            notifyObservers();
            return true;
        }

        private synchronized boolean deactivateWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (!teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                Trace.line(TRACE_VALUE, "Failed to deactivate watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            Trace.line(TRACE_VALUE, "Watchpoint deactivated " + teleWatchpoint.start().toHexString());
            teleWatchpoint.active = true;
            updateCache();
            setChanged();
            notifyObservers();
            return true;
        }

        private synchronized boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
            if (!teleProcess.activateWatchpoint(teleWatchpoint)) {
                Trace.line(TRACE_VALUE, "Failed to activated watchpoint at " + teleWatchpoint.start().toHexString());
                return false;
            }
            Trace.line(TRACE_VALUE, "Watchpoint activated " + teleWatchpoint.start().toHexString());
            teleWatchpoint.active = true;
            updateCache();
            setChanged();
            notifyObservers();
            return true;
        }

        /**
         * Removes an active memory watchpoint from the VM.
         *
         * @param maxWatchpoint an existing watchpoint in the VM
         * @return true if successful; false if watchpoint is not active in VM.
         */
        private synchronized boolean removeWatchpoint(TeleWatchpoint teleWatchpoint) {
            ProgramError.check(teleWatchpoint.active, "Attempt to delete an already deleted watchpoint ");
            if (watchpoints.remove(teleWatchpoint)) {
                if (teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Removed watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                    teleWatchpoint.active = false;

                    if (teleWatchpoint.getTeleObject() != null) {
                        if (relocatableWatchpointsCounter == 1) {
                            final Pointer endOfGCField = teleProcess.teleVM().fields().InspectableHeapInfo_rootEpoch.staticTupleReference(teleProcess.teleVM()).
                                                            toOrigin().plus(teleProcess.teleVM().fields().InspectableHeapInfo_rootEpoch.fieldActor().offset());
                            final TeleWatchpoint endOfGCWatchpoint = (TeleWatchpoint) findWatchpoint(endOfGCField);
                            removeWatchpoint(endOfGCWatchpoint);
                            relocatableWatchpointsCounter = 0;
                        }
                    }

                    updateCache();
                    setChanged();
                    notifyObservers();
                    return true;
                } else {
                    // Can't deactivate for some reason, so put back in the active collection.
                    watchpoints.add(teleWatchpoint);
                }
            }
            Trace.line(TRACE_VALUE, "Failed to remove watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            return false;
        }

        /**
         * Updates the watchpoints of all caches.
         */
        public void updateWatchpointCaches() {
            for (TeleWatchpoint teleWatchpoint : watchpoints) {
                teleWatchpoint.updateTeleWatchpointCache(teleProcess);
            }
        }

        public void disableWatchpointsDuringGC() {
            if (!ongoingGC) {
                for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                    if (!maxWatchpoint.isGC()) {
                        maxWatchpoint.disable();
                    }
                }
                ongoingGC = true;
            }
        }

        public void reenableWatchpointsAfterGC() {
            if (ongoingGC) {
                for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                    if (!maxWatchpoint.isGC()) {
                        maxWatchpoint.reenable();
                    }
                }
                ongoingGC = false;
            }
        }

        /**
         * Finds the watchpoint which triggered a signal.
         * @return triggered watchpoint
         */
        public MaxWatchpoint findTriggeredWatchpoint() {
            triggeredWatchpointAddress = Address.fromLong(teleProcess.readWatchpointAddress());
            if (triggeredWatchpointAddress.equals(Word.zero())) {
                return null;
            }
            triggeredWatchpointCode = teleProcess.readWatchpointAccessCode();

            return findWatchpoint(triggeredWatchpointAddress);
        }

        /**
         * Returns the address which triggered the watchpoint.
         * @return
         */
        public Address getTriggeredWatchpointAddress() {
            return triggeredWatchpointAddress;
        }

        /**
         * Returns the code of the triggered watchpoint.
         * @return
         */
        public String getTriggeredWatchpointCode() {
            return triggeredWatchpointCode;
        }

        /**
         * Find an existing watchpoint set in the VM.
         * <br>
         * thread-safe
         *
         * @param address a memory address in the VM
         * @return the watchpoint whose memory region includes the address, null if none.
         */
        public MaxWatchpoint findWatchpoint(Address address) {
            for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                if (maxWatchpoint.contains(address)) {
                    return maxWatchpoint;
                }
            }
            return null;
        }

        public void lazyUpdateRelocatableWatchpoint() {
            for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                TeleObject teleObject = maxWatchpoint.getTeleObject();
                if (teleObject != null) {
                    MutableTeleGrip grip = (MutableTeleGrip) teleObject.reference().grip();
                    System.out.println(grip.toString());
                }
            }
        }

        /**
         * Find an existing watchpoint set in the VM.
         * <br>
         * thread-safe
         *
         * @param memoryRegion a memory region in the VM
         * @return the watchpoint whose memory region overlaps the specified region, null if none.
         */
        public MaxWatchpoint findWatchpoint(MemoryRegion memoryRegion) {
            for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                if (maxWatchpoint.overlaps(memoryRegion)) {
                    return maxWatchpoint;
                }
            }
            return null;
        }

        /**
         * @return all watchpoints currently set in the VM; thread-safe.
         */
        public synchronized IterableWithLength<MaxWatchpoint> watchpoints() {
            // Hand out the cached, thread-safe summary
            return watchpointsCache;
        }

        public int compare(TeleWatchpoint o1, TeleWatchpoint o2) {
            // For the purposes of the collection, define equality and comparison to be based
            // exclusively on starting address.
            return o1.start().compareTo(o2.start());
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
