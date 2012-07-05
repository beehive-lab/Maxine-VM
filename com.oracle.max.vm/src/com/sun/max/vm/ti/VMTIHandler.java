/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ti;

import java.security.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.thread.*;

/**
 * This a one-way interface between Maxine and any tooling systems, e.g. JVMTI, included in the VM.
 * It is intended to make the Maxine code base independent of the details of the tooling system.
 * In that sense it is similar to the compiler-runtime interface, {@link com.sun.cri}, except that
 * it is one-way, i.e., the interface is for Maxine and there is no intent to provide
 * a VM independent interface for the tooling system.
 *
 * The tooling system must implement and {@link VMTI#registerEventHandler register} an instance
 * of this class to handle events and queries from Maxine.
 */
public interface VMTIHandler {
    /**
     * Called very early in the VM startup to allow the tooling system to perform its initialization,
     * and perhaps influence the way the VM itself initializes, e.g, by changing options.
     * The call is made in {@link Phase#PRISTINE} mode in the main thread, after
     * the {@link Phase#PRISTINE} options have been parsed, but before the schemes
     * have been initialized in {@link Phase#PRISTINE} mode. In particular, the
     * the heap is <b>not</b> available.
     *
     */
    void initialize();

    /**
     * Called when the VM is initialized and fully functional.
     */
    void vmInitialized();

    /**
     * Called just before the VM is terminating.
     */
    void vmDeath();

    /**
     * Called when {@code vmThread} is about to start executing.
     * @param vmThread
     */
    void threadStart(VmThread vmThread);

    /**
     * Called when {@code vmThread} is about to terminate.
     * @param vmThread
     */
    void threadEnd(VmThread vmThread);

    /**
     * A check by Maxine if the tooling system wants to handle class file load hooks.
     * @return {@code true} iff the tooling system wants to handle class file load hooks.
     */
    boolean classFileLoadHookHandled();

    /**
     * If the tooling system returned {@code true} to {@link #classFileLoadHookHandled()}
     * this is invoked to allow the tooling system to modify the class file data before loading.
     * @param classLoader
     * @param className
     * @param protectionDomain
     * @param classfileBytes
     * @return the modified class file bytes or {@code null} if no change was made.
     */
    byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain,
                    byte[] classfileBytes);

    /**
     *
     * @param classActor
     */
    void classLoad(ClassActor classActor);

    /**
     * The given method was compiled and the code loaded into memory.
     * @param classMethodActor
     */
    void methodCompiled(ClassMethodActor classMethodActor);

    /**
     * The code for the given method was unloaded (garbage collected).
     * @param classMethodActor
     */
    void methodUnloaded(ClassMethodActor classMethodActor);

    /**
     * A GC is about to begin.
     */
    void beginGC();

    /**
     * A GC has ended.
     */
    void endGC();

    /**
     * Indicates that the object at address {@code cell} is a survivor of the GC in progress, i.e. remains live.
     * The implementation of this method should be fast and <b>must not</b> allocate.
     * @param cell
     */
    void objectSurviving(Pointer cell);

    /**
     * An exception is being raised.
     * @param throwable the {@link Throwable} being raised
     * @param sp stackpointer
     * @param fp framepointer
     * @param ip instruction pointer
     */
    void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip);


    /**
     * Allows the tooling system to register a special (baseline) compiler to use
     * when {@link #needsVMTICompilation(ClassMethodActor)} returns {@code true}.
     * @param stdRuntimeCompiler the default compiler
     * @return a new runtime compiler if {@code null} if not required.
     */
    RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler);

    /**
     * Checks whether the given method should be compiled with the compiler
     * registered with {@link #runtimeCompiler(RuntimeCompiler)}.
     * @param classMethodActor
     */
    boolean needsVMTICompilation(ClassMethodActor classMethodActor);

    /**
     * If the tooling system supports compiled code breakpoints, checks whether
     * the given method has any set.
     * @param classMethodActor
     */
    boolean hasBreakpoints(ClassMethodActor classMethodActor);

    /**
     * A string to be appended to the bootclasspath before the VM starts up.
     * @return string to append or {@code null} if none.
     */
    String bootclassPathExtension();

    /**
     * An upcall is being made into the VM from native code.
     */
    void beginUpcallVM();

    /**
     * An upcall from native code is returning.
     */
    void endUpcallVM();

    // Following are really JVMTI specific.

    /**
     * An agent associated with the tooling system is registering itself.
     * @param agentHandle
     */
    void registerAgent(Word agentHandle);

    /**
     * Check if given native method needs special compilation treatment.
     * JVMTI specific.
     * @param ma
     */
    boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma);

    /**
     * A check whether the custom {@link #getCallerClassForFindClass(int)} needs to be called.
     */
    boolean needsSpecialGetCallerClass();

    /**
     * A hook to override the default implementation of the getting the caller of {@code JNI} {@code FindClass}.
     * JVMTI specific.
     * @see sun.reflect.Reflection#getCallerClass(int)
     * @param realFramesToSkip
     * @return {@code null} if not implemented, or the caller class
     */
    Class getCallerClassForFindClass(int realFramesToSkip);

}
