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
package com.oracle.max.vm.ext.vma.log.debug;

import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.log.*;
import com.sun.max.vm.*;


public class GCTestVMAdviceHandlerLog extends VMAdviceHandlerLog {

    private Random random = new Random(46737);
    private boolean gcAlways;
    private static AdviceMethod[] methodValues = AdviceMethod.values();
    private static AdviceMode[] modeValues = AdviceMode.values();


    private void randomlyGC(int methodOrd, int adviceOrd) {
        randomlyGC(methodValues[methodOrd].name(), modeValues[adviceOrd].name());
    }

    private void randomlyGC(String ident1, String ident2) {
        int next = random.nextInt(100);
        if (next == 57 || gcAlways) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("GCTestVMAdviceHandlerLog.GC: ");
            Log.print(ident1);
            Log.print(":");
            Log.println(ident2);
            Log.unlock(lockDisabledSafepoints);
            System.gc();
        }
    }

    @Override
    public boolean initializeLog() {
        gcAlways = System.getProperty("max.vma.gctest.everytime") != null;
        return true;
    }

    @Override
    public void finalizeLog() {
    }

    @Override
    public void removal(long id) {
    }

    @Override
    public void unseenObject(String threadName, long objId, String className, long clId) {
        randomlyGC("Unseen", "");

    }

    @Override
    public void resetTime() {
    }

    // BEGIN GENERATED CODE

    enum AdviceMethod {
        GC,
        ThreadStarting,
        ThreadTerminating,
        MethodEntry,
        ConstLoad,
        ConstLoadObject,
        IPush,
        Load,
        ArrayLoad,
        Store,
        StoreObject,
        ArrayStore,
        ArrayStoreObject,
        StackAdjust,
        Operation,
        IInc,
        Conversion,
        If,
        IfObject,
        ReturnObject,
        Return,
        GetStatic,
        PutStatic,
        PutStaticObject,
        GetField,
        PutField,
        PutFieldObject,
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
        Bytecode,
        New,
        NewArray,
        MultiNewArray;
    }
    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGC(String arg1) {
        randomlyGC(0, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterGC(String arg1) {
        randomlyGC(0, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadStarting(String arg1) {
        randomlyGC(1, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadTerminating(String arg1) {
        randomlyGC(2, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMethodEntry(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(3, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, double arg2) {
        randomlyGC(4, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, long arg2) {
        randomlyGC(4, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, float arg2) {
        randomlyGC(4, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoadObject(String arg1, long arg2) {
        randomlyGC(5, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIPush(String arg1, int arg2) {
        randomlyGC(6, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeLoad(String arg1, int arg2) {
        randomlyGC(7, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(String arg1, long arg2, int arg3) {
        randomlyGC(8, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, long arg3) {
        randomlyGC(9, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, float arg3) {
        randomlyGC(9, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, double arg3) {
        randomlyGC(9, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStoreObject(String arg1, int arg2, long arg3) {
        randomlyGC(10, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, float arg4) {
        randomlyGC(11, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, long arg4) {
        randomlyGC(11, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, double arg4) {
        randomlyGC(11, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStoreObject(String arg1, long arg2, int arg3, long arg4) {
        randomlyGC(12, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStackAdjust(String arg1, int arg2) {
        randomlyGC(13, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, long arg3, long arg4) {
        randomlyGC(14, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, float arg3, float arg4) {
        randomlyGC(14, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, double arg3, double arg4) {
        randomlyGC(14, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIInc(String arg1, int arg2, int arg3, int arg4) {
        randomlyGC(15, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, double arg3) {
        randomlyGC(16, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, long arg3) {
        randomlyGC(16, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, float arg3) {
        randomlyGC(16, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIf(String arg1, int arg2, int arg3, int arg4) {
        randomlyGC(17, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIfObject(String arg1, int arg2, long arg3, long arg4) {
        randomlyGC(18, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturnObject(String arg1, long arg2) {
        randomlyGC(19, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, long arg2) {
        randomlyGC(20, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, float arg2) {
        randomlyGC(20, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, double arg2) {
        randomlyGC(20, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1) {
        randomlyGC(20, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(String arg1, String arg2, long arg3, String arg4) {
        randomlyGC(21, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, double arg5) {
        randomlyGC(22, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, long arg5) {
        randomlyGC(22, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, float arg5) {
        randomlyGC(22, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStaticObject(String arg1, String arg2, long arg3, String arg4, long arg5) {
        randomlyGC(23, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(24, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, double arg6) {
        randomlyGC(25, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, long arg6) {
        randomlyGC(25, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, float arg6) {
        randomlyGC(25, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutFieldObject(String arg1, long arg2, String arg3, long arg4, String arg5, long arg6) {
        randomlyGC(26, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeVirtual(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(27, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeSpecial(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(28, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeStatic(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(29, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeInterface(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(30, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLength(String arg1, long arg2, int arg3) {
        randomlyGC(31, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThrow(String arg1, long arg2) {
        randomlyGC(32, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeCheckCast(String arg1, long arg2, String arg3, long arg4) {
        randomlyGC(33, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInstanceOf(String arg1, long arg2, String arg3, long arg4) {
        randomlyGC(34, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorEnter(String arg1, long arg2) {
        randomlyGC(35, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorExit(String arg1, long arg2) {
        randomlyGC(36, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeBytecode(String arg1, int arg2) {
        randomlyGC(37, 0);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeVirtual(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(27, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(28, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeStatic(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(29, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeInterface(String arg1, long arg2, String arg3, long arg4, String arg5) {
        randomlyGC(30, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(String arg1, long arg2, String arg3, long arg4) {
        randomlyGC(38, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        randomlyGC(39, 1);
    }

    // GENERATED -- EDIT AND RUN GCTestAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        randomlyGC(40, 1);
    }


}
