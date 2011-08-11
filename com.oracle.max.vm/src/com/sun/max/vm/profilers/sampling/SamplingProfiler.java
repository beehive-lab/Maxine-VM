/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.profilers.sampling;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.unsafe.*;

/**
 * Sampling profiler. Runs a thread that periodically wakes up, stops all the threads, and records their stack.
 * There is a singleton instance per VM and most state is static/global. Note that the stack is gathered regardless
 * of the state of the thread, e.g., it may be blocked.
 *
 * Attempts to allocate minimal heap memory to limit interference with the application.
 * The strategy is based on the assumption that the same stack traces will occur frequently.
 * The basic data structure is a map from {@link StackInfo} to a list of {@link ThreadSample}
 * instances which hold the sample count for each thread.
 *
 * A singleton global instance, {@link #workingStackInfo}, of {@link StackInfo} is used to gather the stack for a thread,
 * and an exact-length copy is entered into the map when a new stack is discovered.
 *
 * The {@link #terminate} method outputs the data at VM shutdown, but it can also be dumped
 * periodically. Data is output using the {@link Log} class. By default output is sorted by thread and by sample count
 * This has more allocation overhead at the time of output and so is the default only if data is output at
 * VM termination. In unsorted mode the stack traces and samples counts are output in an arbitrary order.
 */
public final class SamplingProfiler extends Thread {
    private static final int DEFAULT_FREQUENCY = 10;
    /**
     * The default depth is quite large mostly because of the depth of the current monitor lock acquisition call stack.
     */
    private static final int DEFAULT_DEPTH = 16;
    private static final Random rand = new Random();
    /**
     * The base period in milliseconds between activations of the profiler.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int sampleFrequency;

    /**
     * To mitigate strobe effects the profiler activations are randomized around {@link #sampleFrequency}.
     * The actual activation period is in the range {@code sampleFrequency - jiggle <-> sampleFrequency + jiggle}.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int jiggle;

    /**
     * The maximum stack depth the profiler will ever gather.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int maxStackDepth;

    /**
     * Used as a scratch object for the working stack being analyzed, to avoid excessive heap allocation.
     * Stacks are analyzed serially and the (working) stack being analyzed is built up in this object,
     * which is reset prior to the analysis. The map lookup uses this object and only if the stack has not
     * been seen before is a new {@link StackInfo} object allocated and the contents copied in.
     * Therefore, once an application reaches a steady-state, allocations should be minimal.
     *
     */
    private static StackInfo workingStackInfo;

    /**
     * Records the depth for the working stack being analyzed.
     */
    private static int workingStackDepth;

    /**
     * Allows profiling to be turned off temporarily.
     */
    private static volatile boolean isProfiling;

    /**
     * Number of samples collected.
     */
    private static int sampleCount;

    /**
     * Period in milliseconds between dumping the traces to the log.
     * Zero implies only dump on VM termination.
     */
    private static long dumpInterval;

    /**
     * The profiler thread itself.
     */
    private static VmThread theProfiler;

    /**
     * {@code true} if an only if we are generated sorted output.
     */
    private static boolean sortedOutput;

    /**
     * {@code true} if and only if we are tracking (system) VM threads.
     */
    private static boolean trackSystemThreads;

    /**
     * A debugging aid; logs the time at which the sampling thread woke up.
     */
    private static boolean logSampleTimes;

    /**
     * For each unique stack trace, we record the list of threads with that trace and their sample count.
     */
    private static Map<StackInfo, List<ThreadSample>> stackInfoMap = new HashMap<StackInfo, List<ThreadSample>>();

    private SamplingProfiler() {
        super(VmThread.systemThreadGroup, "SamplingProfiler");
        setDaemon(true);
    }

    /**
     * Create the profiler with the options given by {@code optionValue}.
     * @param optionValue a string of the form {@code :frequency=f,stackdepth=d,dump=t} where any element may be omitted.
     */
    public static void create(String optionValue) {
        int frequency = 0;
        int stackDepth = 0;
        int dumpPeriod = 0;
        int sortOption = -1;
        if (optionValue.length() > 0) {
            if (optionValue.charAt(0) == ':') {
                String[] options = optionValue.substring(1).split(",");
                for (String option : options) {
                    if (option.startsWith("frequency")) {
                        frequency = getOption(option);
                    } else if (option.startsWith("stackdepth")) {
                        stackDepth = getOption(option);
                        if (stackDepth < 0) {
                            usage();
                        }
                    } else if (option.startsWith("dump")) {
                        dumpPeriod = getOption(option);
                    } else if (option.startsWith("debug")) {
                        logSampleTimes = true;
                    } else if (option.startsWith("systhreads")) {
                        trackSystemThreads = true;
                    } else if (option.startsWith("sort")) {
                        sortOption = getOption(option);
                    } else {
                        usage();
                    }
                }
            } else {
                usage();
            }
        }
        // if sort option is set, honor it, otherwise default based on dump
        sortedOutput = sortOption >= 0 ? (sortOption == 0 ? false : true) : dumpPeriod == 0;
        create(frequency, stackDepth, dumpPeriod);
    }

    private static void usage() {
        System.err.println("usage: -Xprof:frequency=f,stackdepth=d,dump=t,sort=s,systhreads");
        MaxineVM.native_exit(-1);
    }

    private static int getOption(String s) {
        final int index = s.indexOf('=');
        if (index < 0) {
            usage();
        }
        return Integer.parseInt(s.substring(index + 1));
    }

    /**
     * Create a sample-based profiler with given measurement frequency, stack depth and dump period.
     * @param frequency base period for measurements in millisecs, 0 implies {@value DEFAULT_FREQUENCY}
     * @param depth stack depth to record, 0 implies {@value DEFAULT_DEPTH}
     * @param dumpPeriod time in seconds between dumps to log, 0 implies only at termination (default)
     */
    private static void create(int frequency, int depth, int dumpPeriod) {
        sampleFrequency = frequency == 0 ? DEFAULT_FREQUENCY : frequency;
        jiggle = sampleFrequency / 10;
        if (jiggle <= 2) {
            jiggle = 2;
        }
        maxStackDepth = depth == 0 ? DEFAULT_DEPTH : depth;
        dumpInterval = dumpPeriod * 1000000000L;
        workingStackInfo = new StackInfo(maxStackDepth);
        final Thread profileThread = new SamplingProfiler();
        isProfiling = true;
        profileThread.start();
    }

    @Override
    public void run() {
        theProfiler = VmThread.fromJava(this);
        long lastDump = System.nanoTime();
        while (true) {
            try {
                final int thisJiggle = rand.nextInt(jiggle);
                final int thisPeriod = sampleFrequency + (rand.nextBoolean() ? thisJiggle : -thisJiggle);
                Thread.sleep(thisPeriod);
                final long now = System.nanoTime();
                if (isProfiling) {
                    if (logSampleTimes) {
                        boolean state = Log.lock();
                        Log.print("SamplingProfiler running at ");
                        Log.println(now);
                        Log.unlock(state);
                    }
                    stackTraceGatherer.submit();
                    sampleCount++;
                    if (dumpInterval > 0 && now > lastDump + dumpInterval) {
                        dumpTraces();
                        lastDump = now;
                    }
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Encapsulates the basic logic of handling one thread after all threads are frozen at a safepoint.
     */
    private static final class StackTraceGatherer extends VmOperation {
        StackTraceGatherer() {
            super("SamplingProfiler", null, Mode.Safepoint);
        }

        @Override
        protected boolean operateOnThread(VmThread thread) {
            final boolean ignore = thread == theProfiler ||
                (isSystemThread(thread) && !trackSystemThreads);
            return !ignore;
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            SamplingStackTraceVisitor stv = new SamplingStackTraceVisitor();
            final VmStackFrameWalker stackFrameWalker = vmThread.samplingProfilerStackFrameWalker();
            workingStackInfo.reset();
            workingStackDepth = 0;
            stv.walk(stackFrameWalker, ip, sp, fp);
            // Have we seen this stack before?
            List<ThreadSample> threadSampleList = stackInfoMap.get(workingStackInfo);
            if (threadSampleList == null) {
                threadSampleList = new ArrayList<ThreadSample>();
                final StackInfo copy = workingStackInfo.copy(workingStackDepth);
                assert stackInfoMap.put(copy, threadSampleList) == null;
            }
            // Check if this thread has had this stack trace before, allocating a new ThreadSample instance if not
            final ThreadSample threadSample = getThreadSample(threadSampleList, vmThread);
            // bump the number of times the given thread has been in this state
            threadSample.count++;
        }
    }

    private static boolean isSystemThread(VmThread vmThread) {
        // Should be able to determine this via VmThread.systemThreadGroup ...
        return vmThread == VmThread.referenceHandlerThread || vmThread == VmThread.finalizerThread ||
               vmThread == VmThread.signalDispatcherThread;
    }

    private static final StackTraceGatherer stackTraceGatherer = new StackTraceGatherer();

    /**
     * Allocation free stack frame analyzer that builds up the {@StackInfo} in {@link SampleProfiler#workingStackInfo}.
     */
    private static class SamplingStackTraceVisitor extends StackTraceVisitor {

        SamplingStackTraceVisitor() {
            super(null, maxStackDepth);
        }
        @Override
        public boolean add(ClassMethodActor classMethodActor, int sourceLineNumber) {
            workingStackInfo.stack[workingStackDepth].classMethodActor = classMethodActor;
            workingStackInfo.stack[workingStackDepth].lineNumber = sourceLineNumber;
            workingStackDepth++;
            return workingStackDepth < maxStackDepth;
        }

        @Override
        public void clear() {
            workingStackInfo.reset();
            workingStackDepth = 0;
        }

        @Override
        public StackTraceElement[] getTrace() {
            return null;
        }
    }

    private static ThreadSample getThreadSample(List<ThreadSample> threadSampleList, VmThread vmThread) {
        for (ThreadSample  threadSample : threadSampleList) {
            if (threadSample.vmThread == vmThread) {
                return threadSample;
            }
        }
        final ThreadSample  threadSample = new ThreadSample(vmThread);
        threadSampleList.add(threadSample);
        return threadSample;
    }


    /**
     * Value class that records a thread and a sample count.
     */
    private static class ThreadSample {
        VmThread vmThread;
        private long count;

        ThreadSample(VmThread vmThread) {
            this.vmThread = vmThread;
        }

        static void print(VmThread vmThread) {
            final Thread t = vmThread.javaThread();
            Log.print("Thread id=");
            Log.print(t.getId());
            Log.print(", name=\"");
            Log.print(t.getName());
            Log.print("\"");
        }

        void print() {
            print(vmThread);
            Log.print(" count: ");
            Log.print(count);
        }

    }

    /**
     * Value class that captures the essential information on a stack frame element.
     */
    private static class StackElement {
        ClassMethodActor classMethodActor;
        int lineNumber;  // < 0 if unknown

        void print() {
            if (classMethodActor != null) {
                final ClassActor holder = classMethodActor.holder();
                Log.print(holder.name.toString());
                Log.print('.');
                Log.print(classMethodActor.name().toString());
                Log.print('(');
                if (lineNumber > 0) {
                    Log.print(holder.sourceFileName);
                    Log.print(':');
                    Log.print(lineNumber);
                } else {
                    Log.print("Native Method");
                }
                Log.println(')');
            }
        }
    }

    /**
     * The essential information on a sequence of frames, with support for comparison and hashing.
     */
    private static class StackInfo {
        StackElement[] stack;

        StackInfo(int depth) {
            stack = new StackElement[depth];
            for (int i = 0; i < depth; i++) {
                stack[i] = new StackElement();
            }
        }

        protected StackInfo(StackElement[] stack) {
            this.stack = stack;
        }

        void reset() {
            for (int i = 0; i < stack.length; i++) {
                stack[i].classMethodActor = null;
                stack[i].lineNumber = -1;
            }
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (StackElement s : stack) {
                if (s.classMethodActor == null) {
                    break;
                } else {
                    result ^= s.lineNumber ^ s.classMethodActor.hashCode();
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            // array lengths may be different, but can still compare equal
            final StackInfo osi = (StackInfo) o;
            int min = stack.length;
            StackInfo longer = null;
            if (stack.length < osi.stack.length) {
                longer = osi;
            } else if (stack.length > osi.stack.length) {
                min = osi.stack.length;
                longer = this;
            }
            // compare up to min
            for (int i = 0; i < min; i++) {
                if ((osi.stack[i].classMethodActor != stack[i].classMethodActor) ||
                                osi.stack[i].lineNumber != stack[i].lineNumber) {
                    return false;
                }
            }
            // same length or if longer is empty after min
            return  (longer == null) || longer.stack[min].classMethodActor == null;
        }

        /**
         * Shallow copy of this object, truncating length.
         * @return copied object
         */
        StackInfo copy(int depth) {
            final StackInfo result = new StackInfo(depth);
            for (int i = 0; i < result.stack.length; i++) {
                result.stack[i].classMethodActor = this.stack[i].classMethodActor;
                result.stack[i].lineNumber = this.stack[i].lineNumber;
            }
            return result;
        }
    }

    public static void terminate() {
        isProfiling = false;
        dumpTraces();
    }

    private static void dumpTraces() {
        Map<VmThread, CountedStackInfo[]> sortedInfo = null;
        if (sortedOutput) {
            sortedInfo = sortByThread();
        }
        boolean state = Log.lock();
        Log.print("Maxine Sampling Profiler Stack Traces, #samples: ");
        Log.println(sampleCount);
        if (sortedOutput) {
            dumpSortedOutput(sortedInfo);
        } else {
            for (Map.Entry<StackInfo, List<ThreadSample>> entry : stackInfoMap.entrySet()) {
                final StackInfo stackInfo = entry.getKey();
                final List<ThreadSample> threadSampleList = entry.getValue();
                for (StackElement se : stackInfo.stack) {
                    se.print();
                }
                for (ThreadSample ti : threadSampleList) {
                    Log.print("  ");
                    ti.print();
                    Log.println();
                }
                Log.println();
            }
        }
        Log.unlock(state);
    }

    /*
     * All the code below here is only used for sorted output.
     */

    private static void dumpSortedOutput(Map<VmThread, CountedStackInfo[]> sortedInfo) {
        for (Map.Entry<VmThread, CountedStackInfo[]> entry : sortedInfo.entrySet()) {
            ThreadSample.print(entry.getKey());
            Log.println();
            long totalSamples = 0;
            for (CountedStackInfo countedStackInfo : entry.getValue()) {
                totalSamples += countedStackInfo.count;
            }
            for (CountedStackInfo countedStackInfo : entry.getValue()) {
                Log.print("Sample count ");
                Log.print(countedStackInfo.count);
                double percentage = ((double) countedStackInfo.count) * 100.0f / totalSamples;
                Log.print(" (");
                Log.print(percentage);
                Log.println("%)");
                for (StackElement se : countedStackInfo.stack) {
                    se.print();
                }
                Log.println();
            }
        }
    }

    /**
     * Used when sorted output is generated. It effectively pairs a {@link StackInfo} with a
     * sample count for a thread.
     */
    private static class CountedStackInfo extends StackInfo implements Comparator<CountedStackInfo> {
        private long count;
        CountedStackInfo(StackInfo stackInfo, long count) {
            super(stackInfo.stack);
            this.count = count;
        }

        @Override
        public int compare(CountedStackInfo a, CountedStackInfo b) {
            // low count compares higher
            if (a.count < b.count) {
                return +1;
            } else if (a.count > b.count) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private static Map<VmThread, CountedStackInfo[]> sortByThread() {
        Map <VmThread, ArrayList<CountedStackInfo>> tempMap = new HashMap<VmThread, ArrayList<CountedStackInfo>>();
        for (Map.Entry<StackInfo, List<ThreadSample>> entry : stackInfoMap.entrySet()) {
            final StackInfo stackInfo = entry.getKey();
            final List<ThreadSample> threadSampleList = entry.getValue();
            for (ThreadSample ti : threadSampleList) {
                ArrayList<CountedStackInfo> threadCounts = tempMap.get(ti.vmThread);
                if (threadCounts == null) {
                    threadCounts = new ArrayList<CountedStackInfo>();
                    tempMap.put(ti.vmThread, threadCounts);
                }
                threadCounts.add(new CountedStackInfo(stackInfo, ti.count));
            }
        }
        Map<VmThread, CountedStackInfo[]> result = new HashMap<VmThread, CountedStackInfo[]>();
        for (Map.Entry<VmThread, ArrayList<CountedStackInfo>> entry : tempMap.entrySet()) {
            CountedStackInfo[] threadCountsArray = entry.getValue().toArray(new CountedStackInfo[entry.getValue().size()]);
            Arrays.sort(threadCountsArray, threadCountsArray[0]);
            result.put(entry.getKey(), threadCountsArray);
        }
        return result;
    }

}

