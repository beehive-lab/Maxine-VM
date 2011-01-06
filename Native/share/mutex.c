/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include "mutex.h"
#include "log.h"
#include "threads.h"

#define THREAD_MUTEX_FORMAT "thread=%p, mutex=%p"

void mutex_initialize(Mutex mutex) {
#if log_MONITORS
    log_println("mutex_initialize(" THREAD_MUTEX_FORMAT ")", thread_self(), mutex);
#endif
#if os_SOLARIS
    if (mutex_init(mutex, LOCK_RECURSIVE | LOCK_ERRORCHECK, NULL) != 0) {
        c_ASSERT(false);
    }
#elif os_LINUX || os_DARWIN
    pthread_mutexattr_t mutex_attribute;
    if (pthread_mutexattr_init(&mutex_attribute) != 0) {
        c_ASSERT(false);
    }
    if (pthread_mutexattr_settype(&mutex_attribute, PTHREAD_MUTEX_RECURSIVE) != 0) {
        c_ASSERT(false);
    }
    if (pthread_mutex_init(mutex, &mutex_attribute) != 0) {
        c_ASSERT(false);
    }
    if (pthread_mutexattr_destroy(&mutex_attribute) != 0) {
        c_ASSERT(false);
    }
#elif os_MAXVE
    *mutex = maxve_monitor_create();
#   else
        c_UNIMPLEMENTED();
#   endif
}

int mutex_enter_nolog(Mutex mutex) {
#if os_SOLARIS
    return mutex_lock(mutex);
#elif os_LINUX || os_DARWIN
    return pthread_mutex_lock(mutex);
#elif os_MAXVE
    if (maxve_monitor_enter(*mutex) != 0) {
        c_ASSERT(false);
    }
    return 0;
#else
    c_UNIMPLEMENTED();
#endif
}

int mutex_enter(Mutex mutex) {
#if log_MONITORS
    log_println("mutex_enter     (" THREAD_MUTEX_FORMAT ")", thread_self(), mutex);
#endif
    return mutex_enter_nolog(mutex);
}

int mutex_exit_nolog(Mutex mutex) {
#if os_SOLARIS
    return mutex_unlock(mutex);
#elif os_LINUX || os_DARWIN
    return pthread_mutex_unlock(mutex);
#elif os_MAXVE
    if (maxve_monitor_exit(*mutex) != 0) {
        c_ASSERT(false);
    }
    return 0;
#else
    c_UNIMPLEMENTED();
#endif
}

int mutex_exit(Mutex mutex) {
#if log_MONITORS
    log_println("mutex_exit     (" THREAD_MUTEX_FORMAT ")", thread_self(), mutex);
#endif
    return mutex_exit_nolog(mutex);
}

void mutex_dispose(Mutex mutex) {
#if log_MONITORS
    log_println("mutex_dispose   (" THREAD_MUTEX_FORMAT ")", thread_self(), mutex);
#endif
#if os_SOLARIS
    if (mutex_destroy(mutex) != 0) {
        c_ASSERT(false);
    }
#elif os_LINUX || os_DARWIN
    if (pthread_mutex_destroy(mutex) != 0) {
        c_ASSERT(false);
    }
#elif os_MAXVE
    c_UNIMPLEMENTED();
#endif
}
