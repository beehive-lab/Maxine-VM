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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for a region of memory in the {@link TeleVM} used to allocate target code.
 *
 * @author Michael Van De Vanter
 */
public final class TeleCodeRegion extends TeleLinearAllocationMemoryRegion {

    private static final int TRACE_VALUE = 2;

    private final List<TeleTargetMethod> teleTargetMethods = new ArrayList<TeleTargetMethod>();

    TeleCodeRegion(TeleVM teleVM, Reference codeRegionReference) {
        super(teleVM, codeRegionReference);
    }

    /**
     * @return whether this region is the code region contained in the boot image of the {@link TeleVM}.
     */
    private boolean isBootCodeRegion() {
        return this == teleVM().teleBootCodeRegion();
    }

    @Override
    public Size size() {
        if (isBootCodeRegion()) {
            // The explicit representation of the boot {@link CodeRegion} gets "trimmed" by setting its size
            // to the amount allocated within the region.  Other regions don't have this happen.
            // Return the size allocated for the whole region, as recorded in the boot image.
            return Size.fromInt(teleVM().bootImage().header.codeSize);
        }
        return super.size();
    }

    /**
     * @return how much memory in region has been allocated to code, {@link Size#zero()) if memory for region not allocated.
     */
    @Override
    public Size allocatedSize() {
        if (isAllocated()) {
            return mark().minus(start()).asSize();
        }
        return Size.zero();
    }

    public List<TeleTargetMethod> teleTargetMethods() {
        return teleTargetMethods;
    }

    @Override
    public void refresh(long processEpoch) {
        Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
        final long startTimeMillis = System.currentTimeMillis();
        Reference targetMethods = teleVM().teleFields().CodeRegion_targetMethods.readReference(reference());
        int size = teleVM().teleFields().SortedMemoryRegionList_size.readInt(targetMethods);
        Reference regions = teleVM().teleFields().SortedMemoryRegionList_memoryRegions.readReference(targetMethods);
        int index = teleTargetMethods.size();
        final int delta = size - index;
        while (index < size) {
            Reference ref = teleVM().getElementValue(Kind.REFERENCE, regions, index).asReference();
            TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleVM().makeTeleObject(ref);
            assert teleTargetMethod != null;
            teleTargetMethods.add(teleTargetMethod);
            index++;
        }
        Trace.end(TRACE_VALUE, tracePrefix() + "refreshing: new target methods =" + delta, startTimeMillis);
    }
}
