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
/*VCSID=ae5afb85-a3fc-4cf4-9b24-d074b11d535e*/
package com.sun.max.vm.runtime;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.jni.*;

/**
 * This class represents a critical native method that is used by the virtual machine
 * and must be linked while the VM is starting up. Some critical native methods are JNI functions,
 * and this class with pre-allocate the required mangled string name in order to link
 * these methods before allocation works.
 *
 * @author Ben L. Titzer
 */
public class CriticalNativeMethod extends CriticalMethod {

    protected Address _nativeFunction;
    protected final String _symbol;

    private static CriticalNativeMethod[] _criticalNativeMethods = {};

    @PROTOTYPE_ONLY
    private static void registerCriticalNativeMethod(CriticalNativeMethod criticalNativeMethod) {
        _criticalNativeMethods = Arrays.copyOf(_criticalNativeMethods, _criticalNativeMethods.length + 1);
        _criticalNativeMethods[_criticalNativeMethods.length - 1] = criticalNativeMethod;
    }

    /**
     * Creates a new critical entrypoint for the method specified by the Java class and its name
     * as a string.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    @PROTOTYPE_ONLY
    public CriticalNativeMethod(Class javaClass, String methodName) {
        super(javaClass, methodName, CallEntryPoint.C_ENTRY_POINT);
        registerCriticalNativeMethod(this);
        this._symbol = _classMethodActor.nativeSymbol();
    }

    /**
     * Create a new critical entrypoint for the specified class method actor.
     * @param classMethodActor the method for which to create an entrypoint
     */
    @PROTOTYPE_ONLY
    public CriticalNativeMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor, CallEntryPoint.C_ENTRY_POINT);
        registerCriticalNativeMethod(this);
        this._symbol = _classMethodActor.nativeSymbol();
    }

    /**
     * Links the critical native methods in the image.
     */
    public static void initialize() {
        for (CriticalNativeMethod method : _criticalNativeMethods) {
            method.link();
        }
    }

    /**
     * Links the native function.
     * @return the address of the native function's implementation
     */
    public Address link() {
        if (_nativeFunction.isZero()) {
            _nativeFunction = DynamicLinker.lookup(_classMethodActor, _symbol).asAddress();
            _classMethodActor.setNativeFunction(_nativeFunction);
        }
        return _nativeFunction;
    }
}
