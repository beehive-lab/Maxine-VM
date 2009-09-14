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
package com.sun.max.vm.heap.beltway;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.lang.*;
import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A daemon thread that hangs around, waiting, then executes a given procedure when requested, then waits again.
 *
 * All other VM threads are forced into a non-mutating state while a request is being serviced. This can be used to
 * implement stop-the-world GC.
 *
 * @author Christos Kotselidis
 */
public class BeltwayStopTheWorldDaemon extends BlockingServerDaemon {

    private static Safepoint.Procedure suspendProcedure = new Safepoint.Procedure() {
        public void run(Pointer trapState) {
            // note that this procedure always runs with safepoints disabled
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();

            if (VmThreadLocal.inJava(vmThreadLocals)) {
                VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
            } else {
                // GC may already be ongoing
            }

            synchronized (VmThreadMap.ACTIVE) {
                // this is ok even though the GC does not get to scan this frame, because the object involved is in the boot image
            }
        }
    };

    private Runnable procedure = null;

    public BeltwayStopTheWorldDaemon(String name) {
        super(name);
    }

    public BeltwayStopTheWorldDaemon(String name, Runnable procedure) {
        super(name);
        this.procedure = procedure;
    }

    private final Pointer.Procedure triggerSafepoint = new Pointer.Procedure() {
        public void run(Pointer vmThreadLocals) {
            if (vmThreadLocals.isZero()) {
                // Thread is still starting up.
                // Do not need to do anything, because it will try to lock 'VmThreadMap.ACTIVE' and thus block.
            } else {
                Safepoint.runProcedure(vmThreadLocals, suspendProcedure);
            }
        }
    };

    private final Pointer.Procedure resetSafepoint = new Safepoint.ResetSafepoints();

    private final Pointer.Procedure waitUntilNonMutating = new Pointer.Procedure() {

        public void run(Pointer vmThreadLocals) {
            while (VmThreadLocal.inJava(vmThreadLocals)) {
                try {
                    sleep(1);
                } catch (InterruptedException interruptedException) {
                }
            }
            if (LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero()) {
                // Since this thread is in native code it did not get an opportunity to prepare its stack maps,
                // so we will take care of that for it now:
                VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
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
                final VmThread vmThread = UnsafeCast.asVmThread(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals));
                vmThread.stackReferenceMapPreparer().completeStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
            }
        }
    };

    private final Pointer.Procedure fillLastTlabs = new Pointer.Procedure() {

        public void run(Pointer localSpace) {
            /* FIXME:
            if (!localSpace.isZero()) {
                final VmThread thread = VmThread.fromVmThreadLocals(localSpace);
                if (thread != null) {
                    final BeltTLAB tlab = thread.getTLAB();
                    if (!tlab.isFull()) {
                        tlab.fillTLAB();
                    }
                }
            }*/
        }
    };


    private static class TLABScavengerReset implements Procedure<VmThread> {
        public void run(VmThread thread) {
            // thread.getTLAB().unSet();   FIXME
        }
    }

    private TLABScavengerReset tlabScavengerReset = new TLABScavengerReset();

    private static final Pointer.Procedure prepareGCThreadStackMap = new Pointer.Procedure() {
        public void run(Pointer vmThreadLocals) {
            VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
        }
    };

    private final Runnable gcRequest = new Runnable() {

        public void run() {
            synchronized (VmThreadMap.ACTIVE) {
                BeltwayHeapScheme.inGC = true;
                VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCThreadLocalsOrCurrent, triggerSafepoint);
                VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCThreadLocalsOrCurrent, waitUntilNonMutating);
                /*
                 * FIXME:
                if (BeltwayConfiguration.useTLABS) {
                    VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCThreadLocalsOrCurrent, fillLastTlabs);
                }

                if (BeltwayConfiguration.useGCTlabs) {
                    VmThreadMap.ACTIVE.forAllVmThreads(isGCOrStopTheWorldDaemonThread, tlabScavengerReset);
                }*/
                VmThreadMap.ACTIVE.forAllVmThreadLocals(isGCThread, prepareGCThreadStackMap);
                VmThreadLocal.prepareCurrentStackReferenceMap();
                procedure.run();
                BeltwayHeapScheme.inGC = false;
                VmThreadMap.ACTIVE.forAllVmThreadLocals(isNotGCThreadLocalsOrCurrent, resetSafepoint);
            }
        }


    };

    private static final Predicate<VmThread> isGCOrStopTheWorldDaemonThread = new Predicate<VmThread>() {
        public boolean evaluate(VmThread vmThread) {
            final Thread javaThread = vmThread.javaThread();
            return javaThread instanceof BeltwayStopTheWorldDaemon || javaThread instanceof BeltwayCollectorThread;
        }
    };

    private static final Pointer.Predicate isNotGCThreadLocalsOrCurrent = new Pointer.Predicate() {
        public boolean evaluate(Pointer vmThreadLocals) {
            if (vmThreadLocals != VmThread.current().vmThreadLocals()) {
                final Thread javaThread = VmThread.fromVmThreadLocals(vmThreadLocals).javaThread();
                return !(javaThread instanceof BeltwayStopTheWorldDaemon) && !(javaThread instanceof BeltwayCollectorThread);
            }
            return false;
        }
    };

    private static final Pointer.Predicate isGCThread = new Pointer.Predicate() {
        public boolean evaluate(Pointer vmThreadLocals) {
            final Thread javaThread = VmThread.fromVmThreadLocals(vmThreadLocals).javaThread();
            return javaThread instanceof BeltwayCollectorThread;
        }
    };

    public void execute() {
        execute(gcRequest);
    }

    public BeltTLAB getScavengeTLAB() {
        return currentTLAB;
    }

    private BeltTLAB currentTLAB;

}
