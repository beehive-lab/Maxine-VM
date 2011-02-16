/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <alloca.h>

#include "isa.h"
#include "log.h"
#include "jni.h"
#include "threadLocals.h"
#include "teleProcess.h"
#include "teleNativeThread.h"

/*
 * Size and member offsets of the native GUK thread struct.
 * Must be kept in sync with guk/include/sched.h.
 * See guk/tools/offsets/print_thread_offsets.c
 */
#define STRUCT_THREAD_SIZE 192
#define PREEMPT_COUNT_OFFSET 0
#define FLAGS_OFFSET 4
#define REGS_OFFSET 8
#define FPREGS_OFFSET 16
#define ID_OFFSET 24
#define APPSCHED_ID_OFFSET 26
#define GUK_STACK_ALLOCATED_OFFSET 28
#define NAME_OFFSET 32
#define STACK_OFFSET 40
#define STACK_SIZE_OFFSET 48
#define SPECIFIC_OFFSET 56
#define TIMESLICE_OFFSET 64
#define RESCHED_RUNNING_TIME_OFFSET 72
#define START_RUNNING_TIME_OFFSET 80
#define CUM_RUNNING_TIME_OFFSET 88
#define CPU_OFFSET 96
#define LOCK_COUNT_OFFSET 100
#define SP_OFFSET 104
#define IP_OFFSET 112
#define THREAD_LIST_OFFSET 120
#define READY_LIST_OFFSET 136
#define JOINERS_OFFSET 152
#define AUX_THREAD_LIST_OFFSET 168
#define DB_DATA_OFFSET 184

#define STRUCT_LIST_HEAD_SIZE 16
#define NEXT_OFFSET 0
#define PREV_OFFSET 8

#define MAXINE_THREAD_ID 40

#define get_target_value(buf, type, offset) *((type *) &buf[offset])

#define debug_println tele_log_println

#include <xg_public.h>

extern void *memset(void *s, int c, size_t n);
static uint64_t thread_list_address; // address of guk thread list head in target
struct tele_xg_thread;
struct tele_xg_thread {
    struct tele_xg_thread *next;
    uint32_t id;
    uint32_t flags;
    uint32_t cpu;
    struct xg_gdb_regs regs;
};

static struct tele_xg_thread *tele_xg_thread_list;  // local copy of important state

static vcpuid_t resume_vcpu;  // result of last xg_resume_n_wait

static int tele_xg_readbytes(uint64_t src, char *buf, unsigned short size) {
    unsigned short result = (unsigned short) xg_read_mem(src, buf, size, 0);
    return size - result;
}

static int tele_xg_writebytes(uint64_t src, char *buf, unsigned short size) {
    unsigned short  result = (unsigned short) xg_write_mem(src, buf, size, 0);
    return size - result;
}

static struct maxve_memory_handler xg_memory_handler = {
                .readbytes = &tele_xg_readbytes,
                .writebytes = &tele_xg_writebytes
};


/* Custom initialization for XG. */
JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeInit(JNIEnv *env, jclass c) {
    tele_xg_thread_list = NULL;
    resume_vcpu = -1;
    xg_init();
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeAttach(JNIEnv *env, jclass c, jint domainId, jlong extra1) {
    thread_list_address = extra1;
    debug_println("Calling xg_attach on domId=%d, thread_list_addr %lx", domainId, extra1);
    return xg_attach(domainId);
}

static ThreadState_t toThreadState(int state) {
    if (state & AUX1_FLAG) {
        return TS_MONITOR_WAIT;
    }
    if (state & AUX2_FLAG) {
        return TS_NOTIFY_WAIT;
    }
    if (state & JOIN_FLAG) {
        return TS_JOIN_WAIT;
    }
    if (state & SLEEP_FLAG) {
        return TS_SLEEPING;
    }
    if (state & WATCH_FLAG) {
        return TS_WATCHPOINT;
    }
    if (state & DEBUG_SUSPEND_FLAG) {
        return TS_BREAKPOINT;
    }
    if (state & RUNNING_FLAG) {
        // Can't return this state usefully because it prevents the Inspector from accessing thread data.
        // In the Inspector sense the thread is actually suspended because the entire domain is suspended
        // even if the thread was running at the time the domain was suspended.
        // return TS_RUNNING;
    }
    // default
    return TS_SUSPENDED;
}

/*
 * gather the threads by reading the guk thread list, updating tele_xg_thread_list and discarding kernel threads (except primordial)
 */
static void tele_xg_gather_threads() {
    char list_head_struct_buffer[STRUCT_LIST_HEAD_SIZE] ;
    char  thread_struct_buffer[STRUCT_THREAD_SIZE];
    uint64_t thread_struct_address;
    debug_println("tele_xg_gather_threads, resume_cpu %d", resume_vcpu);
    tele_xg_thread_list = NULL;
    // we read the guk thread list from the target, ignoring the guk threads
    c_ASSERT(tele_xg_readbytes(thread_list_address, &list_head_struct_buffer[0], STRUCT_LIST_HEAD_SIZE) == STRUCT_LIST_HEAD_SIZE);
    thread_struct_address = get_target_value(list_head_struct_buffer, uint64_t, NEXT_OFFSET);
    while (thread_struct_address != thread_list_address) {
        debug_println("tele_xg_gather_threads, thread_struct_address %lx", thread_struct_address);
        thread_struct_address -= THREAD_LIST_OFFSET;
        c_ASSERT(tele_xg_readbytes(thread_struct_address, &thread_struct_buffer[0], STRUCT_THREAD_SIZE) == STRUCT_THREAD_SIZE);
        uint32_t flags = get_target_value(thread_struct_buffer, uint32_t, FLAGS_OFFSET);
        uint16_t id = get_target_value(thread_struct_buffer, uint16_t, ID_OFFSET);
        if ((id == MAXINE_THREAD_ID) || (flags & UKERNEL_FLAG) == 0 ) {
            uint32_t cpu = get_target_value(thread_struct_buffer, uint32_t, CPU_OFFSET);
            debug_println("tele_xg_gather_threads %d, cpu %d", id, cpu);

            struct tele_xg_thread *tcb = malloc(sizeof(struct tele_xg_thread));
            tcb->id = id;
            tcb->flags = flags;
            tcb->cpu = cpu;
            if (flags & RUNNING_FLAG) {
                if (resume_vcpu != -1 && resume_vcpu == cpu) {
                    // this thread is in a BPT
                    tcb->flags |= DEBUG_SUSPEND_FLAG;
                }
                debug_println("tele_xg_gather_threads thread is running, flags %x", tcb->flags);
                c_ASSERT(xg_regs_read(GX_GPRS, cpu, &tcb->regs, 64) == 0);
            } else {
                debug_println("tele_xg_gather_threads thread is not running, flags %x", tcb->flags);
                memset(&tcb->regs, 0, sizeof(struct xg_gdb_regs));
                tcb->regs.u.xregs_64.rip = get_target_value(thread_struct_buffer, uint64_t, IP_OFFSET);
                tcb->regs.u.xregs_64.rsp = get_target_value(thread_struct_buffer, uint64_t, SP_OFFSET);
                debug_println("tele_xg_gather_threads ip %lx, sp %lx", tcb->regs.u.xregs_64.rip, tcb->regs.u.xregs_64.rsp);
            }
            tcb->next = tele_xg_thread_list;
            tele_xg_thread_list = tcb;
        } else {
            debug_println("tele_xg_gather_threads ignoring kernel thread %d,", id);
        }
        uint64_t next = get_target_value(thread_struct_buffer, uint64_t, THREAD_LIST_OFFSET);
        thread_struct_address =  next;
    }
}

static struct tele_xg_thread *get_thread(int id) {
    struct tele_xg_thread *tcb;
    if (tele_xg_thread_list == NULL) {
        tele_xg_gather_threads();
    }
    tcb = tele_xg_thread_list;
    while (tcb != NULL) {
        if (tcb->id == id) return tcb;
        tcb = tcb->next;
    }
    c_ASSERT(0);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_guestvm_GuestVMXGNativeTeleChannelProtocol_nativeGatherThreads(JNIEnv *env, jclass c, jobject teleDomain, jobject threadList, jlong tlaList) {
    tele_xg_gather_threads();
    struct tele_xg_thread *tcb = tele_xg_thread_list;
    while (tcb != NULL) {
            debug_println("nativeGatherThreads processing thread %d,", tcb->id);
            TLA threadLocals = (TLA) alloca(tlaSize());
            NativeThreadLocalsStruct nativeThreadLocalsStruct;
            threadLocals = teleProcess_findTLA(&xg_memory_handler, tlaList, tcb->regs.u.xregs_64.rsp, threadLocals, &nativeThreadLocalsStruct);
            teleProcess_jniGatherThread(env, teleDomain, threadList, (jlong) tcb->id, toThreadState(tcb->flags), tcb->regs.u.xregs_64.rip, threadLocals);
            tcb = tcb->next;
    }
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeResume(JNIEnv *env, jobject domain) {
    debug_println("Calling xg_resume_n_wait");
    resume_vcpu = xg_resume_n_wait(64);
    debug_println("xg_resume_n_wait returned %d", resume_vcpu);
    return resume_vcpu == -1 ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeReadBytes(JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    return teleProcess_read(&xg_memory_handler, env, c, src, dst, isDirectByteBuffer, dstOffset, length);
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeWriteBytes(JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    return teleProcess_write(&xg_memory_handler, env, c, dst, src, isDirectByteBuffer, srcOffset, length);
}


JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeSingleStep(JNIEnv *env, jclass c, jint threadId) {
    debug_println("nativeSingleStep %d", threadId);
    struct tele_xg_thread *tcb = get_thread(threadId);
    int rc = xg_step(tcb->cpu, 64);
    c_ASSERT(rc == 0);
    return true;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint threadId, jlong ip) {
    debug_println("nativeSetInstructionPointer %d %lx", threadId, ip);
    struct tele_xg_thread *tcb = get_thread(threadId);
    tcb->regs.u.xregs_64.rip = ip;
    c_ASSERT(xg_regs_write(GX_GPRS, tcb->cpu, &tcb->regs, 64) == 0);
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEXGNativeTeleChannelProtocol_nativeReadRegisters(JNIEnv *env, jclass c, jint threadId,
        jbyteArray integerRegisters, jint integerRegistersLength,
        jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
        jbyteArray stateRegisters, jint stateRegistersLength) {

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;
    struct db_regs db_regs;

    if (integerRegistersLength > sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for integer register data is too large: %d %d", integerRegistersLength, sizeof(canonicalIntegerRegisters));
        return false;
    }

    if (stateRegistersLength > sizeof(canonicalStateRegisters)) {
        log_println("buffer for state register data is too large");
        return false;
    }

    if (floatingPointRegistersLength > sizeof(canonicalFloatingPointRegisters)) {
        log_println("buffer for floating point register data is too large");
        return false;
    }

    debug_println("nativereadRegisters %d", threadId);
    struct tele_xg_thread *tcb = get_thread(threadId);
    db_regs.r15 = tcb->regs.u.xregs_64.r15;
    db_regs.r14 = tcb->regs.u.xregs_64.r14;
    db_regs.r13 = tcb->regs.u.xregs_64.r13;
    db_regs.r12 = tcb->regs.u.xregs_64.r12;
    db_regs.rbp = tcb->regs.u.xregs_64.rbp;
    db_regs.rbx= tcb->regs.u.xregs_64.rbx;
    db_regs.r11 = tcb->regs.u.xregs_64.r11;
    db_regs.r10 = tcb->regs.u.xregs_64.r10;
    db_regs.r9 = tcb->regs.u.xregs_64.r9;
    db_regs.r8 = tcb->regs.u.xregs_64.r8;
    db_regs.rax = tcb->regs.u.xregs_64.rax;
    db_regs.rcx = tcb->regs.u.xregs_64.rcx;
    db_regs.rdx = tcb->regs.u.xregs_64.rdx;
    db_regs.rsi = tcb->regs.u.xregs_64.rsi;
    db_regs.rdi = tcb->regs.u.xregs_64.rdi;
    db_regs.flags = tcb->regs.u.xregs_64.rflags;
    db_regs.rip = tcb->regs.u.xregs_64.rip;
    db_regs.rsp = tcb->regs.u.xregs_64.rsp;

    isa_canonicalizeTeleIntegerRegisters(&db_regs, &canonicalIntegerRegisters);
    isa_canonicalizeTeleStateRegisters(&db_regs, &canonicalStateRegisters);
    isa_canonicalizeTeleFloatingPointRegisters(&db_regs, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}

