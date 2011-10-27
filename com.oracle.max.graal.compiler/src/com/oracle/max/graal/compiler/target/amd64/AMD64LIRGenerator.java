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

package com.oracle.max.graal.compiler.target.amd64;

import static com.oracle.max.graal.compiler.target.amd64.AMD64ArithmeticOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64CompareOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64CompareToIntOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ControlFlowOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOpFI.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOpFL.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64DivOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64HotSpotOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MaxineOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MathIntrinsicOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MoveOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MulOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64Op1.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ShiftOp.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.lir.FrameMap.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * This class implements the X86-specific portion of the LIR generator.
 */
public class AMD64LIRGenerator extends LIRGenerator {

    private static final CiRegisterValue RAX_I = AMD64.rax.asValue(CiKind.Int);
    private static final CiRegisterValue RAX_L = AMD64.rax.asValue(CiKind.Long);
    private static final CiRegisterValue RAX_O = AMD64.rax.asValue(CiKind.Object);
    private static final CiRegisterValue RDX_I = AMD64.rdx.asValue(CiKind.Int);
    private static final CiRegisterValue RDX_L = AMD64.rdx.asValue(CiKind.Long);
    private static final CiRegisterValue RCX_I = AMD64.rcx.asValue(CiKind.Int);

    static {
        StandardOp.MOVE = AMD64MoveOp.MOVE;
        StandardOp.NULL_CHECK = AMD64MoveOp.NULL_CHECK;
        StandardOp.DIRECT_CALL = AMD64CallOp.DIRECT_CALL;
        StandardOp.INDIRECT_CALL = AMD64CallOp.INDIRECT_CALL;
        StandardOp.RETURN = AMD64ControlFlowOp.RETURN;
        StandardOp.XIR = AMD64XirOp.XIR;
    }

    public AMD64LIRGenerator(GraalCompilation compilation) {
        super(compilation);
        ir.methodEndMarker = new AMD64MethodEndStub();
    }

    @Override
    protected boolean canStoreConstant(CiConstant c) {
        // there is no immediate move of 64-bit values on Intel
        switch (c.kind) {
            case Long:   return Util.isInt(c.asLong());
            case Double: return false;
            case Object: return c.isNull();
            default:     return true;
        }
    }

    @Override
    public boolean canInlineAsConstant(ValueNode v) {
        if (!v.isConstant()) {
            return false;
        }
        if (v.kind == CiKind.Long) {
            return NumUtil.isInt(v.asConstant().asLong());
        }
        return v.kind != CiKind.Object || v.isNullConstant();
    }

    @Override
    public CiVariable emitMove(CiValue input) {
        CiVariable result = newVariable(input.kind.stackKind());
        append(MOVE.create(result, input));
        return result;
    }

    @Override
    public CiVariable emitLoad(CiAddress loadAddress, CiKind kind, Object debugInfo) {
        CiVariable result = newVariable(kind.stackKind());
        append(LOAD.create(result, loadAddress, kind, (LIRDebugInfo) debugInfo));
        return result;
    }

    @Override
    public void emitStore(CiAddress storeAddress, CiValue input, CiKind kind, Object debugInfo) {
        append(STORE.create(storeAddress, input, kind, (LIRDebugInfo) debugInfo));
    }

    @Override
    public void emitLabel(Label label, boolean align) {
        append(LABEL.create(label, align));
    }

    @Override
    public void emitJump(LIRBlock block) {
        append(JUMP.create(block));
    }

    @Override
    public void emitJump(Label label, LIRDebugInfo info) {
        append(JUMP.create(label, info));
    }

    @Override
    public void emitBranch(CiValue left, CiValue right, Condition cond, boolean unorderedIsTrue, LIRBlock block) {
        emitCompare(left, right);
        switch (left.kind) {
            case Int:
            case Long:
            case Object: append(BRANCH.create(cond, block)); break;
            case Float:
            case Double: append(FLOAT_BRANCH.create(cond, unorderedIsTrue, block)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void emitBranch(CiValue left, CiValue right, Condition cond, boolean unorderedIsTrue, Label label, LIRDebugInfo info) {
        emitCompare(left, right);
        switch (left.kind) {
            case Int:
            case Long:
            case Object: append(BRANCH.create(cond, label, info)); break;
            case Float:
            case Double: append(FLOAT_BRANCH.create(cond, unorderedIsTrue, label, info)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public CiVariable emitCMove(CiValue left, CiValue right, Condition cond, boolean unorderedIsTrue, CiValue trueValue, CiValue falseValue) {
        emitCompare(left, right);

        CiVariable result = newVariable(trueValue.kind);
        switch (left.kind) {
            case Int:
            case Long:
            case Object: append(CMOVE.create(result, cond, makeVariable(trueValue), falseValue)); break;
            case Float:
            case Double: append(FLOAT_CMOVE.create(result, cond, unorderedIsTrue, makeVariable(trueValue), makeVariable(falseValue))); break;

        }
        return result;
    }

    protected void emitCompare(CiValue leftVal, CiValue right) {
        CiVariable left = makeVariable(leftVal);
        switch (left.kind) {
            case Jsr:
            case Int: append(ICMP.create(left, right)); break;
            case Long: append(LCMP.create(left, right)); break;
            case Object: append(ACMP.create(left, right)); break;
            case Float: append(FCMP.create(left, right)); break;
            case Double: append(DCMP.create(left, right)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitNegate(NegateNode x) {
        CiValue input = loadNonconstant(x.x());
        CiVariable result = createResultVariable(x);

        append(MOVE.create(result, input));
        switch (x.kind) {
            case Int:    append(INEG.create(result)); break;
            case Long:   append(LNEG.create(result)); break;
            case Float:  append(FNEG.create(result)); break;
            case Double: append(DNEG.create(result)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitArithmetic(ArithmeticNode x) {
        assert Util.archKindsEqual(x.x().kind, x.kind) && Util.archKindsEqual(x.y().kind, x.kind);

        int opcode = x.opcode;
        if (opcode == Bytecodes.IDIV || opcode == Bytecodes.IREM || opcode == Bytecodes.LDIV || opcode == Bytecodes.LREM) {
            LIRDebugInfo info = stateFor(x);

            CiVariable left = load(x.x());
            CiVariable right = load(x.y());
            CiVariable result = createResultVariable(x);

            switch (opcode) {
                case Bytecodes.IDIV:
                    append(MOVE.create(RAX_I, left));
                    append(IDIV.create(RAX_I, info, RAX_I, right));
                    append(MOVE.create(result, RAX_I));
                    break;
                case Bytecodes.IREM:
                    append(MOVE.create(RAX_I, left));
                    append(IREM.create(RDX_I, info, RAX_I, right));
                    append(MOVE.create(result, RDX_I));
                    break;
                case Bytecodes.LDIV:
                    append(MOVE.create(RAX_L, left));
                    append(LDIV.create(RAX_L, info, RAX_L, right));
                    append(MOVE.create(result, RAX_L));
                    break;
                case Bytecodes.LREM:
                    append(MOVE.create(RAX_L, left));
                    append(LREM.create(RDX_L, info, RAX_L, right));
                    append(MOVE.create(result, RDX_L));
                    break;
            }

        } else if (x.opcode == Bytecodes.FREM || x.opcode == Bytecodes.DREM) {
            CiVariable left = load(x.x());
            CiVariable right = load(x.y());
            CiVariable result = createResultVariable(x);

            CiValue reg;
            if (x.opcode == Bytecodes.FREM) {
                reg = callRuntime(CiRuntimeCall.ArithmeticFrem, null, left, right);
            } else if (x.opcode == Bytecodes.DREM) {
                reg = callRuntime(CiRuntimeCall.ArithmeticDrem, null, left, right);
            } else {
                throw Util.shouldNotReachHere();
            }
            append(MOVE.create(result, reg));

        } else {
            CiVariable left = load(x.x());
            CiValue right = loadNonconstant(x.y());
            CiVariable result = createResultVariable(x);

            // Two-operand form on Intel
            append(MOVE.create(result, left));
            switch (opcode) {
                case Bytecodes.IADD: append(IADD.create(result, right)); break;
                case Bytecodes.ISUB: append(ISUB.create(result, right)); break;
                case Bytecodes.IMUL: append(IMUL.create(result, right)); break;
                case Bytecodes.LADD: append(LADD.create(result, right)); break;
                case Bytecodes.LSUB: append(LSUB.create(result, right)); break;
                case Bytecodes.LMUL: append(LMUL.create(result, right)); break;
                case Bytecodes.FADD: append(FADD.create(result, right)); break;
                case Bytecodes.FSUB: append(FSUB.create(result, right)); break;
                case Bytecodes.FMUL: append(FMUL.create(result, right)); break;
                case Bytecodes.FDIV: append(FDIV.create(result, right)); break;
                case Bytecodes.DADD: append(DADD.create(result, right)); break;
                case Bytecodes.DSUB: append(DSUB.create(result, right)); break;
                case Bytecodes.DMUL: append(DMUL.create(result, right)); break;
                case Bytecodes.DDIV: append(DDIV.create(result, right)); break;
                default: throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    public void visitLogic(LogicNode x) {
        CiVariable left = load(x.x());
        CiValue right = loadNonconstant(x.y());
        CiVariable result = createResultVariable(x);

        // Two-operand form on Intel
        append(MOVE.create(result, left));
        switch (x.opcode) {
            case Bytecodes.IAND: append(IAND.create(result, right)); break;
            case Bytecodes.LAND: append(LAND.create(result, right)); break;
            case Bytecodes.IOR:  append(IOR.create(result, right)); break;
            case Bytecodes.LOR:  append(LOR.create(result, right)); break;
            case Bytecodes.IXOR: append(IXOR.create(result, right)); break;
            case Bytecodes.LXOR: append(LXOR.create(result, right)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitShift(ShiftNode x) {
        CiVariable left = load(x.x());
        CiValue right = loadNonconstant(x.y());
        CiVariable result = createResultVariable(x);

        // Two-operand form on Intel
        append(MOVE.create(result, left));
        if (!right.isConstant()) {
            // Non-constant shift count must be in RCX
            append(MOVE.create(RCX_I, right));
            right = RCX_I;
        }

        switch (x.opcode) {
            case Bytecodes.ISHL: append(ISHL.create(result, right)); break;
            case Bytecodes.ISHR: append(ISHR.create(result, right)); break;
            case Bytecodes.IUSHR: append(UISHR.create(result, right)); break;
            case Bytecodes.LSHL: append(LSHL.create(result, right)); break;
            case Bytecodes.LSHR: append(LSHR.create(result, right)); break;
            case Bytecodes.LUSHR: append(ULSHR.create(result, right)); break;
            default: Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitConvert(ConvertNode x) {
        CiVariable input = load(x.value());
        CiVariable result = createResultVariable(x);

        switch (x.opcode) {
            case I2L: append(I2L.create(result, input)); break;
            case L2I: append(L2I.create(result, input)); break;
            case I2B: append(I2B.create(result, input)); break;
            case I2C: append(I2C.create(result, input)); break;
            case I2S: append(I2S.create(result, input)); break;
            case F2D: append(F2D.create(result, input)); break;
            case D2F: append(D2F.create(result, input)); break;
            case I2F: append(I2F.create(result, input)); break;
            case I2D: append(I2D.create(result, input)); break;
            case F2I: append(F2I.create(result, stubFor(CompilerStub.Id.f2i), input)); break;
            case D2I: append(D2I.create(result, stubFor(CompilerStub.Id.d2i), input)); break;
            case L2F: append(L2F.create(result, input)); break;
            case L2D: append(L2D.create(result, input)); break;
            case F2L: append(F2L.create(result, stubFor(CompilerStub.Id.f2l), input, newVariable(CiKind.Long))); break;
            case D2L: append(D2L.create(result, stubFor(CompilerStub.Id.d2l), input, newVariable(CiKind.Long))); break;
            case MOV_I2F: append(MOV_I2F.create(result, input)); break;
            case MOV_L2D: append(MOV_L2D.create(result, input)); break;
            case MOV_F2I: append(MOV_F2I.create(result, input)); break;
            case MOV_D2L: append(MOV_D2L.create(result, input)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitMathIntrinsic(MathIntrinsicNode x) {
        CiVariable input = load(x.x());
        CiVariable result = createResultVariable(x);

        switch (x.operation()) {
            case ABS:
                append(MOVE.create(result, input));
                append(DABS.create(result));
                break;
            case SQRT:  append(SQRT.create(result, input)); break;
            case LOG:   append(LOG.create(result, input)); break;
            case LOG10: append(LOG10.create(result, input)); break;
            case SIN:   append(SIN.create(result, input)); break;
            case COS:   append(COS.create(result, input)); break;
            case TAN:   append(TAN.create(result, input)); break;
            default:    throw Util.shouldNotReachHere();
        }
    }


    @Override
    public void integerAdd(ValueNode resultNode, ValueNode leftNode, ValueNode rightNode) {
        CiVariable left = load(leftNode);
        CiValue right = loadNonconstant(rightNode);
        CiVariable result = createResultVariable(resultNode);

        append(MOVE.create(result, left));
        append(IADD.create(result, right));
    }

    @Override
    public void emitUnsignedShiftRight(CiVariable value, CiValue count, CiVariable dest) {
        assert value.equals(dest);
        switch (dest.kind) {
            case Int: append(UISHR.create(dest, count)); break;
            case Long: append(ULSHR.create(dest, count)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void emitAdd(CiVariable a, CiValue b, CiVariable dest) {
        assert a.equals(dest);
        switch (dest.kind) {
            case Int: append(IADD.create(dest, b)); break;
            case Long: append(LADD.create(dest, b)); break;
            case Float: append(FADD.create(dest, b)); break;
            case Double: append(DADD.create(dest, b)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void deoptimizeOn(Condition cond) {
        LIRDebugInfo info = stateFor(null);
        Label stubEntry = createDeoptStub(DeoptAction.InvalidateReprofile, info, cond);
        append(BRANCH.create(cond, stubEntry, info));
    }

    @Override
    public void visitNormalizeCompare(NormalizeCompareNode x) {
        CiVariable result = createResultVariable(x);
        emitCompare(makeOperand(x.x()), makeOperand(x.y()));
        switch (x.x().kind){
            case Float:
            case Double:
                if (x.isUnorderedLess()) {
                    append(CMP2INT_UL.create(result));
                } else {
                    append(CMP2INT_UG.create(result));
                }
                break;
            case Long:
                append(CMP2INT.create(result));
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }



    @Override
    public void visitLoopBegin(LoopBeginNode x) {
    }

    @Override
    public void visitValueAnchor(ValueAnchorNode valueAnchor) {
        // nothing to do for ValueAnchors
    }

    @Override
    public void visitCompareAndSwap(CompareAndSwapNode node) {
        CiKind kind = node.newValue().kind;
        assert kind == node.expected().kind;

        CiValue expected = loadNonconstant(node.expected());
        CiVariable newValue = load(node.newValue());
        CiVariable object = load(node.object());
        CiValue offset = loadNonconstant(node.offset());

        CiAddress address = new CiAddress(compilation.compiler.target.wordKind, object, offset);
        CiVariable loadedAddress = null;
        if (kind == CiKind.Object) {
            loadedAddress = newVariable(compilation.compiler.target.wordKind);
            append(LEA.create(loadedAddress, address));
            preGCWriteBarrier(loadedAddress, false, null);
            address = new CiAddress(compilation.compiler.target.wordKind, loadedAddress);
        }

        CiRegisterValue rax = AMD64.rax.asValue(kind);
        append(MOVE.create(rax, expected));
        append(CAS.create(rax, address, rax, newValue));

        CiVariable result = createResultVariable(node);
        if (node.directResult()) {
            append(MOVE.create(result, rax));
        } else {
            append(CMOVE.create(result, Condition.EQ, makeVariable(CiConstant.TRUE), CiConstant.FALSE));
        }

        if (kind == CiKind.Object) {
            postGCWriteBarrier(loadedAddress, newValue);
        }
    }

    @Override
    public void emitMove(CiValue src, CiValue dst) {
        append(MOVE.create(dst, src));
    }

    @Override
    public void emitLea(CiAddress address, CiVariable dest) {
        append(LEA.create(dest, address));
    }

    @Override
    public CiValue createMonitorAddress(int monitorIndex) {
        CiVariable result = newVariable(target().wordKind);
        append(MONITOR_ADDRESS.create(result, monitorIndex));
        return result;
    }

    @Override
    public void emitMembar(int barriers) {
        int necessaryBarriers = compilation.compiler.target.arch.requiredBarriers(barriers);
        if (compilation.compiler.target.isMP && necessaryBarriers != 0) {
            append(MEMBAR.create(necessaryBarriers));
        }
    }

    @Override
    protected void emitTableSwitch(int lowKey, LIRBlock defaultTargets, LIRBlock[] targets, CiValue index) {
        // Making a copy of the switch value is necessary because jump table destroys the input value
        CiVariable tmp = emitMove(index);
        append(TABLE_SWITCH.create(lowKey, defaultTargets, targets, tmp, newVariable(compilation.compiler.target.wordKind)));
    }

    @Override
    protected Label createDeoptStub(DeoptAction action, LIRDebugInfo info, Object deoptInfo) {
        assert info.state != null : "deoptimize instruction always needs a state";
        assert info.state.bci != FixedWithNextNode.SYNCHRONIZATION_ENTRY_BCI : "bci must not be -1 for deopt framestate";
        AMD64DeoptimizationStub stub = new AMD64DeoptimizationStub(action, info, deoptInfo);
        ir.deoptimizationStubs.add(stub);
        return stub.label;
    }

    @Override
    public void visitStackAllocate(StackAllocateNode x) {
        CiVariable result = createResultVariable(x);
        assert x.size().isConstant() : "ALLOCA bytecode 'size' operand is not a constant: " + x.size();
        StackBlock stackBlock = compilation.frameMap().reserveStackBlock(x.size().asConstant().asInt());
        append(STACK_ALLOCATE.create(result, stackBlock));
    }
}
