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

#include "word.h"
#include "log.h"

static Boolean _logging = false;

static const char* requestToString(int request) {

    if (request == PT_TRACE_ME) {
        return "PT_TRACE_ME";
    }
    if (request == PT_READ_I) {
        return "PT_READ_I";
    }
    if (request == PT_READ_D) {
        return "PT_READ_D";
    }
    if (request == PT_READ_U) {
        return "PT_READ_U";
    }
    if (request == PT_WRITE_I) {
        return "PT_WRITE_I";
    }
    if (request == PT_WRITE_D) {
        return "PT_WRITE_D";
    }
    if (request == PT_WRITE_U) {
        return "PT_WRITE_U";
    }
    if (request == PT_CONTINUE) {
        return "PT_CONTINUE";
    }
    if (request == PT_KILL) {
        return "PT_KILL";
    }
    if (request == PT_STEP) {
        return "PT_STEP";
    }
    if (request == PT_ATTACH) {
        return "PT_ATTACH";
    }
    if (request == PT_DETACH) {
        return "PT_DETACH";
    }
    if (request == PT_SIGEXC) {
        return "PT_SIGEXC";
    }
    if (request == PT_THUPDATE) {
        return "PT_THUPDATE";
    }
    if (request == PT_ATTACHEXC) {
        return "PT_ATTACHEXC";
    }

    if (request == PT_FORCEQUOTA) {
        return "PT_FORCEQUOTA";
    }
    if (request == PT_DENY_ATTACH) {
        return "PT_DENY_ATTACH";
    }

    if (request == PT_FIRSTMACH) {
        return "PT_FIRSTMACH";
    }
    return "<unknown>";
}

int debug_ptrace(const char *func, int line, int request, pid_t pid, caddr_t address, int data) {
    int result;

    if (_logging) {
        log_print("%s:%d ptrace(%s, %d, 0x%lx, %d)", func, line, requestToString(request), pid, address, data);
    }
    result = ptrace(request, pid, address, data);
    if (_logging) {
        log_print(" = %d\n", result);
    }
    return result;
}
