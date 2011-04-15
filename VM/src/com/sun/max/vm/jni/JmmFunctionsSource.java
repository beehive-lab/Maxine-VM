/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jni;

import java.lang.management.*;

import static com.sun.max.vm.jni.JmmFunctions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.runtime.*;

/**
 * Template from which (parts of) {@link JmmFunctions} is generated. The static initializer of
 * {@link JmmFunctions} includes a call to {@link #compile()} to double-check that the source
 * is up-to-date with respect to any edits made to this class.
 *
 * All the methods annotated by {@link VM_ENTRY_POINT} appear in the exact same order as specified in
 * jni.h. In addition, any methods annotated by {@link VM_ENTRY_POINT} that are declared
 * {@code native} have implementations in jni.c and their entries in the JNI function table
 * are initialized in jni.c.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public final class JmmFunctionsSource {

    private JmmFunctionsSource() {
    }

    // Checkstyle: stop method name check

    @VM_ENTRY_POINT
    private static native void reserved1();

    @VM_ENTRY_POINT
    private static native void reserved2();

    @VM_ENTRY_POINT
    private static native int GetVersion(Pointer env);

    @VM_ENTRY_POINT
    private static native int GetOptionalSupport(Pointer env, Pointer support_ptr);

    @VM_ENTRY_POINT
    private static JniHandle GetInputArguments(Pointer env) {
        return JniHandles.createLocalHandle(RuntimeManagement.getVmArguments());
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle ids, int maxDepth, JniHandle infoArray) {
        final ThreadInfo[] threadInfoArray = (ThreadInfo[]) infoArray.unhand();
        final long[] threadIds = (long[]) ids.unhand();
        ThreadManagement.getThreadInfo(threadIds, maxDepth, threadInfoArray);
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle GetInputArgumentArray(Pointer env) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryPools(Pointer env, JniHandle mgr) {
        final Object p = mgr.unhand();
        assert p ==null; // see sun/management/MemoryImpl.c
        return JniHandles.createLocalHandle(MemoryManagement.getMemoryPools());
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryManagers(Pointer env, JniHandle pool) {
        final Object p = pool.unhand();
        assert p ==null; // see sun/management/MemoryImpl.c
        return JniHandles.createLocalHandle(MemoryManagement.getMemoryManagers());
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryPoolUsage(Pointer env, JniHandle pool) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetPeakMemoryPoolUsage(Pointer env, JniHandle pool) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static native Pointer reserved4();

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryUsage(Pointer env, boolean heap) {
        return JniHandles.createLocalHandle(MemoryManagement.getMemoryUsage(heap));
    }

    @VM_ENTRY_POINT
    private static long GetLongAttribute(Pointer env, JniHandle obj, int att) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static boolean GetBoolAttribute(Pointer env, int att) {
        return false;
    }

    @VM_ENTRY_POINT
    private static boolean SetBoolAttribute(Pointer env, int att, boolean flag) {
        switch (att) {
            case JMM_VERBOSE_GC:
                return MemoryManagement.setVerboseGC(flag);
            case JMM_VERBOSE_CLASS:
                return ClassLoadingManagement.setVerboseClass(flag);
            case JMM_THREAD_CONTENTION_MONITORING:
                return ThreadManagement.setThreadCpuTimeEnabled(flag);
            case JMM_THREAD_CPU_TIME:
                return ThreadManagement.setThreadCpuTimeEnabled(flag);
            default:
                    FatalError.unexpected("unknown attribute value " + att +  "to JmmFunctions.SetBoolAttribute");
        }
        return false;
    }

    @VM_ENTRY_POINT
    private static int GetLongAttributes(Pointer env, JniHandle obj, JniHandle atts, int count, JniHandle result) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle FindCircularBlockedThreads(Pointer env) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static long GetThreadCpuTime(Pointer env, long thread_id) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle GetVMGlobalNames(Pointer env) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static int GetVMGlobals(Pointer env, JniHandle names, Pointer globals, int count) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static int GetInternalThreadTimes(Pointer env, JniHandle names, JniHandle times) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static boolean ResetStatistic(Pointer env, Word obj, int type) {
        return false;
    }

    @VM_ENTRY_POINT
    private static void SetPoolSensor(Pointer env, JniHandle pool, int type, JniHandle sensor) {
    }

    @VM_ENTRY_POINT
    private static long SetPoolThreshold(Pointer env, JniHandle pool, int type, long threshold) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle GetPoolCollectionUsage(Pointer env, JniHandle pool) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static int GetGCExtAttributeInfo(Pointer env, JniHandle mgr, Pointer ext_info, int count) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static void GetLastGCStat(Pointer env, JniHandle mgr, Pointer gc_stat) {
    }

    @VM_ENTRY_POINT
    private static long GetThreadCpuTimeWithKind(Pointer env, long thread_id, boolean user_sys_cpu_time) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static native Pointer reserved5();

    @VM_ENTRY_POINT
    private static int DumpHeap0(Pointer env, JniHandle outputfile, boolean live) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle FindDeadlocks(Pointer env, boolean object_monitors_only) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static void SetVMGlobal(Pointer env, JniHandle flag_name, Word new_value) {
    }

    @VM_ENTRY_POINT
    private static native Word reserved6();

    @VM_ENTRY_POINT
    private static JniHandle DumpThreads(Pointer env, JniHandle ids, boolean lockedMonitors, boolean lockedSynchronizers) {
        return JniHandle.zero();
    }
}
