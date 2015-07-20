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

import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;


public abstract class VMAVMLoggerStoreAdapter extends VMAStoreAdapter {

    /**
     * Handles the mapping from internal object references to external ids and
     * object death callbacks.
     */
    protected final IdBitSetObjectState state;

    protected VMAVMLoggerStoreAdapter(IdBitSetObjectState state) {
        super(true, true);
        this.state = state;
    }

    protected VMAVMLoggerStoreAdapter(IdBitSetObjectState state, VmThread vmThread, VMAStore threadStore) {
        super(vmThread, threadStore);
        this.state = state;
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            store = VMAStoreFactory.create(perThread);
            if (store == null || !store.initializeStore(threadBatched, perThread, this)) {
                throw new RuntimeException("VMA store initialization failed");
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (store != null) {
                store.finalizeStore();
            }
        }
    }

    public void dead(long time, ObjectID id) {
        getStoreAdaptorForThread(VmThread.current().uuid).getStore().removal(id.toLong());
    }

    public abstract void unseenObject(long time, ObjectID objId, ClassID classId);

    /*
     * The methods exist to support custom definition of these entities by VMAVMLoggerMaxIdStoreAdapter
     */
    int checkDefineClass(ClassActor ca) {
        return 0;
    }

    void checkDefineField(FieldActor fa) {
    }

    void checkDefineMethod(MethodActor ma) {
    }

// START GENERATED CODE
// EDIT AND RUN VMAVMLoggerStoreAdapterGenerator.main() TO MODIFY

    public abstract void adviseBeforeGC(long arg1);
    public abstract void adviseAfterGC(long arg1);
    public abstract void adviseBeforeThreadStarting(long arg1);
    public abstract void adviseBeforeThreadTerminating(long arg1);
    public abstract void adviseBeforeReturnByThrow(long arg1, int arg2, ObjectID arg3, int arg4);
    public abstract void adviseBeforeConstLoad(long arg1, int arg2, long arg3);
    public abstract void adviseBeforeConstLoad(long arg1, int arg2, ObjectID arg3);
    public abstract void adviseBeforeConstLoad(long arg1, int arg2, float arg3);
    public abstract void adviseBeforeConstLoad(long arg1, int arg2, double arg3);
    public abstract void adviseBeforeLoad(long arg1, int arg2, int arg3);
    public abstract void adviseBeforeArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4);
    public abstract void adviseBeforeStore(long arg1, int arg2, int arg3, ObjectID arg4);
    public abstract void adviseBeforeStore(long arg1, int arg2, int arg3, long arg4);
    public abstract void adviseBeforeStore(long arg1, int arg2, int arg3, float arg4);
    public abstract void adviseBeforeStore(long arg1, int arg2, int arg3, double arg4);
    public abstract void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, long arg5);
    public abstract void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, double arg5);
    public abstract void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, float arg5);
    public abstract void adviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5);
    public abstract void adviseBeforeStackAdjust(long arg1, int arg2, int arg3);
    public abstract void adviseBeforeOperation(long arg1, int arg2, int arg3, double arg4, double arg5);
    public abstract void adviseBeforeOperation(long arg1, int arg2, int arg3, long arg4, long arg5);
    public abstract void adviseBeforeOperation(long arg1, int arg2, int arg3, float arg4, float arg5);
    public abstract void adviseBeforeConversion(long arg1, int arg2, int arg3, float arg4);
    public abstract void adviseBeforeConversion(long arg1, int arg2, int arg3, long arg4);
    public abstract void adviseBeforeConversion(long arg1, int arg2, int arg3, double arg4);
    public abstract void adviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5, int arg6);
    public abstract void adviseBeforeIf(long arg1, int arg2, int arg3, ObjectID arg4, ObjectID arg5, int arg6);
    public abstract void adviseBeforeGoto(long arg1, int arg2, int arg3);
    public abstract void adviseBeforeReturn(long arg1, int arg2, long arg3);
    public abstract void adviseBeforeReturn(long arg1, int arg2, ObjectID arg3);
    public abstract void adviseBeforeReturn(long arg1, int arg2, float arg3);
    public abstract void adviseBeforeReturn(long arg1, int arg2, double arg3);
    public abstract void adviseBeforeReturn(long arg1, int arg2);
    public abstract void adviseBeforeGetStatic(long arg1, int arg2, FieldID arg3);
    public abstract void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, ObjectID arg4);
    public abstract void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, double arg4);
    public abstract void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, long arg4);
    public abstract void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, float arg4);
    public abstract void adviseBeforeGetField(long arg1, int arg2, ObjectID arg3, FieldID arg4);
    public abstract void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5);
    public abstract void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5);
    public abstract void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5);
    public abstract void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5);
    public abstract void adviseBeforeInvokeVirtual(long arg1, int arg2, ObjectID arg3, MethodID arg4);
    public abstract void adviseBeforeInvokeSpecial(long arg1, int arg2, ObjectID arg3, MethodID arg4);
    public abstract void adviseBeforeInvokeStatic(long arg1, int arg2, ObjectID arg3, MethodID arg4);
    public abstract void adviseBeforeInvokeInterface(long arg1, int arg2, ObjectID arg3, MethodID arg4);
    public abstract void adviseAfterArrayLength(long arg1, int arg2, ObjectID arg3, int arg4);
    public abstract void adviseBeforeThrow(long arg1, int arg2, ObjectID arg3);
    public abstract void adviseBeforeCheckCast(long arg1, int arg2, ObjectID arg3, ClassID arg4);
    public abstract void adviseBeforeInstanceOf(long arg1, int arg2, ObjectID arg3, ClassID arg4);
    public abstract void adviseBeforeMonitorEnter(long arg1, int arg2, ObjectID arg3);
    public abstract void adviseBeforeMonitorExit(long arg1, int arg2, ObjectID arg3);
    public abstract void adviseAfterLoad(long arg1, int arg2, int arg3, ObjectID arg4);
    public abstract void adviseAfterArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5);
    public abstract void adviseAfterNew(long arg1, int arg2, ObjectID arg3, ClassID arg4);
    public abstract void adviseAfterNewArray(long arg1, int arg2, ObjectID arg3, ClassID arg4, int arg5);
    public abstract void adviseAfterMethodEntry(long arg1, int arg2, ObjectID arg3, MethodID arg4);
// END GENERATED CODE


}
