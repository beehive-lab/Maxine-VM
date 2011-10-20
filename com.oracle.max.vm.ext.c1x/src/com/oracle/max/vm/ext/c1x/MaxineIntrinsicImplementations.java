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
package com.oracle.max.vm.ext.c1x;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.intrinsics.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ir.Infopoint.Op;
import com.sun.c1x.ir.Value.Flag;
import com.sun.c1x.lir.*;
import com.sun.c1x.value.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.runtime.*;

public class MaxineIntrinsicImplementations {

    public static class BitIntrinsic implements C1XIntrinsicImpl {
        public final LIROpcode opcode;

        public BitIntrinsic(LIROpcode opcode) {
            this.opcode = opcode;
        }

        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 1;
            return b.append(new SignificantBitOp(args[0], opcode));
        }
    }

    public static class UnsafeCastIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            b.compilation.setNotTypesafe();
            RiSignature signature = target.signature();
            int argCount = signature.argumentCount(false);
            RiType accessingClass = b.scope().method.holder();
            RiType fromType;
            RiType toType = signature.returnType(accessingClass);
            assert args.length == 1 || (args.length == 2 && MutableFrameState.isTwoSlot(args[0].kind)) : "method with @UNSAFE_CAST must have exactly 1 argument";
            if (argCount == 1) {
                fromType = signature.argumentTypeAt(0, accessingClass);
            } else {
                assert argCount == 0 : "method with @UNSAFE_CAST must have exactly 1 argument";
                fromType = target.holder();
            }
            CiKind from = fromType.kind(true).stackKind();
            CiKind to = toType.kind(true).stackKind();
            if (b.compilation.target.sizeInBytes(from) != b.compilation.target.sizeInBytes(from) || from == CiKind.Float || from == CiKind.Double || to == CiKind.Float || to == CiKind.Double) {
                throw new CiBailout("Unsupported unsafe cast from " + fromType + " to " + toType);
            }
            return b.append(new UnsafeCast(toType, args[0], from == to));
        }
    }

    public static class PointerReadIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 2 || args.length == 3;
            Value pointer = args[0];
            Value displacement = args.length == 3 ? args[1] : null;
            Value offsetOrIndex = offsetOrIndex(b, args.length == 3 ? args[2] : args[1]);
            return b.append(new LoadPointer(target.signature().returnType(null), pointer, displacement, offsetOrIndex, stateBefore, false));
        }
    }

    public static class PointerWriteIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            // Last parameter can be a double word, in which case args ends with a null slot that must be ignored.
            int numArgs = args[args.length - 1] == null ? args.length - 1 : args.length;

            assert numArgs == 3 || numArgs == 4;
            Value pointer = args[0];
            Value displacement = numArgs == 4 ? args[1] : null;
            Value offsetOrIndex = offsetOrIndex(b, numArgs == 4 ? args[2] : args[1]);
            Value value = args[numArgs - 1];

            RiType dataType = target.signature().argumentTypeAt(target.signature().argumentCount(false) - 1, null);
            b.append(new StorePointer(dataType, pointer, displacement, offsetOrIndex, value, stateBefore, false));
            return null;
        }
    }

    public static class PointerCompareAndSwapIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 4 || args.length == 6;
            Value pointer = args[0];
            Value offset = offsetOrIndex(b, args[1]);
            Value expectedValue = args[2];
            Value newValue = args[args.length == 6 ? 4 : 3];
            return b.append(new CompareAndSwap(target.signature().returnType(null), pointer, offset, expectedValue, newValue, stateBefore, false));
        }
    }


    /**
     * Processes the value producing the scaled-index or the byte offset for a pointer operation.
     * If compiling for a 64-bit platform and the value is an {@link CiKind#Int} parameter,
     * then a conversion is inserted to sign extend the int to a word.
     *
     * This is required as the value is used as a 64-bit value and so the high 32 bits
     * need to be correct.
     */
    private static Value offsetOrIndex(GraphBuilder b, Value offsetOrIndex) {
        if (offsetOrIndex.kind == CiKind.Int && b.compilation.target.arch.is64bit()) {
            return b.append(new Convert(Convert.Op.I2L, offsetOrIndex, CiKind.Long));
        }
        return offsetOrIndex;
    }


    public static class ReadRegisterIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 1;
            int registerId = intConstant(args[0]);

            CiRegister register = b.compilation.registerConfig.getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported READREG operand " + registerId);
            }
            LoadRegister load = new LoadRegister(target.signature().returnKind(false), register, target.signature().returnType(null));
            RiRegisterAttributes regAttr = b.compilation.registerConfig.getAttributesMap()[register.number];
            if (regAttr.isNonZero) {
                load.setFlag(Flag.NonNull);
            }
            return b.append(load);
        }
    }

    public static class WriteRegisterIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 2;
            int registerId = intConstant(args[0]);
            Value value = args[1];

            CiRegister register = b.compilation.registerConfig.getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported READREG operand " + registerId);
            }
            b.append(new StoreRegister(register, value));
            return null;
        }
    }

    private static int intConstant(Value value) {
        if (!value.isConstant() || value.kind != CiKind.Int) {
            throw new CiBailout("instrinc parameter must be compile time integer constant");
        }
        return value.asConstant().asInt();
    }

    public static class InfopointIntrinsic implements C1XIntrinsicImpl {
        private Op op;

        public InfopointIntrinsic(Infopoint.Op op) {
            this.op = op;
        }

        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 0;
            assert op != Infopoint.Op.SAFEPOINT_POLL || !b.scopeData.noSafepointPolls() : "cannot place safepoint poll in uninterruptible code scope";
            return b.append(new Infopoint(op, stateBefore));
        }
    }


    public static class PauseIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            b.append(new Pause());
            return null;
        }
    }

    public static class BreakointTrapIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            b.append(new BreakpointTrap());
            return null;
        }
    }

    public static class StackHandleIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            return b.append(new StackHandle(args[0], target.signature().returnType(null)));
        }
    }

    public static class StackAllocateIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            return b.append(new StackAllocate(args[0], target.signature().returnType(null)));
        }
    }

    public static class IfLatchBitReadIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 2;
            int offset = intConstant(args[0]);
            int bit = intConstant(args[1]);

            CiRegister register = b.compilation.registerConfig.getRegisterForRole(VMRegister.LATCH);
            if (register == null) {
                throw new CiBailout("Unsupported IFLATCHBITREAD operand " + VMRegister.LATCH);
            }

            // The intrinsic implicitly consumes also the following IFEQ or IFNE bytecode.  This is very badly designed,
            // but for now keep it and make a call into GraphBuilder which has access to the bytecode stream.
            b.genIfLatchReadBit(register, offset, bit);
            return null;
        }
    }

    public static class CompareBytecodeIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            int opcode = intConstant(args[0]);
            assert opcode == Bytecodes.LCMP || opcode == Bytecodes.FCMPL || opcode == Bytecodes.FCMPG || opcode == Bytecodes.DCMPL || opcode == Bytecodes.DCMPG;

            assert args.length == 3 || args.length == 5;
            Value x = args[1];
            Value y = args.length == 5 ? args[3] : args[2];
            return b.append(new CompareOp(opcode, CiKind.Int, x, y));
        }
    }


    public static void initialize(IntrinsicImpl.Registry registry) {
        registry.add(LSB, new BitIntrinsic(LIROpcode.Lsb));
        registry.add(MSB, new BitIntrinsic(LIROpcode.Msb));

        registry.add(UNSAFE_CAST, new UnsafeCastIntrinsic());

        registry.add(READREG, new ReadRegisterIntrinsic());
        registry.add(WRITEREG, new WriteRegisterIntrinsic());
        registry.add(IFLATCHBITREAD, new IfLatchBitReadIntrinsic());

        registry.add(PREAD, new PointerReadIntrinsic());
        registry.add(PWRITE, new PointerWriteIntrinsic());
        registry.add(PCMPSWP, new PointerCompareAndSwapIntrinsic());

        registry.add(SAFEPOINT_POLL, new InfopointIntrinsic(Infopoint.Op.SAFEPOINT_POLL));
        registry.add(INFO, new InfopointIntrinsic(Infopoint.Op.INFO));
        registry.add(HERE, new InfopointIntrinsic(Infopoint.Op.HERE));
        registry.add(UNCOMMON_TRAP, new InfopointIntrinsic(Infopoint.Op.UNCOMMON_TRAP));

        registry.add(PAUSE, new PauseIntrinsic());
        registry.add(BREAKPOINT_TRAP, new BreakointTrapIntrinsic());
        registry.add(STACKHANDLE, new StackHandleIntrinsic());
        registry.add(ALLOCA, new StackAllocateIntrinsic());

        registry.add(CMP_BYTECODE, new CompareBytecodeIntrinsic());
    }
}
