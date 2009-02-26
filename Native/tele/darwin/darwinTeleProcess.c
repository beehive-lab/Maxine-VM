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
/*#include <sys/ptrace.h>*/
#include <sys/wait.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "darwinTeleNativeThread.h"
#include "log.h"
#include "debugPtrace.h"
#include "jni.h"
#include "word.h"
#include "virtualMemory.h"

#include "teleProcess.h"
#include "teleNativeThread.h"

extern jboolean disableSingleStepping(jlong task);

jboolean ptraceWaitForSignal(jlong pid, jlong task, int signalnum) {
    while (1) {
        int status;
        int error = waitpid(pid, &status, 0);

        if (error != pid) {
            log_println("waitpid failed with errno: %d", errno);
            return false;
        }
        if (WIFEXITED(status)) {
            return false;
        }
        if (WIFSIGNALED(status)) {
            return false;
        }
        if (WIFSTOPPED(status)) {
            // check whether the process received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);

            if (signalnum == signal && signalnum == SIGTRAP) {
                disableSingleStepping(task);
            }

            if (signal == 0 || signal == signalnum) {
                return true;
            } else {
                error = ptrace(PT_CONTINUE, pid, (char*) 1, signal);
                if (error != 0) {
                    log_println("ptrace(PT_CONTINUE) failed = %d", error);
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
            log_exit(1, "ptrace failed in child process");
        }

        if (putenv("DYLD_FORCE_FLAT_NAMESPACE=1") != 0) {
            /* Without this, libjava.jnilib library will link against the JVM_* functions
             * in lib[client|server].dylib instead of those in Maxine's libjvm.dylib. */
            log_exit(11, "The environment variable DYLD_FORCE_FLAT_NAMESPACE must be defined.");
        }

        char *vmAgentPortSetting;
        if (asprintf(&vmAgentPortSetting, "MAX_AGENT_PORT=%u", vmAgentPort) == -1) {
            log_exit(1, "Could not allocate space for setting MAX_DEBUGGER_PORT environment variable");
        }
        putenv(vmAgentPortSetting);

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
            return (jlong) childPid;
        }
    }
    return -1L;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativePidToTask(JNIEnv *env, jclass c, jlong pid) {
    task_t task;
    if (task_for_pid(mach_task_self(), pid, &task) != KERN_SUCCESS) {
        return -1;
    }
    return (jlong) task;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeKill(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_KILL, pid, 0, 0) == 0;
}

static jmethodID _methodID = NULL;

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeGatherThreads(JNIEnv *env, jobject process, jlong task, jobject result) {
    thread_act_port_array_t threads;
    mach_msg_type_number_t numberOfThreads, i;

    if (task_threads(task, &threads, &numberOfThreads) != KERN_SUCCESS) {
        return false;
    }

    if (_methodID == NULL) {
        jclass c = (*env)->GetObjectClass(env, process);
        c_ASSERT(c != NULL);
        _methodID = (*env)->GetMethodID(env, c, "jniGatherThread", "(Lcom/sun/max/collect/AppendableSequence;JIJJ)V");
        c_ASSERT(_methodID != NULL);
    }

    for (i = 0; i < numberOfThreads; i++) {
        ThreadState_t state = TS_SUSPENDED;

        ThreadState threadState;
        thread_act_port_t thread = threads[i];

        mach_msg_type_number_t count = THREAD_STATE_COUNT;
        kern_return_t error = thread_get_state(thread, THREAD_STATE_FLAVOR, (natural_t *) &threadState, &count);
        if (error != KERN_SUCCESS) {
            log_println("thread_get_state failed, error: %d, %s", error, mach_error_string(error));
            return false;
        }

        mach_vm_address_t stackBase = threadState.__rsp;
        mach_vm_size_t stackSize = 16; // initialized to something small
        mach_port_t objectName;
        vm_region_basic_info_data_64_t info;
        count = VM_REGION_BASIC_INFO_COUNT_64;
        error = mach_vm_region((vm_map_t) task, &stackBase, &stackSize, VM_REGION_BASIC_INFO_64, (vm_region_info_t) &info, &count, &objectName);
        if (error != KERN_SUCCESS) {
            log_println("mach_vm_region failed, error: %d, %s", error, mach_error_string(error));
            return false;
        }

        (*env)->CallVoidMethod(env, process, _methodID, result, (long) thread, state, (jlong) stackBase, (jlong) stackSize);
    }

    if (vm_deallocate(mach_task_self(), (vm_address_t) threads, (numberOfThreads * sizeof(thread_act_port_t))) != KERN_SUCCESS) {
        log_println("vm_deallocate failed");
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeSuspend(JNIEnv *env, jclass c, jlong task) {
    return task_suspend((task_t) task) == KERN_SUCCESS;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeWait(JNIEnv *env, jclass c, jlong pid, jlong task) {
    return ptraceWaitForSignal(pid, task, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeResume(JNIEnv *env, jclass c, jlong pid) {
    int error = ptrace(PT_CONTINUE, pid, (char*) 1, 0);
    if (error != 0) {
        log_println("ptrace(PT_CONTINUE) failed = %d", error);
        return false;
    }

    return true;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeReadBytes(JNIEnv *env, jclass c, jlong task, jlong address, jbyteArray byteArray, jint offset, jint length) {
  mach_vm_size_t bytesRead = length;

  jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
  if (buffer == 0) {
      log_println("failed to malloc byteArray of %d bytes", length);
      return -1;
  }

  kern_return_t result = mach_vm_read_overwrite(task, (vm_address_t) address, length, (vm_address_t) buffer, &bytesRead);
  if (result == KERN_SUCCESS && bytesRead > 0) {
      (*env)->SetByteArrayRegion(env, byteArray, offset, bytesRead, buffer);
  }
  free(buffer);

  return result == KERN_SUCCESS ? (jint) bytesRead : -1;
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


    kern_return_t result = mach_vm_write(task, (vm_address_t) address, (vm_offset_t) buffer, length);
    free(buffer);
    return result == KERN_SUCCESS ? length : -1;
}
