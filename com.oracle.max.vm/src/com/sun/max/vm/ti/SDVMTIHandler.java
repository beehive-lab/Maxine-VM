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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.thread.*;

/**
 * Dispatch to a single VMTI Handler, which is the usual case, where VMTI == JVMTI.
 * Thanks to concrete type/method optimizations in the boot image, the indirect calls
 * below are replaced by direct calls to the actual VMTI implementation method.
 * I.e., the code for these methods does not actually exist in the boot image.
 *
 */
class SDVMTIHandler implements VMTIHandler {

    private final VMTIHandler eventHandler;

    SDVMTIHandler(VMTIHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void threadStart(VmThread vmThread) {
        eventHandler.threadStart(vmThread);
    }

    @Override
    public void threadEnd(VmThread vmThread) {
        eventHandler.threadEnd(vmThread);
    }

    @Override
    public boolean classFileLoadHookHandled() {
        return eventHandler.classFileLoadHookHandled();
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classfileBytes) {
        return eventHandler.classFileLoadHook(classLoader, className, protectionDomain, classfileBytes);
    }

    @Override
    public void classLoad(ClassActor classActor) {
        eventHandler.classLoad(classActor);
    }

    public boolean hasBreakpoints(ClassMethodActor classMethodActor) {
        return eventHandler.hasBreakpoints(classMethodActor);
    }

    public String bootclassPathExtension() {
        return eventHandler.bootclassPathExtension();
    }

    @Override
    public void beginUpcallVM() {
        eventHandler.beginUpcallVM();
    }

    @Override
    public void endUpcallVM() {
        eventHandler.endUpcallVM();
    }

    @Override
    public void initialize() {
        eventHandler.initialize();
    }

    @Override
    public void vmInitialized() {
        eventHandler.vmInitialized();
    }

    @Override
    public void vmDeath() {
        eventHandler.vmDeath();
    }

    @Override
    public void beginGC() {
        eventHandler.beginGC();
    }

    @Override
    public void endGC() {
        eventHandler.endGC();
    }

    @Override
    public boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma) {
        return eventHandler.nativeCallNeedsPrologueAndEpilogue(ma);
    }

    @Override
    public void registerAgent(Word agentHandle) {
        eventHandler.registerAgent(agentHandle);
    }

    @Override
    public void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        eventHandler.raise(throwable, sp, fp, ip);
    }

    @Override
    public RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler) {
        return eventHandler.runtimeCompiler(stdRuntimeCompiler);
    }

    @Override
    public boolean needsVMTICompilation(ClassMethodActor classMethodActor) {
        return eventHandler.needsVMTICompilation(classMethodActor);
    }

    @Override
    public boolean needsSpecialGetCallerClass() {
        return eventHandler.needsSpecialGetCallerClass();
    }

    @Override
    @NEVER_INLINE
    public Class getCallerClassForFindClass(int realFramesToSkip) {
        return eventHandler.getCallerClassForFindClass(realFramesToSkip++);
    }

    @Override
    public void objectSurviving(Pointer cell) {
        eventHandler.objectSurviving(cell);
    }

}
