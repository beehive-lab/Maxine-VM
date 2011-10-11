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
 * This file implements JNI functions that take a variable number of arguments. These
 * functions are essentially wrappers that copy the varargs into a stack allocated
 * jvalue array and then call the version of the same JNI function that takes its
 * arguments in such an array. This isolates the implementation of such functions
 * from the platform/compiler dependent way in which varargs are implemented.
 */
#include <alloca.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

#include "log.h"
#include "kind.h"
#include "word.h"
#include "threads.h"

#include "jni.h"

#ifndef  JNI_VERSION_1_6
#error The version of jni.h being included must define the JNI_VERSION_1_6 macro
#endif
static jint CurrentVersion = JNI_VERSION_1_6;

/**
 * Type that extends the standard JNI function table to add GetNumberOfArguments() and GetKindsOfArguments() at the end.
 */
typedef struct ExtendedJNINativeInterface_ {
    struct JNINativeInterface_ jniNativeInterface;

    jint (JNICALL *GetNumberOfArguments)(JNIEnv *env, jmethodID methodID);
    void (JNICALL *GetKindsOfArguments)(JNIEnv *env, jmethodID methodID, char *kinds);
} *ExtendedJniEnv;

/**
 * The global (extended) JNI function table.
 */
extern struct ExtendedJNINativeInterface_ jni_ExtendedNativeInterface;

/**
 * The global JNI Invocation API function table.
 */
extern struct JavaVM_ main_vm;

/**
 * Gets a pointer to the global JNI function table.
 */
JNIEnv jniEnv() {
    return &jni_ExtendedNativeInterface.jniNativeInterface;
}

/**
 * Gets the thread-local pointer to the pointer to the global JNI function table.
 */
JNIEnv *currentJniEnv() {
    TLA tla = tla_current();
    c_ASSERT(tla != 0);
    JNIEnv *env = (JNIEnv *) tla_addressOf(tla, JNI_ENV);
    c_ASSERT(env != NULL);
    return env;
}

typedef jint (JNICALL *JNI_OnLoad_t)(JavaVM *, void *);

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_jni_DynamicLinker_invokeJNIOnLoad(JNIEnv *env, jclass c, JNI_OnLoad_t JNI_OnLoad) {
    return (*JNI_OnLoad)((JavaVM *) &main_vm, NULL);
}

/**
 * Copies the varargs in 'argumentList' into 'argumentArray' according to the types specified in 'kinds'.
 * Note that argumentArray is an array of jvalue elements and so the arguments are not packed in
 * the array. That is, the address of argument 'n' will be '&argumentArray[n]' and does not depend on
 * the types of the arguments preceeding it.
 */
static void copyVarargsToArray(jvalue *argumentArray, va_list argumentList, int numberOfArguments, char *kinds) {
    int i;
    for (i = 0; i < numberOfArguments; i++) {
        switch (kinds[i]) {
            case kind_BYTE:
            case kind_BOOLEAN:
            case kind_SHORT:
            case kind_CHAR:
            case kind_INT: {
                *((jint *) (&argumentArray[i])) = va_arg(argumentList, jint);
                break;
            }
            case kind_FLOAT: {
                /* Comment from jni.cpp in Hotspot: float is coerced to double wrt va_arg */
                *((jfloat *) (&argumentArray[i])) = (jfloat) va_arg(argumentList, jdouble);
                break;
            }
            case kind_LONG:  {
                *((jlong *) (&argumentArray[i])) = va_arg(argumentList, jlong);
                break;
            }
            case kind_DOUBLE: {
                *((jdouble *) (&argumentArray[i])) = va_arg(argumentList, jdouble);
                break;
            }
            case kind_WORD:
            case kind_REFERENCE: {
                *((Word *) (&argumentArray[i])) = va_arg(argumentList, Word);
                break;
            }
            default: {
	            log_exit(1, "callObjectMethodV: unknown kind = %d\n", kinds[i]);
             }
        }
    }
}

/**
 * Copies the varargs from their platform dependent locations into a jvalue array allocated on
 * the current call stack. This array can then be passed to the corresponding routine that
 * takes such an array of arguments.
 *
 * This copying of arguments allocates two arrays on the stack; a jbyte array for getting
 * the kinds of the arguments and the jvalue array for the copied arguments. The number
 * of arguments is determined by parsing the method's signature.
 */
#define PREPARE_CALL \
    ExtendedJniEnv extendedJniEnv; \
    int numberOfVarArgs; \
    jvalue *argumentArray; \
    char *kinds; \
    \
    extendedJniEnv = (ExtendedJniEnv) *env; \
    numberOfVarArgs = extendedJniEnv->GetNumberOfArguments(env, methodID); \
    /* Space for array of argument kinds is stack allocated and so needs no corresponding deallocation */ \
    kinds = (char *) alloca(numberOfVarArgs); \
    extendedJniEnv->GetKindsOfArguments(env, methodID, kinds); \
    /* Space for arguments is stack allocated and so needs no corresponding deallocation */ \
    argumentArray = (jvalue *) alloca(sizeof(jvalue) * numberOfVarArgs); \
    \
    copyVarargsToArray(argumentArray, argumentList, numberOfVarArgs, kinds);

/*
 * Call<type>Method Routines
 */

#define CALL_METHOD(functionName, returnType) do { \
    va_list argumentList; \
    returnType result; \
    va_start(argumentList, methodID); \
    result = (*env)-> functionName (env, object, methodID, argumentList); \
    va_end(argumentList); \
    return result; \
    } while (0)

#define CALL_VOID_METHOD(functionName) do { \
    va_list argumentList; \
    va_start(argumentList, methodID); \
    (*env)-> functionName (env, object, methodID, argumentList); \
    va_end(argumentList); \
    } while (0)

static jobject jni_CallObjectMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallObjectMethodV, jobject);
}

static jboolean jni_CallBooleanMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallBooleanMethodV, jboolean);
}

static jbyte jni_CallByteMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallByteMethodV, jbyte);
}

static jchar jni_CallCharMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallCharMethodV, jchar);
}

static jshort jni_CallShortMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallShortMethodV, jshort);
}

static jint jni_CallIntMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallIntMethodV, jint);
}

static jlong jni_CallLongMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallLongMethodV, jlong);
}

static jfloat jni_CallFloatMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallFloatMethodV, jfloat);
}

static jdouble jni_CallDoubleMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallDoubleMethodV, jdouble);
}

static void jni_CallVoidMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_VOID_METHOD(CallVoidMethodV);
}

/*
 * CallNonvirtual<type>Method Routines
 */
#define CALL_NONVIRTUAL_METHOD(functionName, returnType) do { \
    va_list argumentList; \
    returnType result; \
    va_start(argumentList, methodID); \
    result = (*env)-> functionName (env, object, javaClass, methodID, argumentList); \
    va_end(argumentList); \
    return result; \
    } while (0)

#define CALL_VOID_NONVIRTUAL_METHOD(functionName) do { \
    va_list argumentList; \
    va_start(argumentList, methodID); \
    (*env)-> functionName (env, object, javaClass, methodID, argumentList); \
    va_end(argumentList); \
    } while (0)

static jobject jni_CallNonvirtualObjectMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualObjectMethodV, jobject);
}

static jboolean jni_CallNonvirtualBooleanMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualBooleanMethodV, jboolean);
}

static jbyte jni_CallNonvirtualByteMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualByteMethodV, jbyte);
}

static jchar jni_CallNonvirtualCharMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualCharMethodV, jchar);
}

static jshort jni_CallNonvirtualShortMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualShortMethodV, jshort);
}

static jint jni_CallNonvirtualIntMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualIntMethodV, jint);
}

static jlong jni_CallNonvirtualLongMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualLongMethodV, jlong);
}

static jfloat jni_CallNonvirtualFloatMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualFloatMethodV, jfloat);
}

static jdouble jni_CallNonvirtualDoubleMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualDoubleMethodV, jdouble);
}

static void jni_CallNonvirtualVoidMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_VOID_NONVIRTUAL_METHOD(CallNonvirtualVoidMethodV);
}

/*
 * CallStatic<type>Method Routines
 */
#define CALL_STATIC_METHOD(functionName, returnType) do { \
    va_list argumentList; \
    returnType result; \
    va_start(argumentList, methodID); \
    result = (*env)-> functionName (env, javaClass, methodID, argumentList); \
    va_end(argumentList); \
    return result; \
    } while (0)

#define CALL_VOID_STATIC_METHOD(functionName) do { \
    va_list argumentList; \
    va_start(argumentList, methodID); \
    (*env)-> functionName (env, javaClass, methodID, argumentList); \
    va_end(argumentList); \
    } while (0)

static jobject jni_CallStaticObjectMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticObjectMethodV, jobject);
}

static jboolean jni_CallStaticBooleanMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticBooleanMethodV, jboolean);
}

static jbyte jni_CallStaticByteMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticByteMethodV, jbyte);
}

static jchar jni_CallStaticCharMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticCharMethodV, jchar);
}

static jshort jni_CallStaticShortMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticShortMethodV, jshort);
}

static jint jni_CallStaticIntMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticIntMethodV, jint);
}

static jlong jni_CallStaticLongMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticLongMethodV, jlong);
}

static jfloat jni_CallStaticFloatMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticFloatMethodV, jfloat);
}

static jdouble jni_CallStaticDoubleMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticDoubleMethodV, jdouble);
}

static void jni_CallStaticVoidMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_VOID_STATIC_METHOD(CallStaticVoidMethodV);
}


/*
 * Call<type>VMethod Routines
 */
#define CALL_METHOD_V(functionName) do { \
    PREPARE_CALL \
    return (*env)->functionName(env, object, methodID, argumentArray); \
    } while (0)

#define CALL_VOID_METHOD_V(functionName)  do { \
    PREPARE_CALL \
    (*env)->functionName(env, object, methodID, argumentArray); \
    } while (0)

static jobject jni_CallObjectMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallObjectMethodA);
}

static jboolean jni_CallBooleanMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallBooleanMethodA);
}

static jbyte jni_CallByteMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallByteMethodA);
}

static jchar jni_CallCharMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallCharMethodA);
}

static jshort jni_CallShortMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallShortMethodA);
}

static jint jni_CallIntMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallIntMethodA);
}

static jlong jni_CallLongMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallLongMethodA);
}

static jfloat jni_CallFloatMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallFloatMethodA);
}

static jdouble jni_CallDoubleMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallDoubleMethodA);
}

static void jni_CallVoidMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
    CALL_VOID_METHOD_V(CallVoidMethodA);
}

/*
 * CallNonvirtual<type>VMethod Routines
 */

#define CALL_NONVIRTUAL_METHOD_V(functionName) do { \
	PREPARE_CALL \
	return (*env)->functionName(env, object, javaClass, methodID, argumentArray); \
    } while (0)

#define CALL_VOID_NONVIRTUAL_METHOD_V(functionName) do { \
    PREPARE_CALL \
    (*env)->functionName(env, object, javaClass, methodID, argumentArray); \
    } while (0)

static jobject jni_CallNonvirtualObjectMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualObjectMethodA);
}

static jboolean jni_CallNonvirtualBooleanMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualBooleanMethodA);
}

static jbyte jni_CallNonvirtualByteMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualByteMethodA);
}

static jchar jni_CallNonvirtualCharMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualCharMethodA);
}

static jshort jni_CallNonvirtualShortMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualShortMethodA);
}

static jint jni_CallNonvirtualIntMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualIntMethodA);
}

static jlong jni_CallNonvirtualLongMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualLongMethodA);
}

static jfloat jni_CallNonvirtualFloatMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualFloatMethodA);
}

static jdouble jni_CallNonvirtualDoubleMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualDoubleMethodA);
}

static void jni_CallNonvirtualVoidMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
    CALL_VOID_NONVIRTUAL_METHOD_V(CallNonvirtualVoidMethodA);
}

/*
 * CallStatic<type>VMethod Routines
 */

#define CALL_STATIC_METHOD_V(functionName) do { \
	PREPARE_CALL \
	return (*env)->functionName(env, javaClass, methodID, argumentArray); \
    } while (0)

#define CALL_VOID_STATIC_METHOD_V(functionName)  do { \
    PREPARE_CALL \
    (*env)->functionName(env, javaClass, methodID, argumentArray); \
    } while (0)

static jobject jni_CallStaticObjectMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticObjectMethodA);
}

static jboolean jni_CallStaticBooleanMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticBooleanMethodA);
}

static jbyte jni_CallStaticByteMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticByteMethodA);
}

static jchar jni_CallStaticCharMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticCharMethodA);
}

static jshort jni_CallStaticShortMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticShortMethodA);
}

static jint jni_CallStaticIntMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticIntMethodA);
}

static jlong jni_CallStaticLongMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticLongMethodA);
}

static jfloat jni_CallStaticFloatMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticFloatMethodA);
}

static jdouble jni_CallStaticDoubleMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticDoubleMethodA);
}

static void jni_CallStaticVoidMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
    CALL_VOID_STATIC_METHOD_V(CallStaticVoidMethodA);
}

/*
 * NewObject Routines
 */
static jobject jni_NewObject(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(NewObjectV, jobject);
}

static jobject jni_NewObjectV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
    CALL_STATIC_METHOD_V(NewObjectA);
}

static jint jni_GetVersion(JNIEnv *env) {
    return CurrentVersion;
}

static jint jni_GetJavaVM(JNIEnv *env, JavaVM **vm) {
    *vm = (JavaVM *) (&main_vm);
    return JNI_OK;
}

static void jni_reserved() {
}

// Structure containing all  functions
struct ExtendedJNINativeInterface_ jni_ExtendedNativeInterface = {
    {
    (void *) jni_reserved,
    (void *) jni_reserved,
    (void *) jni_reserved,
    (void *) jni_reserved,

    jni_GetVersion,

    /* jni_DefineClass */ NULL,
    /* jni_FindClass */ NULL,

    /* jni_FromReflectedMethod */ NULL,
    /* jni_FromReflectedField */ NULL,

    /* jni_ToReflectedMethod */ NULL,

    /* jni_GetSuperclass */ NULL,
    /* jni_IsAssignableFrom */ NULL,

    /* jni_ToReflectedField */ NULL,

    /* jni_Throw */ NULL,
    /* jni_ThrowNew */ NULL,
    /* jni_ExceptionOccurred */ NULL,
    /* jni_ExceptionDescribe */ NULL,
    /* jni_ExceptionClear */ NULL,
    /* jni_FatalError */ NULL,

    /* jni_PushLocalFrame */ NULL,
    /* jni_PopLocalFrame */ NULL,

    /* jni_NewGlobalRef */ NULL,
    /* jni_DeleteGlobalRef */ NULL,
    /* jni_DeleteLocalRef */ NULL,
    /* jni_IsSameObject */ NULL,

    /* jni_NewLocalRef */ NULL,
    /* jni_EnsureLocalCapacity */ NULL,

    /* jni_AllocObject */ NULL,
    jni_NewObject,
    jni_NewObjectV,
    /* jni_NewObjectA */ NULL,

    /* jni_GetObjectClass */ NULL,
    /* jni_IsInstanceOf */ NULL,

    /* jni_GetMethodID */ NULL,

    jni_CallObjectMethod,
    jni_CallObjectMethodV,
    /* jni_CallObjectMethodA */ NULL,
    jni_CallBooleanMethod,
    jni_CallBooleanMethodV,
    /* jni_CallBooleanMethodA */ NULL,
    jni_CallByteMethod,
    jni_CallByteMethodV,
    /* jni_CallByteMethodA */ NULL,
    jni_CallCharMethod,
    jni_CallCharMethodV,
    /* jni_CallCharMethodA */ NULL,
    jni_CallShortMethod,
    jni_CallShortMethodV,
    /* jni_CallShortMethodA */ NULL,
    jni_CallIntMethod,
    jni_CallIntMethodV,
    /* jni_CallIntMethodA */ NULL,
    jni_CallLongMethod,
    jni_CallLongMethodV,
    /* jni_CallLongMethodA */ NULL,
    jni_CallFloatMethod,
    jni_CallFloatMethodV,
    /* jni_CallFloatMethodA */ NULL,
    jni_CallDoubleMethod,
    jni_CallDoubleMethodV,
    /* jni_CallDoubleMethodA */ NULL,
    jni_CallVoidMethod,
    jni_CallVoidMethodV,
    /* jni_CallVoidMethodA */ NULL,

    jni_CallNonvirtualObjectMethod,
    jni_CallNonvirtualObjectMethodV,
    /* jni_CallNonvirtualObjectMethodA */ NULL,
    jni_CallNonvirtualBooleanMethod,
    jni_CallNonvirtualBooleanMethodV,
    /* jni_CallNonvirtualBooleanMethodA */ NULL,
    jni_CallNonvirtualByteMethod,
    jni_CallNonvirtualByteMethodV,
    /* jni_CallNonvirtualByteMethodA */ NULL,
    jni_CallNonvirtualCharMethod,
    jni_CallNonvirtualCharMethodV,
    /* jni_CallNonvirtualCharMethodA */ NULL,
    jni_CallNonvirtualShortMethod,
    jni_CallNonvirtualShortMethodV,
    /* jni_CallNonvirtualShortMethodA */ NULL,
    jni_CallNonvirtualIntMethod,
    jni_CallNonvirtualIntMethodV,
    /* jni_CallNonvirtualIntMethodA */ NULL,
    jni_CallNonvirtualLongMethod,
    jni_CallNonvirtualLongMethodV,
    /* jni_CallNonvirtualLongMethodA */ NULL,
    jni_CallNonvirtualFloatMethod,
    jni_CallNonvirtualFloatMethodV,
    /* jni_CallNonvirtualFloatMethodA */ NULL,
    jni_CallNonvirtualDoubleMethod,
    jni_CallNonvirtualDoubleMethodV,
    /* jni_CallNonvirtualDoubleMethodA */ NULL,
    jni_CallNonvirtualVoidMethod,
    jni_CallNonvirtualVoidMethodV,
    /* jni_CallNonvirtualVoidMethodA */ NULL,

    /* jni_GetFieldID */ NULL,

    /* jni_GetObjectField */ NULL,
    /* jni_GetBooleanField */ NULL,
    /* jni_GetByteField */ NULL,
    /* jni_GetCharField */ NULL,
    /* jni_GetShortField */ NULL,
    /* jni_GetIntField */ NULL,
    /* jni_GetLongField */ NULL,
    /* jni_GetFloatField */ NULL,
    /* jni_GetDoubleField */ NULL,

    /* jni_SetObjectField */ NULL,
    /* jni_SetBooleanField */ NULL,
    /* jni_SetByteField */ NULL,
    /* jni_SetCharField */ NULL,
    /* jni_SetShortField */ NULL,
    /* jni_SetIntField */ NULL,
    /* jni_SetLongField */ NULL,
    /* jni_SetFloatField */ NULL,
    /* jni_SetDoubleField */ NULL,

    /* jni_GetStaticMethodID */ NULL,

    jni_CallStaticObjectMethod,
    jni_CallStaticObjectMethodV,
    /* jni_CallStaticObjectMethodA */ NULL,
    jni_CallStaticBooleanMethod,
    jni_CallStaticBooleanMethodV,
    /* jni_CallStaticBooleanMethodA */ NULL,
    jni_CallStaticByteMethod,
    jni_CallStaticByteMethodV,
    /* jni_CallStaticByteMethodA */ NULL,
    jni_CallStaticCharMethod,
    jni_CallStaticCharMethodV,
    /* jni_CallStaticCharMethodA */ NULL,
    jni_CallStaticShortMethod,
    jni_CallStaticShortMethodV,
    /* jni_CallStaticShortMethodA */ NULL,
    jni_CallStaticIntMethod,
    jni_CallStaticIntMethodV,
    /* jni_CallStaticIntMethodA */ NULL,
    jni_CallStaticLongMethod,
    jni_CallStaticLongMethodV,
    /* jni_CallStaticLongMethodA */ NULL,
    jni_CallStaticFloatMethod,
    jni_CallStaticFloatMethodV,
    /* jni_CallStaticFloatMethodA */ NULL,
    jni_CallStaticDoubleMethod,
    jni_CallStaticDoubleMethodV,
    /* jni_CallStaticDoubleMethodA */ NULL,
    jni_CallStaticVoidMethod,
    jni_CallStaticVoidMethodV,
    /* jni_CallStaticVoidMethodA */ NULL,

    /* jni_GetStaticFieldID */ NULL,

    /* jni_GetStaticObjectField */ NULL,
    /* jni_GetStaticBooleanField */ NULL,
    /* jni_GetStaticByteField */ NULL,
    /* jni_GetStaticCharField */ NULL,
    /* jni_GetStaticShortField */ NULL,
    /* jni_GetStaticIntField */ NULL,
    /* jni_GetStaticLongField */ NULL,
    /* jni_GetStaticFloatField */ NULL,
    /* jni_GetStaticDoubleField */ NULL,

    /* jni_SetStaticObjectField */ NULL,
    /* jni_SetStaticBooleanField */ NULL,
    /* jni_SetStaticByteField */ NULL,
    /* jni_SetStaticCharField */ NULL,
    /* jni_SetStaticShortField */ NULL,
    /* jni_SetStaticIntField */ NULL,
    /* jni_SetStaticLongField */ NULL,
    /* jni_SetStaticFloatField */ NULL,
    /* jni_SetStaticDoubleField */ NULL,

    /* jni_NewString */ NULL,
    /* jni_GetStringLength */ NULL,
    /* jni_GetStringChars */ NULL,
    /* jni_ReleaseStringChars */ NULL,

    /* jni_NewStringUTF */ NULL,
    /* jni_GetStringUTFLength */ NULL,
    /* jni_GetStringUTFChars */ NULL,
    /* jni_ReleaseStringUTFChars */ NULL,

    /* jni_GetArrayLength */ NULL,

    /* jni_NewObjectArray */ NULL,
    /* jni_GetObjectArrayElement */ NULL,
    /* jni_SetObjectArrayElement */ NULL,

    /* jni_NewBooleanArray */ NULL,
    /* jni_NewByteArray */ NULL,
    /* jni_NewCharArray */ NULL,
    /* jni_NewShortArray */ NULL,
    /* jni_NewIntArray */ NULL,
    /* jni_NewLongArray */ NULL,
    /* jni_NewFloatArray */ NULL,
    /* jni_NewDoubleArray */ NULL,

    /* jni_GetBooleanArrayElements */ NULL,
    /* jni_GetByteArrayElements */ NULL,
    /* jni_GetCharArrayElements */ NULL,
    /* jni_GetShortArrayElements */ NULL,
    /* jni_GetIntArrayElements */ NULL,
    /* jni_GetLongArrayElements */ NULL,
    /* jni_GetFloatArrayElements */ NULL,
    /* jni_GetDoubleArrayElements */ NULL,

    /* jni_ReleaseBooleanArrayElements */ NULL,
    /* jni_ReleaseByteArrayElements */ NULL,
    /* jni_ReleaseCharArrayElements */ NULL,
    /* jni_ReleaseShortArrayElements */ NULL,
    /* jni_ReleaseIntArrayElements */ NULL,
    /* jni_ReleaseLongArrayElements */ NULL,
    /* jni_ReleaseFloatArrayElements */ NULL,
    /* jni_ReleaseDoubleArrayElements */ NULL,

    /* jni_GetBooleanArrayRegion */ NULL,
    /* jni_GetByteArrayRegion */ NULL,
    /* jni_GetCharArrayRegion */ NULL,
    /* jni_GetShortArrayRegion */ NULL,
    /* jni_GetIntArrayRegion */ NULL,
    /* jni_GetLongArrayRegion */ NULL,
    /* jni_GetFloatArrayRegion */ NULL,
    /* jni_GetDoubleArrayRegion */ NULL,

    /* jni_SetBooleanArrayRegion */ NULL,
    /* jni_SetByteArrayRegion */ NULL,
    /* jni_SetCharArrayRegion */ NULL,
    /* jni_SetShortArrayRegion */ NULL,
    /* jni_SetIntArrayRegion */ NULL,
    /* jni_SetLongArrayRegion */ NULL,
    /* jni_SetFloatArrayRegion */ NULL,
    /* jni_SetDoubleArrayRegion */ NULL,

    /* jni_RegisterNatives */ NULL,
    /* jni_UnregisterNatives */ NULL,

    /* jni_MonitorEnter */ NULL,
    /* jni_MonitorExit */ NULL,

    jni_GetJavaVM,

    /* jni_GetStringRegion */ NULL,
    /* jni_GetStringUTFRegion */ NULL,

    /* jni_GetPrimitiveArrayCritical */ NULL,
    /* jni_ReleasePrimitiveArrayCritical */ NULL,

    /* jni_GetStringCritical */ NULL,
    /* jni_ReleaseStringCritical */ NULL,

    /* jni_NewWeakGlobalRef */ NULL,
    /* jni_DeleteWeakGlobalRef */ NULL,

    /* jni_ExceptionCheck */ NULL,

    /* jni_NewDirectByteBuffer */ NULL,
    /* jni_GetDirectBufferAddress */ NULL,
    /* jni_GetDirectBufferCapacity */ NULL,

    // New 1_6 features

    /* jni_GetObjectRefType */ NULL,
    },

    // Maxine specific

    /* jni_GetNumberOfArguments */ NULL,
    /* jni_GetNumberOfArguments */ NULL
};

#define JVMTI_VERSION_MASK 0x30000000
extern void* getJVMTIImpl(int version);

jint JNICALL jni_GetEnv(JavaVM *javaVM, void **penv, jint version) {
    TLA tla = tla_current();
    if (tla == 0) {
        *penv = NULL;
        return JNI_EDETACHED;
    }
    if (version && JVMTI_VERSION_MASK) {
        *penv = getJVMTIImpl(version);
        return *penv == NULL ? JNI_EVERSION : JNI_OK;
    }
    JNIEnv *env = (JNIEnv *) tla_addressOf(tla, JNI_ENV);
    c_ASSERT(env != NULL);
    *penv = (void *) env;
    /* TODO: check that requested JNI version is supported */
    return JNI_OK;
}

jint JNICALL JNI_CreateJavaVM(JavaVM **vm, void **penv, void *args) {
    return c_UNIMPLEMENTED();
}

jint JNICALL jni_DestroyJavaVM(JavaVM *vm) {
    return c_UNIMPLEMENTED();
}

/* TODO: Currently, the last argument (args) is ignored. */
jint JNICALL jni_AttachCurrentThread(JavaVM *vm, void **penv, void *args) {
    return thread_attachCurrent(penv, (JavaVMAttachArgs*) args, false);
}

jint JNICALL jni_AttachCurrentThreadAsDaemon(JavaVM *vm, void **penv, void *args) {
    return thread_attachCurrent(penv, (JavaVMAttachArgs*) args, true);
}

jint JNICALL jni_DetachCurrentThread(JavaVM *vm) {
    return thread_detachCurrent();
}

jint JNICALL JNI_GetDefaultJavaVMInitArgs(void *args_) {
    return c_UNIMPLEMENTED();
}

const struct JNIInvokeInterface_ jni_InvokeInterface = {
    (void *) &jni_ExtendedNativeInterface,
    NULL,
    NULL,

    jni_DestroyJavaVM,
    jni_AttachCurrentThread,
    jni_DetachCurrentThread,
    jni_GetEnv,
    jni_AttachCurrentThreadAsDaemon
};

struct JavaVM_ main_vm = {&jni_InvokeInterface};

JNIEXPORT jint JNICALL JNI_GetCreatedJavaVMs_Impl(JavaVM **vm, jsize vmBufLen, jsize *nVMs) {
    if (vmBufLen <= 0) {
        return JNI_EINVAL;
    }
    *vm = (JavaVM *) (&main_vm);
    *nVMs = 1;
    return JNI_OK;
}

JNIEXPORT jint JNICALL JNI_GetCreatedJavaVMs(JavaVM **vm, jsize vmBufLen, jsize *nVMs) {
    return JNI_GetCreatedJavaVMs_Impl(vm, vmBufLen, nVMs);
}

