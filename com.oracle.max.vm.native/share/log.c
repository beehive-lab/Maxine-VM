/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>

#include "log.h"
#include "jni.h"
#include "mutex.h"
#include "threads.h"

#if !os_MAXVE
static FILE *fileStream = NULL;
#endif

void log_assert(boolean condition, char *conditionString, char *fileName, int lineNumber) {
    if (!condition) {
        log_println("log_assert %s[%d]: %s", fileName, lineNumber, conditionString);
        exit(1);
    }
}

static mutex_Struct log_mutexStruct;

void log_initialize(const char *path) {
    mutex_initialize(&log_mutexStruct);
#if !os_MAXVE
    if (path == NULL) {
        path = "stdout";
    }
    if (strncmp(path, "stdout\0", 7) == 0) {
        fileStream = stdout;
        /* Set the file stream to flush whenever a newline character is encountered */
        setlinebuf(fileStream);
    } else if (strncmp(path, "stderr\0", 7) == 0) {
        fileStream = stderr;
    } else {
        fileStream = fopen(path, "w");
        if (fileStream == NULL) {
            fprintf(stderr, "Could not open file for VM output stream: %s\n", path);
            exit(1);
        }
        /* Set the file stream to flush whenever a newline character is encountered */
        setlinebuf(fileStream);
    }
#endif
}

void log_lock(void) {
	int result;
	if ((result = mutex_enter_nolog(&log_mutexStruct)) != 0) {
	    log_exit(-1, "Thread %p could not lock mutex %p: %s", thread_self(), &log_mutexStruct, strerror(result));
	}
}

void log_unlock(void) {
    int result;
	if ((result = mutex_exit_nolog(&log_mutexStruct)) != 0) {
        log_exit(-1, "Thread %p could not unlock mutex %p: %s", thread_self(), &log_mutexStruct, strerror(result));
	}
}

void log_print_format(const char *format, ...) {
    va_list ap;
    va_start(ap, format);
#if !os_MAXVE
    FILE* out = fileStream == NULL ? stdout : fileStream;
    vfprintf(out, format, ap);
#else
    vprintf(format, ap);
#endif
    va_end(ap);
}

void log_flush() {
#if !os_MAXVE
    FILE* out = fileStream == NULL ? stdout : fileStream;
    fflush(out);
#endif
}

void log_print_vformat(const char *format, va_list ap) {
#if !os_MAXVE
    FILE* out = fileStream == NULL ? stdout : fileStream;
    vfprintf(out, format, ap);
#else
    vprintf(format, ap);
#endif
}

void log_print_int(int val) {
    log_print_format("%d", val);
}

void log_print_boolean(char val) {
	if (val == 0) {
	    log_print_format("false");
    } else {
        log_print_format("true");
    }
}

void log_print_char(jchar val) {
	log_print_format("%lc", val);
}

void log_print_long(jlong val) {
	log_print_format("%ld", val);
}

void log_print_bytes(const jbyte *value, int offset, int len) {
    if (value == NULL) {
        log_print_format("null");
    } else {
        if (len < 0) {
            c_ASSERT(offset == 0);
            log_print_format("%s", value);
        } else {
            int i;
            for (i = 0; i < len; i++) {
                log_print_format("%c", *(value + offset + i));
            }
        }
    }
}

void log_print_chars(const jchar *value, int offset, int len) {
    if (value == NULL) {
        log_print_format("null");
    } else {
        int i;
        for (i = 0; i < len; i++) {
            log_print_format("%lc", *(value + offset + i));
        }
    }
}

void log_print_word(Address address) {
    if (address == 0) {
        log_print_format("0");
    } else {
#if os_SOLARIS
        /* On Solaris, the %p format specifier does not include the "0x" prefix so
         * it is added to make the output of log_print_word consistent across all
         * platforms. */
        log_print_format("0x%p", address);
#else
        log_print_format("%p", address);
#endif
    }
}

void log_print_newline() {
    log_print_format(NEWLINE_STRING);
}

void log_print_symbol(Address address) {
#if !os_MAXVE
    Dl_info info;
    if (dladdr((void *) address, &info) != 0) {
        if (info.dli_sname == NULL) {
            log_print("%s (%p+%d)", info.dli_fname, info.dli_fbase, address - (Address) info.dli_fbase);
            return;
        } else {
            log_print("%s (%p) at %s (%p%+d)",
                            info.dli_fname,
                            info.dli_fbase,
                            info.dli_sname,
                            info.dli_saddr,
                            address - (Address) info.dli_saddr);
            return;
        }
    }
#endif
    log_print_word(address);
}

void log_print_float(float f) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
#if os_MAXVE
	log_print_format("%%f not supported");
#else
	log_print_format("%f", f);
#endif
}

void log_print_double(double d) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	log_print_format("%lf", d);
}
