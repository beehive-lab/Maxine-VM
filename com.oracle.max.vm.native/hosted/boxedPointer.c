/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>

#include "word.h"
#include "jni.h"
#include "log.h"

/*
 * We make no assumptions at all as to what alignment is required for any multi-byte access
 * and we make very little assumptions about the sizes of primitive Java types ('j...').
 * So, as a portable solution avoiding any potential alignment issues,
 * we use memcpy() instead of '*p'-style dereferencing.
 */

JNIEXPORT jbyte JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadByte(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jbyte result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jshort JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadShort(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jshort result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jchar JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadChar(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jchar result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadInt(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jint result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jfloat JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadFloat(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jfloat result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadLong(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jlong result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jobject JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadObject(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jobject result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jdouble JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeReadDouble(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jdouble result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteByte(JNIEnv *env, jclass c, jlong pointer, jlong offset, jbyte value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteShort(JNIEnv *env, jclass c, jlong pointer, jlong offset, jshort value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteInt(JNIEnv *env, jclass c, jlong pointer, jlong offset, jint value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteFloat(JNIEnv *env, jclass c, jlong pointer, jlong offset, jfloat value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteLong(JNIEnv *env, jclass c, jlong pointer, jlong offset, jlong value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteObject(JNIEnv *env, jclass c, jlong pointer, jlong offset, jobject value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_BoxedPointer_nativeWriteDouble(JNIEnv *env, jclass c, jlong pointer, jlong offset, jdouble value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}
