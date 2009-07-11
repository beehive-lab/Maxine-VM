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
package com.sun.max.vm.compiler.eir.allocate.some;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.util.timer.Timer;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirTraceObserver.*;
import com.sun.max.vm.compiler.eir.allocate.*;

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


    protected EirSomeAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
    }

    private static final int LOOP_WEIGHT_FACTOR = 8;

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
                        weight *= (operand.instruction().block().loopNestingDepth() * LOOP_WEIGHT_FACTOR) + 1;
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
                final int stackSlotIndex = stackSlot.offset() / methodGeneration().abi.stackSlotSize();
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


    private static final Timer constantAllocationTimer = GlobalMetrics.newTimer("RegisterAllocation-ConstantAllocation", Clock.SYSTEM_MILLISECONDS);
    private static final Timer variableSplittingTimer = GlobalMetrics.newTimer("RegisterAllocation-VariableSplitting", Clock.SYSTEM_MILLISECONDS);
    private static final Timer resettingTimer = GlobalMetrics.newTimer("RegisterAllocation-Resetting", Clock.SYSTEM_MILLISECONDS);
    private static final Timer resetting2Timer = GlobalMetrics.newTimer("RegisterAllocation-Resetting2", Clock.SYSTEM_MILLISECONDS);
    private static final Timer liveRangeBuildingTimer = GlobalMetrics.newTimer("RegisterAllocation-LiveRangeBuilding", Clock.SYSTEM_MILLISECONDS);
    private static final Timer preallocatedVariablePropogationTimer = GlobalMetrics.newTimer("RegisterAllocation-PreallocatedVariablePropogation", Clock.SYSTEM_MILLISECONDS);
    private static final Timer interferencesTimer = GlobalMetrics.newTimer("RegisterAllocation-Interferences", Clock.SYSTEM_MILLISECONDS);
    private static final Timer weighTimer = GlobalMetrics.newTimer("RegisterAllocation-Weigh", Clock.SYSTEM_MILLISECONDS);
    private static final Timer assignRequiredTimer = GlobalMetrics.newTimer("RegisterAllocation-AssignRequired", Clock.SYSTEM_MILLISECONDS);
    private static final Timer allocateTimer = GlobalMetrics.newTimer("RegisterAllocation-Allocate", Clock.SYSTEM_MILLISECONDS);
    private static final Timer coalescingTimer = GlobalMetrics.newTimer("RegisterAllocation-Coalescing", Clock.SYSTEM_MILLISECONDS);
    private static final Timer trimTimer = GlobalMetrics.newTimer("RegisterAllocation-Trimming", Clock.SYSTEM_MILLISECONDS);
    private static final Timer rankTimer = GlobalMetrics.newTimer("RegisterAllocation-Ranking", Clock.SYSTEM_MILLISECONDS);

    @Override
    public void run() {
        constantAllocationTimer.start();
        methodGeneration().allocateConstants();
        constantAllocationTimer.stop();

        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);
        methodGeneration().notifyBeforeTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        variableSplittingTimer.start();
        splitVariables();
        variableSplittingTimer.stop();

        methodGeneration().notifyAfterTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        resettingTimer.start();
        final Pool<EirVariable> variablePool = methodGeneration().variablePool();
        final PoolSet<EirVariable> emptyVariableSet = PoolSet.noneOf(variablePool);
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (final EirInstruction<?, ?> instruction : block.instructions()) {
                instruction.resetLiveVariables(emptyVariableSet);
            }
        }
        resettingTimer.stop();

        resetting2Timer.start();
        for (EirVariable variable : methodGeneration().variables()) {
            variable.resetInterferingVariables(emptyVariableSet);
            variable.resetLiveRange();
        }
        resetting2Timer.stop();

        liveRangeBuildingTimer.start();
        determineLiveRanges();
        liveRangeBuildingTimer.stop();

        final PoolSet<EirVariable> variables = PoolSet.allOf(variablePool);

        preallocatedVariablePropogationTimer.start();
        propagatePreallocatedValues(variables);
        preallocatedVariablePropogationTimer.stop();

        interferencesTimer.start();
        determineInterferences(variables);
        interferencesTimer.stop();


        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.LIVE_RANGES);
        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);

        //EirLoopDetector.determineNestingDepths(methodGeneration().eirBlocks());
        weighTimer.start();
        weighOperands();
        weighTimer.stop();
        assignRequiredTimer.start();
        assignRequiredLocations(variables);
        assignRequiredTimer.stop();
        rankTimer.start();
        final EirVariable[] rankedVariables = rankVariables(variables);
        rankTimer.stop();
        allocateTimer.start();
        allocateVariables(rankedVariables);
        allocateTimer.stop();
        assert assertPlausibleCorrectness();
        coalescingTimer.start();
        coalesceVariables(rankedVariables);
        coalescingTimer.stop();
        assert assertPlausibleCorrectness();
        if (methodGeneration().eirGenerator.compilerScheme().vmConfiguration().debugging()) {
            trimTimer.start();
            trim();
            trimTimer.stop();
        }
    }
}
