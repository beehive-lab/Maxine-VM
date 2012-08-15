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
 * Since the {@link VMAdviceHandler} and {@link ObjectStateHandler} implementations are required to be
 * thread safe, this class is not otherwise synchronised.
 *
 * The majority of the methods follow a common pattern so are automatically generated.
 *
 * Can be built into the boot image or dynamically loaded.
 */

public class SyncStoreVMAdviceHandler extends ObjectStateHandlerAdaptor {

    private VMAdviceHandlerTextStoreAdapter storeAdaptor;

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new SyncStoreVMAdviceHandler());
        ObjectStateHandlerAdaptor.forceCompile();
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
            storeAdaptor = new VMAdviceHandlerTextStoreAdapter();
            storeAdaptor.setThreadMode(false, false);
            storeAdaptor.initialise(phase);
            super.setRemovalTracker(storeAdaptor.getRemovalTracker(state));
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            storeAdaptor.initialise(phase);
        }
    }

    @Override
    public void adviseBeforeGC() {
        storeAdaptor.adviseBeforeGC(getTime());
    }

    @Override
    public void adviseAfterGC() {
        // We log the GC first
        storeAdaptor.adviseAfterGC(getTime());
        super.adviseAfterGC();
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
        storeAdaptor.adviseBeforeThreadStarting(getTime(), vmThread);
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
        storeAdaptor.adviseBeforeThreadTerminating(getTime(), vmThread);
    }

// START GENERATED CODE
// EDIT AND RUN SyncStoreVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(Throwable arg1, int arg2) {
        super.adviseBeforeReturnByThrow(arg1, arg2);
        storeAdaptor.adviseBeforeReturnByThrow(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(float arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(double arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeAdaptor.adviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeLoad(int arg1) {
        super.adviseBeforeLoad(arg1);
        storeAdaptor.adviseBeforeLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        super.adviseBeforeArrayLoad(arg1, arg2);
        storeAdaptor.adviseBeforeArrayLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        super.adviseBeforeStore(arg1, arg2);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        super.adviseBeforeStore(arg1, arg2);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        super.adviseBeforeStore(arg1, arg2);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        super.adviseBeforeStore(arg1, arg2);
        storeAdaptor.adviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        super.adviseBeforeStackAdjust(arg1);
        storeAdaptor.adviseBeforeStackAdjust(getTime(), arg1);
    }

    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeAdaptor.adviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeIf(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        storeAdaptor.adviseBeforeIf(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeBytecode(int arg1) {
        super.adviseBeforeBytecode(arg1);
        storeAdaptor.adviseBeforeBytecode(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn() {
        super.adviseBeforeReturn();
        storeAdaptor.adviseBeforeReturn(getTime());
    }

    @Override
    public void adviseBeforeReturn(long arg1) {
        super.adviseBeforeReturn(arg1);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(float arg1) {
        super.adviseBeforeReturn(arg1);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(double arg1) {
        super.adviseBeforeReturn(arg1);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(Object arg1) {
        super.adviseBeforeReturn(arg1);
        storeAdaptor.adviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        super.adviseBeforeGetStatic(arg1, arg2);
        storeAdaptor.adviseBeforeGetStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        super.adviseBeforeGetField(arg1, arg2);
        storeAdaptor.adviseBeforeGetField(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        storeAdaptor.adviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeVirtual(arg1, arg2);
        storeAdaptor.adviseBeforeInvokeVirtual(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeSpecial(arg1, arg2);
        storeAdaptor.adviseBeforeInvokeSpecial(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeStatic(arg1, arg2);
        storeAdaptor.adviseBeforeInvokeStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeInterface(arg1, arg2);
        storeAdaptor.adviseBeforeInvokeInterface(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        super.adviseBeforeArrayLength(arg1, arg2);
        storeAdaptor.adviseBeforeArrayLength(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeThrow(Object arg1) {
        super.adviseBeforeThrow(arg1);
        storeAdaptor.adviseBeforeThrow(getTime(), arg1);
    }

    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        super.adviseBeforeCheckCast(arg1, arg2);
        storeAdaptor.adviseBeforeCheckCast(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        super.adviseBeforeInstanceOf(arg1, arg2);
        storeAdaptor.adviseBeforeInstanceOf(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        super.adviseBeforeMonitorEnter(arg1);
        storeAdaptor.adviseBeforeMonitorEnter(getTime(), arg1);
    }

    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        super.adviseBeforeMonitorExit(arg1);
        storeAdaptor.adviseBeforeMonitorExit(getTime(), arg1);
    }

    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeVirtual(arg1, arg2);
        storeAdaptor.adviseAfterInvokeVirtual(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeSpecial(arg1, arg2);
        storeAdaptor.adviseAfterInvokeSpecial(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeStatic(arg1, arg2);
        storeAdaptor.adviseAfterInvokeStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeInterface(arg1, arg2);
        storeAdaptor.adviseAfterInvokeInterface(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        storeAdaptor.adviseAfterNew(getTime(), arg1);
    }

    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        storeAdaptor.adviseAfterNewArray(getTime(), arg1, arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1);
    }

    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        super.adviseAfterMethodEntry(arg1, arg2);
        storeAdaptor.adviseAfterMethodEntry(getTime(), arg1, arg2);
    }

// END GENERATED CODE
}
