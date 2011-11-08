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
package com.oracle.max.vm.ext.graal.nodes;

import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

/**
 * Store of a value at a location specified as an offset relative to an object.
 */
public class ExtendedUnsafeStoreNode extends AbstractStateSplit implements Lowerable {

    @Input private ValueNode object;
    @Input private ValueNode offset;
    @Input private ValueNode value;
    @Input private ValueNode displacement;
    @Data private final CiKind storeKind;

    public ExtendedUnsafeStoreNode(ValueNode object, ValueNode displacement, ValueNode offset, ValueNode value, CiKind kind) {
        super(CiKind.Void);
        this.object = object;
        this.displacement = displacement;
        this.offset = offset;
        this.value = value;
        this.storeKind = kind;
    }

    public ValueNode object() {
        return object;
    }

    public ValueNode displacement() {
        return displacement;
    }

    public ValueNode offset() {
        return offset;
    }

    public ValueNode value() {
        return value;
    }

    public CiKind storeKind() {
        return storeKind;
    }

    @Override
    public void lower(CiLoweringTool tool) {
        LocationNode location = ExtendedIndexedLocationNode.create(LocationNode.UNSAFE_ACCESS_LOCATION, storeKind(), displacement(), offset(), graph());
        WriteNode write = graph().add(new WriteNode(object(), value(), location));
        FixedNode next = next();
        setNext(null);
        if (object().kind() == CiKind.Object) {
            write.setGuard((GuardNode) tool.createGuard(graph().unique(new NullCheckNode(object(), false))));
        }
        // TODO: add Maxine-specific write barrier
//        FieldWriteBarrier barrier = graph.add(new FieldWriteBarrier(store.object()));
//        barrier.setNext(next);
//        write.setNext(barrier);
        write.setNext(next);
        write.setStateAfter(stateAfter());
        replaceAtPredecessors(write);
        delete();
    }
}
