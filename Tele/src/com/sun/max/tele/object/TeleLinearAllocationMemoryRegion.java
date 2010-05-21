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
package com.sun.max.tele.object;

import java.lang.management.*;

import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;


public class TeleLinearAllocationMemoryRegion extends TeleRuntimeMemoryRegion {

    /**
     * Cached mark field from the object in the VM.
     */
    private Address mark = Address.zero();

    private MemoryUsage memoryUsage = null;

    public TeleLinearAllocationMemoryRegion(TeleVM teleVM, Reference linearAllocationMemoryRegionReference) {
        super(teleVM, linearAllocationMemoryRegionReference);
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

    @Override
    protected void refresh() {
        if (vm().tryLock()) {
            try {
                final Reference markReference = vm().teleFields().LinearAllocationMemoryRegion_mark.readReference(reference());
                mark = markReference.readWord(AtomicWord.valueOffset()).asPointer();
            } catch (DataIOError dataIOError) {
                // No update; VM not available for some reason.
                // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
            } finally {
                vm().unlock();
            }
        }
        super.refresh();
    }

}
