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

import static java.lang.Double.*;
import static java.lang.Float.*;

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

public class AMD64MoveOp {
    public static final MoveOp MOVE = new MoveOp();
    public static final LoadOp LOAD = new LoadOp();
    public static final StoreOp STORE = new StoreOp();
    public static final LeaOp LEA = new LeaOp();
    public static final MembarOp MEMBAR = new MembarOp();
    public static final NullCheckOp NULL_CHECK = new NullCheckOp();
    public static final CompareAndSwapOp CAS = new CompareAndSwapOp();

    public static class MoveOp implements StandardOp.MoveOpcode<AMD64LIRAssembler, LIRInstruction>, LIROpcode.FirstOperandRegisterHint {
        @Override
        public LIRInstruction create(CiValue result, CiValue input) {
            assert !result.isAddress() && !input.isAddress();
            assert result.kind == result.kind.stackKind();
            return new LIRInstruction(this, result, null, input);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            move(lasm, op.result(), op.operand(0));
        }
    }

    protected static class LoadOp implements LIROpcode<AMD64LIRAssembler, LIRKindInstruction> {
        public LIRInstruction create(CiVariable result, CiAddress loadAddr, CiKind kind, LIRDebugInfo info) {
            return new LIRKindInstruction(this, result, info, kind, loadAddr);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRKindInstruction op) {
            load(lasm, op.result(), (CiAddress) op.operand(0), op.kind, op.info);
        }
    }

    protected static class StoreOp implements LIROpcode<AMD64LIRAssembler, LIRKindInstruction> {
        public LIRInstruction create(CiAddress storeAddr, CiValue input, CiKind kind, LIRDebugInfo info) {
            // Since the registers used by storeAddr are actual input operands, storeAddr is registered as an input.
            return new LIRKindInstruction(this, CiValue.IllegalValue, info, kind, input, storeAddr);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRKindInstruction op) {
            store(lasm, (CiAddress) op.operand(1), op.operand(0), op.kind, op.info);
        }
    }

    protected static class LeaOp implements LIROpcode<AMD64LIRAssembler, LIRInstruction> {
        public LIRInstruction create(CiVariable result, CiAddress loadAddr) {
            return new LIRInstruction(this, result, null, loadAddr);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            lasm.masm.leaq(lasm.asLongReg(op.result()), lasm.asAddress(op.operand(0)));
        }
    }

    protected static class MembarOp implements LIROpcode<AMD64LIRAssembler, LIRInstruction> {
        public LIRInstruction create(int barriers) {
            return new LIRInstruction(this, CiValue.IllegalValue, null, CiConstant.forInt(barriers));
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            lasm.masm.membar(lasm.asIntConst(op.operand(0)));
        }
    }

    protected static class NullCheckOp implements StandardOp.NullCheckOpcode<AMD64LIRAssembler, LIRInstruction> {
        @Override
        public LIRInstruction create(CiVariable input, LIRDebugInfo info) {
            return new LIRInstruction(this, CiValue.IllegalValue, info, input);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            lasm.tasm.recordImplicitException(lasm.masm.codeBuffer.position(), op.info);
            lasm.masm.nullCheck(lasm.asRegister(op.operand(0)));
        }
    }

    protected static class CompareAndSwapOp implements LIROpcode<AMD64LIRAssembler, LIRInstruction> {
        public LIRInstruction create(CiRegisterValue result, CiAddress address, CiRegisterValue cmpValue, CiVariable newValue) {
            return new LIRInstruction(this, result, null, address, cmpValue, newValue);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            CiAddress address = lasm.asAddress(op.operand(0));
            assert lasm.asRegister(op.operand(1)) == AMD64.rax;
            CiRegister newValue = lasm.asRegister(op.operand(2));
            assert lasm.asRegister(op.result()) == AMD64.rax;

            if (lasm.target.isMP) {
                lasm.masm.lock();
            }
            switch (op.operand(1).kind) {
                case Int:    lasm.masm.cmpxchgl(newValue, address); break;
                case Long:
                case Object: lasm.masm.cmpxchgq(newValue, address); break;
                default:     throw Util.shouldNotReachHere();
            }
        }
    }


    protected static void move(AMD64LIRAssembler lasm, CiValue result, CiValue input) {
        if (input.isRegister()) {
            if (result.isRegister()) {
                reg2reg(lasm, result, input);
            } else if (result.isStackSlot()) {
                reg2stack(lasm, result, input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (input.isStackSlot()) {
            if (result.isRegister()) {
                stack2reg(lasm, result, input);
            } else if (result.isStackSlot()) {
                stack2stack(lasm, result, input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (input.isConstant()) {
            if (result.isRegister()) {
                const2reg(lasm, result, (CiConstant) input);
            } else if (result.isStackSlot()) {
                const2stack(lasm, result, (CiConstant) input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private static void reg2reg(AMD64LIRAssembler lasm, CiValue result, CiValue input) {
        if (input.equals(result)) {
            return;
        }
        switch (result.kind) {
            case Jsr:
            case Int:    lasm.masm.movl(lasm.asRegister(result),      lasm.asRegister(input)); break;
            case Long:   lasm.masm.movq(lasm.asRegister(result),     lasm.asRegister(input)); break;
            case Float:  lasm.masm.movflt(lasm.asFloatReg(result),  lasm.asFloatReg(input)); break;
            case Double: lasm.masm.movdbl(lasm.asDoubleReg(result), lasm.asDoubleReg(input)); break;
            case Object: lasm.masm.movq(lasm.asRegister(result),   lasm.asRegister(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void reg2stack(AMD64LIRAssembler lasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:    lasm.masm.movl(lasm.asAddress(result),   lasm.asRegister(input)); break;
            case Long:   lasm.masm.movq(lasm.asAddress(result),   lasm.asRegister(input)); break;
            case Float:  lasm.masm.movflt(lasm.asAddress(result), lasm.asFloatReg(input)); break;
            case Double: lasm.masm.movsd(lasm.asAddress(result),  lasm.asDoubleReg(input)); break;
            case Object: lasm.masm.movq(lasm.asAddress(result),   lasm.asRegister(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void stack2reg(AMD64LIRAssembler lasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:    lasm.masm.movl(lasm.asRegister(result),      lasm.asAddress(input)); break;
            case Long:   lasm.masm.movq(lasm.asRegister(result),     lasm.asAddress(input)); break;
            case Float:  lasm.masm.movflt(lasm.asFloatReg(result),  lasm.asAddress(input)); break;
            case Double: lasm.masm.movdbl(lasm.asDoubleReg(result), lasm.asAddress(input)); break;
            case Object: lasm.masm.movq(lasm.asRegister(result),   lasm.asAddress(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void stack2stack(AMD64LIRAssembler lasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:
            case Float:
                lasm.masm.pushl(lasm.asAddress(input));
                lasm.masm.popl(lasm.asAddress(result));
                break;
            case Long:
            case Double:
            case Object:
                lasm.masm.pushq(lasm.asAddress(input));
                lasm.masm.popq(lasm.asAddress(result));
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private static void const2reg(AMD64LIRAssembler lasm, CiValue result, CiConstant c) {
        switch (result.kind) {
            case Jsr:
            case Int:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                lasm.masm.movl(lasm.asRegister(result), lasm.asIntConst(c));
                break;
            case Long:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                lasm.masm.movq(lasm.asRegister(result), c.asLong());
                break;
            case Float:
                // This is *not* the same as 'constant == 0.0f' in the case where constant is -0.0f
                if (Float.floatToRawIntBits(c.asFloat()) == Float.floatToRawIntBits(0.0f)) {
                    lasm.masm.xorps(lasm.asFloatReg(result), lasm.asFloatReg(result));
                } else {
                    lasm.masm.movflt(lasm.asFloatReg(result), lasm.asFloatConst(c));
                }
                break;
            case Double:
                // This is *not* the same as 'constant == 0.0d' in the case where constant is -0.0d
                if (Double.doubleToRawLongBits(c.asDouble()) == Double.doubleToRawLongBits(0.0d)) {
                    lasm.masm.xorpd(lasm.asDoubleReg(result), lasm.asDoubleReg(result));
                } else {
                    lasm.masm.movdbl(lasm.asDoubleReg(result), lasm.asDoubleConst(c));
                }
                break;
            case Object:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                if (c.isNull()) {
                    lasm.masm.movq(lasm.asRegister(result), 0x0L);
                } else if (lasm.target.inlineObjects) {
                    lasm.tasm.recordDataReferenceInCode(c);
                    lasm.masm.movq(lasm.asRegister(result), 0xDEADDEADDEADDEADL);
                } else {
                    lasm.masm.movq(lasm.asRegister(result), lasm.tasm.recordDataReferenceInCode(c));
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private static void const2stack(AMD64LIRAssembler lasm, CiValue result, CiConstant c) {
        switch (result.kind) {
            case Jsr:
            case Int:    lasm.masm.movl(lasm.asAddress(result), c.asInt()); break;
            case Long:   lasm.masm.movlong(lasm.asAddress(result), c.asLong()); break;
            case Float:  lasm.masm.movl(lasm.asAddress(result), floatToRawIntBits(c.asFloat())); break;
            case Double: lasm.masm.movlong(lasm.asAddress(result), doubleToRawLongBits(c.asDouble())); break;
            case Object:
                if (c.isNull()) {
                    lasm.masm.movlong(lasm.asAddress(result), 0L);
                } else {
                    throw Util.shouldNotReachHere("Non-null object constants must be in register");
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }


    protected static void load(AMD64LIRAssembler lasm, CiValue result, CiAddress loadAddr, CiKind kind, LIRDebugInfo info) {
        if (info != null) {
            lasm.tasm.recordImplicitException(lasm.masm.codeBuffer.position(), info);
        }
        switch (kind) {
            case Boolean:
            case Byte:   lasm.masm.movsxb(lasm.asRegister(result),    loadAddr); break;
            case Char:   lasm.masm.movzxl(lasm.asRegister(result),    loadAddr); break;
            case Short:  lasm.masm.movswl(lasm.asRegister(result),    loadAddr); break;
            case Int:    lasm.masm.movslq(lasm.asRegister(result),    loadAddr); break;
            case Long:   lasm.masm.movq(lasm.asRegister(result),     loadAddr); break;
            case Float:  lasm.masm.movflt(lasm.asFloatReg(result),  loadAddr); break;
            case Double: lasm.masm.movdbl(lasm.asDoubleReg(result), loadAddr); break;
            case Object: lasm.masm.movq(lasm.asRegister(result),   loadAddr); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    protected static void store(AMD64LIRAssembler lasm, CiAddress storeAddr, CiValue input, CiKind kind, LIRDebugInfo info) {
        if (info != null) {
            lasm.tasm.recordImplicitException(lasm.masm.codeBuffer.position(), info);
        }

        if (input.isRegister()) {
            switch (kind) {
                case Boolean:
                case Byte:   lasm.masm.movb(storeAddr,   lasm.asRegister(input)); break;
                case Char:
                case Short:  lasm.masm.movw(storeAddr,   lasm.asRegister(input)); break;
                case Int:    lasm.masm.movl(storeAddr,   lasm.asRegister(input)); break;
                case Long:   lasm.masm.movq(storeAddr,   lasm.asRegister(input)); break;
                case Float:  lasm.masm.movflt(storeAddr, lasm.asFloatReg(input)); break;
                case Double: lasm.masm.movsd(storeAddr,  lasm.asDoubleReg(input)); break;
                case Object: lasm.masm.movq(storeAddr,   lasm.asRegister(input)); break;
                default:     throw Util.shouldNotReachHere();
            }
        } else if (input.isConstant()) {
            CiConstant c = (CiConstant) input;
            switch (kind) {
                case Boolean:
                case Byte:   lasm.masm.movb(storeAddr, c.asInt() & 0xFF); break;
                case Char:
                case Short:  lasm.masm.movw(storeAddr, c.asInt() & 0xFFFF); break;
                case Jsr:
                case Int:    lasm.masm.movl(storeAddr, c.asInt()); break;
                case Long:
                    if (Util.isInt(c.asLong())) {
                        lasm.masm.movslq(storeAddr, (int) c.asLong());
                    } else {
                        throw Util.shouldNotReachHere("Cannot store 64-bit constants to memory");
                    }
                    break;
                case Float:  lasm.masm.movl(storeAddr, floatToRawIntBits(c.asFloat())); break;
                case Double: throw Util.shouldNotReachHere("Cannot store 64-bit constants to memory");
                case Object:
                    if (c.isNull()) {
                        lasm.masm.movptr(storeAddr, 0);
                    } else {
                        throw Util.shouldNotReachHere("Cannot store 64-bit constants to memory");
                    }
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }
}
