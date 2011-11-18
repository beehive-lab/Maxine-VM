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

#include "jmm.h"

static void jmm_reserved() {
}

static jint jmm_GetVersion(JNIEnv *env) {
  return JMM_VERSION;
}

static jint jmm_GetOptionalSupport(JNIEnv *env, jmmOptionalSupport* support) {
    return 0;
}

struct jmmInterface_1_ jmm_interface = {
    (void *) jmm_reserved,
    (void *) jmm_reserved,
    jmm_GetVersion,
    jmm_GetOptionalSupport,
    /* jmm_GetInputArguments */ NULL,
    /* jmm_GetThreadInfo */ NULL,
    /* jmm_GetInputArgumentArray */ NULL,
    /* jmm_GetMemoryPools */ NULL,
    /* jmm_GetMemoryManagers */ NULL,
    /* jmm_GetMemoryPoolUsage */ NULL,
    /* jmm_GetPeakMemoryPoolUsage */ NULL,
    (void *) jmm_reserved,
    /* jmm_GetMemoryUsage */ NULL,
    /* jmm_GetLongAttribute */ NULL,
    /* jmm_GetBoolAttribute */ NULL,
    /* jmm_SetBoolAttribute */ NULL,
    /* jmm_GetLongAttributes */ NULL,
    /* jmm_FindMonitorDeadlockedThreads */ NULL,
    /* jmm_GetThreadCpuTime */ NULL,
    /* jmm_GetVMGlobalNames */ NULL,
    /* jmm_GetVMGlobals */ NULL,
    /* jmm_GetInternalThreadTimes */ NULL,
    /* jmm_ResetStatistic */ NULL,
    /* jmm_SetPoolSensor */ NULL,
    /* jmm_SetPoolThreshold */ NULL,
    /* jmm_GetPoolCollectionUsage */ NULL,
    /* jmm_GetGCExtAttributeInfo */ NULL,
    /* jmm_GetLastGCStat */ NULL,
    /* jmm_GetThreadCpuTimeWithKind */ NULL,
    (void *) jmm_reserved,
    /* jmm_DumpHeap0 */ NULL,
    /* jmm_FindDeadlockedThreads */ NULL,
    /* jmm_SetVMGlobal */ NULL,
    (void *) jmm_reserved,
    /* jmm_DumpThreads */ NULL
};

void* getJMMInterface(int version) {
    if (version == -1 || version == JMM_VERSION_1_0) {
        return (void*) &jmm_interface;
    }
    return NULL;
}
