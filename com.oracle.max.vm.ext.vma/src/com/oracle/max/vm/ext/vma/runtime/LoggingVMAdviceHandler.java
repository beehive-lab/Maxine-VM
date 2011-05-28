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
            return Thread.currentThread().getName();
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
        log.unseenObject(state.readId(obj), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    @Override
    public void gcSurvivor(Pointer cell) {
        ProgramError.unexpected("should not be called");
    }

    protected void removal(long id) {
        log.removal(id);
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseGC(AdviceMode arg1) {
        if (arg1 == AdviceMode.AFTER) {
            log.adviseGC(tng.getThreadName());
        }
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseThreadStarting(AdviceMode arg1, VmThread arg2) {
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseThreadTerminating(AdviceMode arg1, VmThread arg2) {
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(Object arg1) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        log.adviseAfterNew(tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader));
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        final Reference objRef = Reference.fromJava(arg1);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        log.adviseAfterNewArray(tng.getThreadName(), state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader), arg2);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, Object arg3) {
        log.adviseBeforeArrayLoad(tng.getThreadName(), state.readId(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, double arg3) {
        log.adviseBeforeArrayLoad(tng.getThreadName(), state.readId(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, float arg3) {
        log.adviseBeforeArrayLoad(tng.getThreadName(), state.readId(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, long arg3) {
        log.adviseBeforeArrayLoad(tng.getThreadName(), state.readId(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, state.readId(arg3));
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        log.adviseBeforeArrayStore(tng.getThreadName(), state.readId(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforeGetStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforeGetStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforeGetStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforeGetStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), state.readId(arg3));
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        ClassActor ca = ObjectAccess.readClassActor(arg1);
        log.adviseBeforePutStatic(tng.getThreadName(), ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, Object arg3) {
        log.adviseBeforeGetField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, double arg3) {
        log.adviseBeforeGetField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, long arg3) {
        log.adviseBeforeGetField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, float arg3) {
        log.adviseBeforeGetField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name());
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name(), state.readId(arg3));
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        log.adviseBeforePutField(tng.getThreadName(), state.readId(arg1), ObjectAccess.readClassActor(arg1).findInstanceFieldActor(arg2).name(), arg3);
    }

    // GENERATED -- EDIT AND RUN LoggingVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(Object arg1) {
        log.adviseAfterInvokeSpecial(tng.getThreadName(), state.readId(arg1));
    }



}
