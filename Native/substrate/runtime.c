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
/**
 * @author Bernd Mathiske
 */
#include "jni.h"
#include "os.h"

#if os_LINUX || os_DARWIN
#   include <unistd.h>
#elif os_SOLARIS
#   include <sys/types.h>
#   include <sys/pset.h>
#   include <unistd.h>
#elif os_MAXVE
#   include <maxve.h>
#elif os_WINDOWS
	// TODO
#endif

/*
 * Mostly copied from HotSpot.
 */
JNIEXPORT jint JNICALL
Java_java_lang_Runtime_availableProcessors(JNIEnv *env, jclass c, jobject runtime) {
#if os_LINUX || os_DARWIN

	// Cannot find out active processors, so just return the number of online processors:
	return sysconf(_SC_NPROCESSORS_ONLN);

#elif os_SOLARIS

	  pid_t pid = getpid();
	  psetid_t pset = PS_NONE;
	  // Are we running in a processor set?
	  if (pset_bind(PS_QUERY, P_PID, pid, &pset) == 0) {
	    if (pset != PS_NONE) {
	      uint_t cardinality;
	      // Determine the number of CPUs in the processor set:
	      if (pset_info(pset, NULL, &cardinality, NULL) == 0) {
   	          return cardinality;
	      }
	    }
	  }
	  // Not in a processor set.
	  // Return the number of online CPUs:
	  return sysconf(_SC_NPROCESSORS_ONLN);

#elif os_WINDOWS
	SYSTEM_INFO si;
	DWORD_PTR processAffinityMaskPointer = 0;
	DWORD_PTR systemAffinityMaskPointer = 0;

	GetSystemInfo(&si);
	if (si.dwNumberOfProcessors <= sizeof(UINT_PTR) * BitsPerByte &&
	        GetProcessAffinityMask(GetCurrentProcess(), &processAffinityMaskPointer, &systemAffinityMaskPointer)) {
		 // The number of active processors is determined as the number of bits in the process affinity mask:
	    int numberOfBits = 0;
	    while (processAffinityMaskPointer != 0) {
	        processAffinityMaskPointer = processAffinityMaskPointer & (processAffinityMaskPointer - 1);
	        numberOfBits++;
	    }
	    return numberOfBits;
	}
	return si.dwNumberOfProcessors;
#elif os_MAXVE
	return maxve_numProcessors();
#else
	c_UNIMPLEMENTED();
#endif
}
