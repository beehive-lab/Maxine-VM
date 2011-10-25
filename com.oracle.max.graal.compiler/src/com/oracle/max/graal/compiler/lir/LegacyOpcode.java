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
        Breakpoint,
        RuntimeCall,
        Membar,
    EndOp0,
    BeginOp1,
        NullCheck,
        Return,
        Lea,
        TableSwitch,
        Prefetchr,
        Prefetchw,
        Lsb,
        Msb,
        MonitorAddress,
    EndOp1,
    BeginOp2,
        Cmpl2i,
        Ucmpfd2i,
        Cmpfd2i,
    EndOp2,
    NativeCall,
    DirectCall,
    IndirectCall,
    Cas,
    Xir;
    // @formatter:on
    // Checkstyle: resume


    @Override
    public void emitCode(LIRAssembler lasm, LIRInstruction op) {
        LegacyOpcode code = (LegacyOpcode) op.code;
        switch (code) {
            case TableSwitch:
                lasm.emitTableSwitch((LIRTableSwitch) op);
                break;
            case Xir:
                lasm.emitXir((LIRXirInstruction) op);
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

            case Prefetchr:
                lasm.emitReadPrefetch(op.operand(0));
                break;
            case Prefetchw:
                lasm.emitReadPrefetch(op.operand(0));
                break;
            case Return:
                lasm.emitReturn(op.operand(0));
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

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                lasm.emitCompare2Int(op.code, op.operand(0), op.operand(1), op.result());
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }
}
