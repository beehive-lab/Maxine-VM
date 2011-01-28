/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @author Bernd Mathiske
 */
#ifndef __c_h__
#define __c_h__ 1

/*
 * For portability issues, we make sure we pull enough standard includes here to
 * have the appropriate definitions of macros used in MaxineNative's definitions
 * for word sizing, endianness etc. Not doing this may cause (at least on Solaris)
 * incorrect definitions being picked up (e.g., _LP64 isn't defined yet by the
 * time word.h is pulled). Doing the include in this file (which all other depends on)
 * ensures that the necessary definitions have been pulled in.
 * Including stdio only does the trick. [Laurent]
 */
#include <stdio.h>

#define _STRINGIZE(x)            #x
#define STRINGIZE(x)             _STRINGIZE(x)

#define _CONCATENATE(a, b)      a##b
#define CONCATENATE(a, b)       _CONCATENATE(a, b)

typedef unsigned char       Unsigned1;
typedef unsigned short      Unsigned2;
typedef unsigned int        Unsigned4;
typedef unsigned long long  Unsigned8;

typedef char                Signed1;
typedef short               Signed2;
typedef int                 Signed4;
typedef long long           Signed8;

typedef float               Float4;
typedef double              Float8;

#if !defined(false) && !defined(true)
    typedef enum {false, true}  boolean;
#else
    typedef int                 boolean;
#endif

#define MAX_PATH_LENGTH     2048

extern void c_initialize(void);

#define c_UNIMPLEMENTED() _c_unimplemented(__func__, __FILE__, __LINE__)

extern int _c_unimplemented(const char* function, const char* file, int line);

#define c_ASSERT(condition) \
    do { _c_assert((condition), STRINGIZE(condition), __FILE__, __LINE__); } while (0)

extern void _c_assert(boolean condition, char *conditionString, char *fileName, int lineNumber);

#define ARRAY_LENGTH(array) (sizeof(array)/sizeof((array)[0]))

#define c_FATAL() c_ASSERT(false)

#endif /*__c_h__*/
