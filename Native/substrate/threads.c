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
 * @author Ben L. Titzer
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
#include "log.h"
#include "image.h"
#include "jni.h"
#include "word.h"

#include "threads.h"
#include <sys/mman.h>

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_t Thread;
    typedef pthread_key_t SpecificsKey;
#   define thread_setSpecific pthread_setspecific
#elif os_SOLARIS
#   include <thread.h>
    typedef thread_t Thread;
    typedef thread_key_t SpecificsKey;
#   define thread_setSpecific thr_setspecific
#elif os_GUESTVMXEN
#   include "guestvmXen.h"
    typedef guestvmXen_Thread Thread;
    typedef guestvmXen_SpecificsKey SpecificsKey;
#   define thread_setSpecific guestvmXen_thread_setSpecific
#endif

static SpecificsKey _specificsKey;

void threads_initialize() {
#   if os_DARWIN || os_LINUX
        pthread_key_create(&_specificsKey, free);
#   elif os_SOLARIS
        thr_keycreate(&_specificsKey, free);
#   elif os_GUESTVMXEN
        guestvmXen_thread_initializeSpecificsKey(&_specificsKey, free);
#   else
#       error Unimplemented
#   endif
}

thread_Specifics *thread_currentSpecifics() {
#   if os_DARWIN || os_LINUX
        return (thread_Specifics*) pthread_getspecific(_specificsKey);
#   elif os_SOLARIS
        thread_Specifics *value;
        int result = thr_getspecific(_specificsKey, (void**) &value);
        if (result != 0) {
            log_exit(result, "thr_getspecific failed");
        }
        return value;
#   elif os_GUESTVMXEN
        return (thread_Specifics*) guestvmXen_thread_getSpecific(_specificsKey);
#   else
#       error Unimplemented
#   endif
}

thread_Specifics *thread_createSegments(int id, Size stackSize) {
    thread_Specifics *threadSpecifics = calloc(1, sizeof(thread_Specifics));
    threadSpecifics->id = id;

#if (os_LINUX || os_DARWIN || os_GUESTVMXEN)
    threadSpecifics->stackBase = (Address) malloc(stackSize);
    threadSpecifics->stackSize = stackSize;
#endif

    return threadSpecifics;
}

void thread_initSegments(thread_Specifics *threadSpecifics) {
    Address stackBottom;
#if os_SOLARIS
    /* we let the thread library allocate the stack for us. */
    stack_t stackInfo;
    int result = thr_stksegment(&stackInfo);

    if (result != 0) {
        log_exit(result, "thr_stksegment failed");
    }

    threadSpecifics->stackSize = stackInfo.ss_size;
    threadSpecifics->stackBase = (Address) stackInfo.ss_sp - stackInfo.ss_size;
    /* the thread library protects a page below the stack for us. */
    stackBottom = threadSpecifics->stackBase;
#else
    /* the stack is malloc'd on these platforms, protect a page for the thread locals */
    stackBottom = pageAlign(threadSpecifics->stackBase);
    protectPage(stackBottom);
    stackBottom += getPageSize();
#endif
    int vmThreadLocalsSize = image_header()->vmThreadLocalsSize;
    Address current = stackBottom - sizeof(Address);
    Size refMapAreaSize = 1 + threadSpecifics->stackSize / sizeof(Address) / 8;

    /* be sure to clear each of the thread local spaces */
    memset((void *) (current + sizeof(Address)), 0, vmThreadLocalsSize * 3);

    threadSpecifics->triggeredVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->enabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->disabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->refMapArea = current;
    current = pageAlign(current + refMapAreaSize);
    threadSpecifics->stackRedZone = current;
    current += getPageSize();
    threadSpecifics->stackYellowZone = current;
    current += getPageSize();

#if log_THREADS
    int id = threadSpecifics->id;
    log_println("thread %3d: stackBase = %p", id, threadSpecifics->stackBase);
    log_println("thread %3d: stackBase (aligned) = %p", id, pageAlign(threadSpecifics->stackBase));
    log_println("thread %3d: stackSize = %d", id, threadSpecifics->stackSize);
    log_println("thread %3d: stackBottom = %p", id, stackBottom);
    log_println("thread %3d: triggeredVmThreadLocals = %p", id, threadSpecifics->triggeredVmThreadLocals);
    log_println("thread %3d: enabledVmThreadLocals   = %p", id, threadSpecifics->enabledVmThreadLocals);
    log_println("thread %3d: disabledVmThreadLocals  = %p", id, threadSpecifics->disabledVmThreadLocals);
    log_println("thread %3d: refMapArea = %p", id, threadSpecifics->refMapArea);
    log_println("thread %3d: redZone    = %p", id, threadSpecifics->stackRedZone);
    log_println("thread %3d: yellowZone = %p", id, threadSpecifics->stackYellowZone);
    log_println("thread %3d: current    = %p", id, current);
    log_println("thread %3d: endOfStack = %p", id, threadSpecifics->stackBase + threadSpecifics->stackSize);
#endif

  /* make sure we didn't run out of space. */
  c_ASSERT(threadSpecifics->stackBase + threadSpecifics->stackSize > current);

#if os_GUESTVMXEN
    stackinfo_t stackInfo;
    guestvmXen_get_stack_info(&stackInfo);
    c_ASSERT(threadSpecifics->stackBase == (Address)stackInfo.ss_sp - stackInfo.ss_size);
    c_ASSERT(threadSpecifics->stackSize == stackInfo.ss_size);
#endif

    protectPage(threadSpecifics->stackRedZone);
    protectPage(threadSpecifics->stackYellowZone);
}

void tryUnprotectPage(Address address) {
    if (address != (Address) 0) {
        unprotectPage(address);
    }
}

void thread_destroySegments(thread_Specifics *threadSpecifics) {
    /* unprotect pages so some other unfortunate soul doesn't get zapped when reusing the space */
    tryUnprotectPage(threadSpecifics->stackRedZone);
    tryUnprotectPage(threadSpecifics->stackYellowZone);
#if (os_LINUX || os_DARWIN || os_GUESTVMXEN)
    /* these platforms have an extra protected page for the triggered thread locals */
    tryUnprotectPage(pageAlign(threadSpecifics->stackBase));
    /* the stack is free'd by the pthreads library. */
#endif
}
static Thread thread_create(jint id, Size stackSize, int priority) {
    Thread thread;
#if !os_GUESTVMXEN
    int error;
#endif

    if (pageAlign(stackSize) != stackSize) {
        log_println("thread_create: thread stack size must be a multiple of the OS page size (%d)", getPageSize());
        return (Thread) 0;
    }

#if log_THREADS
    log_println("thread_create: id = %d, stack size = %ld", id, stackSize);
#endif

    /* create the native thread locals and allocate stack if necessary */
    thread_Specifics *threadSpecifics = thread_createSegments(id, stackSize);

#if log_THREADS
    log_println("thread_create: stack base %lx", threadSpecifics->stackBase);
#endif

#if os_GUESTVMXEN
    thread = guestvmXen_create_thread_with_stack("java_thread",
		thread_runJava,
		(void*) threadSpecifics->stackBase,
	    threadSpecifics->stackSize,
		priority,
		threadSpecifics);
#elif (os_LINUX || os_DARWIN)
    pthread_attr_t attributes;
    pthread_attr_init(&attributes);
    pthread_attr_setstack(&attributes, (void *) threadSpecifics->stackBase, threadSpecifics->stackSize);
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);

    error = pthread_create(&thread, &attributes, (void *(*)(void *)) thread_runJava, threadSpecifics);
    pthread_attr_destroy(&attributes);
    if (error != 0) {
        log_println("pthread_create failed with error: %d", error);
	thread_destroySegments(threadSpecifics);
        return (Thread) 0;
    }
#elif os_SOLARIS
    /*
     * We let the system allocate the stack as doing so gets us a protected page
     * immediately below the bottom of the stack which is required for safepoints to work.
     */
    error = thr_create((void *) NULL, (size_t) stackSize, thread_runJava, threadSpecifics, THR_NEW_LWP | THR_BOUND, &thread);
    if (error != 0) {
        log_println("%s", strerror(error));
        log_println("thr_create failed with error: %d", error);
        thread_destroySegments(threadSpecifics);
        return (Thread) 0;
    }
#else
#   error Unimplemented
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
#   error Unimplemented
#endif
}

void *thread_self() {
    return (void *) thread_current();
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
#   error Unimplemented
#endif

    if (error != 0) {
        log_println("thread_join failed with error: %d", error);
    }
    return error;
}

void *thread_runJava(void *arg) {
    thread_Specifics *threadSpecifics = (thread_Specifics *) arg;
    Address nativeThread = (Address) thread_current();

    c_ASSERT(threadSpecifics != NULL);
    thread_setSpecific(_specificsKey, threadSpecifics);

#if log_THREADS
    log_println("thread_runJava: BEGIN t=%lx", nativeThread);
#endif

    /* set up the vm thread locals, guard pages, etc */
    thread_initSegments(threadSpecifics);

#if (os_GUESTVMXEN)
    /* mark this thread as a java thread */
    guestvmXen_set_javaId((Thread)nativeThread, threadSpecifics->id);
#endif

    VMThreadRunMethod method = (VMThreadRunMethod) (image_heap() + (Address) image_header()->vmThreadRunMethodOffset);

#if log_THREADS
    log_print("thread_runJava: id=%d, t=%lx, calling method: ", threadSpecifics->id, nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) method);
    log_println("");
#endif

    (*method)(threadSpecifics->id,
              nativeThread,
              threadSpecifics->stackBase,
              threadSpecifics->triggeredVmThreadLocals,
              threadSpecifics->enabledVmThreadLocals,
              threadSpecifics->disabledVmThreadLocals,
              threadSpecifics->refMapArea,
              threadSpecifics->stackRedZone,
              threadSpecifics->stackYellowZone,
              threadSpecifics->stackBase + threadSpecifics->stackSize);
#if (os_GUESTVMXEN)
    /* mark this thread as a non-java thread */
    guestvmXen_set_javaId((Thread)nativeThread, -1);
#endif

    /* destroy thread locals, deallocate stack, restore guard pages */
    thread_destroySegments(threadSpecifics);

#if log_THREADS
    log_println("thread_runJava: END t=%lx", nativeThread);
#endif
    /* Successful thread exit */
    return NULL;
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
#if log_THREADS
    log_println("BEGIN nativeJoin: %lx", thread);
#endif
    if (thread == 0L) {
        return false;
    }
    jboolean result = thread_join((Thread) thread) == 0;
#if log_THREADS
    log_println("END nativeJoin: %lx", thread);
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
    log_println("nativeYield ignored!");
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
    log_println("nativeInterrupt ignored!");
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
        if (error != EINTR && error != 0) {
            log_println("Call to nanosleep failed (other than by being interrupted): %s [remaining sec: %d, remaining nano sec: %d]", strerror(error), remainder.tv_sec, remainder.tv_nsec);
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
        log_println("nativeSetPriority %d failed!", priority);
    }
#elif os_GUESTVMXEN
    guestvmXen_set_priority((void *) nativeThread, priority);
#else
    log_println("nativeSetPriority %d ignored!", priority);
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
	c_ASSERT(wordAlign(base) == base);
#if log_THREADS
    log_println("nativeSetupAlternateSignalStack: alternate stack at %lx, size %lx ", base, size);
#endif
#if os_DARWIN || os_LINUX || os_SOLARIS

	stack_t	signalStack;

	signalStack.ss_size = size;
	signalStack.ss_flags = 0;
	signalStack.ss_sp = (Word) base;


	if (sigaltstack(&signalStack, (stack_t *) NULL) < 0) {
		log_exit(1, "signalstack failed");
	}
#elif os_GUESTVMXEN
	/* Nothing to do */
#else
#   error Unimplemented
#endif
}

