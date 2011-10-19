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

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
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

    private static final CiRegisterValue LDIV_TMP = RDX_L;


    /**
     * The register in which MUL puts the result for 64-bit multiplication.
     */
    private static final CiRegisterValue LMUL_OUT = RAX_L;

    private static final CiRegisterValue SHIFT_COUNT_IN = AMD64.rcx.asValue(CiKind.Int);

    protected static final CiValue ILLEGAL = CiValue.IllegalValue;

    public AMD64LIRGenerator(GraalCompilation compilation) {
        super(compilation);
    }

    @Override
    protected boolean canStoreAsConstant(ValueNode v, CiKind kind) {
        if (kind == CiKind.Short || kind == CiKind.Char) {
            // there is no immediate move of word values in asemblerI486.?pp
            return false;
        }
        return v instanceof ConstantNode;
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
    protected CiAddress genAddress(CiValue base, CiValue index, int shift, int disp, CiKind kind) {
        assert base.isVariableOrRegister();
        if (index.isConstant()) {
            return new CiAddress(kind, base, (((CiConstant) index).asInt() << shift) + disp);
        } else {
            assert index.isVariableOrRegister();
            return new CiAddress(kind, base, (index), CiAddress.Scale.fromShift(shift), disp);
        }
    }

    @Override
    protected void genCmpMemInt(Condition condition, CiValue base, int disp, int c, LIRDebugInfo info) {
        lir.cmpMemInt(condition, base, disp, c, info);
    }

    @Override
    protected void genCmpRegMem(Condition condition, CiValue reg, CiValue base, int disp, CiKind kind, LIRDebugInfo info) {
        lir.cmpRegMem(condition, reg, new CiAddress(kind, base, disp), info);
    }

    @Override
    protected boolean strengthReduceMultiply(CiValue left, int c, CiValue result, CiValue tmp) {
        if (tmp.isLegal()) {
            if (CiUtil.isPowerOf2(c + 1)) {
                lir.move(left, tmp);
                lir.shiftLeft(left, CiUtil.log2(c + 1), left);
                lir.sub(left, tmp, result);
                return true;
            } else if (CiUtil.isPowerOf2(c - 1)) {
                lir.move(left, tmp);
                lir.shiftLeft(left, CiUtil.log2(c - 1), left);
                lir.add(left, tmp, result);
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitNegate(NegateNode x) {
        LIRItem value = new LIRItem(x.x(), this);
        value.setDestroysRegister();
        value.loadItem();
        CiVariable reg = newVariable(x.kind);
        lir.negate(value.result(), reg);
        setResult(x, reg);
    }

    public boolean livesLonger(ValueNode x, ValueNode y) {
        if (y.usages().size() == 1) {
            return true;
        }
        return false;
    }

    public void visitArithmeticOpFloat(ArithmeticNode x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        assert !left.isStack() || !right.isStack() : "can't both be memory operands";
        boolean mustLoadBoth = x.opcode == Bytecodes.FREM || x.opcode == Bytecodes.DREM;

        // Both are in register, swap operands such that the short-living one is on the left side.
        if (x.isCommutative() && left.isRegisterOrVariable() && right.isRegisterOrVariable()) {
            if (livesLonger(x.x(), x.y())) {
                LIRItem tmp = left;
                left = right;
                right = tmp;
            }
        }

        if (left.isRegisterOrVariable() || x.x().isConstant() || mustLoadBoth) {
            left.loadItem();
        }

        if (mustLoadBoth) {
            // frem and drem destroy also right operand, so move it to a new register
            right.setDestroysRegister();
            right.loadItem();
        } else if (right.isRegisterOrVariable()) {
            right.loadItem();
        }

        CiVariable reg;

        if (x.opcode == Bytecodes.FREM) {
            reg = callRuntimeWithResult(CiRuntimeCall.ArithmeticFrem, null, left.result(), right.result());
        } else if (x.opcode == Bytecodes.DREM) {
            reg = callRuntimeWithResult(CiRuntimeCall.ArithmeticDrem, null, left.result(), right.result());
        } else {
            reg = newVariable(x.kind);
            arithmeticOpFpu(x.opcode, reg, left.result(), right.result(), ILLEGAL);
        }

        setResult(x, reg);
    }

    public void visitArithmeticOpLong(ArithmeticNode x) {
        int opcode = x.opcode;
        if (opcode == Bytecodes.LDIV || opcode == Bytecodes.LREM) {
            // emit inline 64-bit code
            LIRDebugInfo info = stateFor(x);
            CiValue dividend = force(x.x(), RAX_L); // dividend must be in RAX
            CiValue divisor = load(x.y());            // divisor can be in any (other) register

            CiValue result = createResultVariable(x);
            CiValue resultReg;
            if (opcode == Bytecodes.LREM) {
                resultReg = RDX_L; // remainder result is produced in rdx
                lir.lrem(dividend, divisor, resultReg, LDIV_TMP, info);
            } else {
                resultReg = RAX_L; // division result is produced in rax
                lir.ldiv(dividend, divisor, resultReg, LDIV_TMP, info);
            }

            lir.move(resultReg, result);
        } else if (opcode == Bytecodes.LMUL) {
            LIRItem right = new LIRItem(x.y(), this);

            // right register is destroyed by the long mul, so it must be
            // copied to a new register.
            right.setDestroysRegister();

            CiValue left = load(x.x());
            right.loadItem();

            arithmeticOpLong(opcode, LMUL_OUT, left, right.result());
            CiValue result = createResultVariable(x);
            lir.move(LMUL_OUT, result);
        } else {
            LIRItem right = new LIRItem(x.y(), this);

            CiValue left = load(x.x());
            // don't load constants to save register
            right.loadNonconstant();
            createResultVariable(x);
            arithmeticOpLong(opcode, x.operand(), left, right.result());
        }
    }

    public void visitArithmeticOpInt(ArithmeticNode x) {
        int opcode = x.opcode;
        if (opcode == Bytecodes.IDIV || opcode == Bytecodes.IREM) {
            // emit code for integer division or modulus

            // Call 'stateFor' before 'force()' because 'stateFor()' may
            // force the evaluation of other instructions that are needed for
            // correct debug info.  Otherwise the live range of the fixed
            // register might be too long.
            LIRDebugInfo info = stateFor(x);

            CiValue dividend = force(x.x(), RAX_I); // dividend must be in RAX
            CiValue divisor = load(x.y());          // divisor can be in any (other) register

            // idiv and irem use rdx in their implementation so the
            // register allocator must not assign it to an interval that overlaps
            // this division instruction.
            CiRegisterValue tmp = RDX_I;

            CiValue result = createResultVariable(x);
            CiValue resultReg;
            if (opcode == Bytecodes.IREM) {
                resultReg = tmp; // remainder result is produced in rdx
                lir.irem(dividend, divisor, resultReg, tmp, info);
            } else {
                resultReg = RAX_I; // division result is produced in rax
                lir.idiv(dividend, divisor, resultReg, tmp, info);
            }

            lir.move(resultReg, result);
        } else {
            // emit code for other integer operations
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);
            LIRItem leftArg = left;
            LIRItem rightArg = right;
            if (x.isCommutative() && left.isStack() && right.isRegisterOrVariable()) {
                // swap them if left is real stack (or cached) and right is real register(not cached)
                leftArg = right;
                rightArg = left;
            }

            leftArg.loadItem();

            // do not need to load right, as we can handle stack and constants
            if (opcode == Bytecodes.IMUL) {
                // check if we can use shift instead
                boolean useConstant = false;
                boolean useTmp = false;
                if (rightArg.result().isConstant()) {
                    int iconst = rightArg.instruction.asConstant().asInt();
                    if (iconst > 0) {
                        if (CiUtil.isPowerOf2(iconst)) {
                            useConstant = true;
                        } else if (CiUtil.isPowerOf2(iconst - 1) || CiUtil.isPowerOf2(iconst + 1)) {
                            useConstant = true;
                            useTmp = true;
                        }
                    }
                }
                if (!useConstant) {
                    rightArg.loadItem();
                }
                CiValue tmp = ILLEGAL;
                if (useTmp) {
                    tmp = newVariable(CiKind.Int);
                }
                createResultVariable(x);

                arithmeticOpInt(opcode, x.operand(), leftArg.result(), rightArg.result(), tmp);
            } else {
                createResultVariable(x);
                CiValue tmp = ILLEGAL;
                arithmeticOpInt(opcode, x.operand(), leftArg.result(), rightArg.result(), tmp);
            }
        }
    }

    @Override
    public void visitArithmetic(ArithmeticNode x) {
        assert Util.archKindsEqual(x.x().kind, x.kind) && Util.archKindsEqual(x.y().kind, x.kind) : "wrong parameter types: " + Bytecodes.nameOf(x.opcode) + ", x: " + x.x() + ", y: " + x.y() + ", kind: " + x.kind;
        switch (x.kind) {
            case Float:
            case Double:
                visitArithmeticOpFloat(x);
                return;
            case Long:
                visitArithmeticOpLong(x);
                return;
            case Int:
                visitArithmeticOpInt(x);
                return;
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    public void visitShift(ShiftNode x) {
        // count must always be in rcx
        CiValue count = makeOperand(x.y());
        boolean mustLoadCount = !count.isConstant() || x.kind == CiKind.Long;
        if (mustLoadCount) {
            // count for long must be in register
            count = force(x.y(), SHIFT_COUNT_IN);
        }

        CiValue value = load(x.x());
        CiValue reg = createResultVariable(x);

        shiftOp(x.opcode, reg, value, count, ILLEGAL);
    }

    @Override
    public void visitLogic(LogicNode x) {
        LIRItem right = new LIRItem(x.y(), this);

        CiValue left = load(x.x());
        right.loadNonconstant();
        CiValue reg = createResultVariable(x);

        logicOp(x.opcode, reg, left, right.result());
    }

    @Override
    public void visitNormalizeCompare(NormalizeCompareNode x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        if (!x.kind.isVoid() && x.x().kind.isLong()) {
            left.setDestroysRegister();
        }
        left.loadItem();
        right.loadItem();

        if (x.kind.isVoid()) {
            lir.cmp(Condition.TRUE, left.result(), right.result());
        } else if (x.x().kind.isFloat() || x.x().kind.isDouble()) {
            CiValue reg = createResultVariable(x);
            lir.fcmp2int(left.result(), right.result(), reg, x.isUnorderedLess());
        } else if (x.x().kind.isLong()) {
            CiValue reg = createResultVariable(x);
            lir.lcmp2int(left.result(), right.result(), reg);
        } else {
            assert false;
        }
    }

    @Override
    public void visitConvert(ConvertNode x) {
        CiValue input = load(x.value());
        CiVariable result = newVariable(x.kind);
        // arguments of lirConvert
        CompilerStub stub = null;
        // Checkstyle: off
        switch (x.opcode) {
            case F2I: stub = stubFor(CompilerStub.Id.f2i); break;
            case F2L: stub = stubFor(CompilerStub.Id.f2l); break;
            case D2I: stub = stubFor(CompilerStub.Id.d2i); break;
            case D2L: stub = stubFor(CompilerStub.Id.d2l); break;
        }
        // Checkstyle: on
        if (stub != null) {
            // Force result to be rax to match global stubs expectation.
            CiValue stubResult = x.kind == CiKind.Int ? RAX_I : RAX_L;
            lir.convert(x.opcode, input, stubResult, stub);
            lir.move(stubResult, result);
        } else {
            lir.convert(x.opcode, input, result, stub);
        }
        setResult(x, result);
    }

    @Override
    public void visitLoopBegin(LoopBeginNode x) {
    }

    @Override
    public void visitValueAnchor(ValueAnchorNode valueAnchor) {
        // nothing to do for ValueAnchors
    }

    @Override
    public void visitMathIntrinsic(MathIntrinsicNode node) {
        assert node.kind == CiKind.Double;
        LIRItem opd = new LIRItem(node.x(), this);
        opd.setDestroysRegister();
        opd.loadItem();
        CiVariable dest = createResultVariable(node);
        switch (node.operation()) {
            case ABS:   lir.abs(opd.result(), dest, CiValue.IllegalValue);   break;
            case SQRT:  lir.sqrt(opd.result(), dest, CiValue.IllegalValue);  break;
            case LOG:   lir.log(opd.result(), dest, CiValue.IllegalValue);   break;
            case LOG10: lir.log10(opd.result(), dest, CiValue.IllegalValue); break;
            case SIN:   lir.sin(opd.result(), dest, CiValue.IllegalValue, CiValue.IllegalValue); break;
            case COS:   lir.cos(opd.result(), dest, CiValue.IllegalValue, CiValue.IllegalValue); break;
            case TAN:   lir.tan(opd.result(), dest, CiValue.IllegalValue, CiValue.IllegalValue); break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitCompareAndSwap(CompareAndSwapNode node) {
        CiKind kind = node.newValue().kind;
        assert (kind == node.expected().kind);

        CiValue expected;
        if (kind == CiKind.Object) {
            expected = force(node.expected(), RAX_O);
        } else if (kind == CiKind.Int) {
            expected = force(node.expected(), RAX_I);
        } else if (kind == CiKind.Long) {
            expected = force(node.expected(), RAX_L);
        } else {
            throw Util.shouldNotReachHere();
        }
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
            lir.casObj(address, expected, newValue);
        } else if (kind == CiKind.Int) {
            lir.casInt(address, expected, newValue);
        } else if (kind == CiKind.Long) {
            lir.casLong(address, expected, newValue);
        }

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
