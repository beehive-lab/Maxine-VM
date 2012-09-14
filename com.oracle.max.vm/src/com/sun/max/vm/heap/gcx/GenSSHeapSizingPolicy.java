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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * Simple heap sizing policy for a generational heap with a old generation made of two semi-spaces and a non-aging nursery.
 *
 * The policy computes the maximum and initial size of the heap based on the maximum and initial heap memory specified via
 * the <code>-Xmx</code> and <code>-Xms</code> command line arguments. Note that the memory specified via these command line argument
 * is not the effective heap size, due to the semi-space nature of the heap. The effective heap size is the young generation size plus the size of a single
 * old generation semi-space. The size of the young generation is computed as a percentage of this effective heap size.
 *
 * Resizing decision takes place after every full collection, and operates in two mode: normal and degraded.
 * During normal mode, the young generation is always sized to the specified maximum percentage of the heap size.
 * Heap size changes are driven by min/max ratio of free space in the old generation: if free space drops below a minimum threshold, the heap is increased;
 *  if it increases above a max threshold, the heap is shrunk.
 *
 * The policy transitions to degraded mode when the old generation is already max-ed up and doesn't have enough space to cover evacuation from the young generation.
 * The degraded mode operates by reducing the percentage of heap used by the young generation in order to reduce the evacuated amount at minor collections,
 * and re-distribute this space to the old generation (equally among the semi-space). This is similar to Appel's generational collector.
 *
 * An out of memory situation occurs when the minimum size for a young generation is met.
 */
public final class GenSSHeapSizingPolicy implements GenHeapSizingPolicy {
     /**
     * Minimal size of the young generation (5% of the effective heap size).
     */
    static final int MinYoungGenPercent = 5;
    /**
     * Absolute lowest bound for the size of a young generation. If MinYoungGenPercent * heap size is lower than this bound, then the bound is used instead.
     * The lowest bound must be enough to fire a out-of-memory exception.
     */
    static final Size MinYoungGenSize = Size.K.times(128);

    public static final HeapSizingPolicyLogger logger = new HeapSizingPolicyLogger();

    /**
     * Minimum effective heap size a generational heap can operate with.
     * @return an effective heap size.
     */
    private static Size minEffectiveHeapSize() { return MinYoungGenSize.times(2); }

    /**
     * Log 2 of the size generations are always aligned to.
     * Generation are always aligned to some power of two, typically a page or a power of two of the page size.
     */
    private int log2Alignment;

    /**
     * Size to which generations are aligned to. In other words, <pre>unitSize = Size.fromInt(1).shiftedLeft({@link #log2Alignment})</pre>
     */
    private Size unitSize;

    /**
     * Max percentage of free space after GC to avoid shrinking.
     */
    private int maxFreePercent = 70;

    /**
     * Min percentage of heap free after GC to increase heap size. Must not be lower than {@link #youngGenMaxHeapPercentage}
     */
    private int minFreePercent = 40;

    /**
     * Min change in heap size due to GC.
     */
    private Size minHeapDeltaBytes = Size.K.times(128);

    private Size minYoungGenDelta;

    /**
     *  Maximum percentage of effective heap size the young generation can occupy.
     *  Used in normal mode to size the young generation.
     */
    private int youngGenMaxHeapPercentage;

    private Size initHeapSize;
    private Size maxHeapSize;

    /**
     * Size of the old generation when the heap is at its maximum size.
     * This is used as a threshold for degraded mode. Past that point,
     * the policy can only reduce the size of the young generation (by reducing {@link #youngGenHeapPercentage})
     * to give more breathing room to the old generation.
     */
    private Size maxHeapOldGenSize;
    /**
     * True if in normal mode, false if degraded.
     */
    private boolean normalMode = true;

    private boolean outOfMemory = false;

    @INSPECTED
    private boolean minorEvacuationOverflow = false;

    /**
     * Young generation heap percentage computed by the last resizing request.
     */
    private int youngGenHeapPercentage;
    /**
     * Heap size computed by the last resizing request.
     */
    private Size heapSize;

    private Size alignUp(Size size) {
        Size alignment = unitSize.minus(1);
        return size.plus(alignment).and(alignment.not());
    }

    private Size alignDown(Size size) {
        Size alignment = unitSize.minus(1);
        return size.and(alignment.not());
    }


    /**
     * Compute the effective heap size (one semi-space old generation plus the young generation) from total space available and the
     * percentage of effective heap size the young generation should occupy.
     * Let OS = old gen semi-space size, YS = young gen size, H = the effective heap size, and YP the percentage of effective heap size
     * the young generation should occupy (i.e., <code>youngGenHeapPercentage</code>), and M the total heap space provided (i.e., <code>heapSpace</code>).
     * We have:
     * (1)  H = OS + YS
     * (2) YS = YP  H
     * (3)   M = 2 OS + YS
     * From these, we can derive H, the effective heap size H =  M / ( 2 - YP)
     * Note that the larger YP, the larger the effective the heap size.
     * However, in practice, YP must not be such that the young generation is larger
     * than a old generation semi-space, or the worst case evacuation for a minor collection cannot be supported.
     *
     * @param heapSpace  total space available for a heap
     * @param youngGenHeapPercentage the percentage of the heap the young generation occupies
     * @return the effective heap size
     */
    private Size computeEffectiveHeapSize(Size heapSpace, int youngGenHeapPercentage) {
        return alignUp(heapSpace.times(100).dividedBy(200 - youngGenHeapPercentage));
    }

    private static Size percent(Size size, int percentage) {
        return size.times(percentage).dividedBy(100);
    }

    public Size minYoungGenSize() {
        Size size =  percent(maxHeapSize, MinYoungGenPercent);
        return alignUp(size.lessThan(MinYoungGenSize) ? MinYoungGenSize : size);
    }

    public GenSSHeapSizingPolicy() {
    }

    /**
     * Construct an instance of the sizing policy, parameterized with an alignment constraint for generation size, and an upper bound of young generation size,
     * wherein the upper bound is specified as a percentage of the effective heap size.
     *
     * @param initSize initial amount of memory available to the heap
     * @param maxSize maximum amount of memory available to the heap
     * @param log2Alignment alignment constraints that each space of the heap should satisfy.
     * @param youngGenMaxHeapPercentage maximum percentage of effective heap size the young generation should occupy
     */
    public void initialize(Size initSize, Size maxSize, int youngGenMaxHeapPercentage, int log2Alignment) {
        // Run validation of heap sizing parameters.
        FatalError.check(youngGenMaxHeapPercentage > 0 && youngGenMaxHeapPercentage < 100, "Not a valid percentage of heap size");
        FatalError.check(log2Alignment > 0 && log2Alignment < Word.widthValue().numberOfBits, "Not a valid log2 alignment");
        this.youngGenMaxHeapPercentage = youngGenMaxHeapPercentage;
        this.log2Alignment = log2Alignment;
        this.unitSize = Size.fromInt(1).shiftedLeft(log2Alignment);

        Size initHS = computeEffectiveHeapSize(initSize, youngGenMaxHeapPercentage);
        initHeapSize = initHS.lessThan(minEffectiveHeapSize()) ? minEffectiveHeapSize() : initHS;
        maxHeapSize = computeEffectiveHeapSize(maxSize, youngGenMaxHeapPercentage);
        minYoungGenDelta = alignUp(percent(maxHeapSize, 1));
        if (maxHeapSize.lessThan(minEffectiveHeapSize())) {
            Log.printToPowerOfTwoUnits(maxSize);
            Log.print(" not enough space to initialize heap (max heap size =");
            Log.printToPowerOfTwoUnits(maxHeapSize);
            Log.print(")");
            MaxineVM.exit(-1);
        }

        heapSize = initHeapSize;
        youngGenHeapPercentage = youngGenMaxHeapPercentage;
        maxHeapOldGenSize = maxHeapSize.minus(minYoungGenSize());
        if (logger.enabled()) {
            logger.logInitializeHeap(heapSize.toLong(), initialYoungGenSize().toLong(), initialOldGenSize().toLong(), maxHeapSize.toLong(), percent(maxHeapSize, youngGenMaxHeapPercentage).toLong(), maxHeapOldGenSize.toLong());
        }
    }

    @Override
    public Size initialYoungGenSize() {
        Size size = percent(initHeapSize, youngGenMaxHeapPercentage);
        return alignUp(size.lessThan(MinYoungGenSize) ? MinYoungGenSize : size);
    }

    @Override
    public Size initialOldGenSize() {
        return initHeapSize.minus(initialYoungGenSize());
    }

    @Override
    public Size maxYoungGenSize() {
        return alignUp(percent(maxHeapSize, youngGenMaxHeapPercentage));
    }

    @Override
    public Size maxOldGenSize() {
        return maxHeapOldGenSize;
    }

    public Size heapSize() {
        return heapSize;
    }

    public Size youngGenSize() {
        Size size = percent(heapSize, youngGenHeapPercentage);
        return alignUp(size.lessThan(MinYoungGenSize) ? MinYoungGenSize : size);
    }

    public Size  oldGenSize() {
        return heapSize.minus(youngGenSize());
    }

    public boolean shouldPerformFullGC(Size estimatedEvacuation, Size oldGenFreeSpace, boolean oldSpaceMutatorOverflow) {
        final boolean needsFullGC = minorEvacuationOverflow || oldSpaceMutatorOverflow || estimatedEvacuation.greaterThan(oldGenFreeSpace);
        if (logger.enabled()) {
            logger.logShouldPerformFullGC(estimatedEvacuation.toLong(), oldGenFreeSpace.toLong(), minorEvacuationOverflow, oldSpaceMutatorOverflow, needsFullGC);
        }
        return needsFullGC;
    }

    public void notifyMinorEvacuationOverflow() {
        minorEvacuationOverflow = true;
    }

    public void notifyMinorEvacuationOverflowRange(Address start, Address end) {
        if (logger.enabled()) {
            logger.logMinorOverflowEvacuation(start, end);
        }
    }

    public void notifyOutOfMemory() {
        outOfMemory = true;
    }

    public boolean minorEvacuationOverflow() {
        return minorEvacuationOverflow;
    }

    private boolean sizeDownYoungGen(Size estimatedEvacuation, Size oldGenFreeSpace) {
        Size ys =  youngGenSize();
        Size minYS = minYoungGenSize();
        if (ys.greaterThan(minYS)) {
            // Reduce nursery size to redistribute space to the old generation.
            // If the estimated evacuation is larger than half the size of the nursery, we only redistribute half of the young gen to avoid
            // sharp drop to the young gen size due to a spike in evacuation.
            Size oldSpaceNeeded = alignUp(estimatedEvacuation.minus(oldGenFreeSpace));
            Size maxYoungGenTax = ys.dividedBy(4);
            if (oldSpaceNeeded.greaterThan(maxYoungGenTax)) {
                oldSpaceNeeded = maxYoungGenTax;
            }
            if (oldSpaceNeeded.lessThan(minYoungGenDelta)) {
                oldSpaceNeeded = minYoungGenDelta;
            }
            Size newYoungGenSize = ys.minus(oldSpaceNeeded.times(2));
            if (newYoungGenSize.lessThan(minYS)) {
                youngGenHeapPercentage = MinYoungGenPercent;
                heapSize = minYS.plus(maxHeapOldGenSize);
            } else {
                // We're taking 2*oldSpaceNeeded off the young generation to redistribute it equally to each of the old gen semi-space.
                // Consequently, the effective heap size is reduced by 1*oldSpaceNeeded.
                Size newHeapSize = heapSize.minus(oldSpaceNeeded);
                int newYoungGenHeapPercentage = newYoungGenSize.times(100).dividedBy(newHeapSize).toInt();
                if (!(newYoungGenHeapPercentage >= MinYoungGenPercent && newYoungGenHeapPercentage <= youngGenMaxHeapPercentage)) {
                    Log.print("newYoungGenHeapPercentage = ");
                    Log.println(newYoungGenHeapPercentage);
                    Log.print("newHeapSize = ");
                    Log.printToPowerOfTwoUnits(newHeapSize);
                    Log.print(" (");
                    Log.print(newHeapSize.toLong());
                    Log.println(")");
                    FatalError.unexpected("incorrect downsizing of young gen");
                }
                heapSize = newHeapSize;
                youngGenHeapPercentage = newYoungGenHeapPercentage;
            }
            normalMode = false;
            if (logger.enabled()) {
                logger.logChangeYoungPercent(heapSize.toLong(), youngGenSize().toLong(), oldGenSize().toLong(), youngGenHeapPercentage);
            }
            return true;
        }
        outOfMemory = true;
        return false;
    }

    private void adjustForEstimatedEvacuation(Size estimatedEvacuation, Size usedSpace, Size targetGrowth) {
        Size newHeapSize = alignUp(usedSpace.plus(estimatedEvacuation).times(100).dividedBy(100 - youngGenHeapPercentage));
        FatalError.check(newHeapSize.greaterEqual(heapSize), "new computed heap size must not be smaller than previous heap size");
        if (newHeapSize.minus(heapSize).lessThan(minHeapDeltaBytes)) {
            newHeapSize = heapSize.plus(minHeapDeltaBytes);
        }
        if (newHeapSize.greaterThan(maxHeapSize)) {
            newHeapSize = maxHeapSize;
        }
        Size delta = targetGrowth.plus(newHeapSize.minus(heapSize)); // for logging only.
        heapSize = newHeapSize;
        if (logger.enabled()) {
            logger.logGrowHeap(heapSize.toLong(), youngGenSize().toLong(), oldGenSize().toLong(), delta.toLong());
        }
    }

    public boolean canIncreaseSizeDuringFullGC(Size overflowEvacuationSize, Size oldGenFreeSpace) {
        FatalError.check(minorEvacuationOverflow, "Shouldn't resizing during full GC without minor evacuation overflow");
        return canIncreaseSize(overflowEvacuationSize, oldGenFreeSpace);
    }

    private boolean canIncreaseSize(Size estimatedEvacuation, Size oldGenFreeSpace) {
        final Size usedSpace = oldGenSize().minus(oldGenFreeSpace);
        if (normalMode) {
            Size freeHeapSpace = heapSize.minus(usedSpace);
            if (heapSize.lessThan(maxHeapSize)) {
                FatalError.check(normalMode, "Heap sizing policy must be in normal mode");
                Size minFreeHeapSpace = percent(heapSize, minFreePercent);
                if (freeHeapSpace.greaterEqual(minFreeHeapSpace)) {
                    if (oldGenFreeSpace.greaterEqual(estimatedEvacuation)) {
                        return false;
                    }
                    // We're above the ratio of free space, but that isn't enough to cover the estimated evacuation space.
                    // Recompute the heap size using the estimated evacuation as free old generation space.
                    adjustForEstimatedEvacuation(estimatedEvacuation, usedSpace, Size.zero());
                    return true;
                }
                minFreeHeapSpace = alignUp(usedSpace.times(minFreePercent).dividedBy(100 - minFreePercent));
                final Size targetGrowth = minFreeHeapSpace.minus(freeHeapSpace);
                heapSize = usedSpace.plus(minFreeHeapSpace);
                if (oldGenSize().minus(usedSpace).lessThan(estimatedEvacuation)) {
                    adjustForEstimatedEvacuation(estimatedEvacuation, usedSpace, targetGrowth);
                } else if (logger.enabled()) {
                    logger.logGrowHeap(heapSize.toLong(), youngGenSize().toLong(), oldGenSize().toLong(), targetGrowth.toLong());
                }
                return true;
            }  else if (oldGenFreeSpace.lessThan(estimatedEvacuation)) {
                return sizeDownYoungGen(estimatedEvacuation, oldGenFreeSpace);
            }
            return false;
        }
        if (oldGenFreeSpace.lessThan(estimatedEvacuation)) {
            return sizeDownYoungGen(estimatedEvacuation, oldGenFreeSpace);
        }
        return false;
    }

    /**
     * Recompute heap and generation size based on information provided.
     * The new heap and generation sizes can be consulted using the methods {@link #heapSize()} {@link #youngGenSize()} {@link #oldGenSize()}.
     *
     * @param estimatedEvacuation
     * @param oldGenFreeSpace
     * @return true if the policy requires changes of generation and heap sizes.
     */
    public boolean resizeAfterFullGC(Size estimatedEvacuation, Size oldGenFreeSpace, boolean oldGenMutatorOverflow) {
        minorEvacuationOverflow = false;
        final Size usedSpace = oldGenSize().minus(oldGenFreeSpace);
        Size freeHeapSpace = heapSize.minus(usedSpace);
        Size maxFreeHeapSpace = percent(heapSize, maxFreePercent);
        // Should we shrink ?
        // For simplicity, we don't if the full GC was trigger because of a mutator overflow, otherwise we risk shrinking below what the mutator was requesting.
        // Trying to be smarter requires providing here the actual size requested by the mutator.
        if (!oldGenMutatorOverflow && freeHeapSpace.greaterThan(maxFreeHeapSpace) && maxFreeHeapSpace.greaterEqual(estimatedEvacuation)) {
            if (normalMode) {
                Size newHeapSize = alignUp(usedSpace.plus(maxFreeHeapSpace));
                Size delta = newHeapSize.minus(heapSize);
                heapSize = newHeapSize;
                if (logger.enabled()) {
                    logger.logShrinkHeap(heapSize.toLong(), youngGenSize().toLong(), oldGenSize().toLong(), delta.toLong());
                }
                return true;
            } else {
                // TODO
                // for now, do nothing.
            }
            return false;
        }
        // Should we grow ?
        return canIncreaseSize(estimatedEvacuation, oldGenFreeSpace);
    }

    public boolean outOfMemory() {
        return outOfMemory;
    }

    /*
     * Interface for logging heap resizing decisions made by the GenSSHeapSizingPolicy.
     * The interface uses long instead of Size to improve human-readability from the inspector's log views.
     */
    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface HeapSizingPolicyLoggerInterface {
        void shouldPerformFullGC(
                        @VMLogParam(name = "estimatedEvacuation") long estimatedEvacuation,
                        @VMLogParam(name = "freeOldSpace") long freeOldSpace,
                        @VMLogParam(name = "minorEvacuationOverflow") boolean minorEvacuationOverflow,
                        @VMLogParam(name = "oldSpaceMutatorOverflow") boolean oldSpaceMutatorOverflow,
                        @VMLogParam(name = "shouldPerformGC") boolean shouldPerformGC
        );
        void minorOverflowEvacuation(
                        @VMLogParam(name = "start") Address start,
                        @VMLogParam(name = "end") Address end
        );
        void changeYoungPercent(
                        @VMLogParam(name = "heapSize") long heapSize,
                        @VMLogParam(name = "youngSize") long youngSize,
                        @VMLogParam(name = "oldSize") long oldSize,
                        @VMLogParam(name = "youngGenHeapPercentage") int youngGenHeapPercentage
        );
        void growHeap(
                        @VMLogParam(name = "heapSize") long heapSize,
                        @VMLogParam(name = "youngSize") long youngSize,
                        @VMLogParam(name = "oldSize") long oldSize,
                        @VMLogParam(name = "delta") long delta
        );
        void shrinkHeap(
                        @VMLogParam(name = "heapSize") long heapSize,
                        @VMLogParam(name = "youngSize") long youngSize,
                        @VMLogParam(name = "oldSize") long oldSize,
                        @VMLogParam(name = "delta") long delta
        );
        void initializeHeap(
                        @VMLogParam(name = "heapSize") long heapSize,
                        @VMLogParam(name = "youngSize") long youngSize,
                        @VMLogParam(name = "oldSize") long oldSize,
                        @VMLogParam(name = "maxHeapSize") long maxHeapSize,
                        @VMLogParam(name = "maxYoungSize") long maxYoungSize,
                        @VMLogParam(name = "maxOldSize") long maxOldSize
        );
    }

    static final class HeapSizingPolicyLogger extends HeapSizingPolicyLoggerAuto {
        HeapSizingPolicyLogger() {
            super("HeapSizingPolicy", "Heap Resizing after full GC");
        }

        private void traceHeapSize(long heapSize, long youngSize, long oldSize) {
            Log.print("Heap:    size = ");
            Log.printToPowerOfTwoUnits(Size.fromLong(heapSize));
            Log.print(" [ young = ");
            Log.printToPowerOfTwoUnits(Size.fromLong(youngSize));
            Log.print(" + old = ");
            Log.printToPowerOfTwoUnits(Size.fromLong(oldSize));
            Log.println("]");
        }

        @Override
        protected void traceChangeYoungPercent(long heapSize, long youngSize, long oldSize, int youngGenHeapPercentage) {
            Log.print("Change young gen heap % = ");
            Log.println(youngGenHeapPercentage);
            traceHeapSize(heapSize, youngSize, oldSize);
        }

        @Override
        protected void traceGrowHeap(long heapSize, long youngSize, long oldSize, long delta) {
            Log.print("Grow heap size +=");
            Log.printlnToPowerOfTwoUnits(Size.fromLong(delta));
            traceHeapSize(heapSize, youngSize, oldSize);
        }

        @Override
        protected void traceShrinkHeap(long heapSize, long youngSize, long oldSize, long delta) {
            Log.print("Shrink heap size -=");
            Log.printlnToPowerOfTwoUnits(Size.fromLong(delta));
            traceHeapSize(heapSize, youngSize, oldSize);
        }

        @Override
        protected void traceShouldPerformFullGC(long estimatedEvacuation, long freeOldSpace, boolean minorEvacuationOverflow, boolean oldSpaceMutatorOverflow, boolean shouldPerformGC) {
            Log.print("Estimated next evacuation: ");
            Log.printToPowerOfTwoUnits(Size.fromLong(estimatedEvacuation));
            Log.print(", Free old space: ");
            Log.printToPowerOfTwoUnits(Size.fromLong(freeOldSpace));
            Log.print(", minorEvacuationOverflow = ");
            Log.print(minorEvacuationOverflow);
            Log.print(", oldSpaceMutatorOverflow = ");
            Log.print(oldSpaceMutatorOverflow);
            Log.print(", shouldPerformGC = ");
            Log.println(shouldPerformGC);
        }

        @Override
        protected void traceInitializeHeap(long heapSize, long youngSize, long oldSize, long maxHeapSize, long maxYoungSize, long maxOldSize) {
            Log.print("Initial ");
            traceHeapSize(heapSize, youngSize, oldSize);
            Log.print(", Max heap size = ");
            Log.printToPowerOfTwoUnits(Size.fromLong(maxHeapSize));
            Log.print(", Max  young size = ");
            Log.printToPowerOfTwoUnits(Size.fromLong(maxYoungSize));
            Log.print(", Max old size = ");
            Log.printlnToPowerOfTwoUnits(Size.fromLong(maxOldSize));
        }

        @Override
        protected void traceMinorOverflowEvacuation(Address start, Address end) {
            Log.print("Minor evacuation overflow [");
            Log.print(start);
            Log.print(", ");
            Log.print(end);
            Log.println(" ]");
        }
    }

// START GENERATED CODE
    private static abstract class HeapSizingPolicyLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            ChangeYoungPercent, GrowHeap, InitializeHeap,
            MinorOverflowEvacuation, ShouldPerformFullGC, ShrinkHeap;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected HeapSizingPolicyLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected HeapSizingPolicyLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logChangeYoungPercent(long heapSize, long youngSize, long oldSize, int youngGenHeapPercentage) {
            log(Operation.ChangeYoungPercent.ordinal(), longArg(heapSize), longArg(youngSize), longArg(oldSize), intArg(youngGenHeapPercentage));
        }
        protected abstract void traceChangeYoungPercent(long heapSize, long youngSize, long oldSize, int youngGenHeapPercentage);

        @INLINE
        public final void logGrowHeap(long heapSize, long youngSize, long oldSize, long delta) {
            log(Operation.GrowHeap.ordinal(), longArg(heapSize), longArg(youngSize), longArg(oldSize), longArg(delta));
        }
        protected abstract void traceGrowHeap(long heapSize, long youngSize, long oldSize, long delta);

        @INLINE
        public final void logInitializeHeap(long heapSize, long youngSize, long oldSize, long maxHeapSize, long maxYoungSize,
                long maxOldSize) {
            log(Operation.InitializeHeap.ordinal(), longArg(heapSize), longArg(youngSize), longArg(oldSize), longArg(maxHeapSize), longArg(maxYoungSize),
                longArg(maxOldSize));
        }
        protected abstract void traceInitializeHeap(long heapSize, long youngSize, long oldSize, long maxHeapSize, long maxYoungSize,
                long maxOldSize);

        @INLINE
        public final void logMinorOverflowEvacuation(Address start, Address end) {
            log(Operation.MinorOverflowEvacuation.ordinal(), start, end);
        }
        protected abstract void traceMinorOverflowEvacuation(Address start, Address end);

        @INLINE
        public final void logShouldPerformFullGC(long estimatedEvacuation, long freeOldSpace, boolean minorEvacuationOverflow, boolean oldSpaceMutatorOverflow, boolean shouldPerformGC) {
            log(Operation.ShouldPerformFullGC.ordinal(), longArg(estimatedEvacuation), longArg(freeOldSpace), booleanArg(minorEvacuationOverflow), booleanArg(oldSpaceMutatorOverflow), booleanArg(shouldPerformGC));
        }
        protected abstract void traceShouldPerformFullGC(long estimatedEvacuation, long freeOldSpace, boolean minorEvacuationOverflow, boolean oldSpaceMutatorOverflow, boolean shouldPerformGC);

        @INLINE
        public final void logShrinkHeap(long heapSize, long youngSize, long oldSize, long delta) {
            log(Operation.ShrinkHeap.ordinal(), longArg(heapSize), longArg(youngSize), longArg(oldSize), longArg(delta));
        }
        protected abstract void traceShrinkHeap(long heapSize, long youngSize, long oldSize, long delta);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //ChangeYoungPercent
                    traceChangeYoungPercent(toLong(r, 1), toLong(r, 2), toLong(r, 3), toInt(r, 4));
                    break;
                }
                case 1: { //GrowHeap
                    traceGrowHeap(toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4));
                    break;
                }
                case 2: { //InitializeHeap
                    traceInitializeHeap(toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4), toLong(r, 5), toLong(r, 6));
                    break;
                }
                case 3: { //MinorOverflowEvacuation
                    traceMinorOverflowEvacuation(toAddress(r, 1), toAddress(r, 2));
                    break;
                }
                case 4: { //ShouldPerformFullGC
                    traceShouldPerformFullGC(toLong(r, 1), toLong(r, 2), toBoolean(r, 3), toBoolean(r, 4), toBoolean(r, 5));
                    break;
                }
                case 5: { //ShrinkHeap
                    traceShrinkHeap(toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
