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
/**
 * JNI test code for the JVM_* native interface.
 * This JNI code supports unit testing of the JVM interface through the JavaTester.
 */

#include "os.h"

#include <stdlib.h>
#include <string.h>

#include "jni.h"

JNIEXPORT jobjectArray JNICALL
JVM_GetClassContext(JNIEnv *env);

JNIEXPORT jboolean JNICALL
JVM_IsNaN(JNIEnv *env, jdouble d);

JNIEXPORT jlong JNICALL
JVM_MaxMemory();

JNIEXPORT jlong JNICALL
JVM_TotalMemory();

JNIEXPORT jlong JNICALL
JVM_FreeMemory();

JNIEXPORT void JNICALL
JVM_ArrayCopy(JNIEnv *env, jclass ignored, jobject src, jint src_pos,
               jobject dst, jint dst_pos, jint length);

JNIEXPORT jobject JNICALL
Java_jtt_jvmni_JVM_1GetClassContext01_call(JNIEnv *env, jclass c)
{
    return JVM_GetClassContext(env);
}

JNIEXPORT jobject JNICALL
Java_jtt_jvmni_JVM_1GetClassContext02_downCall1(JNIEnv *env, jclass c)
{
    jclass jClass;
    jmethodID jMethod;
    char *className = "jtt/jvmni/JVM_GetClassContext02";
    char *methodName = "upCall1";
    char *signature = "()[Ljava/lang/Class;";
    jClass = (*env)->FindClass(env, className);
    if (jClass == NULL) {
        return NULL;
    }
    jMethod = (*env)->GetStaticMethodID(env, jClass, methodName, signature);
    if (jMethod == NULL) {
        return NULL;
    }
    return (*env)->CallStaticObjectMethod(env, jClass, jMethod);
}

JNIEXPORT jobject JNICALL
Java_jtt_jvmni_JVM_1GetClassContext02_downCall2(JNIEnv *env, jclass c)
{
    return JVM_GetClassContext(env);
}


JNIEXPORT jboolean JNICALL
Java_jtt_jvmni_JVM_1IsNaN01_call(JNIEnv *env, jdouble d)
{
	return JVM_IsNaN(env,d);
}

JNIEXPORT jlong JNICALL
Java_jtt_jvmni_JVM_1GetMaxMemory01_call(JNIEnv *env)
{
	return JVM_MaxMemory();
}

JNIEXPORT jlong JNICALL
Java_jtt_jvmni_JVM_1GetTotalMemory01_call(JNIEnv *env)
{
	return JVM_TotalMemory();
}

JNIEXPORT jlong JNICALL
Java_jtt_jvmni_JVM_1GetFreeMemory01_call(JNIEnv *env)
{
	return JVM_FreeMemory();
}

JNIEXPORT void JNICALL
Java_jtt_jvmni_JVM_1ArrayCopy01_call(JNIEnv *env, jclass jc, jobject src, jint src_pos, jobject dest, jint dest_pos, jint len)
{
	JVM_ArrayCopy(env, jc, src, src_pos, dest, dest_pos, len);
}

