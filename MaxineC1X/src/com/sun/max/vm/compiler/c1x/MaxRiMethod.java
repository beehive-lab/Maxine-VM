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

import com.sun.c1x.*;
import com.sun.c1x.util.*;
import com.sun.cri.ri.*;
import com.sun.max.collect.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

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

    public String name() {
        if (methodActor != null) {
            return methodActor.name.toString();
        }
        return methodRef.name(constantPool.constantPool).toString();
    }

    public String jniSymbol() {
        NativeFunction nativeFunction = asClassMethodActor("jniSymbol()").nativeFunction;
        if (nativeFunction != null) {
            return nativeFunction.makeSymbol();
        }
        return null;
    }

    public RiType holder() {
        if (methodActor != null) {
            return constantPool.runtime.canonicalRiType(methodActor.holder(), constantPool, -1);
        }
        int holderCpi = PoolConstant.Static.holderIndex(methodRef);
        return new MaxRiType(constantPool, methodRef.holder(constantPool.constantPool), holderCpi);
    }

    public RiSignature signatureType() {
        if (methodActor != null) {
            return constantPool.cacheSignature(methodActor.descriptor());
        }
        return constantPool.cacheSignature(methodRef.signature(constantPool.constantPool));
    }

    public byte[] code() {
        return codeAttribute("code()").code();
    }

    public boolean hasCode() {
        if (methodActor instanceof ClassMethodActor) {
            return ((ClassMethodActor) methodActor).originalCodeAttribute() != null;
        }
        return false;
    }

    public int maxLocals() {
        return codeAttribute("maxLocals()").maxLocals;
    }

    public int maxStackSize() {
        return codeAttribute("maxStackSize()").maxStack;
    }

    public boolean hasBalancedMonitors() {
        asClassMethodActor("hasBalancedMonitors()");
        return true; // TODO: do the required analysis
    }

    public boolean hasExceptionHandlers() {
        final CodeAttribute codeAttribute = codeAttribute("hasExceptionHandlers()");
        if (codeAttribute != null) {
            final Sequence<ExceptionHandlerEntry> handlerTable = codeAttribute.exceptionHandlerTable();
            return handlerTable != null && handlerTable.length() > 0;
        }
        return false;
    }

    public boolean isLoaded() {
        return methodActor != null;
    }

    public boolean isAbstract() {
        return asMethodActor("isAbstract()").isAbstract();
    }

    public boolean isNative() {
        return asMethodActor("isNative()").isNative();
    }

    public boolean isLeafMethod() {
        MethodActor methodActor = asMethodActor("isLeafMethod()");
        return methodActor.isFinal() || methodActor.isPrivate() || methodActor.holder().isFinal();
    }

    public boolean isSynchronized() {
        return asMethodActor("isSynchronized()").isSynchronized();
    }

    public boolean isStrictFP() {
        return asMethodActor("isStrictFP()").isStrict();
    }

    public boolean isStatic() {
        return asMethodActor("isStatic()").isStatic();
    }

    public boolean isClassInitializer() {
        return asMethodActor("isClassInitializer()").isClassInitializer();
    }

    public boolean isConstructor() {
        return asMethodActor("isConstructor()").isInstanceInitializer();
    }

    public boolean isOverridden() {
        return !canBeStaticallyBound(); // TODO: do leaf method checks
    }

    public RiMethodProfile methodData() {
        return null;
    }

    public BitMap liveness(int bci) {
        return null;
    }

    public boolean canBeStaticallyBound() {
        if (methodActor instanceof ClassMethodActor) {
            final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
            return classMethodActor.isStatic() || classMethodActor.isPrivate() || classMethodActor.isFinal();
        }
        return false;
    }

    public int codeSize() {
        return codeAttribute("codeSize()").code().length;
    }

    public List<RiExceptionHandler> exceptionHandlers() {
        if (exceptionHandlers != null) {
            // return the cached exception handlers
            return exceptionHandlers;
        }
        exceptionHandlers = new ArrayList<RiExceptionHandler>();
        CodeAttribute codeAttribute = codeAttribute("exceptionHandlers()");
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

    CodeAttribute codeAttribute(String operation) {
        return asClassMethodActor(operation).originalCodeAttribute();
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
