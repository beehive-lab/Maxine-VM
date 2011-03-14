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
 * This file implements the parts of HotSpot's "JVM" interface
 * that the native libraries of the JDK require to have something to call back into.
 *
 * In cases where we bypass JDK's native libraries (@see com.sun.max.vm.jdk)
 * we can simply omit unneeded JVM interface functions that would otherwise occur here.
 */
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>

#include "jni.h"
#include "log.h"
#include "trap.h"
#include "mutex.h"
#include "threads.h"
#include "maxine.h"
#include "memory.h"

#if os_SOLARIS
#include <sys/filio.h>
#endif

/**
 * Defined in Native/substrate/jni.c
 */
extern JNIEnv *currentJniEnv();

#if os_DARWIN
#define lseek64 lseek
#endif


// Platform-independent error return values from OS functions
enum OSReturn {
  OS_OK         =  0,        // Operation was successful
  OS_ERR        = -1,        // Operation failed
  OS_INTRPT     = -2,        // Operation was interrupted
  OS_TIMEOUT    = -3,        // Operation timed out
  OS_NOMEM      = -5,        // Operation failed for lack of memory
  OS_NORESOURCE = -6         // Operation failed for lack of nonmemory resource
};

#define RESTARTABLE(_cmd, _result) do { \
		do { \
			_result = _cmd; \
		} while((_result == OS_ERR) && (errno == EINTR)); \
} while(false)

#define RESTARTABLE_RETURN_INT(_cmd) do { \
		int _result; \
		RESTARTABLE(_cmd, _result); \
		return _result; \
} while(false)


/*****************************************************************/
#define JVM_EEXIST -100
//#define DEBUG_JVM_X 1

typedef struct {
    jclass jClass;
    jmethodID jMethod;
} JNIMethod;

JNIMethod resolveCriticalStaticMethod(JNIEnv *env, char *className, char *methodName, char *signature) {
    JNIMethod result;
    result.jClass = (*env)->FindClass(env, className);
    if (result.jClass == NULL) {
        log_exit(-1, "JVM_*: could not resolve critical class \"%s\"", className);
    }
    result.jMethod = (*env)->GetStaticMethodID(env, result.jClass, methodName, signature);
    if (result.jMethod == NULL) {
        log_exit(-1, "JVM_*: could not resolve critical method \"%s.%s%s\"", className, methodName, signature);
    }
    return result;
}

JNIMethod resolveCriticalInstanceMethod(JNIEnv *env, char *className, char *methodName, char *signature) {
    JNIMethod result;
    result.jClass = (*env)->FindClass(env, className);
    if (result.jClass == NULL) {
        log_exit(-1, "JVM_*: could not resolve critical class \"%s\"", className);
    }
    result.jMethod = (*env)->GetMethodID(env, result.jClass, methodName, signature);
    if (result.jMethod == NULL) {
        log_exit(-1, "JVM_*: could not resolve critical method \"%s.%s%s\"", className, methodName, signature);
    }
    return result;
}

#define JVM_INTERFACE_VERSION 4

jint JVM_GetInterfaceVersion(void) {
    return JVM_INTERFACE_VERSION;
}

#define UNIMPLEMENTED_WITH_ENV() { \
    log_println("UNIMPLEMENTED: %s in %s:%d", __func__, __FILE__, __LINE__); \
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "Unimplemented", "()V"); \
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod); \
}

#define UNIMPLEMENTED() { \
    log_println("UNIMPLEMENTED: %s in %s:%d", __func__, __FILE__, __LINE__); \
    JNIEnv *env = currentJniEnv(); \
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "Unimplemented", "()V"); \
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod); \
}

/*************************************************************************
 PART 1: Functions for Native Libraries
 ************************************************************************/
/*
 * java.lang.Object
 */
jint JVM_IHashCode(JNIEnv *env, jobject obj) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Object", "hashCode", "()I");
    return (*env)->CallIntMethod(env, obj, result.jMethod);
}

void JVM_MonitorWait(JNIEnv *env, jobject obj, jlong ms) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Object", "wait", "(J)V");
    (*env)->CallVoidMethod(env, obj, result.jMethod, ms);
}

void JVM_MonitorNotify(JNIEnv *env, jobject obj) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Object", "notify", "()V");
    (*env)->CallVoidMethod(env, obj, result.jMethod);
}

void JVM_MonitorNotifyAll(JNIEnv *env, jobject obj) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Object", "notifyAll", "()V");
    (*env)->CallVoidMethod(env, obj, result.jMethod);
}

jobject JVM_Clone(JNIEnv *env, jobject obj) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Object", "clone", "()Ljava/lang/Object;");
    return (*env)->CallObjectMethod(env, obj, result.jMethod);
}


/*
 * java.lang.String
 */
jstring JVM_InternString(JNIEnv *env, jstring str) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/String", "intern", "()Ljava/lang/String;");
    return (*env)->CallObjectMethod(env, str, result.jMethod);
}

/*
 * java.lang.System
 */
jlong JVM_CurrentTimeMillis(JNIEnv *env, jclass ignored) {
    return native_currentTimeMillis();
}

jlong JVM_NanoTime(JNIEnv *env, jclass ignored) {
    return native_nanoTime();
}

void
JVM_ArrayCopy(JNIEnv *env, jclass ignored, jobject src, jint src_pos, jobject dst, jint dst_pos, jint length) {
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod, src, src_pos, dst, dst_pos, length);
}

jobject JVM_InitProperties(JNIEnv *env, jobject p) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.io.File
 */
void JVM_OnExit(void (*func)(void)) {
    UNIMPLEMENTED();
}


/*
 * java.lang.Runtime
 */
void JVM_Exit(jint code) {
    JNIEnv *env = currentJniEnv();
     JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/MaxineVM", "exit", "(IZ)V");
     (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod, code, JNI_FALSE);
}

void JVM_Halt(jint code) {
    JNIEnv *env = currentJniEnv();
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/MaxineVM", "exit", "(IZ)V");
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod, code, JNI_TRUE);
}

void JVM_GC(void) {
    JNIEnv *env = currentJniEnv();
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/System", "gc", "()V");
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod);
}

jlong JVM_MaxObjectInspectionAge(void) {
    JNIEnv *env = currentJniEnv();
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/heap/Heap", "maxObjectInspectionAge", "()J");
    return (*env)->CallStaticLongMethod(env, result.jClass, result.jMethod);
}

void JVM_TraceInstructions(jboolean on) {
    // safely ignored.
}

void JVM_TraceMethodCalls(jboolean on) {
    // safely ignored.
}

jlong JVM_FreeMemory(void) {
    JNIEnv *env = currentJniEnv();
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/heap/Heap", "reportFreeSpace", "()J");
    return (*env)->CallStaticLongMethod(env, result.jClass, result.jMethod);
}

jlong
JVM_MaxMemory(void) {
    JNIEnv *env = currentJniEnv();
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/heap/Heap", "maxSizeLong", "()J");
    return (*env)->CallStaticLongMethod(env, result.jClass, result.jMethod);
}

jlong JVM_TotalMemory(void) {
    return JVM_MaxMemory();
}

#if os_SOLARIS
#include <sys/pset.h>
#endif

jint
JVM_ActiveProcessorCount(void) {
#if os_SOLARIS
    int online_cpus = sysconf(_SC_NPROCESSORS_ONLN);
    pid_t pid = getpid();
    psetid_t pset = PS_NONE;
    // Are we running in a processor set or is there any processor set around?
    if (pset_bind(PS_QUERY, P_PID, pid, &pset) == 0) {
        uint_t pset_cpus;
        // Query the number of cpus available to us.
        if (pset_info(pset, NULL, &pset_cpus, NULL) == 0) {
            c_ASSERT(pset_cpus > 0 && pset_cpus <= online_cpus);
            return pset_cpus;
        }
    }
    // Otherwise return number of online cpus
    return online_cpus;
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

#if os_SOLARIS || os_LINUX || os_DARWIN
#include <dlfcn.h>
#endif

void *
JVM_LoadLibrary(const char *name) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return dlopen(name, RTLD_LAZY);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

void
JVM_UnloadLibrary(void * handle) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    dlclose(handle);
#else
    UNIMPLEMENTED();
#endif
}

void *
JVM_FindLibraryEntry(void *handle, const char *name) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return dlsym(handle, name);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jboolean
JVM_IsSupportedJNIVersion(jint version) {
    UNIMPLEMENTED();
    return 0;
}

/*
 * java.lang.Float and java.lang.Double
 */
jboolean
JVM_IsNaN(jdouble x) {
    int hx,lx;
    hx = (MATH_HI(x)&0x7fffffff);
    lx = MATH_LO(x);
    hx |= (unsigned)(lx|(-lx))>>31;
    hx = 0x7ff00000 - hx;
    return ((unsigned)(hx))>>31;
}

/*
 * java.lang.Throwable
 */
void
JVM_FillInStackTrace(JNIEnv *env, jobject throwable) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Throwable", "fillInStackTrace", "()V");
    (*env)->CallVoidMethod(env, throwable, result.jMethod);
}

void
JVM_PrintStackTrace(JNIEnv *env, jobject throwable, jobject printable) {
    UNIMPLEMENTED_WITH_ENV();
}

jint
JVM_GetStackTraceDepth(JNIEnv *env, jobject throwable) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Throwable", "getStackTraceDepth", "()I");
    return (*env)->CallIntMethod(env, throwable, result.jMethod);
}

jobject
JVM_GetStackTraceElement(JNIEnv *env, jobject throwable, jint index) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Throwable", "getStackTraceElement", "(I)Ljava/lang/StackTraceElement;");
    return (*env)->CallObjectMethod(env, throwable, result.jMethod, index);
}

/*
 * java.lang.Compiler
 */
void
JVM_InitializeCompiler (JNIEnv *env, jclass compCls) {
    // safely ignored.
}

jboolean
JVM_IsSilentCompiler(JNIEnv *env, jclass compCls) {
    return 1;
}

jboolean
JVM_CompileClass(JNIEnv *env, jclass compCls, jclass cls) {
    // safely ignored (for now).
    return 1;
}

jboolean
JVM_CompileClasses(JNIEnv *env, jclass cls, jstring jname) {
    // safely ignored (for now).
    return 1;
}

jobject
JVM_CompilerCommand(JNIEnv *env, jclass compCls, jobject arg) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

void
JVM_EnableCompiler(JNIEnv *env, jclass compCls) {
    // safely ignored (for now).
}

void
JVM_DisableCompiler(JNIEnv *env, jclass compCls) {
    // safely ignored (for now).
}

/*
 * java.lang.Thread
 */
void
JVM_StartThread(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "start", "()V");
    (*env)->CallVoidMethod(env, thread, result.jMethod);
}

void
JVM_StopThread(JNIEnv *env, jobject thread, jobject exception) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "stop", "(Ljava/lang/Throwable;)V");
    (*env)->CallVoidMethod(env, thread, result.jMethod, exception);
}

jboolean
JVM_IsThreadAlive(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "isAlive", "()Z");
    return (*env)->CallBooleanMethod(env, thread, result.jMethod);
}

void
JVM_SuspendThread(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "suspend", "()V");
    (*env)->CallVoidMethod(env, thread, result.jMethod);
}

void
JVM_ResumeThread(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "resume", "()V");
    (*env)->CallVoidMethod(env, thread, result.jMethod);
}

void
JVM_SetThreadPriority(JNIEnv *env, jobject thread, jint prio) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "setPriority", "(I)V");
    (*env)->CallVoidMethod(env, thread, result.jMethod, prio);
}

#if os_DARWIN
/* This function is expected by libjava.jnilib on Mac OS X. The current signature is just a guess. [Doug].*/
void
JVM_SetNativeThreadName(JNIEnv *env, jobject thread, jobject name) {
    UNIMPLEMENTED_WITH_ENV();
}
#endif

void
JVM_Yield(JNIEnv *env, jclass threadClass) {
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/Thread", "yield", "()V");
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod);
}

void
JVM_Sleep(JNIEnv *env, jclass threadClass, jlong millis) {
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/Thread", "sleep", "(J)V");
    (*env)->CallStaticVoidMethod(env, result.jClass, result.jMethod, millis);
}

jobject
JVM_CurrentThread(JNIEnv *env, jclass threadClass) {
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod);
}

jint
JVM_CountStackFrames(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "countStackFrames", "()I");
    return (*env)->CallIntMethod(env, thread, result.jMethod);
}

void
JVM_Interrupt(JNIEnv *env, jobject thread) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "interrupt", "()V");
    (*env)->CallVoidMethod(env, thread, result.jMethod);
}

jboolean
JVM_IsInterrupted(JNIEnv *env, jobject thread, jboolean clearInterrupted) {
    if (clearInterrupted) {
      // TODO: this is not the correct method to call, since it only checks the current thread
        JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/Thread", "interrupted", "()Z");
        return (*env)->CallStaticBooleanMethod(env, result.jClass, result.jMethod);
    } else {
        JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Thread", "isInterrupted", "()Z");
        return (*env)->CallBooleanMethod(env, thread, result.jMethod);
    }
}

jboolean
JVM_HoldsLock(JNIEnv *env, jclass threadClass, jobject obj) {
    JNIMethod result = resolveCriticalStaticMethod(env, "java/lang/Thread", "holdsLock", "(Ljava/lang/Object;)Z");
    return (*env)->CallStaticBooleanMethod(env, result.jClass, result.jMethod, obj);
}

void
JVM_DumpAllStacks(JNIEnv *env, jclass unused) {
    UNIMPLEMENTED_WITH_ENV();
}

jobjectArray
JVM_GetAllThreads(JNIEnv *env, jclass dummy) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetAllThreads", "()[Ljava/lang/Thread;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod);
}

/* getStackTrace() and getAllStackTraces() method */
jobjectArray
JVM_DumpThreads(JNIEnv *env, jclass threadClass, jobjectArray threads) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.lang.SecurityManager
 */
jclass
JVM_CurrentLoadedClass(JNIEnv *env) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_CurrentClassLoader(JNIEnv *env) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}


jobjectArray
JVM_GetClassContext(JNIEnv *env) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetClassContext", "()[Ljava/lang/Class;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod);
}

jint
JVM_ClassDepth(JNIEnv *env, jstring name) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jint
JVM_ClassLoaderDepth(JNIEnv *env) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.lang.Package
 */
jstring
JVM_GetSystemPackage(JNIEnv *env, jstring name) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetSystemPackage", "(Ljava/lang/String;)Ljava/lang/String;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod, name);
}

jobjectArray
JVM_GetSystemPackages(JNIEnv *env) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetSystemPackages", "()[Ljava/lang/String;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod);
}

/*
 * java.io.ObjectInputStream
 */
jobject
JVM_AllocateNewObject(JNIEnv *env, jobject obj, jclass currClass,
                      jclass initClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_AllocateNewArray(JNIEnv *env, jobject obj, jclass currClass,
                     jint length) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_LatestUserDefinedLoader(JNIEnv *env) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "LatestUserDefinedLoader", "()Ljava/lang/ClassLoader;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod);
}

/*
 * This function has been deprecated and should not be considered
 * part of the specified JVM interface.
 */
jclass
JVM_LoadClass0(JNIEnv *env, jobject obj, jclass currClass,
               jstring currClassName) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.lang.reflect.Array
 */
jint
JVM_GetArrayLength(JNIEnv *env, jobject arr) {
    return (*env)->GetArrayLength(env, arr);
}

jobject
JVM_GetArrayElement(JNIEnv *env, jobject arr, jint index) {
    return (*env)->GetObjectArrayElement(env, arr, index);
}

jvalue
JVM_GetPrimitiveArrayElement(JNIEnv *env, jobject arr, jint index, jint wCode) {
    jvalue v;
    UNIMPLEMENTED_WITH_ENV();
    return v;
}

void
JVM_SetArrayElement(JNIEnv *env, jobject arr, jint index, jobject val) {
    (*env)->SetObjectArrayElement(env, arr, index, val);
}

void
JVM_SetPrimitiveArrayElement(JNIEnv *env, jobject arr, jint index, jvalue v,
                 unsigned char vCode) {
    UNIMPLEMENTED_WITH_ENV();
}

jobject
JVM_NewArray(JNIEnv *env, jclass eltClass, jint length) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_NewMultiArray(JNIEnv *env, jclass eltClass, jintArray dim) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.lang.Class and java.lang.ClassLoader
 */
/*
 * Returns the class in which the code invoking the native method
 * belongs.
 *
 * Note that in JDK 1.1, native methods did not create a frame.
 * In 1.2, they do. Therefore native methods like Class.forName
 * can no longer look at the current frame for the caller class.
 */
jclass
JVM_GetCallerClass(JNIEnv *env, int n) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetCallerClass", "(I)Ljava/lang/Class;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod, n);
}

/*
 * Find primitive classes
 * utf: class name
 */
jclass
JVM_FindPrimitiveClass(JNIEnv *env, const char *utf) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Link the class
 */
void
JVM_ResolveClass(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
}

/*
 * Find a class from a given class loader. Throw ClassNotFoundException
 * or NoClassDefFoundError depending on the value of the last
 * argument.
 */
jclass
JVM_FindClassFromClassLoader(JNIEnv *env, const char *name, jboolean init,
                 jobject loader, jboolean throwError) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Find a class from a given class.
 */
jclass
JVM_FindClassFromClass(JNIEnv *env, const char *name, jboolean init,
                 jclass from) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Find a loaded class cached by the VM */
jclass
JVM_FindLoadedClass(JNIEnv *env, jobject loader, jstring name) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Define a class */
jclass
JVM_DefineClass(JNIEnv *env, const char *name, jobject loader, const jbyte *buf,
                jsize len, jobject pd) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Define a class with a source (added in JDK1.5) */
jclass
JVM_DefineClassWithSource(JNIEnv *env, const char *name, jobject loader,
                          const jbyte *buf, jsize len, jobject pd,
                          const char *source) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Reflection support functions
 */

jstring
JVM_GetClassName(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "getName", "()Ljava/lang/String;");
    return (*env)->CallObjectMethod(env, cls, result.jMethod);
}

jobjectArray
JVM_GetClassInterfaces(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_GetClassLoader(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
    return (*env)->CallObjectMethod(env, cls, result.jMethod);
}

jboolean
JVM_IsInterface(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "isInterface", "()Z");
    return (*env)->CallBooleanMethod(env, cls, result.jMethod);
}

jobjectArray
JVM_GetClassSigners(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "getSigners", "()[Ljava/lang/Object;");
    return (*env)->CallObjectMethod(env, cls, result.jMethod);
}

void
JVM_SetClassSigners(JNIEnv *env, jclass cls, jobjectArray signers) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "setSigners", "([Ljava/lang/Object;)V");
    (*env)->CallObjectMethod(env, cls, result.jMethod, signers);
}

jobject
JVM_GetProtectionDomain(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "getProtectionDomain", "()Ljava/security/ProtectionDomain;");
    return (*env)->CallObjectMethod(env, cls, result.jMethod);
}

void
JVM_SetProtectionDomain(JNIEnv *env, jclass cls, jobject protection_domain) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "setProtectionDomain0", "(Ljava/security/ProtectionDomain;)V");
    (*env)->CallObjectMethod(env, cls, result.jMethod, protection_domain);
}

jboolean
JVM_IsArrayClass(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "isArray", "()Z");
    return (*env)->CallBooleanMethod(env, cls, result.jMethod);
}

jboolean
JVM_IsPrimitiveClass(JNIEnv *env, jclass cls) {
    JNIMethod result = resolveCriticalInstanceMethod(env, "java/lang/Class", "isPrimitive", "()Z");
    return (*env)->CallBooleanMethod(env, cls, result.jMethod);
}

jclass
JVM_GetComponentType(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jint
JVM_GetClassModifiers(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobjectArray
JVM_GetDeclaredClasses(JNIEnv *env, jclass ofClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jclass
JVM_GetDeclaringClass(JNIEnv *env, jclass ofClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Generics support (JDK 1.5) */
jstring
JVM_GetClassSignature(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Annotations support (JDK 1.5) */
jbyteArray
JVM_GetClassAnnotations(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Annotations support (JDK 1.6) */

// field is a handle to a java.lang.reflect.Field object
jbyteArray
JVM_GetFieldAnnotations(JNIEnv *env, jobject field) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

// method is a handle to a java.lang.reflect.Method object
jbyteArray
JVM_GetMethodAnnotations(JNIEnv *env, jobject method) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

// method is a handle to a java.lang.reflect.Method object
jbyteArray
JVM_GetMethodDefaultAnnotationValue(JNIEnv *env, jobject method) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

// method is a handle to a java.lang.reflect.Method object
jbyteArray
JVM_GetMethodParameterAnnotations(JNIEnv *env, jobject method) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}


/*
 * New (JDK 1.4) reflection implementation
 */

jobjectArray
JVM_GetClassDeclaredMethods(JNIEnv *env, jclass ofClass, jboolean publicOnly) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobjectArray
JVM_GetClassDeclaredFields(JNIEnv *env, jclass ofClass, jboolean publicOnly) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobjectArray
JVM_GetClassDeclaredConstructors(JNIEnv *env, jclass ofClass, jboolean publicOnly) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Differs from JVM_GetClassModifiers in treatment of inner classes.
   This returns the access flags for the class as specified in the
   class file rather than searching the InnerClasses attribute (if
   present) to find the source-level access flags. Only the values of
   the low 13 bits (i.e., a mask of 0x1FFF) are guaranteed to be
   valid. */
jint
JVM_GetClassAccessFlags(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Constant pool access; currently used to implement reflective access to annotations (JDK 1.5)
 */

jobject
JVM_GetClassConstantPool(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jint JVM_ConstantPoolGetSize
(JNIEnv *env, jobject unused, jobject jcpool) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jclass JVM_ConstantPoolGetClassAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jclass JVM_ConstantPoolGetClassAtIfLoaded
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject JVM_ConstantPoolGetMethodAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject JVM_ConstantPoolGetMethodAtIfLoaded
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject JVM_ConstantPoolGetFieldAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject JVM_ConstantPoolGetFieldAtIfLoaded
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobjectArray JVM_ConstantPoolGetMemberRefInfoAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jint JVM_ConstantPoolGetIntAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jlong JVM_ConstantPoolGetLongAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jfloat JVM_ConstantPoolGetFloatAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jdouble JVM_ConstantPoolGetDoubleAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jstring JVM_ConstantPoolGetStringAt
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jstring JVM_ConstantPoolGetUTF8At
(JNIEnv *env, jobject unused, jobject jcpool, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * java.security.*
 */

jobject
JVM_DoPrivileged(JNIEnv *env, jclass cls,
         jobject action, jobject context, jboolean wrapException) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_GetInheritedAccessControlContext(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jobject
JVM_GetStackAccessControlContext(JNIEnv *env, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * This function is included primarily as a debugging aid. If Java is
 * running in a console window, then pressing <CTRL-\\> will cause
 * the current state of all active threads and monitors to be written
 * to the console window.
 */

void*
JVM_RegisterSignal(jint sig, void* handler) {
    void *newHandler = handler == (void *) 2 ? (void *) userSignalHandler : handler;
    switch (sig) {
        /* The following are already used by the VM. */
        case SIGFPE:
        case SIGILL:
        case SIGSEGV:
            return (void *)-1;
    }

    void* oldHandler = setSignalHandler(sig, (SignalHandlerFunction) newHandler);

    if (oldHandler == (void *) userSignalHandler) {
        return (void *)2;
    } else {
        return oldHandler;
    }
}

jboolean
JVM_RaiseSignal(jint sig) {
    if (traceSignals) {
        log_print("Thread %p raising signal %d\n", thread_self(), sig);
    }
#if os_SOLARIS || os_DARWIN
    if (raise((int) sig) < 0) {
        log_println("error raising signal %d in current process: %s", sig, strerror(errno));
    }
#elif os_LINUX
    /* For some reason raise(3) does not work on the Linux gate machine.
     * Instead we use kill(2) and getpid(2) explicitly. */
    if (kill(getpid(), sig) < 0) {
        log_println("error raising signal %d in current process: %s", sig, strerror(errno));
    }
#else
    UNIMPLEMENTED();
#endif
    return JNI_TRUE;
}

#if os_DARWIN || os_LINUX
typedef struct {
  const char *name;
  int   number;
} Signal;

Signal signals[] = {
   {"HUP",     SIGHUP},
   {"INT",     SIGINT},
   {"QUIT",    SIGQUIT},
   {"ILL",     SIGILL},
   {"TRAP",    SIGTRAP},
   {"ABRT",    SIGABRT},
#if os_DARWIN
   {"EMT",     SIGEMT},
#endif
   {"FPE",     SIGFPE},
   {"KILL",    SIGKILL},
   {"BUS",     SIGBUS},
   {"SEGV",    SIGSEGV},
   {"SYS",     SIGSYS},
   {"PIPE",    SIGPIPE},
   {"ALRM",    SIGALRM},
   {"TERM",    SIGTERM},
   {"URG",     SIGURG},
   {"STOP",    SIGSTOP},
   {"TSTP",    SIGTSTP},
   {"CONT",    SIGCONT},
   {"CHLD",    SIGCHLD},
   {"TTIN",    SIGTTIN},
   {"TTOU",    SIGTTOU},
   {"IO",      SIGIO},
   {"XCPU",    SIGXCPU},
   {"XFSZ",    SIGXFSZ},
   {"VTALRM",  SIGVTALRM},
   {"PROF",    SIGPROF},
   {"WINCH",   SIGWINCH},
#if os_DARWIN
   {"INFO",    SIGINFO},
#endif
   {"USR1",    SIGUSR1},
   {"USR2",    SIGUSR2},
  };
#endif

jint
JVM_FindSignal(const char *name) {
#if os_DARWIN || os_LINUX
    unsigned int i;
    for (i = 0; i < ARRAY_LENGTH(signals); i++) {
        if(!strcmp(name, signals[i].name)) {
            return signals[i].number;
        }
    }
    return -1;
#elif os_SOLARIS
    int sig;
    if (str2sig(name, &sig) == 0) {
        return sig;
    } else {
        return -1;
    }
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

/*
 * Retrieve the assertion directives for the specified class.
 */
jboolean
JVM_DesiredAssertionStatus(JNIEnv *env, jclass unused, jclass cls) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Retrieve the assertion directives from the VM.
 */
jobject
JVM_AssertionStatusDirectives(JNIEnv *env, jclass unused) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * sun.misc.AtomicLong
 */
jboolean
JVM_SupportsCX8(void) {
    return 0;
}

jboolean
JVM_CX8Field(JNIEnv *env, jobject obj, jfieldID fldID, jlong oldVal, jlong newVal) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*************************************************************************
 PART 2: Support for the Verifier and Class File Format Checker
 ************************************************************************/
/*
 * Return the class name in UTF format. The result is valid
 * until JVM_ReleaseUTf is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetClassNameUTF(JNIEnv *env, jclass cb) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the constant pool types in the buffer provided by "types."
 */
void
JVM_GetClassCPTypes(JNIEnv *env, jclass cb, unsigned char *types) {
    UNIMPLEMENTED_WITH_ENV();
}

/*
 * Returns the number of Constant Pool entries.
 */
jint
JVM_GetClassCPEntriesCount(JNIEnv *env, jclass cb) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the number of *declared* fields or methods.
 */
jint
JVM_GetClassFieldsCount(JNIEnv *env, jclass cb) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

jint
JVM_GetClassMethodsCount(JNIEnv *env, jclass cb) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the CP indexes of exceptions raised by a given method.
 * Places the result in the given buffer.
 *
 * The method is identified by method_index.
 */
void
JVM_GetMethodIxExceptionIndexes(JNIEnv *env, jclass cb, jint method_index,
                unsigned short *exceptions) {
    UNIMPLEMENTED_WITH_ENV();
}
/*
 * Returns the number of exceptions raised by a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxExceptionsCount(JNIEnv *env, jclass cb, jint method_index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the byte code sequence of a given method.
 * Places the result in the given buffer.
 *
 * The method is identified by method_index.
 */
void
JVM_GetMethodIxByteCode(JNIEnv *env, jclass cb, jint method_index,
            unsigned char *code) {
    UNIMPLEMENTED_WITH_ENV();
}

/*
 * Returns the length of the byte code sequence of a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxByteCodeLength(JNIEnv *env, jclass cb, jint method_index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * A structure used to a capture exception table entry in a Java method.
 */
typedef struct {
    jint start_pc;
    jint end_pc;
    jint handler_pc;
    jint catchType;
} JVM_ExceptionTableEntryType;

/*
 * Returns the exception table entry at entry_index of a given method.
 * Places the result in the given buffer.
 *
 * The method is identified by method_index.
 */
void
JVM_GetMethodIxExceptionTableEntry(JNIEnv *env, jclass cb, jint method_index,
                   jint entry_index,
                   JVM_ExceptionTableEntryType *entry) {
    UNIMPLEMENTED_WITH_ENV();
}

/*
 * Returns the length of the exception table of a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxExceptionTableLength(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the modifiers of a given field.
 * The field is identified by field_index.
 */
jint
JVM_GetFieldIxModifiers(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the modifiers of a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxModifiers(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the number of local variables of a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxLocalsCount(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the number of arguments (including this pointer) of a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxArgsSize(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the maximum amount of stack (in words) used by a given method.
 * The method is identified by method_index.
 */
jint
JVM_GetMethodIxMaxStack(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Is a given method a constructor.
 * The method is identified by method_index.
 */
jboolean
JVM_IsConstructorIx(JNIEnv *env, jclass cb, int index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the name of a given method in UTF format.
 * The result remains valid until JVM_ReleaseUTF is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetMethodIxNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the signature of a given method in UTF format.
 * The result remains valid until JVM_ReleaseUTF is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetMethodIxSignatureUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the name of the field referred to at a given constant pool
 * index.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPFieldNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the name of the method referred to at a given constant pool
 * index.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPMethodNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the signature of the method referred to at a given constant pool
 * index.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPMethodSignatureUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the signature of the field referred to at a given constant pool
 * index.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPFieldSignatureUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the class name referred to at a given constant pool index.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPClassNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the class name referred to at a given constant pool index.
 *
 * The constant pool entry must refer to a CONSTANT_Fieldref.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPFieldClassNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the class name referred to at a given constant pool index.
 *
 * The constant pool entry must refer to CONSTANT_Methodref or
 * CONSTANT_InterfaceMethodref.
 *
 * The result is in UTF format and remains valid until JVM_ReleaseUTF
 * is called.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 */
const char *
JVM_GetCPMethodClassNameUTF(JNIEnv *env, jclass cb, jint index) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the modifiers of a field in calledClass. The field is
 * referred to in class cb at constant pool entry index.
 *
 * The caller must treat the string as a constant and not modify it
 * in any way.
 *
 * Returns -1 if the field does not exist in calledClass.
 */
jint
JVM_GetCPFieldModifiers(JNIEnv *env, jclass cb, int index, jclass calledClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Returns the modifiers of a method in calledClass. The method is
 * referred to in class cb at constant pool entry index.
 *
 * Returns -1 if the method does not exist in calledClass.
 */
jint
JVM_GetCPMethodModifiers(JNIEnv *env, jclass cb, int index, jclass calledClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Releases the UTF string obtained from the VM.
 */
void
JVM_ReleaseUTF(const char *utf) {
    UNIMPLEMENTED();
}

/*
 * Compare if two classes are in the same package.
 */
jboolean
JVM_IsSameClassPackage(JNIEnv *env, jclass class1, jclass class2) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/* Constants in class files */

#define JVM_ACC_PUBLIC        0x0001  /* visible to everyone */
#define JVM_ACC_PRIVATE       0x0002  /* visible only to the defining class */
#define JVM_ACC_PROTECTED     0x0004  /* visible to subclasses */
#define JVM_ACC_STATIC        0x0008  /* instance variable is static */
#define JVM_ACC_FINAL         0x0010  /* no further subclassing, overriding */
#define JVM_ACC_SYNCHRONIZED  0x0020  /* wrap method call in monitor lock */
#define JVM_ACC_SUPER         0x0020  /* funky handling of invokespecial */
#define JVM_ACC_VOLATILE      0x0040  /* can not cache in registers */
#define JVM_ACC_BRIDGE        0x0040  /* bridge method generated by compiler */
#define JVM_ACC_TRANSIENT     0x0080  /* not persistent */
#define JVM_ACC_VARARGS       0x0080  /* method declared with variable number of args */
#define JVM_ACC_NATIVE        0x0100  /* implemented in C */
#define JVM_ACC_INTERFACE     0x0200  /* class is an interface */
#define JVM_ACC_ABSTRACT      0x0400  /* no definition provided */
#define JVM_ACC_STRICT          0x0800  /* strict floating point */
#define JVM_ACC_SYNTHETIC     0x1000  /* compiler-generated class, method or field */
#define JVM_ACC_ANNOTATION    0x2000  /* annotation type */
#define JVM_ACC_ENUM          0x4000  /* field is declared as element of enum */

#define JVM_ACC_PUBLIC_BIT        0
#define JVM_ACC_PRIVATE_BIT       1
#define JVM_ACC_PROTECTED_BIT     2
#define JVM_ACC_STATIC_BIT        3
#define JVM_ACC_FINAL_BIT         4
#define JVM_ACC_SYNCHRONIZED_BIT  5
#define JVM_ACC_SUPER_BIT         5
#define JVM_ACC_VOLATILE_BIT      6
#define JVM_ACC_BRIDGE_BIT        6
#define JVM_ACC_TRANSIENT_BIT     7
#define JVM_ACC_VARARGS_BIT       7
#define JVM_ACC_NATIVE_BIT        8
#define JVM_ACC_INTERFACE_BIT     9
#define JVM_ACC_ABSTRACT_BIT      10
#define JVM_ACC_STRICT_BIT      11
#define JVM_ACC_SYNTHETIC_BIT     12
#define JVM_ACC_ANNOTATION_BIT      13
#define JVM_ACC_ENUM_BIT          14

// NOTE: replicated in SA in vm/agent/sun/jvm/hotspot/utilities/ConstantTag.java
enum {
    JVM_CONSTANT_Utf8 = 1,
    JVM_CONSTANT_Unicode,        /* unused */
    JVM_CONSTANT_Integer,
    JVM_CONSTANT_Float,
    JVM_CONSTANT_Long,
    JVM_CONSTANT_Double,
    JVM_CONSTANT_Class,
    JVM_CONSTANT_String,
    JVM_CONSTANT_Fieldref,
    JVM_CONSTANT_Methodref,
    JVM_CONSTANT_InterfaceMethodref,
    JVM_CONSTANT_NameAndType
};

/* Used in the newarray instruction. */

#define JVM_T_BOOLEAN 4
#define JVM_T_CHAR    5
#define JVM_T_FLOAT   6
#define JVM_T_DOUBLE  7
#define JVM_T_BYTE    8
#define JVM_T_SHORT   9
#define JVM_T_INT    10
#define JVM_T_LONG   11

/* JVM method signatures */

#define JVM_SIGNATURE_ARRAY        '['
#define JVM_SIGNATURE_BYTE        'B'
#define JVM_SIGNATURE_CHAR        'C'
#define JVM_SIGNATURE_CLASS        'L'
#define JVM_SIGNATURE_ENDCLASS            ';'
#define JVM_SIGNATURE_ENUM        'E'
#define JVM_SIGNATURE_FLOAT        'F'
#define JVM_SIGNATURE_DOUBLE            'D'
#define JVM_SIGNATURE_FUNC        '('
#define JVM_SIGNATURE_ENDFUNC            ')'
#define JVM_SIGNATURE_INT        'I'
#define JVM_SIGNATURE_LONG        'J'
#define JVM_SIGNATURE_SHORT        'S'
#define JVM_SIGNATURE_VOID        'V'
#define JVM_SIGNATURE_BOOLEAN            'Z'

/*
 * A function defined by the byte-code verifier and called by the VM.
 * This is not a function implemented in the VM.
 *
 * Returns JNI_FALSE if verification fails. A detailed error message
 * will be places in msg_buf, whose length is specified by buf_len.
 */
typedef jboolean (*verifier_fn_t)(JNIEnv *env,
                  jclass cb,
                  char * msg_buf,
                  jint buf_len);


/*
 * Support for a VM-independent class format checker.
 */
typedef struct {
    unsigned long code;    /* byte code */
    unsigned long excs;    /* exceptions */
    unsigned long etab;    /* catch table */
    unsigned long lnum;    /* line number */
    unsigned long lvar;    /* local vars */
} method_size_info;

typedef struct {
    unsigned int constants;    /* constant pool */
    unsigned int fields;
    unsigned int methods;
    unsigned int interfaces;
    unsigned int fields2;      /* number of static 2-word fields */
    unsigned int innerclasses; /* # of records in InnerClasses attr */

    method_size_info clinit;   /* memory used in clinit */
    method_size_info main;     /* used everywhere else */
} class_size_info;

/*
 * Functions defined in libjava.so to perform string conversions.
 *
 */

typedef jstring (*to_java_string_fn_t)(JNIEnv *env, char *str);

typedef char *(*to_c_string_fn_t)(JNIEnv *env, jstring s, jboolean *b);

/* This is the function defined in libjava.so that performs class
 * format checks. This functions fills in size information about
 * the class file and returns:
 *
 *   0: good
 *  -1: out of memory
 *  -2: bad format
 *  -3: unsupported version
 *  -4: bad class name
 */

typedef jint (*check_format_fn_t)(char *class_name,
                  unsigned char *data,
                  unsigned int data_size,
                  class_size_info *class_size,
                  char *message_buffer,
                  jint buffer_length,
                  jboolean measure_only,
                  jboolean check_relaxed);

#define JVM_RECOGNIZED_CLASS_MODIFIERS (JVM_ACC_PUBLIC | \
                    JVM_ACC_FINAL | \
                    JVM_ACC_SUPER | \
                    JVM_ACC_INTERFACE | \
                    JVM_ACC_ABSTRACT | \
                    JVM_ACC_ANNOTATION | \
                    JVM_ACC_ENUM | \
                    JVM_ACC_SYNTHETIC)

#define JVM_RECOGNIZED_FIELD_MODIFIERS (JVM_ACC_PUBLIC | \
                    JVM_ACC_PRIVATE | \
                    JVM_ACC_PROTECTED | \
                    JVM_ACC_STATIC | \
                    JVM_ACC_FINAL | \
                    JVM_ACC_VOLATILE | \
                    JVM_ACC_TRANSIENT | \
                    JVM_ACC_ENUM | \
                    JVM_ACC_SYNTHETIC)

#define JVM_RECOGNIZED_METHOD_MODIFIERS (JVM_ACC_PUBLIC | \
                     JVM_ACC_PRIVATE | \
                     JVM_ACC_PROTECTED | \
                     JVM_ACC_STATIC | \
                     JVM_ACC_FINAL | \
                     JVM_ACC_SYNCHRONIZED | \
                     JVM_ACC_BRIDGE | \
                     JVM_ACC_VARARGS | \
                     JVM_ACC_NATIVE | \
                     JVM_ACC_ABSTRACT | \
                     JVM_ACC_STRICT | \
                     JVM_ACC_SYNTHETIC)

/*
 * This is the function defined in libjava.so to perform path
 * canonicalization. VM call this function before opening jar files
 * to load system classes.
 *
 */

typedef int (*canonicalize_fn_t)(JNIEnv *env, char *orig, char *out, int len);

/*************************************************************************
 PART 3: I/O and Network Support
 ************************************************************************/

/* Note that the JVM IO functions are expected to return JVM_IO_ERR
 * when there is any kind of error. The caller can then use the
 * platform specific support (e.g., errno) to get the detailed
 * error info.  The JVM_GetLastErrorString procedure may also be used
 * to obtain a descriptive error string.
 */
#define JVM_IO_ERR  (-1)

/* For interruptible IO. Returning JVM_IO_INTR indicates that an IO
 * operation has been disrupted by Thread.interrupt. There are a
 * number of technical difficulties related to interruptible IO that
 * need to be solved. For example, most existing programs do not handle
 * InterruptedIOExceptions specially, they simply treat those as any
 * IOExceptions, which typically indicate fatal errors.
 *
 * There are also two modes of operation for interruptible IO. In the
 * resumption mode, an interrupted IO operation is guaranteed not to
 * have any side-effects, and can be restarted. In the termination mode,
 * an interrupted IO operation corrupts the underlying IO stream, so
 * that the only reasonable operation on an interrupted stream is to
 * close that stream. The resumption mode seems to be impossible to
 * implement on Win32 and Solaris. Implementing the termination mode is
 * easier, but it's not clear that's the right semantics.
 *
 * Interruptible IO is not supported on Win32.It can be enabled/disabled
 * using a compile-time flag on Solaris. Third-party JVM ports do not
 * need to implement interruptible IO.
 */
#define JVM_IO_INTR (-2)

/* Write a string into the given buffer, in the platform's local encoding,
 * that describes the most recent system-level error to occur in this thread.
 * Return the length of the string or zero if no error occurred.
 */
jint JVM_GetLastErrorString(char *buffer, int length) {
    if (errno == 0) {
        return 0;
    }
#if os_DARWIN || os_SOLARIS
    return strerror_r(errno, buffer, length);
#elif os_LINUX
    return strlen(strerror_r(errno, buffer, length));
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

/*
 * Convert a pathname into native format.  This function does syntactic
 * cleanup, such as removing redundant separator characters.  It modifies
 * the given pathname string in place.
 */
char *JVM_NativePath(char *path) {
#if log_JVMNI
  log_println("JVM_NativePath(%s)", path);
#endif
    return path; // TODO
}

/*
 * JVM I/O error codes
 */
#define JVM_EEXIST       -100

/*
 * Open a file descriptor. This function returns a negative error code
 * on error, and a non-negative integer that is the file descriptor on
 * success.
 */
jint JVM_Open(char *fileName, jint flags, jint mode) {
#if log_JVMNI
    log_println("JVM_Open(%s)", fileName);
#endif
    int result = open(fileName, flags, mode);
    if (result >= 0) {
        return result;
    }
    if (errno == EEXIST) {
        return JVM_EEXIST;
    }
    return -1;
}

/*
 * Close a file descriptor. This function returns -1 on error, and 0
 * on success.
 *
 * fd        the file descriptor to close.
 */
jint JVM_Close(jint fd) {
    return close(fd);
}

/*
 * Read data from a file decriptor into a char array.
 *
 * fd        the file descriptor to read from.
 * buf       the buffer where to put the read data.
 * nbytes    the number of bytes to read.
 *
 * This function returns -1 on error, and 0 on success.
 */
jint
JVM_Read(jint fd, char *buf, jint nbytes) {
    return (jint) read(fd, buf, (size_t) nbytes);
}

/*
 * Write data from a char array to a file descriptor.
 *
 * fd        the file descriptor to read from.
 * buf       the buffer from which to fetch the data.
 * nbytes    the number of bytes to write.
 *
 * This function returns -1 on error, and 0 on success.
 */
jint
JVM_Write(jint fd, char *buf, jint nbytes) {
    return (jint) write(fd, buf, (size_t) nbytes);
}

/*
 * Returns the number of bytes available for reading from a given file
 * descriptor
 */
jint JVM_Available(jint fd, jlong *pbytes) {
    jlong cur, end;

    struct stat st;
    if (fstat(fd, &st) >= 0) {
        if (S_ISCHR(st.st_mode) || S_ISFIFO(st.st_mode) || S_ISSOCK(st.st_mode)) {
            int n;
            if (ioctl(fd, FIONREAD, &n) >= 0) {
                *pbytes = n;
                return 1;
            }
        }
    }
    if ((cur = lseek64(fd, 0L, SEEK_CUR)) == -1) {
        return 0;
    } else if ((end = lseek64(fd, 0L, SEEK_END)) == -1) {
        return 0;
    } else if (lseek64(fd, cur, SEEK_SET) == -1) {
        return 0;
    }
    *pbytes = end - cur;
    return 1;
}

/*
 * Move the file descriptor pointer from whence by offset.
 *
 * fd        the file descriptor to move.
 * offset    the number of bytes to move it by.
 * whence    the start from where to move it.
 *
 * This function returns the resulting pointer location.
 */
jlong
JVM_Lseek(jint fd, jlong offset, jint whence) {
    return lseek(fd, offset, whence);
}

/*
 * Set the length of the file associated with the given descriptor to the given
 * length.  If the new length is longer than the current length then the file
 * is extended the contents of the extended portion are not defined.  The
 * value of the file pointer is undefined after this procedure returns.
 */
jint
JVM_SetLength(jint fd, jlong length) {
    return ftruncate(fd, length);
}

/*
 * Synchronize the file descriptor's in memory state with that of the
 * physical device.  Return of -1 is an error, 0 is OK.
 */
jint
JVM_Sync(jint fd) {
    return fsync(fd);
}

/*
 * Networking library support
 */

jint
JVM_InitializeSocketLibrary(void) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return 0;
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

#if os_SOLARIS || os_LINUX || os_DARWIN
#include <sys/types.h>
#include <sys/socket.h>
#else
struct sockaddr;
#endif

jint
JVM_Socket(jint domain, jint type, jint protocol) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return socket(domain, type, protocol);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_SocketClose(jint fd) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return close(fd);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_SocketShutdown(jint fd, jint howto) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return shutdown(fd, howto);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Recv(jint fd, char *buf, jint nBytes, jint flags) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return recv(fd, buf, nBytes, flags);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Send(jint fd, char *buf, jint nBytes, jint flags) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return send(fd, buf, nBytes, flags);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Timeout(int fd, long timeout) {
    UNIMPLEMENTED();
    return 0;
}

jint
JVM_Listen(jint fd, jint count) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return listen(fd, count);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Connect(jint fd, struct sockaddr *him, jint len) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return connect(fd, him, len);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Bind(jint fd, struct sockaddr *him, jint len) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return bind(fd, him, len);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_Accept(jint fd, struct sockaddr *him, jint *len) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    if (fd < 0) {
        return -1;
    }
    return accept(fd, him, (socklen_t*) len);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_RecvFrom(jint fd, char *buf, int nBytes,
                  int flags, struct sockaddr *from, int *fromlen) {
#if os_SOLARIS
    return recvfrom(fd, buf, nBytes, (unsigned int) flags, from, (socklen_t *)fromlen);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_SendTo(jint fd, char *buf, int len,
                int flags, struct sockaddr *to, int tolen) {
#if os_SOLARIS
    return sendto(fd, buf, len, (unsigned int) flags, to, tolen);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

#if os_SOLARIS
#include <sys/filio.h>
#endif

jint
JVM_SocketAvailable(jint fd, jint *pbytes) {
#if os_SOLARIS
    if (fd < 0) {
        return 0;
    }

    int ret;
    RESTARTABLE(ioctl(fd, FIONREAD, pbytes), ret);

    // note ioctl can return 0 when successful, JVM_SocketAvailable
    // is expected to return 0 on failure and 1 on success to the jdk.
    return (ret == OS_ERR) ? 0 : 1;
#elif os_LINUX
    // Linux doc says EINTR not returned, unlike Solaris
    int ret = ioctl(fd, FIONREAD, pbytes);

    //%% note ioctl can return 0 when successful, JVM_SocketAvailable
    // is expected to return 0 on failure and 1 on success to the jdk.
    return (ret < 0) ? 0 : 1;
#else
    UNIMPLEMENTED();
    return 0;
#endif
}


jint
JVM_GetSockName(jint fd, struct sockaddr *him, int *len) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return getsockname(fd, him, (socklen_t*) len);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_GetSockOpt(jint fd, int level, int optname, char *optval, int *optlen) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return getsockopt(fd, level, optname, optval, (socklen_t*) optlen);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

jint
JVM_SetSockOpt(jint fd, int level, int optname, const char *optval, int optlen) {
#if os_SOLARIS || os_LINUX || os_DARWIN
    return setsockopt(fd, level, optname, optval, optlen);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

/*
 * These routines are only reentrant on Windows
 */

#ifdef _WINDOWS

struct protoent *
JVM_GetProtoByName(char* name) {
    UNIMPLEMENTED();
    return 0;
}

struct hostent*
JVM_GetHostByAddr(const char* name, int len, int type) {
    UNIMPLEMENTED();
    return 0;
}

struct hostent*
JVM_GetHostByName(char* name) {
    UNIMPLEMENTED();
    return 0;
}

#endif /* _WINDOWS */

int
JVM_GetHostName(char* name, int namelen) {
#if os_SOLARIS || os_LINUX
    return gethostname(name, namelen);
#else
    UNIMPLEMENTED();
    return 0;
#endif
}

/*
 * The standard printing functions supported by the Java VM. (Should they
 * be renamed to JVM_* in the future?
 */

/*
 * BE CAREFUL! The following functions do not implement the
 * full feature set of standard C printf formats.
 */
int
jio_vsnprintf(char *str, size_t count, const char *fmt, va_list args) {
    if ((intptr_t)count <= 0) {
        return -1;
    }
    return vsnprintf(str, count, fmt, args);
}

int
jio_snprintf(char *str, size_t count, const char *fmt, ...) {
    va_list args;
    int len;
    va_start(args, fmt);
    len = jio_vsnprintf(str, count, fmt, args);
    va_end(args);
    return len;
}

int
jio_fprintf(FILE *file, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vfprintf(file, fmt, ap);
    va_end(ap);
    return 0;
}

int
jio_vfprintf(FILE *file, const char *fmt, va_list args) {
    vfprintf(file, fmt, args);
    return 0;
}

void *JVM_RawMonitorCreate(void) {
    Mutex mutex = (Mutex) calloc(sizeof(*mutex), 1);
    mutex_initialize(mutex);
    return mutex;
}


void JVM_RawMonitorDestroy(void *monitor) {
    mutex_dispose((Mutex) monitor);
    free(monitor);
}

jint JVM_RawMonitorEnter(void *monitor) {
    return mutex_enter((Mutex) monitor);
}

void JVM_RawMonitorExit(void *monitor) {
    mutex_exit((Mutex) monitor);
}

/*
 * java.lang.management support
 */
void *JVM_GetManagement(jint version) {
    void* getJMMInterface(int version);
    return getJMMInterface(version);
}

/*
 * com.sun.tools.attach.VirtualMachine support
 *
 * Initialize the agent properties with the properties maintained in the VM.
 */
jobject
JVM_InitAgentProperties(JNIEnv *env, jobject agent_props) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "InitAgentProperties", "(Ljava/util/Properties;)Ljava/util/Properties;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod, agent_props);
}

/* Generics reflection support.
 *
 * Returns information about the given class's EnclosingMethod
 * attribute, if present, or null if the class had no enclosing
 * method.
 *
 * If non-null, the returned array contains three elements. Element 0
 * is the java.lang.Class of which the enclosing method is a member,
 * and elements 1 and 2 are the java.lang.Strings for the enclosing
 * method's name and descriptor, respectively.
 */
jobjectArray
JVM_GetEnclosingMethodInfo(JNIEnv* env, jclass ofClass) {
    UNIMPLEMENTED_WITH_ENV();
    return 0;
}

/*
 * Java thread state support
 */
enum {
    JAVA_THREAD_STATE_NEW           = 0,
    JAVA_THREAD_STATE_RUNNABLE      = 1,
    JAVA_THREAD_STATE_BLOCKED       = 2,
    JAVA_THREAD_STATE_WAITING       = 3,
    JAVA_THREAD_STATE_TIMED_WAITING = 4,
    JAVA_THREAD_STATE_TERMINATED    = 5,
    JAVA_THREAD_STATE_COUNT         = 6
};

/*
 * Returns an array of the threadStatus values representing the
 * given Java thread state.  Returns NULL if the VM version is
 * incompatible with the JDK or doesn't support the given
 * Java thread state.
 */
jintArray
JVM_GetThreadStateValues(JNIEnv* env, jint javaThreadState) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetThreadStateValues", "(I)[I");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod, javaThreadState);
}

/*
 * Returns an array of the substate names representing the
 * given Java thread state.  Returns NULL if the VM version is
 * incompatible with the JDK or the VM doesn't support
 * the given Java thread state.
 * values must be the jintArray returned from JVM_GetThreadStateValues
 * and javaThreadState.
 */
jobjectArray
JVM_GetThreadStateNames(JNIEnv* env, jint javaThreadState, jintArray values) {
    JNIMethod result = resolveCriticalStaticMethod(env, "com/sun/max/vm/jni/JVMFunctions", "GetThreadStateNames", "(I[I)[Ljava/lang/String;");
    return (*env)->CallStaticObjectMethod(env, result.jClass, result.jMethod, javaThreadState, values);
}

/* =========================================================================
 * The following defines a private JVM interface that the JDK can query
 * for the JVM version and capabilities.  sun.misc.Version defines
 * the methods for getting the VM version and its capabilities.
 *
 * When a new bit is added, the following should be updated to provide
 * access to the new capability:
 *    HS:   JVM_GetVersionInfo and Abstract_VM_Version class
 *    SDK:  Version class
 *
 * Similary, a private JDK interface JDK_GetVersionInfo0 is defined for
 * JVM to query for the JDK version and capabilities.
 *
 * When a new bit is added, the following should be updated to provide
 * access to the new capability:
 *    HS:   JDK_Version class
 *    SDK:  JDK_GetVersionInfo0
 *
 * ==========================================================================
 */
typedef struct {
    /* Naming convention of RE build version string: n.n.n[_uu[c]][-<identifier>]-bxx */
    unsigned int jvm_version;   /* Consists of major, minor, micro (n.n.n) */
                                /* and build number (xx) */
    unsigned int update_version : 8;         /* Update release version (uu) */
    unsigned int special_update_version : 8; /* Special update release version (c) */
    unsigned int reserved1 : 16;
    unsigned int reserved2;

    /* The following bits represents JVM supports that JDK has dependency on.
     * JDK can use these bits to determine which JVM version
     * and support it has to maintain runtime compatibility.
     *
     * When a new bit is added in a minor or update release, make sure
     * the new bit is also added in the main/baseline.
     */
    unsigned int is_attachable : 1;
    unsigned int : 31;
    unsigned int : 32;
    unsigned int : 32;
} jvm_version_info;

#define JVM_VERSION_MAJOR(version) ((version & 0xFF000000) >> 24)
#define JVM_VERSION_MINOR(version) ((version & 0x00FF0000) >> 16)
#define JVM_VERSION_MICRO(version) ((version & 0x0000FF00) >> 8)

/* Build number is available only for RE builds.
 * It will be zero for internal builds.
 */
#define JVM_VERSION_BUILD(version) ((version & 0x000000FF))

void
JVM_GetVersionInfo(JNIEnv* env, jvm_version_info* info, size_t info_size);

typedef struct {
    // Naming convention of RE build version string: n.n.n[_uu[c]][-<identifier>]-bxx
    unsigned int jdk_version;   /* Consists of major, minor, micro (n.n.n) */
                                /* and build number (xx) */
    unsigned int update_version : 8;         /* Update release version (uu) */
    unsigned int special_update_version : 8; /* Special update release version (c)*/
    unsigned int reserved1 : 16;
    unsigned int reserved2;

    /* The following bits represents new JDK supports that VM has dependency on.
     * VM implementation can use these bits to determine which JDK version
     * and support it has to maintain runtime compatibility.
     *
     * When a new bit is added in a minor or update release, make sure
     * the new bit is also added in the main/baseline.
     */
    unsigned int thread_park_blocker : 1;
    unsigned int : 31;
    unsigned int : 32;
    unsigned int : 32;
} jdk_version_info;

#define JDK_VERSION_MAJOR(version) ((version & 0xFF000000) >> 24)
#define JDK_VERSION_MINOR(version) ((version & 0x00FF0000) >> 16)
#define JDK_VERSION_MICRO(version) ((version & 0x0000FF00) >> 8)

/* Build number is available only for RE build (i.e. JDK_BUILD_NUMBER is set to bNN)
 * It will be zero for internal builds.
 */
#define JDK_VERSION_BUILD(version) ((version & 0x000000FF))

/*
 * This is the function JDK_GetVersionInfo0 defined in libjava.so
 * that is dynamically looked up by JVM.
 */
typedef void (*jdk_version_info_fn_t)(jdk_version_info* info, size_t info_size);

