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
import com.oracle.max.graal.compiler.asm.*;
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

    public static class MoveOp implements StandardOp.MoveOpcode<AMD64MacroAssembler, LIRInstruction>, LIROpcode.FirstOperandRegisterHint {
        @Override
        public LIRInstruction create(CiValue result, CiValue input) {
            assert !result.isAddress() && !input.isAddress();
            assert result.kind == result.kind.stackKind();
            return new LIRInstruction(this, result, null, input);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            move(tasm, op.result(), op.input(0));
        }
    }

    protected static class LoadOp implements LIROpcode<AMD64MacroAssembler, LIRAddressInstruction> {
        public LIRInstruction create(CiVariable result, CiValue addrBase, CiValue addrIndex, CiAddress.Scale addrScale, int addrDisplacement, CiKind kind, LIRDebugInfo info) {
            return new LIRAddressInstruction(this, result, info, kind, addrScale, addrDisplacement, addrBase, addrIndex);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRAddressInstruction op) {
            load(tasm, op.result(), op.createAddress(0, 1), op.kind, op.info);
        }
    }

    protected static class StoreOp implements LIROpcode<AMD64MacroAssembler, LIRAddressInstruction> {
        public LIRInstruction create(CiValue addrBase, CiValue addrIndex, CiAddress.Scale addrScale, int addrDisplacement, CiValue input, CiKind kind, LIRDebugInfo info) {
            // Since the registers used by storeAddr are actual input operands, storeAddr is registered as an input.
            return new LIRAddressInstruction(this, CiValue.IllegalValue, info, kind, addrScale, addrDisplacement, addrBase, addrIndex, input);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRAddressInstruction op) {
            store(tasm, op.createAddress(0, 1), op.input(2), op.kind, op.info);
        }
    }

    protected static class LeaOp implements LIROpcode<AMD64MacroAssembler, LIRAddressInstruction> {
        public LIRInstruction create(CiVariable result, CiValue addrBase, CiValue addrIndex, CiAddress.Scale addrScale, int addrDisplacement) {
            return new LIRAddressInstruction(this, result, null, CiKind.Illegal, addrScale, addrDisplacement, addrBase, addrIndex);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRAddressInstruction op) {
            tasm.masm.leaq(tasm.asLongReg(op.result()), op.createAddress(0, 1));
        }
    }

    protected static class MembarOp implements LIROpcode<AMD64MacroAssembler, LIRInstruction> {
        public LIRInstruction create(int barriers) {
            return new LIRInstruction(this, CiValue.IllegalValue, null, CiConstant.forInt(barriers));
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            tasm.masm.membar(tasm.asIntConst(op.input(0)));
        }
    }

    protected static class NullCheckOp implements StandardOp.NullCheckOpcode<AMD64MacroAssembler, LIRInstruction> {
        @Override
        public LIRInstruction create(CiVariable input, LIRDebugInfo info) {
            return new LIRInstruction(this, CiValue.IllegalValue, info, input);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            tasm.recordImplicitException(tasm.masm.codeBuffer.position(), op.info);
            tasm.masm.nullCheck(tasm.asRegister(op.input(0)));
        }
    }

    protected static class CompareAndSwapOp implements LIROpcode<AMD64MacroAssembler, LIRAddressInstruction> {
        public LIRInstruction create(CiRegisterValue result, CiValue addrBase, CiValue addrIndex, CiAddress.Scale addrScale, int addrDisplacement, CiRegisterValue cmpValue, CiVariable newValue) {
            return new LIRAddressInstruction(this, result, null, CiKind.Illegal, addrScale, addrDisplacement, addrBase, addrIndex, cmpValue, newValue);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRAddressInstruction op) {
            CiAddress address = op.createAddress(0, 1);
            assert tasm.asRegister(op.input(2)) == AMD64.rax;
            CiRegister newValue = tasm.asRegister(op.input(3));
            assert tasm.asRegister(op.result()) == AMD64.rax;

            if (tasm.target.isMP) {
                tasm.masm.lock();
            }
            switch (op.input(2).kind) {
                case Int:    tasm.masm.cmpxchgl(newValue, address); break;
                case Long:
                case Object: tasm.masm.cmpxchgq(newValue, address); break;
                default:     throw Util.shouldNotReachHere();
            }
        }
    }


    protected static void move(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiValue input) {
        if (input.isRegister()) {
            if (result.isRegister()) {
                reg2reg(tasm, result, input);
            } else if (result.isStackSlot()) {
                reg2stack(tasm, result, input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (input.isStackSlot()) {
            if (result.isRegister()) {
                stack2reg(tasm, result, input);
            } else if (result.isStackSlot()) {
                stack2stack(tasm, result, input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (input.isConstant()) {
            if (result.isRegister()) {
                const2reg(tasm, result, (CiConstant) input);
            } else if (result.isStackSlot()) {
                const2stack(tasm, result, (CiConstant) input);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private static void reg2reg(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiValue input) {
        if (input.equals(result)) {
            return;
        }
        switch (result.kind) {
            case Jsr:
            case Int:    tasm.masm.movl(tasm.asRegister(result),      tasm.asRegister(input)); break;
            case Long:   tasm.masm.movq(tasm.asRegister(result),     tasm.asRegister(input)); break;
            case Float:  tasm.masm.movflt(tasm.asFloatReg(result),  tasm.asFloatReg(input)); break;
            case Double: tasm.masm.movdbl(tasm.asDoubleReg(result), tasm.asDoubleReg(input)); break;
            case Object: tasm.masm.movq(tasm.asRegister(result),   tasm.asRegister(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void reg2stack(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:    tasm.masm.movl(tasm.asAddress(result),   tasm.asRegister(input)); break;
            case Long:   tasm.masm.movq(tasm.asAddress(result),   tasm.asRegister(input)); break;
            case Float:  tasm.masm.movflt(tasm.asAddress(result), tasm.asFloatReg(input)); break;
            case Double: tasm.masm.movsd(tasm.asAddress(result),  tasm.asDoubleReg(input)); break;
            case Object: tasm.masm.movq(tasm.asAddress(result),   tasm.asRegister(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void stack2reg(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:    tasm.masm.movl(tasm.asRegister(result),      tasm.asAddress(input)); break;
            case Long:   tasm.masm.movq(tasm.asRegister(result),     tasm.asAddress(input)); break;
            case Float:  tasm.masm.movflt(tasm.asFloatReg(result),  tasm.asAddress(input)); break;
            case Double: tasm.masm.movdbl(tasm.asDoubleReg(result), tasm.asAddress(input)); break;
            case Object: tasm.masm.movq(tasm.asRegister(result),   tasm.asAddress(input)); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    private static void stack2stack(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiValue input) {
        switch (result.kind) {
            case Jsr:
            case Int:
            case Float:
                tasm.masm.pushl(tasm.asAddress(input));
                tasm.masm.popl(tasm.asAddress(result));
                break;
            case Long:
            case Double:
            case Object:
                tasm.masm.pushq(tasm.asAddress(input));
                tasm.masm.popq(tasm.asAddress(result));
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private static void const2reg(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiConstant c) {
        switch (result.kind) {
            case Jsr:
            case Int:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                tasm.masm.movl(tasm.asRegister(result), tasm.asIntConst(c));
                break;
            case Long:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                tasm.masm.movq(tasm.asRegister(result), c.asLong());
                break;
            case Float:
                // This is *not* the same as 'constant == 0.0f' in the case where constant is -0.0f
                if (Float.floatToRawIntBits(c.asFloat()) == Float.floatToRawIntBits(0.0f)) {
                    tasm.masm.xorps(tasm.asFloatReg(result), tasm.asFloatReg(result));
                } else {
                    tasm.masm.movflt(tasm.asFloatReg(result), tasm.asFloatConstRef(c));
                }
                break;
            case Double:
                // This is *not* the same as 'constant == 0.0d' in the case where constant is -0.0d
                if (Double.doubleToRawLongBits(c.asDouble()) == Double.doubleToRawLongBits(0.0d)) {
                    tasm.masm.xorpd(tasm.asDoubleReg(result), tasm.asDoubleReg(result));
                } else {
                    tasm.masm.movdbl(tasm.asDoubleReg(result), tasm.asDoubleConstRef(c));
                }
                break;
            case Object:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                if (c.isNull()) {
                    tasm.masm.movq(tasm.asRegister(result), 0x0L);
                } else if (tasm.target.inlineObjects) {
                    tasm.recordDataReferenceInCode(c);
                    tasm.masm.movq(tasm.asRegister(result), 0xDEADDEADDEADDEADL);
                } else {
                    tasm.masm.movq(tasm.asRegister(result), tasm.recordDataReferenceInCode(c));
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private static void const2stack(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiConstant c) {
        switch (result.kind) {
            case Jsr:
            case Int:    tasm.masm.movl(tasm.asAddress(result), c.asInt()); break;
            case Long:   tasm.masm.movlong(tasm.asAddress(result), c.asLong()); break;
            case Float:  tasm.masm.movl(tasm.asAddress(result), floatToRawIntBits(c.asFloat())); break;
            case Double: tasm.masm.movlong(tasm.asAddress(result), doubleToRawLongBits(c.asDouble())); break;
            case Object:
                if (c.isNull()) {
                    tasm.masm.movlong(tasm.asAddress(result), 0L);
                } else {
                    throw Util.shouldNotReachHere("Non-null object constants must be in register");
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }


    protected static void load(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue result, CiAddress loadAddr, CiKind kind, LIRDebugInfo info) {
        if (info != null) {
            tasm.recordImplicitException(tasm.masm.codeBuffer.position(), info);
        }
        switch (kind) {
            case Boolean:
            case Byte:   tasm.masm.movsxb(tasm.asRegister(result),    loadAddr); break;
            case Char:   tasm.masm.movzxl(tasm.asRegister(result),    loadAddr); break;
            case Short:  tasm.masm.movswl(tasm.asRegister(result),    loadAddr); break;
            case Int:    tasm.masm.movslq(tasm.asRegister(result),    loadAddr); break;
            case Long:   tasm.masm.movq(tasm.asRegister(result),     loadAddr); break;
            case Float:  tasm.masm.movflt(tasm.asFloatReg(result),  loadAddr); break;
            case Double: tasm.masm.movdbl(tasm.asDoubleReg(result), loadAddr); break;
            case Object: tasm.masm.movq(tasm.asRegister(result),   loadAddr); break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    protected static void store(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiAddress storeAddr, CiValue input, CiKind kind, LIRDebugInfo info) {
        if (info != null) {
            tasm.recordImplicitException(tasm.masm.codeBuffer.position(), info);
        }

        if (input.isRegister()) {
            switch (kind) {
                case Boolean:
                case Byte:   tasm.masm.movb(storeAddr,   tasm.asRegister(input)); break;
                case Char:
                case Short:  tasm.masm.movw(storeAddr,   tasm.asRegister(input)); break;
                case Int:    tasm.masm.movl(storeAddr,   tasm.asRegister(input)); break;
                case Long:   tasm.masm.movq(storeAddr,   tasm.asRegister(input)); break;
                case Float:  tasm.masm.movflt(storeAddr, tasm.asFloatReg(input)); break;
                case Double: tasm.masm.movsd(storeAddr,  tasm.asDoubleReg(input)); break;
                case Object: tasm.masm.movq(storeAddr,   tasm.asRegister(input)); break;
                default:     throw Util.shouldNotReachHere();
            }
        } else if (input.isConstant()) {
            CiConstant c = (CiConstant) input;
            switch (kind) {
                case Boolean:
                case Byte:   tasm.masm.movb(storeAddr, c.asInt() & 0xFF); break;
                case Char:
                case Short:  tasm.masm.movw(storeAddr, c.asInt() & 0xFFFF); break;
                case Jsr:
                case Int:    tasm.masm.movl(storeAddr, c.asInt()); break;
                case Long:
                    if (Util.isInt(c.asLong())) {
                        tasm.masm.movslq(storeAddr, (int) c.asLong());
                    } else {
                        throw Util.shouldNotReachHere("Cannot store 64-bit constants to memory");
                    }
                    break;
                case Float:  tasm.masm.movl(storeAddr, floatToRawIntBits(c.asFloat())); break;
                case Double: throw Util.shouldNotReachHere("Cannot store 64-bit constants to memory");
                case Object:
                    if (c.isNull()) {
                        tasm.masm.movptr(storeAddr, 0);
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
