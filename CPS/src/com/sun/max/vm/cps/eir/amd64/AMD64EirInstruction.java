/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.asm.Assembler.Directives;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.Purpose;
import com.sun.max.vm.cps.eir.amd64.AMD64EirTargetEmitter.StackAddress;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Holds most AMD64 EIR instruction definitions in alphabetical order.
 *
 * The EIR instructions not listed here are either synthetic (AMD64EirPrologue)
 * or generalized and thus relatively complex (AMD64EirLoad, AND64EirStore).
 *
 * @author Bernd Mathiske
 */
public interface AMD64EirInstruction {

    public static class ADD_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public ADD_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().addl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().addl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_add(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().add(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().add(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().add_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADD_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public ADD_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().addq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().addq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_add(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().add(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().add(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().addq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().addq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().addq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().addq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().add_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADDSD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public ADDSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().addsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_addsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().addsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().addsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADDSS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public ADDSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().addss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_addss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().addss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().addss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class AND_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public AND_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().andl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().andl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_and(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().and(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().and(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().and_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class AND_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public AND_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().andq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().andq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_and(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().and(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().and(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().andq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().andq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().andq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().andq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().and_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CALL extends EirCall<EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {
        public static final int DIRECT_METHOD_CALL_INSTRUCTION_LENGTH = 5;

        public static boolean isPatchableCallSite(Address callSite) {
            return callSite.roundedDownBy(WordWidth.BITS_64.numberOfBytes).equals(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH).roundedDownBy(WordWidth.BITS_64.numberOfBytes));

        }
        public CALL(EirBlock block, EirABI abi, EirValue result, EirLocation resultLocation,
                    EirValue function, EirValue[] arguments, EirLocation[] argumentLocations,
                    boolean isNativeFunctionCall, EirMethodGeneration methodGeneration) {
            super(block, abi, result, resultLocation, function, M_G_L_S, arguments, argumentLocations, isNativeFunctionCall, methodGeneration);
        }

        // Direct calls currently are always 5 bytes long: 1 byte for the call opcode,
        // and 4 bytes for a displacement to the method address.
        // We arrange for direct call instructions to be laid out such that they always fit within a 8-byte aligned
        // block of code. Since code always start at an 8-byte aligned address, we only need to align the position in the code buffer.
        // We use a variant of the align directive that pads to next 8-byte align position in the code emitter's buffer with nops only if
        // the instruction about to be emitted would cross an 8 byte-alignment.
        // Note that we don't enforce alignment constraints on templates, since direct calls in these are guaranteed to target
        // compiled runtime functions, and therefore, be free of runtime patches.
        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final EirLocation location = function().location();
            switch (location.category()) {
                case METHOD: {
                    if (!(MaxineVM.isHosted() && emitter.templatesOnly())) {
                        // Only align if not for templates.
                        boolean ok = emitter.assembler().directives().align(WordWidth.BITS_64.numberOfBytes, DIRECT_METHOD_CALL_INSTRUCTION_LENGTH);
                        assert ok;
                    }
                    emitter.addDirectCall(this);
                    final int placeHolderBeforeLinking = -1;
                    emitter.assembler().call(placeHolderBeforeLinking);
                    break;
                }
                case INTEGER_REGISTER: {
                    emitter.addIndirectCall(this);
                    final AMD64EirRegister.General operandRegister = (AMD64EirRegister.General) location;
                    emitter.assembler().call(operandRegister.as64());
                    break;
                }
                case LITERAL: {
                    emitter.addIndirectCall(this);
                    emitter.assembler().rip_call(location.asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    emitter.addIndirectCall(this);
                    final StackAddress operand = emitter.stackAddress(location.asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().call(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().call(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
            if (isNativeFunctionCall) {
                // Sign extend or zero the upper bits of a return value smaller than an int to
                // preserve the invariant that all such values are represented by an int
                // in the VM. We cannot rely on the native C compiler doing this for us.
                Kind resultKind = javaFrameDescriptor().classMethodActor.resultKind();
                switch (resultKind.asEnum) {
                    case BOOLEAN:
                    case BYTE: {
                        assert result().location() == AMD64EirRegister.General.RAX;
                        emitter.assembler().movsxb(AMD64GeneralRegister32.EAX, AMD64GeneralRegister8.AL);
                        break;
                    }
                    case SHORT: {
                        assert result().location() == AMD64EirRegister.General.RAX;
                        emitter.assembler().movsxw(AMD64GeneralRegister32.EAX, AMD64GeneralRegister16.AX);
                        break;
                    }
                    case CHAR: {
                        assert result().location() == AMD64EirRegister.General.RAX;
                        emitter.assembler().movzxw(AMD64GeneralRegister32.EAX, AMD64GeneralRegister16.AX);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    /**
     * A <i>runtime</i> call is a call that occurs within a JIT template to a runtime service
     * (e.g. allocating memory, resolving a constant, etc.). It requires special treatment because
     * it must save the frame pointer and then call optimized code, and then restore the frame pointer.
     */
    public static class RUNTIME_CALL extends CALL {
        protected final AMD64GeneralRegister64 framePointerRegister;
        protected final AMD64GeneralRegister64 stackPointerRegister;

        public RUNTIME_CALL(EirBlock block, EirABI abi, EirValue result, EirLocation resultLocation,
                        EirValue function, EirValue[] arguments, EirLocation[] argumentLocations,
                        EirMethodGeneration methodGeneration) {
            super(block, abi, result, resultLocation, function, arguments, argumentLocations, false, methodGeneration);
            // TODO: get the correct frame pointer and stack pointer registers from ABI
            // the current ABI returns RSP for both framePointer() and stackPointer()
            framePointerRegister = AMD64GeneralRegister64.RBP;
            stackPointerRegister = AMD64GeneralRegister64.RSP;
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            // for a runtime call, the frame pointer (RBP) is caller-saved by pushing it onto the stack
            final int rbpSize = Word.size();
            // because of the word needed for RBP, we may need to align the stack by including a "mini frame"
            final int delta = emitter.abi().targetABI().stackFrameAlignment - rbpSize;
            final AMD64Assembler asm = emitter.assembler();
            if (delta > 0) {
                asm.subq(stackPointerRegister, (byte) delta);
            }
            asm.push(framePointerRegister);
            super.emit(emitter);
            asm.pop(framePointerRegister);
            if (delta > 0) {
                asm.addq(stackPointerRegister, (byte) delta);
            }
        }
    }

    public static class CDQ extends AMD64EirBinaryOperation.Move {

        public CDQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G, source, G, false);
            destinationOperand().setRequiredLocation(AMD64EirRegister.General.RDX);
            sourceOperand().setRequiredLocation(AMD64EirRegister.General.RAX);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().cdq();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVA_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVA_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmova(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVB_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVB_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmovb(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVE_I64 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVE_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmove(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmove(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVE_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmove(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVAE_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVAE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmovae(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmovae(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovae(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovae(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVL_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVL_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmovl(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVBE_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVBE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmovbe(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmovbe(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovbe(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovbe(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVP_I32 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVP_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, true);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmovp(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMP_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public CMP_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmp(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().cmp_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMP_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public CMP_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().cmpq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().cmpq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cmp(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().cmp_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMPSD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        private final AMD64XMMComparison comparison;

        public AMD64XMMComparison comparison() {
            return comparison;
        }

        public CMPSD(EirBlock block, EirValue destination, EirValue source, AMD64XMMComparison comparison) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
            this.comparison = comparison;
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cmpsd(destinationRegister, sourceRegister, comparison);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_cmpsd(destinationRegister, sourceLabel, comparison);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmpsd(destinationRegister, sourceOffset, sourceBasePointer, comparison);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmpsd(destinationRegister, sourceOffset, sourceBasePointer, comparison);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMPSS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        private final AMD64XMMComparison comparison;

        public AMD64XMMComparison comparison() {
            return comparison;
        }

        public CMPSS(EirBlock block, EirValue destination, EirValue source, AMD64XMMComparison comparison) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
            this.comparison = comparison;
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cmpss(destinationRegister, sourceRegister, comparison);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_cmpss(destinationRegister, sourceLabel, comparison);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmpss(destinationRegister, sourceOffset, sourceBasePointer, comparison);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmpss(destinationRegister, sourceOffset, sourceBasePointer, comparison);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class COMISD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public COMISD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().comisd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_comisd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().comisd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().comisd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class COMISS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public COMISS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().comiss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_comiss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().comiss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().comiss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CQO extends AMD64EirBinaryOperation.Move {

        public CQO(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G, source, G, false);
            destinationOperand().setRequiredLocation(AMD64EirRegister.General.RDX);
            sourceOperand().setRequiredLocation(AMD64EirRegister.General.RAX);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().cqo();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTTSD2SI_I32 extends AMD64EirBinaryOperation.Move.XMMToGeneral {

        public CVTTSD2SI_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvttsd2si(destinationRegister.as32(), sourceRegister);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvttsd2si(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvttsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvttsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTTSD2SI_I64 extends AMD64EirBinaryOperation.Move.XMMToGeneral {

        public CVTTSD2SI_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvttsd2si(destinationRegister.as64(), sourceRegister);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvttsd2si(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvttsd2si(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvttsd2si(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSD2SS extends AMD64EirBinaryOperation.Move.XMMToXMM {

        public CVTSD2SS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtsd2ss(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SD_I32 extends AMD64EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SD_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2sdl(destinationRegister, sourceRegister.as32());
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtsi2sdl(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2sdl(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2sdl(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SD_I64 extends AMD64EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SD_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2sdq(destinationRegister, sourceRegister.as64());
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtsi2sdq(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2sdq(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2sdq(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SS_I32 extends AMD64EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SS_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2ssl(destinationRegister, sourceRegister.as32());
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtsi2ssl(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2ssl(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2ssl(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SS_I64 extends AMD64EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SS_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2ssq(destinationRegister, sourceRegister.as64());
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtsi2ssq(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2ssq(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2ssq(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSS2SD extends AMD64EirBinaryOperation.Move.XMMToXMM {

        public CVTSS2SD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvtss2sd(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTTSS2SI_I32 extends AMD64EirBinaryOperation.Move.XMMToGeneral {

        public CVTTSS2SI_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvttss2si(destinationRegister.as32(), sourceRegister);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvttss2si(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvttss2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvttss2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTTSS2SI_I64 extends AMD64EirBinaryOperation.Move.XMMToGeneral {

        public CVTTSS2SI_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().cvttss2si(destinationRegister.as64(), sourceRegister);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_cvttss2si(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvttss2si(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvttss2si(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DEC_I64 extends AMD64EirUnaryOperation {

        public DEC_I64(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().decq(operandGeneralRegister().as64());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIV_I64 extends AMD64EirDivision {

        public DIV_I64(EirBlock block, EirValue rdx, EirValue rax, EirValue divisor) {
            super(block, rdx, rax, divisor);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (divisorLocation().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General divisorRegister = (AMD64EirRegister.General) divisorLocation();
                    emitter.assembler().divq(divisorRegister.as64());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_divq(divisorLocation().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(divisorLocation().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().divq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().divq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIVSD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public DIVSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().divsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_divsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().divsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().divsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIVSS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public DIVSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().divss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_divss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().divss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().divss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IDIV_I32 extends AMD64EirDivision {

        public IDIV_I32(EirBlock block, EirValue rdx, EirValue rax, EirValue divisor) {
            super(block, rdx, rax, divisor);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (divisorLocation().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General divisorRegister = (AMD64EirRegister.General) divisorLocation();
                    emitter.assembler().idivl(divisorRegister.as32());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_idivl(divisorLocation().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(divisorLocation().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().idivl(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().idivl(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IDIV_I64 extends AMD64EirDivision {

        public IDIV_I64(EirBlock block, EirValue rdx, EirValue rax, EirValue divisor) {
            super(block, rdx, rax, divisor);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (divisorLocation().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General divisorRegister = (AMD64EirRegister.General) divisorLocation();
                    emitter.assembler().idivq(divisorRegister.as64());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_idivq(divisorLocation().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(divisorLocation().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().idivq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().idivq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IMUL_I32 extends AMD64EirBinaryOperation.Arithmetic {

        public IMUL_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, G, source, EirOperand.Effect.USE, G_I32_L_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister32 destinationRegister = destinationGeneralRegister().as32();
            switch (sourceOperand().location().category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().imul(destinationRegister, sourceGeneralRegister().as32());
                    break;
                }
                case IMMEDIATE_32: {
                    final Value value = sourceOperand().location().asImmediate().value();
                    if (value.signedEffectiveWidth() == WordWidth.BITS_8) {
                        emitter.assembler().imul(destinationRegister, destinationRegister, value.toByte());
                    } else {
                        emitter.assembler().imul(destinationRegister, destinationRegister, value.toInt());
                    }
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_imul(destinationRegister, sourceOperand().location().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().imul(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().imul(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IMUL_I64 extends AMD64EirBinaryOperation.Arithmetic {

        public IMUL_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, G, source, EirOperand.Effect.USE, G_I32_L_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 destinationRegister = destinationGeneralRegister().as64();
            switch (sourceOperand().location().category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().imul(destinationRegister, sourceGeneralRegister().as64());
                    break;
                }
                case IMMEDIATE_32: {
                    final Value value = sourceOperand().location().asImmediate().value();
                    if (value.signedEffectiveWidth() == WordWidth.BITS_8) {
                        emitter.assembler().imul(destinationRegister, destinationRegister, value.toByte());
                    } else {
                        emitter.assembler().imul(destinationRegister, destinationRegister, value.toInt());
                    }
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_imul(destinationRegister, sourceOperand().location().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().imul(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().imul(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BSR_I64 extends AMD64EirBinaryOperation.Arithmetic {
        public BSR_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, G, source, EirOperand.Effect.USE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 destinationRegister = destinationGeneralRegister().as64();
            // Set destination register to -1. It's value will be unchanged if no bit is set in the source
            emitter.assembler().xor(destinationRegister, destinationRegister);
            emitter.assembler().notq(destinationRegister);

            switch (sourceOperand().location().category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().bsr(destinationRegister, sourceGeneralRegister().as64());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().bsr(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().bsr(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BSF_I64 extends AMD64EirBinaryOperation.Arithmetic {
        public BSF_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, G, source, EirOperand.Effect.USE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 destinationRegister = destinationGeneralRegister().as64();
            // Set destination register to -1. It's value will be unchanged if no bit is set in the source
            emitter.assembler().xor(destinationRegister, destinationRegister);
            emitter.assembler().notq(destinationRegister);
            switch (sourceOperand().location().category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().bsf(destinationRegister, sourceGeneralRegister().as64());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().bsf(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().bsf(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    public static class PAUSE extends AMD64EirOperation {
        public PAUSE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().repe();
            emitter.assembler().nop();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JMP extends AMD64EirLocalControlTransfer implements EirJump {

        public JMP(EirBlock block, EirBlock target) {
            super(block, target);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target) {
            if (!target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jmp(target.asLabel());
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JMP_indirect extends AMD64EirUnaryOperation implements EirControlTransfer {

        public JMP_indirect(EirBlock block, EirValue indirect) {
            super(block, indirect, EirOperand.Effect.USE, G_L_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final EirLocation location = operand().location();
            switch (location.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().jmp(operandGeneralRegister().as64());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_jmp(location.asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(location.asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().jmp(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().jmp(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JA extends AMD64EirConditionalBranch {

        public JA(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jbe(next.asLabel());
            } else {
                emitter.assembler().jnbe(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JAE extends AMD64EirConditionalBranch {

        public JAE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jb(next.asLabel());
            } else {
                emitter.assembler().jnb(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JB extends AMD64EirConditionalBranch {

        public JB(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnb(next.asLabel());
            } else {
                emitter.assembler().jb(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JBE extends AMD64EirConditionalBranch {

        public JBE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnbe(next.asLabel());
            } else {
                emitter.assembler().jbe(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JG extends AMD64EirConditionalBranch {

        public JG(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jle(next.asLabel());
            } else {
                emitter.assembler().jnle(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JGE extends AMD64EirConditionalBranch {

        public JGE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jl(next.asLabel());
            } else {
                emitter.assembler().jnl(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JL extends AMD64EirConditionalBranch {

        public JL(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnl(next.asLabel());
            } else {
                emitter.assembler().jl(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JLE extends AMD64EirConditionalBranch {

        public JLE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnle(next.asLabel());
            } else {
                emitter.assembler().jle(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JNZ extends AMD64EirConditionalBranch {

        public JNZ(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jz(next.asLabel());
            } else {
                emitter.assembler().jnz(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JZ extends AMD64EirConditionalBranch {

        public JZ(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(AMD64EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnz(next.asLabel());
            } else {
                emitter.assembler().jz(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class STACK_ALLOCATE extends AMD64EirUnaryOperation {
        public final int offset;

        public STACK_ALLOCATE(EirBlock block, EirValue operand, int offset) {
            super(block, operand, EirOperand.Effect.DEFINITION, G);
            this.offset = offset;
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 destination = operandGeneralRegister().as64();
            EirStackSlot stackSlot = new EirStackSlot(Purpose.BLOCK, offset);
            final StackAddress source = emitter.stackAddress(stackSlot);
            if (source.isOffsetZero()) {
                emitter.assembler().lea(destination, source.base());
            } else if (source.isOffset8Bit()) {
                emitter.assembler().lea(destination, source.offset8(), source.base());
            } else {
                emitter.assembler().lea(destination, source.offset32(), source.base());
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Assigns the address of a value on the stack to the destination register.
     *
     * @author Doug Simon
     */
    public static class LEA_STACK_ADDRESS extends AMD64EirBinaryOperation {

        /**
         * Creates an instruction that assigns the address of a stack slot to the destination register.
         *
         * @param destination the register in which the address is saved
         * @param source a value that will be allocated to a stack slot
         */
        public LEA_STACK_ADDRESS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.DEFINITION, G, source, EirOperand.Effect.USE, S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 destination = destinationGeneralRegister().as64();
            final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
            if (source.isOffsetZero()) {
                emitter.assembler().lea(destination, source.base());
            } else if (source.isOffset8Bit()) {
                emitter.assembler().lea(destination, source.offset8(), source.base());
            } else {
                emitter.assembler().lea(destination, source.offset32(), source.base());
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class LFENCE extends AMD64EirOperation {
        public LFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().lfence();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MFENCE extends AMD64EirOperation {
        public MFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            //The following is found to be faster than the more natural: emitter.assembler().mfence();
            emitter.assembler().lock();
            emitter.assembler().addl(0, emitter.stackPointer().indirect(), 0);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVD_I32_F32 extends AMD64EirBinaryOperation.Move {

        public MOVD_I32_F32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G_S, source, F, false);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final EirLocation destinationLocation = destinationOperand().location();
            final AMD64XMMRegister sourceRegister = sourceXMMRegister().as();
            switch (destinationLocation.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().movdl(destinationGeneralRegister().as32(), sourceRegister);
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                    if (destination.isOffset8Bit()) {
                        emitter.assembler().movdl(destination.offset8(), destination.base(), sourceRegister);
                    } else {
                        emitter.assembler().movdl(destination.offset32(), destination.base(), sourceRegister);
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
                    break;
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVD_F32_I32 extends AMD64EirBinaryOperation.Move {

        public MOVD_F32_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, F, source, G_S, false);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
            final EirLocation sourceLocation = sourceOperand().location();
            switch (sourceLocation.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().movdl(destinationRegister, sourceGeneralRegister().as32());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().movdl(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().movdl(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
                    break;
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVD_I64_F64 extends AMD64EirBinaryOperation.Move {

        public MOVD_I64_F64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G_S, source, F, false);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final EirLocation destinationLocation = destinationOperand().location();
            final AMD64XMMRegister sourceRegister = sourceXMMRegister().as();
            switch (destinationLocation.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().movdq(destinationGeneralRegister().as64(), sourceRegister);
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                    if (destination.isOffset8Bit()) {
                        emitter.assembler().movdq(destination.offset8(), destination.base(), sourceRegister);
                    } else {
                        emitter.assembler().movdq(destination.offset32(), destination.base(), sourceRegister);
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
                    break;
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVD_F64_I64 extends AMD64EirBinaryOperation.Move {

        public MOVD_F64_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, F, source, G_S, false);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
            final EirLocation sourceLocation = sourceOperand().location();
            switch (sourceLocation.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().movdq(destinationRegister, sourceGeneralRegister().as64());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                    if (source.isOffset8Bit()) {
                        emitter.assembler().movdl(destinationRegister, source.offset8(), source.base());
                    } else {
                        emitter.assembler().movdl(destinationRegister, source.offset32(), source.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
                    break;
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVSX_I8 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public MOVSX_I8(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, false);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceRegister.as8());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_movsxb(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVSX_I16 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public MOVSX_I16(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, false);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceRegister.as16());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_movsxw(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * With REX prefix, sign-extending.
     */
    public static class MOVSXD extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public MOVSXD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, false);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().movsxd(destinationRegister.as64(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_movsxd(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsxd(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsxd(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVZX_I16 extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public MOVZX_I16(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, false);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceRegister.as16());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_movzxw(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Without REX prefix, zero-extending.
     */
    public static class MOVZXD extends AMD64EirBinaryOperation.Move.GeneralToGeneral {

        public MOVZXD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source, false);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().movzxd(destinationRegister.as64(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_movzxd(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movzxd(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movzxd(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MULSD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public MULSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().mulsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_mulsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().mulsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().mulsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MULSS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public MULSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().mulss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_mulss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().mulss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().mulss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NEG_I32 extends AMD64EirUnaryOperation {

        public NEG_I32(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().negq(operandGeneralRegister().as64());
                    break;
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().negq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().negq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NEG_I64 extends AMD64EirUnaryOperation {

        public NEG_I64(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().negq(operandGeneralRegister().as64());
                    break;
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().negq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().negq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NOT_I32 extends AMD64EirUnaryOperation {

        public NOT_I32(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().notq(operandGeneralRegister().as64());
                    break;
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().notq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().notq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NOT_I64 extends AMD64EirUnaryOperation {

        public NOT_I64(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().notq(operandGeneralRegister().as64());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().notq(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().notq(operand.offset32(), operand.base());
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class OR_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public OR_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().orl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().orl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_or(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().or(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().or(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().or_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class OR_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public OR_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().orq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().orq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_or(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().or(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().or(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().orq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().orq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().orq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().orq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().or_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class POP extends AMD64EirBinaryOperation {

        public POP(EirBlock block, EirValue destination, EirValue stackPointer) {
            super(block, destination, EirOperand.Effect.DEFINITION, G, stackPointer, EirOperand.Effect.UPDATE, G);
            assert stackPointer.location() == AMD64EirRegister.General.RSP;
            assert destination.location() != stackPointer.location();
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (destinationLocation().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General destinationRegister = (AMD64EirRegister.General) destinationLocation();
                    emitter.assembler().pop(destinationRegister.as64());
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PUSH extends AMD64EirBinaryOperation {

        public PUSH(EirBlock block, EirValue stackPointer, EirValue value) {
            super(block, stackPointer, EirOperand.Effect.UPDATE, G, value, EirOperand.Effect.USE, G_I32_L);
            assert stackPointer.location() == AMD64EirRegister.General.RSP;
            assert value.location() != stackPointer.location();
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (sourceLocation().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation();
                    emitter.assembler().push(sourceRegister.as64());
                    break;
                }
                case IMMEDIATE_32: {
                    emitter.assembler().push(sourceLocation().asImmediate().value().toInt());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().rip_push(sourceLocation().asLiteral().asLabel());
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class RET extends AMD64EirOperation implements EirControlTransfer {

        public RET(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().ret();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAL_I32 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SAL_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shll___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shll___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shll___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shll___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shll___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shll___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shll(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shll(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shll(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAL_I64 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SAL_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.LONG, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shlq___CL(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shlq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shlq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shlq___1(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shlq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shlq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shlq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shlq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shlq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAR_I32 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SAR_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().sarl___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().sarl___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().sarl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAR_I64 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SAR_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.LONG, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().sarq___CL(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().sarq___1(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().sarq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETB extends AMD64EirUnaryOperation {

        public SETB(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setb(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETBE extends AMD64EirUnaryOperation {

        public SETBE(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setbe(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETL extends AMD64EirUnaryOperation {

        public SETL(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setl(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETNBE extends AMD64EirUnaryOperation {

        public SETNBE(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setnbe(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETNB extends AMD64EirUnaryOperation {

        public SETNB(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setnb(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETNLE extends AMD64EirUnaryOperation {

        public SETNLE(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setnle(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETNP extends AMD64EirUnaryOperation {

        public SETNP(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setnp(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SETP extends AMD64EirUnaryOperation {

        public SETP(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().setp(operandGeneralRegister().as8());
                    break;
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SFENCE extends AMD64EirOperation {
        public SFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            emitter.assembler().sfence();
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SHR_I32 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SHR_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shrl___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shrl___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shrl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SHR_I64 extends AMD64EirBinaryOperation.Arithmetic.Shift {

        public SHR_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.LONG, destination, source);
        }

        @Override
        protected void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shrq___CL(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrq___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister) {
            emitter.assembler().shrq___1(destinationRegister.as64());
        }

        @Override
        protected void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrq___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shrq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUB_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public SUB_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().subl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().subl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_sub(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().sub(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().sub(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().sub_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUB_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public SUB_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().subq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().subq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_sub(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().sub(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().sub(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().subq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().subq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().subq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().subq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().sub_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUBSD extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public SUBSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().subsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_subsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().subsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().subsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUBSS extends AMD64EirBinaryOperation.Arithmetic.XMM {

        public SUBSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().subss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().rip_subss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().subss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().subss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * @see com.sun.max.asm.dis.amd64.AMD64SwitchDisassembler
     */
    public static class SWITCH_I32 extends AMD64EirSwitch {

        public SWITCH_I32(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget) {
            super(block, tag, matches, targets, defaultTarget);
        }

        private int minMatchValue() {
            return matches()[0].value().asInt();
        }

        private int maxMatchValue() {
            return matches()[matches().length - 1].value().asInt();
        }

        private long numberOfTableElements() {
            assert minMatchValue() <= maxMatchValue();
            return ((long) maxMatchValue() - (long) minMatchValue()) + 1L;
        }

        private int numberOfSwitchKeys() {
            return matches().length;
        }

        public static final int COMPARE_AND_BRANCH_MAX_SIZE = 3;
        public static final double TABLE_SWITCH_MIN_DENSITY_PERCENT = 25.0;

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            if (numberOfSwitchKeys() <= COMPARE_AND_BRANCH_MAX_SIZE) {
                assembleCompareAndBranch(emitter);
            } else {
                final double keyDensityPercentage = (100.0 * numberOfSwitchKeys()) / numberOfTableElements();
                if (keyDensityPercentage >= TABLE_SWITCH_MIN_DENSITY_PERCENT) {
                    assembleTableSwitch(emitter);
                } else if (numberOfSwitchKeys() <= COMPARE_AND_BRANCH_MAX_SIZE + 1) {
                    assembleCompareAndBranch(emitter);
                } else {
                    assembleLookupSwitch(emitter);
                }
            }
        }

        private void assembleCompareAndBranch(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 tagRegister = tagGeneralRegister().as64();
            for (int i = 0; i < matches().length; i++) {
                emitter.assembler().cmpq(tagRegister, matches()[i].value().asInt());
                emitter.assembler().jz(targets[i].asLabel());
            }
            JMP.emit(emitter, defaultTarget());
        }

        private void assembleTableSwitch(AMD64EirTargetEmitter emitter) {
            final Directives directives = emitter.assembler().directives();
            final EirOperand[] matches = matches();
            final Label[] targetLabels = new Label[matches.length];
            final Label defaultTargetLabel = defaultTarget().asLabel();
            final Label jumpTable = new Label();

            for (int i = 0; i < matches.length; i++) {
                targetLabels[i] = targets[i].asLabel();
            }

            final AMD64GeneralRegister64 indexRegister = tagGeneralRegister().as64();

            if (minMatchValue() != 0) {
                emitter.assembler().subq(indexRegister, minMatchValue());
            }
            assert numberOfTableElements() <= 0xFFFFFFFFL;
            emitter.assembler().cmpq(indexRegister, (int) numberOfTableElements());
            emitter.assembler().jnb(defaultTargetLabel);

            final AMD64EirRegister.General tableEirRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.LONG);
            final AMD64GeneralRegister64 tableRegister = tableEirRegister.as64();
            emitter.assembler().rip_lea(tableRegister, jumpTable);

            emitter.assembler().movsxd(indexRegister, tableRegister.base(), indexRegister.index(), SCALE_4);
            final AMD64GeneralRegister64 targetAddressRegister = tableRegister;
            emitter.assembler().add(targetAddressRegister, indexRegister);
            emitter.assembler().jmp(targetAddressRegister);

            directives.align(WordWidth.BITS_32.numberOfBytes);
            emitter.assembler().bindLabel(jumpTable);

            for (int i = 0; i < matches.length; i++) {
                directives.inlineOffset(targetLabels[i], jumpTable, WordWidth.BITS_32);
                if (i + 1 < matches.length) {
                    // jump to the default target for any "missing" entries
                    final int currentMatch = matches[i].value().asInt();
                    final int nextMatch = matches[i + 1].value().asInt();
                    for (int j = currentMatch + 1; j < nextMatch; j++) {
                        directives.inlineOffset(defaultTargetLabel, jumpTable, WordWidth.BITS_32);
                    }
                }
            }

            emitter.inlineDataRecorder().add(new InlineDataDescriptor.JumpTable32(jumpTable, minMatchValue(), maxMatchValue()));
        }

        private void translateLookupBinarySearch(AMD64EirTargetEmitter emitter, int bottomIndex, int topIndex) {
            final AMD64GeneralRegister32 tagRegister32 = AMD64GeneralRegister32.from(tagGeneralRegister().as64());
            final int middleIndex = (bottomIndex + topIndex) >> 1;

            emitter.assembler().cmpl(tagRegister32, matches()[middleIndex].value().asInt());
            emitter.assembler().jz(targets[middleIndex].asLabel());

            if (bottomIndex == topIndex) {
                JMP.emit(emitter, defaultTarget());
            } else {
                final Label searchAbove = new Label();
                emitter.assembler().jnle(searchAbove);

                if (bottomIndex == middleIndex) {
                    JMP.emit(emitter, defaultTarget());
                } else {
                    translateLookupBinarySearch(emitter, bottomIndex, middleIndex - 1);
                }

                emitter.assembler().bindLabel(searchAbove);

                if (topIndex == middleIndex) {
                    JMP.emit(emitter, defaultTarget());
                } else {
                    translateLookupBinarySearch(emitter, middleIndex + 1, topIndex);
                }
            }
        }

        private void assembleLookupSwitch(AMD64EirTargetEmitter emitter) {
            translateLookupBinarySearch(emitter, 0, matches().length - 1);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XCHG extends AMD64EirBinaryOperation {

        public XCHG(EirBlock block, EirValue r1, EirValue r2) {
            super(block, r1, EirOperand.Effect.UPDATE, G, r2, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64GeneralRegister64 r1 = destinationGeneralRegister().as64();
            final AMD64GeneralRegister64 r2 = sourceGeneralRegister().as64();
            emitter.assembler().xchg(r1, r2);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XOR_I32 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public XOR_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, xorDestinationEffect(destination, source), source, xorSourceEffect(destination, source));
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().xorl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().xorl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_xor(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().xor(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().xor(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().xor_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XOR_I64 extends AMD64EirBinaryOperation.Arithmetic.General.RA {

        public XOR_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, xorDestinationEffect(destination, source), source, xorSourceEffect(destination, source));
        }

        @Override
        protected void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationRegister.as64(), sourceRegister.as64());
        }

        @Override
        protected void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().xorq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().xorq(destinationRegister.as64(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().rip_xor(destinationRegister.as64(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().xor(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset) {
            emitter.assembler().xor(destinationRegister.as64(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().xorq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().xorq(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as64());
        }

        @Override
        protected void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().xor_RAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XORPD extends AMD64EirBinaryOperation.Arithmetic.XMM128 {

        public XORPD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, xorDestinationEffect(destination, source), source, xorSourceEffect(destination, source));
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().xorpd(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XORPS extends AMD64EirBinaryOperation.Arithmetic.XMM128 {

        public XORPS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, xorDestinationEffect(destination, source), source, xorSourceEffect(destination, source));
        }

        @Override
        protected void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister) {
            emitter.assembler().xorps(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class ZERO extends AMD64EirUnaryOperation {

        public static PoolSet<EirLocationCategory> locationCategories(Kind kind) {
            switch (kind.asEnum) {
                case INT:
                case LONG:
                case WORD:
                case REFERENCE:
                    return G;
                case FLOAT:
                case DOUBLE:
                    return F;
                default:
                    throw ProgramError.unknownCase();
            }
        }

        private final Kind kind;

        public Kind kind() {
            return kind;
        }

        public ZERO(EirBlock block, Kind kind, EirValue operand) {
            super(block, operand, EirOperand.Effect.DEFINITION, locationCategories(kind));
            this.kind = kind;
        }

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER: {
                    final AMD64EirRegister.General register = (AMD64EirRegister.General) operand().location();
                    emitter.assembler().xor(register.as64(), register.as64());
                    break;
                }
                case FLOATING_POINT_REGISTER: {
                    switch (kind.asEnum) {
                        case FLOAT: {
                            final AMD64EirRegister.XMM register = (AMD64EirRegister.XMM) operand().location();
                            emitter.assembler().xorps(register.as(), register.as());
                            break;
                        }
                        case DOUBLE: {
                            final AMD64EirRegister.XMM register = (AMD64EirRegister.XMM) operand().location();
                            emitter.assembler().xorpd(register.as(), register.as());
                            break;
                        }
                        default:
                            ProgramError.unknownCase();
                            break;
                    }
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }

        @Override
        public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

}
