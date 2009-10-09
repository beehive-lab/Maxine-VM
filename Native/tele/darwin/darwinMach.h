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

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

extern void report_mach_error(const char *file, int line, kern_return_t krn, const char* name, const char* argsFormat, ...);

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
