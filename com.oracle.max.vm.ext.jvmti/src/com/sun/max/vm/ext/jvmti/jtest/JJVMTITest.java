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
package com.sun.max.vm.ext.jvmti.jtest;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.security.*;

import com.sun.max.vm.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * A test class for the {@link JJVMTI} interface.
 * Currently this must be built into the boot image as the VM cannot be
 * extended at runtime.
 */
class JJVMTITest extends JVMTI.JavaEnv implements JJVMTI.EventCallbacks {

    static final String JJVMTI_TEST = "max.jvmti.jtest";

    private static String JJVMTITestArgs;

    static {
        VMOptions.addFieldOption("-XX:", "JJVMTITestArgs", "test arguments");
        JJVMTIImpl.instance.register(new JJVMTITest());
    }

    @Override
    public void agentOnLoad() {
        JJVMTIImpl.instance.setEventNotificationMode(this, JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, null);
        JJVMTIImpl.instance.setEventNotificationMode(this, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
    }

    @Override
    public void vmInit() { }
    @Override
    public void breakpoint(Thread thread, Method method, long location) { }
    @Override
    public void garbageCollectionStart() { }
    @Override
    public void garbageCollectionFinish() { }
    @Override
    public void classLoad(Thread thread, Class klass) { }
    @Override
    public void classPrepare(Thread thread, Class klass) { }
    @Override
    public byte[] classFileLoadHook(ClassLoader loader, String name,
                           ProtectionDomain protectionDomain, byte[] classData) {
        return null;
    }
    @Override
    public void threadStart(Thread thread) { }
    @Override
    public void threadEnd(Thread thread) { }
    @Override
    public void vmDeath() { }

}
