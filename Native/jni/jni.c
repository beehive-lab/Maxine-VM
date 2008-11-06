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
 * This file implements JNI functions that take a variable number of arguments. These
 * functions are essentially wrappers that copy the varargs into a stack allocated
 * jvalue array and then call the version of the same JNI function that takes its
 * arguments in such an array. This isolates the implementation of such functions
 * from the platform/compiler dependent way in which varargs are implemented.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
#include <alloca.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

#include "debug.h"
#include "kind.h"
#include "word.h"

#include "jni.h"

typedef struct {
    struct JNINativeInterface_ jniNativeInterface;

    jint (JNICALL *GetNumberOfArguments)(JNIEnv *env, jmethodID methodID);
    void (JNICALL *GetKindsOfArguments)(JNIEnv *env, jmethodID methodID, char *kinds);
} ExtendedJniNativeInterface, *ExtendedJniEnv;

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
	      fprintf(stderr, "callObjectMethodV: unknown kind = %d\n", kinds[i]);
                exit(1);
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

static jobject CallObjectMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallObjectMethodV, jobject);
}

static jboolean CallBooleanMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallBooleanMethodV, jboolean);
}

static jbyte CallByteMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallByteMethodV, jbyte);
}

static jchar CallCharMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallCharMethodV, jchar);
}

static jshort CallShortMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallShortMethodV, jshort);
}

static jint CallIntMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallIntMethodV, jint);
}

static jlong CallLongMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallLongMethodV, jlong);
}

static jfloat CallFloatMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallFloatMethodV, jfloat);
}

static jdouble CallDoubleMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
    CALL_METHOD(CallDoubleMethodV, jdouble);
}

static void CallVoidMethod(JNIEnv *env, jobject object, jmethodID methodID, ...) {
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

static jobject CallNonvirtualObjectMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualObjectMethodV, jobject);
}

static jboolean CallNonvirtualBooleanMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualBooleanMethodV, jboolean);
}

static jbyte CallNonvirtualByteMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualByteMethodV, jbyte);
}

static jchar CallNonvirtualCharMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualCharMethodV, jchar);
}

static jshort CallNonvirtualShortMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualShortMethodV, jshort);
}

static jint CallNonvirtualIntMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualIntMethodV, jint);
}

static jlong CallNonvirtualLongMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualLongMethodV, jlong);
}

static jfloat CallNonvirtualFloatMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualFloatMethodV, jfloat);
}

static jdouble CallNonvirtualDoubleMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
    CALL_NONVIRTUAL_METHOD(CallNonvirtualDoubleMethodV, jdouble);
}

static void CallNonvirtualVoidMethod(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, ...) {
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

static jobject CallStaticObjectMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticObjectMethodV, jobject);
}

static jboolean CallStaticBooleanMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticBooleanMethodV, jboolean);
}

static jbyte CallStaticByteMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticByteMethodV, jbyte);
}

static jchar CallStaticCharMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticCharMethodV, jchar);
}

static jshort CallStaticShortMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticShortMethodV, jshort);
}

static jint CallStaticIntMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticIntMethodV, jint);
}

static jlong CallStaticLongMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticLongMethodV, jlong);
}

static jfloat CallStaticFloatMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticFloatMethodV, jfloat);
}

static jdouble CallStaticDoubleMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(CallStaticDoubleMethodV, jdouble);
}

static void CallStaticVoidMethod(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
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

static jobject CallObjectMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallObjectMethodA);
}

static jboolean CallBooleanMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallBooleanMethodA);
}

static jbyte CallByteMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallByteMethodA);
}

static jchar CallCharMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallCharMethodA);
}

static jshort CallShortMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallShortMethodA);
}

static jint CallIntMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallIntMethodA);
}

static jlong CallLongMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallLongMethodA);
}

static jfloat CallFloatMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallFloatMethodA);
}

static jdouble CallDoubleMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
	CALL_METHOD_V(CallDoubleMethodA);
}

static void CallVoidMethodV(JNIEnv *env, jobject object, jmethodID methodID, va_list argumentList) {
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

static jobject CallNonvirtualObjectMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualObjectMethodA);
}

static jboolean CallNonvirtualBooleanMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualBooleanMethodA);
}

static jbyte CallNonvirtualByteMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualByteMethodA);
}

static jchar CallNonvirtualCharMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualCharMethodA);
}

static jshort CallNonvirtualShortMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualShortMethodA);
}

static jint CallNonvirtualIntMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualIntMethodA);
}

static jlong CallNonvirtualLongMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualLongMethodA);
}

static jfloat CallNonvirtualFloatMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualFloatMethodA);
}

static jdouble CallNonvirtualDoubleMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_NONVIRTUAL_METHOD_V(CallNonvirtualDoubleMethodA);
}

static void CallNonvirtualVoidMethodV(JNIEnv *env, jobject object, jclass javaClass, jmethodID methodID, va_list argumentList) {
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

static jobject CallStaticObjectMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticObjectMethodA);
}

static jboolean CallStaticBooleanMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticBooleanMethodA);
}

static jbyte CallStaticByteMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticByteMethodA);
}

static jchar CallStaticCharMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticCharMethodA);
}

static jshort CallStaticShortMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticShortMethodA);
}

static jint CallStaticIntMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticIntMethodA);
}

static jlong CallStaticLongMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticLongMethodA);
}

static jfloat CallStaticFloatMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticFloatMethodA);
}

static jdouble CallStaticDoubleMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
	CALL_STATIC_METHOD_V(CallStaticDoubleMethodA);
}

static void CallStaticVoidMethodV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
    CALL_VOID_STATIC_METHOD_V(CallStaticVoidMethodA);
}

/*
 * NewObject Routines
 */
static jobject NewObject(JNIEnv *env, jclass javaClass, jmethodID methodID, ...) {
    CALL_STATIC_METHOD(NewObjectV, jobject);
}

static jobject NewObjectV(JNIEnv *env, jclass javaClass, jmethodID methodID, va_list argumentList) {
    CALL_STATIC_METHOD_V(NewObjectA);
}


#define ASSIGN_FUNCTION(jniFunctionName) \
    do { *((void **) &(env->jniFunctionName)) = (void*)(jniFunctionName); } while (0)

/**
 * Patches the JNI functions array for certain JNI functions that are implemented in C for
 * portability reasons (i.e. handling of varargs).
 */
void nativeInitializeJniInterface(JNIEnv env) {
#if debug_LOADER
	debug_println("BEGIN jni nativeInitialize");
#endif
	debug_ASSERT((jint) -1 < 0);
    debug_ASSERT(sizeof(jint) == 4);

    debug_ASSERT((jlong) -1 < 0);
    debug_ASSERT(sizeof(jlong) == 8);

    ASSIGN_FUNCTION(CallObjectMethod);
    ASSIGN_FUNCTION(CallBooleanMethod);
    ASSIGN_FUNCTION(CallByteMethod);
    ASSIGN_FUNCTION(CallCharMethod);
    ASSIGN_FUNCTION(CallShortMethod);
    ASSIGN_FUNCTION(CallIntMethod);
    ASSIGN_FUNCTION(CallLongMethod);
    ASSIGN_FUNCTION(CallFloatMethod);
    ASSIGN_FUNCTION(CallDoubleMethod);
    ASSIGN_FUNCTION(CallVoidMethod);

    ASSIGN_FUNCTION(CallNonvirtualObjectMethod);
    ASSIGN_FUNCTION(CallNonvirtualBooleanMethod);
    ASSIGN_FUNCTION(CallNonvirtualByteMethod);
    ASSIGN_FUNCTION(CallNonvirtualCharMethod);
    ASSIGN_FUNCTION(CallNonvirtualShortMethod);
    ASSIGN_FUNCTION(CallNonvirtualIntMethod);
    ASSIGN_FUNCTION(CallNonvirtualLongMethod);
    ASSIGN_FUNCTION(CallNonvirtualFloatMethod);
    ASSIGN_FUNCTION(CallNonvirtualDoubleMethod);
    ASSIGN_FUNCTION(CallNonvirtualVoidMethod);

    ASSIGN_FUNCTION(CallStaticObjectMethod);
    ASSIGN_FUNCTION(CallStaticBooleanMethod);
    ASSIGN_FUNCTION(CallStaticByteMethod);
    ASSIGN_FUNCTION(CallStaticCharMethod);
    ASSIGN_FUNCTION(CallStaticShortMethod);
    ASSIGN_FUNCTION(CallStaticIntMethod);
    ASSIGN_FUNCTION(CallStaticLongMethod);
    ASSIGN_FUNCTION(CallStaticFloatMethod);
    ASSIGN_FUNCTION(CallStaticDoubleMethod);
    ASSIGN_FUNCTION(CallStaticVoidMethod);

    ASSIGN_FUNCTION(CallObjectMethodV);
    ASSIGN_FUNCTION(CallBooleanMethodV);
    ASSIGN_FUNCTION(CallByteMethodV);
    ASSIGN_FUNCTION(CallCharMethodV);
    ASSIGN_FUNCTION(CallShortMethodV);
    ASSIGN_FUNCTION(CallIntMethodV);
    ASSIGN_FUNCTION(CallLongMethodV);
    ASSIGN_FUNCTION(CallFloatMethodV);
    ASSIGN_FUNCTION(CallDoubleMethodV);
    ASSIGN_FUNCTION(CallVoidMethodV);

    ASSIGN_FUNCTION(CallNonvirtualObjectMethodV);
    ASSIGN_FUNCTION(CallNonvirtualBooleanMethodV);
    ASSIGN_FUNCTION(CallNonvirtualByteMethodV);
    ASSIGN_FUNCTION(CallNonvirtualCharMethodV);
    ASSIGN_FUNCTION(CallNonvirtualShortMethodV);
    ASSIGN_FUNCTION(CallNonvirtualIntMethodV);
    ASSIGN_FUNCTION(CallNonvirtualLongMethodV);
    ASSIGN_FUNCTION(CallNonvirtualFloatMethodV);
    ASSIGN_FUNCTION(CallNonvirtualDoubleMethodV);
    ASSIGN_FUNCTION(CallNonvirtualVoidMethodV);

    ASSIGN_FUNCTION(CallStaticObjectMethodV);
    ASSIGN_FUNCTION(CallStaticBooleanMethodV);
    ASSIGN_FUNCTION(CallStaticByteMethodV);
    ASSIGN_FUNCTION(CallStaticCharMethodV);
    ASSIGN_FUNCTION(CallStaticShortMethodV);
    ASSIGN_FUNCTION(CallStaticIntMethodV);
    ASSIGN_FUNCTION(CallStaticLongMethodV);
    ASSIGN_FUNCTION(CallStaticFloatMethodV);
    ASSIGN_FUNCTION(CallStaticDoubleMethodV);
    ASSIGN_FUNCTION(CallStaticVoidMethodV);

    ASSIGN_FUNCTION(NewObject);
    ASSIGN_FUNCTION(NewObjectV);
#if debug_LOADER
	debug_println("END jni nativeInitialize");
#endif
}
