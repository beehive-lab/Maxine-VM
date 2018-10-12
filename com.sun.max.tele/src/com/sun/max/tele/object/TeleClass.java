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

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.actor.holder.*;

/**
 * Canonical surrogate for an object of type {@link Class} in the VM.
 */
public final class TeleClass extends TeleTupleObject implements ClassObjectProvider {

    private Class clazz;

    protected TeleClass(TeleVM vm, RemoteReference classReference) {
        super(vm, classReference);
    }

    /**
     * @return the {@link ClassActor} in the VM corresponding to this {@link Class} in the VM.
     */
    public TeleClassActor getTeleClassActor() {
        final RemoteReference classActorReference = fields().Class_classActor.readRemoteReference(reference());
        final TeleClassActor teleClassActor = (TeleClassActor) objects().makeTeleObject(classActorReference);
        return teleClassActor;
    }

    /**
     * @return the local instance of {@link Class} equivalent to this {@link Class} in the VM.
     */
    public Class toJava() {
        if (clazz == null) {
            final TeleClassActor teleClassActor = getTeleClassActor();
            clazz = teleClassActor.classActor().toJava();
        }
        return clazz;
    }

    @Override
    public Object shallowCopy() {
        // Translate into local equivalent
        return toJava();
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return toJava();
    }

    @Override
    public String maxineRole() {
        return "Class";
    }

    @Override
    public String maxineTerseRole() {
        return "Class";
    }

    public ReferenceTypeProvider getReflectedType() {
        return getTeleClassActor();
    }

}
