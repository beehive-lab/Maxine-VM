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
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * Object initialization right after allocation.
 *
 * @author Bernd Mathiske
 */
public class Cell {

    /**
     * Write an initial array object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Object plantArray(Pointer cell, Size size, DynamicHub hub, int length) {
        DebugHeap.writeCellTag(cell);
        Memory.clear(cell, size);
        final Pointer origin = Layout.arrayCellToOrigin(cell);
        Layout.writeArrayLength(origin, length);
        Layout.writeHubReference(origin, Reference.fromJava(hub));
        return Reference.fromOrigin(origin).toJava();
    }

    /**
     * Write an initial array object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Object plantArray(Pointer cell, DynamicHub hub, int length) {
        final Size size = Layout.getArraySize(hub.classActor().componentClassActor().kind(), length);
        return plantArray(cell, size, hub, length);
    }

    /**
     * Write an initial tuple object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Object plantTuple(Pointer cell, Hub hub) {
        DebugHeap.writeCellTag(cell);
        Memory.clear(cell, hub.tupleSize());
        final Pointer origin = Layout.tupleCellToOrigin(cell);
        Layout.writeHubReference(origin, Reference.fromJava(hub));
        return Reference.fromOrigin(origin).toJava();
    }


    /**
     * Write an initial hybrid object image into an existing cell.
     */
    @INLINE
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Object plantHybrid(Pointer cell, Size size, DynamicHub hub) {
        DebugHeap.writeCellTag(cell);
        Memory.clear(cell, size);
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
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Hybrid plantExpandedHybrid(Pointer cell, Size size, Hybrid hybrid, int length) {
        DebugHeap.writeCellTag(cell);
        final Pointer oldOrigin = Reference.fromJava(hybrid).toOrigin();
        final Pointer oldCell = Layout.hybridOriginToCell(oldOrigin);
        final Size oldSize = Layout.hybridLayout().getArraySize(hybrid.length());

        final Pointer newOrigin = Layout.hybridCellToOrigin(cell);
        Memory.clear(newOrigin, size);
        Memory.copyBytes(oldCell, cell, oldSize);
        Layout.writeArrayLength(newOrigin, length);
        return UnsafeLoophole.cast(Reference.fromOrigin(newOrigin).toJava());
    }

    /**
     * Write a shallow copy of the given object into an existing cell.
     */
    @NO_SAFEPOINTS("avoid inconsistent object contents")
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
    @NO_SAFEPOINTS("avoid inconsistent object contents")
    public static Object plantClone(Pointer cell, Object object) {
        return plantClone(cell, Layout.size(Reference.fromJava(object)), object);
    }
}
