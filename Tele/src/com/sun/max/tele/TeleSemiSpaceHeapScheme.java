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

import java.util.*;

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.runtime.*;

/**
 * Implementation details about the heap in the VM, specialized
 * for the semi-space implementation.
 * <br>
 * Forwarding pointer stored in the "Hub" field of objects.
 *
 * @author Hannes Payer
 * @author Michael Van De Vanter
 * @see SemiSpaceHeapScheme
 */
public final class TeleSemiSpaceHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme{

    private static final List<MaxCodeLocation> EMPTY_METHOD_LIST = Collections.emptyList();

    TeleSemiSpaceHeapScheme(TeleVM teleVM) {
        super(teleVM);
    }

    public Class heapSchemeClass() {
        return SemiSpaceHeapScheme.class;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        return EMPTY_METHOD_LIST;
    }

    public Offset gcForwardingPointerOffset() {
        return Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB);
    }

    public boolean isInLiveMemory(Address address) {

        if (vm().heap().isInGC()) { // this assumption needs to be proofed; basically it means that during GC both heaps are valid
            return true;
        }

        for (MaxHeapRegion heapRegion : vm().heap().heapRegions()) {
            if (heapRegion.memoryRegion().contains(address)) {
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) { // everything in from-space is dead
                    return false;
                }
                if (!heapRegion.memoryRegion().containsInAllocated(address)) {
                    // everything in to-space after the global allocation mark is dead
                    return false;
                }
                for (TeleNativeThread teleNativeThread : vm().teleProcess().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                    TeleThreadLocalsArea teleThreadLocalsArea = teleNativeThread.localsBlock().threadLocalsAreaFor(Safepoint.State.ENABLED);
                    if (teleThreadLocalsArea != null) {
                        Word tlabDisabledWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME);
                        Word tlabMarkWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME);
                        Word tlabTopWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME);
                        if (!tlabDisabledWord.isZero() && !tlabMarkWord.isZero() && !tlabTopWord.isZero()) {
                            if (address.greaterEqual(tlabMarkWord.asAddress()) && tlabTopWord.asAddress().greaterThan(address)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean isObjectForwarded(Pointer origin) {
        if (!origin.isZero()) {
            Pointer possibleForwardingPointer = vm().dataAccess().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                return true;
            }
        }
        return false;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return (!pointer.isZero()) && pointer.and(1).toLong() == 1;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return isForwardingPointer(pointer) ? pointer.minus(1) : pointer;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        if (!origin.isZero()) {
            Pointer possibleForwardingPointer = vm().dataAccess().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                final Pointer newCell = getTrueLocationFromPointer(possibleForwardingPointer);
                if (!newCell.isZero()) {
                    return Layout.generalLayout().cellToOrigin(newCell);
                }
            }
        }
        return origin;
    }
}
