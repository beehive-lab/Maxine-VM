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

import com.sun.max.tele.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Constructor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class TeleConstructor extends TeleTupleObject {

    private Constructor constructor;

    protected TeleConstructor(TeleVM teleVM, Reference constructorReference) {
        super(teleVM, constructorReference);
    }

    /**
     * @return the local instance of {@link Constructor} equivalent to this object in the{@link TeleVM}.
     */
    public Constructor toJava() {
        if (constructor == null) {
            final Reference methodActorReference = teleVM().fields().Constructor_methodActor.readReference(reference());
            final TeleMethodActor teleMethodActor = (TeleMethodActor) teleVM().makeTeleObject(methodActorReference);
            if (teleMethodActor != null) {
                constructor = teleMethodActor.methodActor().toJavaConstructor();
            }
        }
        return constructor;
    }

    @Override
    public String maxineRole() {
        return "Constructor";
    }

    @Override
    public String maxineTerseRole() {
        return "Constr";
    }


}
