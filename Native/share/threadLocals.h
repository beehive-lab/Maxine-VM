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
 * @author Doug Simon
 */
#ifndef __threadLocals_h__
#define __threadLocals_h__ 1

#include "os.h"
#include "word.h"

extern void threadLocals_initialize(int threadLocalsSize, int javaFrameAnchorSize);

/**
 * The indexes of the VM thread locals accessed by native code.
 *
 * These values must be kept in sync with those declared in VmThreadLocals.java.
 * The boot image includes a copy of these values so that they can be checked at image load time.
 *
 * All reads/write to these thread locals should use the 'getThreadLocal()' and 'setThreadLocal()' macros below.
 */
typedef enum ThreadLocal {
    SAFEPOINT_LATCH = 0,
    SAFEPOINTS_ENABLED_THREAD_LOCALS = 1,
    SAFEPOINTS_DISABLED_THREAD_LOCALS = 2,
    SAFEPOINTS_TRIGGERED_THREAD_LOCALS = 3,
    NATIVE_THREAD_LOCALS = 4,
    FORWARD_LINK = 5,
    BACKWARD_LINK = 6,
    ID = 9,
    JNI_ENV = 11,
    LAST_JAVA_FRAME_ANCHOR = 12,
    TRAP_NUMBER = 15,
    TRAP_INSTRUCTION_POINTER = 16,
    TRAP_FAULT_ADDRESS = 17,
    TRAP_LATCH_REGISTER = 18
} ThreadLocal_t;

typedef Address ThreadLocals;

/**
 * Gets the size of the storage required for a set of thread locals.
 */
extern int threadLocalsSize();

/**
 * Gets the size of a Java frame anchor.
 */
extern int javaFrameAnchorSize();

/**
 * Sets the value of a specified thread local.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define setThreadLocal(tl, name, value) do { *((Address *) tl + name) = (Address) (value); } while (0)

/**
 * Gets the value of a specified thread local.
 *
 * @param type the type to which the retrieved thread local value is cast
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @return the value of the named thread local, cast to 'type'
 */
#define getThreadLocal(type, tl, name) ((type) *((Address *) tl + name))

/**
 * Gets the address of a specified thread local.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to address
 * @return the address of the named thread local, cast to Address
 */
#define getThreadLocalAddress(tl, name) ((Address) tl + (name * sizeof(Address)))

/**
 * Sets the value of a specified thread local to all three thread local spaces.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define setConstantThreadLocal(tl, name, value) do { \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_ENABLED_THREAD_LOCALS) + name) = (Address) (value); \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS) + name) = (Address) (value); \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS) + name) = (Address) (value); \
} while (0)

typedef struct {
    jint id; //  0: denotes the primordial thread
             // >0: denotes a VmThread
    Address stackBase;
    Size stackSize;
    Address refMapArea;
    Address stackYellowZone; // unmapped to cause a trap on access
    Address stackRedZone;    // unmapped always - fatal exit if accessed

    /*
     * The blue zone is a page that is much closer to the base of the stack and is optionally protected.
     * This can be used, e.g., to determine the actual stack size needed by a thread, or to avoid
     * reserving actual real memory until it is needed.
     */
    Address stackBlueZone;

    /*
     * Place to hang miscellaneous OS dependent record keeping data.
     */
    void *osData;  //
} NativeThreadLocalsStruct, *NativeThreadLocals;

/**
 * Prints a selection of the fields in a given ThreadLocals object.
 *
 * @param tl the ThreadLocals to be printed
 */
extern void threadLocals_println(ThreadLocals);

/**
 * Prints the elements in a list of thread locals.
 *
 * @param tl the head of a list of thread locals
 */
extern void threadLocals_printList(ThreadLocals tl);

#endif /*__threadLocals_h__*/
