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

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.unsafe.*;
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
        return VmThreadMap.getThreads(false);
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
                        maxDepth == 0 ? new StackTraceElement[0] : getStackTrace(thread, maxDepth),
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
     * Support method for {@link Thread#getAllStackTraces}.
     * @param threads
     * @return
     */
    public static StackTraceElement[][] dumpThreads(Thread[] threads) {
        return getStackTrace(threads, Integer.MAX_VALUE);
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
            final Thread[] threads = VmThreadMap.getThreads(false);
            ids = new long[threads.length];
            for (int i = 0; i < threads.length; i++) {
                Thread t = threads[i];
                ids[i] = t.getId();
            }
        }
        ThreadInfo[] threadInfoArray = new ThreadInfo[ids.length];
        getThreadInfo(ids, Integer.MAX_VALUE, threadInfoArray);
        return threadInfoArray;
    }

    public static Thread findThread(long id) {
        FindProcedure proc = new FindProcedure(id);
        synchronized (VmThreadMap.ACTIVE) {
            VmThreadMap.ACTIVE.forAllThreadLocals(null, proc);
        }
        return proc.result;
    }

    static class FindProcedure implements Pointer.Procedure {
        Thread result = null;
        private long id;
        FindProcedure(long id) {
            this.id = id;
        }
        public void run(Pointer vmThreadLocals) {
            final Thread t = VmThread.fromVmThreadLocals(vmThreadLocals).javaThread();
            if (t.getId() == id) {
                result = t;
            }
        }
    }

    private static StackTraceElement[] getStackTrace(Thread thread, int maxDepth) {
        assert maxDepth > 0;
        Thread[] threads = {thread};
        return getStackTrace(threads, maxDepth)[0];
    }

    private static StackTraceElement[][] getStackTrace(Thread[] threads, int maxDepth) {
        final StackTraceElement[][] traces = new StackTraceElement[threads.length][];
        int currentThreadIndex = -1;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == Thread.currentThread()) {
                // special case of current thread
                currentThreadIndex = i;
                // null it out so StackTraceGatherer will not try to stop it
                threads[i] = null;
                StackTraceElement[] trace = new Exception().getStackTrace();
                if (maxDepth < trace.length) {
                    trace = Arrays.copyOf(trace, maxDepth);
                }
                traces[i] = trace;
            }
        }
        new StackTraceGatherer(Arrays.asList(threads), traces, maxDepth).run();
        if (currentThreadIndex >= 0) {
            threads[currentThreadIndex] = Thread.currentThread();
        }
        return traces;
    }

    /**
     * A thread-freezing operation to get a stack trace for a given set of threads.
     *
     * @author Doug Simon
     */
    static final class StackTraceGatherer extends FreezeThreads {
        final int maxDepth;
        final StackTraceElement[][] traces;
        final List<Thread> threads;
        StackTraceGatherer(List<Thread> threads, StackTraceElement[][] result, int maxDepth) {
            super("StackTraceGatherer", new ThreadListPredicate(threads));
            this.threads = threads;
            this.maxDepth = maxDepth;
            this.traces = result;
        }

        @Override
        public void doThread(Pointer threadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
            VmThread vmThread = VmThread.fromVmThreadLocals(threadLocals);
            final List<StackFrame> frameList = new ArrayList<StackFrame>();
            new VmStackFrameWalker(threadLocals).frames(frameList, instructionPointer, stackPointer, framePointer);
            Thread thread = vmThread.javaThread();
            traces[threads.indexOf(thread)] = JDK_java_lang_Throwable.asStackTrace(frameList, null, maxDepth);
        }
    }


    public static Thread[] findMonitorDeadlockedThreads() {
        return null;
    }

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

