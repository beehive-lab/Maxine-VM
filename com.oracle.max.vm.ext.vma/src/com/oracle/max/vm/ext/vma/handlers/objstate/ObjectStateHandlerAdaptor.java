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
package com.oracle.max.vm.ext.vma.handlers.objstate;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.objstate.bitset.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * An adaptor class that handles the state (id, liveness) management for advice handlers.
 *
 * Leaves the actual handling of unseen and removed (dead) objects to subclass.
 *
 * Currently hard-wires {@link SimpleObjectStateHandler} as the state implementation.
 *
 */

public abstract class ObjectStateHandlerAdaptor extends VMAdviceHandler {

    protected ObjectStateHandler state;
    protected ObjectStateHandler.DeadObjectHandler deadObjectHandler;

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if ((phase == MaxineVM.Phase.BOOTSTRAPPING) ||
            (phase == MaxineVM.Phase.RUNNING && state == null)) {
            state = new SimpleObjectStateHandler();
        }
    }

    protected void setDeadObjectHandler(ObjectStateHandler.DeadObjectHandler deadObjectHandler) {
        this.deadObjectHandler = deadObjectHandler;
    }

    /**
     * Notify our specific subclass that a previously unseen object, i.e.,
     * one whose allocation was not seen, has been observed.
     * @param obj
     */
    protected abstract void unseenObject(Object obj);

    /**
     * Ensure that {@code obj} has a valid unique id.
     * @param obj
     */
    private void checkId(Object obj) {
        if (obj != null) {
            long id = state.readId(obj);
            if (id == 0) {
                state.assignUnseenId(obj);
                // check the classloader also
                final Reference objRef = Reference.fromJava(obj);
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
                checkId(hub.classActor.classLoader);
                unseenObject(obj);
            }
        }
    }

    private void checkClassLoaderId(Object staticTuple) {
        checkId(ObjectAccess.readClassActor(staticTuple).classLoader);
    }


    @Override
    public void adviseAfterGC() {
        // (possibly) generate log records for objects that didn't survive this GC
        state.gc(deadObjectHandler);
    }

// START GENERATED CODE
// EDIT AND RUN ObjectStateHandlerAdaptorGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        state.assignId(objRef);
        checkId(hub.classActor.classLoader);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        state.assignId(objRef);
        checkId(hub.classActor.classLoader);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        checkId(arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        checkId(arg2);
        checkId(arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        checkId(arg3);
        checkId(arg4);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, int arg3) {
        checkClassLoaderId(arg2);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, float arg4) {
        checkClassLoaderId(arg2);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, double arg4) {
        checkClassLoaderId(arg2);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, long arg4) {
        checkClassLoaderId(arg2);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, int arg3, Object arg4) {
        checkClassLoaderId(arg2);
        checkId(arg4);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, int arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, float arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, long arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, Object arg4) {
        checkId(arg2);
        checkId(arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, int arg3, double arg4) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        checkId(arg2);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        checkId(arg2);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        checkId(arg2);
    }

// END GENERATED CODE
}
