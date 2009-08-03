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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Inspector's canonical surrogate for an object implemented as a {@link Hybrid} in the tele VM,
 * one of the three kinds of low level Maxine heap implementation objects.
 * A {@link Hybrid} object has both fields, as in a tuple, and array elements; it cannot be expressed
 * as an ordinary Java type.
 * It is allocated as if it were an array of words, with length specified in the header as with Java arrays.
 * The detailed breakdown of the object's structure is expressed in the subclass {@link TeleHub}.
 *
 * @author Michael Van De Vanter
  */
public abstract class TeleHybridObject extends TeleObject {

    private static final EnumSet<Layout.HeaderField> headerFields = EnumSet.of(HeaderField.HUB, HeaderField.MISC, HeaderField.LENGTH);

    protected TeleHybridObject(TeleVM teleVM, Reference reference) {
        super(teleVM, reference);
    }

    @Override
    public ObjectKind getObjectKind() {
        return ObjectKind.HYBRID;
    }

    @Override
    public Size objectSize() {
        // A hybrid object is sized as if it were all one big array, even though the memory will
        // be used differently in different parts.
        return teleVM().layoutScheme().hybridLayout.getArraySize(readArrayLength());
    }

    @Override
    public EnumSet<Layout.HeaderField> getHeaderFields() {
        return headerFields;
    }

    @Override
    public Address getFieldAddress(FieldActor fieldActor) {
        return getCurrentOrigin().plus(fieldActor.offset());
    }

    @Override
    protected Size getFieldSize(FieldActor fieldActor) {
        return Size.fromInt(fieldActor.kind.width.numberOfBytes);
    }

    @Override
    public Value readFieldValue(FieldActor fieldActor) {
        if (fieldActor.kind == Kind.REFERENCE) {
            return TeleReferenceValue.from(teleVM(), teleVM().wordToReference(reference().readWord(fieldActor.offset())));
        }
        return fieldActor.readValue(reference());
    }

    /**
     * @return length of the word array part of this hybrid in the tele VM
     */
    private int readWordArrayLength() {
        return teleVM().layoutScheme().wordArrayLayout.readLength(reference());
    }

    /**
     * @return an element of the word array part of this hybrid in the tele VM
     */
    public Word readWord(int wordIndex) {
        return teleVM().layoutScheme().wordArrayLayout.getWord(reference(), wordIndex);
    }

    /**
     * @return length of the int array part of this hybrid in the tele VM
     */
    public int readArrayLength() {
        return  teleVM().layoutScheme().arrayHeaderLayout.readLength(reference());
    }

    /**
     * @return an element of the int array part of this hybrid in the tele VM
     */
    public int readArrayInt(int intIndex) {
        return teleVM().layoutScheme().intArrayLayout.getInt(reference(), intIndex);
    }

    @Override
    public Object shallowCopy() {
        final ClassActor classActor = classActorForType();
        final Class<?> javaClass = classActor.toJava();
        try {
            final Object protoHybridObject =  Objects.allocateInstance(javaClass);
            ClassActor holderClassActor = classActor;
            // The tuple part
            do {
                for (FieldActor fieldActor : holderClassActor.localInstanceFieldActors()) {
                    if (!(fieldActor.kind == Kind.REFERENCE || fieldActor.isInjected())) {
                        final Field field = fieldActor.toJava();
                        field.setAccessible(true);
                        try {
                            field.set(protoHybridObject, readFieldValue(fieldActor).asBoxedJavaValue());
                        } catch (IllegalAccessException illegalAccessException) {
                            ProgramError.unexpected("could not access field: " + field, illegalAccessException);
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
            ProgramError.unexpected("could not allocate instance: " + javaClass, instantiationException);
        }
        return null;
    }

}
