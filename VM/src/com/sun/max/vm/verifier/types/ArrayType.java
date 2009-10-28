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
package com.sun.max.vm.verifier.types;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 *
 * @author Doug Simon
 */
public class ArrayType extends ObjectType {

    VerificationType componentType;

    public ArrayType(TypeDescriptor typeDescriptor, Verifier verifier) {
        super(typeDescriptor, verifier);
    }

    @Override
    public VerificationType componentType() {
        if (componentType == null) {
            componentType = verifier.getVerificationType(typeDescriptor().componentTypeDescriptor());
        }
        return componentType;
    }

    /**
     * Gets the {@linkplain ClassActor#elementClassActor() element type} of this array type.
     */
    public VerificationType elementType() {
        return verifier.getVerificationType(typeDescriptor().elementTypeDescriptor());
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        // Any object class is assignable from null
        if (from == NULL) {
            return true;
        }

        if (!from.isArray()) {
            return false;
        }

        // Now both are arrays.
        // If either item's element type isn't a reference type, promote it
        // up to an object or array of object.
        VerificationType thisElement = elementType();
        int thisDimension = JavaTypeDescriptor.getArrayDimensions(typeDescriptor());
        VerificationType fromElement = ((ArrayType) from).elementType();
        int fromDimension = JavaTypeDescriptor.getArrayDimensions(from.typeDescriptor());

        if (thisDimension != fromDimension) {
            if (thisElement == OBJECT && thisDimension < fromDimension) {
                return true;
            }
            return false;
        }

        if (thisElement instanceof ReferenceType) {
            return thisElement.isAssignableFrom(fromElement);
        }
        return thisElement == fromElement;
    }

    @Override
    protected VerificationType mergeWithDifferentType(VerificationType from) {
        // value is null (uninitialized) or value is not a reference type, return bogus.
        if (!(from instanceof ObjectType)) {
            return TOP;
        }

        // value is null reference, unchanged.
        if (from == NULL) {
            return this;
        }

        if (!from.isArray()) {
            if (from == CLONEABLE || from == SERIALIZABLE) {
                return from;
            }
            // value is another non-array object type, return java.lang.Object type.
            return OBJECT;
        }

        // Now both are arrays.
        // If either item's element type isn't a reference type, promote it
        // up to an object or array of object.
        VerificationType thisElement = elementType();
        int thisDimension = JavaTypeDescriptor.getArrayDimensions(typeDescriptor());
        if (!(thisElement instanceof ReferenceType)) {
            if (thisDimension == 0) {
                return TOP;
            }
            thisElement = OBJECT;
            thisDimension--;
        }

        int fromDimension = JavaTypeDescriptor.getArrayDimensions(from.typeDescriptor());
        VerificationType fromElement = ((ArrayType) from).elementType();
        if (!(fromElement instanceof ReferenceType)) {
            if (fromDimension == 0) {
                return TOP;
            }
            fromElement = OBJECT;
            fromDimension--;
        }

        // Both are now objects or arrays of some sort of object type.
        if (thisDimension == fromDimension) {
            // Arrays of the same dimension. Merge their element types.
            final VerificationType mergedElement = thisElement.mergeWith(fromElement);
            if (mergedElement == TOP) {
                return TOP;
            }
            if (thisDimension == 0) {
                return mergedElement;
            }
            return verifier.getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(mergedElement.typeDescriptor(), thisDimension));
        }

        // Arrays of different dimensions. Result is java.lang.Object,
        // with a dimension of the smaller of the two.
        final int newDimension = (thisDimension < fromDimension) ? thisDimension : fromDimension;
        if (newDimension == 0) {
            return OBJECT;
        }

        return verifier.getVerificationType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.OBJECT, newDimension));
    }
}
