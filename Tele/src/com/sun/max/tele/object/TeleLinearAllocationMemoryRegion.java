/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.management.*;

import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;


public class TeleLinearAllocationMemoryRegion extends TeleRuntimeMemoryRegion {

    private static final int TRACE_VALUE = 2;

    /**
     * Cached mark field from the object in the VM.
     */
    private Address mark = Address.zero();

    private MemoryUsage memoryUsage = null;

    public TeleLinearAllocationMemoryRegion(TeleVM teleVM, Reference linearAllocationMemoryRegionReference) {
        super(teleVM, linearAllocationMemoryRegionReference);
    }

    @Override
    protected int getObjectUpdateTraceValue() {
        return 1;
    }

    @Override
    protected void updateObjectCache(StatsPrinter statsPrinter) {
        super.updateObjectCache(statsPrinter);
        try {
            final Reference markReference = vm().teleFields().LinearAllocationMemoryRegion_mark.readReference(reference());
            mark = markReference.readWord(AtomicWord.valueOffset()).asPointer();
        } catch (DataIOError dataIOError) {
            // No update; data read failed for some reason other than VM availability
            TeleWarning.message("TeleLinearAllocationMemoryRegion: ", dataIOError);
            dataIOError.printStackTrace();
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
    }

    @Override
    public MemoryUsage getUsage() {
        if (memoryUsage == null) {
            if (isAllocated() && !mark.isZero()) {
                memoryUsage = new MemoryUsage(-1, mark.minus(getRegionStart()).toLong(), getRegionSize().toLong(), -1);
            }
        }
        if (memoryUsage != null) {
            return memoryUsage;
        }
        return super.getUsage();
    }

    @Override
    public boolean containsInAllocated(Address address) {
        if (isAllocated()) {
            return address.greaterEqual(getRegionStart()) && address.lessThan(mark);
        }
        return super.containsInAllocated(address);
    }

    /**
     * Reads from the VM the mark field of the {@link LinearAllocationMemoryRegion}.
     */
    public Address mark() {
        return mark;
    }

}
