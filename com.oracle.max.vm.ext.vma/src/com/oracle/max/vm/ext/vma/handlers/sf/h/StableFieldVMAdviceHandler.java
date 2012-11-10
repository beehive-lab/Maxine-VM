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
package com.oracle.max.vm.ext.vma.handlers.sf.h;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.max.vm.ext.vma.handlers.util.*;
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * A <i>stable</i> field is one that retains a single value for a long period of time.
 * Evidently, a {@code final} field is the ultimate stable field. However, experimentally,
 * many fields are either not marked {@code final} when they could be, or the field
 * is modified shortly after the constructor exits, by a <i>setter</i>, and then is stable
 * for the rest of its life.
 *
 * This handler attempts to discover such fields.
 *
 * A {@code NEW} is immediately followed by the {@code INVOKESPECIAL} of the constructor.
 * We are interested in determining when the constructor ends since any mutations prior
 * this this point are considered initializers and not mutations.
 *
 * This constructor may call sibling or superclass constructors, invoke arbitrary other methods
 * and, of course, create other objects. Therefore we need to track method entry and return
 * for each {@code NEW} in a thread, detecting the return from the initial {@code INVOKESPECIAL}.
 * This analysis would be slightly easier if we could get <i>after</i> advice for {@code INVOKESPECIAL},
 * but that is currently not possible.
 *
 * There is an offline version of this analysis in the {@code QueryAnalysis} tool.
 * The benefit of an online analysis is that does not require storage of vast quantities
 * of advice traces. The cost is the additional data that is required to be created at
 * runtime.
 *
 */
public class StableFieldVMAdviceHandler extends ObjectStateAdapter {

    private static final int MODIFIED_BIT = 0;
    private static final int UNDER_CONSTRUCTION_BIT = 1;

    private static class ClassData {
        long instances;
        long mcount;
    }

    private static class ObjInit {
        Object obj;
        int callDepth;

    }

    /**
     * Every thread that is being advised has an instance of this.
     */
    private static class InitTracker {
        ObjInit[] stack = new ObjInit[16];
        /**
         * Next free index. {@code zero} means empty.
         */
        int topIndex = 0;

        InitTracker() {
            for (int i = 0; i < stack.length; i++) {
                stack[i] = new ObjInit();
            }
        }

        void push(Object obj) {
            if (topIndex >= stack.length) {
                ObjInit[] newStack = new ObjInit[2 * stack.length];
                System.arraycopy(stack, 0, newStack, 0, stack.length);
                stack = newStack;
            }
            ObjInit objInit = stack[topIndex];
            if (objInit == null) {
                objInit = new ObjInit();
                stack[topIndex] = objInit;
            }
            objInit.callDepth = 0;
            objInit.obj = obj;
            topIndex++;
        }

        boolean empty() {
            return topIndex == 0;
        }

        void pop() {
            topIndex--;
        }

        ObjInit peek() {
            if (topIndex == 0) {
                return null;
            } else {
                return stack[topIndex - 1];
            }
        }
    }

    private static class InitTrackerThreadLocal extends ThreadLocal<InitTracker> {
        @Override
        protected InitTracker initialValue() {
            return new InitTracker();
        }
    }

    private static final InitTrackerThreadLocal initTrackerTL = new InitTrackerThreadLocal();

    private BitSet modified;
    private ConcurrentHashMap<ClassActor, ClassData> classActors;

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new StableFieldVMAdviceHandler());
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.RUNNING) {
            modified = new BitSet(32768);
            classActors = new ConcurrentHashMap<ClassActor, ClassData>();
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            PrintStream ps = System.out;
            for (Map.Entry<ClassActor, ClassData> entry : classActors.entrySet()) {
                ps.printf("%40s %d %d%n", entry.getKey().name(), entry.getValue().instances, entry.getValue().mcount);
            }

            for (int b = 0; b < modified.length(); b++) {

            }
        }
    }

    @Override
    protected void unseenObject(Object obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void adviseBeforeGC() {
    }

    @Override
    public void adviseAfterGC() {
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
        initTrackerTL.get();
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
    }

    private void recordNew(Object obj, boolean isArray) {
        ClassActor ca = ObjectAccess.readClassActor(obj);
        ClassData data = classActors.get(ca);
        if (data == null) {
            data = new ClassData();
            ClassData cData = classActors.putIfAbsent(ca, data);
            if (cData != null) {
                // some other thread got there first
                data = cData;
            }
        }
        data.instances++;
        if (!isArray) {
            state.writeBit(obj, UNDER_CONSTRUCTION_BIT, 1);
            InitTracker it = initTrackerTL.get();
            it.push(obj);
        }
    }

    private void recordPutField(Object obj, FieldActor fieldActor) {
        recordStore(obj);
    }

    private void recordStore(Object obj) {
        if (obj == null) {
            return;
        }
        ObjectID objId = state.readId(obj);
        int id = (int) objId.toLong();
        if (id > 0) {
            if (state.readBit(obj, UNDER_CONSTRUCTION_BIT) == 0) {
                if (state.readBit(obj, MODIFIED_BIT) == 0) {
                    state.writeBit(obj, MODIFIED_BIT, 1);
                    ClassData data = classActors.get(ObjectAccess.readClassActor(obj));
                    data.mcount++;
                }
            }
            modified.set(id);
        }

    }

    @NEVER_INLINE
    private static void debug(Object obj) {

    }

    private void recordArrayStore(Object obj) {
        recordStore(obj);
    }

    /**
     * We check if we are in the context of a constructor for a NEW and if so
     * maintain the call depth count so that we can find the end of the constructor.
     * Would not be necessary if we had {@code AFTER} advice for {@code INVOKESPECIAL}.
     */
    private void recordMethodEntry(Object obj, MethodActor ma) {
        ObjInit objInit = initTrackerTL.get().peek();
        if (objInit != null) {
            objInit.callDepth++;
        }
    }

    /**
     * If we are in the context of a NEW, then check whether the constructor is
     * instrumented. if not, we can't track it.
     */
    private void recordInvokeSpecial(Object obj, MethodActor ma) {
        InitTracker it = initTrackerTL.get();
        ObjInit objInit = it.peek();
        if (objInit != null && objInit.obj == obj && objInit.callDepth == 0) {
            // check if this method is instrumented, if not can't track this NEW
            if (!VMAOptions.instrumentForAdvising((ClassMethodActor) ma)) {
                it.pop();
                state.writeBit(objInit.obj, UNDER_CONSTRUCTION_BIT, 0);
                objInit.obj = null;
            }
        }
    }


    private void recordReturn(int popDepth) {
        InitTracker it = initTrackerTL.get();
        ObjInit objInit = it.peek();
        if (objInit != null) {
            assert objInit.callDepth > 0;
            objInit.callDepth -= popDepth;
            if (objInit.callDepth == 0) {
                it.pop();
                state.writeBit(objInit.obj, UNDER_CONSTRUCTION_BIT, 0);
                objInit.obj = null;
            }
        }
    }

// START GENERATED CODE
// EDIT AND RUN StableFieldVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
        recordReturn(arg3);
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        super.adviseAfterNew(arg1, arg2);
        recordNew(arg2, false);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        super.adviseAfterNewArray(arg1, arg2, arg3);
        recordNew(arg2, true);
        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3);
        recordMethodEntry(arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        recordInvokeSpecial(arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        super.adviseBeforeGoto(arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        super.adviseBeforeReturn(arg1);
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        recordReturn(1);
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        super.adviseBeforeThrow(arg1, arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        super.adviseAfterLoad(arg1, arg2, arg3);
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        adviseAfterNewArray(arg1, arg2, arg3[0]);
    }

// END GENERATED CODE

}
