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
 * @author Doug Simon
 */
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include "log.h"
#include "jni.h"
#include "word.h"
#include "image.h"

#include "threads.h"
#include "threadLocals.h"
#include "virtualMemory.h"
#include "mutex.h"

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_t Thread;
    typedef pthread_key_t ThreadLocalsKey;
    typedef void (*ThreadLocalsBlockDestructor)(void *);
#elif os_SOLARIS
#   include <thread.h>
    typedef thread_t Thread;
    typedef thread_key_t ThreadLocalsKey;
    typedef void (*ThreadLocalsBlockDestructor)(void *);
#elif os_GUESTVMXEN
#   include "guestvmXen.h"
    typedef guestvmXen_Thread Thread;
    typedef guestvmXen_SpecificsKey ThreadLocalsKey;
    typedef void (*ThreadLocalsBlockDestructor)(void *);
#endif

int theThreadLocalsAreaSize = -1;

/**
 * The global key used to retrieve a ThreadLocals object for a thread.
 */
static ThreadLocalsKey theThreadLocalsKey;

static Address allocateThreadLocalBlock(size_t tlBlockSize) {
#if os_GUESTVMXEN
	return (Address) guestvmXen_virtualMemory_allocate(tlBlockSize, DATA_VM);
#else
	return (Address) valloc(tlBlockSize);
#endif
}

static void deallocateThreadLocalBlock(Address tlBlock, Size tlBlockSize) {
#if os_GUESTVMXEN
	guestvmXen_virtualMemory_deallocate((void *) tlBlock, tlBlockSize, DATA_VM);
#else
	free ((void *) tlBlock);
#endif
}

/**
 * Allocates and initializes a thread locals block.
 *
 * @param id  > 0: the identifier reserved in the thread map for the thread being started
 *           == 0: the primordial thread
 *            < 0: temporary identifier (derived from the native thread handle) of a thread
 *                 that is being attached to the VM
 */
Address threadLocalsBlock_create(jint id) {

    c_ASSERT(threadLocalsBlock_current() == 0);

    const int tlSize = threadLocalsAreaSize();
    const int pageSize = virtualMemory_getPageSize();
    const jboolean attaching = id < 0;
    const jboolean primordial = id == 0;

    Size stackSize;
    Address stackBase;
    thread_getStackInfo(&stackBase, &stackSize);

    /* See diagram at top of threadLocals.h */
    const int triggerPageSize = pageSize;
    int stackWords = stackSize / sizeof(Address);
    Size refMapSize = primordial ? 0 : wordAlign(1 + (stackWords / 8));
    const int tlBlockSize = triggerPageSize +
                            (3 * tlSize) +
                            sizeof(NativeThreadLocalsStruct) +
                            (primordial ? 0 : refMapSize);

    c_ASSERT(wordAlign(tlBlockSize) == (Address) tlBlockSize);
    Address tlBlock = allocateThreadLocalBlock(tlBlockSize);
    if (tlBlock == 0) {
        return 0;
    }

    ThreadLocals triggered_tl = tlBlock + pageSize - sizeof(Address);
    ThreadLocals enabled_tl  = triggered_tl + tlSize;
    ThreadLocals disabled_tl = enabled_tl + tlSize;

    Address current = (Address) disabled_tl + tlSize;
    NativeThreadLocals ntl = (NativeThreadLocals) current;
    current += sizeof(NativeThreadLocalsStruct);
    Address refMap = current;
    if (!primordial) {
        current = current + refMapSize;
    }

    /* Clear each of the thread local spaces: */
    memset((void *) triggered_tl, 0, tlSize);
    memset((void *) enabled_tl, 0, tlSize);
    memset((void *) disabled_tl, 0, tlSize);

    /* Clear the NativeThreadLocals: */
    memset((void *) ntl, 0, sizeof(NativeThreadLocalsStruct));

    ntl->handle = (Address) thread_self();
    ntl->stackBase = stackBase;
    ntl->stackSize = stackSize;
    ntl->tlBlock = tlBlock;
    ntl->tlBlockSize = tlBlockSize;

    Address startGuardZone;
    int guardZonePages;
    if (primordial) {
        ntl->stackRedZone = 0;
        ntl->stackYellowZone = 0;
        startGuardZone = 0;
        guardZonePages = 0;
    } else if (!attaching) {
        /* Thread library creates a red-zone guard page just below the stack */
        ntl->stackRedZone = ntl->stackBase - (STACK_RED_ZONE_PAGES * pageSize);

        /* Yellow guard page is bottom page of stack */
        ntl->stackYellowZone = ntl->stackBase;

        startGuardZone = ntl->stackYellowZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES;
    } else {
        /* Cannot determine if the thread library created a red-zone guard page */
        ntl->stackRedZone = ntl->stackBase;

        /* Yellow guard page is just above red-zone */
        ntl->stackYellowZone = ntl->stackBase + (STACK_RED_ZONE_PAGES * pageSize);

        startGuardZone = ntl->stackRedZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES + STACK_RED_ZONE_PAGES;
    }

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

    setConstantThreadLocal(enabled_tl, NATIVE_THREAD_LOCALS, ntl);
    setConstantThreadLocal(enabled_tl, ID, id);
    setConstantThreadLocal(enabled_tl, STACK_REFERENCE_MAP, refMap);
    setConstantThreadLocal(enabled_tl, STACK_REFERENCE_MAP_SIZE, refMapSize);

    Address endGuardZone = startGuardZone + (guardZonePages * pageSize);
    Address sp = (Address) &ntl; // approximation of stack pointer
    const int safetyMargin = pageSize;
    if (sp < endGuardZone + safetyMargin) {
        log_exit(11, "Stack is too small to safely place stack guard zones");
    }

#if os_GUESTVMXEN
    // all page protection is handled in the following call
    guestvmXen_initStack(ntl);
#else
    ntl->stackBlueZone = ntl->stackYellowZone;

    if (guardZonePages != 0) {
        virtualMemory_protectPages(startGuardZone, guardZonePages);
    }
#endif

#if log_THREADS
    log_println("thread %3d: stackEnd     = %p", id, ntl->stackBase + ntl->stackSize);
    log_println("thread %3d: sp           ~ %p", id, &id);
    log_println("thread %3d: stackBase    = %p", id, ntl->stackBase);
    log_println("thread %3d: stackSize    = %d (%p)", id, ntl->stackSize, ntl->stackSize);
    log_println("thread %3d: redZone      = %p", id, ntl->stackRedZone);
    log_println("thread %3d: yellowZone   = %p", id, ntl->stackYellowZone);
    log_println("thread %3d: blueZone     = %p", id, ntl->stackBlueZone);
    log_println("thread %3d: triggered_tl = %p", id, triggered_tl);
    log_println("thread %3d: enabled_tl   = %p", id, enabled_tl);
    log_println("thread %3d: disabled_tl  = %p", id, disabled_tl);
    log_println("thread %3d: anchor       = %p", id, getThreadLocal(Address, enabled_tl, LAST_JAVA_FRAME_ANCHOR));
    log_println("thread %3d: ntl          = %p", id, ntl);
    log_println("thread %3d: refMap       = %p", id, refMap);
    log_println("thread %3d: refMapSize   = %d (%p)", id, refMapSize, refMapSize);
#endif

    /* Protect the first page of the TL block (which contains the first word of the triggered thread locals) */
    virtualMemory_protectPages(tlBlock, 1);

    threadLocalsBlock_setCurrent(tlBlock);
    return tlBlock;
}

/**
 * Declared in threads.c
 */
extern Mutex globalThreadAndGCLock;

/**
 * See the documentation in threadLocals.h for this function.
 */
void threadLocalsBlock_destroy(Address tlBlock) {
    // The native thread library de-registers the value for a thread local key
    // before calling the associated destructor
    c_ASSERT(threadLocalsBlock_current() == 0);

    // Temporarily re-register the block for the duration of this function
    // so that traps have a better chance of printing something useful
    threadLocalsBlock_setCurrent(tlBlock);

#if log_THREADS
    Address nativeThread = (Address) thread_self();
    log_println("threadLocalsBlock_destroy: BEGIN t=%p", nativeThread);
#endif

    Address tl = THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);

    int id = getThreadLocal(int, tl, ID);
    if (id >= 0) {
        VmThreadDetachMethod method = image_offset_as_address(VmThreadDetachMethod, vmThreadDetachMethodOffset);
#if log_THREADS
        log_print("threadLocalsBlock_destroy: id=%d, t=%p, calling VmThread.detach(): ", id, nativeThread);
        void image_printAddress(Address address);
        image_printAddress((Address) method);
        log_println("");
#endif
        (*method)(tl);
    } else {
#if log_THREADS
        log_print("threadLocalsBlock_destroy: id=%d, t=%p, never successfully attached: ", id, nativeThread);
#endif
    }

    c_ASSERT(getThreadLocal(Address, tl, FORWARD_LINK) == 0);
    c_ASSERT(getThreadLocal(Address, tl, BACKWARD_LINK) == 0);

    const jboolean attached = ntl->stackRedZone == ntl->stackBase;
    Address startGuardZone;
    int guardZonePages;
    if (!attached) {
        startGuardZone = ntl->stackYellowZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES;
    } else {
        startGuardZone = ntl->stackRedZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES + STACK_RED_ZONE_PAGES;
    }

    /* Unprotect the first page of the TL block which contains the first word of the triggered thread locals */
    virtualMemory_unprotectPages(tlBlock, 1);

    /* Unprotect the stack guard pages */
#if !os_GUESTVMXEN
    virtualMemory_unprotectPages(startGuardZone, guardZonePages);
#else
    // on GUESTVMXEN stack protection is handled elsewhere
#endif

    // Undo the temporary re-establishment of the thread locals block
    threadLocalsBlock_setCurrent(0);

    /* Release the memory of the TL block. */
    deallocateThreadLocalBlock(tlBlock, ntl->tlBlockSize);

#if log_THREADS
    log_println("threadLocalsBlock_destroy: END t=%p", nativeThread);
#endif
}

void threadLocals_initialize(int threadLocalsAreaSize) {
    theThreadLocalsAreaSize = threadLocalsAreaSize;
#if !TELE
#if os_DARWIN || os_LINUX
    pthread_key_create(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
#elif os_SOLARIS
    thr_keycreate(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
#elif os_GUESTVMXEN
    guestvmXen_thread_initializeSpecificsKey(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
#else
    c_UNIMPLEMENTED();
#endif
#endif
}


Address threadLocalsBlock_current() {
    Address tlBlock;
#if os_DARWIN || os_LINUX
    tlBlock = (Address) pthread_getspecific(theThreadLocalsKey);
#elif os_SOLARIS
    Address value;
    int result = thr_getspecific(theThreadLocalsKey, (void**) &value);
    if (result != 0) {
        log_exit(result, "thr_getspecific failed");
    }
    tlBlock = value;
#elif os_GUESTVMXEN
    tlBlock = (Address) guestvmXen_thread_getSpecific(theThreadLocalsKey);
#else
    c_UNIMPLEMENTED();
#endif
    return tlBlock;
}

void threadLocalsBlock_setCurrent(Address tlBlock) {
#if (os_DARWIN || os_LINUX)
    pthread_setspecific(theThreadLocalsKey, (void *) tlBlock);
#elif os_SOLARIS
    thr_setspecific(theThreadLocalsKey, (void *) tlBlock);
#elif os_GUESTVMXEN
    guestvmXen_thread_setSpecific(theThreadLocalsKey, (void *) tlBlock);
#endif
}

ThreadLocals threadLocals_current() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock == 0) {
        return 0;
    }
    return THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
}

NativeThreadLocals nativeThreadLocals_current() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock == 0) {
        return 0;
    }
    return NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
}

int threadLocalsAreaSize() {
    c_ASSERT(theThreadLocalsAreaSize > 0);
    return theThreadLocalsAreaSize;
}

void threadLocals_println(ThreadLocals tl) {
    NativeThreadLocals ntl = getThreadLocal(NativeThreadLocals, tl, NATIVE_THREAD_LOCALS);
    int id = getThreadLocal(int, tl, ID);
#if defined(_LP64)
    log_println("ThreadLocals[%d: base=%p, end=%p, size=%lu, tlBlock=%p, tlBlockSize=%lu]",
#else
    // for 32 bit host
    log_println("ThreadLocals[%d: base=%llx, end=%llx, size=%llu, tlBlock=%llx, tlBlockSize=%llu]",
#endif
                    id, ntl->stackBase, ntl->stackBase + ntl->stackSize, ntl->stackSize, ntl->tlBlock, ntl->tlBlockSize);
}

void threadLocals_printList(ThreadLocals tl) {
    while (tl != 0) {
        threadLocals_println(tl);
        tl = getThreadLocal(ThreadLocals, tl, FORWARD_LINK);
    };
}
