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

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Inspector's canonical surrogate for an object implemented as a {@link Hybrid} in the VM,
 * one of the three kinds of low level Maxine heap implementation objects.
 * A {@link Hybrid} object has both fields, as in a tuple, and array elements; it cannot be expressed
 * as an ordinary Java type.
 * It is allocated as if it were an array of words, with length specified in the header as with Java arrays.
 * The detailed breakdown of the object's structure is expressed in the subclass {@link TeleHub}.
 *
 * @author Michael Van De Vanter
  */
public abstract class TeleHybridObject extends TeleObject {

    protected TeleHybridObject(TeleVM teleVM, Reference reference) {
        super(teleVM, reference, Layout.hybridLayout());
    }

    @Override
    public ObjectKind kind() {
        return ObjectKind.HYBRID;
    }

    @Override
    public Size objectSize() {
        // A hybrid object is sized as if it were all one big array, even though the memory will
        // be used differently in different parts.
        return Layout.hybridLayout().getArraySize(readArrayLength());
    }

    @Override
    public HeaderField[] headerFields() {
        return Layout.hybridLayout().headerFields();
    }

    @Override
    public Address fieldAddress(FieldActor fieldActor) {
        return origin().plus(fieldActor.offset());
    }

    @Override
    public Size fieldSize(FieldActor fieldActor) {
        return Size.fromInt(fieldActor.kind.width.numberOfBytes);
    }

    @Override
    public Value readFieldValue(FieldActor fieldActor) {
        if (fieldActor.kind.isReference) {
            return TeleReferenceValue.from(vm(), vm().wordToReference(reference().readWord(fieldActor.offset())));
        }
        return fieldActor.readValue(reference());
    }

    /**
     * @return length of the word array part of this hybrid in the VM
     */
    private int readWordArrayLength() {
        return Layout.wordArrayLayout().readLength(reference());
    }

    /**
     * @return an element of the word array part of this hybrid in the VM
     */
    public Word readWord(int wordIndex) {
        return Layout.getWord(reference(), wordIndex);
    }

    /**
     * @return length of the int array part of this hybrid in the VM
     */
    public int readArrayLength() {
        return  Layout.readArrayLength(reference());
    }

    /**
     * @return an element of the int array part of this hybrid in the VM
     */
    public int readArrayInt(int intIndex) {
        return Layout.getInt(reference(), intIndex);
    }

    @Override
    public Object shallowCopy() {
        final ClassActor classActor = classActorForObjectType();
        final Class<?> javaClass = classActor.toJava();
        try {
            final Object protoHybridObject =  Objects.allocateInstance(javaClass);
            ClassActor holderClassActor = classActor;
            // The tuple part
            do {
                for (FieldActor fieldActor : holderClassActor.localInstanceFieldActors()) {
                    if (!(fieldActor.kind.isReference || fieldActor.isInjected())) {
                        final Field field = fieldActor.toJava();
                        field.setAccessible(true);
                        try {
                            field.set(protoHybridObject, readFieldValue(fieldActor).asBoxedJavaValue());
                        } catch (IllegalAccessException illegalAccessException) {
                            TeleError.unexpected("could not access field: " + field, illegalAccessException);
                        }
                    }
                }
                holderClassActor = holderClassActor.superClassActor;
            } while (holderClassActor != null);
            // Expand to include the array part
            final int wordArrayLength = readWordArrayLength();
            Hybrid newHybridObject = (Hybrid) protoHybridObject;
            newHybridObject = newHybridObject.expand(wordArrayLength);
            for (int wordIndex = newHybridObject.firstWordIndex(); wordIndex <= newHybridObject.lastWordIndex(); wordIndex++) {
                newHybridObject.setWord(wordIndex, readWord(wordIndex));
            }
            for (int intIndex = newHybridObject.firstIntIndex(); intIndex <= newHybridObject.lastIntIndex(); intIndex++) {
                newHybridObject.setInt(intIndex, readArrayInt(intIndex));
            }
            return newHybridObject;
        } catch (InstantiationException instantiationException) {
            TeleError.unexpected("could not allocate instance: " + javaClass, instantiationException);
        }
        return null;
    }

}
