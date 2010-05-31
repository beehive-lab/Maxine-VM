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

import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.ms.*;

/**
 * Implementation details about the heap in the VM, specialized for the mark-sweep implementation.
 *
 * @author Laurent Daynes
 *
 */
public final class TeleMSHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme {

    TeleMSHeapScheme(TeleVM teleVM) {
        super(teleVM);
    }

    public Class heapSchemeClass() {
        return MSHeapScheme.class;
    }

    public Offset gcForwardingPointerOffset() {
        // MS is a non-moving collector. Doesn't do any forwarding.
        return null;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        // MS is a non-moving collector. Doesn't do any forwarding.
        return origin;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return pointer;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return false;
    }

    public boolean isInLiveMemory(Address address) {
        if (vm().isInGC()) {
            // Unclear what the semantics of this should be during GC.
            // We should be able to tell past the marking phase if an address point to a live object.
            // But what about during the marking phase ? The only thing that can be told is that
            // what was dead before marking begin should still be dead during marking.
            return true;
        }
        // TODO:
        // This requires the inspector to know intimately about the heap structures.
        // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
        // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
        // all reusable free space as such (instead of the chunk of free list as is done now). in any case.
        return true;
    }

    public boolean isObjectForwarded(Pointer origin) {
        return false;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        // TODO
        return Collections.emptyList();
    }

}
