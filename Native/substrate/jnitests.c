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
 * A set of tests that need to be in the target.
 *
 * @author Doug Simon
 */
#include "log.h"
#include "jni.h"

#define BUFSIZE 8192
JNIEXPORT jint JNICALL
Java_jtt_jni_JNI_1OverflowArguments_read1(JNIEnv *env, jclass cls, jlong zfile,
														jlong zentry, jlong pos, jbyteArray bytes, jint off, jint len) {
  if (len > BUFSIZE) {
	 len = BUFSIZE;
  }
  return len;
}

JNIEXPORT jint JNICALL
Java_jtt_jni_JNI_1OverflowArguments_read2(JNIEnv *env, jclass cls, jlong zfile,
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

JNIEXPORT void JNICALL
Java_jtt_jni_JNI_1Nop_nop(JNIEnv *env, jclass c) {
}

JNIEXPORT void JNICALL
Java_jtt_jni_JNI_1Nop_sync_1nop(JNIEnv *env, jclass c) {
}

JNIEXPORT jobject JNICALL
Java_jtt_jni_JNI_1IdentityObject_id_1object (JNIEnv *env, jclass c, jobject o) {
    return o;
}
JNIEXPORT jbyte JNICALL
Java_jtt_jni_JNI_1IdentityByte_id_1byte(JNIEnv *env, jclass c, jbyte b) {
    return b;
}
JNIEXPORT jboolean JNICALL
Java_jtt_jni_JNI_1IdentityBoolean_id_1boolean(JNIEnv *env, jclass c, jboolean b) {
    return b;
}
JNIEXPORT jchar JNICALL
Java_jtt_jni_JNI_1IdentityChar_id_1char(JNIEnv *env, jclass c, jchar ch) {
    return ch;
}
JNIEXPORT jshort JNICALL
Java_jtt_jni_JNI_1IdentityShort_id_1short(JNIEnv *env, jclass c, jshort s) {
    return s;
}
JNIEXPORT jint JNICALL
Java_jtt_jni_JNI_1IdentityInt_id_1int(JNIEnv *env, jclass c, jint i) {
    return i;
}
JNIEXPORT jfloat JNICALL
Java_jtt_jni_JNI_1IdentityFloat_id_1float(JNIEnv *env, jclass c, jfloat f) {
    return f;
}
JNIEXPORT jlong JNICALL
Java_jtt_jni_JNI_1IdentityLong_id_1long(JNIEnv *env, jclass c, jlong l) {
    return l;
}

JNIEXPORT void JNICALL
Java_jtt_jni_JNI_1ManyParameters_manyParameters(JNIEnv *env, jclass clazz,
        jobject reflectedMethod,
        jobject sb,
        jobject object1,
        jint int1,
        jlong long1,
        jshort short1,
        jchar char1,
        jobject object2,
        jint int2,
        jlong long2,
        jshort short2,
        jchar char2)
{
    jmethodID methodID = (*env)->FromReflectedMethod(env, reflectedMethod);
    (*env)->CallStaticVoidMethod(env, clazz, methodID, sb, object1, int1, long1, short1, char1, object2, int2, long2, short2, char2);
}

JNIEXPORT jobject JNICALL
Java_jtt_jni_JNI_1ManyObjectParameters_manyObjectParameters(JNIEnv *env, jclass clazz,
        jobject array,
        jobject object0,
        jobject object1,
        jobject object2,
        jobject object3,
        jobject object4,
        jobject object5,
        jobject object6,
        jobject object7,
        jobject object8,
        jobject object9,
        jobject object10,
        jobject object11,
        jobject object12,
        jobject object13,
        jobject object14,
        jobject object15,
        jobject object16,
        jobject object17,
        jobject object18,
        jobject object19,
        jobject object20,
        jobject object21,
        jobject object22,
        jobject object23,
        jobject object24,
        jobject object25,
        jobject object26,
        jobject object27,
        jobject object28,
        jobject object29,
        jobject object30,
        jobject object31,
        jobject object32,
        jobject object33,
        jobject object34,
        jobject object35,
        jobject object36,
        jobject object37,
        jobject object38,
        jobject object39,
        jobject object40,
        jobject object41,
        jobject object42,
        jobject object43,
        jobject object44,
        jobject object45,
        jobject object46,
        jobject object47,
        jobject object48,
        jobject object49,
        jobject object50,
        jobject object51,
        jobject object52,
        jobject object53,
        jobject object54,
        jobject object55)
{
    (*env)->SetObjectArrayElement(env, array, 0, object0);
    (*env)->SetObjectArrayElement(env, array, 1, object1);
    (*env)->SetObjectArrayElement(env, array, 2, object2);
    (*env)->SetObjectArrayElement(env, array, 3, object3);
    (*env)->SetObjectArrayElement(env, array, 4, object4);
    (*env)->SetObjectArrayElement(env, array, 5, object5);
    (*env)->SetObjectArrayElement(env, array, 6, object6);
    (*env)->SetObjectArrayElement(env, array, 7, object7);
    (*env)->SetObjectArrayElement(env, array, 8, object8);
    (*env)->SetObjectArrayElement(env, array, 9, object9);
    (*env)->SetObjectArrayElement(env, array, 10, object10);
    (*env)->SetObjectArrayElement(env, array, 11, object11);
    (*env)->SetObjectArrayElement(env, array, 12, object12);
    (*env)->SetObjectArrayElement(env, array, 13, object13);
    (*env)->SetObjectArrayElement(env, array, 14, object14);
    (*env)->SetObjectArrayElement(env, array, 15, object15);
    (*env)->SetObjectArrayElement(env, array, 16, object16);
    (*env)->SetObjectArrayElement(env, array, 17, object17);
    (*env)->SetObjectArrayElement(env, array, 18, object18);
    (*env)->SetObjectArrayElement(env, array, 19, object19);
    (*env)->SetObjectArrayElement(env, array, 20, object20);
    (*env)->SetObjectArrayElement(env, array, 21, object21);
    (*env)->SetObjectArrayElement(env, array, 22, object22);
    (*env)->SetObjectArrayElement(env, array, 23, object23);
    (*env)->SetObjectArrayElement(env, array, 24, object24);
    (*env)->SetObjectArrayElement(env, array, 25, object25);
    (*env)->SetObjectArrayElement(env, array, 26, object26);
    (*env)->SetObjectArrayElement(env, array, 27, object27);
    (*env)->SetObjectArrayElement(env, array, 28, object28);
    (*env)->SetObjectArrayElement(env, array, 29, object29);
    (*env)->SetObjectArrayElement(env, array, 30, object30);
    (*env)->SetObjectArrayElement(env, array, 31, object31);
    (*env)->SetObjectArrayElement(env, array, 32, object32);
    (*env)->SetObjectArrayElement(env, array, 33, object33);
    (*env)->SetObjectArrayElement(env, array, 34, object34);
    (*env)->SetObjectArrayElement(env, array, 35, object35);
    (*env)->SetObjectArrayElement(env, array, 36, object36);
    (*env)->SetObjectArrayElement(env, array, 37, object37);
    (*env)->SetObjectArrayElement(env, array, 38, object38);
    (*env)->SetObjectArrayElement(env, array, 39, object39);
    (*env)->SetObjectArrayElement(env, array, 40, object40);
    (*env)->SetObjectArrayElement(env, array, 41, object41);
    (*env)->SetObjectArrayElement(env, array, 42, object42);
    (*env)->SetObjectArrayElement(env, array, 43, object43);
    (*env)->SetObjectArrayElement(env, array, 44, object44);
    (*env)->SetObjectArrayElement(env, array, 45, object45);
    (*env)->SetObjectArrayElement(env, array, 46, object46);
    (*env)->SetObjectArrayElement(env, array, 47, object47);
    (*env)->SetObjectArrayElement(env, array, 48, object48);
    (*env)->SetObjectArrayElement(env, array, 49, object49);
    (*env)->SetObjectArrayElement(env, array, 50, object50);
    (*env)->SetObjectArrayElement(env, array, 51, object51);
    (*env)->SetObjectArrayElement(env, array, 52, object52);
    (*env)->SetObjectArrayElement(env, array, 53, object53);
    (*env)->SetObjectArrayElement(env, array, 54, object54);
    (*env)->SetObjectArrayElement(env, array, 55, object55);
    return array;
}
