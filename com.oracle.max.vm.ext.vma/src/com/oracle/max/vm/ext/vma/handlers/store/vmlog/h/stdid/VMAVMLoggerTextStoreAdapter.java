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
package com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.stdid;

import com.oracle.max.vm.ext.vma.handlers.store.sync.h.*;
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.*;
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * Uses a vanilla {@link VMATextStore}, which involves converting back from the ID types
 * to the values expected by {@link VMATextStore}, using the application defined
 * names for threads, classes, fields and methods. This fails to exploit the
 * fact that the {@link ObjectID}, {@link ClassID} types, etc., are already defined
 * as unique, relatively, small integers, but means that the store contents
 * are equivalent to that produced by {@link SyncStoreVMAdviceHandler}.
 */
public class VMAVMLoggerTextStoreAdapter extends VMAVMLoggerStoreAdapter {

    /**
     * An appropriately typed copy of {@link super#store}.
     */
    private VMANSFTextStoreIntf txtStore;

    public VMAVMLoggerTextStoreAdapter(IdBitSetObjectState state) {
        super(state);
    }

    protected VMAVMLoggerTextStoreAdapter(IdBitSetObjectState state, VmThread vmThread, VMAStore threadStore) {
        super(state, vmThread, threadStore);
    }

    @Override
    protected VMAStoreAdapter[] createArray(int length) {
        return new VMAVMLoggerTextStoreAdapter[length];
    }

    @Override
    protected VMAStoreAdapter createThreadStoreAdapter(VmThread vmThread) {
        VMAStore threadStore = store.newThread(vmThread.getName());
        VMAVMLoggerTextStoreAdapter sa = new VMAVMLoggerTextStoreAdapter(state, vmThread, threadStore);
        sa.txtStore = (VMANSFTextStoreIntf) threadStore;
        return sa;
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            txtStore = (VMANSFTextStoreIntf) store;
        }
    }

    @Override
    public void unseenObject(long time, ObjectID objID, ClassID classID) {
        ClassActor ca = ClassID.toClassActor(classID);
        txtStore.unseenObject(time, null, 0, objID.toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

// START GENERATED CODE
// EDIT AND RUN VMAVMLoggerTextStoreAdapterGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeGC(long arg1) {
        txtStore.adviseBeforeGC(arg1, null);
    }

    @Override
    public void adviseAfterGC(long arg1) {
        txtStore.adviseAfterGC(arg1, null);
    }

    @Override
    public void adviseBeforeThreadStarting(long arg1) {
        txtStore.adviseBeforeThreadStarting(arg1, null);
    }

    @Override
    public void adviseBeforeThreadTerminating(long arg1) {
        txtStore.adviseBeforeThreadTerminating(arg1, null);
    }

    @Override
    public void adviseBeforeReturnByThrow(long arg1, int arg2, ObjectID arg3, int arg4) {
        txtStore.adviseBeforeReturnByThrow(arg1, null, arg2, arg3.toLong(), arg4);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, int arg2, long arg3) {
        txtStore.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeConstLoadObject(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, int arg2, float arg3) {
        txtStore.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, int arg2, double arg3) {
        txtStore.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeLoad(long arg1, int arg2, int arg3) {
        txtStore.adviseBeforeLoad(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4) {
        txtStore.adviseBeforeArrayLoad(arg1, null, arg2, arg3.toLong(), arg4);
    }

    @Override
    public void adviseBeforeStore(long arg1, int arg2, int arg3, ObjectID arg4) {
        txtStore.adviseBeforeStoreObject(arg1, null, arg2, arg3, arg4.toLong());
    }

    @Override
    public void adviseBeforeStore(long arg1, int arg2, int arg3, long arg4) {
        txtStore.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeStore(long arg1, int arg2, int arg3, float arg4) {
        txtStore.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeStore(long arg1, int arg2, int arg3, double arg4) {
        txtStore.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, long arg5) {
        txtStore.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, double arg5) {
        txtStore.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, float arg5) {
        txtStore.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
        txtStore.adviseBeforeArrayStoreObject(arg1, null, arg2, arg3.toLong(), arg4, arg5.toLong());
    }

    @Override
    public void adviseBeforeStackAdjust(long arg1, int arg2, int arg3) {
        txtStore.adviseBeforeStackAdjust(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(long arg1, int arg2, int arg3, double arg4, double arg5) {
        txtStore.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeOperation(long arg1, int arg2, int arg3, long arg4, long arg5) {
        txtStore.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeOperation(long arg1, int arg2, int arg3, float arg4, float arg5) {
        txtStore.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeConversion(long arg1, int arg2, int arg3, float arg4) {
        txtStore.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeConversion(long arg1, int arg2, int arg3, long arg4) {
        txtStore.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeConversion(long arg1, int arg2, int arg3, double arg4) {
        txtStore.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        txtStore.adviseBeforeIf(arg1, null, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeIf(long arg1, int arg2, int arg3, ObjectID arg4, ObjectID arg5, int arg6) {
        txtStore.adviseBeforeIfObject(arg1, null, arg2, arg3, arg4.toLong(), arg5.toLong(), arg6);
    }

    @Override
    public void adviseBeforeGoto(long arg1, int arg2, int arg3) {
        txtStore.adviseBeforeGoto(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeReturn(long arg1, int arg2, long arg3) {
        txtStore.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeReturn(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeReturnObject(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseBeforeReturn(long arg1, int arg2, float arg3) {
        txtStore.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeReturn(long arg1, int arg2, double arg3) {
        txtStore.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    @Override
    public void adviseBeforeReturn(long arg1, int arg2) {
        txtStore.adviseBeforeReturn(arg1, null, arg2);
    }

    @Override
    public void adviseBeforeGetStatic(long arg1, int arg2, FieldID arg3) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        txtStore.adviseBeforeGetStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name());
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, ObjectID arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        txtStore.adviseBeforePutStaticObject(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4.toLong());
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, double arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        txtStore.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, long arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        txtStore.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, float arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        txtStore.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    @Override
    public void adviseBeforeGetField(long arg1, int arg2, ObjectID arg3, FieldID arg4) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        txtStore.adviseBeforeGetField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name());
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        txtStore.adviseBeforePutFieldObject(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5.toLong());
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        txtStore.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        txtStore.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        txtStore.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    @Override
    public void adviseBeforeInvokeVirtual(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        txtStore.adviseBeforeInvokeVirtual(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    @Override
    public void adviseBeforeInvokeSpecial(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        txtStore.adviseBeforeInvokeSpecial(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    @Override
    public void adviseBeforeInvokeStatic(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        txtStore.adviseBeforeInvokeStatic(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    @Override
    public void adviseBeforeInvokeInterface(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        txtStore.adviseBeforeInvokeInterface(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    @Override
    public void adviseAfterArrayLength(long arg1, int arg2, ObjectID arg3, int arg4) {
        txtStore.adviseAfterArrayLength(arg1, null, arg2, arg3.toLong(), arg4);
    }

    @Override
    public void adviseBeforeThrow(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeThrow(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseBeforeCheckCast(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
        ClassActor ca = ClassID.toClassActor(arg4);
        txtStore.adviseBeforeCheckCast(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    @Override
    public void adviseBeforeInstanceOf(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
        ClassActor ca = ClassID.toClassActor(arg4);
        txtStore.adviseBeforeInstanceOf(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    @Override
    public void adviseBeforeMonitorEnter(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeMonitorEnter(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseBeforeMonitorExit(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeMonitorExit(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseAfterLoad(long arg1, int arg2, int arg3, ObjectID arg4) {
        txtStore.adviseAfterLoadObject(arg1, null, arg2, arg3, arg4.toLong());
    }

    @Override
    public void adviseAfterArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
        txtStore.adviseAfterArrayLoadObject(arg1, null, arg2, arg3.toLong(), arg4, arg5.toLong());
    }

    @Override
    public void adviseAfterNew(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
        ClassActor ca = ClassID.toClassActor(arg4);
        txtStore.adviseAfterNew(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    @Override
    public void adviseAfterNewArray(long arg1, int arg2, ObjectID arg3, ClassID arg4, int arg5) {
        ClassActor ca = ClassID.toClassActor(arg4);
        txtStore.adviseAfterNewArray(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), arg5);
    }

    @Override
    public void adviseAfterMethodEntry(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        txtStore.adviseAfterMethodEntry(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

// END GENERATED CODE
}
