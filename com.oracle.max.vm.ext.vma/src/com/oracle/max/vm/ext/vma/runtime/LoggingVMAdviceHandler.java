/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.log.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Implements the advice handling by logging to an implementation of {@link VMAdviceHandlerLog}.
 *
 * There are no "smarts" in this adaptor; it just logs and assumes that object id assignment
 * has already been done and that it can access the id using the provided implementation
 * of {@link ObjectStateHandler}.
 *
 */
public class LoggingVMAdviceHandler extends VMAdviceHandler {

    static abstract class ThreadNameGenerator {
        @INLINE
        abstract String getThreadName();
    }

    protected static class CurrentThreadNameGenerator extends ThreadNameGenerator {
        @INLINE(override = true)
        @Override
        String getThreadName() {
            return VmThread.current().getName();
        }
    }

    /**
     * Handle to the log instance.
     */
    private VMAdviceHandlerLog log;
    private ThreadNameGenerator tng;

    public VMAdviceHandlerLog getLog() {
        return log;
    }

    protected void setThreadNameGenerator(ThreadNameGenerator tng) {
        this.tng = tng;
    }

    @Override
    public void initialise(ObjectStateHandler state) {
        super.initialise(state);
        if (tng == null) {
            tng = new CurrentThreadNameGenerator();
        }

        log = VMAdviceHandlerLogFactory.create();

        if (log == null || !log.initializeLog()) {
            throw new RuntimeException("log creation failed");
        }
    }

    @Override
    public void finalise() {
        if (log != null) {
            log.finalizeLog();
        }
    }

    protected void unseenObject(Object obj) {
        final Reference objRef = Reference.fromJava(obj);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        log.unseenObject(tng.getThreadName(), state.readId(obj), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    @Override
    public void gcSurvivor(Pointer cell) {
        ProgramError.unexpected("should not be called");
    }

    protected void removal(long id) {
        log.removal(id);
    }

// START GENERATED CODE
// EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeGC() {
        log.adviseBeforeGC(tng.getThreadName());
    }

    @Override
    public void adviseAfterGC() {
        log.adviseAfterGC(tng.getThreadName());
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread arg1) {
        log.adviseBeforeThreadStarting(tng.getThreadName());
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread arg1) {
        log.adviseBeforeThreadTerminating(tng.getThreadName());
    }

    @Override
    public void adviseAfterNew(Object arg1) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        log.adviseAfterNew(tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        log.adviseAfterNewArray(tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader), arg2);
    }

    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    @Override
    public void adviseBeforeConstLoad(double arg1) {
        log.adviseBeforeConstLoad(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(float arg1) {
        log.adviseBeforeConstLoad(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1) {
        log.adviseBeforeConstLoad(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        log.adviseBeforeConstLoadObject(tng.getThreadName(), state.readId(arg1));
    }

    @Override
    public void adviseBeforeLoad(int arg1) {
        log.adviseBeforeLoad(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        log.adviseBeforeArrayLoad(tng.getThreadName(), state.readId(arg1), arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        log.adviseBeforeStoreObject(tng.getThreadName(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        log.adviseBeforeStore(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        log.adviseBeforeStore(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        log.adviseBeforeStore(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        log.adviseBeforeArrayStoreObject(tng.getThreadName(), state.readId(arg1), arg2, state.readId(arg3));
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        log.adviseBeforeStackAdjust(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        log.adviseBeforeOperation(tng.getThreadName(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        log.adviseBeforeOperation(tng.getThreadName(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        log.adviseBeforeOperation(tng.getThreadName(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        log.adviseBeforeConversion(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        log.adviseBeforeConversion(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        log.adviseBeforeConversion(tng.getThreadName(), arg1, arg2);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        log.adviseBeforeIf(tng.getThreadName(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        log.adviseBeforeIfObject(tng.getThreadName(), arg1, state.readId(arg2), state.readId(arg3));
    }

    @Override
    public void adviseBeforeReturn(Object arg1) {
        log.adviseBeforeReturnObject(tng.getThreadName(), state.readId(arg1));
    }

    @Override
    public void adviseBeforeReturn() {
        log.adviseBeforeReturn(tng.getThreadName());
    }

    @Override
    public void adviseBeforeReturn(double arg1) {
        log.adviseBeforeReturn(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeReturn(float arg1) {
        log.adviseBeforeReturn(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeReturn(long arg1) {
        log.adviseBeforeReturn(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforeGetStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStaticObject(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), state.readId(arg3));
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        log.adviseBeforeGetField(tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name());
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        log.adviseBeforePutFieldObject(tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), state.readId(arg3));
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        FieldActor fa = ca.findInstanceFieldActor(arg2);
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name(), arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        log.adviseBeforeInvokeVirtual(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        log.adviseBeforeInvokeSpecial(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        log.adviseBeforeInvokeStatic(tng.getThreadName(), 0, arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        log.adviseBeforeInvokeInterface(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        log.adviseBeforeArrayLength(tng.getThreadName(), state.readId(arg1), arg2);
    }

    @Override
    public void adviseBeforeThrow(Object arg1) {
        log.adviseBeforeThrow(tng.getThreadName(), state.readId(arg1));
    }

    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        ClassActor ca = (ClassActor) arg2;
        log.adviseBeforeCheckCast(tng.getThreadName(), state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        ClassActor ca = (ClassActor) arg2;
        log.adviseBeforeInstanceOf(tng.getThreadName(), state.readId(arg1), ca.name(), state.readId(ca.classLoader));
    }

    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        log.adviseBeforeMonitorEnter(tng.getThreadName(), state.readId(arg1));
    }

    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        log.adviseBeforeMonitorExit(tng.getThreadName(), state.readId(arg1));
    }

    @Override
    public void adviseBeforeBytecode(int arg1) {
        log.adviseBeforeBytecode(tng.getThreadName(), arg1);
    }

    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        log.adviseAfterInvokeVirtual(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        log.adviseAfterInvokeSpecial(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        log.adviseAfterInvokeStatic(tng.getThreadName(), 0, arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        log.adviseAfterInvokeInterface(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        log.adviseAfterMethodEntry(tng.getThreadName(), state.readId(arg1), arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name());
    }

// END GENERATED CODE
}
