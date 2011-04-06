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
            if (classMethodActor.isNative() && classMethodActor.intrinsic() == 0) {
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
