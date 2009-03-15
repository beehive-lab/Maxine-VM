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

#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/ptrace.h>

#include "word.h"
#include "log.h"

static const char* requestToString(int request, char *unknownRequestNameBuf, int unknownRequestNameBufLength) {
#define MATCH(req) if (request == req) return #req
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
    snprintf(unknownRequestNameBuf, unknownRequestNameBufLength, "<unknown:%d>", request);
    return unknownRequestNameBuf;
}

int _ptrace(const char *file, int line, int request, pid_t pid, caddr_t address, int data) {
    int result;

    const char *requestName;
    char unknownRequestNameBuf[100];

    if (log_TELE) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_print("%s:%d ptrace(%s, %d, %p, %d)", file, line, requestName, pid, address, data);
    }

    errno = 0;
    result = ptrace(request, pid, address, data);
    int error = errno;

    if (log_TELE) {
        log_println(" = %p", result);
    }
    if (error != 0) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_println("%s:%d ptrace(%s, %d, %p, %d) caused error %d [%s]", file, line, requestName, pid, address, data, error, strerror(error));
    }
    return result;
}
