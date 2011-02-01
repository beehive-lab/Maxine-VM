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
#include <sys/time.h>
#include <time.h>
#include <string.h>

#include "log.h"
#include "threads.h"

#include "condition.h"

#define THREAD_CONDVAR_MUTEX_FORMAT "thread=%p, condvar=%p, mutex=%p"
#define THREAD_CONDVAR_FORMAT "thread=%p, condvar=%p"

void condition_initialize(Condition condition) {
#if log_MONITORS
    log_println("condition_initialize(" THREAD_CONDVAR_FORMAT ")", thread_self(), condition);
#endif
#if os_SOLARIS
    if (cond_init(condition, NULL, NULL) != 0) {
        c_FATAL();
    }
#elif os_LINUX || os_DARWIN
    if (pthread_cond_init(condition, NULL) != 0) {
        c_FATAL();
    }
#elif os_MAXVE
    *condition = maxve_condition_create();
#else
#   error
#endif
}

void condition_destroy(Condition condition) {
#if log_MONITORS
    log_println("condition_destroy   (" THREAD_CONDVAR_FORMAT ")", thread_self(), condition);
#endif
#if os_SOLARIS
    if (cond_destroy(condition) != 0) {
        c_FATAL();
    }
#elif os_LINUX || os_DARWIN
    if (pthread_cond_destroy(condition) != 0) {
        c_FATAL();
    }
#endif
}

#if os_MAXVE
#   define ETIMEDOUT -1
#endif

/**
 * Atomically blocks the current thread waiting on a given condition variable and unblocks a given mutex.
 * The waiting thread unblocks only after another thread calls 'condition_notify' or 'condition_notifyAll'
 * with the same condition variable or the thread was interrupted by Thread.interrupt(). In the case of the
 * latter, the 'interrupted' field in the relevant VmThread object will have been set to true.
 *
 * @param condition a condition variable on which the current thread will wait
 * @param mutex a mutex locked by the current thread
 * @return false if an error occurred, true otherwise (i.e. the thread was notified or interrupted).
 *        In either case, the current thread has reacquired the lock on 'mutex'.
 */
boolean condition_wait(Condition condition, Mutex mutex) {
#if log_MONITORS
    log_println("condition_wait      (" THREAD_CONDVAR_MUTEX_FORMAT ")", thread_self(), condition, mutex);
#endif
    int error;
#if (os_DARWIN || os_LINUX)
    error = pthread_cond_wait(condition, mutex);
#elif os_SOLARIS
    error = cond_wait(condition, mutex);
    if (error == EINTR) {
#if log_MONITORS
        log_println("condition_wait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) interrupted", thread_self(), condition, mutex);
#endif
        return true;
    }
#elif os_MAXVE
    error = maxve_condition_wait(*condition, *mutex, 0);
    if (error == 1) {
        /* (Doug) I assume 1 means EINTR */
        return true;
    }
#endif
    if (error != 0) {
        log_println("condition_wait      (" THREAD_CONDVAR_MUTEX_FORMAT ") unexpected error code %d [%s]", thread_self(), condition, mutex, error, strerror(error));
        return false;
    }
#if log_MONITORS
    log_println("condition_wait      (" THREAD_CONDVAR_MUTEX_FORMAT ") finished", thread_self(), condition, mutex);
#endif
    return true;
}

#if (os_DARWIN || os_LINUX)
/*
 * This function is taken from HotSpot (os_linux.cpp).
 */
static struct timespec* compute_abstime(struct timespec* abstime, jlong millis) {
    if (millis < 0) {
        millis = 0;
    }
    struct timeval now;
    int status = gettimeofday(&now, NULL);
    c_ASSERT(status == 0);
    jlong seconds = millis / 1000;
    millis %= 1000;
    if (seconds > 50000000) { // see man cond_timedwait(3T)
        seconds = 50000000;
    }
    abstime->tv_sec = now.tv_sec  + seconds;
    long       usec = now.tv_usec + millis * 1000;
    if (usec >= 1000000) {
        abstime->tv_sec += 1;
        usec -= 1000000;
    }
    abstime->tv_nsec = usec * 1000;
    return abstime;
}
#endif

/**
 * Atomically blocks the current thread waiting on a given condition variable and unblocks a given mutex.
 * The waiting thread unblocks only after another thread calls 'condition_notify' or 'condition_notifyAll'
 * with the same condition variable, or a given amount of time specified in milliseconds has elapsed.
 *
 * @param condition a condition variable on which the current thread will wait
 * @param mutex a mutex locked by the current thread
 * @param timeoutMilliSeconds the maximum amount of time (in milliseconds) that the wait should last for.
 *        A value of 0 means an infinite timeout.
 * @return false if an error occurred, true otherwise (i.e. the thread was notified, the timeout expired or the thread was interrupted).
 *        In either case, the current thread has reacquired the lock on 'mutex'.
 */
boolean condition_timedWait(Condition condition, Mutex mutex, Unsigned8 timeoutMilliSeconds) {
    if (timeoutMilliSeconds <= 0) {
        return condition_wait(condition, mutex);
    }
#if log_MONITORS
    log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d)", thread_self(), condition, mutex, timeoutMilliSeconds);
#endif
	int error;
#if (os_DARWIN || os_LINUX)
	struct timeval now;
	int status = gettimeofday(&now, NULL);
	c_ASSERT(status != -1);
	struct timespec abstime;
	compute_abstime(&abstime, timeoutMilliSeconds);
	error = pthread_cond_timedwait(condition, mutex, &abstime);
	if (error == ETIMEDOUT) {
#if log_MONITORS
	    log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) timed-out", thread_self(), condition, mutex, timeoutMilliSeconds);
#endif
	    return true;
	}
#elif os_SOLARIS
	timestruc_t reltime;
	reltime.tv_sec = timeoutMilliSeconds / 1000;
	reltime.tv_nsec = (timeoutMilliSeconds % 1000) * 1000000;
	error = cond_reltimedwait(condition, mutex, &reltime);
	if (error == ETIME) {
#if log_MONITORS
	    log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) timed-out", thread_self(), condition, mutex, timeoutMilliSeconds);
#endif
	    return true;
	}
	if (error == EINTR) {
#if log_MONITORS
	    log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) interrupted", thread_self(), condition, mutex, timeoutMilliSeconds);
#endif
	    return true;
	}
#elif os_MAXVE
	struct maxve_TimeSpec reltime;
	reltime.tv_sec = timeoutMilliSeconds / 1000;
	reltime.tv_nsec = (timeoutMilliSeconds % 1000) * 1000000;
	error = maxve_condition_wait(*condition, *mutex, &reltime);
	if (error == 1) {
	    /* (Doug) I assume 1 means EINTR */
	    return true;
	}
#else
#    error
#endif
	if (error != 0) {
        log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) unexpected error code %d [%s]",
                        thread_self(), condition, mutex, timeoutMilliSeconds, error, strerror(error));
	    return false;
    }
#if log_MONITORS
    log_println("condition_timedWait (" THREAD_CONDVAR_MUTEX_FORMAT ", %d) finished", thread_self(), condition, mutex, timeoutMilliSeconds);
#endif
	return true;
}

boolean condition_notify(Condition condition) {
#if log_MONITORS
    log_println("condition_notify    (" THREAD_CONDVAR_FORMAT ")", thread_self(), condition);
#endif
#if (os_DARWIN || os_LINUX)
    return pthread_cond_signal(condition) == 0;
#elif os_SOLARIS
    return cond_signal(condition) == 0;
#elif os_MAXVE
    return maxve_condition_notify(*condition, 0) == 0;
#else
#  error
#endif
}

boolean condition_notifyAll(Condition condition) {
#if log_MONITORS
    log_println("condition_notifyAll (" THREAD_CONDVAR_FORMAT ")", thread_self(), condition);
#endif
#if (os_DARWIN || os_LINUX)
    return pthread_cond_broadcast(condition) == 0;
#elif os_SOLARIS
    return cond_broadcast(condition) == 0;
#elif os_MAXVE
    return maxve_condition_notify(*condition, 1) == 0;
#else
#   error
#endif
}
