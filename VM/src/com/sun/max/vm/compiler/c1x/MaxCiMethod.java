/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * The <code>MaxCiMethod</code> implements a compiler interface method. A method can
 * be either resolved or unresolved. A resolved method has a reference to its
 * associated <code>MethodActor</code> and unresolved method has a reference
 * to its <code>MethodRefConstant</code> some method calls are only appropriate
 * for resolved methods and will result in a <code>MaxCiUnresolved</code>
 * exception if called on an unresolved method.
 *
 * @author Ben L. Titzer
 */
public class MaxCiMethod implements CiMethod {

    final MaxCiConstantPool _constantPool;
    MethodRefConstant _methodRef;
    MethodActor _methodActor;
    List<CiExceptionHandler> _exceptionHandlers;

    /**
     * Creates a new resolved compiler interface method from the specified method actor.
     * @param constantPool the constant pool
     * @param methodActor the method actor
     */
    public MaxCiMethod(MaxCiConstantPool constantPool, MethodActor methodActor) {
        _constantPool = constantPool;
        _methodActor = methodActor;
    }

    /**
     * Creates a new unresolved compiler interface method from the specified method ref.
     * @param constantPool the constant pool
     * @param methodRef the method ref
     */
    public MaxCiMethod(MaxCiConstantPool constantPool, MethodRefConstant methodRef) {
        _constantPool = constantPool;
        _methodRef = methodRef;
    }

    /**
     * Gets the name of this method.
     * @return the name of this method as a string
     */
    public String name() {
        if (_methodActor != null) {
            return _methodActor.name().toString();
        }
        return _methodRef.name(_constantPool._constantPool).toString();
    }

    /**
     * Gets the compiler interface type of the holder of this method.
     * @return the holder of this method
     */
    public CiType holder() {
        if (_methodActor != null) {
            return _constantPool.canonicalCiType(_methodActor.holder());
        }
        return new MaxCiType(_constantPool, _methodRef.holder(_constantPool._constantPool));
    }

    /**
     * Checks whether this method will link at the specified location for the specified bytecode.
     * @param where the type from which the method is referenced
     * @param opcode the opcode used to reference the method
     * @return <code>true</code> if the method will link successfully
     */
    public boolean willLink(CiType where, int opcode) {
        return _methodActor != null; // TODO: this is not correct
    }

    /**
     * Gets the compiler interface signature for this method.
     * @return the signature of this method
     */
    public CiSignature signatureType() {
        if (_methodActor != null) {
            return _constantPool.cacheSignature(_methodActor.descriptor());
        }
        return _constantPool.cacheSignature(_methodRef.signature(_constantPool._constantPool));
    }

    /**
     * Gets the code of this method as byte array.
     * @return the code of this method
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public byte[] code() {
        return asClassMethodActor("code()").rawCodeAttribute().code();
    }

    /**
     * Gets the maximum number of locals used in the code of this method.
     * @return the maximum number of locals
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public int maxLocals() {
        return asClassMethodActor("maxLocals()").rawCodeAttribute().maxLocals();
    }

    /**
     * Gets the maximum stack size used in the code of this method.
     * @return the maximum stack size
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public int maxStackSize() {
        return asClassMethodActor("maxStackSize()").rawCodeAttribute().maxStack();
    }

    /**
     * Checks whether this method has balanced monitor operations.
     * @return <code>true</code> if the monitor operations are balanced correctly
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean hasBalancedMonitors() {
        asClassMethodActor("hasBalancedMonitors()");
        return true; // TODO: do the analyzes
    }

    /**
     * Checks whether this method has any exception handlers.
     * @return <code>true</code> if this method has any exception handlers
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean hasExceptionHandlers() {
        return asClassMethodActor("hasExceptionHandlers()").rawCodeAttribute().exceptionHandlerTable().length() > 0;
    }

    /**
     * Checks whether this compiler interface method is loaded (i.e. resolved).
     * @return <code>true</code> if this method is loaded
     */
    public boolean isLoaded() {
        return _methodActor != null;
    }

    /**
     * Checks whether this method is abstract.
     * @return <code>true</code> if this method is abstract
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isAbstract() {
        return asClassMethodActor("isAbstract()").isAbstract();
    }

    /**
     * Checks whether this method is native.
     * @return <code>true</code> if this method is native
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isNative() {
        return asClassMethodActor("isNative()").isNative();
    }

    /**
     * Checks whether this method is final.
     * @return <code>true</code> if this method is final
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isFinalMethod() {
        return asClassMethodActor("isFinalMethod()").isFinal();
    }

    /**
     * Checks whether this method is synchronized.
     * @return <code>true</code> if this method is synchronized
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isSynchronized() {
        return asClassMethodActor("isSynchronized()").isSynchronized();

    }

    /**
     * Checks whether this method is strict-fp.
     * @return <code>true</code> if this method is strict-fp
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isStrictFP() {
        return asClassMethodActor("isStrictFP()").isStrict();
    }

    /**
     * Checks whether this method is static.
     * @return <code>true</code> if this method is static
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isStatic() {
        return asClassMethodActor("isStatic()").isStatic();
    }

    /**
     * Checks whether this method has been overridden in the current runtime environment.
     * @return <code>true</code> if this method has been overridden
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public boolean isOverridden() {
        return !canBeStaticallyBound(); // TODO: do leaf method checks
    }

    /**
     * Gets the virtual table index of this method.
     * @return the virtual table index of this method
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public int vtableIndex() {
        if (_methodActor instanceof VirtualMethodActor) {
            return ((VirtualMethodActor) _methodActor).vTableIndex();
        }
        return -1;
    }

    /**
     * Gets the method instrumentation for this method.
     * @return the method instruction for this method; <code>null</code> if no instrumentation
     * is available
     */
    public CiMethodData methodData() {
        return null;
    }

    /**
     * Gets the liveness information for local variables at a particular bytecode index.
     * @param bci the bytecode index
     * @return a bitmap representing which locals are live; <code>null</code> if no liveness
     * information is available
     */
    public BitMap liveness(int bci) {
        return null;
    }

    /**
     * Checks whether this method can be statically bound (i.e. it is final or private).
     * @return <code>true</code> if this method can be statically bound; <code>false</code>
     * if the field is unresolved or cannot be statically bound
     */
    public boolean canBeStaticallyBound() {
        if (_methodActor instanceof ClassMethodActor) {
            final ClassMethodActor classMethodActor = (ClassMethodActor) _methodActor;
            return classMethodActor.isStatic() || classMethodActor.isPrivate() || classMethodActor.isFinal();
        }
        return false;
    }

    /**
     * Gets the size of the code in this method.
     * @return the size of the code in bytes
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public int codeSize() {
        return asClassMethodActor("codeSize()").rawCodeAttribute().code().length;
    }

    /**
     * Gets the exception handlers for this method.
     * @return the exception handlers
     * @throws MaxCiUnresolved if the method is unresolved
     */
    public List<CiExceptionHandler> exceptionHandlers() {
        if (_exceptionHandlers != null) {
            // return the cached exception handlers
            return _exceptionHandlers;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor("exceptionHandlers()");
        _exceptionHandlers = new ArrayList<CiExceptionHandler>();
        for (ExceptionHandlerEntry entry : classMethodActor.rawCodeAttribute().exceptionHandlerTable()) {
            _exceptionHandlers.add(new MaxCiExceptionHandler((char) entry.startPosition(),
                                                             (char) entry.endPosition(),
                                                             (char) entry.handlerPosition(),
                                                             (char) entry.catchTypeIndex()));
        }
        return _exceptionHandlers;
    }

    ClassMethodActor asClassMethodActor(String operation) {
        if (_methodActor instanceof ClassMethodActor) {
            return (ClassMethodActor) _methodActor;
        }
        throw unresolved(operation);
    }

    private MaxCiUnresolved unresolved(String operation) {
        throw new MaxCiUnresolved(operation + " not defined for unresolved method " + _methodRef.toString(_constantPool._constantPool));
    }

    /**
     * Gets the hashcode for this compiler interface method. This is the
     * identity hash code for the method actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (_methodActor != null) {
            return System.identityHashCode(_methodActor); // use the method actor's hashcode
        }
        return System.identityHashCode(this);
    }

    /**
     * Checks whether this compiler interface method is equal to another object.
     * If the method is resolved, the objects are equivalent if the refer
     * to the same method actor. Otherwise they are equivalent if they
     * reference the same compiler interface method object.
     * @param o the object to check
     * @return <code>true</code> if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (_methodActor != null && o instanceof MaxCiMethod) {
            return _methodActor == ((MaxCiMethod) o)._methodActor;
        }
        return o == this;
    }

    /**
     * Converts this compiler interface method to a string.
     */
    @Override
    public String toString() {
        if (_methodActor != null) {
            return _methodActor.toString();
        }
        return _methodRef.toString() + " [unresolved]";
    }
}
