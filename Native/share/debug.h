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
 * @author Bernd Mathiske
 */
#ifndef __debug_h__
#define __debug_h__ 1

#include "c.h"
#include "word.h"
#include "jni.h"

#define debug_ASSERT(condition) \
    do { debug_assert((condition), STRINGIZE(condition), __FILE__, __LINE__); } while (0)

extern void debug_assert(Boolean condition, char *conditionString, char *fileName, int lineNumber);

extern void debug_lock(void);

extern void debug_unlock(void);

#define STDOUT 1
#define STDERR 2
#define LOGFILE -1

extern void debug_print_int(int fd, int val);
extern void debug_print_boolean(int fd, char val);
extern void debug_print_char(int fd, int val);
extern void debug_print_long(int fd, jlong val);
extern void debug_print_word(int fd, Address val);
extern void debug_print_buffer(int fd, char *buffer);
extern void debug_print_format(int fd, char *format, ...);
extern void debug_print_newline(int fd);
extern void debug_print_float(int fd, float f);
extern void debug_print_double(int fd, double d);

#if os_WINDOWS
#define NEWLINE_STRING "\r\n"
#else
#define NEWLINE_STRING "\n"
#endif

#define debug_print(format, ...) debug_print_format(LOGFILE, format, ##__VA_ARGS__)
#define debug_println(format, ...) debug_print_format(LOGFILE, format NEWLINE_STRING, ##__VA_ARGS__)
#define debug_exit(code, format, ...) do {\
    debug_print_format(STDERR, format NEWLINE_STRING, ##__VA_ARGS__); \
    exit(code); \
} while(0)

#if word_64_BITS
#define ADDRESS_FORMAT "0x%016lx"
#else
#define ADDRESS_FORMAT "0x%08x"
#endif

#define DEBUG_LOADER 0
#define DEBUG_TRAP 1
#define DEBUG_TRAP_REGISTERS 1
#define DEBUG_MONITOR 1
#define DEBUG_LINKER 0
#define DEBUG_JVMNI 0
#define DEBUG_THREADS 1

#endif /*__debug_h__*/
