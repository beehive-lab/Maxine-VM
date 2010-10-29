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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for a region of memory in the VM used to allocate compiled code.
 *
 * @author Michael Van De Vanter
 */
public final class TeleCodeRegion extends TeleLinearAllocationMemoryRegion {

    private static final int TRACE_VALUE = 2;

    private boolean initialized = false;
    private boolean isBootCodeRegion = false;
    private final List<TeleTargetMethod> teleTargetMethods = new ArrayList<TeleTargetMethod>();

    private final Object localStatsPrinter = new Object() {

        private int previousMethodCount = 0;

        @Override
        public String toString() {
            final int methodCount = teleTargetMethods.size();
            final int newMethodCount =  methodCount - previousMethodCount;
            final StringBuilder msg = new StringBuilder();
            msg.append("#methods=(").append(methodCount);
            msg.append(",new=").append(newMethodCount).append(")");
            previousMethodCount = methodCount;
            return msg.toString();
        }
    };

    TeleCodeRegion(TeleVM vm, Reference codeRegionReference) {
        super(vm, codeRegionReference);
    }

    /**
     * @return whether this region is the code region contained in the boot image of the VM.
     */
    private boolean isBootCodeRegion() {
        initialize();
        return isBootCodeRegion;
    }

    private void initialize() {
        if (!initialized) {
            isBootCodeRegion = getRegionName().equals(vm().codeCache().bootCodeRegionName());
            initialized = true;
        }
    }

    @Override
    protected void updateObjectCache(StatsPrinter statsPrinter) {
        super.updateObjectCache(statsPrinter);
        // Register any new compiled methods that have appeared since the previous refresh
        // Don't try this until the code cache is ready, which it isn't early in the startup sequence.
        // Also make sure that the region has actually been allocated before trying.
        if (vm().codeCache().isInitialized() && isAllocated()) {
            Reference targetMethodsReference = vm().teleFields().CodeRegion_targetMethods.readReference(reference());
            int size = vm().teleFields().SortedMemoryRegionList_size.readInt(targetMethodsReference);
            Reference regionsReference = vm().teleFields().SortedMemoryRegionList_memoryRegions.readReference(targetMethodsReference);
            int index = teleTargetMethods.size();
            while (index < size) {
                try {
                    Reference targetMethodReference = vm().getElementValue(Kind.REFERENCE, regionsReference, index).asReference();
                    TeleTargetMethod teleTargetMethod = (TeleTargetMethod) heap().makeTeleObject(targetMethodReference);
                    assert teleTargetMethod != null;
                    teleTargetMethods.add(teleTargetMethod);
                } catch (InvalidReferenceException e) {
                    vm().invalidReferencesLogger().record(e.getReference(), TeleTargetMethod.class);
                } finally {
                    index++;
                }
            }
            statsPrinter.addStat(localStatsPrinter);
        } else {
            statsPrinter.addStat(" skipping update");
        }
    }

    @Override
    public Size getRegionSize() {
        if (isBootCodeRegion()) {
            // The explicit representation of the boot {@link CodeRegion} gets "trimmed" by setting its size
            // to the amount allocated within the region.  Other regions don't have this happen.
            // Return the size allocated for the whole region, as recorded in the boot image.
            return Size.fromInt(vm().bootImage().header.codeSize);
        }
        return super.getRegionSize();
    }

    /**
     * Gets all method compilations; assumes no eviction, no reordering.
     *
     * @return all compiled methods known to be in the region.
     */
    public List<TeleTargetMethod> teleTargetMethods() {
        return teleTargetMethods;
    }
}
