/*
 * Copyright (c) 2018, 2019, APT Group, School of Computer Science,
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

package com.sun.max.vm.profilers.allocation;

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

import uk.ac.manchester.jnumautils.JNumaUtils;

import static com.sun.max.vm.MaxineVM.isHosted;

public class Profiler {

    @C_FUNCTION
    static native void allocationProfiler_lock();

    @C_FUNCTION
    static native void allocationProfiler_unlock();

    /**
     * The profiling outcome is stored in histograms.
     * Size Histogram: stores the allocated objects sizes for the profiling method chain.
     * Type Histogram: stores the allocated objects sizes in relation with their object type for the profiling method chain.
     *
     */
    public static int initialSize = 6;
    public static int currentSize = initialSize;
    public static int growStep = 10;
    public static int profilingCycle;

    public static int currentIndex = 0;

    public static ProfilerBuffer objects;
    public static HeapConfiguration heapConfig;

    public JNumaUtils utilsObject;

    private static boolean AllocationProfilerPrintHistogram;
    private static boolean AllocationProfilerAll;
    private static boolean AllocationProfilerDump;

    /**
     * Use -XX:+AllocationProfilerPrintHistogram flag to accompany the profiler stats with a complete histogram view.
     * Use -XX:+AllocationProfilerAll to profile all application objects unconditionally.
     */
    static {
        VMOptions.addFieldOption("-XX:", "AllocationProfilerPrintHistogram", Profiler.class, "Print Dynamic Profiler's Histogram after every GC. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerAll", Profiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerDump", Profiler.class, "Dump profiled objects to a file. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public Profiler() {
        objects = new ProfilerBuffer();

        // Initialize a JNumaUtils object. We need to allocate it early because it is going to be used inside safepoints.
        utilsObject = new JNumaUtils();

        profilingCycle = 0;
    }

    public int getProfilingCycle() {
        return profilingCycle;
    }

    public static boolean profileAll() {
        return AllocationProfilerAll;
    }

    /**
     * Grow the histogram array after GC.
     *
     */
    public void growHistograms(Heap.GCCallbackPhase when) {
        final boolean lockDisabledSafepoints = lock();
/*
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
            newTypeHistogram[i] = new TypeHistogramCell(heapConfig.virtSpaces, 1);
        }

        currentSize = newSize;
        sizeHistogram = newHistogram;
        typeHistogram = newTypeHistogram;
*/
        unlock(lockDisabledSafepoints);
    }

    /**
     * This method is called when a profiled object is allocated.
     */
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void profile(int size, String type, long address) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();
        //int numaNode = JNumaUtils.findNode(address);
        objects.record(currentIndex, type, size, address, 0);
        currentIndex++;
        unlock(lockDisabledSafepoints);
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void profileGC(int size, String type) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();

        unlock(lockDisabledSafepoints);
    }

    public void printCycle() {
        final boolean lockDisabledSafepoints = lock();
        objects.print(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    /**
     * Dump Profiler findings/stats to Maxine's Log output (for validation purposes).
     * TODO: create a -XX option for that functionality
     */
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void printStats() {
        final boolean lockDisabledSafepoints = lock();
        /*
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

        if (AllocationProfilerPrintHistogram) {
            //printHistogram();
            printTypeHistogram();
        }
        */
        unlock(lockDisabledSafepoints);
    }

    /**
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     * We create the numa virtual memory map using the createNumaMap native function.
     */
    public void preGCActions() {
        utilsObject.test();
    }

    /**
     *  This method is called every time a GC has been completed.
     *  At this point the profiler has completed a full profiling cycle.
     *  We check if the Histogram needs more space for a potential next profiling cycle and we increase the cycle counter.
     */
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void postGCActions() {
        final boolean lockDisabledSafepoints = lock();
        /*
        // for validation purposes
        Log.print("Collected heap used space = ");
        final float reportCollectedHeapInMbs = (float) Heap.reportUsedSpace() / 1048576;
        Log.print(reportCollectedHeapInMbs);
        Log.println(" MBs");

        //scan the recently collected heap and profile the survived objects
        VMConfiguration.vmConfig().heapScheme().scanAndProfile();

        Log.print("HistogramGC heap used space = ");
        final float reportCollectedHeapHistogramInMbs = (float) sizeHistogram[profilingCycle].totalObjectsizeGC / 1048576;
        Log.print(reportCollectedHeapHistogramInMbs);
        Log.println(" MBs\n");
        */

        printCycle();
        objects.resetCycle();
        //ProfiledObjects tmp = new ProfiledObjects();
        //objects = tmp;

        //if we need more space, grow histograms
        if ((profilingCycle + 1) == currentSize) {
            growHistograms(Heap.GCCallbackPhase.AFTER);
        }

        profilingCycle++;

        unlock(lockDisabledSafepoints);
    }

    public void terminate() {

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
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public static boolean lock() {
        if (isHosted()) {
            return true;
        }

        boolean wasDisabled = SafepointPoll.disable();
        Profiler.allocationProfiler_lock();
        if (lockDepth == 0) {
            FatalError.check(lockOwner == null, "allocation profiler lock should have no owner with depth 0");
            lockOwner = VmThread.current();
        }
        lockDepth++;
        return !wasDisabled;
    }

    /**
     * lock() and unlock() methods have been implemented according to the Log.lock() and Log.unlock() ones.
     *
     */
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public static void unlock(boolean lockDisabledSafepoints) {
        if (isHosted()) {
            return;
        }

        --lockDepth;
        FatalError.check(lockDepth >= 0, "mismatched lock/unlock");
        FatalError.check(lockOwner == VmThread.current(), "allocation profiler lock should be owned by current thread");
        if (lockDepth == 0) {
            lockOwner = null;
        }
        Profiler.allocationProfiler_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by Profiler.lock() and Profiler.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }
}
