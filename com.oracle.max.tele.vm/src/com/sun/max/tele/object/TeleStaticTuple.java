/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * Representation for a {@link StaticTuple} in the VM.
 * <p>
 * {@link StaticTuple}s are special in the VM in that, although represented as an ordinary tuple, they have no Java
 * type, not even in the extended type system supported by Maxine. Any code that acts based on the type of a
 * {@link TeleObject} must handle this type specially.
 * @see StaticTuple
 */
public class TeleStaticTuple extends TeleTupleObject {

    /**
     * This constructor follows no {@link References}. This avoids the infinite regress that can occur when the VM
     * object and another are mutually referential.
     *
     * @param vm
     * @param reference
     */
    protected TeleStaticTuple(TeleVM vm, RemoteReference reference) {
        super(vm, reference);
    }

    @Override
    public Set<FieldActor> getFieldActors() {
        // Static tuples do not inherit fields; return only the local static fields.
        final Set<FieldActor> staticFieldActors = new HashSet<FieldActor>();
        for (FieldActor fieldActor : classActorForObjectType().localStaticFieldActors()) {
            staticFieldActors.add(fieldActor);
        }
        return staticFieldActors;
    }

    @Override
    public String maxineRole() {
        return "StaticTuple";
    }

    @Override
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        // Some tuples might be some IR object associated with a method, but not static tuples.
        return null;
    }

    @Override
    public Object shallowCopy() {
        return ClassActor.create(classActorForObjectType());
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        return classActorForObjectType().staticTuple();
    }

}
