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
#ifndef __guestvmXen_h__
#define __guestvmXen_h__ 1

#include "os.h"
#include <maxine.h>

#if os_GUESTVMXEN
    typedef void *guestvmXen_Thread;
    extern void *guestvmXen_create_thread_with_stack(char *name, void (*function)(void *), void *stack, unsigned long stacksize, int priority, void *data);
    extern void* guestvmXen_get_current(void);
    extern int guestvmXen_thread_join(guestvmXen_Thread);
    typedef void *guestvmXen_monitor_t;
    typedef void *guestvmXen_condition_t;
    typedef struct guestvmXen_TimeSpec {
    	long tv_sec;
    	long tv_nsec;
    } *guestvmXen_TimeSpec_t;
    extern guestvmXen_monitor_t guestvmXen_monitor_create(void);
    extern int guestvmXen_monitor_enter(guestvmXen_monitor_t *monitor);
    extern int guestvmXen_monitor_exit(guestvmXen_monitor_t *monitor);
    extern int guestvmXen_sleep(long millisecs);
    extern guestvmXen_condition_t *guestvmXen_condition_create(void);
    extern int guestvmXen_condition_wait(guestvmXen_condition_t *condition, guestvmXen_monitor_t *monitor, guestvmXen_TimeSpec_t timespec);
    extern int guestvmXen_condition_notify(guestvmXen_condition_t *condition, int all);
    extern int guestvmXen_holds_monitor(guestvmXen_monitor_t *monitor);
    extern void guestvmXen_yield(void);
    extern void guestvmXen_interrupt(void *thread);
    extern void guestvmXen_set_priority(void *thread, int priority);
    extern void *guestvmXen_allocate_stack(void *threadSpecifics, size_t size);
    extern void *guestvmXen_virtualMemory_allocate(size_t size, int type);
    extern void *guestvmXen_virtualMemory_deallocate(void *address, size_t size, int type);
    extern void *guestvmXen_virtualMemory_allocateIn31BitSpace(size_t size, int type);
    extern void *guestvmXen_virtualMemory_allocateAtFixedAddress(unsigned long address, size_t size, int type);
    extern int guestvmXen_virtualMemory_pageSize();
    extern int guestvmXen_virtualMemory_protectPage(unsigned long address);
    extern int guestvmXen_virtualMemory_unProtectPage(unsigned long address);
    extern void guestvmXen_set_javaId(guestvmXen_Thread, int id);
    extern void guestvmXen_initStack(void *threadSpecifics);
    extern void guestvmXen_blue_zone_trap(void *threadSpecifics);
    extern unsigned long guestvmXen_remap_boot_code_region(unsigned long base, size_t size);
    extern void guestvmXen_native_props(native_props_t *native_props);


    typedef unsigned int guestvmXen_SpecificsKey;
    extern void* guestvmXen_thread_getSpecific(guestvmXen_SpecificsKey key);
    extern void guestvmXen_thread_setSpecific(guestvmXen_SpecificsKey key, void *value);
    extern int guestvmXen_thread_initializeSpecificsKey(guestvmXen_SpecificsKey *key, void (*destructor)(void *));
    extern int guestvmXen_numProcessors(void);

    struct fault_regs {
    	unsigned long r15;
    	unsigned long r14;
    	unsigned long r13;
    	unsigned long r12;
    	unsigned long rbp;
    	unsigned long rbx;
     	unsigned long r11;
    	unsigned long r10;
    	unsigned long r9;
    	unsigned long r8;
    	unsigned long rax;
    	unsigned long rcx;
    	unsigned long rdx;
    	unsigned long rsi;
    	unsigned long rdi;
    	unsigned long orig_rax;
    	unsigned long rip;
    	unsigned long cs;
    	unsigned long eflags;
    	unsigned long rsp;
   	unsigned long ss;
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
    extern void register_fault_handler(int fault, fault_handler_t fault_handler);
    typedef	struct {
    	void	*ss_sp;
    	size_t	ss_size;
    } stackinfo_t;

    extern int guestvmXen_get_stack_info(stackinfo_t *info);
#endif

#endif /*__guestvmXen_h__*/
