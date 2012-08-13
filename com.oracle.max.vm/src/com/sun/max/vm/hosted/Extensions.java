/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import com.sun.max.vm.run.java.*;

/**
 * A collection of useful methods for controlling extensions to the Maxine image.
 * These are essentially just pass throughs to other methods collected into one place.
 */
public class Extensions {

    /**
     * Causes the named field in named class to be reset to its default value in the boot image.
     * This also prevents any objects solely reachable from the field from being include in the boot heap.
     *
     * @param className fully qualified class name
     * @param fieldName name of field to be rest to default value
     */
    public static void resetField(String className, String fieldName) {
        JDKInterceptor.resetField(className, fieldName);
    }

    /**
     * Registers a given method to be a root for determining inclusion in the boot image.
     * @param className fully qualified class name
     * @param methodName name of method, {@code null} or "*" to denote all methods
     */
    public static void registerVMEntryPoint(String className, String methodName) {
        CompiledPrototype.registerVMEntryPoint(className + "." + (methodName == null ? "*" : methodName));
    }

    /**
     * Register a class for reinitialisation during VM startup.
     * Typically, if {@link #resetField} is called, this method should also be called.
     * @param className
     */
    public static void registerClassForReInit(String className) {
        JavaRunScheme.registerClassForReInit(className);
    }
}
