/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

import static com.sun.max.vm.heap.SpecialReferenceManager.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * The procedure that is run by the GC thread to perform a garbage collection.
 */
public abstract class GCOperation extends VmOperation {

    /**
     * Executes a single garbage collection.
     *
     * @param invocationCount the number of previous executions
     */
    protected abstract void collect(int invocationCount);

    @Override
    protected boolean disablesHeapAllocation() {
        return true;
    }

    /**
     * Stops the current mutator thread for a garbage collection. Just before stopping, the
     * thread prepares its own stack reference map up to the trap frame. The remainder of the
     * stack reference map is prepared by a GC thread once this thread has stopped by blocking
     * on the global GC and thread lock ({@link VmThreadMap#THREAD_LOCK}).
     */
    @Override
    protected void doAtSafepointBeforeBlocking(Pointer trapState) {
        // note that this procedure always runs with safepoints disabled
        final Pointer tla = Safepoint.getLatchRegister();
        final Pointer etla = ETLA.load(tla);
        Heap.disableAllocationForCurrentThread();

        if (!LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(etla).isZero()) {
            FatalError.unexpected("Stack reference map preparer should be cleared before GC");
        }

        VmThreadLocal.prepareStackReferenceMapFromTrap(tla, trapState);
    }

    @Override
    public void doAtSafepointAfterBlocking(Pointer trapState) {
        final Pointer tla = Safepoint.getLatchRegister();
        final Pointer etla = ETLA.load(tla);
        if (!LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(etla).isZero()) {
            FatalError.unexpected("Stack reference map preparer should be cleared after GC");
        }

        Heap.enableAllocationForCurrentThread();
    }

    @Override
    public void doAfterFrozen(VmThread vmThread) {

        Pointer tla = vmThread.tla();

        final boolean threadWasInNative = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(tla).isZero();
        if (threadWasInNative) {
            if (VmOperationThread.TraceVmOperations) {
                Log.print("Building full stack reference map for ");
                Log.printThread(VmThread.fromTLA(tla), true);
            }
            // Since this thread is in native code it did not get an opportunity to prepare any of its stack reference map,
            // so we will take care of that for it now:
            stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(tla);
        } else {
            // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
            // The part of the stack between the trap stub frame and the frame of the JNI stub that enters into the
            // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
            if (VmOperationThread.TraceVmOperations) {
                Log.print("Building partial stack reference map for ");
                Log.printThread(VmThread.fromTLA(tla), true);
            }
            final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
            stackReferenceMapPreparer.completeStackReferenceMap(tla);
            stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
        }
    }

    @Override
    protected void doBeforeThawingThread(VmThread thread) {
        // Indicates that the stack reference map for the thread is once-again unprepared.
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.store3(thread.tla(), Address.zero());
    }

    long stackReferenceMapPreparationTime;

    public GCOperation(String name) {
        super(name == null ? "GC" : name, null, Mode.Safepoint);
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean doItPrologue(boolean nested) {
        if (!nested) {
            // The lock for the special reference manager must be held before starting GC
            Monitor.enter(REFERENCE_LOCK);
        } else {
            // The VM operation thread must not attempt to lock REFERENCE_LOCK as doing
            // so may deadlock the system.
        }
        return true;
    }

    @Override
    protected void doItEpilogue(boolean nested) {
        if (Heap.traceGCTime()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Stack reference map preparation time: ");
            Log.print(stackReferenceMapPreparationTime);
            Log.println(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
            Log.unlock(lockDisabledSafepoints);
            stackReferenceMapPreparationTime = 0;
        }

        if (!nested) {
            // Notify the reference handler thread so it can process any pending references.
            REFERENCE_LOCK.notifyAll();

            Monitor.exit(REFERENCE_LOCK);
        } else {
            // The VM operation thread cannot notify the REFERENCE_LOCK as it doesn't hold.
            // This notification will occur during the next non-nested GC operation.
        }
    }

    @Override
    public void doIt() {
        // The next 2 statements *must* be adjacent as the reference map for this frame must
        // be the same at both calls.
        stackReferenceMapPreparationTime = VmThreadLocal.prepareCurrentStackReferenceMap();
        collect();
    }

    private int invocationCount;

    @NEVER_INLINE
    private void collect() {
        if (Heap.verbose()) {
            Log.print("--Start GC ");
            Log.print(invocationCount);
            Log.println("--");
        }
        collect(invocationCount);
        if (Heap.verbose()) {
            Log.print("--End GC ");
            Log.print(invocationCount);
            Log.println("--");
        }
        invocationCount++;
        if (Heap.TraceGCSuppressionCount > 0) {
            Heap.TraceGCSuppressionCount--;
        }
    }

}
