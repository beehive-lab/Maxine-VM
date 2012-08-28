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
 * that it can access the id using the provided implementation of {@link ObjectStateHandler} passed in the constructor.
 *
 * {@link VMATextStore} methods require the name of the generating thread, whereas {@link VMAdviceHandler} methods do
 * not, as it is implicit, so one task of the adaptor is to associate a thread name with an advice call, which is
 * handled by an instance of the {@link ThreadNameGenerator} class. The default implementation assumes the current
 * thread. However, it is possible that intermediate threads are used in the logging, so a different instance
 * can be registered with {@link #setThreadNameGenerator(ThreadNameGenerator).
 *
 * Per-thread adaptors are supported in {@link #perThread per-thread} mode. In this case each adaptor has its
 * own {@link VMATextStore} instance, avoiding any synchronization in the storing process. The caller
 * must announce new threads via the {@link #newThread} method  and subsequently use the returned
 * {@link ThreadVMAdviceHandlerTextStoreAdapter} when adapting records for that thread.
 *
 *
 * The majority of the advice methods are automatically generated as the conversion from VM types to external,
 * string-based, types is a rote process.
 *
 *
 */
public class VMAdviceHandlerTextStoreAdapter implements ObjectStateHandler.RemovalTracker {

    interface ThreadNameGenerator {
        String getThreadName();
    }

    protected static class CurrentThreadNameGenerator implements ThreadNameGenerator {
        @Override
        public final String getThreadName() {
            return VmThread.current().getName();
        }
    }

    private static class ThreadVMAdviceHandlerTextStoreAdapter extends VMAdviceHandlerTextStoreAdapter implements ThreadNameGenerator {
        private final String threadName;

        public ThreadVMAdviceHandlerTextStoreAdapter(VmThread vmThread, ObjectStateHandler state, boolean threadBatched, boolean perThread) {
            super(state, threadBatched, perThread);
            this.threadName = vmThread.getName();
        }

        @Override
        public String getThreadName() {
            // return threadName;
            return null;
        }

    }

    private static VMAdviceHandlerTextStoreAdapter[] storeAdaptors = new VMAdviceHandlerTextStoreAdapter[16];

    /**
     * Handle to the store instance.
     */
    protected VMATextStore store;

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
    private final ObjectStateHandler state;

    /**
     * Denotes whether the log records are batched per thread.
     */
    private final boolean threadBatched;

    /**
     * {@code true} when there are per thread adaptors.
     */
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

    public VMAdviceHandlerTextStoreAdapter getStoreAdaptorForThread(int vmThreadId) {
        if (perThread) {
            return storeAdaptors[vmThreadId];
        } else {
            return this;
        }
    }

    /**
     * In per-thread mode must be called to notify the start of a new thread, typically from
     * {@code adviseBeforeThreadStarting}, but certainly before any {@code adviseXXX} methods of this class are called.
     *
     * @param vmThread
     * @return in per-thread mode a per-thread adaptor, else {@code this}.
     */
    public VMAdviceHandlerTextStoreAdapter newThread(VmThread vmThread) {
        if (perThread) {
            int id = vmThread.id();
            synchronized (storeAdaptors) {
                if (id >= storeAdaptors.length) {
                    VMAdviceHandlerTextStoreAdapter[] newStoreAdaptors = new VMAdviceHandlerTextStoreAdapter[2 * storeAdaptors.length];
                    System.arraycopy(storeAdaptors, 0, newStoreAdaptors, 0, storeAdaptors.length);
                    storeAdaptors = newStoreAdaptors;
                }
            }
            ThreadVMAdviceHandlerTextStoreAdapter sa = new ThreadVMAdviceHandlerTextStoreAdapter(vmThread, state, true, true);
            sa.store = store.newThread(vmThread.getName());
            sa.tng = sa;
            storeAdaptors[id] = sa;
            return sa;
        } else {
            return this;
        }
    }

    /**
     * Must be called in {@link #threadBatched} mode to indicate records now for given thread.
     * @param time
     * @param vmThread
     */
    public void threadSwitch(long time, VmThread vmThread) {
        if (!perThread) {
            store.threadSwitch(time, vmThread.getName());
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

// In the BytecodeAdvice method equivalents below, parameter arg1 is the bci value.

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

    public void adviseBeforeReturnByThrow(long time, int arg1, Throwable arg2, int arg3) {
        store.adviseBeforeReturnByThrow(time, tng.getThreadName(), arg1, state.readId(arg2), arg3);
    }

    public void adviseAfterNew(long time, int arg1, Object arg2) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        store.adviseAfterNew(time, tng.getThreadName(), arg1, state.readId(arg2), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    public void adviseAfterNewArray(long time, int arg1, Object arg2, int arg3) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        store.adviseAfterNewArray(time, tng.getThreadName(), arg1, state.readId(arg2), hub.classActor.name(), state.readId(hub.classActor.classLoader), arg3);
    }

    public void adviseAfterMultiNewArray(long time, int arg1, Object arg2, int[] arg3) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    public void adviseBeforeConstLoad(long time, int arg1, float arg2) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConstLoad(long time, int arg1, double arg2) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeConstLoad(long time, int arg1, Object arg2) {
        store.adviseBeforeConstLoadObject(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseBeforeConstLoad(long time, int arg1, long arg2) {
        store.adviseBeforeConstLoad(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeLoad(long time, int arg1, int arg2) {
        store.adviseBeforeLoad(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeArrayLoad(long time, int arg1, Object arg2, int arg3) {
        store.adviseBeforeArrayLoad(time, tng.getThreadName(), arg1, state.readId(arg2), arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, Object arg3) {
        store.adviseBeforeStoreObject(time, tng.getThreadName(), arg1, arg2, state.readId(arg3));
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, float arg3) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, double arg3) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeStore(long time, int arg1, int arg2, long arg3) {
        store.adviseBeforeStore(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, Object arg4) {
        store.adviseBeforeArrayStoreObject(time, tng.getThreadName(), arg1, state.readId(arg2), arg3, state.readId(arg4));
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, float arg4) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), arg1, state.readId(arg2), arg3, arg4);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, long arg4) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), arg1, state.readId(arg2), arg3, arg4);
    }

    public void adviseBeforeArrayStore(long time, int arg1, Object arg2, int arg3, double arg4) {
        store.adviseBeforeArrayStore(time, tng.getThreadName(), arg1, state.readId(arg2), arg3, arg4);
    }

    public void adviseBeforeStackAdjust(long time, int arg1, int arg2) {
        store.adviseBeforeStackAdjust(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, double arg3, double arg4) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, long arg3, long arg4) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeOperation(long time, int arg1, int arg2, float arg3, float arg4) {
        store.adviseBeforeOperation(time, tng.getThreadName(), arg1, arg2, arg3, arg4);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, long arg3) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, float arg3) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeConversion(long time, int arg1, int arg2, double arg3) {
        store.adviseBeforeConversion(time, tng.getThreadName(), arg1, arg2, arg3);
    }

    public void adviseBeforeIf(long time, int arg1, int arg2, int arg3, int arg4, int arg5) {
        store.adviseBeforeIf(time, tng.getThreadName(), arg1, arg2, arg3, arg4, arg5);
    }

    public void adviseBeforeIf(long time, int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        store.adviseBeforeIfObject(time, tng.getThreadName(), arg1, arg2, state.readId(arg3), state.readId(arg4), arg5);
    }

    public void adviseBeforeGoto(long time, int arg1, int arg2) {
        store.adviseBeforeGoto(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, double arg2) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, long arg2) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, float arg2) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1, arg2);
    }

    public void adviseBeforeReturn(long time, int arg1, Object arg2) {
        store.adviseBeforeReturnObject(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseBeforeReturn(long time, int arg1) {
        store.adviseBeforeReturn(time, tng.getThreadName(), arg1);
    }

    public void adviseBeforeGetStatic(long time, int arg1, Object arg2, int arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        store.adviseBeforeGetStatic(time, tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg3).name());
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, int arg3, float arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        store.adviseBeforePutStatic(time, tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg3).name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, int arg3, double arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        store.adviseBeforePutStatic(time, tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg3).name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, int arg3, long arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        store.adviseBeforePutStatic(time, tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg3).name(), arg4);
    }

    public void adviseBeforePutStatic(long time, int arg1, Object arg2, int arg3, Object arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        store.adviseBeforePutStaticObject(time, tng.getThreadName(), arg1, ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg3).name(), state.readId(arg4));
    }

    public void adviseBeforeGetField(long time, int arg1, Object arg2, int arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        FieldActor fa = ca.findInstanceFieldActor(arg3);
        store.adviseBeforeGetField(time, tng.getThreadName(), arg1, state.readId(arg2), fa.holder().name(), state.readId(ca.classLoader), fa.name());
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, int arg3, float arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        FieldActor fa = ca.findInstanceFieldActor(arg3);
        store.adviseBeforePutField(time, tng.getThreadName(), arg1, state.readId(arg2), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg4);
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, int arg3, long arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        FieldActor fa = ca.findInstanceFieldActor(arg3);
        store.adviseBeforePutField(time, tng.getThreadName(), arg1, state.readId(arg2), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg4);
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, int arg3, Object arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        FieldActor fa = ca.findInstanceFieldActor(arg3);
        store.adviseBeforePutFieldObject(time, tng.getThreadName(), arg1, state.readId(arg2), fa.holder().name(), state.readId(ca.classLoader), fa.name(), state.readId(arg4));
    }

    public void adviseBeforePutField(long time, int arg1, Object arg2, int arg3, double arg4) {
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        FieldActor fa = ca.findInstanceFieldActor(arg3);
        store.adviseBeforePutField(time, tng.getThreadName(), arg1, state.readId(arg2), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg4);
    }

    public void adviseBeforeInvokeVirtual(long time, int arg1, Object arg2, MethodActor arg3) {
        store.adviseBeforeInvokeVirtual(time, tng.getThreadName(), arg1, state.readId(arg2), arg3.holder().name(), state.readId(arg3.holder().classLoader), arg3.name());
    }

    public void adviseBeforeInvokeSpecial(long time, int arg1, Object arg2, MethodActor arg3) {
        store.adviseBeforeInvokeSpecial(time, tng.getThreadName(), arg1, state.readId(arg2), arg3.holder().name(), state.readId(arg3.holder().classLoader), arg3.name());
    }

    public void adviseBeforeInvokeStatic(long time, int arg1, Object arg2, MethodActor arg3) {
        store.adviseBeforeInvokeStatic(time, tng.getThreadName(), arg1, 0, arg3.holder().name(), state.readId(arg3.holder().classLoader), arg3.name());
    }

    public void adviseBeforeInvokeInterface(long time, int arg1, Object arg2, MethodActor arg3) {
        store.adviseBeforeInvokeInterface(time, tng.getThreadName(), arg1, state.readId(arg2), arg3.holder().name(), state.readId(arg3.holder().classLoader), arg3.name());
    }

    public void adviseBeforeArrayLength(long time, int arg1, Object arg2, int arg3) {
        store.adviseBeforeArrayLength(time, tng.getThreadName(), arg1, state.readId(arg2), arg3);
    }

    public void adviseBeforeThrow(long time, int arg1, Object arg2) {
        store.adviseBeforeThrow(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseBeforeCheckCast(long time, int arg1, Object arg2, Object arg3) {
        ClassActor ca = (ClassActor) arg3;
        store.adviseBeforeCheckCast(time, tng.getThreadName(), arg1, state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    public void adviseBeforeInstanceOf(long time, int arg1, Object arg2, Object arg3) {
        ClassActor ca = (ClassActor) arg3;
        store.adviseBeforeInstanceOf(time, tng.getThreadName(), arg1, state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    public void adviseBeforeMonitorEnter(long time, int arg1, Object arg2) {
        store.adviseBeforeMonitorEnter(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseBeforeMonitorExit(long time, int arg1, Object arg2) {
        store.adviseBeforeMonitorExit(time, tng.getThreadName(), arg1, state.readId(arg2));
    }

    public void adviseAfterMethodEntry(long time, int arg1, Object arg2, MethodActor arg3) {
        store.adviseAfterMethodEntry(time, tng.getThreadName(), arg1, state.readId(arg2), arg3.holder().name(), state.readId(arg3.holder().classLoader), arg3.name());
    }

// END GENERATED CODE
}
