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
package com.oracle.max.graal.compiler.lir;

import com.oracle.max.asm.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiValue.Formatter;

public class LIRBranch extends LIRInstruction {

    /**
     * The condition when this branch is taken, or {@code null} if it is an unconditional branch.
     */
    public final Condition cond;

    /**
     * For floating point branches only. True when the branch should be taken when the comparison is unordered.
     */
    public final boolean unorderedIsTrue;

    private Label label;

    /**
     * The target block of this branch.
     */
    private LIRBlock block;



    public LIRBranch(LIROpcode code, Condition cond, boolean unorderedIsTrue, Label label, LIRDebugInfo info) {
        super(code, CiValue.IllegalValue, info);
        this.cond = cond;
        this.unorderedIsTrue = unorderedIsTrue;
        this.label = label;
    }

    public LIRBranch(LIROpcode code, Condition cond, boolean unorderedIsTrue, LIRBlock block) {
        super(code, CiValue.IllegalValue, block.debugInfo());
        this.cond = cond;
        this.unorderedIsTrue = unorderedIsTrue;
        this.label = block.label();
        this.block = block;
    }

    public Label label() {
        return label;
    }

    public LIRBlock block() {
        return block;
    }

    @Override
    public String operationString(Formatter operandFmt) {
        StringBuilder buf = new StringBuilder(cond.operator).append(' ');
        if (block() != null) {
            buf.append("[B").append(block.blockID()).append(']');
        } else if (label().isBound()) {
            buf.append("[label:0x").append(Integer.toHexString(label().position())).append(']');
        } else {
            buf.append("[label:??]");
        }
        if (unorderedIsTrue) {
            buf.append(" unorderedIsTrue");
        }
        return buf.toString();
    }

    public void substitute(LIRBlock oldBlock, LIRBlock newBlock) {
        assert newBlock != null;
        if (block == oldBlock) {
            block = newBlock;
            LIRInstruction instr = newBlock.lir().get(0);
            assert instr instanceof LIRLabel : "first instruction of block must be label";
            label = ((LIRLabel) instr).label;
        }
    }
}
