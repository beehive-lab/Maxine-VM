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
package com.oracle.max.vm.ext.vma.handlers.timetest.h;

import java.io.*;

import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

/**
 * Tests the correlation between {@link VMATimeMode#ID} mode and {@link VMATimeMode#WALLNS}.
 */
public class TimeTestVMAdviceHandler extends ObjectStateAdapter {

    private static final String MAXCOUNT_PROPERTY = "max.vma.handler.timetest.maxcount";
    private static final String INTERVAL_PROPERTY = "max.vma.handler.timetest.interval";
    private static final String LOGFILE_PROPERTY = "max.vma.handler.timetest.file";

    private static final String DEFAULT_LOGFILE = "timetest.vma";
    private static final int DEFAULT_INTERVAL = 100;
    private static final int DEFAULT_MAXCOUNT = 1000000;

    private static class TimeInfo {
        long wallTime;
        long timeModeTime;
    }

    private static VMATimeMode timeMode;
    private static long startWallTime;
    private static int checkInterval;
    private static TimeInfo[] timeInfoArray;
    private static int timeInfoIndex;

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new TimeTestVMAdviceHandler());
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.RUNNING) {
            timeMode = VMATimeMode.IDATOMIC;
            String intervalProp = System.getProperty(INTERVAL_PROPERTY);
            if (intervalProp == null) {
                checkInterval = DEFAULT_INTERVAL;
            } else {
                checkInterval = Integer.parseInt(intervalProp);
            }
            String maxCountProp = System.getProperty(MAXCOUNT_PROPERTY);
            int maxCount = DEFAULT_MAXCOUNT;
            if (maxCountProp != null) {
                maxCount = Integer.parseInt(maxCountProp);
            }
            timeInfoArray = new TimeInfo[maxCount];
            for (int i = 0; i < maxCount; i++) {
                timeInfoArray[i] = new TimeInfo();
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            PrintStream ps = null;
            try {
                String prop = System.getProperty(LOGFILE_PROPERTY);
                if (prop == null) {
                    prop = DEFAULT_LOGFILE;
                } else {
                    if (prop.length() == 0) {
                        ps = System.out;
                    }
                }
                if (ps == null) {
                    ps = new PrintStream(new FileOutputStream(prop));
                }

                for (int i = 1; i < timeInfoIndex; i++) {
                    TimeInfo pti = timeInfoArray[i - 1];
                    TimeInfo ti = timeInfoArray[i];
                    long diffW = ti.wallTime - pti.wallTime;
                    long diffT = ti.timeModeTime - pti.timeModeTime;
                    ps.printf("id: %d, w: %d, w/id %f, wi/idi %f%n", ti.timeModeTime, ti.wallTime,
                                    (double) ti.wallTime / (double) ti.timeModeTime,
                                    (double) diffW / (double) diffT);
                }

                ps.println();
            } catch (IOException ex) {
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    @Override
    protected void unseenObject(Object obj) {
        recordTime();
    }

    private static synchronized void recordTime() {
        if (startWallTime == 0) {
            startWallTime = System.nanoTime();
        }
        long timeModeTime = timeMode.getTime();
        if (timeModeTime % checkInterval == 0 && timeInfoIndex < timeInfoArray.length) {
            long elapsedWallTime = System.nanoTime() - startWallTime;
            TimeInfo timeInfo = timeInfoArray[timeInfoIndex++];
            timeInfo.timeModeTime = timeModeTime;
            timeInfo.wallTime = elapsedWallTime;
        }
    }

// START GENERATED CODE
// EDIT AND RUN TimeTestVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseAfterGC() {
        recordTime();
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread arg1) {
        recordTime();
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread arg1) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeGC() {
        recordTime();
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        recordTime();
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        recordTime();
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        recordTime();
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        recordTime();
    }

    @Override
    public void adviseAfterArrayLength(int arg1, Object arg2, int arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        recordTime();
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        recordTime();
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        recordTime();
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        recordTime();
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        recordTime();
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        recordTime();
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        recordTime();
    }

// END GENERATED CODE

}
