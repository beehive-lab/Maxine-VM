/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * JNI code for any of the JavaTester tests that use native methods.
 */
#include "os.h"

#include <pthread.h>
#include "jni.h"

JNIEXPORT void JNICALL
Java_test_output_MixedFrames_nativeUpdateFields(JNIEnv *env, jobject object, int n, jint i, jobject o) {
    jclass thisClass = (*env)->GetObjectClass(env, object);
    if (thisClass == NULL) {
        printf("Could not get class of object\n");
        return;
    }
    //printf("[native] n = %d\n", n);
    //fflush(stdout);

    jmethodID mid = (*env)->GetMethodID(env, thisClass, "testNative", "(I)V");
    if (mid == NULL) {
        printf("Could not find method testNative(int)\n");
        return;
    }
    if (n == 0) {
        jfieldID iField;
        jfieldID oField;
        iField = (*env)->GetFieldID(env, thisClass, "i", "I");
        if (iField == NULL) {
            printf("Could not find field i\n");
            return;
        }

        oField = (*env)->GetFieldID(env, thisClass, "o", "Ljava/lang/Object;");
        if (oField == NULL) {
            printf("Could not find field o\n");
            return;
        }

        (*env)->SetIntField(env, object, iField, i);
        (*env)->SetObjectField(env, object, oField, o);
    }
    (*env)->CallVoidMethod(env, object, mid, n, i, o);
}

void upcall(jclass cls) {
    JavaVM *vm;
    jsize vmBufLen = 1;
    jsize nVMs;
    JNIEnv *env;
    JavaVMAttachArgs attachArgs;
    jmethodID mid;
    jstring jstr;

    attachArgs.version = JNI_VERSION_1_2;
    attachArgs.name = "pthread";
    attachArgs.group = NULL;

    JNI_GetCreatedJavaVMs(&vm, vmBufLen, &nVMs);
    int result = (*vm)->AttachCurrentThread(vm, (void **)&env, &attachArgs);
    if (result != JNI_OK) {
        fprintf(stderr, "Could not attach to VM: error=%d\n", result);
        return;
    }

    mid = (*env)->GetStaticMethodID(env, cls, "helloWorld", "(Ljava/lang/String;)V");
    if (mid == 0) {
        fprintf(stderr, "Can't find method helloWorld(String)\n");
        return;
    }

    jstr = (*env)->NewStringUTF(env, "(from upcall)");
    (*env)->CallStaticVoidMethod(env, cls, mid, jstr);

    (*env)->DeleteGlobalRef(env, cls);
    (*vm)->DetachCurrentThread(vm);
}

void *thread_function(void *arguments) {
    upcall((jclass) arguments);
    return NULL;
}

JNIEXPORT void JNICALL
Java_test_output_AttachThread_callHelloWorldOnAttachedThread(JNIEnv *env, jclass clazz) {
    pthread_t thread_id;
    pthread_attr_t attributes;

    /* Convert argument to be a global handle as it is going to the new thread */
    clazz = (*env)->NewGlobalRef(env, clazz);
    void *arguments = clazz;

    pthread_attr_init(&attributes);
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);
    pthread_create(&thread_id, &attributes, thread_function, arguments);
    pthread_attr_destroy(&attributes);
}
