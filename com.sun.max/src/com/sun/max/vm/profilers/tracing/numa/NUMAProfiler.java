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

import com.sun.max.annotate.C_FUNCTION;
import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.program.ProgramError;
import com.sun.max.unsafe.Address;
import com.sun.max.util.NUMALib;
import com.sun.max.vm.Intrinsics;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.runtime.SafepointPoll;
import com.sun.max.vm.thread.VmThread;

import static com.sun.max.vm.MaxineVM.isHosted;
import static com.sun.max.vm.MaxineVM.vm;

public class NUMAProfiler {

    @C_FUNCTION
    static native void allocationProfiler_lock();

    @C_FUNCTION
    static native void allocationProfiler_unlock();

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

    private static boolean AllocationProfilerAll;
    public static boolean AllocationProfilerVerbose;
    public static int AllocationProfilerBufferSize;
    public static boolean AllocationProfilerDebug;

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
     * method returns false as long as the iteration counter is below the AllocationProfilerExplicitGCThreshold, which
     * is given by the user, ignoring any object allocation up to that point.
     */
    public static int AllocationProfilerExplicitGCThreshold;
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
     * method returns false as long as the AllocationProfilerFlareObject counter is below the AllocationProfilerFlareAllocationThreshold,
     * which is given by the user, ignoring any object allocation up to that point.
     * The AllocationProfilerFlareProfileWindow (default 1) indicates how many Flare Objects we need
     * to allocate before we stop the profiling.
     */
    public static int AllocationProfilerFlareAllocationThreshold;
    public static int AllocationProfilerFlareProfileWindow = 1;
    public static String AllocationProfilerFlareObject = "AllocationProfilerFlareObject";

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

    /**
     * The options a user can pass to the Allocation Profiler.
     */
    static {
        VMOptions.addFieldOption("-XX:", "AllocationProfilerAll", NUMAProfiler.class, "Profile all allocated objects. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerVerbose", NUMAProfiler.class, "Verbose numa profiler output. (default: false)", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AllocationProfilerBufferSize", NUMAProfiler.class, "Allocation Profiler's Buffer Size.");
        VMOptions.addFieldOption("-XX:", "AllocationProfilerExplicitGCThreshold", NUMAProfiler.class, "The number of the Explicit GCs to be performed before the Allocation Profiler starts recording. (default: 0)");
        VMOptions.addFieldOption("-XX:", "AllocationProfilerFlareObject", NUMAProfiler.class, "The Class of the Object to be sought after by the Allocation Profiler to drive the profiling process. (default: 'AllocationProfilerFlareObject')");
        VMOptions.addFieldOption("-XX:", "AllocationProfilerFlareAllocationThreshold", NUMAProfiler.class, "The number of the Flare objects to be allocated before the Allocation Profiler starts recording. (default: 0)");
        VMOptions.addFieldOption("-XX:", "AllocationProfilerFlareProfileWindow", NUMAProfiler.class, "The number of the Flare objects to be allocated before the Allocation Profiler stops recording. (default: 1)");
        VMOptions.addFieldOption("-XX:", "AllocationProfilerDebug", NUMAProfiler.class, "Print information to help in Allocation Profiler's Validation. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public NUMAProfiler() {
        assert NUMALib.numalib_available() != -1 : "NUMAProfiler cannot be run without NUMA support";

        if (AllocationProfilerVerbose) {
            Log.println("(Allocation Profiler): NUMAProfiler Initialization.");
        }

        if (AllocationProfilerBufferSize != 0) {
            if (AllocationProfilerBufferSize < MINIMUMBUFFERSIZE) {
                Log.print("WARNING: Small Buffer Size. Minimum Buffer Size applied! (=");
                Log.print(MINIMUMBUFFERSIZE);
                Log.println(")");
                allocBufferSize = MINIMUMBUFFERSIZE;
                survBufferSize = MINIMUMBUFFERSIZE;
            } else {
                allocBufferSize = AllocationProfilerBufferSize;
                survBufferSize = AllocationProfilerBufferSize;
            }
        }

        newObjects = new RecordBuffer(allocBufferSize, "New Objects Buffer");

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Survivor Objects NUMAProfiler Buffers.");
        }
        survivors1 = new RecordBuffer(survBufferSize, "Survivors Buffer No1");
        survivors2 = new RecordBuffer(survBufferSize, "Survivors Buffer No2");

        charArrayBuffer = new char[RecordBuffer.MAX_CHARS];

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialize the Heap Boundaries Buffer.");
        }
        initializeHeapBoundariesBuffer();

        numaConfig = new NUMALib();

        profilingCycle = 1;
        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Initialization Complete.");

            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
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
        return AllocationProfilerAll;
    }

    public static boolean warmupFinished() {
        return iteration > AllocationProfilerExplicitGCThreshold || AllocationProfilerExplicitGCThreshold == 0;
    }

    public static boolean objectWarmupFinished() {
        return MaxineVM.flareObjectCounter >= AllocationProfilerFlareAllocationThreshold && MaxineVM.flareObjectCounter <= AllocationProfilerFlareAllocationThreshold + (AllocationProfilerFlareProfileWindow - 1);
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
        final int coreId = Intrinsics.getCpuID() % 128;
        return numaConfig.getNUMANodeOfCPU(coreId);
    }

    public int getObjectNumaNode(long firstPageAddress, long address) {
        int pageSize = NUMALib.numaPageSize();
        long numerator = address - firstPageAddress;
        long div = numerator / (long) pageSize;
        int pageIndex = (int) div;

        int objNumaNode = heapPages.readNumaNode(pageIndex);
        // if no node found use jnumatils to find it and update heapPages accordingly
        if (objNumaNode == -14) {
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
    }

    /**
     * Dump NUMAProfiler Buffer to Maxine's Log output.
     */
    public void dumpBuffer() {
        final boolean lockDisabledSafepoints = lock();
        if (AllocationProfilerVerbose) {
            Log.print("==== Profiling Cycle ");
            Log.print(profilingCycle);
            Log.println(" ====");
        }
        //newObjects.print(profilingCycle, 1);
        unlock(lockDisabledSafepoints);
    }

    public void dumpSurvivors() {
        final boolean lockDisabledSafepoints = lock();
        if (AllocationProfilerVerbose) {
            Log.print("==== Survivors Cycle ");
            Log.print(profilingCycle);
            Log.println(" ====");
        }
        if ((profilingCycle % 2) == 0) {
            //survivors2.print(profilingCycle, 0);
        } else {
            //survivors1.print(profilingCycle, 0);
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
        if (AllocationProfilerVerbose) {
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
     * This method is called by ProfilerGCCallbacks in every pre-gc callback phase.
     */
    public void preGCActions() {

        /**
         * Pause Profiling during GC
         */
        if (MaxineVM.inProfilingSession) {
            MaxineVM.inProfilingSession = false;
            MaxineVM.isProfilingPaused = true;
        }

        if (AllocationProfilerVerbose) {
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

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump NUMAProfiler Buffer. [pre-GC phase]");
        }

        if (!AllocationProfilerDebug) {
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

        if (AllocationProfilerVerbose) {
            printObjectAccessStats();
        }

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Pre-GC Phase.");
        }
    }

    /**
     *  This method is called every time a GC has been completed.
     */
    public void postGCActions() {

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Entering Post-GC Phase.");
        }

        profileSurvivors();

        if ((profilingCycle % 2) == 0) {
            if (AllocationProfilerVerbose) {
                Log.println("(NUMA Profiler): Clean-up Survivor1 Buffer. [post-gc phase]");
            }
            survivors1.resetBuffer();
        } else {
            if (AllocationProfilerVerbose) {
                Log.println("(NUMA Profiler): Clean-up Survivor2 Buffer. [post-gc phase]");
            }
            survivors2.resetBuffer();
        }

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Clean-up NUMAProfiler Buffer. [post-gc phase]");
        }
        newObjects.resetBuffer();

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Reset HeapBoundaries Buffer. [post-gc phase]");
        }
        resetHeapBoundaries();

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Dump Survivors Buffer. [pre-GC phase]");
        }

        if (!AllocationProfilerDebug) {
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
        }

        profilingCycle++;

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Leaving Post-GC Phase.");
            Log.print("(NUMA Profiler): Start Profiling. [Cycle ");
            Log.print(getProfilingCycle());
            Log.println("]");
        }

        /**
         *  Re-enable Profiling if it's paused
         */
        if (MaxineVM.isProfilingPaused) {
            MaxineVM.inProfilingSession = true;
            MaxineVM.isProfilingPaused = false;
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
     * Allocation Profiler's termination. It is triggered when JavaRunScheme
     * is being terminated. Dumps the final profiling cycle which is not
     * followed by any GC.
     */
    public void terminate() {

        //end profiling
        MaxineVM.inProfilingSession = false;

        if (AllocationProfilerVerbose) {
            Log.println("(NUMA Profiler): Termination");
        }

        //heapPages.print(profilingCycle);
        //heapPages.printStats(profilingCycle);

        // guard libnuma sys call usage during non-profiling cycles
        if (newObjects.currentIndex > 0) {
            findNumaNodeForPages();
            findObjectNumaNode();
        }

        if (!AllocationProfilerDebug) {
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

        if (AllocationProfilerVerbose) {
            printObjectAccessStats();
            Log.println("(NUMA Profiler): Release Reserved Memory.");
        }
        releaseReservedMemory();

        if (AllocationProfilerVerbose) {
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
        NUMAProfiler.allocationProfiler_lock();
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
        NUMAProfiler.allocationProfiler_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by NUMAProfiler.lock() and NUMAProfiler.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }
}
