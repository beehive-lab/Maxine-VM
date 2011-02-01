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
#include "os.h"
#include <stdlib.h>

#include "log.h"
#include "word.h"
#include "jni.h"
#include "dataio.h"
#include "relocation.h"

#define DEBUG_RELOCATION 0

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
                            if (DEBUG_RELOCATION) { \
                                log_println("%p: %p -> %p", p, value, value + relocatedHeap); \
                            } \
                            value = value + relocatedHeap; \
                            putWord(p, value); \
                        } \
                    } \
                    dataOffset += wordSize; \
                } \
            } \
        } \
    } while (0)

/**
 * Relocates the pointers in the heap and code. All the pointers are assumed to be
 * canonicalized; their current values assume that the heap and code start address 0.
 *
 * @param heap the physical address at which the (contiguous) heap and code reside
 * @param relocatedHeap the logical address to which the heap and code is being relocated
 * @param relocationData the bit map denoting where all the pointers are in the heap and code
 * @param relocationDataSize the size (in bytes) of the bit map
 */
void relocation_apply(void *heap, Address relocatedHeap, void *relocationData, int relocationDataSize, int isBigEndian, int wordSize) {
    int i, bit;
    Address base = (Address) heap;
    Byte *bytes = (Byte *) relocationData;
    int dataOffset = 0;

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
        log_println("wordSize=%d", wordSize);
        c_ASSERT(false);
    }
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_hosted_BootImage_nativeRelocate(JNIEnv *env, jclass c, jlong heap, jlong relocatedHeap,
                                                       jbyteArray relocationData, jint relocationDataSize,
                                                       jint isBigEndian, jint wordSize) {
    jboolean isCopy;
    jbyte *bytes = (*env)->GetByteArrayElements(env, relocationData, &isCopy);
    relocation_apply((void *) (Address) heap, (Address) relocatedHeap, bytes, relocationDataSize, isBigEndian, wordSize);
    (*env)->ReleaseByteArrayElements(env, relocationData, bytes, JNI_ABORT);
}
