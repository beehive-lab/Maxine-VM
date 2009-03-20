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
#include "messenger.h"

#include "threads.h"
#include "threadSpecifics.h"
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

/**
 * The list of 'threadSpecifics' for the active Java threads. This list is only allocated and
 * updated if a debugger is attached to the VM. All modifications to the list are synchronized.
 *
 * Once 'threads_initialize()' has been called, the address of this list is accessible from Java
 * via the '_threadSpecificsList' static field in the VmThread class.
 */
static ThreadSpecificsList _threadSpecificsList = NULL;

void threads_initialize(Address primordialVmThreadLocals, Size vmThreadLocalsSize) {
#if os_DARWIN || os_LINUX
    pthread_key_create(&_specificsKey, free);
#elif os_SOLARIS
    thr_keycreate(&_specificsKey, free);
#elif os_GUESTVMXEN
    guestvmXen_thread_initializeSpecificsKey(&_specificsKey, free);
#else
    c_UNIMPLEMENTED();
#endif
    if (debugger_attached()) {
        _threadSpecificsList = calloc(1, sizeof(ThreadSpecificsListStruct));
        if (_threadSpecificsList == NULL) {
            log_exit(11, "Could not allocate memory for 'threadSpecificsList'");
        }
        mutex_initialize(&(_threadSpecificsList->lock));

        /* Set the pointer in the boot image to the global thread specifics list
         * so that it can be found by the debugger. */
        image_write_value(ThreadSpecificsList, threadSpecificsListOffset, _threadSpecificsList);

#if log_THREADS
        log_println("Allocated global thread specifics list at %p", _threadSpecificsList);
#endif

        /* Create thread specifics for the primordial thread so that its VM thread locals
         * can be accessed by the debugger. */
        ThreadSpecifics primordialThreadSpecifics = calloc(1, sizeof(ThreadSpecificsStruct));
        if (primordialThreadSpecifics == NULL) {
            log_exit(11, "Could not allocate primoridial thread specifics.");
        }
        primordialThreadSpecifics->next = primordialThreadSpecifics;
        primordialThreadSpecifics->triggeredVmThreadLocals = primordialVmThreadLocals;
        primordialThreadSpecifics->enabledVmThreadLocals = primordialVmThreadLocals;
        primordialThreadSpecifics->disabledVmThreadLocals = primordialVmThreadLocals;

#if os_SOLARIS
        stack_t stackInfo;
        int result = thr_stksegment(&stackInfo);

        if (result != 0) {
            log_exit(result, "thr_stksegment failed");
        }

        primordialThreadSpecifics->stackSize = stackInfo.ss_size;
        primordialThreadSpecifics->stackBase = (Address) stackInfo.ss_sp - stackInfo.ss_size;
#elif os_DARWIN || os_LINUX
        /* There's no support for finding the base and size of the current thread's stack in Linux or Darwin
         * so we simply make a guess based the stack memory allocated (via alloca)
         * in the stack frame maxine() in maxine.c and the default stack size for a pthread. */
        pthread_attr_t attr;
        size_t stackSize;
        pthread_attr_init(&attr);
        pthread_attr_getstacksize (&attr, &stackSize);
        Address stackEnd = primordialVmThreadLocals + vmThreadLocalsSize;
        primordialThreadSpecifics->stackBase = stackEnd - stackSize;
        primordialThreadSpecifics->stackSize = stackSize;
        pthread_attr_destroy(&attr);
#elif os_GUESTVMXEN
        stackinfo_t stackInfo;
        guestvmXen_get_stack_info(&stackInfo);
        primordialThreadSpecifics->stackBase = stackInfo.ss_sp - stackInfo.ss_size;
        primordialThreadSpecifics->stackSize = stackInfo.ss_size;
#else
        c_UNIMPLEMENTED();
#endif
        threadSpecificsList_add(_threadSpecificsList, primordialThreadSpecifics);
#if log_THREADS
        log_print("Added to global thread specifics list: ");
        threadSpecifics_println(primordialThreadSpecifics);
#endif
    }
}

ThreadSpecifics thread_currentSpecifics() {
#if os_DARWIN || os_LINUX
    return (ThreadSpecifics) pthread_getspecific(_specificsKey);
#elif os_SOLARIS
    ThreadSpecifics value;
    int result = thr_getspecific(_specificsKey, (void**) &value);
    if (result != 0) {
        log_exit(result, "thr_getspecific failed");
    }
    return value;
#elif os_GUESTVMXEN
    return (ThreadSpecifics) guestvmXen_thread_getSpecific(_specificsKey);
#else
    c_UNIMPLEMENTED();
#endif
}

ThreadSpecifics thread_createSegments(int id, Size stackSize) {
    ThreadSpecifics threadSpecifics = calloc(1, sizeof(ThreadSpecificsStruct));
    if (threadSpecifics == NULL) {
    	return NULL;
    }
    threadSpecifics->id = id;
    threadSpecifics->next = threadSpecifics;

#if os_SOLARIS
    // stack is allocated as part of Solaris thread creation
#else
#if (os_LINUX || os_DARWIN)
    threadSpecifics->stackBase = (Address) malloc(stackSize);
#elif os_GUESTVMXEN
    threadSpecifics->stackBase = (Address) guestvmXen_allocate_stack(threadSpecifics, stackSize);
#endif
    if (threadSpecifics->stackBase == 0) {
    	free(threadSpecifics);
    	threadSpecifics = NULL;
    } else {
        threadSpecifics->stackSize = stackSize;
    }
#endif
    return threadSpecifics;
}

void initStackProtection(ThreadSpecifics threadSpecifics) {
#if os_GUESTVMXEN
	// all page protection is handled in following call
	guestvmXen_initStack(threadSpecifics);
#else
	threadSpecifics->stackBlueZone = threadSpecifics->stackYellowZone;
    virtualMemory_protectPage(threadSpecifics->stackRedZone);
    virtualMemory_protectPage(threadSpecifics->stackYellowZone);
#if !os_SOLARIS
    virtualMemory_protectPage(virtualMemory_pageAlign(threadSpecifics->stackBase));
#endif
#endif

}

void thread_initSegments(ThreadSpecifics threadSpecifics) {
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
    /* N.B. do not read or write the contents of the stack until initStackProtection returns. */
    stackBottom = virtualMemory_pageAlign(threadSpecifics->stackBase) + virtualMemory_getPageSize();
#endif
    int vmThreadLocalsSize = image_header()->vmThreadLocalsSize;
    Address current = stackBottom - sizeof(Address);
    Size refMapAreaSize = 1 + threadSpecifics->stackSize / sizeof(Address) / 8;

    threadSpecifics->triggeredVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->enabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->disabledVmThreadLocals = current;
    current += vmThreadLocalsSize;
    threadSpecifics->refMapArea = current;
    current = virtualMemory_pageAlign(current + refMapAreaSize);
    threadSpecifics->stackRedZone = current;
    current += virtualMemory_getPageSize();
    threadSpecifics->stackYellowZone = current;
    current += virtualMemory_getPageSize();
    initStackProtection(threadSpecifics);

    /* be sure to clear each of the thread local spaces */
    memset((void *) (threadSpecifics->triggeredVmThreadLocals + sizeof(Address)), 0, vmThreadLocalsSize * 3);

#if log_THREADS
    int id = threadSpecifics->id;
    log_println("thread %3d: stackBase = %p", id, threadSpecifics->stackBase);
    log_println("thread %3d: stackBase (aligned) = %p", id, virtualMemory_pageAlign(threadSpecifics->stackBase));
    log_println("thread %3d: stackSize = %d (%p)", id, threadSpecifics->stackSize, threadSpecifics->stackSize);
    log_println("thread %3d: stackBottom = %p", id, stackBottom);
    log_println("thread %3d: triggeredVmThreadLocals = %p", id, threadSpecifics->triggeredVmThreadLocals);
    log_println("thread %3d: enabledVmThreadLocals   = %p", id, threadSpecifics->enabledVmThreadLocals);
    log_println("thread %3d: disabledVmThreadLocals  = %p", id, threadSpecifics->disabledVmThreadLocals);
    log_println("thread %3d: refMapArea = %p", id, threadSpecifics->refMapArea);
    log_println("thread %3d: redZone    = %p", id, threadSpecifics->stackRedZone);
    log_println("thread %3d: yellowZone = %p", id, threadSpecifics->stackYellowZone);
    log_println("thread %3d: blueZone   = %p", id, threadSpecifics->stackBlueZone);
    log_println("thread %3d: current    = %p", id, current);
    log_println("thread %3d: endOfStack = %p", id, threadSpecifics->stackBase + threadSpecifics->stackSize);
#endif

  /* make sure we didn't run out of space. */
  c_ASSERT(threadSpecifics->stackBase + threadSpecifics->stackSize > current);

 }

void tryUnprotectPage(Address address) {
    if (address != (Address) 0) {
    	virtualMemory_unprotectPage(address);
    }
}

void thread_destroySegments(ThreadSpecifics threadSpecifics) {
#if !os_GUESTVMXEN
    /* unprotect pages so some other unfortunate soul doesn't get zapped when reusing the space */
    tryUnprotectPage(threadSpecifics->stackRedZone);
    tryUnprotectPage(threadSpecifics->stackYellowZone);
#if (os_LINUX || os_DARWIN)
    /* these platforms have an extra protected page for the triggered thread locals */
    tryUnprotectPage(virtualMemory_pageAlign(threadSpecifics->stackBase));
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
    ThreadSpecifics threadSpecifics = thread_createSegments(id, stackSize);
    if (threadSpecifics == NULL) {
    	return (Thread) 0;
    }

    if (_threadSpecificsList != NULL) {
        /* Add 'threadSpecifics' to the global list so that the debugger can find it. */
        /* It is removed when a thread terminates; see the end of 'thread_runJava()'. */
        threadSpecificsList_add(_threadSpecificsList, threadSpecifics);
#if log_THREADS
        log_print("Added to global thread specifics list: ");
        threadSpecifics_println(threadSpecifics);
#endif
    }

#if os_GUESTVMXEN
    thread = guestvmXen_create_thread_with_stack("java_thread",
    	(void (*)(void *)) thread_runJava,
		(void*) threadSpecifics->stackBase,
	    threadSpecifics->stackSize,
		priority,
		(void*) threadSpecifics);
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
    ThreadSpecifics threadSpecifics = (ThreadSpecifics) arg;
    Address nativeThread = (Address) thread_current();

    c_ASSERT(threadSpecifics != NULL);
    thread_setSpecific(_specificsKey, threadSpecifics);

#if log_THREADS
    log_println("thread_runJava: BEGIN t=%p", nativeThread);
#endif

    /* set up the vm thread locals, guard pages, etc */
    thread_initSegments(threadSpecifics);

#if (os_GUESTVMXEN)
    /* mark this thread as a java thread */
    guestvmXen_set_javaId((Thread)nativeThread, threadSpecifics->id);
#endif

    VMThreadRunMethod method = image_offset_as_address(VMThreadRunMethod, vmThreadRunMethodOffset);

#if log_THREADS
    log_print("thread_runJava: id=%d, t=%p, calling method: ", threadSpecifics->id, nativeThread);
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

    if (_threadSpecificsList != NULL) {
        /* remove 'threadSpecifics' from the list known by the debugger; from this point on
         * the thread will be detected as a non-Java thread. */
        threadSpecificsList_remove(_threadSpecificsList, threadSpecifics);
#if log_THREADS
        log_print("Removed from global thread specifics list: ");
        threadSpecifics_println(threadSpecifics);
#endif
    }

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
#elif os_GUESTVMXEN
    guestvmXen_yield();
#else
    log_println("nativeYield ignored!");
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
