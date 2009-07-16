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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * Makes critical state information about the object heap
 * remotely inspectable.
 * Active only when VM is being inspected.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class InspectableHeapInfo {

    private InspectableHeapInfo() {
    }

    @INSPECTED
    private static MemoryRegion[] memoryRegions;

    public static void registerMemoryRegions(MemoryRegion... memoryRegions) {
        if (MaxineMessenger.isVmInspected()) {
            if (roots.isZero()) {
                roots = Memory.allocate(MAX_NUMBER_OF_ROOTS * Pointer.size());
            }
            InspectableHeapInfo.memoryRegions = memoryRegions;
        }
    }

    public static final int MAX_NUMBER_OF_ROOTS = Ints.M / 8;



    @INSPECTED
    public static Pointer roots = Pointer.zero();
    //private static Object[] roots = new Object[MAX_NUMBER_OF_ROOTS];

    @INSPECTED
    private static long rootEpoch;

    @INSPECTED
    private static long collectionEpoch;

    /**
     * For remote inspection:  records that a GC has begun.
     */
    public static void beforeGarbageCollection() {
        collectionEpoch++;
    }

    /**
     * For remote inspection:  records that a GC has concluded.
     */
    public static void afterGarbageCollection() {
        rootEpoch = collectionEpoch;
    }
}
