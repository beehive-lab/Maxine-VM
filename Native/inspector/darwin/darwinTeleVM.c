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
#include <mach/mach.h>

#include <unistd.h>
#include <stdlib.h>

#include "log.h"
#include "debugPtrace.h"
#include "jni.h"

#define MAX_MMAPS 16
#define MMAP_SYSCALL 0xc5

void check(int cond, char* msg) {
    if (!cond) {
        fprintf(stderr, "%s\n", msg);
        exit(-1);
    }
}

/* Thanks to Andreas Gal for help with this. */
JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinTeleVM_nativeLoadBootHeap(JNIEnv *env, jclass c, jlong childPID, jlong handle, jlong mappingSize) {
    mach_port_t task = (mach_port_t) handle;
    thread_act_port_array_t threadList;
    mach_msg_type_number_t numberOfThreads;
    int errcode = KERN_SUCCESS;

    /* Get the list of threads in the task */
    check(task_threads(task, &threadList, &numberOfThreads) == KERN_SUCCESS, "task_threads failed.");
    check(numberOfThreads == 1, "task has more than one thread.");

    /* If we find the syscall we are looking for we set this flag, continue and then check
       the result of the mmap syscall, which is the address of the new mapping. */
    Boolean found = false;

    int mmaps = 0;
    int numberOfInstructions = 0;

    while (true) {
        /* Wait for updates from the child process we are tracing */
        int status;

        /* Read the register state of the child via Mach (since we are single-stepping the child is guaranteed to be suspended at the moment. */
        x86_thread_state64_t state;
        mach_msg_type_number_t stateCount = x86_THREAD_STATE64_COUNT;
        errcode = thread_get_state(threadList[0], x86_THREAD_STATE64, (thread_state_t) &state, &stateCount);
        if (errcode != KERN_SUCCESS) {
            fprintf(stderr, "%d - thread_get_state failed: %d\n", numberOfInstructions, errcode);
            return 0;
        }

        /* If the previous step found an interesting system call, and we just stepped over it, analyze it now. */
        if (found) {
            /* remote process is now stopped at the instruction after the syscall, and we can take control of it */
            return state.__rax;
        }

        /* If RAX contains the MMAP syscall code, check to see if we are at a syscall instruction. */
        if ((state.__rax & 0xffff) == MMAP_SYSCALL) {
            /* read and check the machine code at the instruction pointer */
            char data[16];
            vm_size_t dataCount;
            check(vm_read_overwrite(task, (vm_address_t) state.__rip, 16, (vm_address_t) data, &dataCount) == KERN_SUCCESS, "vm_read_overwrite failed.");
            check(dataCount == 16, "vm_read_overwrite didn't return 16 bytes as expected.");

            /* System calls on Darwin use SYSCALL (0x0F 0x05) */
            if (data[0] == 0x0f && data[1] == 0x05) {
                if (mmaps++ >= MAX_MMAPS) {
                    /* too many mmaps before we found ours */
                    break;
                }
                if ((jlong) state.__rsi == mappingSize) {
                    /* Step over the syscall and analyze the result next time around. */
                    found = true;
                }
            }
        }

        /* step and wait again */
        errcode = ptrace(PT_STEP, childPID, (char*) 1, 0);
        if (errcode != 0) {
            fprintf(stderr, "%d - ptrace(PT_STEP) failed = %d.\n", numberOfInstructions, errcode);
            return 0;
        }
        do {
            errcode = waitpid(childPID, &status, 0);
        } while (errcode != childPID);

        if (WIFEXITED(status)) {
            fprintf(stderr, "%d - remote process exited.\n", numberOfInstructions);
            return 0;
        }

        numberOfInstructions++;
    }

    /* couldn't seem to find the mapping. */
    return (jlong) 0;
}
