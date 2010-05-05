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
package com.sun.max.vm.classfile.constant;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;

/**
 * An {@linkplain RiType#isResolved() unresolved} type with a back reference to the
 * constant pool entry from which it was derived.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class UnresolvedType implements RiType {

    public final ConstantPool constantPool;
    public final TypeDescriptor typeDescriptor;
    public final int cpi;

    /**
     * Creates a new unresolved type for a specified type descriptor.
     *
     * @param typeDescriptor the type's descriptor
     * @param constantPool the constant pool containing the unresolved type reference
     * @param cpi the index in {@code constantPool} of the unresolved type reference
     */
    public UnresolvedType(TypeDescriptor typeDescriptor, ConstantPool constantPool, int cpi) {
        this.constantPool = constantPool;
        this.typeDescriptor = typeDescriptor;
        this.cpi = cpi;
    }

    public String name() {
        return typeDescriptor.string;
    }

    public Class<?> javaClass() {
        throw unresolved("javaClass");
    }

    public boolean hasSubclass() {
        throw unresolved("hasSubclass()");
    }

    public boolean hasFinalizer() {
        throw unresolved("hasFinalizer()");
    }

    public boolean hasFinalizableSubclass() {
        throw unresolved("hasFinalizableSubclass()");
    }

    public boolean isInterface() {
        throw unresolved("isInterface()");
    }

    public boolean isArrayClass() {
        throw unresolved("isArrayClass()");
    }

    public boolean isInstanceClass() {
        throw unresolved("isInstanceClass()");
    }

    public int accessFlags() {
        throw unresolved("accessFlags()");
    }

    public boolean isResolved() {
        return false;
    }

    public boolean isInitialized() {
        throw unresolved("isInitialized()");
    }

    public boolean isSubtypeOf(RiType other) {
        throw unresolved("isSubtypeOf()");
    }

    public boolean isInstance(Object obj) {
        throw unresolved("isInstance()");
    }

    public RiType componentType() {
        return TypeDescriptor.toRiType(typeDescriptor.componentTypeDescriptor(), null);
    }

    public RiType exactType() {
        return null;
    }

    /**
     * Gets the compiler interface type representing an array of this compiler interface type.
     * @return the compiler interface type representing an array with elements of this compiler interface type
     */
    public RiType arrayOf() {
        return TypeDescriptor.toRiType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(typeDescriptor, 1), null);
    }

    public RiMethod resolveMethodImpl(RiMethod method) {
        throw unresolved("resolveMethodImpl()");
    }

    public CiKind kind() {
        return typeDescriptor.toKind().ciKind;
    }

    public CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved class " + typeDescriptor.toString());
    }

    private static boolean isFinalOrPrimitive(ClassActor classActor) {
        return classActor.isFinal() || classActor.isPrimitiveClassActor();
    }

    @Override
    public int hashCode() {
        return typeDescriptor.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public String toString() {
        return name() + " [unresolved]";
    }

    public CiConstant getEncoding(RiType.Representation r) {
        throw unresolved("getEncoding()");
    }

    public CiKind getRepresentationKind(RiType.Representation r) {
        // all portions of a type are represented by objects in Maxine
        return CiKind.Object;
    }
}
