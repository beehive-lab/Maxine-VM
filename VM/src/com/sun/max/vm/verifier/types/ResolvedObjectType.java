/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.verifier.types;

import com.sun.max.vm.actor.holder.*;

/**
 * Represents object types for which the corresponding ClassActor already exists. That is,
 * {@linkplain #resolve() resolving} this verification type is guaranteed not to cause class loading.
 *
 * @author Doug Simon
 */
public class ResolvedObjectType extends ObjectType implements ResolvedType {

    private final ClassActor classActor;

    public ResolvedObjectType(ClassActor classActor) {
        super(classActor.typeDescriptor, null);
        assert !classActor.isArrayClass();
        this.classActor = classActor;
    }

    @Override
    public ClassActor resolve() {
        return classActor;
    }
}
