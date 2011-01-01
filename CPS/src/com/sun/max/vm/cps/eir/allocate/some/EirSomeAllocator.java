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
package com.sun.max.vm.cps.eir.allocate.some;

import java.util.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirTraceObserver.*;
import com.sun.max.vm.cps.eir.allocate.*;

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
        final EirVariable[] array = variables.toArray(new EirVariable[variables.size()]);
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
        final EirRegister_Type register = Utils.cast(type, variable.location());
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
                final int stackSlotIndex = stackSlot.offset / methodGeneration().abi.stackSlotSize();
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
            final EirRegister_Type register = Utils.cast(type, location);
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
            EirOperand[] operands = new EirOperand[variable.operands().size()];
            int i = 0;
            for (EirOperand element : variable.operands()) {
                operands[i] = element;
                i++;
            }
            for (EirOperand operand : operands) {
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
    private boolean coalesce(final EirVariable acquirer, EirInstruction assignment, final EirVariable acquiree, LinkedIdentityHashSet<EirOperand> operands) {
        assert !acquirer.isInterferingWith(acquiree);
        if (isUnallocatableRegister(acquirer.location()) || acquiree.isLocationFixed() || !acquirer.isReferenceCompatibleWith(acquiree)) {
            return false;
        }

        if (acquirer.kind().isReference && acquiree.location() instanceof EirStackSlot && acquirer.location() instanceof EirStackSlot) {
            EirStackSlot s1 = (EirStackSlot) acquiree.location();
            EirStackSlot s2 = (EirStackSlot) acquirer.location();
            if (s1.purpose != s2.purpose) {
                // Reference parameters passed via the stack are copied to local
                // stack locations as the GC refmaps do not cover the parameter stack locations.
                // Normally this is ok as the parameter stack locations are in the frame
                // of the caller which will have them covered by a stack reference map.
                // However, if there is an adapter frame in between, then the parameter
                // stack locations are in a frame covered by no reference map.
                // This test prevents coalescing the copy of a parameter stack slot to
                // a local stack slot.
                return false;
            }
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
        EirOperand[] operandsCopy = new EirOperand[acquiree.operands().size()];
        int i = 0;
        for (EirOperand element : acquiree.operands()) {
            operandsCopy[i] = element;
            i++;
        }

        final EirOperand[] originalOperands = operandsCopy;
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
        final LinkedIdentityHashSet<EirOperand> operands = new LinkedIdentityHashSet<EirOperand>();
        for (EirOperand operand : v.operands()) {
            operands.add(operand);
        }
        while (!operands.isEmpty()) {
            final EirOperand operand = operands.getOne();
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
            while (i < block.instructions().size()) {
                if (block.instructions().get(i).isRedundant()) {
                    block.removeInstruction(i);
                } else {
                    i++;
                }
            }
        }
    }

    @Override
    public void run() {
        methodGeneration().allocateConstants();

        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);
        methodGeneration().notifyBeforeTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        splitVariables();

        methodGeneration().notifyAfterTransformation(methodGeneration().eirBlocks(), Transformation.VARIABLE_SPLITTING);

        final Pool<EirVariable> variablePool = methodGeneration().variablePool();
        final PoolSet<EirVariable> emptyVariableSet = PoolSet.noneOf(variablePool);
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (final EirInstruction<?, ?> instruction : block.instructions()) {
                instruction.resetLiveVariables(emptyVariableSet);
            }
        }

        for (EirVariable variable : methodGeneration().variables()) {
            variable.resetInterferingVariables(emptyVariableSet);
            variable.resetLiveRange();
        }

        determineLiveRanges();

        final PoolSet<EirVariable> variables = PoolSet.allOf(variablePool);

        propagatePreallocatedValues(variables);

        determineInterferences(variables);

        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.LIVE_RANGES);
        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.INTERFERENCE_GRAPH);

        //EirLoopDetector.determineNestingDepths(methodGeneration().eirBlocks());
        weighOperands();
        assignRequiredLocations(variables);
        final EirVariable[] rankedVariables = rankVariables(variables);
        allocateVariables(rankedVariables);
        assert assertPlausibleCorrectness();
        coalesceVariables(rankedVariables);
        assert assertPlausibleCorrectness();
        if (MaxineVM.isDebug()) {
            trim();
        }
    }
}
