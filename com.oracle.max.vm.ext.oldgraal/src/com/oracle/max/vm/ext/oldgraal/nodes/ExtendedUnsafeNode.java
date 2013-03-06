/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.oldgraal.nodes;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.type.*;
import com.sun.cri.ci.*;

/**
 * Load of a value from a location specified as an offset relative to an object.
 */
public class ExtendedUnsafeNode extends AbstractStateSplit {

    @Input protected ValueNode object;
    @Input protected ValueNode index;
    @Input protected ValueNode displacement;
    @Data protected final CiKind valueKind;

    public ValueNode object() {
        return object;
    }

    public ValueNode displacement() {
        return displacement;
    }

    public ValueNode index() {
        return index;
    }

    public ExtendedUnsafeNode(ValueNode object, ValueNode displacement, ValueNode index, CiKind kind) {
        super(StampFactory.forKind(kind.stackKind()));
        this.object = object;
        this.displacement = displacement;
        this.index = index;
        this.valueKind = kind;
    }

    public CiKind valueKind() {
        return valueKind;
    }

    protected LocationNode createLocation() {
        if (displacement == null) {
            return IndexedLocationNode.create(LocationNode.ANY_LOCATION, valueKind(), 0, index(), graph(), false);
        } else if (displacement.isConstant() && displacement.asConstant().asLong() == (int) displacement.asConstant().asLong()) {
            return IndexedLocationNode.create(LocationNode.ANY_LOCATION, valueKind(), (int) displacement.asConstant().asLong(), index(), graph(), true);
        } else {
            return ExtendedIndexedLocationNode.create(LocationNode.ANY_LOCATION, valueKind(), displacement(), index(), graph());
        }
    }
}
