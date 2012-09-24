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

import java.io.*;
import java.security.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.thread.*;

/**
 * Dispatch to two VMTI event handlers, e.g., JVMTI and VMA.
 * Thanks to concrete type/method optimizations in the boot image, the indirect calls
 * below are replaced by direct calls to the actual VMTI implementation methods.
 * I.e., the code for these methods does not actually exist in the boot image.
 */
public class DDVMTIHandler implements VMTIHandler {

    private final VMTIHandler eventHandler1;
    private final VMTIHandler eventHandler2;

    DDVMTIHandler(VMTIHandler eventHandler1, VMTIHandler eventHandler2) {
        this.eventHandler1 = eventHandler1;
        this.eventHandler2 = eventHandler2;
    }

    @Override
    public void threadStart(VmThread vmThread) {
        eventHandler1.threadStart(vmThread);
        eventHandler2.threadStart(vmThread);
    }

    @Override
    public void threadEnd(VmThread vmThread) {
        eventHandler1.threadEnd(vmThread);
        eventHandler2.threadEnd(vmThread);
    }

    @Override
    public boolean classFileLoadHookHandled() {
        return eventHandler1.classFileLoadHookHandled() || eventHandler2.classFileLoadHookHandled();
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classfileBytes) {
        return eventHandler1.classFileLoadHook(classLoader, className, protectionDomain, classfileBytes);
    }

    @Override
    public void classLoad(ClassActor classActor) {
        eventHandler1.classLoad(classActor);
        eventHandler2.classLoad(classActor);
    }

    public boolean hasBreakpoints(ClassMethodActor classMethodActor) {
        return eventHandler1.hasBreakpoints(classMethodActor) || eventHandler2.hasBreakpoints(classMethodActor);
    }

    public String bootclassPathExtension() {
        String result = null;
        String s1 =  eventHandler1.bootclassPathExtension();
        if (s1 != null) {
            result = s1;
        }
        String s2 = eventHandler2.bootclassPathExtension();
        if (s2 != null) {
            if (result == null) {
                result = s2;
            } else {
                result += result + File.pathSeparator + s2;
            }
        }
        return result;
    }

    @Override
    public void beginUpcallVM() {
        eventHandler1.beginUpcallVM();
        eventHandler2.beginUpcallVM();
    }

    @Override
    public void endUpcallVM() {
        eventHandler1.endUpcallVM();
        eventHandler2.endUpcallVM();
    }

    @Override
    public void initialize() {
        eventHandler1.initialize();
        eventHandler2.initialize();
    }

    @Override
    public void vmInitialized() {
        eventHandler1.vmInitialized();
        eventHandler2.vmInitialized();
    }

    @Override
    public void vmDeath() {
        eventHandler1.vmDeath();
        eventHandler2.vmDeath();
    }

    @Override
    public void beginGC() {
        eventHandler1.beginGC();
        eventHandler2.beginGC();
    }

    @Override
    public void endGC() {
        eventHandler1.endGC();
        eventHandler2.endGC();
    }

    @Override
    public boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma) {
        return eventHandler1.nativeCallNeedsPrologueAndEpilogue(ma) || eventHandler2.nativeCallNeedsPrologueAndEpilogue(ma);
    }

    @Override
    public void registerAgent(Word agentHandle) {
        eventHandler1.registerAgent(agentHandle);
        eventHandler2.registerAgent(agentHandle);
    }

    @Override
    public void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        eventHandler1.raise(throwable, sp, fp, ip);
        eventHandler2.raise(throwable, sp, fp, ip);
    }

    @Override
    public RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler) {
        RuntimeCompiler rc1 = eventHandler1.runtimeCompiler(stdRuntimeCompiler);
        if (rc1 != null) {
            return rc1;
        }
        return eventHandler2.runtimeCompiler(stdRuntimeCompiler);
    }

    @Override
    public boolean needsVMTICompilation(ClassMethodActor classMethodActor) {
        return eventHandler1.needsVMTICompilation(classMethodActor) || eventHandler1.needsVMTICompilation(classMethodActor);
    }

    @Override
    public boolean needsSpecialGetCallerClass() {
        return eventHandler1.needsSpecialGetCallerClass() || eventHandler2.needsSpecialGetCallerClass();
    }

    @Override
    @NEVER_INLINE
    public Class getCallerClassForFindClass(int realFramesToSkip) {
        return eventHandler1.getCallerClassForFindClass(realFramesToSkip++);
    }

    @Override
    public void objectSurviving(Pointer cell) {
        eventHandler1.objectSurviving(cell);
        eventHandler2.objectSurviving(cell);
    }

    @Override
    public void methodCompiled(ClassMethodActor classMethodActor) {
        eventHandler1.methodCompiled(classMethodActor);
        eventHandler2.methodCompiled(classMethodActor);
    }

    @Override
    public void methodUnloaded(ClassMethodActor classMethodActor, Pointer codeAddr) {
        eventHandler1.methodUnloaded(classMethodActor, codeAddr);
        eventHandler2.methodUnloaded(classMethodActor, codeAddr);
    }

    @Override
    public int activeAgents() {
        return eventHandler1.activeAgents() + eventHandler2.activeAgents();
    }

}
