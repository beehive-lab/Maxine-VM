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
package com.oracle.max.vm.ext.vma.store.txt;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * An adapter that handles the conversion from the signatures of {@link VMAdviceHandler} that use VM internal types to
 * the external representation types used by {@link VMATextStore}.
 *
 * There are no conversion smarts in this adaptor; it just assumes that object id assignment has already been done and
 * that it can access the id using the provided implementation of {@link IdBitSetObjectState} passed in the constructor.
 *
 * {@link VMATextStore} methods require the name of the generating thread, whereas {@link VMAdviceHandler} methods do
 * not, as it is implicit, so one task of the adaptor is to associate a thread name with an advice call, which is
 * handled by an instance of the {@link ThreadNameGenerator} class. The default implementation assumes the current
 * thread. However, it is possible that intermediate threads are used in the logging, so a different instance
 * can be registered with {@link #setThreadNameGenerator(ThreadNameGenerator).
 *
 * The majority of the advice methods are automatically generated as the conversion from VM types to external,
 * string-based, types is a rote process.
 *
 *
 */
public class VMAdviceHandlerTextStoreAdapter extends VMAStoreAdapter {

    interface ThreadNameGenerator {
        String getThreadName();
    }

    protected static class CurrentThreadNameGenerator implements ThreadNameGenerator {
        @Override
        public final String getThreadName() {
            return VmThread.current().getName();
        }
    }

    /**
     * Mechanism for generating the thread name.
     * Default uses the current thread invoking the log method, which
     * is appropriate for synchronous logging.
     */
    protected ThreadNameGenerator tng;

    /**
     * Handles the mapping from internal object references to external ids and
     * object death callbacks.
     */
    private final IdBitSetObjectState state;

    private VMANSFTextStoreIntf txtStore;

    protected void setThreadNameGenerator(ThreadNameGenerator tng) {
        this.tng = tng;
    }

    public VMAdviceHandlerTextStoreAdapter(IdBitSetObjectState state, boolean threadBatched, boolean perThread) {
        super(threadBatched, perThread);
        this.state = state;
    }

    @Override
    protected VMAStoreAdapter[] createArray(int length) {
        return new VMAdviceHandlerTextStoreAdapter[length];
    }

    @Override
    protected VMAStoreAdapter createThreadStoreAdapter(VmThread vmThread) {
        VMAdviceHandlerTextStoreAdapter ta = new VMAdviceHandlerTextStoreAdapter(state, true, true);
        this.vmThread = vmThread;
        this.txtStore = (VMANSFTextStoreIntf) store;
        ta.tng = null;
        return ta;
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            if (tng == null) {
                tng = new CurrentThreadNameGenerator();
            }

            store = VMAStoreFactory.create(perThread);
            txtStore = (VMANSFTextStoreIntf) store;

            if (store == null || !store.initializeStore(threadBatched, perThread, null)) {
                throw new RuntimeException("VMA store initialization failed");
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (store != null) {
                store.finalizeStore();
            }
        }
    }

    public void unseenObject(long time, Object obj) {
        final Reference objRef = Reference.fromJava(obj);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        // no bci field,so pass zero to match signature
        txtStore.unseenObject(time, tng.getThreadName(), 0, state.readId(obj).toLong(), hub.classActor.name(), state.readId(hub.classActor.classLoader).toLong());
    }

// In the BytecodeAdvice method equivalents below, parameter arg1 is the bci value.

// START GENERATED CODE
// EDIT AND RUN VMAdviceHandlerTextStoreAdaptorGenerator.main() TO MODIFY

    public void adviseBeforeGC(long time) {
        txtStore.adviseBeforeGC(time, perThread ? null : tng.getThreadName());
    }

    public void adviseAfterGC(long time) {
        txtStore.adviseAfterGC(time, perThread ? null : tng.getThreadName());
    }

    public void adviseBeforeThreadStarting(long time, VmThread arg1) {
        txtStore.adviseBeforeThreadStarting(time, perThread ? null : tng.getThreadName());
    }

    public void adviseBeforeThreadTerminating(long time, VmThread arg1) {
        txtStore.adviseBeforeThreadTerminating(time, perThread ? null : tng.getThreadName());
    }

    public void adviseBeforeReturnByThrow(long time, int arg1, Throwable arg2, int arg3) {
        txtStore.adviseBeforeReturnByThrow(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3);
    }

    public void adviseAfterNew(long time, int arg1, Object arg2) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        txtStore.adviseAfterNew(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), hub.classActor.name(), state.readId(hub.classActor.classLoader).toLong());
    }

    public void adviseAfterNewArray(long time, int arg1, Object arg2, int arg3) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        txtStore.adviseAfterNewArray(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), hub.classActor.name(), state.readId(hub.classActor.classLoader).toLong(), arg3);
    }

    public void adviseAfterMultiNewArray(long time, int arg1, Object arg2, int[] arg3) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    public void adviseBeforeConstLoad(long time, int arg1, float arg2) {
        txtStore.adviseBeforeConstLoad(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConstLoad(long time, int arg1, double arg2) {
        txtStore.adviseBeforeConstLoad(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConstLoad(long time, int arg1, Object arg2) {
        txtStore.adviseBeforeConstLoadObject(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong());
    }

    public void adviseBeforeConstLoad(long time, int arg1, long arg2) {
        txtStore.adviseBeforeConstLoad(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeLoad(long time, int arg1, int arg2) {
        txtStore.adviseBeforeLoad(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeArrayLoad(long time, int arg1, Object arg2, int arg3) {
        txtStore.adviseBeforeArrayLoad(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, Object arg3) {
        txtStore.adviseBeforeStoreObject(time, perThread ? null : tng.getThreadName(), arg1, arg2, state.readId(arg3).toLong());
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, float arg3) {
        txtStore.adviseBeforeStore(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, double arg3) {
        txtStore.adviseBeforeStore(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, long arg3) {
        txtStore.adviseBeforeStore(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, Object arg4) {
        txtStore.adviseBeforeArrayStoreObject(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3, state.readId(arg4).toLong());
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, float arg4) {
        txtStore.adviseBeforeArrayStore(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3, arg4);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, long arg4) {
        txtStore.adviseBeforeArrayStore(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3, arg4);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, double arg4) {
        txtStore.adviseBeforeArrayStore(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3, arg4);
    }

    public void adviseBeforeStackAdjust(long time, int arg1, int arg2) {
        txtStore.adviseBeforeStackAdjust(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, double arg3, double arg4) {
        txtStore.adviseBeforeOperation(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, long arg3, long arg4) {
        txtStore.adviseBeforeOperation(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, float arg3, float arg4) {
        txtStore.adviseBeforeOperation(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, long arg3) {
        txtStore.adviseBeforeConversion(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, float arg3) {
        txtStore.adviseBeforeConversion(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, double arg3) {
        txtStore.adviseBeforeConversion(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeIf(long time, int arg1, int arg2, int arg3, int arg4, int arg5) {
        txtStore.adviseBeforeIf(time, perThread ? null : tng.getThreadName(), arg1, arg2, arg3, arg4, arg5);
    }

    public void adviseBeforeIf(long time, int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        txtStore.adviseBeforeIfObject(time, perThread ? null : tng.getThreadName(), arg1, arg2, state.readId(arg3).toLong(), state.readId(arg4).toLong(), arg5);
    }

    public void adviseBeforeGoto(long time, int arg1, int arg2) {
        txtStore.adviseBeforeGoto(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, Object arg2) {
        txtStore.adviseBeforeReturnObject(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong());
    }

    public void adviseBeforeReturn(long time, int arg1, long arg2) {
        txtStore.adviseBeforeReturn(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, float arg2) {
        txtStore.adviseBeforeReturn(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, double arg2) {
        txtStore.adviseBeforeReturn(time, perThread ? null : tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1) {
        txtStore.adviseBeforeReturn(time, perThread ? null : tng.getThreadName(), arg1);
    }

    public void adviseBeforeGetStatic(long time, int arg1, Object arg2, FieldActor arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforeGetStatic(time, perThread ? null : tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader).toLong(), arg3.name());
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, FieldActor arg3, float arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutStatic(time, perThread ? null : tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, FieldActor arg3, double arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutStatic(time, perThread ? null : tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, FieldActor arg3, long arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutStatic(time, perThread ? null : tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, FieldActor arg3, Object arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutStaticObject(time, perThread ? null : tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader).toLong(), arg3.name(), state.readId(arg4).toLong());
    }

    public void adviseBeforeGetField(long time, int arg1, Object arg2, FieldActor arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforeGetField(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name());
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, FieldActor arg3, float arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutField(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, FieldActor arg3, long arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutField(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, FieldActor arg3, Object arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutFieldObject(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name(), state.readId(arg4).toLong());
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, FieldActor arg3, double arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        txtStore.adviseBeforePutField(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name(), arg4);
    }

    public void adviseBeforeInvokeVirtual(long time, int arg1, Object arg2, MethodActor arg3) {
        txtStore.adviseBeforeInvokeVirtual(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name());
    }

    public void adviseBeforeInvokeSpecial(long time, int arg1, Object arg2, MethodActor arg3) {
        txtStore.adviseBeforeInvokeSpecial(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name());
    }

    public void adviseBeforeInvokeStatic(long time, int arg1, Object arg2, MethodActor arg3) {
        txtStore.adviseBeforeInvokeStatic(time, perThread ? null : tng.getThreadName(), arg1, 0, arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name());
    }

    public void adviseBeforeInvokeInterface(long time, int arg1, Object arg2, MethodActor arg3) {
        txtStore.adviseBeforeInvokeInterface(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name());
    }

    public void adviseAfterArrayLength(long time, int arg1, Object arg2, int arg3) {
        txtStore.adviseAfterArrayLength(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3);
    }

    public void adviseBeforeThrow(long time, int arg1, Object arg2) {
        txtStore.adviseBeforeThrow(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong());
    }

    public void adviseBeforeCheckCast(long time, int arg1, Object arg2, Object arg3) {
        ClassActor ca = (ClassActor) arg3;
        txtStore.adviseBeforeCheckCast(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    public void adviseBeforeInstanceOf(long time, int arg1, Object arg2, Object arg3) {
        ClassActor ca = (ClassActor) arg3;
        txtStore.adviseBeforeInstanceOf(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), ca.name(), state.readId(ca.classLoader).toLong());
    }

    public void adviseBeforeMonitorEnter(long time, int arg1, Object arg2) {
        txtStore.adviseBeforeMonitorEnter(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong());
    }

    public void adviseBeforeMonitorExit(long time, int arg1, Object arg2) {
        txtStore.adviseBeforeMonitorExit(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong());
    }

    public void adviseAfterLoad(long time, int arg1, int arg2, Object arg3) {
        txtStore.adviseAfterLoadObject(time, perThread ? null : tng.getThreadName(), arg1, arg2, state.readId(arg3).toLong());
    }

    public void adviseAfterArrayLoad(long time, int arg1, Object arg2, int arg3, Object arg4) {
        txtStore.adviseAfterArrayLoadObject(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3, state.readId(arg4).toLong());
    }

    public void adviseAfterMethodEntry(long time, int arg1, Object arg2, MethodActor arg3) {
        txtStore.adviseAfterMethodEntry(time, perThread ? null : tng.getThreadName(), arg1, state.readId(arg2).toLong(), arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name());
    }

// END GENERATED CODE
}
