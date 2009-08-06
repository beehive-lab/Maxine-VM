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
 * A set of tests that need to be in the target.
 *
 * @author Doug Simon
 */
#include "log.h"
#include "jni.h"

JNIEXPORT void JNICALL
Java_com_sun_max_vm_run_compilerTest_CompilerTestRunScheme_nativeUpdateFields(JNIEnv *env, jobject object, int n, jint i, jobject o) {
	jclass _class = (*env)->GetObjectClass(env, object);
    if (_class == NULL) {
    	printf("Could not get class of object\n");
    	return;
    }

    if (n > 0) {
    	jmethodID mid = (*env)->GetMethodID(env, _class, "testNative", "(I)V");
    	if (mid == NULL) {
        	printf("Could not find method testNative(int)\n");
        	return;
    	}
    	(*env)->CallVoidMethod(env, object, mid, n, i, o);
    } else {
        jfieldID _i;
        jfieldID _o;
    	_i = (*env)->GetFieldID(env, _class, "_i", "I");
    	if (_i == NULL) {
    		printf("Could not find field _i\n");
    		return;
    	}

    	_o = (*env)->GetFieldID(env, _class, "_o", "Ljava/lang/Object;");
    	if (_o == NULL) {
    		printf("Could not find field _o\n");
    		return;
    	}

    	(*env)->SetIntField(env, object, _i, i);
    	(*env)->SetObjectField(env, object, _o, o);
    }
}


#define BUFSIZE 8192
JNIEXPORT jint JNICALL
Java_test_jni_JNI_1OverflowArguments_read1(JNIEnv *env, jclass cls, jlong zfile,
														jlong zentry, jlong pos, jbyteArray bytes, jint off, jint len) {
  if (len > BUFSIZE) {
	 len = BUFSIZE;
  }
  return len;
}

JNIEXPORT jint JNICALL
Java_test_jni_JNI_1OverflowArguments_read2(JNIEnv *env, jclass cls, jlong zfile,
														jlong zentry, jlong pos, jbyteArray bytes, jint off, jint len) {
  return off;
}

JNIEXPORT jlong JNICALL
Java_test_bench_threads_JNI_1invocations_nativework(JNIEnv *env, jclass cls, jlong workload) {
    int i = 0;
    int sum = 0;
    for (i=0; i<workload; i++) {
        sum +=i;
    }
    return sum;
}
