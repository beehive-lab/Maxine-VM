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
package com.sun.max.vm.compiler.eir;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirInstruction<EirInstructionVisitor_Type extends EirInstructionVisitor,
                                     EirTargetEmitter_Type extends EirTargetEmitter>
                extends EirPosition implements IrInstruction {

    public interface Procedure {
        void run(EirInstruction instruction);
    }

    public EirInstruction(EirBlock block) {
        super(block);
    }

    public void swap(int index) {
        final EirInstruction other = block().instructions().get(index);
        block().setInstruction(index, this);
        block().setInstruction(index(), other);
    }

    public void backward() {
        swap(index() - 1);
    }

    public void forward() {
        swap(index() + 1);
    }

    public void remove() {
        block().removeInstruction(index());
    }

    public void move(int index) {
        if (index < index()) {
            for (int i = index(); i > index; i--) {
                block().setInstruction(i, block().instructions().get(i - 1));
            }
        } else {
            for (int i = index(); i < index; i++) {
                block().setInstruction(i, block().instructions().get(i + 1));
            }
        }
        block().setInstruction(index, this);
    }

    public abstract void acceptVisitor(EirInstructionVisitor_Type visitor) throws InvocationTargetException;

    public abstract void emit(EirTargetEmitter_Type emitter);

    public void visitOperands(EirOperand.Procedure procedure) {
    }

    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
    }

    /**
     * Select a preferred successor block that is also in the given set of eligible blocks.
     */
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    protected static void impossibleLocationCategory() {
        ProgramError.unexpected("impossible location category");
    }

    protected static void impossibleImmediateWidth() {
        ProgramError.unexpected("impossible immediate width");
    }

    private PoolSet<EirRegister> _registers;

    public boolean hasRegister(EirRegister register) {
        if (_registers == null) {
            return true;
        }
        return _registers.contains(register);
    }

    public void removeRegister(EirRegister register, Pool<EirRegister> registerPool) {
        if (_registers == null) {
            _registers = PoolSet.allOf(registerPool);
        }
        _registers.remove(register);
    }

    private PoolSet<EirVariable> _liveVariables;

    public void resetLiveVariables(PoolSet<EirVariable> emptyVariableSet) {
        _liveVariables = emptyVariableSet.clone();
    }

    public PoolSet<EirVariable> liveVariables() {
        return _liveVariables;
    }

    public void addLiveVariable(EirVariable variable) {
        _liveVariables.add(variable);
    }

    public void removeLiveVariable(EirVariable variable) {
        _liveVariables.remove(variable);
    }

    public void recordEffects() {
        visitOperands(new EirOperand.Procedure() {
            public void run(EirOperand operand) {
                switch (operand.effect()) {
                    case USE:
                        operand.eirValue().recordUse(operand);
                        break;
                    case UPDATE:
                        operand.eirValue().recordUse(operand);
                        operand.eirValue().recordDefinition(operand);
                        break;
                    case DEFINITION:
                        operand.eirValue().recordDefinition(operand);
                        break;
                }
            }
        });
    }

    public EirOperand.Effect getEffect(final EirValue eirValue) {
        final MutableInnerClassGlobal<EirOperand.Effect> effect = new MutableInnerClassGlobal<EirOperand.Effect>(null);
        visitOperands(new EirOperand.Procedure() {
            public void run(EirOperand operand) {
                if (operand.eirValue() == eirValue) {
                    switch (operand.effect()) {
                        case USE:
                            if (effect.value() == null) {
                                effect.setValue(EirOperand.Effect.USE);
                            } else if (effect.value() == EirOperand.Effect.DEFINITION) {
                                effect.setValue(EirOperand.Effect.UPDATE);
                                break;
                            }
                            break;
                        case UPDATE:
                            effect.setValue(EirOperand.Effect.UPDATE);
                            break;
                        case DEFINITION:
                            if (effect.value() == null) {
                                effect.setValue(EirOperand.Effect.DEFINITION);
                            } else if (effect.value() == EirOperand.Effect.USE) {
                                effect.setValue(EirOperand.Effect.UPDATE);
                                break;
                            }
                            break;
                    }
                }
            }
        });
        return effect.value();
    }

    public void replaceEirValue(final EirValue oldEirValue, final EirValue newEirValue) {
        visitOperands(new EirOperand.Procedure() {
            public void run(EirOperand operand) {
                if (operand.eirValue() == oldEirValue) {
                    operand.setEirValue(newEirValue);
                }
            }
        });
    }

    public boolean holdsVariable(final EirVariable variable) {
        final MutableInnerClassGlobal<Boolean> result = new MutableInnerClassGlobal<Boolean>(false);
        visitOperands(new EirOperand.Procedure() {
            public void run(EirOperand operand) {
                if (operand.eirValue() == variable) {
                    result.setValue(true);
                }
            }
        });
        return result.value();
    }

    public boolean isRedundant() {
        return false;
    }

    public boolean areLiveVariablesIntact(EirMethodGeneration methodGeneration) {
        final PoolSet<EirVariable> nonLiveVariables = PoolSet.allOf(methodGeneration.variablePool());
        for (EirVariable variable : liveVariables()) {
            assert variable.liveRange().contains(this);
            nonLiveVariables.remove(variable);
        }
        for (EirVariable variable : nonLiveVariables) {
            assert !variable.liveRange().contains(this);
        }
        return true;
    }

    private static final BitSet _emptyLocationFlags = new BitSet();

    private BitSet _locationFlags;

    public BitSet locationFlags() {
        if (_locationFlags == null) {
            assert _emptyLocationFlags.isEmpty();
            return _emptyLocationFlags;
        }
        return _locationFlags;
    }

    public void setLocationFlag(int index) {
        if (_locationFlags == null) {
            _locationFlags = new BitSet();
        }
        _locationFlags.set(index);
    }

    private static final EirOperand.Procedure _operandCleanupProcedure = new EirOperand.Procedure() {
        public void run(EirOperand operand) {
            operand.cleanup();
        }
    };

    public void cleanup() {
        _registers = null;
        _locationFlags = null;
        visitOperands(_operandCleanupProcedure);
    }

    public void cleanupAfterEmitting() {
        _liveVariables = null;
    }

    public static EirOperand.Effect xorDestinationEffect(EirValue destination, EirValue source) {
        if (destination == source) {
            return EirOperand.Effect.DEFINITION;
        }
        return EirOperand.Effect.UPDATE;
    }

    public static EirOperand.Effect xorSourceEffect(EirValue destination, EirValue source) {
        if (destination == source) {
            return EirOperand.Effect.DEFINITION;
        }
        return EirOperand.Effect.USE;
    }
}
