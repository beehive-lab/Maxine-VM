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

/*
 * Functions for controlling and accessing the memory of a Linux task (i.e. thread or process) via ptrace(2).
 */
#ifndef __linuxTask_h__
#define __linuxTask_h__ 1

#include "isa.h"

/**
 * Copies the general purpose, state and floating-point registers from 'tid'.
 * If any of the parameters to this function are NULL, then the corresponding set of registers are not copied.
 *
 * @param canonicalIntegerRegisters the backing storage into which the integer registers should be copied
 * @param canonicalStateRegisters the backing storage into which the state registers should be copied
 * @param canonicalFloatingPointRegisters the backing storage into which the floating point registers should be copied
 * @return true if the requested copy was successful, false otherwise
 */
boolean task_read_registers(pid_t tid,
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
 *
 * @param tgid the task group id of the traced process
 * @param address the address at which the memory of tgid is to be read
 * @return a file descriptor opened on the memory file and positioned at 'address' or -1 if there was an error.
 *        If there was no error, it is the caller's responsibility to close the file descriptor.
 */
int task_memory_read_fd(int tgid, const void *address);

/**
 * Copies 'size' bytes from 'src' in the address space of 'tgid' to 'dst' in the caller's address space.
 */
size_t task_read(pid_t tgid, pid_t tid, const void *src, void *dst, size_t size);

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
 * Waits for at least one thread in a given process to stop on a SIGTRAP or SIGSTOP at which
 * time, all threads in the process group will be stopped (via SIGSTOP).
 *
 * @param pid the PID of the process whose threads are to stopped once any one of them hits a
 *        breakpoint or receives some other debugger related signal
 * @return the number of stopped threads or -1 if an error occurred
 */
int process_wait_all_threads_stopped(pid_t pid);

/**
 * Converts a directory entry to a numeric PID.
 *
 * @param entry the directory entry to convert
 * @return the numeric PID corresponding to 'entry->d_name' or 0 if the name
 *         is not a valid PID or entry does is not a directory
 */
int dirent_task_pid(const struct dirent *entry);

/**
 * Scans a directory in the /proc filesystem for task subdirectories.
 *
 * @param pid the PID of the process whose /proc/<pid>/task directory will be scanned
 * @param tasks [out] an array of PIDs corresponding to the entries in the scanned directory
 *        for which dirent_task_pid() returns a non-zero result. The memory allocated for this
 *        array needs to be reclaimed by the caller.
 * @return the number of entries returned in 'tasks' or -1 if an error occurs. If an error occurs,
 *        no memory has been allocated and the value of '*tasks' is undefined.
 */
int scan_process_tasks(pid_t pid, pid_t **tasks);

/**
 * Prints the contents of /proc/<tgid>/task/<tid>/stat in a human readable to the log stream.
 *
 * @param tgid a task group id
 * @param tid a task id
 * @param messageFormat the format specification of the message to be printed to the log prior to the stat details
 */
void log_task_stat(pid_t tgid, pid_t tid, const char* messageFormat, ...);

#define TASK_RETRY_PAUSE_MICROSECONDS 200 * 1000

#endif
