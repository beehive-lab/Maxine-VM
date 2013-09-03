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
import com.oracle.graal.compiler.gen.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.type.*;
import com.sun.max.unsafe.*;

/**
 * Maxine variants of {@link BitScanForwardNode} and {@link BitScanReverseNode}.
 * Can't use the Graal versions because at the time the intrinsic is processed the
 * type is still {@code Word} and Graal's {@link Kind#getBitCount} method objects to
 * {@code Kind == Object}. Plus, Maxine's MSB/LSB intrinsics specify a result of -1
 * if no bits are set.
 */
public class MaxBitScanNode extends FloatingNode implements LIRGenLowerable {
    @Input private ValueNode value;

    private boolean forward;

    public MaxBitScanNode(ValueNode value, boolean forward) {
        super(StampFactory.forInteger(Kind.Int, 0,
                        value.kind() == Kind.Object ? Word.size() : value.kind().getBitCount()));
        this.value = value;
        this.forward = forward;
    }

    @Override
    public void generate(LIRGenerator gen) {
        Variable result = gen.newVariable(Kind.Int);
        Value operand = gen.operand(value);
        // default result if no bits set; could also do this with a CMOVEZ after the test
        gen.emitMove(result, Constant.INT_MINUS_1);
        if (forward) {
            gen.emitBitScanForward(result, operand);
        } else {
            gen.emitBitScanReverse(result, operand);
        }
        gen.setResult(this, result);
    }

}
