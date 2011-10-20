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
import static com.oracle.max.graal.compiler.target.amd64.AMD64MulOp.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64DivOp.*;

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
    protected void genCmpMemInt(Condition condition, CiValue base, int disp, int c, LIRDebugInfo info) {
        lir.cmpMemInt(condition, base, disp, c, info);
    }

    @Override
    protected void genCmpRegMem(Condition condition, CiValue reg, CiValue base, int disp, CiKind kind, LIRDebugInfo info) {
        lir.cmpRegMem(condition, reg, new CiAddress(kind, base, disp), info);
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
    public void integerAdd(ValueNode resultNode, ValueNode leftNode, ValueNode rightNode) {
        CiVariable left = load(leftNode);
        CiValue right = loadNonconstant(rightNode);
        CiVariable result = createResultVariable(resultNode);

        lir.move(left, result);
        lir.append(IADD.create(result, right));
    }

    @Override
    public void emitUnsignedShiftRight(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        lir().unsignedShiftRight(value, count, dst, tmp);
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
