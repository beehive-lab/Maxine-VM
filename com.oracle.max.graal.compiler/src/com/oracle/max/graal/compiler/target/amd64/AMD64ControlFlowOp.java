/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.target.amd64;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;

public class AMD64ControlFlowOp {
    public static final LabelOp LABEL = new LabelOp();
    public static final JumpOp JUMP = new JumpOp();
    public static final BranchOp BRANCH = new BranchOp();
    public static final FloatBranchOp FLOAT_BRANCH = new FloatBranchOp();
    public static final CondMoveOp CMOVE = new CondMoveOp();
    public static final FloatCondMoveOp FLOAT_CMOVE = new FloatCondMoveOp();

    protected static class LabelOp implements LIROpcode<AMD64LIRAssembler, LIRLabel> {
        public LIRInstruction create(Label label) {
            return new LIRLabel(this, label);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRLabel op) {
            lasm.masm.bind(op.label);
        }
    }

    protected static class JumpOp implements LIROpcode<AMD64LIRAssembler, LIRBranch> {
        public LIRInstruction create(LIRBlock block) {
            return new LIRBranch(this, null, false, block);
        }

        public LIRInstruction create(Label label, LIRDebugInfo info) {
            return new LIRBranch(this, null, false, label, info);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRBranch op) {
            lasm.masm.jmp(op.label());
        }
    }

    protected static class BranchOp implements LIROpcode<AMD64LIRAssembler, LIRBranch> {
        public LIRInstruction create(Condition cond, LIRBlock block) {
            return new LIRBranch(this, cond, false, block);
        }

        public LIRInstruction create(Condition cond, Label label, LIRDebugInfo info) {
            return new LIRBranch(this, cond, false, label, info);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRBranch op) {
            lasm.masm.jcc(intCond(op.cond), op.label());
        }
    }

    protected static class FloatBranchOp implements LIROpcode<AMD64LIRAssembler, LIRBranch> {
        public LIRInstruction create(Condition cond, boolean unorderedIsTrue, LIRBlock block) {
            return new LIRBranch(this, cond, unorderedIsTrue, block);
        }

        public LIRInstruction create(Condition cond, boolean unorderedIsTrue, Label label, LIRDebugInfo info) {
            return new LIRBranch(this, cond, unorderedIsTrue, label, info);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRBranch op) {
            ConditionFlag cond = floatCond(op.cond);
            Label endLabel = new Label();
            if (op.unorderedIsTrue && !trueOnUnordered(cond)) {
                lasm.masm.jcc(ConditionFlag.parity, op.label());
            } else if (!op.unorderedIsTrue && trueOnUnordered(cond)) {
                lasm.masm.jcc(ConditionFlag.parity, endLabel);
            }
            lasm.masm.jcc(cond, op.label());
            lasm.masm.bind(endLabel);
        }
    }

    protected static class CondMoveOp implements LIROpcode<AMD64LIRAssembler, LIRCondition>, LIROpcode.SecondOperandRegisterHint {
        public LIRInstruction create(CiVariable result, Condition cond, CiVariable trueValue, CiValue falseValue) {
            return new LIRCondition(this, result, null, false, 0, 1, cond, false, trueValue, falseValue, trueValue);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRCondition op) {
            CiValue result = op.result();
            CiValue trueValue = op.operand(0);
            CiValue falseValue = op.operand(1);
            ConditionFlag cond = intCond(op.condition);
            // check that we don't overwrite an input operand before it is used.
            assert !result.equals(trueValue);

            AMD64MoveOp.move(lasm, result, falseValue);
            cmove(lasm, result, cond, trueValue);

        }
    }

    protected static class FloatCondMoveOp implements LIROpcode<AMD64LIRAssembler, LIRCondition>, LIROpcode.SecondOperandRegisterHint {
        public LIRInstruction create(CiVariable result, Condition cond, boolean unorderedIsTrue, CiVariable trueValue, CiVariable falseValue) {
            return new LIRCondition(this, result, null, false, 0, 1, cond, unorderedIsTrue, trueValue, falseValue, trueValue);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRCondition op) {
            CiValue result = op.result();
            CiValue trueValue = op.operand(0);
            CiValue falseValue = op.operand(1);
            ConditionFlag cond = floatCond(op.condition);
            boolean unorderedIsTrue = op.unorderedIsTrue;
            // check that we don't overwrite an input operand before it is used.
            assert !result.equals(trueValue);

            AMD64MoveOp.move(lasm, result, falseValue);
            cmove(lasm, result, cond, trueValue);
            if (unorderedIsTrue && !trueOnUnordered(cond)) {
                cmove(lasm, result, ConditionFlag.parity, trueValue);
            } else if (!unorderedIsTrue && trueOnUnordered(cond)) {
                cmove(lasm, result, ConditionFlag.parity, falseValue);
            }
        }
    }

    protected static void cmove(AMD64LIRAssembler lasm, CiValue result, ConditionFlag cond, CiValue other) {
        if (other.isRegister()) {
            assert lasm.asRegister(other) != lasm.asRegister(result) : "other already overwritten by previous move";
            switch (other.kind) {
                case Int:  lasm.masm.cmovl(cond, lasm.asRegister(result), lasm.asRegister(other)); break;
                case Long: lasm.masm.cmovq(cond, lasm.asRegister(result), lasm.asRegister(other)); break;
                default:   throw Util.shouldNotReachHere();
            }
        } else {
            switch (other.kind) {
                case Int:  lasm.masm.cmovl(cond, lasm.asRegister(result), lasm.asAddress(other)); break;
                case Long: lasm.masm.cmovq(cond, lasm.asRegister(result), lasm.asAddress(other)); break;
                default:   throw Util.shouldNotReachHere();
            }
        }
    }

    protected static ConditionFlag intCond(Condition cond) {
        switch (cond) {
            case EQ: return ConditionFlag.equal;
            case NE: return ConditionFlag.notEqual;
            case LT: return ConditionFlag.less;
            case LE: return ConditionFlag.lessEqual;
            case GE: return ConditionFlag.greaterEqual;
            case GT: return ConditionFlag.greater;
            case BE: return ConditionFlag.belowEqual;
            case AE: return ConditionFlag.aboveEqual;
            case AT: return ConditionFlag.above;
            case BT: return ConditionFlag.below;
            case OF: return ConditionFlag.overflow;
            case NOF: return ConditionFlag.noOverflow;
            default: throw Util.shouldNotReachHere();
        }
    }

    protected static ConditionFlag floatCond(Condition cond) {
        switch (cond) {
            case EQ: return ConditionFlag.equal;
            case NE: return ConditionFlag.notEqual;
            case BT: return ConditionFlag.below;
            case BE: return ConditionFlag.belowEqual;
            case AE: return ConditionFlag.aboveEqual;
            case AT: return ConditionFlag.above;
            default: throw Util.shouldNotReachHere();
        }
    }

    protected static boolean trueOnUnordered(ConditionFlag condition) {
        switch(condition) {
            case aboveEqual:
            case notEqual:
            case above:
                return false;
            case equal:
            case belowEqual:
            case below:
                return true;
            default:
                throw Util.shouldNotReachHere();
        }
    }
}
