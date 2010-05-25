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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for objects in the VM that represent a region of memory.
 *
 * @author Michael Van De Vanter
 */
public class TeleRuntimeMemoryRegion extends TeleTupleObject {

    private static final int TRACE_VALUE = 2;

    private Address regionStart = Address.zero();
    private Size regionSize = Size.zero();
    private String regionName = null;
    private MemoryUsage memoryUsage = null;

    TeleRuntimeMemoryRegion(TeleVM teleVM, Reference runtimeMemoryRegionReference) {
        super(teleVM, runtimeMemoryRegionReference);
        refresh();
    }

    /**
     * @return the descriptive name assigned to the memory region object in the VM.
     */
    public final String getRegionName() {
        return regionName;
    }

    /**
     * @return starting location in VM memory of the region; zero if not yet allocated.
     */
    public final Address getRegionStart() {
        return regionStart;
    }

    /**
     * @return the size of the VM memory, as described by the memory region object in the VM.
     */
    public Size getRegionSize() {
        return regionSize;
    }

    /**
     * Computes the usage of the memory region, if available; default is to assume 100% utilized,
     * but specific subclasses may have more refined information available.
     */
    public MemoryUsage getUsage() {
        return memoryUsage;
    }

    /**
     * Determines whether an address is in the allocated portion of the memory region.
     * The default is to assume that all of the region is allocated, but
     * specific subclasses may have more refined information available.
     */
    public boolean containsInAllocated(Address address) {
        if (!isAllocated()) {
            return false;
        }
        // Default:  is the address anywhere in the region
        return address.greaterEqual(getRegionStart()) && address.lessThan(getRegionStart().plus(getRegionSize()));
    }

    /**
     * @return whether memory has been allocated yet in the VM for this region.
     */
    public final boolean isAllocated() {
        return !getRegionStart().isZero() && !getRegionSize().isZero();
    }

    /**
     * @return whether this region of VM memory might be relocated, once allocated.
     */
    public boolean isRelocatable() {
        return true;
    }

    @Override
    protected void refresh() {
        super.refresh();

        if (!isRelocatable() && isAllocated()) {
            // Optimization: if we know the region won't be moved by the VM, and
            // we already have the location information, then don't bother to refresh.
            return;
        }
        if (vm().tryLock()) {
            try {
                final Size newRegionSize = vm().teleFields().RuntimeMemoryRegion_size.readWord(reference()).asSize();

                final Reference regionNameStringReference = vm().teleFields().RuntimeMemoryRegion_regionName.readReference(reference());
                final TeleString teleString = (TeleString) vm().makeTeleObject(regionNameStringReference);
                final String newRegionName = teleString == null ? "<null>" : teleString.getString();

                Address newRegionStart = vm().teleFields().RuntimeMemoryRegion_start.readWord(reference()).asAddress();
                if (newRegionStart.isZero() && newRegionName != null) {
                    if (newRegionName.equals(vm().heap().bootHeapRegionName())) {
                        // Ugly special case:  the regionStart field of the static that defines the boot heap region
                        // is set at zero in the boot image, only set to the real value when the VM starts running.
                        // Lie about it.
                        newRegionStart = vm().bootImageStart();
                    }
                }
                // Quasi-atomic update
                if (newRegionStart.isZero()) {
                    Trace.line(TRACE_VALUE, tracePrefix() + "zero start address read from VM for region " + this);
                }
                this.regionStart = newRegionStart;
                this.regionSize = newRegionSize;
                this.regionName = newRegionName;
                final long sizeAsLong = this.regionSize.toLong();
                this.memoryUsage = new MemoryUsage(-1, sizeAsLong, sizeAsLong, -1);
            } catch (DataIOError dataIOError) {
                ProgramWarning.message("TeleRuntimeMemoryRegion dataIOError:");
                dataIOError.printStackTrace();
                // No update; VM not available for some reason.
                // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
            } finally {
                vm().unlock();
            }
        } else {
            ProgramWarning.message("TeleRuntimeMemoryRegion unable to refresh: VM busy");
        }
    }

}
