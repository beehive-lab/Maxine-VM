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
package com.oracle.max.vm.ext.graal.nodes;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code UnsafeCastNode} produces the same value as its input, but with a different type.
 */
public final class MaxineUnsafeCastNode extends FloatingNode implements LIRLowerable {

    @Input private ValueNode x;
    @Data private RiResolvedType toType;

    public ValueNode x() {
        return x;
    }

    public MaxineUnsafeCastNode(ValueNode x, RiResolvedType toType) {
        super(toType.kind(false).stackKind());
        this.x = x;
        this.toType = toType;
    }

    @Override
    public RiResolvedType declaredType() {
        return toType;
    }

    @NodeIntrinsic
    public static <T> T cast(Object object, @ConstantNodeParameter Class<?> toType) {
        throw new UnsupportedOperationException("This method may only be compiled with the Graal compiler");
    }

    @Override
    public void generate(LIRGeneratorTool gen) {
        CiValue operand = gen.operand(x);
        if (this.kind != operand.kind) {
            CiValue dest = gen.newVariable(this.kind);
            gen.emitMove(operand, dest);
            gen.setResult(this, dest);
        } else {
            gen.setResult(this, operand);
        }
    }
}
