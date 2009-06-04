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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A daemon thread that hangs around, waiting, then executes a given procedure when requested, then waits again.
 *
 * All other VM threads are forced into a non-mutating state while a request is being serviced. This can be used to
 * implement stop-the-world GC.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public class StopTheWorldDaemon extends BlockingServerDaemon {

    private static Safepoint.Procedure _suspendProcedure = new Safepoint.Procedure() {
        @Override
        public void run(Pointer trapState) {
            // note that this procedure always runs with safepoints disabled
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();
            if (VmThreadLocal.inJava(vmThreadLocals)) {
                VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(vmThreadLocals, Reference.fromJava(Safepoint.Venue.JAVA));
                VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
            } else {
                // GC may already be ongoing
            }

            synchronized (VmThreadMap.ACTIVE) {
                // Stops this thread until GC is done.
            }
            VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(vmThreadLocals, Reference.fromJava(Safepoint.Venue.NATIVE));
        }
    };

    private Runnable _procedure = null;

    public StopTheWorldDaemon(String name, Runnable procedure) {
        super(name);
        _procedure = procedure;

    }

    @Override
    public void start() {
        // If the _waitUntilNonMutating Procedure tries to link nativeSleep in response to a safepoint request
        // from System.gc(), then we have a deadlock. (As the thread calling System.gc() gets a lock on the
        // Heap scheme, which prevents the GC thread from allocating the MethodActor for nativeSleep).
        // So we link it here.
        try {
            sleep(1);
        } catch (InterruptedException interruptedException) {
        }

        super.start();
    }

    private final Pointer.Procedure _triggerSafepoint = new Pointer.Procedure() {
        public void run(Pointer vmThreadLocals) {
            if (vmThreadLocals.isZero()) {
                // Thread is still starting up.
                // Do not need to do anything, because it will try to lock 'VmThreadMap.ACTIVE' and thus block.
            } else {
                Safepoint.runProcedure(vmThreadLocals, _suspendProcedure);
            }
        }
    };

    private final Pointer.Procedure _resetSafepoint = new Pointer.Procedure() {
        public void run(Pointer vmThreadLocals) {
            Safepoint.cancelProcedure(vmThreadLocals, _suspendProcedure);
            Safepoint.reset(vmThreadLocals);
        }
    };

    static class WaitUntilNonMutating implements Pointer.Procedure {
        long _stackReferenceMapPreparationTime;
        public void run(Pointer vmThreadLocals) {
            while (VmThreadLocal.inJava(vmThreadLocals)) {
                try {
                    // Wait for safepoint to fire
                    sleep(1);
                } catch (InterruptedException interruptedException) {
                }
            }
            if (VmThreadLocal.SAFEPOINT_VENUE.getVariableReference(vmThreadLocals).toJava() == Safepoint.Venue.NATIVE) {
                // Since this thread is in native code it did not get an opportunity to prepare its stack maps,
                // so we will take care of that for it now:
                _stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
            } else {
                // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
                // The part of the stack between suspendCurrentThread() and the JNI stub that enters into the
                // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
                final Pointer instructionPointer = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals).asPointer();
                if (instructionPointer.isZero()) {
                    FatalError.unexpected("A mutator thread in Java at safepoint should be stopped in native monitor code");
                }
                final Pointer stackPointer = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
                final Pointer framePointer = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals).asPointer();
                final VmThread vmThread = UnsafeLoophole.cast(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals));
                final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
                stackReferenceMapPreparer.completeStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
                _stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
            }
        }
    }

    private final WaitUntilNonMutating _waitUntilNonMutating = new WaitUntilNonMutating();

    private static final Pointer.Predicate _isNotGCOrCurrentThread = new Pointer.Predicate() {
        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals() && !VmThread.current(vmThreadLocals).isGCThread();
        }
    };

    class GCRequest implements Runnable {
        public void run() {
            synchronized (SpecialReferenceManager.LOCK) {
                // the lock for the special reference manager must be held before starting GC
                synchronized (VmThreadMap.ACTIVE) {
                    _waitUntilNonMutating._stackReferenceMapPreparationTime = 0;
                    VmThreadMap.ACTIVE.forAllVmThreadLocals(_isNotGCOrCurrentThread, _triggerSafepoint);
                    VmThreadMap.ACTIVE.forAllVmThreadLocals(_isNotGCOrCurrentThread, _waitUntilNonMutating);

                    // The next 2 statements *must* be adjacent as the reference map for this frame must
                    // be the same at both calls. This is verified by StopTheWorldDaemon.checkInvariants().
                    final long time = VmThreadLocal.prepareCurrentStackReferenceMap();
                    _procedure.run();

                    VmThreadMap.ACTIVE.forAllVmThreadLocals(_isNotGCOrCurrentThread, _resetSafepoint);
                    if (Heap.traceGCTime()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Stack reference map preparation time: ");
                        Log.print(time + _waitUntilNonMutating._stackReferenceMapPreparationTime);
                        Log.println(HeapScheme.GC_TIMING_CLOCK.getHZAsSuffix());
                        Log.unlock(lockDisabledSafepoints);
                    }
                }
            }
        }
    }

    /**
     * This must be called from {@link HeapScheme#finalize(com.sun.max.vm.MaxineVM.Phase)} of any {@link HeapScheme}
     * implementation that uses the {@link StopTheWorldDaemon}.
     */
    @PROTOTYPE_ONLY
    public static void checkInvariants() {
        final ClassMethodActor classMethodActor = ClassActor.fromJava(GCRequest.class).findLocalClassMethodActor(SymbolTable.makeSymbol("run"), SignatureDescriptor.VOID);
        final TargetMethod targetMethod = CompilationScheme.Static.getCurrentTargetMethod(classMethodActor);
        if (targetMethod != null) {
            final ClassMethodActor[] directCallees = targetMethod.directCallees();
            for (int stopIndex = 0; stopIndex < directCallees.length; ++stopIndex) {
                if (directCallees[stopIndex].name().string().equals("prepareCurrentStackReferenceMap")) {
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

    private final Runnable _gcRequest = new GCRequest();

    public void execute() {
        execute(_gcRequest);
    }
}
