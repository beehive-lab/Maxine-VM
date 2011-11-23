/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.jdwp.vm.proxy.VMValue.Type;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 *Canonical surrogate for an object of type {@link FieldActor} in the VM.
 */
public final class TeleFieldActor extends TeleMemberActor implements FieldProvider {

    private FieldActor fieldActor;

    // Keep construction minimal for both performance and synchronization.
    protected TeleFieldActor(TeleVM vm, Reference fieldActorReference) {
        super(vm, fieldActorReference);
    }

    /**
     * @return local {@link FieldActor} corresponding the the VM's {@link FieldActor} for this field.
     */
    public FieldActor fieldActor() {
        if (fieldActor == null && actorName() != null) {
            TypeDescriptor type = (TypeDescriptor) getTeleDescriptor().descriptor();
            fieldActor = getTeleHolder().classActor().findLocalFieldActor(actorName(), type);
        }
        return fieldActor;
    }

    @Override
    public Actor actor() {
        return fieldActor();
    }

    @Override
    public String maxineRole() {
        return "FieldActor";
    }

    public String getName() {
        return actorName().string;
    }

    public VMValue getStaticValue() {
        final TeleStaticTuple teleStaticTuple = this.getTeleHolder().getTeleStaticTuple();
        return vm().maxineValueToJDWPValue(teleStaticTuple.readFieldValue(fieldActor));
    }

    public VMValue getValue(ObjectProvider object) {
        final TeleObject teleObject = (TeleObject) object;
        return vm().maxineValueToJDWPValue(teleObject.readFieldValue(fieldActor));
    }

    public void setStaticValue(VMValue value) {
        assert false : "Not implemented.";
    }

    public void setValue(ObjectProvider object, VMValue value) {
        assert false : "Not implemented.";
    }

    public ReferenceTypeProvider getReferenceTypeHolder() {
        return super.getTeleHolder();
    }

    public Type getType() {
        return TeleVM.maxineKindToJDWPType(fieldActor().kind);
    }

    public String getSignature() {
        return fieldActor().jniSignature();
    }

    public String getGenericSignature() {
        return fieldActor().genericSignatureString();
    }

}
