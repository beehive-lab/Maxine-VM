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

static void gatherThread(JNIEnv *env, pid_t tgid, pid_t tid, jobject linuxTeleProcess, jobject threadList, jlong tlaList) {

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;

    char taskState = task_state(tgid, tid);

    TLA tla = 0;
    if (taskState == 'T' && task_read_registers(tid, &canonicalIntegerRegisters, &canonicalStateRegisters, NULL)) {
        Address stackPointer = (Address) canonicalIntegerRegisters.rsp;
        TLA threadLocals = (TLA) alloca(tlaSize());
        NativeThreadLocalsStruct nativeThreadLocalsStruct;
        ProcessHandleStruct ph = {tgid, tid};
        tla = teleProcess_findTLA(&ph, tlaList, stackPointer, threadLocals, &nativeThreadLocalsStruct);
    }
    teleProcess_jniGatherThread(env, linuxTeleProcess, threadList, tid, toThreadState(taskState, tid), (jlong) canonicalStateRegisters.rip, tla);
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_linux_LinuxNativeTeleChannelProtocol_nativeGatherThreads(JNIEnv *env, jclass c, jlong pid, jobject linuxTeleProcess, jobject threads, long tlaList) {

    pid_t *tasks;
    const int nTasks = scan_process_tasks(pid, &tasks);
    if (nTasks < 0) {
        log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
        return;
    }

    int n = 0;
    while (n < nTasks) {
        pid_t tid = tasks[n];
        gatherThread(env, pid, tid, linuxTeleProcess, threads, tlaList);
        n++;
    }
    free(tasks);
}
