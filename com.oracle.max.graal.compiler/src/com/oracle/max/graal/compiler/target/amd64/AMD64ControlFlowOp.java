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

import static com.sun.cri.ci.CiRegister.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.*;
import com.sun.cri.ci.CiTargetMethod.*;

public class AMD64ControlFlowOp {
    public static final LabelOp LABEL = new LabelOp();
    public static final ReturnOp RETURN = new ReturnOp();
    public static final JumpOp JUMP = new JumpOp();
    public static final BranchOp BRANCH = new BranchOp();
    public static final TableSwitchOp TABLE_SWITCH = new TableSwitchOp();
    public static final FloatBranchOp FLOAT_BRANCH = new FloatBranchOp();
    public static final CondMoveOp CMOVE = new CondMoveOp();
    public static final FloatCondMoveOp FLOAT_CMOVE = new FloatCondMoveOp();

    protected static class LabelOp implements LIROpcode<AMD64MacroAssembler, LIRLabel> {
        public LIRInstruction create(Label label, boolean align) {
            return new LIRLabel(this, label, align);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRLabel op) {
            if (op.align) {
                tasm.masm.align(tasm.target.wordSize);
            }
            tasm.masm.bind(op.label);
        }
    }

    protected static class ReturnOp implements StandardOp.ReturnOpcode<AMD64MacroAssembler, LIRInstruction> {
        public LIRInstruction create(CiValue input) {
            return new LIRInstruction(this, CiValue.IllegalValue, null, input);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            tasm.masm.ret(0);
        }
    }

    protected static class JumpOp implements LIROpcode<AMD64MacroAssembler, LIRBranch> {
        public LIRInstruction create(LIRBlock block) {
            return new LIRBranch(this, null, false, block);
        }

        public LIRInstruction create(Label label, LIRDebugInfo info) {
            return new LIRBranch(this, null, false, label, info);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRBranch op) {
            tasm.masm.jmp(op.label());
        }
    }

    protected static class BranchOp implements LIROpcode<AMD64MacroAssembler, LIRBranch> {
        public LIRInstruction create(Condition cond, LIRBlock block) {
            return new LIRBranch(this, cond, false, block);
        }

        public LIRInstruction create(Condition cond, Label label, LIRDebugInfo info) {
            return new LIRBranch(this, cond, false, label, info);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRBranch op) {
            tasm.masm.jcc(intCond(op.cond), op.label());
        }
    }

    protected static class TableSwitchOp implements LIROpcode<AMD64MacroAssembler, LIRTableSwitch> {
        public LIRInstruction create(int lowKey, LIRBlock defaultTargets, LIRBlock[] targets, CiVariable index, CiVariable scratch) {
            return new LIRTableSwitch(this, lowKey, defaultTargets, targets, 1, 1, index, scratch);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRTableSwitch op) {
            CiRegister value = tasm.asIntReg(op.operand(0));
            CiRegister scratch = tasm.asLongReg(op.operand(1));

            AMD64MacroAssembler masm = tasm.masm;
            Buffer buf = masm.codeBuffer;

            // Compare index against jump table bounds
            int highKey = op.lowKey + op.targets.length - 1;
            if (op.lowKey != 0) {
                // subtract the low value from the switch value
                masm.subl(value, op.lowKey);
                masm.cmpl(value, highKey - op.lowKey);
            } else {
                masm.cmpl(value, highKey);
            }

            // Jump to default target if index is not within the jump table
            masm.jcc(ConditionFlag.above, op.defaultTarget.label());

            // Set scratch to address of jump table
            int leaPos = buf.position();
            masm.leaq(scratch, new CiAddress(tasm.target.wordKind, InstructionRelative.asValue(), 0));
            int afterLea = buf.position();

            // Load jump table entry into scratch and jump to it
            masm.movslq(value, new CiAddress(CiKind.Int, scratch.asValue(), value.asValue(), Scale.Times4, 0));
            masm.addq(scratch, value);
            masm.jmp(scratch);

            // Inserting padding so that jump table address is 4-byte aligned
            if ((buf.position() & 0x3) != 0) {
                masm.nop(4 - (buf.position() & 0x3));
            }

            // Patch LEA instruction above now that we know the position of the jump table
            int jumpTablePos = buf.position();
            buf.setPosition(leaPos);
            masm.leaq(scratch, new CiAddress(tasm.target.wordKind, InstructionRelative.asValue(), jumpTablePos - afterLea));
            buf.setPosition(jumpTablePos);

            // Emit jump table entries
            for (LIRBlock target : op.targets) {
                Label label = target.label();
                int offsetToJumpTableBase = buf.position() - jumpTablePos;
                if (label.isBound()) {
                    int imm32 = label.position() - jumpTablePos;
                    buf.emitInt(imm32);
                } else {
                    label.addPatchAt(buf.position());

                    buf.emitByte(0); // psuedo-opcode for jump table entry
                    buf.emitShort(offsetToJumpTableBase);
                    buf.emitByte(0); // padding to make jump table entry 4 bytes wide
                }
            }

            JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
            tasm.targetMethod.addAnnotation(jt);
        }
    }

    protected static class FloatBranchOp implements LIROpcode<AMD64MacroAssembler, LIRBranch> {
        public LIRInstruction create(Condition cond, boolean unorderedIsTrue, LIRBlock block) {
            return new LIRBranch(this, cond, unorderedIsTrue, block);
        }

        public LIRInstruction create(Condition cond, boolean unorderedIsTrue, Label label, LIRDebugInfo info) {
            return new LIRBranch(this, cond, unorderedIsTrue, label, info);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRBranch op) {
            ConditionFlag cond = floatCond(op.cond);
            Label endLabel = new Label();
            if (op.unorderedIsTrue && !trueOnUnordered(cond)) {
                tasm.masm.jcc(ConditionFlag.parity, op.label());
            } else if (!op.unorderedIsTrue && trueOnUnordered(cond)) {
                tasm.masm.jcc(ConditionFlag.parity, endLabel);
            }
            tasm.masm.jcc(cond, op.label());
            tasm.masm.bind(endLabel);
        }
    }

    protected static class CondMoveOp implements LIROpcode<AMD64MacroAssembler, LIRCondition>, LIROpcode.SecondOperandRegisterHint {
        public LIRInstruction create(CiVariable result, Condition cond, CiVariable trueValue, CiValue falseValue) {
            return new LIRCondition(this, result, null, false, 0, 1, cond, false, trueValue, falseValue, trueValue);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRCondition op) {
            CiValue result = op.result();
            CiValue trueValue = op.operand(0);
            CiValue falseValue = op.operand(1);
            ConditionFlag cond = intCond(op.condition);
            // check that we don't overwrite an input operand before it is used.
            assert !result.equals(trueValue);

            AMD64MoveOp.move(tasm, result, falseValue);
            cmove(tasm, result, cond, trueValue);

        }
    }

    protected static class FloatCondMoveOp implements LIROpcode<AMD64MacroAssembler, LIRCondition>, LIROpcode.SecondOperandRegisterHint {
        public LIRInstruction create(CiVariable result, Condition cond, boolean unorderedIsTrue, CiVariable trueValue, CiVariable falseValue) {
            return new LIRCondition(this, result, null, false, 0, 1, cond, unorderedIsTrue, trueValue, falseValue, trueValue);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRCondition op) {
            CiValue result = op.result();
            CiValue trueValue = op.operand(0);
            CiValue falseValue = op.operand(1);
            ConditionFlag cond = floatCond(op.condition);
            boolean unorderedIsTrue = op.unorderedIsTrue;
            // check that we don't overwrite an input operand before it is used.
            assert !result.equals(trueValue);

            AMD64MoveOp.move(tasm, result, falseValue);
            cmove(tasm, result, cond, trueValue);
            if (unorderedIsTrue && !trueOnUnordered(cond)) {
                cmove(tasm, result, ConditionFlag.parity, trueValue);
            } else if (!unorderedIsTrue && trueOnUnordered(cond)) {
                cmove(tasm, result, ConditionFlag.parity, falseValue);
            }
        }
    }

    protected static void cmove(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, ConditionFlag cond, CiValue other) {
        if (other.isRegister()) {
            assert tasm.asRegister(other) != tasm.asRegister(result) : "other already overwritten by previous move";
            switch (other.kind) {
                case Int:  tasm.masm.cmovl(cond, tasm.asRegister(result), tasm.asRegister(other)); break;
                case Long: tasm.masm.cmovq(cond, tasm.asRegister(result), tasm.asRegister(other)); break;
                default:   throw Util.shouldNotReachHere();
            }
        } else {
            switch (other.kind) {
                case Int:  tasm.masm.cmovl(cond, tasm.asRegister(result), tasm.asAddress(other)); break;
                case Long: tasm.masm.cmovq(cond, tasm.asRegister(result), tasm.asAddress(other)); break;
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
