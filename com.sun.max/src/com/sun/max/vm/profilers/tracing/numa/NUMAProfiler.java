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

package com.sun.max.vm.profilers.tracing.numa;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

public class NUMAProfiler {

    public static int flareObjectCounter = 0;

    @C_FUNCTION
    static native void numaProfiler_lock();

    @C_FUNCTION
    static native void numaProfiler_unlock();

    public static int profilingCycle;
    public static int uniqueId = 0;
    /**
     * The NUMAProfiler Buffer for newly allocated objects.
     */
    public static RecordBuffer newObjects;

    /**
     * The NUMAProfiler Buffers for survivor objects. We use two identical buffers because
     * allocation is disabled at the point we need to move the data in a clean buffer.
     */
    public static RecordBuffer survivors1;
    public static RecordBuffer survivors2;

    /**
     * The Buffer who keeps track of the physical NUMA node of any virtual memory page allocated for the JVM Heap.
     */
    public static VirtualPagesBuffer heapPages;

    private static boolean NUMAProfilerAll;
    public static boolean NUMAProfilerVerbose;
    public static int NUMAProfilerBufferSize;
    public static boolean NUMAProfilerDebug;

    public static int totalNewSize = 0;
    public static int totalSurvSize = 0;

    /**
     * A buffer to transform a String object to char array.
     */
    private static char[] charArrayBuffer;
    public static int charArrayBufferLength;

    /**
     * PROFILING POLICY 1: Explicit GC Driven
     * Trigger Event: An Application's System.gc() call.
     * The following two variables are used to help us ignore the application's
     * warmup iterations in order to profile only the effective part. The iteration
     * is calculated by the number of System.gc() calls. The MaxineVM.profileThatObject()
     * method returns false as long as the iteration counter is below the NUMAProfilerExplicitGCThreshold, which
     * is given by the user, ignoring any object allocation up to that point.
     */
    public static int NUMAProfilerExplicitGCThreshold;
    public static int iteration = 0;

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
    public static int NUMAProfilerFlareAllocationThreshold;
    public static int NUMAProfilerFlareProfileWindow = 1;
    public static String NUMAProfilerFlareObject = "NUMAProfilerFlareObject";

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
     * The underlying hardware configuration.
     */
    public static NUMALib numaConfig;

    public static int tupleWrites = 0;
    public static int localTupleWrites = 0;
    public static int remoteTupleWrites = 0;

    public static int arrayWrites = 0;
    public static int localArrayWrites = 0;
    public static int remoteArrayWrites = 0;

    public static int tupleReads = 0;
    public static int localTupleReads = 0;
    public static int remoteTupleReads = 0;

    private int wasProfiling;

    /**
     * The options a user can pass to the NUMA Profiler.
     */
    static {
        VMOptions.addFieldOption("-XX:", "NUMAProfilerAll", NUMAProfiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "NUMAProfilerVerbose", NUMAProfiler.class, "Verbose numa profiler output. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "NUMAProfilerBufferSize", NUMAProfiler.class, "NUMAProfiler's Buffer Size.");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerExplicitGCThreshold", NUMAProfiler.class, "The number of the Explicit GCs to be performed before the NUMAProfiler starts recording. (default: 0)");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareObject", NUMAProfiler.class, "The Class of the Object to be sought after by the NUMAProfiler to drive the profiling process. (default: 'AllocationProfilerFlareObject')");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareAllocationThreshold", NUMAProfiler.class, "The number of the Flare objects to be allocated before the NUMAProfiler starts recording. (default: 0)");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerFlareProfileWindow", NUMAProfiler.class, "The number of the Flare objects to be allocated before the NUMAProfiler stops recording. (default: 1)");
        VMOptions.addFieldOption("-XX:", "NUMAProfilerDebug", NUMAProfiler.class, "Print information to help in NUMAProfiler's Validation. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public NUMAProfiler() {
        assert NUMALib.numalib_available() != -1 : "NUMAProfiler cannot be run without NUMA support";

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): NUMAProfiler Initialization.");
        }

        if (NUMAProfilerBufferSize != 0) {
            if (NUMAProfilerBufferSize < MINIMUMBUFFERSIZE) {
                Log.print("WARNING: Small Buffer Size. Minimum Buffer Size applied! (=");
                Log.print(MINIMUMBUFFERSIZE);
                Log.println(")");
                allocBufferSize = MINIMUMBUFFERSIZE;
                survBufferSize = MINIMUMBUFFERSIZE;
            } else {
                allocBufferSize = NUMAProfilerBufferSize;
                survBufferSize = NUMAProfilerBufferSize;
            }
        }

        newObjects = new RecordBuffer(allocBufferSize, "New Objects Buffer");

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Survivor Objects NUMAProfiler Buffers.");
        }
        survivors1 = new RecordBuffer(survBufferSize, "Survivors Buffer No1");
        survivors2 = new RecordBuffer(survBufferSize, "Survivors Buffer No2");

        charArrayBuffer = new char[RecordBuffer.MAX_CHARS];

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Heap Boundaries Buffer.");
        }
        initializeHeapBoundariesBuffer();

        numaConfig = new NUMALib();

        profilingCycle = 1;
        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialization Complete.");

            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }
    }

    /**
     * Check if the given hub is a hub of a Flare object and increase the
     * {@link #flareObjectCounter} if so.
     *
     * @param hub
     */
    public static void checkForFlareObject(Hub hub) {
        if (MaxineVM.useNUMAProfiler) {
            String type = hub.classActor.name();
            if (NUMAProfilerFlareAllocationThreshold != 0 &&
                            (NUMAProfilerFlareAllocationThreshold + NUMAProfilerFlareProfileWindow > flareObjectCounter) &&
                            type.contains(NUMAProfilerFlareObject)) {
                flareObjectCounter++;
                // FIXME: currently enables profiling only for current thread
                if (flareObjectCounter > NUMAProfilerFlareAllocationThreshold &&
                                flareObjectCounter < (NUMAProfilerFlareAllocationThreshold + NUMAProfilerFlareProfileWindow)) {
                    PROFILER_TLA.store3(VmThread.currentTLA(), Address.fromInt(1));
                    if (NUMAProfilerVerbose) {
                        Log.println("Enable profiling Flare");
                    }
                } else if (flareObjectCounter >= (NUMAProfilerFlareAllocationThreshold + NUMAProfilerFlareProfileWindow)) {
                    PROFILER_TLA.store3(VmThread.currentTLA(), Address.fromInt(0));
                    if (NUMAProfilerVerbose) {
                        Log.println("Disable profiling Flare");
                    }
                }
            }
        }
    }

    /**
     * Find heap's first page address and Numa Node and set it in heapPages.
     */
    public void findFirstHeapPage() {
        Address startAddress = vm().config.heapScheme().getHeapStartAddress();
        int node = NUMALib.numaNodeOfAddress(startAddress.toLong());
        heapPages.writeAddr(0, startAddress.toLong());
        heapPages.writeNumaNode(0, node);
    }

    public void initializeHeapBoundariesBuffer() {
        int pageSize = NUMALib.numaPageSize();
        int bufSize = Heap.maxSize().dividedBy(pageSize).toInt();
        heapPages = new VirtualPagesBuffer(bufSize);
        findFirstHeapPage();
    }

    public int getProfilingCycle() {
        return profilingCycle;
    }

    public static boolean profileAll() {
        return NUMAProfilerAll;
    }

    public static boolean warmupFinished() {
        return iteration > NUMAProfilerExplicitGCThreshold || NUMAProfilerExplicitGCThreshold == 0;
    }

    public static boolean objectWarmupFinished() {
        return flareObjectCounter >= NUMAProfilerFlareAllocationThreshold && flareObjectCounter <= NUMAProfilerFlareAllocationThreshold + (NUMAProfilerFlareProfileWindow - 1);
    }

    /**
     * This method has the same functionality as the String.toCharArray() but
     * we avoid the new object creation.
     * @param str, The String to be converted to char array.
     */
    public static char[] asCharArray(String str) {
        int i = 0;
        while (i < str.length()) {
            charArrayBuffer[i] = str.charAt(i);
            i++;
        }
        charArrayBuffer[i] = '\0';
        charArrayBufferLength = i;
        return charArrayBuffer;
    }

    public void printCharArrayBuffer(char[] array, int length) {
        for (int i = 0; i < length; i++) {
            Log.print(array[i]);
        }
        Log.println("");
    }

    public static boolean ongoingAllocation = false;

    /**
     * This method is called when a profiled object is allocated.
     */
    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public void profileNew(int size, String type, long address) {
        /* PROFILER_TLA is currently a thread local that has it's value maintained
         * only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA. That
         * said if we lock and disable safepoints it is no longer accessible, thus
         * we read it before locking. */
        final boolean lockDisabledSafepoints = lock();
        //transform the object type from String to char[] and pass the charArrayBuffer[] to record
        charArrayBuffer = asCharArray(type);
        final int threadId = VmThread.current().id();
        //detect recursive allocations if another allocation is ongoing
        if (ongoingAllocation) {
            Log.println("Recursive Allocation. ");
            Log.println(type);
            throw FatalError.unexpected("Recursive Allocation.");
        }
        ongoingAllocation = true;
        //guard RecordBuffer from overflow
        if (newObjects.currentIndex >= newObjects.bufferSize) {
            Log.print("Allocations Buffer out of bounds. Increase the Buffer Size.");
            MaxineVM.exit(1);
        }
        newObjects.record(uniqueId, threadId, charArrayBuffer, size, address);
        uniqueId++;
        totalNewSize = totalNewSize + size;
        ongoingAllocation = false;
        unlock(lockDisabledSafepoints);
    }

    public int getThreadNumaNode() {
        return Intrinsics.getCpuID() >> MaxineIntrinsicIDs.NUMA_NODE_SHIFT;
    }

    /**
     * Fetch the physical NUMA node id from {@link NUMALib#coreToNUMANodeMap}
     *
     * We check whether the NUMA node is found. It might return EFAULT in case the page was still
     * unallocated to a physical NUMA node by the last {@link NUMALib#coreToNUMANodeMap}'s update.
     * In that case the system call from NUMALib is called directly and the values are updated.
     *
     * @param firstPageAddress
     * @param address
     * @return physical NUMA node id
     */
    public int getObjectNumaNode(long firstPageAddress, long address) {
        int pageSize = NUMALib.numaPageSize();
        long numerator = address - firstPageAddress;
        long div = numerator / (long) pageSize;
        int pageIndex = (int) div;

        int objNumaNode = heapPages.readNumaNode(pageIndex);
        // check whether the numa node is found
        if (objNumaNode == NUMALib.EFAULT) {
            long pageAddr = heapPages.readAddr(pageIndex);
            int node = NUMALib.numaNodeOfAddress(pageAddr);
            heapPages.writeNumaNode(pageIndex, node);
            objNumaNode = node;
        }
        return objNumaNode;
    }

    public boolean inDataHeap(long firstPageAddress, long address) {
        return address >= firstPageAddress;
    }

    public boolean isRemoteAccess(long firstPageAddress, long address) {
        // get the Numa Node where the thread which is performing the write is running
        final int threadNumaNode = getThreadNumaNode();
        // get the Numa Node where the written object is placed
        final int objectNumaNode = getObjectNumaNode(firstPageAddress, address);

        return threadNumaNode != objectNumaNode;
    }

    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public void profileWriteAccessTuple(long tupleAddress) {
        long firstPageAddress = heapPages.readAddr(0);

        // if the written object is not part of the data heap
        // TODO: implement some action, currently ignore
        if (!inDataHeap(firstPageAddress, tupleAddress)) {
            // no heap object, ignore
            return;
        }

        // increment local or remote writes
        if (isRemoteAccess(firstPageAddress, tupleAddress)) {
            remoteTupleWrites++;
        } else {
            localTupleWrites++;
        }

        // increment total writes
        tupleWrites++;
    }


    public void profileWriteAccessArray(long arrayAddress) {
        long firstPageAddress = heapPages.readAddr(0);

        // if the written array is not part of the data heap
        // TODO: implement some action, currently ignore
        if (!inDataHeap(firstPageAddress, arrayAddress)) {
            // no heap array, ignore
            return;
        }

        // increment local or remote writes
        if (isRemoteAccess(firstPageAddress, arrayAddress)) {
            remoteArrayWrites++;
        } else {
            localArrayWrites++;
        }

        // increment total writes
        arrayWrites++;
    }

    @NO_SAFEPOINT_POLLS("numa profiler call chain must be atomic")
    @NEVER_INLINE
    public void profileReadAccessTuple(long tupleAddress) {
        long firstPageAddress = heapPages.readAddr(0);

        // if the read object is not part of the data heap
        // TODO: implement some action, currently ignore
        if (!inDataHeap(firstPageAddress, tupleAddress)) {
            // no heap object, ignore
            return;
        }

        // increment local or remote reads
        if (isRemoteAccess(firstPageAddress, tupleAddress)) {
            remoteTupleReads++;
        } else {
            localTupleReads++;
        }

        // increment total writes
        tupleReads++;
    }

    /**
     * Print the stats for Object Accesses.
     */
    public void printObjectAccessStats() {
        Log.print("(NUMA Profiler): Total Tuple Writes = ");
        Log.println(tupleWrites);
        Log.print("(NUMA Profiler): Remote Tuple Writes = ");
        Log.println(remoteTupleWrites);
        Log.print("(NUMA Profiler): Local Tuple Writes = ");
        Log.println(localTupleWrites);

        Log.print("(NUMA Profiler): Total Array Writes = ");
        Log.println(arrayWrites);
        Log.print("(NUMA Profiler): Remote Array Writes = ");
        Log.println(remoteArrayWrites);
        Log.print("(NUMA Profiler): Local Array Writes = ");
        Log.println(localArrayWrites);

        Log.print("(NUMA Profiler): Total Tuple Reads = ");
        Log.println(tupleReads);
        Log.print("(NUMA Profiler): Remote Tuple Reads = ");
        Log.println(remoteTupleReads);
        Log.print("(NUMA Profiler): Local Tuple Reads = ");
        Log.println(localTupleReads);
    }

    /**
     * Dump NUMAProfiler Buffer to Maxine's Log output.
     */
    public void dumpBuffer() {
        final boolean lockDisabledSafepoints = lock();
        if (NUMAProfilerVerbose) {
            Log.print("==== Profiling Cycle ");
            Log.print(profilingCycle);
            Log.println(" ====");
        }
        newObjects.print(profilingCycle, 1);
        unlock(lockDisabledSafepoints);
    }

    public void dumpSurvivors() {
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

    public void dumpHeapBoundaries() {
        final boolean lockDisabledSafepoints = lock();
        heapPages.printStats(profilingCycle);
        unlock(lockDisabledSafepoints);
    }

    public void resetHeapBoundaries() {
        final boolean lockDisabledSafepoints = lock();
        heapPages.resetBuffer();
        findFirstHeapPage();
        unlock(lockDisabledSafepoints);
    }

    /**
     * Find the NUMA Node for each allocated Object.
     * For every object, find the virtual memory page where the object is placed and get its physical NUMA Node.
     */
    public void findObjectNumaNode() {
        int pageSize = NUMALib.numaPageSize();
        long objectAddress;
        long firstPageAddress = heapPages.readAddr(0);
        int pageIndex;
        int maxPageIndex = heapPages.pagesCurrentIndex;
        for (int i = 0; i < newObjects.currentIndex; i++) {
            objectAddress = newObjects.readAddr(i);
            // safe for heap up to 8TB
            pageIndex = (int) (objectAddress - firstPageAddress) / pageSize;
            if (pageIndex > maxPageIndex) {
                Log.println("Heap Ranges Overflow");
                MaxineVM.exit(1);
            }
            int node = heapPages.readNumaNode(pageIndex);
            // compare the calculated object numa node with the libnuma system
            // call returned value for validation (note: increased overhead)
            assert node == NUMALib.numaNodeOfAddress(newObjects.readAddr(i));
            newObjects.writeNode(i, node);

            if (VirtualPagesBuffer.debug) {
                Log.print("object in address ");
                Log.print(objectAddress);
                Log.print(" found in range ");
                Log.print(pageIndex);
                Log.print(" [");
                Log.print(heapPages.readAddr(pageIndex));
                Log.println("]");
            }
        }
    }

    /**
     * Find the NUMA Node for each virtual memory page of the JVM Heap.
     */
    public void findNumaNodeForPages() {
        assert vm().config.heapScheme() instanceof SemiSpaceHeapScheme;
        Address startAddress = vm().config.heapScheme().getHeapStartAddress();
        Address currentAddress = startAddress;
        int     index          = 0;
        while (vm().config.heapScheme().contains(currentAddress)) {
            int node = NUMALib.numaNodeOfAddress(currentAddress.toLong());
            if (VirtualPagesBuffer.debug) {
                Log.print("write starting address ");
                Log.print(currentAddress.toLong());
                Log.print(" of range ");
                Log.println(index);
            }
            heapPages.writeAddr(index, currentAddress.toLong());
            heapPages.writeNumaNode(index, node);
            index++;
            currentAddress = currentAddress.plus(NUMALib.numaPageSize());
            //update stats
            if (node < 0) {
                node = VirtualPagesBuffer.maxNumaNodes;
            }
            int count = heapPages.readStats(node);
            heapPages.writeStats(node, count + 1);
        }

    }

    /**
     * Search "from" buffer for survivor objects and store them into "to" buffer.
     * @param from the source buffer in which we search for survivor objects.
     * @param to the destination buffer in which we store the survivor objects.
     */
    public void storeSurvivors(RecordBuffer from, RecordBuffer to) {
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
                from.readType(i);
                //guard survivors RecordBuffer from overflow
                if (to.currentIndex >= to.bufferSize) {
                    Log.print("Survivor Buffer out of bounds! Increase the Buffer Size.");
                    MaxineVM.exit(1);
                }
                // write it to Buffer
                to.record(from.readId(i), from.readThreadId(i), from.readStringBuffer, from.readSize(i), newAddr, node);
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
     * A procedure for resetting PROFILING_TLA of a thread.
     */
    private static final Pointer.Procedure resetProfilingTLA = new Pointer.Procedure() {
        public void run(Pointer tla) {
            PROFILER_TLA.store3(tla, Address.fromInt(0));
        }
    };

    /**
     * A procedure for resetting PROFILING_TLA of a thread.
     */
    private static final Pointer.Procedure setProfilingTLA = new Pointer.Procedure() {
        public void run(Pointer tla) {
            PROFILER_TLA.store3(tla, Address.fromInt(1));
        }
    };

    // FIXME: if a single thread had profiling enabled all threads will end up with profiling enabled, ideally we
    //  would like a map to be able to restore profiling only on threads where it was actually enabled.
    private final Pointer.Procedure readProfilingTLA = new Pointer.Procedure() {
        @Override
        public void run(Pointer tla) {
            if (PROFILER_TLA.load(tla).toInt() == 1) {
                wasProfiling = 1;
            }
        }
    };

    private final Pointer.Predicate profilingPredicate = new Pointer.Predicate() {
        @Override
        public boolean evaluate(Pointer tla) {
            VmThread vmThread = VmThread.fromTLA(tla);
            return vmThread.javaThread() != null &&
                    !vmThread.isVmOperationThread();
        }
    };

    /**
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     */
    public void preGCActions() {

        // Disable profiling
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, readProfilingTLA);
        if (wasProfiling == 1) {
            Log.println("(NUMA Profiler): Disabling profiling for GC. [pre-GC phase]");
            VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, resetProfilingTLA);
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Entering Pre-GC Phase.");
            Log.print("(NUMA Profiler): Cycle ");
            Log.print(getProfilingCycle());
            Log.println(" Profiling Is Now Complete. [pre-GC phase]");
        }

        // guard libnuma sys call usage during non-profiling cycles
        if (newObjects.currentIndex > 0) {
            findNumaNodeForPages();
            findObjectNumaNode();
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump NUMAProfiler Buffer. [pre-GC phase]");
        }

        if (!NUMAProfilerDebug) {
            dumpHeapBoundaries();
            dumpBuffer();
        } else {
            //in validation mode don't dump buffer
            Log.print("Cycle ");
            Log.println(profilingCycle);

            Log.print("=> (NUMAProfiler Reports): New Objects Size =");
            Log.print((float) totalNewSize / (1024 * 1024));
            Log.println(" MB");

            Log.print("=> (VM Reports): Heap Used Space =");
            Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
            Log.println(" MB");

            newObjects.printUsage();

            Log.println("Garbage Collection");
        }

        if (NUMAProfilerVerbose) {
            printObjectAccessStats();
        }

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Pre-GC Phase.");
        }
    }

    /**
     *  This method is called every time a GC has been completed.
     */
    public void postGCActions() {

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
            Log.println("(NUMA Profiler): Reset HeapBoundaries Buffer. [post-gc phase]");
        }
        resetHeapBoundaries();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump Survivors Buffer. [post-GC phase]");
        }

        if (!NUMAProfilerDebug) {
            dumpSurvivors();
        } else {
            //in validation mode don't dump buffer
            Log.print("=> (NUMAProfiler Reports): Survivor Objects Size =");
            Log.print((float) totalSurvSize / (1024 * 1024));
            Log.println(" MB");

            Log.print("=> (VM Reports): Heap Used Space =");
            Log.print((float) Heap.reportUsedSpace() / (1024 * 1024));
            Log.println(" MB\n");

            survivors1.printUsage();
            survivors2.printUsage();
        }

        totalNewSize = totalSurvSize;
        totalSurvSize = 0;

        /**
         * Check if the current GC is explicit. If yes, increase the iteration counter.
         */
        if (isExplicitGC) {
            iteration++;
            isExplicitGC = false;
            if (iteration > NUMAProfiler.NUMAProfilerExplicitGCThreshold) {
                if (NUMAProfilerVerbose) {
                    Log.println("(NUMA Profiler): Enabling profiling. [post-GC phase]");
                }
                wasProfiling = 1; // don't set the TLA here to avoid profiling inside the method
            }
        }

        profilingCycle++;

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Post-GC Phase.");
            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }

        // Re-enable profiling if needed
        if (wasProfiling == 1) {
            if (NUMAProfilerVerbose) {
                Log.println("(NUMA Profiler): Re-enabling profiling. [post-GC phase]");
            }
            VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, setProfilingTLA);
        }
    }

    public void releaseReservedMemory() {
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
        // Disable profiling
        VmThreadMap.ACTIVE.forAllThreadLocals(profilingPredicate, resetProfilingTLA);

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Termination");
        }

        //heapPages.print(profilingCycle);
        //heapPages.printStats(profilingCycle);

        // guard libnuma sys call usage during non-profiling cycles
        if (newObjects.currentIndex > 0) {
            findNumaNodeForPages();
            findObjectNumaNode();
        }

        if (!NUMAProfilerDebug) {
            dumpHeapBoundaries();
            dumpBuffer();
        } else {
            //in validation mode don't dump buffer
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

        if (NUMAProfilerVerbose) {
            printObjectAccessStats();
            Log.println("(NUMA Profiler): Release Reserved Memory.");
        }
        releaseReservedMemory();

        if (NUMAProfilerVerbose) {
            Log.println("(NUMA Profiler): Terminating... Bye!");
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
