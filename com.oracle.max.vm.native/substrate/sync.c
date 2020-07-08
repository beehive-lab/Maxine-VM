/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

jint nativeMutexSize(void) {
	return sizeof(mutex_Struct);
}

void nativeMutexInitialize(Mutex mutex) {
	mutex_initialize(mutex);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_runtime_OSMonitor_nativeMutexLock(JNIEnv *env, jclass c, Mutex mutex) {
    return mutex_enter(mutex) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_NativeMutex_nativeMutexLock(JNIEnv *env, jclass c, Mutex mutex) {
	return mutex_enter(mutex) == 0;
}

jboolean nativeMutexUnlock(Mutex mutex) {
    return mutex_exit(mutex) == 0;
}

jboolean nativeMutexTryLock(Mutex mutex) {
    if (mutex_try_enter(mutex) != 0) {
        return false;
    } else {
        return  true;
    }
}

jint nativeConditionSize(void) {
    return sizeof(condition_Struct);
}

void nativeConditionInitialize(Condition condition) {
    condition_initialize(condition);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_runtime_OSMonitor_nativeConditionWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
    return condition_timedWait(condition, mutex, timeoutMilliSeconds);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_monitor_modal_sync_nat_NativeConditionVariable_nativeConditionWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
	return condition_timedWait(condition, mutex, timeoutMilliSeconds);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_runtime_OSMonitor_nativeTakeLockAndWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
    c_ASSERT(mutex_enter(mutex) == 0);
    jboolean result = condition_timedWait(condition, mutex, timeoutMilliSeconds);
    c_ASSERT(mutex_exit(mutex) == 0);
    c_ASSERT(result == true);
    return result;
}

jboolean nativeTakeLockAndNotify(Mutex mutex, Condition condition, jboolean all) {
    if (mutex_try_enter(mutex) != 0) {
        return false;
    }
    if (all) {
        c_ASSERT(condition_notifyAll(condition) == true);
    } else {
        c_ASSERT(condition_notify(condition) == true);
    }
    c_ASSERT(mutex_exit(mutex) == 0);
    return true;
}

jboolean nativeConditionNotify(Condition condition, jboolean all) {
    if (all) {
        return condition_notifyAll(condition);
    }
    return condition_notify(condition);
}
