/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Instances of subclasses apply functionality to stack reference maps.
 * See {@link StackReferenceMapPreparer}.
 */
public abstract class FrameReferenceMapVisitor {

    /**
     * Addresses the bits belonging to a certain frame's reference map.
     *
     * @param cursor the cursor corresponding to the frame whose bits are dealt with
     * @param slotPointer the pointer to the slot that corresponds to bit 0 in the reference map
     * @param refMap an integer containing up to 32 reference map bits for up to 32 successive slots in the frame
     * @param numBits the number of bits in the reference map
     */
    public abstract void visitReferenceMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits);

    public abstract void logPrepareReferenceMap(TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer, String label);

    /**
     * @see StackReferenceMapPreparer#logReferenceMapByteBefore(int, byte, String)
     */
    public abstract void logReferenceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel);

    /**
     * @see StackReferenceMapPreparer#logReferenceMapByteAfter(Pointer, int, byte)
     */
    public abstract void logReferenceMapByteAfter(Pointer framePointer, int baseSlotIndex, final byte referenceMapByte);

    /**
     * Gets the reference-map index of a given stack slot (i.e. which bit in the reference map is correlated with the slot).
     *
     * @param slotAddress an address within the range of stack addresses covered by the reference map
     * @return the index of the bit for {@code slotAddress} in the reference map
     */
    public abstract int referenceMapBitIndex(Address slotAddress);

    public abstract void setBits(int baseSlotIndex, byte referenceMapByte);

}
