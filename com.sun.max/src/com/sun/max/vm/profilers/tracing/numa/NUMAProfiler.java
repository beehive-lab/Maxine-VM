/*
 * Copyright (c) 2020, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
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

package com.sun.max.vm.profilers.tracing.numa;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

import java.util.Arrays;

public class NUMAProfiler {

    /**
     * Values that {@link VmThreadLocal#PROFILER_STATE} can take.
     */
    public enum PROFILING_STATE {
        /**
         * Indicates that profiling is disabled on this thread.
         */
        DISABLED(0),
        /**
         * Indicates that profiling is enabled on this thread.
         */
        ENABLED(1),
        /**
         * Indicates that profiling is enabled on this thread, and the thread is currently profiling a memory access.
         * This state is used to avoid nested profiling.
         */
        ONGOING(2);

        public int getValue() {
            return value;
        }

        private final int value;

        PROFILING_STATE(int i) {
            value = i;
        }
    }

    public static int flareObjectCounter = 0;

    public static int start_counter = 0;
    public static int end_counter = 0;

    @C_FUNCTION
    static native void numaProfiler_lock();

    @C_FUNCTION
    static native void numaProfiler_unlock();

    private static int          profilingCycle;
    private static int          uniqueId = 0;
    /**
     * The NUMAProfiler Buffer for newly allocated objects.
     */
    private static RecordBuffer newObjects;

    /**
     * The NUMAProfiler Buffers for survivor objects. We use two identical buffers because
     * allocation is disabled at the point we need to move the data in a clean buffer.
     */
    private static RecordBuffer survivors1;
    private static RecordBuffer survivors2;

    /**
     * The Buffer who keeps track of the physical NUMA node of any virtual memory page allocated for the JVM Heap.
     */
    private static VirtualPagesBuffer heapPages;
    private static VirtualPagesBuffer previousHeapPages;

    @SuppressWarnings("unused")
    private static boolean NUMAProfilerVerbose;
    @SuppressWarnings("unused")
    private static int     NUMAProfilerBufferSize;
    @SuppressWarnings("unused")
    private static boolean NUMAProfilerDebug;
    @SuppressWarnings("unused")
    private static boolean NUMAProfilerIncludeFinalization;
    @SuppressWarnings("unused")
    public static boolean NUMAProfilerIsolateDominantThread;

    private static int totalNewSize  = 0;
    private static int totalSurvSize = 0;

    /**
     * PROFILING POLICY 1: Explicit GC Driven
     * Trigger Event: An Application's System.gc() call.
     * The following two variables are used to help us ignore the application's
     * warmup iterations in order to profile only the effective part. The iteration
     * is calculated by the number of System.gc() calls. The MaxineVM.profileThatObject()
     * method returns false as long as the iteration counter is below the NUMAProfilerExplicitGCThreshold, which
     * is given by the user, ignoring any object allocation up to that point. Its default value
     * has been chosen as -1 because by 0 it means that we want to profile everything.
     */
    @SuppressWarnings("unused")
    public static int NUMAProfilerExplicitGCThreshold = -1;
    public static  int iteration = 0;

    /**
     * This field stores the GC type information (implicit or explicit).
     * By default is false. It is set to true, when an explicit GC is triggered
     * at JDK_java_lang_Runtime class. It is then accessed by the NUMAProfiler at the post-gc phase.
     * If an explicit gc is found, the explicit gc counter is incremented.
     * This way, the Explicit-GC Policy is, timing-wise, more accurate since the NUMAProfiler
     * is switched on exactly at the point when the last warm-up iteration has been finished
     * and therefore, profiles only what it should.
     */
    public static boolean isExplicitGC = false;

    /**
     * PROFILING POLICY 2: Flare-Object Driven
     * Trigger Event: A Flare-Object Allocation by the Application.
     * The following variable is used to help us ignore the application's
     * warmup iterations in order to profile only the effective part. The MaxineVM.profileThatObject()
     * method returns false as long as the NUMAProfilerFlareObject counter is below the NUMAProfilerFlareAllocationThreshold,
     * which is given by the user, ignoring any object allocation up to that point.
     * The NUMAProfilerFlareProfileWindow (default 1) indicates how many Flare Objects we need
     * to allocate before we stop the profiling.
     */
    public static  String NUMAProfilerFlareAllocationThresholds = "0";
    public static int[]  flareAllocationThresholds;
    @SuppressWarnings("FieldCanBeLocal")
    private static String NUMAProfilerFlareObjectStart          = "NUMAProfilerFlareObjectStart";
    @SuppressWarnings("FieldCanBeLocal")
    private static String NUMAProfilerFlareObjectEnd            = "NUMAProfilerFlareObjectEnd";

    /**
     * Buffers that keep the threadId of the threads that started profiling due to reaching the flare object
     * allocation threshold.
     */
    public static int[] flareObjectThreadIdBuffer;

    /**
     * A boolean variable, to show when the profiler is ON for the Flare Object Policy.
     */
    public static boolean enableFlareObjectProfiler = false;

    private static final int MINIMUMBUFFERSIZE = 500000;

    /**
     * The underlying hardware configuration.
     */
    static NUMALib numaConfig;

    /**
     * This String array holds the counters' names. Those names are passed to each VmThreadLocal instance initialization.
     */
    public static String[] objectAccessCounterNames;

    @CONSTANT_WHEN_NOT_ZERO
    private static Address heapStart;

    public Address toStart;
    public Address toEnd;
    public Address fromStart;
    public Address fromEnd;
    public final int memoryPageSize;

    /**
     * An enum that maps each Object Access Counter name with a {@link VmThreadLocal#profilingCounters} index.
     */
    public enum ACCESS_COUNTER {
        LOCAL_TUPLE_WRITE(0), INTERNODE_TUPLE_WRITE(1), INTERBLADE_TUPLE_WRITE(2),
        LOCAL_ARRAY_WRITE(3), INTERNODE_ARRAY_WRITE(4), INTERBLADE_ARRAY_WRITE(5),
        LOCAL_TUPLE_READ(6), INTERNODE_TUPLE_READ(7), INTERBLADE_TUPLE_READ(8),
        LOCAL_ARRAY_READ(9), INTERNODE_ARRAY_READ(10), INTERBLADE_ARRAY_READ(11);

        private final int value;

        ACCESS_COUNTER(int i) {
            value = i;
        }
    }

    // The options a user can pass to the NUMA Profiler.
    static {
        VMOptions.addFieldOption("-XX:", "NUMAProfilerVerbose", NUMAProfiler.class, "Verbose numa profiler output. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "NUMAProfilerBufferSize", NUMAProfiler.class, "NUMAProfiler's Buffer Size.");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerExplicitGCThreshold", NUMAProfiler.class,
                "The number of the Explicit GCs to be performed before the NUMAProfiler starts recording. " +
                "It cannot be used in combination with \"NUMAProfilerFlareAllocationThresholds\". (default: -1)");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareObjectStart", NUMAProfiler.class, "The Class of the Object to be sought after by the NUMAProfiler to start the profiling process. (default: 'AllocationProfilerFlareObject')");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareObjectEnd", NUMAProfiler.class, "The Class of the Object to be sought after by the NUMAProfiler to stop the profiling process. (default: 'AllocationProfilerFlareObject')");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareAllocationThresholds", NUMAProfiler.class,
                "The number of the Flare start objects to be allocated before the NUMAProfiler starts recording. " +
                "Multiple \"windows\" may be profiled by providing a comma separated list, " +
                "e.g. \"100,200,500\" will start profiling after the 100th, the 200th, and the 500th Flare object " +
                "allocation till the thread that started the profiling allocates a Flare end object. It cannot be used in combination with \"NUMAProfilerExplicitGCThreshold\". (default: \"0\")");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerDebug", NUMAProfiler.class, "Print information to help in NUMAProfiler's Validation. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "NUMAProfilerIncludeFinalization", NUMAProfiler.class, "Include memory accesses performed due to Finalization. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "NUMAProfilerIsolateDominantThread", NUMAProfiler.class, "Isolate the dominant thread object allocations (default: false)", MaxineVM.Phase.PRISTINE);

        objectAccessCounterNames = new String[]{
            "LOCAL_TUPLE_WRITES", "INTERNODE_TUPLE_WRITES", "INTERBLADE_TUPLE_WRITES",
            "LOCAL_ARRAY_WRITES", "INTERNODE_ARRAY_WRITES", "INTERBLADE_ARRAY_WRITES",
            "LOCAL_TUPLE_READS", "INTERNODE_TUPLE_READS", "INTERBLADE_TUPLE_READS",
            "LOCAL_ARRAY_READS", "INTERNODE_ARRAY_READS", "INTERBLADE_ARRAY_READS"
        };
    }

    public NUMAProfiler() {
        assert NUMALib.numalib_available() != -1 : "NUMAProfiler cannot be run without NUMA support";

        float beforeAllocProfiler = (float) Heap.reportUsedSpace() / (1024 * 1024);

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): NUMAProfiler Initialization.");
        }

        splitStringtoSortedIntegers();

        int survivorBufferSize = MINIMUMBUFFERSIZE;
        int allocatorBufferSize = MINIMUMBUFFERSIZE;

        if (NUMAProfilerBufferSize != 0) {
            if (NUMAProfilerBufferSize < MINIMUMBUFFERSIZE) {
                Log.print("WARNING: Small Buffer Size. Minimum Buffer Size applied! (=");
                Log.print(MINIMUMBUFFERSIZE);
                Log.println(")");
                allocatorBufferSize = MINIMUMBUFFERSIZE;
                survivorBufferSize = MINIMUMBUFFERSIZE;
            } else {
                allocatorBufferSize = NUMAProfilerBufferSize;
                survivorBufferSize = NUMAProfilerBufferSize;
            }
        }

        newObjects = new RecordBuffer(allocatorBufferSize, "New Objects Buffer");

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Survivor Objects NUMAProfiler Buffers.");
        }
        survivors1 = new RecordBuffer(survivorBufferSize, "Survivors Buffer No1");
        survivors2 = new RecordBuffer(survivorBufferSize, "Survivors Buffer No2");

        memoryPageSize = NUMALib.numaPageSize();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Heap Boundaries Buffer.");
        }
        initializeHeapBoundariesBuffer();

        numaConfig = new NUMALib();

        heapStart = vm().config.heapScheme().getHeapStartAddress();

        profilingCycle = 1;
        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialization Complete.");

            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(profilingCycle);
            Log.println("]");
        }

        float afterAllocProfiler = (float) Heap.reportUsedSpace() / (1024 * 1024);

        //initialize thread local counters
        initProfilingCounters();

        if (NUMAProfilerExplicitGCThreshold == 0) {
            enableProfiling();
        }

        if (NUMAProfilerDebug) {
            Log.println("*===================================================*\n" +
                    "* NUMA Profiler is on validation mode.\n" +
                    "*===================================================*\n" +
                    "* You can use NUMA Profiler with confidence if:\n" +
                    "* => a) VM Reported Heap Used Space = Initial Used Heap Space + NUMA Profiler Size + New Objects Size\n" +
                    "* => b) VM Reported Heap Used Space after GC = Initial Used Heap Space + NUMA Profiler Size + Survivor Objects Size\n" +
                    "* => c) Next Cycle's VM Reported Heap Used Space = Initial Used Heap Space + NUMA Profiler Size + Survivor Object Size\n" +
                    "*===================================================*\n");
            Log.println("Initial Used Heap Size = " + beforeAllocProfiler + " MB");
            float allocProfilerSize = afterAllocProfiler - beforeAllocProfiler;
            Log.println("NUMA Profiler Size = " + allocProfilerSize + " MB\n");
        }
    }

    /**
     * Check if the given hub is a hub of a Flare object and increase the
     * {@link #flareObjectCounter} if so.
     *
     * @param hub
     */
    public static void checkForFlareObject(Hub hub) {
        final boolean lockDisabledSafepoints = lock();
        if (MaxineVM.useNUMAProfiler && !NUMAProfilerFlareAllocationThresholds.equals("0")) {
            String type = hub.classActor.name();
            final int currentThreadID = VmThread.current().id();
            if (type.contains(NUMAProfilerFlareObjectStart)) {
                flareObjectCounter++;
                if (NUMAProfilerVerbose) {
                    Log.print("(NUMA Profiler): Start Flare-Object Counter: ");
                    Log.println(flareObjectCounter);
                }
                if (flareObjectCounter == flareAllocationThresholds[start_counter]) {
                    if (enableFlareObjectProfiler) {
                        throw FatalError.unexpected("The NUMA Profiler supports only a single profiling instance a time. " +
                            "It seams that there is already an ongoing Flare-Object profiling");
                    }
                    flareObjectThreadIdBuffer[start_counter] = currentThreadID;
                    if (NUMAProfilerVerbose) {
                        Log.print("(NUMA Profiler): Enable profiling due to flare object allocation for id ");
                        Log.println(currentThreadID);
                    }
                    if (start_counter < flareAllocationThresholds.length - 1) {
                        start_counter++;
                    }
                    if (NUMAProfiler.NUMAProfilerIsolateDominantThread) {
                        setProfilingTLA.run(VmThread.currentTLA());
                    } else {
                        enableProfiling();
                    }
                    enableFlareObjectProfiler = true;
                }
            } else if (enableFlareObjectProfiler == true && flareObjectThreadIdBuffer[end_counter] == currentThreadID && type.contains(NUMAProfilerFlareObjectEnd)) {
                if (NUMAProfilerVerbose) {
                    Log.print("(NUMA Profiler): Disable profiling due to flare end object allocation for id ");
                    Log.println(currentThreadID);
                }
                end_counter++;
                if (NUMAProfiler.NUMAProfilerIsolateDominantThread) {
                    resetProfilingTLA.run(VmThread.currentTLA());
                } else {
                    disableProfiling();
                }
                enableFlareObjectProfiler = false;
            }
        }
        unlock(lockDisabledSafepoints);
    }

    public static boolean shouldProfile() {
        if (MaxineVM.useNUMAProfiler) {
            int profilerTLA = PROFILER_STATE.load(VmThread.currentTLA()).toInt();
            return profilerTLA == PROFILING_STATE.ENABLED.getValue();
        }
        return false;
    }

    private void initializeHeapBoundariesBuffer() {
        int bufSize = Heap.maxSize().dividedBy(memoryPageSize).toInt();
        heapPages = new VirtualPagesBuffer(bufSize);
        heapPages.writeNumaNode(0, NUMALib.numaNodeOfAddress(heapStart.toLong()));
    }

    /**
     * This method is called when a profiled object is allocated.
     */
    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public static void profileNew(int size, String type, long address) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();
        //transform the object type from String to char[] and pass the charArrayBuffer[] to record
        final int threadId = VmThread.current().id();
        //guard RecordBuffer from overflow
        FatalError.check(newObjects.currentIndex < newObjects.bufferSize, "Allocations Buffer out of bounds. Increase the Buffer Size.");
        newObjects.record(uniqueId, threadId, JDK_java_lang_String.getCharArray(type), size, address);
        uniqueId++;
        totalNewSize = totalNewSize + size;
        unlock(lockDisabledSafepoints);
    }

    /**
     * This method assesses the locality of a memory access and returns the {@link ACCESS_COUNTER} value to be incremented.
     * A memory access can be either local (a thread running on N numa node accesses an object on N numa node),
     * inter-node (a thread running on N numa node accesses an object on M numa node with both N and M being on the same blade),
     * or inter-blade (a thread running on N numa node accesses an object on Z numa node which is part of another blade).
     * @param address
     * @return {@code accessCounterValue} + 0 for LOCAL access, {@code accessCounterValue} + 1 for INTER-NODE access, {@code accessCounterValue} + 2 for INTER-BLADE access (see {@link ACCESS_COUNTER} values)
     *
     */
    private static int assessAccessLocality(long address, int accessCounterValue) {
        // get the Numa Node where the thread which is performing the write is running
        final int threadNumaNode = Intrinsics.getCpuID() >> MaxineIntrinsicIDs.NUMA_NODE_SHIFT;
        // get the Numa Node where the written object is placed
        final int objectNumaNode = getNumaNodeForAddress(address);

        if (threadNumaNode != objectNumaNode) {
            // get the Blade where the thread Numa Node is located
            final int threadBlade = threadNumaNode / 6;
            // get the Blade where the object Numa Node is located
            final int objectBlade = objectNumaNode / 6;
            if (threadBlade != objectBlade) {
                return accessCounterValue + 2;
            } else {
                return accessCounterValue + 1;
            }
        } else {
            return accessCounterValue;
        }
    }

    private static void increaseAccessCounter(int counter) {
        Pointer tla = VmThread.currentTLA();
        assert ETLA.load(tla) == tla;
        long value = profilingCounters[counter].load(tla).toLong() + 1;
        profilingCounters[counter].store(tla, Address.fromLong(value));
    }

    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public static void profileAccess(ACCESS_COUNTER counter, long address) {

        // if the written object is not part of the data heap
        // TODO: implement some action, currently ignore
        if (!vm().config.heapScheme().contains(Address.fromLong(address))) {
            return;
        }

        final int accessCounter = assessAccessLocality(address, counter.value);

        // increment local or remote writes
        increaseAccessCounter(accessCounter);
    }

    /**
     * Dump NUMAProfiler Buffer to Maxine's Log output.
     */
    private void dumpBuffer() {
        final boolean lockDisabledSafepoints = lock();
        if (NUMAProfilerVerbose) {
            Log.print("==== Profiling Cycle ");
            Log.print(profilingCycle);
            Log.println(" ====");
        }
        newObjects.print(profilingCycle, 1);
        unlock(lockDisabledSafepoints);
    }

    private void dumpSurvivors() {
        final boolean lockDisabledSafepoints = lock();
        if (NUMAProfilerVerbose) {
            Log.print("==== Survivors Cycle ");
            Log.print(profilingCycle);
            Log.println(" ====");
        }
        if ((profilingCycle % 2) == 0) {
            survivors2.print(profilingCycle, 0);
        } else {
            survivors1.print(profilingCycle, 0);
        }
        unlock(lockDisabledSafepoints);
    }

    private void dumpHeapBoundaries() {
        final boolean lockDisabledSafepoints = lock();
        heapPages.printStats(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    private void resetHeapBoundaries() {
        final boolean lockDisabledSafepoints = lock();
        heapPages.resetBuffer();
        heapPages.writeNumaNode(0, NUMALib.numaNodeOfAddress(heapStart.toLong()));
        unlock(lockDisabledSafepoints);
    }

    /**
     * Finds the index of the memory page of an address in the heapPages Buffer.
     * It is based on the calculation:
     * pageIndex = (address - firstPageAddress) / pageSize
     * @param address an address
     * @return the memory page index of the address
     */
    private static int getHeapPagesIndexOfAddress(Address address) {
        return address.minus(heapStart).dividedBy(numaProfiler.memoryPageSize).toInt();
    }

    /**
     * Find the NUMA Node for each allocated Object.
     * For every object, call the {@linkplain #getNumaNodeForAddress(long)} method to get its physical NUMA Node.
     * Write the node in newObjects Buffer.
     */
    private void findNumaNodeForAllAllocatedObjects() {
        long objectAddress;

        for (int i = 0; i < newObjects.currentIndex; i++) {
            objectAddress = newObjects.readAddr(i);
            int node = getNumaNodeForAddress(objectAddress);

            // compare the calculated object numa node with the libnuma system
            // call returned value for validation (note: increased overhead)
            assert node == NUMALib.numaNodeOfAddress(newObjects.readAddr(i));

            // Write the node in the buffer
            newObjects.writeNode(i, node);
        }
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native MemoryRegion asMemoryRegion(Object object);

    private final Pointer.Procedure findNumaNodeForSpace = new Pointer.Procedure() {
        public void run(Pointer pointer) {
            Reference    reference = Reference.fromOrigin(pointer);
            MemoryRegion space     = asMemoryRegion(reference);
            findNumaNodeForAllSpaceMemoryPages(space);
        }
    };

    /**
     * Find the NUMA Node for each virtual memory page of the JVM Heap.
     * Currently implemented only for the {@link SemiSpaceHeapScheme}.
     */
    private void findNumaNodeForAllHeapMemoryPages() {
        vm().config.heapScheme().forAllSpaces(findNumaNodeForSpace);
    }

    /**
     * Find the NUMA node for each memory page in the premises of a specific Memory Space.
     *
     * @param space
     */
    private void findNumaNodeForAllSpaceMemoryPages(MemoryRegion space) {
        int pageIndex;
        int node;

        Address currentAddress = space.start();

        while (currentAddress.lessThan(space.end())) {
            // Get NUMA node of address using NUMALib
            node = NUMALib.numaNodeOfAddress(currentAddress.toLong());
            // Get the index of the memory page in the heapPages Buffer
            pageIndex = getHeapPagesIndexOfAddress(currentAddress);
            // Write the NUMA node of the page in the heapPages Buffer
            heapPages.writeNumaNode(pageIndex, node);

            // Get the next memory page address
            currentAddress = currentAddress.plus(memoryPageSize);

            // if no NUMA node is found the page is still unallocated
            if (node == NUMALib.EFAULT) {
                node = VirtualPagesBuffer.maxNumaNodes;
            }

            // update stats
            int count = heapPages.readStats(node);
            heapPages.writeStats(node, count + 1);
        }

    }

    /**
     * Get the physical NUMA node id for a virtual address.
     *
     * We use {@code heapPages} (a {@link VirtualPagesBuffer} instance) as a "cache" that stores a mapping
     * to a physical NUMA node for each virtual memory page. We calculate the index of the memory page into
     * the cache (to avoid the linear search) and we get the corresponding NUMA node.
     * It might return EFAULT (=-14) in case it is the first hit of the memory page in the current cycle.
     * In that case the system call from NUMALib is called directly and the values are updated.
     *
     * @param address
     * @return physical NUMA node id
     */
    private static int getNumaNodeForAddress(long address) {
        int pageIndex = getHeapPagesIndexOfAddress(Address.fromLong(address));

        int objNumaNode = heapPages.readNumaNode(pageIndex);
        // if outdated, use the sys call to get the numa node and update heapPages buffer
        if (objNumaNode == NUMALib.EFAULT) {
            Address pageAddr = heapStart.plus(Address.fromInt(numaProfiler.memoryPageSize).times(pageIndex));
            int node = NUMALib.numaNodeOfAddress(pageAddr.toLong());
            heapPages.writeNumaNode(pageIndex, node);
            objNumaNode = node;
        }
        return objNumaNode;
    }

    /**
     * Search "from" buffer for survivor objects and store them into "to" buffer.
     * @param from the source buffer in which we search for survivor objects.
     * @param to the destination buffer in which we store the survivor objects.
     */
    private void storeSurvivors(RecordBuffer from, RecordBuffer to) {
        if (NUMAProfilerVerbose) {
            Log.print("(NUMA Profiler): Copy Survived Objects from ");
            Log.print(from.buffersName);
            Log.print(" to ");
            Log.println(to.buffersName);
        }
        for (int i = 0; i < from.currentIndex; i++) {
            long address = from.readAddr(i);
            /*
            if an object is alive, update both its Virtual Address and
            NUMA Node before copy it to the survivors buffer
             */
            if (Heap.isSurvivor(address)) {
                // update Virtual Address
                long newAddr = Heap.getForwardedAddress(address);
                // update NUMA Node
                int node = NUMALib.numaNodeOfAddress(newAddr);
                //guard survivors RecordBuffer from overflow
                FatalError.check(to.currentIndex < to.bufferSize, "Survivor Buffer out of bounds! Increase the Buffer Size.");
                // write it to Buffer
                to.record(from.readId(i), from.readThreadId(i), from.readType(i), from.readSize(i), newAddr, node);
                totalSurvSize = totalSurvSize + from.readSize(i);
            }
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
    private void profileSurvivors() {

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

    private void printProfilingCyclyStats() {
        Log.print("Cycle ");
        Log.println(profilingCycle);

        Log.print("=> (NUMAProfiler Reports): New Objects Size =");
        Log.print((float) totalNewSize / (1024 * 1024));
        Log.println(" MB");

        Log.print("=> (VM Reports): Heap Used Space =");
        Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
        Log.println(" MB");

        newObjects.printUsage();
    }

    /**
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     */
    void preGCActions() {

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Entering Pre-GC Phase.");
            Log.print("(NUMA Profiler): Cycle ");
            Log.print(profilingCycle);
            Log.println(" Profiling Is Now Complete. [pre-GC phase]");
        }

        // guard libnuma sys call usage during non-profiling cycles
        if (newObjects.currentIndex > 0) {
            findNumaNodeForAllHeapMemoryPages();
            findNumaNodeForAllAllocatedObjects();
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump NUMAProfiler Buffer. [pre-GC phase]");
        }

        if (NUMAProfilerDebug) {
            //in validation mode don't dump buffer
            printProfilingCyclyStats();
            Log.println("Garbage Collection");
        } else {
            dumpHeapBoundaries();
            dumpBuffer();
        }

        printProfilingCounters();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Pre-GC Phase.");
        }
    }

    /**
     *  This method is called every time a GC has been completed.
     */
    void postGCActions() {

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Entering Post-GC Phase.");
        }

        profileSurvivors();

        if ((profilingCycle % 2) == 0) {
            if (NUMAProfilerVerbose) {
                Log.println("(NUMA Profiler): Clean-up Survivor1 Buffer. [post-gc phase]");
            }
            survivors1.resetBuffer();
        } else {
            if (NUMAProfilerVerbose) {
                Log.println("(NUMA Profiler): Clean-up Survivor2 Buffer. [post-gc phase]");
            }
            survivors2.resetBuffer();
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Clean-up NUMAProfiler Buffer. [post-gc phase]");
        }
        newObjects.resetBuffer();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump Survivors Buffer. [post-GC phase]");
        }

        if (NUMAProfilerDebug) {
            //in validation mode don't dump buffer
            Log.print("=> (NUMAProfiler Reports): Survivor Objects Size =");
            Log.print((float) totalSurvSize / (1024 * 1024));
            Log.println(" MB");

            Log.print("=> (VM Reports): Heap Used Space =");
            Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
            Log.println(" MB\n");

            survivors1.printUsage();
            survivors2.printUsage();
        } else {
            dumpSurvivors();
        }

        totalNewSize = totalSurvSize;
        totalSurvSize = 0;

        // Check if the current GC is explicit. If yes, increase the iteration counter.
        if (isExplicitGC) {
            iteration++;
            isExplicitGC = false;
            if (iteration == NUMAProfiler.NUMAProfilerExplicitGCThreshold) {
                if (NUMAProfilerVerbose) {
                    Log.println("(NUMA Profiler): Enabling profiling. [post-GC phase]");
                }
                enableProfiling();
            }
        }

        profilingCycle++;

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Post-GC Phase.");
            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(profilingCycle);
            Log.println("]");
        }

    }

    /**
     *  A method to transform a string of that form "int0,int1,int2" into an integer array [int0, int1, int2].
     */
    private void splitStringtoSortedIntegers() {

        String[] thresholds = NUMAProfilerFlareAllocationThresholds.split(",");
        flareObjectThreadIdBuffer = new int[thresholds.length];
        flareAllocationThresholds = new int[thresholds.length];
        for (int i = 0; i < thresholds.length; i++) {
            flareAllocationThresholds[i] = Integer.parseInt(thresholds[i]);
        }
        Arrays.sort(flareAllocationThresholds);

    }


    private static final Pointer.Predicate profilingPredicate = new Pointer.Predicate() {
        @Override
        public boolean evaluate(Pointer tla) {
            VmThread vmThread = VmThread.fromTLA(tla);
            return vmThread.javaThread() != null &&
                    !vmThread.isVmOperationThread() &&
                    (NUMAProfilerIncludeFinalization || !vmThread.getName().equals("Finalizer"));
        }
    };

    private static final Pointer.Procedure setProfilingTLA = new Pointer.Procedure() {
        public void run(Pointer tla) {
            Pointer etla = ETLA.load(tla);
            PROFILER_STATE.store(etla, Address.fromInt(PROFILING_STATE.ENABLED.value));
        }
    };

    private static void enableProfiling() {
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, setProfilingTLA);
    }

    private static final Pointer.Procedure resetProfilingTLA = new Pointer.Procedure() {
        public void run(Pointer tla) {
            Pointer etla = ETLA.load(tla);
            PROFILER_STATE.store(etla, Address.fromInt(PROFILING_STATE.DISABLED.value));
        }
    };

    private static void disableProfiling() {
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, resetProfilingTLA);
    }

    /**
     * A {@link Pointer.Procedure} that prints a thread's all Object Access Profiling Counters}.
     */
    private static final Pointer.Procedure printThreadLocalProfilingCounters = new Pointer.Procedure() {
        public void run(Pointer tla) {
            final boolean lockDisabledSafepoints = lock();
            Pointer etla = ETLA.load(tla);
            for (int i = 0; i < profilingCounters.length; i++) {
                VmThreadLocal profilingCounter = profilingCounters[i];
                final long count = profilingCounter.load(etla).toLong();
                if (count != 0) {
                    Log.print("(accessCounter);");
                    Log.print(profilingCycle);
                    Log.print(";");
                    Log.print(VmThread.fromTLA(etla).id());
                    Log.print(";");
                    Log.print(profilingCounter.name);
                    Log.print(";");
                    Log.println(count);
                }
                //reset counter
                profilingCounter.store(etla, Address.fromInt(0));
            }
            unlock(lockDisabledSafepoints);
        }
    };

    /**
     * Call {@link #initThreadLocalProfilingCounters} for all ACTIVE threads.
     */
    private static void printProfilingCounters() {
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, printThreadLocalProfilingCounters);
    }

    /**
     * A method to print the Access Profiling Counters of one specific thread.
     * @param tla
     */
    public static void printProfilingCountersOfThread(Pointer tla) {
        printThreadLocalProfilingCounters.run(tla);
    }

    /**
     * A {@link Pointer.Procedure} that initializes a thread's all Object Access Profiling Counters}.
     */
    private static final Pointer.Procedure initThreadLocalProfilingCounters = new Pointer.Procedure() {
        public void run(Pointer tla) {
            Pointer etla = ETLA.load(tla);
            for (int i = 0; i < profilingCounters.length; i++) {
                VmThreadLocal profilingCounter = profilingCounters[i];
                profilingCounter.store(etla, Address.fromInt(0));
            }
        }
    };

    /**
     * Call {@link #initThreadLocalProfilingCounters} for all ACTIVE threads.
     */
    private static void initProfilingCounters() {
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, initThreadLocalProfilingCounters);
    }

    private void releaseReservedMemory() {
        newObjects.deallocateAll();
        survivors1.deallocateAll();
        survivors2.deallocateAll();
        heapPages.deallocateAll();
    }

    /**
     * This method can be used for actions need to take place right before
     * NUMA Profiler's termination. It is triggered when JavaRunScheme
     * is being terminated. Dumps the final profiling cycle which is not
     * followed by any GC.
     */
    public void terminate() {

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Disable profiling for termination");
        }

        // Disable profiling for shutdown
        PROFILER_STATE.store(VmThread.currentTLA(), Address.fromInt(PROFILING_STATE.DISABLED.value));

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Termination");
        }

        // guard libnuma sys call usage during non-profiling cycles
        if (newObjects.currentIndex > 0) {
            findNumaNodeForAllHeapMemoryPages();
            findNumaNodeForAllAllocatedObjects();
        }

        if (!NUMAProfilerDebug) {
            dumpHeapBoundaries();
            dumpBuffer();
        } else {
            //in validation mode don't dump buffer
            printProfilingCyclyStats();
        }

        printProfilingCounters();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Release Reserved Memory.");
        }

        if (!NUMAProfilerIncludeFinalization) {
            releaseReservedMemory();
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Terminating... Bye!");
        }
    }

    private static VmThread lockOwner;
    private static int lockDepth;

    /**
     * lock() and unlock() methods have been implemented according to the Log.lock() and Log.unlock() ones.
     *
     */
    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public static boolean lock() {
        if (isHosted()) {
            return true;
        }

        boolean wasDisabled = SafepointPoll.disable();
        NUMAProfiler.numaProfiler_lock();
        if (lockDepth == 0) {
            FatalError.check(lockOwner == null, "numa profiler lock should have no owner with depth 0");
            lockOwner = VmThread.current();
        }
        lockDepth++;
        return !wasDisabled;
    }

    /**
     * lock() and unlock() methods have been implemented according to the Log.lock() and Log.unlock() ones.
     *
     */
    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public static void unlock(boolean lockDisabledSafepoints) {
        if (isHosted()) {
            return;
        }

        --lockDepth;
        FatalError.check(lockDepth >= 0, "mismatched lock/unlock");
        FatalError.check(lockOwner == VmThread.current(), "numa profiler lock should be owned by current thread");
        if (lockDepth == 0) {
            lockOwner = null;
        }
        NUMAProfiler.numaProfiler_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by NUMAProfiler.lock() and NUMAProfiler.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }
}
