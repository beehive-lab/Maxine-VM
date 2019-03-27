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
     * The Profiler Buffers for survivor objects. We use two identical buffers because
     * allocation is disabled at the point we need to move the data in a clean buffer.
     */
    public static ProfilerBuffer survivors1;
    public static ProfilerBuffer survivors2;

    /**
     * The external JNumaUtils library object.
     */
    public JNumaUtils utilsObject;

    private static boolean AllocationProfilerPrintHistogram;
    private static boolean AllocationProfilerAll;
    private static boolean AllocationProfilerDump;
    public static boolean VerboseAllocationProfiler;
    public static int BufferSize;
    public static boolean ValidateAllocationProfiler;

    public static int totalNewSize = 0;
    public static int totalSurvSize = 0;

    /**
     * The following two variables are used to help us ignore the application's
     * warmup iterations in order to profile only the effective part. The iteration
     * is calculated by the number of System.gc() calls. The MaxineVM.profileThatObject()
     * method returns false as long as the iteration is below the WarmupThreshold, which
     * is given by the user, ignoring any object allocation by that point.
     *
     * NOTE: This technique is applicable only for dacapo benchmarks, since the iterations
     * are margined with explicit System.gc() calls and might not be working for any benchmark suite.
     */
    public static int WarmupThreshold;
    public static int iteration = 0;

    public final static int MINIMUMBUFFERSIZE = 500000;
    /**
     * The size of the Allocation Profiling Buffer.
     */
    public int allocBufferSize = MINIMUMBUFFERSIZE;

    /**
     * The size of each Survivors Profiling Buffer.
     */
    public int survBufferSize = MINIMUMBUFFERSIZE;

    /**
     * The options a user can pass to the Allocation Profiler.
     */
    static {
        VMOptions.addFieldOption("-XX:", "AllocationProfilerAll", Profiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerDump", Profiler.class, "Dump profiled objects to a file. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "VerboseAllocationProfiler", Profiler.class, "Verbose profiler output. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "BufferSize", Profiler.class, "Allocation Buffer Size.");
        VMOptions.addFieldOption("-XX:", "WarmupThreshold", Profiler.class, "The warmup threshold defines the number of warmup iterations (margined by System.gc()) are due before the allocation profiling begins. (default: 0)");
        VMOptions.addFieldOption("-XX:", "ValidateAllocationProfiler", Profiler.class, "Print information to help in Allocation Profiler's Validation. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public Profiler() {
        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Profiler Initialization.");
        }

        if (BufferSize != 0) {
            if (BufferSize < MINIMUMBUFFERSIZE) {
                Log.print("WARNING: Small Buffer Size. Minimum Buffer Size applied! (=");
                Log.print(MINIMUMBUFFERSIZE);
                Log.println(")");
                allocBufferSize = MINIMUMBUFFERSIZE;
                survBufferSize = MINIMUMBUFFERSIZE;
            } else {
                allocBufferSize = BufferSize;
                survBufferSize = BufferSize;
            }
        }

        newObjects = new ProfilerBuffer(allocBufferSize, "New Objects Buffer");

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
        survivors1 = new ProfilerBuffer(survBufferSize, "Survivors Buffer No1");
        survivors2 = new ProfilerBuffer(survBufferSize, "Survivors Buffer No2");

        profilingCycle = 1;
        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Initialization Complete.");

            Log.print("(Allocation Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }
    }

    /**
     * This method used to force resolution for native methods.
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

    public static boolean warmupFinished() {
        return iteration >= WarmupThreshold;
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
        totalNewSize = totalNewSize + size;
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

    /**
     * We search "from" buffer for survivor objects and store them into "to" buffer.
     * @param from the source buffer in which we search for survivor objects.
     * @param to the destination buffer in which we store the survivor objects.
     */
    public void storeSurvivors(ProfilerBuffer from, ProfilerBuffer to) {
        if (VerboseAllocationProfiler) {
            Log.print("(Allocation Profiler): Copy Survived Objects from ");
            Log.print(from.buffersName);
            Log.print(" to ");
            Log.println(to.buffersName);
        }
        for (int i = 0; i < from.currentIndex; i++) {
            long address = from.address[i];
            if (Heap.isSurvivor(address)) {
                //object is alive -> update it's address -> copy it to to buffer
                long newAddr = Heap.getForwardedAddress(address);
                to.record(from.index[i], from.type[i], from.size[i], newAddr, from.node[i]);
                totalSurvSize = totalSurvSize + from.size[i];
            }
            from.cleanBufferCell(i);
        }
    }

    /**
     * This method is called from postGC actions. Given the profiling cycle,
     * it decides in which buffer should we search for survivor objects.
     * Firstly we search in the so far survivor objects (survivors<num> buffer)
     * and then in the recently allocated objects (newObjects buffer).
     * The found survivor objects are stored in a clean survivor buffer.
     * In even profiling cycles we use the survivor2 buffer.
     * In odd profiling cycles we use the survivor1 buffer.
     */
    public void profileSurvivors() {

        if ((profilingCycle % 2) == 0) {
            //even cycles
            storeSurvivors(survivors1, survivors2);
            storeSurvivors(newObjects, survivors2);
        } else {
            //odd cycles
            storeSurvivors(survivors2, survivors1);
            storeSurvivors(newObjects, survivors1);
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

        if (!ValidateAllocationProfiler) {
            dumpBuffer();
        } else {
            //in validation mode don't dump buffer
            Log.print("Cycle ");
            Log.println(profilingCycle);

            Log.print("=> (Profiler Reports): New Objects Size =");
            Log.print((float) totalNewSize / (1024 * 1024));
            Log.println(" MB");

            Log.print("=> (VM Reports): Heap Used Space =");
            Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
            Log.println(" MB");

            Log.println("Garbage Collection");
        }

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Dump Profiler Buffer. [pre-GC phase]");
        }

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Leaving Pre-GC Phase.");
        }
    }

    /**
     *  This method is called every time a GC has been completed.
     */
    public void postGCActions() {

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Entering Post-GC Phase.");
        }

        profileSurvivors();

        if ((profilingCycle % 2) == 0) {
            if (VerboseAllocationProfiler) {
                Log.println("(Allocation Profiler): Clean-up Survivor1 Buffer. [post-gc phase]");
            }
            survivors1.resetBuffer();
        } else {
            if (VerboseAllocationProfiler) {
                Log.println("(Allocation Profiler): Clean-up Survivor2 Buffer. [post-gc phase]");
            }
            survivors2.resetBuffer();
        }

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Clean-up Profiler Buffer. [post-gc phase]");
        }
        newObjects.resetBuffer();

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Dump Survivors Buffer. [pre-GC phase]");
        }

        if (!ValidateAllocationProfiler) {
            dumpSurvivors();
        } else {
            //in validation mode don't dump buffer
            Log.print("=> (Profiler Reports): Survivor Objects Size =");
            Log.print((float) totalSurvSize / (1024 * 1024));
            Log.println(" MB");

            Log.print("=> (VM Reports): Heap Used Space =");
            Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
            Log.println(" MB\n");
        }

        totalNewSize = totalSurvSize;
        totalSurvSize = 0;

        profilingCycle++;

        if (VerboseAllocationProfiler) {
            Log.println("(Allocation Profiler): Leaving Post-GC Phase.");
            Log.print("(Allocation Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }
    }

    /**
     * This method can be used for actions need to take place right before
     * Allocation Profiler's termination. It is triggered when JavaRunScheme
     * is being terminated. Currently empty.
     */
    public void terminate() {
        if (getProfilingCycle() == 1) {
            dumpBuffer();
            dumpSurvivors();
        }
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
