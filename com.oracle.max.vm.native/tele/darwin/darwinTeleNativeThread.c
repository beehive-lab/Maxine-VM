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
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "darwin.h"
#include "log.h"
#include "ptrace.h"
#include "jni.h"
#include "word.h"

boolean thread_read_registers(thread_t thread,
    isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
    isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters,
    isa_CanonicalStateRegistersStruct *canonicalStateRegisters) {

    OsIntegerRegistersStruct osIntegerRegisters;
    OsFloatingPointRegistersStruct osFloatRegisters;
    OsStateRegistersStruct osStateRegisters;

    mach_msg_type_number_t count;
    if (canonicalIntegerRegisters != NULL) {
        count = INTEGER_REGISTER_COUNT;
        if (thread_get_state((thread_act_t) thread, INTEGER_REGISTER_FLAVOR, (thread_state_t) &osIntegerRegisters, &count) != KERN_SUCCESS) {
            return false;
        }
        isa_canonicalizeTeleIntegerRegisters(&osIntegerRegisters, canonicalIntegerRegisters);
    }

    if (canonicalStateRegisters != NULL) {
        count = STATE_REGISTER_COUNT;
        if (thread_get_state((thread_act_t) thread, STATE_REGISTER_FLAVOR, (thread_state_t) &osStateRegisters, &count) != KERN_SUCCESS) {
            return false;
        }
        isa_canonicalizeTeleStateRegisters(&osStateRegisters, canonicalStateRegisters);
    }

    if (canonicalFloatingPointRegisters != NULL) {
        count = FLOATING_POINT_REGISTER_COUNT;
        if (thread_get_state((thread_act_t) thread, FLOAT_REGISTER_FLAVOR, (thread_state_t) &osFloatRegisters, &count) != KERN_SUCCESS) {
            return false;
        }
        isa_canonicalizeTeleFloatingPointRegisters(&osFloatRegisters, canonicalFloatingPointRegisters);
    }

    return TRUE;
}

/*
 * Function copies from native register data structures to Java byte arrays. Does 3 things:
 * 1. Checks size of provided array lengths
 * 2. Canonicalizes the native register data structures
 * 3. Copies the canonicalized structures into the byte arrays
 */
static jboolean copyRegisters(JNIEnv *env, jobject  this,
                OsIntegerRegistersStruct *osIntegerRegisters, OsStateRegistersStruct *osStateRegisters, OsFloatingPointRegistersStruct *osFloatRegisters,
        jbyteArray integerRegisters, jint integerRegistersLength,
        jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
        jbyteArray stateRegisters, jint stateRegistersLength) {
    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;

    if (integerRegistersLength > (jint) sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for integer register data is too large");
        return false;
    }

    if (stateRegistersLength > (jint) sizeof(canonicalStateRegisters)) {
        log_println("buffer for state register data is too large");
        return false;
    }

    if (floatingPointRegistersLength > (jint) sizeof(canonicalFloatingPointRegisters)) {
        log_println("buffer for floating point register data is too large");
        return false;
    }

    isa_canonicalizeTeleIntegerRegisters(osIntegerRegisters, &canonicalIntegerRegisters);
    isa_canonicalizeTeleStateRegisters(osStateRegisters, &canonicalStateRegisters);
    isa_canonicalizeTeleFloatingPointRegisters(osFloatRegisters, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_readRegisters(JNIEnv *env, jobject c, jlong task, jlong thread,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {
    OsIntegerRegistersStruct osIntegerRegisters;
    OsFloatingPointRegistersStruct osFloatRegisters;
    OsStateRegistersStruct osStateRegisters;

    mach_msg_type_number_t count;
    count = INTEGER_REGISTER_COUNT;
    if (thread_get_state((thread_act_t) thread, INTEGER_REGISTER_FLAVOR, (thread_state_t) &osIntegerRegisters, &count) != KERN_SUCCESS) {
        return false;
    }

    count = STATE_REGISTER_COUNT;
    if (thread_get_state((thread_act_t) thread, STATE_REGISTER_FLAVOR, (thread_state_t) &osStateRegisters, &count) != KERN_SUCCESS) {
        return false;
    }

    count = FLOATING_POINT_REGISTER_COUNT;
    if (thread_get_state((thread_act_t) thread, FLOAT_REGISTER_FLAVOR, (thread_state_t) &osFloatRegisters, &count) != KERN_SUCCESS) {
        return false;
    }

    return copyRegisters(env, c, &osIntegerRegisters, &osStateRegisters, &osFloatRegisters,
                    integerRegisters, integerRegistersLength,
                    floatingPointRegisters, floatingPointRegistersLength,
                    stateRegisters, stateRegistersLength);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_setInstructionPointer(JNIEnv *env, jobject this, jlong task, jlong thread, jlong instructionPointer) {
    OsStateRegistersStruct osStateRegisters;
    mach_msg_type_number_t count = STATE_REGISTER_COUNT;
    if (thread_get_state((thread_act_t) thread, STATE_REGISTER_FLAVOR, (thread_state_t) &osStateRegisters, &count) != KERN_SUCCESS) {
        return false;
    }
    osStateRegisters.__rip = instructionPointer;
    if (thread_set_state((thread_act_t) thread, STATE_REGISTER_FLAVOR, (thread_state_t) &osStateRegisters, count) != KERN_SUCCESS) {
        return false;
    }
    return true;
}

boolean thread_set_single_step(thread_t thread, void *arg) {
    ThreadState threadState;

    const boolean isEnabled = arg != NULL;
    mach_msg_type_number_t count = THREAD_STATE_COUNT;
    kern_return_t error = thread_get_state(thread, THREAD_STATE_FLAVOR, (natural_t *) &threadState, &count);
    if (error != KERN_SUCCESS) {
        log_println("thread_get_state failed, error: %d, %s", error, mach_error_string(error));
        return false;
    }

    if (isEnabled) {
        threadState.__rflags |= 0x100UL;
    } else {
        threadState.__rflags &= ~0x100UL;
    }
    error = thread_set_state(thread, THREAD_STATE_FLAVOR, (natural_t *) &threadState, count);
    if (error != KERN_SUCCESS) {
        log_println("thread_set_state failed, error: %d, %s", error, mach_error_string(error));
        return false;
    }
    return true;
}

static boolean suspend_noncurrent_thread(thread_t thread, void *current) {
    kern_return_t kr;

    if ((thread_t) (Address) current == thread) {
        return true;
    }

    struct thread_basic_info info;
    unsigned int info_count = THREAD_BASIC_INFO_COUNT;

    kr = thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &info_count);
    if (kr != KERN_SUCCESS) {
        log_println("thread_info() failed when suspending thread %d", thread);
    } else {
        if (info.suspend_count == 0) {
            kr = thread_suspend(thread);
            if (kr != KERN_SUCCESS) {
                log_println("thread_suspend() failed when suspending thread %d", thread);
            }
        }
    }
    return true;
}

static boolean resume_noncurrent_thread(thread_t thread, void *current) {
    kern_return_t kr;
    unsigned i;

    if ((thread_t) (Address) current == thread) {
        return true;
    }

    struct thread_basic_info info;
    unsigned int info_count = THREAD_BASIC_INFO_COUNT;

    kr = thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &info_count);
    if (kr != KERN_SUCCESS) {
        log_println("thread_info() failed when resuming thread %d", thread);
    } else {
        for (i = 0; i < (unsigned) info.suspend_count; i++) {
            kr = thread_resume(thread);
            if (kr != KERN_SUCCESS) {
                log_println("thread_resume() failed when resuming thread %d", thread);
                break;
            }
        }
    }
    return true;
}

void resume_task(task_t task);

static boolean task_resume_thread(jlong task, thread_t thread) {
    kern_return_t kr;
    struct thread_basic_info info;
    unsigned int info_count = THREAD_BASIC_INFO_COUNT;
    unsigned int j;

    // get info for the current thread
    kr = thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &info_count);
    if (kr != KERN_SUCCESS) {
        log_println("thread_info() failed when resuming thread %d", thread);
        return false;
    }

    // if the thread is WAITING it will not resume unless we abort it first
    // the thread is WAITING if if stopped because of a trap
    if (info.run_state == TH_STATE_WAITING) {
        thread_abort(thread);
    }

    // resume the thread
    for (j = 0; j < (unsigned) info.suspend_count; j++) {
        thread_resume(thread);
    }

    // the thread will not resume unless the task is also resumed
    resume_task(task);
    //task_resume(task);

    return true;
}

/**
 * Single stepping works by setting the single step flag in the rFLAGS register and then resuming the thread.
 * After the TRAP signal is received the single stepping flag is cleared for all threads using the disableSingleStepping() method.
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_singleStep(JNIEnv *env, jobject this, jlong task, jlong thread) {
#if log_TELE
    log_println("Before single-stepping thread %d", thread);
    log_task_info((task_t) task);
#endif
    tele_log_println("Single stepping");
    jboolean result = thread_set_single_step((thread_t) thread, (void *) true)
        && forall_threads(task, suspend_noncurrent_thread, (void *) thread)
        && task_resume_thread(task, (thread_t) thread)
        && forall_threads(task, resume_noncurrent_thread, (void *) thread);
    return result;
}

// The following methods support core-dump access for Darwin
JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDumpThreadAccess_threadRegisters(JNIEnv *env, jclass class, jobject bytebuffer_gregs, jobject bytebuffer_fpregs,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {
    OsIntegerRegistersStruct *osIntegerRegisters = (OsIntegerRegistersStruct *) ((*env)->GetDirectBufferAddress(env, bytebuffer_gregs));
    OsFloatingPointRegistersStruct *osFloatRegisters = (OsFloatingPointRegistersStruct *) ((*env)->GetDirectBufferAddress(env, bytebuffer_fpregs));
    return copyRegisters(env, class, osIntegerRegisters, osIntegerRegisters, osFloatRegisters,
                    integerRegisters, integerRegistersLength,
                    floatingPointRegisters, floatingPointRegistersLength,
                    stateRegisters, stateRegistersLength);
}


