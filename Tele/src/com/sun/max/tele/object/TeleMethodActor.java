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

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for an object of type {@link MethodActor} in the VM.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleMethodActor extends TeleMemberActor implements MethodProvider {

    private MethodActor methodActor;

    // Keep construction minimal for both performance and synchronization.
    protected TeleMethodActor(TeleVM vm, Reference methodActorReference) {
        super(vm, methodActorReference);
    }

    /**
     * @return local {@link MethodActor} corresponding the the VM's {@link MethodActor} for this method.
     */
    public MethodActor methodActor() {
        if (methodActor == null) {
            Utf8Constant actorName = actorName();
            if (actorName != null) {
                // Cannot use member index as it may be different in local ClassActor
                SignatureDescriptor signature = (SignatureDescriptor) getTeleDescriptor().descriptor();
                ClassActor classActor = getTeleHolder().classActor();
                methodActor = classActor.findLocalMethodActor(actorName, signature);
                assert methodActor != null : "Could not find " + actorName.string + signature + " in " + classActor;
                assert actorName.string.equals(methodActor.name.string);
            }
        }
        return methodActor;
    }

    @Override
    public Actor actor() {
        return methodActor();
    }

    /**
     * @return Whether this method has bytecodes in the VM.
     */
    public boolean hasCodeAttribute() {
        return getTeleCodeAttribute() != null;
    }

    /**
     * @return The bytecodes associated with this method in the VM.
     * The {@link CodeAttribute} will not be the expected one from the classfile of the
     * method's {@link ClassActor holder}, in the event that the method was substituted.
     */
    public abstract TeleCodeAttribute getTeleCodeAttribute();

    /**
     * @return whether the method in the VM had its {@link CodeAttribute}  substituted from another class.
     */
    public boolean isSubstituted() {
        return teleClassActorSubstitutedFrom() != null;
    }

    /**
     * Local surrogate for the {@link ClassActor} in the VM from which a code substitution for this
     * method originated, null if the method has not been substituted.
     */
    public TeleClassActor teleClassActorSubstitutedFrom() {
        final TeleCodeAttribute teleCodeAttribute = getTeleCodeAttribute();
        if (teleCodeAttribute != null) {
            final TeleClassActor codeAttributeHolder = teleCodeAttribute.getTeleConstantPool().getTeleHolder();
            return codeAttributeHolder == getTeleHolder() ? null : codeAttributeHolder;
        }
        return null;
    }

    @Override
    public String maxineTerseRole() {
        return "MethodActor";
    }

    public VMValue invoke(ObjectProvider object, VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded, boolean nonVirtual) {
        final VMValue[] newArgs = new VMValue[args.length + 1];
        newArgs[0] = vm().vmAccess().createObjectProviderValue(object);
        System.arraycopy(args, 0, newArgs, 1, args.length);

        // TODO: Currently the nonVirtual parameter is ignored.
        return invokeStatic(newArgs, threadProvider, singleThreaded);
    }

    public VMValue invokeStatic(VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded) {
        // TODO: Check ClassMethodActor / MethodActor relationship
        final com.sun.max.vm.value.Value[] realArgs = new com.sun.max.vm.value.Value[args.length];
        for (int i = 0; i < args.length; i++) {
            realArgs[i] = vm().jdwpValueToMaxineValue(args[i]);
        }
        try {
            final com.sun.max.vm.value.Value result = TeleInterpreter.execute(vm(), (ClassMethodActor) methodActor(), realArgs);
            return vm().maxineValueToJDWPValue(result);
        } catch (TeleInterpreterException teleInterpreterException) {
            TeleError.unexpected("method interpretation failed", teleInterpreterException);
            return null;
        }
    }

    public ReferenceTypeProvider getReferenceTypeHolder() {
        return super.getTeleHolder();
    }

    public String getSignature() {
        return methodActor().descriptor().toString();
    }

    public String getSignatureWithGeneric() {
        return methodActor().genericSignatureString();
    }

    public LineTableEntry[] getLineTable() {
        return new LineTableEntry[0];
    }

    public VariableTableEntry[] getVariableTable() {
        return new VariableTableEntry[0];
    }

    public int getNumberOfArguments() {
        return (methodActor().isStatic() ? 0 : 1) + methodActor().descriptor().computeNumberOfSlots();
    }
}
