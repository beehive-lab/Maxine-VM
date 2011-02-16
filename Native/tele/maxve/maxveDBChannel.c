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

extern void gather_and_trace_threads(void);

static int trace = 0;         // set to non-zero to trace thread resumption/blocking
static int terminated = 0;    // target domain has terminated
static struct db_thread *threads_at_rest = NULL;  // cache of threads on return from resume
static int num_threads_at_rest;
static volatile int suspend_all_request = 0;
static struct maxve_memory_handler db_memory_handler = {
                .readbytes = &db_readbytes,
                .writebytes = &db_writebytes
};

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeAttach(JNIEnv *env, jclass c, jint domainId, jlong extra1) {
    // The agent can handle multiple connections serially, so we must re-initialize the static state
    terminated = 0;
    threads_at_rest = NULL;
    num_threads_at_rest = 0;
    suspend_all_request = 0;
    tele_log_println("Calling do_attach on domId=%d", domainId);
    return db_attach(domainId);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeDetach(JNIEnv *env, jclass c) {
    tele_log_println("Calling do_detach");
    return db_detach();
}

static struct db_regs *checked_get_regs(char *f, int threadId) {
    struct db_regs *db_regs;
    db_regs = db_get_regs(threadId);
    if (db_regs == NULL) {
        log_println("%s: cannot get registers for thread %d", f, threadId);
        gather_and_trace_threads();
    }
    return db_regs;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeSuspendAll(JNIEnv *env, jclass c) {
	suspend_all_request = 1;
	return true;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeSuspend(JNIEnv *env, jclass c, jint threadId) {
    db_suspend(threadId);
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeSingleStep(JNIEnv *env, jclass c, jint threadId) {
    int rc = db_single_step(threadId);
    return rc == 0;
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint threadId, jlong ip) {
    return db_set_ip(threadId, ip);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeReadRegisters(JNIEnv *env, jclass c, jint threadId,
		jbyteArray integerRegisters, jint integerRegistersLength,
		jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
		jbyteArray stateRegisters, jint stateRegistersLength) {

    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;
    struct db_regs *db_regs;

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

    db_regs = checked_get_regs("nativeReadRegisters", threadId);
    if (db_regs == NULL) {
    	return false;
    }

	isa_canonicalizeTeleIntegerRegisters(db_regs, &canonicalIntegerRegisters);
	isa_canonicalizeTeleStateRegisters(db_regs, &canonicalStateRegisters);
	isa_canonicalizeTeleFloatingPointRegisters(db_regs, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
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
    // default
    return TS_SUSPENDED;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeGatherThreads(JNIEnv *env, jclass c, jobject teleDomain, jobject threadList, jlong tlaList) {
    struct db_thread *threads;
    int num_threads;

    threads = db_gather_threads(&num_threads);
    int i;
    for (i=0; i<num_threads; i++) {
        tele_log_println("nativeGatherThreads processing thread %d,", threads[i].id);
        TLA threadLocals = (TLA) alloca(tlaSize());
        NativeThreadLocalsStruct nativeThreadLocalsStruct;
        struct db_regs *db_regs = checked_get_regs("nativeGatherThreads", threads[i].id);
        threadLocals = teleProcess_findTLA(&db_memory_handler, tlaList, db_regs->rsp, threadLocals, &nativeThreadLocalsStruct);
        teleProcess_jniGatherThread(env, teleDomain, threadList, (jlong) threads[i].id, toThreadState(threads[i].flags), db_regs->rip, threadLocals);
    }
    free(threads);

    return 0;
}

int is_state(int state, int flag) {
	return state & flag ? 1 : 0;
}

int is_th_state(struct db_thread *thread, int flag) {
	return is_state(thread->flags, flag);
}

void trace_thread(struct db_thread *thread) {
    int state = thread->flags;
        tele_log_println("thread %d, ra %d, r %d, dying %d, rds %d, ds %d, mw %d, nw %d, jw %d, sl %d, wp %d",
            thread->id, is_state(state, RUNNABLE_FLAG), is_state(state, RUNNING_FLAG),
            is_state(state, DYING_FLAG), is_state(state, REQ_DEBUG_SUSPEND_FLAG),
            is_state(state, DEBUG_SUSPEND_FLAG), is_state(state, AUX1_FLAG),
            is_state(state, AUX2_FLAG), is_state(state, JOIN_FLAG), is_state(state, SLEEP_FLAG), is_state(state, WATCH_FLAG));
}

void trace_threads(struct db_thread *threads, int num_threads) {
    int i;
    for(i=0; i<num_threads; i++) {
        trace_thread(&threads[i]);
    }
}

void gather_and_trace_threads(void) {
    struct db_thread *threads;
    int num_threads;
    if (terminated) return;
    threads = db_gather_threads(&num_threads);
    trace_threads(threads, num_threads);
    free(threads);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeResume(JNIEnv *env, jobject domain) {
    unsigned long sleep_time = 0;
    struct db_thread *threads;
    int num_threads, i;

    tele_log_println("resuming all runnable threads");
    if (threads_at_rest != NULL) free(threads_at_rest);
    db_resume_all();
    /* Poll waiting for the thread to block or we get a suspendAll request, sleep for a short while to give domain chance to do something */
    usleep(500);
    while(suspend_all_request == 0) {
        tele_log_println("waiting for a thread to block");
        threads = db_gather_threads(&num_threads);
        if (threads == NULL) {
            // target domain has explicitly terminated
            // send signoff
            db_signoff();
            terminated = 1;
            tele_log_println("domain terminated");
            return 1;
        }
        trace_threads(threads, num_threads);

        for (i=0; i<num_threads; i++) {
            if (is_th_state(&threads[i], DEBUG_SUSPEND_FLAG)) {
            	suspend_all_request = 1;
                break;
            }
        }
        free(threads);
        if (suspend_all_request == 0) {
            sleep_time += 2000;  // usecs
            usleep(sleep_time);
        }
    }

// At this point at least one thread is debug_suspend'ed or we
// got a suspendAll request. Now suspend any other runnable threads.
// N.B. This is not an atomic operation and threads
// may become runnable, e.g., if a sleep expires
// or a driver thread is woken by an interrupt.
// However, those threads will debug_suspend themselves in that case.

    suspend_all_request = 0;
    tele_log_println("suspending all threads");
    db_suspend_all();
    threads = db_gather_threads(&num_threads);
    trace_threads(threads, num_threads);
    threads_at_rest = threads;
    num_threads_at_rest = num_threads;
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeGetBootHeapStart(JNIEnv *env, jclass c) {
    return db_app_specific1(0);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeSetTransportDebugLevel(JNIEnv *env, jclass c, jint level) {
    return db_debug(level);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeReadBytes(JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    return teleProcess_read(&db_memory_handler, env, c, src, dst, isDirectByteBuffer, dstOffset, length);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeWriteBytes(JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    return teleProcess_write(&db_memory_handler, env, c, dst, src, isDirectByteBuffer, srcOffset, length);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeMaxByteBufferSize(JNIEnv *env, jclass c) {
	return db_multibytebuffersize();
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeActivateWatchpoint(JNIEnv *env, jclass c, jlong address, jlong size, jboolean after, jboolean read, jboolean write, jboolean exec) {
    int kind = 0;
    if (after) kind |= AFTER_W;
    if (read) kind |= READ_W;
    if (write) kind |= WRITE_W;
    if (exec) kind |= EXEC_W;
	if (!after) return false;
    return db_activate_watchpoint(address, size, kind);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeDeactivateWatchpoint(JNIEnv *env, jclass c, jlong address, jlong size) {
	return db_deactivate_watchpoint(address, size);
}

static int get_wp_thread() {
    int i;
    for (i = 0; i < num_threads_at_rest; i++) {
    	if (is_th_state(&threads_at_rest[i], WATCH_FLAG)) {
    		return threads_at_rest[i].id;
    	}
    }
    return -1;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeReadWatchpointAddress(JNIEnv *env, jclass c) {
	int thread_id = get_wp_thread();
	int kind;
	if (thread_id < 0) {
		log_println("readWatchpointAddress: no thread at watchpoint");
		return 0;
	}
	return db_watchpoint_info(thread_id, &kind);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_maxve_MaxVEDBNativeTeleChannelProtocol_nativeReadWatchpointAccessCode(JNIEnv *env, jclass c) {
	int thread_id = get_wp_thread();
	int kind;
	if (thread_id < 0) {
		log_println("readWatchpointAccessCode: no thread at watchpoint");
		return 0;
	}
	db_watchpoint_info(thread_id, &kind);
	return kind & ~AFTER_W;

}

