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
package com.sun.max.tele.grip;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.tele.*;

/**
 * Manage the remote root table where roots can be added for remotely held references.
 *
 * @author Bernd Mathiske
 */
public final class TeleRoots extends AbstractTeleVMHolder{

    private final TeleGripScheme teleGripScheme;
    private final WordArrayLayout wordArrayLayout;

    TeleRoots(TeleGripScheme teleGripScheme) {
        super(teleGripScheme.teleVM());
        this.teleGripScheme = teleGripScheme;
        this.wordArrayLayout = teleGripScheme.teleVM().layoutScheme().wordArrayLayout;
    }

    // Points to the static field {@link TeleHeap#_roots TeleHeap._roots} in the {@link TeleVM}, assuming that the
    // static tuple of the class will not be relocated because it is in the boot image.
    private Pointer teleRootsPointer = Pointer.zero();

    private final Address[] cachedRoots = new Address[InspectableHeapInfo.MAX_NUMBER_OF_ROOTS];
    private final BitSet usedIndices = new BitSet();

    private RemoteTeleGrip teleRoots() {
        if (teleRootsPointer.isZero()) {
            final int offset = teleVM().fields().InspectableHeapInfo_roots.fieldActor().offset();
            teleRootsPointer = teleVM().fields().InspectableHeapInfo_roots.staticTupleReference(teleVM()).toOrigin().plus(offset);
        }
        return teleGripScheme.createTemporaryRemoteTeleGrip(teleVM().dataAccess().readWord(teleRootsPointer).asAddress());
    }


    /**
     * Register a VM location in the VM's Inspector root table.
     */
    int register(Address rawGrip) {
        final int index = usedIndices.nextClearBit(0);
        usedIndices.set(index);
        // Local copy of root table
        WordArray.set(cachedRoots, index, rawGrip);
        // Remote root table
        //wordArrayLayout.setWord(teleRoots(), index, rawGrip);
        teleRoots().setWord(0, index, rawGrip);
        return index;
    }

    /**
     * Remove a VM location from the VM's Tele root table.
     */
    void unregister(int index) {
        WordArray.set(cachedRoots, index, Address.zero());
        usedIndices.clear(index);
        //wordArrayLayout.setWord(teleRoots(), index, Word.zero());
        teleRoots().setWord(0, index, Word.zero());
    }

    /**
     * The remote location bits currently at a position in the Inspector root table.
     */
    Address getRawGrip(int index) {
        //WordArray.set(cachedRoots, index, teleRoots().getWord(0, index).asAddress());
        return WordArray.get(cachedRoots, index);
    }

    /**
     * Flush local cache; copy remote contents of Inspectors' root table into Inspector's local cache.
     */
    void refresh() {
        final int numberOfIndices = usedIndices.length();
        for (int i = 0; i < numberOfIndices; i++) {
            //WordArray.set(cachedRoots, i, wordArrayLayout.getWord(teleRoots(), i).asAddress());
            WordArray.set(cachedRoots, i, teleRoots().getWord(0, i).asAddress());
        }
    }

}
