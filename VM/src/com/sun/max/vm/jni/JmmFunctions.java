/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.jni.JniFunctions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.runtime.*;

/**
 * Upcalls from C that implement the JMM Interface Functions.
 * <p>
 * <b>DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.</b>
 * <p>
 * Instead, modify the corresponding source in JmmFunctionsSource.java denoted by the "// Source: ..." comments.
 * Once finished with editing, execute {@link JniFunctionsGenerator} as a Java application to refresh this file.
 *
 * @see NativeInterfaces
 * @see JmmFunctionsSource
 * @see Native/substrate/jmm.c
 *
 * @author Doug Simon
 */
public final class JmmFunctions {

    public static final int JMM_CLASS_LOADED_COUNT             = 1;    /* Total number of loaded classes */
    public static final int JMM_CLASS_UNLOADED_COUNT           = 2;    /* Total number of unloaded classes */
    public static final int JMM_THREAD_TOTAL_COUNT             = 3;    /* Total number of threads that have been started */
    public static final int JMM_THREAD_LIVE_COUNT              = 4;    /* Current number of live threads */
    public static final int JMM_THREAD_PEAK_COUNT              = 5;    /* Peak number of live threads */
    public static final int JMM_THREAD_DAEMON_COUNT            = 6;    /* Current number of daemon threads */
    public static final int JMM_JVM_INIT_DONE_TIME_MS          = 7;    /* Time when the JVM finished initialization */
    public static final int JMM_COMPILE_TOTAL_TIME_MS          = 8;    /* Total accumulated time spent in compilation */
    public static final int JMM_GC_TIME_MS                     = 9;    /* Total accumulated time spent in collection */
    public static final int JMM_GC_COUNT                       = 10;   /* Total number of collections */

    public static final int JMM_INTERNAL_ATTRIBUTE_INDEX       = 100;
    public static final int JMM_CLASS_LOADED_BYTES             = 101;  /* Number of bytes loaded instance classes */
    public static final int JMM_CLASS_UNLOADED_BYTES           = 102;  /* Number of bytes unloaded instance classes */
    public static final int JMM_TOTAL_CLASSLOAD_TIME_MS        = 103;  /* Accumulated VM class loader time (TraceClassLoadingTime) */
    public static final int JMM_VM_GLOBAL_COUNT                = 104;  /* Number of VM internal flags */
    public static final int JMM_SAFEPOINT_COUNT                = 105;  /* Total number of safepoints */
    public static final int JMM_TOTAL_SAFEPOINTSYNC_TIME_MS    = 106;  /* Accumulated time spent getting to safepoints */
    public static final int JMM_TOTAL_STOPPED_TIME_MS          = 107;  /* Accumulated time spent at safepoints */
    public static final int JMM_TOTAL_APP_TIME_MS              = 108;  /* Accumulated time spent in Java application */
    public static final int JMM_VM_THREAD_COUNT                = 109;  /* Current number of VM internal threads */
    public static final int JMM_CLASS_INIT_TOTAL_COUNT         = 110;  /* Number of classes for which initializers were run */
    public static final int JMM_CLASS_INIT_TOTAL_TIME_MS       = 111;  /* Accumulated time spent in class initializers */
    public static final int JMM_METHOD_DATA_SIZE_BYTES         = 112;  /* Size of method data in memory */
    public static final int JMM_CLASS_VERIFY_TOTAL_TIME_MS     = 113;  /* Accumulated time spent in class verifier */
    public static final int JMM_SHARED_CLASS_LOADED_COUNT      = 114;  /* Number of shared classes loaded */
    public static final int JMM_SHARED_CLASS_UNLOADED_COUNT    = 115;  /* Number of shared classes unloaded */
    public static final int JMM_SHARED_CLASS_LOADED_BYTES      = 116;  /* Number of bytes loaded shared classes */
    public static final int JMM_SHARED_CLASS_UNLOADED_BYTES    = 117;  /* Number of bytes unloaded shared classes */

    public static final int JMM_OS_ATTRIBUTE_INDEX             = 200;
    public static final int JMM_OS_PROCESS_ID                  = 201;  /* Process id of the JVM */
    public static final int JMM_OS_MEM_TOTAL_PHYSICAL_BYTES    = 202;  /* Physical memory size */

    public static final int JMM_GC_EXT_ATTRIBUTE_INFO_SIZE     = 401;  /* the size of the GC specific attributes for a given GC memory manager */

    public static final int JMM_VERBOSE_GC                     = 21;
    public static final int JMM_VERBOSE_CLASS                  = 22;
    public static final int JMM_THREAD_CONTENTION_MONITORING   = 23;
    public static final int JMM_THREAD_CPU_TIME                = 24;
  //} jmmBoolAttribute;


  //enum {
    public static final int JMM_THREAD_STATE_FLAG_SUSPENDED = 0x00100000;
    public static final int JMM_THREAD_STATE_FLAG_NATIVE    = 0x00400000;
  //};

    public static final int JMM_THREAD_STATE_FLAG_MASK = 0xFFF00000;

  //typedef enum {
    public static final int JMM_STAT_PEAK_THREAD_COUNT         = 801;
    public static final int JMM_STAT_THREAD_CONTENTION_COUNT   = 802;
    public static final int JMM_STAT_THREAD_CONTENTION_TIME    = 803;
    public static final int JMM_STAT_THREAD_CONTENTION_STAT    = 804;
    public static final int JMM_STAT_PEAK_POOL_USAGE           = 805;
    public static final int JMM_STAT_GC_STAT                   = 806;
  //} jmmStatisticType;

    public static final int JMM_USAGE_THRESHOLD_HIGH            = 901;
    public static final int JMM_USAGE_THRESHOLD_LOW             = 902;
    public static final int JMM_COLLECTION_USAGE_THRESHOLD_HIGH = 903;
    public static final int JMM_COLLECTION_USAGE_THRESHOLD_LOW  = 904;

  /* Should match what is allowed in globals.hpp */
    public static final int JMM_VMGLOBAL_TYPE_UNKNOWN  = 0;
    public static final int JMM_VMGLOBAL_TYPE_JBOOLEAN = 1;
    public static final int JMM_VMGLOBAL_TYPE_JSTRING  = 2;
    public static final int JMM_VMGLOBAL_TYPE_JLONG    = 3;

    public static final int JMM_VMGLOBAL_ORIGIN_DEFAULT      = 1;   /* Default value */
    public static final int JMM_VMGLOBAL_ORIGIN_COMMAND_LINE = 2;   /* Set at command line (or JNI invocation) */
    public static final int JMM_VMGLOBAL_ORIGIN_MANAGEMENT   = 3;   /* Set via management interface */
    public static final int JMM_VMGLOBAL_ORIGIN_ENVIRON_VAR  = 4;   /* Set via environment variables */
    public static final int JMM_VMGLOBAL_ORIGIN_CONFIG_FILE  = 5;   /* Set via config file (such as .hotspotrc) */
    public static final int JMM_VMGLOBAL_ORIGIN_ERGONOMIC    = 6;   /* Set via ergonomic */
    public static final int JMM_VMGLOBAL_ORIGIN_OTHER        = 99;  /* Set via some other mechanism */

    // Checkstyle: stop method name check

// START GENERATED CODE

    @VM_ENTRY_POINT
    private static native void reserved1();
        // Source: JmmFunctionsSource.java:55

    @VM_ENTRY_POINT
    private static native void reserved2();
        // Source: JmmFunctionsSource.java:58

    @VM_ENTRY_POINT
    private static native int GetVersion(Pointer env);
        // Source: JmmFunctionsSource.java:61

    @VM_ENTRY_POINT
    private static native int GetOptionalSupport(Pointer env, Pointer support_ptr);
        // Source: JmmFunctionsSource.java:64

    @VM_ENTRY_POINT
    private static JniHandle GetInputArguments(Pointer env) {
        // Source: JmmFunctionsSource.java:67
        Pointer anchor = prologue(env, "GetInputArguments");
        try {
            return JniHandles.createLocalHandle(RuntimeManagement.getVmArguments());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetInputArguments");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle ids, int maxDepth, JniHandle infoArray) {
        // Source: JmmFunctionsSource.java:72
        Pointer anchor = prologue(env, "GetThreadInfo");
        try {
            final ThreadInfo[] threadInfoArray = (ThreadInfo[]) infoArray.unhand();
            final long[] threadIds = (long[]) ids.unhand();
            ThreadManagement.getThreadInfo(threadIds, maxDepth, threadInfoArray);
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadInfo");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetInputArgumentArray(Pointer env) {
        // Source: JmmFunctionsSource.java:80
        Pointer anchor = prologue(env, "GetInputArgumentArray");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetInputArgumentArray");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryPools(Pointer env, JniHandle mgr) {
        // Source: JmmFunctionsSource.java:85
        Pointer anchor = prologue(env, "GetMemoryPools");
        try {
            final Object p = mgr.unhand();
            assert p ==null; // see sun/management/MemoryImpl.c
            return JniHandles.createLocalHandle(MemoryManagement.getMemoryPools());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetMemoryPools");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryManagers(Pointer env, JniHandle pool) {
        // Source: JmmFunctionsSource.java:92
        Pointer anchor = prologue(env, "GetMemoryManagers");
        try {
            final Object p = pool.unhand();
            assert p ==null; // see sun/management/MemoryImpl.c
            return JniHandles.createLocalHandle(MemoryManagement.getMemoryManagers());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetMemoryManagers");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryPoolUsage(Pointer env, JniHandle pool) {
        // Source: JmmFunctionsSource.java:99
        Pointer anchor = prologue(env, "GetMemoryPoolUsage");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetMemoryPoolUsage");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetPeakMemoryPoolUsage(Pointer env, JniHandle pool) {
        // Source: JmmFunctionsSource.java:104
        Pointer anchor = prologue(env, "GetPeakMemoryPoolUsage");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetPeakMemoryPoolUsage");
        }
    }

    @VM_ENTRY_POINT
    private static native Pointer reserved4();
        // Source: JmmFunctionsSource.java:109

    @VM_ENTRY_POINT
    private static JniHandle GetMemoryUsage(Pointer env, boolean heap) {
        // Source: JmmFunctionsSource.java:112
        Pointer anchor = prologue(env, "GetMemoryUsage");
        try {
            return JniHandles.createLocalHandle(MemoryManagement.getMemoryUsage(heap));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetMemoryUsage");
        }
    }

    @VM_ENTRY_POINT
    private static long GetLongAttribute(Pointer env, JniHandle obj, int att) {
        // Source: JmmFunctionsSource.java:117
        Pointer anchor = prologue(env, "GetLongAttribute");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLongAttribute");
        }
    }

    @VM_ENTRY_POINT
    private static boolean GetBoolAttribute(Pointer env, int att) {
        // Source: JmmFunctionsSource.java:122
        Pointer anchor = prologue(env, "GetBoolAttribute");
        try {
            return false;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "GetBoolAttribute");
        }
    }

    @VM_ENTRY_POINT
    private static boolean SetBoolAttribute(Pointer env, int att, boolean flag) {
        // Source: JmmFunctionsSource.java:127
        Pointer anchor = prologue(env, "SetBoolAttribute");
        try {
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
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "SetBoolAttribute");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLongAttributes(Pointer env, JniHandle obj, JniHandle atts, int count, JniHandle result) {
        // Source: JmmFunctionsSource.java:144
        Pointer anchor = prologue(env, "GetLongAttributes");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLongAttributes");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle FindCircularBlockedThreads(Pointer env) {
        // Source: JmmFunctionsSource.java:149
        Pointer anchor = prologue(env, "FindCircularBlockedThreads");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "FindCircularBlockedThreads");
        }
    }

    @VM_ENTRY_POINT
    private static long GetThreadCpuTime(Pointer env, long thread_id) {
        // Source: JmmFunctionsSource.java:154
        Pointer anchor = prologue(env, "GetThreadCpuTime");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadCpuTime");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetVMGlobalNames(Pointer env) {
        // Source: JmmFunctionsSource.java:159
        Pointer anchor = prologue(env, "GetVMGlobalNames");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetVMGlobalNames");
        }
    }

    @VM_ENTRY_POINT
    private static int GetVMGlobals(Pointer env, JniHandle names, Pointer globals, int count) {
        // Source: JmmFunctionsSource.java:164
        Pointer anchor = prologue(env, "GetVMGlobals");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetVMGlobals");
        }
    }

    @VM_ENTRY_POINT
    private static int GetInternalThreadTimes(Pointer env, JniHandle names, JniHandle times) {
        // Source: JmmFunctionsSource.java:169
        Pointer anchor = prologue(env, "GetInternalThreadTimes");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetInternalThreadTimes");
        }
    }

    @VM_ENTRY_POINT
    private static boolean ResetStatistic(Pointer env, Word obj, int type) {
        // Source: JmmFunctionsSource.java:174
        Pointer anchor = prologue(env, "ResetStatistic");
        try {
            return false;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return false;
        } finally {
            epilogue(anchor, "ResetStatistic");
        }
    }

    @VM_ENTRY_POINT
    private static void SetPoolSensor(Pointer env, JniHandle pool, int type, JniHandle sensor) {
        // Source: JmmFunctionsSource.java:179
        Pointer anchor = prologue(env, "SetPoolSensor");
        try {
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetPoolSensor");
        }
    }

    @VM_ENTRY_POINT
    private static long SetPoolThreshold(Pointer env, JniHandle pool, int type, long threshold) {
        // Source: JmmFunctionsSource.java:183
        Pointer anchor = prologue(env, "SetPoolThreshold");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetPoolThreshold");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetPoolCollectionUsage(Pointer env, JniHandle pool) {
        // Source: JmmFunctionsSource.java:188
        Pointer anchor = prologue(env, "GetPoolCollectionUsage");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "GetPoolCollectionUsage");
        }
    }

    @VM_ENTRY_POINT
    private static int GetGCExtAttributeInfo(Pointer env, JniHandle mgr, Pointer ext_info, int count) {
        // Source: JmmFunctionsSource.java:193
        Pointer anchor = prologue(env, "GetGCExtAttributeInfo");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetGCExtAttributeInfo");
        }
    }

    @VM_ENTRY_POINT
    private static void GetLastGCStat(Pointer env, JniHandle mgr, Pointer gc_stat) {
        // Source: JmmFunctionsSource.java:198
        Pointer anchor = prologue(env, "GetLastGCStat");
        try {
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "GetLastGCStat");
        }
    }

    @VM_ENTRY_POINT
    private static long GetThreadCpuTimeWithKind(Pointer env, long thread_id, boolean user_sys_cpu_time) {
        // Source: JmmFunctionsSource.java:202
        Pointer anchor = prologue(env, "GetThreadCpuTimeWithKind");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadCpuTimeWithKind");
        }
    }

    @VM_ENTRY_POINT
    private static native Pointer reserved5();
        // Source: JmmFunctionsSource.java:207

    @VM_ENTRY_POINT
    private static int DumpHeap0(Pointer env, JniHandle outputfile, boolean live) {
        // Source: JmmFunctionsSource.java:210
        Pointer anchor = prologue(env, "DumpHeap0");
        try {
            return 0;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "DumpHeap0");
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle FindDeadlocks(Pointer env, boolean object_monitors_only) {
        // Source: JmmFunctionsSource.java:215
        Pointer anchor = prologue(env, "FindDeadlocks");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "FindDeadlocks");
        }
    }

    @VM_ENTRY_POINT
    private static void SetVMGlobal(Pointer env, JniHandle flag_name, Word new_value) {
        // Source: JmmFunctionsSource.java:220
        Pointer anchor = prologue(env, "SetVMGlobal");
        try {
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        } finally {
            epilogue(anchor, "SetVMGlobal");
        }
    }

    @VM_ENTRY_POINT
    private static native Word reserved6();
        // Source: JmmFunctionsSource.java:224

    @VM_ENTRY_POINT
    private static JniHandle DumpThreads(Pointer env, JniHandle ids, boolean lockedMonitors, boolean lockedSynchronizers) {
        // Source: JmmFunctionsSource.java:227
        Pointer anchor = prologue(env, "DumpThreads");
        try {
            return JniHandle.zero();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor, "DumpThreads");
        }
    }
// END GENERATED CODE
}
