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

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 *  Canonical surrogate for an object of type {@link MethodActor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 *
 */
public abstract class TeleMethodActor extends TeleMemberActor implements TeleRoutine, MethodProvider {

    /**
     * @return local {@link MethodActor} corresponding the the {@link TeleVM}'s {@link MethodActor} for this method.
     */
    @Override
    protected Actor initActor() {
        // Cannot use member index as it may be different in local ClassActor
        Utf8Constant name = getTeleName().utf8Constant();
        SignatureDescriptor signature = (SignatureDescriptor) getTeleDescriptor().descriptor();
        MethodActor methodActor = getTeleHolder().classActor().findLocalMethodActor(name, signature);
        assert getName().equals(methodActor.name.string);
        return methodActor;
    }

    public MethodActor methodActor() {
        return (MethodActor) actor();
    }

    // Keep construction minimal for both performance and synchronization.
    protected TeleMethodActor(TeleVM teleVM, Reference methodActorReference) {
        super(teleVM, methodActorReference);
    }

    /**
     * @return Whether this method has bytecodes in the {@link TeleVM}.
     */
    public boolean hasCodeAttribute() {
        return getTeleCodeAttribute() != null;
    }

    /**
     * @return The bytecodes associated with this method in the {@link TeleVM}.
     * The {@link CodeAttribute} will not be the expected one from the classfile of the
     * method's {@link ClassActor holder}, in the event that the method was substituted.
     */
    public abstract TeleCodeAttribute getTeleCodeAttribute();

    /**
     * @return whether the method in the {@link TeleVM} had its {@link CodeAttribute}  substituted from another class.
     */
    public boolean isSubstituted() {
        return teleClassActorSubstitutedFrom() != null;
    }

    /**
     * Local surrogate for the {@link ClassActor} in the {@link TeleVM} from which a code substitution for this
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

    public String getUniqueName() {
        return "TeleJavaMethod: " + methodActor().format("%R %H.%n(%P)");
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
            ProgramError.unexpected("method interpretation failed", teleInterpreterException);
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
