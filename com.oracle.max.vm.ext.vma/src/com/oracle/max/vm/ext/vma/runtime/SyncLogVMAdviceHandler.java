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
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link VMAdviceHandler} that synchronously logs events.
 *
 * Tracking is controlled by the {@link VMAJavaRunScheme#VM_ADVISING} {@link VmThreadLocal}.
 * It is assumed that all calls to the methods of this class are guarded by a check that tracking is enabled,
 * which is controlled by a bit in the {@link VMAJavaRunScheme#VM_ADVISING} {@link VmThreadLocal}.
 * It also assumes that tracking has been disabled to avoid recursive calls while logging.
 *
 * State for unique ids and lifetime tracking is provided by an implementation of the {@link ObjectStateHandler} class,
 * which is created at image build time. All objects have to be checked for having ids, and may log
 * an "unseen" event first.
 *
 * Since the {@link VMAdviceHandler} and {@link ObjectStateHandler} implementations are required to be
 * thread safe, this class is not otherwise synchronised.
 *
 * The majority of the methods follow a common pattern so are automatically generated.
 *
 */

public class SyncLogVMAdviceHandler extends ObjectStateHandlerAdaptor {

    static class LogRemovalTracker extends ObjectStateHandler.RemovalTracker {
        LoggingVMAdviceHandler lta;
        LogRemovalTracker(LoggingVMAdviceHandler lta) {
            this.lta = lta;
        }

        @Override
        public void removed(long id) {
            lta.removal(id);
        }
    }

    private LoggingVMAdviceHandler logHandler;

    @Override
    protected void handleUnseen(Object obj) {
        logHandler.unseenObject(obj);

    }

    @Override
    public void initialise(ObjectStateHandler state) {
        super.initialise(state);
        logHandler = new LoggingVMAdviceHandler();
        logHandler.initialise(state);
        super.setRemovalTracker(new LogRemovalTracker(logHandler));
    }

    @Override
    public void finalise() {
        logHandler.finalise();
    }

    @Override
    public void adviseGC(AdviceMode adviceMode) {
        // We log the GC first
        logHandler.adviseGC(adviceMode);
        super.adviseGC(adviceMode);
    }

    @Override
    public void adviseThreadStarting(AdviceMode adviceMode, VmThread vmThread) {
    }

    @Override
    public void adviseThreadTerminating(AdviceMode adviceMode, VmThread vmThread) {
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        logHandler.adviseBeforeArrayStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        logHandler.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        logHandler.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        logHandler.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        logHandler.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        logHandler.adviseBeforePutStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        logHandler.adviseBeforePutStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        logHandler.adviseBeforePutStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        logHandler.adviseBeforePutStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, long arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        logHandler.adviseBeforeGetField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, float arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        logHandler.adviseBeforeGetField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, double arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        logHandler.adviseBeforeGetField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        logHandler.adviseBeforeGetField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        logHandler.adviseBeforePutField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        logHandler.adviseBeforePutField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        logHandler.adviseBeforePutField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        logHandler.adviseBeforePutField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(Object arg1) {
        super.adviseAfterInvokeSpecial(arg1);
        logHandler.adviseAfterInvokeSpecial(arg1);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        logHandler.adviseAfterNew(arg1);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        logHandler.adviseAfterNewArray(arg1, arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1);
    }

    // GENERATED -- EDIT AND RUN SyncLogVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }


}
