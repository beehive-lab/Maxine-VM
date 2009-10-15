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

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The {@code MaxRiType} class represents a compiler interface type,
 * either resolved or unresolved. A resolved compiler interface type refers
 * to the {@code ClassActor}, and instances are unique with respect
 * to a {@code MaxRiRuntime} instance. Unresolved compiler interface
 * types refer to {@code TypeDescriptors}. In either case, both
 * refer to the constant pool from which they were referenced.
 *
 * @author Ben L. Titzer
 */
public class MaxRiType implements RiType {

    final MaxRiConstantPool constantPool;
    ClassActor classActor;
    TypeDescriptor typeDescriptor;
    final CiKind basicType;
    final int cpi;

    /**
     * Creates a new resolved compiler interface type for the specified class actor.
     * @param constantPool the constant pool
     * @param classActor the class actor
     */
    public MaxRiType(MaxRiConstantPool constantPool, ClassActor classActor, int cpi) {
        this.constantPool = constantPool;
        this.classActor = classActor;
        this.typeDescriptor = classActor.typeDescriptor;
        this.basicType = kindToBasicType(typeDescriptor.toKind());
        this.cpi = cpi;
    }

    /**
     * Creates a new unresolved compiler interface type for the specified class ref.
     * @param constantPool the constant pool
     * @param classRef the class ref
     * @param cpi the constant pool index
     */
    public MaxRiType(MaxRiConstantPool constantPool, ClassConstant classRef, int cpi) {
        this.constantPool = constantPool;
        this.typeDescriptor = classRef.typeDescriptor();
        this.basicType = kindToBasicType(typeDescriptor.toKind());
        this.cpi = cpi;
        assert cpi >= 0 : "must have valid cpi for resolution";
    }

    /**
     * Creates a new unresolved compiler interface type for the specified type descriptor.
     * @param constantPool the constant pool
     * @param typeDescriptor the type descriptor
     * @param cpi the constant pool index
     */
    public MaxRiType(MaxRiConstantPool constantPool, TypeDescriptor typeDescriptor, int cpi) {
        this.constantPool = constantPool;
        if (typeDescriptor instanceof JavaTypeDescriptor.AtomicTypeDescriptor) {
            final JavaTypeDescriptor.AtomicTypeDescriptor atom = (JavaTypeDescriptor.AtomicTypeDescriptor) typeDescriptor;
            this.classActor = ClassActor.fromJava(atom.javaClass);
        }

        this.typeDescriptor = typeDescriptor;
        this.basicType = kindToBasicType(typeDescriptor.toKind());
        this.cpi = cpi;
        assert classActor != null || cpi >= 0 : "must have valid cpi for resolution";
    }

    /**
     * Gets the name of this type as a string.
     * @return the name
     */
    public String name() {
        return typeDescriptor.toString();
    }

    /**
     * Gets the Java class object for this compiler interface type.
     * @return the class object
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public Class<?> javaClass() {
        return asClassActor("javaClass").mirror();
    }

    /**
     * Checks whether this compiler interface type has any subclasses in the current
     * runtime environment.
     * @return {@code true} if this class has any subclasses
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean hasSubclass() {
        return !asClassActor("hasSubclass()").isFinal(); // TODO: leaf type assumptions
    }

    /**
     * Checks whether this compiler interface type has a finalizer method.
     * @return {@code true} if this class has a finalizer method
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean hasFinalizer() {
        return asClassActor("hasFinalizer()").hasFinalizer();
    }

    /**
     * Checks whether this compiler interface type has any subclasses that have finalizers.
     * @return {@code true} if this class has any subclasses with finalizers
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean hasFinalizableSubclass() {
        return asClassActor("hasFinalizer()").hasFinalizer(); // TODO: is this correct?
    }

    /**
     * Checks whether this compiler interface type is an interface.
     * @return {@code true} if this class is an interface
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isInterface() {
        return asClassActor("isInterface()").isInterfaceActor();
    }

    /**
     * Checks whether this compiler interface type is an array class.
     * @return {@code true} if this class is an interface
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isArrayKlass() {
        return asClassActor("isArrayClass()") instanceof ArrayClassActor;
    }

    /**
     * Checks whether this compiler interface type is an instance class.
     * @return {@code true} if this class is an instance class
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isInstanceClass() {
        final ClassActor classActor = asClassActor("isInstanceClass()");
        return classActor.isTupleClassActor() || classActor.isHybridClassActor();
    }

    /**
     * Checks whether this compiler interface type is final.
     * @return {@code true} if this class is final
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isFinal() {
        return asClassActor("isFinal()").isFinal();
    }

    /**
     * Checks whether this compiler interface type is loaded (i.e. resolved).
     * @return {@code true} if the type is loaded
     */
    public boolean isLoaded() {
        return classActor != null;
    }

    /**
     * Checks whether this compiler interface type is initialized.
     * @return {@code true} if this class is initialized
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isInitialized() {
        return asClassActor("isInitialized()").isInitialized();
    }

    /**
     * Checks whether this compiler interface type is a subtype of another type.
     * @return {@code true} if this class is a subtype of the other type
     * @throws MaxRiUnresolved if either class is not resolved
     */
    public boolean isSubtypeOf(RiType other) {
        final MaxRiType otherType = (MaxRiType) other;
        return otherType.asClassActor("isSubtypeOf()").isAssignableFrom(this.asClassActor("isSubtypeOf()"));
    }

    /**
     * Checks whether the specified object is an instance of this compiler interface type.
     * @return {@code true} if this object is an instance of this type
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public boolean isInstance(Object obj) {
        ClassActor classActor = asClassActor("isInstance()");
        if (MaxineVM.isHosted()) {
            return classActor.toJava().isInstance(obj);
        }
        return classActor.isInstance(obj);
    }

    /**
     * Gets the component type of this compiler interface type.
     * @return the component type if this class is an array
     * @throws MaxRiUnresolved if the class is not resolved
     */
    public RiType componentType() {
        if (classActor instanceof ArrayClassActor) {
            // the type is already resolved
            return constantPool.runtime.canonicalRiType(classActor.componentClassActor(), constantPool, -1);
        }
        // the type is not resolved, but we can get the type of the elements
        // TODO: what type to use for an unresolved component type?
        return new MaxRiType(constantPool, typeDescriptor.componentTypeDescriptor(), -1);
    }

    /**
     * Gets the exact type of this compiler interface type. If the class is final or a primitive,
     * or an array of final classes or primitives, this method will return {@code this}.
     * Otherwise, or if the type is unresolved, this method will return null.
     * @return the exact type of this type, if it is known; {@code null} otherwise
     */
    public RiType exactType() {
        if (classActor != null) {
            if (isFinalOrPrimitive(classActor)) {
                return this;
            }
            if (classActor.isArrayClassActor() && isFinalOrPrimitive(classActor.componentClassActor())) {
                return this;
            }
        }
        return null;
    }

    /**
     * Gets the compiler interface type representing an array of this compiler interface type.
     * @return the compiler interface type representing an array with elements of this compiler interface type
     */
    public RiType arrayOf() {
        if (classActor != null) {
            ArrayClassActor arrayClassActor = MaxineVM.usingTarget(new Function<ArrayClassActor>() {
                public ArrayClassActor call() {
                    return ArrayClassActor.forComponentClassActor(classActor);
                }
            });
            return constantPool.runtime.canonicalRiType(arrayClassActor, constantPool, -1);
        }
        // TODO: what cpi to use for an unresolved constant?
        return new MaxRiType(constantPool, JavaTypeDescriptor.getArrayDescriptorForDescriptor(typeDescriptor, 1), -1);
    }

    /**
     * Resolves the method implementation of the specified compiler interface method for objects
     * of this dynamic type.
     * @param method the method for which to resolve the implementation
     * @return the compiler interface method corresponding to the method implementation that would
     * be selected for objects of this dynamic type
     * @throws MaxRiUnresolved if this type or the method is unresolved
     */
    public RiMethod resolveMethodImpl(RiMethod method) {
        final MethodActor methodActor = ((MaxRiMethod) method).methodActor;
        final ClassActor resolvedClassActor = asClassActor("resolveMethod()");
        if (methodActor instanceof InterfaceMethodActor) {
            InterfaceMethodActor interfaceActor = (InterfaceMethodActor) methodActor;
            final int interfaceIIndex = classActor.dynamicHub().getITableIndex(interfaceActor.holder().id) - classActor.dynamicHub().iTableStartIndex;
            final VirtualMethodActor implementation = classActor.getVirtualMethodActorByIIndex(interfaceIIndex + interfaceActor.iIndexInInterface());
            return constantPool.runtime.canonicalRiMethod(implementation, constantPool, -1);
        } else if (methodActor instanceof VirtualMethodActor) {

            // resolve the actual method implementation in this class
            final int index = ((VirtualMethodActor) methodActor).vTableIndex();
            final VirtualMethodActor implementation = resolvedClassActor.getVirtualMethodActorByVTableIndex(index);
            return constantPool.runtime.canonicalRiMethod(implementation, constantPool, -1);
        } else {
            assert methodActor.isFinal() || methodActor.isPrivate();
            return method;
        }

    }

    /**
     * Gets the basic type for this compiler interface type.
     * @return the basic type
     */
    public CiKind basicType() {
        return basicType;
    }

    ClassActor asClassActor(String operation) {
        if (classActor != null) {
            return classActor;
        }
        throw unresolved(operation);
    }

    private MaxRiUnresolved unresolved(String operation) {
        throw new MaxRiUnresolved(operation + " not defined for unresolved class " + typeDescriptor.toString());
    }

    private static boolean isFinalOrPrimitive(ClassActor classActor) {
        return classActor.isFinal() || classActor.isPrimitiveClassActor();
    }

    /**
     * Converts a kind to a basic type.
     * @param kind the kind
     * @return the associated basic type
     */
    public static CiKind kindToBasicType(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return CiKind.Byte;
            case BOOLEAN:
                return CiKind.Boolean;
            case SHORT:
                return CiKind.Short;
            case CHAR:
                return CiKind.Char;
            case INT:
                return CiKind.Int;
            case FLOAT:
                return CiKind.Float;
            case LONG:
                return CiKind.Long;
            case DOUBLE:
                return CiKind.Double;
            case WORD:
                return C1XOptions.SupportWordTypes ? CiKind.Word : CiKind.Object;
            case REFERENCE:
                return CiKind.Object;
            case VOID:
                return CiKind.Void;
            default:
                throw ProgramError.unknownCase();
        }
    }

    /**
     * Gets the hashcode for this compiler interface type. This is the
     * identity hash code for the class actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (classActor != null) {
            return System.identityHashCode(classActor); // use the class actor's hashcode
        }
        return System.identityHashCode(this);
    }

    /**
     * Checks whether this compiler interface type is equal to another object.
     * If the type is resolved, the objects are equivalent if the refer
     * to the same class actor. Otherwise they are equivalent if they
     * reference the same compiler interface type object.
     * @param o the object to check
     * @return {@code true} if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (classActor != null && o instanceof MaxRiType) {
            return classActor == ((MaxRiType) o).classActor;
        }
        return o == this;
    }

    /**
     * Converts this compiler interface type to a string.
     */
    @Override
    public String toString() {
        if (classActor != null) {
            return classActor.toString();
        }
        return typeDescriptor.toString() + " [unresolved]";
    }

    public CiConstant getEncoding(RiType.Representation r) {
        switch (r) {
            case StaticFields:
                // static fields are stored in static tuple
                // XXX: cache the constant for repeated accesses
                return CiConstant.forObject(asClassActor("getEncoding()").staticTuple());
            case JavaClass:
                // java class object is the "mirror"
                // XXX: cache the constant for repeated accesses
                return CiConstant.forObject(asClassActor("getEncoding()").mirror());
            case ObjectHub:
                // hub is the dynamic hub
                // XXX: cache the constant for repeated accesses
                return CiConstant.forObject(asClassActor("getEncoding()").dynamicHub());
            case TypeInfo:
                // type info is represented by the class actor
                // XXX: cache the constant for repeated accesses
                return CiConstant.forObject(asClassActor("getEncoding()"));
        }
        throw ProgramError.unexpected();
    }

    public CiKind getBasicType(RiType.Representation r) {
        // all portions of a type are represented by objects in Maxine
        return CiKind.Object;
    }
}
