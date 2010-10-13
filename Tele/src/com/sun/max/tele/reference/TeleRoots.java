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
package com.sun.max.tele.reference;

import static com.sun.max.vm.VMConfiguration.*;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.tele.*;

/**
 * Access to and management of a special array in the VM
 * that holds GC roots on behalf of references held in the Inspector.
 * <br>
 * Remote references held by the inspector are implemented as a
 * handle that identifies an entry in this table, which contains actual
 * memory addresses in the VM.  In order to track object movement
 * across GC invocations, a mirror of this table is maintained in the
 * VM, where it can be updated by the GC.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 *
 * @see InspectableHeapInfo
 * @see TeleHeap
 */
public final class TeleRoots extends AbstractTeleVMHolder implements TeleVMCache {


    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private final TeleReferenceScheme teleReferenceScheme;
    private final WordArrayLayout wordArrayLayout;

    private final Address[] cachedRoots = new Address[InspectableHeapInfo.MAX_NUMBER_OF_ROOTS];
    private final BitSet usedIndices = new BitSet();

    TeleRoots(TeleReferenceScheme teleReferenceScheme) {
        super(teleReferenceScheme.vm());
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleReferenceScheme = teleReferenceScheme;
        this.wordArrayLayout = vmConfig().layoutScheme().wordArrayLayout;
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    public void updateCache() {
        updateTracer.begin();
        // Flush local cache; copy remote contents of Inspectors' root table into Inspector's local cache.
        final int numberOfIndices = usedIndices.length();
        for (int i = 0; i < numberOfIndices; i++) {
            WordArray.set(cachedRoots, i, teleRootsReference().getWord(0, i).asAddress());
        }
        updateTracer.end(null);
    }


    private RemoteTeleReference teleRootsReference() {
        return teleReferenceScheme.createTemporaryRemoteTeleReference(vm().dataAccess().readWord(heap().teleRootsPointer()).asAddress());
    }

    /**
     * Register a VM location in the VM's Inspector root table.
     */
    int register(Address rawReference) {
        final int index = usedIndices.nextClearBit(0);
        usedIndices.set(index);
        // Local copy of root table
        WordArray.set(cachedRoots, index, rawReference);
        // Remote root table
        teleRootsReference().setWord(0, index, rawReference);
        return index;
    }

    /**
     * Remove a VM location from the VM's Tele root table.
     */
    void unregister(int index) {
        WordArray.set(cachedRoots, index, Address.zero());
        usedIndices.clear(index);
        teleRootsReference().setWord(0, index, Word.zero());
    }

    /**
     * The remote location bits currently at a position in the Inspector root table.
     */
    Address getRawReference(int index) {
        return WordArray.get(cachedRoots, index).asAddress();
    }

}
