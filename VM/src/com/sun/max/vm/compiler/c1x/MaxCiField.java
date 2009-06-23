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
import com.sun.c1x.value.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.host.*;

/**
 * The <code>MaxCiField</code> implements a compiler interface field. A field can
 * be either resolved or unresolved. A resolved field has a reference to its
 * associated <code>FieldActor</code> and unresolved field has a reference
 * to its <code>FieldRefConstant</code> some method calls are only appropriate
 * for resolved fields and will result in a <code>MaxCiUnresolved</code>
 * exception if called on an unresolved field.
 *
 * @author Ben L. Titzer
 */
public class MaxCiField implements CiField {

    final MaxCiConstantPool constantPool;
    final BasicType basicType; // cached for performance
    FieldRefConstant fieldRef;
    FieldActor fieldActor;

    /**
     * Creates a new resolved field with the specified field actor.
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldActor the field actor
     */
    public MaxCiField(MaxCiConstantPool constantPool, FieldActor fieldActor) {
        this.constantPool = constantPool;
        this.fieldActor = fieldActor;
        this.basicType = MaxCiType.kindToBasicType(fieldActor.kind);
    }

    /**
     * Creates a new unresolved field with the specified field ref constant.
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldRef the field reference
     */
    public MaxCiField(MaxCiConstantPool constantPool, FieldRefConstant fieldRef) {
        this.constantPool = constantPool;
        this.fieldRef = fieldRef;
        this.basicType = MaxCiType.kindToBasicType(fieldRef.type(constantPool.constantPool).toKind());
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
    public CiType type() {
        if (fieldActor != null) {
            return constantPool.canonicalCiType(fieldActor.type());
        }
        return new MaxCiType(constantPool, fieldRef.type(constantPool.constantPool));
    }

    /**
     * Gets the basic type for the this field.
     * @return the basic type for this field
     */
    public BasicType basicType() {
        return basicType;
    }

    /**
     * Gets the holder of this field.
     * @return the compiler interface type that represents the holder
     */
    public CiType holder() {
        if (fieldActor != null) {
            return constantPool.canonicalCiType(fieldActor.holder());
        }
        return new MaxCiType(constantPool, fieldRef.holder(constantPool.constantPool));
    }

    /**
     * Checks whether this compiler interface field is loaded (i.e. resolved).
     * @return <code>true</code> if this field is loaded
     */
    public boolean isLoaded() {
        return fieldActor != null;
    }

    /**
     * Checks whether this field is static.
     * @return <code>true</code> if this field is static
     * @throws MaxCiUnresolved if the field is unresolved
     */
    public boolean isStatic() {
        if (fieldActor != null) {
            return fieldActor.isStatic();
        }
        throw unresolved("isStatic()");
    }

    /**
     * Checks whether this field is volatile.
     * @return <code>true</code> if the field is volatile
     * @throws MaxCiUnresolved if the field is unresolved
     */
    public boolean isVolatile() {
        if (fieldActor != null) {
            return fieldActor.isVolatile();
        }
        throw unresolved("isVolatile()");
    }

    /**
     * Checks whether this field is a constant.
     * @return <code>true</code> if the field is resolved and is a constant
     */
    public boolean isConstant()  {
        return fieldActor != null && fieldActor.isConstant();
    }

    /**
     * Gets the offset from the origin of the object for this field.
     * @return the offset in bytes
     * @throws MaxCiUnresolved if the field is unresolved
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
            return new MaxCiConstant(HostTupleAccess.readValue(null, fieldActor));
        }
        return null;
    }

    private MaxCiUnresolved unresolved(String operation) {
        throw new MaxCiUnresolved(operation + " not defined for unresolved field " + fieldRef.toString(constantPool.constantPool));
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
     * @return <code>true</code> if this object is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (fieldActor != null && o instanceof MaxCiField) {
            return fieldActor == ((MaxCiField) o).fieldActor;
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
