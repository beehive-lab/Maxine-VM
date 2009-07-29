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
#ifndef __threads_h__
#define __threads_h__ 1

#include "os.h"
#include "jni.h"
#include "word.h"
#include "threadSpecifics.h"

/*
 * This file should have been called "thread.c" and its header file "thread.h".
 * Alas, we did not manage to have every C compiler properly distinguish "thread.h" from <thread.h>.
 */

/**
 * Global symbol that the Inspector can look up to check whether a thread's start function is this one.
 */
extern void *thread_runJava(void *jniNativeInterface);

/**
 * The signature of the Java method entrypoint for new threads.
 * This must match the signature of 'com.sun.max.vm.thread.VMThread.run()':
 */
typedef void (*VMThreadRunMethod)(jint id, Address nativeThread,
	            Address stackBase,
	            Address triggeredVmThreadLocals,
	            Address enabledVmThreadLocals,
	            Address disabledVmThreadLocals,
	            Address refMapArea,
	            Address stackRedZone,
	            Address yellowZone,
	            Address stackEnd);

/**
 * Sleeps the current thread for a given number of milliseconds.
 *
 * @return true if the sleep was interrupted
 */
extern jboolean thread_sleep(jlong numberOfMilliSeconds);

extern void threads_initialize(Address primordialVmThreadLocals, Size vmThreadLocalsSize);

extern ThreadSpecifics thread_currentSpecifics(void);

/**
 * For debugging purposes:
 */
extern void *thread_self(void);

#endif /*__threads_h__*/
