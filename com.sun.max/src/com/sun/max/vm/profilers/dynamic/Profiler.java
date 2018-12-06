/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */

package com.sun.max.vm.profilers.dynamic;

import com.sun.max.annotate.C_FUNCTION;
import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.program.ProgramError;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.runtime.SafepointPoll;
import com.sun.max.vm.thread.VmThread;

import static com.sun.max.vm.MaxineVM.isHosted;

public class Profiler {

    @C_FUNCTION
    static native void dynamicProfiler_lock();

    @C_FUNCTION
    static native void dynamicProfiler_unlock();

    /**
     * The profiling outcome is stored in histograms.
     * Size Histogram: stores the allocated objects sizes for the profiling method chain.
     * Type Histogram: stores the allocated objects sizes in relation with their object type for the profiling method chain.
     *
     */
    public static int initialSize = 6;
    public static int currentSize = initialSize;
    public static int growStep = 10;
    public static HistogramCell[] sizeHistogram;
    public static TypeHistogramCell[] typeHistogram;
    public static int profilingCycle;

    private static boolean PrintHistogram;
    private static boolean ProfileAll;
    public static int profilerTLAcounter = 0;

    /**
     * Use -XX:+PrintHistogram flag to accompany the profiler stats with a complete histogram view.
     * Use -XX:+ProfileAll to profile all application objects unconditionally.
     */
    static {
        VMOptions.addFieldOption("-XX:", "PrintHistogram", Profiler.class, "Print Dynamic Profiler's Histogram after every GC. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "ProfileAll", Profiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public Profiler() {
        sizeHistogram = new HistogramCell[initialSize];
        typeHistogram = new TypeHistogramCell[initialSize];

        //initial cell allocation for our histogram
        for (int i = 0; i < initialSize; i++) {
            sizeHistogram[i] = new HistogramCell();
            typeHistogram[i] = new TypeHistogramCell();
        }
        profilingCycle = 0;

    }

    public boolean profileAll(){
        return ProfileAll;
    }

    /**
     * Grow the histogram array after GC.
     *
     */
    public void growHistograms(Heap.GCCallbackPhase when) {
        final boolean lockDisabledSafepoints = lock();

        final int newSize =  currentSize + growStep;

        HistogramCell[] newHistogram = new HistogramCell[newSize];
        TypeHistogramCell[] newTypeHistogram = new TypeHistogramCell[newSize];

        //copy the contents of the old to the new histogram
        for (int i = 0; i < currentSize - 1; i++) {
            newHistogram[i] = sizeHistogram[i];
            newTypeHistogram[i] = typeHistogram[i];
        }

        //allocate new cells in the histogram
        for (int i = currentSize - 1; i < newSize; i++) {
            newHistogram[i] = new HistogramCell();
            newTypeHistogram[i] = new TypeHistogramCell();
        }

        currentSize = newSize;
        sizeHistogram = newHistogram;
        typeHistogram = newTypeHistogram;

        unlock(lockDisabledSafepoints);
    }

    /**
     * This method is called when a profiled object is allocated.
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void profile(int size, String type) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();
            sizeHistogram[profilingCycle].record(size);
            typeHistogram[profilingCycle].record(size, type);
        unlock(lockDisabledSafepoints);
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void profileGC(int size, String type) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();
            sizeHistogram[profilingCycle].recordGC(size);
            typeHistogram[profilingCycle].recordGC(size, type);
        unlock(lockDisabledSafepoints);
    }

    /**
     * Sort and print Histogram.
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void printHistogram() {
        sizeHistogram[profilingCycle].sortHistogram();

        int lastEntry = sizeHistogram[profilingCycle].lastEntry;

        Log.println("====HISTOGRAM====");
        for (int i = 1; i < lastEntry; i++) {
            Log.print("[");
            Log.print(sizeHistogram[profilingCycle].mutatorHistogram[i][0]);
            Log.print("]\t\t");
            Log.println(sizeHistogram[profilingCycle].mutatorHistogram[i][1]);
        }
        Log.print("Total histogram objects =");
        Log.println(sizeHistogram[profilingCycle].totalRecordedObjects);
        Log.println("=======END=======");

    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void printTypeHistogram() {
        typeHistogram[profilingCycle].sortHistogram();

        int lastEntry = typeHistogram[profilingCycle].lastEntry;

        Log.println("====TYPE HISTOGRAM====");
        for (int i = 1; i < lastEntry; i++) {
            Log.print("[");
            Log.print(typeHistogram[profilingCycle].mutatorTypes[i]);
            Log.print("] ");
            Log.print(" [");
            Log.print(typeHistogram[profilingCycle].mutatorHistogram[i][0]);
            Log.print(" Bytes]  : ");
            Log.println(typeHistogram[profilingCycle].mutatorHistogram[i][1]);
        }
        Log.print("Total histogram objects =");
        Log.println(sizeHistogram[profilingCycle].totalRecordedObjects);
        Log.println("=======END=======");

    }

    /**
     * Dump Profiler findings/stats to Maxine's Log output (for validation purposes).
     * TODO: create a -XX option for that functionality
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void printStats() {
        final boolean lockDisabledSafepoints = lock();
        final float reportInMbs = (float) Heap.reportUsedSpace() / 1048576;
        final float histogramInMbs = (float) sizeHistogram[profilingCycle].totalObjectsize / 1048576;

        Log.print("Reported heap used space = ");
        Log.print(reportInMbs);
        Log.println(" MB");

        Log.print("Histogram total object size = ");
        Log.print(histogramInMbs);
        Log.println(" MB");

        //padding = reportInMbs - histogramInMbs;
        //Log.print("TLAB Padding = ");
        //Log.print(padding);
        //Log.println(" MB\n");

        if (PrintHistogram) {
            //printHistogram();
            printTypeHistogram();
        }

        unlock(lockDisabledSafepoints);
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void scanAndProfile(Heap.GCCallbackPhase when) {
        final HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();
        assert heapScheme instanceof SemiSpaceHeapScheme;
        SemiSpaceHeapScheme semiSpaceHeapScheme = (SemiSpaceHeapScheme) heapScheme;
        semiSpaceHeapScheme.profilerScan();
    }

    /**
     *  This method is called every time a GC has been completed.
     *  At this point the profiler has completed a full profiling cycle.
     *  We check if the Histogram needs more space for a potential next profiling cycle and we increase the cycle counter.
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void postGCActions() {
        final boolean lockDisabledSafepoints = lock();

        // for validation purposes
        Log.print("Collected heap used space = ");
        final float reportCollectedHeapInMbs = (float) Heap.reportUsedSpace() / 1048576;
        Log.print(reportCollectedHeapInMbs);
        Log.println(" MBs");

        //scan the recently collected heap and profile the survived objects
        scanAndProfile(Heap.GCCallbackPhase.AFTER);

        Log.print("HistogramGC heap used space = ");
        final float reportCollectedHeapHistogramInMbs = (float) sizeHistogram[profilingCycle].totalObjectsizeGC / 1048576;
        Log.print(reportCollectedHeapHistogramInMbs);
        Log.println(" MBs\n");

        //if we need more space, grow histograms
        if ((profilingCycle + 1) == currentSize) {
            growHistograms(Heap.GCCallbackPhase.AFTER);
        }

        profilingCycle++;

        unlock(lockDisabledSafepoints);
    }

    private static VmThread lockOwner;
    private static int lockDepth;

    /**
     * Gets the thread that current holds the log lock.
     */
    public static VmThread lockOwner() {
        return lockOwner;
    }

    /**
     * lock() and unlock() methods have been implemented according to the Log.lock() and Log.unlock() ones.
     *
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public static boolean lock() {
        if (isHosted()) {
            return true;
        }

        boolean wasDisabled = SafepointPoll.disable();
        Profiler.dynamicProfiler_lock();
        if (lockDepth == 0) {
            FatalError.check(lockOwner == null, "dynamic profiler lock should have no owner with depth 0");
            lockOwner = VmThread.current();
        }
        lockDepth++;
        return !wasDisabled;
    }

    /**
     * lock() and unlock() methods have been implemented according to the Log.lock() and Log.unlock() ones.
     *
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public static void unlock(boolean lockDisabledSafepoints) {
        if (isHosted()) {
            return;
        }

        --lockDepth;
        FatalError.check(lockDepth >= 0, "mismatched lock/unlock");
        FatalError.check(lockOwner == VmThread.current(), "dynamic profiler lock should be owned by current thread");
        if (lockDepth == 0) {
            lockOwner = null;
        }
        Profiler.dynamicProfiler_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by Profiler.lock() and Profiler.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }
}
