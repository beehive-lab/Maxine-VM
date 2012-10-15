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
package com.oracle.max.vm.ext.vma.handlers.store.sync.h;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.*;
import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link VMAdviceHandler} that synchronously stores events in a {@link VMATextStore}.
 *
 * State for unique ids and lifetime tracking is provided by an implementation of the {@link ObjectStateHandler} class,
 * All objects have to be checked for having ids, and may log an {@link #unseenObject(Object)} event first.
 *
 * Since the store file must be time ordered and the advice time is generated here, the methods are synchronized.
 *
 * The majority of the methods follow a common pattern so are automatically generated.
 *
 * Can be built into the boot image or dynamically loaded.
 */

public class SyncStoreVMAdviceHandler extends ObjectStateHandlerAdaptor {

    private VMAdviceHandlerTextStoreAdapter storeAdaptor;

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new SyncStoreVMAdviceHandler());
    }

    private static long getTime() {
        return System.nanoTime();
    }

    @Override
    protected void unseenObject(Object obj) {
        storeAdaptor.unseenObject(getTime(), obj);
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            storeAdaptor = new VMAdviceHandlerTextStoreAdapter(state, false, false);
            storeAdaptor.initialise(phase);
            super.setDeadObjectHandler(storeAdaptor.getRemovalTracker());
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            synchronized (this) {
                storeAdaptor.initialise(phase);
            }
        }
    }

    @Override
    public synchronized void adviseBeforeGC() {
        storeAdaptor.adviseBeforeGC(getTime());
    }

    @Override
    public synchronized void adviseAfterGC() {
        // We log the GC first
        storeAdaptor.adviseAfterGC(getTime());
        super.adviseAfterGC();
    }

    @Override
    public synchronized void adviseBeforeThreadStarting(VmThread vmThread) {
        storeAdaptor.adviseBeforeThreadStarting(getTime(), vmThread);
    }

    @Override
    public synchronized void adviseBeforeThreadTerminating(VmThread vmThread) {
        storeAdaptor.adviseBeforeThreadTerminating(getTime(), vmThread);
    }

// START GENERATED CODE
// EDIT AND RUN SyncStoreVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public synchronized void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeReturnByThrow(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseAfterNew(int arg1, Object arg2) {
        super.adviseAfterNew(arg1, arg2);
        storeAdaptor.adviseAfterNew(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        super.adviseAfterNewArray(arg1, arg2, arg3);
        storeAdaptor.adviseAfterNewArray(getTime(), arg1, arg2, arg3);
        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        adviseAfterNewArray(arg1, arg2, arg3[0]);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(int arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(int arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(int arg1, Object arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(int arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeLoad(int arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
        storeAdaptor.adviseBeforeLoad(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayLoad(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(int arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(int arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(int arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeStackAdjust(int arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
        storeAdaptor.adviseBeforeStackAdjust(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        storeAdaptor.adviseBeforeIf(getTime(), arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        storeAdaptor.adviseBeforeIf(getTime(), arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeGoto(int arg1, int arg2) {
        super.adviseBeforeGoto(arg1, arg2);
        storeAdaptor.adviseBeforeGoto(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(int arg1, Object arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(int arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(int arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(int arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(int arg1) {
        super.adviseBeforeReturn(arg1);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public synchronized void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeGetStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeGetField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeInvokeVirtual(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeInvokeSpecial(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeInvokeStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeInvokeInterface(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayLength(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeThrow(int arg1, Object arg2) {
        super.adviseBeforeThrow(arg1, arg2);
        storeAdaptor.adviseBeforeThrow(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeCheckCast(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeInstanceOf(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
        storeAdaptor.adviseBeforeMonitorEnter(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeMonitorExit(int arg1, Object arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
        storeAdaptor.adviseBeforeMonitorExit(getTime(), arg1, arg2);
    }

    @Override
    public synchronized void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        super.adviseAfterLoad(arg1, arg2, arg3);
        storeAdaptor.adviseAfterLoad(getTime(), arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
        storeAdaptor.adviseAfterArrayLoad(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3);
        storeAdaptor.adviseAfterMethodEntry(getTime(), arg1, arg2, arg3);
    }

// END GENERATED CODE
}
