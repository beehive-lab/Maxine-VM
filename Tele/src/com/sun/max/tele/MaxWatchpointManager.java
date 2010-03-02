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
package com.sun.max.tele;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.tele.MaxWatchpoint.*;
import com.sun.max.tele.debug.TeleWatchpoint.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;

/**
 * Client access to VM watchpoint creation and management.
 *
 * @author Michael Van De Vanter
 */
public interface MaxWatchpointManager {

    /**
     * Adds a listener for watchpoint changes.
     * <br>
     * Thread-safe
     *
     * @param listener a watchpoint listener
     */
    void addListener(MaxWatchpointListener listener);

    /**
     * Removes a listener for watchpoint changes.
     * <br>
     * Thread-safe
     *
     * @param listener a watchpoint listener
     */
    void removeListener(MaxWatchpointListener listener);

    /**
     * Creates a new, active watchpoint that covers a given memory region in the VM.
     * <br>
     * The trigger occurs <strong>after</strong> the specified event.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param memoryRegion the region of memory in the VM to be watched.
     * @param settings initial settings for the watchpoint
     *
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createRegionWatchpoint(String description, MemoryRegion memoryRegion, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers an entire heap object's memory in the VM.
     * <br>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createObjectWatchpoint(String description, TeleObject teleObject, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a heap object's field in the VM. If the object is live,
     * than this watchpoint will track the object's location during GC.
     * <br>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param fieldActor description of a field in object of that type
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createFieldWatchpoint(String description, TeleObject teleObject, FieldActor fieldActor, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers an element in an array in the VM.
     * <br>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM that contains the array
     * @param elementKind the type category of the array elements
     * @param arrayOffsetFromOrigin location relative to the object's origin of element 0 in the array
     * @param index index of the element to watch
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createArrayElementWatchpoint(String description, TeleObject teleObject, Kind elementKind, Offset arrayOffsetFromOrigin, int index, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a field in an object's header in the VM.  If the object is live,
     * than this watchpoint will track the object's location during GC.
     * <br>
     * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param teleObject a heap object in the VM
     * @param headerField a field in the object's header
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createHeaderWatchpoint(String description, TeleObject teleObject, HeaderField headerField, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * Creates a new, active watchpoint that covers a thread local variable in the VM.
     *
     * @param description text useful to a person, for example capturing the intent of the watchpoint
     * @param threadLocaVariable a thread local variable i the VM
     * @param settings initial settings for the watchpoint
     * @return a new watchpoint, if successful
     * @throws TooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
     * @throws DuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
     * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
     */
    MaxWatchpoint createVmThreadLocalWatchpoint(String description, MaxThreadLocalVariable threadLocaVariable, WatchpointSettings settings)
        throws TooManyWatchpointsException, DuplicateWatchpointException, MaxVMBusyException;

    /**
     * All existing watchpoints set in the VM.
     * <br>
     * The collection is immutable and thus thread-safe,
     * but the state of the members is not immutable.
     *
     * @return all existing watchpoints; empty if none.
     */
    IterableWithLength<MaxWatchpoint> watchpoints();

    /**
     * Finds all VM watchpoints that overlap a specified memory region.
     * <br>
     *  Immutable collection; membership is thread-safe.
     *
     * @param memoryRegion an area of memory in the VM
     * @return the watchpoints whose memory region overlaps, empty sequence if none.
     */
    Sequence<MaxWatchpoint> findWatchpoints(MemoryRegion memoryRegion);

    /**
     * Writes a textual description of each existing watchpoint.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);

}
