/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __ptrace_h__
#define __ptrace_h__ 1

#define PT_TRACE_ME 0   /* child declares it's being traced */
#define PT_READ_I   1   /* read word in child's I space */
#define PT_READ_D   2   /* read word in child's D space */
#define PT_READ_U   3   /* read word in child's user structure */
#define PT_WRITE_I  4   /* write word in child's I space */
#define PT_WRITE_D  5   /* write word in child's D space */
#define PT_WRITE_U  6   /* write word in child's user structure */
#define PT_CONTINUE 7   /* continue the child */
#define PT_KILL     8   /* kill the child process */
#define PT_STEP     9   /* single step the child */
#define PT_ATTACH   10  /* trace some running process */
#define PT_DETACH   11  /* stop tracing a process */
#define PT_SIGEXC   12  /* signals as exceptions for current_proc */
#define PT_THUPDATE 13  /* signal for thread# */
#define PT_ATTACHEXC    14  /* attach to running process with signal exception */

#define PT_FORCEQUOTA   30  /* Enforce quota for root */
#define PT_DENY_ATTACH  31

#define PT_FIRSTMACH    32  /* for machine-specific requests */

extern int _ptrace(const char *file, int line, int _request, pid_t _pid, caddr_t _addr, int _data);
#define ptrace(request, pid, address, data) _ptrace(__FILE__, __LINE__, (request), (pid), (address), (data))

#endif
