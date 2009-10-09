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
package com.sun.max.vm.compiler.eir.sparc;

import static com.sun.max.asm.sparc.GPR.*;
import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.dir.eir.sparc.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirStackSlot.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirRegister.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirTargetEmitter.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;


/**
 * A marker interface for SPARC Eir Instruction. The interface doesn't define any method. It is used to
 * hold the definition of most SPARC EIR instruction definitions in alphabetical order.
 *
 * The EIR instructions not listed here are either synthetic (SPARCEirPrologue)
 * or generalized and thus relatively complex.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public interface SPARCEirInstruction {

    public static class BA extends SPARCEirLocalControlTransfer implements EirJump {

        public BA(EirBlock block, EirBlock target) {
            super(block, target);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if (!target().isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().ba(annulBit(), target().asLabel());
                emitDelayedSlot(emitter);
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public abstract static class BranchOnIntegerConditionCode extends SPARCEirConditionalBranch {
        private final ICCOperand conditionCode;

        public ICCOperand conditionCode() {
            return conditionCode;
        }

        public BranchOnIntegerConditionCode(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next);
            conditionCode = comparisonKind.width.equals(WordWidth.BITS_64) ? ICCOperand.XCC : ICCOperand.ICC;
        }

        @Override
        public String toString() {
            final String s = super.toString();
            return s + " %" + conditionCode;
        }
    }

    public static class BE extends BranchOnIntegerConditionCode {
        public BE(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().be(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bne(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    public static class BNE extends BranchOnIntegerConditionCode {
        public BNE(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bne(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().be(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    public static class BL extends BranchOnIntegerConditionCode {
        public BL(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bl(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bge(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    public static class BLE extends BranchOnIntegerConditionCode {
        public BLE(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().ble(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bg(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BGE extends BranchOnIntegerConditionCode {
        public BGE(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bge(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bl(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BG extends BranchOnIntegerConditionCode {
        public BG(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bg(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().ble(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Branch on Less  Unsigned.
     */
    public static class BLU extends BranchOnIntegerConditionCode {
        public BLU(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bcs(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bcc(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Branch on Less or Equal Unsigned.
     */
    public static class BLEU extends BranchOnIntegerConditionCode {
        public BLEU(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bleu(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bgu(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Branch on Greater or Equals  Unsigned.
     */
    public static class BGEU extends BranchOnIntegerConditionCode {
        public BGEU(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bcc(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bcs(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Branch on Greater Unsigned.
     */
    public static class BGU extends BranchOnIntegerConditionCode {
        public BGU(EirBlock block, EirBlock target, EirBlock next, Kind comparisonKind) {
            super(block, target, next, comparisonKind);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bgu(annulBit(), BranchPredictionBit.PT, conditionCode(),  target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().bleu(annulBit(), BranchPredictionBit.PN, conditionCode(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public abstract static class SPARCEirBranchOnIntegerRegister extends SPARCEirConditionalBranch {
        private final EirOperand testedOperand;

        public EirValue testedValue() {
            return testedOperand.eirValue();
        }

        public EirLocation testedOperandLocation() {
            return testedOperand.location();
        }

        public SPARCEirBranchOnIntegerRegister(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next);
            testedOperand = new EirOperand(this, EirOperand.Effect.USE, G);
            testedOperand.setEirValue(testedValue);
        }

        public SPARCEirRegister.GeneralPurpose testedOperandGeneralRegister() {
            return (SPARCEirRegister.GeneralPurpose) testedOperandLocation();
        }

        @Override
        public void visitOperands(EirOperand.Procedure visitor) {
            super.visitOperands(visitor);
            visitor.run(testedOperand);
        }

        @Override
        public String toString() {
            return super.toString() + " [" + testedOperand + "]";
        }
    }

    public static class BRZ extends SPARCEirBranchOnIntegerRegister {
        public BRZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brz(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brnz(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BRNZ extends SPARCEirBranchOnIntegerRegister {
        public BRNZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brnz(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brz(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BRLZ extends SPARCEirBranchOnIntegerRegister {
        public BRLZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brlz(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brgez(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BRLEZ extends SPARCEirBranchOnIntegerRegister {
        public BRLEZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brlez(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brgz(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BRGEZ extends SPARCEirBranchOnIntegerRegister {
        public BRGEZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brgez(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brlz(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class BRGZ extends SPARCEirBranchOnIntegerRegister {
        public BRGZ(EirBlock block, EirBlock target, EirBlock next, EirValue testedValue) {
            super(block, target, next, testedValue);
        }

        @Override
        public  void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brgz(annulBit(), BranchPredictionBit.PT, testedOperandGeneralRegister().as(), target().asLabel());
        }

        @Override
        public  void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label) {
            emitter.assembler().brlez(annulBit(), BranchPredictionBit.PN, testedOperandGeneralRegister().as(), next().asLabel());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    int REGISTER_WINDOW_SIZE = (8 + 8) * 8; // (input registers + output registers) * (register size)

    public static class FLAT_CALL extends EirCall<EirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction {


        public FLAT_CALL(EirBlock block, EirABI abi, EirValue result, EirLocation resultLocation,
                        EirValue function, EirValue[] arguments, EirLocation[] argumentLocations,
                        EirMethodGeneration methodGeneration) {
            super(block, abi, result, resultLocation, function, M, arguments, argumentLocations, false, methodGeneration);
        }

        private void assembleStaticTrampolineCall(SPARCEirTargetEmitter emitter) {
            if (MaxineVM.isPrototyping()) {
                final int arbitraryPlaceHolderBeforeLinkingTheBootCodeRegion = -1;
                emitter.assembler().call(arbitraryPlaceHolderBeforeLinkingTheBootCodeRegion);
            } else {
                final Label label = new Label();
                emitter.fixLabel(label, StaticTrampoline.codeStart().plus(CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart()));
                emitter.assembler().call(label);
            }
            emitter.assembler().stx(O7, O6, REGISTER_WINDOW_SIZE); // "SAVE" register O7
        }

        @Override
        public void addFrameReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
            SPARCEirGenerator.addFrameReferenceMapAtCall(liveVariables(), arguments, stackSlotWidth, map);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            final EirLocation location = function().location();
            switch (location.category()) {
                case METHOD: {
                    emitter.addDirectCall(this);
                    assembleStaticTrampolineCall(emitter);
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }
    }

    public static class FLAT_RETURN extends SPARCEirOperation implements EirControlTransfer {

        public FLAT_RETURN(EirBlock block) {
            super(block);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().ret();
            emitter.assembler().ldx(O6, REGISTER_WINDOW_SIZE, O7); // "RESTORE" register O7
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     *  Standard call on SPARC. Save's current instruction pointer in %o7.
     *  Moving the register windows is the callee's decision. See EirPrologue and return instructions.
     */
    public static class CALL extends EirCall<EirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction  {
        final SPARCEirRegister.GeneralPurpose savedSafepointLatch;
        final SPARCEirRegister.GeneralPurpose safepointLatch;

        public SPARCEirRegister.GeneralPurpose savedSafepointLatch() {
            return savedSafepointLatch;
        }

        public CALL(EirBlock block, EirABI abi, EirValue result, EirLocation resultLocation,
                        EirValue function, EirValue[] arguments, EirLocation[] argumentLocations,
                        boolean isNativeFunctionCall, EirMethodGeneration methodGeneration) {
            super(block, abi, result, resultLocation, function, M_G, arguments, argumentLocations, isNativeFunctionCall, methodGeneration);
            final SPARCEirABI sparcAbi = (SPARCEirABI) abi;
            safepointLatch = (SPARCEirRegister.GeneralPurpose) sparcAbi.safepointLatchRegister();
            final DirToSPARCEirMethodTranslation sparcMethodGeneration = (DirToSPARCEirMethodTranslation) methodGeneration;
            if (sparcAbi.callerSavedRegisters().contains(safepointLatch) && sparcMethodGeneration.callerMustSaveLatchRegister()) {
                savedSafepointLatch = DirToSPARCEirMethodTranslation.SAVED_SAFEPOINT_LATCH_LOCAL;
                sparcMethodGeneration.needsSavingSafepointLatchInLocal();
            } else {
                savedSafepointLatch = null;
            }
        }

        @Override
        public void addFrameReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
            SPARCEirGenerator.addFrameReferenceMapAtCall(liveVariables(), arguments, stackSlotWidth, map);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            final EirLocation location = function().location();
            switch (location.category()) {
                case METHOD: {
                    emitter.addDirectCall(this);
                    final int placeHolderBeforeLinking = 0;
                    emitter.assembler().call(placeHolderBeforeLinking);
                    break;
                }
                case INTEGER_REGISTER: {
                    emitter.addIndirectCall(this);
                    final SPARCEirRegister.GeneralPurpose operandRegister = (SPARCEirRegister.GeneralPurpose) location;
                    emitter.assembler().call(operandRegister.as(), G0);
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
            // This is a workaround to a problem with the current register allocator which makes it very hard to reload a spilled - preallocated caller save
            // register (the only occurrence of which is the safepoint latch) into its preallocated location.
            if (savedSafepointLatch != null) {
                // Save safepoint latch in delay slot
                emitter.assembler().mov(safepointLatch.as(), savedSafepointLatch.as());
                // Restore it on return
                emitter.assembler().mov(savedSafepointLatch.as(), safepointLatch.as());
            } else {
                // TODO: fill delay slot.
                emitter.assembler().nop();
            }
        }
    }


    public static class RET extends SPARCEirOperation implements EirControlTransfer {
        public enum FROM {
            JAVA_METHOD,
            TRAMPOLINE,
            STATIC_TRAMPOLINE,
            TRAP_STUB
        }

        final FROM from;
        public RET(EirBlock block, FROM returnFrom) {
            super(block);
            from = returnFrom;
        }
        public RET(EirBlock block) {
            this(block, FROM.JAVA_METHOD);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            switch(from) {
                case TRAMPOLINE:
                    emitter.assembler().jmpl(O0, G0, G0);
                    emitter.assembler().restore(O1, G0, O0);   // Restore the receiver in %o0
                    break;
                case STATIC_TRAMPOLINE:
                    // Static trampolines return at the call instruction in order to re-execute the call instruction
                    // The call instruction is stored in i7 (the ret pseudo instruction is just jmpl %i7 +8).
                    emitter.assembler().jmpl(I7, G0, G0);
                    emitter.assembler().restore(G0, G0, G0);
                    break;
                case TRAP_STUB:
                    // TrapStubEpilogue set the return address into L0. Can't touch I7 (or any other I register)
                    // because this one should contains whatever value was in the trapped instruction's O7 register.
                    emitter.assembler().jmpl(L0, G0, G0);
                    emitter.assembler().restore(G0, G0, G0);
                    break;
                case JAVA_METHOD:
                    emitter.assembler().ret();
                    emitter.assembler().restore(G0, G0, G0);
                    break;
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NEG_I32 extends SPARCEirUnaryOperation {

        public NEG_I32(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().neg(operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NEG_I64 extends SPARCEirUnaryOperation {

        public NEG_I64(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().neg(operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    /**
     * Floating point single precision negation.
     * @author Laurent Daynes
     */
    public static class FNEG_S extends SPARCEirUnaryOperation {

        public FNEG_S(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(FLOATING_POINT_REGISTER)) {
                final SFPR registerOperand = operandFloatingPointRegister().asSinglePrecision();
                emitter.assembler().fnegs(registerOperand, registerOperand);
            } else {
                impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Floating point double precision negation.
     * @author Laurent Daynes
     */
    public static class FNEG_D extends SPARCEirUnaryOperation {

        public FNEG_D(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(FLOATING_POINT_REGISTER)) {
                final DFPR registerOperand = operandFloatingPointRegister().asDoublePrecision();
                emitter.assembler().fnegd(registerOperand, registerOperand);
            } else {
                impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NOT_I32 extends SPARCEirUnaryOperation {

        public NOT_I32(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().not(operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class NOT_I64 extends SPARCEirUnaryOperation {

        public NOT_I64(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().not(operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }
    public static class JMP_indirect extends SPARCEirUnaryOperation {
        public JMP_indirect(EirBlock block, EirValue indirect) {
            super(block, indirect, EirOperand.Effect.USE, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().jmp(operandGeneralRegister().as(), G0);
            } else {
                impossibleLocationCategory();
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class RDPC extends  SPARCEirUnaryOperation {
        public RDPC(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                emitter.assembler().rd(StateRegister.PC, operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SET_I32 extends SPARCEirUnaryOperation {
        private final  EirOperand immediateOperand;

        public EirOperand immediateOperand() {
            return immediateOperand;
        }

        public SET_I32(EirBlock block, EirValue destination, EirConstant immediateSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, G);
            immediateOperand = new EirOperand(this, EirOperand.Effect.USE, I);
            immediateOperand.setEirValue(immediateSource);
        }
        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            if  (operand().location().category().equals(INTEGER_REGISTER)) {
                final int immediateValue =  immediateOperand.value().asInt();
                emitter.assembler().set(immediateValue, operandGeneralRegister().as());
            } else {
                impossibleLocationCategory();
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADD_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public ADD_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public ADD_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().add(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().add(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class ADD_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public ADD_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }
        public ADD_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().addcc(leftRegister.as(), rightRegister.as(), destinationGeneralRegister().as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().addcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class AND_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public AND_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }
        public AND_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().andcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().andcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class AND_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public AND_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public AND_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().andcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().andcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class OR_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public OR_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }
        public OR_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().orcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().orcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class OR_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public OR_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public OR_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().orcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().orcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XOR_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public XOR_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public XOR_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().xorcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().xorcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class XOR_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public XOR_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public XOR_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().xorcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().xorcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }


    public static class SLL_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SLL_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SLL_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().sll(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().sll(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SLL_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SLL_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SLL_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().sllx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().sllx(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SRL_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SRL_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SRL_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().srl(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().srl(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SRL_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SRL_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SRL_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().srlx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().srlx(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SRA_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SRA_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SRA_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().sra(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().sra(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SRA_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SRA_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SRA_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().srax(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().srax(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }


    public static class CMP_I32 extends SPARCEirBinaryOperation.Compare.General {
        public CMP_I32(EirBlock block, EirValue leftValue, EirValue rightValue) {
            super(block, leftValue, rightValue);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CMP_I64 extends SPARCEirBinaryOperation.Compare.General {
        public CMP_I64(EirBlock block, EirValue leftValue, EirValue rightValue) {
            super(block, leftValue, rightValue);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FADD_S extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FADD_S(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FADD_S(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fadds(sourceRegister1.asSinglePrecision(), sourceRegister2.asSinglePrecision(), destinationRegister.asSinglePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FADD_D extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FADD_D(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FADD_D(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().faddd(sourceRegister1.asDoublePrecision(), sourceRegister2.asDoublePrecision(), destinationRegister.asDoublePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FCMP_S extends  SPARCEirBinaryOperation.Compare.FloatingPoint {
        public FCMP_S(EirBlock block, EirValue leftValue, EirValue rightValue) {
            super(block, leftValue, rightValue);
        }

        @Override
        protected void emit_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint leftRegister, SPARCEirRegister.FloatingPoint rightRegister) {
            emitter.assembler().fcmps(selectedConditionCode(), leftRegister.asSinglePrecision(), rightRegister.asSinglePrecision());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FCMP_D extends  SPARCEirBinaryOperation.Compare.FloatingPoint {
        public FCMP_D(EirBlock block, EirValue leftValue, EirValue rightValue) {
            super(block, leftValue, rightValue);
        }

        @Override
        protected void emit_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint leftRegister, SPARCEirRegister.FloatingPoint rightRegister) {
            emitter.assembler().fcmpd(selectedConditionCode(), leftRegister.asDoublePrecision(), rightRegister.asDoublePrecision());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FDIV_S extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FDIV_S(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FDIV_S(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fdivs(sourceRegister1.asSinglePrecision(), sourceRegister2.asSinglePrecision(), destinationRegister.asSinglePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FDIV_D extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FDIV_D(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FDIV_D(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fdivd(sourceRegister1.asDoublePrecision(), sourceRegister2.asDoublePrecision(), destinationRegister.asDoublePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FMUL_S extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FMUL_S(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FMUL_S(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fmuls(sourceRegister1.asSinglePrecision(), sourceRegister2.asSinglePrecision(), destinationRegister.asSinglePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FMUL_D extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FMUL_D(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FMUL_D(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fmuld(sourceRegister1.asDoublePrecision(), sourceRegister2.asDoublePrecision(), destinationRegister.asDoublePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FSUB_S extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FSUB_S(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FSUB_S(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fsubs(sourceRegister1.asSinglePrecision(), sourceRegister2.asSinglePrecision(), destinationRegister.asSinglePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FSUB_D extends  SPARCEirBinaryOperation.Arithmetic.FloatingPoint {
        public FSUB_D(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public FSUB_D(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint sourceRegister1, SPARCEirRegister.FloatingPoint sourceRegister2) {
            emitter.assembler().fsubd(sourceRegister1.asDoublePrecision(), sourceRegister2.asDoublePrecision(), destinationRegister.asDoublePrecision());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FSTOD extends  SPARCEirBinaryOperation {
        public FSTOD(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fstod(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asDoublePrecision());
        }
    }

    public static class FDTOS extends  SPARCEirBinaryOperation {
        public FDTOS(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fdtos(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asSinglePrecision());
        }
    }

    /**
     * EIR instruction for converting 32-bit signed integer into a single-precision floating point.
     *
     * @author Laurent Daynes
     */
    public static class FITOS extends  SPARCEirBinaryOperation {
        public FITOS(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FITOS(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fitos(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asSinglePrecision());
        }
    }

    /**
     * EIR instruction for converting 32-bit signed integer into a double-precision floating point.
     *
     * @author Laurent Daynes
     */
    public static class FITOD extends  SPARCEirBinaryOperation {
        public FITOD(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FITOD(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fitod(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asDoublePrecision());
        }
    }

    /**
     * EIR instruction for converting 64-bit signed integer into a single-precision floating point.
     *
     * @author Laurent Daynes
     */
    public static class FXTOS extends  SPARCEirBinaryOperation {
        public FXTOS(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FXTOS(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fxtos(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asSinglePrecision());
        }
    }

    /**
     * EIR instruction for converting 64-bit signed integer into a double-precision floating point.
     *
     * @author Laurent Daynes
     */
    public static class FXTOD extends  SPARCEirBinaryOperation {
        public FXTOD(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FXTOD(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fxtod(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asDoublePrecision());
        }
    }

    /**
     * EIR instruction for converting single-precision floating point to a 32-bits signed integer.
     *
     * @author Laurent Daynes
     */
    public static class  FSTOI extends  SPARCEirBinaryOperation {
        public FSTOI(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FSTOI(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fstoi(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asSinglePrecision());
        }
    }

    /**
     * EIR instruction for converting double-precision floating point to a 32-bits signed integer.
     *
     * @author Laurent Daynes
     */
    public static class  FDTOI extends  SPARCEirBinaryOperation {
        public FDTOI(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FDTOI(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fdtoi(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asSinglePrecision());
        }
    }
    /**
     * EIR instruction for converting single-precision floating point to a 64-bits signed integer.
     *
     * @author Laurent Daynes
     */
    public static class  FSTOX extends  SPARCEirBinaryOperation {
        public FSTOX(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FSTOX(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fstox(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asDoublePrecision());
        }
    }
    /**
     * EIR instruction for converting doublle-precision floating point to a 64-bits signed integer.
     *
     * @author Laurent Daynes
     */
    public static class  FDTOX extends  SPARCEirBinaryOperation {
        public FDTOX(EirBlock block, EirValue destination) {
            super(block, destination, EirOperand.Effect.UPDATE, F, destination, EirOperand.Effect.USE, F);
        }

        public FDTOX(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, F, rightSource, EirOperand.Effect.USE, F);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().fdtox(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asDoublePrecision());
        }
    }

    public static class MUL_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public MUL_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public MUL_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().mulx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
            // make sure we set the sign for 32-bit
            emitter.assembler().sra(destinationRegister.as(), G0, destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().mulx(leftRegister.as(), rightImmediate, destinationRegister.as());
            // make sure we set the sign for 32-bit
            emitter.assembler().sra(destinationRegister.as(), G0, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MUL_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public MUL_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public MUL_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().mulx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().mulx(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIV_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public DIV_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public DIV_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }

        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().sdivx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
            // make sure we set the sign for 32-bit
            emitter.assembler().sra(destinationRegister.as(), G0, destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().sdivx(leftRegister.as(), rightImmediate, destinationRegister.as());
            // make sure we set the sign for 32-bit
            emitter.assembler().sra(destinationRegister.as(), G0, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class DIV_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public DIV_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public DIV_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().sdivx(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().sdivx(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUB_I32 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SUB_I32(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SUB_I32(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().subcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().subcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SUB_I64 extends SPARCEirBinaryOperation.Arithmetic.General {
        public SUB_I64(EirBlock block, EirValue destination, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.UPDATE, rightSource, EirOperand.Effect.USE);
        }

        public SUB_I64(EirBlock block, EirValue destination, EirValue leftSource, EirValue rightSource) {
            super(block, destination, EirOperand.Effect.DEFINITION, leftSource, EirOperand.Effect.USE, rightSource, EirOperand.Effect.USE);
        }
        @Override
        protected  void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister) {
            emitter.assembler().subcc(leftRegister.as(), rightRegister.as(), destinationRegister.as());
        }
        @Override
        protected  void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, int rightImmediate) {
            emitter.assembler().subcc(leftRegister.as(), rightImmediate, destinationRegister.as());
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class FLUSHW extends SPARCEirOperation {
        public FLUSHW(EirBlock block) {
            super(block);
        }
        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().flushw();
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MEMBAR extends SPARCEirOperation {
        private final MembarOperand ordering;

        public MEMBAR(EirBlock block, MembarOperand ordering) {
            super(block);
            this.ordering = ordering;
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            emitter.assembler().membar(ordering);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return super.toString() + " " + ordering;
        }
    }

    public static class MOV_I32 extends SPARCEirBinaryOperation.Move.GeneralToGeneral {
        public MOV_I32(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().or(sourceRegister.as(), G0, destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().set(sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class MOV_I64 extends SPARCEirBinaryOperation.Move.GeneralToGeneral {
        public MOV_I64(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().or(sourceRegister.as(), G0, destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
            emitter.assembler().setx(sourceImmediate, scratchRegister.as(), destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class STACK_ALLOCATE extends SPARCEirUnaryOperation {
        public final int offset;

        public STACK_ALLOCATE(EirBlock block, EirValue operand, int offset) {
            super(block, operand, EirOperand.Effect.DEFINITION, G);
            this.offset = offset;
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            EirStackSlot stackSlot = new EirStackSlot(Purpose.BLOCK, offset);
            final StackAddress source = emitter.stackAddress(stackSlot);
            final GeneralPurpose destination = operandGeneralRegister();
            if (isSimm13(source.offset)) {
                emitter.assembler().add(source.base, source.offset, destination.as());
            } else {
                emitter.assembler().set(source.offset, destination.as());
                emitter.assembler().add(source.base,  destination.as(), destination.as());
            }
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SET_STACK_ADDRESS extends SPARCEirBinaryOperation {
        public SET_STACK_ADDRESS(EirBlock block, EirValue destination, EirValue source) {
            super(block, destination,   EirOperand.Effect.DEFINITION, G, source, EirOperand.Effect.USE, S);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            final StackAddress source = emitter.stackAddress(sourceOperand().location().asStackSlot());
            final int offset = source.offset;
            if (isSimm13(offset)) {
                emitter.assembler().add(source.base, offset, destinationGeneralRegister().as());
            } else {
                emitter.assembler().set(offset, destinationGeneralRegister().as());
                emitter.assembler().add(source.base,  destinationGeneralRegister().as(), destinationGeneralRegister().as());
            }
        }
    }

    /**
     * Conditional move on inequality. Takes integer operands
     * and the ICCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of the number of bits. It only checks the condition code and jumps.
     */
    public static class MOVNE extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVNE(EirBlock block, ICCOperand iccConditionCode, EirValue destination, EirValue source) {
            super(block, iccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movne((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movne((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on inequality. Takes floating point operands
     * and the FCCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVFNE extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVFNE(EirBlock block, FCCOperand fccConditionCode, EirValue destination, EirValue source) {
            super(block, fccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movne((FCCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movne((FCCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }


    /**
     * Conditional move on equality. Takes integer operands
     * and the ICCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVE extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVE(EirBlock block, ICCOperand iccConditionCode, EirValue destination, EirValue source) {
            super(block, iccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().move((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().move((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on equality. Takes floating point operands
     * and the FCCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVFE extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVFE(EirBlock block, FCCOperand fccConditionCode, EirValue destination, EirValue source) {
            super(block, fccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().move((FCCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().move((FCCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }


    /**
     * Conditional move on signed greater than. Takes integer operands
     * and the ICCOperand condition code register. Because this is always preceeded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVG extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVG(EirBlock block, ICCOperand iccConditionCode, EirValue destination, EirValue source) {
            super(block, iccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movg((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movg((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

   /**
    * Conditional move on unsigned greater than. Takes only integer operands
    * and the ICCOperand condition code register. Because this is always preceded
    * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
    * of number of bits. It only checks the condition code and jumps.
    */
    public static class MOVGU extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVGU(EirBlock block, ICCOperand iccConditionCode, EirValue destination, EirValue source) {
            super(block, iccConditionCode, destination, source);
        }       @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movg((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movg((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on carry clear, used to test unsigned greater than or equal. Takes only integer operands
     * and the ICCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     *
     * @author Aritra Bandyopadhyay
     */
    public static class MOVCC extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVCC(EirBlock block, ICCOperand conditionCode, EirValue destination, EirValue source) {
            super(block, conditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movcc((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movcc((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on signed less than. Takes only integer operands
     * and the ICCOperand condition code register. Because this is always preceeded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVL extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVL(EirBlock block, ICCOperand iccConditionCode, EirValue destination, EirValue source) {
            super(block, iccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movl((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movl((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on integer registers on less than, for floating point operands
     * and the ICCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVFL extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVFL(EirBlock block, FCCOperand fccConditionCode, EirValue destination, EirValue source) {
            super(block, fccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            if (conditionCode instanceof FCCOperand) {
                emitter.assembler().movl((FCCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
            }
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            if (conditionCode instanceof FCCOperand) {
                emitter.assembler().movl((FCCOperand) conditionCode, sourceImmediate, destinationRegister.as());
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on integer registers on greater than. Takes floating point operands
     * and the FCCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVFG extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVFG(EirBlock block, FCCOperand fccConditionCode, EirValue destination, EirValue source) {
            super(block, fccConditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            if (conditionCode instanceof FCCOperand) {
                emitter.assembler().movg((FCCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
            }
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            if (conditionCode instanceof FCCOperand) {
                emitter.assembler().movg((FCCOperand) conditionCode, sourceImmediate, destinationRegister.as());
            }
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }




    /**
     * Conditional move on carry set, used to test unsigned less than. Takes only integer operands
     * and the ICCOperand condition code register. Because this is always preceded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVCS extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVCS(EirBlock block, ICCOperand conditionCode, EirValue destination, EirValue source) {
            super(block, conditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movl((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movl((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Conditional move on unsigned less or equal. Takes only integer operands
     * and the ICCOperand condition code register. Because this is always preceeded
     * by a 32-bit or a 64-bit comparison instruction, this instruction is independent
     * of number of bits. It only checks the condition code and jumps.
     */
    public static class MOVLEU extends SPARCEirBinaryOperation.Move.GeneralToGeneral.OnCondition {

        public MOVLEU(EirBlock block, ICCOperand conditionCode, EirValue destination, EirValue source) {
            super(block, conditionCode, destination, source);
        }
        @Override
        public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
            emitter.assembler().movle((ICCOperand) conditionCode, sourceRegister.as(), destinationRegister.as());
        }
        @Override
        public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
            emitter.assembler().movle((ICCOperand) conditionCode, sourceImmediate, destinationRegister.as());
        }
        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class ZERO extends SPARCEirUnaryOperation {
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

        public final Kind kind;

        public ZERO(EirBlock block, Kind kind, EirValue operand) {
            super(block, operand, EirOperand.Effect.DEFINITION, locationCategories(kind));
            this.kind = kind;
        }

        @Override
        public void emit(SPARCEirTargetEmitter emitter) {
            switch (operand().location().category()) {
                case INTEGER_REGISTER:
                    emitter.assembler().mov(G0, operandGeneralRegister().as());
                    break;
                case FLOATING_POINT_REGISTER: {
                    switch (kind.asEnum) {
                        case FLOAT:  {
                            final SFPR freg = operandFloatingPointRegister().asSinglePrecision();
                            emitter.assembler().fsubs(freg, freg, freg);
                            break;
                        }
                        case DOUBLE: {
                            final DFPR freg = operandFloatingPointRegister().asDoublePrecision();
                            emitter.assembler().fsubd(freg, freg, freg);
                            break;
                        }
                        default: {
                            ProgramError.unknownCase();
                            break;
                        }
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
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class SWITCH_I32 extends SPARCEirIntSwitch {

        private final EirOperand indexRegister;

        public EirOperand indexRegister() {
            return indexRegister;
        }

        public SPARCEirRegister.GeneralPurpose indexGeneralRegister() {
            return (SPARCEirRegister.GeneralPurpose) indexRegister().location();
        }

        public SWITCH_I32(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget, EirVariable indexRegister) {
            super(block, tag, matches, targets, defaultTarget);
            if (selectedImplementation().equals(INT_SWITCH_SELECTED_IMPLEMENTATION.TABLE_SWITCH)) {
                this.indexRegister = new EirOperand(this, EirOperand.Effect.DEFINITION, G);
                this.indexRegister.setEirValue(indexRegister);
            } else {
                this.indexRegister = null;
            }
        }

        @Override
        public void visitOperands(EirOperand.Procedure visitor) {
            super.visitOperands(visitor);
            if (selectedImplementation().equals(INT_SWITCH_SELECTED_IMPLEMENTATION.TABLE_SWITCH)) {
                visitor.run(indexRegister);
            }
        }

        @Override
        protected void assembleCompareAndBranch(SPARCEirTargetEmitter emitter) {
            final GPR tagRegister = tagGeneralRegister().as();
            if (SPARCEirOperation.isSimm13(minMatchValue()) && SPARCEirOperation.isSimm13(maxMatchValue())) {
                for (int i = 0; i < matches().length; i++) {
                    emitter.assembler().cmp(tagRegister, matches()[i].value().asInt());
                    emitter.assembler().be(targets[i].asLabel());
                    emitter.assembler().nop(); // empty delay slot
                }
            } else {
                final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
                final GPR matchRegister = scratchRegister.as();
                final int last = matches().length - 1;
                int i = 0;
                int match = matches()[i].value().asInt();
                if (SPARCEirOperation.isSimm13(match)) {
                    emitter.assembler().sethi(SPARCAssembler.hi(match), matchRegister);
                }
                do {
                    if (SPARCEirOperation.isSimm13(match)) {
                        emitter.assembler().cmp(tagRegister, match);
                    } else {
                        final int lo = SPARCAssembler.lo(match);
                        if (lo != 0) { // has low 13-bits ?
                            emitter.assembler().or(matchRegister, lo, matchRegister);
                        }
                        if (match < 0) { // needs sign
                            emitter.assembler().sra(matchRegister, G0, matchRegister);
                        }
                        emitter.assembler().cmp(tagRegister, matchRegister);
                    }
                    emitter.assembler().be(AnnulBit.NO_A, targets[i].asLabel());
                    if (i == last) {
                        emitter.assembler().nop(); // empty delay slot
                        break;
                    }
                    // Look ahead the next match to decide what to do in the delay slot.
                    match = matches()[++i].value().asInt();
                    if (SPARCEirOperation.isSimm13(match)) {
                        emitter.assembler().nop(); // empty delay slot
                    } else {
                        // We may be wasting this one if we have a match, but it's just 1 cycle in the delay slot.
                        // Doesn't matter what ends up in the matchRegister if we're branching.
                        emitter.assembler().sethi(SPARCAssembler.hi(match), matchRegister);
                    }
                } while (true);
            }
            if (!defaultTarget().isAdjacentSuccessorOf(emitter.currentEirBlock())) {
                emitter.assembler().ba(defaultTarget().asLabel());
                emitter.assembler().nop(); // empty delay slot
            }
        }

        private void branchToDefaultTarget(SPARCEirTargetEmitter emitter) {
            emitter.assembler().ba(AnnulBit.NO_A, defaultTarget().asLabel());
            emitter.assembler().nop(); // TODO -- exploit delay slot
        }

        @Override
        protected void assembleTableSwitch(SPARCEirTargetEmitter emitter) {
            final Directives directives = emitter.assembler().directives();
            final EirOperand[] matches = matches();
            final Label[] targetLabels = new Label[matches.length];
            final Label defaultTargetLabel = defaultTarget().asLabel();
            final Label jumpTable = new Label();
            for (int i = 0; i < matches.length; i++) {
                targetLabels[i] = targets[i].asLabel();
            }
            final GPR tagRegister = tagGeneralRegister().as();
            final int numElements = numberOfTableElements();
            assert numElements <= 0xFFFFFFFFL;
            final SPARCEirRegister.GeneralPurpose scratchEirRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);

            final int imm = minMatchValue();
            if (imm != 0) {
                if (SPARCEirOperation.isSimm13(imm)) {
                    emitter.assembler().sub(tagRegister, imm, tagRegister);
                } else {
                    emitter.assembler().setsw(imm, scratchEirRegister.as());
                    emitter.assembler().sub(tagRegister, scratchEirRegister.as(), tagRegister);
                }
            }
            if (SPARCEirOperation.isSimm13(numElements)) {
                emitter.assembler().cmp(tagRegister, numElements);
            } else {
                final GPR numElementsRegister = scratchEirRegister.as();
                emitter.assembler().setuw(numElements, numElementsRegister);
                emitter.assembler().cmp(tagRegister, numElementsRegister);
            }
            final GPR tableRegister = scratchEirRegister.as();
            final GPR targetAddressRegister = tableRegister;
            final Label here = new Label();
            emitter.assembler().bindLabel(here);
            emitter.assembler().rd(StateRegister.PC, tableRegister);
            // Branch on unsigned greater than or equal.
            emitter.assembler().bcc(AnnulBit.NO_A, defaultTargetLabel);
            // complete loading of the jump table in the delay slot, regardless of whether we're taking the branch.
            emitter.assembler().addcc(tableRegister, here, jumpTable, tableRegister);
            emitter.assembler().sll(tagRegister, 2, indexGeneralRegister().as());
            emitter.assembler().ldsw(tableRegister, indexGeneralRegister().as(), O7);
            emitter.assembler().jmp(targetAddressRegister, O7);
            // TODO: exploit delay slot
            emitter.assembler().nop(); // delay slot
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

        private void translateLookupBinarySearch(SPARCEirTargetEmitter emitter, int bottomIndex, int topIndex) {
            final SPARCEirRegister.GeneralPurpose scratchEirRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
            final GPR tagRegister = tagGeneralRegister().as();
            final int middleIndex = (bottomIndex + topIndex) >> 1;
            final int match = matches()[middleIndex].value().asInt();

            if (SPARCEirOperation.isSimm13(match)) {
                emitter.assembler().cmp(tagRegister, match);
            } else {
                final GPR matchRegister = scratchEirRegister.as();
                emitter.assembler().setuw(match, matchRegister);
                emitter.assembler().cmp(tagRegister, matchRegister);
            }
            emitter.assembler().be(AnnulBit.NO_A, targets[middleIndex].asLabel());
            emitter.assembler().nop(); // TODO -- exploit delay slot
            if (bottomIndex == topIndex) {
                branchToDefaultTarget(emitter);
            } else {
                final Label searchAbove = new Label();
                emitter.assembler().bg(AnnulBit.NO_A, searchAbove);
                emitter.assembler().nop(); // TODO -- exploit delay slot
                if (bottomIndex == middleIndex) {
                    branchToDefaultTarget(emitter);
                } else {
                    translateLookupBinarySearch(emitter, bottomIndex, middleIndex - 1);
                }
                emitter.assembler().bindLabel(searchAbove);
                if (topIndex == middleIndex) {
                    branchToDefaultTarget(emitter);
                }  else {
                    translateLookupBinarySearch(emitter, middleIndex + 1, topIndex);
                }
            }
        }

        @Override
        protected  void assembleLookupSwitch(SPARCEirTargetEmitter emitter) {
            translateLookupBinarySearch(emitter, 0, matches().length - 1);
        }

        @Override
        public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder(super.toString());

            if (indexRegister() != null) {
                result.append("; indexRegister=" + indexRegister());
            }

            return result.toString();
        }

    }
}
