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
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.stdid.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vm.ext.vma.store.txt.sbps.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * This variant exploits the fact that the {@link ObjectID}, {@link ClassID} types, etc., are already defined
 * as unique, relatively small integers. So rather than converting to application defined strings
 * and then to opaque short forms of those, as happens in {@link VMAVMLoggerTextStoreAdapter},
 * the ids are used as short forms directly and special calls are made to {@link VMAIdTextStore}.
 * The path is therefore considerably more efficient. However, there is one problem in that the
 * normal "short form" path that maps class, field, method names from their {@code String} form to
 * small integers on first encounter doesn't occur. It would reintroduce much of the the slow path to do the check
 * here so we assume that this has already been handled by {@link VMLogStoreVMAdviceHandler}, which
 * has access to the {@link Actor} forms and can do this efficiently.
 */
public class VMAVMLoggerMaxIdTextStoreAdapter extends VMAVMLoggerStoreAdapter {

    public static class ThisSBPSRawVMATextStore extends SBPSRawVMATextStore {

        private VmThread vmThread;

        public ThisSBPSRawVMATextStore() {
            super();
        }

        protected ThisSBPSRawVMATextStore(String threadName) {
            super(threadName);
        }

        @Override
        public VMATextStore newThread(String threadName) {
            // use the id as the short form
            final String shortThreadName = Integer.toString(vmThread.uuid);
            ThisSBPSRawVMATextStore threadStore = (ThisSBPSRawVMATextStore) super.newThread(shortThreadName);
            threadStore.addThreadShortFormDef(threadName, shortThreadName);
            return threadStore;
        }

        synchronized VMATextStore newThread(VmThread vmThread) {
            this.vmThread = vmThread;
            return newThread(vmThread.getName());
        }

        @Override
        protected ThisSBPSRawVMATextStore createThreadStore(String threadName) {
            return new ThisSBPSRawVMATextStore(threadName);
        }

        @Override
        public void unseenObject(long time, String threadName, long objId, String className, long clId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseAfterNew(long time, String threadName, int bci, long objId, String className, long clId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseAfterNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseAfterMultiNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeGetStatic(long time, String threadName, int bci, String className, long clId, String fieldName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, float value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, double value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, long value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutStaticObject(long time, String threadName, int bci, String className, long clId, String fieldName, long value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeGetField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, float value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, long value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutFieldObject(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, long value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, double value) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeInvokeVirtual(long time, String threadName, int bci, long objId, String className, long clId, String methodName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeInvokeSpecial(long time, String threadName, int bci, long objId, String className, long clId, String methodName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeInvokeStatic(long time, String threadName, int bci, long objId, String className, long clId, String methodName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeInvokeInterface(long time, String threadName, int bci, long objId, String className, long clId, String methodName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeCheckCast(long time, String threadName, int bci, long objId, String className, long clId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseBeforeInstanceOf(long time, String threadName, int bci, long objId, String className, long clId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void adviseAfterMethodEntry(long time, String threadName, int bci, long objId, String className, long clId, String methodName) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * An appropriately typed copy of {@link super#store}.
     */
    private CVMATextStore txtStore;

    public VMAVMLoggerMaxIdTextStoreAdapter(ObjectStateHandler state) {
        super(state);
    }

    protected VMAVMLoggerMaxIdTextStoreAdapter(ObjectStateHandler state, VmThread vmThread, VMAStore threadStore) {
        super(state, vmThread, threadStore);
    }

    @Override
    protected VMAStoreAdapter[] createArray(int length) {
        return new VMAVMLoggerMaxIdTextStoreAdapter[length];
    }

    @Override
    protected VMAStoreAdapter createThreadStoreAdapter(VmThread vmThread) {
        ThisSBPSRawVMATextStore thisStore = (ThisSBPSRawVMATextStore) store;
        VMAStore threadStore = thisStore.newThread(vmThread);
        VMAVMLoggerMaxIdTextStoreAdapter sa = new VMAVMLoggerMaxIdTextStoreAdapter(state, vmThread, threadStore);
        sa.txtStore = (CVMATextStore) threadStore;
        return sa;
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        VMAStoreFactory.setClass(ThisSBPSRawVMATextStore.class);
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            txtStore = (CVMATextStore) store;
        }
    }

    @Override
    public void unseenObject(long time, ObjectID objID, ClassID classID) {
        txtStore.unseenObject(time, objID.toLong(), ClassID.asInt(classID));
    }

    /*
     * We want a fast way to determine if we have output the definition of the given actor
     * to the store. We use bit 0 of the ObjectState bitset to record that the definition has been done.
     * N.B. We are called with the receiver as the global store adapter, so must locate
     * per thread adapter in order to do the definition.
     *
     * This might be overkill but, since class ids are typically 4 figures, owing to the number
     * of classes in the boot image, we could map them down to small integers, as most of the
     * early ids are VM classes. The problem is that it would require MethodID and FieldID to be
     * hacked to use the short form of the class.
     */

    private static final int DEFINED_BIT = 0;

    private boolean isUndefined(Object obj) {
        return state.readBit(obj, DEFINED_BIT) == 0;
    }

    private void setDefined(Object obj) {
        state.writeBit(obj, DEFINED_BIT, 1);
    }

    @Override
    int checkDefineClass(ClassActor ca) {
        if (isUndefined(ca)) {
            VMAVMLoggerMaxIdTextStoreAdapter threadStoreAdapter = (VMAVMLoggerMaxIdTextStoreAdapter) getStoreAdaptorForThread(VmThread.current().uuid);
            threadStoreAdapter.txtStore.addClassShortFormDef(ca.name(), state.readId(ca.classLoader).toLong(), Integer.toString(ca.id));
            setDefined(ca);
        }
        return ca.id;
    }

    @Override
    void checkDefineField(FieldActor fa) {
        checkDefineMember(fa, VMATextStoreFormat.Key.FIELD_DEFINITION, fa.name());
    }

    @Override
    void checkDefineMethod(MethodActor ma) {
        checkDefineMember(ma, VMATextStoreFormat.Key.METHOD_DEFINITION, ma.name());
    }

    void checkDefineMember(MemberActor ma, VMATextStoreFormat.Key key, String name) {
        if (isUndefined(ma)) {
            int caId = checkDefineClass(ma.holder());
            VMAVMLoggerMaxIdTextStoreAdapter threadStoreAdapter = (VMAVMLoggerMaxIdTextStoreAdapter) getStoreAdaptorForThread(VmThread.current().uuid);
            threadStoreAdapter.txtStore.addMemberShortFormDef(key,
                            Integer.toString(caId), name, Integer.toString(ma.memberIndex()));
            setDefined(ma);
        }
    }


// START GENERATED CODE
// EDIT AND RUN VMAVMLoggerMaxIdTextStoreAdapterGenerator.main() TO MODIFY

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
        txtStore.adviseBeforeGetStatic(arg1, arg2, MemberID.getMemberIDAsInt(arg3));
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, ObjectID arg4) {
        txtStore.adviseBeforePutStaticObject(arg1, arg2, MemberID.getMemberIDAsInt(arg3), arg4.toLong());
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, double arg4) {
        txtStore.adviseBeforePutStatic(arg1, arg2, MemberID.getMemberIDAsInt(arg3), arg4);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, long arg4) {
        txtStore.adviseBeforePutStatic(arg1, arg2, MemberID.getMemberIDAsInt(arg3), arg4);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, int arg2, FieldID arg3, float arg4) {
        txtStore.adviseBeforePutStatic(arg1, arg2, MemberID.getMemberIDAsInt(arg3), arg4);
    }

    @Override
    public void adviseBeforeGetField(long arg1, int arg2, ObjectID arg3, FieldID arg4) {
        txtStore.adviseBeforeGetField(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5) {
        txtStore.adviseBeforePutFieldObject(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4), arg5.toLong());
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5) {
        txtStore.adviseBeforePutField(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4), arg5);
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5) {
        txtStore.adviseBeforePutField(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4), arg5);
    }

    @Override
    public void adviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5) {
        txtStore.adviseBeforePutField(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4), arg5);
    }

    @Override
    public void adviseBeforeInvokeVirtual(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        txtStore.adviseBeforeInvokeVirtual(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

    @Override
    public void adviseBeforeInvokeSpecial(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        txtStore.adviseBeforeInvokeSpecial(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

    @Override
    public void adviseBeforeInvokeStatic(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        txtStore.adviseBeforeInvokeStatic(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

    @Override
    public void adviseBeforeInvokeInterface(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        txtStore.adviseBeforeInvokeInterface(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

    @Override
    public void adviseBeforeArrayLength(long arg1, int arg2, ObjectID arg3, int arg4) {
        txtStore.adviseBeforeArrayLength(arg1, null, arg2, arg3.toLong(), arg4);
    }

    @Override
    public void adviseBeforeThrow(long arg1, int arg2, ObjectID arg3) {
        txtStore.adviseBeforeThrow(arg1, null, arg2, arg3.toLong());
    }

    @Override
    public void adviseBeforeCheckCast(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
        txtStore.adviseBeforeCheckCast(arg1, arg2, arg3.toLong(), ClassID.asInt(arg4));
    }

    @Override
    public void adviseBeforeInstanceOf(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
        txtStore.adviseBeforeInstanceOf(arg1, arg2, arg3.toLong(), ClassID.asInt(arg4));
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
        txtStore.adviseAfterNew(arg1, arg2, arg3.toLong(), ClassID.asInt(arg4));
    }

    @Override
    public void adviseAfterNewArray(long arg1, int arg2, ObjectID arg3, ClassID arg4, int arg5) {
        txtStore.adviseAfterNewArray(arg1, arg2, arg3.toLong(), ClassID.asInt(arg4), arg5);
    }

    @Override
    public void adviseAfterMethodEntry(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
        txtStore.adviseAfterMethodEntry(arg1, arg2, arg3.toLong(), MemberID.getMemberIDAsInt(arg4));
    }

// END GENERATED CODE

}
