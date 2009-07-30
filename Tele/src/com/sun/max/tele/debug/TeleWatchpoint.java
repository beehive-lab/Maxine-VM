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
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.tele.*;
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
    private boolean isEnabledDuringGC;

    private byte[] teleWatchpointCache;

    private boolean eagerRelocationUpdate = false;

    /**
     * Creates a watchpoint with memory location not yet configured.
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @see RuntimeMemoryRegion#setStart(Address)
     * @see RuntimeMemoryRegion#setSize(Size)
     */
    private TeleWatchpoint(Factory factory, String description, boolean after, boolean read, boolean write, boolean exec, boolean isEnabledDuringGC) {
        this(factory, description, Address.zero(), Size.zero(), after, read, write, exec, isEnabledDuringGC);
    }

    private TeleWatchpoint(Factory factory, String description, Address start, Size size, boolean after, boolean read, boolean write, boolean exec, boolean isEnabledDuringGC) {
        super(start, size);
        setDescription(description);
        this.factory = factory;
        this.after = after;
        this.read = read;
        this.write = write;
        this.exec = exec;
        this.isEnabledDuringGC = isEnabledDuringGC;
    }

    private TeleWatchpoint(Factory factory, String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean isEnabledDuringGC) {
        super(memoryRegion);
        setDescription(description);
        this.factory = factory;
        this.after = after;
        this.read = read;
        this.write = write;
        this.exec = exec;
        this.isEnabledDuringGC = isEnabledDuringGC;
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
        if (factory.isInGCMode() && !isEnabledDuringGC) {
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
        if (factory.isInGCMode() && !isEnabledDuringGC) {
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
        if (factory.isInGCMode() && !isEnabledDuringGC) {
            return true;
        }
        return factory.resetWatchpoint(this);
    }

    public boolean isEnabledDuringGC() {
        return isEnabledDuringGC;
    }

    public void setEnabledDuringGC(boolean isEnabledDuringGC) {
        ProgramError.check(active, "Attempt to set flag on disabled watchpoint");
        this.isEnabledDuringGC = isEnabledDuringGC;
        if (factory.isInGCMode() && !isEnabledDuringGC) {
            disable();
        } else if (factory.isInGCMode() && isEnabledDuringGC) {
            enable();
        }
    }

    public boolean isEnabled() {
        return active && (read || write || exec);
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

    public void setEagerRelocationUpdate(boolean eagerRelocationUpdate) {
        this.eagerRelocationUpdate = eagerRelocationUpdate;
        if (getTeleObject() != null) {
            TeleWatchpoint watchpoint;
            int index = InspectableHeapInfo.getCardTableIndex(start, factory.teleProcess.teleVM().teleHeapRegionsArray());
            int objectReferences = factory.teleProcess.teleVM().readCardTableEntry(index);
            Address cardTableAddress = factory.teleProcess.teleVM().getCardTableAddress(index);
            if (eagerRelocationUpdate) {
                if (objectReferences > 0) {
                    watchpoint = (TeleWatchpoint) factory.findInvisibleWatchpoint(cardTableAddress);
                    watchpoint.disable();
                } else {
                    watchpoint = factory.createInvisibleWatchpoint("Card table watchpoint", new FixedMemoryRegion(cardTableAddress, Size.fromInt(Word.size()), "Card table entry"), true, true, false, false, true);
                }
                objectReferences++;
                factory.teleProcess.teleVM().writeCardTableEntry(index, objectReferences);
                watchpoint.enable();
            } else {
                watchpoint = (TeleWatchpoint) factory.findInvisibleWatchpoint(cardTableAddress);
                watchpoint.disable();
                objectReferences--;
                factory.teleProcess.teleVM().writeCardTableEntry(index, objectReferences);
                if (objectReferences > 0) {
                    watchpoint.enable();
                } else {
                    factory.removeInvisibleWatchpoint(watchpoint);
                }
            }
        }
    }

    public boolean isEagerRelocationUpdateSet() {
        return eagerRelocationUpdate;
    }

    protected void updateTeleWatchpointCache(TeleProcess teleProcess) {
        if (teleWatchpointCache == null || teleWatchpointCache.length != size.toInt()) {
            teleWatchpointCache = new byte[size.toInt()];
        }
        try {
            teleWatchpointCache = teleProcess.dataAccess().readFully(start, size.toInt());
        } catch (DataIOError e) {
            // Must be a watchpoint in an address space that doesn't (yet?) exist in the VM process.
        }
    }

    public boolean disable() {
        return factory.deactivateWatchpoint(this);
    }

    public boolean enable() {
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

    private static final class TeleInvisibleWatchpoint extends TeleWatchpoint {

        public TeleInvisibleWatchpoint(Factory factory, String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            super(factory, description, memoryRegion, after, read, write, exec, gc);
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
        private final TreeSet<TeleWatchpoint> watchpoints = new TreeSet<TeleWatchpoint>(this);

        // A thread-safe, immutable collection of the current watchpoint list.
        // This list will be read many, many more times than it will change.
        private volatile IterableWithLength<MaxWatchpoint> watchpointsCache;

        // CardTable watchpoints used for eager watchpoint relocation mechanism
        private final TreeSet<TeleWatchpoint> invisibleWatchpoints = new TreeSet<TeleWatchpoint>(this);
        private volatile IterableWithLength<MaxWatchpoint> invisibleWatchpointsCache;

        private boolean inGCMode = false;

        private int relocatableWatchpointsCounter = 0;

        private TeleWatchpoint endOfGCWatchpoint;

        private Address triggeredWatchpointAddress;
        private String triggeredWatchpointCode;

        public Factory(TeleProcess teleProcess) {
            this.teleProcess = teleProcess;
            updateCache();
            teleProcess.teleVM().addVMStateObserver(new TeleVMStateObserver() {

                public void upate(MaxVMState maxVMState) {
                    if (maxVMState.processState() == ProcessState.TERMINATED) {
                        watchpoints.clear();
                        invisibleWatchpoints.clear();
                        updateCache();
                        setChanged();
                        notifyObservers();
                    }
                }
            });
        }

        public void initFactory() {
            endOfGCWatchpoint = createInvisibleWatchpoint("End of GC", new FixedMemoryRegion(teleProcess.teleVM().rootEpochAddress(), Size.fromInt(Pointer.size()), "Root epoch address"), true, false, true, false, true);
            //endOfGCWatchpoint.enable(); // TODO: REMOVE, should be dynamic
        }

        public boolean isInGCMode() {
            return inGCMode;
        }

        private void updateCache() {
            watchpointsCache = new VectorSequence<MaxWatchpoint>(watchpoints);
            invisibleWatchpointsCache = new VectorSequence<MaxWatchpoint>(invisibleWatchpoints);
        }

        /**
         * Does the bookkeeping of set relocatable watchpoints in our system.
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
                if (relocatableWatchpointsCounter == 1) {
                    endOfGCWatchpoint.enable();
                }
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

        /**
         * Creates a new invisible watchpoint. This watchpoint is not shown in the list of current watchpoints.
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
         *
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        public synchronized TeleWatchpoint createInvisibleWatchpoint(String description, MemoryRegion memoryRegion, boolean after, boolean read, boolean write, boolean exec, boolean gc) {
            final TeleWatchpoint teleWatchpoint = new TeleInvisibleWatchpoint(this, description, memoryRegion, after, read, write, exec, gc);
            invisibleWatchpoints.add(teleWatchpoint);
            return teleWatchpoint;
        }

        public synchronized boolean removeInvisibleWatchpoint(TeleWatchpoint teleWatchpoint) {
            ProgramError.check(teleWatchpoint.active, "Attempt to delete an already deleted watchpoint ");
            if (invisibleWatchpoints.remove(teleWatchpoint)) {
                if (teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Removed invisible watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                    teleWatchpoint.active = false;

                    updateCache();
                    return true;
                } else {
                    // Can't deactivate for some reason, so put back in the active collection.
                    invisibleWatchpoints.add(teleWatchpoint);
                }
            }
            Trace.line(TRACE_VALUE, "Failed to remove watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            return false;
        }

        /**
         * Adds a watchpoint to the list of current watchpoints, and activates this watchpoint.
         * @param teleWatchpoint
         * @return Watchpoint
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized TeleWatchpoint addWatchpoint(TeleWatchpoint teleWatchpoint)  throws TooManyWatchpointsException, DuplicateWatchpointException {
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
            if (teleWatchpoint.isEnabledDuringGC() || !isInGCMode()) {
                if (!teleProcess.activateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Failed to create watchpoint " + teleWatchpoint.toString());
                    watchpoints.remove(teleWatchpoint);
                    return null;
                }
            } else {
                Trace.line(TRACE_VALUE, "Watchpoint deactivated during GC " + teleWatchpoint.toString());
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
                            endOfGCWatchpoint.disable();
                        }
                        relocatableWatchpointsCounter--;
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
         * Relocates a watchpoint to the new address of the object.
         *
         * @param teleWatchpoint
         * @return true when relocation of watchpoint succeeded
         *
         * @throws TooManyWatchpointsException
         * @throws DuplicateWatchpointException
         */
        private synchronized boolean relocateWatchpoint(TeleWatchpoint teleWatchpoint) throws TooManyWatchpointsException, DuplicateWatchpointException {
            ProgramError.check(teleWatchpoint.active, "Attempt to relocate an already deleted watchpoint ");
            TeleObject teleObject = teleWatchpoint.getTeleObject();
            System.out.println("RELOCATE WATCHPOINT");
            if (teleObject != null) {
                System.out.println("tele object still reachable");
                if (teleObject.isLive()) {
                    System.out.println("tele object still live");
                    Address newAddress = teleObject.getCurrentOrigin();

                    if (removeWatchpoint(teleWatchpoint)) {
                        teleWatchpoint.setStart(newAddress);
                        if (addWatchpoint(teleWatchpoint) != null) {
                            return true;
                        } else {
                            // Can't store relocated watchpoint in watchpoints list.
                            Trace.line(TRACE_VALUE, "Failed to store relocated watchpoint in watchpoints list start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                        }
                    }
                    Trace.line(TRACE_VALUE, "Failed to relocate watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                } else {
                    System.out.println("tele object dead");
                    if (removeWatchpoint(teleWatchpoint)) {
                        TeleWatchpoint teleRegionWatchpoint = setRegionWatchpoint("RegionWatchpoint - GC removed corresponding Object", new FixedMemoryRegion(teleWatchpoint.start(), teleWatchpoint.size(), "Old memory location of watched object"),
                            teleWatchpoint.after, teleWatchpoint.read, teleWatchpoint.write, teleWatchpoint.exec, teleWatchpoint.isEnabledDuringGC);
                        if (teleRegionWatchpoint != null) {
                            teleRegionWatchpoint.setEagerRelocationUpdate(true);
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

        public synchronized boolean relocateCardTableWatchpoint(Address oldAddress, Address newAddress) {
            TeleWatchpoint watchpoint;
            final int oldIndex = InspectableHeapInfo.getCardTableIndex(oldAddress, teleProcess.teleVM().teleHeapRegionsArray());
            final int newIndex = InspectableHeapInfo.getCardTableIndex(newAddress, teleProcess.teleVM().teleHeapRegionsArray());
            if (oldIndex == newIndex) {
                return false;
            }

            int oldObjectReferences = teleProcess.teleVM().readCardTableEntry(oldIndex);
            final Address oldCardTableAddress = teleProcess.teleVM().getCardTableAddress(oldIndex);
            watchpoint = (TeleWatchpoint) findInvisibleWatchpoint(oldCardTableAddress);
            if (watchpoint == null) {
                return false;
            }
            oldObjectReferences--;
            if (oldObjectReferences == 0) {
                removeInvisibleWatchpoint(watchpoint);
            } else {
                watchpoint.disable();
            }
            teleProcess.teleVM().writeCardTableEntry(oldIndex, oldObjectReferences);
            if (oldObjectReferences > 0) {
                watchpoint.enable();
            }

            int newObjectReferences = teleProcess.teleVM().readCardTableEntry(newIndex);
            final Address newCardTableAddress = teleProcess.teleVM().getCardTableAddress(newIndex);
            if (newObjectReferences == 0) {
                watchpoint = createInvisibleWatchpoint("Card table watchpoint", new FixedMemoryRegion(newCardTableAddress, Size.fromInt(Word.size()), "Card table entry"), true, true, false, false, true);
            } else {
                watchpoint = (TeleWatchpoint) findInvisibleWatchpoint(newCardTableAddress);
                watchpoint.disable();
            }
            newObjectReferences++;
            teleProcess.teleVM().writeCardTableEntry(newIndex, newObjectReferences);
            watchpoint.enable();

            return true;
        }

        /**
         * Updates the watchpoints of all caches.
         */
        public void updateWatchpointCaches() {
            for (TeleWatchpoint teleWatchpoint : watchpoints) {
                teleWatchpoint.updateTeleWatchpointCache(teleProcess);
            }
        }

        /**
         * Disables all watchpoints during GC, which are not interested in GC actions.
         */
        public void disableWatchpointsDuringGC() {
            if (!inGCMode) {
                for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                    if (!maxWatchpoint.isEnabledDuringGC()) {
                        maxWatchpoint.disable();
                    }
                }
                inGCMode = true;
            }
        }

        /**
         * Re-enables all watchpoints after GC, which got deactived during GC.
         */
        public void reenableWatchpointsAfterGC() {
            if (inGCMode) {
                for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                    if (!maxWatchpoint.isEnabledDuringGC()) {
                        maxWatchpoint.enable();
                    }
                }
                inGCMode = false;
            }
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

        public MaxWatchpoint findInvisibleWatchpoint(Address address) {
            for (MaxWatchpoint maxWatchpoint : invisibleWatchpointsCache) {
                if (maxWatchpoint.contains(address)) {
                    return maxWatchpoint;
                }
            }
            return null;
        }

        public void lazyUpdateRelocatableWatchpoints() throws TooManyWatchpointsException, DuplicateWatchpointException {
            for (MaxWatchpoint maxWatchpoint : watchpointsCache) {
                relocateWatchpoint((TeleWatchpoint) maxWatchpoint);
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
