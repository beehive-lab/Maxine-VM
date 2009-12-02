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
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * A collection of routines useful for placing and operating on special
 * tags in an object heap in aid of debugging a garbage collector in
 * a {@linkplain MaxineVM#isDebug() debug} build of the VM.
 *
 * @author Doug Simon
 */
public final class DebugHeap {

    private DebugHeap() {
    }

    private static final long LONG_OBJECT_TAG = 0xccccddddddddeeeeL;
    private static final long LONG_OBJECT_PAD = 0xeeeeddddddddccccL;

    private static final int INT_OBJECT_TAG = 0xcccceeee;
    private static final int INT_OBJECT_PAD = 0xeeeecccc;

    public static byte[] tagBytes(DataModel dataModel) {
        return dataModel.wordWidth == WordWidth.BITS_64 ? dataModel.toBytes(DebugHeap.LONG_OBJECT_TAG) : dataModel.toBytes(DebugHeap.INT_OBJECT_TAG);
    }

    @INLINE
    private static Word tagWord() {
        if (Word.width() == 64) {
            return Address.fromLong(LONG_OBJECT_TAG);
        }
        return Address.fromInt(INT_OBJECT_TAG);
    }

    @INLINE
    private static Word padWord() {
        if (Word.width() == 64) {
            return Address.fromLong(LONG_OBJECT_PAD);
        }
        return Address.fromInt(INT_OBJECT_PAD);
    }

    public static boolean isValidCellTag(Word word) {
        return word.equals(tagWord());
    }

    public static void writeCellPadding(Pointer start, int words) {
        FatalError.check(MaxineVM.isDebug(), "Can only be called in debug VM");
        Memory.setWords(start, words, padWord());
    }

    public static int writeCellPadding(Pointer start, Address end) {
        FatalError.check(MaxineVM.isDebug(), "Can only be called in debug VM");
        final int words = end.minus(start).dividedBy(Word.size()).toInt();
        Memory.setWords(start, words, padWord());
        return words;
    }

    @INLINE
    public static void writeCellTag(Pointer cell) {
        if (MaxineVM.isDebug()) {
            cell.setWord(-1, tagWord());
        }
    }

    public static Pointer skipCellPadding(Pointer cell) {
        if (MaxineVM.isDebug()) {
            while (cell.getWord().equals(padWord())) {
                cell = cell.plusWords(1);
            }
        }
        return cell;
    }

    @INLINE
    public static Pointer checkDebugCellTag(Address from, Pointer cell) {
        if (MaxineVM.isDebug()) {
            if (!isValidCellTag(cell.getWord(0))) {
                Log.print("Invalid object tag @ ");
                Log.print(cell);
                if (!from.isZero()) {
                    Log.print(" (start + ");
                    Log.print(cell.minus(from).asOffset().toInt());
                    Log.print(")");
                }
                Log.println();
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

    private static void checkGripTag(Grip grip) {
        if (MaxineVM.isDebug()) {
            if (!grip.isZero()) {
                final Pointer origin = grip.toOrigin();
                final Pointer cell = Layout.originToCell(origin);
                checkCellTag(cell, cell.minusWords(1).getWord(0));
            }
        }
    }

    public static void checkNonNullGripTag(Grip grip) {
        if (MaxineVM.isDebug()) {
            final Pointer origin = grip.toOrigin();
            final Pointer cell = Layout.originToCell(origin);
            checkCellTag(cell, cell.minusWords(1).getWord(0));
        }
    }

    /**
     * Verifies that a reference value denoted by a given base pointer and index points into a known object address space.
     *
     * @param address the base pointer of a reference. If this value is {@link Address#zero()}, then both it and {@code
     *            index} are ignored.
     * @param index the offset in words from {@code address} of the reference to be verified
     * @param grip the reference to be verified
     * @param space1 an address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions. This value is
     *            ignored if null.
     * @param space2 another address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions. This value is
     *            ignored if null.
     */
    public static void verifyGripAtIndex(Address address, int index, Grip grip, MemoryRegion space1, MemoryRegion space2) {
        if (grip.isZero()) {
            return;
        }
        if (MaxineVM.isDebug()) {
            checkNonNullGripTag(grip);
        }
        final Pointer origin = grip.toOrigin();
        if (Heap.bootHeapRegion.contains(origin) || Code.contains(origin) || ImmortalHeap.getImmortalHeap().contains(origin)) {
            return;
        }
        if (space1 != null && space1.contains(origin)) {
            return;
        }
        if (space2 != null && space2.contains(origin)) {
            return;
        }
        Log.print("invalid grip: ");
        Log.print(origin.asAddress());
        if (!address.isZero()) {
            Log.print(" @ ");
            Log.print(address);
            Log.print("+");
            Log.print(index * Word.size());
        }
        Log.println();
        FatalError.unexpected("invalid grip");
    }

    private static Hub checkHub(Pointer origin, MemoryRegion space) {
        final Grip hubGrip = Layout.readHubGrip(origin);
        FatalError.check(!hubGrip.isZero(), "null hub");
        final int hubIndex = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).dividedBy(Word.size()).toInt();
        verifyGripAtIndex(origin, hubIndex, hubGrip, space, null);
        final Hub hub = UnsafeCast.asHub(hubGrip.toJava());

        Hub h = hub;
        if (h instanceof StaticHub) {
            final ClassActor classActor = hub.classActor;
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }

        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
        return hub;
    }

    /**
     * Verifies that a given memory region consists of a contiguous objects is well formed.
     * The memory region is well formed if:
     *
     * a. It starts with an object preceded by a debug tag word.
     * b. Each object in the region is immediately succeeded by another object with the preceding debug tag.
     * c. Each reference embedded in an object points to an address in the given memory region, the boot
     *    {@linkplain Heap#bootHeapRegion heap} or {@linkplain Code#bootCodeRegion code} region.
     *
     * This method can only be called in a {@linkplain MaxineVM#isDebug() debug} VM.
     *
     * @param description a description of the region being verified
     * @param start the start of the memory region to verify
     * @param end the end of memory region
     * @param space the address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions.
     * @param verifier a {@link PointerOffsetVisitor} instance that will call
     *            {@link #verifyGripAtIndex(Address, int, Grip, MemoryRegion, MemoryRegion)} for a reference value denoted by a base
     *            pointer and offset
     */
    public static void verifyRegion(String description, Address start, final Address end, final MemoryRegion space, PointerIndexVisitor verifier) {

        if (Heap.traceGCPhases()) {
            Log.print("Verifying region ");
            Log.print(description);
            Log.print(" [");
            Log.print(start);
            Log.print(" .. ");
            Log.print(end);
            Log.println(")");
        }

        Pointer cell = start.asPointer();
        while (cell.lessThan(end)) {
            cell = skipCellPadding(cell);
            if (cell.greaterEqual(end)) {
                break;
            }
            cell = checkDebugCellTag(start, cell);

            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin, space);

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Verifying ");
                Log.print(hub.classActor.name.string);
                Log.print(" at ");
                Log.print(cell);
                Log.print(" [");
                Log.print(Layout.size(origin).toInt());
                Log.println(" bytes]");
                Log.unlock(lockDisabledSafepoints);
            }

            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, verifier);
                cell = cell.plus(hub.tupleSize);
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitReferences(hub, origin, verifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        verifyGripAtIndex(origin, index, Layout.getGrip(origin, index), space, null);
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }
    }

    /**
     * Increments a given allocation mark to reserve space for a {@linkplain writeCellTag debug tag} if
     * this is a {@linkplain MaxineVM#isDebug() debug} VM.
     *
     * @param mark an address at which a cell will be allocated
     * @return the given allocation address increment by 1 word if necessary
     */
    @INLINE
    public static Pointer adjustForDebugTag(Pointer mark) {
        if (MaxineVM.isDebug()) {
            return mark.plusWords(1);
        }
        return mark;
    }

}
