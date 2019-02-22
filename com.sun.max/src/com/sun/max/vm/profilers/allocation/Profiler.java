/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.heap.*;
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

    public static int profilingCycle;
    public static int currentIndex = 0;
    public static ProfilerBuffer objects;
    public static ProfilerBuffer survivors;
    public static HeapConfiguration heapConfig;
    public JNumaUtils utilsObject;
    public static boolean preMutation = false;
    public static int survivedObjNum = 0;

    private static boolean AllocationProfilerPrintHistogram;
    private static boolean AllocationProfilerAll;
    private static boolean AllocationProfilerDump;
    public static boolean VerboseAllocationProfiler;

    /**
     * The size of the Allocation Profiling Buffer
     * TODO: auto-configurable
     */
    public final int ALLOCBUFFERSIZE = 500000;

    /**
     * Use -XX:+AllocationProfilerPrintHistogram flag to accompany the profiler stats with a complete histogram view.
     * Use -XX:+AllocationProfilerAll to profile all application objects unconditionally.
     * Use -XX:+AllocationProfilerDump
     * Use -XX:+VerboseAllocationProfiler to verbose profiler actions. For understanding or debugging purposes.
     */
    static {
        VMOptions.addFieldOption("-XX:", "AllocationProfilerPrintHistogram", Profiler.class, "Print Dynamic Profiler's Histogram after every GC. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerAll", Profiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerDump", Profiler.class, "Dump profiled objects to a file. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "VerboseAllocationProfiler", Profiler.class, "Verbose profiler output . (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public Profiler() {
        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Profiler Initialization.");
        }
        objects = new ProfilerBuffer(ALLOCBUFFERSIZE);

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): JNumaUtils Initialization.");
        }
        // Initialize a JNumaUtils object. We need to allocate it early because it is going to be used when allocation is disabled.
        utilsObject = new JNumaUtils();

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Resolve createNumaMaps Native Method.");
        }

        resolveNativeMethods();

        profilingCycle = 1;
        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Initialization Complete.");

            Log.print("(verbose msg): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }
    }

    /**
     * This method forces each native method's resolution.
     * It is used during profiler's initialization, when allocation is still enabled.
     */
    public void resolveNativeMethods() {
        //all methods that will be called when allocation is disabled need to have already been resolved.
        utilsObject.findNode(0L);
    }

    public int getProfilingCycle() {
        return profilingCycle;
    }

    public static boolean profileAll() {
        return AllocationProfilerAll;
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
        /*
         * A pre-Mutation "pseudo" callback.
         */
        if (preMutation) {
            preMutationActions();
        }
        objects.record(currentIndex, type, size, address);
        currentIndex++;
        unlock(lockDisabledSafepoints);
    }

    /**
     * Dump Profiler Buffer to Maxine's Log output.
     */
    public void dumpBuffer() {
        final boolean lockDisabledSafepoints = lock();
        Log.print("==== Profiling Cycle ");
        Log.print(profilingCycle);
        Log.println(" ====");
        objects.print(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    public void dumpSurvivors() {
        final boolean lockDisabledSafepoints = lock();
        Log.print("==== Survivors Cycle ");
        Log.print(profilingCycle);
        Log.println(" ====");
        survivors.print(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    public void findNumaNodes() {
        for (int i = 0; i < objects.currentIndex; i++) {
            int node = utilsObject.findNode(objects.address[i]);
            objects.setNodeOf(i, node);
        }
    }

    public void removeCollected() {
        for (int i = 0; i < objects.currentIndex; i++) {
            long address = objects.address[i];
            if (Heap.stillExists(address)) {
                //object is alive -> update it's address
                long newAddr = Heap.getUpdatedAddress(address);
                objects.address[i] = newAddr;
                survivedObjNum++;
            } else {
                //object is dead
                objects.markAsDead(i);
            }
        }
    }

    public ProfilerBuffer createSurvivorBuffer(int survivedObjNum) {
        ProfilerBuffer s = new ProfilerBuffer(survivedObjNum);
        int j = 0;
        for(int i = 0; i < objects.currentIndex; i++) {
            if (objects.index[i] > 0) {
                s.index[j] = objects.index[i];
                s.type[j] = objects.type[i];
                s.size[j] = objects.size[i];
                s.address[j] = objects.address[i];
                s.node[j] = objects.node[i];
                j++;
            }
        }
        assert j == survivedObjNum;
        return s;
    }

    /**
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     * We create the numa virtual memory map using the createNumaMap native function.
     */
    public void preGCActions() {

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Entering Pre-GC Phase.");
            Log.print("(verbose msg): Cycle ");
            Log.print(getProfilingCycle());
            Log.println(" Profiling Is Now Complete. [pre-GC phase]");
        }
        findNumaNodes();

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Dump Profiler Buffer. [pre-GC phase]");
        }
        dumpBuffer();
        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Leaving Pre-GC Phase.");
        }

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

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Entering Post-GC Phase.");
        }

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Leaving Post-GC Phase.");
        }

        // Enable the pre-Mutation flag before leaving post-GC phase
        preMutation = true;

        unlock(lockDisabledSafepoints);
    }

    /**
     * This method is called when the pre-Mutation "pseudo" callback is triggered.
     * Pre-Mutation phase: The moment we leave safepoint after a GC cycle and before execution,
     * where allocation is enabled again.
     */
    public void preMutationActions() {
        final boolean lockDisabledSafepoints = lock();
        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Entering Pre-Mutation Phase.");
        }

        // all pre-Mutation actions take place here
        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Remove Collected Objects From Profiler Buffer. [pre-mutation phase]");
        }
        removeCollected();

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Create Survivor Buffer. [pre-mutation phase]");
        }
        survivors = createSurvivorBuffer(survivedObjNum);
        //TODO: Make removeCollected() and createSurvivorBuffer() an one-step process

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Clean-up Profiler Buffer. [pre-mutation phase]");
        }

        objects.resetCycle();

        profilingCycle++;

        if (VerboseAllocationProfiler) {
            Log.println("(verbose msg): Leaving Pre-Mutation Phase. ");
            Log.print("(verbose msg): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }

        // Disable the pre-Mutation flag after all pre-Mutation actions are complete.
        preMutation = false;
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
