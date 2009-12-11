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

#ifndef __darwinMach_h__
#define __darwinMach_h__ 1

#include "isa.h"

#if isa_AMD64
#   include <mach/x86_64/thread_act.h>

#   define INTEGER_REGISTER_COUNT x86_THREAD_STATE64_COUNT
#   define STATE_REGISTER_COUNT x86_THREAD_STATE64_COUNT
#   define FLOATING_POINT_REGISTER_COUNT x86_FLOAT_STATE64_COUNT
#   define THREAD_STATE_COUNT x86_THREAD_STATE64_COUNT

#   define INTEGER_REGISTER_FLAVOR x86_THREAD_STATE64
#   define STATE_REGISTER_FLAVOR x86_THREAD_STATE64
#   define FLOAT_REGISTER_FLAVOR x86_FLOAT_STATE64
#   define THREAD_STATE_FLAVOR x86_THREAD_STATE64

    typedef _STRUCT_X86_THREAD_STATE64 OsIntegerRegistersStruct;
    typedef _STRUCT_X86_THREAD_STATE64 OsStateRegistersStruct;
    typedef _STRUCT_X86_FLOAT_STATE64 OsFloatingPointRegistersStruct;
    typedef x86_thread_state64_t ThreadState;
#else
#   error "Only x64 is supported on Darwin"
#endif

/**
 * Prints an error message for a Mach API call whose return code is not KERN_SUCCESS.
 *
 * @param msg name of Mach API function called
 * @param kr the return value of the call
 */
#define REPORT_MACH_ERROR(msg, kr) do { \
    if (kr != KERN_SUCCESS) {\
        char *machErrorMessage = mach_error_string(kr); \
        if (machErrorMessage != NULL && strlen(machErrorMessage) != 0) { \
            log_println("%s:%d: %s: %s", __FILE__, __LINE__, msg, machErrorMessage); \
        } else { \
            log_println("%s:%d: %s: [errno: %d]", __FILE__, __LINE__, msg, kr); \
        } \
    } \
} while (0)

/**
 * Checks whether a Mach API call failed and if so prints an error message and then
 * goes to the label named 'out' in the caller's context.
 *
 * @param msg name of Mach API function called
 * @param kr the return value of the call
 */
#define OUT_ON_MACH_ERROR(msg, kr) \
    if (kr != KERN_SUCCESS) {\
        REPORT_MACH_ERROR(msg, kr); \
        goto out; \
    }

/**
 * Checks whether a Mach API call failed and if so prints an error message and then
 * executes a return from the caller's context.
 *
 * @param msg name of Mach API function called
 * @param kr the return value of the call
 */
#define RETURN_ON_MACH_ERROR(msg, kr, retval) do { \
    if (kr != KERN_SUCCESS) { \
        REPORT_MACH_ERROR(msg, kr); \
        return retval; \
    } \
} while (0)


extern boolean thread_read_registers(thread_t thread,
        isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
        isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters,
        isa_CanonicalStateRegistersStruct *canonicalStateRegisters);

/**
 * Callback for iterating over the threads in a task with 'forall_threads'.
 *
 * @param task the task whose threads are being iterated over
 * @param thread the thread being visited as part of the iteration
 * @return true if the iteration should proceed, false if it shop stop
 */
typedef boolean (*thread_visitor)(thread_t thread, void *arg);

/**
 * Iterates over all the threads in a given task with a given visitor function.
 *
 * @param task the task to iterate over
 * @param visitor the function to call for each thread in the task
 * @return true on success, false on failure
 */
extern boolean forall_threads(task_t task, thread_visitor visitor, void *arg);

/**
 * Sets the single-stepping mode for a given thread.
 *
 * @param thread the thread whose single-stepping mode is to be set
 * @param arg if NULL, then single-stepping is disabled for 'thread' otherwise it is enabled
 */
boolean thread_set_single_step(thread_t thread, void *arg);

extern void log_task_info(task_t task);
extern boolean log_thread_info(thread_t thread, void *arg);

#endif
