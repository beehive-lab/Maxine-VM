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
 * This header is a collection of the definitions and macros normal found in <sys/ptrace.h>.
 * The reason that <sys/ptrace.h> cannot be directly used is that its content is inconsistent
 * across various versions of glibc found in Linux distros. As such, all
 * ptrace functionality that the tele code depends on is included here.
 *
 * This header also includes a mechanism for interposing on ptrace for the
 * purpose of tracing and/or error checking calls to ptrace. To use this mechanism,
 * define the macro INTERPOSE_PTRACE and supply a definition of the _ptrace() function
 * declared in this header.
 */

#ifndef __ptrace_h__
#define __ptrace_h__ 1

#define PT_TRACEME  0   /* child declares it's being traced */
#define PT_READ_I   1   /* read word in child's I space */
#define PT_READ_D   2   /* read word in child's D space */
#define PT_READ_U   3   /* read word in child's user structure */
#define PT_WRITE_I  4   /* write word in child's I space */
#define PT_WRITE_D  5   /* write word in child's D space */
#define PT_WRITE_U  6   /* write word in child's user structure */
#define PT_CONTINUE 7   /* continue the child */
#define PT_KILL     8   /* kill the child process */
#define PT_STEP     9   /* single step the child */
#define PT_GETREGS  12  /* read integer registers */
#define PT_SETREGS  13  /* set integer registers */
#define PT_GETFPREGS 14 /* read floating point registers */
#define PT_ATTACH   16  /* trace some running process */
#define PT_DETACH   17  /* stop tracing a process */

#define PT_SETOPTIONS  0x4200
#define PT_GETEVENTMSG 0x4201
#define PT_GETSIGINFO  0x4202
#define PT_SETSIGINFO  0x4203

#define PTRACE_O_TRACESYSGOOD   0x00000001
#define PTRACE_O_TRACEFORK      0x00000002
#define PTRACE_O_TRACEVFORK     0x00000004
#define PTRACE_O_TRACECLONE     0x00000008
#define PTRACE_O_TRACEEXEC      0x00000010
#define PTRACE_O_TRACEVFORKDONE 0x00000020
#define PTRACE_O_TRACEEXIT      0x00000040
#define PTRACE_O_MASK           0x0000007f

#define PTRACE_EVENT_FORK       1
#define PTRACE_EVENT_VFORK      2
#define PTRACE_EVENT_CLONE      3
#define PTRACE_EVENT_EXEC       4
#define PTRACE_EVENT_VFORK_DONE 5
#define PTRACE_EVENT_EXIT       6

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

#ifdef INTERPOSE_PTRACE
/**
 * The signature of a function that wraps a call to ptrace().
 */
extern long _ptrace(POS_PARAMS, int request, pid_t pid, void *address, void *data);

#define ptrace(request, pid, address, data) _ptrace(POS, (request), (pid), (void *) (address), (void *) (data))
#else
extern long int ptrace (int request, pid_t pid, void *address, void *data);
#endif

/**
 * Extracts the ptrace event code from the status value returned by a call to waitpid.
 */
#define PTRACE_EVENT(waitpidStatus) (((waitpidStatus) & 0xFF0000) >> 16)

/**
 * Gets the name of a given ptrace event.
 *
 * @param event a ptrace event code
 * @return "<unknown>" if 'event' is not a valid __ptrace_eventcodes value
 */
extern const char* ptraceEventName(int event);

/**
 * Checks that the current task/thread is the one designated as the parent of the ptraced process 'pid'.
 * The ptraced process can only be accessed from this parent.
 */
void ptrace_check_tracer(POS_PARAMS, pid_t pid);

#endif
