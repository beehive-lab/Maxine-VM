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

import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.jni.JniHandles.*;
import static com.sun.max.vm.jni.VMFunctions.*;

import java.security.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.VMFunctions.LatestUserDefinedLoaderVisitor;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;
import com.sun.max.vm.type.*;

/**
 * Source for the VM interface functions defined in {@link VMFunctions}.
 */
public class VMFunctionsSource {

    // Checkstyle: stop method name check

    @VM_ENTRY_POINT
    private static void Unimplemented(Pointer env) {
        throw FatalError.unimplemented();
    }

    @VM_ENTRY_POINT
    private static int HashCode(Pointer env, JniHandle obj) {
        return obj.unhand().hashCode();
    }

    @VM_ENTRY_POINT
    private static void MonitorWait(Pointer env, JniHandle obj, long timeout) throws InterruptedException {
        obj.unhand().wait(timeout);
    }

    @VM_ENTRY_POINT
    private static void MonitorNotify(Pointer env, JniHandle obj) {
        obj.unhand().notify();
    }

    @VM_ENTRY_POINT
    private static void MonitorNotifyAll(Pointer env, JniHandle obj) {
        obj.unhand().notifyAll();
    }

    @VM_ENTRY_POINT
    private static JniHandle Clone(Pointer env, JniHandle obj) throws CloneNotSupportedException {
        if (obj.unhand() instanceof Cloneable) {
            return createLocalHandle(Heap.clone(obj.unhand()));
        }
        throw new CloneNotSupportedException();
    }

    @VM_ENTRY_POINT
    private static JniHandle InternString(Pointer env, JniHandle s) {
        return createLocalHandle(s.unhand(String.class).intern());
    }

    @VM_ENTRY_POINT
    private static void Exit(Pointer env, int code) {
        System.exit(code);
    }

    @VM_ENTRY_POINT
    private static void Halt(Pointer env, int code) {
        MaxineVM.exit(code);
    }

    @VM_ENTRY_POINT
    private static void GC(Pointer env) {
        System.gc();
    }

    @VM_ENTRY_POINT
    private static long MaxObjectInspectionAge(Pointer env) {
        return Heap.maxObjectInspectionAge();
    }

    @VM_ENTRY_POINT
    private static long FreeMemory(Pointer env) {
        return Heap.reportFreeSpace();
    }

    @VM_ENTRY_POINT
    private static long MaxMemory(Pointer env) {
        return Heap.maxSizeLong();
    }

    @VM_ENTRY_POINT
    private static void FillInStackTrace(Pointer env, JniHandle throwable) {
        throwable.unhand(Throwable.class).fillInStackTrace();
    }

    @VM_ENTRY_POINT
    private static int GetStackTraceDepth(Pointer env, JniHandle throwable) {
        return JDK_java_lang_Throwable.asJLT(throwable.unhand(Throwable.class)).getStackTraceDepth();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetStackTraceElement(Pointer env, JniHandle throwable, int index) {
        return createLocalHandle(JDK_java_lang_Throwable.asJLT(throwable.unhand(Throwable.class)).getStackTraceElement(index));
    }

    @VM_ENTRY_POINT
    private static void StartThread(Pointer env, JniHandle thread) {
        thread.unhand(Thread.class).start();
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void StopThread(Pointer env, JniHandle thread, JniHandle throwable) {
        thread.unhand(Thread.class).stop(throwable.unhand(Throwable.class));
    }

    @VM_ENTRY_POINT
    private static boolean IsThreadAlive(Pointer env, JniHandle thread) {
        return thread.unhand(Thread.class).isAlive();
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void SuspendThread(Pointer env, JniHandle thread) {
        thread.unhand(Thread.class).suspend();
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static void ResumeThread(Pointer env, JniHandle thread) {
        thread.unhand(Thread.class).resume();
    }

    @VM_ENTRY_POINT
    private static void SetThreadPriority(Pointer env, JniHandle thread, int newPriority) {
        thread.unhand(Thread.class).setPriority(newPriority);
    }

    @VM_ENTRY_POINT
    private static void Yield(Pointer env) {
        Thread.yield();
    }

    @VM_ENTRY_POINT
    private static void Sleep(Pointer env, long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @VM_ENTRY_POINT
    private static JniHandle CurrentThread(Pointer env) {
        return createLocalHandle(Thread.currentThread());
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static int CountStackFrames(Pointer env, JniHandle thread) {
        return thread.unhand(Thread.class).countStackFrames();
    }

    @VM_ENTRY_POINT
    private static void Interrupt(Pointer env, JniHandle thread) {
        thread.unhand(Thread.class).interrupt();
    }

    @VM_ENTRY_POINT
    private static boolean IsInterrupted(Pointer env, JniHandle thread) {
        return thread.unhand(Thread.class).isInterrupted();
    }

    @VM_ENTRY_POINT
    private static boolean HoldsLock(Pointer env, JniHandle obj) {
        return Thread.holdsLock(obj.unhand());
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassContext(Pointer env) {
        return createLocalHandle(getClassContext());
    }

    @SuppressWarnings("deprecation")
    @VM_ENTRY_POINT
    private static JniHandle GetCallerClass(Pointer env, int depth) {
        // Additionally ignore this method, as well as the Reflection method we call.
        return createLocalHandle(Reflection.getCallerClass(depth + 2));
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSystemPackage(Pointer env, JniHandle name) {
        return createLocalHandle(BootClassLoader.BOOT_CLASS_LOADER.packageSource(name.unhand(String.class)));
    }

    @VM_ENTRY_POINT
    private static JniHandle GetSystemPackages(Pointer env) {
        return createLocalHandle(BootClassLoader.BOOT_CLASS_LOADER.packageNames());
    }

    @VM_ENTRY_POINT
    private static JniHandle LatestUserDefinedLoader(Pointer env) {
        LatestUserDefinedLoaderVisitor visitor = new LatestUserDefinedLoaderVisitor();
        new VmStackFrameWalker(VmThread.current().tla()).inspect(Pointer.fromLong(here()),
            VMRegister.getCpuStackPointer(),
            VMRegister.getCpuFramePointer(),
            visitor);
        return createLocalHandle(visitor.result);
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassName(Pointer env, JniHandle c) {
        return createLocalHandle(c.unhand(Class.class).getName());
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassLoader(Pointer env, JniHandle c) {
        return createLocalHandle(c.unhand(Class.class).getClassLoader());
    }

    @VM_ENTRY_POINT
    private static boolean IsInterface(Pointer env, JniHandle c) {
        return c.unhand(Class.class).isInterface();
    }

    @VM_ENTRY_POINT
    private static boolean IsArrayClass(Pointer env, JniHandle c) {
        return c.unhand(Class.class).isArray();
    }

    @VM_ENTRY_POINT
    private static boolean IsPrimitiveClass(Pointer env, JniHandle c) {
        return c.unhand(Class.class).isPrimitive();
    }

    @VM_ENTRY_POINT
    private static JniHandle GetClassSigners(Pointer env, JniHandle c) {
        return createLocalHandle(c.unhand(Class.class).getSigners());
    }

    @VM_ENTRY_POINT
    private static void SetClassSigners(Pointer env, JniHandle c, JniHandle signers) {
        final ClassActor classActor = ClassActor.fromJava(c.unhand(Class.class));
        classActor.signers = signers.unhand(Object[].class);
    }

    @VM_ENTRY_POINT
    private static JniHandle GetProtectionDomain(Pointer env, JniHandle c) {
        return createLocalHandle(c.unhand(Class.class).getProtectionDomain());
    }

    @VM_ENTRY_POINT
    private static void SetProtectionDomain(Pointer env, JniHandle c, JniHandle pd) {
        ClassActor.fromJava(c.unhand(Class.class)).setProtectionDomain(pd.unhand(ProtectionDomain.class));
    }

    @VM_ENTRY_POINT
    private static void ArrayCopy(Pointer env, JniHandle src, int srcPos, JniHandle dest, int destPos, int length) {
        System.arraycopy(src.unhand(), srcPos, dest.unhand(), destPos, length);
    }

    @VM_ENTRY_POINT
    private static JniHandle GetAllThreads(Pointer env) {
        return createLocalHandle(VmThreadMap.getThreads(false));
    }

    @VM_ENTRY_POINT
    private static JniHandle GetThreadStateValues(Pointer env, int javaThreadState) {
        // 1-1
        final int[] result = new int[1];
        result[0] = javaThreadState;
        return createLocalHandle(result);
    }

    @VM_ENTRY_POINT
    private static JniHandle GetThreadStateNames(Pointer env, int javaThreadState, JniHandle threadStateValues) {
        assert threadStateValues.unhand(int[].class).length == 1;
        // 1-1
        final String[] result = new String[1];
        final Thread.State[] ts = Thread.State.values();
        result[0] = ts[javaThreadState].name();
        return createLocalHandle(result);
    }

    @VM_ENTRY_POINT
    private static JniHandle InitAgentProperties(Pointer env, JniHandle props) {
        Properties p = props.unhand(Properties.class);
        // sun.jvm.args, sun.jvm.flags, sun.java.command
        p.put("sun.jvm.args", VMOptions.getVmArguments());
        p.put("sun.jvm.flags", "");
        p.put("sun.java.command", VMOptions.mainClassAndArguments());
        return props;
    }

    @VM_ENTRY_POINT
    private static int GetNumberOfArguments(Pointer env, MethodID methodID) throws NoSuchMethodException {
        final MethodActor methodActor = MethodID.toMethodActor( methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        return methodActor.descriptor().numberOfParameters();
    }

    @VM_ENTRY_POINT
    private static void GetKindsOfArguments(Pointer env, MethodID methodID, Pointer kinds) throws Exception {
        final MethodActor methodActor = MethodID.toMethodActor(methodID);
        if (methodActor == null) {
            throw new NoSuchMethodException();
        }
        final SignatureDescriptor signature = methodActor.descriptor();
        for (int i = 0; i < signature.numberOfParameters(); ++i) {
            final Kind kind = signature.parameterDescriptorAt(i).toKind();
            kinds.setByte(i, (byte) kind.asEnum.ordinal());
        }
    }

    @VM_ENTRY_POINT
    private static void SetJVMTIEnv(Pointer env, Pointer jvmtiEnv) {
        VMTI.handler().registerAgent(jvmtiEnv);
    }

    // Checkstyle: resume method name check
}
