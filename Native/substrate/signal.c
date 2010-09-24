/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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

/**
 * Native functions for SignalDispatcher.java.
 *
 * @author Doug Simon
 */
#include <errno.h>
#include <string.h>
#include <stdlib.h>

#include "c.h"
#include "threads.h"
#include "log.h"

#if os_DARWIN
#include <mach/mach.h>
static semaphore_t signal_sem;
#elif os_SOLARIS
#include <synch.h>
#define sem_post    sema_post
#define sem_wait    sema_wait
#define sem_init    sema_init
#define sem_destroy sema_destroy
#define sem_t       sema_t
static sem_t signal_sem;
#elif os_LINUX
#include <semaphore.h>
static sem_t signal_sem;
#elif os_GUESTVMXEN
// no signals, so nothing necessary
#endif

boolean traceSignals = false;

/**
 *  ATTENTION: this signature must match the signatures of 'com.sun.max.vm.runtime.SignalDispatcher.tryPostSignal(int)'.
 */
typedef boolean (*TryPostSignalFunction)(int signal);

static TryPostSignalFunction tryPostSignal = NULL;

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalNotify().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify(JNIEnv *env, jclass c) {
#if os_DARWIN
    kern_return_t kr = semaphore_signal(signal_sem);
    if (kr != KERN_SUCCESS) {
        log_exit(11, "semaphore_signal failed: %s", mach_error_string(kr));
    }
#elif os_LINUX || os_SOLARIS
    if (sem_post(&signal_sem) != 0) {
        log_exit(11, "sem_post failed: %s", strerror(errno));
    }
#elif os_GUESTVMXEN
#else
    c_UNIMPLEMENTED();
#endif
}

/**
 * Called from userSignalHandler() in trap.c to deliver a signal dispatched by Signal.java.
 * This function atomically updates the pending signal queue by calling SignalDispatcher.tryPostSignal().
 */
void postSignal(int signal) {
    c_ASSERT(tryPostSignal != NULL);
    if (traceSignals) {
        log_print("Thread %p posting Java signal semaphore [signal: %d]\n", thread_self(), signal);
    }
    while (!tryPostSignal(signal)) {
        if (traceSignals) {
            log_print("Thread %p posting Java signal semaphore [signal: %d] -- retrying\n", thread_self(), signal);
        }
        /* empty body */
    }
    Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify(NULL, NULL);
    if (traceSignals) {
        log_print("Thread %p posted Java signal semaphore [signal: %d]\n", thread_self(), signal);
    }
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalWait().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait(JNIEnv *env, jclass c) {
    if (traceSignals) {
        log_print("Thread %p waiting on Java signal semaphore\n", thread_self());
    }
#if os_DARWIN
    kern_return_t kr = semaphore_wait(signal_sem);
    if (kr != KERN_SUCCESS) {
        log_exit(11, "semaphore_wait failed: %s", mach_error_string(kr));
    }
#elif os_LINUX || os_SOLARIS
    int ret;
    while ((ret = sem_wait(&signal_sem) == EINTR)) {
    }
    if (ret != 0) {
        log_exit(11, "sem_wait failed: %s", strerror(errno));
    }
#elif os_GUESTVMXEN
#else
    c_UNIMPLEMENTED();
#endif
    if (traceSignals) {
        log_print("Thread %p woke on Java signal semaphore\n", thread_self());
    }
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalInit().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalInit(JNIEnv *env, jclass c, Address tryPostSignalAddress) {
#if os_DARWIN
    kern_return_t kr = semaphore_create(mach_task_self(), &signal_sem, SYNC_POLICY_FIFO, 0);
    if (kr != KERN_SUCCESS) {
        log_exit(11, "semaphore_create failed: %s", mach_error_string(kr));
    }
#elif os_LINUX
    if (sem_init(&signal_sem, 0, 0) != 0) {
        log_exit(11, "sem_init failed: %s", strerror(errno));
    }
#elif os_SOLARIS
    if (sem_init(&signal_sem, 0, USYNC_THREAD, NULL) != 0) {
        log_exit(11, "sema_init failed: %s", strerror(errno));
    }
#elif os_GUESTVMXEN
    return;
#else
    c_UNIMPLEMENTED();
#endif

    /* Calling these functions during initialization ensures the underlying
     * semaphores functions are linked now as linking during a trap handler
     * appears to cause problems. */
    Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify(NULL, NULL);
    Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait(NULL, NULL);

    tryPostSignal = (TryPostSignalFunction) tryPostSignalAddress;
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalFinalize().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalFinalize(JNIEnv *env, jclass c) {
#if os_DARWIN
    kern_return_t kr = semaphore_destroy(mach_task_self(), signal_sem);
    if (kr != KERN_SUCCESS) {
        log_exit(11, "semaphore_destroy failed: %s", mach_error_string(kr));
    }
#elif os_LINUX || os_SOLARIS
    if (sem_destroy(&signal_sem) != 0) {
        log_exit(11, "sema_destroy failed: %s", strerror(errno));
    }
#endif
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSetTracing().
 */
void nativeSetSignalTracing(boolean flag) {
    traceSignals = flag;
}
