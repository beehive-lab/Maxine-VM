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
/*VCSID=556238ab-6190-40bd-a032-76f2ef829cc9*/
package com.sun.max.tele.object;

import java.lang.reflect.*;

import com.sun.max.tele.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for an object implemented as a {@link Tuple} in the tele VM,
 * one of the three kinds of low level Maxine heap implementation objects.
 *
 * @author Michael Van De Vanter
  */
public class TeleTupleObject extends TeleObject {

    protected TeleTupleObject(TeleVM teleVM, Reference reference) {
        super(teleVM, reference);
    }

    @Override
    public Value readFieldValue(FieldActor fieldActor) {
        if (fieldActor.kind() == Kind.REFERENCE) {
            return TeleReferenceValue.from(teleVM(), teleVM().wordToReference(reference().readWord(fieldActor.offset())));
        }
        return fieldActor.readValue(reference());
    }

    @Override
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        final Class<?> javaClass = classActorForType().toJava();
        if (IrMethod.class.isAssignableFrom(javaClass)) {
            final Reference classMethodActorReference = teleVM().fields().IrMethod_classMethodActor(javaClass.asSubclass(IrMethod.class)).readReference(reference());
            return (TeleClassMethodActor) TeleObject.make(teleVM(), classMethodActorReference);
        }
        return null;
    }

    @Override
    public Object shallowCopy() {
        final ClassActor classActor = classActorForType();
        final Class<?> javaClass = classActor.toJava();
        try {
            final Object newTupleObject = _unsafe.allocateInstance(javaClass);
            ClassActor holderClassActor = classActor;
            do {
                for (FieldActor fieldActor : holderClassActor.localInstanceFieldActors()) {
                    if (!(fieldActor.kind() == Kind.REFERENCE || fieldActor.isInjected())) {
                        final Field field = fieldActor.toJava();
                        field.setAccessible(true);
                        final Value fieldValue = readFieldValue(fieldActor);
                        final Object asBoxedJavaValue = fieldValue.asBoxedJavaValue();
                        try {
                            field.set(newTupleObject, asBoxedJavaValue);
                        } catch (IllegalAccessException illegalAccessException) {
                            throw new TeleError("could not access field: " + field, illegalAccessException);
                        } catch (IllegalArgumentException illegalArgumentException) {
                            throw new TeleError("illegal argument field: " + field, illegalArgumentException);
                        }
                    }
                }
                holderClassActor = holderClassActor.superClassActor();
            } while (holderClassActor != null);
            return newTupleObject;
        } catch (InstantiationException instantiationException) {
            throw new TeleError("could not allocate instance: " + javaClass, instantiationException);
        }
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        final ClassActor classActor = classActorForType();
        final Class<?> javaClass = classActor.toJava();
        try {
            final Object newTuple = _unsafe.allocateInstance(javaClass);
            context.register(this, newTuple);
            ClassActor holderClassActor = classActor;
            do {
                for (FieldActor fieldActor : holderClassActor.localInstanceFieldActors()) {
                    copyField(context, this, newTuple, fieldActor);
                }
                holderClassActor = holderClassActor.superClassActor();
            } while (holderClassActor != null);
            return newTuple;
        } catch (InstantiationException instantiationException) {
            throw new TeleError("could not allocate instance: " + javaClass, instantiationException);
        }
    }

}
