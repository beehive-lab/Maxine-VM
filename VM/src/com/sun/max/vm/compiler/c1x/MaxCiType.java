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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The <code>MaxCiType</code> class represents a compiler interface type,
 * either resolved or unresolved. A resolved compiler interface type refers
 * to the <code>ClassActor</code>, and instances are unique with respect
 * to a <code>MaxCiRuntime</code> instance. Unresolved compiler interface
 * types refer to <code>TypeDescriptors</code>. In either case, both
 * refer to the constant pool from which they were referenced.
 *
 * @author Ben L. Titzer
 */
public class MaxCiType implements CiType {

    final MaxCiConstantPool constantPool;
    ClassActor classActor;
    TypeDescriptor typeDescriptor;
    final BasicType basicType;

    /**
     * Creates a new resolved compiler interface type for the specified class actor.
     * @param constantPool the constant pool
     * @param classActor the class actor
     */
    public MaxCiType(MaxCiConstantPool constantPool, ClassActor classActor) {
        this.constantPool = constantPool;
        this.classActor = classActor;
        this.typeDescriptor = classActor.typeDescriptor;
        this.basicType = kindToBasicType(typeDescriptor.toKind());
    }

    /**
     * Creates a new unresolved compiler interface type for the specified class ref.
     * @param constantPool the constant pool
     * @param classRef the class ref
     */
    public MaxCiType(MaxCiConstantPool constantPool, ClassConstant classRef) {
        this.constantPool = constantPool;
        this.typeDescriptor = classRef.typeDescriptor();
        this.basicType = kindToBasicType(typeDescriptor.toKind());
    }

    /**
     * Creates a new unresolved compiler interface type for the specified type descriptor.
     * @param constantPool the constant pool
     * @param typeDescriptor the type descriptor
     */
    public MaxCiType(MaxCiConstantPool constantPool, TypeDescriptor typeDescriptor) {
        this.constantPool = constantPool;
        if (typeDescriptor instanceof JavaTypeDescriptor.AtomicTypeDescriptor) {
            final JavaTypeDescriptor.AtomicTypeDescriptor atom = (JavaTypeDescriptor.AtomicTypeDescriptor) typeDescriptor;
            this.classActor = ClassActor.fromJava(atom.javaClass);
        }
        this.typeDescriptor = typeDescriptor;
        this.basicType = kindToBasicType(typeDescriptor.toKind());
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
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public Class<?> javaClass() {
        return asClassActor("javaClass").mirror();
    }

    /**
     * Checks whether this compiler interface type has any subclasses in the current
     * runtime environment.
     * @return <code>true</code> if this class has any subclasses
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean hasSubclass() {
        return !asClassActor("hasSubclass()").isFinal(); // TODO: leaf type assumptions
    }

    /**
     * Checks whether this compiler interface type has a finalizer method.
     * @return <code>true</code> if this class has a finalizer method
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean hasFinalizer() {
        return asClassActor("hasFinalizer()").hasFinalizer();
    }

    /**
     * Checks whether this compiler interface type has any subclasses that have finalizers.
     * @return <code>true</code> if this class has any subclasses with finalizers
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean hasFinalizableSubclass() {
        return asClassActor("hasFinalizer()").hasFinalizer(); // TODO: is this correct?
    }

    /**
     * Checks whether this compiler interface type is an interface.
     * @return <code>true</code> if this class is an interface
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isInterface() {
        return asClassActor("isInterface()").isInterfaceActor();
    }

    /**
     * Checks whether this compiler interface type is an instance class.
     * @return <code>true</code> if this class is an instance class
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isInstanceClass() {
        return asClassActor("isInstanceClass()").isTupleClassActor();
    }

    /**
     * Checks whether this compiler interface type is an array.
     * @return <code>true</code> if this class is an array.
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isTypeArrayClass() {
        // TODO: should this check whether this is a primitive array?
        return asClassActor("isTypeArrayClass()").isArrayClassActor();
    }

    /**
     * Checks whether this compiler interface type is final.
     * @return <code>true</code> if this class is final
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isFinal() {
        return asClassActor("isFinal()").isFinal();
    }

    /**
     * Checks whether this compiler interface type is loaded (i.e. resolved).
     * @return <code>true</code> if the type is loaded
     */
    public boolean isLoaded() {
        return classActor != null;
    }

    /**
     * Checks whether this compiler interface type is initialized.
     * @return <code>true</code> if this class is initialized
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isInitialized() {
        return asClassActor("isInitialized()").isInitialized();
    }

    /**
     * Checks whether this compiler interface type is a subtype of another type.
     * @return <code>true</code> if this class is a subtype of the other type
     * @throws MaxCiUnresolved if either class is not resolved
     */
    public boolean isSubtypeOf(CiType other) {
        final MaxCiType otherType = (MaxCiType) other;
        return otherType.asClassActor("isSubtypeOf()").isAssignableFrom(this.asClassActor("isSubtypeOf()"));
    }

    /**
     * Checks whether the specified object is an instance of this compiler interface type.
     * @return <code>true</code> if this object is an instance of this type
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public boolean isInstance(Object obj) {
        return asClassActor("isInstance()").isInstance(obj);
    }

    /**
     * Gets the element type of this compiler interface type.
     * @return the element type if this class is an array
     * @throws MaxCiUnresolved if the class is not resolved
     */
    public CiType elementType() {
        if (classActor instanceof ArrayClassActor) {
            // the type is already resolved
            return constantPool.canonicalCiType(classActor.elementClassActor());
        }
        // the type is not resolved, but we can get the type of the elements
        return new MaxCiType(constantPool, typeDescriptor.elementTypeDescriptor());
    }

    /**
     * Gets the exact type of this compiler interface type. If the class is final or a primitive,
     * or an array of final classes or primitives, this method will return <code>this</code>.
     * Otherwise, or if the type is unresolved, this method will return null.
     * @return the exact type of this type, if it is known; <code>null</code> otherwise
     */
    public CiType exactType() {
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
    public CiType arrayOf() {
        if (classActor != null) {
            return constantPool.canonicalCiType(ArrayClassActor.forComponentClassActor(classActor));
        }
        return new MaxCiType(constantPool, JavaTypeDescriptor.getArrayDescriptorForDescriptor(typeDescriptor, 1));
    }

    /**
     * Resolves the method implementation of the specified compiler interface method for objects
     * of this dynamic type.
     * @param method the method for which to resolve the implementation
     * @return the compiler interface method corresponding to the method implementation that would
     * be selected for objects of this dynamic type
     * @throws MaxCiUnresolved if this type or the method is unresolved
     */
    public CiMethod resolveMethodImpl(CiMethod method) {
        final MethodActor methodActor = ((MaxCiMethod) method).methodActor;
        final ClassActor resolvedClassActor = asClassActor("resolveMethod()");
        if (methodActor instanceof InterfaceMethodActor) {
            // resolve the actual method implementation in this class
            final int index = ((InterfaceMethodActor) methodActor).iIndexInInterface();
            final VirtualMethodActor implementation = resolvedClassActor.getVirtualMethodActorByIIndex(index);
            return constantPool.canonicalCiMethod(implementation);
        } else if (methodActor instanceof VirtualMethodActor) {
            // resolve the actual method implementation in this class
            final int index = ((VirtualMethodActor) methodActor).vTableIndex();
            final VirtualMethodActor implementation = resolvedClassActor.getVirtualMethodActorByVTableIndex(index);
            return constantPool.canonicalCiMethod(implementation);
        } else {
            assert methodActor.isFinal() || methodActor.isPrivate();
            return method;
        }

    }

    /**
     * Gets the basic type for this compiler interface type.
     * @return the basic type
     */
    public BasicType basicType() {
        return basicType;
    }

    ClassActor asClassActor(String operation) {
        if (classActor != null) {
            return classActor;
        }
        throw unresolved(operation);
    }

    ArrayClassActor asArrayClassActor(String operation) {
        return (ArrayClassActor) asClassActor(operation);
    }

    private MaxCiUnresolved unresolved(String operation) {
        throw new MaxCiUnresolved(operation + " not defined for unresolved class " + typeDescriptor.toString());
    }

    private static boolean isFinalOrPrimitive(ClassActor classActor) {
        return classActor.isFinal() || classActor.isPrimitiveClassActor();
    }

    /**
     * Converts a kind to a basic type.
     * @param kind the kind
     * @return the associated basic type
     */
    public static BasicType kindToBasicType(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return BasicType.Byte;
            case BOOLEAN:
                return BasicType.Boolean;
            case SHORT:
                return BasicType.Short;
            case CHAR:
                return BasicType.Char;
            case INT:
                return BasicType.Int;
            case FLOAT:
                return BasicType.Float;
            case LONG:
                return BasicType.Long;
            case DOUBLE:
                return BasicType.Double;
            case WORD:
                return BasicType.Object; // TODO: this is not really an object!
            case REFERENCE:
                return BasicType.Object;
            case VOID:
                return BasicType.Void;
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
     * @return <code>true</code> if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (classActor != null && o instanceof MaxCiType) {
            return classActor == ((MaxCiType) o).classActor;
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

    @Override
    public boolean layoutHelperNeedsSlowPath() {
        throw Util.unimplemented();
    }

    @Override
    public int sizeHelper() {
        throw Util.unimplemented();
    }

    @Override
    public Object encoding() {
        return this;
    }

    @Override
    public boolean isArrayKlass() {
        throw Util.unimplemented();
    }

    @Override
    public int superCheckOffset() {
        throw Util.unimplemented();
    }

}
