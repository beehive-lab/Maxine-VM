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
#elif os_GUESTVMXEN
#   include <guestvmXen.h>
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
#elif os_GUESTVMXEN
	return guestvmXen_numProcessors();
#else
	c_UNIMPLEMENTED();
#endif
}
