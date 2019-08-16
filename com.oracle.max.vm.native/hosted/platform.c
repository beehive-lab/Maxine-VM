/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
#include <unistd.h>
#include "word.h"
#include "isa.h"
#include "jni.h"
#include <string.h>
#include <stdlib.h>

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
#elif os_MAXVE
    return (*env)->NewStringUTF(env, "MAXVE");
#else
#   error
#endif
}

JNIEXPORT jint JNICALL
Java_com_sun_max_platform_Platform_nativeGetPageSize(JNIEnv *env, jclass c) {
    return (jint) sysconf(_SC_PAGESIZE);
}


JNIEXPORT jint JNICALL
Java_com_sun_max_platform_Platform_nativeHasIDiv(JNIEnv *env, jclass c) {
#ifdef arm
    FILE *cpuinfo = fopen("/proc/cpuinfo", "rb");
    char *arg = 0;
    size_t size = 0;
    while(getdelim(&arg, &size, 0, cpuinfo) != -1) {
        if (strstr(arg, "idiva") != 0) {
           return (jint) 1;
        }
    }
    free(arg);
    fclose(cpuinfo);
#else
    return (jint) 1;
#endif
    return (jint) 0;
}

/*
 *  ATTENTION: return value must correspond to an ISA enum value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_platform_Platform_nativeGetISA(JNIEnv *env, jclass c)
{
#if isa_AMD64
    return (*env)->NewStringUTF(env, "AMD64");
#elif isa_IA32
    return (*env)->NewStringUTF(env, "IA32");
#elif isa_POWER
    return (*env)->NewStringUTF(env, "PPC");
#elif isa_SPARC
    return (*env)->NewStringUTF(env, "SPARC");
#elif isa_ARM
    return (*env)->NewStringUTF(env, "ARM");
#elif isa_AARCH64
    return (*env)->NewStringUTF(env, "Aarch64");
#elif isa_RISCV64
    return (*env)->NewStringUTF(env, "Riscv64");
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
#elif os_MAXVE
    return 0;
#else
#   error
#endif
}

JNIEXPORT jstring JNICALL
Java_com_sun_max_platform_Platform_nativeJniHeaderFilePath(JNIEnv *env, jclass c)
{
#ifndef JNI_H_PATH
#error JNI_H_PATH should be defined
#endif
    return (*env)->NewStringUTF(env, JNI_H_PATH);
}
