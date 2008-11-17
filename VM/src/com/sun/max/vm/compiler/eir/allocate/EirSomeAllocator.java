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
package com.sun.max.vm.compiler.eir.allocate;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.profile.Metrics.Timer;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirTraceObserver.*;
import com.sun.max.vm.type.*;

/**
 * A medium speed, medium quality register allocator.
 *
 * 1. Allocate all constants.
 * 2. Split off all operands that cannot be allocated freely.
 *    These have a required location or do not allow stack slots.
 * 3. Allocate all variables with required locations.
 * 4. Allocate all variables that do not allow stack slots.
 * 5. Allocate all remaining variables.
 * 6. Coalesce variables, eliminating redundant assignments.
 * 7. If debugging, clean up some leftovers (filler, redundant try).
 *
 * Variable allocation is in ranking order.
 * Variables are ranked by the sum of "weights" of their operands.
 * Operand weights depend on operand effect and loop nesting depth.
 *
 * @author Bernd Mathiske
 */
public abstract class EirSomeAllocator<EirRegister_Type extends EirRegister> extends EirAllocator<EirRegister_Type> {

    protected abstract PoolSet<EirRegister_Type> noRegisters();

    protected abstract PoolSet<EirRegister_Type> allocatableIntegerRegisters();

    protected abstract PoolSet<EirRegister_Type> allocatableFloatingPointRegisters();

    protected EirSomeAllocator(EirMethodGeneration methodGeneration) {
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
        final EirInstruction assignment = methodGeneration().createAssignment(instruction.block(), destination.kind(), destination, constant);
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
        // Make a copy of the existing variables so that they iterated over while new variables are being created:
        final EirVariable[] variables = Sequence.Static.toArray(methodGeneration().variables(), EirVariable.class);
        for (EirVariable variable : variables) {
            if (!variable.isLocationFixed() && !variable.isSpillingPrevented() && variable.kind() != Kind.VOID) {
                final EirOperand[] operands = Sequence.Static.toArray(variable.operands(), EirOperand.class);
                for (EirOperand operand : operands) {
                    if (!operand.locationCategories().contains(EirLocationCategory.STACK_SLOT) ||
                                    operand.requiredLocation() != null ||
                                    operand.requiredRegister() != null ||
                                    (operand.eirValue() != null && operand.eirValue().isLocationFixed())) {
                        splitVariableAtOperand(variable, operand);
                    }
                }
            }
        }
    }

    private static final int _LOOP_WEIGHT_FACTOR = 8;

    private void weighOperands() {
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (EirInstruction instruction : block.instructions()) {
                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        int weight = 0;
                        switch (operand.effect()) {
                            case DEFINITION:
                                weight = 2;
                                break;
                            case USE:
                                weight = 3;
                                break;
                            case UPDATE:
                                weight = 4;
                                break;
                        }
                        weight *= (operand.instruction().block().loopNestingDepth() * _LOOP_WEIGHT_FACTOR) + 1;
                        operand.setWeight(weight);
                    }
                });
            }
        }
    }

    private EirLocation requiredLocation(EirVariable variable) {
        EirLocation location = null;
        for (EirOperand operand : variable.operands()) {
            if (operand.requiredLocation() != null) {
                assert location == null || operand.requiredLocation() == location;
                location = operand.requiredLocation();
            } else if (operand.requiredRegister() != null) {
                assert location == null || operand.requiredRegister() == location;
                location = operand.requiredRegister();
            }
        }
        return location;
    }

    private void assignRequiredLocations(PoolSet<EirVariable> variables) {
        for (EirVariable variable : methodGeneration().variables()) {
            if (variable.isLocationFixed()) {
                variables.remove(variable);
            } else {
                final EirLocation location = requiredLocation(variable);
                if (location != null) {
                    variable.setLocation(location);
                    variables.remove(variable);
                }
            }
        }
    }

    private EirVariable[] rankVariables(PoolSet<EirVariable> variables) {
        for (EirVariable variable : variables) {
            if (variable.weight() == 0) {
                int weight = 0;
                for (EirOperand operand : variable.operands()) {
                    weight += operand.weight();
                }
                variable.setWeight(weight);
            }
        }
        final EirVariable[] array = PoolSet.toArray(variables, EirVariable.class);
        java.util.Arrays.sort(array);
        return array;
    }

    /**
     * Allocates a free register from the specified register pool to the specified variable.
     * Architectures that alias registers depending on the width of data may have to override this method.
     *
     * @param variable
     * @param registers
     * @return null if no register can be allocated from the set to the variable, an EirRegister otherwise.
     */
    protected EirRegister_Type allocateRegisterFor(EirVariable variable, PoolSet<EirRegister_Type> registers) {
        if (registers.isEmpty()) {
            return null;
        }
        if (registers.isEmpty()) {
            return null;
        }
        return registers.removeOne();
    }

    /**
     * Remove register(s) allocated to an interfering variable from the specified set of available registers.
     * Architectures that alias registers depending on the width of data may have to override this method
     * to remove more than one registers off the set.
     *
     * @param interfering variable
     * @param available registers
     */
    protected void removeInterferingRegisters(EirVariable variable, PoolSet<EirRegister_Type> availableRegisters) {
        @JavacSyntax("type checker weakness")
        final Class<EirRegister_Type> type = null;
        final EirRegister_Type register = StaticLoophole.cast(type, variable.location());
        availableRegisters.remove(register);
    }

    private void allocateVariable(EirVariable variable, PoolSet<EirRegister_Type> registers) {
        final PoolSet<EirRegister_Type> availableRegisters = registers.clone();
        final BitSet stackSlots = new BitSet();
        for (EirVariable v : variable.interferingVariables()) {
            if (v.location() instanceof EirRegister) {
                removeInterferingRegisters(v, availableRegisters);
            } else if (v.location() instanceof EirStackSlot) {
                final EirStackSlot stackSlot = (EirStackSlot) v.location();
                final int stackSlotIndex = stackSlot.offset() / methodGeneration().abi().stackSlotSize();
                stackSlots.set(stackSlotIndex);
            }
        }
        final EirRegister_Type register = allocateRegisterFor(variable, availableRegisters);
        if (register == null) {
            variable.setLocation(methodGeneration().localStackSlotFromIndex(stackSlots.nextClearBit(0)));
        } else {
            variable.setLocation(register);
        }
    }

    private void allocateVariable(EirVariable variable) {
        if (variable.locationCategories().contains(EirLocationCategory.INTEGER_REGISTER)) {
            allocateVariable(variable, allocatableIntegerRegisters());
        } else if (variable.locationCategories().contains(EirLocationCategory.FLOATING_POINT_REGISTER)) {
            allocateVariable(variable, allocatableFloatingPointRegisters());
        } else {
            allocateVariable(variable, noRegisters());
        }
    }

    private void allocateVariables(final EirVariable[] variables) {
        for (EirVariable variable : variables) {
            if (!variable.locationCategories().contains(EirLocationCategory.STACK_SLOT)) {
                allocateVariable(variable);
            }
        }
        for (EirVariable variable : variables) {
            if (variable.location() == null) {
                allocateVariable(variable);
            }
        }
    }

    private void removeInstruction(EirInstruction instruction) {
        instruction.visitOperands(new EirOperand.Procedure() {
            public void run(EirOperand operand) {
                operand.clearEirValue();
            }
        });
        final EirFiller filler = new EirFiller(instruction.block());
        instruction.block().setInstruction(instruction.index(), filler);
    }

    private boolean mayUseLocation(EirVariable variable, EirLocation location) {
        if (variable.isLocationFixed() || !variable.locationCategories().contains(location.category())) {
            return false;
        }
        if (location.category() == EirLocationCategory.STACK_SLOT && variable.location().category() != EirLocationCategory.STACK_SLOT) {
            return false;
        }
        for (EirVariable v : variable.interferingVariables()) {
            if (v.location().equals(location)) {
                return false;
            }
        }
        if (requiredLocation(variable) != null) {
            return false;
        }
        return true;
    }

    private boolean isUnallocatableRegister(EirLocation location) {
        if (location instanceof EirRegister) {
            final Class<EirRegister_Type> type = null;
            final EirRegister_Type register = StaticLoophole.cast(type, location);
            return !allocatableIntegerRegisters().contains(register) && !allocatableFloatingPointRegisters().contains(register);
        }
        return false;
    }

    private void propagatePreallocatedValue(EirVariable variable, EirInstruction assignment, EirValue.Preallocated preallocatedValue, PoolSet<EirVariable> variables) {
        if (variables.contains(variable)) {
            if (requiredLocation(variable) != null || !variable.isReferenceCompatibleWith(preallocatedValue)) {
                return;
            }
            for (EirOperand operand : variable.operands()) {
                if (operand.effect() != EirOperand.Effect.USE && operand.instruction() != assignment) {
                    return;
                }
            }
            for (EirOperand definition : preallocatedValue.definitions()) {
                if (variable.liveRange().contains(definition.instruction())) {
                    return;
                }
            }
            for (EirOperand operand : Sequence.Static.toArray(variable.operands(), EirOperand.class)) {
                operand.setEirValue(preallocatedValue);
            }
            variable.setLocation(null);
            variable.resetLocationCategories();
            variables.remove(variable);
        }
        removeInstruction(assignment);
    }

    // TODO: propagate constants as well - then undo for some of them at the end, if often used and registers are available
    private void propagatePreallocatedValues(PoolSet<EirVariable> variables) {
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (EirInstruction instruction : block.instructions()) {
                if (instruction instanceof EirAssignment) {
                    final EirAssignment assignment = (EirAssignment) instruction;
                    if (assignment.sourceOperand().eirValue() instanceof EirValue.Preallocated && assignment.destinationOperand().eirValue() instanceof EirVariable) {
                        propagatePreallocatedValue((EirVariable) assignment.destinationOperand().eirValue(),
                                                   instruction,
                                                   (EirValue.Preallocated) assignment.sourceOperand().eirValue(),
                                                   variables);
                    }
                }
            }
        }
    }

    /**
     * Attempts to coalesce two variables into one. The two variables comprise the source and target of a copy operation.
     * If coalescing is successful, then the copy operation is removed and all uses of the {@code acquiree} in the method
     * are replaced with {@code acquirer}.
     *
     * @param acquirer the variable that will subsume {@code acquiree} if coalescing is successful
     * @param assignment the copy operation that assigns {@code acquiree} to {@code acquirer} or vice versa
     * @param acquiree the variable that will be subsumed by {@code acquirer} if coalescing is successful
     * @param operands {@code acquiree}'s operands are copied into this set if coalescing is successful
     * @return true if {@code acquiree} is coalesced into {@code acquirer}, false otherwise
     */
    private boolean coalesce(final EirVariable acquirer, EirInstruction assignment, final EirVariable acquiree, GrowableDeterministicSet<EirOperand> operands) {
        assert !acquirer.isInterferingWith(acquiree);
        if (isUnallocatableRegister(acquirer.location()) || acquiree.isLocationFixed() || !acquirer.isReferenceCompatibleWith(acquiree)) {
            return false;
        }

        // Make a copy of acquiree's current interfering variables as this set is potentially modified by the following loop
        final PoolSet<EirVariable> originalInterferingVariables = acquiree.interferingVariables().clone();
        for (EirVariable v : originalInterferingVariables) {
            v.beNotInterferingWith(acquiree);
            v.beInterferingWith(acquirer);
        }

        // Replace all the instances of 'acquiree' with 'acquirer' in the live variable set of each instruction at which 'acquiree' is live
        acquiree.liveRange().forAllLiveInstructions(new EirInstruction.Procedure() {
            public void run(EirInstruction instruction) {
                instruction.removeLiveVariable(acquiree);
                instruction.addLiveVariable(acquirer);
            }
        });

        // Merge the live range of 'acquiree' into 'acquirer'
        acquirer.liveRange().add(acquiree.liveRange());

        // Remove the copy operation
        removeInstruction(assignment);

        // Make a copy of acquiree's current operands as this set is potentially modified by the following loop
        final EirOperand[] originalOperands = Sequence.Static.toArray(acquiree.operands(), EirOperand.class);
        for (EirOperand operand : originalOperands) {
            operands.add(operand);
            operand.setEirValue(acquirer);
        }

        acquiree.setLocation(null);
        acquirer.resetLocationCategories();
        return true;
    }

    private void coalesceVariable(EirVariable variable) {
        EirVariable v = variable;
        final VariableDeterministicSet<EirOperand> operands = new LinkedIdentityHashSet<EirOperand>();
        for (EirOperand operand : v.operands()) {
            operands.add(operand);
        }
        while (!operands.isEmpty()) {
            final EirOperand operand = operands.first();
            operands.remove(operand);

            final EirInstruction instruction = operand.instruction();
            assert instruction.block().instructions().get(instruction.index()) == instruction;

            if (instruction instanceof EirAssignment) {
                final EirAssignment assignment = (EirAssignment) instruction;
                EirValue otherValue = assignment.destinationOperand().eirValue();
                if (otherValue == v) {
                    otherValue = assignment.sourceOperand().eirValue();
                } else {
                    assert assignment.sourceOperand().eirValue() == v;
                }

                if (otherValue instanceof EirVariable) {
                    final EirVariable otherVariable = (EirVariable) otherValue;
                    if (otherVariable == v) {
                        removeInstruction(instruction);
                        operands.remove(assignment.destinationOperand());
                        operands.remove(assignment.sourceOperand());
                    } else if (otherVariable.location().equals(v.location())) {
                        if (!coalesce(v, instruction, otherVariable, operands)) {
                            if (coalesce(otherVariable, instruction, v, operands)) {
                                v = otherVariable;
                            }
                        }
                    } else if (!v.isInterferingWith(otherVariable)) {
                        if (mayUseLocation(otherVariable, v.location())) {
                            if (!coalesce(v, instruction, otherVariable, operands)) {
                                if (mayUseLocation(v, otherVariable.location())) {
                                    if (coalesce(otherVariable, instruction, v, operands)) {
                                        v = otherVariable;
                                    }
                                }
                            }
                        } else if (mayUseLocation(v, otherVariable.location())) {
                            if (coalesce(otherVariable, instruction, v, operands)) {
                                v = otherVariable;
                            }
                        }
                    }
                }
            }
        }
    }

    private void coalesceVariables(EirVariable[] variables) {
        for (EirVariable variable : variables) {
            if (variable.location() != null) {
                coalesceVariable(variable);
            }
        }
    }

    private void trim() {
        for (EirBlock block : methodGeneration().eirBlocks()) {
            int i = 0;
            while (i < block.instructions().length()) {
                if (block.instructions().get(i).isRedundant()) {
                    block.removeInstruction(i);
                } else {
                    i++;
                }
            }
        }
    }


    private static final Timer _constantAllocationTimer = GlobalMetrics.newTimer("RegisterAllocation-ConstantAllocation", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _variableSplittingTimer = GlobalMetrics.newTimer("RegisterAllocation-VariableSplitting", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _resettingTimer = GlobalMetrics.newTimer("RegisterAllocation-Resetting", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _resetting2Timer = GlobalMetrics.newTimer("RegisterAllocation-Resetting2", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _liveRangeBuildingTimer = GlobalMetrics.newTimer("RegisterAllocation-LiveRangeBuilding", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _preallocatedVariablePropogationTimer = GlobalMetrics.newTimer("RegisterAllocation-PreallocatedVariablePropogation", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _interferencesTimer = GlobalMetrics.newTimer("RegisterAllocation-Interferences", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _weighTimer = GlobalMetrics.newTimer("RegisterAllocation-Weigh", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _assignRequiredTimer = GlobalMetrics.newTimer("RegisterAllocation-AssignRequired", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _allocateTimer = GlobalMetrics.newTimer("RegisterAllocation-Allocate", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _coalescingTimer = GlobalMetrics.newTimer("RegisterAllocation-Coalescing", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _trimTimer = GlobalMetrics.newTimer("RegisterAllocation-Trimming", Clock.SYSTEM_MILLISECONDS);
    private static final Timer _rankTimer = GlobalMetrics.newTimer("RegisterAllocation-Ranking", Clock.SYSTEM_MILLISECONDS);

    @Override
    public void run() {
        _constantAllocationTimer.start();
        allocateConstants();
        _constantAllocationTimer.stop();

        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);
        methodGeneration().notifyBeforeTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        _variableSplittingTimer.start();
        splitVariables();
        _variableSplittingTimer.stop();

        methodGeneration().notifyAfterTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        _resettingTimer.start();
        final Pool<EirVariable> variablePool = methodGeneration().variablePool();
        final PoolSet<EirVariable> emptyVariableSet = PoolSet.noneOf(variablePool);
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (final EirInstruction<?, ?> instruction : block.instructions()) {
                instruction.resetLiveVariables(emptyVariableSet);
            }
        }
        _resettingTimer.stop();

        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.LIVE_RANGES);
        _resetting2Timer.start();
        for (EirVariable variable : methodGeneration().variables()) {
            variable.resetInterferingVariables(emptyVariableSet);
            variable.resetLiveRange();
        }
        _resetting2Timer.stop();

        _liveRangeBuildingTimer.start();
        determineLiveRanges();
        _liveRangeBuildingTimer.stop();

        final PoolSet<EirVariable> variables = PoolSet.allOf(variablePool);

        _preallocatedVariablePropogationTimer.start();
        propagatePreallocatedValues(variables);
        _preallocatedVariablePropogationTimer.stop();

        _interferencesTimer.start();
        determineInterferences(variables);
        _interferencesTimer.stop();


        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.LIVE_RANGES);
        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);

        //EirLoopDetector.determineNestingDepths(methodGeneration().eirBlocks());
        _weighTimer.start();
        weighOperands();
        _weighTimer.stop();
        _assignRequiredTimer.start();
        assignRequiredLocations(variables);
        _assignRequiredTimer.stop();
        _rankTimer.start();
        final EirVariable[] rankedVariables = rankVariables(variables);
        _rankTimer.stop();
        _allocateTimer.start();
        allocateVariables(rankedVariables);
        _allocateTimer.stop();
        assert assertPlausibleCorrectness();
        _coalescingTimer.start();
        coalesceVariables(rankedVariables);
        _coalescingTimer.stop();
        assert assertPlausibleCorrectness();
        if (methodGeneration().eirGenerator().compilerScheme().vmConfiguration().debugging()) {
            _trimTimer.start();
            trim();
            _trimTimer.stop();
        }
    }
}
