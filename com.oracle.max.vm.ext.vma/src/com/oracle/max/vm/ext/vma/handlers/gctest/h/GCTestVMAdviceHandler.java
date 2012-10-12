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
package com.oracle.max.vm.ext.vma.handlers.gctest.h;

import java.util.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

/**
 * In early development there were problems with GC ref maps relative to the advice calls in generated code. This
 * "handler" performs GC on a random basis for each advice method which, hopefully, is a useful regression test that the
 * maps are ok.
 *
 * By default, a GC will be invoked (randomly) on approximately 10% of the advice methods.
 * The percentage can be controlled by the {@link #PERCENT_PROPERTY} property. Setting the
 * value at 100 will invoke a GC on every advice method.
 *
 * Can be built into the boot image or dynamically loaded.
 */
public class GCTestVMAdviceHandler extends VMAdviceHandler {

    private static final String PERCENT_PROPERTY = "max.vma.handler.gctest.percent";
    private static final String VERBOSE_PROPERTY = "max.vma.handler.gctest.verbose";
    private static final String METHODS_PROPERTY = "max.vma.handler.gctest.methods";

    private Random random = new Random(46737);
    private static int frequency;
    private static Pattern methodsPattern;
    private static boolean verbose;
    private static final AdviceMethod[] methodValues = AdviceMethod.values();
    private static final AdviceMode[] modeValues = AdviceMode.values();


    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new GCTestVMAdviceHandler());
    }

    private void randomlyGC(int methodOrd, int adviceOrd) {
        randomlyGC(methodValues[methodOrd].name(), modeValues[adviceOrd].name());
    }

    private void randomlyGC(String method, String mode) {
        if (methodsPattern != null) {
            if (!matchMethod(method, mode)) {
                return;
            }
        }

        int next = random.nextInt(100);
        if (next % frequency == 0) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("GCTestVMAdviceHandler.GC: ");
            Log.print(method);
            Log.print(":");
            Log.println(mode);
            Log.unlock(lockDisabledSafepoints);
            System.gc();
        }
    }

    private static boolean matchMethod(String method, String mode) {
        return methodsPattern.matcher(method).matches();
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.RUNNING) {
            String methodPatternProp = System.getProperty(METHODS_PROPERTY);
            if (methodPatternProp != null) {
                methodsPattern = Pattern.compile(methodPatternProp);
            }
            String freq = System.getProperty(PERCENT_PROPERTY);
            if (freq == null) {
                frequency = 10;
            } else {
                frequency = 100 / Integer.parseInt(freq);
            }
            verbose = System.getProperty(VERBOSE_PROPERTY) != null;
        }
    }

// START GENERATED CODE
// EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY

    enum AdviceMethod {
        GC,
        ThreadStarting,
        ThreadTerminating,
        ReturnByThrow,
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
        New,
        NewArray,
        MultiNewArray,
        MethodEntry;
    }
    @Override
    public void adviseBeforeGC() {
        randomlyGC(0, 0);
    }

    @Override
    public void adviseAfterGC() {
        randomlyGC(0, 1);
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread arg1) {
        randomlyGC(1, 0);
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread arg1) {
        randomlyGC(2, 0);
    }

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        randomlyGC(3, 0);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        randomlyGC(4, 0);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        randomlyGC(4, 0);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        randomlyGC(4, 0);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        randomlyGC(4, 0);
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        randomlyGC(5, 0);
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        randomlyGC(6, 0);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        randomlyGC(7, 0);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        randomlyGC(7, 0);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        randomlyGC(7, 0);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        randomlyGC(7, 0);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        randomlyGC(8, 0);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        randomlyGC(8, 0);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        randomlyGC(8, 0);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        randomlyGC(8, 0);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        randomlyGC(9, 0);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        randomlyGC(10, 0);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        randomlyGC(10, 0);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        randomlyGC(10, 0);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        randomlyGC(11, 0);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        randomlyGC(11, 0);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        randomlyGC(11, 0);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        randomlyGC(12, 0);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        randomlyGC(12, 0);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        randomlyGC(13, 0);
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        randomlyGC(14, 0);
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        randomlyGC(14, 0);
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        randomlyGC(14, 0);
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        randomlyGC(14, 0);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        randomlyGC(14, 0);
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, int arg3) {
        randomlyGC(15, 0);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, Object arg4) {
        randomlyGC(16, 0);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, double arg4) {
        randomlyGC(16, 0);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, long arg4) {
        randomlyGC(16, 0);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, float arg4) {
        randomlyGC(16, 0);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, int arg3) {
        randomlyGC(17, 0);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, Object arg4) {
        randomlyGC(18, 0);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, double arg4) {
        randomlyGC(18, 0);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, long arg4) {
        randomlyGC(18, 0);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, float arg4) {
        randomlyGC(18, 0);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        randomlyGC(19, 0);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        randomlyGC(20, 0);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        randomlyGC(21, 0);
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        randomlyGC(22, 0);
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        randomlyGC(23, 0);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        randomlyGC(24, 0);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        randomlyGC(25, 0);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        randomlyGC(26, 0);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        randomlyGC(27, 0);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        randomlyGC(28, 0);
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        randomlyGC(5, 1);
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        randomlyGC(6, 1);
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        randomlyGC(29, 1);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        randomlyGC(30, 1);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        randomlyGC(31, 1);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        randomlyGC(32, 1);
    }

// END GENERATED CODE

}
