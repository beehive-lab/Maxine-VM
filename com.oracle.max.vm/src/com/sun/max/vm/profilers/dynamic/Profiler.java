/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.heap.Heap;
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
     * Histogram: the data structure that stores the profiling outcome.
     * The bins/buckets (the keys of the HashMap) contain Long type object sizes.
     * The values of the HashMap contain the sum of the equal-sized objects have been profiled so far.
     */
    //public static int SIZE = 52428800; //50MB
    public static int SIZE = 10000;
    public static int SIZE2 = 2;
    public static int[][] histogram = new int[SIZE][SIZE2];
    public static int totalObjectsize, totalRecordedObjects;
    public static int lastEntry;
    private static boolean PrintHistogram;

    static{
        VMOptions.addFieldOption("-XX:", "PrintHistogram", Profiler.class, "Print Dynamic Profiler's Histogram after every GC. (default: false)", MaxineVM.Phase.PRISTINE);
    }

    public Profiler(){
        for(int i=0; i<SIZE; i++){
            histogram[i][0] = 0;
            histogram[i][1] = 0;
        }
        lastEntry = 0;
        totalObjectsize = 0;
        totalRecordedObjects = 0;
    }

    /**
     * Returns the found index if is existed, or -1 if is not.
     * @param size
     * @return found index
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public int searchFor(int size){
        for(int i=0; i<lastEntry; i++){
            if (histogram[i][0] == size)
                return i;
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public void recordNewEntry(int size, int index){
        histogram[index][0] = size;
        histogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public void increment(int index){
        histogram[index][1]++;
    }

    /**
     * Updates the histogram with the size of the profiled object.
     * If that size has never been met again, a new bin/bucket is inserted.
     * Else, the value of the corresponding bin/bucket is incremented.
     *
     * @param size
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    synchronized void record(int size) {
        int entry = searchFor(size);

        if (entry == -1){
            recordNewEntry(size,lastEntry);
            lastEntry++;
        }
        else{
            increment(entry);
        }
        totalRecordedObjects++;
        totalObjectsize = totalObjectsize + size;
    }

    /**
     * This method is called when a profiled object is allocated.
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void profile(int size) {
        //if (VmThread.current().PROFILE) {
            //if (MaxineVM.isRunning()) {
                final boolean lockDisabledSafepoints = lock();
                record(size);
                unlock(lockDisabledSafepoints);
            //}
        //}
    }

    // bubble sort
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void sortHistogram() {
        int n = lastEntry+1;
        int k;
        for (int m = n; m >= 0; m--) {
            for (int i = 0; i < n - 1; i++) {
                k = i + 1;
                if (histogram[i][0] > histogram[k][0]) {
                    swapEntries(i, k);
                }
            }
        }
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void swapEntries(int i, int j) {
        int temp0,temp1;
        temp0 = histogram[i][0];
        temp1 = histogram[i][1];
        histogram[i][0] = histogram[j][0];
        histogram[i][1] = histogram[j][1];
        histogram[j][0] = temp0;
        histogram[j][1] = temp1;
    }

    /**
     * Sort and print Histogram.
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public void printHistogram(){
        sortHistogram();

        Log.println("====HISTOGRAM====");
        for(int i=1; i<lastEntry; i++) {
            //if (histogram[i][0] > 0) {
            Log.print("[");
            Log.print(histogram[i][0]);
            Log.print("]\t\t");
            Log.println(histogram[i][1]);
            //}
        }
        Log.print("Total histogram objects =");
        Log.println(totalRecordedObjects);
        Log.println("=======END=======");

    }

    /**
     * Dump Profiler findings to Maxine's Log output.
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void dumpHistogram() {
        final boolean lockDisabledSafepoints = lock();
        Log.print("Histogram total object size = ");
        Log.print(totalObjectsize);
        Log.print("\n");
        Log.print("Histogram total object size = ");
        Log.print(totalObjectsize/1048576);
        Log.print(" MB");
        Log.print("\n");

        if (PrintHistogram)
            printHistogram();
        unlock(lockDisabledSafepoints);
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void reportHeap(){
        final boolean lockDisabledSafepoints = lock();
        Log.print("Reported heap free space = ");
        Log.print(Heap.reportFreeSpace()/1048576);
        Log.println(" MB");

        Log.print("Reported heap used space = ");
        Log.println(Heap.reportUsedSpace());

        Log.print("Reported heap used space = ");
        Log.print(Heap.reportUsedSpace()/1048576);
        Log.println(" MB");
        unlock(lockDisabledSafepoints);
    }

    /**
     * Reset the histogram.
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public void resetHistogram() {
        final boolean lockDisabledSafepoints = lock();
        Log.print("Collected heap used space = ");
        Log.println(Heap.reportUsedSpace());
        Log.print("Collected heap used space = ");
        long survived = Heap.reportUsedSpace();
        Log.print(survived/1048576);
        Log.println(" MB");
        Log.println(" ");
        for(int i=0; i<lastEntry; i++){
            histogram[i][0] = 0;
            histogram[i][1] = 0;
        }
        lastEntry = 0;
        totalObjectsize = (int) survived;
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
     * Attempts to acquire the global lock on all debug output, blocking until the lock is successfully acquired. This
     * lock can be acquired recursively by a thread. The lock is not released for other threads until the thread that
     * owns the lock calls {@link #unlock} the same number of times it called this method.
     *
     * This method ensures that safepoints are disabled before it returns.
     *
     * @return true if this call caused safepoints to be disabled (i.e. they were enabled upon entry to this method).
     *         This value must be passed to the paired call to {@link #unlock} so that safepoints are restored to the
     *         appropriate state (i.e. the state they had before the sequence of code protected by this lock was
     *         entered).
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public static boolean lock() {
        if (isHosted()) {
            return true;
        }

        boolean wasDisabled = SafepointPoll.disable();
        Profiler.dynamicProfiler_lock();
        if (lockDepth == 0) {
            FatalError.check(lockOwner == null, "log lock should have no owner with depth 0");
            lockOwner = VmThread.current();
        }
        lockDepth++;
        return !wasDisabled;
    }

    /**
     * Attempts to releases the global lock on all debug output. This must only be called by a thread that currently
     * owns the lock - failure to do so causes the VM to exit. The lock is not released for other threads until this
     * method is called the same number of times as {@link #lock()} was called when acquiring the lock.
     *
     * @param lockDisabledSafepoints specifies if the adjoining call to {@link #lock()} disabled safepoints. If so, then
     *            this call will re-enable them.
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    public static void unlock(boolean lockDisabledSafepoints) {
        if (isHosted()) {
            return;
        }

        --lockDepth;
        FatalError.check(lockDepth >= 0, "mismatched lock/unlock");
        FatalError.check(lockOwner == VmThread.current(), "log lock should be owned by current thread");
        if (lockDepth == 0) {
            lockOwner = null;
        }
        Profiler.dynamicProfiler_unlock();
        ProgramError.check(SafepointPoll.isDisabled(), "Safepoints must not be re-enabled in code surrounded by Log.lock() and Log.unlock()");
        if (lockDisabledSafepoints) {
            SafepointPoll.enable();
        }
    }
}
