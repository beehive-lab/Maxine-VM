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
#ifndef __LIB_GUESTVMXEN_DB__
#define __LIB_GUESTVMXEN_DB__ 1

// This file contains copies of declarations from the GuestVM microkernel to avoid a compilation dependency in Maxine

#include <inttypes.h>

#define RUNNABLE_FLAG           0x00000001     /* Thread can be run on a CPU */
#define RUNNING_FLAG            0x00000002     /* Thread is currently runnig */
#define RESCHED_FLAG            0x00000004     /* Scheduler should be called at  the first opportunity. */
#define DYING_FLAG              0x00000008     /* Thread scheduled to die */
#define REQ_DEBUG_SUSPEND_FLAG  0x00000010     /* Thread is to be put to sleep in response to suspend request/breakpoint */
#define STEPPING_FLAG           0x00000020     /* Thread is to be single stepped */
#define DEBUG_SUSPEND_FLAG      0x00000040     /* Thread was actually put to sleep because of REQ_DEBUG_SUSPEND */
#define INTERRUPTED_FLAG        0x00000080     /* Thread was interrupted during last wait */
#define JOIN_FLAG               0x00000200     /* Thread is waiting for joinee */
#define AUX1_FLAG              0x00000400     /* monitor block */
#define AUX2_FLAG              0x00000800    /* monitor wait */
#define SLEEP_FLAG           0x00002000    /* sleeping */

struct db_thread {
    int id;
    int flags;
    unsigned long stack;
    unsigned long stack_size;
};

struct db_regs {
    unsigned long xmm0;
    unsigned long xmm1;
    unsigned long xmm2;
    unsigned long xmm3;
    unsigned long xmm4;
    unsigned long xmm5;
    unsigned long xmm6;
    unsigned long xmm7;
    unsigned long xmm8;
    unsigned long xmm9;
    unsigned long xmm10;
    unsigned long xmm11;
    unsigned long xmm12;
    unsigned long xmm13;
    unsigned long xmm14;
    unsigned long xmm15;
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
    unsigned long flags;
    unsigned long rsp;
};

int db_attach(int domain_id);
int db_detach(void);
uint64_t read_u64(unsigned long address);
void write_u64(unsigned long address, uint64_t value);
uint16_t readbytes(unsigned long address, char *buffer, uint16_t n);
uint16_t writebytes(unsigned long address, char *buffer, uint16_t n);
uint16_t multibytebuffersize(void);
struct db_thread* gather_threads(int *num);
int suspend(uint16_t thread_id);
int resume(uint16_t thread_id);
int single_step(uint16_t thread_id);
struct db_regs* get_regs(uint16_t thread_id);
struct thread_state* get_thread_state(uint16_t thread_id);
int set_ip(uint16_t thread_id, unsigned long ip);
int get_thread_stack(uint16_t thread_id,
                     unsigned long *stack_start,
                     unsigned long *stack_size);
uint64_t app_specific1(uint64_t arg);
int db_debug(int level);
void db_signoff(void);
#endif /* __LIB_GUESTVMXEN_DB__ */
