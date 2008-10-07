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
public final class TeleRoots {

    private final TeleGripScheme _teleGripScheme;
    private final TeleVM _teleVM;
    private final WordArrayLayout _wordArrayLayout;

    TeleRoots(TeleGripScheme teleGripScheme) {
        _teleGripScheme = teleGripScheme;
        _teleVM = teleGripScheme.teleVM();
        _wordArrayLayout = _teleVM.layoutScheme().wordArrayLayout();
    }

    private Pointer _teleRootsPointer = Pointer.zero();

    private final Address[] _cachedRoots = new Address[TeleHeap.MAX_NUMBER_OF_ROOTS];
    private final BitSet _usedIndices = new BitSet();

    private RemoteTeleGrip teleRoots() {
        if (_teleRootsPointer.isZero()) {
            _teleRootsPointer = _teleVM.fields().TeleHeap_roots.staticTupleReference(_teleVM).toOrigin().plus(_teleVM.fields().TeleHeap_roots.fieldActor().offset());
        }
        return _teleGripScheme.createTemporaryRemoteTeleGrip(_teleVM.teleProcess().dataAccess().readWord(_teleRootsPointer).asAddress());
    }

    /**
     * Register a VM location in the VM's Inspector root table.
     */
    int register(Address rawGrip) {
        final int index = _usedIndices.nextClearBit(0);
        _usedIndices.set(index);
        // Local copy of root table
        WordArray.set(_cachedRoots, index, rawGrip);
        // Remote root table
        _wordArrayLayout.setWord(teleRoots(), index, rawGrip);
        return index;
    }

    /**
     * Remove a VM location from the VM's Tele root table.
     */
    void unregister(int index) {
        WordArray.set(_cachedRoots, index, Address.zero());
        _usedIndices.clear(index);
        _wordArrayLayout.setWord(teleRoots(), index, Word.zero());
    }

    /**
     * The remote location bits currently at a position in the Inspector root table.
     */
    Address getRawGrip(int index) {
        return WordArray.get(_cachedRoots, index);
    }

    /**
     * Flush local cache; copy remote contents of Inspectors' root table into Inspector's local cache.
     */
    void refresh() {
        final int numberOfIndices = _usedIndices.length();
        for (int i = 0; i < numberOfIndices; i++) {
            WordArray.set(_cachedRoots, i, _wordArrayLayout.getWord(teleRoots(), i).asAddress());
        }
    }

}
