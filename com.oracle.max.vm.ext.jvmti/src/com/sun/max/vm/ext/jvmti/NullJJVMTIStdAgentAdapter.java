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
package com.sun.max.vm.ext.jvmti;

import java.lang.reflect.*;
import java.security.*;


public class NullJJVMTIStdAgentAdapter extends JJVMTIStdAgentAdapter {
    @Override
    public void agentStartup() {
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
        return null;
    }

    @Override
    public void garbageCollectionStart() {
    }

    @Override
    public void garbageCollectionFinish() {
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
    public void breakpoint(Thread thread, Member method, long location) {
    }

    @Override
    public void classLoad(Thread thread, Class<?> klass) {
    }

    @Override
    public void methodEntry(Thread thread, Member method) {
    }

    @Override
    public void methodExit(Thread thread, Member method, boolean exeception, Object returnValue) {
    }

    @Override
    public void fieldAccess(Thread thread, Method method, long location, Class<?> klass, Object object, Field field) {
    }

    @Override
    public void fieldModification(Thread thread, Method method, long location, Class<?> klass, Object object, Field field, Object newValue) {
    }


}
