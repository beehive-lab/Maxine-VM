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
#include <sys/ptrace.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <syscall.h>

#include "word.h"
#include "log.h"

static const char* requestToString(int request, char *unknownRequestNameBuf, int unknownRequestNameBufLength) {
#define CASE(req) case req: return STRINGIZE(req)
    switch (request) {
        CASE(PT_TRACE_ME);
        CASE(PT_READ_I);
        CASE(PT_READ_D);
        CASE(PT_READ_U);
        CASE(PT_WRITE_I);
        CASE(PT_WRITE_D);
        CASE(PT_WRITE_U);
        CASE(PT_CONTINUE);
        CASE(PT_KILL);
        CASE(PT_STEP);
        CASE(PT_ATTACH);
        CASE(PT_DETACH);
        CASE(PT_GETREGS);
        CASE(PT_SETREGS);
        CASE(PT_GETFPREGS);
        CASE(PT_SETOPTIONS);
        CASE(PT_GETEVENTMSG);
        CASE(PT_GETSIGINFO);
        CASE(PT_SETSIGINFO);
    }
    snprintf(unknownRequestNameBuf, unknownRequestNameBufLength, "<unknown:%d>", request);
    return unknownRequestNameBuf;
#undef CASE
}

const char* ptraceEventName(int event) {
#define CASE(evt) case evt: return STRINGIZE(evt)
    switch (event) {
        case 0: return "NONE";
        CASE(PTRACE_EVENT_FORK);
        CASE(PTRACE_EVENT_VFORK);
        CASE(PTRACE_EVENT_CLONE);
        CASE(PTRACE_EVENT_EXEC);
        CASE(PTRACE_EVENT_VFORK_DONE);
        CASE(PTRACE_EVENT_EXIT);
    }
    return "<unknown>";
#undef CASE
}

#if 0
/* Linux ptrace() is unreliable, but apparently retrying after descheduling helps. */
long ptrace_withRetries(int request, int processID, void *address, void *data) {
    int microSeconds = 100000;
    int i = 0;
    while (true) {
        long result = ptrace(request, processID, address, data);
        int error = errno;
        if (error == 0) {
            return result;
        }
        if (error != ESRCH || i >= 150) {
            return -1;
        }
        usleep(microSeconds);
        i++;
        if (i % 10 == 0) {
            log_println("ptrace retrying");
        }
    }
}
#else
#define ptrace_withRetries ptrace
#endif

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
    if (request != PT_TRACE_ME) {
        ptrace_check_tracer(POS, pid);
    }
    long result;
    static int lastRequest = 0;

    Boolean trace = log_TELE && (request != PT_READ_D || lastRequest != PT_READ_D);

    const char *requestName = NULL;
    char unknownRequestNameBuf[100];

    if (trace) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_print("%s:%d ptrace(%s, %d, %p, %p)", file, line, requestName, pid, address, data);
    }
    errno = 0;
    result = ptrace_withRetries(request, pid, address, data);
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
