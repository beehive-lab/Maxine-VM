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

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirOperand.*;
import com.sun.max.vm.type.*;

/**
 * SPARC Eir Binary Operation.
 * Binary operation can operate on two source operands that can be both distinct from the destination operand. The destination operand is always a register.
 * The first source operand is always a register that may be distinct from the the destination register. The second operand is either a register (that may be different from the destination register) or
 * an immediate value. SPARC support different width of immediate value, depending on the operation (e.g., arithmetic typically supports 13 bits, condition move on condition code supports
 * 11 bits, condition move on register value supports 10 bits, etc...).
 * Any immediate value with a width larger than the supported limit is loaded first in the scratch register specified in the target ABI.
 *
 * Binary operation sub-class based on the effect on their operand and the number of operands they use. They are categorized as follow:
 * Arithmetic: One destination operand (always a register), two source operands, a register, and a register or immediate. The effect on the destination is Define.
 *
 * By convention, if a binary operation specifies a single operand, the first source operand is implicitly the destination operand.
 * With this convention, an EIR binary operations may only have one of the following formats:
 * binop regSrc2, regSrc_or_imm, regDest
 * binop regSrc_or_imm, regDest
 * binop regSrc, regDest
 *
 * By convention, the reg_or_imm operand is always the right operand of the binary operation. The second operand (register only) is always the left operand.
 * When no second operand is specified, the destination operand is the left operand.
 *
 * @author Laurent Daynes
 */
public abstract class SPARCEirBinaryOperation extends SPARCEirUnaryOperation {

    public EirOperand destinationOperand() {
        return operand();
    }

    public EirValue destinationValue() {
        return operandValue();
    }

    public EirLocation destinationLocation() {
        return operandLocation();
    }

    public SPARCEirRegister.GeneralPurpose destinationGeneralRegister() {
        return operandGeneralRegister();
    }

    public SPARCEirRegister.FloatingPoint destinationFloatingPointRegister() {
        return operandFloatingPointRegister();
    }

    /**
     * The right operand of the binary operation. May be a register or an immediate.
     */
    private final EirOperand registerOrImmediateOperand;

    public EirOperand registerOrImmediateOperand() {
        return registerOrImmediateOperand;
    }

    public EirOperand sourceOperand() {
        return registerOrImmediateOperand;
    }
    public EirValue sourceValue() {
        return registerOrImmediateOperand.eirValue();
    }

    public EirLocation sourceLocation() {
        return registerOrImmediateOperand.location();
    }

    public EirOperand rightOperand() {
        return registerOrImmediateOperand;
    }

    public EirLocation rightLocation() {
        return registerOrImmediateOperand.location();
    }

    public SPARCEirRegister.GeneralPurpose rightGeneralRegister() {
        return sourceGeneralRegister();
    }
    public SPARCEirRegister.FloatingPoint rightFloatingPointRegister() {
        return (SPARCEirRegister.FloatingPoint) rightLocation();
    }

    public SPARCEirRegister.GeneralPurpose sourceGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) sourceLocation();
    }

    public SPARCEirRegister.FloatingPoint sourceFloatingPointRegister() {
        return (SPARCEirRegister.FloatingPoint) sourceLocation();
    }

    /**
     * The left operand of the binary operation. Always a register. Same as the destination operand if not specified.
     */
    private final EirOperand registerOperand;

    public EirOperand registerOperand() {
        return registerOperand;
    }

    public EirOperand leftOperand() {
        return registerOperand;
    }

    public EirValue leftValue() {
        return registerOperand.eirValue();
    }

    public EirLocation leftLocation() {
        return registerOperand.location();
    }

    public SPARCEirRegister.GeneralPurpose leftGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) leftLocation();
    }

    public SPARCEirRegister.FloatingPoint leftFloatingPointRegister() {
        return (SPARCEirRegister.FloatingPoint) leftLocation();
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        if (registerOperand != null && !registerOperand.equals(destinationOperand())) {
            visitor.run(registerOperand);
        }
        if (registerOrImmediateOperand != null) {
            visitor.run(registerOrImmediateOperand);
        }
    }

    /**
     * Constructor for a binary operation when the left operand is implicitly the destination operand.
     */
    public SPARCEirBinaryOperation(EirBlock block, EirValue destinationOperand, Effect destinationEffect,  PoolSet<EirLocationCategory> destinationCategory,
                    EirValue rightOperandValue, EirOperand.Effect rightOperandEffect, PoolSet<EirLocationCategory> rightLocationCategories) {
        super(block, destinationOperand, destinationEffect, destinationCategory);
        registerOperand =  destinationOperand();
        registerOrImmediateOperand =  new EirOperand(this, rightOperandEffect, rightLocationCategories);
        registerOrImmediateOperand.setEirValue(rightOperandValue);
    }

    public SPARCEirBinaryOperation(EirBlock block, EirValue destinationOperand, Effect destinationEffect,  PoolSet<EirLocationCategory> destinationCategory,
                    EirValue leftOperandValue, EirOperand.Effect leftOperandEffect, PoolSet<EirLocationCategory> leftOperandLocationCategories,
                    EirValue rightOperandValue, EirOperand.Effect rightOperandEffect, PoolSet<EirLocationCategory> rightLocationCategories) {
        super(block, destinationOperand, destinationEffect, destinationCategory);
        registerOperand = new EirOperand(this, leftOperandEffect, leftOperandLocationCategories);
        registerOperand.setEirValue(leftOperandValue);
        registerOrImmediateOperand =  new EirOperand(this, rightOperandEffect, rightLocationCategories);
        registerOrImmediateOperand.setEirValue(rightOperandValue);
    }

    public SPARCEirBinaryOperation(EirBlock block, EirValue destinationOperand, Effect destinationEffect,  PoolSet<EirLocationCategory> destinationCategory,
                    EirValue leftOperandValue, EirOperand.Effect leftOperandEffect, PoolSet<EirLocationCategory> leftOperandLocationCategories, EirOperand rightOperand) {
        super(block, destinationOperand, destinationEffect, destinationCategory);
        registerOperand = new EirOperand(this, leftOperandEffect, leftOperandLocationCategories);
        registerOperand.setEirValue(leftOperandValue);
        registerOrImmediateOperand =  rightOperand;
    }

    /**
     * Construct a binary operation with an specific destination operand. The destination operand may be null (e.g., compare operation).
     */
    public SPARCEirBinaryOperation(EirBlock block, EirOperand destinationOperand,
                    EirValue leftOperandValue, EirOperand.Effect leftOperandEffect, PoolSet<EirLocationCategory> leftOperandLocationCategories,
                    EirValue rightOperandValue, EirOperand.Effect rightOperandEffect, PoolSet<EirLocationCategory> rightLocationCategories) {
        super(block, destinationOperand);
        registerOperand = new EirOperand(this, leftOperandEffect, leftOperandLocationCategories);
        registerOperand.setEirValue(leftOperandValue);
        registerOrImmediateOperand =  new EirOperand(this, rightOperandEffect, rightLocationCategories);
        registerOrImmediateOperand.setEirValue(rightOperandValue);
    }

    /**
     * Helper interface implemented  by sub-classes of SPARCEirBinaryOperation with general register or immediate operands.
     * This help factoring out code in common to all these classes.
     *
     * @author Laurent Daynes
     */
    protected interface GeneralBinaryOperationEmitter {
        void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister);
        void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int simm);
        boolean canUseImmediate(int simm);
    }

    /**
     * Emit a binary operation with two operands: a register and a 32-bits immediate value.
     * @param emitter
     * @param generalBinOpEmitter
     * @param value
     */
    protected void emit_G_I32(SPARCEirTargetEmitter emitter, GeneralBinaryOperationEmitter generalBinOpEmitter, SPARCEirRegister.GeneralPurpose destinationRegister, int value) {
        if (generalBinOpEmitter.canUseImmediate(value)) {
            generalBinOpEmitter.emit_G_I(emitter, destinationRegister, value);
        } else {
            final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
            emitter.assembler().setsw(value, scratchRegister.as());
            generalBinOpEmitter.emit_G_G(emitter, destinationRegister, scratchRegister);
        }
    }
    /**
     * Common code for emitting a binary operation with two operands, a register, and a register or immediate.
     * @param emitter
     * @param generalBinOpEmitter
     */
    protected void emit_G_GI(SPARCEirTargetEmitter emitter, GeneralBinaryOperationEmitter generalBinOpEmitter) {
        if  (destinationLocation().category().equals(INTEGER_REGISTER)) {
            emit_G_GI(emitter, generalBinOpEmitter, destinationGeneralRegister());
        } else {
            impossibleLocationCategory();
        }
    }

    protected void emit_G_GI(SPARCEirTargetEmitter emitter, GeneralBinaryOperationEmitter generalBinOpEmitter, SPARCEirRegister.GeneralPurpose destinationRegister) {
        assert destinationRegister.category().equals(INTEGER_REGISTER);
        switch(sourceLocation().category()) {
            case INTEGER_REGISTER:
                generalBinOpEmitter.emit_G_G(emitter, destinationGeneralRegister(), sourceGeneralRegister());
                break;
            case IMMEDIATE_64:
                final long value = sourceLocation().asImmediate().value().toLong();
                if (Longs.numberOfEffectiveSignedBits(value) < Kind.INT.width.numberOfBits) {
                    emit_G_I32(emitter, generalBinOpEmitter, destinationRegister, (int) value);
                } else {
                    final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
                    // O7 is a secondary scratch register we can use if NOT in a leaf function.
                    emitter.assembler().setx(value, GPR.O7, scratchRegister.as());
                    generalBinOpEmitter.emit_G_G(emitter, destinationGeneralRegister(), scratchRegister);
                    break;
                }
                break;
            case IMMEDIATE_8:
            case IMMEDIATE_32:
                emit_G_I32(emitter, generalBinOpEmitter, destinationRegister, sourceLocation().asImmediate().value().toInt());
                break;
            default:
                impossibleLocationCategory();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "  " + destinationOperand() + " :=  " + leftOperand() + "," + rightOperand();
    }

    /**
     * Arithmetic binary operations always have a register as destination location, and a register or immediate as a source.
     * They may have a third source register, distinct from the destination, used to designate an alternate "first" operand for the operation.
     * That is, a binary operation is either of the form:
     *
     *  d = d op s1
     *
     *  d = s2 op s1
     *
     *  The right operand is always s1 and can be an register or an immediate. The left operand must always be a register.
     *
     * When using only two operands, the destination's effect is always UPDATE, and the source's is USE.
     * When using three operands, the destination effect is DEFINE, and the sources' are USE..
     *
     * @author Laurent Daynes
     */
    public abstract static class Arithmetic extends SPARCEirBinaryOperation {

        /**
         * Two-operand Arithmetic operation, wherein the destination is implicitly the first operand of the operation.
         * The destination is always a register with a UPDATE effect. The source is a register or immediate.
         * @param block
         * @param destination
         * @param destinationEffect
         * @param destinationLocationCategories
         * @param regOrImmSource
         * @param sourceEffect
         * @param sourceLocationCategories
         */
        protected Arithmetic(EirBlock block, EirValue destinationOperand, Effect destinationEffect,  PoolSet<EirLocationCategory> destinationCategory,
                        EirValue leftOperandValue, EirOperand.Effect leftOperandEffect, PoolSet<EirLocationCategory> leftOperandLocationCategories) {
            super(block, destinationOperand, destinationEffect, destinationCategory, leftOperandValue, leftOperandEffect, leftOperandLocationCategories);
        }

        /**
         *
         * @param block
         * @param destination
         * @param destinationEffect
         * @param destinationLocationCategories
         * @param regSource
         * @param regSourceEffect
         * @param regSourceLocationCategories
         * @param regOrImmSource
         * @param regOrImmSourceEffect
         * @param regOrImmSourceLocationCategories
         */
        protected Arithmetic(EirBlock block, EirValue destinationOperand, Effect destinationEffect,  PoolSet<EirLocationCategory> destinationCategory,
                        EirValue leftOperandValue, EirOperand.Effect leftOperandEffect, PoolSet<EirLocationCategory> leftOperandLocationCategories,
                        EirValue rightOperandValue, EirOperand.Effect rightOperandEffect, PoolSet<EirLocationCategory> rightLocationCategories) {
            super(block, destinationOperand, destinationEffect, destinationCategory, leftOperandValue, leftOperandEffect, leftOperandLocationCategories,
                            rightOperandValue, rightOperandEffect, rightLocationCategories);
        }

        public abstract static class General extends Arithmetic {
            protected General(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue rightSource, EirOperand.Effect rightSourceEffect) {
                super(block, destination, destinationEffect, G, rightSource, rightSourceEffect, G_I8_I32);
            }

            protected General(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect,
                            EirValue leftSource, EirOperand.Effect leftSourceEffect, EirValue rightSource, EirOperand.Effect rightSourceEffect) {
                super(block, destination, destinationEffect, G, leftSource, leftSourceEffect, G, rightSource, rightSourceEffect, G_I8_I32);
            }

            protected abstract void emit_G_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose leftRegister, SPARCEirRegister.GeneralPurpose rightRegister);
            protected abstract void emit_G_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister,  SPARCEirRegister.GeneralPurpose leftRegister, int simm13);

            @Override
            public void emit(SPARCEirTargetEmitter emitter) {
                if  (destinationLocation().category().equals(INTEGER_REGISTER) && leftLocation().category().equals(INTEGER_REGISTER)) {
                    switch(sourceLocation().category()) {
                        case INTEGER_REGISTER:
                            emit_G_G_G(emitter, destinationGeneralRegister(), leftGeneralRegister(), rightGeneralRegister());
                            break;
                        case IMMEDIATE_8:
                            emit_G_G_I(emitter, destinationGeneralRegister(), leftGeneralRegister(), rightLocation().asImmediate().value().toInt());
                            break;
                        case IMMEDIATE_32: {
                            final int simm13 = rightLocation().asImmediate().value().toInt();
                            if (isSimm13(simm13)) {
                                emit_G_G_I(emitter, destinationGeneralRegister(), leftGeneralRegister(), simm13);
                            } else {
                                final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
                                emitter.assembler().setsw(simm13, scratchRegister.as());
                                emit_G_G_G(emitter, destinationGeneralRegister(), leftGeneralRegister(), scratchRegister);
                            }
                            break;
                        }
                        default:
                            impossibleLocationCategory();
                    }
                } else {
                    // SPARC only allow for the destination and first source operand to be in register, and the second operand to be a register or an immediate. Anything else is incorrect
                    impossibleLocationCategory();
                }
            }
        }

        public abstract static class FloatingPoint extends Arithmetic {
            protected FloatingPoint(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue leftSource, EirOperand.Effect leftSourceEffect, EirValue rightSource, EirOperand.Effect rightSourceEffect) {
                super(block, destination, destinationEffect, F, leftSource, leftSourceEffect, F, rightSource, rightSourceEffect, F);
            }
            protected FloatingPoint(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue leftSource, EirOperand.Effect leftSourceEffect) {
                super(block, destination, destinationEffect, F, leftSource, leftSourceEffect, F);
            }

            protected abstract void emit_F_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint leftRegister, SPARCEirRegister.FloatingPoint rightRegister);
            protected void emit_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint destinationRegister, SPARCEirRegister.FloatingPoint rightRegister) {
                emit_F_F_F(emitter, destinationRegister, destinationRegister, rightRegister);
            }

            @Override
            public void emit(SPARCEirTargetEmitter emitter) {
                if  (destinationLocation().category().equals(FLOATING_POINT_REGISTER) && rightLocation().category().equals(FLOATING_POINT_REGISTER) && leftLocation().category().equals(FLOATING_POINT_REGISTER)) {
                    emit_F_F_F(emitter, destinationFloatingPointRegister(), leftFloatingPointRegister(), rightFloatingPointRegister());
                } else {
                    // SPARC only allow for the destination and first source operand to be in register, and the second operand to be a register or an immediate. Anything else is incorrect
                    impossibleLocationCategory();
                }
            }
        }
    }

    public abstract static class Compare extends SPARCEirBinaryOperation {
        private static final EirOperand nullOperand = null;

        protected Compare(EirBlock block, EirValue leftValue, PoolSet<EirLocationCategory> leftLocationCategories,
                        EirValue rightValue, PoolSet<EirLocationCategory> rightLocationCategories) {
            super(block, nullOperand, leftValue, EirOperand.Effect.USE, leftLocationCategories, rightValue, EirOperand.Effect.USE, rightLocationCategories);
        }

        public abstract static class FloatingPoint extends Compare {
            private FCCOperand selectedConditionCode;

            /**
             * Returns the condition code this floating-point comparison instruction is setting.
             * @see FCCOperand
             * @return
             */
            public FCCOperand selectedConditionCode() {
                return selectedConditionCode;
            }

            protected FloatingPoint(EirBlock block, EirValue leftValue, EirValue rightValue, FCCOperand fcc) {
                super(block, leftValue, F, rightValue, F);
                selectedConditionCode = fcc;
            }

            protected FloatingPoint(EirBlock block, EirValue leftValue, EirValue rightValue) {
                super(block, leftValue, F, rightValue, F);
                selectedConditionCode = FCCOperand.FCC0;
            }

            protected abstract void emit_F_F(SPARCEirTargetEmitter emitter, SPARCEirRegister.FloatingPoint operand1Register, SPARCEirRegister.FloatingPoint operand2Register);

            @Override
            public void emit(SPARCEirTargetEmitter emitter) {
                if  (leftLocation().category().equals(FLOATING_POINT_REGISTER) && rightLocation().category().equals(FLOATING_POINT_REGISTER)) {
                    emit_F_F(emitter, leftFloatingPointRegister(), rightFloatingPointRegister());
                } else {
                    // SPARC only allow for the destination and first source operand to be in register, and the second operand to be a register or an immediate. Anything else is incorrect
                    impossibleLocationCategory();
                }
            }
        }

        public abstract static class General extends Compare  {
            protected General(EirBlock block, EirValue leftValue, EirValue rightValue) {
                super(block, leftValue, G, rightValue, G_I);
            }

            /**
             * Indicates if the integer value passed in argument can be used as an immediate for this comparison instruction.
             * @param simm
             * @return
             */
            public boolean canUseImmediate(int simm) {
                return isSimm13(simm);
            }

            @Override
            public void emit(SPARCEirTargetEmitter emitter) {
                switch(sourceLocation().category()) {
                    case INTEGER_REGISTER:
                        emitter.assembler().cmp(leftGeneralRegister().as(), rightGeneralRegister().as());
                        break;
                    case IMMEDIATE_8:
                        emitter.assembler().cmp(leftGeneralRegister().as(), rightLocation().asImmediate().value().toInt());
                        break;
                    case IMMEDIATE_64:
                        final long value = sourceLocation().asImmediate().value().toLong();
                        if (Longs.numberOfEffectiveSignedBits(value) >= Kind.INT.width.numberOfBits) {
                            final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
                            // O7 is a secondary scratch register we can use if NOT in a leaf function.
                            emitter.assembler().setx(value, GPR.O7, scratchRegister.as());
                            emitter.assembler().cmp(leftGeneralRegister().as(), scratchRegister.as());
                            break;
                        }
                        // Otherwise, fall through
                    case IMMEDIATE_16:
                    case IMMEDIATE_32: {
                        final int simm13 = rightLocation().asImmediate().value().toInt();
                        if (canUseImmediate(simm13)) {
                            emitter.assembler().cmp(leftGeneralRegister().as(), rightLocation().asImmediate().value().toInt());
                        } else {
                            final Kind kind = rightLocation().asImmediate().value().kind();
                            final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(kind);
                            emitter.assembler().setsw(simm13, scratchRegister.as());
                            emitter.assembler().cmp(leftGeneralRegister().as(), scratchRegister.as());
                        }
                        break;
                    }
                    default:
                        impossibleLocationCategory();
                }
            }
        }
    }

    public abstract static class Move extends SPARCEirBinaryOperation {
        protected Move(EirBlock block, EirValue destination, PoolSet<EirLocationCategory> destinationLocationCategories,
                        EirValue source, PoolSet<EirLocationCategory> sourceLocationCategories) {
            super(block, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories, source, EirOperand.Effect.USE, sourceLocationCategories);
            if (source instanceof EirVariable) {
                final EirVariable variable = (EirVariable) source;
                if (variable.aliasedVariables() != null) {
                    final EirVariable destinationVariable = (EirVariable) destination;
                    for (EirVariable aliasedVariable : variable.aliasedVariables()) {
                        destinationVariable.setAliasedVariable(aliasedVariable);
                    }
                }
            }
        }
        protected Move(EirBlock block, EirValue destination, Effect effectOnDestination, PoolSet<EirLocationCategory> destinationLocationCategories,
                        EirValue source, PoolSet<EirLocationCategory> sourceLocationCategories) {
            super(block, destination, effectOnDestination, destinationLocationCategories, source, EirOperand.Effect.USE, sourceLocationCategories);
            if (source instanceof EirVariable) {
                final EirVariable variable = (EirVariable) source;
                if (variable.aliasedVariables() != null) {
                    final EirVariable destinationVariable = (EirVariable) destination;
                    for (EirVariable aliasedVariable : variable.aliasedVariables()) {
                        destinationVariable.setAliasedVariable(aliasedVariable);
                    }
                }
            }
        }

        public abstract static class GeneralToGeneral extends Move  implements GeneralBinaryOperationEmitter {

            protected GeneralToGeneral(EirBlock block, EirValue destination, EirValue source) {
                super(block, destination, G, source, G_I);
            }

            protected GeneralToGeneral(EirBlock block, EirValue destination, Effect effectOnDestination, EirValue source) {
                super(block, destination, effectOnDestination, G, source, G_I);
            }

            public abstract void emit_G_G(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, SPARCEirRegister.GeneralPurpose sourceRegister);
            public abstract void emit_G_I(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose destinationRegister, int simm);
            public boolean canUseImmediate(int simm) {
                return isSimm13(simm);
            }

            @Override
            public void emit(SPARCEirTargetEmitter emitter) {
                emit_G_GI(emitter, this);
            }

            public abstract static class OnCondition extends GeneralToGeneral {
                /**
                 * Condition code to be tested: integer (icc) or long (xcc), or floating (fcc0, fcc1, fcc2, fcc3).
                 */
                protected ConditionCodeRegister conditionCode;
                protected OnCondition(EirBlock block, ConditionCodeRegister conditionCode, EirValue destination, EirValue source) {
                    super(block, destination, Effect.UPDATE, source);
                    this.conditionCode = conditionCode;
                }
                @Override
                public boolean canUseImmediate(int simm) {
                    return isSimm11(simm);
                }

                public ConditionCodeRegister testedConditionCode() {
                    return conditionCode;
                }
            }
        }
    }
}

