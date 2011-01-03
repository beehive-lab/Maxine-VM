/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Inspector's canonical surrogate for an object implemented as an {@link Array} in the {@link TeleVM},
 * one of the three kinds of low level Maxine heap implementation objects.
 *
 * @author Michael Van De Vanter
  */
public class TeleArrayObject extends TeleObject implements ArrayProvider {

    private static final Logger LOGGER = Logger.getLogger(TeleArrayObject.class.getName());

    private int length = -1;

    private final Kind componentKind;

    protected TeleArrayObject(TeleVM vm, Reference reference, Kind componentKind, SpecificLayout layout) {
        super(vm, reference, layout);
        this.componentKind = componentKind;
    }

    @Override
    public ObjectKind kind() {
        return ObjectKind.ARRAY;
    }

    @Override
    public HeaderField[] headerFields() {
        return Layout.arrayLayout().headerFields();
    }

    /**
     * @return length of this array in the VM.
     */
    public int getLength() {
        if (length < 0) {
            length = Layout.readArrayLength(reference());
        }
        return length;
    }

    public TypeDescriptor componentType() {
        return classActorForObjectType().componentClassActor().typeDescriptor;
    }

    public Kind componentKind() {
        return componentKind;
    }

    @Override
    public Size objectSize() {
        return Layout.getArraySize(componentKind(), length);
    }

    @Override
    public Set<FieldActor> getFieldActors() {
        return new HashSet<FieldActor>();
    }

    /**
     * @param index
     * @return the value read from the specified field in this array in the VM
     */
    public Value readElementValue(int index) {
        return vm().getElementValue(componentKind(), reference(), index);
    }

    public void copyElements(int srcIndex, Object dst, int dstIndex, int length) {
        vm().copyElements(componentKind(), reference(), srcIndex, dst, dstIndex, length);
    }

    @Override
    public  Address fieldAddress(FieldActor fieldActor) {
        throw TeleError.unexpected("Maxine Array objects don't contain fields");
    }

    @Override
    public Size fieldSize(FieldActor fieldActor) {
        throw TeleError.unexpected("Maxine Array objects don't contain fields");
    }

    @Override
    public Value readFieldValue(FieldActor fieldActor) {
        throw TeleError.unexpected("Maxine Array objects don't contain fields");
    }

    @Override
    public Object shallowCopy() {
        final int length = getLength();
        if (componentKind().isReference) {
            final Reference[] newRefArray = new Reference[length];
            for (int index = 0; index < length; index++) {
                newRefArray[index] = readElementValue(index).asReference();
            }
            return newRefArray;
        }
        final Class<?> componentJavaClass = classActorForObjectType().componentClassActor().toJava();
        final Object newArray = Array.newInstance(componentJavaClass, length);
        for (int index = 0; index < length; index++) {
            Array.set(newArray, index, readElementValue(index).asBoxedJavaValue());
        }
        return newArray;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        final Kind componentKind = componentKind();
        final int length = getLength();
        final Class<?> componentJavaClass = classActorForObjectType().componentClassActor().toJava();
        final Object newArray = Array.newInstance(componentJavaClass, length);
        context.register(this, newArray, true);
        if (length != 0) {
            if (componentKind != Kind.REFERENCE) {
                copyElements(0, newArray, 0, length);
            } else {
                Object[] referenceArray = (Object[]) newArray;
                for (int index = 0; index < length; index++) {
                    final Value value = readElementValue(index);
                    final TeleObject teleValueObject = heap().makeTeleObject(value.asReference());
                    if (teleValueObject != null) {
                        referenceArray[index] = teleValueObject.makeDeepCopy(context);
                    }
                }
            }
        }
        return newArray;
    }

    public ArrayTypeProvider getArrayType() {
        return (ArrayTypeProvider) this.getReferenceType();
    }

    public VMValue getValue(int i) {
        return vm().maxineValueToJDWPValue(readElementValue(i));
    }

    public int length() {
        return getLength();
    }

    public void setValue(int i, VMValue value) {
        LOGGER.info("Command received to SET ARRAY at index " + i + " + to + " + value);
    }

}
