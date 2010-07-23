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
package com.sun.max.vm.management;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.management.*;
import java.lang.reflect.Constructor;
import java.util.*;

import com.sun.cri.bytecode.Bytecodes.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * This class provides the entry point to all the thread management functions in Maxine.
 * TODO complete
 *
 * @author Mick Jordan
 */

public class ThreadManagement {
    /**
     * Support for creating {@link ThreadInfo} objects by reflection.
     */
    private static Constructor<?> threadInfoConstructor;

    public static Thread[] getThreads() {
        return VmThreadMap.getThreads();
    }

    public static int getDaemonThreadCount() {
        return VmThreadMap.getDaemonThreadCount();
    }

    public static int getPeakThreadCount() {
        return VmThreadMap.getPeakThreadCount();
    }

    public static void resetPeakThreadCount() {
        VmThreadMap.resetPeakThreadCount();
    }

    public static int getTotalStartedThreadCount() {
        return VmThreadMap.getTotalStartedThreadCount();
    }

    public static int getTotalThreadCount() {
        return VmThreadMap.getLiveTheadCount();
    }

    public static boolean setThreadCpuTimeEnabled(boolean enable) {
        // TODO
        return false;
    }

    public static boolean setThreadContentionMonitoringEnabled(boolean enable) {
        // TODO
        return false;
    }

    public static void getThreadInfo(long[] ids, int maxDepth, ThreadInfo[] result) {
        // The ids are java.lang.Thread ids from getId()
        // maxDepth is -1 when the entire stack is requested, not MAX_VALUE as in API call (see sun.management.ThreadImpl)
        // we revert that rather odd decision here
        checkThreadInfoConstructor();
        if (maxDepth < 0) {
            maxDepth = Integer.MAX_VALUE;
        }
        for (int i = 0; i < ids.length; i++) {
            final Thread thread = findThread(ids[i]);
            if (thread == null || thread.getState() == Thread.State.TERMINATED) {
                result[i] = null;
            } else {
                // we don't handle any of the lock information yet
                try {
                    final Object obj = threadInfoConstructor.newInstance(new Object[] {
                        thread, thread.getState().ordinal(), null, null,
                        0, 0,
                        0, 0,
                        getStackTrace(thread, maxDepth),
                        null,
                        null,
                        null
                    });
                    result[i] = (ThreadInfo) obj;
                } catch (Throwable t) {
                    FatalError.unexpected("failed to create ThreadInfo", t);
                }
            }
        }
    }

    /**
     * Support method for {@link Thread.getAllStackTraces}.
     * @param threads
     * @return
     */
    public static StackTraceElement[][] dumpThreads(Thread[] threads) {
        StackTraceElement[][] result = new StackTraceElement[threads.length][];
        for (int t = 0; t < threads.length; t++) {
            result[t] = getStackTrace(threads[t], Integer.MAX_VALUE);
        }
        return result;
    }

    /**
     * Return info on all or a subset of threads, with optional info on locking.
     * @param ids thread ids or null if all threads
     * @param lockedMonitors
     * @param lockedSynchronizers
     * @return
     */
    public static ThreadInfo[] dumpThreads(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers) {
        if (ids == null) {
            final Thread[] threads = VmThreadMap.getThreads();
            ids = new long[threads.length];
            for (int i = 0; i < threads.length; i++) {
                Thread t = threads[i];
                ids[i] = t.getId();
            }
        }
        ThreadInfo[] threadInfoArray = new ThreadInfo[ids.length];
        getThreadInfo(ids, 0, threadInfoArray);
        return threadInfoArray;
    }

    public static Thread findThread(long id) {
        FindProcedure proc = new FindProcedure(id);
        synchronized (VmThreadMap.ACTIVE) {
            VmThreadMap.ACTIVE.forAllThreads(null, proc);
        }
        return proc.result;
    }

    static class FindProcedure implements Procedure<VmThread> {
        Thread result = null;
        private long id;
        FindProcedure(long id) {
            this.id = id;
        }
        public void run(VmThread vmThread) {
            final Thread t = vmThread.javaThread();
            if (t.getId() == id) {
                result = t;
            }
        }
    }

    private static StackTraceElement[] getStackTrace(Thread thread, int maxDepth) {
        StackTraceElement[] result = null;
        if (maxDepth == 0) {
            result = new StackTraceElement[0];
        } else if (Thread.currentThread() == thread) {
            // special case
            result = new Exception().getStackTrace();
            if (maxDepth < result.length) {
                final StackTraceElement[] subResult = new StackTraceElement[maxDepth];
                System.arraycopy(result, 0, subResult, 0, maxDepth);
                result = subResult;
            }
        } else {
            /*
             * May need to stop the thread with a safepoint. To deal properly with threads transitioning to/from native
             * code, we treat this like a GC.
             * TODO Refactor this into StopTheWorld(GC)Daemon
             * TODO What if GC already underway or allocation here triggers a GC?
             */
            final Pointer theThreadLocals = VmThread.fromJava(thread).vmThreadLocals();
            Pointer.Predicate matchOne = new Pointer.Predicate() {

                public boolean evaluate(Pointer threadLocals) {
                    return threadLocals == theThreadLocals;
                }
            };
            final TrapStateAccess.MethodState methodState = new TrapStateAccess.MethodState();
            final StackTraceSafepointProcedure afterSafepoint = new StackTraceSafepointProcedure(methodState);
            Pointer.Procedure stopThread = new Pointer.Procedure() {

                public void run(Pointer threadLocals) {
                    GC_STATE.setVariableWord(threadLocals, Address.fromInt(1));
                    Safepoint.runProcedure(threadLocals, afterSafepoint);
                }
            };

            Pointer.Procedure waitUntilStopped = new Pointer.Procedure() {

                public void run(Pointer threadLocals) {
                    while (MUTATOR_STATE.getVariableWord(threadLocals).equals(Safepoint.THREAD_IN_JAVA)) {
                        // Wait for thread to be in native code, either as a result of a safepoint or because
                        // that's where it was when its GC_STATE flag was set to true.
                        Thread.yield();
                    }
                }
            };

            synchronized (VmThreadMap.ACTIVE) {
                VmThreadMap.ACTIVE.forAllThreadLocals(matchOne, stopThread);
                MemoryBarriers.storeLoad();
                // wait for target to reach safepoint
                VmThreadMap.ACTIVE.forAllThreadLocals(matchOne, waitUntilStopped);
                // We detect that the thread was stopped in native code by the methodState being null since the safepoint procedure will not have run
                if (methodState.instructionPointer.isZero()) {
                    Pointer frameAnchor = JavaFrameAnchor.from(theThreadLocals);
                    assert !frameAnchor.isZero();
                    methodState.instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
                    methodState.stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
                    methodState.framePointer = JavaFrameAnchor.FP.get(frameAnchor);
                }
                final List<StackFrame> frameList = new ArrayList<StackFrame>();
                new VmStackFrameWalker(theThreadLocals).frames(frameList, methodState.instructionPointer, methodState.stackPointer, methodState.framePointer);
                VmThreadMap.ACTIVE.forAllThreadLocals(matchOne, resetMutator);

                int depth = maxDepth < frameList.size() ? maxDepth : frameList.size();
                final List<StackTraceElement> listResult = new ArrayList<StackTraceElement>(depth);
                // TODO this code is partially copied from JDK_java_lang_Throwable.fillInStackTrace and probably should
                // be relocated to there and shared
                for (StackFrame stackFrame : frameList) {
                    if (stackFrame instanceof AdapterStackFrame) {
                        continue;
                    }
                    final TargetMethod targetMethod = stackFrame.targetMethod();
                    if (targetMethod == null || targetMethod.classMethodActor() == null) {
                        // native frame or stub frame without a class method actor
                        continue;
                    }
                    JDK_java_lang_Throwable.addStackTraceElements(listResult, targetMethod, stackFrame, false);
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
                result = new StackTraceElement[listResult.size()];
                listResult.toArray(result);
            }

        }
        return result;
    }

    private static class StackTraceSafepointProcedure implements Safepoint.Procedure {
        private TrapStateAccess.MethodState methodState;

        StackTraceSafepointProcedure(TrapStateAccess.MethodState methodState) {
            this.methodState = methodState;
        }

        public void run(Pointer trapState) {
            TrapStateAccess.getMethodState(trapState, methodState);
            synchronized (VmThreadMap.ACTIVE) {
                // block until initiator has got the stack trace
            }
        }
    }

    public static final class ResetMutator extends Safepoint.ResetSafepoints {
        @Override
        public void run(Pointer vmThreadLocals) {
            super.run(vmThreadLocals);
            GC_STATE.setVariableWord(vmThreadLocals, Address.zero());
        }
    }

    private static final ResetMutator resetMutator = new ResetMutator();

    public static void checkThreadInfoConstructor() {
        if (threadInfoConstructor == null) {
            try {
                threadInfoConstructor = Class.forName("java.lang.management.ThreadInfo").getDeclaredConstructor(
                    Thread.class, int.class, Object.class, Thread.class,
                    long.class, long.class,
                    long.class, long.class,
                    StackTraceElement[].class,
                    Object[].class,
                    int[].class,
                    Object[].class);
                threadInfoConstructor.setAccessible(true);
            } catch (ClassNotFoundException ex) {
                FatalError.unexpected("failed to load ThreadInfo class", ex);
            } catch (NoSuchMethodException ex) {
                FatalError.unexpected("failed to find ThreadInfo constructor", ex);
            }
        }
    }


}
