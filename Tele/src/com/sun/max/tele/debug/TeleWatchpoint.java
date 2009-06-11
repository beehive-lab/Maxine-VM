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

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class TeleWatchpoint implements MaxWatchpoint {

    private static final int TRACE_VALUE = 1;

    private final Factory _factory;

    // For thread-safety, never change the internal state (location) of the region.
    private final MemoryRegion _memoryRegion;

    public TeleWatchpoint(Factory factory, MemoryRegion memoryRegion) {
        _factory = factory;
        _memoryRegion = memoryRegion;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#memoryRegion()
     */
    public MemoryRegion memoryRegion() {
        return _memoryRegion;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxWatchpoint#remove()
     */
    public boolean remove() {
        return  _factory.removeWatchpoint(this);
    }

    @Override
    public String toString() {
        return "TeleWatchpoint@" + _memoryRegion.toString();
    }

    /**
     * A factory for creating and managing process watchpoints.
     * <br>
     * <b>Implementation Restriction</b>: currently limited to one set at a time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Factory extends Observable {

        // TODO (mlvdv) generalize to permit more than watchpoint
        // TODO (mlvdv) generalize to support more platforms

        private final TeleProcess _teleProcess;

        // The map implementation is not thread-safe; this factory must take care of that.
        private final Map<MemoryRegion, TeleWatchpoint> _watchpoints = new HashMap<MemoryRegion, TeleWatchpoint>();

        // A thread-safe, immutable collection of the current watchpoint list.
        // This list will be read many, many more times than it will change.
        private IterableWithLength<MaxWatchpoint> _watchpointsCache;

        public Factory(TeleProcess teleProcess) {
            _teleProcess = teleProcess;
            updateCache();
        }

        private void updateCache() {
            _watchpointsCache = new VectorSequence<MaxWatchpoint>(_watchpoints.values());
        }
        /**
         * Creates a new watchpoint that covers a given memory region in the VM.
         *
         * @param memoryRegion a region to cover with a memory watchpoint
         * @return a new watchpoint, if successful
         * @throws TooManyWatchpointsException
         */
        public synchronized TeleWatchpoint makeWatchpoint(MemoryRegion memoryRegion) throws TooManyWatchpointsException {
            if (_watchpoints.size() >= _teleProcess.maximumWatchpointCount()) {
                throw new TooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    _teleProcess.maximumWatchpointCount() + ") exceeded");
            }
            final TeleWatchpoint watchpoint = new TeleWatchpoint(this, memoryRegion);
            if (_teleProcess.activateWatchpoint(watchpoint.memoryRegion())) {
                Trace.line(TRACE_VALUE, "Created watchpoint at " + memoryRegion);
                _watchpoints.put(memoryRegion, watchpoint);
                updateCache();
                setChanged();
                notifyObservers();
                return watchpoint;
            }
            Trace.line(TRACE_VALUE, "Failed to create watchpoint at " + memoryRegion);
            return null;
        }

        /**
         * Removes an active memory watchpoint from the VM.
         *
         * @param maxWatchpoint an existing watchpoint in the VM
         * @return true if successful; false if watchpoint is not active in VM.
         */
        public synchronized boolean removeWatchpoint(MaxWatchpoint maxWatchpoint) {
            Problem.unimplemented("Can't remove watchpoints yet");

            final TeleWatchpoint removedWatchpoint = _watchpoints.remove(maxWatchpoint.memoryRegion());
            if (removedWatchpoint == null) {
                return false;
            }
            // Remove the watchpoints from the VM.
            updateCache();
            return true;
        }

        /**
         * @return all watchpoints currently set in the VM; thread-safe.
         */
        public synchronized IterableWithLength<MaxWatchpoint> watchpoints() {
            // Hand out the cached, thread-safe summary
            return _watchpointsCache;
        }

    }

    public static class TooManyWatchpointsException extends MaxException {
        TooManyWatchpointsException(String message) {
            super(message);
        }
    }
}
