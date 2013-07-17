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
package com.oracle.max.vm.ext.vma.handlers.util.objstate;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * An adaptor for {@link VMAdviceHandler} that handles the unique id management for objects
 * passed as arguments to the advice methods. Handles "unseen" objects by invoking {@link #unseenObject}
 * which must be implemented by the concrete subclass.
 *
 * By default hard-wires {@link SimpleObjectStateHandler} as the {@link ObjectState} implementation,
 * but a subclass can override this by calling {@link #setObjectState(IdBitSetObjectState)}.
 *
 */

public abstract class ObjectStateAdapter extends VMAdviceHandler {

    public static final String STATE_PROPERTY = "max.vma.state.class";

    protected IdBitSetObjectState state;

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if ((phase == MaxineVM.Phase.BOOTSTRAPPING || phase == MaxineVM.Phase.RUNNING) && state == null) {
            state = new SimpleObjectState();
        }
    }

    /**
     * Allows a handler to override the default state implementation.
     * Should be called before {@code super.initialise}.
     * @param state
     */
    protected void setObjectState(IdBitSetObjectState state) {
        this.state = state;
    }

    /**
     * Notify our specific subclass that a previously unseen object, i.e.,
     * one whose allocation was not seen, has been observed.
     * @param obj
     */
    protected abstract void unseenObject(Object obj);

    /**
     * All advice methods that pass application objects invoke this method on the object.
     * The default behavior is to check for an assigned id, but this can be overridden
     * by a subclass. The important distinction between this method and {@link #checkId}
     * itself, is that the latter also checks the id of the class loader associated with the object.
     * N.B. visit is called for New and NewArray even though the default behavior is irrelevant.
     */
    protected void visit(Object obj) {
        checkId(obj);
    }

    /**
     * Ensure that {@code obj} has a valid unique id.
     * @param obj
     */
    private void checkId(Object obj) {
        if (obj != null) {
            ObjectID id = state.readId(obj);
            if (id.isZero()) {
                state.assignUnseenId(obj);
                // check the classloader also
                final Reference objRef = Reference.fromJava(obj);
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
                checkClassLoaderId(hub.classActor);
                unseenObject(obj);
            }
        }
    }

    private void checkClassLoaderId(ClassActor classActor) {
        checkId(classActor.classLoader);
    }

    private void checkClassLoaderIdOfStaticTuple(Object staticTuple) {
        checkClassLoaderId(ObjectAccess.readClassActor(staticTuple));
    }

    private void checkClassLoaderIdOfMemberActor(MemberActor ma) {
        checkClassLoaderId(ma.holder());
    }

    private void checkClassLoaderIdOfClassActor(Object obj) {
        ClassActor ca = UnsafeCast.asClassActor(obj);
        checkId(ca);
    }

// START GENERATED CODE
// EDIT AND RUN ObjectStateAdapterGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        state.assignId(objRef);
        checkClassLoaderId(hub.classActor);
        visit(arg2);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        final Reference objRef = Reference.fromJava(arg2);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        state.assignId(objRef);
        checkClassLoaderId(hub.classActor);
        visit(arg2);
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        visit(arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        visit(arg2);
        visit(arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        visit(arg3);
        visit(arg4);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
        visit(arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

    @Override
    public void adviseAfterArrayLength(int arg1, Object arg2, int arg3) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        visit(arg2);
        checkClassLoaderIdOfClassActor(arg3);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        visit(arg2);
        checkClassLoaderIdOfClassActor(arg3);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        visit(arg2);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        visit(arg2);
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        visit(arg3);
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        visit(arg2);
        visit(arg4);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        visit(arg2);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        visit(arg2);
        checkClassLoaderIdOfMemberActor(arg3);
    }

// END GENERATED CODE
}
