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
        synchronized (VmThreadMap.THREAD_LOCK) {
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
        public void run(Pointer tla) {
            final Thread t = VmThread.fromTLA(tla).javaThread();
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
        VmOperationThread.submit(new StackTraceGatherer(Arrays.asList(threads), traces, maxDepth));
        if (currentThreadIndex >= 0) {
            threads[currentThreadIndex] = Thread.currentThread();
        }
        return traces;
    }

    /**
     * A thread-freezing operation to get a stack trace for a given set of threads.
     *
     */
    static final class StackTraceGatherer extends VmOperation {
        final int maxDepth;
        final StackTraceElement[][] traces;
        final List<Thread> threads;
        StackTraceGatherer(List<Thread> threads, StackTraceElement[][] result, int maxDepth) {
            super("StackTraceGatherer", null, Mode.Safepoint);
            this.threads = threads;
            this.maxDepth = maxDepth;
            this.traces = result;
        }

        @Override
        protected boolean operateOnThread(VmThread thread) {
            return threads.contains(thread.javaThread());
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            Thread thread = vmThread.javaThread();
            if (ip.isZero()) {
                traces[threads.indexOf(thread)] = new StackTraceElement[0];
            } else {
                VmStackFrameWalker sfw = new VmStackFrameWalker(vmThread.tla());
                traces[threads.indexOf(thread)] = JDK_java_lang_Throwable.getStackTrace(sfw, ip, sp, fp, null, maxDepth);
            }
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

