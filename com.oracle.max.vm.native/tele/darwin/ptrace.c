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
        if (request == PT_READ_D || request == PT_READ_I || request == PT_READ_U) {
            log_println(" = %p", result);
        } else {
            log_print_newline();
        }
    }
    if (error != 0) {
        requestName = requestToString(request, unknownRequestNameBuf, sizeof(unknownRequestNameBuf));
        log_println("%s:%d ptrace(%s, %d, %p, %d) caused error %d [%s]", file, line, requestName, pid, address, data, error, strerror(error));
    }
    return result;
}
