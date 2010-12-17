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
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.type.ClassRegistry.Property.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of class or interface members.
 * These can be fields or methods.
 *
 * @see FieldActor
 * @see MethodActor
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class MemberActor extends Actor {

    @INSPECTED
    public final Descriptor descriptor;

    protected MemberActor(Utf8Constant name, Descriptor descriptor, int flags) {
        super(name, flags);
        this.descriptor = descriptor;
    }

    @CONSTANT
    @INSPECTED
    private ClassActor holder;

    @INLINE
    public final ClassActor holder() {
        return holder;
    }

    @Override
    public String qualifiedName() {
        if (holder == null) {
            return name.string;
        }
        return holder().qualifiedName() + "." + name;
    }

    /**
     * Index of this field or method within a holder's local field or method array.
     * The index space is shared within each member type (method or field) as follows:
     *
     * Method indexes: [virtual methods][static methods][interface methods]
     * Field indexes:  [instance fields][static fields]
     *
     * Note: This field is of type char which means a limit of 65535 members of a particular
     * member type (method or field) is supported.
     */
    @CONSTANT
    @INSPECTED
    private char memberIndex;

    @INLINE
    public final int memberIndex() {
        return memberIndex;
    }

    public final void assignHolder(ClassActor classActor, int index) {
        assert (char) index == index : "exceeded member index range";
        this.holder = classActor;
        this.memberIndex = (char) index;
        if (MaxineVM.isHosted() && this instanceof ClassMethodActor) {
            ClassMethodActor classMethodActor = (ClassMethodActor) this;
            if (classMethodActor.isNative() &&
                !classMethodActor.isBuiltin() &&
                classMethodActor.intrinsic() == 0) {
                // Make sure the C symbol for a native method is cooked into the boot image
                classMethodActor.nativeFunction.makeSymbol();
            }
        }
    }

    @Override
    public Utf8Constant genericSignature() {
        return holder().classRegistry().get(GENERIC_SIGNATURE, this);
    }

    @Override
    public byte[] runtimeVisibleAnnotationsBytes() {
        return holder().classRegistry().get(RUNTIME_VISIBLE_ANNOTATION_BYTES, this);
    }

    @Override
    public abstract String toString();

    public abstract boolean isHiddenToReflection();

    public final boolean matchesNameAndType(Utf8Constant name, Descriptor desc) {
        return this.name == name && descriptor == desc;
    }

    @Override
    public boolean isAccessibleBy(ClassActor accessor) {
        if (accessor == null || holder() == accessor || isPublic() || accessor.isReflectionStub()) {
            return true;
        }
        if (isPrivate()) {
            return false;
        }
        if (holder().packageName().equals(accessor.packageName())) {
            return true;
        }
        if (isProtected()) {
            return holder().isAssignableFrom(accessor.superClassActor);
        }
        return false;
    }

}
