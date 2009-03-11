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

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "debugMach.h"
#include "log.h"

void report_mach_result(const char *file, int line, kern_return_t krn, const char* name, const char* argsFormat, ...) {
    log_print("%s:%d %s(", file, line, name);
    va_list ap;
    va_start(ap, argsFormat);
    log_print_vformat(argsFormat, ap);
    va_end(ap);
    log_print(") failed");
    char *machErrorMessage = mach_error_string(krn);
    if (machErrorMessage != NULL && strlen(machErrorMessage) != 0) {
        log_println(" [%s]", machErrorMessage);
    } else {
        log_print_newline();
    }
}

#define wrapped_mach_call(name, argsFormat, arg1, ...) { \
    if (log_TELE) { \
        log_println("%s:%d: %s(" argsFormat ")", file, line, STRINGIZE(name), ##__VA_ARGS__); \
    } \
    kern_return_t krn = name(arg1, ##__VA_ARGS__ ); \
    if (krn != KERN_SUCCESS)  { \
        report_mach_result(file, line, krn, STRINGIZE(name), argsFormat, ##__VA_ARGS__); \
    } \
    return krn; \
}

kern_return_t Task_for_pid(POS_PARAMS, mach_port_name_t target_tport, int pid, mach_port_name_t *t)
wrapped_mach_call(task_for_pid, "%d, %d, %p", target_tport, pid, t)

kern_return_t Pid_for_task(POS_PARAMS, mach_port_name_t t, int *x)
wrapped_mach_call(pid_for_task, "%d, %p", t, x)

kern_return_t Task_threads(POS_PARAMS, task_t task, thread_act_array_t *thread_list, mach_msg_type_number_t* thread_count)
wrapped_mach_call(task_threads, "%d, %p, %p", task, thread_list, thread_count)

kern_return_t Vm_deallocate(POS_PARAMS, vm_map_t target_task, vm_address_t address, vm_size_t size)
wrapped_mach_call(vm_deallocate, "%d, %p, %d", target_task, address, size)

kern_return_t Thread_get_state(POS_PARAMS, thread_act_t thread, thread_state_flavor_t flavor, thread_state_t old, mach_msg_type_number_t *count)
wrapped_mach_call(thread_get_state, "%d, %d, %p, %p", thread, flavor, old, count)

kern_return_t Mach_vm_region(POS_PARAMS,
    vm_map_t target_task,
    mach_vm_address_t *address,
    mach_vm_size_t *size,
    vm_region_flavor_t flavor,
    vm_region_info_t info,
    mach_msg_type_number_t *infoCnt,
    mach_port_t *object_name)
wrapped_mach_call(mach_vm_region, "%d, %p, %p, %d, %p, %p, %p", target_task, address, size, flavor, info, infoCnt, object_name)
