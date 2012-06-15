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
package com.oracle.max.vm.ext.jjvmti.agents.util;

import java.security.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * Default empty implementations of the {@link JJVMTI.EventCallbacks}.
 *
 */
public class NullJJVMTICallbacks extends JJVMTIAgentAdapter implements JJVMTI.EventCallbacks {

    @Override
    public void onBoot() {
    }

    @Override
    public void breakpoint(Thread thread, MethodActor method, long location) {
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
        return null;
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
    }

    @Override
    public void fieldAccess(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field) {
    }

    @Override
    public void fieldModification(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field, Object newValue) {
    }

    @Override
    public void garbageCollectionStart() {
    }

    @Override
    public void garbageCollectionFinish() {
    }

    @Override
    public void methodEntry(Thread thread, MethodActor method) {
    }

    @Override
    public void methodExit(Thread thread, MethodActor method, boolean exeception, Object returnValue) {
    }

    @Override
    public void threadStart(Thread thread) {
    }

    @Override
    public void threadEnd(Thread thread) {
    }

    @Override
    public void vmDeath() {
    }

    @Override
    public void vmInit() {
    }

    @Override
    public void compiledMethodLoad(MethodActor method, int codeSize, Address codeAddr, AddrLocation[] map, Object compileInfo) {
    }

    @Override
    public void compiledMethodUnload(MethodActor method, Address codeAddr) {
    }

    @Override
    public void dataDumpRequest() {
    }

    @Override
    public void dynamicCodeGenerated(String name, Address codeAddr, int length) {
    }

    @Override
    public void exception(Thread thread, MethodActor method, long location, Object exception, MethodActor catchMethod, long catchLocation) {
    }

    @Override
    public void exceptionCatch(Thread thread, MethodActor method, long location, Object exception) {
    }

    @Override
    public void framePop(Thread thread, MethodActor method, boolean wasPoppedByException) {
    }

    @Override
    public void monitorContendedEnter(Thread thread, Object object) {
    }

    @Override
    public void monitorContendedEntered(Thread thread, Object object) {
    }

    @Override
    public void monitorWait(Thread thread, Object object, long timeout) {
    }

    @Override
    public void monitorWaited(Thread thread, Object object, long timeout) {
    }

    @Override
    public void objectFree(Object tag) {
    }

    @Override
    public void resourceExhausted(int flags, String description) {
    }

    @Override
    public void singleStep(Thread thread, MethodActor method, long location) {
    }

    @Override
    public void vmObjectAllocation(Thread thread, Object object, ClassActor classActor, int size) {
    }

}
