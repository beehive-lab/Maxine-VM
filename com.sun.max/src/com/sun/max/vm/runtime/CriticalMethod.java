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
package com.sun.max.vm.runtime;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * This class represents a method that is a critical entrypoint in the virtual machine, which means
 * that it is typically used by very low-level subsystems that must have the method compiled to
 * target code at image build time and must have the address of the machine code readily available.
 * This is typically needed for subsystems that interact closely with the substrate at startup.
 */
public class CriticalMethod {
    public final ClassMethodActor classMethodActor;
    private final CallEntryPoint callEntryPoint;
    protected Address address;

    /**
     * Create a new critical entrypoint for the method specified by the java class and its name
     * as a string. This constructor uses the default {link CallEntryPoint optimized} entrypoint.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @param methodSignature the signature of the method
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    @HOSTED_ONLY
    public CriticalMethod(Class javaClass, String methodName, SignatureDescriptor methodSignature) {
        this(javaClass, methodName, methodSignature, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    /**
     * Create a new critical entrypoint for the method specified by the java class and its name
     * as a string.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @param methodSignature the signature of the method to find. If this value is {@code null}, then the first method found
     *            based on {@code name} is returned.
     * @param callEntryPoint the entrypoint in the method that is desired
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    @HOSTED_ONLY
    public CriticalMethod(Class javaClass, String methodName, SignatureDescriptor methodSignature, CallEntryPoint callEntryPoint) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final Utf8Constant name = SymbolTable.makeSymbol(methodName);
        ClassMethodActor classMethodActor = classActor.findLocalClassMethodActor(name, methodSignature);
        if (classMethodActor == null) {
            classMethodActor = classActor.findLocalStaticMethodActor(name);
        }
        if (classMethodActor == null) {
            throw new NoSuchMethodError(methodName);
        }
        this.classMethodActor = classMethodActor;
        this.callEntryPoint = callEntryPoint;
        MaxineVM.registerCriticalMethod(this);
    }

    /**
     * Create a new critical entrypoint for the specified class method actor.
     *
     * @param classMethodActor the method for which to create an entrypoint
     * @param callEntryPoint the call entrypoint of the method that is desired
     */
    @HOSTED_ONLY
    public CriticalMethod(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        this.classMethodActor = classMethodActor;
        this.callEntryPoint = callEntryPoint;
        MaxineVM.registerCriticalMethod(this);
    }

    /**
     * Create a new critical entrypoint for the specified method {@link java.lang.reflect.Method}.
     * @param method
     */
    @HOSTED_ONLY
    public CriticalMethod(Method method) {
        this(method.getDeclaringClass(), method.getName(), SignatureDescriptor.create(method.getReturnType(), method.getParameterTypes()));
    }

    /**
     * Create a new critical entrypoint for the specified method {@link java.lang.reflect.Method} in specified class.
     * @param method
     */
    @HOSTED_ONLY
    public CriticalMethod(Class javaClass, Method method) {
        this(javaClass, method.getName(), SignatureDescriptor.create(method.getReturnType(), method.getParameterTypes()));
    }

    /**
     * Gets the address of the entrypoint to the compiled code of the method.
     *
     * @return the address of the first instruction of the compiled code of this method
     */
    public Address address() {
        if (address.isZero()) {
            address = classMethodActor.currentTargetMethod().getEntryPoint(callEntryPoint).toAddress();
        }
        return address;
    }

    /**
     * Gets the target method compiled for this critical method.
     *
     * @return {@code null} if this critical method has not been compiled.
     */
    public TargetMethod targetMethod() {
        return classMethodActor.currentTargetMethod();
    }
}
