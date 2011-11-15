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
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Enum} in the VM.
 */
public final class TeleEnum extends TeleTupleObject {

    private Enum enumCopy;

    protected TeleEnum(TeleVM vm, Reference enumReference) {
        super(vm, enumReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return toJava();
    }

    public Enum toJava() {
        if (enumCopy == null) {
            final ClassActor classActor = classActorForObjectType();
            Class enumClass = classActor.toJava();
            // find the class for this enum that directly extends Enum (i.e. that has the values).
            while (enumClass.getSuperclass() != Enum.class) {
                enumClass = enumClass.getSuperclass();
                if (enumClass == null) {
                    throw TeleError.unexpected(classActor + " is not a valid enum class");
                }
            }
            enumCopy = (Enum) enumClass.getEnumConstants()[fields().Enum_ordinal.readInt(reference())];
        }
        return enumCopy;
    }

    @Override
    public String maxineRole() {
        return "Enum";
    }

    @Override
    public String maxineTerseRole() {
        return "Enum.";
    }

}
