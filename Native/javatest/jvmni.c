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
/*VCSID=f2514637-aaf0-41b7-a6b7-3c0144ed20e6*/
/**
 * JNI test code for the JVM_* native interface.
 * This JNI code supports unit testing of the JVM interface through the JavaTester.
 */

#include "os.h"

#include <stdlib.h>
#include <string.h>

#include "jni.h"

JNIEXPORT jobject JNICALL
Java_test_jvmni_JVM_1GetClassContext01_call(JNIEnv *env, jclass c)
{
    return JVM_GetClassContext(env);
}

JNIEXPORT jboolean JNICALL
Java_test_jvmni_JVM_1IsNaN01_call(JNIEnv *env, jdouble d)
{
	return JVM_IsNaN(env,d);
}

JNIEXPORT jlong JNICALL
Java_test_jvmni_JVM_1GetMaxMemory01_call(JNIEnv *env)
{
	return JVM_MaxMemory();
}

JNIEXPORT jlong JNICALL
Java_test_jvmni_JVM_1GetTotalMemory01_call(JNIEnv *env)
{
	return JVM_TotalMemory();
}

JNIEXPORT jlong JNICALL
Java_test_jvmni_JVM_1GetFreeMemory01_call(JNIEnv *env)
{
	return JVM_FreeMemory();
}

JNIEXPORT void JNICALL
Java_test_jvmni_JVM_1ArrayCopy01_call(JNIEnv *env, jclass jc, jobject src, jint src_pos, jobject dest, jint dest_pos, jint len)
{
	JVM_ArrayCopy(env, jc, src, src_pos, dest, dest_pos, len);
}

