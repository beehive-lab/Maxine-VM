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

#include <sys/types.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "darwinTeleNativeThread.h"
#include "log.h"
#include "ptrace.h"
#include "darwinMach.h"
#include "jni.h"
#include "word.h"
#include "virtualMemory.h"
#include "threads.h"

#include "teleProcess.h"
#include "teleNativeThread.h"

extern jboolean task_disable_single_stepping(jlong task);

#if 0
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

void log_thread_info(thread_t thread) {
    struct thread_basic_info info;
    unsigned int info_count = THREAD_BASIC_INFO_COUNT;
    thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &info_count);
    log_print("  Thread[%d]: suspend_count=%d, user_time=%u, system_time=%u, cpu_usage=%d, run_state=",
               thread, info.suspend_count, info.user_time, info.system_time, info.cpu_usage);

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
}

typedef void (*thread_visitor)(thread_t thread);

void forall_threads(task_t task, thread_visitor visitor) {
    thread_array_t thread_list = NULL;
    unsigned int nthreads = 0;
    unsigned i;

    task_threads((task_t) task, &thread_list, &nthreads);
    for (i = 0; i < nthreads; i++) {
        thread_t thread = thread_list[i];
        (*visitor)(thread);
    }

    // deallocate thread list
    vm_deallocate(mach_task_self(), (vm_address_t) thread_list, (nthreads * sizeof(int)));
}

void log_task_info(const char *file, int line, task_t task) {
    struct task_basic_info info;
    unsigned int info_count = TASK_BASIC_INFO_COUNT;
    task_info(task, TASK_BASIC_INFO, (task_info_t) &info, &info_count);
    log_println("\n\n%s:%d", file, line);
    log_println("Task[%d]: suspend_count=%d, virtual_size=%u, resident_size=%u, user_time=%u, system_time=%u", task,
                    info.suspend_count, info.virtual_size, info.resident_size, info.user_time, info.system_time);
    forall_threads(task, log_thread_info);
}
#endif

int task_read(task_t task, vm_address_t src, void *dst, size_t size) {
  mach_vm_size_t bytesRead;
  kern_return_t result = Mach_vm_read_overwrite(POS, task, src, size, (vm_address_t) dst, &bytesRead);
  return result == KERN_SUCCESS ? (jint) bytesRead : -1;
}

jboolean waitForSignal(jlong task, int signalnum) {
    int pid;
    Pid_for_task(POS, task, &pid);
    while (1) {
        int status;
        int error = waitpid(pid, &status, 0);
        if (error != pid) {
            log_println("waitpid failed with error: %d [%s]", errno, strerror(error));
            return false;
        }
        if (WIFEXITED(status)) {
            log_println("Process %d exited with exit code %d", pid, WEXITSTATUS(status));
            return false;
        }
        if (WIFSIGNALED(status)) {
            int signal = WTERMSIG(status);
            log_println("Process %d terminated due to signal %d [%s]", pid, signal, strsignal(signal));
            return false;
        }
        if (WIFSTOPPED(status)) {
            // check whether the process received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);

            tele_log_println("Process %d stopped due to signal %d [%s]", pid, signal, strsignal(signal));

            //log_task_info(__FILE__, __LINE__, task);

            if (signalnum == signal && signalnum == SIGTRAP) {
                task_disable_single_stepping(task);
            }

            if (signal == 0 || signal == signalnum) {
                return true;
            } else {
                ptrace(PT_CONTINUE, pid, (char*) 1, signal);

                error = errno;
                if (error != 0) {
                    log_println("Continuing process %d failed: %d [%s]", error, strerror(error));
                    return false;
                }
            }
        }
    }
}

extern char **environ;

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeCreateChild(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
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
            if (Task_for_pid(POS, mach_task_self(), childPid, &childTask) != KERN_SUCCESS) {
                return -1;
            }
            return (jlong) childTask;
        }
    }
    return -1L;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeKill(JNIEnv *env, jclass c, jint task) {
    int pid;
    Pid_for_task(POS, (task_t) task, &pid);
    return ptrace(PT_KILL, pid, 0, 0) == 0;
}

static jmethodID _methodID = NULL;

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeGatherThreads(JNIEnv *env, jobject process, jlong task, jobject result, jlong threadSpecificsListAddress) {
    thread_act_array_t threads;
    mach_msg_type_number_t numberOfThreads, i;

    if (Task_threads(POS, (task_t) task, &threads, &numberOfThreads) != KERN_SUCCESS) {
        return;
    }

    c_ASSERT(threadSpecificsListAddress != 0);

    if (_methodID == NULL) {
        jclass c = (*env)->GetObjectClass(env, process);
        c_ASSERT(c != NULL);
        _methodID = (*env)->GetMethodID(env, c, "jniGatherThread", "(Lcom/sun/max/collect/AppendableSequence;JIJJJJJ)V");
        c_ASSERT(_methodID != NULL);
    }

    for (i = 0; i < numberOfThreads; i++) {
        ThreadState_t state = TS_SUSPENDED;

        ThreadState threadState;
        thread_act_port_t thread = threads[i];

        mach_msg_type_number_t count = THREAD_STATE_COUNT;
        if (Thread_get_state(POS, thread, THREAD_STATE_FLAVOR, (natural_t *) &threadState, &count) != KERN_SUCCESS) {
            return;
        }

        ThreadSpecificsStruct tss;
        if (!threadSpecificsList_search(task, threadSpecificsListAddress, threadState.__rsp, &tss)) {
            memset(&tss, 0, sizeof(tss));
            tss.id = -2;
        }

        tele_log_println("Gathered thread[id=%d, thread=%lu, stackBase=%p, stackEnd=%p, stackSize=%lu, triggeredVmThreadLocals=%p, enabledVmThreadLocals=%p, disabledVmThreadLocals=%p]",
                        tss.id,
                        thread,
                        tss.stackBase,
                        tss.stackBase + tss.stackSize,
                        tss.stackSize,
                        tss.triggeredVmThreadLocals,
                        tss.enabledVmThreadLocals,
                        tss.disabledVmThreadLocals);

        (*env)->CallVoidMethod(env, process, _methodID, result,
                        (long) thread,
                        state,
                        tss.stackBase,
                        tss.stackSize,
                        tss.triggeredVmThreadLocals,
                        tss.enabledVmThreadLocals,
                        tss.disabledVmThreadLocals);
    }

    if (Vm_deallocate(POS, mach_task_self(), (vm_address_t) threads, (numberOfThreads * sizeof(thread_act_port_t))) != KERN_SUCCESS) {
        return;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeSuspend(JNIEnv *env, jclass c, jlong task) {
    int pid;
    if (Pid_for_task(POS, (task_t) task, &pid) != KERN_SUCCESS) {
        log_println("Could not get PID for task %d", task);
    }
    int error = kill(pid, SIGTRAP);
    if (error != 0) {
        log_println("Error sending SIGTRAP to process %d: %s", pid, strerror(error));
    }
    return error == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeWait(JNIEnv *env, jclass c, jlong pid, jlong task) {
    return waitForSignal(task, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeResume(JNIEnv *env, jclass c, jlong task) {
    int pid;
    Pid_for_task(POS, task, &pid);
    ptrace(PT_CONTINUE, pid, (char*) 1, 0);
    return true;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeReadBytes(JNIEnv *env, jclass c, jlong task, jlong address, jbyteArray byteArray, jint offset, jint length) {
  void* buffer = (void *) malloc(length * sizeof(jbyte));
  if (buffer == 0) {
      log_println("Failed to malloc byteArray of %d bytes", length);
      return -1;
  }
  jint bytesRead = task_read(task, address, buffer, length);
  if (bytesRead > 0) {
      (*env)->SetByteArrayRegion(env, byteArray, offset, bytesRead, buffer);
  }
  free(buffer);

  return bytesRead;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeWriteBytes(JNIEnv *env, jclass c, jlong task, jlong address, jbyteArray byteArray, jint offset, jint length) {
    jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
    if (buffer == 0) {
        log_println("failed to malloc byteArray of %d bytes", length);
        return -1;
    }

    (*env)->GetByteArrayRegion(env, byteArray, offset, length, buffer);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        log_println("failed to copy %d bytes from byteArray into buffer", length);
        return -1;
    }


    kern_return_t result = Mach_vm_write(POS, task, (vm_address_t) address, (vm_offset_t) buffer, length);
    free(buffer);
    return result == KERN_SUCCESS ? length : -1;
}
