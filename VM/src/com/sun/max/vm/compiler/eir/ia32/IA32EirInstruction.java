/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.MakeStackVariable.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.IA32EirTargetEmitter.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Holds most IA32 EIR instruction definitions in alphabetical order.
 * 
 * The EIR instructions not listed here are either synthetic (IA32EirPrologue)
 * or generalized and thus relatively complex (IA32EirLoad, AND64EirStore).
 *
 * @author Bernd Mathiske
 */
public interface IA32EirInstruction {

    public static class ADD extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public ADD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().addl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().addl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_add(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().add(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().add(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().addl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().add(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            // TODO: need to generalize this recording of constant value to other instruction generation.
            // Currently,we know that only annotations to this one are being exploited.
//          final ConstantRecorder recorder = new ConstantRecorder(emitter.assembler(), _constantValueRecorder);
            emitter.assembler().add_EAX(sourceImmediate);
//          recorder.record(Kind.INT);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADDSD extends IA32EirBinaryOperation.Arithmetic.XMM {

        public ADDSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().addsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_addsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().addsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().addsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADDSS extends IA32EirBinaryOperation.Arithmetic.XMM {

        public ADDSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().addss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_addss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().addss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().addss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class AND extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public AND(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().andl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().andl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_and(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().and(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().and(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().andl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().and(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().and_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CALL extends EirCall<EirInstructionVisitor, IA32EirTargetEmitter> {

        public CALL(EirBlock block, EirABI abi, EirValue result, EirLocation resultLocation,
                    EirValue function, EirValue[] arguments, EirLocation[] argumentLocations,
                    EirMethodGeneration methodGeneration) {
            super(block, abi, result, resultLocation, function, M_G_L_S, arguments, argumentLocations, methodGeneration);
        }

        public CALL(EirBlock block, EirValue result, EirLocation resultLocation, EirValue function, EirMethodGeneration methodGeneration) {
            super(block, null, result, resultLocation, function, M_G_L_S, null, null, methodGeneration); // TODO
        }

        private void assembleStaticTrampolineCall(IA32EirTargetEmitter emitter) {
            if (MaxineVM.isPrototyping()) {
                final int arbitraryPlaceHolderBeforeLinkingTheBootCodeRegion = -1;
                emitter.assembler().call(arbitraryPlaceHolderBeforeLinkingTheBootCodeRegion);
            } else {
                final Label label = new Label();
                emitter.assembler().fixLabel(label, StaticTrampoline.codeStart().plus(OPTIMIZED_ENTRY_POINT.offsetFromCodeStart()).toInt());
                emitter.assembler().call(label);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final EirLocation location = function().location();
            switch (location.category()) {
                case METHOD: {
                    emitter.addDirectCall(this);
                    assembleStaticTrampolineCall(emitter);
                    break;
                }
                case INTEGER_REGISTER: {
                    emitter.addIndirectCall(this);
                    final IA32EirRegister.General operandRegister = (IA32EirRegister.General) location;
                    emitter.assembler().call(operandRegister.as32());
                    break;
                }
                case LITERAL: {
                    emitter.addIndirectCall(this);
                    emitter.assembler().m_call(location.asLiteral().asLabel());
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
        }
    }

    public static class CDQ extends IA32EirBinaryOperation.Move {

        public CDQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G, source, G);
            destinationOperand().setRequiredLocation(IA32EirRegister.General.EDX);
            sourceOperand().setRequiredLocation(IA32EirRegister.General.EAX);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().cdq();
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVA_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVA_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmova(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmova(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVB_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVB_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovb(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVE_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmove(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmove(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVG_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVG_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovg(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovg(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovg(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovg(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVGE_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVGE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovge(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovge(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovge(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovge(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVL_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVL_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovl(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovl(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVLE_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVLE_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovle(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovle(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovle(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovle(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMOVP_I32 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public CMOVP_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmovp(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmovp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMP_I32 extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public CMP_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmp(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().cmp_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMP_I64 extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public CMP_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().cmpl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cmp(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmp(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().cmpl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cmp(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().cmp_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMPSD extends IA32EirBinaryOperation.Arithmetic.XMM {

        private final IA32XMMComparison _comparison;

        public IA32XMMComparison comparison() {
            return _comparison;
        }

        public CMPSD(EirBlock block, EirValue destination, EirValue source, IA32XMMComparison comparison) {
            super(block, destination, EirOperand.Effect.USE, source);
            _comparison = comparison;
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cmpsd(destinationRegister, sourceRegister, _comparison);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_cmpsd(destinationRegister, sourceLabel, _comparison);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmpsd(destinationRegister, sourceOffset, sourceBasePointer, _comparison);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmpsd(destinationRegister, sourceOffset, sourceBasePointer, _comparison);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMPSS extends IA32EirBinaryOperation.Arithmetic.XMM {

        private final IA32XMMComparison _comparison;

        public IA32XMMComparison comparison() {
            return _comparison;
        }

        public CMPSS(EirBlock block, EirValue destination, EirValue source, IA32XMMComparison comparison) {
            super(block, destination, EirOperand.Effect.USE, source);
            _comparison = comparison;
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cmpss(destinationRegister, sourceRegister, _comparison);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_cmpss(destinationRegister, sourceLabel, _comparison);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cmpss(destinationRegister, sourceOffset, sourceBasePointer, _comparison);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cmpss(destinationRegister, sourceOffset, sourceBasePointer, _comparison);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class COMISD extends IA32EirBinaryOperation.Arithmetic.XMM {

        public COMISD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().comisd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_comisd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().comisd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().comisd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class COMISS extends IA32EirBinaryOperation.Arithmetic.XMM {

        public COMISS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.USE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().comiss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_comiss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().comiss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().comiss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSD2SI_I32 extends IA32EirBinaryOperation.Move.XMMToGeneral {

        public CVTSD2SI_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceRegister);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtsd2si(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }


        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSD2SI_I64 extends IA32EirBinaryOperation.Move.XMMToGeneral {

        public CVTSD2SI_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceRegister);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtsd2si(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsd2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSD2SS extends IA32EirBinaryOperation.Move.XMMToXMM {

        public CVTSD2SS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtsd2ss(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsd2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SD extends IA32EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2sd(destinationRegister, sourceRegister.as32());
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtsi2sd(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSI2SS_I32 extends IA32EirBinaryOperation.Move.GeneralToXMM {

        public CVTSI2SS_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_G(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().cvtsi2ss(destinationRegister, sourceRegister.as32());
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtsi2ss(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtsi2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtsi2ss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSS2SD extends IA32EirBinaryOperation.Move.XMMToXMM {

        public CVTSS2SD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtss2sd(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtss2sd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CVTSS2SI extends IA32EirBinaryOperation.Move.XMMToGeneral {

        public CVTSS2SI(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_X(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().cvtss2si(destinationRegister.as32(), sourceRegister);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_cvtss2si(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().cvtss2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().cvtss2si(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIV_I64 extends IA32EirDivision {

        public DIV_I64(EirBlock block, EirValue rd, EirValue ra, EirValue divisor) {
            super(block, rd, ra, divisor);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            switch (divisorLocation().category()) {
                case INTEGER_REGISTER: {
                    //final IA32EirRegister.General divisorRegister = (IA32EirRegister.General) divisorLocation();
                    Problem.unimplemented();
                    break;
                }
                case LITERAL: {
                    Problem.unimplemented();
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(divisorLocation().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        Problem.unimplemented();
                    } else {
                        Problem.unimplemented();
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
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIVSD extends IA32EirBinaryOperation.Arithmetic.XMM {

        public DIVSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().divsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_divsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().divsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().divsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIVSS extends IA32EirBinaryOperation.Arithmetic.XMM {

        public DIVSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().divss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_divss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().divss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().divss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IDIV extends IA32EirDivision {

        public IDIV(EirBlock block, EirValue rd, EirValue ra, EirValue divisor) {
            super(block, rd, ra, divisor);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            switch (divisorLocation().category()) {
                case INTEGER_REGISTER: {
                    final IA32EirRegister.General divisorRegister = (IA32EirRegister.General) divisorLocation();
                    emitter.assembler().idivl___EAX(divisorRegister.as32());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().m_idivl___EAX(divisorLocation().asLiteral().asLabel());
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(divisorLocation().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().idivl___EAX(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().idivl___EAX(operand.offset32(), operand.base());
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
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class IMUL extends IA32EirBinaryOperation.Arithmetic {

        public IMUL(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, G, source, G_I32_L_S);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final IA32GeneralRegister32 destinationRegister = destinationGeneralRegister().as32();
            switch (sourceOperand().location().category()) {
                case INTEGER_REGISTER: {
                    final IA32GeneralRegister32 sourceRegister = sourceGeneralRegister().as32();
                    if (destinationRegister == IA32GeneralRegister32.EAX) {
                        emitter.assembler().imull___EAX(sourceRegister);
                    } else {
                        emitter.assembler().imul(destinationRegister, sourceRegister);
                    }
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
                    emitter.assembler().m_imul(destinationRegister, sourceOperand().location().asLiteral().asLabel());
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
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JMP extends IA32EirLocalControlTransfer {

        public JMP(EirBlock block, EirBlock target) {
            super(block, target);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target) {
            if (!target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jmp(target.asLabel());
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JMP_indirect extends IA32EirUnaryOperation implements EirControlTransfer {

        public JMP_indirect(EirBlock block, EirValue indirect) {
            super(block, indirect, EirOperand.Effect.USE, G_L_S);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final EirLocation location = operand().location();
            switch (location.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().jmp(operandGeneralRegister().as32());
                    break;
                }
                case LITERAL: {
                    emitter.assembler().m_jmp(location.asLiteral().asLabel());
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
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JA extends IA32EirConditionalBranch {

        public JA(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jbe(next.asLabel());
            } else {
                emitter.assembler().jnbe(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JAE extends IA32EirConditionalBranch {

        public JAE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jb(next.asLabel());
            } else {
                emitter.assembler().jnb(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JB extends IA32EirConditionalBranch {

        public JB(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnb(next.asLabel());
            } else {
                emitter.assembler().jb(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JBE extends IA32EirConditionalBranch {

        public JBE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnbe(next.asLabel());
            } else {
                emitter.assembler().jbe(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JG extends IA32EirConditionalBranch {

        public JG(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jle(next.asLabel());
            } else {
                emitter.assembler().jnle(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JGE extends IA32EirConditionalBranch {

        public JGE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jl(next.asLabel());
            } else {
                emitter.assembler().jnl(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JL extends IA32EirConditionalBranch {

        public JL(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnl(next.asLabel());
            } else {
                emitter.assembler().jl(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JLE extends IA32EirConditionalBranch {

        public JLE(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnle(next.asLabel());
            } else {
                emitter.assembler().jle(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JNZ extends IA32EirConditionalBranch {

        public JNZ(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jz(next.asLabel());
            } else {
                emitter.assembler().jnz(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class JZ extends IA32EirConditionalBranch {

        public JZ(EirBlock block, EirBlock target, EirBlock next) {
            super(block, target, next);
        }

        public static void emit(IA32EirTargetEmitter emitter, EirBlock target, EirBlock next) {
            if (target.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().jnz(next.asLabel());
            } else {
                emitter.assembler().jz(target.asLabel());
                JMP.emit(emitter, next);
            }
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emit(emitter, target(), next());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Assigns the approximate current program counter into the destination register.
     *
     * @author Bernd Mathiske
     */
    public static class LEA_PC extends IA32EirUnaryOperation {

        public LEA_PC(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final Label label = new Label();
            emitter.assembler().bindLabel(label);
            emitter.assembler().m_lea(operandGeneralRegister().as32(), label);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Assigns the address of a value on the stack to the destination register.
     *
     * @author Doug Simon
     */
    public static class LEA_STACK_ADDRESS extends IA32EirBinaryOperation {

        private final StackVariable _stackVariableKey;

        /**
         * Creates an instruction that assigns the address of a stack slot to the destination register.
         * 
         * @param destination
         *                the register in which the address is saved
         * @param source
         *                a value that will be allocated to a stack slot
         */
        public LEA_STACK_ADDRESS(EirBlock block, EirValue destination, EirValue source, StackVariable stackVariableKey) {
            super(block, destination, EirOperand.Effect.DEFINITION, G, source, EirOperand.Effect.USE, S);
            _stackVariableKey = stackVariableKey;
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final IA32GeneralRegister32 destination = destinationGeneralRegister().as32();
            final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
            if (source.isOffsetZero()) {
                emitter.assembler().lea(destination, source.base());
            } else if (source.isOffset8Bit()) {
                emitter.assembler().lea(destination, source.offset8(), source.base());
            } else {
                emitter.assembler().lea(destination, source.offset32(), source.base());
            }

            if (_stackVariableKey != null) {
                emitter.recordStackVariableOffset(_stackVariableKey, source.offset());
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class LFENCE extends IA32EirOperation {
        public LFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().lfence();
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MFENCE extends IA32EirOperation {
        public MFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().mfence();
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVD extends IA32EirBinaryOperation.Move {

        public MOVD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, G_S, source, F); // TODO: allow F_L_S, use MOV_LOW
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final EirLocation destinationLocation = destinationOperand().location();
            final IA32XMMRegister sourceRegister = sourceXMMRegister().as();
            switch (destinationLocation.category()) {
                case INTEGER_REGISTER: {
                    emitter.assembler().movd(destinationGeneralRegister().as32(), sourceRegister);
                    break;
                }
                case STACK_SLOT: {
                    final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                    if (destination.isOffset8Bit()) {
                        emitter.assembler().movd(destination.offset8(), destination.base(), sourceRegister);
                    } else {
                        emitter.assembler().movd(destination.offset32(), destination.base(), sourceRegister);
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
                    break;
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVSD extends IA32EirBinaryOperation.Move.XMMToXMM {

        public MOVSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().movsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_movsd(destinationRegister, sourceLiteralLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVSX_I8 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public MOVSX_I8(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceRegister.as8());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_movsxb(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsxb(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVSX_I16 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public MOVSX_I16(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceRegister.as16());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_movsxw(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movsxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOVZX_I16 extends IA32EirBinaryOperation.Move.GeneralToGeneral {

        public MOVZX_I16(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceRegister.as16());
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_movzxw(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().movzxw(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MULSD extends IA32EirBinaryOperation.Arithmetic.XMM {

        public MULSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().mulsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_mulsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().mulsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().mulsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MULSS extends IA32EirBinaryOperation.Arithmetic.XMM {

        public MULSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().mulss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_mulss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().mulss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().mulss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NEG extends IA32EirUnaryOperation {

        public NEG(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().negl(operandGeneralRegister().as32());
                    break;
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().negl(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().negl(operand.offset32(), operand.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NOT extends IA32EirUnaryOperation {

        public NOT(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G_S);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().notl(operandGeneralRegister().as32());
                    break;
                case STACK_SLOT: {
                    final StackAddress operand = emitter.stackAddress(operand().location().asStackSlot());
                    if (operand.isOffset8Bit()) {
                        emitter.assembler().notl(operand.offset8(), operand.base());
                    } else {
                        emitter.assembler().notl(operand.offset32(), operand.base());
                    }
                    break;
                }
                default:
                    impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class OR extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public OR(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().orl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().orl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_or(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().or(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().or(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().orl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().or(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().or_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PADDQ extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public PADDQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().paddq(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PCMPEQD extends IA32EirBinaryOperation {
        public PCMPEQD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, source, EirOperand.Effect.USE, F);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().pcmpeqd(destinationXMMRegister().as(), sourceXMMRegister().as());
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PAND extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public PAND(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().pand(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PANDN extends IA32EirBinaryOperation.Arithmetic.XMM128 {
        public PANDN(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().pandn(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class POR extends IA32EirBinaryOperation.Arithmetic.XMM128 {
        public POR(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().por(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PSLLQ extends IA32EirBinaryOperation.Arithmetic.ShiftXMM {

        public PSLLQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().psllq(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_I8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, byte sourceImmediate) {
            emitter.assembler().psllq(destinationRegister, sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PSRLQ extends IA32EirBinaryOperation.Arithmetic.ShiftXMM {

        public PSRLQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().psrlq(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_I8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, byte sourceImmediate) {
            emitter.assembler().psrlq(destinationRegister, sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PSUBQ extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public PSUBQ(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().psubq(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class PXOR extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public PXOR(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().pxor(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class RET extends IA32EirOperation implements EirControlTransfer {

        public RET(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().ret();
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAL extends IA32EirBinaryOperation.Arithmetic.ShiftGeneral {

        public SAL(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().shll___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shll___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shll___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().shll___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shll___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shll___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shll(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shll(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shll(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SAR extends IA32EirBinaryOperation.Arithmetic.ShiftGeneral {

        public SAR(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().sarl___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().sarl___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().sarl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().sarl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().sarl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().sarl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SFENCE extends IA32EirOperation {
        public SFENCE(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            emitter.assembler().sfence();
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SHR extends IA32EirBinaryOperation.Arithmetic.ShiftGeneral {

        public SHR(EirBlock block, EirValue destination, EirValue source) {
            super(block, Kind.INT, destination, source);
        }

        @Override
        protected void emit_G_CL(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().shrl___CL(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_CL(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrl___CL(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_1(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister) {
            emitter.assembler().shrl___1(destinationRegister.as32());
        }

        @Override
        protected void emit_S8_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset) {
            emitter.assembler().shrl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_S32_1(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset) {
            emitter.assembler().shrl___1(destinationOffset, destinationBasePointer);
        }

        @Override
        protected void emit_G_I(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().shrl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_S8_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().shrl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUB extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public SUB(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().subl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().subl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_sub(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().sub(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().sub(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().subl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().sub(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().sub_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class STORE_HIGH extends IA32EirBinaryOperation.Move {

        public STORE_HIGH(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, S, source, G);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final StackAddress destination = emitter.stackAddress(destinationLocation().asStackSlot());
            final int offset = destination.offset() + Ints.SIZE;
            if (WordWidth.signedEffective(offset) == WordWidth.BITS_8) {
                emitter.assembler().mov((byte) offset, destination.base(), sourceGeneralRegister().as32());
            } else {
                emitter.assembler().mov(offset, destination.base(), sourceGeneralRegister().as32());
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class STORE_LOW extends IA32EirBinaryOperation.Move {

        public STORE_LOW(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, S, source, G);
        }

        @Override
        public void emit(IA32EirTargetEmitter emitter) {
            final StackAddress destination = emitter.stackAddress(destinationLocation().asStackSlot());
            if (destination.isOffset8Bit()) {
                emitter.assembler().mov(destination.offset8(), destination.base(), sourceGeneralRegister().as32());
            } else {
                emitter.assembler().mov(destination.offset32(), destination.base(), sourceGeneralRegister().as32());
            }
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUBSD extends IA32EirBinaryOperation.Arithmetic.XMM {

        public SUBSD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().subsd(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_subsd(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().subsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().subsd(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUBSS extends IA32EirBinaryOperation.Arithmetic.XMM {

        public SUBSS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().subss(destinationRegister, sourceRegister);
        }

        @Override
        protected void emit_X_L(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, Label sourceLabel) {
            emitter.assembler().m_subss(destinationRegister, sourceLabel);
        }

        @Override
        protected void emit_X_S8(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().subss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_X_S32(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().subss(destinationRegister, sourceOffset, sourceBasePointer);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * @see com.sun.max.asm.dis.ia32.IA32SwitchDisassembler
     */
    public static class SWITCH_I32 extends IA32EirSwitch {

        public SWITCH_I32(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget) {
            super(block, tag, matches, targets, defaultTarget);
        }

        @Override
        protected void assembleCompareAndBranch(IA32EirTargetEmitter emitter) {
            final IA32GeneralRegister32 tagRegister = tagGeneralRegister().as32();
            for (int i = 0; i < matches().length; i++) {
                emitter.assembler().cmpl(tagRegister, matches()[i].value().asInt());
                emitter.assembler().jz(targets()[i].asLabel());
            }
            JMP.emit(emitter, defaultTarget());
        }

        /**
         * ATTENTION: If you change the generated assembly instructions for the table switch,
         *            then you MUST also update the {@link com.sun.max.asm.dis.ia32.IA32SwitchDisassembler}!
         */
        @Override
        protected void assembleTableSwitch(IA32EirTargetEmitter emitter) {
            final Directives directives = emitter.assembler().directives();
            final Label[] targetLabels = new Label[matches().length];
            final Label defaultTargetLabel = defaultTarget().asLabel();
            final Label jumpTable = new Label();

            for (int i = 0; i < matches().length; i++) {
                targetLabels[i] = targets()[i].asLabel();
            }

            final IA32GeneralRegister32 indexRegister = tagGeneralRegister().as32();

            if (minMatchValue() != 0) {
                emitter.assembler().subl(indexRegister, minMatchValue());
            }
            assert numberOfTableElements() <= 0xFFFFFFFFL;
            emitter.assembler().cmpl(indexRegister,  numberOfTableElements());
            emitter.assembler().jnb(defaultTargetLabel);

            final IA32EirRegister.General tableEirRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.LONG);
            final IA32GeneralRegister32 tableRegister = tableEirRegister.as32();
            emitter.assembler().m_lea(tableRegister, jumpTable);
            emitter.assembler().mov(IA32GeneralRegister32.from(indexRegister), tableRegister.base(), indexRegister.index(), SCALE_4);
            final IA32GeneralRegister32 targetAddressRegister = tableRegister;
            emitter.assembler().add(targetAddressRegister, indexRegister);
            emitter.assembler().jmp(targetAddressRegister);

            directives.align(WordWidth.BITS_32.numberOfBytes());
            emitter.assembler().bindLabel(jumpTable);
            for (int i = 0; i < matches().length; i++) {
                directives.inlineOffset(targetLabels[i], jumpTable, WordWidth.BITS_32);
                if (i + 1 < matches().length) {
                    // jump to the default target for any "missing" entries
                    final int currentMatch = matches()[i].value().asInt();
                    final int nextMatch = matches()[i + 1].value().asInt();
                    for (int j = currentMatch + 1; j < nextMatch; j++) {
                        directives.inlineOffset(defaultTargetLabel, jumpTable, WordWidth.BITS_32);
                    }
                }
            }
        }

        private void translateLookupBinarySearch(IA32EirTargetEmitter emitter, int bottomIndex, int topIndex) {
            final IA32GeneralRegister32 tagRegister = tagGeneralRegister().as32();
            final int middleIndex = (bottomIndex + topIndex) / 2;

            emitter.assembler().cmpl(tagRegister, matches()[middleIndex].value().asInt());
            emitter.assembler().jz(targets()[middleIndex].asLabel());

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

        @Override
        protected  void assembleLookupSwitch(IA32EirTargetEmitter emitter) {
            translateLookupBinarySearch(emitter, 0, matches().length - 1);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XOR extends IA32EirBinaryOperation.Arithmetic.General.RA {

        public XOR(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_G_G(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationRegister.as32(), sourceRegister.as32());
        }

        @Override
        protected void emit_G_I8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, byte sourceImmediate) {
            emitter.assembler().xorl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_I32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, int sourceImmediate) {
            emitter.assembler().xorl(destinationRegister.as32(), sourceImmediate);
        }

        @Override
        protected void emit_G_L(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, Label sourceLiteralLabel) {
            emitter.assembler().m_xor(destinationRegister.as32(), sourceLiteralLabel);
        }

        @Override
        protected void emit_G_S8(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, byte sourceOffset) {
            emitter.assembler().xor(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_G_S32(IA32EirTargetEmitter emitter, IA32EirRegister.General destinationRegister, IA32IndirectRegister32 sourceBasePointer, int sourceOffset) {
            emitter.assembler().xor(destinationRegister.as32(), sourceOffset, sourceBasePointer);
        }

        @Override
        protected void emit_S8_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, int sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S8_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, byte destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_S32_I8(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, byte sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_I32(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, int sourceImmediate) {
            emitter.assembler().xorl(destinationOffset, destinationBasePointer, sourceImmediate);
        }

        @Override
        protected void emit_S32_G(IA32EirTargetEmitter emitter, IA32IndirectRegister32 destinationBasePointer, int destinationOffset, IA32EirRegister.General sourceRegister) {
            emitter.assembler().xor(destinationOffset, destinationBasePointer, sourceRegister.as32());
        }

        @Override
        protected void emit_RA_I32(IA32EirTargetEmitter emitter, int sourceImmediate) {
            emitter.assembler().xor_EAX(sourceImmediate);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XORPD extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public XORPD(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().xorpd(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XORPS extends IA32EirBinaryOperation.Arithmetic.XMM128 {

        public XORPS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, EirOperand.Effect.UPDATE, source);
        }

        @Override
        protected void emit_X_X(IA32EirTargetEmitter emitter, IA32XMMRegister destinationRegister, IA32XMMRegister sourceRegister) {
            emitter.assembler().xorps(destinationRegister, sourceRegister);
        }

        @Override
        public void acceptVisitor(IA32EirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
}
