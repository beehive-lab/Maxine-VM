/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.hosted;

import com.sun.max.vm.run.java.*;

/**
 * A collection of useful methods for controlling extensions to the Maxine image.
 * These are essentially just pass throughs to other methods collected into one place.
 *
 *
 * @author Mick Jordan
 */
public class Extensions {

    /**
     * Causes the named field in named class to be reset to its default value in the boot image.
     * This also prevents any objects solely reachable from the field from being include in the boot heap.
     *
     * @param classname fully qualified class name
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
