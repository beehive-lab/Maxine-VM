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
