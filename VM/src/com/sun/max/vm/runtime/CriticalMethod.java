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
/*VCSID=34479331-a851-4ced-b5cd-6449cf8f1d6b*/
package com.sun.max.vm.runtime;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;

/**
 * This class represents a method that is a critical entrypoint in the virtual machine, which means
 * that it is typically used by very low-level subsystems that must have the method compiled to
 * target code at image build time and must have the address of the machine code readily available.
 * This is typically needed for subsystems that interact closely with the substrate at startup.
 *
 * @author Ben L. Titzer
 */
public class CriticalMethod {
    protected final ClassMethodActor _classMethodActor;
    protected final CallEntryPoint _callEntryPoint;
    protected Address _address;

    /**
     * Create a new critical entrypoint for the method specified by the java class and its name
     * as a string. This constructor uses the default {link CallEntryPoint optimized} entrypoint.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    public CriticalMethod(Class javaClass, String methodName) {
        this(javaClass, methodName, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    /**
     * Create a new critical entrypoint for the method specified by the java class and its name
     * as a string.
     *
     * @param javaClass the class in which the method was declared
     * @param methodName the name of the method as a string
     * @param callEntryPoint the entrypoint in the method that is desired
     * @throws NoSuchMethodError if a method with the specified name could not be found in the specified class
     */
    public CriticalMethod(Class javaClass, String methodName, CallEntryPoint callEntryPoint) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final Utf8Constant name = SymbolTable.makeSymbol(methodName);
        ClassMethodActor classMethodActor = classActor.findLocalClassMethodActor(name);
        if (classMethodActor == null) {
            classMethodActor = classActor.findLocalStaticMethodActor(name);
        }
        if (classMethodActor == null) {
            throw new NoSuchMethodError(methodName);
        }
        _classMethodActor = classMethodActor;
        _callEntryPoint = callEntryPoint;
        MaxineVM.registerCriticalMethod(this);
    }

    /**
     * Create a new critical entrypoint for the specified class method actor.
     *
     * @param classMethodActor the method for which to create an entrypoint
     * @param callEntryPoint the call entrypoint of the method that is desired
     */
    public CriticalMethod(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        _classMethodActor = classMethodActor;
        _callEntryPoint = callEntryPoint;
        MaxineVM.registerCriticalMethod(this);
    }

    /**
     * Gets the address of the entrypoint to the compiled code of the method.
     *
     * @return the address of the first instruction of the compiled code of this method
     */
    public Address address() {
        if (_address.isZero()) {
            _address = CompilationScheme.Static.getCriticalEntryPoint(_classMethodActor, _callEntryPoint);
        }
        return _address;
    }

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }
}
