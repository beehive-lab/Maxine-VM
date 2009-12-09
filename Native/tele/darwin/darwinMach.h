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

extern void report_mach_error(const char *file, int line, kern_return_t krn, const char* name, const char* argsFormat, ...);

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

extern kern_return_t Task_for_pid(POS_PARAMS,
    mach_port_name_t target_tport,
    int pid,
    mach_port_name_t *t);

extern kern_return_t Pid_for_task(POS_PARAMS,
    mach_port_name_t t,
    int *x);

extern kern_return_t Task_threads(POS_PARAMS,
    task_t task,
    thread_act_array_t *thread_list,
    mach_msg_type_number_t* thread_count);

extern kern_return_t Vm_deallocate(POS_PARAMS,
    vm_map_t target_task,
    vm_address_t address,
    vm_size_t size);

extern kern_return_t Mach_vm_read_overwrite(POS_PARAMS,
    vm_map_t target_task,
    vm_address_t address,
    mach_vm_size_t size,
    mach_vm_address_t data,
    mach_vm_size_t *outsize);

extern kern_return_t Mach_vm_write(POS_PARAMS,
    vm_map_t target_task,
    vm_address_t address,
    vm_offset_t data,
    mach_msg_type_number_t dataCnt);

extern kern_return_t Thread_get_state(POS_PARAMS,
    thread_act_t target_act,
    thread_state_flavor_t flavor,
    thread_state_t old_state,
    mach_msg_type_number_t *old_stateCnt);

extern kern_return_t Mach_vm_region(POS_PARAMS,
    vm_map_t target_task,
    mach_vm_address_t *address,
    mach_vm_size_t *size,
    vm_region_flavor_t flavor,
    vm_region_info_t info,
    mach_msg_type_number_t *infoCnt,
    mach_port_t *object_name);

#endif
