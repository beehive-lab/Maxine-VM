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

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeCompiledMethodLoadCallback(JNIEnv *env, jclass c, jvmtiEventCompiledMethodLoad callback,
                jvmtiEnv *jvmti_env, jmethodID method, jint code_size, const void* code_addr, jint map_length,
                const jvmtiAddrLocationMap* map, const void* compile_info) {
    (*callback)(jvmti_env, method, code_size, code_addr, map_length, map, compile_info);

}

JNIEXPORT void JNICALL
Java_com_sun_max_vm_ext_jvmti_JVMTICallbacks_invokeCompiledMethodUnloadCallback(JNIEnv *env, jclass c, jvmtiEventCompiledMethodUnload callback,
                jvmtiEnv *jvmti_env, jmethodID method, const void* code_addr) {
    (*callback)(jvmti_env, method, code_addr);

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
    /* 2 : jvmti_SetEventNotificationMode */ NULL,
    (void *) jvmti_reserved,
    /* 4 : jvmti_GetAllThreads */ NULL,
    /* 5 : jvmti_SuspendThread */ NULL,
    /* 6 : jvmti_ResumeThread */ NULL,
    /* 7 : jvmti_StopThread */ NULL,
    /* 8 : jvmti_InterruptThread */ NULL,
    /* 9 : jvmti_GetThreadInfo */ NULL,
    /* 10 : jvmti_GetOwnedMonitorInfo */ NULL,
    /* 11 : jvmti_GetCurrentContendedMonitor */ NULL,
    /* 12 : jvmti_RunAgentThread */ NULL,
    /* 13 : jvmti_GetTopThreadGroups */ NULL,
    /* 14 : jvmti_GetThreadGroupInfo */ NULL,
    /* 15 : jvmti_GetThreadGroupChildren */ NULL,
    /* 16 : jvmti_GetFrameCount */ NULL,
    /* 17 : jvmti_GetThreadState */ NULL,
    /* 18 : jvmti_GetCurrentThread */ NULL,
    /* 19 : jvmti_GetFrameLocation */ NULL,
    /* 20 : jvmti_NotifyFramePop */ NULL,
    /* 21 : jvmti_GetLocalObject */ NULL,
    /* 22 : jvmti_GetLocalInt */ NULL,
    /* 23 : jvmti_GetLocalLong */ NULL,
    /* 24 : jvmti_GetLocalFloat */ NULL,
    /* 25 : jvmti_GetLocalDouble */ NULL,
    /* 26 : jvmti_SetLocalObject */ NULL,
    /* 27 : jvmti_SetLocalInt */ NULL,
    /* 28 : jvmti_SetLocalLong */ NULL,
    /* 29 : jvmti_SetLocalFloat */ NULL,
    /* 30 : jvmti_SetLocalDouble */ NULL,
    /* 31 : jvmti_CreateRawMonitor */ NULL,
    /* 32 : jvmti_DestroyRawMonitor */ NULL,
    /* 33 : jvmti_RawMonitorEnter */ NULL,
    /* 34 : jvmti_RawMonitorExit */ NULL,
    /* 35 : jvmti_RawMonitorWait */ NULL,
    /* 36 : jvmti_RawMonitorNotify */ NULL,
    /* 37 : jvmti_RawMonitorNotifyAll */ NULL,
    /* 38 : jvmti_SetBreakpoint */ NULL,
    /* 39 : jvmti_ClearBreakpoint */ NULL,
    (void *) jvmti_reserved,
    /* 41 : jvmti_SetFieldAccessWatch */ NULL,
    /* 42 : jvmti_ClearFieldAccessWatch */ NULL,
    /* 43 : jvmti_SetFieldModificationWatch */ NULL,
    /* 44 : jvmti_ClearFieldModificationWatch */ NULL,
    /* 45 : jvmti_IsModifiableClass */ NULL,
    /* 46 : jvmti_Allocate */ NULL,
    /* 47 : jvmti_Deallocate */ NULL,
    /* 48 : jvmti_GetClassSignature */ NULL,
    /* 49 : jvmti_GetClassStatus */ NULL,
    /* 50 : jvmti_GetSourceFileName */ NULL,
    /* 51 : jvmti_GetClassModifiers */ NULL,
    /* 52 : jvmti_GetClassMethods */ NULL,
    /* 53 : jvmti_GetClassFields */ NULL,
    /* 54 : jvmti_GetImplementedInterfaces */ NULL,
    /* 55 : jvmti_IsInterface */ NULL,
    /* 56 : jvmti_IsArrayClass */ NULL,
    /* 57 : jvmti_GetClassLoader */ NULL,
    /* 58 : jvmti_GetObjectHashCode */ NULL,
    /* 59 : jvmti_GetObjectMonitorUsage */ NULL,
    /* 60 : jvmti_GetFieldName */ NULL,
    /* 61 : jvmti_GetFieldDeclaringClass */ NULL,
    /* 62 : jvmti_GetFieldModifiers */ NULL,
    /* 63 : jvmti_IsFieldSynthetic */ NULL,
    /* 64 : jvmti_GetMethodName */ NULL,
    /* 65 : jvmti_GetMethodDeclaringClass */ NULL,
    /* 66 : jvmti_GetMethodModifiers */ NULL,
    (void *) jvmti_reserved,
    /* 68 : jvmti_GetMaxLocals */ NULL,
    /* 69 : jvmti_GetArgumentsSize */ NULL,
    /* 70 : jvmti_GetLineNumberTable */ NULL,
    /* 71 : jvmti_GetMethodLocation */ NULL,
    /* 72 : jvmti_GetLocalVariableTable */ NULL,
    /* 73 : jvmti_SetNativeMethodPrefix */ NULL,
    /* 74 : jvmti_SetNativeMethodPrefixes */ NULL,
    /* 75 : jvmti_GetBytecodes */ NULL,
    /* 76 : jvmti_IsMethodNative */ NULL,
    /* 77 : jvmti_IsMethodSynthetic */ NULL,
    /* 78 : jvmti_GetLoadedClasses */ NULL,
    /* 79 : jvmti_GetClassLoaderClasses */ NULL,
    /* 80 : jvmti_PopFrame */ NULL,
    /* 81 : jvmti_ForceEarlyReturnObject */ NULL,
    /* 82 : jvmti_ForceEarlyReturnInt */ NULL,
    /* 83 : jvmti_ForceEarlyReturnLong */ NULL,
    /* 84 : jvmti_ForceEarlyReturnFloat */ NULL,
    /* 85 : jvmti_ForceEarlyReturnDouble */ NULL,
    /* 86 : jvmti_ForceEarlyReturnVoid */ NULL,
    /* 87 : jvmti_RedefineClasses */ NULL,
    /* 88 : jvmti_GetVersionNumber */ NULL,
    /* 89 : jvmti_GetCapabilities */ NULL,
    /* 90 : jvmti_GetSourceDebugExtension */ NULL,
    /* 91 : jvmti_IsMethodObsolete */ NULL,
    /* 92 : jvmti_SuspendThreadList */ NULL,
    /* 93 : jvmti_ResumeThreadList */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* 100 : jvmti_GetAllStackTraces */ NULL,
    /* 101 : jvmti_GetThreadListStackTraces */ NULL,
    /* 102 : jvmti_GetThreadLocalStorage */ NULL,
    /* 103 : jvmti_SetThreadLocalStorage */ NULL,
    /* 104 : jvmti_GetStackTrace */ NULL,
    (void *) jvmti_reserved,
    /* 106 : jvmti_GetTag */ NULL,
    /* 107 : jvmti_SetTag */ NULL,
    /* 108 : jvmti_ForceGarbageCollection */ NULL,
    /* 109 : jvmti_IterateOverObjectsReachableFromObject */ NULL,
    /* 110 : jvmti_IterateOverReachableObjects */ NULL,
    /* 111 : jvmti_IterateOverHeap */ NULL,
    /* 112 : jvmti_IterateOverInstancesOfClass */ NULL,
    (void *) jvmti_reserved,
    /* 114 : jvmti_GetObjectsWithTags */ NULL,
    /* 115 : jvmti_FollowReferences */ NULL,
    /* 116 : jvmti_IterateThroughHeap */ NULL,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    (void *) jvmti_reserved,
    /* 120 : jvmti_SetJNIFunctionTable */ NULL,
    /* 121 : jvmti_GetJNIFunctionTable */ NULL,
    /* 122 : jvmti_SetEventCallbacks */ NULL,
    /* 123 : jvmti_GenerateEvents */ NULL,
    /* 124 : jvmti_GetExtensionFunctions */ NULL,
    /* 125 : jvmti_GetExtensionEvents */ NULL,
    /* 126 : jvmti_SetExtensionEventCallback */ NULL,
    /* 127 : jvmti_DisposeEnvironment */ NULL,
    /* 128 : jvmti_GetErrorName */ NULL,
    /* 129 : jvmti_GetJLocationFormat */ NULL,
    /* 130 : jvmti_GetSystemProperties */ NULL,
    /* 131 : jvmti_GetSystemProperty */ NULL,
    /* 132 : jvmti_SetSystemProperty */ NULL,
    /* 133 : jvmti_GetPhase */ NULL,
    /* 134 : jvmti_GetCurrentThreadCpuTimerInfo */ NULL,
    /* 135 : jvmti_GetCurrentThreadCpuTime */ NULL,
    /* 136 : jvmti_GetThreadCpuTimerInfo */ NULL,
    /* 137 : jvmti_GetThreadCpuTime */ NULL,
    /* 138 : jvmti_GetTimerInfo */ NULL,
    /* 139 : jvmti_GetTime */ NULL,
    /* 140 : jvmti_GetPotentialCapabilities */ NULL,
    (void *) jvmti_reserved,
    /* 142 : jvmti_AddCapabilities */ NULL,
    /* 143 : jvmti_RelinquishCapabilities */ NULL,
    /* 144 : jvmti_GetAvailableProcessors */ NULL,
    /* 145 : jvmti_GetClassVersionNumbers */ NULL,
    /* 146 : jvmti_GetConstantPool */ NULL,
    /* 147 : jvmti_GetEnvironmentLocalStorage */ NULL,
    /* 148 : jvmti_SetEnvironmentLocalStorage */ NULL,
    /* 149 : jvmti_AddToBootstrapClassLoaderSearch */ NULL,
    /* 150 : jvmti_SetVerboseFlag */ NULL,
    /* 151 : jvmti_AddToSystemClassLoaderSearch */ NULL,
    /* 152 : jvmti_RetransformClasses */ NULL,
    /* 153 : jvmti_GetOwnedMonitorStackDepthInfo */ NULL,
    /* 154 : jvmti_GetObjectSize */ NULL
#ifdef JDK7
    , /* 155 : jvmti_GetLocalInstance */ NULL
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

