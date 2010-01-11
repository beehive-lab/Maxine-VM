/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.jni;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Template from which (parts of) {@link JmmFunctions} is generated. The static initializer of
 * {@link JmmFunctions} includes a call to {@link #generate()} to double-check that the source
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
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle ids, int maxDepth, JniHandle infoArray) {
        return 0;
    }

    @VM_ENTRY_POINT
    private static JniHandle GetInputArgumentArray(Pointer env) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryPools(Pointer env, JniHandle mgr) {
        return JniHandle.zero();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryManagers(Pointer env, JniHandle pool) {
        return JniHandle.zero();
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
        return JniHandle.zero();
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
