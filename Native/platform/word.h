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
#ifndef __word_h__
#define __word_h__ 1

#include "os.h"

#undef word_LITTLE_ENDIAN
#undef word_BIG_ENDIAN

#if defined(__x86) || defined(__x86_64) || defined(_X86_) || defined(i386)
#   define word_LITTLE_ENDIAN 1
#   define word_BIG_ENDIAN    0
#   define MATH_HI(x) *(1+(int*)&x)
#   define MATH_LO(x) *((int*)&x)
#else
#   define word_LITTLE_ENDIAN 0
#   define word_BIG_ENDIAN    1
#   define MATH_HI(x) (*(int*)&x)
#   define MATH_LO(x) *(1+(int*)&x)
#endif

#undef word_64_BITS

#if defined(_LP64)
#   define word_64_BITS 1
#endif

#ifdef word_64_BITS
#   define word_32_BITS 0
#else
#   define word_32_BITS 1
#   define word_64_BITS 0
#endif

typedef unsigned char   Byte;

#if word_32_BITS
    typedef Unsigned4 Word;
    typedef Unsigned4   Address;
    typedef Signed4     Offset;
#elif word_64_BITS
    typedef Unsigned8 Word;
    typedef Unsigned8   Address;
    typedef Signed8     Offset;
#else
#   error
#endif

typedef Address         Size;

/*
 * Aligns a given address up to the next word-aligned address if it is not already word-aligned.
 */
#define wordAlign(value) ((((Address) value) + sizeof(Address)-1) & (~(sizeof(Address) - 1)))

#endif /*__word_h__*/
