/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * A model for data tables that represent regions of memory in the VM, one region per row.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorMemoryTableModel extends InspectorTableModel {

    private static final List<MaxWatchpoint> EMPTY_WATCHPOINT_LIST = Collections.emptyList();

    private final Size wordSize;

    // Memory location from which to compute offsets
    private Address origin = Address.zero();

    public InspectorMemoryTableModel(Inspection inspection, Address origin) {
        super(inspection);
        this.origin = origin;
        wordSize = inspection.vm().wordSize();
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
    public abstract MaxMemoryRegion getMemoryRegion(int row);

    /**
     * Returns all memory watchpoints, if any, whose coverage intersects memory corresponding
     * to a row in the model of VM memory.
     *
     * @param row a row in the table model of memory
     * @return memory watchpoints whose region intersects the memory for this row in the model, empty sequence if none.
     */
    public List<MaxWatchpoint> getWatchpoints(int row) {
        // Gets called a lot, usually empty result;  allocate as little as possible
        List<MaxWatchpoint> watchpoints = null;
        final MaxMemoryRegion rowMemoryRegion = getMemoryRegion(row);
        if (vm().watchpointManager() != null) {
            for (MaxWatchpoint watchpoint : vm().watchpointManager().watchpoints()) {
                if (watchpoint.memoryRegion().overlaps(rowMemoryRegion)) {
                    if (watchpoints == null) {
                        watchpoints = new ArrayList<MaxWatchpoint>(4);
                    }
                    watchpoints.add(watchpoint);
                }
            }
        }
        return watchpoints == null ? EMPTY_WATCHPOINT_LIST : watchpoints;
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
