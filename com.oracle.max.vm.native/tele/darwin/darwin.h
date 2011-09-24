/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @return true if the iteration should proceed, false if it should stop
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
