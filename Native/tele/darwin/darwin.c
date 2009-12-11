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
