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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Enum} in the tele VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleEnum extends TeleTupleObject {

    private Enum enumCopy;

    protected TeleEnum(TeleVM teleVM, Reference enumReference) {
        super(teleVM, enumReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return toJava();
    }

    public Enum toJava() {
        if (enumCopy == null) {
            final ClassActor classActor = classActorForObjectType();
            Class enumClass = classActor.toJava();
            // find the class for this enum that directly extends Enum (i.e. that has the values).
            while (enumClass.getSuperclass() != Enum.class) {
                enumClass = enumClass.getSuperclass();
                if (enumClass == null) {
                    throw ProgramError.unexpected(classActor + " is not a valid enum class");
                }
            }
            enumCopy = (Enum) enumClass.getEnumConstants()[vm().teleFields().Enum_ordinal.readInt(reference())];
        }
        return enumCopy;
    }

    @Override
    public String maxineRole() {
        return "Enum";
    }

    @Override
    public String maxineTerseRole() {
        return "Enum.";
    }

}
