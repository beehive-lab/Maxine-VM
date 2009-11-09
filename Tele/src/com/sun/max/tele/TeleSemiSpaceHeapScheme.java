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

import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.runtime.*;

/**
 * Implementation details about the heap in the VM, specialized
 * for the semi-space implementation.
 *
 * @author Hannes Payer
 * @author Michael Van De Vanter
 * @see SemiSpaceHeapScheme
 */
public final class TeleSemiSpaceHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme{

    TeleSemiSpaceHeapScheme(TeleVM teleVM) {
        super(teleVM);
    }

    public Class heapSchemeClass() {
        return SemiSpaceHeapScheme.class;
    }

    public boolean isInLiveMemory(Address address) {

        if (teleVM().isInGC()) { // this assumption needs to be proofed; basically it means that during GC both heaps are valid
            return true;
        }

        for (TeleRuntimeMemoryRegion teleHeapRegion : teleVM().teleHeapRegions()) {
            if (teleHeapRegion.contains(address)) {
                if (teleHeapRegion.description().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) { // everything in from-space is dead
                    return false;
                }
                if (address.greaterEqual(teleHeapRegion.mark())) { // everything in to-space after the global allocation mark is dead
                    return false;
                }
                for (TeleNativeThread teleNativeThread : teleVM().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                    TeleThreadLocalValues teleThreadLocalValues = teleNativeThread.threadLocalsFor(Safepoint.State.ENABLED);
                    if (!teleThreadLocalValues.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME).equals(Word.zero())) {
                        if (address.greaterEqual(teleThreadLocalValues.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME).asAddress())
                                        && teleThreadLocalValues.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME).asAddress().greaterThan(address)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return true;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return (!pointer.isZero()) &&  pointer.and(1).toLong() == 1;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return isForwardingPointer(pointer) ? pointer.minus(1) : pointer;
    }

    public Pointer getForwardedObject(Pointer objectPointer, DataAccess dataAccess) {
        if (!objectPointer.isZero()) {
            Pointer pointer = dataAccess.readWord(objectPointer.plus(Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB))).asPointer();
            if (isForwardingPointer(pointer)) {
                final Pointer newPointer = getTrueLocationFromPointer(pointer);
                if (!newPointer.isZero()) {
                    return newPointer;
                }
            }
        }
        return objectPointer;
    }
}
