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

// from guk/include/guk/sched.h
#define RUNNABLE_FLAG           0x00000001     /* Thread can be run on a CPU */
#define RUNNING_FLAG            0x00000002     /* Thread is currently runnig */
#define RESCHED_FLAG            0x00000004     /* Scheduler should be called at  the first opportunity. */
#define DYING_FLAG              0x00000008     /* Thread scheduled to die */
#define REQ_DEBUG_SUSPEND_FLAG  0x00000010     /* Thread is to be put to sleep in response to suspend request/breakpoint */
#define STEPPING_FLAG           0x00000020     /* Thread is to be single stepped */
#define DEBUG_SUSPEND_FLAG      0x00000040     /* Thread was actually put to sleep because of REQ_DEBUG_SUSPEND */
#define INTERRUPTED_FLAG        0x00000080     /* Thread was interrupted during last wait */
#define JOIN_FLAG               0x00000200     /* Thread is waiting for joinee */
#define AUX1_FLAG               0x00000400     /* monitor block */
#define AUX2_FLAG               0x00000800     /* monitor wait */
#define SLEEP_FLAG              0x00001000     /* sleeping */
#define APPSCHED_FLAG           0x00002000     /* application scheduler */
#define WATCH_FLAG              0x00004000     /* at watchpoint */

// from guk/tools/db-front/db-if.h
struct db_thread {
    uint16_t id;
    uint16_t pad;
    uint32_t flags;
    uint64_t stack;
    uint64_t stack_size;
};


struct db_regs {
    uint64_t xmm0;
    uint64_t xmm1;
    uint64_t xmm2;
    uint64_t xmm3;
    uint64_t xmm4;
    uint64_t xmm5;
    uint64_t xmm6;
    uint64_t xmm7;
    uint64_t xmm8;
    uint64_t xmm9;
    uint64_t xmm10;
    uint64_t xmm11;
    uint64_t xmm12;
    uint64_t xmm13;
    uint64_t xmm14;
    uint64_t xmm15;
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
    uint64_t rip;
    uint64_t flags;
    uint64_t rsp;
};

int db_attach(int domain_id);
int db_detach(void);
uint64_t read_u64(uint64_t address);
void write_u64(uint64_t address, uint64_t value);
uint16_t readbytes(uint64_t address, char *buffer, uint16_t n);
uint16_t writebytes(uint64_t address, char *buffer, uint16_t n);
uint16_t multibytebuffersize(void);
struct db_thread* gather_threads(int *num);
int suspend(uint16_t thread_id);
int resume(uint16_t thread_id);
int suspend_all(void);
int resume_all(void);
int single_step(uint16_t thread_id);
struct db_regs* get_regs(uint16_t thread_id);
struct thread_state* get_thread_state(uint16_t thread_id);
int set_ip(uint16_t thread_id, uint64_t ip);
int get_thread_stack(uint16_t thread_id,
                     uint64_t *stack_start,
                     uint64_t *stack_size);
uint64_t app_specific1(uint64_t arg);
int db_debug(int level);
void db_signoff(void);

#define READ_W 1
#define WRITE_W 2
#define EXEC_W 4
#define AFTER_W 8
int activate_watchpoint(uint64_t address, uint64_t size, int kind);
int deactivate_watchpoint(uint64_t address, uint64_t size);
uint64_t watchpoint_info(uint16_t thread_id, int *kind);

#endif /* __LIB_GUESTVMXEN_DB__ */
