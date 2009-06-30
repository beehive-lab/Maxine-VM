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
#include "relocation.h"

#define RELOCATION_LOOP(wordType, getWord, putWord) do { \
        for (i = 0; i < relocationDataSize; i++) { \
            Byte byte = bytes[i]; \
            if (byte == 0) { \
                dataOffset += 8 * wordSize; \
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
                    dataOffset += wordSize; \
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
            RELOCATION_LOOP(Unsigned4, readBigEndianUnsigned4, writeBigEndianUnsigned4);
        } else {
            RELOCATION_LOOP(Unsigned4, readLittleEndianUnsigned4, writeLittleEndianUnsigned4);
        }
    } else if (wordSize == sizeof(Unsigned8)) {
        if (isBigEndian) {
            RELOCATION_LOOP(Unsigned8, readBigEndianUnsigned8, writeBigEndianUnsigned8);
        } else {
            RELOCATION_LOOP(Unsigned8, readLittleEndianUnsigned8, writeLittleEndianUnsigned8);
        }
    } else {
        c_ASSERT(false);
    }
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_prototype_BootImage_nativeRelocate(JNIEnv *env, jclass c, jlong heap, jint relocationScheme,
                                                       jbyteArray relocationData, jint relocationDataSize, jint alignmentSize,
                                                       jint isBigEndian, jint wordSize) {
    jboolean isCopy;
    jbyte *bytes = (*env)->GetByteArrayElements(env, relocationData, &isCopy);
    relocation_apply((void *) (Address) heap, relocationScheme, bytes, relocationDataSize, alignmentSize, isBigEndian, wordSize);
    (*env)->ReleaseByteArrayElements(env, relocationData, bytes, JNI_ABORT);
}
