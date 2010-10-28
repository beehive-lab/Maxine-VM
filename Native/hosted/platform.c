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
#include <unistd.h>
#include "word.h"
#include "isa.h"
#include "jni.h"

JNIEXPORT void JNICALL
JVM_OnLoad(JavaVM *vm, char *options, void *arg)
{
    c_initialize();
}

/*
 *  ATTENTION: return value must correspond to an OS enum value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_platform_Platform_nativeGetOS(JNIEnv *env, jclass c)
{
#if os_DARWIN
    return (*env)->NewStringUTF(env, "DARWIN");
#elif os_LINUX
    return (*env)->NewStringUTF(env, "LINUX");
#elif os_SOLARIS
    return (*env)->NewStringUTF(env, "SOLARIS");
#elif os_WINDOWS
    return (*env)->NewStringUTF(env, "WINDOWS");
#elif os_GUESTVMXEN
    return (*env)->NewStringUTF(env, "GUESTVM");
#else
#   error
#endif
}

JNIEXPORT jint JNICALL
Java_com_sun_max_platform_Platform_nativeGetPageSize(JNIEnv *env, jclass c) {
    return (jint) sysconf(_SC_PAGESIZE);
}

/*
 *  ATTENTION: return value must correspond to an InstructionSet enum value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_platform_Platform_nativeGetInstructionSet(JNIEnv *env, jclass c)
{
#if isa_AMD64
    return (*env)->NewStringUTF(env, "AMD64");
#elif isa_IA32
    return (*env)->NewStringUTF(env, "IA32");
#elif isa_POWER
    return (*env)->NewStringUTF(env, "PPC");
#elif isa_SPARC
    return (*env)->NewStringUTF(env, "SPARC");
#else
#   error
#endif
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_platform_Platform_nativeIsBigEndian(JNIEnv *env, jclass c)
{
    return word_BIG_ENDIAN;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_platform_Platform_nativeGetWordWidth(JNIEnv *env, jclass c)
{
    return word_64_BITS ? 64 : 32;
}

#if os_DARWIN || os_LINUX || os_WINDOWS
#include <signal.h>
#endif

JNIEXPORT jint JNICALL
Java_com_sun_max_platform_Platform_nativeNumberOfSignals(JNIEnv *env, jclass c)
{
#if os_DARWIN || os_LINUX || os_WINDOWS
    return NSIG;
#elif os_SOLARIS
    return SIGRTMAX;
#elif os_GUESTVMXEN
    return 0;
#else
#   error
#endif
}

JNIEXPORT jstring JNICALL
Java_com_sun_max_platform_Platform_jniHeaderFilePath(JNIEnv *env, jclass c)
{
#ifndef JNI_H_PATH
#error JNI_H_PATH should be defined
#endif
    return (*env)->NewStringUTF(env, STRINGIZE(JNI_H_PATH));
}

