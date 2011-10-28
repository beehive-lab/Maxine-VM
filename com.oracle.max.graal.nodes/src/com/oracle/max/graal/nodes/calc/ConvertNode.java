/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes.calc;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

/**
 * The {@code ConvertNode} class represents a conversion between primitive types.
 */
public final class ConvertNode extends FloatingNode implements Canonicalizable {

    public enum Op {
        I2L, L2I, I2B, I2C, I2S, F2D, D2F, I2F, I2D, F2I, D2I, L2F, L2D, F2L, D2L, MOV_I2F, MOV_L2D, MOV_F2I, MOV_D2L
    }

    @Input private ValueNode value;

    @Data public final Op opcode;

    public ValueNode value() {
        return value;
    }

    /**
     * Constructs a new Convert instance.
     * @param kind the result type of this instruction
     * @param opcode the operation
     * @param value the instruction producing the input value
     * @param graph
     */
    public ConvertNode(CiKind kind, Op opcode, ValueNode value) {
        super(kind);
        this.opcode = opcode;
        this.value = value;
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitConvert(this);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (value instanceof ConstantNode) {
            CiConstant c = ((ConstantNode) value).asConstant();
            switch (opcode) {
                case I2L: return ConstantNode.forLong(c.asInt(), graph());
                case L2I: return ConstantNode.forInt((int) c.asLong(), graph());
                case I2B: return ConstantNode.forByte((byte) c.asInt(), graph());
                case I2C: return ConstantNode.forChar((char) c.asInt(), graph());
                case I2S: return ConstantNode.forShort((short) c.asInt(), graph());
                case F2D: return ConstantNode.forDouble(c.asFloat(), graph());
                case D2F: return ConstantNode.forFloat((float) c.asDouble(), graph());
                case I2F: return ConstantNode.forFloat(c.asInt(), graph());
                case I2D: return ConstantNode.forDouble(c.asInt(), graph());
                case F2I: return ConstantNode.forInt((int) c.asFloat(), graph());
                case D2I: return ConstantNode.forInt((int) c.asDouble(), graph());
                case L2F: return ConstantNode.forFloat(c.asLong(), graph());
                case L2D: return ConstantNode.forDouble(c.asLong(), graph());
                case F2L: return ConstantNode.forLong((long) c.asFloat(), graph());
                case D2L: return ConstantNode.forLong((long) c.asDouble(), graph());
                case MOV_I2F: return ConstantNode.forFloat(Float.intBitsToFloat(c.asInt()), graph());
                case MOV_L2D: return ConstantNode.forDouble(Double.longBitsToDouble(c.asLong()), graph());
                case MOV_F2I: return ConstantNode.forInt(Float.floatToRawIntBits(c.asFloat()), graph());
                case MOV_D2L: return ConstantNode.forLong(Double.doubleToRawLongBits(c.asDouble()), graph());
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static <S, T> S convert(@ConstantNodeParameter CiKind kind, @ConstantNodeParameter Op op, T value) {
        throw new UnsupportedOperationException();
    }
}
