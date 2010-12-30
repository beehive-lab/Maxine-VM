/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
#include "os.h"
#include "jni.h"
#include "maxine.h"

#include <sys/types.h>
#include <sys/time.h>

#if os_DARWIN
#include <mach/mach_time.h>
#include <mach/kern_return.h>

#endif


jlong native_nanoTime(void) {
#if os_SOLARIS
	return gethrtime();
#elif os_DARWIN
	struct mach_timebase_info temp;
	static struct mach_timebase_info timebase = { 0, 0 };
	static double factor = 0.0;
	static int failed = 0;

	/* get the factors the first time */
	if (timebase.denom == 0) {
		if (!failed) {
			if (mach_timebase_info(&temp) != KERN_SUCCESS) {
				factor = (double)temp.numer / (double)temp.denom;
				timebase = temp;
			} else {
				timebase.denom = timebase.numer = 0;
				failed = 1;
			}
		}
	}

	/* special case: absolute time is in nanoseconds */
	if (timebase.denom == 1 && timebase.numer == 1) {
		return mach_absolute_time();
	}

	/* general case: multiply by factor to get nanoseconds. */
	if (factor != 0.0) {
		return mach_absolute_time() * factor;
	}

	/* worst case: fallback to gettimeofday(). */
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (uint64_t)tv.tv_sec * (uint64_t)(1000 * 1000 * 1000) + (uint64_t)(tv.tv_usec * 1000);
#elif os_LINUX
	 struct timeval time;
	 int status = gettimeofday(&time, NULL);
	 c_ASSERT(status != -1);
	 jlong usecs = ((jlong) time.tv_sec) * (1000 * 1000) + (jlong) time.tv_usec;
	 return 1000 * usecs;
#else
	return 1;
#endif
}

jlong native_currentTimeMillis(void) {
#if os_SOLARIS || os_DARWIN || os_LINUX
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (tv.tv_sec * 1000) + (tv.tv_usec / 1000);
#else
	return 1;
#endif
}
