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

import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;

/**
 * Inspector's surrogate for an object of type {@link MemberActor} in the VM.
 */
public abstract class TeleMemberActor extends TeleActor {

    /**
     * This constructor follows no {@link References}. This avoids the infinite regress that can occur when the VM
     * object and another are mutually referential.
     */
    protected TeleMemberActor(TeleVM vm, Reference memberActorReference) {
        super(vm, memberActorReference);
    }

    /**
     * @return surrogate for the {@link ClassActor} object in the VM that contains this member
     */
    public TeleClassActor getTeleHolder() {
        final Reference classActorReference =  jumpForwarder(fields().MemberActor_holder.readReference(reference()));
        return (TeleClassActor) objects().makeTeleObject(classActorReference);
    }

    /**
     * Field is final once non-null so cache it.
     */
    private TeleDescriptor descriptor;

    protected final TeleDescriptor getTeleDescriptor() {
        if (descriptor == null) {
            final Reference reference = jumpForwarder(fields().MemberActor_descriptor.readReference(reference()));
            descriptor = (TeleDescriptor) objects().makeTeleObject(reference);
        }
        return descriptor;
    }

}
