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

#ifndef __vm_h__
#define __vm_h__

#include "jni.h"

/*
 * DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.
 *
 * Instead, modify VMFunctionsSource.java and (re)run 'mx jnigen'.
 */

/*
 * A set of VM functions implemented in Java that can (only) be called from C code.
 * These are defined in VMFunctionsSource.java.
 */
typedef struct vmInterface_ {
// START GENERATED CODE
        void (JNICALL *Unimplemented) (JNIEnv *env);
        jint (JNICALL *HashCode) (JNIEnv *env, jobject obj);
        void (JNICALL *MonitorWait) (JNIEnv *env, jobject obj, jlong timeout);
        void (JNICALL *MonitorNotify) (JNIEnv *env, jobject obj);
        void (JNICALL *MonitorNotifyAll) (JNIEnv *env, jobject obj);
        jobject (JNICALL *Clone) (JNIEnv *env, jobject obj);
        jobject (JNICALL *InternString) (JNIEnv *env, jobject s);
        void (JNICALL *Exit) (JNIEnv *env, jint code);
        void (JNICALL *Halt) (JNIEnv *env, jint code);
        void (JNICALL *GC) (JNIEnv *env);
        jlong (JNICALL *MaxObjectInspectionAge) (JNIEnv *env);
        jlong (JNICALL *FreeMemory) (JNIEnv *env);
        jlong (JNICALL *MaxMemory) (JNIEnv *env);
        void (JNICALL *FillInStackTrace) (JNIEnv *env, jobject throwable);
        jint (JNICALL *GetStackTraceDepth) (JNIEnv *env, jobject throwable);
        jobject (JNICALL *GetStackTraceElement) (JNIEnv *env, jobject throwable, jint index);
        void (JNICALL *StartThread) (JNIEnv *env, jobject thread);
        void (JNICALL *StopThread) (JNIEnv *env, jobject thread, jobject throwable);
        jboolean (JNICALL *IsThreadAlive) (JNIEnv *env, jobject thread);
        void (JNICALL *SuspendThread) (JNIEnv *env, jobject thread);
        void (JNICALL *ResumeThread) (JNIEnv *env, jobject thread);
        void (JNICALL *SetThreadPriority) (JNIEnv *env, jobject thread, jint newPriority);
        void (JNICALL *Yield) (JNIEnv *env);
        void (JNICALL *Sleep) (JNIEnv *env, jlong millis);
        jobject (JNICALL *CurrentThread) (JNIEnv *env);
        jint (JNICALL *CountStackFrames) (JNIEnv *env, jobject thread);
        void (JNICALL *Interrupt) (JNIEnv *env, jobject thread);
        jboolean (JNICALL *IsInterrupted) (JNIEnv *env, jobject thread);
        jboolean (JNICALL *HoldsLock) (JNIEnv *env, jobject obj);
        jobject (JNICALL *GetClassContext) (JNIEnv *env);
        jobject (JNICALL *GetCallerClass) (JNIEnv *env, jint depth);
        jobject (JNICALL *GetSystemPackage) (JNIEnv *env, jobject name);
        jobject (JNICALL *GetSystemPackages) (JNIEnv *env);
        jobject (JNICALL *LatestUserDefinedLoader) (JNIEnv *env);
        jobject (JNICALL *GetClassName) (JNIEnv *env, jobject c);
        jobject (JNICALL *GetClassLoader) (JNIEnv *env, jobject c);
        jboolean (JNICALL *IsInterface) (JNIEnv *env, jobject c);
        jboolean (JNICALL *IsArrayClass) (JNIEnv *env, jobject c);
        jboolean (JNICALL *IsPrimitiveClass) (JNIEnv *env, jobject c);
        jobject (JNICALL *GetClassSigners) (JNIEnv *env, jobject c);
        void (JNICALL *SetClassSigners) (JNIEnv *env, jobject c, jobject signers);
        jobject (JNICALL *GetProtectionDomain) (JNIEnv *env, jobject c);
        void (JNICALL *SetProtectionDomain) (JNIEnv *env, jobject c, jobject pd);
        void (JNICALL *ArrayCopy) (JNIEnv *env, jobject src, jint srcPos, jobject dest, jint destPos, jint length);
        jobject (JNICALL *GetAllThreads) (JNIEnv *env);
        jobject (JNICALL *GetThreadStateValues) (JNIEnv *env, jint javaThreadState);
        jobject (JNICALL *GetThreadStateNames) (JNIEnv *env, jint javaThreadState, jobject threadStateValues);
        jobject (JNICALL *InitAgentProperties) (JNIEnv *env, jobject props);
        jint (JNICALL *GetNumberOfArguments) (JNIEnv *env, jmethodID methodID);
        void (JNICALL *GetKindsOfArguments) (JNIEnv *env, jmethodID methodID, void* kinds);
        void (JNICALL *SetJVMTIEnv) (JNIEnv *env, void* jvmtiEnv);
// END GENERATED CODE
} VMInterface;

extern struct vmInterface_ vm;

/**
 * Gets a pointer to the global VM/JNI/JMM/JVMTI function table.
 *
 * Defined in Native/substrate/{vm.c,jni.c,jmm.c,jvmti.c}
 */
extern VMInterface *getVMInterface();
extern JNIEnv jniEnv();
void* getJMMInterface(int version);
void* getJVMTIInterface(int version);

/**
 * Defined in Native/substrate/jni.c
 */
extern JNIEnv *currentJniEnv();

#endif /* !__vm_h__ */
