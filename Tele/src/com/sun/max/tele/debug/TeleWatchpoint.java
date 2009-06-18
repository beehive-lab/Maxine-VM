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
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class TeleWatchpoint extends RuntimeMemoryRegion implements MaxWatchpoint {

    private static final int TRACE_VALUE = 1;

    private final Factory _factory;

    // true iff active in VM
    private boolean _active = false;

    public TeleWatchpoint(Factory factory, Address address, Size size) {
        super(address, size);
        _factory = factory;
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

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#isActive()
     */
    public boolean isActive() {
        return _active;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#remove()
     */
    public boolean remove() {
        return  _factory.removeWatchpoint(this);
    }

    @Override
    public String toString() {
        return "TeleWatchpoint@" + super.toString();
    }

    /**
     * A factory for creating and managing process watchpoints.
     * <br>
     * <b>Implementation Restriction</b>: currently limited to one set at a time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Factory extends Observable implements Comparator<TeleWatchpoint> {

        private final TeleProcess _teleProcess;

        // This implementation is not thread-safe; this factory must take care of that.
        // Keep the set ordered by start address only, implemented by the comparator and equals().
        // An additional constraint imposed by this factory is that no regions overlap,
        // either in part or whole, with others in the set.
        private TreeSet<TeleWatchpoint> _watchpoints = new TreeSet<TeleWatchpoint>(this);

        // A thread-safe, immutable collection of the current watchpoint list.
        // This list will be read many, many more times than it will change.
        private volatile IterableWithLength<MaxWatchpoint> _watchpointsCache;

        public Factory(TeleProcess teleProcess) {
            _teleProcess = teleProcess;
            updateCache();
            teleProcess.teleVM().addVMStateObserver(new TeleVMStateObserver() {

                public void upate(MaxVMState maxVMState) {
                    if (maxVMState.processState() == ProcessState.TERMINATED) {
                        _watchpoints.clear();
                        updateCache();
                        setChanged();
                        notifyObservers();
                    }
                }
            });
        }

        private void updateCache() {
            _watchpointsCache = new VectorSequence<MaxWatchpoint>(_watchpoints);
        }

        /**
         * Creates a new watchpoint that covers a given memory region in the VM.
         *
         * @param address start of the memory region
         * @param size size of the memory region
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         */
        public synchronized TeleWatchpoint setWatchpoint(Address address, Size size) throws TooManyWatchpointsException, DuplicateWatchpointException {
            if (_watchpoints.size() >= _teleProcess.maximumWatchpointCount()) {
                throw new TooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    _teleProcess.maximumWatchpointCount() + ") exceeded");
            }
            final TeleWatchpoint teleWatchpoint = new TeleWatchpoint(this, address, size);
            if (!_watchpoints.add(teleWatchpoint)) {
                // An existing watchpoint starts at the same location
                throw new DuplicateWatchpointException("Watchpoint already exists at location: " + address.toHexString());
            }
            // Check for possible overlaps with predecessor or successor (according to start location)
            final TeleWatchpoint lowerWatchpoint = _watchpoints.lower(teleWatchpoint);
            final TeleWatchpoint higherWatchpoint = _watchpoints.higher(teleWatchpoint);
            if ((lowerWatchpoint != null && lowerWatchpoint.overlaps(teleWatchpoint)) ||
                            (higherWatchpoint != null && higherWatchpoint.overlaps(teleWatchpoint))) {
                _watchpoints.remove(teleWatchpoint);
                throw new DuplicateWatchpointException("Watchpoint already exists that overlaps with start=" + address.toHexString() + ", size=" + size.toString());
            }
            if (!_teleProcess.activateWatchpoint(teleWatchpoint)) {
                Trace.line(TRACE_VALUE, "Failed to create watchpoint at " + teleWatchpoint.start().toHexString());
                _watchpoints.remove(teleWatchpoint);
                return null;
            }
            Trace.line(TRACE_VALUE, "Created watchpoint at start=" + address.toHexString() + ", size=" + size.toString());
            teleWatchpoint._active = true;
            updateCache();
            setChanged();
            notifyObservers();
            return teleWatchpoint;
        }

        /**
         * Removes an active memory watchpoint from the VM.
         *
         * @param maxWatchpoint an existing watchpoint in the VM
         * @return true if successful; false if watchpoint is not active in VM.
         */
        private synchronized boolean removeWatchpoint(TeleWatchpoint teleWatchpoint) {
            ProgramError.check(teleWatchpoint._active, "Attempt to delete an already deleted watchpoint ");
            if (_watchpoints.remove(teleWatchpoint)) {
                if (_teleProcess.deactivateWatchpoint(teleWatchpoint)) {
                    Trace.line(TRACE_VALUE, "Removed watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
                    teleWatchpoint._active = false;
                    updateCache();
                    setChanged();
                    notifyObservers();
                    return true;
                } else {
                    // Can't deactivate for some reason, so put back in the active collection.
                    _watchpoints.add(teleWatchpoint);
                }
            }
            Trace.line(TRACE_VALUE, "Failed to remove watchpoint at start=" + teleWatchpoint.start().toHexString() + ", size=" + teleWatchpoint.size().toString());
            return false;
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
            for (MaxWatchpoint maxWatchpoint : _watchpointsCache) {
                if (maxWatchpoint.contains(address)) {
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
            return _watchpointsCache;
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
