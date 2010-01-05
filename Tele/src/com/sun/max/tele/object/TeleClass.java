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

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Class} in the tele VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleClass extends TeleTupleObject implements ClassObjectProvider {

    private Class clazz;

    protected TeleClass(TeleVM teleVM, Reference classReference) {
        super(teleVM, classReference);
    }

    /**
     * @return the {@link ClassActor} in the tele VM corresponding to this {@link Class} in the tele VM.
     */
    public TeleClassActor getTeleClassActor() {
        final Reference classActorReference = teleVM().teleFields().Class_classActor.readReference(reference());
        final TeleClassActor teleClassActor = (TeleClassActor) teleVM().makeTeleObject(classActorReference);
        return teleClassActor;
    }

    /**
     * @return the local instance of {@link Class} equivalent to this {@link Class} in the tele VM.
     */
    public Class toJava() {
        if (clazz == null) {
            final TeleClassActor teleClassActor = getTeleClassActor();
            clazz = teleClassActor.classActor().toJava();
        }
        return clazz;
    }

    @Override
    public Object shallowCopy() {
        // Translate into local equivalent
        return toJava();
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return toJava();
    }

    @Override
    public String maxineRole() {
        return "Class";
    }

    @Override
    public String maxineTerseRole() {
        return "Class";
    }

    public ReferenceTypeProvider getReflectedType() {
        return getTeleClassActor();
    }

}
