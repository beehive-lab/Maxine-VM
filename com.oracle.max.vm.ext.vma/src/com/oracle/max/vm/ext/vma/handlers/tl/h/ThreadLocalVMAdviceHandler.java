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
package com.oracle.max.vm.ext.vma.handlers.tl.h;

import com.oracle.max.vm.ext.vma.handlers.util.*;
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * An in-memory analyzer for thread local objects.
 * Records the thread that created an object in the {@link ObjectState} field
 * and then checks references to objects from other threads.
 */
public class ThreadLocalVMAdviceHandler extends ObjectStateAdapter {

    private static class Data {
        private String threadName;
        private long allocCount;
        private long outAccessCount;
        private long inAccessCount;

        Data(String threadName) {
            this.threadName = threadName;
        }

    }

    private static boolean verbose;
    private static final int REPORTED_BIT = 0;
    private IdBitSetObjectState state;
    private Data[] threadData;

    @Override
    protected void unseenObject(Object obj) {
        ProgramError.unexpected("unseenObject called");
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            state = new SimpleObjectState();
            threadData = new Data[1024];
            threadData[0] = new Data("UNKNOWN");
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            for (int i = 0; i < threadData.length; i++) {
                Data data = threadData[i];
                if (data != null) {
                    Log.print("Thread ");
                    Log.print(data.threadName);
                    Log.print(" alloc count: ");
                    Log.print(data.allocCount);
                    Log.print(", local access by others: ");
                    Log.print(data.inAccessCount);
                    Log.print(", external access by this: ");
                    Log.println(data.outAccessCount);
                }
            }
        }

    }

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new ThreadLocalVMAdviceHandler());
        if (args != null) {
            String[] argsArray = args.split(",");
            for (int i = 0; i < argsArray.length; i++) {
                if (argsArray[i].equals("verbose")) {
                    verbose = true;
                }
            }
        }
    }

    /**
     * Instead of checking that an object id is assigned, we just check for access by
     * a different thread.
     */
    @Override
    protected void checkId(Object obj) {
        if (obj == null) {
            return;
        }
        int cuuid = VmThread.current().uuid;
        int ouuid = (int) state.readId(obj).toLong();
        if (ouuid != cuuid) {
            // non thread local access
            if (state.readBit(obj, REPORTED_BIT) == 0) {
                state.writeBit(obj, REPORTED_BIT, 1);
                Data cdata = threadData[cuuid];
                cdata.outAccessCount++;
                Data odata = threadData[ouuid];
                odata.inAccessCount++;
                if (verbose) {
                    boolean lockDisabledSafepoints = Log.lock();
                    try {
                        Log.print("object ");
                        Log.print(ObjectAccess.readClassActor(obj).name());
                        Log.print(" created by ");
                        Log.print(threadData[ouuid].threadName);
                        Log.print(" is accessed by ");
                        Log.println(cdata.threadName);
                    } finally {
                        Log.unlock(lockDisabledSafepoints);
                    }
                }
            }
        }
    }

    private void recordNew(Object obj) {
        int uuid = VmThread.current().uuid;
        state.writeID(obj, ObjectID.fromWord(Address.fromInt(uuid)));
        Data data = threadData[uuid];
        data.allocCount++;
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
        int uuid = vmThread.uuid;
        if (uuid >= threadData.length) {
            synchronized (threadData) {
                Data[] newThreadNames = new Data[threadData.length * 2];
                System.arraycopy(threadData, 0, newThreadNames, 0, threadData.length);
                threadData = newThreadNames;
            }
        }
        threadData[uuid] = new Data(VmThread.current().getName());
    }

    @Override
    public void adviseBeforeGC() {
    }

    @Override
    public void adviseAfterGC() {
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
    }

// START GENERATED CODE
// EDIT AND RUN ThreadLocalVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        recordNew(arg2);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        recordNew(arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        super.adviseBeforeGoto(arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        super.adviseBeforeReturn(arg1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        super.adviseBeforeThrow(arg1, arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        super.adviseAfterLoad(arg1, arg2, arg3);
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        super.adviseAfterMultiNewArray(arg1, arg2, arg3);
        adviseAfterNewArray(arg1, arg2, arg3[0]);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3);
    }

// END GENERATED CODE
}
