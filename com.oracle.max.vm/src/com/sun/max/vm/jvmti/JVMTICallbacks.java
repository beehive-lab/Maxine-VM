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
package com.sun.max.vm.jvmti;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;


public class JVMTICallbacks {
    static {
        new CriticalNativeMethod(JVMTICallbacks.class, "invokeAgentOnLoad");
        new CriticalNativeMethod(JVMTICallbacks.class, "invokeAgentOnUnLoad");
        new CriticalNativeMethod(JVMTICallbacks.class, "invokeStartFunction");
        new CriticalNativeMethod(JVMTICallbacks.class, "invokeStartFunctionNoArg");
        new CriticalNativeMethod(JVMTICallbacks.class, "invokeGarbageCollectionCallback");
    }

    static native int invokeAgentOnLoad(Address onLoad, Pointer options);
    static native int invokeAgentOnUnLoad(Address onLoad);

    static native void invokeStartFunction(Pointer callback, Pointer jvmtiEnv, Word arg);
    static native void invokeStartFunctionNoArg(Pointer callback, Pointer jvmtiEnv);
    static native void invokeGarbageCollectionCallback(Pointer callback, Pointer jvmtiEnv);
}
