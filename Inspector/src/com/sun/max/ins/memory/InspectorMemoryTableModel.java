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
package com.sun.max.ins.memory;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * A model for data tables that represent regions of memory in the VM, one region per row.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorMemoryTableModel extends InspectorTableModel {

    private final Inspection inspection;
    private final Size wordSize;

    // Memory location from which to compute offsets
    private Address origin = Address.zero();

    public InspectorMemoryTableModel(Inspection inspection, Address origin) {
        this.inspection = inspection;
        this.origin = origin;
        wordSize = inspection.maxVM().wordSize();
    }

    /**
     * Returns the memory location corresponding to a row in the model of VM memory.
     *
     * @param row a row in the table model of memory
     * @return the first location in VM memory corresponding to a row in the table model
     */
    public Address getAddress(int row) {
        return getMemoryRegion(row).start();
    }

    /**
     * Returns the region of memory corresponding to a row in the model of VM memory.
     *
     * @param row a row in the table model of memory
     * @return the first location in VM memory corresponding to a row in the table model
     */
    public abstract MemoryRegion getMemoryRegion(int row);

    /**
     * Returns all memory watchpoints, if any, whose coverage intersects memory corresponding
     * to a row in the model of VM memory.
     *
     * @param row a row in the table model of memory
     * @return memory watchpoints whose region intersects the memory for this row in the model, empty sequence if none.
     */
    public Sequence<MaxWatchpoint> getWatchpoints(int row) {
        DeterministicSet<MaxWatchpoint> watchpoints = DeterministicSet.Static.empty(MaxWatchpoint.class);
        for (MaxWatchpoint watchpoint : inspection.maxVM().watchpoints()) {
            if (watchpoint.overlaps(getMemoryRegion(row))) {
                if (watchpoints.isEmpty()) {
                    watchpoints = new DeterministicSet.Singleton<MaxWatchpoint>(watchpoint);
                } else if (watchpoints.length() == 1) {
                    GrowableDeterministicSet<MaxWatchpoint> newSet = new LinkedIdentityHashSet<MaxWatchpoint>(watchpoints.first());
                    newSet.add(watchpoint);
                    watchpoints = newSet;
                } else {
                    final GrowableDeterministicSet<MaxWatchpoint> growableSet = (GrowableDeterministicSet<MaxWatchpoint>) watchpoints;
                    growableSet.add(watchpoint);
                }
            }
        }
        return watchpoints;
    }

    /**
     * Sets the address in VM memory from which offsets for this model are computed.
     * <br>
     * Calls {@link #update()} after change.
     *
     * @param origin a memory location in the VM.
     */
    public void setOrigin(Address origin) {
        this.origin = origin;
        update();
    }

    /**
     * Returns an address in VM memory from which offsets for this model are computed.
     *
     * @return a memory address understood to be the zero offset for this model
     * @see #getOffset(int)
     */
    public Address getOrigin() {
        return origin;
    }

    /**
     * Returns an offset in bytes, from an origin in VM memory, for the beginning of the memory
     * corresponding to this row in the model.
     *
     * @param row a row in the table model of memory
     * @return location of the memory for this row, specified in a byte offset from an origin in VM memory
     * @see InspectorMemoryTableModel#getOrigin()
     */
    public abstract Offset getOffset(int row);

    /**
     * Locates the row, if any, that represent a range of memory that includes a specific location in VM memory.
     *
     * @param address a location in VM memory.
     * @return the row in the model corresponding to the location, -1 if none
     */
    public abstract int findRow(Address address);

    protected Size getWordSize() {
        return wordSize;
    }

    /**
     * Update internal state of the model after some parameter is set.
     *
     * @see #setOrigin(Address)
     */
    protected void update() {
    }

}
