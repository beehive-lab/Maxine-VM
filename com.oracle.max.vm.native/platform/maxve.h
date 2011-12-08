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
#ifndef __maxve_h__
#define __maxve_h__ 1

#include "os.h"
#include <maxine.h>

#if os_MAXVE
#include <stdint.h>
    typedef void *maxve_Thread;
    extern void *maxve_create_thread(void (*function)(void *), uint64_t stacksize, int priority, void *runArg);
    extern void* maxve_get_current(void);
    extern int maxve_thread_join(maxve_Thread);
    typedef void *maxve_monitor_t;
    typedef void *maxve_condition_t;
    typedef struct maxve_TimeSpec {
    	long tv_sec;
    	long tv_nsec;
    } *maxve_TimeSpec_t;
    extern maxve_monitor_t maxve_monitor_create(void);
    extern int maxve_monitor_enter(maxve_monitor_t *monitor);
    extern int maxve_monitor_exit(maxve_monitor_t *monitor);
    extern int maxve_sleep(long millisecs);
    extern maxve_condition_t *maxve_condition_create(void);
    extern int maxve_condition_wait(maxve_condition_t *condition, maxve_monitor_t *monitor, maxve_TimeSpec_t timespec);
    extern int maxve_condition_notify(maxve_condition_t *condition, int all);
    extern int maxve_holds_monitor(maxve_monitor_t *monitor);
    extern void maxve_yield(void);
    extern void maxve_interrupt(void *thread);
    extern void maxve_set_priority(void *thread, int priority);
    extern void *maxve_virtualMemory_allocate(size_t size, int type);
    extern void *maxve_virtualMemory_deallocate(void *address, size_t size, int type);
    extern void *maxve_virtualMemory_allocateIn31BitSpace(size_t size, int type);
    extern void *maxve_virtualMemory_allocateAtFixedAddress(uint64_t address, size_t size, int type);
    extern int maxve_virtualMemory_pageSize(void);
    extern int maxve_virtualMemory_protectPages(uint64_t address, int count);
    extern int maxve_virtualMemory_unProtectPages(uint64_t address, int count);
    extern void maxve_set_javaId(maxve_Thread, int id);
    extern void maxve_initStack(void *nativeThreadLocals);
    extern void maxve_blue_zone_trap(void *nativeThreadLocals);
    extern uint64_t maxve_remap_boot_code_region(uint64_t base, size_t size);
    extern void maxve_native_props(native_props_t *native_props);


    typedef unsigned int maxve_SpecificsKey;
    extern void* maxve_thread_getSpecific(maxve_SpecificsKey key);
    extern void maxve_thread_setSpecific(maxve_SpecificsKey key, void *value);
    extern int maxve_thread_initializeSpecificsKey(maxve_SpecificsKey *key, void (*destructor)(void *));
    extern int maxve_numProcessors(void);

    struct fault_regs {
    	uint64_t r15;
    	uint64_t r14;
    	uint64_t r13;
    	uint64_t r12;
    	uint64_t rbp;
    	uint64_t rbx;
     	uint64_t r11;
    	uint64_t r10;
    	uint64_t r9;
    	uint64_t r8;
    	uint64_t rax;
    	uint64_t rcx;
    	uint64_t rdx;
    	uint64_t rsi;
    	uint64_t rdi;
    	uint64_t orig_rax;
    	uint64_t rip;
    	uint64_t cs;
    	uint64_t eflags;
    	uint64_t rsp;
   	uint64_t ss;
   };

#undef SIGFPE
#define SIGFPE 0
#undef SIGSEGV
#define SIGSEGV 13
#undef SIGILL
#define SIGILL 6

    typedef void *SigInfo; // address of faulting memory reference, illegal instruction, etc.
    typedef void (*fault_handler_t)(int fault, SigInfo sigInfo, struct fault_regs regs);
    typedef struct fault_regs UContext;
    extern void maxve_register_fault_handler(int fault, fault_handler_t fault_handler);
    typedef	struct {
    	uint64_t ss_base;
    	size_t	ss_size;
    } maxve_stackinfo_t;

    extern void maxve_get_stack_info(maxve_stackinfo_t *info);
#endif

#endif /*__maxve_h__*/
