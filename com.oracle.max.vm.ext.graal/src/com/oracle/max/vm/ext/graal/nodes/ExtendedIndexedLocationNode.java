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

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.Scale;

public final class ExtendedIndexedLocationNode extends LocationNode implements LIRLowerable, Canonicalizable {

    @Input private ValueNode index;
    @Input private ValueNode displacement;

    public ValueNode index() {
        return index;
    }

    public static Object getArrayLocation(CiKind elementKind) {
        return elementKind;
    }

    public static ExtendedIndexedLocationNode create(Object identity, CiKind kind, ValueNode displacement, ValueNode index, StructuredGraph graph) {
        return graph.unique(new ExtendedIndexedLocationNode(identity, kind, index, displacement));
    }

    private ExtendedIndexedLocationNode(Object identity, CiKind kind, ValueNode index, ValueNode displacement) {
        super(identity, kind, 0);
        this.index = index;
        this.displacement = displacement;
    }

    @Override
    public CiAddress createAddress(LIRGeneratorTool gen, ValueNode object) {
        CiValue base = gen.operand(object);
        if (base.isConstant() && ((CiConstant) base).isNull()) {
            base = CiValue.IllegalValue;
        }

        CiValue indexValue = gen.operand(index());
        CiValue offset = gen.newVariable(CiKind.Long);
        gen.emitMove(indexValue, offset);
        Scale indexScale = Scale.fromInt(gen.target().sizeInBytes(getValueKind()));
        if (indexScale != Scale.Times1) {
            gen.emitShl(offset, CiConstant.forInt(indexScale.log2));
        }
        CiValue displacementTemp = gen.newVariable(CiKind.Long);
        gen.emitMove(gen.operand(displacement), displacementTemp);

        gen.emitAdd(offset, displacementTemp);

        return new CiAddress(getValueKind(), base, indexValue, Scale.Times1, 0);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        CiConstant constantDisplacement = displacement.asConstant();
        if (constantDisplacement != null && constantDisplacement.kind.stackKind().isInt()) {
            return IndexedLocationNode.create(locationIdentity(), getValueKind(), constantDisplacement.asInt(), index(), graph());
        }
        return this;
    }
}
