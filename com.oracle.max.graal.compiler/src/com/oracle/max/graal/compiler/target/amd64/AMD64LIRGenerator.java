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
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOpFI.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ConvertOpFL.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64DivOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MathIntrinsicOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MulOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64Op1.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64ShiftOp.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.alloc.OperandPool.VariableFlag;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
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

    protected static final CiValue ILLEGAL = CiValue.IllegalValue;

    public AMD64LIRGenerator(GraalCompilation compilation) {
        super(compilation);
    }

    @Override
    protected boolean canStoreAsConstant(CiKind storeKind) {
        // there is no immediate move of word values in asemblerI486.?pp
        // TODO: check if this is necessary
        return storeKind != CiKind.Short && storeKind != CiKind.Char;
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
    protected CiVariable makeByteVariable(CiValue cur) {
        CiVariable result = operands.newVariable(cur.kind.stackKind(), VariableFlag.MustBeByteRegister);
        lir.move(cur, result);
        return result;
    }

    @Override
    public void visitNegate(NegateNode x) {
        CiValue input = loadNonconstant(x.x());
        CiVariable result = createResultVariable(x);

        lir.move(input, result);
        switch (x.kind) {
            case Int:    lir.append(INEG.create(result)); break;
            case Long:   lir.append(LNEG.create(result)); break;
            case Float:  lir.append(FNEG.create(result)); break;
            case Double: lir.append(DNEG.create(result)); break;
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
                    lir.move(left, RAX_I);
                    lir.append(IDIV.create(RAX_I, info, RAX_I, right));
                    lir.move(RAX_I, result);
                    break;
                case Bytecodes.IREM:
                    lir.move(left, RAX_I);
                    lir.append(IREM.create(RDX_I, info, RAX_I, right));
                    lir.move(RDX_I, result);
                    break;
                case Bytecodes.LDIV:
                    lir.move(left, RAX_L);
                    lir.append(LDIV.create(RAX_L, info, RAX_L, right));
                    lir.move(RAX_L, result);
                    break;
                case Bytecodes.LREM:
                    lir.move(left, RAX_L);
                    lir.append(LREM.create(RDX_L, info, RAX_L, right));
                    lir.move(RDX_L, result);
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
            lir.move(reg, result);

        } else {
            CiVariable left = load(x.x());
            CiValue right = loadNonconstant(x.y());
            CiVariable result = createResultVariable(x);

            // Two-operand form on Intel
            lir.move(left, result);
            switch (opcode) {
                case Bytecodes.IADD: lir.append(IADD.create(result, right)); break;
                case Bytecodes.ISUB: lir.append(ISUB.create(result, right)); break;
                case Bytecodes.IMUL: lir.append(IMUL.create(result, right)); break;
                case Bytecodes.LADD: lir.append(LADD.create(result, right)); break;
                case Bytecodes.LSUB: lir.append(LSUB.create(result, right)); break;
                case Bytecodes.LMUL: lir.append(LMUL.create(result, right)); break;
                case Bytecodes.FADD: lir.append(FADD.create(result, right)); break;
                case Bytecodes.FSUB: lir.append(FSUB.create(result, right)); break;
                case Bytecodes.FMUL: lir.append(FMUL.create(result, right)); break;
                case Bytecodes.FDIV: lir.append(FDIV.create(result, right)); break;
                case Bytecodes.DADD: lir.append(DADD.create(result, right)); break;
                case Bytecodes.DSUB: lir.append(DSUB.create(result, right)); break;
                case Bytecodes.DMUL: lir.append(DMUL.create(result, right)); break;
                case Bytecodes.DDIV: lir.append(DDIV.create(result, right)); break;
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
        lir.move(left, result);
        switch (x.opcode) {
            case Bytecodes.IAND: lir.append(IAND.create(result, right)); break;
            case Bytecodes.LAND: lir.append(LAND.create(result, right)); break;
            case Bytecodes.IOR:  lir.append(IOR.create(result, right)); break;
            case Bytecodes.LOR:  lir.append(LOR.create(result, right)); break;
            case Bytecodes.IXOR: lir.append(IXOR.create(result, right)); break;
            case Bytecodes.LXOR: lir.append(LXOR.create(result, right)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitShift(ShiftNode x) {
        CiVariable left = load(x.x());
        CiValue right = loadNonconstant(x.y());
        CiVariable result = createResultVariable(x);

        // Two-operand form on Intel
        lir.move(left, result);
        if (!right.isConstant()) {
            // Non-constant shift count must be in RCX
            lir.move(right, RCX_I);
            right = RCX_I;
        }

        switch (x.opcode) {
            case Bytecodes.ISHL: lir.append(ISHL.create(result, right)); break;
            case Bytecodes.ISHR: lir.append(ISHR.create(result, right)); break;
            case Bytecodes.IUSHR: lir.append(UISHR.create(result, right)); break;
            case Bytecodes.LSHL: lir.append(LSHL.create(result, right)); break;
            case Bytecodes.LSHR: lir.append(LSHR.create(result, right)); break;
            case Bytecodes.LUSHR: lir.append(ULSHR.create(result, right)); break;
            default: Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitConvert(ConvertNode x) {
        CiVariable input = load(x.value());
        CiVariable result = createResultVariable(x);

        switch (x.opcode) {
            case I2L: lir.append(I2L.create(result, input)); break;
            case L2I: lir.append(L2I.create(result, input)); break;
            case I2B: lir.append(I2B.create(result, input)); break;
            case I2C: lir.append(I2C.create(result, input)); break;
            case I2S: lir.append(I2S.create(result, input)); break;
            case F2D: lir.append(F2D.create(result, input)); break;
            case D2F: lir.append(D2F.create(result, input)); break;
            case I2F: lir.append(I2F.create(result, input)); break;
            case I2D: lir.append(I2D.create(result, input)); break;
            case F2I: lir.append(F2I.create(result, stubFor(CompilerStub.Id.f2i), input)); break;
            case D2I: lir.append(D2I.create(result, stubFor(CompilerStub.Id.d2i), input)); break;
            case L2F: lir.append(L2F.create(result, input)); break;
            case L2D: lir.append(L2D.create(result, input)); break;
            case F2L: lir.append(F2L.create(result, stubFor(CompilerStub.Id.f2l), input, newVariable(CiKind.Long))); break;
            case D2L: lir.append(D2L.create(result, stubFor(CompilerStub.Id.d2l), input, newVariable(CiKind.Long))); break;
            case MOV_I2F: lir.append(MOV_I2F.create(result, input)); break;
            case MOV_L2D: lir.append(MOV_L2D.create(result, input)); break;
            case MOV_F2I: lir.append(MOV_F2I.create(result, input)); break;
            case MOV_D2L: lir.append(MOV_D2L.create(result, input)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitMathIntrinsic(MathIntrinsicNode x) {
        CiVariable input = load(x.x());
        CiVariable result = createResultVariable(x);

        switch (x.operation()) {
            case ABS:
                lir.move(input, result);
                lir.append(DABS.create(result));
                break;
            case SQRT:  lir.append(SQRT.create(result, input)); break;
            case LOG:   lir.append(LOG.create(result, input)); break;
            case LOG10: lir.append(LOG10.create(result, input)); break;
            case SIN:   lir.append(SIN.create(result, input)); break;
            case COS:   lir.append(COS.create(result, input)); break;
            case TAN:   lir.append(TAN.create(result, input)); break;
            default:    throw Util.shouldNotReachHere();
        }
    }


    @Override
    public void integerAdd(ValueNode resultNode, ValueNode leftNode, ValueNode rightNode) {
        CiVariable left = load(leftNode);
        CiValue right = loadNonconstant(rightNode);
        CiVariable result = createResultVariable(resultNode);

        lir.move(left, result);
        lir.append(IADD.create(result, right));
    }

    @Override
    public void emitUnsignedShiftRight(CiVariable value, CiValue count, CiVariable dest) {
        assert value.equals(dest);
        switch (dest.kind) {
            case Int: lir.append(UISHR.create(dest, count)); break;
            case Long: lir.append(ULSHR.create(dest, count)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void emitAdd(CiVariable a, CiValue b, CiVariable dest) {
        assert a.equals(dest);
        switch (dest.kind) {
            case Int: lir.append(IADD.create(dest, b)); break;
            case Long: lir.append(LADD.create(dest, b)); break;
            case Float: lir.append(FADD.create(dest, b)); break;
            case Double: lir.append(DADD.create(dest, b)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompare(ValueNode leftNode, ValueNode rightNode) {
        CiVariable left = load(leftNode);
        CiValue right = loadNonconstant(rightNode);

        switch (left.kind) {
            case Int: lir.append(ICMP.create(left, right)); break;
            case Long: lir.append(LCMP.create(left, right)); break;
            case Object: lir.append(ACMP.create(left, right)); break;
            case Float: lir.append(FCMP.create(left, right)); break;
            case Double: lir.append(DCMP.create(left, right)); break;
            default: throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitNormalizeCompare(NormalizeCompareNode x) {
        CiVariable left = load(x.x());
        CiVariable right = load(x.y());
        CiVariable result = createResultVariable(x);

        switch (x.x().kind){
            case Float:
            case Double:
                lir.fcmp2int(left, right, result, x.isUnorderedLess());
                break;
            case Long:
                lir.lcmp2int(left, right, result);
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

        CiValue expected = force(node.expected(), AMD64.rax.asValue(kind));
        CiValue newValue = load(node.newValue());
        CiValue object = load(node.object());
        CiValue offset;
        if (node.offset().isConstant()) {
            // NOTE: need int for addressing, but could be wrong for very large offsets (unlikely)
            offset = CiConstant.forInt((int) node.offset().asConstant().asLong());
        } else {
            offset = load(node.offset());
        }
        CiValue address = newVariable(compilation.compiler.target.wordKind);
        lir.lea(new CiAddress(node.object().kind, object, offset), address);

        if (kind == CiKind.Object) {
            preGCWriteBarrier(address, false, null);
        }
        lir.cas(address, expected, newValue, expected);

        CiValue result = createResultVariable(node);
        lir.cmove(Condition.EQ, CiConstant.TRUE, CiConstant.FALSE, result);

        if (kind == CiKind.Object) {
            postGCWriteBarrier(address, newValue);
        }
    }

    @Override
    public Condition floatingPointCondition(Condition cond) {
        switch(cond) {
            case LT:
                return Condition.BT;
            case LE:
                return Condition.BE;
            case GT:
                return Condition.AT;
            case GE:
                return Condition.AE;
            default :
                return cond;
        }
    }

    @Override
    public void emitMove(CiValue src, CiValue dst) {
        lir().move(src, dst);
    }

    @Override
    public void emitLea(CiAddress address, CiVariable dest) {
        lir().lea(address, dest);
    }
}
