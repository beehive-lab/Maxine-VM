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

import com.sun.c1x.ci.CiConstant;
import com.sun.c1x.ci.CiField;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.BasicType;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.classfile.constant.FieldRefConstant;

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

    final MaxCiConstantPool _constantPool;
    final BasicType _basicType; // cached for performance
    FieldRefConstant _fieldRef;
    FieldActor _fieldActor;

    /**
     * Creates a new resolved field with the specified field actor.
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldActor the field actor
     */
    public MaxCiField(MaxCiConstantPool constantPool, FieldActor fieldActor) {
        _constantPool = constantPool;
        _fieldActor = fieldActor;
        _basicType = MaxCiType.kindToBasicType(fieldActor.kind());
    }

    /**
     * Creates a new unresolved field with the specified field ref constant
     * @param constantPool the constant pool in which the field is referenced
     * @param fieldRef the field reference
     */
    public MaxCiField(MaxCiConstantPool constantPool, FieldRefConstant fieldRef) {
        _constantPool = constantPool;
        _fieldRef = fieldRef;
        _basicType = MaxCiType.kindToBasicType(fieldRef.type(_constantPool._constantPool).toKind());
    }

    /**
     * Gets the name of this field as a string.
     * @return the name of the field
     */
    public String name() {
        if (_fieldActor != null) {
            return _fieldActor.name().toString();
        } else {
            return _fieldRef.name(_constantPool._constantPool).toString();
        }
    }

    /**
     * Gets the compiler interface type of this field.
     * @return the compiler interface type
     */
    public CiType type() {
        if (_fieldActor != null) {
            return _constantPool.canonicalCiType(_fieldActor.type());
        } else {
            return new MaxCiType(_constantPool, _fieldRef.type(_constantPool._constantPool));
        }
    }

    /**
     * Gets the basic type for the this field.
     * @return the basic type for this field
     */
    public BasicType basicType() {
        return _basicType;
    }

    /**
     * Gets the holder of this field.
     * @return the compiler interface type that represents the holder
     */
    public CiType holder() {
        if (_fieldActor != null) {
            return _constantPool.canonicalCiType(_fieldActor.holder());
        } else {
            return new MaxCiType(_constantPool, _fieldRef.holder(_constantPool._constantPool));
        }
    }

    /**
     * Checks whether a reference to this field will link successfully at runtime.
     * @param where the type where the link will occur
     * @param opcode the operation to check
     * @return <code>true</code> if a reference to this field will link successfully at runtime
     */
    public boolean willLink(CiType where, int opcode) {
        return _fieldActor != null; // TODO: this is not correct
    }

    /**
     * Checks whether this field is volatile.
     * @return <code>true</code> if the field is volatile
     * @throws MaxCiUnresolved if the field is unresolved
     */
    public boolean isVolatile() {
        if (_fieldActor != null) {
            return _fieldActor.isVolatile();
        }
        throw unresolved("isVolatile()");
    }

    /**
     * Checks whether this field is a constant.
     * @return <code>true</code> if the field is resolved and is a constant
     */
    public boolean isConstant()  {
        return _fieldActor != null && _fieldActor.isConstant();
    }

    /**
     * Gets the offset from the origin of the object for this field.
     * @return the offset in bytes
     * @throws MaxCiUnresolved if the field is unresolved
     */
    public int offset() {
        if (_fieldActor != null) {
            return _fieldActor.offset();
        }
        throw unresolved("offset()");
    }

    /**
     * Gets the constant value for this field, if it is a constant.
     * @return the compiler interface constant for this field
     */
    public CiConstant constantValue() {
        if (_fieldActor != null) {
            return new MaxCiConstant(_fieldActor.constantValue());
        }
        return null;
    }

    private MaxCiUnresolved unresolved(String operation) {
        throw new MaxCiUnresolved(operation + " not defined for unresolved field " + _fieldRef.toString(_constantPool._constantPool));
    }

    /**
     * Gets the hashcode for this compiler interface field. This is the
     * identity hash code for the field actor if the field is resolved,
     * otherwise the identity hash code for this object.
     * @return the hashcode
     */
    public int hashCode() {
        if (_fieldActor != null) {
            return System.identityHashCode(_fieldActor); // use the field actor's hashcode
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
    public boolean equals(Object o) {
        if (_fieldActor != null && o instanceof MaxCiField) {
            return _fieldActor == ((MaxCiField)o)._fieldActor;
        }
        return o == this;
    }

}
