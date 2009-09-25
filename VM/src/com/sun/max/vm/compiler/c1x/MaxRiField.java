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
import com.sun.c1x.ri.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.value.*;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.reference.Reference;

/**
 * The {@code MaxRiField} implements a compiler interface field. A field can
 * be either resolved or unresolved. A resolved field has a reference to its
 * associated {@code FieldActor} and unresolved field has a reference
 * to its {@code FieldRefConstant} some method calls are only appropriate
 * for resolved fields and will result in a {@code MaxCiUnresolved}
 * exception if called on an unresolved field.
 *
 * @author Ben L. Titzer
 */
public class MaxRiField implements RiField {

    final MaxRiConstantPool constantPool;
    final CiKind basicType; // cached for performance
    final FieldRefConstant fieldRef;
    final int cpi;
    FieldActor fieldActor;

    /**
     * Creates a new resolved field with the specified field actor.
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldActor the field actor
     */
    public MaxRiField(MaxRiConstantPool constantPool, FieldActor fieldActor) {
        this.constantPool = constantPool;
        this.fieldActor = fieldActor;
        this.basicType = MaxRiType.kindToBasicType(fieldActor.kind);
        this.fieldRef = null;
        this.cpi = 0;
    }

    /**
     * Creates a new unresolved field with the specified field ref constant.
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldRef the field reference
     * @param cpi the constant pool index
     */
    public MaxRiField(MaxRiConstantPool constantPool, FieldRefConstant fieldRef, int cpi) {
        this.constantPool = constantPool;
        this.fieldRef = fieldRef;
        this.basicType = MaxRiType.kindToBasicType(fieldRef.type(constantPool.constantPool).toKind());
        this.cpi = cpi;
    }

    /**
     * Gets the name of this field as a string.
     * @return the name of the field
     */
    public String name() {
        if (fieldActor != null) {
            return fieldActor.name.string;
        }
        return fieldRef.name(constantPool.constantPool).string;
    }

    /**
     * Gets the compiler interface type of this field.
     * @return the compiler interface type
     */
    public RiType type() {
        if (fieldActor != null) {
            return constantPool.runtime.canonicalRiType(fieldActor.type(), constantPool);
        }
        // TODO: get the correct CPI of the field's type
        return new MaxRiType(constantPool, fieldRef.type(constantPool.constantPool), 0);
    }

    /**
     * Gets the basic type for the this field.
     * @return the basic type for this field
     */
    public CiKind kind() {
        return basicType;
    }

    /**
     * Gets the holder of this field.
     * @return the compiler interface type that represents the holder
     */
    public RiType holder() {
        if (fieldActor != null) {
            return constantPool.runtime.canonicalRiType(fieldActor.holder(), constantPool);
        }
        // TODO: get the correct cpi of the field's holder
        return new MaxRiType(constantPool, fieldRef.holder(constantPool.constantPool), 0);
    }

    /**
     * Checks whether this compiler interface field is loaded (i.e. resolved).
     * @return {@code true} if this field is loaded
     */
    public boolean isLoaded() {
        return fieldActor != null;
    }

    /**
     * Checks whether this field is static.
     * @return {@code true} if this field is static
     * @throws MaxRiUnresolved if the field is unresolved
     */
    public boolean isStatic() {
        if (fieldActor != null) {
            return fieldActor.isStatic();
        }
        throw unresolved("isStatic()");
    }

    /**
     * Checks whether this field is volatile.
     * @return {@code true} if the field is volatile
     * @throws MaxRiUnresolved if the field is unresolved
     */
    public boolean isVolatile() {
        if (fieldActor != null) {
            return fieldActor.isVolatile();
        }
        throw unresolved("isVolatile()");
    }

    /**
     * Checks whether this field is a constant.
     * @return {@code true} if the field is resolved and is a constant
     */
    public boolean isConstant()  {
        return fieldActor != null && fieldActor.isConstant();
    }

    /**
     * Gets the offset from the origin of the object for this field.
     * @return the offset in bytes
     * @throws MaxRiUnresolved if the field is unresolved
     */
    public int offset() {
        if (fieldActor != null) {
            return fieldActor.offset();
        }
        throw unresolved("offset()");
    }

    /**
     * Gets the constant value for this field, if it is a constant.
     * @return the compiler interface constant for this field
     */
    public CiConstant constantValue() {
        if (fieldActor != null && fieldActor.isConstant()) {
            Value v;
            if (MaxineVM.isPrototyping()) {
                v = HostTupleAccess.readValue(null, fieldActor);
            } else {
                v = fieldActor.readValue(Reference.fromJava(fieldActor.holder().staticTuple()));
            }
            return new CiConstant(MaxRiType.kindToBasicType(v.kind()), v.asBoxedJavaValue());
        }
        return null;
    }

    private MaxRiUnresolved unresolved(String operation) {
        throw new MaxRiUnresolved(operation + " not defined for unresolved field " + fieldRef.toString(constantPool.constantPool));
    }

    /**
     * Gets the hashcode for this compiler interface field. This is the
     * identity hash code for the field actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (fieldActor != null) {
            return System.identityHashCode(fieldActor); // use the field actor's hashcode
        }
        return System.identityHashCode(this);
    }

    /**
     * Checks whether this compiler interface field is equal to another object.
     * If the field is resolved, the objects are equivalent if the refer
     * to the same field actor. Otherwise they are equivalent if they
     * reference the same compiler interface field object.
     * @param o the object to check
     * @return {@code true} if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (fieldActor != null && o instanceof MaxRiField) {
            return fieldActor == ((MaxRiField) o).fieldActor;
        }
        return o == this;
    }

    /**
     * Converts this compiler interface field to a string.
     */
    @Override
    public String toString() {
        if (fieldActor != null) {
            return fieldActor.toString();
        }
        return fieldRef.toString() + " [unresolved]";
    }

}
