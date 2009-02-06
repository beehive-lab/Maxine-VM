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
package com.sun.max.tele.object;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Inspector's canonical surrogate for an object implemented as an {@link Array} in the {@link teleVM},
 * one of the three kinds of low level Maxine heap implementation objects.
 *
 * @author Michael Van De Vanter
  */
public class TeleArrayObject extends TeleObject implements ArrayProvider {

    private static final Logger LOGGER = Logger.getLogger(TeleArrayObject.class.getName());

    private int _length = -1;

    protected TeleArrayObject(TeleVM teleVM, Reference reference) {
        super(teleVM, reference);
    }

    @Override
    public ObjectKind getObjectKind() {
        return ObjectKind.ARRAY;
    }

    /**
     * @return length of this array in the {@link teleVM}.
     */
    public int getLength() {
        if (_length < 0) {
            _length = teleVM().layoutScheme().arrayHeaderLayout().readLength(reference());
        }
        return _length;
    }

    public Kind componentKind() {
        return classActorForType().componentClassActor().kind();
    }

    @Override
    public Set<FieldActor> getFieldActors() {
        return new HashSet<FieldActor>();
    }

    /**
     * @param index
     * @return the value read from the specified field in this array in the {@link teleVM}
     */
    public Value readElementValue(int index) {
        return teleVM().getElementValue(componentKind(), reference(), index);
    }

    @Override
    public Value readFieldValue(FieldActor fieldActor) {
        Problem.error("Maxine Array objects don't contain fields");
        return null;
    }

    @Override
    public Object shallowCopy() {
        final int length = getLength();
        if (componentKind() == Kind.REFERENCE) {
            final Reference[] newRefArray = new Reference[length];
            for (int index = 0; index < length; index++) {
                newRefArray[index] = readElementValue(index).asReference();
            }
            return newRefArray;
        }
        final Class<?> componentJavaClass = classActorForType().componentClassActor().toJava();
        final Object newArray = Array.newInstance(componentJavaClass, length);
        for (int index = 0; index < length; index++) {
            Array.set(newArray, index, readElementValue(index).asBoxedJavaValue());
        }
        return newArray;
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        final Kind componentKind = componentKind();
        final int length = getLength();
        final Class<?> componentJavaClass = classActorForType().componentClassActor().toJava();
        final Object newArray = Array.newInstance(componentJavaClass, length);
        context.register(this, newArray);
        for (int index = 0; index < length; index++) {
            final Value value = readElementValue(index);
            final Object newJavaValue;
            if (componentKind == Kind.REFERENCE) {
                final TeleObject teleValueObject = teleVM().makeTeleObject(value.asReference());
                if (teleValueObject == null) {
                    newJavaValue = null;
                } else {
                    newJavaValue = teleValueObject.makeDeepCopy(context);
                }
            } else {
                newJavaValue = value.asBoxedJavaValue();
            }
            Array.set(newArray, index, newJavaValue);
        }
        return newArray;
    }

    public ArrayTypeProvider getArrayType() {
        final ReferenceTypeProvider referenceTypeProvider = this.getReferenceType();
        final ArrayTypeProvider arrayTypeProvider = (ArrayTypeProvider) referenceTypeProvider;
        return arrayTypeProvider;
    }

    @Override
    public VMValue getValue(int i) {
        return teleVM().maxineValueToJDWPValue(readElementValue(i));
    }

    @Override
    public int length() {
        return getLength();
    }

    @Override
    public void setValue(int i, VMValue value) {
        LOGGER.info("Command received to SET ARRAY at index " + i + " + to + " + value);
    }

}
