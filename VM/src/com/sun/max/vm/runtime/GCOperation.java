/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
 *
 * @author Doug Simon
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
        final Pointer vmThreadLocals = Safepoint.getLatchRegister();
        final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        Heap.disableAllocationForCurrentThread();

        if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
            FatalError.unexpected("Stack reference map preparer should be cleared before GC");
        }

        VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
    }

    @Override
    public void doAtSafepointAfterBlocking(Pointer trapState) {
        final Pointer vmThreadLocals = Safepoint.getLatchRegister();
        final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
            FatalError.unexpected("Stack reference map preparer should be cleared after GC");
        }

        Heap.enableAllocationForCurrentThread();
    }

    @Override
    public void doAfterFrozen(VmThread vmThread) {

        Pointer vmThreadLocals = vmThread.vmThreadLocals();

        final boolean threadWasInNative = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero();
        if (threadWasInNative) {
            if (VmOperationThread.TraceVmOperations) {
                Log.print("Building full stack reference map for ");
                Log.printThread(VmThread.fromVmThreadLocals(vmThreadLocals), true);
            }
            // Since this thread is in native code it did not get an opportunity to prepare any of its stack reference map,
            // so we will take care of that for it now:
            stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
        } else {
            // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
            // The part of the stack between the trap stub frame and the frame of the JNI stub that enters into the
            // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
            if (VmOperationThread.TraceVmOperations) {
                Log.print("Building partial stack reference map for ");
                Log.printThread(VmThread.fromVmThreadLocals(vmThreadLocals), true);
            }
            final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
            stackReferenceMapPreparer.completeStackReferenceMap(vmThreadLocals);
            stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
        }
    }

    @Override
    protected void doBeforeThawingThread(VmThread thread) {
        // Indicates that the stack reference map for the thread is once-again unprepared.
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(thread.vmThreadLocals(), Address.zero());
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
