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

#ifndef __ptrace_h__
#define __ptrace_h__ 1

#define PT_TRACEME 0   /* child declares it's being traced */
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

extern long _ptrace(POS_PARAMS, int request, pid_t pid, void *address, void *data);
#define ptrace(request, pid, address, data) _ptrace(POS, (request), (pid), (void *) (Address) (address), (void *) (Address) (data))

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
