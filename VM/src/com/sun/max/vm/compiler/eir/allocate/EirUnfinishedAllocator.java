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
/*VCSID=5a063688-ba30-41f3-abe7-3cda9905e122*/
package com.sun.max.vm.compiler.eir.allocate;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Another unfinished register allocator.
 * 
 * @author Bernd Mathiske
 */
public abstract class EirUnfinishedAllocator<EirRegister_Type extends EirRegister> extends EirAllocator<EirRegister_Type> {

    protected abstract int locationToIndex(EirLocation location);

    protected abstract EirLocation locationFromIndex(int index);

    protected EirUnfinishedAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
    }

    private void introduceInstructionBefore(EirPosition position, EirInstruction instruction) {
        if (position.index() > 0) {
            final EirInstruction previousInstruction = position.block().instructions().get(position.index() - 1);
            if (previousInstruction.isRedundant()) {
                position.block().setInstruction(previousInstruction.index(), instruction);
                return;
            }
        }
        position.block().insertInstruction(position.index(), instruction);
    }

    private void introduceInstructionAfter(EirInstruction<?, ?> position, EirInstruction instruction) {
        final IndexedSequence<EirInstruction> instructions = position.block().instructions();
        final int nextIndex = position.index() + 1;
        if (nextIndex == instructions.length()) {
            position.block().appendInstruction(instruction);
            return;
        }
        final EirInstruction nextInstruction = instructions.get(nextIndex);
        if (nextInstruction.isRedundant()) {
            position.block().setInstruction(nextIndex, instruction);
            return;
        }
        position.block().insertInstruction(nextInstruction.index(), instruction);
    }

    private EirOperand splitConstantAtUse(EirConstant constant, EirOperand operand) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirVariable destination = methodGeneration().createEirVariable(constant.kind());
        final EirInstruction assignment =
            methodGeneration().createAssignment(instruction.block(), destination.kind(), destination, constant);
        introduceInstructionBefore(instruction, assignment);
        operand.setEirValue(destination);
        final EirAssignment a = (EirAssignment) assignment;
        return a.sourceOperand();
    }

    final MutableQueue<EirConstant> _constants = new MutableQueue<EirConstant>();

    private void allocateConstant(EirConstant constant) {
        final EirLocationCategory[] categories = new EirLocationCategory[constant.operands().length()];
        final EirOperand[] operands = new EirOperand[constant.operands().length()];
        int i = 0;
        for (EirOperand operand : constant.operands()) {
            operands[i] = operand;
            categories[i] = decideConstantLocationCategory(constant.value(), operand);
            i++;
        }
        for (i = 0; i < operands.length; i++) {
            if (categories[i] == null) {
                operands[i] = splitConstantAtUse(constant, operands[i]);
                categories[i] = decideConstantLocationCategory(constant.value(), operands[i]);
            }
        }

        EirConstant original = constant;
        final EirConstant[] categoryToConstant = new EirConstant[EirLocationCategory.VALUES.length()];
        for (i = 0; i < operands.length; i++) {
            final int categoryIndex = categories[i].ordinal();
            EirConstant c = categoryToConstant[categoryIndex];
            if (c == null) {
                if (original != null) {
                    c = original;
                    original = null;
                } else {
                    c = methodGeneration().createEirConstant(constant.value());
                    _constants.add(c);
                }
                c.setLocation(getConstantLocation(constant.value(), categories[i]));
                categoryToConstant[categoryIndex] = c;
            }
            if (c != constant) {
                operands[i].setEirValue(c);
            }
        }
    }

    private boolean assertConstantsAllocated() {
        for (EirBlock eirBlock : methodGeneration().eirBlocks()) {
            for (EirInstruction instruction : eirBlock.instructions()) {
                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        if (operand.eirValue().isConstant()) {
                            assert operand.eirValue().location() != null;
                        }
                    }
                });
            }
        }
        return true;
    }

    protected void allocateConstants() {
        for (EirConstant constant : methodGeneration().constants()) {
            _constants.add(constant);
        }
        while (!_constants.isEmpty()) {
            allocateConstant(_constants.removeFirst());
        }
        assert assertConstantsAllocated();
        setConstantsAllocated();
    }

    private EirVariable splitVariableAtDefinition(EirVariable variable, EirOperand operand) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirVariable source = methodGeneration().createEirVariable(variable.kind());
        final EirInstruction assignment = methodGeneration().createAssignment(instruction.block(), variable.kind(), variable, source);
        introduceInstructionAfter(instruction, assignment);
        operand.setEirValue(source);
        return source;
    }

    private EirVariable splitVariableAtUse(EirVariable variable, EirOperand operand) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirVariable destination = methodGeneration().createEirVariable(variable.kind());
        final EirInstruction assignment = methodGeneration().createAssignment(instruction.block(), variable.kind(), destination, variable);
        introduceInstructionBefore(instruction, assignment);
        operand.setEirValue(destination);
        return destination;
    }

    private EirVariable splitVariableAtUpdate(EirVariable variable, EirOperand operand) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirVariable temporary = methodGeneration().createEirVariable(variable.kind());
        final EirInstruction assignment1 = methodGeneration().createAssignment(instruction.block(), variable.kind(), temporary, variable);
        introduceInstructionBefore(instruction, assignment1);
        final EirInstruction assignment2 = methodGeneration().createAssignment(instruction.block(), variable.kind(), variable, temporary);
        introduceInstructionAfter(instruction, assignment2);
        operand.setEirValue(temporary);
        return temporary;
    }

    private EirVariable splitVariableAtOperand(EirVariable variable, EirOperand operand) {
        EirVariable result = null;
        switch (operand.instruction().getEffect(variable)) {
            case DEFINITION:
                result = splitVariableAtDefinition(variable, operand);
                break;
            case USE:
                result = splitVariableAtUse(variable, operand);
                break;
            case UPDATE:
                result = splitVariableAtUpdate(variable, operand);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        return result;
    }

    private void splitVariables() {
        final EirVariable[] variables = Sequence.Static.toArray(methodGeneration().variables(), EirVariable.class);
        for (EirVariable variable : variables) {
            if (!variable.isLocationFixed()) {
                final EirOperand[] operands = Sequence.Static.toArray(variable.operands(), EirOperand.class);
                for (EirOperand operand : operands) {
                    if (!operand.locationCategories().contains(EirLocationCategory.STACK_SLOT)) {
                        splitVariableAtOperand(variable, operand);
                    }
                }
            }
        }
    }

    private void recordLocationLiveRange(EirVariable variable, final EirLocation location) {
        variable.liveRange().visitInstructions(new EirInstruction.Procedure() {
            public void run(EirInstruction instruction) {
                instruction.setLocationFlag(locationToIndex(location));
            }
        });
    }

    private void setVariableLocation(EirVariable variable, EirLocation location) {
        variable.setLocation(location);
        recordLocationLiveRange(variable, location);
    }

    protected abstract BitSet unallocatableLocationFlags();
    protected abstract BitSet integerRegisterFlags();
    protected abstract BitSet floatingPointRegisterFlags();

    private void allocateVariable(EirVariable variable) {
        final BitSet locationFlags = new BitSet();
        locationFlags.or(unallocatableLocationFlags());
        for (EirOperand operand : variable.operands()) {
            locationFlags.or(operand.instruction().locationFlags());
            if (!operand.locationCategories().contains(EirLocationCategory.INTEGER_REGISTER)) {
                locationFlags.or(integerRegisterFlags());
            }
            if (!operand.locationCategories().contains(EirLocationCategory.FLOATING_POINT_REGISTER)) {
                locationFlags.or(floatingPointRegisterFlags());
            }
        }
        variable.liveRange().visitInstructions(new EirInstruction.Procedure() {
            public void run(EirInstruction instruction) {
                locationFlags.or(instruction.locationFlags());
            }
        });
        final int index = locationFlags.nextClearBit(0);
        setVariableLocation(variable, locationFromIndex(index));
    }

    private void allocateVariables() {
        for (EirVariable variable : methodGeneration().variables()) {
            if (variable.isLocationFixed()) {
                recordLocationLiveRange(variable, variable.location());
            } else {
                for (EirOperand operand : variable.operands()) {
                    if (requiredLocation(operand) != null) {
                        setVariableLocation(variable, requiredLocation(operand));
                    }
                }
            }
        }
        for (EirVariable variable : methodGeneration().variables()) {
            if (variable.location() == null && !variable.locationCategories().contains(EirLocationCategory.STACK_SLOT)) {
                allocateVariable(variable);
            }
        }
        for (EirVariable variable : methodGeneration().variables()) {
            if (variable.location() == null) {
                allocateVariable(variable);
            }
        }
    }

    private boolean hasNoUse(EirVariable variable, EirPosition position) {
        return false; // TODO
    }

    private boolean isUseless(EirAssignment assignment) {
        final EirValue destination = assignment.destinationOperand().eirValue();
        if (destination instanceof EirVariable) {
            final EirVariable variable = (EirVariable) destination;
            if (hasNoUse(variable, (EirPosition) assignment)) {
                return true;
            }
        }
        final EirValue source = assignment.sourceOperand().eirValue();
        return destination.location().equals(source.location());
    }

    private void removeUselessAssignments() {
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (EirInstruction instruction : block.instructions()) {
                if (instruction instanceof EirAssignment) {
                    final EirAssignment assignment = (EirAssignment) instruction;
                    if (isUseless(assignment)) {
                        block.setInstruction(instruction.index(), new EirFiller(block));
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        allocateConstants();
        splitVariables();
        determineLiveRanges();
        allocateVariables();
        assert assertPlausibleCorrectness();
        removeUselessAssignments();
    }
}
