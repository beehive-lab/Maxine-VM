/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <alloca.h>
#include <libgen.h>

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "auth.h"
#include "log.h"
#include "ptrace.h"
#include "darwin.h"
#include "jni.h"
#include "word.h"
#include "virtualMemory.h"
#include "threads.h"

#include "teleProcess.h"
#include "teleNativeThread.h"

const char *threadRunStateAsString(int state) {
    switch (state) {
        case TH_STATE_RUNNING: return "RUNNING";
        case TH_STATE_STOPPED: return "STOPPED";
        case TH_STATE_WAITING: return "WAITING";
        case TH_STATE_UNINTERRUPTIBLE: return "UNINTERRUPTIBLE";
        case TH_STATE_HALTED: return "HALTED";
        default: return NULL;
    }
}

boolean log_thread_info(thread_t thread, void *arg) {
    struct thread_basic_info info;
    ThreadState state;
    unsigned int info_count = THREAD_BASIC_INFO_COUNT;
    thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &info_count);
    mach_msg_type_number_t state_count = THREAD_STATE_COUNT;
    thread_get_state(thread, THREAD_STATE_FLAVOR, (natural_t *) &state, &state_count);
    log_print("  Thread[%d]: suspend_count=%d, user_time=%u, system_time=%u, cpu_usage=%d, sp=%p, fp=%p, pc=%p run_state=",
               thread, info.suspend_count, info.user_time, info.system_time, info.cpu_usage, state.__rsp, state.__rbp, state.__rip);

    const char *runState = threadRunStateAsString(info.run_state);
    if (runState == NULL) {
        log_print("%d", info.run_state);
    } else {
        log_print("%s", runState);
    }
    log_println(", flags=0x%x, sleep_time=%d", info.flags, info.system_time);

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;

    thread_read_registers(thread, &canonicalIntegerRegisters, &canonicalFloatingPointRegisters, &canonicalStateRegisters);
    return true;
}

void log_task_info(task_t task) {
    struct task_basic_info info;
    unsigned int info_count = TASK_BASIC_INFO_COUNT;
    task_info(task, TASK_BASIC_INFO, (task_info_t) &info, &info_count);
    log_println("Task[%d]: suspend_count=%d, virtual_size=%u, resident_size=%u, user_time=%u, system_time=%u", task,
                    info.suspend_count, info.virtual_size, info.resident_size, info.user_time, info.system_time);
    forall_threads(task, log_thread_info, NULL);
}

int task_read(task_t task, vm_address_t src, void *dst, size_t size) {
    mach_vm_size_t bytesRead;
    kern_return_t result = mach_vm_read_overwrite(task, src, size, (vm_address_t) dst, &bytesRead);
    return result == KERN_SUCCESS ? (jint) bytesRead : -1;
}


int task_write(task_t task, vm_address_t dst, void *src, size_t size) {
    // check writable (only really needed for setting breakpoints in native code)
    vm_region_submap_short_info_data_64_t info;
    mach_vm_address_t dst_base = dst;
    mach_msg_type_number_t count;
    mach_vm_size_t region_length;
    natural_t region_depth;

    // check for write protection
    region_depth = 100000;
    count = VM_REGION_SUBMAP_SHORT_INFO_COUNT_64;
    kern_return_t result = mach_vm_region_recurse(task, &dst_base, &region_length, &region_depth,
                                  (vm_region_recurse_info_t) &info, &count);
    REPORT_MACH_ERROR("mach_vm_region_recurse", result);

    // try to increase max protection if necessary
    if (!(info.max_protection & VM_PROT_WRITE)) {
        result = mach_vm_protect(task, dst, region_length, TRUE, info.max_protection | VM_PROT_WRITE | VM_PROT_COPY);
        REPORT_MACH_ERROR("mach_vm_protect max", result);
        if (result != KERN_SUCCESS) return -1;
    }
    // try to increase current protection
    if (!(info.protection & VM_PROT_WRITE)) {
        result = mach_vm_protect(task, dst, region_length, FALSE, info.protection | VM_PROT_WRITE);
        REPORT_MACH_ERROR("mach_vm_protect", result);
        if (result != KERN_SUCCESS) return -1;
    }

    result = mach_vm_write(task, (vm_address_t) dst, (vm_offset_t) src, size);
    return result == KERN_SUCCESS ? (int) size : -1;
}

jint waitForSignal(jlong task, int signalnum) {
    int pid;
    kern_return_t kr = pid_for_task(task, &pid);
    REPORT_MACH_ERROR("pid_for_task", kr);

    while (1) {
        int status;

        // log_println("waitpid(%d)...", pid);
        int error = waitpid(pid, &status, 0);
        // log_println("waitpid(%d)... done", pid);
        if (error != pid) {
            log_println("waitpid failed with error: %d [%s]", errno, strerror(error));
            return PS_UNKNOWN;
        }
        if (WIFEXITED(status)) {
            log_println("Process %d exited with exit code %d", pid, WEXITSTATUS(status));
            return PS_TERMINATED;
        }
        if (WIFSIGNALED(status)) {
            int signal = WTERMSIG(status);
            log_println("Process %d terminated due to signal %d [%s]", pid, signal, strsignal(signal));
            return PS_TERMINATED;
        }
        if (WIFSTOPPED(status)) {
            // check whether the process received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);

            tele_log_println("Process %d stopped due to signal %d [%s]", pid, signal, strsignal(signal));
#if log_TELE
            log_println("After waitForSignal():");
            log_task_info(task);
#endif
            if (signalnum == signal && signalnum == SIGTRAP) {
                forall_threads(task, thread_set_single_step, (void *) false);
            }

            if (signal == 0 || signal == signalnum) {
                return PS_STOPPED;
            } else {
                ptrace(PT_CONTINUE, pid, (char*) 1, signal);

                error = errno;
                if (error != 0) {
                    log_println("Continuing process %d failed: %d [%s]", error, strerror(error));
                    return PS_UNKNOWN;
                }
            }
        }
    }
}


JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_createChild(JNIEnv *env, jobject this, jlong commandLineArgumentArray, jint vmAgentPort) {
    char **argv = (char**) commandLineArgumentArray;

    int childPid = fork();
    if (childPid == 0) {
        /*child:*/

        if (ptrace(PT_TRACE_ME, 0, 0, 0) != 0) {
            log_exit(1, "Failed to create initialize ptrace for VM process %d");
        }

        char *portDef;
        if (asprintf(&portDef, "MAX_AGENT_PORT=%u", vmAgentPort) == -1) {
            log_exit(1, "Could not allocate space for setting MAX_AGENT_PORT environment variable");
        }
        putenv(portDef);

        /*
         * See comment in 'main' function (Native/launch/maxvm.c) explaining why DYLD_LIBRARY_PATH is used.
         */
        char *dyldLibraryPathDef;
        if (asprintf(&dyldLibraryPathDef, "DYLD_LIBRARY_PATH=%s", dirname(argv[0])) == -1) {
            fprintf(stderr, "Could not allocate space for defining DYLD_LIBRARY_PATH environment variable\n");
            exit(1);
        }
        putenv(dyldLibraryPathDef);

        /* This call does not return if it succeeds: */
        execv(argv[0], argv);

        log_exit(1, "execv failed in child process");
    } else if (childPid < 0) {
        log_println("fork failed");
        return -1L;
    } else {
        /* parent: */
        int status;
        if (waitpid(childPid, &status, 0) == childPid && WIFSTOPPED(status)) {
            task_t childTask;
            boolean acquireTaskportRightIsNowFunctional = false;
            if (acquireTaskportRightIsNowFunctional && acquireTaskportRight() != 0) {
                return -1;
            }
            kern_return_t kr = task_for_pid(mach_task_self(), childPid, &childTask);
            if (kr != KERN_SUCCESS) {
                log_println("");
                log_println("    **** Could not access task for pid %d [%s]. You need to launch the Inspector as root ****", childPid, mach_error_string(kr));
                log_println("");
                return -1;
            }
            return (jlong) childTask;
        }
    }
    return -1L;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_kill(JNIEnv *env, jobject this, jint task) {
    int pid;
    kern_return_t kr =  pid_for_task((task_t) task, &pid);
    REPORT_MACH_ERROR("pid_for_task", kr);

    return ptrace(PT_KILL, pid, 0, 0) == 0;
}

typedef struct {
    JNIEnv *env;
    jobject process;
    jlong task;
    jobject threadList;
    jlong tlaList;
} GatherThreadArgs;

static boolean gatherThread(thread_t thread, void* args) {
    GatherThreadArgs *a = (GatherThreadArgs *) args;
    ThreadState_t state = TS_SUSPENDED;
    ThreadState threadState;

    mach_msg_type_number_t count = THREAD_STATE_COUNT;

    kern_return_t kr = thread_get_state(thread, THREAD_STATE_FLAVOR, (natural_t *) &threadState, &count);
    RETURN_ON_MACH_ERROR("thread_get_state", kr, true);

    TLA threadLocals = (TLA) alloca(tlaSize());
    NativeThreadLocalsStruct nativeThreadLocalsStruct;
    TLA tla = teleProcess_findTLA(a->task, a->tlaList, threadState.__rsp, threadLocals, &nativeThreadLocalsStruct);
    teleProcess_jniGatherThread(a->env, a->process, a->threadList, thread, state, threadState.__rip, tla);
    return true;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_gatherThreads(JNIEnv *env, jobject this, jlong task, jobject teleProcess, jobject threadList, jlong tlaList) {
    GatherThreadArgs args = {env, teleProcess, task, threadList, tlaList};
    forall_threads(task, gatherThread, (void *) &args);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_suspend(JNIEnv *env, jobject this, jlong task) {
    int pid;
    kern_return_t kr = pid_for_task((task_t) task, &pid);
    REPORT_MACH_ERROR("pid_for_task", kr);

    int error = kill(pid, SIGTRAP);
    if (error != 0) {
        log_println("Error sending SIGTRAP to process %d: %s", pid, strerror(error));
    }
    return error == 0;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_waitUntilStopped(JNIEnv *env, jobject this, jlong task) {
    return waitForSignal(task, SIGTRAP);
}

/**
 * Continually calls task_resume() on a given task while its 'suspend_count' > 0.
 */
void resume_task(task_t task) {
    boolean warningPrinted = false;
    while (true) {
        struct task_basic_info info;
        unsigned int info_count = TASK_BASIC_INFO_COUNT;
        kern_return_t kr = task_info(task, TASK_BASIC_INFO, (task_info_t) &info, &info_count);
        if (kr != KERN_SUCCESS) {
            log_println("task_info() failed when resuming task %d", task);
            return;
        }
        if (info.suspend_count > 0) {
            if (info.suspend_count > 1 && !warningPrinted) {
                warningPrinted = true;
                /* This only happens (I think) when 2 or more threads hit a breakpoint simultaneously.
                 * Given the mechanism by which deferred breakpoints are implemented (i.e. with a hidden
                 * breakpoint on a method called after every compilation), this case is not so rare.
                 * However, there is an unresolved issue where the VM process occasionally not resume properly after
                 * such an event (specifically, the next call to waitpid() never returns) and so it's useful
                 * to know that such an event just occurred. */
                log_println("*** INFO ***: Resuming task %d %d more times indicating more than one thread trapped on a breakpoint", task, info.suspend_count);
            }
            task_resume((task_t) task);
        } else {
            break;
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_resume(JNIEnv *env, jobject this, jlong task) {
    int pid;
    kern_return_t kr = pid_for_task((task_t) task, &pid);
    REPORT_MACH_ERROR("pid_for_task", kr);

#if log_TELE
    log_println("Before resume:");
    log_task_info(task);
#endif
    ptrace(PT_CONTINUE, pid, (char*) 1, 0);
    return true;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_readBytes(JNIEnv *env, jobject this, jlong task, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    return teleProcess_read(task, env, this, src, dst, isDirectByteBuffer, dstOffset, length);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_writeBytes(JNIEnv *env, jobject this, jlong task, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    return teleProcess_write(task, env, this, dst, src, isDirectByteBuffer, srcOffset, length);
}
