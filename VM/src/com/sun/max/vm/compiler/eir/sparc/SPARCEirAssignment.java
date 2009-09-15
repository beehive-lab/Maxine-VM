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
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Assignments from one SPARC EIR location to another.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public class SPARCEirAssignment extends SPARCEirBinaryOperation.Move implements SPARCEirInstruction, EirAssignment, SPARCEirBinaryOperation.GeneralBinaryOperationEmitter {

    private final Kind kind;

    public Kind kind() {
        return kind;
    }

    private Type type = Type.NORMAL;

    public Type type() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    private static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                return G_S;
            case FLOAT:
            case DOUBLE:
                return F_S;
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    private static PoolSet<EirLocationCategory> sourceLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case INT:
                return G_I32_L_S;
            case LONG:
            case WORD:
            case REFERENCE:
                return G_I32_I64_L_S;
            case FLOAT:
            case DOUBLE:
                return F_L_S;
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    /**
     * Indicates whether the integer value passed in parameter can be used as an immediate.
     *
     */
    public boolean canUseImmediate(int simm) {
        return isSimm13(simm);
    }

    private static SPARCEirRegister.GeneralPurpose offsetRegister(SPARCEirTargetEmitter emitter) {
        return  (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
    }

    public void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister) {
        emitter.assembler().or(sourceRegister.as(), G0, destinationRegister.as());
    }

    public void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int sourceImmediate) {
        try {
            emitter.assembler().setsw(sourceImmediate, destinationRegister.as());
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    /**
     * Emits load of a stack slot into a register (general or floating point).
     * @param emitter
     * @param destinationRegister
     */
    private void emit_S_GF(SPARCEirTargetEmitter emitter, EirRegister destinationRegister) {
        final SPARCEirTargetEmitter.StackAddress stackAddress = emitter.stackAddress(sourceLocation().asStackSlot());
        final int offset = stackAddress.offset() + JitStackFrameLayout.offsetInStackSlot(kind());

        if (canUseImmediate(offset)) {
            SPARCEirLoad.emit(emitter, kind(), SPARCEirRegister.GeneralPurpose.from(stackAddress.base()), offset, destinationRegister);
        } else {
            try {
                final SPARCEirRegister.GeneralPurpose offsetRegister = offsetRegister(emitter);
                emitter.assembler().setsw(offset, offsetRegister.as());
                SPARCEirLoad.emit(emitter, kind(), SPARCEirRegister.GeneralPurpose.from(stackAddress.base()), offsetRegister, destinationRegister);
            } catch (AssemblyException e) {
                ProgramError.unexpected();
            }
        }
    }

    private void emit_S_GF(SPARCEirTargetEmitter emitter) {
        emit_S_GF(emitter, (EirRegister) destinationLocation());
    }

    /**
     * Emit store to stack slot of a value from a register (general or floating point).
     * @param emitter
     * @param sourceRegister
     */
    private void emit_GF_S(SPARCEirTargetEmitter emitter, EirRegister sourceRegister) {
        final SPARCEirTargetEmitter.StackAddress stackAddress = emitter.stackAddress(destinationLocation().asStackSlot());
        final int offset = stackAddress.offset() + JitStackFrameLayout.offsetInStackSlot(kind());
        if (canUseImmediate(offset)) {
            SPARCEirStore.emit(emitter, kind(), sourceRegister, SPARCEirRegister.GeneralPurpose.from(stackAddress.base()), offset);
        } else {
            try {
                final SPARCEirRegister.GeneralPurpose offsetRegister = offsetRegister(emitter);
                emitter.assembler().setsw(offset, offsetRegister.as());
                SPARCEirStore.emit(emitter, kind(), sourceRegister, SPARCEirRegister.GeneralPurpose.from(stackAddress.base()), offsetRegister);
            } catch (AssemblyException e) {
                ProgramError.unexpected();
            }
        }
    }

    private void emit_GF_S(SPARCEirTargetEmitter emitter) {
        emit_GF_S(emitter,  (EirRegister) sourceLocation());
    }

    private void emit_L_GF(SPARCEirTargetEmitter emitter) {
        final SPARCEirRegister.GeneralPurpose literalBase =  ((SPARCEirABI) emitter.abi()).literalBaseRegister();
        switch(kind().asEnum) {
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().ldx(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), destinationGeneralRegister().as());
                break;
            case FLOAT:
                emitter.assembler().ld(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), destinationFloatingPointRegister().asSinglePrecision());
                break;
            case DOUBLE:
                emitter.assembler().ldd(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), destinationFloatingPointRegister().asDoublePrecision());
                break;
            default:
                impossibleLocationCategory();
        }
    }

    private void emit_L_S(SPARCEirTargetEmitter emitter) {
        final SPARCEirRegister.GeneralPurpose literalBase = ((SPARCEirABI) emitter.abi()).literalBaseRegister();
        switch(kind().asEnum) {
            case LONG:
            case WORD:
            case REFERENCE: {
                final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.LONG);
                emitter.assembler().ldx(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), scratchRegister.as());
                emit_GF_S(emitter, scratchRegister);
                break;
            }
            case FLOAT: {
                final SPARCEirRegister.FloatingPoint scratchRegister = (SPARCEirRegister.FloatingPoint) emitter.abi().getScratchRegister(Kind.FLOAT);
                emitter.assembler().ld(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), scratchRegister.asSinglePrecision());
                emit_GF_S(emitter, scratchRegister);
                break;
            }
            case DOUBLE: {
                final SPARCEirRegister.FloatingPoint scratchRegister = (SPARCEirRegister.FloatingPoint) emitter.abi().getScratchRegister(Kind.DOUBLE);
                emitter.assembler().ldd(literalBase.as(), emitter.literalBaseLabel(), sourceLocation().asLiteral().asLabel(), scratchRegister.asDoublePrecision());
                emit_GF_S(emitter, scratchRegister);
                break;
            }
            default:
                impossibleLocationCategory();
        }
    }

    /**
     * Gets a scratch register for temporarily storing a stack slot.
     * The scratch register is already used for large stack offset, so we need a different integer scratch register if the slot is an integer/long type.
     * For float/double value, the abi's scratch register is free to use.
     * @param emitter
     * @param kind
     * @return
     */
    private static EirRegister stackSlotScratch(SPARCEirTargetEmitter emitter, Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                return SPARCEirRegister.GeneralPurpose.from(O7);
            case FLOAT:
                return  emitter.abi().getScratchRegister(Kind.FLOAT);
            case DOUBLE:
                return  emitter.abi().getScratchRegister(Kind.DOUBLE);
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    public SPARCEirAssignment(EirBlock block, Kind kind, EirValue destination, EirValue source) {
        super(block, destination, destinationLocationCategories(kind), source, sourceLocationCategories(kind));
        this.kind = kind;
    }

    /**
     * Emit assignment from stack to stack. Use an extra scratch register to temporarily hold the stack slot content.
     * @see stackSlotScratch
     * @param emitter
     */
    private void emit_S_S(SPARCEirTargetEmitter emitter) {
        final EirRegister temp = stackSlotScratch(emitter, kind());
        emit_S_GF(emitter, temp); // load source stack slot in temp
        emit_GF_S(emitter, temp); // store temp in destination stack slot.
    }

    /**
     * Emit assignment of an integer immediate to a stack slot.
     * @param emitter
     */
    private void emit_I_S(SPARCEirTargetEmitter emitter)  {
        final Value value = sourceLocation().asImmediate().value();
        if (value.isZero()) {
            emit_GF_S(emitter, SPARCEirRegister.GeneralPurpose.G0);
            return;
        }
        // load immediate in template, then store in stack
        final SPARCEirRegister.GeneralPurpose temp = (SPARCEirRegister.GeneralPurpose) stackSlotScratch(emitter, kind());
        emit_G_GI(emitter, this,  temp); // load immediate in temp
        emit_GF_S(emitter, temp); // store temp in stack
    }


    private void emit_G(SPARCEirTargetEmitter emitter) {
        switch (destinationLocation().category()) {
            case INTEGER_REGISTER: {
                switch (sourceLocation().category()) {
                    case IMMEDIATE_64:
                    case IMMEDIATE_32: {
                        final Value value = sourceLocation().asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().mov(G0, destinationGeneralRegister().as());
                            break;
                        }
                        // Otherwise, fall through
                    }
                    case INTEGER_REGISTER:
                        emit_G_GI(emitter, this);
                        break;
                    case LITERAL: {
                        emit_L_GF(emitter);
                        break;
                    }
                    case STACK_SLOT: {
                        emit_S_GF(emitter);
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                    }
                }
                break;
            }
            // Destination is stack slot
            case STACK_SLOT: {
                switch (sourceLocation().category()) {
                    case INTEGER_REGISTER:
                        emit_GF_S(emitter);
                        break;
                    case IMMEDIATE_64:
                    case IMMEDIATE_32:
                        emit_I_S(emitter);
                        break;
                    case LITERAL: {
                        emit_L_S(emitter);
                        break;
                    }
                    case STACK_SLOT:
                        emit_S_S(emitter);
                        break;
                    default:
                        impossibleLocationCategory();
                }
                break;
            }
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    private  void emit_F(SPARCEirTargetEmitter emitter) {
        switch (destinationLocation().category()) {
            case FLOATING_POINT_REGISTER: {
                switch (sourceLocation().category()) {
                    case FLOATING_POINT_REGISTER:
                        if (kind().isCategory1()) {
                            emitter.assembler().fmovs(sourceFloatingPointRegister().asSinglePrecision(), destinationFloatingPointRegister().asSinglePrecision());
                        } else {
                            emitter.assembler().fmovd(sourceFloatingPointRegister().asDoublePrecision(), destinationFloatingPointRegister().asDoublePrecision());
                        }
                        break;
                    case LITERAL: {
                        emit_L_GF(emitter);
                        break;
                    }
                    case STACK_SLOT:
                        emit_S_GF(emitter);
                        break;
                    default:
                        impossibleLocationCategory();
                }
                break;
            }
            // Destination is stack slot
            case STACK_SLOT: {
                switch (sourceLocation().category()) {
                    case FLOATING_POINT_REGISTER:
                        emit_GF_S(emitter);
                        break;
                    case LITERAL: {
                        emit_L_S(emitter);
                        break;
                    }
                    case STACK_SLOT:
                        emit_S_S(emitter);
                        break;
                    default: {
                        impossibleLocationCategory();
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
    public void emit(SPARCEirTargetEmitter emitter) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                emit_G(emitter);
                break;
            case FLOAT:
            case DOUBLE:
                emit_F(emitter);
                break;
            default:
                ProgramError.unknownCase();
        }
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(destinationOperand());
        visitor.run(sourceOperand());
    }

    @Override
    public String toString() {
        String result = "assign-" + kind + " " + destinationOperand() + " := " + sourceOperand();

        if (type() != Type.NORMAL) {
            result += " (" + type().toString() + ")";
        }

        return result;
    }

}
