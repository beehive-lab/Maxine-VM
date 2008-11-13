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

#include "relocation.h"

#if (word_LITTLE_ENDIAN)
#define GET_LITTLE_ENDIAN_DATUM(type) return *((type *) p);
#define PUT_LITTLE_ENDIAN_DATUM(type) do { *((type *) p) = value; } while(0)
#define GET_BIG_ENDIAN_DATUM(type) do { \
        int i; \
        type result = 0; \
        Byte *pByte = (Byte *) p; \
        for (i = sizeof(type) - 1; i >= 0; --i) { \
            result |= *pByte++ << (i * 8); \
        } \
        return result; \
    } while(0)
#define PUT_BIG_ENDIAN_DATUM(type) do { \
        int i; \
        Byte *pByte = (Byte *) p; \
        for (i = sizeof(type) - 1; i >= 0; --i) { \
            *pByte++ = ((value >> (i * 8)) & 0xff); \
        } \
    } while(0)
#else
#define GET_BIG_ENDIAN_DATUM(type) return *((type *) p);
#define PUT_BIG_ENDIAN_DATUM(type) do { *((type *) p) = value; } while(0)
#define GET_LITTLE_ENDIAN_DATUM(type) do { \
        int i; \
        type result = 0; \
        Byte *pByte = (Byte *) p; \
        for (i = 0; i < sizeof(type); i++) { \
            result |= *pByte++ << (i * 8); \
        } \
        return result; \
    } while(0)
#define PUT_LITTLE_ENDIAN_DATUM(type) do { \
        int i; \
        Byte *pByte = (Byte *) p; \
        for (i = 0; i < sizeof(type); i++) { \
            *pByte++ = (value & 0xff); \
            value >>= 8; \
        } \
    } while(0)
#endif

static Unsigned8 getLittleEndianUnsigned8(Address p) {
    GET_LITTLE_ENDIAN_DATUM(Unsigned8);
}

static Unsigned8 getBigEndianUnsigned8(Address p) {
    GET_BIG_ENDIAN_DATUM(Unsigned8);
}

static Unsigned4 getLittleEndianUnsigned4(Address p) {
    GET_LITTLE_ENDIAN_DATUM(Unsigned4);
}

static Unsigned4 getBigEndianUnsigned4(Address p) {
    GET_BIG_ENDIAN_DATUM(Unsigned4);
}



static void putLittleEndianUnsigned8(Address p, Unsigned8 value) {
    PUT_LITTLE_ENDIAN_DATUM(Unsigned8);
}

static void putBigEndianUnsigned8(Address p, Unsigned8 value) {
    PUT_BIG_ENDIAN_DATUM(Unsigned8);
}

static void putLittleEndianUnsigned4(Address p, Unsigned4 value) {
    PUT_LITTLE_ENDIAN_DATUM(Unsigned4);
}

static void putBigEndianUnsigned4(Address p, Unsigned4 value) {
    PUT_BIG_ENDIAN_DATUM(Unsigned4);
}

#define RELOCATION_LOOP(wordType, getWord, putWord) do { \
        for (i = 0; i < relocationDataSize; i++) { \
            Byte byte = bytes[i]; \
            if (byte == 0) { \
                dataOffset += 8 * alignmentSize; \
            } else { \
                for (bit = 0; bit < 8; bit++) { \
                    if ((byte & (1 << bit)) != 0) { \
                        Address p = (base + dataOffset); \
                        wordType value = getWord(p); \
                        if (value != (wordType) 0) { \
                            value = value + base; \
                            putWord(p, value); \
                        } \
                    } \
                    dataOffset += alignmentSize; \
                } \
            } \
        } \
    } while (0)

void relocation_apply(void *heap, int relocationScheme, void *relocationData, int relocationDataSize, int alignmentSize, int isBigEndian, int wordSize) {
    int i, bit;
    Address base = (Address) heap;
    Byte *bytes = (Byte *) relocationData;
    int dataOffset = 0;
    c_ASSERT(relocationScheme == relocation_DEFAULT_SCHEME);

    if (wordSize == sizeof(Unsigned4)) {
        if (isBigEndian) {
            RELOCATION_LOOP(Unsigned4, getBigEndianUnsigned4, putBigEndianUnsigned4);
        } else {
            RELOCATION_LOOP(Unsigned4, getLittleEndianUnsigned4, putLittleEndianUnsigned4);
        }
    } else if (wordSize == sizeof(Unsigned8)) {
        if (isBigEndian) {
            RELOCATION_LOOP(Unsigned8, getBigEndianUnsigned8, putBigEndianUnsigned8);
        } else {
            RELOCATION_LOOP(Unsigned8, getLittleEndianUnsigned8, putLittleEndianUnsigned8);
        }
    } else {
        c_ASSERT(false);
    }
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_prototype_BootImage_nativeRelocate(JNIEnv *env, jclass c, jlong heap, jint relocationScheme, jbyteArray relocationData, jint relocationDataSize, jint alignmentSize, jint isBigEndian, jint wordSize) {
    jboolean isCopy;
    jbyte *bytes = (*env)->GetByteArrayElements(env, relocationData, &isCopy);
    relocation_apply((void *) (Address) heap, relocationScheme, bytes, relocationDataSize, alignmentSize, isBigEndian, wordSize);
    (*env)->ReleaseByteArrayElements(env, relocationData, bytes, JNI_ABORT);
}
