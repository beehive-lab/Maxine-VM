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
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

/**
 * Adds a Safepoint to the generated code and possibly create a safepoint.
 */
public final class SafepointNode extends StateSplit implements LIRLowerable {

    public static enum Op {
        SAFEPOINT_POLL, HERE, INFO, BREAKPOINT, PAUSE
    }

    @Data private final Op op;

    public SafepointNode(Op op) {
        super(op == Op.HERE ? CiKind.Long : CiKind.Illegal);
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
        switch (op) {
            case SAFEPOINT_POLL:
                gen.emitSafepointPoll(this);
                break;
            case HERE:
                gen.setResult(this, gen.emitLea(new CiAddress(CiKind.Byte, CiRegister.InstructionRelative.asValue())));
                gen.append(AMD64MaxineOpcode.SafepointOpcode.SAFEPOINT.create(gen.state()));
                break;
            case INFO:
                gen.append(AMD64MaxineOpcode.SafepointOpcode.SAFEPOINT.create(gen.state()));
                break;
            case BREAKPOINT:
                gen.append(AMD64MaxineOpcode.BreakpointOpcode.BREAKPOINT.create());
                break;
            case PAUSE:
                break;
        }
    }
}
