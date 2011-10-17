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
package com.oracle.max.graal.nodes.extended;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

/**
 * The {@code FPConversionNode} applies a raw integer/floating point conversion to its input. (Int->Float, Double->Long...)
 */
public final class FPConversionNode extends FloatingNode implements Canonicalizable, LIRLowerable {

    @Input private ValueNode value;

    public ValueNode value() {
        return value;
    }

    public FPConversionNode(CiKind kind, ValueNode value) {
        super(kind);
        this.value = value;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (value instanceof ConstantNode) {
            CiKind fromKind = value.kind;
            if (kind == CiKind.Int && fromKind == CiKind.Float) {
                return ConstantNode.forInt(Float.floatToRawIntBits(((ConstantNode) value).asConstant().asFloat()), graph());
            } else if (kind == CiKind.Long && fromKind == CiKind.Double) {
                return ConstantNode.forLong(Double.doubleToRawLongBits(((ConstantNode) value).asConstant().asDouble()), graph());
            } else if (kind == CiKind.Float && fromKind == CiKind.Int) {
                return ConstantNode.forFloat(Float.intBitsToFloat(((ConstantNode) value).asConstant().asInt()), graph());
            } else if (kind == CiKind.Double && fromKind == CiKind.Long) {
                return ConstantNode.forDouble(Double.longBitsToDouble(((ConstantNode) value).asConstant().asLong()), graph());
            }
        }
        return this;
    }

    @Override
    public void generate(LIRGeneratorTool generator) {
        CiValue reg = generator.createResultVariable(this);
        CiValue value = generator.load(value());
        CiValue tmp = generator.forceToSpill(value, kind, false);
        generator.emitMove(tmp, reg);
    }

    @NodeIntrinsic
    public static <S, T> S convert(CiKind kind, @NodeParameter T value) {
        throw new UnsupportedOperationException();
    }
}
