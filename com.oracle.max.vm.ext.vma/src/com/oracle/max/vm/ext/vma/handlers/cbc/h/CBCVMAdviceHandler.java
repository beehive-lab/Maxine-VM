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
package com.oracle.max.vm.ext.vma.handlers.cbc.h;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

/**
 * Counts the advice calls and outputs a summary in {@link #initialise} at termination. Not completely equivalent to
 * counting bytecodes owing to the bundling of similar bytecodes into one advice call, but similar. Counts are
 * maintained per-thread and then totaled. Can be built into the boot image or dynamically loaded.
 */
public class CBCVMAdviceHandler extends VMAdviceHandler {

    private static final String SORT_PROPERTY = "max.vma.handler.cbc.sort";

    private static boolean sort;

    private static ThreadCounts[] threadMap = new ThreadCounts[1024];

    private static boolean done;

    private static class ThreadCounts {
        VmThread vmThread;
        long[][] data = new long[AdviceMethod.values().length][AdviceMode.values().length];
        long total;

        static ThreadCounts get() {
            ThreadCounts threadCounts = threadMap[VmThread.current().uuid];
            if (threadCounts.vmThread == null) {
                threadCounts.vmThread = VmThread.current();
            }
            return threadCounts;
        }
    }

    private static class SortableCount implements Comparable<SortableCount> {
        private AdviceMethod method;
        private AdviceMode mode;
        private long count;

        SortableCount(AdviceMethod method, AdviceMode mode, long count) {
            this.method = method;
            this.mode = mode;
            this.count = count;
        }

        @Override
        public int compareTo(SortableCount o) {
            return count < o.count ? 1 : (count > o.count ? -1 : 0);
        }

    }

    static {
        for (int i = 0; i < threadMap.length; i++) {
            threadMap[i] = new ThreadCounts();
        }
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.TERMINATING) {
            done = true;
            PrintStream ps = System.out;
            try {
                String logpathProperty = System.getProperty("max.test.logpathbase");
                if (logpathProperty != null) {
                    ps = new PrintStream(new FileOutputStream(logpathProperty + ".vma"));
                }
                ThreadCounts allThreadCounts = new ThreadCounts();
                for (int i = 0; i < threadMap.length; i++) {
                    ThreadCounts threadCounts = threadMap[i];
                    if (threadCounts.vmThread != null) {
                        printCounts(ps, threadCounts, allThreadCounts);
                    }
                }
                printCounts(ps, allThreadCounts, allThreadCounts);

                String sortProperty = System.getProperty(SORT_PROPERTY);
                if (sortProperty == null || !sortProperty.equals("false")) {
                    SortableCount[] sortedCounts = new SortableCount[AdviceMethod.values().length * AdviceMode.values().length];
                    int index = 0;
                    for (AdviceMethod method : AdviceMethod.values()) {
                        for (AdviceMode mode : AdviceMode.values()) {
                            sortedCounts[index++] = new SortableCount(method, mode, allThreadCounts.data[method.ordinal()][mode.ordinal()]);
                        }
                    }
                    Arrays.sort(sortedCounts);
                    ps.println("Sorted counts");
                    for (int i = 0; i < sortedCounts.length; i++) {
                        SortableCount sc = sortedCounts[i];
                        if (sc.count == 0) {
                            ps.println("zero for remainder");
                            break;
                        }
                        ps.printf("  %-20s %s %s%n", sc.count, sc.method, sc.mode);
                    }
                }
            } catch (IOException ex) {
                System.err.println(ex);
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private static void printCounts(PrintStream ps, ThreadCounts counts, ThreadCounts allThreadCounts) {
        if (counts == allThreadCounts) {
            ps.println("Total for all threads");
        } else {
            ps.println("Thread: " + counts.vmThread.getName());
        }
        for (AdviceMethod am : AdviceMethod.values()) {
            long beforeCount = counts.data[am.ordinal()][0];
            long afterCount = counts.data[am.ordinal()][1];
            long beforeAfterCount = beforeCount + afterCount;
            ps.printf("  %-20s B:%,d, A:%,d%n", am.name(), beforeCount, afterCount);
            if (counts != allThreadCounts) {
                counts.total += beforeAfterCount;
                allThreadCounts.data[am.ordinal()][0] += beforeCount;
                allThreadCounts.data[am.ordinal()][1] += afterCount;
                allThreadCounts.total += beforeAfterCount;
            }
        }
        ps.printf("Total: %,d%n", counts.total);

    }

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new CBCVMAdviceHandler());
    }

// START GENERATED CODE
// EDIT AND RUN CBCVMAdviceHandlerGenerator.main() TO MODIFY

    enum AdviceMethod {
        GC,
        ThreadStarting,
        ThreadTerminating,
        ReturnByThrow,
        New,
        NewArray,
        MultiNewArray,
        ConstLoad,
        Load,
        ArrayLoad,
        Store,
        ArrayStore,
        StackAdjust,
        Operation,
        Conversion,
        If,
        Goto,
        Return,
        GetStatic,
        PutStatic,
        GetField,
        PutField,
        InvokeVirtual,
        InvokeSpecial,
        InvokeStatic,
        InvokeInterface,
        ArrayLength,
        Throw,
        CheckCast,
        InstanceOf,
        MonitorEnter,
        MonitorExit,
        MethodEntry;
    }

    private static final int MAX_LENGTH = 17;

    @Override
    public void adviseBeforeGC() {
        ThreadCounts.get().data[0][0]++;
    }

    @Override
    public void adviseAfterGC() {
        ThreadCounts.get().data[0][1]++;
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread arg1) {
        ThreadCounts.get().data[1][0]++;
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread arg1) {
        ThreadCounts.get().data[2][0]++;
    }

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        ThreadCounts.get().data[3][0]++;
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        ThreadCounts.get().data[4][1]++;
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        ThreadCounts.get().data[5][1]++;
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        ThreadCounts.get().data[6][1]++;
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        ThreadCounts.get().data[7][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        ThreadCounts.get().data[7][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        ThreadCounts.get().data[7][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        ThreadCounts.get().data[7][0]++;
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        ThreadCounts.get().data[8][0]++;
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        ThreadCounts.get().data[9][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        ThreadCounts.get().data[10][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        ThreadCounts.get().data[10][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        ThreadCounts.get().data[10][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        ThreadCounts.get().data[10][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        ThreadCounts.get().data[11][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        ThreadCounts.get().data[11][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        ThreadCounts.get().data[11][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        ThreadCounts.get().data[11][0]++;
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        ThreadCounts.get().data[12][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        ThreadCounts.get().data[13][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        ThreadCounts.get().data[13][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        ThreadCounts.get().data[13][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        ThreadCounts.get().data[14][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        ThreadCounts.get().data[14][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        ThreadCounts.get().data[14][0]++;
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        ThreadCounts.get().data[15][0]++;
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        ThreadCounts.get().data[15][0]++;
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        ThreadCounts.get().data[16][0]++;
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        ThreadCounts.get().data[17][0]++;
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        ThreadCounts.get().data[17][0]++;
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        ThreadCounts.get().data[17][0]++;
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        ThreadCounts.get().data[17][0]++;
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        ThreadCounts.get().data[17][0]++;
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        ThreadCounts.get().data[18][0]++;
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        ThreadCounts.get().data[19][0]++;
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        ThreadCounts.get().data[19][0]++;
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        ThreadCounts.get().data[19][0]++;
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        ThreadCounts.get().data[19][0]++;
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        ThreadCounts.get().data[20][0]++;
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        ThreadCounts.get().data[21][0]++;
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        ThreadCounts.get().data[21][0]++;
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        ThreadCounts.get().data[21][0]++;
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        ThreadCounts.get().data[21][0]++;
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        ThreadCounts.get().data[22][0]++;
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        ThreadCounts.get().data[23][0]++;
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        ThreadCounts.get().data[24][0]++;
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        ThreadCounts.get().data[25][0]++;
    }

    @Override
    public void adviseAfterArrayLength(int arg1, Object arg2, int arg3) {
        ThreadCounts.get().data[26][0]++;
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        ThreadCounts.get().data[27][0]++;
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        ThreadCounts.get().data[28][0]++;
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        ThreadCounts.get().data[29][0]++;
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        ThreadCounts.get().data[30][0]++;
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        ThreadCounts.get().data[31][0]++;
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        ThreadCounts.get().data[8][1]++;
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        ThreadCounts.get().data[9][1]++;
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        ThreadCounts.get().data[32][1]++;
    }

// END GENERATED CODE

}
