/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.debug;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.SpecialReferenceManager.JDKRefAlias;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
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

    @FOLD
    public static boolean isTagging() {
        return MaxineVM.isDebug() && vmConfig().heapScheme().supportsTagging();
    }

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
        if (isTagging()) {
            cell.setWord(-1, tagWord());
        }
    }

    public static Pointer skipCellPadding(Pointer cell) {
        if (MaxineVM.isDebug()) {
            Pointer cellStart = cell;
            while (cell.getWord().equals(padWord())) {
                cell = cell.plusWords(1);
            }
            if (!cell.equals(cellStart) && Heap.traceGC()) {
                Log.print("Skipped ");
                Log.print(cell.minus(cellStart).toInt());
                Log.println(" cell padding bytes");
            }
        }
        return cell;
    }

    @INLINE
    public static Pointer checkDebugCellTag(Address from, Pointer cell) {
        if (isTagging()) {
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

    private static void checkRefTag(Reference ref) {
        if (isTagging()) {
            if (!ref.isZero()) {
                final Pointer origin = ref.toOrigin();
                final Pointer cell = Layout.originToCell(origin);
                checkCellTag(cell, cell.minusWords(1).getWord(0));
            }
        }
    }

    public static void checkNonNullRefTag(Reference ref) {
        if (isTagging()) {
            final Pointer origin = ref.toOrigin();
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
     * @param ref the reference to be verified
     * @param space1 an address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions. This value is
     *            ignored if null.
     * @param space2 another address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions. This value is
     *            ignored if null.
     */
    public static void verifyRefAtIndex(Address address, int index, Reference ref, MemoryRegion space1, MemoryRegion space2) {
        if (ref.isZero()) {
            return;
        }
        if (isTagging()) {
            checkNonNullRefTag(ref);
        }
        final Pointer origin = ref.toOrigin();
        if (Heap.bootHeapRegion.contains(origin) || Code.contains(origin) || ImmortalHeap.contains(origin)) {
            return;
        }
        if (space1 != null && space1.contains(origin)) {
            return;
        }
        if (space2 != null && space2.contains(origin)) {
            return;
        }
        Log.print("invalid ref: ");
        Log.print(origin.asAddress());
        if (!address.isZero()) {
            Log.print(" @ ");
            Log.print(address);
            Log.print("+");
            Log.print(index * Word.size());
        }
        Log.println();
        FatalError.unexpected("invalid ref");
    }

    private static Hub checkHub(Pointer origin, MemoryRegion space) {
        final Reference hubRef = Layout.readHubReference(origin);
        FatalError.check(!hubRef.isZero(), "null hub");
        final int hubIndex = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).dividedBy(Word.size()).toInt();
        verifyRefAtIndex(origin, hubIndex, hubRef, space, null);
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());

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
     *            {@link #verifyRefAtIndex(Address, int, Reference, MemoryRegion, MemoryRegion)} for a reference value denoted by a base
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

            if (hub.classActor.isSpecialReference()) {
                JDKRefAlias refAlias = SpecialReferenceManager.asJDKRefAlias(Reference.fromOrigin(origin).toJava());
                if (refAlias.discovered != null) {
                    Log.print("Special reference of type ");
                    Log.print(hub.classActor.name.string);
                    Log.print(" at ");
                    Log.print(cell);
                    Log.print(" has non-null value for 'discovered' field: ");
                    Log.println(Reference.fromJava(refAlias.discovered).toOrigin());
                    FatalError.unexpected("invalid special ref");
                }
            }

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
                        verifyRefAtIndex(origin, index, Layout.getReference(origin, index), space, null);
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
        if (isTagging()) {
            return mark.plusWords(1);
        }
        return mark;
    }

    public static boolean isValidNonnullRef(Reference ref) {
        if (isTagging()) {
            final Pointer origin = ref.toOrigin();
            final Pointer cell = Layout.originToCell(origin);
            return isValidCellTag(cell.minusWords(1).getWord(0));
        }
        return true;
    }
}
