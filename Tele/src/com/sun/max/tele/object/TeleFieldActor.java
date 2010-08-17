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
import com.sun.max.jdwp.vm.proxy.VMValue.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 *Canonical surrogate for an object of type {@link FieldActor} in the tele VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleFieldActor extends TeleMemberActor implements FieldProvider {

    /**
     * @return local {@link FieldActor} corresponding the the target VM's {@link FieldActor} for this field.
     */
    public FieldActor fieldActor() {
        return (FieldActor) actor();
    }

    @Override
    protected Actor initActor() {
        // Cannot use member index as it may be different in local ClassActor
        Utf8Constant name = getTeleName().utf8Constant();
        TypeDescriptor type = (TypeDescriptor) getTeleDescriptor().descriptor();
        return getTeleHolder().classActor().findLocalFieldActor(name, type);
    }

    // Keep construction minimal for both performance and synchronization.
    protected TeleFieldActor(TeleVM teleVM, Reference fieldActorReference) {
        super(teleVM, fieldActorReference);
    }

    @Override
    public String maxineRole() {
        return "FieldActor";
    }

    public VMValue getStaticValue() {
        final Pointer pointer = this.getTeleHolder().getTeleStaticTuple().getReference().toOrigin();
        final int offset = fieldActor().offset();
        final Kind kind = fieldActor().kind;
        return vm().maxineValueToJDWPValue(vm().readValue(kind, pointer, offset));
    }

    public VMValue getValue(ObjectProvider object) {
        final Reference reference = ((TeleObject) object).getReference();
        return vm().maxineValueToJDWPValue(vm().readValue(fieldActor().kind, reference.toOrigin(), fieldActor().offset()));
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
