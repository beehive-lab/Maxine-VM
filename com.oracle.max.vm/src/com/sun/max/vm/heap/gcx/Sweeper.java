/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;

/**
 * Interface that space managers must implement to be notified of sweeping events emitted by a sweeping collector.
 * The interface allows both precise and imprecise sweeping. Precise sweeping inspects
 * every marked location to determine the end of the live data and identify every
 * dead space, and invoke the space manager for every. Imprecise sweeping only inspects marked location separated by a minimum
 * distance, thus avoiding inspecting object when the size of a potential free chunk is
 * too small to be of interest to the free space manager.
 */
public abstract class Sweeper {

    static boolean TraceSweep = false;
    static {
        if (MaxineVM.isDebug()) {
            VMOptions.addFieldOption("-XX:", "TraceSweep", Sweeper.class, "Trace heap sweep operations", Phase.PRISTINE);
        }
    }

    static final VMIntOption freeChunkMinSizeOption =
        register(new VMIntOption("-XX:FreeChunkMinSize=", 256,
                        "Minimum size of contiguous space considered for space reclamation." +
                        "Below this size, the space is ignored (dark matter)"),
                        MaxineVM.Phase.PRISTINE);

    protected SweepLogger logger = MaxineVM.isDebug() ? new SweepLogger(true) : new SweepLogger();

    /**
     * Invoked when doing precise sweeping on the first black object following the pointer last returned by this method.
     * @param liveObject a pointer to a live cell in the heap
     * @return a pointer to the position in the heap where to resume sweeping.
     */
    public abstract Pointer processLiveObject(Pointer liveObject);

    /**
     * Invoked when doing imprecise sweeping to process an large interval between to marked locations.
     * Imprecise heap sweeping ignores any space before two live objects smaller than a specified amount of space.
     * When the distance between two live marks is large enough to indicate a potentially large chunk of free space,
     * the sweeper invoke this method.
     *
     * @param leftLiveObject
     * @param rightLiveObject
     * @return
     */
    public abstract Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject);

    /**
     * Invoked to record a known chunk of free space.
     * Used both by precise and imprecise sweeper, typically to record the unmarked space
     * at both end of the traced space.
     * @param freeChunk
     * @param size
     */
    public abstract void processDeadSpace(Address freeChunk, Size size);

    /**
     * Prepare the space manager for a sweep of the space it manages.
     */
    public abstract void beginSweep();

    /**
     * Notify the space manager that sweeping is terminated.
     *
     * @return total free spaces
     */
    public abstract void endSweep();

    /**
     * Total free space after sweeping.
     * May not be accurate with actual free space if allocation took place since endSweep was called.
     * @return
     */
    public abstract Size freeSpaceAfterSweep();

    /**
     * Minimum size to be considered reclaimable.
     */
    public abstract Size minReclaimableSpace();

    /**
     * Linearly walk over the spaces swept by the sweeper to perform post-mark-sweep verification using the verifier given in argument.
     * @param verifier
     */
    public abstract void verify(AfterMarkSweepVerifier verifier);

    /**
     * Start of the region being swept.
     * @return an address
     */
    public abstract Address startOfSweepingRegion();

    /**
     * End of the region being swept.
     * @return an address
     */
    public abstract Address endOfSweepingRegion();

    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface SweepLoggerInterface {
        void gap(
                        @VMLogParam(name = "leftLiveObject") Pointer leftLiveObject,
                        @VMLogParam(name = "rightLiveObject") Pointer rightLiveObject);
        void deadSpace(
                        @VMLogParam(name = "deadSpace") Address deadSpace,
                        @VMLogParam(name = "size") Size size);
        void freeSpace(
                        @VMLogParam(name = "freeSpace") Address freeSpace,
                        @VMLogParam(name = "size") Size size);
    }

    static final class SweepLogger extends SweepLoggerAuto {
        SweepLogger(boolean active) {
            super("Swept", "Trace dead space reclaimed by the sweeper");
        }
        SweepLogger() {
        }
        private void traceSpace(String spaceType, Address space, Size size) {
            Log.print(spaceType);
            Log.print(" Space @");
            Log.print(space);
            Log.print("(");
            Log.print(size.toLong());
            Log.println(")");
        }

        @Override
        protected void traceDeadSpace(Address deadSpace, Size size) {
            traceSpace("Dead", deadSpace, size);
        }

        @Override
        protected void traceGap(Pointer leftLiveObject, Pointer rightLiveObject) {
            Log.print("Gap between [");
            Log.print(leftLiveObject);
            Log.print(", ");
            Log.print(rightLiveObject);
            Log.print("]");
        }
        @Override
        protected void traceFreeSpace(Address freeSpace, Size size) {
            traceSpace("Free", freeSpace, size);
        }
    }

// START GENERATED CODE
    private static abstract class SweepLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            DeadSpace, FreeSpace, Gap;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected SweepLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected SweepLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logDeadSpace(Address deadSpace, Size size) {
            log(Operation.DeadSpace.ordinal(), deadSpace, size);
        }
        protected abstract void traceDeadSpace(Address deadSpace, Size size);

        @INLINE
        public final void logFreeSpace(Address freeSpace, Size size) {
            log(Operation.FreeSpace.ordinal(), freeSpace, size);
        }
        protected abstract void traceFreeSpace(Address freeSpace, Size size);

        @INLINE
        public final void logGap(Pointer leftLiveObject, Pointer rightLiveObject) {
            log(Operation.Gap.ordinal(), leftLiveObject, rightLiveObject);
        }
        protected abstract void traceGap(Pointer leftLiveObject, Pointer rightLiveObject);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //DeadSpace
                    traceDeadSpace(toAddress(r, 1), toSize(r, 2));
                    break;
                }
                case 1: { //FreeSpace
                    traceFreeSpace(toAddress(r, 1), toSize(r, 2));
                    break;
                }
                case 2: { //Gap
                    traceGap(toPointer(r, 1), toPointer(r, 2));
                    break;
                }
            }
        }
    }

// END GENERATED CODE
}
