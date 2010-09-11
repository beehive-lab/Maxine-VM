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
/**
 * @author Simon Wilkinson
 * @author Doug Simon
 */

#include <string.h>
#include <stdlib.h>

#include "condition.h"
#include "log.h"
#include "jni.h"
#include "mutex.h"
#include "os.h"
#include "word.h"
#include "threads.h"

#if os_DARWIN
#include <mach/mach.h>
#elif os_SOLARIS
#include <synch.h>
#elif os_LINUX
#include <semaphore.h>
#endif

jint nativeMutexSize(void) {
	return sizeof(mutex_Struct);
}

void nativeMutexInitialize(Mutex mutex) {
	mutex_initialize(mutex);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_NativeMutex_nativeMutexLock(JNIEnv *env, jclass c, Mutex mutex) {
	return mutex_enter(mutex) == 0;
}

jboolean nativeMutexUnlock(Mutex mutex) {
    return mutex_exit(mutex) == 0;
}

jint nativeConditionSize(void) {
    return sizeof(condition_Struct);
}

void nativeConditionInitialize(Condition condition) {
    condition_initialize(condition);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_NativeConditionVariable_nativeConditionWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
	return condition_timedWait(condition, mutex, timeoutMilliSeconds);
}

jboolean nativeConditionNotify(Condition condition, jboolean all) {
    if (all) {
        return condition_notifyAll(condition);
    }
    return condition_notify(condition);
}

#if os_DARWIN
static semaphore_t signal_sem;
#elif os_SOLARIS
#define sem_post    sema_post
#define sem_wait    sema_wait
#define sem_init    sema_init
#define sem_destroy sema_destroy
#define sem_t       sema_t
static sem_t signal_sem;
#elif os_SOLARIS || os_LINUX
static sem_t signal_sem;
#endif

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalNotify().
 */
void nativeSignalNotify() {
#if os_DARWIN
    kern_return_t kr = semaphore_signal(signal_sem);
    if (kr != KERN_SUCCESS) {
        log_exit(11, "semaphore_signal failed: %s", mach_error_string(kr));
    }
#elif os_LINUX || os_SOLARIS
    if (sem_post(&signal_sem) != 0) {
        log_exit(11, "sem_post failed: %s", strerror(errno));
    }
#else
    c_UNIMPLEMENTED();
#endif
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalWait().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait() {
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
#else
    c_UNIMPLEMENTED();
#endif
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalInit().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalInit() {
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
#else
    c_UNIMPLEMENTED();
#endif

    /* Calling these functions during initialization ensures the underlying
     * semaphores functions are linked now as linking during a trap handler
     * appears to cause problems. */
    nativeSignalNotify();
    Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait();
}

/**
 * Implementation of com.sun.max.vm.runtime.SignalDispatcher.nativeSignalFinalize().
 */
JNIEXPORT void JNICALL
Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalFinalize() {
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


