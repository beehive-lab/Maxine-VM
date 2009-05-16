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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;

/**
 * The special region describing the heap in the boot image.
 *
 * @author Doug Simon
 */
public class BootHeapRegion extends LinearAllocatorHeapRegion {

    private byte[] _referenceMapBytes;

    private Pointer _referenceMap;

    public BootHeapRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    @PROTOTYPE_ONLY
    public void setReferenceMap(byte[] referenceMap) {
        ProgramError.check(Address.fromInt(referenceMap.length).isAligned(), "Boot heap reference map must have word-aligned size");
        _referenceMapBytes = referenceMap;
    }

    public void visitPointers(PointerIndexVisitor pointerIndexVisitor) {
        if (_referenceMap.isZero()) {
            _referenceMap = Grip.fromJava(_referenceMapBytes).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(0));
        }
        final Pointer referenceMap = _referenceMap;
        final int referenceMapWords = _referenceMapBytes.length / Word.size();
        if (Heap.traceGCRootScanning()) {
            Log.print("Scanning boot heap: heap start=");
            Log.println(start());
        }
        for (int i = 0; i < referenceMapWords; ++i) {
            final Address refmapWord = referenceMap.getWord(i).asAddress();
            if (!refmapWord.isZero()) {
                for (int bitIndex = 0; bitIndex < Word.width().numberOfBits(); bitIndex++) {
                    final Address mask = Address.fromLong(1L << bitIndex);
                    if (!refmapWord.and(mask).isZero()) {
                        final int heapWordIndex = i * Word.size() + bitIndex;
                        if (Heap.traceGCRootScanning()) {
                            Log.print("  index=");
                            Log.print(heapWordIndex);
                            Log.print(", address=");
                            final Pointer address = start().asPointer().plus(heapWordIndex * Word.size());
                            Log.print(address);
                            Log.print(", value=");
                            Log.println(address.readWord(0));
                        }
                        pointerIndexVisitor.visitPointerIndex(start().asPointer(), heapWordIndex);
                    }
                }
            }
        }
    }
}

