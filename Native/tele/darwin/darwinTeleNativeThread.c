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

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_readRegisters(JNIEnv *env, jobject this, jlong task, jlong thread,
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

    if (!thread_read_registers(thread, &canonicalIntegerRegisters, &canonicalFloatingPointRegisters, &canonicalStateRegisters)) {
        return false;
    }

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return TRUE;
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
