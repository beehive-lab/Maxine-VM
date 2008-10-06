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
/*VCSID=8803535e-0bc9-4b0f-8182-c507488c58ea*/

#ifndef __debugPtrace_h__
#define __debugPtrace_h__ 1

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

extern int debug_ptrace(const char *func, int line, int _request, pid_t _pid, caddr_t _addr, int _data);
#define ptrace(request, pid, address, data) debug_ptrace(__func__, __LINE__, request, pid, address, data)

#endif
