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
#include <alloca.h>
#include <unistd.h>

#include "os.h"
#include "isa.h"
#include "virtualMemory.h"

#include <stdlib.h>
#include <memory.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include "debug.h"
#include "image.h"
#include "jni.h"
#include "word.h"

#include "threads.h"
#include <sys/mman.h>

NativeThreadLocals *thread_createSegments(int id, Size stackSize);
void thread_initSegments(NativeThreadLocals *nativeThreadLocals);
void thread_destroySegments(NativeThreadLocals *nativeThreadLocals);

#if os_SOLARIS
void* thread_getSpecific(ThreadKey key) {
    void *value;
    int result = thr_getspecific(key, &value);
    if (result != 0) {
        debug_exit(result, "thr_getspecific failed");
    }
    return value;
}
#endif

ThreadKey nativeThreadLocalsKey;

void threads_initialize() {
    thread_initializeThreadKey(&nativeThreadLocalsKey, free);
}

static Thread thread_create(jint id, Size stackSize, int priority) {
    Thread thread;
#if !os_GUESTVMXEN
    int error;
#endif

    if (pageAlign(stackSize) != stackSize) {
        debug_println("thread_create: thread stack size must be a multiple of the OS page size (%d)", getPageSize());
        return (Thread) 0;
    }

#if DEBUG_THREADS
    debug_println("thread_create: id = %d, stack size = %ld", id, stackSize);
#endif

    /* create the native thread locals and allocate stack if necessary */
    NativeThreadLocals *nativeThreadLocals = thread_createSegments(id, stackSize);

#if DEBUG_THREADS
    debug_println("thread_create: stack base %lx", nativeThreadLocals->stackBase);
#endif

#if os_GUESTVMXEN
    thread = guestvmXen_create_thread_with_stack("java_thread",
		thread_runJava,
		nativeThreadLocals->stackBase,
	        nativeThreadLocals->stackSize,
		priority,
		nativeThreadLocals);
#elif (os_LINUX || os_DARWIN)
    pthread_attr_t attributes;
    pthread_attr_init(&attributes);
    pthread_attr_setstack(&attributes, (void *) nativeThreadLocals->stackBase, nativeThreadLocals->stackSize);
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);

    error = pthread_create(&thread, &attributes, (void *(*)(void *)) thread_runJava, nativeThreadLocals);
    pthread_attr_destroy(&attributes);
    if (error != 0) {
        debug_println("pthread_create failed with error: %d", error);
	thread_destroySegments(nativeThreadLocals);
        return (Thread) 0;
    }
#elif os_SOLARIS
    /*
     * We let the system allocate the stack as doing so gets us a protected page
     * immediately below the bottom of the stack which is required for safepoints to work.
     */
    error = thr_create((void *) NULL, (size_t) stackSize, thread_runJava, nativeThreadLocals, THR_NEW_LWP | THR_BOUND, &thread);
    if (error != 0) {
        debug_println("%s", strerror(error));
        debug_println("thr_create failed with error: %d", error);
        thread_destroySegments(nativeThreadLocals);
        return (Thread) 0;
    }

#else
#error Unknown operating system target
#endif

    return thread;
}

static Thread thread_current(void) {
#if (os_DARWIN || os_LINUX)
    return (Thread) pthread_self();
#elif os_SOLARIS
    return thr_self();
#elif os_GUESTVMXEN
    return guestvmXen_get_current();
#else
#error Unknown operating system target
#endif
}

static int thread_join(Thread thread) {
    int error = -1;

#if (os_DARWIN || os_LINUX)

    int status;
    error = pthread_join(thread, (void **) &status);
    return error == 0;

#elif os_SOLARIS

    void *status;
    error = thr_join(thread, NULL, &status);

#elif os_GUESTVMXEN
    error = guestvmXen_thread_join(thread);
#else
#error Unimplemented
#endif

    if (error != 0) {
        debug_println("thread_join failed with error: %d", error);
    }
    return error;
}

void thread_runJava(void *arg) {
    NativeThreadLocals *nativeThreadLocals = (NativeThreadLocals *) arg;
    Address nativeThread = (Address) thread_current();

    debug_ASSERT(nativeThreadLocals != NULL);
    thread_setSpecific(nativeThreadLocalsKey, nativeThreadLocals);

#if DEBUG_THREADS
    debug_println("thread_runJava: BEGIN t=%lx", nativeThread);
#endif

    /* set up the vm thread locals, guard pages, etc */
    thread_initSegments(nativeThreadLocals);

#if (os_GUESTVMXEN)
    /* mark this thread as a java thread */
    guestvmXen_set_javaId((Thread)nativeThread, nativeThreadLocals->id);
#endif

    VMThreadRunMethod method = (VMThreadRunMethod) (image_heap() + (Address) image_header()->vmThreadRunMethodOffset);

#if DEBUG_THREADS
    debug_print("thread_runJava: id=%d, t=%lx, calling method: ", nativeThreadLocals->id, nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) method);
    debug_println("");
#endif

    (*method)(nativeThreadLocals->id, nativeThread,
            nativeThreadLocals->stackBase,
            nativeThreadLocals->triggeredVmThreadLocals,
            nativeThreadLocals->enabledVmThreadLocals,
            nativeThreadLocals->disabledVmThreadLocals,
            nativeThreadLocals->refMapArea,
            nativeThreadLocals->stackRedZone,
            nativeThreadLocals->stackYellowZone,
            nativeThreadLocals->stackBase + nativeThreadLocals->stackSize);
#if (os_GUESTVMXEN)
    /* mark this thread as a non-java thread */
    guestvmXen_set_javaId((Thread)nativeThread, -1);
#endif

    /* destroy thread locals, deallocate stack, restore guard pages */
    thread_destroySegments(nativeThreadLocals);

#if DEBUG_THREADS
    debug_println("thread_runJava: END t=%lx", nativeThread);
#endif
}

NativeThreadLocals *thread_createSegments(int id, Size stackSize) {
    NativeThreadLocals *nativeThreadLocals = calloc(1, sizeof(NativeThreadLocals));
    nativeThreadLocals->id = id;

#if (os_LINUX || os_DARWIN || os_GUESTVMXEN)
    nativeThreadLocals->stackBase = (Address) malloc(stackSize);
    nativeThreadLocals->stackSize = stackSize;
#endif

    return nativeThreadLocals;
}

void thread_initSegments(NativeThreadLocals *nativeThreadLocals) {
    Address stackBottom;
#if os_SOLARIS
    /* we let the thread library allocate the stack for us. */
    stack_t stackInfo;
    int result = thr_stksegment(&stackInfo);

    if (result != 0) {
        debug_exit(result, "thr_stksegment failed");
    }

    nativeThreadLocals->stackSize = stackInfo.ss_size;
    nativeThreadLocals->stackBase = stackInfo.ss_sp - stackInfo.ss_size;
    /* the thread library protects a page below the stack for us. */
    stackBottom = nativeThreadLocals->stackBase;
#else
    /* the stack is malloc'd on these platforms, protect a page for the thread locals */
    stackBottom = pageAlign(nativeThreadLocals->stackBase);
    protectPage(stackBottom);
    stackBottom += getPageSize();
#endif
    int vmThreadLocalsSize = image_header()->vmThreadLocalsSize;
    Address current = stackBottom - sizeof(Address);
    Size refMapAreaSize = 1 + nativeThreadLocals->stackSize / sizeof(Address) / 8;

    /* be sure to clear each of the thread local spaces */
    memset((void *) (current + sizeof(Address)), 0, vmThreadLocalsSize * 3);

    nativeThreadLocals->triggeredVmThreadLocals = current;
    current += vmThreadLocalsSize;
    nativeThreadLocals->enabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    nativeThreadLocals->disabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    nativeThreadLocals->refMapArea = current;
    current = pageAlign(current + refMapAreaSize);
    nativeThreadLocals->stackRedZone = current;
    current += getPageSize();
    nativeThreadLocals->stackYellowZone = current;
    current += getPageSize();

#if DEBUG_THREADS
    int id = nativeThreadLocals->id;
    debug_println("thread %3d: stackBase = %p", id, nativeThreadLocals->stackBase);
    debug_println("thread %3d: stackBase (aligned) = %p", id, pageAlign(nativeThreadLocals->stackBase));
    debug_println("thread %3d: stackSize = %d", id, nativeThreadLocals->stackSize);
    debug_println("thread %3d: stackBottom = %p", id, stackBottom);
    debug_println("thread %3d: triggeredVmThreadLocals = %p", id, nativeThreadLocals->triggeredVmThreadLocals);
    debug_println("thread %3d: enabledVmThreadLocals   = %p", id, nativeThreadLocals->enabledVmThreadLocals);
    debug_println("thread %3d: disabledVmThreadLocals  = %p", id, nativeThreadLocals->disabledVmThreadLocals);
    debug_println("thread %3d: refMapArea = %p", id, nativeThreadLocals->refMapArea);
    debug_println("thread %3d: redZone    = %p", id, nativeThreadLocals->stackRedZone);
    debug_println("thread %3d: yellowZone = %p", id, nativeThreadLocals->stackYellowZone);
    debug_println("thread %3d: current    = %p", id, current);
    debug_println("thread %3d: endOfStack = %p", id, nativeThreadLocals->stackBase + nativeThreadLocals->stackSize);
#endif

  /* make sure we didn't run out of space. */
  debug_ASSERT(nativeThreadLocals->stackBase + nativeThreadLocals->stackSize > current);

#if os_GUESTVMXEN
    stackinfo_t stackInfo;
    guestvmXen_get_stack_info(&stackInfo);
    debug_ASSERT(nativeThreadLocals->stackBase == (Address)stackInfo.ss_sp - stackInfo.ss_size);
    debug_ASSERT(nativeThreadLocals->stackSize == stackInfo.ss_size);
#endif

    protectPage(nativeThreadLocals->stackRedZone);
    protectPage(nativeThreadLocals->stackYellowZone);
}

void tryUnprotectPage(Address address) {
    if (address != (Address) 0) {
        unprotectPage(address);
    }
}

void thread_destroySegments(NativeThreadLocals *nativeThreadLocals) {
    /* unprotect pages so some other unfortunate soul doesn't get zapped when reusing the space */
    tryUnprotectPage(nativeThreadLocals->stackRedZone);
    tryUnprotectPage(nativeThreadLocals->stackYellowZone);
#if (os_LINUX || os_DARWIN || os_GUESTVMXEN)
    /* these platforms have an extra protected page for the triggered thread locals */
    tryUnprotectPage(pageAlign(nativeThreadLocals->stackBase));
    /* the stack is free'd by the pthreads library. */
#endif
}

/*
 * Create a thread.
 * @C_FUNCTION - called from Java
 */
Address nativeThreadCreate(jint id, Size stackSize, jint priority) {
    return (Address) thread_create(id, stackSize, priority);
}

/*
 * Join a thread.
 * @C_FUNCTION - called from Java
 */
jboolean nativeJoin(Address thread) {
#if DEBUG_THREADS
    debug_println("BEGIN nativeJoin: %lx", thread);
#endif
    if (thread == 0L) {
        return false;
    }
    jboolean result = thread_join((Thread) thread) == 0;
#if DEBUG_THREADS
    debug_println("END nativeJoin: %lx", thread);
#endif
    return result;
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeYield(JNIEnv *env, jclass c) {
#if os_SOLARIS
    thr_yield();
#elif os_GUESTVMXEN
    guestvmXen_yield();
#else
    debug_println("nativeYield ignored!");
#endif
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeInterrupt(JNIEnv *env, jclass c, Address nativeThread) {
 #if os_SOLARIS
    // Signals the thread
	int result = thr_kill(nativeThread, SIGUSR1);
	if (result != 0) {
    }
#elif os_GUESTVMXEN
	guestvmXen_interrupt((void*)nativeThread);
#else
    debug_println("nativeInterrupt ignored!");
 #endif
}

jboolean thread_sleep(jlong numberOfMilliSeconds) {
#if os_GUESTVMXEN
    return guestvmXen_sleep(numberOfMilliSeconds * 1000000);
#else
    struct timespec time, remainder;

    time.tv_sec = numberOfMilliSeconds / 1000;
    time.tv_nsec = (numberOfMilliSeconds % 1000) * 1000000;
    int value = nanosleep(&time, &remainder);

    if (value == -1) {
        int error = errno;
        if (error != EINTR) {
            //TODO: handle this. Either throw and exception or retry.
            debug_println("Call to nanosleep failed (other than by being interrupted): %s [remaining sec: %d, remaining nano sec: %d]", strerror(error), remainder.tv_sec, remainder.tv_nsec);
        }
    }
    return value;
#endif
}

void nonJniNativeSleep(long numberOfMilliSeconds) {
    thread_sleep(numberOfMilliSeconds);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeSleep(JNIEnv *env, jclass c, jlong numberOfMilliSeconds) {
    return thread_sleep(numberOfMilliSeconds);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeSetPriority(JNIEnv *env, jclass c, Address nativeThread, jint priority) {
#if os_SOLARIS
    int result = thr_setprio(nativeThread, priority);
    if (result != 0) {
        debug_println("nativeSetPriority %d failed!", priority);
    }
#elif os_GUESTVMXEN
    guestvmXen_set_priority((void*)nativeThread, priority);
#else
    debug_println("nativeSetPriority %d ignored!", priority);
#endif
}

long nativeGetDefaultThreadSignalStackSize() {
#if os_GUESTVMXEN
	return 0;
#else
	return SIGSTKSZ;
#endif
}

void nativeSetupAlternateSignalStack(Address base, long size) {
	debug_ASSERT(wordAlign(base) == base);
#if DEBUG_THREADS
    debug_println("nativeSetupAlternateSignalStack: alternate stack at %lx, size %lx ", base, size);
#endif
#if os_DARWIN || os_LINUX || os_SOLARIS

	stack_t	signalStack;

	signalStack.ss_size = size;
	signalStack.ss_flags = 0;
	signalStack.ss_sp = (Word) base;


	if (sigaltstack(&signalStack, (stack_t *) NULL) < 0) {
		debug_exit(1, "signalstack failed");
	}
#elif os_GUESTVMXEN
	/* Nothing to do */
#else
#   error Unimplemented
#endif
}

