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
 * Functions for controlling and accessing the memory of a Linux task (i.e. thread or process) via ptrace(2).
 *
 * @author Doug Simon
 */
#ifndef __linuxTask_h__
#define __linuxTask_h__ 1

#include "isa.h"

int task_read_registers(pid_t tid,
    isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
    isa_CanonicalStateRegistersStruct *canonicalStateRegisters,
    isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters);

/**
 * Reads the stat of a task from /proc/<tgid>/task/<tid>/stat. See proc(5).
 *
 * @param format the format string for parsing the fields of the stat string
 * @param ... the arguments for storing the fields parsed from the stat string according to 'format'
 */
void task_stat(pid_t tgid, pid_t tid, const char* format, ...);

/**
 * Gets the state of a given task.
 *
 * @param tgid the task group id of the task
 * @param tid the id of the task
 * @return one of the following characters denoting the state of task 'tid':
 *     R: running
 *     S: sleeping in an interruptible wait
 *     D: waiting in uninterruptible disk sleep
 *     Z: zombie
 *     T: traced or stopped (on a signal)
 *     W: is paging
 */
char task_state(pid_t tgid, pid_t tid);

/* Used to enforce the constraint that all access of the ptraced process from the same process. */
extern pid_t _ptracerTask;

/* Required to make lseek64 and off64_t available. */
#define _LARGEFILE64_SOURCE 1

/**
 * Gets an open file descriptor on /proc/<pid>/mem for reading the memory of the traced process 'tgid'.
 * Unfortunately, this mechanism cannot be used for writing; ptrace must be used instead.
 *
 * TODO: Ensure that the file descriptor is closed once tracing is complete.
 */
int task_memory_read_fd(int tgid, void *address);

/**
 * Copies 'size' bytes from 'src' in the address space of 'tgid' to 'dst' in the caller's address space.
 */
size_t task_read(pid_t tgid, pid_t tid, void *src, void *dst, size_t size);

/**
 * Copies 'size' bytes from 'src' in the caller's address space to 'dst' in the address space of 'tgid'.
 * The value of 'size' must be >= 0 and < sizeof(Word).
 */
int task_write_subword(jint tgid, jint tid, void *dst, const void *src, size_t size);

/**
 * Copies 'size' bytes from 'src' in the caller's address space to 'dst' in the address space of 'tgid'.
 */
size_t task_write(pid_t tgid, pid_t tid, void *dst, const void *src, size_t size);

/**
 * Prints the contents of /proc/<tgid>/task/<tid>/stat in a human readable to the log stream.
 *
 * @param tgid a task group id
 * @param tid a task id
 * @param messageFormat the format specification of the message to be printed to the log prior to the stat details
 */
void log_task_stat(pid_t tgid, pid_t tid, const char* messageFormat, ...);

#define TASK_RETRY_PAUSE_MICROSECONDS 2000000

#include <unistd.h>
#define task_wait_for_state(tgid, tid, states) do { \
    char state; \
    while (strchr(states, state = task_state(tgid, tid)) == NULL) { \
        tele_log_println("%s:%d: Task %d waiting for one of \"%s\" states, current state is %c", __FILE__, __LINE__, tid, states, state); \
        usleep(TASK_RETRY_PAUSE_MICROSECONDS); \
    } \
} while(0)

#endif
