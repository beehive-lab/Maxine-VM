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
#else
	return 1;
#endif
}

jlong native_currentTimeMillis(void) {
#if os_SOLARIS || os_DARWIN
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (tv.tv_sec * 1000) + (tv.tv_usec / 1000);
#else
	return 1;
#endif
}

