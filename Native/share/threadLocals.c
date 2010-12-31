/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
#elif os_MAXVE
#   include "maxve.h"
    typedef maxve_Thread Thread;
    typedef maxve_SpecificsKey ThreadLocalsKey;
    typedef void (*ThreadLocalsBlockDestructor)(void *);
#endif

int theTLASize = -1;

/**
 * The global key used to retrieve a ThreadLocals object for a thread.
 */
static ThreadLocalsKey theThreadLocalsKey;

static Address allocateThreadLocalBlock(size_t tlBlockSize) {
#if os_MAXVE
	return (Address) maxve_virtualMemory_allocate(tlBlockSize, DATA_VM);
#else
	return (Address) valloc(tlBlockSize);
#endif
}

static void deallocateThreadLocalBlock(Address tlBlock, Size tlBlockSize) {
#if os_MAXVE
	maxve_virtualMemory_deallocate((void *) tlBlock, tlBlockSize, DATA_VM);
#else
	free ((void *) tlBlock);
#endif
}

/**
 * Allocates and/or initializes a thread locals block.
 *
 * @param id  > 0: the identifier reserved in the thread map for the thread being started
 *           == 0: the primordial thread
 *            < 0: temporary identifier (derived from the native thread handle) of a thread
 *                 that is being attached to the VM
 * @param init true iff initializing a previously created thread locals block
 * @param stackSize only set if id > 0 && init == false;
 */
Address threadLocalsBlock_create(jint id, jboolean init, Size stackSize) {
    Address tlBlock;
    const int s = tlaSize();
    const int tlaSize = s;
    const int pageSize = virtualMemory_getPageSize();
    const jboolean attaching = id < 0;
    const jboolean primordial = id == 0;
    const jboolean threadIsCreated = init || attaching || primordial;

    Address stackBase;
    if (threadIsCreated) {
        thread_getStackInfo(&stackBase, &stackSize);
    }

    if (init) {
        tlBlock = threadLocalsBlock_current();
        c_ASSERT(tlBlock != 0);
    }

    /* See diagram at top of threadLocals.h */
    const int triggerPageSize = pageSize;
    int stackWords = stackSize / sizeof(Address);
    Size refMapSize = primordial ? 0 : wordAlign(1 + (stackWords / 8));
    const int tlBlockSize = triggerPageSize +
                            (3 * tlaSize) +
                            sizeof(NativeThreadLocalsStruct) +
                            (primordial ? 0 : refMapSize);

    c_ASSERT(wordAlign(tlBlockSize) == (Address) tlBlockSize);
    if (!init) {
        tlBlock= allocateThreadLocalBlock(tlBlockSize);
        // if we are creating a VM thread, initialization is deferred.
        if (!(attaching || primordial)) {
            return tlBlock;
        }
    }

    TLA ttla = tlBlock + pageSize - sizeof(Address);
    TLA etla  = ttla + tlaSize;
    TLA dtla = etla + tlaSize;

    Address current = (Address) dtla + tlaSize;
    NativeThreadLocals ntl = (NativeThreadLocals) current;
    current += sizeof(NativeThreadLocalsStruct);
    Address refMap = current;
    if (!primordial) {
        current = current + refMapSize;
    }

    /* Clear each of the thread local spaces: */
    memset((void *) ttla, 0, tlaSize);
    memset((void *) etla, 0, tlaSize);
    memset((void *) dtla, 0, tlaSize);

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
        ntl->stackRedZoneIsProtectedByVM = false;

        /* Yellow guard page is bottom page of stack */
        ntl->stackYellowZone = ntl->stackBase;

        startGuardZone = ntl->stackYellowZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES;
    } else {
        /* Cannot determine if the thread library created a red-zone guard page */
        ntl->stackRedZone = ntl->stackBase;
        ntl->stackRedZoneIsProtectedByVM = true;

        /* Yellow guard page is just above red-zone */
        ntl->stackYellowZone = ntl->stackBase + (STACK_RED_ZONE_PAGES * pageSize);

        startGuardZone = ntl->stackRedZone;
        guardZonePages = STACK_YELLOW_ZONE_PAGES + STACK_RED_ZONE_PAGES;
    }

    tla_store(etla, ETLA, etla);
    tla_store(etla, DTLA, dtla);
    tla_store(etla, TTLA, ttla);

    tla_store(dtla, ETLA, etla);
    tla_store(dtla, DTLA, dtla);
    tla_store(dtla, TTLA, ttla);

    tla_store(ttla, ETLA, etla);
    tla_store(ttla, DTLA, dtla);
    tla_store(ttla, TTLA, ttla);

    tla_store(etla, SAFEPOINT_LATCH, etla);
    tla_store(dtla, SAFEPOINT_LATCH, dtla);

    tla_store3(etla, NATIVE_THREAD_LOCALS, ntl);
    tla_store3(etla, ID, id);
    tla_store3(etla, STACK_REFERENCE_MAP, refMap);
    tla_store3(etla, STACK_REFERENCE_MAP_SIZE, refMapSize);

    Address endGuardZone = startGuardZone + (guardZonePages * pageSize);
    Address sp = (Address) &ntl; // approximation of stack pointer
    const int safetyMargin = pageSize;
    if (sp < endGuardZone + safetyMargin) {
        log_exit(11, "Stack is too small to safely place stack guard zones");
    }

    ntl->stackBlueZone = ntl->stackYellowZone;  // default is no blue zone

    // no protection for the primordial thread
    if (guardZonePages != 0) {
#if os_MAXVE
        // custom stack initialization
        maxve_initStack(ntl);
#else
        virtualMemory_protectPages(startGuardZone, guardZonePages);
#endif
    }

#if log_THREADS
    log_println("thread %3d: stackEnd     = %p", id, ntl->stackBase + ntl->stackSize);
    log_println("thread %3d: sp           ~ %p", id, &id);
    log_println("thread %3d: stackBase    = %p", id, ntl->stackBase);
    log_println("thread %3d: stackSize    = %d (%p)", id, ntl->stackSize, ntl->stackSize);
    log_println("thread %3d: redZone      = %p", id, ntl->stackRedZone);
    log_println("thread %3d: yellowZone   = %p", id, ntl->stackYellowZone);
    log_println("thread %3d: blueZone     = %p", id, ntl->stackBlueZone);
    log_println("thread %3d: ttla         = %p", id, ttla);
    log_println("thread %3d: etla         = %p", id, etla);
    log_println("thread %3d: dtla         = %p", id, dtla);
    log_println("thread %3d: anchor       = %p", id, tla_load(Address, etla, LAST_JAVA_FRAME_ANCHOR));
    log_println("thread %3d: ntl          = %p", id, ntl);
    log_println("thread %3d: refMap       = %p", id, refMap);
    log_println("thread %3d: refMapSize   = %d (%p)", id, refMapSize, refMapSize);
#endif

    /* Protect the first page of the TL block (which contains the first word of the triggered thread locals) */
    virtualMemory_protectPages(tlBlock, 1);

    if (!init) {
        threadLocalsBlock_setCurrent(tlBlock);
    }
    return tlBlock;
}

Address threadLocalsBlock_createForExistingThread(jint id) {
    return threadLocalsBlock_create(id, JNI_FALSE, 0);
}

/**
 * Declared in threads.c
 */
extern Mutex globalThreadLock;

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

    Address tla = ETLA_FROM_TLBLOCK(tlBlock);
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);

    int id = tla_load(int, tla, ID);
    if (id >= 0) {
        VmThreadDetachMethod method = image_offset_as_address(VmThreadDetachMethod, vmThreadDetachMethodOffset);
#if log_THREADS
        log_print("threadLocalsBlock_destroy: id=%d, t=%p, calling VmThread.detach(): ", id, nativeThread);
        void image_printAddress(Address address);
        image_printAddress((Address) method);
        log_println("");
#endif
        (*method)(tla);
    } else {
#if log_THREADS
        log_print("threadLocalsBlock_destroy: id=%d, t=%p, never successfully attached: ", id, nativeThread);
#endif
    }

    c_ASSERT(tla_load(Address, tla, FORWARD_LINK) == 0);
    c_ASSERT(tla_load(Address, tla, BACKWARD_LINK) == 0);

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
#if !os_MAXVE
    virtualMemory_unprotectPages(startGuardZone, guardZonePages);
#else
    // on MAXVE stack protection is handled elsewhere
#endif

    // Undo the temporary re-establishment of the thread locals block
    threadLocalsBlock_setCurrent(0);

    /* Release the memory of the TL block. */
    deallocateThreadLocalBlock(tlBlock, ntl->tlBlockSize);

#if log_THREADS
    log_println("threadLocalsBlock_destroy: END t=%p", nativeThread);
#endif
}

void tla_initialize(int tlaSize) {
    theTLASize = tlaSize;
#if !TELE
#if os_DARWIN || os_LINUX
    pthread_key_create(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
#elif os_SOLARIS
    thr_keycreate(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
#elif os_MAXVE
    maxve_thread_initializeSpecificsKey(&theThreadLocalsKey, (ThreadLocalsBlockDestructor) threadLocalsBlock_destroy);
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
#elif os_MAXVE
    tlBlock = (Address) maxve_thread_getSpecific(theThreadLocalsKey);
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
#elif os_MAXVE
    maxve_thread_setSpecific(theThreadLocalsKey, (void *) tlBlock);
#endif
}

TLA tla_current() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock == 0) {
        return 0;
    }
    return ETLA_FROM_TLBLOCK(tlBlock);
}

NativeThreadLocals nativeThreadLocals_current() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock == 0) {
        return 0;
    }
    return NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
}

int tlaSize() {
    c_ASSERT(theTLASize > 0);
    return theTLASize;
}

void tla_println(TLA tla) {
    NativeThreadLocals ntl = tla_load(NativeThreadLocals, tla, NATIVE_THREAD_LOCALS);
    int id = tla_load(int, tla, ID);
#if defined(_LP64)
    log_println("TLA[%d: base=%p, end=%p, size=%lu, tlBlock=%p, tlBlockSize=%lu]",
#else
    // for 32 bit host
    log_println("TLA[%d: base=%llx, end=%llx, size=%llu, tlBlock=%llx, tlBlockSize=%llu]",
#endif
                    id, ntl->stackBase, ntl->stackBase + ntl->stackSize, ntl->stackSize, ntl->tlBlock, ntl->tlBlockSize);
}

void tla_printList(TLA tla) {
    while (tla != 0) {
        tla_println(tla);
        tla = tla_load(TLA, tla, FORWARD_LINK);
    };
}
