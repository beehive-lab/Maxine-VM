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
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Paul Caprioli
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
#include <limits.h>
#include "log.h"
#include "image.h"
#include "jni.h"
#include "word.h"
#include "mutex.h"
#include "trap.h"
#include "threads.h"
#include "threadLocals.h"
#include <sys/mman.h>

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_t Thread;
#define thread_current() ((Thread) pthread_self())
#elif os_SOLARIS
#   include <thread.h>
    typedef thread_t Thread;
#define thread_current() (thr_self())
#elif os_MAXVE
#   include "maxve.h"
    typedef maxve_Thread Thread;
#define thread_current() (maxve_get_current())
#endif

/**
 * The native mutex associated with VmThreadMap.THREAD_LOCK.
 */
Mutex globalThreadLock;

/**
 * Gets the address and size of the calling thread's stack. The returned values denote
 * the stack memory above the red-zone guard page (if any) configured by the native thread library.
 *
 * @param stackBase the base (i.e. lowest) address of the stack is returned in this argument
 * @param stackSize the size of the stack is returned in this argument
 */
void thread_getStackInfo(Address *stackBase, Size* stackSize) {
#if os_SOLARIS
    stack_t stackInfo;
    int result = thr_stksegment(&stackInfo);
    if (result != 0) {
        log_exit(result, "Could not get the address and size of the current thread [%s]", strerror(result));
    }
    *stackSize = stackInfo.ss_size;
    *stackBase = (Address) stackInfo.ss_sp - stackInfo.ss_size;
#elif os_LINUX
    pthread_attr_t attr;
    int result = pthread_getattr_np(pthread_self(), &attr);
    if (result != 0) {
        log_exit(result, "Could not get the address and size of the current thread [%s]", strerror(result));
    }
    result = pthread_attr_getstack(&attr, (void**) stackBase, (size_t *) stackSize);
    if (result != 0) {
        log_exit(11, "Cannot locate current stack attributes [%s]", strerror(result));
    }

    size_t guardSize;
    result = pthread_attr_getguardsize(&attr, &guardSize);
    if (result != 0) {
        log_exit(11, "Cannot locate current stack guard size [%s]", strerror(result));
    }
    if (guardSize != 0) {
        *stackSize -= guardSize;
        *stackBase += guardSize;
    }

    pthread_attr_destroy(&attr);
#elif os_DARWIN
    pthread_t self = pthread_self();
    void *stackTop = pthread_get_stackaddr_np(self);
    if (stackTop == NULL) {
        log_exit(11, "Cannot get current stack address");
    }
    *stackSize = pthread_get_stacksize_np(self);
    if (*stackSize == 0) {
        log_exit(11, "Cannot get current stack size");
    }
    *stackBase = (Address) stackTop - *stackSize;
#elif os_MAXVE
    maxve_stackinfo_t stackInfo;
    maxve_get_stack_info(&stackInfo);
    *stackBase = stackInfo.ss_base;
    *stackSize = stackInfo.ss_size;
#else
    c_UNIMPLEMENTED();
#endif

}

/**
 * OS-specific thread creation.
 *
 * @param id the identifier reserved in the thread map for the thread to be started
 * @param stackSize the requested size of the thread's stack
 * @param priority the initial priority of the thread
 * @return the native thread handle (e.g. pthread_self()) of the started thread or 0 in the case of failure
 */
static Thread thread_create(jint id, Size stackSize, int priority) {
    Thread thread;
#if !os_MAXVE
    int error;
#endif

    if (virtualMemory_pageAlign(stackSize) != stackSize) {
        log_println("thread_create: thread stack size must be a multiple of the OS page size (%d)", virtualMemory_getPageSize());
        return (Thread) 0;
    }

#if log_THREADS
    log_println("thread_create: id = %d, stack size = %ld", id, stackSize);
#endif

#if (os_LINUX || os_DARWIN)
    if (stackSize < PTHREAD_STACK_MIN) {
        stackSize = PTHREAD_STACK_MIN;
    }
#endif

    // Allocate the threadLocals block and the struct for passing this to the created thread.
    // We do this to ensure that all memory allocation problems are addressed here before the thread runs.
    Address tlBlock = threadLocalsBlock_create(id, 0, stackSize);
    if (tlBlock == 0) {
        return (Thread) 0;
    }

    TLA etla = ETLA_FROM_TLBLOCK(tlBlock);
    tla_store(etla, ID, id);

#if os_MAXVE
    thread = maxve_create_thread(
    	(void (*)(void *)) thread_run,
	    stackSize,
		priority,
		(void *) tlBlock);
    if (thread == NULL) {
        return (Thread) 0;
    }
#elif (os_LINUX || os_DARWIN)
    pthread_attr_t attributes;
    pthread_attr_init(&attributes);

    /* The thread library allocates the stack and sets the red-zone
     * guard page at (Linux) or just below (Darwin) the bottom of the stack. */
    pthread_attr_setstacksize(&attributes, stackSize);
    pthread_attr_setguardsize(&attributes, virtualMemory_getPageSize());
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);

    error = pthread_create(&thread, &attributes, (void *(*)(void *)) thread_run, (void *) tlBlock);
    pthread_attr_destroy(&attributes);
    if (error != 0) {
        log_println("pthread_create failed with error: %d", error);
        return (Thread) 0;
    }
#elif os_SOLARIS
    if (stackSize < thr_min_stack()) {
        stackSize = thr_min_stack();
    }
    /* The thread library allocates the stack and sets the red-zone
     * guard page just below the bottom of the stack. */
    error = thr_create((void *) NULL, (size_t) stackSize, thread_run, (void *) tlBlock, THR_NEW_LWP | THR_BOUND, &thread);
    if (error != 0) {
        log_println("thr_create failed with error: %d [%s]", error, strerror(error));
        return (Thread) 0;
    }
#else
    c_UNIMPLEMENTED();
#endif
    return thread;
}

void *thread_self() {
    return (void *) thread_current();
}

static int thread_join(Thread thread) {
    int error;
#if (os_DARWIN || os_LINUX)
    int status;
    error = pthread_join(thread, (void **) &status);
#elif os_SOLARIS
    void *status;
    error = thr_join(thread, NULL, &status);
#elif os_MAXVE
    error = maxve_thread_join(thread);
#else
    c_UNIMPLEMENTED();
#endif

    if (error != 0) {
        log_println("Joining thread %p with thread %p failed (%s %d)", thread_current(), thread, strerror(error), error);
    }
    return error;
}

/**
 * The start routine called by the native threading library once the new thread starts.
 *
 * @param arg the pre-allocated, but uninitialized, thread locals block.
 */
void *thread_run(void *arg) {

    Address tlBlock = (Address) arg;
    TLA etla = ETLA_FROM_TLBLOCK(tlBlock);
    jint id = tla_load(jint, etla, ID);
    Address nativeThread = (Address) thread_current();

#if log_THREADS
    log_println("thread_run: BEGIN t=%p", nativeThread);
#endif

    threadLocalsBlock_setCurrent(tlBlock);
    // initialize the thread locals block
    if (id != PRIMORDIAL_THREAD_ID) {
        threadLocalsBlock_create(id, tlBlock, 0);
    }
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);

    /* Grab the global thread lock so that:
     *   1. This thread can atomically be added to the thread list
     *   2. This thread is blocked if a GC is currently underway. Once we have the lock,
     *      GC is blocked and cannot occur until we completed the upcall to
     *      VmThread.add().
     */
#if log_THREADS
    log_println("thread_run: t=%p acquiring global thread lock", nativeThread);
#endif
    mutex_enter(globalThreadLock);
#if log_THREADS
    log_println("thread_run: t=%p acquired  global thread lock", nativeThread);
#endif

    VmThreadAddMethod addMethod = image_offset_as_address(VmThreadAddMethod, vmThreadAddMethodOffset);

#if log_THREADS
    log_print("thread_run: id=%d, t=%p, calling VmThread.add(): ", id, nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) addMethod);
    log_println("");
#endif
    Address stackEnd = ntl->stackBase + ntl->stackSize;
    int result = (*addMethod)(id,
              false,
              nativeThread,
              etla,
              ntl->stackBase,
              stackEnd,
              ntl->yellowZone);

#if log_THREADS
    log_println("thread_run: t=%p releasing global GC and thread list lock", nativeThread);
#endif
    mutex_exit(globalThreadLock);
#if log_THREADS
    log_println("thread_run: t=%p released  global GC and thread list lock", nativeThread);
#endif

    /* Adding a VM created thread to the thread list should never fail. */
    c_ASSERT(result == 0 || result == 1);
    setCurrentThreadSignalMask(result == 1);

    VmThreadRunMethod runMethod = image_offset_as_address(VmThreadRunMethod, vmThreadRunMethodOffset);

#if log_THREADS
    log_print("thread_run: id=%d, t=%p, calling VmThread.run(): ", id, nativeThread);
    image_printAddress((Address) runMethod);
    log_println("");
#endif
    (*runMethod)(etla, ntl->stackBase, stackEnd);

#if log_THREADS
    log_println("thread_run: END t=%p", nativeThread);
#endif
    /* Successful thread exit */
    return NULL;
}

/**
 * Support for the AttachCurrentThread/AttachCurrentThreadAsDaemon JNI functions.
 *
 * @param penv the JNIEnv pointer for the attached thread is returned in this value
 */
int thread_attachCurrent(void **penv, JavaVMAttachArgs* args, boolean daemon) {
    Address nativeThread = (Address) thread_current();
#if log_THREADS
    log_println("thread_attach: BEGIN t=%p", nativeThread);
#endif
    if (tla_current() != 0) {
        // If the thread has been attached, this operation is a no-op
        extern JNIEnv *currentJniEnv();
        *penv = (void *) currentJniEnv();
#if log_THREADS
    log_println("thread_attach: END t=%p (already attached)", nativeThread);
#endif
        return JNI_OK;
    }

    /* Give the thread a temporary id based on its native handle. The id must
     * be negative to indicate that it is not (yet) in the thread map. */
    jint handle = (jint) nativeThread;
    jint id = handle < 0 ? handle : -handle;

    Address tlBlock = threadLocalsBlock_create(id, 0, 0);
    if (tlBlock == 0) {
        return JNI_ENOMEM;
    }
    TLA etla = ETLA_FROM_TLBLOCK(tlBlock);
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);

    while (true) {

        /* Grab the global thread lock so that:
         *   1. This thread can atomically be added to the thread list
         *   2. This thread is blocked if a GC is currently underway. Once we have the lock,
         *      GC is blocked and cannot occur until we completed the upcall to
         *      VmThread.attach().
         */
#if log_THREADS
    log_println("thread_attach: t=%p acquiring global GC and thread list lock", nativeThread);
#endif
        mutex_enter(globalThreadLock);

        VmThreadAddMethod addMethod = image_offset_as_address(VmThreadAddMethod, vmThreadAddMethodOffset);

#if log_THREADS
        log_print("thread_attach: id=%d, t=%p, calling VmThread.add(): ", id, nativeThread);
        void image_printAddress(Address address);
        image_printAddress((Address) addMethod);
        log_println("");
#endif
        Address stackEnd = ntl->stackBase + ntl->stackSize;
        int result = (*addMethod)(id,
                        daemon,
                        nativeThread,
                        etla,
                        ntl->stackBase,
                        stackEnd,
                        ntl->yellowZone);
        mutex_exit(globalThreadLock);
#if log_THREADS
    log_println("thread_attach: t=%p released global GC and thread list lock", nativeThread);
#endif

        if (result == 0) {
            id = tla_load(jint, etla, ID);

            /* TODO: Save current thread signal mask so that it can be restored when this thread is detached. */
            setCurrentThreadSignalMask(false);
            break;
        } else if (result == -1) {
#if log_THREADS
            log_print("thread_attach: id=%d, t=%p, lost race for thread-for-attach object; trying again in 1ms", id, nativeThread);
#endif
            /* Short sleep to allow one of the other attaching threads to allocate and register the
             * next thread-for-attach object. */
            thread_sleep(1);
        } else {
            c_ASSERT(result == -2);
#if log_THREADS
            log_print("thread_attach: id=%d, t=%p, cannot attach - main thread has terminated", id, nativeThread);
#endif
            thread_detachCurrent();
            return JNI_EDETACHED;
        }
    }

    VmThreadAttachMethod attachMethod = image_offset_as_address(VmThreadAttachMethod, vmThreadAttachMethodOffset);
#if log_THREADS
    log_print("thread_attach: id=%d, t=%p, calling VmThread.attach(): ", id, nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) attachMethod);
    log_println("");
#endif
    Address stackEnd = ntl->stackBase + ntl->stackSize;
    int result = (*attachMethod)(
              (Address) (args == NULL ? NULL : args->name),
              (Address) (args == NULL ? NULL : args->group),
              daemon,
              ntl->stackBase,
              stackEnd,
              etla);

#if log_THREADS
    log_println("thread_attach: END id=%d, t=%p", id, nativeThread);
#endif

    if (result == JNI_OK) {
        *penv = (JNIEnv *) tla_addressOf(etla, JNI_ENV);
    } else {
        if (result == JNI_EDETACHED) {
            log_println("Cannot attach thread to a VM whose main thread has exited");
        }
        *penv = NULL;
    }
    return result;
}

int thread_detachCurrent() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock == 0) {
        // If the thread has been detached, this operation is a no-op
#if log_THREADS
    log_println("thread_detach: END (already detached)");
#endif
        return JNI_OK;
    }
    threadLocalsBlock_setCurrent(0);
    threadLocalsBlock_destroy(tlBlock);
    return JNI_OK;
}

/**
 * Declared in VmThreadMap.java.
 */
void nativeSetGlobalThreadLock(Mutex mutex) {
#if log_THREADS
    log_println("Global thread lock mutex: %p", mutex);
#endif
    globalThreadLock = mutex;
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
jboolean nonJniNativeJoin(Address thread) {
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

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeJoin(JNIEnv *env, jclass c, Address thread) {
	return nonJniNativeJoin(thread);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_thread_VmThread_nativeYield(JNIEnv *env, jclass c) {
#if os_SOLARIS
    thr_yield();
#elif os_DARWIN
    sched_yield();
#elif os_LINUX
    pthread_yield();
#elif os_MAXVE
    maxve_yield();
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
#elif os_MAXVE
	maxve_interrupt((void*) nativeThread);
#else
    c_UNIMPLEMENTED();
 #endif
}

jboolean thread_sleep(jlong numberOfMilliSeconds) {
#if os_MAXVE
    return maxve_sleep(numberOfMilliSeconds * 1000000);
#else
    struct timespec time, remainder;

    time.tv_sec = numberOfMilliSeconds / 1000;
    time.tv_nsec = (numberOfMilliSeconds % 1000) * 1000000;
    int value = nanosleep(&time, &remainder);

    if (value == -1) {
        int error = errno;
        if (error != EINTR && error != 0) {
            /* log_println("Call to nanosleep failed (other than by being interrupted): %s [remaining sec: %d, remaining nano sec: %d]", strerror(error), remainder.tv_sec, remainder.tv_nsec); */
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
    int err = thr_setprio(nativeThread, priority);
    c_ASSERT(err != ESRCH);
    c_ASSERT(err != EINVAL);
#elif os_MAXVE
    maxve_set_priority((void *) nativeThread, priority);
#else
    //    log_println("nativeSetPriority %d ignored!", priority);
#endif
}
