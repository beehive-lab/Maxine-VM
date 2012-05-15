/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <jvmti.h>
#include <jni.h>
#include "vm.h"
#include "mutex.h"
#include "condition.h"

// (cwi): Java 7 added a new JVMTI function, so it is necessary to distinguish between Java 6 and Java 7.
// This is the only #define that I found in the header files that allows this distinction.
#if os_LINUX || os_SOLARIS
#include <classfile_constants.h>
#endif

#if JVM_CLASSFILE_MAJOR_VERSION >= 51
#define JAVA_7
#endif

extern struct JavaVM_ main_vm;

typedef jint (JNICALL *Agent_OnLoad_t)(JavaVM *, char *, void *reserved);
typedef jint (JNICALL *Agent_OnAttach_t)(JavaVM *, char *);
typedef jint (JNICALL *Agent_OnUnLoad_t)(JavaVM *);
typedef void (JNICALL *GarbageCollectionCallback) (jvmtiEnv *jvmti_env);
typedef void (JNICALL *jvmtiStartFunctionNoArg) (jvmtiEnv* jvmti_env, JNIEnv* jni_env);
typedef void (JNICALL *ThreadObjectCall) (jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jclass klass);


JNIEXPORT jint JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeAgentOnLoad(JNIEnv *env, jclass c, Agent_OnLoad_t Agent_OnLoad, char *options) {
    return (*Agent_OnLoad)((JavaVM *) &main_vm, options, NULL);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeAgentOnUnLoad(JNIEnv *env, jclass c, Agent_OnUnLoad_t Agent_OnUnLoad) {
    return (*Agent_OnUnLoad)((JavaVM *) &main_vm);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeStartFunction(JNIEnv *env, jclass c, jvmtiStartFunction callback, jvmtiEnv *jvmti_env, void *arg) {
    (*callback)(jvmti_env, env, arg);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeStartFunctionNoArg(JNIEnv *env, jclass c, jvmtiStartFunctionNoArg callback, jvmtiEnv *jvmti_env) {
    (*callback)(jvmti_env, env);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeGarbageCollectionCallback(JNIEnv *env, jclass c, GarbageCollectionCallback callback, jvmtiEnv *jvmti_env) {
    (*callback)(jvmti_env);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeThreadObjectCallback(JNIEnv *env, jclass c, ThreadObjectCall callback, jvmtiEnv *jvmti_env, jthread thread, jobject object) {
    (*callback)(jvmti_env, env, thread, object);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeClassfileLoadHookCallback(JNIEnv *env, jclass c, jvmtiEventClassFileLoadHook callback, jvmtiEnv *jvmti_env,
                jclass klass, jobject loader, char *name, jobject protection_domain,
                jint class_data_len,
                const unsigned char* class_data,
                jint* new_class_data_len,
                unsigned char** new_class_data) {
    (*callback)(jvmti_env, env, klass, loader, name, protection_domain, class_data_len, class_data, new_class_data_len, new_class_data);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeFieldWatchCallback(JNIEnv *env, jclass c, void *callback,
                jvmtiEnv *jvmti_env,
                jthread thread, jmethodID method, jlocation location, jclass field_class,
                jobject object, jfieldID field,
                char signature_type,
                jvalue new_value) {
    if (signature_type == 0) {
        jvmtiEventFieldAccess a_callback = (jvmtiEventFieldAccess) callback;
        (*a_callback)(jvmti_env, env, thread, method, location, field_class, object, field);
    } else {
        jvmtiEventFieldModification m_callback = (jvmtiEventFieldModification) callback;
        (*m_callback)(jvmti_env, env, thread, method, location, field_class, object, field, signature_type, new_value);
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeHeapIterationCallback(JNIEnv *env, jclass c, jvmtiHeapIterationCallback callback,
                long class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
    return (*callback)(class_tag, size, tag_ptr, length, user_data);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeBreakpointCallback(JNIEnv *env, jclass c, jvmtiEventBreakpoint callback,
                jvmtiEnv *jvmti_env, jthread thread,
                jmethodID method, jint location) {
    (*callback)(jvmti_env, env, thread, method, location);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeFramePopCallback(JNIEnv *env, jclass c, jvmtiEventFramePop callback,
                jvmtiEnv *jvmti_env, jthread thread,
                jmethodID method, jboolean wasPoppedByException) {
    (*callback)(jvmti_env, env, thread, method, wasPoppedByException);
}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeExceptionCallback(JNIEnv *env, jclass c, void *callbackX,
                jvmtiEnv *jvmti_env, jboolean is_catch, jthread thread,
                jmethodID method, jint location,
                jobject throwable,
                jmethodID catch_method, jint catch_location) {
    if (is_catch) {
        jvmtiEventExceptionCatch callback = (jvmtiEventExceptionCatch) callbackX;
        (*callback)(jvmti_env, env, thread, catch_method, catch_location, throwable);
    } else {
        jvmtiEventException callback =  (jvmtiEventException) callbackX;
        (*callback)(jvmti_env, env, thread, method, location, throwable, catch_method, catch_location);
    }
}

void setJVMTIThreadInfo(jvmtiThreadInfo *threadInfo, char *name, jint priority, jboolean is_daemon,
                jobject thread_group, jobject context_class_loader) {
    threadInfo->name = name;
    threadInfo->priority = priority;
    threadInfo->is_daemon = is_daemon;
    threadInfo->thread_group = thread_group;
    threadInfo->context_class_loader = context_class_loader;
}

int getJVMTILineNumberEntrySize() {
    return sizeof(jvmtiLineNumberEntry);
}

void setJVMTILineNumberEntry(jvmtiLineNumberEntry *table, jint index, jlocation location, jint line_number) {
    table[index].start_location = location;
    table[index].line_number = line_number;
}

int getJVMTILocalVariableEntrySize() {
    return sizeof(jvmtiLocalVariableEntry);
}

void setJVMTILocalVariableEntry(jvmtiLocalVariableEntry *table, jint index, char * name, char * signature,
                char *generic_signature, jlocation location, jint length, jint slot) {
    table[index].name = name;
    table[index].signature = signature;
    table[index].generic_signature = generic_signature;
    table[index].start_location = location;
    table[index].length = length;
    table[index].slot = slot;
}

int getJVMTIStackInfoSize() {
    return sizeof(jvmtiStackInfo);
}

void setJVMTIStackInfo(jvmtiStackInfo *stackInfo, jint index, jthread thread, jint state, jvmtiFrameInfo *frame_buffer, jint frame_count) {
    stackInfo[index].thread = thread;
    stackInfo[index].state = state;
    stackInfo[index].frame_buffer = frame_buffer;
    stackInfo[index].frame_count = frame_count;
}

void setJVMTIFrameInfo(jvmtiFrameInfo *frameInfo, jint index, jmethodID methodID, jlocation location) {
    frameInfo[index].method = methodID;
    frameInfo[index].location = location;
}

void setThreadGroupInfo(jvmtiThreadGroupInfo *info, jobject parent, char *name, jint max_priority, jboolean is_daemon) {
    info->parent = parent;
    info->name = name;
    info->max_priority = max_priority;
    info->is_daemon = is_daemon;
}

static void jvmti_reserved() {
}

struct jvmtiInterface_1_ jvmti_interface = {
    (void *) jvmti_reserved,
    /* jvmti_SetEventNotificationMode */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_GetAllThreads */ NULL,
    /* jvmti_SuspendThread */ NULL,
    /* jvmti_ResumeThread */ NULL,
    /* jvmti_StopThread */ NULL,
    /* jvmti_InterruptThread */ NULL,
    /* jvmti_GetThreadInfo */ NULL,
    /* jvmti_GetOwnedMonitorInfo */ NULL,
    /* jvmti_GetCurrentContendedMonitor */ NULL,
    /* jvmti_RunAgentThread */ NULL,
    /* jvmti_GetTopThreadGroups */ NULL,
    /* jvmti_GetThreadGroupInfo */ NULL,
    /* jvmti_GetThreadGroupChildren */ NULL,
    /* jvmti_GetFrameCount */ NULL,
    /* jvmti_GetThreadState */ NULL,
    /* jvmti_GetCurrentThread */ NULL,
    /* jvmti_GetFrameLocation */ NULL,
    /* jvmti_NotifyFramePop */ NULL,
    /* jvmti_GetLocalObject */ NULL,
    /* jvmti_GetLocalInt */ NULL,
    /* jvmti_GetLocalLong */ NULL,
    /* jvmti_GetLocalFloat */ NULL,
    /* jvmti_GetLocalDouble */ NULL,
    /* jvmti_SetLocalObject */ NULL,
    /* jvmti_SetLocalInt */ NULL,
    /* jvmti_SetLocalLong */ NULL,
    /* jvmti_SetLocalFloat */ NULL,
    /* jvmti_SetLocalDouble */ NULL,
    /* jvmti_CreateRawMonitor */ NULL,
    /* jvmti_DestroyRawMonitor */ NULL,
    /* jvmti_RawMonitorEnter */ NULL,
    /* jvmti_RawMonitorExit */ NULL,
    /* jvmti_RawMonitorWait */ NULL,
    /* jvmti_RawMonitorNotify */ NULL,
    /* jvmti_RawMonitorNotifyAll */ NULL,
    /* jvmti_SetBreakpoint */ NULL,
    /* jvmti_ClearBreakpoint */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_SetFieldAccessWatch */ NULL,
    /* jvmti_ClearFieldAccessWatch */ NULL,
    /* jvmti_SetFieldModificationWatch */ NULL,
    /* jvmti_ClearFieldModificationWatch */ NULL,
    /* jvmti_IsModifiableClass */ NULL,
    /* jvmti_Allocate */ NULL,
    /* jvmti_Deallocate */ NULL,
    /* jvmti_GetClassSignature */ NULL,
    /* jvmti_GetClassStatus */ NULL,
    /* jvmti_GetSourceFileName */ NULL,
    /* jvmti_GetClassModifiers */ NULL,
    /* jvmti_GetClassMethods */ NULL,
    /* jvmti_GetClassFields */ NULL,
    /* jvmti_GetImplementedInterfaces */ NULL,
    /* jvmti_IsInterface */ NULL,
    /* jvmti_IsArrayClass */ NULL,
    /* jvmti_GetClassLoader */ NULL,
    /* jvmti_GetObjectHashCode */ NULL,
    /* jvmti_GetObjectMonitorUsage */ NULL,
    /* jvmti_GetFieldName */ NULL,
    /* jvmti_GetFieldDeclaringClass */ NULL,
    /* jvmti_GetFieldModifiers */ NULL,
    /* jvmti_IsFieldSynthetic */ NULL,
    /* jvmti_GetMethodName */ NULL,
    /* jvmti_GetMethodDeclaringClass */ NULL,
    /* jvmti_GetMethodModifiers */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_GetMaxLocals */ NULL,
    /* jvmti_GetArgumentsSize */ NULL,
    /* jvmti_GetLineNumberTable */ NULL,
    /* jvmti_GetMethodLocation */ NULL,
    /* jvmti_GetLocalVariableTable */ NULL,
    /* jvmti_SetNativeMethodPrefix */ NULL,
    /* jvmti_SetNativeMethodPrefixes */ NULL,
    /* jvmti_GetBytecodes */ NULL,
    /* jvmti_IsMethodNative */ NULL,
    /* jvmti_IsMethodSynthetic */ NULL,
    /* jvmti_GetLoadedClasses */ NULL,
    /* jvmti_GetClassLoaderClasses */ NULL,
    /* jvmti_PopFrame */ NULL,
    /* jvmti_ForceEarlyReturnObject */ NULL,
    /* jvmti_ForceEarlyReturnInt */ NULL,
    /* jvmti_ForceEarlyReturnLong */ NULL,
    /* jvmti_ForceEarlyReturnFloat */ NULL,
    /* jvmti_ForceEarlyReturnDouble */ NULL,
    /* jvmti_ForceEarlyReturnVoid */ NULL,
    /* jvmti_RedefineClasses */ NULL,
    /* jvmti_GetVersionNumber */ NULL,
    /* jvmti_GetCapabilities */ NULL,
    /* jvmti_GetSourceDebugExtension */ NULL,
    /* jvmti_IsMethodObsolete */ NULL,
    /* jvmti_SuspendThreadList */ NULL,
    /* jvmti_ResumeThreadList */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* jvmti_GetAllStackTraces */ NULL,
    /* jvmti_GetThreadListStackTraces */ NULL,
    /* jvmti_GetThreadLocalStorage */ NULL,
    /* jvmti_SetThreadLocalStorage */ NULL,
    /* jvmti_GetStackTrace */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_GetTag */ NULL,
    /* jvmti_SetTag */ NULL,
    /* jvmti_ForceGarbageCollection */ NULL,
    /* jvmti_IterateOverObjectsReachableFromObject */ NULL,
    /* jvmti_IterateOverReachableObjects */ NULL,
    /* jvmti_IterateOverHeap */ NULL,
    /* jvmti_IterateOverInstancesOfClass */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_GetObjectsWithTags */ NULL,
    /* jvmti_FollowReferences */ NULL,
    /* jvmti_IterateThroughHeap */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* jvmti_SetJNIFunctionTable */ NULL,
    /* jvmti_GetJNIFunctionTable */ NULL,
    /* jvmti_SetEventCallbacks */ NULL,
    /* jvmti_GenerateEvents */ NULL,
    /* jvmti_GetExtensionFunctions */ NULL,
    /* jvmti_GetExtensionEvents */ NULL,
    /* jvmti_SetExtensionEventCallback */ NULL,
    /* jvmti_DisposeEnvironment */ NULL,
    /* jvmti_GetErrorName */ NULL,
    /* jvmti_GetJLocationFormat */ NULL,
    /* jvmti_GetSystemProperties */ NULL,
    /* jvmti_GetSystemProperty */ NULL,
    /* jvmti_SetSystemProperty */ NULL,
    /* jvmti_GetPhase */ NULL,
    /* jvmti_GetCurrentThreadCpuTimerInfo */ NULL,
    /* jvmti_GetCurrentThreadCpuTime */ NULL,
    /* jvmti_GetThreadCpuTimerInfo */ NULL,
    /* jvmti_GetThreadCpuTime */ NULL,
    /* jvmti_GetTimerInfo */ NULL,
    /* jvmti_GetTime */ NULL,
    /* jvmti_GetPotentialCapabilities */ NULL,
    (void *) jvmti_reserved,
    /* jvmti_AddCapabilities */ NULL,
    /* jvmti_RelinquishCapabilities */ NULL,
    /* jvmti_GetAvailableProcessors */ NULL,
    /* jvmti_GetClassVersionNumbers */ NULL,
    /* jvmti_GetConstantPool */ NULL,
    /* jvmti_GetEnvironmentLocalStorage */ NULL,
    /* jvmti_SetEnvironmentLocalStorage */ NULL,
    /* jvmti_AddToBootstrapClassLoaderSearch */ NULL,
    /* jvmti_SetVerboseFlag */ NULL,
    /* jvmti_AddToSystemClassLoaderSearch */ NULL,
    /* jvmti_RetransformClasses */ NULL,
    /* jvmti_GetOwnedMonitorStackDepthInfo */ NULL,
    /* jvmti_GetObjectSize */ NULL,
#ifdef JAVA_7
    /* jvmti_GetLocalInstance */ NULL,
#endif
};


typedef struct {
    const struct jvmtiInterface_1_ *functions;
    jvmtiEventCallbacks *callbacks;
    jvmtiCapabilities *capabilities;
} JVMTIEnvImplStruct, *JVMTIEnvImpl;

void *getJVMTIInterface(int version) {
    if (version == -1 || version == JVMTI_VERSION) {
        return (void*) &jvmti_interface;
    }
    return NULL;
}

void *getJVMTIImpl(JNIEnv *env, int version) {
    JVMTIEnvImplStruct *jvmtienv_impl = malloc(sizeof(JVMTIEnvImplStruct));
    if (jvmtienv_impl == NULL) return NULL;
    jvmtienv_impl->functions = &jvmti_interface;
    jvmtienv_impl->callbacks = malloc(sizeof(jvmtiEventCallbacks));
    if (jvmtienv_impl->callbacks == NULL) return NULL;
    jvmtienv_impl->capabilities = malloc(sizeof(jvmtiCapabilities));
    if (jvmtienv_impl->capabilities == NULL) return NULL;
    JVMTIEnvImpl jvmti = (JVMTIEnvImpl) jvmtienv_impl;
    getVMInterface()->SetJVMTIEnv(env, jvmti);
    return (void *)jvmti;
}

