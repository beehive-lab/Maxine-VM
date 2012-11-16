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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.oracle.max.vm.ext.vma.handlers.cbc.h.*;
import com.oracle.max.vm.ext.vma.handlers.util.*;
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vm.ext.vma.handlers.util.tl.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * A <i>stable</i> field is one that retains a single value for a long period of time. Evidently, a {@code final} field
 * is the ultimate stable field. However, experimentally, many fields are either not marked {@code final} when they
 * could be, or the field is modified shortly after the constructor exits, by a <i>setter</i>, and then is stable for
 * the rest of its life.
 *
 * This handler attempts to discover such fields.
 *
 * A {@code NEW} is followed by the {@code INVOKESPECIAL} of the constructor, with possible interleaved execution due to
 * argument evaluation. We are interested in determining when the constructor ends since any mutations prior this this
 * point are considered initializers and not mutations.
 *
 * This constructor may call sibling or superclass constructors, invoke arbitrary other methods and, of course, create
 * other objects. Therefore we need to track method entry and return for each {@code NEW} in a thread, detecting the
 * return from the initial {@code INVOKESPECIAL}. This analysis would be slightly easier if we could get <i>after</i>
 * advice for {@code INVOKESPECIAL}, but that is currently not possible (T1X limitation).
 *
 * Currently this analysis does not scale as it requires an array indexed by {@link ObjectID#toLong()} to record the
 * access/modification time information and , since object ids are not reused, the size of this array needs to be large
 * enough to hold all the objects allocated by the application. The required size of the array can be estimated by
 * running {@link CBCVMAdviceHandler} and looking at the total number of {@code NEW/NEWARRAY} bytecodes.
 *
 * Determining that an object is dead, i.e. not reachable and collected by the GC, depends on support from the garbage
 * collector. Using a weak reference per object is very expensive and puts a large strain on the collector. Optionally,
 * this handler attempts to determine object death by iterating the heap after a GC and detecting objects that did not
 * survive the collection. This is inaccurate for a generational collector unless it is forced into "always full gc"
 * mode, but works reasonably well for the {@link SemiSpaceHeapScheme}. Arguably the time of the "last access" to an
 * object is a more interesting measure. However, it is not equivalent to unreachable, especially for an interactive
 * application.
 *
 * There is an offline version of this analysis in the {@code QueryAnalysis} tool. The benefit of an online analysis is
 * that does not require storage of vast quantities (gigabytes) of advice traces. The cost is the additional data that
 * is required to be created at runtime.
 *
 * This handler should be run with the {@code objectuse} VMA configuration for the most accurate results.
 *
 * Output is sent to the file defined by the property {@link @#DEFAULT_LOGFILE}.
 *
 */
public class StableFieldVMAdviceHandler extends ObjectStateAdapter {

    private static final String IDMAX_PROPERTY = "max.vma.handler.sf.idmax";
    private static final String DEAD_PROPERTY = "max.vma.handler.sf.dead";
    private static final String SUMMARY_PROPERTY = "max.vma.handler.sf.summary";
    private static final String LOGFILE_PROPERTY = "max.vma.handler.sf.file";
    private static final String DEFAULT_LOGFILE = "sfhandler.vma";
    private static final int DEFAULT_LIFETIMES_SIZE = 32768;
    private static final int MODIFIED_BIT = 0;
    private static final int UNDER_CONSTRUCTION_BIT = 1;


    /**
     * Absolute abstract time of the start of the run, used when time is being reported in relative mode.
     */
    private long startTime;
    private static AccessInfo[] accessInfo;

    /**
     * The information stored per object in {@link #accessInfo}.
     * N.B. Access to the {@code lastModified/lastAccessed} fields is unsynchronized.
     */
    private static class AccessInfo {
        int classId;
        /**
         * The time at which the object is constructed, zero means unused.
         * This is the time of the NEW for an array or an object whose constructor
         * we cannot track, otherwise it is the end of the constructor.
         *
         */
        long constructed;
        /**
         * The last time a field or array member was modified, not including during construction.
         */
        long lastModified;
        /**
         * except during GC, 0 means object is alive
         * < 0 after AfterGC analysis means object live at end of last GC.
         * > 0 means object is dead at that GC time
         */
        long death;
        /**
         * This field may not be set and its interpretation can vary.
         * If "object access" bytecodes are advised, then it records the last
         * time the state of the object was accessed. If "object use"
         * bytecodes are advised, then it records the last time the object
         * reference was loaded/stored.
         */
        long lastAccessed;

        /**
         * From end of construction to death.
         */
        long lifeTime() {
            return death - constructed;
        }

        /**
         * From end of construction to last access (or death if not known).
         */
        long effectiveLifeTime() {
            return lastAccessed == 0 ? lifeTime() : lastAccessed - constructed;
        }

        /**
         * From last modification to death.
         */
        long stableLifeTime() {
            return death - (lastModified == 0 ? constructed : lastModified);
        }

        /**
         * From last modification to last access (or death if not known).
         * @return
         */
        long effectiveStableLifeTime() {
            return (lastAccessed == 0 ? stableLifeTime() : lastAccessed) - (lastModified == 0 ? constructed : lastModified);
        }

    }

    public static double percent(long a, long b) {
        if (a == 0) {
            return 0.0;
        } else if (b == 0) {
            return 100.0;
        } else {
            return (double) (a * 100) / (double) b;
        }
    }


    private static class ClassData {
        long instances;
        long mcount;
    }

    private static class ObjInit {
        Object obj;
        int callDepth;
    }

    /**
     * Every thread that is being advised has an instance of this, which is stored
     * in the {@link VMAThreadLocal} area for fast access. It maintains salient information
     * on the call stack during an object constructor execution.
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

    @INTRINSIC(UNSAFE_CAST)
    private static InitTracker asInitTracker(Object obj) {
        return (InitTracker) obj;
    }

    private static InitTracker getInitTracker() {
        return asInitTracker(VMAThreadLocal.get());
    }

    /**
     * {@link JJVMTI} handler to iterate the heap to detect live objects.
     */
    private class AfterGCJVMTIHandler extends NullJJVMTICallbacks implements JJVMTI.HeapCallbacks {

        @Override
        public int heapIteration(Object classTag, long size, Object objectTag, int length, Object userData) {
            assert false;
            return 0;
        }

        @Override
        public int heapIterationMax(Object object, Object userData) {
            long objId = state.readId(object).toLong();
            if (objId > 0) {
                // Mark object as live
                accessInfo[(int) objId].death = -1;
            }
            return JVMTIConstants.JVMTI_VISIT_OBJECTS;
        }

    }

    /**
     * Non-null when we are trying to determine object death by comparing before/after heap state.
     */
    private static AfterGCJVMTIHandler deadHandler;

    private static ConcurrentHashMap<ClassActor, ClassData> classActors;

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new StableFieldVMAdviceHandler());
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.setObjectState(state);
        super.initialise(phase);
        if (phase == MaxineVM.Phase.BOOTSTRAPPING || phase == MaxineVM.Phase.RUNNING) {
            if (classActors == null) {
                classActors = new ConcurrentHashMap<ClassActor, ClassData>();
            }
            if (accessInfo == null) {
                String prop = System.getProperty(IDMAX_PROPERTY);
                int ltSize = DEFAULT_LIFETIMES_SIZE;
                if (prop != null) {
                    ltSize = Integer.parseInt(prop);
                }
                accessInfo = new AccessInfo[ltSize];
                for (int i = 0; i < accessInfo.length; i++) {
                    accessInfo[i] = new AccessInfo();
                }
            }
            String prop = System.getProperty(DEAD_PROPERTY);
            boolean handleDead = prop != null;

            if (handleDead) {
                if (deadHandler == null) {
                    deadHandler = (AfterGCJVMTIHandler) JJVMTIAgentAdapter.register(new AfterGCJVMTIHandler());
                }
            } else {
                deadHandler = null;
            }
            if (phase == MaxineVM.Phase.RUNNING) {
                if (deadHandler != null) {
                    // the heapIteration method must be compiled before iterateThroughHeap is called,
                    // as allocation is disabled inside iterateThroughHeap
                    CompileHelper.forceCompile(AfterGCJVMTIHandler.class, "heapIterationMax", Nature.OPT);
                }
                timeMode = VMAOptions.getTimeMode();
                startTime = timeMode.getTime();
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            long endTime = time();
            PrintStream ps = null;
            try {
                String prop = System.getProperty(LOGFILE_PROPERTY);
                if (prop == null) {
                    prop = DEFAULT_LOGFILE;
                } else {
                    if (prop.length() == 0) {
                        ps = System.out;
                    }
                }
                if (ps == null) {
                    ps = new PrintStream(new FileOutputStream(prop));
                }

                boolean summary = System.getProperty(SUMMARY_PROPERTY) != null;

                for (Map.Entry<ClassActor, ClassData> entry : classActors.entrySet()) {
                    ClassActor ca = entry.getKey();
                    ps.printf("%s, total instances %d, # mutable %d%n", ca.name(), entry.getValue().instances, entry.getValue().mcount);

                    if (!summary) {
                        for (int i = 1; i < accessInfo.length; i++) {
                            AccessInfo lt = accessInfo[i];
                            if (ca.id == lt.classId) {
                                if (lt.constructed > 0) {
                                    // all objects considered dead at end of run
                                    if (lt.death == 0) {
                                        lt.death = endTime;
                                    }
                                    ps.printf("%d: c %d, d %d, lm %d, la %d, stable for %f (%f)%n", i, lt.constructed, lt.death, lt.lastModified, lt.lastAccessed,
                                                    percent(lt.stableLifeTime(), lt.lifeTime()), percent(lt.effectiveStableLifeTime(), lt.effectiveLifeTime()));
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    private VMATimeMode timeMode;

    private long time() {
        long time = timeMode.getTime();
        return timeMode.isAbsolute() ? time : time - startTime;
    }

    @Override
    protected void unseenObject(Object obj) {
        // not interested
    }

    @Override
    public void adviseBeforeGC() {
    }

    @Override
    public void adviseAfterGC() {
        if (deadHandler == null) {
            return;
        }
        long time = time();
        // survivors have their death time set to -1
        deadHandler.iterateThroughHeapMax(0, null, deadHandler, null);
        for (int i = 1; i < accessInfo.length; i++) {
            AccessInfo lt = accessInfo[i];
            if (lt.constructed > 0) {
                if (lt.death == 0) {
                    // hasn't died before but didn't survive last GC
                    lt.death = time;
                } else if (lt.death < 0) {
                    // reset
                    lt.death = 0;
                }
            }
        }
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
        VMAThreadLocal.put(new InitTracker());
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
    }

    public void advanceTime() {
        if (timeMode.canAdvance) {
            timeMode.advance(1);
        }
    }

    private void recordNew(Object obj, boolean isArray) {
        AccessInfo info = accessInfo[(int) state.readId(obj).toLong()];
        info.constructed = time();
        ClassActor ca = ObjectAccess.readClassActor(obj);
        info.classId = ca.id;
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
            InitTracker it = getInitTracker();
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
                AccessInfo info = accessInfo[id];
                long time = time();
                info.lastModified = info.lastAccessed = time;
                if (state.readBit(obj, MODIFIED_BIT) == 0) {
                    state.writeBit(obj, MODIFIED_BIT, 1);
                    ClassData data = classActors.get(ObjectAccess.readClassActor(obj));
                    data.mcount++;
                }
            }
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
        ObjInit objInit = getInitTracker().peek();
        if (initialInit(objInit, obj, ma)) {
            // mark object as under construction
            state.writeBit(objInit.obj, UNDER_CONSTRUCTION_BIT, 1);
            objInit.callDepth++;
        } else if (underConstruction(objInit)) {
            objInit.callDepth++;
        }
    }

    /**
     * Returns {@code true} iff the top object on the init stack is marked with {@link #UNDER_CONSTRUCTION_BIT}.
     */
    private boolean underConstruction(ObjInit objInit) {
        return objInit != null && state.readBit(objInit.obj, UNDER_CONSTRUCTION_BIT) != 0;
    }

    /**
     * Returns {@code true} iff this is the initial call to {@code <init>} for the given object.
     */
    private boolean initialInit(ObjInit objInit, Object obj, MethodActor ma) {
        return objInit != null && objInit.obj == obj && objInit.callDepth == 0 && ma.isInitializer();
    }

    /**
     * If we are in the context of a NEW, then check whether the constructor is
     * instrumented. if not, we can't track it.
     */
    private void recordInvokeSpecial(Object obj, MethodActor ma) {
        InitTracker it = getInitTracker();
        ObjInit objInit = it.peek();
        if (initialInit(objInit, obj, ma)) {
            // Check if this method is instrumented, if not can't track this NEW
            if (!VMAOptions.instrumentForAdvising((ClassMethodActor) ma)) {
                it.pop();
                objInit.obj = null;
            }
            // Can't mark as under construction until we actually enter the <init> method
            // because of (VM) calls that may occur between now and then, that might
            // invoke instrumented JDK methods. This would cause an erroneous
            // match for the end of the constructor in recordReturn because calldepth==0
        } else if (objInit == null || objInit.obj != obj) {
            // Some other object
            recordAccess(obj);
        }
    }

    private void recordAccess(Object obj) {
        ObjectID objId = state.readId(obj);
        int id = (int) objId.toLong();
        if (id > 0) {
            if (state.readBit(obj, UNDER_CONSTRUCTION_BIT) == 0) {
                accessInfo[id].lastAccessed = time();
            }
        }
    }


    private void recordReturn(int popDepth) {
        InitTracker it = getInitTracker();
        ObjInit objInit = it.peek();
        if (underConstruction(objInit)) {
            assert objInit.callDepth > 0;
            objInit.callDepth -= popDepth;
            if (objInit.callDepth == 0) {
                it.pop();
                state.writeBit(objInit.obj, UNDER_CONSTRUCTION_BIT, 0);
                accessInfo[(int) state.readId(objInit.obj).toLong()].constructed = time();
                objInit.obj = null;
            }
        }
    }

// START GENERATED CODE
// EDIT AND RUN StableFieldVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
        advanceTime();
        recordReturn(arg3);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        advanceTime();
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        advanceTime();
        recordAccess(arg3);
        recordAccess(arg4);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        super.adviseBeforeGoto(arg1, arg2);
        advanceTime();
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        advanceTime();
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        super.adviseBeforeReturn(arg1);
        advanceTime();
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        advanceTime();
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        advanceTime();
        recordReturn(1);
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        advanceTime();
        recordReturn(1);
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        advanceTime();
        recordAccess(arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        advanceTime();
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3);
        advanceTime();
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        super.adviseBeforeThrow(arg1, arg2);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        super.adviseAfterLoad(arg1, arg2, arg3);
        advanceTime();
        recordAccess(arg3);
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
        advanceTime();
        recordAccess(arg2);
        recordAccess(arg4);
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        adviseAfterNewArray(arg1, arg2, arg3[0]);
        advanceTime();
        recordAccess(arg2);
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        super.adviseAfterNew(arg1, arg2);
        advanceTime();
        recordNew(arg2, false);
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        super.adviseAfterNewArray(arg1, arg2, arg3);
        advanceTime();
        recordNew(arg2, true);
        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        advanceTime();
        recordPutField(arg2, arg3);
        recordAccess(arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        advanceTime();
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        advanceTime();
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        advanceTime();
        recordPutField(arg2, arg3);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3);
        advanceTime();
        recordMethodEntry(arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        advanceTime();
        recordInvokeSpecial(arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        advanceTime();
        recordArrayStore(arg2);
        recordAccess(arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        advanceTime();
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        advanceTime();
        recordArrayStore(arg2);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        advanceTime();
        recordArrayStore(arg2);
    }

// END GENERATED CODE

}
