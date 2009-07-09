/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.heap.sequential;

import static com.sun.max.vm.runtime.Safepoint.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A daemon thread that hangs around, waiting, then executes a given GC procedure when requested, then waits again.
 *
 * All other VM threads are forced into a non-mutating state while a request is being serviced. This can be used to
 * implement stop-the-world GC.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public class StopTheWorldGCDaemon extends BlockingServerDaemon {

    private static final VMBooleanXXOption traceGCDaemon = VMOptions.register(new VMBooleanXXOption("-XX:-TraceGCDaemon", "Print GC daemon activity"), MaxineVM.Phase.STARTING);

    /**
     * The procedure that is run on a mutator thread that has been stopped by a safepoint for the
     * purpose of performing a stop-the-world garbage collection.
     */
    static final class Suspend implements Safepoint.Procedure {

        /**
         * Stops the current mutator thread for a garbage collection. Just before stopping, the
         * thread prepares its own stack reference map up to the trap frame. The remainder of the
         * stack reference map is prepared by a GC thread once this thread has stopped by blocking
         * on the global GC lock ({@link VmThreadMap#ACTIVE}).
         */
        public void run(Pointer trapState) {
            if (Safepoint.UseThreadStateWordForGCMutatorSynchronization) {
                final Pointer vmThreadLocals = Safepoint.getLatchRegister();

                MUTATOR_STATE.setVariableWord(vmThreadLocals, Address.fromInt(THREAD_IN_JAVA_STOPPING_FOR_GC));

                VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);

                synchronized (VmThreadMap.ACTIVE) {
                    // Stops this thread until GC is done.
                }
            } else {
                // note that this procedure always runs with safepoints disabled
                final Pointer vmThreadLocals = Safepoint.getLatchRegister();
                if (!VmThreadLocal.inJava(vmThreadLocals)) {
                    FatalError.unexpected("Mutator thread trapped while in native code");
                }
                if (!LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero()) {
                    FatalError.unexpected("Stack reference map preparer should be cleared before GC");
                }
                VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);

                synchronized (VmThreadMap.ACTIVE) {
                    // Stops this thread until GC is done.
                }
                if (!LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero()) {
                    FatalError.unexpected("Stack reference map preparer should be cleared after GC");
                }
            }
        }
        @Override
        public String toString() {
            return "SuspendMutatorForGC";
        }
    }

    private static Suspend suspendProcedure = new Suspend();

    /**
     * The procedure supplied by the {@link HeapScheme} that implements the GC algorithm.
     */
    private final Runnable procedure;

    public StopTheWorldGCDaemon(String name, Runnable procedure) {
        super(name);
        this.procedure = procedure;

    }

    @Override
    public void start() {
        // If the waitUntilNonMutating Procedure tries to link nativeSleep in response to a safepoint request
        // from System.gc(), then we have a deadlock. (As the thread calling System.gc() gets a lock on the
        // Heap scheme, which prevents the GC thread from allocating the MethodActor for nativeSleep).
        // So we link it here.
        try {
            sleep(1);
        } catch (InterruptedException interruptedException) {
        }

        super.start();
    }

    static final class IsNotGCOrCurrentThread implements Pointer.Predicate {

        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals() && !VmThread.current(vmThreadLocals).isGCThread();
        }
    }

    /**
     * The procedure that is run on the GC thread to trigger safepoints for a given thread.
     */
    final class TriggerSafepoints implements Pointer.Procedure {

        /**
         * Triggers safepoints for the thread associated with the given thread locals.
         */
        public void run(Pointer vmThreadLocals) {
            if (vmThreadLocals.isZero()) {
                // Thread is still starting up.
                // Do not need to do anything, because it will try to lock 'VmThreadMap.ACTIVE' and thus block.
            } else {
                GC_STATE.setVariableWord(vmThreadLocals, Address.fromInt(1));
                Safepoint.runProcedure(vmThreadLocals, suspendProcedure);
            }
        }
    }

    private final TriggerSafepoints triggerSafepoints = new TriggerSafepoints();

    private final class GCResetSafepoints extends Safepoint.ResetSafepoints {
        @Override
        public void run(Pointer vmThreadLocals) {
            GC_STATE.setVariableWord(vmThreadLocals, Address.zero());
            super.run(vmThreadLocals);
        }
    }

    /**
     * The procedure that is run on the GC thread to reset safepoints for a given thread.
     */
    private final GCResetSafepoints resetSafepoints = new GCResetSafepoints();

    static class WaitUntilNonMutating implements Pointer.Procedure {
        long stackReferenceMapPreparationTime;
        public void run(Pointer vmThreadLocals) {
            if (Safepoint.UseThreadStateWordForGCMutatorSynchronization) {
                final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
                int currentState;
                while (true) {
                    if (enabledVmThreadLocals.readInt(MUTATOR_STATE.offset) == THREAD_IN_GC_FROM_JAVA) {
                        // Transitioned thread into GC
                        currentState = THREAD_IN_GC_FROM_JAVA;
                        break;
                    }
                    if (Safepoint.casMutatorState(enabledVmThreadLocals, THREAD_IN_NATIVE, THREAD_IN_GC_FROM_NATIVE) == THREAD_IN_NATIVE) {
                        // Transitioned thread into GC
                        currentState = THREAD_IN_GC_FROM_NATIVE;
                        break;
                    }
                    try {
                        // Wait for safepoint to fire
                        sleep(1);
                    } catch (InterruptedException interruptedException) {
                    }
                }
                if (currentState == THREAD_IN_GC_FROM_NATIVE) {
                    // Since this thread is in native code it did not get an opportunity to prepare its stack maps,
                    // so we will take care of that for it now:
                    stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
                } else {
                    // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
                    // The part of the stack between the trap stub frame and the frame of the JNI stub that enters into the
                    // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
                    final Pointer instructionPointer = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    if (instructionPointer.isZero()) {
                        FatalError.unexpected("A mutator thread in Java at safepoint should be stopped in native monitor code");
                    }
                    final Pointer stackPointer = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    final Pointer framePointer = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    final VmThread vmThread = UnsafeLoophole.cast(VM_THREAD.getConstantReference(vmThreadLocals));
                    final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
                    stackReferenceMapPreparer.completeStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
                    stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
                }
            } else {
                while (MUTATOR_STATE.getVariableWord(vmThreadLocals).asAddress().toInt() == THREAD_IN_JAVA) {
                    try {
                        // Wait for safepoint to fire
                        sleep(1);
                    } catch (InterruptedException interruptedException) {
                    }
                }

                if (LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero()) {
                    // Since this thread is in native code it did not get an opportunity to prepare any of its stack reference map,
                    // so we will take care of that for it now:
                    stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
                } else {
                    // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
                    // The part of the stack between the trap stub frame and the frame of the JNI stub that enters into the
                    // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
                    final Pointer instructionPointer = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    if (instructionPointer.isZero()) {
                        FatalError.unexpected("A mutator thread in Java at safepoint should be stopped in native monitor code");
                    }
                    final Pointer stackPointer = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    final Pointer framePointer = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals).asPointer();
                    final VmThread vmThread = UnsafeLoophole.cast(VM_THREAD.getConstantReference(vmThreadLocals));
                    final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
                    stackReferenceMapPreparer.completeStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
                    stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
                }
            }
        }
    }

    private final WaitUntilNonMutating waitUntilNonMutating = new WaitUntilNonMutating();

    private static final IsNotGCOrCurrentThread isNotGCOrCurrentThread = new IsNotGCOrCurrentThread();

    /**
     * The procedure that is run the on GC thread to perform a garbage collection.
     */
    class GCRequest implements Runnable {
        public void run() {
            synchronized (SpecialReferenceManager.LOCK) {
                // the lock for the special reference manager must be held before starting GC
                synchronized (VmThreadMap.ACTIVE) {
                    waitUntilNonMutating.stackReferenceMapPreparationTime = 0;

                    if (traceGCDaemon.getValue()) {
                        Log.println("GCDaemon: Triggering safepoints for all mutators");
                    }

                    VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCOrCurrentThread, triggerSafepoints);

                    // Ensures the GC_STATE variable is visible for each thread before the GC thread reads
                    // the MUTATOR_STATE variable for each thread.
                    MemoryBarrier.storeLoad();

                    if (traceGCDaemon.getValue()) {
                        Log.println("GCDaemon: Waiting for all mutators to stop");
                    }

                    VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCOrCurrentThread, waitUntilNonMutating);

                    if (traceGCDaemon.getValue()) {
                        Log.println("GCDaemon: Running GC algorithm");
                    }

                    // The next 2 statements *must* be adjacent as the reference map for this frame must
                    // be the same at both calls. This is verified by StopTheWorldDaemon.checkInvariants().
                    final long time = VmThreadLocal.prepareCurrentStackReferenceMap();
                    procedure.run();

                    if (traceGCDaemon.getValue()) {
                        Log.println("GCDaemon: Resetting mutators");
                    }

                    VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCOrCurrentThread, resetSafepoints);
                    if (Heap.traceGCTime()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Stack reference map preparation time: ");
                        Log.print(time + waitUntilNonMutating.stackReferenceMapPreparationTime);
                        Log.println(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                        Log.unlock(lockDisabledSafepoints);
                    }

                    if (traceGCDaemon.getValue()) {
                        Log.println("GCDaemon: Completed GC request");
                    }
                }
            }
        }
    }

    /**
     * This must be called from {@link HeapScheme#finalize(com.sun.max.vm.MaxineVM.Phase)} of any {@link HeapScheme}
     * implementation that uses the {@link StopTheWorldGCDaemon}.
     */
    @PROTOTYPE_ONLY
    public static void checkInvariants() {
        final ClassMethodActor classMethodActor = ClassActor.fromJava(GCRequest.class).findLocalClassMethodActor(SymbolTable.makeSymbol("run"), SignatureDescriptor.VOID);
        final TargetMethod targetMethod = CompilationScheme.Static.getCurrentTargetMethod(classMethodActor);
        if (targetMethod != null) {
            final ClassMethodActor[] directCallees = targetMethod.directCallees();
            for (int stopIndex = 0; stopIndex < directCallees.length; ++stopIndex) {
                if (directCallees[stopIndex].name.string.equals("prepareCurrentStackReferenceMap")) {
                    final int stopPosition = targetMethod.stopPosition(stopIndex);
                    final int nextCallPosition = targetMethod.findNextCall(stopPosition);
                    if (nextCallPosition >= 0) {
                        final int[] stopPositions = targetMethod.stopPositions();
                        for (int nextCallStopIndex = 0; nextCallStopIndex < stopPositions.length; ++nextCallStopIndex) {
                            if (stopPositions[nextCallStopIndex] == nextCallPosition) {
                                final ByteArrayBitMap nextCallRefmap = targetMethod.frameReferenceMapFor(nextCallStopIndex);
                                final ByteArrayBitMap firstCallRefmap = targetMethod.frameReferenceMapFor(stopIndex);
                                if (nextCallRefmap.equals(firstCallRefmap)) {
                                    // OK
                                    return;
                                }
                                throw ProgramError.unexpected(String.format(
                                    "Reference maps not equal in %s for calls to VmThreadLocal.prepareCurrentStackReferenceMap() and _procedure.run():%n" +
                                    "    frame refmap 1: %s%n" +
                                    "    frame refmap 2: %s",
                                    classMethodActor.format("%H.%n(%p)"), firstCallRefmap, nextCallRefmap));
                            }
                        }
                    }
                    throw ProgramError.unexpected("Cannot find stop in " + classMethodActor.format("%H.%n(%p)") + " for call to _procedure.run()");
                }
            }
            throw ProgramError.unexpected("Cannot find stop in " + classMethodActor.format("%H.%n(%p)") + " for call to VmThreadLocal.prepareCurrentStackReferenceMap()");
        }
        ProgramWarning.message("Could not find target method for " + classMethodActor);
    }

    private final GCRequest gcRequest = new GCRequest();

    public void execute() {
        execute(gcRequest);
    }
}
