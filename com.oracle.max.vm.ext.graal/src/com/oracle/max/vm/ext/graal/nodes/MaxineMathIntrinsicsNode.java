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

import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.target.amd64.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

public final class MaxineMathIntrinsicsNode extends FloatingNode implements LIRLowerable {

    @Input private ValueNode value;
    @Data private final Op op;

    public static enum Op {
        MSB, LSB
    }

    public MaxineMathIntrinsicsNode(ValueNode value, Op op) {
        super(value.kind);
        this.value = value;
        this.op = op;
    }

    @Override
    public void accept(ValueVisitor v) {
        // nothing to do
    }

    @Override
    public void generate(LIRGeneratorTool tool) {
        // TODO(ls) this is just experimental - we cannot use LIRGenerator and AMD64 here
        LIRGenerator gen = (LIRGenerator) tool;
        CiVariable result = gen.createResultVariable(this);
        CiVariable input = gen.load(value);
        switch (op) {
            case MSB:
                gen.append(AMD64MaxineOpcode.SignificantBitOpcode.MSB.create(result, input));
                break;
            case LSB:
                gen.append(AMD64MaxineOpcode.SignificantBitOpcode.LSB.create(result, input));
                break;
            default:
                throw new RuntimeException();
        }
    }
}
