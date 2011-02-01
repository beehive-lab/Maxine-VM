/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include <malloc.h>
#include <signal.h>
#include <string.h>
#include <dirent.h>
#include <errno.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/user.h>
#include <alloca.h>

#include "log.h"
#include "jni.h"
#include "isa.h"
#include "threads.h"
#include "ptrace.h"

#include "teleProcess.h"
#include "teleNativeThread.h"
#include "linuxTask.h"

boolean task_read_registers(pid_t tid,
    isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
    isa_CanonicalStateRegistersStruct *canonicalStateRegisters,
    isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters) {

    if (canonicalIntegerRegisters != NULL || canonicalStateRegisters != NULL) {
        struct user_regs_struct osIntegerRegisters;
        if (ptrace(PT_GETREGS, tid, 0, &osIntegerRegisters) != 0) {
            return false;
        }
        if (canonicalIntegerRegisters != NULL) {
            isa_canonicalizeTeleIntegerRegisters(&osIntegerRegisters, canonicalIntegerRegisters);
        }
        if (canonicalStateRegisters != NULL) {
            isa_canonicalizeTeleStateRegisters(&osIntegerRegisters, canonicalStateRegisters);
        }
    }

    if (canonicalFloatingPointRegisters != NULL) {
        struct user_fpregs_struct osFloatRegisters;
        if (ptrace(PT_GETFPREGS, tid, 0, &osFloatRegisters) != 0) {
            return false;
        }
        isa_canonicalizeTeleFloatingPointRegisters(&osFloatRegisters, canonicalFloatingPointRegisters);
    }

    return true;
}

ThreadState_t toThreadState(char taskState, pid_t tid) {
    ThreadState_t threadState;
    switch (taskState) {
        case 'W':
        case 'D':
        case 'S': threadState = TS_SLEEPING; break;
        case 'R': threadState = TS_RUNNING; break;
        case 'T': threadState = TS_SUSPENDED; break;
        case 'Z': threadState = TS_DEAD; break;
        default:
            log_println("Unknown task state '%c' for task %d interpreted as thread state TS_DEAD", taskState, tid);
            threadState = TS_DEAD;
            break;
    }
    return threadState;
}

static void gatherThread(JNIEnv *env, pid_t tgid, pid_t tid, jobject linuxTeleProcess, jobject threadList, jlong tlaList, long primordialETLA) {

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;

    char taskState = task_state(tgid, tid);

    TLA tla = 0;
    if (taskState == 'T' && task_read_registers(tid, &canonicalIntegerRegisters, &canonicalStateRegisters, NULL)) {
        Address stackPointer = (Address) canonicalIntegerRegisters.rsp;
        TLA threadLocals = (TLA) alloca(tlaSize());
        NativeThreadLocalsStruct nativeThreadLocalsStruct;
        ProcessHandleStruct ph = {tgid, tid};
        tla = teleProcess_findTLA(&ph, tlaList, primordialETLA, stackPointer, threadLocals, &nativeThreadLocalsStruct);
    }
    teleProcess_jniGatherThread(env, linuxTeleProcess, threadList, tid, toThreadState(taskState, tid), (jlong) canonicalStateRegisters.rip, tla);
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_linux_LinuxNativeTeleChannelProtocol_nativeGatherThreads(JNIEnv *env, jclass c, jlong pid, jobject linuxTeleProcess, jobject threads, long tlaList, long primordialETLA) {

    pid_t *tasks;
    const int nTasks = scan_process_tasks(pid, &tasks);
    if (nTasks < 0) {
        log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
        return;
    }

    int n = 0;
    while (n < nTasks) {
        pid_t tid = tasks[n];
        gatherThread(env, pid, tid, linuxTeleProcess, threads, tlaList, primordialETLA);
        n++;
    }
    free(tasks);
}
