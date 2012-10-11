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
package com.oracle.max.vm.ext.vma.handlers.store.vmlog.h;

import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;


public class VMAVMLoggerTextStoreAdapter extends TextStoreAdapter {

    /**
     * Handles the mapping from internal object references to external ids and
     * object death callbacks.
     */
    private final ObjectStateHandler state;

    protected VMAVMLoggerTextStoreAdapter(ObjectStateHandler state) {
        super(true, true);
        this.state = state;
    }

    private VMAVMLoggerTextStoreAdapter(ObjectStateHandler state, VmThread vmThread) {
        super(vmThread);
        this.state = state;
    }

    @Override
    protected TextStoreAdapter[] createArray(int length) {
        return new VMAVMLoggerTextStoreAdapter[length];
    }

    @Override
    protected TextStoreAdapter createThreadTextStoreAdapter(VmThread vmThread) {
        return new VMAVMLoggerTextStoreAdapter(state, vmThread);
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            store = VMAStoreFactory.create(perThread);

            if (store == null || !store.initializeStore(threadBatched, perThread)) {
                throw new RuntimeException("VMA store initialization failed");
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (store != null) {
                store.finalizeStore();
            }
        }
    }
    public void unseenObject(long time, ObjectID objID, ClassID classID, ObjectID clID) {
        ClassActor ca = ClassID.toClassActor(classID);
        store.unseenObject(time, null, objID.toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    public void dead(long time, ObjectID id) {
        getStoreAdaptorForThread(VmThread.current().id()).getStore().removal(id.toLong());
    }

// START GENERATED CODE
// EDIT AND RUN VMAVMLoggerTextStoreAdapterGenerator.main() TO MODIFY

    public void adviseBeforeGC(long arg1) {
        store.adviseBeforeGC(arg1, null);
    }

    public void adviseAfterGC(long arg1) {
        store.adviseAfterGC(arg1, null);
    }

    public void adviseBeforeThreadStarting(long arg1) {
        store.adviseBeforeThreadStarting(arg1, null);
    }

    public void adviseBeforeThreadTerminating(long arg1) {
        store.adviseBeforeThreadTerminating(arg1, null);
    }

    public void adviseBeforeReturnByThrow(long arg1, int arg2, ObjectID arg3, int arg4) {
        store.adviseBeforeReturnByThrow(arg1, null, arg2, arg3.toLong(), arg4);
    }

    public void adviseBeforeConstLoad(long arg1, int arg2, long arg3) {
        store.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    public void adviseBeforeConstLoad(long arg1, int arg2, ObjectID arg3) {
        store.adviseBeforeConstLoadObject(arg1, null, arg2, arg3.toLong());
    }

    public void adviseBeforeConstLoad(long arg1, int arg2, float arg3) {
        store.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    public void adviseBeforeConstLoad(long arg1, int arg2, double arg3) {
        store.adviseBeforeConstLoad(arg1, null, arg2, arg3);
    }

    public void adviseBeforeLoad(long arg1, int arg2, int arg3) {
        store.adviseBeforeLoad(arg1, null, arg2, arg3);
    }

    public void adviseBeforeArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4) {
        store.adviseBeforeArrayLoad(arg1, null, arg2, arg3.toLong(), arg4);
    }

    public void adviseBeforeStore(long arg1, int arg2, int arg3, ObjectID arg4) {
        store.adviseBeforeStoreObject(arg1, null, arg2, arg3, arg4.toLong());
    }

    public void adviseBeforeStore(long arg1, int arg2, int arg3, long arg4) {
        store.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeStore(long arg1, int arg2, int arg3, float arg4) {
        store.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeStore(long arg1, int arg2, int arg3, double arg4) {
        store.adviseBeforeStore(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, long arg5) {
        store.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, double arg5) {
        store.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, float arg5) {
        store.adviseBeforeArrayStore(arg1, null, arg2, arg3.toLong(), arg4, arg5);
    }

    public void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
        store.adviseBeforeArrayStoreObject(arg1, null, arg2, arg3.toLong(), arg4, arg5.toLong());
    }

    public void adviseBeforeStackAdjust(long arg1, int arg2, int arg3) {
        store.adviseBeforeStackAdjust(arg1, null, arg2, arg3);
    }

    public void adviseBeforeOperation(long arg1, int arg2, int arg3, double arg4, double arg5) {
        store.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    public void adviseBeforeOperation(long arg1, int arg2, int arg3, long arg4, long arg5) {
        store.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    public void adviseBeforeOperation(long arg1, int arg2, int arg3, float arg4, float arg5) {
        store.adviseBeforeOperation(arg1, null, arg2, arg3, arg4, arg5);
    }

    public void adviseBeforeConversion(long arg1, int arg2, int arg3, float arg4) {
        store.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeConversion(long arg1, int arg2, int arg3, long arg4) {
        store.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeConversion(long arg1, int arg2, int arg3, double arg4) {
        store.adviseBeforeConversion(arg1, null, arg2, arg3, arg4);
    }

    public void adviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        store.adviseBeforeIf(arg1, null, arg2, arg3, arg4, arg5, arg6);
    }

    public void adviseBeforeIf(long arg1, int arg2, int arg3, ObjectID arg4, ObjectID arg5, int arg6) {
        store.adviseBeforeIfObject(arg1, null, arg2, arg3, arg4.toLong(), arg5.toLong(), arg6);
    }

    public void adviseBeforeGoto(long arg1, int arg2, int arg3) {
        store.adviseBeforeGoto(arg1, null, arg2, arg3);
    }

    public void adviseBeforeReturn(long arg1, int arg2, long arg3) {
        store.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    public void adviseBeforeReturn(long arg1, int arg2, ObjectID arg3) {
        store.adviseBeforeReturnObject(arg1, null, arg2, arg3.toLong());
    }

    public void adviseBeforeReturn(long arg1, int arg2, float arg3) {
        store.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    public void adviseBeforeReturn(long arg1, int arg2, double arg3) {
        store.adviseBeforeReturn(arg1, null, arg2, arg3);
    }

    public void adviseBeforeReturn(long arg1, int arg2) {
        store.adviseBeforeReturn(arg1, null, arg2);
    }

    public void adviseBeforeGetStatic(long arg1, int arg2, FieldID arg3) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        store.adviseBeforeGetStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name());
    }

    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, ObjectID arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        store.adviseBeforePutStaticObject(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4.toLong());
    }

    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, double arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        store.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, long arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        store.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, float arg4) {
        FieldActor fa = FieldID.toFieldActor(arg3);
        store.adviseBeforePutStatic(arg1, null, arg2, fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg4);
    }

    public void adviseBeforeGetField(long arg1, int arg2, ObjectID arg3, FieldID arg4) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        store.adviseBeforeGetField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name());
    }

    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        store.adviseBeforePutFieldObject(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5.toLong());
    }

    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        store.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        store.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5) {
        FieldActor fa = FieldID.toFieldActor(arg4);
        store.adviseBeforePutField(arg1, null, arg2, arg3.toLong(), fa.holder().name(), state.readId(fa.holder().classLoader).toLong(), fa.name(), arg5);
    }

    public void adviseBeforeInvokeVirtual(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        store.adviseBeforeInvokeVirtual(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    public void adviseBeforeInvokeSpecial(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        store.adviseBeforeInvokeSpecial(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    public void adviseBeforeInvokeStatic(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        store.adviseBeforeInvokeStatic(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    public void adviseBeforeInvokeInterface(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        store.adviseBeforeInvokeInterface(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

    public void adviseBeforeArrayLength(long arg1, int arg2, ObjectID arg3, int arg4) {
        store.adviseBeforeArrayLength(arg1, null, arg2, arg3.toLong(), arg4);
    }

    public void adviseBeforeThrow(long arg1, int arg2, ObjectID arg3) {
        store.adviseBeforeThrow(arg1, null, arg2, arg3.toLong());
    }

    public void adviseBeforeCheckCast(long arg1, int arg2, ObjectID arg3, ClassID arg4, ObjectID arg5) {
        store.adviseBeforeCheckCast(arg1, null, arg2, arg3.toLong(), ClassID.toClassActor(arg4).name(), arg5.toLong());
    }

    public void adviseBeforeInstanceOf(long arg1, int arg2, ObjectID arg3, ClassID arg4, ObjectID arg5) {
        store.adviseBeforeInstanceOf(arg1, null, arg2, arg3.toLong(), ClassID.toClassActor(arg4).name(), arg5.toLong());
    }

    public void adviseBeforeMonitorEnter(long arg1, int arg2, ObjectID arg3) {
        store.adviseBeforeMonitorEnter(arg1, null, arg2, arg3.toLong());
    }

    public void adviseBeforeMonitorExit(long arg1, int arg2, ObjectID arg3) {
        store.adviseBeforeMonitorExit(arg1, null, arg2, arg3.toLong());
    }

    public void adviseAfterLoad(long arg1, int arg2, int arg3, ObjectID arg4) {
        store.adviseAfterLoadObject(arg1, null, arg2, arg3, arg4.toLong());
    }

    public void adviseAfterArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
        store.adviseAfterArrayLoadObject(arg1, null, arg2, arg3.toLong(), arg4, arg5.toLong());
    }

    public void adviseAfterNew(long arg1, int arg2, ObjectID arg3, ClassID arg4, ObjectID arg5) {
        store.adviseAfterNew(arg1, null, arg2, arg3.toLong(), ClassID.toClassActor(arg4).name(), arg5.toLong());
    }

    public void adviseAfterNewArray(long arg1, int arg2, ObjectID arg3, ClassID arg4, ObjectID arg5, int arg6) {
        store.adviseAfterNewArray(arg1, null, arg2, arg3.toLong(), ClassID.toClassActor(arg4).name(), arg5.toLong(), arg6);
    }

    public void adviseAfterMethodEntry(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        MethodActor ma = MethodID.toMethodActor(arg4);
        ClassActor ca = ma.holder();
        store.adviseAfterMethodEntry(arg1, null, arg2, arg3.toLong(), ca.name(), state.readId(ca.classLoader).toLong(), ma.name());
    }

// END GENERATED CODE
}
