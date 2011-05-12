/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
#include "os.h"
#include <stdlib.h>

#include "log.h"
#include "word.h"
#include "jni.h"

#include "dataio.h"

#if (word_LITTLE_ENDIAN)
#define GET_LITTLE_ENDIAN_DATUM(src, type) return *((type *) src);
#define PUT_LITTLE_ENDIAN_DATUM(dst, type) do { *((type *) dst) = value; } while(0)
#define GET_BIG_ENDIAN_DATUM(src, type) do { \
        int i; \
        type result = 0; \
        Byte *srcByte = (Byte *) src; \
        for (i = sizeof(type) - 1; i >= 0; --i) { \
            result |= *srcByte++ << (i * 8); \
        } \
        return result; \
    } while(0)
#define PUT_BIG_ENDIAN_DATUM(dst, type) do { \
        int i; \
        Byte *dstByte = (Byte *) dst; \
        for (i = sizeof(type) - 1; i >= 0; --i) { \
            *dstByte++ = ((value >> (i * 8)) & 0xff); \
        } \
    } while(0)
#else
#define GET_BIG_ENDIAN_DATUM(src, type) return *((type *) src);
#define PUT_BIG_ENDIAN_DATUM(dst, type) do { *((type *) dst) = value; } while(0)
#define GET_LITTLE_ENDIAN_DATUM(src, type) do { \
        int i; \
        type result = 0; \
        Byte *srcByte = (Byte *) src; \
        for (i = 0; i < sizeof(type); i++) { \
            result |= *srcByte++ << (i * 8); \
        } \
        return result; \
    } while(0)
#define PUT_LITTLE_ENDIAN_DATUM(dst, type) do { \
        int i; \
        Byte *dstByte = (Byte *) dst; \
        for (i = 0; i < sizeof(type); i++) { \
            *dstByte++ = (value & 0xff); \
            value >>= 8; \
        } \
    } while(0)
#endif

Unsigned8 readLittleEndianUnsigned8(Address src) {
    GET_LITTLE_ENDIAN_DATUM(src, Unsigned8);
}

Unsigned8 readBigEndianUnsigned8(Address src) {
    GET_BIG_ENDIAN_DATUM(src, Unsigned8);
}

Unsigned4 readLittleEndianUnsigned4(Address src) {
    GET_LITTLE_ENDIAN_DATUM(src, Unsigned4);
}

Unsigned4 readBigEndianUnsigned4(Address src) {
    GET_BIG_ENDIAN_DATUM(src, Unsigned4);
}



void writeLittleEndianUnsigned8(Address dst, Unsigned8 value) {
    PUT_LITTLE_ENDIAN_DATUM(dst, Unsigned8);
}

void writeBigEndianUnsigned8(Address dst, Unsigned8 value) {
    PUT_BIG_ENDIAN_DATUM(dst, Unsigned8);
}

void writeLittleEndianUnsigned4(Address dst, Unsigned4 value) {
    PUT_LITTLE_ENDIAN_DATUM(dst, Unsigned4);
}

void writeBigEndianUnsigned4(Address dst, Unsigned4 value) {
    PUT_BIG_ENDIAN_DATUM(dst, Unsigned4);
}
