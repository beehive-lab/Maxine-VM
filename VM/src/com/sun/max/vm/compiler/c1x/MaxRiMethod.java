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

import com.sun.c1x.ri.*;
import com.sun.c1x.*;
import com.sun.c1x.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.collect.Sequence;

/**
 * The {@code MaxRiMethod} implements a compiler interface method. A method can
 * be either resolved or unresolved. A resolved method has a reference to its
 * associated {@code MethodActor} and unresolved method has a reference
 * to its {@code MethodRefConstant} some method calls are only appropriate
 * for resolved methods and will result in a {@code MaxCiUnresolved}
 * exception if called on an unresolved method.
 *
 * @author Ben L. Titzer
 */
public class MaxRiMethod implements RiMethod {

    final MaxRiConstantPool constantPool;
    final MethodRefConstant methodRef;
    final MethodActor methodActor;
    List<RiExceptionHandler> exceptionHandlers;
    final int cpi;

    /**
     * Creates a new resolved compiler interface method from the specified method actor.
     * @param constantPool the constant pool
     * @param methodActor the method actor
     */
    public MaxRiMethod(MaxRiConstantPool constantPool, MethodActor methodActor, int cpi) {
        this.constantPool = constantPool;
        this.methodActor = methodActor;
        this.methodRef = null;
        this.cpi = cpi;
        if (methodActor instanceof ClassMethodActor && ((ClassMethodActor) methodActor).isDeclaredFoldable()) {
            C1XIntrinsic.registerFoldableMethod(this, methodActor.toJava());
        }
    }

    /**
     * Creates a new unresolved compiler interface method from the specified method ref.
     * @param constantPool the constant pool
     * @param methodRef the method ref
     * @param cpi the constant pool index
     */
    public MaxRiMethod(MaxRiConstantPool constantPool, MethodRefConstant methodRef, int cpi) {
        this.constantPool = constantPool;
        this.methodRef = methodRef;
        this.methodActor = null;
        this.cpi = cpi;
    }

    /**
     * Gets the name of this method.
     * @return the name of this method as a string
     */
    public String name() {
        if (methodActor != null) {
            return methodActor.name.toString();
        }
        return methodRef.name(constantPool.constantPool).toString();
    }

    /**
     * Gets the compiler interface type of the holder of this method.
     * @return the holder of this method
     */
    public RiType holder() {
        if (methodActor != null) {
            return constantPool.runtime.canonicalRiType(methodActor.holder(), constantPool, -1);
        }
        // TODO: get the correct CPI of the holder
        return new MaxRiType(constantPool, methodRef.holder(constantPool.constantPool), -1);
    }

    /**
     * Gets the compiler interface signature for this method.
     * @return the signature of this method
     */
    public RiSignature signatureType() {
        if (methodActor != null) {
            return constantPool.cacheSignature(methodActor.descriptor());
        }
        return constantPool.cacheSignature(methodRef.signature(constantPool.constantPool));
    }

    /**
     * Gets the code of this method as byte array.
     * @return the code of this method
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public byte[] code() {
        return asClassMethodActor("code()").originalCodeAttribute().code();
    }

    /**
     * Checks whether this method has bytecode. For unresolved, abstract, or native methods,
     * this method returns {@code false}.
     * @return {@code true} if bytecode is available for the method
     */
    public boolean hasCode() {
        if (methodActor instanceof ClassMethodActor && !methodActor.isNative()) {
            return ((ClassMethodActor) methodActor).originalCodeAttribute() != null;
        }
        return false;
    }

    /**
     * Gets the maximum number of locals used in the code of this method.
     * @return the maximum number of locals
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public int maxLocals() {
        return asClassMethodActor("maxLocals()").originalCodeAttribute().maxLocals();
    }

    /**
     * Gets the maximum stack size used in the code of this method.
     * @return the maximum stack size
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public int maxStackSize() {
        return asClassMethodActor("maxStackSize()").originalCodeAttribute().maxStack();
    }

    /**
     * Checks whether this method has balanced monitor operations.
     * @return {@code true} if the monitor operations are balanced correctly
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean hasBalancedMonitors() {
        asClassMethodActor("hasBalancedMonitors()");
        return true; // TODO: do the analyzes
    }

    /**
     * Checks whether this method has any exception handlers.
     * @return {@code true} if this method has any exception handlers
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean hasExceptionHandlers() {
        final CodeAttribute codeAttribute = asClassMethodActor("hasExceptionHandlers()").originalCodeAttribute();
        if (codeAttribute != null) {
            final Sequence<ExceptionHandlerEntry> handlerTable = codeAttribute.exceptionHandlerTable();
            return handlerTable != null && handlerTable.length() > 0;
        }
        return false;
    }

    /**
     * Checks whether this compiler interface method is loaded (i.e. resolved).
     * @return {@code true} if this method is loaded
     */
    public boolean isLoaded() {
        return methodActor != null;
    }

    /**
     * Checks whether this method is abstract.
     * @return {@code true} if this method is abstract
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isAbstract() {
        return asMethodActor("isAbstract()").isAbstract();
    }

    /**
     * Checks whether this method is native.
     * @return {@code true} if this method is native
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isNative() {
        return asMethodActor("isNative()").isNative();
    }

    /**
     * Checks whether this method is final.
     * @return {@code true} if this method is final
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isFinalMethod() {
        return asMethodActor("isFinalMethod()").isFinal();
    }

    /**
     * Checks whether this method is synchronized.
     * @return {@code true} if this method is synchronized
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isSynchronized() {
        return asMethodActor("isSynchronized()").isSynchronized();

    }

    /**
     * Checks whether this method is strict-fp.
     * @return {@code true} if this method is strict-fp
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isStrictFP() {
        return asMethodActor("isStrictFP()").isStrict();
    }

    /**
     * Checks whether this method is static.
     * @return {@code true} if this method is static
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isStatic() {
        return asMethodActor("isStatic()").isStatic();
    }

    /**
     * Checks whether this method has been overridden in the current runtime environment.
     * @return {@code true} if this method has been overridden
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public boolean isOverridden() {
        return !canBeStaticallyBound(); // TODO: do leaf method checks
    }

    /**
     * Gets the virtual table index of this method.
     * @return the virtual table index of this method
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public int vtableIndex() {
        if (methodActor instanceof VirtualMethodActor) {
            return ((VirtualMethodActor) methodActor).vTableIndex();
        } else if (methodActor instanceof InterfaceMethodActor) {
            return ((InterfaceMethodActor) methodActor).iIndexInInterface();
        }
        return -1;
    }

    /**
     * Gets the method instrumentation for this method.
     * @return the method instruction for this method; {@code null} if no instrumentation
     * is available
     */
    public RiMethodProfile methodData() {
        return null;
    }

    /**
     * Gets the liveness information for local variables at a particular bytecode index.
     * @param bci the bytecode index
     * @return a bitmap representing which locals are live; {@code null} if no liveness
     * information is available
     */
    public BitMap liveness(int bci) {
        return null;
    }

    /**
     * Checks whether this method can be statically bound (i.e. it is final or private).
     * @return {@code true} if this method can be statically bound; {@code false}
     * if the field is unresolved or cannot be statically bound
     */
    public boolean canBeStaticallyBound() {
        if (methodActor instanceof ClassMethodActor) {
            final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
            return classMethodActor.isStatic() || classMethodActor.isPrivate() || classMethodActor.isFinal();
        }
        return false;
    }

    /**
     * Gets the size of the code in this method.
     * @return the size of the code in bytes
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public int codeSize() {
        return asClassMethodActor("codeSize()").originalCodeAttribute().code().length;
    }

    /**
     * Gets the exception handlers for this method.
     * @return the exception handlers
     * @throws MaxRiUnresolved if the method is unresolved
     */
    public List<RiExceptionHandler> exceptionHandlers() {
        if (exceptionHandlers != null) {
            // return the cached exception handlers
            return exceptionHandlers;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor("exceptionHandlers()");
        exceptionHandlers = new ArrayList<RiExceptionHandler>();
        CodeAttribute codeAttribute = classMethodActor.originalCodeAttribute();
        for (ExceptionHandlerEntry entry : codeAttribute.exceptionHandlerTable()) {
            RiType catchType;
            if (entry.catchTypeIndex() == 0) {
                catchType = null;
            } else {
                RiConstantPool riConstantPool = constantPool.runtime.getConstantPool(this);
                catchType = riConstantPool.resolveType((char) entry.catchTypeIndex());
            }
            exceptionHandlers.add(new MaxRiExceptionHandler((char) entry.startPosition(),
                                                             (char) entry.endPosition(),
                                                             (char) entry.handlerPosition(),
                                                             (char) entry.catchTypeIndex(), catchType));
        }
        return exceptionHandlers;
    }

    ClassMethodActor asClassMethodActor(String operation) {
        if (methodActor instanceof ClassMethodActor) {
            return (ClassMethodActor) methodActor;
        }
        throw unresolved(operation);
    }

    InterfaceMethodActor asInterfaceMethodActor(String operation) {
        if (methodActor instanceof InterfaceMethodActor) {
            return (InterfaceMethodActor) methodActor;
        }
        throw unresolved(operation);
    }

    VirtualMethodActor asVirtualMethodActor(String operation) {
        if (methodActor instanceof VirtualMethodActor) {
            return (VirtualMethodActor) methodActor;
        }
        throw unresolved(operation);
    }

    MethodActor asMethodActor(String operation) {
        if (methodActor != null) {
            return methodActor;
        }
        throw unresolved(operation);
    }

    private MaxRiUnresolved unresolved(String operation) {
        String name;
        if (methodActor != null) {
            name = methodActor.toString();
        } else {
            name = methodRef.toString(constantPool.constantPool);
        }
        throw new MaxRiUnresolved(operation + " not defined for unresolved method " + name);
    }

    /**
     * Gets the hashcode for this compiler interface method. This is the
     * identity hash code for the method actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (methodActor != null) {
            return System.identityHashCode(methodActor); // use the method actor's hashcode
        }
        return System.identityHashCode(this);
    }

    /**
     * Checks whether this compiler interface method is equal to another object.
     * If the method is resolved, the objects are equivalent if the refer
     * to the same method actor. Otherwise they are equivalent if they
     * reference the same compiler interface method object.
     * @param o the object to check
     * @return {@code true} if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (methodActor != null && o instanceof MaxRiMethod) {
            return methodActor == ((MaxRiMethod) o).methodActor;
        }
        return o == this;
    }

    /**
     * Converts this compiler interface method to a string.
     * @return a string representation of this compiler interface method
     */
    @Override
    public String toString() {
        if (methodActor != null) {
            return methodActor.toString();
        }
        return methodRef.toString() + " [unresolved]";
    }

    public int javaCodeAtBci(int bci) {
        return code()[bci] & 0xff;
    }

    public int interfaceID() {

        if (methodActor.holder() instanceof InterfaceActor) {
            return ((InterfaceActor) methodActor.holder()).id;
        }

        return -1;
    }

    public int indexInInterface() {
        if (methodActor instanceof InterfaceMethodActor) {
            return ((InterfaceMethodActor) methodActor).iIndexInInterface();
        }

        return -1;
    }
}
