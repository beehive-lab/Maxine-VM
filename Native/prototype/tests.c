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
 * @author Doug Simon
 */
#include "debug.h"
#include "jni.h"

JNIEXPORT void JNICALL
Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_nop(JNIEnv *env, jclass c)
{
    return;
}

JNIEXPORT void JNICALL
Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_nop_1cfunction(JNIEnv *env, jclass c)
{
    return;
}

JNIEXPORT void JNICALL
Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_nop_1cfunction_1jni(JNIEnv *env, jclass c)
{
    return;
}

JNIEXPORT jboolean JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_booleanIdentity(JNIEnv *env, jclass clazz, jboolean value) {
    return value;
}

JNIEXPORT jbyte JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_byteIdentity(JNIEnv *env, jclass clazz, jbyte value) {
    return value;
}

JNIEXPORT jchar JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_charIdentity(JNIEnv *env, jclass clazz, jchar value) {
    return value;
}

JNIEXPORT jshort JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_shortIdentity(JNIEnv *env, jclass clazz, jshort value) {
    return value;
}

JNIEXPORT jint JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_intIdentity(JNIEnv *env, jclass clazz, jint value) {
    return value;
}

JNIEXPORT jlong JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_longIdentity(JNIEnv *env, jclass clazz, jlong value) {
    return value;
}

JNIEXPORT jfloat JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_floatIdentity(JNIEnv *env, jclass clazz, jfloat value) {
    return value;
}

JNIEXPORT jdouble JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_doubleIdentity(JNIEnv *env, jclass clazz, jdouble value) {
    return value;
}

JNIEXPORT jobject JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_referenceIdentity(JNIEnv *env, jclass clazz, jobject value) {
    return value;
}

JNIEXPORT void JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_manyParameters
  (JNIEnv *env, jclass clazz, jobject reflectedMethod, jobject sb, jobject object1, jint int1, jlong long1, jshort short1, jchar char1, jobject object2, jint int2, jlong long2, jshort short2, jchar char2)
{
    jmethodID methodID = (*env)->FromReflectedMethod(env, reflectedMethod);
    (*env)->CallStaticObjectMethod(env, clazz, methodID, sb, object1, int1, long1, short1, char1, object2, int2, long2, short2, char2);
}

JNIEXPORT jobject JNICALL Java_test_com_sun_max_vm_compiler_bytecode_BytecodeTest_1native_manyObjectParameters
    (JNIEnv *env, jclass clazz,
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
