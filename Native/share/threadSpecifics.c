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

#include "threadSpecifics.h"

/**
 * Prints the elements in a ThreadSpecificsList to the log stream.
 *
 * @param threadSpecificsList the ThreadSpecificsList whose elements are to be printed
 * @param separator the string to be printed between elements
 */
void threadSpecifics_print(ThreadSpecifics ts) {
    log_print("ThreadSpecifics[%d: stackBase=%p, stackEnd=%p]", ts->id, ts->stackBase, ts->stackBase + ts->stackSize);
}

void threadSpecificsList_printList(ThreadSpecificsList threadSpecificsList, const char *separator) {
    ThreadSpecifics threadSpecifics = threadSpecificsList->head;
    while (threadSpecifics != NULL) {
        threadSpecifics_print(threadSpecifics);
        threadSpecifics = threadSpecifics->next;
        if (threadSpecifics != NULL) {
            log_print("%s", separator);
        }
    }
}

#define TSL_MUTEX_DO(tsl, action) do { \
    int result; \
    if ((result = action(&(threadSpecificsList->lock))) != 0) { \
        log_exit(-1, "Could not %s threadSpecificsList->lock: %s", STRINGIZE(action), strerror(result)); \
    } \
} while (0)

void threadSpecificsList_add(ThreadSpecificsList threadSpecificsList, ThreadSpecifics threadSpecifics) {
    TSL_MUTEX_DO(threadSpecificList, mutex_enter);

    // Ensure that 'threadSpecifics' is not already on the list
    c_ASSERT(threadSpecifics->next == threadSpecifics);

#if log_THREADS
    log_print("Added ");
    threadSpecifics_print(threadSpecifics);
    log_println(" to global list");
#endif

    threadSpecifics->next = threadSpecificsList->head;
    threadSpecificsList->head = threadSpecifics;

    TSL_MUTEX_DO(threadSpecificList, mutex_exit);
}

void threadSpecificsList_remove(ThreadSpecificsList threadSpecificsList, ThreadSpecifics threadSpecifics) {
    TSL_MUTEX_DO(threadSpecificList, mutex_enter);

    // Ensure that 'threadSpecifics' is on the list
    c_ASSERT(threadSpecifics->next != threadSpecifics);

    if (threadSpecificsList->head == threadSpecifics) {
        // At the head of the list:
        threadSpecificsList->head = threadSpecifics->next;
    } else {
        // Not at the head of the list:
        ThreadSpecifics previous = threadSpecificsList->head;
        ThreadSpecifics current = previous->next;
        while (current != threadSpecifics) {
            c_ASSERT(current != NULL);
            previous = current;
            current = current->next;
        }
        previous->next = current->next;
    }

    // Denote that 'threadSpecifics' is no longer on a list
    threadSpecifics->next = threadSpecifics;

#if log_THREADS
    log_print("Removed ");
    threadSpecifics_print(threadSpecifics);
    log_println(" from global list");
#endif

    TSL_MUTEX_DO(threadSpecificList, mutex_exit);
}

#if TELE

/**
 * Searches a ThreadSpecificsList (see threadSpecifics.h) in the VM's address space for the ThreadSpecifics entry whose
 * 'stackBase' and 'stackSize' imply that its stack contains 'stackPointer'. If such an entry is found, then its contents
 * are copied from the VM to the 'threadSpecifics' struct. If no such entry is found, then the fields
 * of 'threadSpecifics' are zeroed except for 'stackBase' which is assigned 'stackPointer'.
 */
void threadSpecificsList_search(PROCESS_MEMORY_PARAMS Address threadSpecificsListAddress, Address stackPointer, ThreadSpecifics threadSpecifics) {
    ThreadSpecificsListStruct threadSpecificsListStruct;

    ThreadSpecificsList threadSpecificsList = &threadSpecificsListStruct;
    READ_PROCESS_MEMORY(threadSpecificsListAddress, threadSpecificsList, sizeof(ThreadSpecificsListStruct));

    Address threadSpecificsAddress = (Address) threadSpecificsList->head;
    while (threadSpecificsAddress != 0) {
        READ_PROCESS_MEMORY(threadSpecificsAddress, threadSpecifics, sizeof(ThreadSpecificsStruct));
        if (threadSpecifics->id == -1) {
            /* primordial thread: adjust stack size based on current stack pointer */
            Size stackSize = (threadSpecifics->triggeredVmThreadLocals - stackPointer);
            threadSpecifics->stackSize = stackSize;
            threadSpecifics->stackBase = stackPointer;
        }
        Address stackBase = threadSpecifics->stackBase;
        Size stackSize = threadSpecifics->stackSize;
        if (stackBase <= stackPointer && stackPointer < (stackBase + stackSize)) {
            return;
        }
        threadSpecificsAddress = (Address) threadSpecifics->next;
    }
    memset((void *) threadSpecifics, 0, sizeof(ThreadSpecificsStruct));

    threadSpecifics->stackBase = stackPointer;
}

#endif
