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

#if defined(w64)
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
