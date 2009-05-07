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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

public abstract class IrInspector<IrInspector_Type extends IrInspector> extends UniqueInspector<IrInspector_Type> {

    private final Reference _irMethodReference;

    public Reference irMethodReference() {
        return _irMethodReference;
    }

    private final TeleClassMethodActor _teleClassMethodActor;

    public TeleClassMethodActor teleClassMethodActor() {
        return _teleClassMethodActor;
    }
    protected IrInspector(Inspection inspection, Reference irMethodReference) {
        super(inspection, irMethodReference);
        _irMethodReference = irMethodReference;
        final Reference classMethodActorReference =  vm().fields().IrMethod_classMethodActor(vm().makeClassActorForTypeOf(_irMethodReference).toJava().asSubclass(IrMethod.class)).readReference(irMethodReference());
        _teleClassMethodActor = (TeleClassMethodActor) vm().makeTeleObject(classMethodActorReference);
    }

    protected IrInspector(Inspection inspection, Reference irMethodReference, Value subject) {
        super(inspection, subject);
        _irMethodReference = irMethodReference;
        final Reference classMethodActorReference =  vm().fields().IrMethod_classMethodActor(vm().makeClassActorForTypeOf(_irMethodReference).toJava().asSubclass(IrMethod.class)).readReference(irMethodReference());
        _teleClassMethodActor = (TeleClassMethodActor) vm().makeTeleObject(classMethodActorReference);
    }

}
