/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;

/**
 * This class represents a critical native method that is used by the virtual machine
 * and must be linked while the VM is starting up. Some critical native methods are JNI functions,
 * and this class will pre-allocate the required mangled string name in order to link
 * these methods before allocation works.
 *
 * @author Ben L. Titzer
 */
public class CriticalNativeMethod extends CriticalMethod {

    private static CriticalNativeMethod[] criticalNativeMethods = {};

    @HOSTED_ONLY
    private static void registerCriticalNativeMethod(CriticalNativeMethod criticalNativeMethod) {
        criticalNativeMethods = Arrays.copyOf(criticalNativeMethods, criticalNativeMethods.length + 1);
        criticalNativeMethods[criticalNativeMethods.length - 1] = criticalNativeMethod;
    }

    /**
     * Creates a new critical entrypoint for the method specified by the Java class and its name
     * as a string.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    @HOSTED_ONLY
    public CriticalNativeMethod(Class javaClass, String methodName) {
        super(javaClass, methodName, null, CallEntryPoint.C_ENTRY_POINT);
        registerCriticalNativeMethod(this);
        assert classMethodActor.isNative() : classMethodActor + " cannot be registered as a " + CriticalNativeMethod.class.getSimpleName() +
        " as it is not native";
    }

    /**
     * Create a new critical entrypoint for the specified class method actor.
     * @param classMethodActor the method for which to create an entrypoint
     */
    @HOSTED_ONLY
    public CriticalNativeMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor, CallEntryPoint.C_ENTRY_POINT);
        registerCriticalNativeMethod(this);
    }

    /**
     * Links the critical native methods in the image.
     */
    public static void linkAll() {
        for (CriticalNativeMethod method : criticalNativeMethods) {
            method.link();
        }
    }

    /**
     * Links the native function.
     * @return the address of the native function's implementation
     */
    public final Address link() {
        return classMethodActor.nativeFunction.link().asAddress();
    }
}
