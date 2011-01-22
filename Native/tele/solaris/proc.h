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

