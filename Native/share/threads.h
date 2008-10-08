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

/*
 * This file should have been called "thread.c" and its header file "thread.h".
 * Alas, we did not manage to have every C compiler properly distinguish "thread.h" from <thread.h>.
 */

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_t Thread;
    typedef pthread_key_t ThreadKey;
#   define thread_getSpecific pthread_getspecific
#   define thread_setSpecific pthread_setspecific
#   define thread_initializeThreadKey pthread_key_create
#elif os_SOLARIS
#   include <thread.h>
    typedef thread_t Thread;
    typedef thread_key_t ThreadKey;
#   define thread_setSpecific thr_setspecific
#   define thread_initializeThreadKey thr_keycreate
    extern void* thread_getSpecific(ThreadKey key);
#elif os_GUESTVMXEN
#   include "guestvmXen.h"
    typedef guestvmXen_Thread Thread;
    typedef guestvmXen_ThreadKey ThreadKey;
#   define thread_getSpecific guestvmXen_thread_getSpecific
#   define thread_setSpecific guestvmXen_thread_setSpecific
#   define thread_initializeThreadKey guestvmXen_thread_initializeThreadKey
#endif

/**
 * Global symbol that the Inspector can look up to check whether a thread's start function is this one.
 */
extern void thread_runJava(void *jniNativeInterface);

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

extern void threads_initialize();

#define STACK_GUARD_PAGES 2

typedef struct {
    jint id;
    Address stackBase;
    Size stackSize;
    Address triggeredVmThreadLocals;
    Address enabledVmThreadLocals;
    Address disabledVmThreadLocals;
    Address refMapArea;
    Address stackYellowZone;
    Address stackRedZone;
} NativeThreadLocals;

extern ThreadKey nativeThreadLocalsKey;

#endif /*__threads_h__*/
