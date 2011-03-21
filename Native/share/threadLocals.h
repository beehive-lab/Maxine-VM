/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @author Doug Simon
 */
#ifndef __threadLocals_h__
#define __threadLocals_h__ 1

#include "os.h"
#include "word.h"

/*
 * A thread locals block is a block of memory allocated on a page boundary (e.g. by valloc(3c)).
 * It contains all the VM and native thread local data for a thread.
 * This block of memory is laid out as follows:
 *
 * (low addresses)
 *
 *           page aligned --> +---------------------------------------------+ <-- threadLocalsBlock_current()
 *                            | X X X          unmapped page          X X X |
 *                            | X X X                                 X X X |
 *           page aligned --> +---------------------------------------------+
 *                            |                 tla (triggered)             |
 *                            +---------------------------------------------+ <-- tla_current()
 *                            |                 tla (enabled)               |
 *                            +---------------------------------------------+
 *                            |                 tla (disabled)              |
 *                            +---------------------------------------------+  <-- nativeThreadLocals_current()
 *                            |           NativeThreadLocalsStruct          |
 *                            +---------------------------------------------+
 *                            |                                             |
 *                            |               reference map                 |
 *                            |                                             |
 *                            +---------------------------------------------+
 *
 * (high addresses)
 *
 */

#define ETLA_FROM_TLBLOCK(tlBlock)        ((TLA)       (tlBlock + virtualMemory_getPageSize() - sizeof(Address) +  tlaSize()))
#define NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock) ((NativeThreadLocals) (tlBlock + virtualMemory_getPageSize() - sizeof(Address) + (tlaSize() * 3)))

extern void tla_initialize(int tlaSize);

/**
 * Creates and/or initializes the thread locals block (see diagram above) for the current thread.
 * This includes protecting certain pages of the stack for stack overflow detection.
 * To clean up these resources, the threadLocalsBlock_destroy() function should be
 * called on the value returned by this function.
 *
 * @param id  > 0: the identifier reserved in the thread map for the thread being started
 *            < 0: temporary identifier (derived from the native thread handle) of a thread
 *                 that is being attached to the VM
 * @param tlBlock a previously created thread locals block
 * @param stackSize ignored if tlBlock != 0
 * @return the thread locals block for the current thread.
 *         If init || id <= 0 this value has been registered as the value
 *         associated with the ThreadLocalsKey for this thread. Otherwise,
 *         the space is allocated and will be initialized and registered in a
 *         subsequent call with init = true. The destructor
 *         function specified when registering the value is threadLocalsBlock_destroy().
 */
extern Address threadLocalsBlock_create(jint id, Address tlBlock, Size stackSize);

/**
 * Simplified version of above, when thread already created by native code, i.e. where id <= 0.
 */

extern Address threadLocalsBlock_createForExistingThread(jint id);

/**
 * Releases the resources for the current thread allocated and protected by threadLocalsBlock_create().
 * This is the function specified as the destructor for the value associated with the ThreadLocalsKey
 * for this thread
 *
 * @param tlBlock a value returned by threadLocalsBlock_create()
 */
extern void threadLocalsBlock_destroy(Address tlBlock);

/**
 * The names and indexes of the VM thread locals accessed by native code.
 *
 * These values must be kept in sync with those declared in VmThreadLocals.java.
 * The boot image includes a copy of these values that are checked at image load time.
 *
 * All reads/write to these thread locals should use the 'getThreadLocal()' and 'setThreadLocal()' macros below.
 */
#define FOR_ALL_THREAD_LOCALS(macro) \
    macro(SAFEPOINT_LATCH, 0) \
    macro(ETLA, 1) \
    macro(DTLA, 2) \
    macro(TTLA, 3) \
    macro(NATIVE_THREAD_LOCALS, 4) \
    macro(FORWARD_LINK, 5) \
    macro(BACKWARD_LINK, 6) \
    macro(ID, 9) \
    macro(JNI_ENV, 11) \
    macro(LAST_JAVA_FRAME_ANCHOR, 12) \
    macro(TRAP_NUMBER, 15) \
    macro(TRAP_INSTRUCTION_POINTER, 16) \
    macro(TRAP_FAULT_ADDRESS, 17) \
    macro(TRAP_LATCH_REGISTER, 18) \
    macro(STACK_REFERENCE_MAP, 22) \
    macro(STACK_REFERENCE_MAP_SIZE, 23)

#define DECLARE_THREAD_LOCAL(name, index) name = index,
typedef enum ThreadLocal {
    FOR_ALL_THREAD_LOCALS(DECLARE_THREAD_LOCAL)
} ThreadLocal_t;

/**
 * This typedef is only to clarify intent when using thread locals.
 */
typedef Address TLA;

/**
 * Gets the block of memory allocated for the native and VM thread locals associated with the current thread.
 *
 * This value is accessed via the native thread library's thread locals mechanism (e.g. pthread_getspecific(3c)).
 */
extern Address threadLocalsBlock_current(void);

/**
 * Sets the block of memory allocated for the native and VM thread locals associated with the current thread.
 *
 * This value is accessed via the native thread library's thread locals mechanism (e.g. pthread_setspecific(3c)).
 */
extern void threadLocalsBlock_setCurrent(Address tlBlock);

/**
 * Gets a pointer to the safepoints-enabled copy of thread locals associated with the current thread.
 */
extern TLA tla_current(void);

/**
 * Gets the size of a thread locals area.
 */
extern int tlaSize(void);

/**
 * Sets the value of a specified thread local.
 *
 * @param tla a TLA value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define tla_store(tla, name, value) do { *((Address *) tla + name) = (Address) (value); } while (0)

/**
 * Gets the value of a specified thread local.
 *
 * @param type the type to which the retrieved thread local value is cast
 * @param tla a TLA value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @return the value of the named thread local, cast to 'type'
 */
#define tla_load(type, tla, name) ((type) *((Address *) tla + name))

/**
 * Gets the address of a specified thread local.
 *
 * @param tla a TLA value
 * @param name the name of the thread local to address
 * @return the address of the named thread local, cast to Address
 */
#define tla_addressOf(tla, name) ((Address) tla + (name * sizeof(Address)))

/**
 * Sets the value of a specified thread local to all three thread local spaces.
 *
 * @param tla a TLA value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define tla_store3(tla, name, value) do { \
    *((Address *) tla_load(TLA, tla, ETLA) + name) = (Address) (value); \
    *((Address *) tla_load(TLA, tla, DTLA) + name) = (Address) (value); \
    *((Address *) tla_load(TLA, tla, TTLA) + name) = (Address) (value); \
} while (0)

typedef struct {
    Address stackBase;
    Size stackSize;
    Address handle;    // e.g. pthread_self()
    Address tlBlock;
    Address tlBlockSize;
    Address stackYellowZone; // unmapped to cause a trap on access
    Address stackRedZone;    // unmapped always - fatal exit if accessed
    Address stackRedZoneIsProtectedByVM; // non-zero if the VM explicitly mprotected the red zone

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
 * Gets a pointer to NativeThreadLocalsStruct associated with the current thread.
 */
extern NativeThreadLocals nativeThreadLocals_current(void);

/**
 * Prints a selection of the fields in a given TLA object.
 *
 * @param tla the TLA to be printed
 */
extern void tla_println(TLA tla);

/**
 * Prints the elements in a list of thread locals.
 *
 * @param tla the head of a list of thread locals
 */
extern void tla_printList(TLA tla);

#endif /*__threadLocals_h__*/
