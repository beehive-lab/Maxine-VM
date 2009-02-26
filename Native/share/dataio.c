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
        Byte *srcByte = (Byte *) p; \
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
