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

#include <stdlib.h>
#include <sys/types.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <syscall.h>

#include "word.h"
#include "log.h"

#undef INTERPOSE_PTRACE
#include "ptrace.h"

static const char* requestToString(int request, char *unknownRequestNameBuf, int unknownRequestNameBufLength) {
    switch (request) {
        case PT_TRACEME: return "TRACEME";;
        case PT_READ_I: return "READ_I";;
        case PT_READ_D: return "READ_D";;
        case PT_READ_U: return "READ_U";;
        case PT_WRITE_I: return "WRITE_I";;
        case PT_WRITE_D: return "WRITE_D";;
        case PT_WRITE_U: return "WRITE_U";;
        case PT_CONTINUE: return "CONTINUE";;
        case PT_KILL: return "KILL";;
        case PT_STEP: return "STEP";;
        case PT_ATTACH: return "ATTACH";;
        case PT_DETACH: return "DETACH";;
        case PT_GETREGS: return "GETREGS";;
        case PT_SETREGS: return "SETREGS";;
        case PT_GETFPREGS: return "GETFPREGS";;
        case PT_SETOPTIONS: return "SETOPTIONS";;
        case PT_GETEVENTMSG: return "GETEVENTMSG";;
        case PT_GETSIGINFO: return "GETSIGINFO";;
        case PT_SETSIGINFO: return "SETSIGINFO";;
    }
    snprintf(unknownRequestNameBuf, unknownRequestNameBufLength, "<unknown:%d>", request);
    return unknownRequestNameBuf;
#undef CASE
}

const char* ptraceEventName(int event) {
    switch (event) {
        case 0: return "NONE";
        case 1: return "PTRACE_EVENT_FORK";
        case 2: return "PTRACE_EVENT_VFORK";
        case 3: return "PTRACE_EVENT_CLONE";
        case 4: return "PTRACE_EVENT_EXEC";
        case 5: return "PTRACE_EVENT_VFORK_DONE";
        case 6: return "PTRACE_EVENT_EXIT";
    }
    return "<unknown>";
}

/*
 * Used to enforce the constraint that all access of the ptraced process from the same task/thread.
 * This value is initialized in linuxTask.c.
 */
static pid_t _ptracerTask = 0;

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

void ptrace_check_tracer(POS_PARAMS, pid_t pid) {
    pid_t tid = syscall(__NR_gettid);
    if (_ptracerTask == 0) {
        _ptracerTask = tid;
    } else if(_ptracerTask != tid) {
        log_exit(11, "%s:%d: Can only ptrace %d from task %d, not task %d", file, line, pid, _ptracerTask, tid);
    }
}

long _ptrace(POS_PARAMS, int request, pid_t pid, void *address, void *data) {
    if (request != PT_TRACEME) {
        ptrace_check_tracer(POS, pid);
    }
    long result;
    static int lastRequest = 0;

    boolean trace = log_TELE && (request != PT_READ_D || lastRequest != PT_READ_D);

    const char *requestName = NULL;
    char unknownRequestNameBuf[100];

    if (trace) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_print("%s:%d ptrace(%s, %d, %p, %p)", file, line, requestName, pid, address, data);
    }
    errno = 0;
    result = ptrace(request, pid, address, data);
    int error = errno;
    if (trace) {
        if (request == PT_READ_D || request == PT_READ_I || request == PT_READ_U) {
            log_println(" = %p", result);
        } else {
            log_print_newline();
        }
    }
    if (error != 0) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_println("%s:%d ptrace(%s, %d, %p, %d) caused an error [%s]", file, line, requestName, pid, address, data, strerror(error));
    }
    lastRequest = request;

    /* Reset errno to its state immediately after the real ptrace call. */
    errno = error;

    return result;
}
