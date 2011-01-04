/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __maxve_db__
#define __maxve_db__ 1

// This file contains copies of declarations from the GUK microkernel to avoid a compilation dependency in Maxine

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
#define UKERNEL_FLAG            0x00000100     /* Thread is a ukerrnel thread */
#define JOIN_FLAG               0x00000200     /* Thread is waiting for joinee */
#define AUX1_FLAG               0x00000400     /* monitor block */
#define AUX2_FLAG               0x00000800     /* monitor wait */
#define SLEEP_FLAG              0x00001000     /* sleeping */
#define APPSCHED_FLAG           0x00002000     /* application scheduler */
#define WATCH_FLAG              0x00004000     /* at watchpoint */

// from guk/tools/db-front/dbif.h
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
uint64_t db_read_u64(uint64_t address);
void db_write_u64(uint64_t address, uint64_t value);
uint16_t db_readbytes(uint64_t address, char *buffer, uint16_t n);
uint16_t db_writebytes(uint64_t address, char *buffer, uint16_t n);
uint16_t db_multibytebuffersize(void);
struct db_thread* db_gather_threads(int *num);
int db_suspend(uint16_t thread_id);
int db_resume(uint16_t thread_id);
int db_suspend_all(void);
int db_resume_all(void);
int db_single_step(uint16_t thread_id);
struct db_regs* db_get_regs(uint16_t thread_id);
struct thread_state* db_get_thread_state(uint16_t thread_id);
int db_set_ip(uint16_t thread_id, uint64_t ip);
int db_get_thread_stack(uint16_t thread_id,
                     uint64_t *stack_start,
                     uint64_t *stack_size);
uint64_t db_app_specific1(uint64_t arg);
int db_debug(int level);
void db_signoff(void);

#define READ_W 1
#define WRITE_W 2
#define EXEC_W 4
#define AFTER_W 8
int db_activate_watchpoint(uint64_t address, uint64_t size, int kind);
int db_deactivate_watchpoint(uint64_t address, uint64_t size);
uint64_t db_watchpoint_info(uint16_t thread_id, int *kind);

#endif /* __maxve_db__ */
