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

import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.xir.CiXirAssembler.XirMark;

/**
 * The {@code LirOpcode} enum represents the Operation code of each LIR instruction.
 */
public enum LegacyOpcode implements LIROpcode<LIRAssembler, LIRInstruction> {
    // Checkstyle: stop
    // @formatter:off
    BeginOp0,
        Label,
        Breakpoint,
        RuntimeCall,
        Membar,
        Branch,
        CondFloatBranch,
    EndOp0,
    BeginOp1,
        NullCheck,
        Return,
        Lea,
        Neg,
        TableSwitch,
        Move,
        Prefetchr,
        Prefetchw,
        Convert,
        Lsb,
        Msb,
        MonitorAddress,
    EndOp1,
    BeginOp2,
        Cmp,
        Cmpl2i,
        Ucmpfd2i,
        Cmpfd2i,
        Cmove,
        FCmove,
        UFCmove,
//        Add,
//        Sub,
//        Mul,
//        Div,
//        Rem,
        Sqrt,
        Abs,
        Sin,
        Cos,
        Tan,
        Log,
        Log10,
        LogicAnd,
        LogicOr,
        LogicXor,
        Shl,
        Shr,
        Ushr,
        CompareTo,
    EndOp2,
//    BeginOp3,
//        Idiv,
//        Irem,
//        Iudiv,
//        Iurem,
//        Ldiv,
//        Lrem,
//        Ludiv,
//        Lurem,
//    EndOp3,
    NativeCall,
    DirectCall,
    IndirectCall,
    InstanceOf,
    CheckCast,
    StoreCheck,
    Cas,
    Xir;
    // @formatter:on
    // Checkstyle: resume


    @Override
    public void emitCode(LIRAssembler lasm, LIRInstruction op) {
        lasm.verifyOopMap(op.info);

        LegacyOpcode code = (LegacyOpcode) op.code;
        switch (code) {
            case Label:
                lasm.asm.bind(((LIRLabel) op).label);
                break;
            case CondFloatBranch:
            case Branch:
                lasm.emitBranch((LIRBranch) op);
                break;
            case TableSwitch:
                lasm.emitTableSwitch((LIRTableSwitch) op);
                break;
            case Xir:
                lasm.emitXir((LIRXirInstruction) op);
                break;
            case Convert:
                lasm.emitConvert((LIRConvert) op);
                break;

            case Breakpoint:
                lasm.emitBreakpoint();
                break;
            case Membar:
                lasm.emitMemoryBarriers(((CiConstant) op.operand(0)).asInt());
                break;
            case MonitorAddress:
                lasm.emitMonitorAddress(((CiConstant) op.operand(0)).asInt(), op.result());
                break;

            case Cas:
                lasm.emitCompareAndSwap(op);
                break;

            case DirectCall: {
                LIRCall call = (LIRCall) op;
                lasm.emitCallAlignment(call.code);
                if (call.marks != null) {
                    call.marks.put(XirMark.CALLSITE, lasm.tasm.recordMark(null, new Mark[0]));
                }
                lasm.emitDirectCall(call.target, call.info);
                break;
            }
            case IndirectCall: {
                LIRCall call = (LIRCall) op;
                lasm.emitCallAlignment(call.code);
                if (call.marks != null) {
                    call.marks.put(XirMark.CALLSITE, lasm.tasm.recordMark(null, new Mark[0]));
                }
                lasm.emitIndirectCall(call.target, call.info, call.targetAddress());
                break;
            }
            case NativeCall: {
                LIRCall call = (LIRCall) op;
                lasm.emitNativeCall((String) call.target, call.info, call.targetAddress());
                break;
            }

            case Move:
                LIRMove move = (LIRMove) op;
                lasm.moveOp(op.operand(0), op.result(), move.kind, op.info);
                break;
            case Prefetchr:
                lasm.emitReadPrefetch(op.operand(0));
                break;
            case Prefetchw:
                lasm.emitReadPrefetch(op.operand(0));
                break;
            case Return:
                lasm.emitReturn(op.operand(0));
                break;
            case Neg:
                lasm.emitNegate(op.operand(0), op.result());
                break;
            case Lea:
                lasm.emitLea(op.operand(0), op.result());
                break;
            case NullCheck:
                lasm.emitNullCheck(op.operand(0), op.info);
                break;
            case Lsb:
                lasm.emitSignificantBitOp(code,  op.operand(0), op.result());
                break;
            case Msb:
                lasm.emitSignificantBitOp(code,  op.operand(0), op.result());
                break;

            case Cmp: {
                LIRCondition condOp = (LIRCondition) op;
                lasm.emitCompare(condOp.condition, op.operand(0), op.operand(1));
                break;
            }
            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                lasm.emitCompare2Int(op.code, op.operand(0), op.operand(1), op.result());
                break;

            case FCmove: {
                LIRCondition condOp = (LIRCondition) op;
                lasm.emitConditionalMove(condOp.condition, op.operand(0), op.operand(1), op.result(), true, false);
                break;
            }
            case UFCmove: {
                LIRCondition condOp = (LIRCondition) op;
                lasm.emitConditionalMove(condOp.condition, op.operand(0), op.operand(1), op.result(), true, true);
                break;
            }
            case Cmove: {
                LIRCondition condOp = (LIRCondition) op;
                lasm.emitConditionalMove(condOp.condition, op.operand(0), op.operand(1), op.result(), false, false);
                break;
            }

            case Shl:
            case Shr:
            case Ushr:
                if (op.operand(1).isConstant()) {
                    lasm.emitShiftOp(code, op.operand(0), ((CiConstant) op.operand(1)).asInt(), op.result());
                } else {
                    lasm.emitShiftOp(code, op.operand(0), op.operand(1), op.result());
                }
                break;

//            case Add:
//            case Sub:
//            case Mul:
//            case Div:
//            case Rem:
//                lasm.emitArithOp(code, op.operand(0), op.operand(1), op.result(), op.info);
//                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                lasm.emitIntrinsicOp(code, op.operand(0), op.operand(1), op.result());
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                lasm.emitLogicOp(code, op.operand(0), op.operand(1), op.result());
                break;


//            case Idiv  :
//            case Irem  : lasm.arithmeticIdiv(op.code, op.operand(0), op.operand(1), op.result(), op.info); break;
//            case Iudiv :
//            case Iurem : lasm.arithmeticIudiv(op.code, op.operand(0), op.operand(1), op.result(), op.info); break;
//            case Ldiv  :
//            case Lrem  : lasm.arithmeticLdiv(op.code, op.operand(0), op.operand(1), op.result(), op.info); break;
//            case Ludiv :
//            case Lurem : lasm.arithmeticLudiv(op.code, op.operand(0), op.operand(1), op.result(), op.info); break;

            default:
                throw Util.shouldNotReachHere();
        }
    }



}
