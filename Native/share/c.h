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
typedef enum {false, true}  Boolean;
#else
typedef int                 Boolean;
#endif

#define MAX_PATH_LENGTH     2048

extern void c_initialize(void);

#define c_unimplemented() c_unimplemented_impl(__func__, __FILE__, __LINE__)
extern int c_unimplemented_impl(const char* function, const char* file, int line);

#define c_ASSERT(condition) \
    do { c_assert((condition), STRINGIZE(condition), __FILE__, __LINE__); } while (0)

extern void c_assert(Boolean condition, char *conditionString, char *fileName, int lineNumber);

#endif /*__c_h__*/
