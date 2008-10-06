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
/*VCSID=1e31bdf4-3bd1-4bdb-bb5c-5197d2e9d56a*/
package com.sun.max.vm.compiler.eir.amd64;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;
import static com.sun.max.vm.compiler.eir.amd64.AMD64EirRegister.General.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.AMD64EirTargetEmitter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirBinaryOperation extends AMD64EirUnaryOperation {

    public EirOperand destinationOperand() {
        return operand();
    }

    public EirValue destinationValue() {
        return operandValue();
    }

    public EirLocation destinationLocation() {
        return operandLocation();
    }

    public AMD64EirRegister.General destinationGeneralRegister() {
        return (AMD64EirRegister.General) destinationLocation();
    }

    public AMD64EirRegister.XMM destinationXMMRegister() {
        return (AMD64EirRegister.XMM) destinationLocation();
    }

    private final EirOperand _sourceOperand;

    public EirOperand sourceOperand() {
        return _sourceOperand;
    }

    public EirValue sourceValue() {
        return _sourceOperand.eirValue();
    }

    public EirLocation sourceLocation() {
        return _sourceOperand.location();
    }

    public AMD64EirRegister.General sourceGeneralRegister() {
        return (AMD64EirRegister.General) sourceLocation();
    }

    public AMD64EirRegister.XMM sourceXMMRegister() {
        return (AMD64EirRegister.XMM) sourceLocation();
    }

    public AMD64EirBinaryOperation(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, PoolSet<EirLocationCategory> destinationLocationCategories,
                                                   EirValue source, EirOperand.Effect sourceEffect, PoolSet<EirLocationCategory> sourceLocationCategories) {
        super(block, destination, destinationEffect, destinationLocationCategories);
        _sourceOperand = new EirOperand(this, sourceEffect, sourceLocationCategories);
        _sourceOperand.setEirValue(source);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(_sourceOperand);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "  " + destinationOperand() + ", " + sourceOperand();
    }

    public abstract static class Arithmetic extends AMD64EirBinaryOperation {

        protected Arithmetic(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, PoolSet<EirLocationCategory> destinationLocationCategories,
                                             EirValue source, EirOperand.Effect sourceEffect, PoolSet<EirLocationCategory> sourceLocationCategories) {
            super(block, destination, destinationEffect, destinationLocationCategories, source, sourceEffect, sourceLocationCategories);
        }

        public abstract static class General extends Arithmetic {
            protected General(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue source, EirOperand.Effect sourceEffect) {
                super(block, destination, destinationEffect, G_S, source, sourceEffect, G_I32_L_S);
            }

            protected abstract void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister);
            protected abstract void emit_G_I8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate);
            protected abstract void emit_G_I32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, int sourceImmediate);
            protected abstract void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel);
            protected abstract void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);
            protected abstract void emit_S8_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate);
            protected abstract void emit_S8_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, int sourceImmediate);
            protected abstract void emit_S8_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, AMD64EirRegister.General sourceRegister);
            protected abstract void emit_S32_I8(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate);
            protected abstract void emit_S32_I32(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, int sourceImmediate);
            protected abstract void emit_S32_G(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, AMD64EirRegister.General sourceRegister);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                switch (destinationLocation().category()) {
                    case INTEGER_REGISTER: {
                        final AMD64EirRegister.General destinationRegister = destinationGeneralRegister();
                        switch (sourceLocation().category()) {
                            case INTEGER_REGISTER: {
                                emit_G_G(emitter, destinationRegister, sourceGeneralRegister());
                                break;
                            }
                            case IMMEDIATE_32: {
                                final Value value = sourceLocation().asImmediate().value();
                                if (value.signedEffectiveWidth() == WordWidth.BITS_8) {
                                    emit_G_I8(emitter, destinationRegister, value.toByte());
                                } else {
                                    emit_G_I32(emitter, destinationRegister, value.toInt());
                                }
                                break;
                            }
                            case LITERAL: {
                                emit_G_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                                break;
                            }
                            case STACK_SLOT: {
                                final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                                if (source.isOffset8Bit()) {
                                    emit_G_S8(emitter, destinationRegister, source.base(), source.offset8());
                                } else {
                                    emit_G_S32(emitter, destinationRegister, source.base(), source.offset32());
                                }
                                break;
                            }
                            default: {
                                impossibleLocationCategory();
                                break;
                            }
                        }
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress destination = emitter.stackAddress(destinationLocation().asStackSlot());
                        if (destination.isOffset8Bit()) {
                            switch (sourceLocation().category()) {
                                case INTEGER_REGISTER: {
                                    emit_S8_G(emitter, destination.base(), destination.offset8(), sourceGeneralRegister());
                                    break;
                                }
                                case IMMEDIATE_32: {
                                    final Value value = sourceLocation().asImmediate().value();
                                    if (value.signedEffectiveWidth() == WordWidth.BITS_8) {
                                        emit_S8_I8(emitter, destination.base(), destination.offset8(), value.toByte());
                                    } else {
                                        emit_S8_I32(emitter, destination.base(), destination.offset8(), value.toInt());
                                    }
                                    break;
                                }
                                case LITERAL: {
                                    final Kind sourceKind = sourceLocation().asLiteral().value().kind();
                                    final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(sourceKind, sourceLocation());
                                    emit_S8_G(emitter, destination.base(), destination.offset8(), scratchRegister);
                                    break;
                                }
                                case STACK_SLOT: {
                                    final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(Kind.LONG, sourceLocation());
                                    emit_S8_G(emitter, destination.base(), destination.offset8(), scratchRegister);
                                    break;
                                }
                                default: {
                                    impossibleLocationCategory();
                                    break;
                                }
                            }
                        } else {
                            switch (sourceLocation().category()) {
                                case INTEGER_REGISTER: {
                                    emit_S32_G(emitter, destination.base(), destination.offset32(), sourceGeneralRegister());
                                    break;
                                }
                                case IMMEDIATE_32: {
                                    final Value value = sourceLocation().asImmediate().value();
                                    if (value.signedEffectiveWidth() == WordWidth.BITS_8) {
                                        emit_S32_I8(emitter, destination.base(), destination.offset32(), value.toByte());
                                    } else {
                                        emit_S32_I32(emitter, destination.base(), destination.offset32(), value.toInt());
                                    }
                                    break;
                                }
                                case LITERAL: {
                                    final Kind sourceKind = sourceLocation().asLiteral().value().kind();
                                    final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(sourceKind, sourceLocation());
                                    emit_S32_G(emitter, destination.base(), destination.offset32(), scratchRegister);
                                    break;
                                }
                                case STACK_SLOT: {
                                    final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(Kind.LONG, sourceLocation());
                                    emit_S32_G(emitter, destination.base(), destination.offset32(), scratchRegister);
                                    break;
                                }
                                default: {
                                    impossibleLocationCategory();
                                    break;
                                }
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

            public abstract static class RA extends General {
                protected RA(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue source, EirOperand.Effect sourceEffect) {
                    super(block, destination, destinationEffect, source, sourceEffect);
                    destinationOperand().setPreferredRegister(RAX);
                }

                protected abstract void emit_RA_I32(AMD64EirTargetEmitter emitter, int sourceImmediate);

                @Override
                public void emit(AMD64EirTargetEmitter emitter) {
                    if (destinationLocation() == AMD64EirRegister.General.RAX && destinationLocation().category() == EirLocationCategory.IMMEDIATE_32) {
                        emit_RA_I32(emitter, sourceLocation().asImmediate().value().asInt());
                    } else {
                        super.emit(emitter);
                    }
                }
            }
        }

        public abstract static class Shift extends Arithmetic {
            private final Kind _kind;

            protected Shift(EirBlock block, Kind kind, EirValue destination, EirValue source) {
                super(block, destination, EirOperand.Effect.UPDATE, G_S, source, EirOperand.Effect.USE, G_I32_I64);
                _kind = kind;
                sourceOperand().setRequiredRegister(RCX);
            }

            protected abstract void emit_G_CL(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister);
            protected abstract void emit_S8_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset);
            protected abstract void emit_S32_CL(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset);
            protected abstract void emit_G_1(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister);
            protected abstract void emit_S8_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset);
            protected abstract void emit_S32_1(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset);
            protected abstract void emit_G_I(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, byte sourceImmediate);
            protected abstract void emit_S8_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, byte destinationOffset, byte sourceImmediate);
            protected abstract void emit_S32_I(AMD64EirTargetEmitter emitter, AMD64IndirectRegister64 destinationBasePointer, int destinationOffset, byte sourceImmediate);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final EirLocation destinationLocation = destinationLocation();
                final EirLocation sourceLocation = sourceLocation();
                switch (sourceLocation.category()) {
                    case INTEGER_REGISTER: {
                        assert sourceGeneralRegister() == AMD64EirRegister.General.RCX;
                        switch (destinationLocation.category()) {
                            case INTEGER_REGISTER: {
                                emit_G_CL(emitter, destinationGeneralRegister());
                                break;
                            }
                            case STACK_SLOT: {
                                final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                                if (destination.isOffset8Bit()) {
                                    emit_S8_CL(emitter, destination.base(), destination.offset8());
                                } else {
                                    emit_S32_CL(emitter, destination.base(), destination.offset32());
                                }
                                break;
                            }
                            default: {
                                impossibleLocationCategory();
                                break;
                            }
                        }
                        break;
                    }
                    case IMMEDIATE_32:
                    case IMMEDIATE_64: {
                        final byte sourceImmediate = sourceLocation.asImmediate().value().unsignedToByte();
                        if (sourceImmediate % _kind.width().numberOfBits() == 1) {
                            switch (destinationLocation.category()) {
                                case INTEGER_REGISTER: {
                                    emit_G_1(emitter, destinationGeneralRegister());
                                    break;
                                }
                                case STACK_SLOT: {
                                    final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                                    if (destination.isOffset8Bit()) {
                                        emit_S8_1(emitter, destination.base(), destination.offset8());
                                    } else {
                                        emit_S32_1(emitter, destination.base(), destination.offset32());
                                    }
                                    break;
                                }
                                default: {
                                    impossibleLocationCategory();
                                    break;
                                }
                            }
                        } else {
                            switch (destinationLocation.category()) {
                                case INTEGER_REGISTER: {
                                    emit_G_I(emitter, destinationGeneralRegister(), sourceImmediate);
                                    break;
                                }
                                case STACK_SLOT: {
                                    final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                                    if (destination.isOffset8Bit()) {
                                        emit_S8_I(emitter, destination.base(), destination.offset8(), sourceImmediate);
                                    } else {
                                        emit_S32_I(emitter, destination.base(), destination.offset32(), sourceImmediate);
                                    }
                                    break;
                                }
                                default: {
                                    impossibleLocationCategory();
                                    break;
                                }
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
        }

        public abstract static class XMM extends Arithmetic {
            protected XMM(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue source, EirOperand.Effect sourceEffect) {
                super(block, destination, destinationEffect, F, source, sourceEffect, F_L_S);
            }

            protected abstract void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister);
            protected abstract void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLabel);
            protected abstract void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
                switch (sourceLocation().category()) {
                    case FLOATING_POINT_REGISTER: {
                        emit_X_X(emitter, destinationRegister, sourceXMMRegister().as());
                        break;
                    }
                    case LITERAL: {
                        emit_X_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                        if (source.isOffset8Bit()) {
                            emit_X_S8(emitter, destinationRegister, source.base(), source.offset8());
                        } else {
                            emit_X_S32(emitter, destinationRegister, source.base(), source.offset32());
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
    }

    public abstract static class XMM128 extends Arithmetic {
        protected XMM128(EirBlock block, EirValue destination, EirOperand.Effect destinationEffect, EirValue source, EirOperand.Effect sourceEffect) {
            super(block, destination, destinationEffect, F, source, sourceEffect, F);
        }

        protected abstract void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister);

        @Override
        public void emit(AMD64EirTargetEmitter emitter) {
            final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
            switch (sourceLocation().category()) {
                case FLOATING_POINT_REGISTER: {
                    emit_X_X(emitter, destinationRegister, sourceXMMRegister().as());
                    break;
                }
                default: {
                    impossibleLocationCategory();
                    break;
                }
            }
        }
    }

    public abstract static class Move extends AMD64EirBinaryOperation {
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

        public abstract static class GeneralToGeneral extends Move {
            protected GeneralToGeneral(EirBlock block, EirValue destination, EirValue source) {
                super(block, destination, G, source, G_L_S);
            }

            protected abstract void emit_G_G(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64EirRegister.General sourceRegister);
            protected abstract void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel);
            protected abstract void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final AMD64EirRegister.General destinationRegister = destinationGeneralRegister();
                switch (sourceLocation().category()) {
                    case INTEGER_REGISTER:
                        emit_G_G(emitter, destinationRegister, sourceGeneralRegister());
                        break;
                    case LITERAL:
                        emit_G_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                        break;
                    case STACK_SLOT:
                        final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                        if (source.isOffset8Bit()) {
                            emit_G_S8(emitter, destinationRegister, source.base(), source.offset8());
                        } else {
                            emit_G_S32(emitter, destinationRegister, source.base(), source.offset32());
                        }
                        break;
                    default:
                        impossibleLocationCategory();
                        break;
                }
            }
        }

        public abstract static class GeneralToXMM extends Move {
            protected GeneralToXMM(EirBlock block, EirValue destination, EirValue source) {
                super(block, destination, F, source, G_L_S);
            }

            protected abstract void emit_X_G(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64EirRegister.General sourceRegister);
            protected abstract void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel);
            protected abstract void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
                switch (sourceLocation().category()) {
                    case INTEGER_REGISTER:
                        emit_X_G(emitter, destinationRegister, sourceGeneralRegister());
                        break;
                    case LITERAL:
                        emit_X_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                        break;
                    case STACK_SLOT:
                        final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                        if (source.isOffset8Bit()) {
                            emit_X_S8(emitter, destinationRegister, source.base(), source.offset8());
                        } else {
                            emit_X_S32(emitter, destinationRegister, source.base(), source.offset32());
                        }
                        break;
                    default:
                        impossibleLocationCategory();
                        break;
                }
            }
        }

        public abstract static class XMMToXMM extends Move {
            protected XMMToXMM(EirBlock block, EirValue destination, EirValue source) {
                super(block, destination, F, source, F_L_S);
            }

            protected abstract void emit_X_X(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64XMMRegister sourceRegister);
            protected abstract void emit_X_L(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, Label sourceLiteralLabel);
            protected abstract void emit_X_S8(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_X_S32(AMD64EirTargetEmitter emitter, AMD64XMMRegister destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final AMD64XMMRegister destinationRegister = destinationXMMRegister().as();
                switch (sourceLocation().category()) {
                    case FLOATING_POINT_REGISTER:
                        emit_X_X(emitter, destinationRegister, sourceXMMRegister().as());
                        break;
                    case LITERAL:
                        emit_X_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                        break;
                    case STACK_SLOT:
                        final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                        if (source.isOffset8Bit()) {
                            emit_X_S8(emitter, destinationRegister, source.base(), source.offset8());
                        } else {
                            emit_X_S32(emitter, destinationRegister, source.base(), source.offset32());
                        }
                        break;
                    default:
                        impossibleLocationCategory();
                        break;
                }
            }
        }

        public abstract static class XMMToGeneral extends Move {
            protected XMMToGeneral(EirBlock block, EirValue destination, EirValue source) {
                super(block, destination, G, source, F_L_S);
            }

            protected abstract void emit_G_X(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64XMMRegister sourceRegister);
            protected abstract void emit_G_L(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, Label sourceLiteralLabel);
            protected abstract void emit_G_S8(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, byte sourceOffset);
            protected abstract void emit_G_S32(AMD64EirTargetEmitter emitter, AMD64EirRegister.General destinationRegister, AMD64IndirectRegister64 sourceBasePointer, int sourceOffset);

            @Override
            public void emit(AMD64EirTargetEmitter emitter) {
                final AMD64EirRegister.General destinationRegister = destinationGeneralRegister();
                switch (sourceLocation().category()) {
                    case FLOATING_POINT_REGISTER:
                        emit_G_X(emitter, destinationRegister, sourceXMMRegister().as());
                        break;
                    case LITERAL:
                        emit_G_L(emitter, destinationRegister, sourceLocation().asLiteral().asLabel());
                        break;
                    case STACK_SLOT:
                        final StackAddress source = emitter.stackAddress(sourceLocation().asStackSlot());
                        if (source.isOffset8Bit()) {
                            emit_G_S8(emitter, destinationRegister, source.base(), source.offset8());
                        } else {
                            emit_G_S32(emitter, destinationRegister, source.base(), source.offset32());
                        }
                        break;
                    default:
                        impossibleLocationCategory();
                        break;
                }
            }
        }
    }

}
