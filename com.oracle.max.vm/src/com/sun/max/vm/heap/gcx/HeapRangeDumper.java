/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.memory.Memory.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Debugging support for dumping a non-empty range of heap space to the log output stream.
 * The dump tolerates zapped areas and zero-filled areas, and indicates the range of addresses occupied by these areas.
 *  The dump also conservatively prints  the content of unparseable location in the range where an object is expected, and report
 *  reference field that don't comprise valid object addresses.
 *
 *  A heap range dumper may be used with @link {@link FatalError#setOnVMOpError(Runnable)} to dump a range of heap responsible for an
 *  unexpected memory fault. This is typically used to catch corrupted memory due to GC bugs: before iterating over an assumed iterable heap range,
 *  a heap dumper for the range may be passed to @link {@link FatalError#setOnVMOpError(Runnable)} so that if a memory error occur the
 *  range is dumped.
 *
 *  A HeapRangeDump may be specified a {@link #HeapRangeDumper.DumpRangeRefinement} object that can be used to refined the area to be dumped.
 *  For instance, heap using a cards may specify a refinement of the dumping range by reseting the bounds of the range to the card holding the corrupted
 *  cell or reference.
 *  @see RefineDumpRangeToCard
 */
public final class HeapRangeDumper implements Runnable {
    public static boolean DumpOnError = false;
    static {
        VMOptions.addFieldOption("-XX:", "DumpOnError", HeapRangeDumper.class, "Dump faulty heap range on error (Debug mode only)", Phase.PRISTINE);
    }

    /**
     * Handler to refine the range of heap to dump after a first unparseable location is found.
     * A handler may be set for a HeapRangeDumper. If the handler is set, and the dumper is in
     * "refinement" mode, it first iterates over the range originally specified
     * until a first broken heap location is found (e.g., an incorrectly formatted cell, or a zapped area).
     * The provided handler is then called to narrow the range to dump, and restart iterating with this new range,
     * with dumping on.
     */
    public interface DumpRangeRefinement {
        void refineRange(HeapRangeDumper heapDumper, Address unparsable);
    }

    private final Pointer dynamicHubHubPtr;
    private final Pointer staticHubHubPtr;
    /**
     * Bounds of the heap space. Used to conservatively identify potential heap reference.
     */
    private MemoryRegion heapBounds;
    /**
     * Start of the heap range to dump.
     */
    private Address start;
    /**
     * End of the heap range to dump.
     */
    private Address end;
    /**
     * Pre-allocated signal to interrupt heap range iteration.
     */
    private final RuntimeException unparsableAddressException;
    /**
     * When true, prints out references in dumped cells.
     */
    private boolean printReferences = true;

    /**
     * Create an instance of an HeapRangeDumper.
     *
     * @param heapBounds bounds of the contiguous virtual space range holding all valid heap references.
     */
    public HeapRangeDumper(MemoryRegion heapBounds) {
        this.heapBounds = heapBounds;
        dynamicHubHubPtr = Reference.fromJava(ClassActor.fromJava(DynamicHub.class).dynamicHub()).toOrigin();
        staticHubHubPtr = Reference.fromJava(ClassActor.fromJava(StaticHub.class).dynamicHub()).toOrigin();
        unparsableAddressException = new RuntimeException();
    }

    /**
     * Set the range of heap to dump.
     * This may be reset by a handler.
     * @param start
     * @param end
     */
    public void setRange(Address start, Address end) {
        FatalError.check(start.lessThan(end.minus(1)) && heapBounds.contains(start) && heapBounds.contains(end), "Invalid dumping range");
        this.start = start;
        this.end = end;
    }

    private boolean isValidHub(Pointer hubOrigin) {
        if (heapBounds.contains(hubOrigin)) {
            Pointer hubhubPtr = hubOrigin.getReference(PointerIndexAndHeaderVisitor.hubIndex()).toOrigin();
            return hubhubPtr.equals(dynamicHubHubPtr) ||  hubhubPtr.equals(staticHubHubPtr);
        }
        return false;
    }

    private Pointer skip(Pointer p, long pattern, String patternName) {
        final int maxIndex = end.minus(p).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        int i = 1;
        while (i < maxIndex) {
            if (p.getLong(i++) != pattern) {
                break;
            }
        }
        Pointer last = p.plusWords(i);
        Log.print(patternName);
        Log.print(" [");
        Log.print(p);
        Log.print(",");
        Log.print(last);
        Log.println("[");
        return last;
    }

    private DumpRangeRefinement handler;

    public void refineOnFirstUnparsableWith(DumpRangeRefinement handler) {
        this.handler = handler;
    }

    public void run() {
        if (handler != null) {
            findFirstUnparsableInRange();
        }
        dumpRange();
    }

    private boolean findFirstUnparsableInRange() {
        final Pointer zappedMarker = Pointer.fromLong(ZAPPED_MARKER);
        try {
            Pointer p = start.asPointer();
            while (p.lessThan(end)) {
                Pointer origin = Layout.cellToOrigin(p);
                Pointer hubOrigin = origin.getReference(PointerIndexAndHeaderVisitor.hubIndex()).toOrigin();
                if (hubOrigin.isZero() || hubOrigin.equals(zappedMarker)) {
                    handler.refineRange(this, origin);
                    return true;
                }
                if (isValidHub(hubOrigin)) {
                    final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
                    if (hub == HeapFreeChunk.heapFreeChunkHub()) {
                        Size chunkSize = HeapFreeChunk.getFreechunkSize(p);
                        p = p.plus(chunkSize);
                        continue;
                    }
                    // Cell start
                    final SpecificLayout specificLayout = hub.specificLayout;
                    if (specificLayout.isTupleLayout()) {
                        TupleReferenceMap.visitReferences(hub, origin, checkUnparsableVisitor, start, end);
                        if (hub.isJLRReference) {
                            checkUnparsableVisitor.visit(origin, SpecialReferenceManager.referentIndex());
                        }
                        p = p.plus(hub.tupleSize);
                        continue;
                    }
                    Size size = Layout.size(origin);
                    if (specificLayout.isHybridLayout()) {
                        TupleReferenceMap.visitReferences(hub, origin, checkUnparsableVisitor, start, end);
                    } else if (specificLayout.isReferenceArrayLayout()) {
                        visitReferenceArray(origin, checkUnparsableVisitor);
                    }
                    p = p.plus(size);
                } else {
                    // non-zero, non-zapped, invalid hub here. Just print addressed pattern  if it looked like a heap reference.
                    handler.refineRange(this, origin);
                    return true;
                }
            }
        } catch (RuntimeException e) {
            if (e == unparsableAddressException) {
                return true;
            }
        }
        return false;
    }

    private void dumpRange() {
        final Pointer zappedMarker = Pointer.fromLong(ZAPPED_MARKER);
        Pointer p = start.asPointer();
        final boolean lockDisabledSafepoints = Log.lock();
        while (p.lessThan(end)) {
            Pointer origin = Layout.cellToOrigin(p);
            Pointer hubOrigin = origin.getReference(PointerIndexAndHeaderVisitor.hubIndex()).toOrigin();
            if (hubOrigin.equals(zappedMarker)) {
                // Skip zap markers.
                p = skip(origin, ZAPPED_MARKER, "ZAPPED AREA");
                // Assume the first non-zapped location is an object origin, so loop back
                continue;
            }
            if (hubOrigin.isZero()) {
                // Skip zeroed out area.
                p = skip(origin, 0L, "Zeroed-out AREA");
                // Assume the first non-zapped location is an object origin, so loop back
                continue;
            }
            if (isValidHub(hubOrigin)) {
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
                if (hub == HeapFreeChunk.heapFreeChunkHub()) {
                    Size chunkSize = HeapFreeChunk.getFreechunkSize(p);
                    Log.print("HeapFreeChunk"); Log.print(p); Log.print(" (size=");
                    Log.printToPowerOfTwoUnits(chunkSize);
                    Log.println(")");
                    p = p.plus(chunkSize);
                    continue;
                }
                // Cell start
                Log.print(origin);
                final SpecificLayout specificLayout = hub.specificLayout;
                if (specificLayout.isTupleLayout()) {
                    Size size = hub.tupleSize;
                    Log.print(" T ("); Log.print(hubOrigin); Log.print(", size = "); Log.print(size.toInt()); Log.println(")");
                    if (printReferences) {
                        TupleReferenceMap.visitReferences(hub, origin, dumpVisitor, start, end);
                        if (hub.isJLRReference) {
                            Log.print("<s>     ");
                            printRef(origin, SpecialReferenceManager.referentIndex());
                        }
                    }
                    p = p.plus(size);
                    continue;
                }
                Size size = Layout.size(origin);
                if (specificLayout.isHybridLayout()) {
                    Log.print(" H ("); Log.print(hubOrigin); Log.print(", size = "); Log.print(size.toInt()); Log.println(")");
                    if (printReferences) {
                        TupleReferenceMap.visitReferences(hub, origin, dumpVisitor, start, end);
                    }
                } else if (specificLayout.isReferenceArrayLayout()) {
                    Log.print("AR ("); Log.print(hubOrigin); Log.print(", size = "); Log.print(size.toInt()); Log.println(")");
                    if (printReferences) {
                        visitReferenceArray(origin, dumpVisitor);
                    }
                } else {
                    Log.print("AS ("); Log.print(hubOrigin); Log.print(", size = "); Log.print(size.toInt()); Log.println(")");
                }
                p = p.plus(size);
            } else {
                // non-zero, non-zapped, invalid hub here. Just print addressed pattern  if it looked like a heap reference.
                Log.print(origin);
                Log.print(" ->");
                if (heapBounds.contains(hubOrigin)) {
                    Log.print("  ");
                    Log.print(hubOrigin);
                }
                Log.println();
                p = p.plusWords(1);
            }
        }
        Log.unlock(lockDisabledSafepoints);
    }

    private void printRef(Pointer pointer, int wordIndex) {
        Log.print(pointer.plusWords(wordIndex)); Log.print(" : ");
        final Address referencedCell = pointer.getWord(wordIndex).asAddress();
        Log.print(referencedCell);
        if (!referencedCell.isZero()) {
            Log.print(" R = "); Log.print(RegionTable.theRegionTable().regionID(referencedCell));
        }
        Log.println();
    }

    PointerIndexAndHeaderVisitor dumpVisitor = new PointerIndexAndHeaderVisitor() {
        @Override
        public void visit(Pointer pointer, int wordIndex) {
            Log.print("        ");
            printRef(pointer, wordIndex);
        }
    };

    PointerIndexAndHeaderVisitor checkUnparsableVisitor = new PointerIndexAndHeaderVisitor() {
        @Override
        public void visit(Pointer pointer, int wordIndex) {
            Pointer referencedOrigin = Layout.cellToOrigin(pointer.getWord(wordIndex).asPointer());
            if (referencedOrigin.isZero() || referencedOrigin.equals(Pointer.fromLong(ZAPPED_MARKER)) || !heapBounds.contains(referencedOrigin)) {
                handler.refineRange(HeapRangeDumper.this, pointer.plusWords(wordIndex));
                throw unparsableAddressException;
            }
            Pointer hubOrigin = referencedOrigin.getWord(hubIndex()).asPointer();
            if (hubOrigin.isZero() || hubOrigin.equals(Pointer.fromLong(ZAPPED_MARKER)) || !isValidHub(hubOrigin)) {
                handler.refineRange(HeapRangeDumper.this, pointer.plusWords(wordIndex));
                throw unparsableAddressException;
            }
        }
    };

    private void visitReferenceArray(Pointer refArrayOrigin, PointerIndexAndHeaderVisitor visitor) {
        final int length = Layout.readArrayLength(refArrayOrigin);
        final Address firstElementAddr = refArrayOrigin.plusWords(PointerIndexAndHeaderVisitor.firstElementIndex());
        final Address endOfArrayAddr = firstElementAddr.plusWords(length);
        final int firstIndex = firstElementAddr.lessEqual(start) ? start.minus(firstElementAddr).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : 0;
        final int endIndex = endOfArrayAddr.greaterThan(end) ? end.minus(firstElementAddr).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : length;
        for (int i = firstIndex; i < endIndex; i++) {
            visitor.visit(refArrayOrigin, i);
        }
    }
}
