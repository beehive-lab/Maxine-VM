/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <mach/mach.h>
#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>
#include <errno.h>

#include "darwin.h"
#include "log.h"

boolean forall_threads(task_t task, thread_visitor visitor, void *arg) {
    thread_array_t thread_list = NULL;
    unsigned int nthreads = 0;
    unsigned i;

    kern_return_t kr = task_threads((task_t) task, &thread_list, &nthreads);
    RETURN_ON_MACH_ERROR("task_threads", kr, false);

    for (i = 0; i < nthreads; i++) {
        thread_t thread = thread_list[i];
        if (!(*visitor)(thread, arg)) {
            break;
        }
    }

    // deallocate thread list
    kr = vm_deallocate(mach_task_self(), (vm_address_t) thread_list, (nthreads * sizeof(int)));
    RETURN_ON_MACH_ERROR("vm_deallocate", kr, false);

    return true;
}

void report_mach_error(const char *file, int line, kern_return_t krn, const char* name, const char* argsFormat, ...) {
    log_print("%s:%d %s(", file, line, name);
    va_list ap;
    va_start(ap, argsFormat);
    log_print_vformat(argsFormat, ap);
    va_end(ap);
    log_print(") failed");
    char *machErrorMessage = mach_error_string(krn);
    if (machErrorMessage != NULL && strlen(machErrorMessage) != 0) {
        log_println(" [%s]", machErrorMessage);
    } else {
        log_print_newline();
    }
}

#define wrapped_mach_call0(name, argsFormat, arg1, ...) { \
	static void* lastCall = 0; \
    boolean trace = log_TELE && (name != (void *) mach_vm_read_overwrite || lastCall != (void *) mach_vm_read_overwrite); \
    if (trace) { \
        log_println("%s:%d: %s(" argsFormat ")", file, line, STRINGIZE(name), arg1, ##__VA_ARGS__); \
    } \
    kern_return_t krn = name(arg1, ##__VA_ARGS__ ); \
    int error = errno; \
    if (krn != KERN_SUCCESS)  { \
        report_mach_error(file, line, krn, STRINGIZE(name), argsFormat, arg1, ##__VA_ARGS__); \
    } \
    lastCall = name; \
    errno = error; \
    return krn; \
}
