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
#include <string.h>

#include "word.h"
#include "jni.h"
#include "debug.h"

/*
 * We make no assumptions at all as to what alignment is required for any multi-byte access
 * and we make very little assumptions about the sizes of primitive Java types ('j...').
 * So, as a portable solution avoiding any potential alignment issues,
 * we use memcpy() instead of '*p'-style dereferencing.
 */

JNIEXPORT jbyte JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadByteAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jbyte result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jbyte JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadByteAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jbyte result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT jshort JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadShortAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jshort result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jshort JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadShortAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jshort result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT jchar JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadCharAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jchar result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jchar JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadCharAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jchar result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT jint JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadIntAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jint result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadIntAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jint result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT jfloat JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadFloatAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jfloat result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jfloat JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadFloatAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jfloat result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT jlong JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadLongAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jlong result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadLongAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jlong result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jobject JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadObjectAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jobject result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jdouble JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadDoubleAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset)
{
    jdouble result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}

JNIEXPORT jdouble JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeReadDoubleAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset)
{
    jdouble result;
    memcpy(&result, (char *) ((Address) pointer) + offset, sizeof(result));
    return result;
}


JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteByteAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jbyte value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteByteAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jbyte value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}


JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteShortAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jshort value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteShortAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jshort value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteIntAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jint value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteIntAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jint value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}


JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteFloatAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jfloat value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteFloatAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jfloat value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}


JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteLongAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jlong value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteLongAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jlong value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteObjectAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jobject value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteDoubleAtLongOffset(JNIEnv *env, jclass c, jlong pointer, jlong offset, jdouble value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}

JNIEXPORT void JNICALL
Java_com_sun_max_unsafe_box_BoxedPointer_nativeWriteDoubleAtIntOffset(JNIEnv *env, jclass c, jlong pointer, jint offset, jdouble value)
{
    memcpy((char *) ((Address) pointer) + offset, &value, sizeof(value));
}
