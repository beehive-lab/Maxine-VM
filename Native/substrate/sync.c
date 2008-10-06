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
/*VCSID=43edb4a0-8d60-46b7-9f20-e4bfad437383*/
/**
 * @author Simon Wilkinson
 */

#include <string.h>

#include "condition.h"
#include "debug.h"
#include "jni.h"
#include "mutex.h"
#include "os.h"
#include "word.h"
#include "threads.h"

jint nativeMutexSize(void) {
	return sizeof(mutex_Struct);
}

jint nativeConditionSize(void) {
	return sizeof(condition_Struct);
}

void nativeMutexInitialize(Mutex mutex) {
	mutex_initialize(mutex);
}

void nativeConditionInitialize(Condition condition) {
    condition_initialize(condition);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_Mutex_nativeMutexLock(JNIEnv *env, jclass c, Mutex mutex) {
	return mutex_lock(mutex) == 0;
}

jboolean nativeMutexUnlock(Mutex mutex) {
    return mutex_unlock(mutex) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_ConditionVariable_nativeConditionWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
	return condition_timedWait(condition, mutex, timeoutMilliSeconds);
}

jboolean nativeConditionNotify(Condition condition, jboolean all) {
#if os_SOLARIS || os_LINUX || os_DARWIN || os_GUESTVMXEN
	if (all) {
		return condition_notifyAll(condition) == 0;
	}
    return condition_notify(condition) == 0;
#else
#error Unimplemented
#endif
}
