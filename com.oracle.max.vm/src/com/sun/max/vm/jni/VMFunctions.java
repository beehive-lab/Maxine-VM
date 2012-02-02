/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jni;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.jni.JniFunctions.*;
import static com.sun.max.vm.jni.JniHandles.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A collection of VM functions that can be called from C. Some of these functions
 * implements (part of) the Maxine version of the JVM_* interface which the VM presents
 * to the JDK's native code.
 * <p>
 * <b>DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.</b>
 * <p>
 * Instead, modify the corresponding source in VMFunctionsSource.java denoted by the "// Source: ..." comments.
 * Once finished with editing, execute 'mx jnigen' to refresh this file.
 */
public class VMFunctions {

    // Checkstyle: stop method name check

    public static void Unimplemented() {
        throw FatalError.unimplemented();
    }

    static class ClassContext extends SourceFrameVisitor {
        boolean skippingUntilNativeMethod;
        ArrayList<Class> classes = new ArrayList<Class>(20);

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (!skippingUntilNativeMethod) {
                if (method.holder().isReflectionStub() || method.isNative()) {
                    // ignore reflection stubs and native methods (according to JVM_GetClassContext in HotSpot)
                } else {
                    classes.add(method.holder().toJava());
                }
            } else {
                if (method.isNative()) {
                    skippingUntilNativeMethod = false;
                }
            }
            return true;
        }
    }

    public static Class[] getClassContext() {
        ClassContext classContext = new ClassContext();

        // In Maxine VE there are no native frames, or JNI calls on the stack that need to be ignored
        classContext.skippingUntilNativeMethod = platform().os != OS.MAXVE;

        classContext.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        ArrayList<Class> classes = classContext.classes;
        return classContext.classes.toArray(new Class[classes.size()]);
    }

    static final CriticalMethod javaLangReflectMethodInvoke = new CriticalMethod(Method.class, "invoke",
        SignatureDescriptor.create(Object.class, Object.class, Object[].class));

    static class LatestUserDefinedLoaderVisitor extends RawStackFrameVisitor {
        ClassLoader result;
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            TargetMethod targetMethod = current.targetMethod();
            if (current.isTopFrame() || targetMethod == null || targetMethod.classMethodActor == null || targetMethod.classMethodActor() == javaLangReflectMethodInvoke.classMethodActor) {
                return true;
            }
            final ClassLoader cl = targetMethod.classMethodActor().holder().classLoader;
            if (cl != null && cl != BootClassLoader.BOOT_CLASS_LOADER) {
                result = cl;
                return false;
            }
            return true;
        }
    }

    /*
     * DO NOT EDIT CODE BETWEEN "START GENERATED CODE" AND "END GENERATED CODE" IN THIS FILE.
     *
     * Instead, modify the corresponding source in VMFunctionsSource.java denoted by the "// Source: ..." comments.
     * Once finished with editing, execute JniFunctionsGenerator as a Java application to refresh this file.
     */

// START GENERATED CODE

    private static final boolean INSTRUMENTED = false;

    @VM_ENTRY_POINT
    private static void Unimplemented(Pointer env) {
        // Source: VMFunctionsSource.java:56
        Pointer anchor = prologue(env);
        // Log entry
        try {
            throw FatalError.unimplemented();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static int HashCode(Pointer env, JniHandle obj) {
        // Source: VMFunctionsSource.java:61
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return obj.unhand().hashCode();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void MonitorWait(Pointer env, JniHandle obj, long timeout) throws InterruptedException {
        // Source: VMFunctionsSource.java:66
        Pointer anchor = prologue(env);
        // Log entry
        try {
            obj.unhand().wait(timeout);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void MonitorNotify(Pointer env, JniHandle obj) {
        // Source: VMFunctionsSource.java:71
        Pointer anchor = prologue(env);
        // Log entry
        try {
            obj.unhand().notify();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void MonitorNotifyAll(Pointer env, JniHandle obj) {
        // Source: VMFunctionsSource.java:76
        Pointer anchor = prologue(env);
        // Log entry
        try {
            obj.unhand().notifyAll();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle Clone(Pointer env, JniHandle obj) throws CloneNotSupportedException {
        // Source: VMFunctionsSource.java:81
        Pointer anchor = prologue(env);
        // Log entry
        try {
            if (obj.unhand() instanceof Cloneable) {
                return createLocalHandle(Heap.clone(obj.unhand()));
            }
            throw new CloneNotSupportedException();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle InternString(Pointer env, JniHandle s) {
        // Source: VMFunctionsSource.java:89
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(s.unhand(String.class).intern());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void Exit(Pointer env, int code) {
        // Source: VMFunctionsSource.java:94
        Pointer anchor = prologue(env);
        // Log entry
        try {
            System.exit(code);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void Halt(Pointer env, int code) {
        // Source: VMFunctionsSource.java:99
        Pointer anchor = prologue(env);
        // Log entry
        try {
            MaxineVM.exit(code);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void GC(Pointer env) {
        // Source: VMFunctionsSource.java:104
        Pointer anchor = prologue(env);
        // Log entry
        try {
            System.gc();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static long MaxObjectInspectionAge(Pointer env) {
        // Source: VMFunctionsSource.java:109
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return Heap.maxObjectInspectionAge();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static long FreeMemory(Pointer env) {
        // Source: VMFunctionsSource.java:114
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return Heap.reportFreeSpace();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static long MaxMemory(Pointer env) {
        // Source: VMFunctionsSource.java:119
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return Heap.maxSizeLong();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void FillInStackTrace(Pointer env, JniHandle throwable) {
        // Source: VMFunctionsSource.java:124
        Pointer anchor = prologue(env);
        // Log entry
        try {
            throwable.unhand(Throwable.class).fillInStackTrace();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static int GetStackTraceDepth(Pointer env, JniHandle throwable) {
        // Source: VMFunctionsSource.java:129
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return JDK_java_lang_Throwable.asJLT(throwable.unhand(Throwable.class)).getStackTraceDepth();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStackTraceElement(Pointer env, JniHandle throwable, int index) {
        // Source: VMFunctionsSource.java:134
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(JDK_java_lang_Throwable.asJLT(throwable.unhand(Throwable.class)).getStackTraceElement(index));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void StartThread(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:139
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).start();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void StopThread(Pointer env, JniHandle thread, JniHandle throwable) {
        // Source: VMFunctionsSource.java:145
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).stop(throwable.unhand(Throwable.class));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsThreadAlive(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:150
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return thread.unhand(Thread.class).isAlive();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void SuspendThread(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:156
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).suspend();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void ResumeThread(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:162
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).resume();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void SetThreadPriority(Pointer env, JniHandle thread, int newPriority) {
        // Source: VMFunctionsSource.java:167
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).setPriority(newPriority);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void Yield(Pointer env) {
        // Source: VMFunctionsSource.java:172
        Pointer anchor = prologue(env);
        // Log entry
        try {
            Thread.yield();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void Sleep(Pointer env, long millis) throws InterruptedException {
        // Source: VMFunctionsSource.java:177
        Pointer anchor = prologue(env);
        // Log entry
        try {
            Thread.sleep(millis);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle CurrentThread(Pointer env) {
        // Source: VMFunctionsSource.java:182
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(Thread.currentThread());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static int CountStackFrames(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:188
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return thread.unhand(Thread.class).countStackFrames();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void Interrupt(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:193
        Pointer anchor = prologue(env);
        // Log entry
        try {
            thread.unhand(Thread.class).interrupt();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsInterrupted(Pointer env, JniHandle thread) {
        // Source: VMFunctionsSource.java:198
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return thread.unhand(Thread.class).isInterrupted();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean HoldsLock(Pointer env, JniHandle obj) {
        // Source: VMFunctionsSource.java:203
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return Thread.holdsLock(obj.unhand());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassContext(Pointer env) {
        // Source: VMFunctionsSource.java:208
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(getClassContext());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetCallerClass(Pointer env, int depth) {
        // Source: VMFunctionsSource.java:213
        Pointer anchor = prologue(env);
        // Log entry
        try {
            // Additionally ignore this method
            return createLocalHandle(Reflection.getCallerClass(depth + 1));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSystemPackage(Pointer env, JniHandle name) {
        // Source: VMFunctionsSource.java:219
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(BootClassLoader.BOOT_CLASS_LOADER.packageSource(name.unhand(String.class)));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSystemPackages(Pointer env) {
        // Source: VMFunctionsSource.java:224
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(BootClassLoader.BOOT_CLASS_LOADER.packageNames());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle LatestUserDefinedLoader(Pointer env) {
        // Source: VMFunctionsSource.java:229
        Pointer anchor = prologue(env);
        // Log entry
        try {
            LatestUserDefinedLoaderVisitor visitor = new LatestUserDefinedLoaderVisitor();
            new VmStackFrameWalker(VmThread.current().tla()).inspect(Pointer.fromLong(here()),
                VMRegister.getCpuStackPointer(),
                VMRegister.getCpuFramePointer(),
                visitor);
            return createLocalHandle(visitor.result);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassName(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:239
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(c.unhand(Class.class).getName());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassLoader(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:244
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(c.unhand(Class.class).getClassLoader());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsInterface(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:249
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return c.unhand(Class.class).isInterface();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsArrayClass(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:254
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return c.unhand(Class.class).isArray();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static boolean IsPrimitiveClass(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:259
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return c.unhand(Class.class).isPrimitive();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return false;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassSigners(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:264
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(c.unhand(Class.class).getSigners());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void SetClassSigners(Pointer env, JniHandle c, JniHandle signers) {
        // Source: VMFunctionsSource.java:269
        Pointer anchor = prologue(env);
        // Log entry
        try {
            final ClassActor classActor = ClassActor.fromJava(c.unhand(Class.class));
            classActor.signers = signers.unhand(Object[].class);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetProtectionDomain(Pointer env, JniHandle c) {
        // Source: VMFunctionsSource.java:275
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(c.unhand(Class.class).getProtectionDomain());
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void SetProtectionDomain(Pointer env, JniHandle c, JniHandle pd) {
        // Source: VMFunctionsSource.java:280
        Pointer anchor = prologue(env);
        // Log entry
        try {
            ClassActor.fromJava(c.unhand(Class.class)).setProtectionDomain(pd.unhand(ProtectionDomain.class));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void ArrayCopy(Pointer env, JniHandle src, int srcPos, JniHandle dest, int destPos, int length) {
        // Source: VMFunctionsSource.java:285
        Pointer anchor = prologue(env);
        // Log entry
        try {
            System.arraycopy(src.unhand(), srcPos, dest.unhand(), destPos, length);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetAllThreads(Pointer env) {
        // Source: VMFunctionsSource.java:290
        Pointer anchor = prologue(env);
        // Log entry
        try {
            return createLocalHandle(VmThreadMap.getThreads(false));
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetThreadStateValues(Pointer env, int javaThreadState) {
        // Source: VMFunctionsSource.java:295
        Pointer anchor = prologue(env);
        // Log entry
        try {
            // 1-1
            final int[] result = new int[1];
            result[0] = javaThreadState;
            return createLocalHandle(result);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle GetThreadStateNames(Pointer env, int javaThreadState, JniHandle threadStateValues) {
        // Source: VMFunctionsSource.java:303
        Pointer anchor = prologue(env);
        // Log entry
        try {
            assert threadStateValues.unhand(int[].class).length == 1;
            // 1-1
            final String[] result = new String[1];
            final Thread.State[] ts = Thread.State.values();
            result[0] = ts[javaThreadState].name();
            return createLocalHandle(result);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static JniHandle InitAgentProperties(Pointer env, JniHandle props) {
        // Source: VMFunctionsSource.java:313
        Pointer anchor = prologue(env);
        // Log entry
        try {
            Properties p = props.unhand(Properties.class);
            // sun.jvm.args, sun.jvm.flags, sun.java.command
            p.put("sun.jvm.args", VMOptions.getVmArguments());
            p.put("sun.jvm.flags", "");
            p.put("sun.java.command", VMOptions.mainClassAndArguments());
            return props;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return asJniHandle(0);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws NoSuchMethodException {
        // Source: VMFunctionsSource.java:323
        Pointer anchor = prologue(env);
        // Log entry
        try {
            final MethodActor methodActor = MethodID.toMethodActor( methodID);
            if (methodActor == null) {
                throw new NoSuchMethodException();
            }
            return methodActor.descriptor().numberOfParameters();
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
        // Source: VMFunctionsSource.java:332
        Pointer anchor = prologue(env);
        // Log entry
        try {
            final MethodActor methodActor = MethodID.toMethodActor(methodID);
            if (methodActor == null) {
                throw new NoSuchMethodException();
            }
            final SignatureDescriptor signature = methodActor.descriptor();
            for (int i = 0; i < signature.numberOfParameters(); ++i) {
                final Kind kind = signature.parameterDescriptorAt(i).toKind();
                kinds.setByte(i, (byte) kind.asEnum.ordinal());
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    @VM_ENTRY_POINT
    private static void SetJVMTIEnv(Pointer env, Pointer jvmtiEnv) {
        // Source: VMFunctionsSource.java:345
        Pointer anchor = prologue(env);
        // Log entry
        try {
            JVMTI.setJVMTIEnv(jvmtiEnv);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor);
        // Log exit
        }
    }

    // Checkstyle: resume method name check
// END GENERATED CODE

}
