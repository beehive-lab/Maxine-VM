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
#ifndef __threadSpecifics_h__
#define __threadSpecifics_h__ 1

#include "os.h"
#include "word.h"
#include "mutex.h"

typedef struct thread_SpecificsStruct {
    struct thread_SpecificsStruct *next; // Points to self if not on a list
    jint id; // -1 denotes the thread specifics for the primordial thread created when a debugger is attached
    Address stackBase;
    Size stackSize;
    Address triggeredVmThreadLocals;
    Address enabledVmThreadLocals;
    Address disabledVmThreadLocals;
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
} ThreadSpecificsStruct, *ThreadSpecifics;

typedef struct {
    mutex_Struct lock;
    ThreadSpecifics head;
} ThreadSpecificsListStruct, *ThreadSpecificsList;

/**
 * Prints a selection of the fields in a given ThreadSpecifics object.
 *
 * @param threadSpecifics the ThreadSpecifics to be printed
 */
extern void threadSpecifics_print(ThreadSpecifics threadSpecifics);

/**
 * Adds a given ThreadSpecifics object to a given ThreadSpecificsList.
 * This operation synchronizes on the lock of 'threadSpecificsList'.
 *
 * @param threadSpecificsList the list to which 'threadSpecifics' is to be added
 * @param threadSpecifics the element to be added. This element must not be a member of any other list
 *        (i.e. its 'next' field must point to itself).
 */
extern void threadSpecificsList_add(ThreadSpecificsList threadSpecificsList, ThreadSpecifics threadSpecifics);

/**
 * Removes a given ThreadSpecifics object from a given ThreadSpecificsList.
 * This operation synchronizes on the lock of 'threadSpecificsList'.
 *
 * @param threadSpecificsList the list from which 'threadSpecifics' is to be removed
 * @param threadSpecifics the element to be removed. This element must be a member of 'threadSpecificsList'.
 */
extern void threadSpecificsList_remove(ThreadSpecificsList threadSpecificsList, ThreadSpecifics threadSpecifics);

/**
 * Prints the elements in a ThreadSpecificsList to the log stream.
 *
 * @param threadSpecificsList the ThreadSpecificsList whose elements are to be printed
 * @param separator the string to be printed between elements
 */
extern void threadSpecificsList_printList(ThreadSpecificsList threadSpecificsList, const char *separator);

#ifdef TELE

#if os_LINUX
task_read(pid_t tgid, pid_t tid, void *src, void *dst, size_t size);
#define PROCESS_MEMORY_PARAMS pid_t tgid, pid_t tid,
#define READ_PROCESS_MEMORY(src, dst, size) task_read(tgid, tid, (void *) src, (void *) dst, (size_t) size)
#elif os_DARWIN
#include <mach/mach.h>
int task_read(task_t task, vm_address_t src, void *dst, size_t size);
#define PROCESS_MEMORY_PARAMS task_t task,
#define READ_PROCESS_MEMORY(src, dst, size) task_read(task, (vm_address_t) src, (void *) dst, (size_t) size)
#elif os_SOLARIS
ssize_t _proc_Pread(const char *func, int line, struct ps_prochandle *ph, void *dst, size_t size, uintptr_t src);
#define PROCESS_MEMORY_PARAMS struct ps_prochandle *ph,
#define READ_PROCESS_MEMORY(src, dst, size) _proc_Pread(__FUNC__, __LINE, ph, (void *) dst, (size_t) size, (uintptr_t) src)
#elif os_GUESTVMXEN
uint16_t readbytes(unsigned long address, char *buffer, uint16_t n);
#define PROCESS_MEMORY_PARAMS
#define READ_PROCESS_MEMORY(src, dst, size) readbytes((unsigned long) src, (char *) dst, (uint16_t) size)
#else
#error
#endif

/**
 * Searches a ThreadSpecificsList (see threadSpecifics.h) in the VM process address space for the ThreadSpecifics entry whose
 * 'stackBase' and 'stackSize' imply that its stack contains 'stackPointer'. If such an entry is found, then its contents
 * are copied from the VM to the 'threadSpecifics' struct. If no such entry is found, then the fields
 * of 'threadSpecifics' are zeroed except for 'stackBase' which is assigned the value of 'stackPointer'.
 */
extern void threadSpecificsList_search(PROCESS_MEMORY_PARAMS Address threadSpecificsListAddress, Address stackPointer, ThreadSpecifics threadSpecifics);

#endif

#endif /*__threadSpecifics_h__*/
