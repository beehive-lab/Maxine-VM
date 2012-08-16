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
import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.sun.max.annotate.*;
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
 * An adapter that handles the conversion from the signatures of {@link VMAdviceHandler} that
 * use VM internal types to the external representation types used by {@link VMATextStore}.
 *
 * There are no "smarts" in this adaptor; it just assumes that object id assignment
 * has already been done and that it can access the id using the provided implementation
 * of {@link ObjectStateHandler} passed in {@link #getRemovalTracker(ObjectStateHandler),
 * which <b>must</b> called by the adapter client before any advice calls occur.
 *
 */
public class VMAdviceHandlerTextStoreAdapter implements ObjectStateHandler.RemovalTracker {

    static abstract class ThreadNameGenerator {
        abstract String getThreadName();
    }

    protected static class CurrentThreadNameGenerator extends ThreadNameGenerator {
        @INLINE
        @Override
        final String getThreadName() {
            return VmThread.current().getName();
        }
    }

    /**
     * Handle to the store instance.
     */
    private VMATextStore store;

    /**
     * Mechanism for generating the thread name.
     * Default used the current thread invoking the log method, which
     * is appropriate for synchronous logging.
     */
    private ThreadNameGenerator tng;

    /**
     * Handles the mapping from internal object references to external ids and
     * object death callbacks. Must be set by caller using {@link #getRemovalTracker()}.
     */
    private final ObjectStateHandler state;

    /**
     * Denotes whether the log records are batched per thread.
     * Default is {@code false}, but can be changed by {@link #setThreadMode(boolean, boolean)}.
     */
    private final boolean threadBatched;

    private final boolean perThread;

    public VMATextStore getStore() {
        return store;
    }

    protected void setThreadNameGenerator(ThreadNameGenerator tng) {
        this.tng = tng;
    }

    public ObjectStateHandler.RemovalTracker getRemovalTracker() {
        return this;
    }

    public VMAdviceHandlerTextStoreAdapter(ObjectStateHandler state, boolean threadBatched, boolean perThread) {
        this.state = state;
        this.threadBatched = threadBatched;
        this.perThread = perThread;
    }

    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.RUNNING) {
            if (tng == null) {
                tng = new CurrentThreadNameGenerator();
            }

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

    public void unseenObject(long time, Object obj) {
        final Reference objRef = Reference.fromJava(obj);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        store.unseenObject(time, tng.getThreadName(), state.readId(obj), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    @Override
    public void removed(long id) {
        store.removal(id);
    }

// START GENERATED CODE
// EDIT AND RUN VMAdviceHandlerTextStoreAdaptorGenerator.main() TO MODIFY

    public void adviseBeforeGC(long time) {
        store.adviseBeforeGC(time, tng.getThreadName());
    }

    public void adviseAfterGC(long time) {
        store.adviseAfterGC(time, tng.getThreadName());
    }

    public void adviseBeforeThreadStarting(long time, VmThread arg1) {
        store.adviseBeforeThreadStarting(time, tng.getThreadName());
    }

    public void adviseBeforeThreadTerminating(long time, VmThread arg1) {
        store.adviseBeforeThreadTerminating(time, tng.getThreadName());
    }

    public void adviseBeforeReturnByThrow(long time, Throwable arg1, int arg2) {
        store.adviseBeforeReturnByThrow(time, tng.getThreadName(), state.readId(arg1), arg2);
    }

    public void adviseAfterNew(long time, Object arg1) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        store.adviseAfterNew(time, tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    public void adviseAfterNewArray(long time, Object arg1, int arg2) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        store.adviseAfterNewArray(time, tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader), arg2);
    }

    public void adviseAfterMultiNewArray(long time, Object arg1, int[] arg2) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    public void adviseBeforeConstLoad(long time, double arg1) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeConstLoad(long time, Object arg1) {
        store.adviseBeforeConstLoadObject(time, tng.getThreadName(), state.readId(arg1));
    }

    public void adviseBeforeConstLoad(long time, long arg1) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeConstLoad(long time, float arg1) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeLoad(long time, int arg1) {
        store.adviseBeforeLoad(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeArrayLoad(long time, Object arg1, int arg2) {
        store.adviseBeforeArrayLoad(time, tng.getThreadName(), state.readId(arg1), arg2);
    }

    public void adviseBeforeStore(long time, int arg1, Object arg2) {
        store.adviseBeforeStoreObject(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseBeforeStore(long time, int arg1, float arg2) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeStore(long time, int arg1, double arg2) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeStore(long time, int arg1, long arg2) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeArrayStore(long time, Object arg1, int arg2, Object arg3) {
        store.adviseBeforeArrayStoreObject(time, tng.getThreadName(), state.readId(arg1), arg2, state.readId(arg3));
    }

    public void adviseBeforeArrayStore(long time, Object arg1, int arg2, float arg3) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    public void adviseBeforeArrayStore(long time, Object arg1, int arg2, long arg3) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    public void adviseBeforeArrayStore(long time, Object arg1, int arg2, double arg3) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    public void adviseBeforeStackAdjust(long time, int arg1) {
        store.adviseBeforeStackAdjust(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeOperation(long time, int arg1, double arg2, double arg3) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeOperation(long time, int arg1, long arg2, long arg3) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeOperation(long time, int arg1, float arg2, float arg3) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeConversion(long time, int arg1, long arg2) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConversion(long time, int arg1, float arg2) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConversion(long time, int arg1, double arg2) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeIf(long time, int arg1, int arg2, int arg3) {
        store.adviseBeforeIf(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeIf(long time, int arg1, Object arg2, Object arg3) {
        store.adviseBeforeIfObject(time, tng.getThreadName(), arg1, state.readId(arg2), state.readId(arg3));
    }

    public void adviseBeforeBytecode(long time, int arg1) {
        store.adviseBeforeBytecode(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeReturn(long time) {
        store.adviseBeforeReturn(time, tng.getThreadName());
    }

    public void adviseBeforeReturn(long time, long arg1) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeReturn(long time, float arg1) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeReturn(long time, double arg1) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeReturn(long time, Object arg1) {
        store.adviseBeforeReturnObject(time, tng.getThreadName(), state.readId(arg1));
    }

    public void adviseBeforeGetStatic(long time, Object arg1, int arg2) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        store.adviseBeforeGetStatic(time, tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    public void adviseBeforePutStatic(long time, Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        store.adviseBeforePutStatic(time, tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    public void adviseBeforePutStatic(long time, Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        store.adviseBeforePutStatic(time, tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    public void adviseBeforePutStatic(long time, Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        store.adviseBeforePutStatic(time, tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    public void adviseBeforePutStatic(long time, Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        store.adviseBeforePutStaticObject(time, tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), state.readId(arg3));
    }

    public void adviseBeforeGetField(long time, Object arg1, int arg2) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        store.adviseBeforeGetField(time, tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name());
    }

    public void adviseBeforePutField(long time, Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        store.adviseBeforePutField(time, tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    public void adviseBeforePutField(long time, Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        store.adviseBeforePutField(time, tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    public void adviseBeforePutField(long time, Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        store.adviseBeforePutFieldObject(time, tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), state.readId(arg3));
    }

    public void adviseBeforePutField(long time, Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        store.adviseBeforePutField(time, tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    public void adviseBeforeInvokeVirtual(long time, Object arg1, MethodActor arg2) {
        store.adviseBeforeInvokeVirtual(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseBeforeInvokeSpecial(long time, Object arg1, MethodActor arg2) {
        store.adviseBeforeInvokeSpecial(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseBeforeInvokeStatic(long time, Object arg1, MethodActor arg2) {
        store.adviseBeforeInvokeStatic(time, tng.getThreadName(), 0, arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseBeforeInvokeInterface(long time, Object arg1, MethodActor arg2) {
        store.adviseBeforeInvokeInterface(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseBeforeArrayLength(long time, Object arg1, int arg2) {
        store.adviseBeforeArrayLength(time, tng.getThreadName(), state.readId(arg1), arg2);
    }

    public void adviseBeforeThrow(long time, Object arg1) {
        store.adviseBeforeThrow(time, tng.getThreadName(), state.readId(arg1));
    }

    public void adviseBeforeCheckCast(long time, Object arg1, Object arg2) {
        ClassActor ca = (ClassActor) arg2;
        store.adviseBeforeCheckCast(time, tng.getThreadName(), state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    public void adviseBeforeInstanceOf(long time, Object arg1, Object arg2) {
        ClassActor ca = (ClassActor) arg2;
        store.adviseBeforeInstanceOf(time, tng.getThreadName(), state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    public void adviseBeforeMonitorEnter(long time, Object arg1) {
        store.adviseBeforeMonitorEnter(time, tng.getThreadName(), state.readId(arg1));
    }

    public void adviseBeforeMonitorExit(long time, Object arg1) {
        store.adviseBeforeMonitorExit(time, tng.getThreadName(), state.readId(arg1));
    }

    public void adviseAfterInvokeVirtual(long time, Object arg1, MethodActor arg2) {
        store.adviseAfterInvokeVirtual(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseAfterInvokeSpecial(long time, Object arg1, MethodActor arg2) {
        store.adviseAfterInvokeSpecial(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseAfterInvokeStatic(long time, Object arg1, MethodActor arg2) {
        store.adviseAfterInvokeStatic(time, tng.getThreadName(), 0, arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseAfterInvokeInterface(long time, Object arg1, MethodActor arg2) {
        store.adviseAfterInvokeInterface(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    public void adviseAfterMethodEntry(long time, Object arg1, MethodActor arg2) {
        store.adviseAfterMethodEntry(time, tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

// END GENERATED CODE
}
