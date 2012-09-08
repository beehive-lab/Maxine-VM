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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.thread.*;

/**
 * The null implementation. Used when no VMTI handlers are registered in a build.
 */
public class NullVMTIHandler implements VMTIHandler {
    public void threadStart(VmThread vmThread) {
    }

    public void threadEnd(VmThread vmThread) {
    }

    public boolean classFileLoadHookHandled() {
        return false;
    }

    public byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain,
                    byte[] classfileBytes) {
        return null;
    }

    public void classLoad(ClassActor classActor) {
    }

    public boolean hasBreakpoints(ClassMethodActor classMethodActor) {
        return false;
    }

    public String bootclassPathExtension() {
        return null;
    }

    @Override
    public void beginUpcallVM() {
    }

    @Override
    public void endUpcallVM() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void vmInitialized() {
    }

    @Override
    public void vmDeath() {
    }

    @Override
    public void beginGC() {
    }

    @Override
    public void endGC() {
    }

    @Override
    public boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma) {
        return false;
    }

    @Override
    public void registerAgent(Word agentHandle) {
    }

    @Override
    public void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
    }

    @Override
    public RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler) {
        return null;
    }

    @Override
    public boolean needsVMTICompilation(ClassMethodActor classMethodActor) {
        return false;
    }

    @Override
    public boolean needsSpecialGetCallerClass() {
        return false;
    }

    @Override
    public Class getCallerClassForFindClass(int realFramesToSkip) {
        return null;
    }

    @Override
    public void objectSurviving(Pointer cell) {
    }

    @Override
    public void methodCompiled(ClassMethodActor classMethodActor) {
    }

    @Override
    public void methodUnloaded(ClassMethodActor classMethodActor, Pointer codeAddr) {
    }

    @Override
    public int activeAgents() {
        return 0;
    }

}
