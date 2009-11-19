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

    int theThreadLocalsSize = -1;
int theJavaFrameAnchorSize = -1;

/**
 * The global key used to retrieve a ThreadLocals object for a thread.
 */
static ThreadLocalsKey theThreadLocalsKey;

/**
 * Allocates and initializes a thread locals block.
 *
 * @param id if >= 0, the identifier reserved in the thread map for the thread to be started otherwise
 *           a temporary identifier (derived from the native thread handle) for a thread that is being
 *           attached to the VM
 * @param refMap the value in which the address of the stack reference map will be returned. If NULL, then
 *           no memory is allocated for a stack reference map.
 */
Address threadLocalsBlock_create(jint id, Address *refMap) {

    c_ASSERT(threadLocalsBlock_current() == 0);

    const int tlSize = threadLocalsSize();
    const int jfaSize = javaFrameAnchorSize();
    const int pageSize = virtualMemory_getPageSize();
    const jboolean attaching = id < 0;

    Size stackSize;
    Address stackBase;
    thread_getStackInfo(&stackBase, &stackSize);

    Size refMapAreaSize = refMap == NULL ? 0 : 1 + stackSize / sizeof(Address) / 8;

    /* See diagram at top of threadLocals.h */
    const int triggerPage = pageSize;
    const int tlBlockSize =
                    triggerPage +
                    (3 * tlSize) +
                    sizeof(NativeThreadLocalsStruct) +
                    jfaSize +
                    refMapAreaSize;

    Address tlBlock = (Address) valloc(tlBlockSize);
    if (tlBlock == 0) {
        return 0;
    }

    ThreadLocals triggered_tl = tlBlock + pageSize - sizeof(Address);
    ThreadLocals enabled_tl  = triggered_tl + tlSize;
    ThreadLocals disabled_tl = enabled_tl + tlSize;

    Address current = (Address) disabled_tl + tlSize;
    NativeThreadLocals ntl = (NativeThreadLocals) current;
    current += sizeof(NativeThreadLocalsStruct);
    Address anchor = current;
    current += jfaSize;
    if (refMap != NULL) {
        *refMap = current;
        current = current + refMapAreaSize;
    }

    /* Clear each of the thread local spaces: */
    memset((void *) triggered_tl, 0, tlSize);
    memset((void *) enabled_tl, 0, tlSize);
    memset((void *) disabled_tl, 0, tlSize);

    /* Clear the base Java frame anchor: */
    memset((void *) anchor, 0, jfaSize);

    /* Clear the NativeThreadLocals: */
    memset((void *) ntl, 0, sizeof(NativeThreadLocalsStruct));

    ntl->stackBase = stackBase;
    ntl->stackSize = stackSize;
    
    Address startGuardZone;
    int guardZonePages;
    if (!attaching) {
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

#if os_GUESTVMXEN
    // all page protection is handled in the following call
    guestvmXen_initStack(ntl);
#else
    Address endGuardZone = startGuardZone + (guardZonePages * pageSize);
    Address sp = (Address) &ntl; // approximation of stack pointer
    const int safetyMargin = pageSize;
    if (sp < endGuardZone + safetyMargin) {
        log_exit(11, "Stack is too small to safely place stack guard zones");
    }

    ntl->stackBlueZone = ntl->stackYellowZone;

    /* Need to write to the yellow so that it is mapped in before being protected. */
    *((Address *) ntl->stackYellowZone) = 0;

    virtualMemory_protectPages(startGuardZone, guardZonePages);
#endif

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

    setThreadLocal(enabled_tl, LAST_JAVA_FRAME_ANCHOR, anchor);

#if log_THREADS
    log_println("thread %3d: stackBase = %p", id, ntl->stackBase);
    log_println("thread %3d: stackSize = %d (%p)", id, ntl->stackSize, ntl->stackSize);
    log_println("thread %3d: redZone    = %p", id, ntl->stackRedZone);
    log_println("thread %3d: yellowZone = %p", id, ntl->stackYellowZone);
    log_println("thread %3d: blueZone   = %p", id, ntl->stackBlueZone);
    log_println("thread %3d: endOfStack = %p", id, ntl->stackBase + ntl->stackSize);
    log_println("thread %3d: triggered_tl = %p", id, triggered_tl);
    log_println("thread %3d: enabled_tl   = %p", id, enabled_tl);
    log_println("thread %3d: disabled_tl  = %p", id, disabled_tl);
    log_println("thread %3d: anchor       = %p", id, anchor);
    log_println("thread %3d: ntl          = %p", id, ntl);
    if (refMap != NULL) {
    log_println("thread %3d: refMapArea   = %p", id, *refMap);
    }
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

void threadLocalsBlock_destroy(Address tlBlock) {
    c_ASSERT(threadLocalsBlock_current() == 0);

#if log_THREADS
    Address nativeThread = (Address) thread_self();
    log_println("threadLocalsBlock_destroy: BEGIN t=%p", nativeThread);
#endif

    /* Grab the global thread and GC lock so that:
     *   1. We can safely remove this thread from the thread list and thread map.
     *   2. We are blocked if a GC is currently underway. Once we have the lock,
     *      GC is blocked and cannot occur until we complete the upcall to
     *      VmThread.detach().
     */
    mutex_enter(globalThreadAndGCLock);

    Address tl = THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
    VmThreadDetachMethod method = image_offset_as_address(VmThreadDetachMethod, vmThreadDetachMethodOffset);
#if log_THREADS
    log_print("threadLocalsBlock_destroy: id=%d, t=%p, calling method: ", getThreadLocal(int, tl, ID), nativeThread);
    void image_printAddress(Address address);
    image_printAddress((Address) method);
    log_println("");
#endif
    (*method)(tl);

    mutex_exit(globalThreadAndGCLock);

    c_ASSERT(getThreadLocal(Address, tl, FORWARD_LINK) == 0);
    c_ASSERT(getThreadLocal(Address, tl, BACKWARD_LINK) == 0);

    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
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

    /* Release the memory of the TL block. */
    free((void *) tlBlock);

#if log_THREADS
    log_println("threadLocalsBlock_destroy: END t=%p", nativeThread);
#endif
}

void threadLocals_initialize(int threadLocalsSize, int javaFrameAnchorSize) {
    theThreadLocalsSize = threadLocalsSize;
    theJavaFrameAnchorSize = javaFrameAnchorSize;
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

int threadLocalsSize() {
    c_ASSERT(theThreadLocalsSize > 0);
    return theThreadLocalsSize;
}

int javaFrameAnchorSize() {
    c_ASSERT(theJavaFrameAnchorSize > 0);
    return theJavaFrameAnchorSize;
}

void threadLocals_println(ThreadLocals tl) {
    NativeThreadLocals ntl = getThreadLocal(NativeThreadLocals, tl, NATIVE_THREAD_LOCALS);
    int id = getThreadLocal(int, tl, ID);
    log_println("ThreadLocals[%d: base=%p, end=%p, size=%lu, triggered=%p, enabled=%p, disabled=%p]",
                    id, ntl->stackBase, ntl->stackBase + ntl->stackSize, ntl->stackSize,
                    getThreadLocal(Address, tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS),
                    getThreadLocal(Address, tl, SAFEPOINTS_ENABLED_THREAD_LOCALS),
                    getThreadLocal(Address, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS));
}

void threadLocals_printList(ThreadLocals tl) {
    while (tl != 0) {
        threadLocals_println(tl);
        tl = getThreadLocal(ThreadLocals, tl, FORWARD_LINK);
    };
}
