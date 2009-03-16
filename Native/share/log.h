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

/**
 * Low level VM logging facility.
 *
 * The native methods in VM/src/com/sun/max/vm/debug/Log.java map directly the functions declared in this file.
 *
 * @author Bernd Mathiske
 */
#ifndef __log_h__
#define __log_h__ 1

#include "c.h"
#include "word.h"
#include "jni.h"

extern void log_lock(void);

extern void log_unlock(void);

extern void log_print_int(int val);
extern void log_print_boolean(char val);
extern void log_print_char(int val);
extern void log_print_long(jlong val);
extern void log_print_word(Address val);
extern void log_print_buffer(const char *buffer);
extern void log_print_format(const char *format, ...);
extern void log_print_vformat(const char *format, va_list ap);
extern void log_print_newline(void);
extern void log_print_float(float f);
extern void log_print_double(double d);

#if os_WINDOWS
#define NEWLINE_STRING "\r\n"
#else
#define NEWLINE_STRING "\n"
#endif

#define log_print(format, ...) log_print_format(format, ##__VA_ARGS__)
#define log_println(format, ...) log_print_format(format NEWLINE_STRING, ##__VA_ARGS__)
#define log_exit(code, format, ...) do {\
    log_print_format(format NEWLINE_STRING, ##__VA_ARGS__); \
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

#if log_TELE
#define tele_log_println log_println
#define tele_log_print log_print
#else
#define log_null(format, ...)
#define tele_log_println log_null
#define tele_log_print log_null
#endif

#endif /*__log_h__*/
