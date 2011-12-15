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
 * Canonical surrogate for an object of type {@link Method} in the VM.
 */
public class TeleMethod extends TeleTupleObject {

    private Method method;

    protected TeleMethod(TeleVM vm, Reference methodReference) {
        super(vm, methodReference);
    }

    /**
     * @return the local instance of {@link Method} equivalent to this object in the VM.
     */
    public Method toJava() {
        if (method == null) {
            final Reference methodActorReference = fields().Method_methodActor.readReference(reference());
            final TeleMethodActor teleMethodActor = (TeleMethodActor) objects().makeTeleObject(methodActorReference);
            method = teleMethodActor.methodActor().toJava();
        }
        return method;
    }

    @Override
    public String maxineRole() {
        return "Method";
    }

    @Override
    public String maxineTerseRole() {
        return "Method";
    }

}
