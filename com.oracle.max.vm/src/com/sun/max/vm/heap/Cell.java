/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * Object initialization right after allocation.
 */
public class Cell {

    /**
     * Write an initial array object image into an existing cell.
     *
     * @param cell the start address of a cell containing an array
     * @param size the
     * @param hub the hub to be written into the hub word of the cell
     * @param length the number of elements in the array
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantArray(Pointer cell, Size size, DynamicHub hub, int length) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.arrayCellToOrigin(cell);
        Layout.writeArrayLength(origin, length);
        Layout.writeHubReference(origin, Reference.fromJava(hub));
        return Reference.fromOrigin(origin).toJava();
    }

    /**
     * Write an initial array object image into an existing cell.
     *
     * @param cell the start address of a cell containing an array
     * @param hub the hub to be written into the hub word of the cell
     * @param length the number of elements in the array
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantArray(Pointer cell, DynamicHub hub, int length) {
        final Size size = Layout.getArraySize(hub.classActor.componentClassActor().kind, length);
        return plantArray(cell, size, hub, length);
    }

    /**
     * Write an initial tuple object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantTuple(Pointer cell, Hub hub) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.tupleCellToOrigin(cell);
        Layout.writeHubReference(origin, Reference.fromJava(hub));
        return Reference.fromOrigin(origin).toJava();
    }

    /**
     * Write an initial hybrid object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantHybrid(Pointer cell, Size size, DynamicHub hub) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.hybridCellToOrigin(cell);
        Layout.writeHubReference(origin, Reference.fromJava(hub));
        Layout.writeArrayLength(origin, hub.firstWordIndex());
        return Reference.fromOrigin(origin).toJava();
    }

    /**
     * Clone a hybrid object that only holds the tuple part so far into image into a cell with additional space for the
     * array part.
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Hybrid plantExpandedHybrid(Pointer cell, Size size, Hybrid hybrid, int length) {
        DebugHeap.writeCellTag(cell);
        final Pointer oldOrigin = Reference.fromJava(hybrid).toOrigin();
        final Pointer oldCell = Layout.hybridOriginToCell(oldOrigin);
        final Size oldSize = Layout.hybridLayout().getArraySize(hybrid.length());

        final Pointer newOrigin = Layout.hybridCellToOrigin(cell);
        Memory.copyBytes(oldCell, cell, oldSize);
        Layout.writeArrayLength(newOrigin, length);
        return UnsafeCast.asHybrid(Reference.fromOrigin(newOrigin).toJava());
    }

    /**
     * Write a shallow copy of the given object into an existing cell.
     */
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantClone(Pointer cell, Size size, Object object) {
        DebugHeap.writeCellTag(cell);
        Memory.copyBytes(Layout.originToCell(Reference.fromJava(object).toOrigin()), cell, size);
        final Pointer origin = Layout.cellToOrigin(cell);
        Layout.writeMisc(origin, Word.zero());
        return Reference.fromOrigin(origin).toJava();
    }

    /**
     * Write a shallow copy of the given object into an existing cell.
     */
    @NO_SAFEPOINT_POLLS("avoid inconsistent object contents")
    public static Object plantClone(Pointer cell, Object object) {
        return plantClone(cell, Layout.size(Reference.fromJava(object)), object);
    }
}
