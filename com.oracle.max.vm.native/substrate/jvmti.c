/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <jvmti.h>
#include <jni.h>
#include "mutex.h"
#include "condition.h"

extern struct JavaVM_ main_vm;

typedef jint (JNICALL *Agent_OnLoad_t)(JavaVM *, char *, void *reserved);
typedef jint (JNICALL *Agent_OnAttach_t)(JavaVM *, char *);
typedef jint (JNICALL *Agent_OnUnLoad_t)(JavaVM *);
typedef void (JNICALL *GarbageCollectionCallback) (jvmtiEnv *jvmti_env);
typedef void (JNICALL *jvmtiStartFunctionNoArg) (jvmtiEnv* jvmti_env, JNIEnv* jni_env);


JNIEXPORT jint JNICALL
Java_com_sun_max_vm_jvmti_JvmtiCallbacks_invokeAgentOnLoad(JNIEnv *env, jclass c, Agent_OnLoad_t Agent_OnLoad, char *options) {
    return (*Agent_OnLoad)((JavaVM *) &main_vm, options, NULL);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_jvmti_JvmtiCallbacks_invokeAgentOnUnLoad(JNIEnv *env, jclass c, Agent_OnUnLoad_t Agent_OnUnLoad) {
    return (*Agent_OnUnLoad)((JavaVM *) &main_vm);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_jvmti_JvmtiCallbacks_invokeStartFunction(JNIEnv *env, jclass c, jvmtiStartFunction callback, jvmtiEnv *jvmti_env, void *arg) {
    (*callback)(jvmti_env, env, arg);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_jvmti_JvmtiCallbacks_invokeStartFunctionNoArg(JNIEnv *env, jclass c, jvmtiStartFunctionNoArg callback, jvmtiEnv *jvmti_env) {
    (*callback)(jvmti_env, env);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_jvmti_JvmtiCallbacks_invokeGarbageCollectionCallback(JNIEnv *env, jclass c, GarbageCollectionCallback callback, jvmtiEnv *jvmti_env) {
    (*callback)(jvmti_env);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_jvmti_JvmtiRawMonitor_nativeMutexLock(JNIEnv *env, jclass c, Mutex mutex) {
    return mutex_enter(mutex) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_jvmti_JvmtiRawMonitor_nativeConditionWait(JNIEnv *env, jclass c, Mutex mutex, Condition condition, jlong timeoutMilliSeconds) {
    return condition_timedWait(condition, mutex, timeoutMilliSeconds);
}

static void jvmti_reserved() {
}

typedef struct ExtendedJVMTINativeInterface_ {
    struct jvmtiInterface_1_ jvmtiNativeInterface;

    void (JNICALL *SetJVMTIEnv)(jvmtiEnv *env);
} *ExtendedJvmtiEnv;


struct /*jvmtiInterface_1_*/ ExtendedJVMTINativeInterface_ jvmti_extended_interface = {
    {
    (void *) jvmti_reserved,
    /* jvmti_*SetEventNotificationMode */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*GetAllThreads */ NULL,
    /* jvmti_*SuspendThread */ NULL,
    /* jvmti_*ResumeThread */ NULL,
    /* jvmti_*StopThread */ NULL,
    /* jvmti_*InterruptThread */ NULL,
    /* jvmti_*GetThreadInfo */ NULL,
    /* jvmti_*GetOwnedMonitorInfo */ NULL,
    /* jvmti_*GetCurrentContendedMonitor */ NULL,
    /* jvmti_*RunAgentThread */ NULL,
    /* jvmti_*GetTopThreadGroups */ NULL,
    /* jvmti_*GetThreadGroupInfo */ NULL,
    /* jvmti_*GetThreadGroupChildren */ NULL,
    /* jvmti_*GetFrameCount */ NULL,
    /* jvmti_*GetThreadState */ NULL,
    /* jvmti_*GetCurrentThread */ NULL,
    /* jvmti_*GetFrameLocation */ NULL,
    /* jvmti_*NotifyFramePop */ NULL,
    /* jvmti_*GetLocalObject */ NULL,
    /* jvmti_*GetLocalInt */ NULL,
    /* jvmti_*GetLocalLong */ NULL,
    /* jvmti_*GetLocalFloat */ NULL,
    /* jvmti_*GetLocalDouble */ NULL,
    /* jvmti_*SetLocalObject */ NULL,
    /* jvmti_*SetLocalInt */ NULL,
    /* jvmti_*SetLocalLong */ NULL,
    /* jvmti_*SetLocalFloat */ NULL,
    /* jvmti_*SetLocalDouble */ NULL,
    /* jvmti_*CreateRawMonitor */ NULL,
    /* jvmti_*DestroyRawMonitor */ NULL,
    /* jvmti_*RawMonitorEnter */ NULL,
    /* jvmti_*RawMonitorExit */ NULL,
    /* jvmti_*RawMonitorWait */ NULL,
    /* jvmti_*RawMonitorNotify */ NULL,
    /* jvmti_*RawMonitorNotifyAll */ NULL,
    /* jvmti_*SetBreakpoint */ NULL,
    /* jvmti_*ClearBreakpoint */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*SetFieldAccessWatch */ NULL,
    /* jvmti_*ClearFieldAccessWatch */ NULL,
    /* jvmti_*SetFieldModificationWatch */ NULL,
    /* jvmti_*ClearFieldModificationWatch */ NULL,
    /* jvmti_*IsModifiableClass */ NULL,
    /* jvmti_*Allocate */ NULL,
    /* jvmti_*Deallocate */ NULL,
    /* jvmti_*GetClassSignature */ NULL,
    /* jvmti_*GetClassStatus */ NULL,
    /* jvmti_*GetSourceFileName */ NULL,
    /* jvmti_*GetClassModifiers */ NULL,
    /* jvmti_*GetClassMethods */ NULL,
    /* jvmti_*GetClassFields */ NULL,
    /* jvmti_*GetImplementedInterfaces */ NULL,
    /* jvmti_*IsInterface */ NULL,
    /* jvmti_*IsArrayClass */ NULL,
    /* jvmti_*GetClassLoader */ NULL,
    /* jvmti_*GetObjectHashCode */ NULL,
    /* jvmti_*GetObjectMonitorUsage */ NULL,
    /* jvmti_*GetFieldName */ NULL,
    /* jvmti_*GetFieldDeclaringClass */ NULL,
    /* jvmti_*GetFieldModifiers */ NULL,
    /* jvmti_*IsFieldSynthetic */ NULL,
    /* jvmti_*GetMethodName */ NULL,
    /* jvmti_*GetMethodDeclaringClass */ NULL,
    /* jvmti_*GetMethodModifiers */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*GetMaxLocals */ NULL,
    /* jvmti_*GetArgumentsSize */ NULL,
    /* jvmti_*GetLineNumberTable */ NULL,
    /* jvmti_*GetMethodLocation */ NULL,
    /* jvmti_*GetLocalVariableTable */ NULL,
    /* jvmti_*SetNativeMethodPrefix */ NULL,
    /* jvmti_*SetNativeMethodPrefixes */ NULL,
    /* jvmti_*GetBytecodes */ NULL,
    /* jvmti_*IsMethodNative */ NULL,
    /* jvmti_*IsMethodSynthetic */ NULL,
    /* jvmti_*GetLoadedClasses */ NULL,
    /* jvmti_*GetClassLoaderClasses */ NULL,
    /* jvmti_*PopFrame */ NULL,
    /* jvmti_*ForceEarlyReturnObject */ NULL,
    /* jvmti_*ForceEarlyReturnInt */ NULL,
    /* jvmti_*ForceEarlyReturnLong */ NULL,
    /* jvmti_*ForceEarlyReturnFloat */ NULL,
    /* jvmti_*ForceEarlyReturnDouble */ NULL,
    /* jvmti_*ForceEarlyReturnVoid */ NULL,
    /* jvmti_*RedefineClasses */ NULL,
    /* jvmti_*GetVersionNumber */ NULL,
    /* jvmti_*GetCapabilities */ NULL,
    /* jvmti_*GetSourceDebugExtension */ NULL,
    /* jvmti_*IsMethodObsolete */ NULL,
    /* jvmti_*SuspendThreadList */ NULL,
    /* jvmti_*ResumeThreadList */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* jvmti_*GetAllStackTraces */ NULL,
    /* jvmti_*GetThreadListStackTraces */ NULL,
    /* jvmti_*GetThreadLocalStorage */ NULL,
    /* jvmti_*SetThreadLocalStorage */ NULL,
    /* jvmti_*GetStackTrace */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*GetTag */ NULL,
    /* jvmti_*SetTag */ NULL,
    /* jvmti_*ForceGarbageCollection */ NULL,
    /* jvmti_*IterateOverObjectsReachableFromObject */ NULL,
    /* jvmti_*IterateOverReachableObjects */ NULL,
    /* jvmti_*IterateOverHeap */ NULL,
    /* jvmti_*IterateOverInstancesOfClass */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*GetObjectsWithTags */ NULL,
    /* jvmti_*FollowReferences */ NULL,
    /* jvmti_*IterateThroughHeap */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* jvmti_*SetJNIFunctionTable */ NULL,
    /* jvmti_*GetJNIFunctionTable */ NULL,
    /* jvmti_*SetEventCallbacks */ NULL,
    /* jvmti_*GenerateEvents */ NULL,
    /* jvmti_*GetExtensionFunctions */ NULL,
    /* jvmti_*GetExtensionEvents */ NULL,
    /* jvmti_*SetExtensionEventCallback */ NULL,
    /* jvmti_*DisposeEnvironment */ NULL,
    /* jvmti_*GetErrorName */ NULL,
    /* jvmti_*GetJLocationFormat */ NULL,
    /* jvmti_*GetSystemProperties */ NULL,
    /* jvmti_*GetSystemProperty */ NULL,
    /* jvmti_*SetSystemProperty */ NULL,
    /* jvmti_*GetPhase */ NULL,
    /* jvmti_*GetCurrentThreadCpuTimerInfo */ NULL,
    /* jvmti_*GetCurrentThreadCpuTime */ NULL,
    /* jvmti_*GetThreadCpuTimerInfo */ NULL,
    /* jvmti_*GetThreadCpuTime */ NULL,
    /* jvmti_*GetTimerInfo */ NULL,
    /* jvmti_*GetTime */ NULL,
    /* jvmti_*GetPotentialCapabilities */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_*AddCapabilities */ NULL,
    /* jvmti_*RelinquishCapabilities */ NULL,
    /* jvmti_*GetAvailableProcessors */ NULL,
    /* jvmti_*GetClassVersionNumbers */ NULL,
    /* jvmti_*GetConstantPool */ NULL,
    /* jvmti_*GetEnvironmentLocalStorage */ NULL,
    /* jvmti_*SetEnvironmentLocalStorage */ NULL,
    /* jvmti_*AddToBootstrapClassLoaderSearch */ NULL,
    /* jvmti_*SetVerboseFlag */ NULL,
    /* jvmti_*AddToSystemClassLoaderSearch */ NULL,
    /* jvmti_*RetransformClasses */ NULL,
    /* jvmti_*GetOwnedMonitorStackDepthInfo */ NULL,
    /* jvmti_*GetObjectSize */ NULL,
    },

    // Maxine
    /* jvmti_SetJVMTIEnv */ NULL
};


typedef struct {
    const struct /*jvmtiInterface_1_*/ExtendedJVMTINativeInterface_ *functions;
    jvmtiEventCallbacks *callbacks;
    jvmtiCapabilities *capabilities;
    long eventMask;
} JVMTIEnvImplStruct, *JVMTIEnvImpl;

// TODO This all needs to be dynamically allocated per agent.

JVMTIEnvImplStruct jvmtienv_impl;
jvmtiEventCallbacks jvmtienv_impl_callbacks;
jvmtiCapabilities jvmtienv_impl_capabilities;

void *getJVMTIInterface(int version) {
    if (version == -1 || version == JVMTI_VERSION) {
        return (void*) &jvmti_extended_interface.jvmtiNativeInterface;
    }
    return NULL;
}

//extern JNIEnv *currentJniEnv();
/**
 * Gets the thread-local pointer to the pointer to the global JNI function table.
 */
#include <threadLocals.h>

JNIEnv *jvmtiCurrentJniEnv() {
    TLA tla = tla_current();
//    c_ASSERT(tla != 0);
    if (tla == 0) {
        return NULL;
    }
    JNIEnv *env = (JNIEnv *) tla_addressOf(tla, JNI_ENV);
    c_ASSERT(env != NULL);
    return env;
}

void *getJVMTIImpl(int version) {
    jvmtienv_impl.functions = &jvmti_extended_interface;
    jvmtienv_impl.callbacks = &jvmtienv_impl_callbacks;
    jvmtienv_impl.capabilities = &jvmtienv_impl_capabilities;
    jvmtienv_impl.eventMask = 0;
    ExtendedJvmtiEnv *jvmti = (ExtendedJvmtiEnv*) &jvmtienv_impl;
    (*jvmti)->SetJVMTIEnv((jvmtiEnv*) jvmti);
    return (void *)jvmti;
}

