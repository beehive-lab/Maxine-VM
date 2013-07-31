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
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.ConvertNode.Op;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;


/**
 * As {@link IndexedLocationNode} but allows a non-constant displacement.
 */
public class ExtendedIndexedLocationNode extends LocationNode implements Canonicalizable {

    private final Kind valueKind;
    private final LocationIdentity locationIdentity;
    @Input private ValueNode displacement;
    @Input private ValueNode scaledIndex;

    private ExtendedIndexedLocationNode(LocationIdentity identity, Kind kind, ValueNode displacement, ValueNode scaledIndex) {
        super(StampFactory.extension());
        assert kind != Kind.Illegal && kind != Kind.Void;
        this.valueKind = kind;
        this.locationIdentity = identity;
        this.displacement = displacement;
        this.scaledIndex = scaledIndex;
    }

    public static LocationNode create(LocationIdentity identity, Kind kind, ValueNode displacement, Graph graph, ValueNode scaledIndex) {
        if (displacement.isConstant()) {
            return IndexedLocationNode.create(identity, kind, displacement.asConstant().asInt(), scaledIndex, graph, 1);
        } else {
            return graph.unique(new ExtendedIndexedLocationNode(identity, kind, displacement, scaledIndex));
        }
    }

    public ValueNode displacement() {
        return displacement;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        if (displacement.isConstant()) {
            int intDisplacement = displacement.asConstant().asInt();
            return IndexedLocationNode.create(locationIdentity, valueKind, intDisplacement, scaledIndex, graph(), 1);
        }
        if (scaledIndex.isConstant()) {
            // switch scaledIndex and displacement
            int intScaledIndex = scaledIndex.asConstant().asInt();
            return IndexedLocationNode.create(locationIdentity, valueKind, intScaledIndex, displacement, graph(), 1);
        }
        return this;
    }

    @Override
    public Kind getValueKind() {
        return valueKind;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return locationIdentity;
    }

    @Override
    public Value generateAddress(LIRGeneratorTool gen, Value base) {
        Value displacementRegister = gen.emitConvert(Op.I2L, gen.operand(displacement));
        Value v = gen.emitAdd(base, displacementRegister);
        return gen.emitAddress(v, 0, gen.operand(scaledIndex), 1);
    }

}
