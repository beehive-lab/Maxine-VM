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

extern void debug_print_int(int val);
extern void debug_print_boolean(char val);
extern void debug_print_char(int val);
extern void debug_print_long(jlong val);
extern void debug_print_word(Address val);
extern void debug_print_buffer(char *buffer);
extern void debug_print_format(char *format, ...);
extern void debug_print_newline(void);
extern void debug_print_float(float f);
extern void debug_print_double(double d);

#if os_WINDOWS
#define NEWLINE_STRING "\r\n"
#else
#define NEWLINE_STRING "\n"
#endif

#define debug_print(format, ...) debug_print_format(format, ##__VA_ARGS__)
#define debug_println(format, ...) debug_print_format(format NEWLINE_STRING, ##__VA_ARGS__)
#define debug_exit(code, format, ...) do {\
    debug_print_format(format NEWLINE_STRING, ##__VA_ARGS__); \
    exit(code); \
} while(0)

#if word_64_BITS
#define ADDRESS_FORMAT "0x%016lx"
#else
#define ADDRESS_FORMAT "0x%08x"
#endif

#define debug_LOADER 0
#define debug_TRAP 0
#define debug_MONITOR 1
#define debug_LINKER 0
#define debug_JVMNI 0
#define debug_THREADS 0
#define debug_INSPECTOR_NATIVE 0

#endif /*__debug_h__*/
