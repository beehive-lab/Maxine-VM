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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;

/**
 * Simple free heap space management.
 * Nothing ambitious, just to get going and test the tracing algorithm of the future hybrid mark-sweep-evacuate.
 *
 * @author Laurent Daynes.
 */
public class FreeHeapSpaceManager extends HeapSweeper {
    private static final VMIntOption largeObjectsMinSizeOption =
        register(new VMIntOption("-XX:LargeObjectsMinSize=", Size.K.times(16).toInt(),
                        "Minimum size to be treated as a large object"), MaxineVM.Phase.PRISTINE);

    private static final VMIntOption freeChunkMinSizeOption =
        register(new VMIntOption("-XX:FreeChunkMinSize=", 256,
                        "Minimum size of contiguous space considered for space reclamation." +
                        "Below this size, the space is ignored (dark matter)"),
                        MaxineVM.Phase.PRISTINE);

    private static final VMBooleanXXOption doImpreciseSweepOption = register(new VMBooleanXXOption("-XX:+",
                    "ImpreciseSweep",
                    "Perform imprecise sweeping phase"),
                    MaxineVM.Phase.PRISTINE);

    // Temporary tracing
    private static final boolean TraceSweep = true;

    /**
     * Minimum size to be treated as a large object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size minLargeObjectSize;

    /**
     * Log 2 of the maximum size to enter the first bin of free space.
     */
    int log2FirstBinSize;

    private static  final Size TINY_OBJECT_SIZE = Size.fromInt(Word.size() * 2);

    static interface AllocationFailureHandler {
        /**
         * Handle allocation failure.
         * API still in flux. Not pretty at the moment: a valid pointer to some allocated space of the requested size may be returned,
         * or null. The former is typically because allocation was performed by some other allocator. The latter is when the heap ran
         * out of space and space was reclaimed for the current allocator. The null value indicates that the allocator should retry allocating.
         *
         * @param allocator
         * @param size
         * @return
         */
        Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size);
        Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size, int alignment);
    }

    /**
     * A linear space allocator.
     * Allocate space linearly from a region of the heap.
     * Delegate to an AllocationFailureHandler when it cannot satisfy a
     * request.
     *
     * FIXME: needs HEADROOM like semi-space to make sure we're never left with not enough space
     * at the end of a chunk to plant a dead object (for heap parsability).
     */
    class HeapSpaceAllocator extends LinearAllocationMemoryRegion {
        /**
         * End of space allocator.
         */
        private Address end;

        /**
         * Allocation failure handler.
         */
        private AllocationFailureHandler allocationFailureHandler;

        /**
         * Maximum size one can allocate with this allocator. Request for size larger than this
         * gets delegated to the allocation failure handler.
         */
        @CONSTANT_WHEN_NOT_ZERO
        private Size sizeLimit;

        HeapSpaceAllocator(String description, AllocationFailureHandler allocationFailureHandler) {
            super(description);
            this.allocationFailureHandler = allocationFailureHandler;
        }

        void initialize(Address initialChunk, Size initialChunkSize, Size maxObjectSize) {
            sizeLimit = maxObjectSize;
            if (initialChunk.isZero()) {
                clear();
            } else {
                refill(initialChunk, initialChunkSize);
            }
        }

        // FIXME: concurrency
        void clear() {
            start = Address.zero();
            end = Address.zero();
            mark.set(Address.zero());
        }

        // FIXME: concurrency
        void refill(Address chunk, Size chunkSize) {
            start = chunk;
            size = chunkSize;
            end = chunk.plus(chunkSize);
            mark.set(start);
        }

        Size freeSpaceLeft() {
            return size.minus(used());
        }

        @INLINE
        private Pointer top() {
            return mark.get().asPointer();
        }

        private boolean isLarge(Size size) {
            return size.greaterThan(sizeLimit);
        }

        /**
         * Allocate space of the specified size.
         *
         * @param size
         * @return
         */
        final Pointer allocate(Size size) {
            if (MaxineVM.isDebug()) {
                FatalError.check(size.isWordAligned(), "Size must be word aligned");
            }
            // Try first a non-blocking allocation out of the current chunk.
            // This may fail for a variety of reasons, all captured by the test
            // against the current chunk limit.
            Pointer cell;
            Pointer nextMark;
            size = DebugHeap.adjustForDebugTag(size.asPointer()).asSize();
            do {
                cell = top();
                nextMark = cell.plus(size);
                if (nextMark.greaterThan(end)) {
                    cell = allocationFailureHandler.handleAllocationFailure(this, size);
                    if (!cell.isZero()) {
                        return cell;
                    }
                    // loop back to retry.
                    continue;
                }
            } while (mark.compareAndSwap(cell, nextMark) != cell);
            return DebugHeap.adjustForDebugTag(cell);
        }

        @INLINE
        private Pointer setTopToEnd() {
            Pointer cell;
            do {
                cell = top();
                if (cell.equals(end)) {
                    // Already at end
                    return cell;
                }
            } while(mark.compareAndSwap(cell, end) != cell);
            return cell;
        }

        /**
         * Fill up the allocator and return address of its allocation mark
         * before filling.
         * This is used to ease concurrent refill: a thread requesting a refill first
         * grab a refill monitor, then fill up the allocator to force every racer to
         * to grab the refill monitor.
         * Refill can then be performed by changing first the bounds of the allocator, then
         * the allocation mark.
         *
         * @return
         */
        Pointer fillUp() {
            Pointer cell = setTopToEnd();
            if (cell.lessThan(end)) {
                HeapSchemeAdaptor.fillWithTaggedDeadObject(cell.asPointer(), end.asPointer());
            }
            return cell;
        }

        void makeParsable() {
            fillUp();
        }


        // FIXME: revisit this.
        Pointer allocateAligned(Size size, int alignment) {
            Pointer cell;
            Pointer alignedCell;
            Pointer nextMark;
            do {
                cell = top();
                alignedCell = cell.aligned(alignment).asPointer();
                if (alignedCell.minus(cell).lessThan(TINY_OBJECT_SIZE)) {
                    // Needs enough space to insert a dead object if we want
                    // the heap to be parseable.
                    alignedCell = alignedCell.plus(alignment);
                }
                nextMark = alignedCell.plus(size).asPointer();
                if (nextMark.greaterThan(end)) {
                    // FIXME: need this aligned as well!!!
                    if (isLarge(size)) {
                        return allocateLarge(size);
                    }
                    cell = allocationFailureHandler.handleAllocationFailure(this, size, alignment);
                    if (!cell.isZero()) {
                        return cell;
                    }
                    // loop back to retry.
                    continue;
                }
            } while(mark.compareAndSwap(cell, nextMark) != cell);
            // Make junk before aligned cell a dead object.
            if (alignedCell.greaterThan(cell)) {
                HeapSchemeAdaptor.fillWithTaggedDeadObject(cell, alignedCell);
            }
            return alignedCell;
        }
    }

    /**
     * The currently committed heap space.
     */
    private final RuntimeMemoryRegion committedHeapSpace;
    private boolean doImpreciseSweep;
    private final HeapSpaceAllocator largeObjectAllocator;
    private final HeapSpaceAllocator smallObjectAllocator;

    /**
     * Head of a linked list of free space recovered by the Sweeper.
     * Chunks are appended in the list only during sweeping.
     * The entries are therefore ordered from low to high addresses
     * (they are entered as the sweeper discover them).
     */
    final class FreeSpaceList {
        Address head;
        Address last;
        long totalSize;
        long totalChunks;

        FreeSpaceList() {
            head = Address.zero();
            last = Address.zero();
            totalSize = 0L;
            totalChunks = 0L;
        }
        void append(Address chunk, Size size) {
            HeapFreeChunk.format(chunk, size);
            if (last.isZero()) {
                head = chunk;
            } else {
                HeapFreeChunk.setFreeChunkNext(last, chunk);
            }
            last = chunk;
            totalSize += size.toLong();
            totalChunks++;
        }
    }

    /**
     * Free space is managed via segregated list. The minimum chunk size managed is minFreeChunkSize.
     */
    final FreeSpaceList [] freeChunkBins = new FreeSpaceList[10];

    /**
     * Total space in free chunks. This doesn't include space of chunks allocated to heap space allocator.
     */
    long totalFreeChunkSpace;

    @INLINE
    private int binIndex(Size size) {
        final long l = size.toLong() >> log2FirstBinSize;
        return  (l < freeChunkBins.length) ?  (int) l : (freeChunkBins.length - 1);
    }

    @INLINE
    private void recordFreeSpace(Address chunk, Size size) {
        freeChunkBins[binIndex(size)].append(chunk, size);
        totalFreeChunkSpace += size.toLong();
    }

    /**
     * Recording of free chunk of space.
     * Chunks are recording in different list depending on their size.
     * @param freeChunk
     * @param size
     */
    @Override
    public final void processDeadSpace(Address freeChunk, Size size) {
        recordFreeSpace(freeChunk, size);
    }

    private Size minReclaimableSpace;
    private Pointer endOfLastVisitedObject;
    private Size darkMatter = Size.zero();

    @INLINE
    private Pointer setEndOfLastVisitedObject(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        endOfLastVisitedObject = cell.plus(Layout.size(origin));
        return endOfLastVisitedObject;
    }

    void setMinReclaimableSpace(Size size) {
        minReclaimableSpace = size;
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        final Size deadSpace = liveObject.minus(endOfLastVisitedObject).asSize();
        if (deadSpace.greaterThan(minReclaimableSpace)) {
            recordFreeSpace(endOfLastVisitedObject, deadSpace);
        } else {
            darkMatter.plus(deadSpace);
        }
        endOfLastVisitedObject = liveObject.plus(Layout.size(Layout.cellToOrigin(liveObject)));
        return endOfLastVisitedObject;
    }

    private void printNotifiedGap(Pointer leftLiveObject, Pointer rightLiveObject, Pointer gapAddress, Size gapSize) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Gap between [");
        Log.print(leftLiveObject);
        Log.print(", ");
        Log.print(rightLiveObject);
        Log.print("] = @");
        Log.print(gapAddress);
        Log.print("(");
        Log.print(gapSize.toLong());
        Log.print(")");

        if (gapSize.greaterEqual(minReclaimableSpace)) {
            Log.print("=> bin #");
            Log.println(binIndex(gapSize));
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        Size deadSpace = rightLiveObject.minus(endOfLeftObject).asSize();
        if (TraceSweep) {
            printNotifiedGap(leftLiveObject, rightLiveObject, endOfLeftObject, deadSpace);
        }
        if (deadSpace.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(endOfLeftObject, deadSpace);
        } else {
            darkMatter.plus(deadSpace);
        }
        return rightLiveObject.plus(Layout.size(Layout.cellToOrigin(rightLiveObject)));
    }

    void print() {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Min reclaimable space: "); Log.print(minReclaimableSpace);
        Log.print("Dark matter: "); Log.print(darkMatter);
        for (int i = 0; i < freeChunkBins.length; i++) {
            Log.print("Bin ["); Log.print(i); Log.print("] (");
            Log.print(i << log2FirstBinSize); Log.print("<= chunk size < "); Log.print((i + 1) << log2FirstBinSize);
            Log.print(") total chunks: "); Log.print(freeChunkBins[i].totalChunks);
            Log.print("total space : "); Log.println(freeChunkBins[i].totalSize);
        }
        Log.unlock(lockDisabledSafepoints);
    }

    public RuntimeMemoryRegion committedHeapSpace() {
        return committedHeapSpace;
    }

    public FreeHeapSpaceManager() {
        committedHeapSpace = new RuntimeMemoryRegion("Heap");
        totalFreeChunkSpace = 0;
        for (int i = 0; i < freeChunkBins.length; i++) {
            freeChunkBins[i] = new FreeSpaceList();
        }
        smallObjectAllocator = new HeapSpaceAllocator("Small Objects Allocator", new SmallObjectAllocationFailureHandler());
        largeObjectAllocator = new HeapSpaceAllocator("Large Objects Allocator", new LargeObjectAllocationFailureHandler());
    }

    public void initialize(Address start, Size initSize) {
        committedHeapSpace.setStart(start);
        committedHeapSpace.setSize(initSize);
        // Round down to power of two.
        minLargeObjectSize = Size.fromInt(Integer.highestOneBit(largeObjectsMinSizeOption.getValue()));
        log2FirstBinSize = Integer.numberOfTrailingZeros(minLargeObjectSize.toInt());
        minReclaimableSpace = Size.fromInt(freeChunkMinSizeOption.getValue());
        doImpreciseSweep = doImpreciseSweepOption.getValue();
        smallObjectAllocator.initialize(committedHeapSpace.start(), committedHeapSpace.size(), minLargeObjectSize);
        largeObjectAllocator.initialize(Address.zero(), Size.zero(), Size.fromLong(Long.MAX_VALUE));
        InspectableHeapInfo.init(smallObjectAllocator, largeObjectAllocator);
        // InspectableHeapInfo.init(committedSpace);
    }

    public void reclaim(TricolorHeapMarker heapMarker) {
        if (doImpreciseSweep) {
            darkMatter.plus(heapMarker.impreciseSweep(this, minReclaimableSpace));
        } else {
            endOfLastVisitedObject = committedHeapSpace.start().asPointer();
            heapMarker.sweep(this);
        }
        print();
    }

    public void makeParsable() {
        smallObjectAllocator.makeParsable();
        largeObjectAllocator.makeParsable();
    }

    /**
     * Estimated free space left.
     * @return
     */
    public synchronized Size freeSpaceLeft() {
        return Size.fromLong(totalFreeChunkSpace).plus(smallObjectAllocator.freeSpaceLeft()).plus(largeObjectAllocator.freeSpaceLeft());
    }

    /**
     *
     */
    class SmallObjectAllocationFailureHandler implements AllocationFailureHandler {
        /**
         * Remaining free chunks assigned to this allocator.
         * We use an address and not a HeapFreeChunk
         * to keep this out of reach of live object tracing.
         */
        private Address freeChunks;

        /**
         * Size beyond which this allocator doesn't allocate and fallback to the FreeHeapSpace manager
         * for allocation.
         */
        /**
         * Find first fit and move it at the head of the list.
         * @param chunkList
         * @param size
         * @return the first chunk in the list that can allocate the specified size, or zero if none.
         */
        private Address findFirstFit(Size size) {
            // REVISIT THIS
            HeapFreeChunk prevChunk = null;
            HeapFreeChunk chunk = HeapFreeChunk.toHeapFreeChunk(freeChunks);
            HeapFreeChunk head = chunk;
            while (chunk != null) {
                if (chunk.size.greaterEqual(size)) {
                    // Found one. Move it ahead of the list.
                    prevChunk.next = chunk.next;
                    chunk.next = head;
                    return Reference.fromJava(chunk).toOrigin().asAddress();
                }
                prevChunk = chunk;
                chunk = chunk.next;
            }
            return Address.zero();
        }

        public Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size) {
            return handleAllocationFailure(allocator, size, Word.size());
        }

        @Override
        public Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size, int alignment) {
            if (allocator.isLarge(size)) {
                return allocateLarge(size);
            }
            if (!Heap.collectGarbage(size)) {
                // FIXME: handle the case where there isn't enough memory for this -- put in common the safety zone code of semi-space ?
                throw new OutOfMemoryError();
            }
            return null;
        }
    }

    class LargeObjectAllocationFailureHandler implements AllocationFailureHandler {

        @Override
        public Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size) {
            return handleAllocationFailure(allocator, size, Word.size());
        }

        @Override
        public Pointer handleAllocationFailure(HeapSpaceAllocator allocator, Size size, int alignment) {
            if (!Heap.collectGarbage(size)) {
                // FIXME: handle the case where there isn't enough memory for this -- put in common the safety zone code of semi-space ?
                throw new OutOfMemoryError();
            }
            return null; // Force allocation retry by caller.
        }
    }

    @INLINE
    public final Pointer allocate(Size size) {
        return smallObjectAllocator.allocate(size);
    }

    @INLINE
    public final Pointer allocateTLAB(Size size) {
        return smallObjectAllocator.allocate(size);
    }


    @INLINE
    public final Pointer allocateLarge(Size size) {
        return largeObjectAllocator.allocate(size);
    }
}
