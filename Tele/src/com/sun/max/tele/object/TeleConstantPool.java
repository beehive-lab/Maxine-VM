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

import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for an object of type {@link ConstantPool} in the {@link TeleVM} .
 *
 * @author Michael Van De Vanter
 */
public final class TeleConstantPool extends TeleTupleObject{

    private Reference _constantsArrayReference;

    TeleConstantPool(TeleVM teleVM, Reference constantPoolReference) {
        super(teleVM, constantPoolReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        // Translate into local equivalent
        return getTeleHolder().classActor().constantPool();
    }

    private Reference constantsArrayReference() {
        if (_constantsArrayReference == null) {
            _constantsArrayReference = teleVM().fields().ConstantPool_constants.readReference(reference());
        }
        return _constantsArrayReference;
    }
    /**
     * @param index specifies an entry in this pool
     * @return surrogate for the entry in the {@link TeleVM}  of this pool
     */
    public TelePoolConstant readTelePoolConstant(int index) {
        final Reference poolConstantReference = teleVM().getElementValue(Kind.REFERENCE, constantsArrayReference(), index).asReference();
        final TeleObject teleObject = TeleObject.make(teleVM(), poolConstantReference);
        if (!(teleObject instanceof TelePoolConstant)) {
            return null;
        }
        return (TelePoolConstant) teleObject;
    }

    /**
     * @return surrogate for the {@link ClassActor} object in the {@link TeleVM}  that includes this pool
     */
    public TeleClassActor getTeleHolder() {
        final Reference classActorReference = teleVM().fields().ConstantPool_holder.readReference(reference());
        return (TeleClassActor) TeleObject.make(teleVM(), classActorReference);
    }

    @Override
    public String maxineRole() {
        return "ConstantPool";
    }

    @Override
    public String maxineTerseRole() {
        return "ConstantPool";
    }

}
