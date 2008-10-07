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
/*VCSID=3c3317fe-e0df-4ac2-869e-b6e2359c4c1b*/
#ifndef __LIB_GUESTVMXEN_DB__
#define __LIB_GUESTVMXEN_DB__ 1

// This file is a copy from the Xen tools db-front package (to avoid cross-compilation problems)

#include <inttypes.h>

struct thread_state {
    int is_runnable;
    int is_running;
    int is_dying;
    int is_debug_suspend;
    int is_req_debug_suspend;
    int is_stepping;
    int is_joining;
    int is_xen;
    int is_aux1;
    int is_aux2;
    int is_sleeping;
};

struct thread {
    int id;
    char *name;
    struct thread_state state;
};

struct minios_regs {
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
	unsigned long rip;
	unsigned long rsp; 
};

int db_attach(int domain_id);
int db_detach(void);
uint64_t read_u64(unsigned long address);
void write_u64(unsigned long address, uint64_t value);
uint16_t readbytes(unsigned long address, char *buffer, uint16_t n);
uint16_t writebytes(unsigned long address, char *buffer, uint16_t n);
uint16_t multibytebuffersize(void);
struct thread* gather_threads(int *num);
int suspend(uint16_t thread_id);
int resume(uint16_t thread_id);
int single_step(uint16_t thread_id);
struct minios_regs* get_regs(uint16_t thread_id);
struct thread_state* get_thread_state(uint16_t thread_id);
int set_ip(uint16_t thread_id, unsigned long ip);
int get_thread_stack(uint16_t thread_id, 
                     unsigned long *stack_start,
                     unsigned long *stack_size);
uint64_t app_specific1(uint64_t arg);
int db_debug(int level);
void db_signoff(void);
#endif /* __LIB_GUESTVMXEN_DB__ */
