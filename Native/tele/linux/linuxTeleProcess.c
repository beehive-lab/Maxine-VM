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

#include "log.h"
#include "jni.h"
#include "isa.h"
#include "threads.h"
#include "ptrace.h"

#include "teleProcess.h"
#include "teleNativeThread.h"
#include "linuxTask.h"

void teleProcess_initialize(void) {
}

static void gatherThread(JNIEnv *env, pid_t tgid, pid_t tid, jobject linuxTeleProcess, jobject threadSequence, jlong threadSpecificsListAddress) {
    task_wait_for_state(tgid, tid, "T");

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    task_read_registers(tid, &canonicalIntegerRegisters, NULL, NULL);

    Address stackPointer = (Address) canonicalIntegerRegisters.rsp;
    ThreadSpecificsStruct threadSpecificsStruct;
    ThreadSpecifics threadSpecifics = threadSpecificsList_search(tgid, tid, threadSpecificsListAddress, stackPointer, &threadSpecificsStruct);
    teleProcess_jniGatherThread(env, process, result, threadSequence, state, threadSpecifics);
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTeleProcess_nativeGatherThreads(JNIEnv *env, jobject linuxTeleProcess, jlong pid, jobject threads, long threadSpecificsListAddress) {

    char *taskDirPath;
    asprintf(&taskDirPath, "/proc/%d/task", (pid_t) pid);
    c_ASSERT(taskDirPath != NULL);
    DIR *taskDir = opendir(taskDirPath);
    if (taskDir == NULL) {
        int error = errno;
        log_println("Error opening %d: %s", taskDirPath, strerror(error));
        free(taskDirPath);
        return;
    }

    do {
        errno = 0;
        struct dirent *task = readdir(taskDir);
        if (task == NULL) {
            int error = errno;
            if (error != 0) {
                log_println("Error reading entry in %s directory: %s", taskDirPath, strerror(error));
            }
            break;
        }
        char *endptr;
        pid_t tid = (pid_t) strtol(task->d_name, &endptr, 10);
        if (*endptr == '\0') {
            c_ASSERT(tid > 0);
            if (errno != 0) {
                log_println("Error converting %s to a task id: %s", task->d_name, strerror(errno));
            } else {
                gatherThread(env, pid, tid, linuxTeleProcess, threads, threadSpecificsListAddress);
            }
        }
    } while (true);
    closedir(taskDir);
    if (errno != 0) {
        log_println("Error closing %s directory: %s", taskDirPath, strerror(errno));
    }
    free(taskDirPath);
}
