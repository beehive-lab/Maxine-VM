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
package com.sun.max.vm.debug;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

public final class DebugHeap {

    private DebugHeap() {
    }

    public static final int UNINITIALIZED = 0xdeadbeef;

    public static final long LONG_OBJECT_TAG = 0xccccddddddddeeeeL;

    public static final int INT_OBJECT_TAG = 0xcccceeee;

    public static boolean isValidCellTag(Word word) {
        if (Word.width() == WordWidth.BITS_64) {
            if (word.asAddress().toLong() == LONG_OBJECT_TAG) {
                return true;
            }
        }
        return word.asAddress().toInt() == INT_OBJECT_TAG;
    }

    @INLINE
    public static void writeCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (Word.width() == WordWidth.BITS_64) {
                cell.setLong(-1, DebugHeap.LONG_OBJECT_TAG);
            } else {
                cell.setInt(-1, DebugHeap.INT_OBJECT_TAG);
            }
        }
    }

    @INLINE
    public static Pointer checkDebugCellTag(RuntimeMemoryRegion from, Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!isValidCellTag(cell.getWord(0))) {
                Log.print("Invalid object tag @ ");
                Log.print(cell);
                Log.print("(start + ");
                Log.print(cell.minus(from.start()).asOffset().toInt());
                Log.println(")");
                FatalError.unexpected("INVALID CELL TAG");
            }
            return cell.plusWords(1);
        }
        return cell;
    }

    private static void checkCellTag(Pointer cell, Word tag) {
        if (!isValidCellTag(tag)) {
            Log.print("cell: ");
            Log.print(cell);
            Log.print("  origin: ");
            Log.print(Layout.cellToOrigin(cell));
            Log.println();
            FatalError.unexpected("missing object tag");
        }
    }

    public static void checkGripTag(Grip grip) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!grip.isZero()) {
                final Pointer origin = grip.toOrigin();
                final Pointer cell = origin.minusWords(1);
                checkCellTag(cell, cell.getWord(0));
            }
        }
    }

    @INLINE
    public static Pointer checkDebugCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            checkCellTag(cell, cell.getWord(0));
            return cell.plusWords(1);
        }
        return cell;
    }

    public static void verifyGripAtIndex(Address address, int index, Grip grip, MemoryRegion space) {
        if (grip.isZero()) {
            return;
        }
        checkGripTag(grip);
        final Pointer origin = grip.toOrigin();
        if (!(space.contains(origin) || Heap.bootHeapRegion().contains(origin) || Code.contains(origin))) {
            Log.print("invalid grip: ");
            Log.print(origin.asAddress());
            Log.print(" @ ");
            Log.print(address);
            Log.print(" + ");
            Log.print(index);
            Log.println();
            FatalError.unexpected("invalid grip");
        }
    }

    public static Hub checkHub(Pointer origin, MemoryRegion space) {
        final Grip hubGrip = Layout.readHubGrip(origin);
        FatalError.check(!hubGrip.isZero(), "null hub");
        verifyGripAtIndex(origin, 0, hubGrip, space); // zero is not strictly correctly here
        final Hub hub = UnsafeLoophole.cast(hubGrip.toJava());

        Hub h = hub;
        if (h instanceof StaticHub) {
            final ClassActor classActor = hub.classActor();
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }

        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
        return hub;
    }

    public static Pointer verifyRegion(Pointer start, final Address end, final MemoryRegion space, PointerOffsetVisitor verifier) {
        Pointer cell = start;
        while (cell.lessThan(end)) {
            cell = checkDebugCellTag(cell);

            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin, space);

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Verifying ");
                Log.print(hub.classActor().name().string());
                Log.print(" at ");
                Log.print(cell);
                Log.print(" [");
                Log.print(Layout.size(origin).toInt());
                Log.println(" bytes]");
                Log.unlock(lockDisabledSafepoints);
            }

            final SpecificLayout specificLayout = hub.specificLayout();
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, verifier);
                cell = cell.plus(hub.tupleSize());
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, verifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        verifyGripAtIndex(origin, index * Kind.REFERENCE.size(), Layout.getGrip(origin, index), space);
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }
        return cell;
    }

}
