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
package com.oracle.max.vma.tools.qa.queries;

import java.io.*;
import java.util.*;

import javax.swing.tree.*;

import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.callgraph.*;
import com.oracle.max.vma.tools.qa.callgraph.CallGraphDisplay.*;

/**
 * Essentially a debugging aid. Simulates a sampling profiler with either a given frequency
 * or a provided list of sample times and produces the "ideal" profile.
 */
public class ProfileSimulatorQuery extends QueryBase {
    private static final int DEFAULT_SAMPLE_FREQUENCY = 10; // ms
    private int sampleFrequency = DEFAULT_SAMPLE_FREQUENCY;
    private SampleTimes sampleTimeGenerator;
    private StackInfo workingStackInfo = new StackInfo(1024);

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        boolean showMissing = false;
        boolean sortByThread = false;
        String sampleFile = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            args[i] = null; // assume match
            if (arg.equals("-freq")) {
                i++;
                sampleFrequency = Integer.parseInt(args[i]);
                args[i] = null;
            } else if (arg.equals("-missing")) {
                showMissing = true;
            } else if (arg.equals("-file")) {
                sampleFile = args[++i];
                args[i] = null;
            } else if (arg.equals("-sort")) {
                sortByThread = true;
            } else {
                args[i] = arg;
            }
        }
        // Checkstyle: resume modified control variable check
        CallGraphDisplay cg = CallGraphDisplay.queryMain(traceRun, removeProcessedArgs(args));
        CallGraphDisplay.timeDisplay = TimeDisplay.WallRel;
        cg.traceStartTime = traceRun.startTime;
        sampleTimeGenerator = sampleFile == null ? new DefaultSampleTimes(traceRun.startTime) : new FileSampleTimes(sampleFile, traceRun.lastTime);

        sampleFrequency = sampleFrequency * 1000000; // nanos
        ThreadState[] threadStates = new ThreadState[cg.threadCallGraphs.size()];
        int tsx = 0;
        for (DefaultMutableTreeNode root : cg.threadCallGraphs.values()) {
            threadStates[tsx++] = new ThreadState(root);
        }

        int missingCount = 0;
        int matchCount = 0;
        long sampleTime = sampleTimeGenerator.nextSampleTime();
        while (sampleTime < traceRun.lastTime) {
            sampleTime = sampleTimeGenerator.nextSampleTime();
            for (ThreadState threadState : threadStates) {
                DefaultMutableTreeNode tn = threadState.methodNodeAt(sampleTime);
                if (tn == null) {
                    if (showMissing) {
                        ps.printf("%-20d no method found%n", sampleTime);
                    }
                    missingCount++;
                } else {
                    createStackInfo(tn, matchCount);
                    matchCount++;
                }
            }
        }
        ps.printf("Maxine Simulated Sampling Profiler Stack Traces, #samples: %d, no match at %d%n", sampleTimeGenerator.sampleCount(), missingCount);
        if (sortByThread) {
            Map<String, CountedStackInfo[]> sortedInfo = sortByThread();
            for (Map.Entry<String, CountedStackInfo[]> entry : sortedInfo.entrySet()) {
                ps.printf("Thread %s%n", entry.getKey());
                for (CountedStackInfo countedStackInfo : entry.getValue()) {
                    ps.printf("sample count %d%n", countedStackInfo.count);
                    for (StackElement se : countedStackInfo.stack) {
                        se.print(ps);
                    }
                    ps.println();
                }
            }
        } else {
            dumpTraces(ps, sampleTimeGenerator.sampleCount(), missingCount);
        }
        return true;
    }

    private void createStackInfo(final DefaultMutableTreeNode tn, int matchCount) {
        DefaultMutableTreeNode ttn = tn;
        MethodData md = (MethodData) ttn.getUserObject();
        String threadName = null;
        workingStackInfo.reset();
        int wx = 0;
        while (md instanceof TimeMethodData) {
            if (threadName == null) {
                threadName = md.thread;
            } else {
                assert threadName.equals(md.thread);
            }
            workingStackInfo.stack[wx++].classMethodActor = md.methodName;
            ttn = (DefaultMutableTreeNode) ttn.getParent();
            md = (MethodData) ttn.getUserObject();
        }
        validate(matchCount);
        // Have we seen this stack before?
        List<ThreadInfo> threadInfoList = stackInfoMap.get(workingStackInfo);
        if (threadInfoList == null) {
            threadInfoList = new ArrayList<ThreadInfo>();
            final StackInfo copy = workingStackInfo.copy(wx);
            assert stackInfoMap.put(copy, threadInfoList) == null;
        }
        // Check if this thread has had this stack trace before, allocating a new {@link ThreadInfo} instance if not
        final ThreadInfo threadInfo = getThreadInfo(threadInfoList, threadName);
        // bump the number of times the given thread has been in this state
        threadInfo.count++;
        validate(matchCount + 1);
    }

    private void validate(int count) {
        int countCheck = 0;
        for (Map.Entry<StackInfo, List<ThreadInfo>> entry : stackInfoMap.entrySet()) {
            final List<ThreadInfo> threadInfoList = entry.getValue();
            for (ThreadInfo ti : threadInfoList) {
                countCheck += ti.count;
            }
        }
        assert countCheck == count;
    }

    private void dumpTraces(PrintStream ps, int sampleCount, int missingCount) {
        int countCheck = missingCount;
        for (Map.Entry<StackInfo, List<ThreadInfo>> entry : stackInfoMap.entrySet()) {
            final StackInfo stackInfo = entry.getKey();
            final List<ThreadInfo> threadInfoList = entry.getValue();
            for (StackElement se : stackInfo.stack) {
                se.print(ps);
            }
            for (ThreadInfo ti : threadInfoList) {
                ti.print(ps);
                countCheck += ti.count;
            }
            ps.println();
        }
        assert countCheck == sampleCount;
    }

    private Map<String, CountedStackInfo[]> sortByThread() {
        Map <String, ArrayList<CountedStackInfo>> tempMap = new HashMap<String, ArrayList<CountedStackInfo>>();
        for (Map.Entry<StackInfo, List<ThreadInfo>> entry : stackInfoMap.entrySet()) {
            final StackInfo stackInfo = entry.getKey();
            final List<ThreadInfo> threadInfoList = entry.getValue();
            for (ThreadInfo ti : threadInfoList) {
                ArrayList<CountedStackInfo> threadCounts = tempMap.get(ti.threadName);
                if (threadCounts == null) {
                    threadCounts = new ArrayList<CountedStackInfo>();
                    tempMap.put(ti.threadName, threadCounts);
                }
                threadCounts.add(new CountedStackInfo(stackInfo, ti.count));
            }
        }
        Map<String, CountedStackInfo[]> result = new HashMap<String, CountedStackInfo[]>();
        for (Map.Entry<String, ArrayList<CountedStackInfo>> entry : tempMap.entrySet()) {
            CountedStackInfo[] threadCountsArray = entry.getValue().toArray(new CountedStackInfo[entry.getValue().size()]);
            Arrays.sort(threadCountsArray, threadCountsArray[0]);
            result.put(entry.getKey(), threadCountsArray);
        }
        return result;
    }

    class ThreadState {
        DefaultMutableTreeNode node;
        Enumeration iter;
        long earliestTime;

        ThreadState(DefaultMutableTreeNode node) {
            this.node = node;
            earliestTime = ((TimeMethodData) (((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject())).entryTimeInfo.wallTime;
            iter = node.depthFirstEnumeration();
        }

        DefaultMutableTreeNode next() {
            if (iter.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) iter.nextElement();
                if (child != node) {
                    return child;
                }
            }
            return null;
        }

        DefaultMutableTreeNode methodNodeAt(long sampleTime) {
            if (sampleTime < earliestTime) {
                // thread not running at this sample time
                return null;
            }
            while (true) {
                DefaultMutableTreeNode tn = next();
                if (tn == null) {
                    // out of methods, i.e., thread terminated by this sample time
                    return null;
                }
                TimeMethodData tmd = (TimeMethodData) tn.getUserObject();
                // need to be careful about a sample that lands in a "hole" in time;
                // e.g., the latency introduced by VMA itself can mean a sample time
                // is in a hole between the logged method executions
                if (sampleTime < tmd.entryTimeInfo.wallTime) {
                    return null;
                }
                if (tmd.entryTimeInfo.wallTime <= sampleTime && sampleTime <= tmd.exitTimeInfo.wallTime) {
                    return tn;
                }
            }
        }
    }

    abstract class SampleTimes {
        abstract long nextSampleTime();
        abstract int sampleCount();
    }

    class DefaultSampleTimes extends SampleTimes {
        private int sampleCount;
        private long sampleTime;

        DefaultSampleTimes(long startTime) {
            this.sampleTime = startTime;
        }

        @Override
        long nextSampleTime() {
            sampleTime += sampleFrequency;
            sampleCount++;
            return sampleTime;
        }

        @Override
        int sampleCount() {
            return sampleCount;
        }
    }

    class FileSampleTimes extends SampleTimes {
        private static final String KEY = "SamplingProfiler running";
        private ArrayList<Long> sampleTimes = new ArrayList<Long>();
        private int index;
        private long lastTime;

        FileSampleTimes(String pathname, long lastTime) {
            this.lastTime = lastTime;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(pathname));
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }
                    if (line.startsWith(KEY)) {
                        String[] parts = line.split(" ");
                        sampleTimes.add(Long.parseLong(parts[parts.length - 1]));
                    }
                }
            } catch (IOException ex) {
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                    }
                }
            }

        }

        @Override
        long nextSampleTime() {
            if (index < sampleTimes.size()) {
                return sampleTimes.get(index++);
            } else {
                return lastTime;
            }
        }

        @Override
        int sampleCount() {
            return sampleTimes.size();
        }
    }

    // Following essentially copied from SamplingProfiler

    /**
     * For each unique stack trace, we record the list of threads with that trace.
     */
    private static Map<StackInfo, List<ThreadInfo>> stackInfoMap = new HashMap<StackInfo, List<ThreadInfo>>();

    private static ThreadInfo getThreadInfo(List<ThreadInfo> threadInfoList, String threadName) {
        for (ThreadInfo  threadInfo : threadInfoList) {
            if (threadInfo.threadName.equals(threadName)) {
                return threadInfo;
            }
        }
        final ThreadInfo  threadInfo = new ThreadInfo(threadName);
        threadInfoList.add(threadInfo);
        return threadInfo;
    }

    /**
     * Value class that records a thread and a sample count.
     */
    private static class ThreadInfo {
        long count;
        String threadName;
        ThreadInfo(String threadName) {
            this.threadName = threadName;
        }

        void print(PrintStream ps) {
            if (threadName != null) {
                ps.print("  Thread name=\"");
                ps.print(threadName);
                ps.print("\" count: ");
                ps.println(count);
            }
        }

        @Override
        public String toString() {
            return threadName + ":" + count;
        }
    }

    /**
     * Value class that captures the essential information on a stack frame element.
     */
    private static class StackElement {
        String classMethodActor;
        int lineNumber = -1;  // < 0 if unknown

        void print(PrintStream ps) {
            if (classMethodActor != null) {
                ps.println(classMethodActor);
            }
        }

        @Override
        public String toString() {
            return classMethodActor;
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
                if (!osi.stack[i].classMethodActor.equals(stack[i].classMethodActor) ||
                                (osi.stack[i].lineNumber != stack[i].lineNumber)) {
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

        @Override
        public String toString() {
            String result = "[";
            boolean first = true;
            for (StackElement s : stack) {
                if (s.classMethodActor != null) {
                    if (!first) {
                        result += " ";
                    }
                    first = false;
                    result += s.classMethodActor;
                }
            }
            return result + "]";
        }
    }

    private static class CountedStackInfo extends StackInfo implements Comparator<CountedStackInfo> {
        long count;
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


}
