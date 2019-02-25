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
    public static int uniqueId = 0;
    /**
     * The Profiler Buffer for newly allocated objects.
     */
    public static ProfilerBuffer newObjects;
    /**
     * The Profiler Buffers for survivor objects.
     */
    public static ProfilerBuffer survivors1;
    public static ProfilerBuffer survivors2;
    public JNumaUtils utilsObject;

    private static boolean AllocationProfilerPrintHistogram;
    private static boolean AllocationProfilerAll;
    private static boolean AllocationProfilerDump;
    public static boolean VerboseAllocationProfiler;

    /**
     * The size of the Allocation Profiling Buffer.
     * TODO: auto-configurable
     */
    public final int ALLOCBUFFERSIZE = 500000;

    /**
     * The size of each Survivors Profiling Buffer.
     */
    public final int SURVBUFFERSIZE = 500000;

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
            Log.println("(Allocation Profiler): Profiler Initialization.");
        }
        newObjects = new ProfilerBuffer(ALLOCBUFFERSIZE, "New Objects Buffer");

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): JNumaUtils Initialization.");
        }
        // Initialize a JNumaUtils object. We need to allocate it early because it is going to be used when allocation is disabled.
        utilsObject = new JNumaUtils();

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Force Native Methods's resolution.");
        }
        resolveNativeMethods();

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Initialize the Survivor Objects Profiler Buffers.");
        }
        survivors1 = new ProfilerBuffer(SURVBUFFERSIZE, "Survivors Buffer No1");
        survivors2 = new ProfilerBuffer(SURVBUFFERSIZE, "Survivors Buffer No2");

        profilingCycle = 1;
        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Initialization Complete.");

            Log.print("(Allocation Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }
    }

    /**
     * This method forces each native method's resolution.
     * All methods that will be called when allocation is disabled need to have already been resolved.
     */
    public void resolveNativeMethods() {
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
        newObjects.record(uniqueId, type, size, address);
        uniqueId++;
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
        newObjects.print(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    public void dumpSurvivors() {
        final boolean lockDisabledSafepoints = lock();
        Log.print("==== Survivors Cycle ");
        Log.print(profilingCycle);
        Log.println(" ====");
        if ((profilingCycle % 2) == 0) {
            survivors2.print(profilingCycle);
        } else {
            survivors1.print(profilingCycle);
        }
        unlock(lockDisabledSafepoints);
    }

    public void findNumaNodes() {
        for (int i = 0; i < newObjects.currentIndex; i++) {
            int node = utilsObject.findNode(newObjects.address[i]);
            newObjects.setNode(i, node);
        }
    }

    public void copySurvivors(ProfilerBuffer from, ProfilerBuffer to) {
        if (VerboseAllocationProfiler) {
            Log.print("(Allocation Profiler): Copy Survived Objects from ");
            Log.print(from.buffersName);
            Log.print(" to ");
            Log.println(to.buffersName);
        }
        for (int i = 0; i < from.currentIndex; i++) {
            long address = from.address[i];
            if (Heap.stillExists(address)) {
                //object is alive -> update it's address -> copy it to to buffer
                long newAddr = Heap.getUpdatedAddress(address);
                to.record(from.index[i], from.type[i], from.size[i], newAddr, from.node[i]);
            }
            //clean up cell
            //maybe useless
            from.cleanBufferCell(i);
        }
    }

    /**
     * In even profiling cycle we store surviving objects to survivor2.
     * In odd profiling cycle we store surviving objects to survivor1.
     * TODO: write proper comments
     */
    public void findSurvivors() {
        if ((profilingCycle % 2) == 0) {
            //even
            copySurvivors(survivors1, survivors2);
            copySurvivors(newObjects, survivors2);
        } else {
            //odd
            copySurvivors(survivors2, survivors1);
            copySurvivors(newObjects, survivors1);
        }
    }

    /**
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     */
    public void preGCActions() {

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Entering Pre-GC Phase.");
            Log.print("(Allocation Profiler): Cycle ");
            Log.print(getProfilingCycle());
            Log.println(" Profiling Is Now Complete. [pre-GC phase]");
        }
        findNumaNodes();

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Dump Profiler Buffer. [pre-GC phase]");
        }
        dumpBuffer();
        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Leaving Pre-GC Phase.");
        }

    }

    /**
     *  This method is called every time a GC has been completed.
     *  We search for survivor objects among recently allocated and older survived objects.
     */
    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void postGCActions() {
        final boolean lockDisabledSafepoints = lock();

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Entering Post-GC Phase.");
        }

        findSurvivors();

        if ((profilingCycle % 2) == 0) {
            if (VerboseAllocationProfiler) {
                Log.println("(Allocation Profiler): Clean-up Profiler Buffer. [post-gc phase]");
                Log.println("(Allocation Profiler): Clean-up Survivor1 Buffer. [post-gc phase]");
            }
            newObjects.resetBuffer();
            survivors1.resetBuffer();
        } else {
            if (VerboseAllocationProfiler) {
                Log.println("(Allocation Profiler): Clean-up Profiler Buffer. [post-gc phase]");
                Log.println("(Allocation Profiler): Clean-up Survivor2 Buffer. [post-gc phase]");
            }
            newObjects.resetBuffer();
            survivors2.resetBuffer();
        }

        dumpSurvivors();

        profilingCycle++;

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Leaving Post-GC Phase.");
            Log.print("(Allocation Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }

        unlock(lockDisabledSafepoints);
    }

    /**
     * This method can be used for actions need to take place right before
     * Allocation Profiler's termination. It is triggered when JavaRunScheme
     * is being terminated. Currently empty.
     */
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
