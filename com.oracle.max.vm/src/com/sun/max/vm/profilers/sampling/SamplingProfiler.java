/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.unsafe.*;

/**
 * Sampling profiler. It periodically stops all the threads and records stacks of some of them
 * The period of sampling is measured in units defined in derived classes.
 *
 * Attempts to allocate minimal heap memory to limit interference with the application.
 * The strategy is based on the assumption that the same stack traces will occur frequently.
 * The basic data structure is a map from {@link StackInfo} to a list of {@link ThreadSample}
 * instances which hold the sample count for each thread.
 *
 * Field {@link #workingStackInfo}, of {@link StackInfo} is used to gather the stack for a thread,
 * and an exact-length copy is entered into the map when a new stack is discovered.
 *
 * Note that due to the way the {@link VmOperation} works, all threads will be stopped in a
 * native method at the time the stack is gathered. Some threads may indeed be blocked
 * on a native method called by the thread. Others will be in the monitor code after
 * taking the trap that starts the thread stopping machinery. These stack frames should not
 * be presented to the user. This is handled by {@link SamplingStackTraceVisitor#clear()}.
 * Unfortunately this does mean that the stack depth control can't be honored trivially
 * as the stack is being gathered. This is optimized with {@link #workingStackClearSeen}.
 * The extra mechanism for unnecessary stack elimination can be enabled by setting
 * {@link SamplingStackTraceVisitor#stackTraceGatheringStartMarker} marker forcing {@link SamplingStackTraceVisitor}
 * to abandon gathering the chain of calls up to the marker inclusively.
 *
 * The {@link #terminate} method outputs the data at VM shutdown, but it can also be dumped
 * periodically. Data is output using the {@link Log} class. By default output is sorted by thread and by sample count
 * This has more allocation overhead at the time of output and so is the default only if data is output at
 * VM termination. In unsorted mode the stack traces and samples counts are output in an arbitrary order.
 */
public abstract class SamplingProfiler extends Thread {

    /**
     * Sampling profiler name.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected String samplingProfilerName;

    /**
     * Flag indicates whether a dedicated thread is used for sampling profiling.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected boolean useDedicatedThread;

    /**
     * The prefix of the option.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected String optionPrefix;

    /**
     * The default sampling period measured in unites defined in derived classes.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected int defaultPeriod;

    /**
     * The default flat argument.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected boolean defaultFlat;

    /**
     * Sample count increment.
     */
    protected long sampleCountIncrement;

    /**
     * This is a conservative empirically derived number that includes the {@link VmOperation}
     * frames for a stopped thread.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected int minimumDepth;

    /**
     * The default depth is quite large mostly because of the depth of the current monitor lock acquisition call stack.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected int defaultDepth;

    /**
     * The default sort argument.
     */
    private static final boolean DEFAULT_SORT = true;

    /**
     * The default systhreads argument.
     */
    private static final boolean DEFAULT_SYSTHREADS = false;

    /**
     * The minimum jiggle..
     */
    private static final int MINIMUM_JIGGLE = 2;

    /**
     * Pseuderandom numbers generator.
     */
    protected final Random rand = new Random();

    /**
     * The base period between activations of the profiler.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected int samplePeriod;

    /**
     * To mitigate strobe effects the profiler activations are randomized around {@link #samplePeriod}.
     * The actual activation period is in the range {@code samplePeriod - jiggle <-> samplePeriod + jiggle}.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected int jiggle;

    /**
     * The maximum stack depth the profiler will ever gather.
     * N.B. This applies to "user" frames, not the raw frames that include those caused
     * by the {@link VmOperation} thread stopping mechanism.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private int maxStackDepth;

    /**
     * Used as a scratch object for the working stack being analyzed, to avoid excessive heap allocation.
     * Stacks are analyzed serially and the (working) stack being analyzed is built up in this object,
     * which is reset prior to the analysis. The map lookup uses this object and only if the stack has not
     * been seen before is a new {@link StackInfo} object allocated and the contents copied in.
     * Therefore, once an application reaches a steady-state, allocations should be minimal.
     *
     */
    private StackInfo workingStackInfo;

    /**
     * Records the depth for the working stack being analyzed.
     */
    private int workingStackDepth;

    /**
     * This value is set {@code false} before any stack is gathered and set {@code true}
     * if and when {@link SamplingStackTraceVisitor#clear} is called.
     */
    private boolean workingStackClearSeen;

    /**
     * Allows profiling to be turned off temporarily.
     */
    protected volatile boolean isProfiling;

    /**
     * Number of samples collected.
     */
    protected int sampleCount;

    /**
     * Period in milliseconds between dumping the traces to the log.
     * Zero implies only dump on VM termination.
     */
    protected long dumpInterval;

    /**
     * The profiler thread itself.
     */
    protected VmThread theProfiler;

    /**
     * {@code true} if an only if we are generating sorted output.
     */
    private boolean sortedOutput;

    /**
     * Produces "flat" output like Hotspot. This implies {@link #sortedOutput} {@code = true}
     * and {@link #maxStackDepth} {@code = 1}.
     */
    private boolean flat;

    /**
     * {@code true} if and only if we are tracking (system) VM threads.
     */
    protected boolean trackSystemThreads;

    /**
     * A debugging aid; logs the time at which the sampling thread woke up.
     */
    protected boolean logSampleTimes;

    /**
     * For each unique stack trace, we record the list of threads with that trace and their sample count.
     */
    private Map<StackInfo, List<ThreadSample>> stackInfoMap = new HashMap<StackInfo, List<ThreadSample>>();

    /**
     * Constructor.
     *
     * @param threadName
     *        the name of the dedicated thread
     *
     */
    protected SamplingProfiler(String threadName) {
        super(VmThread.systemThreadGroup, threadName);
        setDaemon(true);
    }

    /**
     * Create the profiler with the options given by {@code optionValue}.
     * @param optionValue a string of the form {@code :frequency=f,stackdepth=d,dump=t} where any element may be omitted.
     */
    protected void create(String optionValue) {
        int period = 0;
        int stackDepth = 0;
        int dumpPeriod = 0;
        boolean sortedOutputOptionSet = false;

        flat = defaultFlat;
        sortedOutput = DEFAULT_SORT;
        trackSystemThreads = DEFAULT_SYSTHREADS;
        if (optionValue.length() > 0) {
            if (optionValue.charAt(0) == ':') {
                String[] options = optionValue.substring(1).split(",");
                for (String option : options) {
                    if (option.startsWith("frequency")) {
                        period = getOption(option);
                    } else if (option.startsWith("depth")) {
                        stackDepth = getOption(option);
                        if (stackDepth < 0) {
                            usage();
                        }
                    } else if (option.startsWith("dump")) {
                        dumpPeriod = getOption(option);
                    } else if (option.startsWith("debug")) {
                        logSampleTimes = true;
                    } else if (option.startsWith("systhreads")) {
                        trackSystemThreads = getBoolOption(option);
                    } else if (option.startsWith("sort")) {
                        sortedOutputOptionSet = true;
                        sortedOutput = getBoolOption(option);
                    } else if (option.startsWith("flat")) {
                        flat = getBoolOption(option);
                    } else {
                        usage();
                    }
                }
            } else {
                usage();
            }
        }
        // the default value is true unless dump is non-zero, as the sorting incurs both CPU and allocation overhead.
        if (sortedOutputOptionSet == false && dumpPeriod != 0) {
            sortedOutput = false;
        }
        if (flat) {
            stackDepth = 1;
        }
        create(period, stackDepth, dumpPeriod);
    }

    private void usage() {
        System.err.println("usage: " + optionPrefix + ":frequency=f,depth=d,systhreads,dump=t,sort[=t],flat[=t]");
        MaxineVM.native_exit(1);
    }

    private boolean getBoolOption(String s) {
        final int index = s.indexOf('=');
        if (index < 0) {
            return true;
        }
        return Boolean.parseBoolean(s.substring(index + 1));
    }

    private int getOption(String s) {
        final int index = s.indexOf('=');
        if (index < 0) {
            usage();
        }
        return Integer.parseInt(s.substring(index + 1));
    }

    /**
     * Create a sample-based profiler with given measurement frequency, stack depth and dump period.
     * @param period base period for measurements, 0 implies {@link #defaultPeriod}
     * @param depth stack depth to record, 0 implies {@link #defaultDepth}
     * @param dumpPeriod time in seconds between dumps to log, 0 implies only at termination (default)
     */
    private void create(int period, int depth, int dumpPeriod) {
        samplePeriod = period == 0 ? defaultPeriod : period;
        jiggle = samplePeriod / 10;
        if (jiggle <= MINIMUM_JIGGLE) {
            jiggle = MINIMUM_JIGGLE;
        }
        maxStackDepth = Math.max(minimumDepth, depth == 0 ? defaultDepth : depth);
        dumpInterval = dumpPeriod * 1000L;
        workingStackInfo = new StackInfo(maxStackDepth);
        isProfiling = true;
        if (useDedicatedThread || dumpInterval != 0) {
            final Thread profileThread = (Thread) this;
            profileThread.start();
        }
    }

    @Override
    public abstract void run();

    /**
     * Encapsulates the basic logic of handling one thread after all threads are frozen at a safepoint.
     */
    protected abstract class StackTraceGatherer extends VmOperation {

        StackTraceGatherer(String name) {
            super(name, null, Mode.Safepoint);
        }

        @Override
        protected abstract boolean operateOnThread(VmThread thread);

        /**
         * {@link ThreadSample} used to record samples of {@link VmOperation} thread.
         * Note that stack traces are not gathered for {@link VmOperation} thread.
         */
        private ThreadSample vmOperationThreadSample;

        /**
         * Gets parameter for {@link SamplingStackTraceVisitor} constructor marking the method in the stack
         * below which the methods should be gathered.
         */
        protected abstract ClassMethodActor getStackTraceGatheringStartMarker(VmThread vmThread);

        /**
         * Initialize sampling profiling on {@link VmOperation} thread itself.
         */
        public void initVMOperationThreadSample() {
            workingStackInfo.reset(0);
            List<ThreadSample> threadSampleList = stackInfoMap.get(workingStackInfo);
            assert threadSampleList == null;
            threadSampleList = new ArrayList<ThreadSample>();
            final StackInfo copy = workingStackInfo.copy(0);
            List<ThreadSample> existing = stackInfoMap.put(copy, threadSampleList);
            assert existing == null;
            vmOperationThreadSample = getThreadSample(threadSampleList, VmThread.vmOperationThread);
        }

        /**
         * Performs sampling profiling on {@link VmOperation} thread itself.
         */
        public void doVMOperationThread() {
            if (vmOperationThreadSample != null) {
                vmOperationThreadSample.count += sampleCountIncrement;
            }
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            ClassMethodActor classActor = getStackTraceGatheringStartMarker(vmThread);
            SamplingStackTraceVisitor sstv = new SamplingStackTraceVisitor(classActor);
            final VmStackFrameWalker stackFrameWalker = vmThread.samplingProfilerStackFrameWalker();
            workingStackInfo.reset(0);
            workingStackDepth = 0;
            workingStackClearSeen = false;
            sstv.walk(stackFrameWalker, ip, sp, fp);
            if (!workingStackClearSeen) {
                // we may have gathered > maxStackDepth frames; fix that here before we do the lookup
                if (workingStackDepth > maxStackDepth) {
                    workingStackInfo.reset(maxStackDepth);
                }
            }
            // Have we seen this stack before?
            List<ThreadSample> threadSampleList = stackInfoMap.get(workingStackInfo);
            if (threadSampleList == null) {
                threadSampleList = new ArrayList<ThreadSample>();
                final StackInfo copy = workingStackInfo.copy(maxStackDepth);
                List<ThreadSample> existing = stackInfoMap.put(copy, threadSampleList);
                assert existing == null;
            }
            // Check if this thread has had this stack trace before, allocating a new ThreadSample instance if not
            final ThreadSample threadSample = getThreadSample(threadSampleList, vmThread);
            // bump the number of times the given thread has been in this state
            threadSample.count += sampleCountIncrement;
        }
    }

    protected boolean isSystemThread(VmThread vmThread) {
        return vmThread.javaThread().getThreadGroup() == VmThread.systemThreadGroup;
    }

    @CONSTANT_WHEN_NOT_ZERO
    protected StackTraceGatherer stackTraceGatherer;

    /**
     * Allocation free stack frame analyzer that builds up the stack info in {@link SamplingProfiler#workingStackInfo}.
     */
    public class SamplingStackTraceVisitor extends StackTraceVisitor {
        /**
         *  Class method actor marking the method in the stack below which the methods should be gathered.
         *  This is used to abandon gathering the chain of calls which should not be presented to the user.
         *  If this value is {@code null}, then gathering is performed from the top of the stack.
         */
        private ClassMethodActor stackTraceGatheringStartMarker;

        SamplingStackTraceVisitor(ClassMethodActor stackTraceGatheringStartMarker) {
            super(null);
            this.stackTraceGatheringStartMarker = stackTraceGatheringStartMarker;
        }

        @Override
        public boolean add(ClassMethodActor classMethodActor, int sourceLineNumber) {
            assert classMethodActor != null;
            if (stackTraceGatheringStartMarker == null) {
                workingStackInfo.stack[workingStackDepth].classMethodActor = classMethodActor;
                workingStackInfo.stack[workingStackDepth].lineNumber = sourceLineNumber;
                workingStackDepth++;
                return workingStackClearSeen ? workingStackDepth < maxStackDepth : workingStackDepth < workingStackInfo.stack.length;
            } else {
                if (stackTraceGatheringStartMarker == classMethodActor) {
                    stackTraceGatheringStartMarker = null;
                }
                return true;
            }
        }

        @Override
        public void clear() {
            workingStackInfo.reset(0);
            workingStackDepth = 0;
            workingStackClearSeen = true;
        }

        @Override
        public StackTraceElement[] getTrace() {
            return null;
        }
    }

    private ThreadSample getThreadSample(List<ThreadSample> threadSampleList, VmThread vmThread) {
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
     *  Prints out {@link VmThread} and its samples.
     */
    private void printVmThreadAndSamples(VmThread vmThread, long samples) {
        final Thread t = vmThread.javaThread();
        Log.print("Thread id=");
        Log.print(t.getId());
        Log.print(", name=\"");
        Log.print(t.getName());
        Log.print("\"");
        Log.print(", #samples: ");
        Log.print(samples);
        Log.print(" (");
        printSamplesInPeriodUnits(samples);
        Log.print(")");
        Log.println();
    }

    /**
     *  Prints out {@link ThreadSample}.
     */
    private void printThreadSample(ThreadSample ts) {
        printVmThreadAndSamples(ts.vmThread, ts.count);
    }

    /**
     * Value class that records a thread and a sample count.
     */
    public static class ThreadSample {
        VmThread vmThread;
        private long count;

        ThreadSample(VmThread vmThread) {
            this.vmThread = vmThread;
        }
    }

    /**
     * Value class that captures the essential information on a stack frame element.
     */
    private class StackElement {
        ClassMethodActor classMethodActor;
        int lineNumber;  // < 0 if unknown

        StackElement() {
            classMethodActor = null;
            lineNumber = -1;
        }

        void print() {
            Log.print("  ");
            printName();
            Log.print('(');
            if (classMethodActor.nativeFunction == null) {
                Log.print(classMethodActor.holder().sourceFileName);
                if (lineNumber > 0) {
                    Log.print(':');
                    Log.print(lineNumber);
                }
            } else {
                Log.print("Native Method");
            }
            Log.println(')');
        }

        void printName() {
            Log.print(classMethodActor.holder().name.toString());
            Log.print('.');
            Log.print(classMethodActor.name().toString());
        }
    }

    /**
     * The essential information on a sequence of frames, with support for comparison and hashing.
     * The "logical" length of the stack is the number of elements, starting from zero,
     * for which classMethodActor != null. Comparison and hashing use the logical length.
     */
    public class StackInfo {
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

        void reset(int start) {
            for (int i = start; i < stack.length; i++) {
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
                ClassMethodActor cm = this.stack[i].classMethodActor;
                if (cm == null) {
                    break;
                }
                result.stack[i].classMethodActor = cm;
                result.stack[i].lineNumber = this.stack[i].lineNumber;
            }
            return result;
        }
    }

    public void restart() {
        isProfiling = false;
        stackInfoMap.clear();
        isProfiling = true;
    }

    public void terminate() {
        isProfiling = false;
        dumpTraces();
    }

    /**
     * Dumps traces. It should be synchronized with sampling profiling..
     */
    protected synchronized void dumpTraces() {
        Map<VmThread, CountedStackInfo[]> sortedInfo = null;
        if (sortedOutput) {
            sortedInfo = sortByThread();
        }
        boolean state = Log.lock();
        Log.print(samplingProfilerName + ", #samples: ");
        Log.print(sampleCount);

        Log.print(" (");
        printSamplesInPeriodUnits(sampleCount);
        Log.println(")");

        Log.println();
        if (sortedOutput) {
            dumpSortedOutput(sortedInfo);
        } else {
            for (Map.Entry<StackInfo, List<ThreadSample>> entry : stackInfoMap.entrySet()) {
                final StackInfo stackInfo = entry.getKey();
                final List<ThreadSample> threadSampleList = entry.getValue();
                for (ThreadSample ti : threadSampleList) {
                    printThreadSample(ti);
                }
                for (StackElement se : stackInfo.stack) {
                    if (se.classMethodActor == null) {
                        break;
                    }
                    se.print();
                }
                Log.println();
            }
        }
        Log.unlock(state);
    }


    /**
     * Prints samples in period units.
     */
    protected abstract void printSamplesInPeriodUnits(long samples);

    /*
     * All the code below here is only used for sorted output.
     */
    private void dumpSortedOutput(Map<VmThread, CountedStackInfo[]> sortedInfo) {
        for (Map.Entry<VmThread, CountedStackInfo[]> entry : sortedInfo.entrySet()) {
            long totalSamples = 0;
            for (CountedStackInfo countedStackInfo : entry.getValue()) {
                totalSamples += countedStackInfo.count;
            }
            if (totalSamples == 0) {
                assert entry.getKey() == VmThread.vmOperationThread;
                continue;
            }
            printVmThreadAndSamples(entry.getKey(), totalSamples);
            for (CountedStackInfo countedStackInfo : entry.getValue()) {
                // percentage to two decimal places, rounded
                long p1000 = (countedStackInfo.count * 100000) / totalSamples;
                long last = p1000 % 10;
                long p100 = p1000 / 10;
                if (last >= 5) {
                    p100++;
                }

                if (flat) {
                    printPercentage(p100);
                    Log.print("   ");
                    printCount(countedStackInfo.count);
                    Log.print(" (");
                    printSamplesInPeriodUnits(countedStackInfo.count);
                    Log.print(")");
                    Log.print("   ");
                    countedStackInfo.stack[0].printName();
                } else {
                    Log.print("Sample count ");
                    Log.print(countedStackInfo.count);
                    Log.print(" (");
                    printSamplesInPeriodUnits(countedStackInfo.count);
                    Log.print(")");
                    Log.print(" (");
                    printPercentage(p100);
                    Log.println(")");
                    for (StackElement se : countedStackInfo.stack) {
                        if (se.classMethodActor == null) {
                            break;
                        }
                        se.print();
                    }
                }
                Log.println();
            }
            Log.println();
        }
    }

    private void printPercentage(long p100) {
        long d1 = p100 / 100;
        printSpacesForLongOfPrintSize(d1, 3);
        Log.print(d1);
        Log.print('.');
        long d2 = p100 % 100;
        if (d2 < 10) {
            Log.print('0');
        }
        Log.print(d2);
        Log.print("%");
    }

    private void printCount(long count) {
        printSpacesForLongOfPrintSize(count, 8);
        Log.print(count);
    }

    protected void printSpacesForLongOfPrintSize(long value, int printSize) {
        long c = value;
        int s = 0;
        if (c <= 0) {
            c = -c;
            s = 1;
        }
        while (c > 0) {
            c = c / 10;
            s++;
        }
        for (int i = 0; i < printSize - s; i++) {
            Log.print(' ');
        }
    }

    /**
     * Used when sorted output is generated. It effectively pairs a {@link StackInfo} with a
     * sample count for a thread.
     */
    private class CountedStackInfo extends StackInfo implements Comparator<CountedStackInfo> {
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

    /**
     * Comparator to sort threads in decreasing order by the greatest sample count of the thread.
     */
    private class CountedStackInfosComparator implements Comparator<VmThread> {
        Map<VmThread, CountedStackInfo[]> unsortedMap;

        public CountedStackInfosComparator(Map<VmThread, CountedStackInfo[]> unsortedMap) {
            this.unsortedMap = unsortedMap;
        }
        public int compare(VmThread a, VmThread b) {
            if (unsortedMap.get(a)[0].count >= unsortedMap.get(b)[0].count) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private Map<VmThread, CountedStackInfo[]> sortByThread() {
        Map<VmThread, ArrayList<CountedStackInfo>> tempMap1 = new HashMap<VmThread, ArrayList<CountedStackInfo>>();
        for (Map.Entry<StackInfo, List<ThreadSample>> entry : stackInfoMap.entrySet()) {
            final StackInfo stackInfo = entry.getKey();
            final List<ThreadSample> threadSampleList = entry.getValue();
            for (ThreadSample ti : threadSampleList) {
                ArrayList<CountedStackInfo> threadCounts = tempMap1.get(ti.vmThread);
                if (threadCounts == null) {
                    threadCounts = new ArrayList<CountedStackInfo>();
                    tempMap1.put(ti.vmThread, threadCounts);
                }
                threadCounts.add(new CountedStackInfo(stackInfo, ti.count));
            }
        }
        HashMap<VmThread, CountedStackInfo[]> tempMap2 = new HashMap<VmThread, CountedStackInfo[]>();
        for (Map.Entry<VmThread, ArrayList<CountedStackInfo>> entry : tempMap1.entrySet()) {
            CountedStackInfo[] threadCountsArray = entry.getValue().toArray(new CountedStackInfo[entry.getValue().size()]);
            Arrays.sort(threadCountsArray, threadCountsArray[0]);
            tempMap2.put(entry.getKey(), threadCountsArray);
        }
        CountedStackInfosComparator comparator = new CountedStackInfosComparator(tempMap2);
        TreeMap<VmThread, CountedStackInfo[]> result = new TreeMap<VmThread, CountedStackInfo[]>(comparator);
        result.putAll(tempMap2);
        return result;
    }

}
