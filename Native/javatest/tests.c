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
    void *arguments = clazz;

    pthread_attr_init(&attributes);
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_JOINABLE);
    pthread_create(&thread_id, &attributes, thread_function, arguments);
    pthread_attr_destroy(&attributes);

    pthread_join(thread_id, NULL);
}
