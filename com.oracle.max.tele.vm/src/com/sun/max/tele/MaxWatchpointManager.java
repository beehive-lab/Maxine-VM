/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import java.io.*;
import java.util.*;

import com.sun.max.tele.MaxWatchpoint.WatchpointSettings;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;

/**
 * Client access to VM watchpoint creation and management.
 */
public interface MaxWatchpointManager {

    /**
     * An exception thrown when an attempt is made to create more watchpoints
     * in the VM than is supported by the platform.
     */
    public static final class MaxTooManyWatchpointsException extends MaxException {

        public MaxTooManyWatchpointsException(String message) {
            super(message);
        }
    }

    /**
     * An exception thrown when an attempt is made to create a watchpoint
     * in the VM that overlaps with an existing watchpoint.
     */
    public static final class MaxDuplicateWatchpointException extends MaxException {

        public MaxDuplicateWatchpointException(String message) {
            super(message);
        }
    }

    /**
     * Adds a listener for watchpoint changes.
     * <p>
     * Thread-safe
     *
     * @param listener a watchpoint listener
     */
    void addListener(MaxWatchpointListener listener);

    /**
     * Removes a listener for watchpoint changes.
     * <p>
     * Thread-safe
     *
     * @param listener a watchpoint listener
     */
    void removeListener(MaxWatchpointListener listener);

    /**
     * Creates a new, active watchpoint that covers a given memory region in the VM.
     * <p>
     * The trigger occurs <strong>after</strong> the specified event.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param memoryRegion the region of memory in the VM to be watched.
     * @param settings initial settings for the watchpoint
     *
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createRegionWatchpoint(String description, MaxMemoryRegion memoryRegion, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers an entire heap object's memory in the VM.
     * <p>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createObjectWatchpoint(String description, TeleObject teleObject, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a heap object's field in the VM. If the object is live,
     * than this watchpoint will track the object's location during GC.
     * <p>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param fieldActor description of a field in object of that type
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createFieldWatchpoint(String description, TeleObject teleObject, FieldActor fieldActor, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers an element in an array in the VM.
     * <p>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM that contains the array
     * @param elementKind the type category of the array elements
     * @param arrayOffsetFromOrigin location relative to the object's origin of element 0 in the array
     * @param index index of the element to watch
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createArrayElementWatchpoint(String description, TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a field in an object's header in the VM.  If the object is live,
     * than this watchpoint will track the object's location during GC.
     * <p>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param headerField a field in the object's header
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createHeaderWatchpoint(String description, TeleObject teleObject, HeaderField headerField, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a thread local variable in the VM.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param threadLocaVariable a thread local variable i the VM
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createVmThreadLocalWatchpoint(String description, MaxThreadLocalVariable threadLocaVariable, WatchpointSettings settings)
        throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException;

    /**
     * All existing watchpoints set in the VM.
     * <p>
     * The collection is immutable and thus thread-safe,
     * but the state of the members is not immutable.
     *
     * @return all existing watchpoints; empty if none.
     */
    List<MaxWatchpoint> watchpoints();

    /**
     * Finds all VM watchpoints that overlap a specified memory region.
     * <p>
     *  Immutable collection; membership is thread-safe.
     *
     * @param memoryRegion an area of memory in the VM
     * @return the watchpoints whose memory region overlaps, empty sequence if none.
     */
    List<MaxWatchpoint> findWatchpoints(MaxMemoryRegion memoryRegion);

    /**
     * Writes a textual description of each existing watchpoint.
     * <p>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);

}
