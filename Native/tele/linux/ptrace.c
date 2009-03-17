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

#include <sys/types.h>
#include <sys/ptrace.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>

#include "word.h"
#include "log.h"

static const char* requestToString(int request) {
#define MATCH(req) do { if (request == req) { return STRINGIZE(req); } } while (0)
    MATCH(PT_TRACE_ME);
    MATCH(PT_READ_I);
    MATCH(PT_READ_D);
    MATCH(PT_READ_U);
    MATCH(PT_WRITE_I);
    MATCH(PT_WRITE_D);
    MATCH(PT_WRITE_U);
    MATCH(PT_READ_I);
    MATCH(PT_READ_I);
    MATCH(PT_READ_I);
    MATCH(PT_CONTINUE);
    MATCH(PT_KILL);
    MATCH(PT_STEP);
    MATCH(PT_ATTACH);
    MATCH(PT_DETACH);
    MATCH(PT_GETREGS);
    MATCH(PT_SETREGS);
    MATCH(PT_GETFPREGS);
    MATCH(PT_SETOPTIONS);
    return NULL;
#undef MATCH
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

long _ptrace(const char *file, const char *func, int line, int request, pid_t pid, void *address, void *data) {
    long result;
    static int lastRequest = 0;

    Boolean trace = log_TELE && (request != PT_READ_D || lastRequest != PT_READ_D);

    const char *requestName;
    char unknownRequestNameBuf[100];

    if (trace) {
        requestName = requestToString(request);
        if (requestName == NULL) {
            sprintf(unknownRequestNameBuf, "<unknown:%d>", request);
            requestName = unknownRequestNameBuf;
        }
        log_print("%s:%d ptrace(%s, %d, %p, %p)", file, line, requestName, pid, address, data);
    }
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
        log_println("%s:%d ptrace(%s, %d, %p, %d) caused an error [%s]", file, line, requestName, pid, address, data, strerror(error));
    }
    lastRequest = request;
    return result;
}
