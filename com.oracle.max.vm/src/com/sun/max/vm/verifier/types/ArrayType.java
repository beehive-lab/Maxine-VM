/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.verifier.types;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
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
