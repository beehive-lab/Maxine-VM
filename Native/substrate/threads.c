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
 * @author Doug Simon
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
#include "messenger.h"

#include "threads.h"
#include "threadLocals.h"
#include <sys/mman.h>

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_t Thread;
    typedef pthread_key_t ThreadLocalsKey;
    typedef void (*ThreadLocalsDestructor)(void *);
#   define thread_setThreadLocals pthread_setspecific
#elif os_SOLARIS
#   include <thread.h>
    typedef thread_t Thread;
    typedef thread_key_t ThreadLocalsKey;
    typedef void (*ThreadLocalsDestructor)(void *);
#   define thread_setThreadLocals thr_setspecific
#elif os_GUESTVMXEN
#   include "guestvmXen.h"
    typedef guestvmXen_Thread Thread;
    typedef guestvmXen_SpecificsKey ThreadLocalsKey;
    typedef void (*ThreadLocalsDestructor)(void *);
#   define thread_setThreadLocals guestvmXen_thread_setSpecific
#endif

/**
 * The global key used to retrieve a ThreadLocals object for a thread.
 */
static ThreadLocalsKey theThreadLocalsKey;

/**
 * De-allocates the NativeThreadLocals object associated with a ThreadLocals object.
 *
 * @param tl a pointer to the thread locals whose native thread locals are to be freed
 */
void freeThreadLocals(ThreadLocals tl) {
    NativeThreadLocals ntl = getThreadLocal(NativeThreadLocals, tl, NATIVE_THREAD_LOCALS);
    if (ntl != NULL) {
        free(ntl);
        setThreadLocal(tl, NATIVE_THREAD_LOCALS, 0);
    }
}

void threads_initialize(ThreadLocals primordial_tl) {
#if os_DARWIN || os_LINUX
    pthread_key_create(&theThreadLocalsKey, (ThreadLocalsDestructor) freeThreadLocals);
#elif os_SOLARIS
    thr_keycreate(&theThreadLocalsKey, (ThreadLocalsDestructor) freeThreadLocals);
#elif os_GUESTVMXEN
    guestvmXen_thread_initializeSpecificsKey(&theThreadLocalsKey, (ThreadLocalsDestructor) freeThreadLocals);
#else
    c_UNIMPLEMENTED();
#endif

    /* Create native thread locals for the primordial thread so that there's a better chance
     * of reporting something sensible if it takes a trap. This also enables its VM thread locals
     * to be accessed by the debugger. */
    NativeThreadLocals primordial_ntl = calloc(1, sizeof(NativeThreadLocalsStruct));
    if (primordial_ntl == NULL) {
        log_exit(11, "Could not allocate primordial native thread locals");
    }
    setThreadLocal(primordial_tl, ID, 0);
    setThreadLocal(primordial_tl, NATIVE_THREAD_LOCALS, primordial_ntl);
    setThreadLocal(primordial_tl, SAFEPOINTS_ENABLED_THREAD_LOCALS, primordial_tl);
    setThreadLocal(primordial_tl, SAFEPOINTS_DISABLED_THREAD_LOCALS, primordial_tl);
    setThreadLocal(primordial_tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS, primordial_tl);
    primordial_ntl->id = 0;

#if os_SOLARIS
    stack_t stackInfo;
    int result = thr_stksegment(&stackInfo);

    if (result != 0) {
        log_exit(result, "thr_stksegment failed");
    }

    primordial_ntl->stackSize = stackInfo.ss_size;
    primordial_ntl->stackBase = (Address) stackInfo.ss_sp - stackInfo.ss_size;
#elif os_DARWIN || os_LINUX
    /* There's no support for finding the base and size of the current thread's stack in Linux or Darwin
     * so we simply make a guess based the stack memory allocated (via alloca)
     * in the stack frame of maxine() in maxine.c and the default stack size for a pthread. */
    pthread_attr_t attr;
    size_t stackSize;
    pthread_attr_init(&attr);
    pthread_attr_getstacksize (&attr, &stackSize);
    Address stackEnd = (Address) primordial_tl + threadLocalsSize();
    primordial_ntl->stackBase = stackEnd - stackSize;
    primordial_ntl->stackSize = stackSize;
    pthread_attr_destroy(&attr);
#elif os_GUESTVMXEN
    stackinfo_t stackInfo;
    guestvmXen_get_stack_info(&stackInfo);
    primordial_ntl->stackBase = stackInfo.ss_sp - stackInfo.ss_size;
    primordial_ntl->stackSize = stackInfo.ss_size;
#else
    c_UNIMPLEMENTED();
#endif
    thread_setThreadLocals(theThreadLocalsKey, (void *) primordial_tl);
}

ThreadLocals thread_currentThreadLocals() {
#if os_DARWIN || os_LINUX
    return (ThreadLocals) pthread_getspecific(theThreadLocalsKey);
#elif os_SOLARIS
    ThreadLocals value;
    int result = thr_getspecific(theThreadLocalsKey, (void**) &value);
    if (result != 0) {
        log_exit(result, "thr_getspecific failed");
    }
    return value;
#elif os_GUESTVMXEN
    return (ThreadLocals) guestvmXen_thread_getSpecific(theThreadLocalsKey);
#else
    c_UNIMPLEMENTED();
#endif
}

NativeThreadLocals thread_createSegments(int id, Size stackSize) {
    NativeThreadLocals ntl = calloc(1, sizeof(NativeThreadLocalsStruct));
    if (ntl == NULL) {
    	return NULL;
    }
    ntl->id = id;

#if os_SOLARIS
    // stack is allocated as part of Solaris thread creation
#else
#if (os_LINUX || os_DARWIN)
    ntl->stackBase = (Address) malloc(stackSize);
#elif os_GUESTVMXEN
    ntl->stackBase = (Address) guestvmXen_allocate_stack(ntl, stackSize);
#endif
    if (ntl->stackBase == 0) {
    	free(ntl);
    	ntl = NULL;
    } else {
        ntl->stackSize = stackSize;
    }
#endif
    return ntl;
}

void initStackProtection(NativeThreadLocals ntl) {
#if os_GUESTVMXEN
	// all page protection is handled in following call
	guestvmXen_initStack(ntl);
#else
	ntl->stackBlueZone = ntl->stackYellowZone;
    virtualMemory_protectPage(ntl->stackRedZone);
    virtualMemory_protectPage(ntl->stackYellowZone);
#if !os_SOLARIS
    virtualMemory_protectPage(virtualMemory_pageAlign(ntl->stackBase));
#endif
#endif

}

/**
 * Initializes the VM thread locals and guard pages on the stack.
 *
 * @param ntl the data structure containing the address of the stack. The other members of this
 *            struct are initialized by this function.
 * @return the pointer to the safepoints-enabled thread locals for the current thread
 */
ThreadLocals thread_initSegments(NativeThreadLocals ntl) {
    Address stackBottom;
#if os_SOLARIS
    /* we let the thread library allocate the stack for us. */
    stack_t stackInfo;
    int result = thr_stksegment(&stackInfo);

    if (result != 0) {
        log_exit(result, "thr_stksegment failed");
    }

    ntl->stackSize = stackInfo.ss_size;
    ntl->stackBase = (Address) stackInfo.ss_sp - stackInfo.ss_size;
    /* the thread library protects a page below the stack for us. */
    stackBottom = ntl->stackBase;
#else
    /* the stack is malloc'd on these platforms, protect a page for the thread locals */
    /* N.B. do not read or write the contents of the stack until initStackProtection returns. */
    stackBottom = virtualMemory_pageAlign(ntl->stackBase) + virtualMemory_getPageSize();
#endif
    const int tlSize = threadLocalsSize();
    Address current = stackBottom - sizeof(Address);
    Size refMapAreaSize = 1 + ntl->stackSize / sizeof(Address) / 8;

    ThreadLocals triggered_tl = (ThreadLocals) (current + (tlSize * 0));
    ThreadLocals enabled_tl   = (ThreadLocals) (current + (tlSize * 1));
    ThreadLocals disabled_tl  = (ThreadLocals) (current + (tlSize * 2));

    /* Clear each of the thread local spaces: */
    memset((void *) ((Address) triggered_tl + sizeof(Address)), 0, tlSize - sizeof(Address));
    memset((void *) enabled_tl, 0, tlSize);
    memset((void *) disabled_tl, 0, tlSize);

    setThreadLocal(enabled_tl, SAFEPOINTS_ENABLED_THREAD_LOCALS, enabled_tl);
    setThreadLocal(enabled_tl, SAFEPOINTS_DISABLED_THREAD_LOCALS, disabled_tl);
    setThreadLocal(enabled_tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS, triggered_tl);

    setThreadLocal(disabled_tl, SAFEPOINTS_ENABLED_THREAD_LOCALS, enabled_tl);
    setThreadLocal(disabled_tl, SAFEPOINTS_DISABLED_THREAD_LOCALS, disabled_tl);
    setThreadLocal(disabled_tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS, triggered_tl);

    setThreadLocal(triggered_tl, SAFEPOINTS_ENABLED_THREAD_LOCALS, enabled_tl);
    setThreadLocal(triggered_tl, SAFEPOINTS_DISABLED_THREAD_LOCALS, disabled_tl);
    setThreadLocal(triggered_tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS, triggered_tl);

    setThreadLocal(enabled_tl, SAFEPOINT_LATCH, enabled_tl);
    setThreadLocal(disabled_tl, SAFEPOINT_LATCH, disabled_tl);

    setThreadLocal(enabled_tl, NATIVE_THREAD_LOCALS, ntl);
    setThreadLocal(disabled_tl, NATIVE_THREAD_LOCALS, ntl);
    setThreadLocal(triggered_tl, NATIVE_THREAD_LOCALS, ntl);

    setThreadLocal(enabled_tl, ID, ntl->id);
    setThreadLocal(disabled_tl, ID, ntl->id);
    setThreadLocal(triggered_tl, ID, ntl->id);

    current = (Address) disabled_tl + tlSize;
    ntl->refMapArea = current;
    current = virtualMemory_pageAlign(current + refMapAreaSize);
    ntl->stackRedZone = current;
    current += virtualMemory_getPageSize();
    ntl->stackYellowZone = current;
    current += virtualMemory_getPageSize();
    initStackProtection(ntl);

#if log_THREADS
    int id = ntl->id;
    log_println("thread %3d: stackBase = %p", id, ntl->stackBase);
    log_println("thread %3d: stackBase (aligned) = %p", id, virtualMemory_pageAlign(ntl->stackBase));
    log_println("thread %3d: stackSize = %d (%p)", id, ntl->stackSize, ntl->stackSize);
    log_println("thread %3d: stackBottom = %p", id, stackBottom);
    log_println("thread %3d: triggeredVmThreadLocals = %p", id, triggered_tl);
    log_println("thread %3d: enabledVmThreadLocals   = %p", id, enabled_tl);
    log_println("thread %3d: disabledVmThreadLocals  = %p", id, disabled_tl);
    log_println("thread %3d: refMapArea = %p", id, ntl->refMapArea);
    log_println("thread %3d: redZone    = %p", id, ntl->stackRedZone);
    log_println("thread %3d: yellowZone = %p", id, ntl->stackYellowZone);
    log_println("thread %3d: blueZone   = %p", id, ntl->stackBlueZone);
    log_println("thread %3d: current    = %p", id, current);
    log_println("thread %3d: endOfStack = %p", id, ntl->stackBase + ntl->stackSize);
#endif

    /* make sure we didn't run out of space. */
    c_ASSERT(ntl->stackBase + ntl->stackSize > current);

    return enabled_tl;
}

void tryUnprotectPage(Address address) {
    if (address != (Address) 0) {
    	virtualMemory_unprotectPage(address);
    }
}

void thread_destroySegments(NativeThreadLocals ntl) {
#if !os_GUESTVMXEN
    /* unprotect pages so some other unfortunate soul doesn't get zapped when reusing the space */
    tryUnprotectPage(ntl->stackRedZone);
    tryUnprotectPage(ntl->stackYellowZone);
#if (os_LINUX || os_DARWIN)
    /* these platforms have an extra protected page for the triggered thread locals */
    tryUnprotectPage(virtualMemory_pageAlign(ntl->stackBase));
    /* the stack is free'd by the pthreads library. */
#endif
    // on GUESTVMXEN stack protection is handled elsewhere
#endif
}

/*
 * OS-specific thread creation, including allocation of the thread locals area and the stack.
 * Returns 0 in the case of failure.
 */
static Thread thread_create(jint id, Size stackSize, int priority) {
    Thread thread;
#if !os_GUESTVMXEN
    int error;
#endif

    if (virtualMemory_pageAlign(stackSize) != stackSize) {
        log_println("thread_create: thread stack size must be a multiple of the OS page size (%d)", virtualMemory_getPageSize());
        return (Thread) 0;
    }

#if log_THREADS
    log_println("thread_create: id = %d, stack size = %ld", id, stackSize);
#endif

    /* create the native thread locals and allocate stack if necessary */
    NativeThreadLocals ntl = thread_createSegments(id, stackSize);
    if (ntl == NULL) {
    	return (Thread) 0;
    }

#if os_GUESTVMXEN
    thread = guestvmXen_create_thread_with_stack("java_thread",
    	(void (*)(void *)) thread_runJava,
		(void*) ntl->stackBase,
	    ntl->stackSize,
		priority,
		(void*) ntl);
#elif (os_LINUX || os_DARWIN)
    pthread_attr_t attributes;
    pthread_attr_init(&attributes);
    pthread_attr_setstack(&attributes, (void *) ntl->stackBase, ntl->stackSize);
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);

    error = pthread_create(&thread, &attributes, (void *(*)(void *)) thread_runJava, ntl);
    pthread_attr_destroy(&attributes);
    if (error != 0) {
        log_println("pthread_create failed with error: %d", error);
        thread_destroySegments(ntl);
        return (Thread) 0;
    }
#elif os_SOLARIS
    /*
     * We let the system allocate the stack as doing so gets us a protected page
     * immediately below the bottom of the stack which is required for safepoints to work.
     */
    error = thr_create((void *) NULL, (size_t) stackSize, thread_runJava, ntl, THR_NEW_LWP | THR_BOUND, &thread);
    if (error != 0) {
        log_println("%s", strerror(error));
        log_println("thr_create failed with error: %d", error);
        thread_destroySegments(ntl);
        return (Thread) 0;
    }
#else
    c_UNIMPLEMENTED();
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
    c_UNIMPLEMENTED();
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
    c_UNIMPLEMENTED();
#endif

    if (error != 0) {
        log_println("thread_join failed with error: %d", error);
    }
    return error;
}

/**
 * The start routine called by the native threading library once the new thread starts.
 */
void *thread_runJava(void *arg) {
    NativeThreadLocals ntl = (NativeThreadLocals) arg;
    Address nativeThread = (Address) thread_current();

    c_ASSERT(ntl != NULL);

#if log_THREADS
    log_println("thread_runJava: BEGIN t=%p", nativeThread);
#endif

    /* set up the VM thread locals, guard pages, etc */
    ThreadLocals tl = thread_initSegments(ntl);
    thread_setThreadLocals(theThreadLocalsKey, (void *) tl);

    VMThreadRunMethod method = image_offset_as_address(VMThreadRunMethod, vmThreadRunMethodOffset);

#if log_THREADS
    log_print("thread_runJava: id=%d, t=%p, calling method: ", ntl->id, nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) method);
    log_println("");
#endif

    (*method)(ntl->id,
              nativeThread,
              ntl->stackBase,
              getThreadLocal(Address, tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS),
              getThreadLocal(Address, tl, SAFEPOINTS_ENABLED_THREAD_LOCALS),
              getThreadLocal(Address, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS),
              ntl->refMapArea,
              ntl->stackRedZone,
              ntl->stackYellowZone,
              ntl->stackBase + ntl->stackSize);

    /* destroy thread locals, deallocate stack, restore guard pages */
    thread_destroySegments(ntl);

#if log_THREADS
    log_println("thread_runJava: END t=%p", nativeThread);
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
    log_println("BEGIN nativeJoin: %p", thread);
#endif
    if (thread == 0L) {
        return false;
    }
    jboolean result = thread_join((Thread) thread) == 0;
#if log_THREADS
    log_println("END nativeJoin: %p", thread);
#endif
    return result;
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeYield(JNIEnv *env, jclass c) {
#if os_SOLARIS
    thr_yield();
#elif os_DARWIN
    sched_yield();
#elif os_LINUX
    pthread_yield();
#elif os_GUESTVMXEN
    guestvmXen_yield();
#else
    c_UNIMPLEMENTED();
#endif
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeInterrupt(JNIEnv *env, jclass c, Address nativeThread) {
#if log_MONITORS
    log_println("Interrupting thread %p", nativeThread);
#endif
#if os_SOLARIS
    // Signals the thread
    int result = thr_kill(nativeThread, SIGUSR1);
    if (result != 0) {
        log_exit(11, "Error sending signal SIGUSR1 to native thread %p", nativeThread);
    }
#elif os_LINUX || os_DARWIN
    // Signals the thread
    int result = pthread_kill((pthread_t) nativeThread, SIGUSR1);
    if (result != 0) {
        log_exit(11, "Error sending signal SIGUSR1 to native thread %p", nativeThread);
    }
#elif os_GUESTVMXEN
	guestvmXen_interrupt((void*) nativeThread);
#else
    c_UNIMPLEMENTED();
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
        log_println("thread %p: nativeSetPriority %d failed [%s]", thread_current(), priority, strerror(result));
    }
#elif os_GUESTVMXEN
    guestvmXen_set_priority((void *) nativeThread, priority);
#else
    log_println("nativeSetPriority %d ignored!", priority);
#endif
}
