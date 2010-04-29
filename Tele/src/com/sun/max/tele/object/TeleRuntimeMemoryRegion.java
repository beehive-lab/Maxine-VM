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

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for objects in the VM that represent a region of memory.
 *
 * @author Michael Van De Vanter
 */
public class TeleRuntimeMemoryRegion extends TeleTupleObject {

    private Address regionStart = Address.zero();
    private Size regionSize = Size.zero();
    private String regionName = null;

    TeleRuntimeMemoryRegion(TeleVM teleVM, Reference runtimeMemoryRegionReference) {
        super(teleVM, runtimeMemoryRegionReference);
    }

    public final Address getRegionStart() {
        return regionStart;
    }

    public Size getRegionSize() {
        return regionSize;
    }

    public final String getRegionName() {
        return regionName;
    }

    /**
     * @return whether memory has been allocated yet in the VM for this region.
     */
    public final boolean isAllocated() {
        return !getRegionStart().isZero();
    }

    @Override
    protected void refresh() {
        if (vm().tryLock()) {
            try {
                final Size newRegionSize = vm().teleFields().RuntimeMemoryRegion_size.readWord(reference()).asSize();

                final Reference regionNameStringReference = vm().teleFields().RuntimeMemoryRegion_regionName.readReference(reference());
                final TeleString teleString = (TeleString) vm().makeTeleObject(regionNameStringReference);
                final String newRegionName = teleString.getString();

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
                this.regionStart = newRegionStart;
                this.regionSize = newRegionSize;
                this.regionName = newRegionName;
            } catch (DataIOError dataIOError) {
                System.err.println("TeleRuntimeMemoryRegion dataIOError:");
                dataIOError.printStackTrace();
                // No update; VM not available for some reason.
                // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
            } finally {
                vm().unlock();
            }
        }
        super.refresh();
    }


}
