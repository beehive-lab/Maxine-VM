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
package com.sun.max.vm.heap.debug;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.SpecialReferenceManager.JLRRAlias;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A collection of routines useful for placing and operating on special
 * tags in an object heap in aid of debugging a garbage collector in
 * a {@linkplain MaxineVM#isDebug() debug} build of the VM.
 *
 * These methods are common to all schemes.
 * TODO: check above assumption.
 */
public class DebugHeap {
    @FOLD
    public static boolean isTagging() {
        return MaxineVM.isDebug() && vmConfig().heapScheme().supportsTagging();
    }

    @FOLD
    public static boolean isPadding() {
        return MaxineVM.isDebug() && vmConfig().heapScheme().supportsPadding();
    }

    @FOLD
    private static int hubIndex() {
        return Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).dividedBy(Word.size()).toInt();
    }

    /**
     * Increments a given allocation mark to reserve space for a {@linkplain #writeCellTag(Pointer) debug tag} if
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

    /**
     * Checking the tag a cell.
     * A region start address may be supplied for tracing purposes if the cell reside in a specific region of memory.
     *
     * @param regionStart Address to the first byte of the memory region where the cell is allocated
     * @param cell cell whose tag is checked
     * @return Address to beginning of the cell stripped off its tag.
     */
    @INLINE
    public static Pointer checkDebugCellTag(Address regionStart, Pointer cell) {
        if (isTagging()) {
            if (!isValidCellTag(cell.getWord(0))) {
                Log.print("Invalid object tag @ ");
                Log.print(cell);
                if (!regionStart.isZero()) {
                    Log.print(" (start + ");
                    Log.print(cell.minus(regionStart).asOffset().toInt());
                    Log.print(")");
                }
                Log.println();
                FatalError.unexpected("INVALID CELL TAG");
            }
            return cell.plusWords(1);
        }
        return cell;
    }

    public static boolean isValidCellTag(Word word) {
        return word.equals(tagWord());
    }

    private static final long LONG_OBJECT_TAG = 0xccccddddddddeeeeL;
    private static final long LONG_OBJECT_PAD = 0xeeeeddddddddccccL;

    private static final int INT_OBJECT_TAG = 0xcccceeee;
    private static final int INT_OBJECT_PAD = 0xeeeecccc;

    public static byte[] tagBytes(DataModel dataModel) {
        return dataModel.wordWidth == WordWidth.BITS_64 ? dataModel.toBytes(LONG_OBJECT_TAG) : dataModel.toBytes(INT_OBJECT_TAG);
    }

    @INLINE
    private static Word tagWord() {
        if (Word.width() == 64) {
            return Address.fromLong(LONG_OBJECT_TAG);
        }
        return Address.fromInt(INT_OBJECT_TAG);
    }

    @INLINE
    protected
    static Word padWord() {
        if (Word.width() == 64) {
            return Address.fromLong(LONG_OBJECT_PAD);
        }
        return Address.fromInt(INT_OBJECT_PAD);
    }

    public static void writeCellPadding(Pointer start, int words) {
        FatalError.check(isPadding(), "Can only be called in debug VM");
        Memory.setWords(start, words, padWord());
    }

    public static int writeCellPadding(Pointer start, Address end) {
        FatalError.check(isPadding(), "Can only be called in debug VM");
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

    public static final class ReferenceFinder extends PointerIndexVisitor implements CellVisitor {
        private boolean fatalErrorIfFound = false;
        private Address searched;
        private long visitedRefsCount;
        private long visitedCellsCount;

        public ReferenceFinder(boolean fatalErrorIfFound) {
            this.fatalErrorIfFound = fatalErrorIfFound;
        }

        public void setSearchedReference(Address origin) {
            this.searched = origin;
            this.visitedRefsCount = 0L;
            this.visitedCellsCount = 0L;
        }

        public void setSearchedReference(Address origin, boolean fatalErrorIfFound) {
            this.searched = origin;
            this.visitedRefsCount = 0L;
            this.visitedCellsCount = 0L;
            this.fatalErrorIfFound = fatalErrorIfFound;
        }

        public long visitedRefsCount() {
            return visitedRefsCount;
        }
        public long visitedCellsCount() {
            return visitedCellsCount;
        }
        private void reportFoundRef(Pointer pointer, int wordIndex) {
            Log.print("found reference ");
            Log.print(searched);
            Log.print(" @ ");
            Log.print(pointer);
            Log.print(" + ");
            Log.println(wordIndex);
            FatalError.breakpoint();
            if (fatalErrorIfFound) {
                FatalError.unexpected("unexpected reference");
            }
        }
        @INLINE
        private void checkRef(Pointer pointer, int wordIndex) {
            visitedRefsCount++;
            if (pointer.getWord(wordIndex).asAddress().equals(searched)) {
                reportFoundRef(pointer, wordIndex);
            }
        }
        @Override
        public void visit(Pointer pointer, int wordIndex) {
            checkRef(pointer, wordIndex);
        }

        @Override
        public Pointer visitCell(Pointer cell) {
            final Pointer origin = Layout.cellToOrigin(cell);
            visitedCellsCount++;
            checkRef(origin,  Layout.hubIndex());
            final Hub hub =  Layout.getHub(origin);
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout == Layout.tupleLayout()) {
                //TupleReferenceMap.visitReferences(hub, origin, this);
                hub.visitMappedReferences(origin, this);
                if (hub.isJLRReference) {
                    checkRef(origin, SpecialReferenceManager.referentIndex());
                }
                return cell.plus(hub.tupleSize);
            }
            final int length = Layout.readArrayLength(origin);
            if (specificLayout == Layout.hybridLayout()) {
                hub.visitMappedReferences(origin, this);
                //TupleReferenceMap.visitReferences(hub, origin, this);
                return cell.plus(Layout.hybridLayout().getArraySize(length));
            } else if (specificLayout == Layout.referenceArrayLayout()) {
                int wordIndex = Layout.firstElementIndex();
                final int endIndex = wordIndex + length;
                while (wordIndex < endIndex) {
                    checkRef(origin, wordIndex++);
                }
                return cell.plus(Layout.referenceArrayLayout().getArraySize(Kind.REFERENCE, length));
            }
            return cell.plus(Layout.size(origin));
        }
    }

    /**
     * Verifies that a reference points into the bounds of  valid heap space.
     * This is a gross verification, i.e., it doesn't verify if the reference is actual to a valid object in a live part of the heap.
     */
    public static final class RefVerifier extends PointerIndexVisitor {
        /**
         * An address space in which valid objects can be found apart from the boot {@linkplain Heap#bootHeapRegion heap} and
         * {@linkplain Code#bootCodeRegion code} regions. This value is ignored if {@code null}.
         */
        MemoryRegion space1;
        /**
         * A second address space in which valid objects can be found apart from the boot {@linkplain Heap#bootHeapRegion heap} and
         * {@linkplain Code#bootCodeRegion code} regions. This value is ignored if {@code null}.
         */
        MemoryRegion space2;
        /**
         * Array holding references values (other than {@code null}  that must be excluded from verification.
         */
        long [] exclusions;

        public RefVerifier() {
        }

        public RefVerifier(MemoryRegion space) {
            this.space1 = space;
        }

        public RefVerifier(MemoryRegion space1, MemoryRegion space2) {
            this.space1 = space1;
            this.space2 = space2;
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
        public void verifyRefAtIndex(Address address, int index, Reference ref) {
            if (ref.isZero()) {
                return;
            }
            if (isTagging()) {
                checkNonNullRefTag(ref);
            }
            if (CodePointer.isCodePointer(ref)) {
                return;
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
            if (exclusions != null) {
                for (int i = 0; i < exclusions.length; i++) {
                    if (origin.equals(Pointer.fromLong(exclusions[i]))) {
                        return;
                    }
                }
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

        @Override
        public void visit(Pointer pointer, int index) {
            verifyRefAtIndex(pointer, index, pointer.getReference(index));
        }

        /**
         * Set the verifier to include one extra valid memory region in addition to the boot, code and immortal heap regions.
         * This replaces all previous heap space (except for the default one).
         * @param space a memory region
         */
        public void setValidHeapSpace(MemoryRegion space) {
            this.space1 = space;
            this.space2 = null;
        }

        /**
         * Set the verifier to include two extra valid memory region in addition to the boot, code and immortal heap regions.
         * This replaces all previous heap space (except for the default ones).
         * @param space1 a memory region
         * @param space2 a memory region
         */
        public void setValidSpaces(MemoryRegion space1, MemoryRegion space2) {
            this.space1 = space1;
            this.space2 = space2;
        }

        public void setExclusions(long [] exclusions) {
            if (exclusions != null) {
                for (int i = 0; i < exclusions.length; i++) {
                    if (exclusions[i] == 0) {
                        FatalError.unexpected("Can't set null as a special address");
                    }
                }
            }
            this.exclusions = exclusions;
        }
    }

    protected static Hub checkHub(Pointer origin, RefVerifier refVerifier) {
        final Reference hubRef = Layout.readHubReference(origin);
        FatalError.check(!hubRef.isZero(), "null hub");
        refVerifier.visit(origin, hubIndex());
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

    public static boolean isValidNonnullRef(Reference ref) {
        if (isTagging()) {
            final Pointer origin = ref.toOrigin();
            final Pointer cell = Layout.originToCell(origin);
            return isValidCellTag(cell.minusWords(1).getWord(0));
        }
        return true;
    }

    /**
     * Verifies that a given memory region consisting of contiguous objects is well formed.
     * The memory region is well formed if each reference embedded in an object points to an address in the given memory region, the boot
     *    {@linkplain Heap#bootHeapRegion heap} or {@linkplain Code#bootCodeRegion code} region.
     * In addition if the heap scheme supports tagging, the memory region is well formed if
     * <ul>
     *    <li>It starts with an object preceded by a debug tag word.</li>
     *    <li>Each object in the region is immediately succeeded by another object with the preceding debug tag.</li>
     * </ul>
     * This method doesn't do anything if not called in a {@linkplain MaxineVM#isDebug() debug} VM.
     *
     * @param region the region being verified
     * @param start the start of the memory region to verify
     * @param end the end of memory region
     * @param verifier a {@link PointerIndexVisitor} instance that will call
     *            {@link #verifyRefAtIndex(Address, int, Reference, MemoryRegion, MemoryRegion)} for a reference value denoted by a base
     *            pointer and offset
     */
    public static void verifyRegion(MemoryRegion region, Address start, final Address end, RefVerifier verifier, DetailLogger detailLogger) {
        if (!MaxineVM.isDebug()) {
            return;
        }
        Pointer cell = start.asPointer();
        while (cell.lessThan(end)) {
            if (isPadding()) {
                cell = skipCellPadding(cell, detailLogger);
            }
            if (cell.greaterEqual(end)) {
                break;
            }
            cell = checkDebugCellTag(start, cell);

            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin, verifier);

            if (hub.isJLRReference) {
                JLRRAlias refAlias = SpecialReferenceManager.asJLRRAlias(Reference.fromOrigin(origin).toJava());
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

            if (detailLogger.enabled()) {
                detailLogger.logVerifyObject(hub.classActor, cell, Layout.size(origin).toInt());
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
                        verifier.visit(origin, Layout.firstElementIndex());
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }
    }

    private static Pointer skipCellPadding(Pointer cell, DetailLogger detailLogger) {
        if (isPadding()) {
            Pointer cellStart = cell;
            while (cell.getWord().equals(DebugHeap.padWord())) {
                cell = cell.plusWords(1);
            }
            if (!cell.equals(cellStart)) {
                if (detailLogger.enabled()) {
                    detailLogger.logSkipped(cell.minus(cellStart).toInt());
                }
            }
        }
        return cell;
    }

    @HOSTED_ONLY
    @VMLoggerInterface
    private interface DetailLoggerInterface {
        void visitCell(
            @VMLogParam(name = "cell") Pointer cell);

        // Use class ID and not ClassActor since the logger boiler plate will automatically log a class id that it converts back into a class actor name when tracing.
        // The latter involves a ClassID.toClassActor method call, a fairly complicated code with virtual call etc.,  which might come across forwarding reference and crash as a result.
        void forward(
            @VMLogParam(name = "classID") int classID,
            @VMLogParam(name = "at") Pointer at,
            @VMLogParam(name = "fromCell") Pointer fromCell,
            @VMLogParam(name = "toCell") Pointer toCell,
            @VMLogParam(name = "size") int size);

        void skipped(
            @VMLogParam(name = "padBytes") int padBytes);

        void verifyObject(
            @VMLogParam(name = "hubClassActor") ClassActor hubClassActor,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") int size);
    }

    /**
     * A generic logger for moving collector details.
     * Provides method for logging visited cells, forwarded objects, etc.
     *
     */
    public static final class DetailLogger extends DetailLoggerAuto {

        public DetailLogger() {
            super("GCDetail", "detailed operation.");
        }

        @Override
        public void checkOptions() {
            super.checkOptions();
            checkDominantLoggerOptions(Heap.gcAllLogger);
        }

        @Override
        protected void traceVisitCell(Pointer cell) {
            Log.print("Visiting cell ");
            Log.println(cell);
        }

        @Override
        protected void traceForward(int classID, Pointer at, Pointer fromCell, Pointer toCell, int size) {
            Log.print("Forwarding <classId=");
            Log.print(classID);
            Log.print("> @");
            Log.print(at);
            Log.print(" from ");
            Log.print(fromCell);
            Log.print(" to ");
            Log.print(toCell);
            Log.print(" [");
            Log.print(size);
            Log.println(" bytes]");
        }

        @Override
        protected void traceSkipped(int padBytes) {
            Log.print("Skipped ");
            Log.print(padBytes);
            Log.println(" cell padding bytes");
        }

        @Override
        protected void traceVerifyObject(ClassActor hubClassActor, Pointer cell, int size) {
            Log.print("Verifying ");
            Log.print(hubClassActor.name.string);
            Log.print(" at ");
            Log.print(cell);
            Log.print(" [");
            Log.print(size);
            Log.println(" bytes]");
        }
    }

// START GENERATED CODE
    private static abstract class DetailLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Forward, Skipped, VerifyObject,
            VisitCell;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected DetailLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logForward(int classID, Pointer at, Pointer fromCell, Pointer toCell, int size) {
            log(Operation.Forward.ordinal(), intArg(classID), at, fromCell, toCell, intArg(size));
        }
        protected abstract void traceForward(int classID, Pointer at, Pointer fromCell, Pointer toCell, int size);

        @INLINE
        public final void logSkipped(int padBytes) {
            log(Operation.Skipped.ordinal(), intArg(padBytes));
        }
        protected abstract void traceSkipped(int padBytes);

        @INLINE
        public final void logVerifyObject(ClassActor hubClassActor, Pointer cell, int size) {
            log(Operation.VerifyObject.ordinal(), classActorArg(hubClassActor), cell, intArg(size));
        }
        protected abstract void traceVerifyObject(ClassActor hubClassActor, Pointer cell, int size);

        @INLINE
        public final void logVisitCell(Pointer cell) {
            log(Operation.VisitCell.ordinal(), cell);
        }
        protected abstract void traceVisitCell(Pointer cell);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Forward
                    traceForward(toInt(r, 1), toPointer(r, 2), toPointer(r, 3), toPointer(r, 4), toInt(r, 5));
                    break;
                }
                case 1: { //Skipped
                    traceSkipped(toInt(r, 1));
                    break;
                }
                case 2: { //VerifyObject
                    traceVerifyObject(toClassActor(r, 1), toPointer(r, 2), toInt(r, 3));
                    break;
                }
                case 3: { //VisitCell
                    traceVisitCell(toPointer(r, 1));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
