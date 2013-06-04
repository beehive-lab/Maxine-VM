/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.sun.max.vm.intrinsics.*;

/**
 * A variant of {@link UnsafeAccessNode} that allows a {@link ValueNode} displacement for the
 * {@link MaxineIntrinsicIDs#PREAD_IDX} and {@link MaxineIntrinsicIDs#PWRITE_IDX} intrinsics.
 */
public class ExtendedUnsafeAccessNode extends FixedWithNextNode implements Canonicalizable {

    protected ExtendedUnsafeAccessNode(Stamp stamp, ValueNode object, ValueNode displacement, ValueNode offset, Kind accessKind) {
        super(stamp);
        assert accessKind != null;
        this.object = object;
        this.displacement = displacement;
        this.offset = offset;
        this.accessKind = accessKind;
    }

    @Input private ValueNode object;
    @Input private ValueNode offset;
    @Input private ValueNode displacement;
    private final Kind accessKind;

    public ValueNode object() {
        return object;
    }

    public ValueNode displacement() {
        return displacement;
    }

    public ValueNode offset() {
        return offset;
    }

    public Kind accessKind() {
        return accessKind;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        if (displacement.isConstant()) {
            int intDisplacement = displacement.asConstant().asInt();
            FixedWithNextNode replacement = this instanceof ExtendedUnsafeLoadNode ?
                            new UnsafeLoadNode(this.stamp(), object, intDisplacement, offset, accessKind) :
                            new UnsafeStoreNode(this.stamp(), object, intDisplacement, offset, ((ExtendedUnsafeStoreNode) this).value(), accessKind);

            return graph().add(replacement);
        }
        if (offset().isConstant()) {
            // switch offset and displacement and use UnsafeXXXNode
            int constantOffset = offset().asConstant().asInt();
            FixedWithNextNode replacement = this instanceof ExtendedUnsafeLoadNode ?
                            new UnsafeLoadNode(this.stamp(), object, constantOffset, displacement, accessKind) :
                            new UnsafeStoreNode(this.stamp(), object, constantOffset, displacement, ((ExtendedUnsafeStoreNode) this).value(), accessKind);
            return graph().add(replacement);
        }
        return this;
    }
}
