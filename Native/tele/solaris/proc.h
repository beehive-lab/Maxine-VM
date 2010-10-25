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
#include "libproc.h"

/**
 * Logs the complete state of a given process, including the state for each thread in the process.
 */
void log_process(struct ps_prochandle *ps);

/**
 * Writes a string to the debug log stream describing each status flag that is set in a given thread or process flags value.
 */
extern void log_flags(const char *prefix, int pr_flags, const char *suffix);

/**
 * Writes a string to the debug log stream describing the why a given lwp is stopped (if it is stopped).
 */
extern void log_printWhyStopped(const char *prefix, const lwpstatus_t *ls, const char *suffix);

/**
 * Convenience macro for initializing a variable to hold the handle to a LWP denoted by a process handle and a LWP identifier.
 */
#define INIT_LWP_HANDLE(lh, ph, lwpId, errorReturnValue) \
    int error; \
    struct ps_lwphandle *lh = Lgrab((struct ps_prochandle *) ph, (lwpid_t) lwpId, &error); \
    if (error != 0) { \
        log_println("Lgrab failed: %s", Lgrab_error(error)); \
        return errorReturnValue; \
    }

