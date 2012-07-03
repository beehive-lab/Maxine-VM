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

import java.lang.reflect.*;

import com.sun.max.tele.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Constructor} in the {@link TeleVM}.
 */
public final class TeleConstructor extends TeleTupleObject {

    private Constructor constructor;

    protected TeleConstructor(TeleVM vm, Reference constructorReference) {
        super(vm, constructorReference);
    }

    /**
     * @return the local instance of {@link Constructor} equivalent to this object in the{@link TeleVM}.
     */
    public Constructor toJava() {
        if (constructor == null) {
            final Reference methodActorReference = fields().Constructor_methodActor.readReference(reference());
            final TeleMethodActor teleMethodActor = (TeleMethodActor) objects().makeTeleObject(methodActorReference);
            if (teleMethodActor != null) {
                constructor = teleMethodActor.methodActor().toJavaConstructor();
            }
        }
        return constructor;
    }

    @Override
    public String maxineRole() {
        return "Constructor";
    }

    @Override
    public String maxineTerseRole() {
        return "Constr";
    }

}
