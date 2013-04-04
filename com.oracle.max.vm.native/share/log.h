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

/**
 * Low level VM logging facility.
 *
 * The native methods in VM/src/com/sun/max/vm/debug/Log.java map directly the functions declared in this file.
 */
#ifndef __log_h__
#define __log_h__ 1

#include "c.h"
#include "word.h"
#include "jni.h"
/**
 * Initializes the low-level VM logging facility.
 */
extern void log_initialize(const char *path);

extern void log_lock(void);
extern void log_unlock(void);

extern void log_print_int(int val);
extern void log_print_boolean(char val);
extern void log_print_char(jchar val);
extern void log_print_long(jlong val);
extern void log_print_word(Address val);
extern void log_print_bytes(const jbyte *value, int offset, int len);
extern void log_print_chars(const jchar *value, int offset, int len);
extern void log_print_format(const char *format, ...);
extern void log_print_vformat(const char *format, va_list ap);
extern void log_print_newline(void);
extern void log_print_symbol(Address address);
extern void log_print_float(float f);
extern void log_print_double(double d);
extern void log_flush(void);

#if os_WINDOWS
#define NEWLINE_STRING "\r\n"
#else
#define NEWLINE_STRING "\n"
#endif

#define log_print(...) log_print_format(__VA_ARGS__)
#define log_println(...) do {\
    log_print_format(__VA_ARGS__); \
    log_print_format(NEWLINE_STRING); \
} while(0)
#define log_exit(code, ...) do {\
    log_print_format(__VA_ARGS__); \
    log_print_format(NEWLINE_STRING); \
    exit(code); \
} while(0)

#define log_ALL 0

#define log_LOADER (log_ALL || 0)
#define log_TRAP (log_ALL || 0)
#define log_MONITORS (log_ALL || 0)
#define log_LINKER (log_ALL || 0)
#define log_JVMNI (log_ALL || 0)
#define log_THREADS (log_ALL || 0)
#define log_TELE (log_ALL || 0)

#if log_JVMNI
#define jvmni_log_println log_println
#define jvmni_log_print log_print
#else
#define jvmni_log_null(format, ...)
#define jvmni_log_println jvmni_log_null
#define jvmni_log_print jvmni_log_null

#endif

#if log_TELE
#define tele_log_println log_println
#define tele_log_print log_print
#else
#define tele_log_null(format, ...)
#define tele_log_println tele_log_null
#define tele_log_print tele_log_null
#endif

#endif /*__log_h__*/
