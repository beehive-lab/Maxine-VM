/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.heap;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * The special region describing the heap in the boot image.
 */
public class BootHeapRegion extends LinearAllocatorHeapRegion {

    private byte[] referenceMapBytes;

    private Pointer referenceMap;

    private java.lang.ref.Reference[] specialReferences = {};

    public BootHeapRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    @HOSTED_ONLY
    public void init(byte[] refMap, java.lang.ref.Reference[] specialRefs) {
        ProgramError.check(Address.fromInt(refMap.length).isWordAligned(), "Boot heap reference map must have word-aligned size");
        this.referenceMapBytes = refMap;
        this.specialReferences = specialRefs;
    }

    public void visitReferences(PointerIndexVisitor pointerIndexVisitor) {
        if (referenceMap.isZero()) {
            referenceMap = ArrayAccess.elementPointer(referenceMapBytes, 0);
        }

        final Pointer refMap = referenceMap;
        final int referenceMapWords = UnsignedMath.divide(referenceMapBytes.length, Word.size());
        if (Heap.traceRootScanning()) {
            Log.print("Scanning boot heap: start=");
            Log.print(start());
            Log.print(", end=");
            Log.print(end());
            Log.print(", mutable references end=");
            Log.println(start().plus(referenceMapWords * Word.size()));
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, true);
        } else {
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, false);
        }

        for (java.lang.ref.Reference specialReference : specialReferences) {
            SpecialReferenceManager.discoverSpecialReference(Reference.fromJava(specialReference).toOrigin());
        }
    }
}

