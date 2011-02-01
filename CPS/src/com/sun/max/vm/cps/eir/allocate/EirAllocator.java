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
package com.sun.max.vm.cps.eir.allocate;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirTraceObserver.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirAllocator<EirRegister_Type extends EirRegister> {

    protected abstract PoolSet<EirRegister_Type> noRegisters();
    protected abstract PoolSet<EirRegister_Type> allocatableIntegerRegisters();
    protected abstract PoolSet<EirRegister_Type> allocatableFloatingPointRegisters();

    private final EirMethodGeneration methodGeneration;

    public EirMethodGeneration methodGeneration() {
        return methodGeneration;
    }

    protected EirAllocator(EirMethodGeneration methodGeneration) {
        this.methodGeneration = methodGeneration;
    }

    public abstract void run();

    protected void splitVariables() {
        // Make a copy of the existing variables so that they iterated over while new variables are being created:
        final EirVariable[] variables = methodGeneration().variables().toArray(new EirVariable[methodGeneration().variables().size()]);
        for (EirVariable variable : variables) {
            if (!variable.isLocationFixed() && !variable.isSpillingPrevented() && variable.kind() != Kind.VOID) {
                EirOperand[] operands = new EirOperand[variable.operands().size()];
                int i = 0;
                for (EirOperand element : variable.operands()) {
                    operands[i] = element;
                    i++;
                }
                for (EirOperand operand : operands) {
                    if (!operand.locationCategories().contains(EirLocationCategory.STACK_SLOT) ||
                                    operand.requiredLocation() != null ||
                                    operand.requiredRegister() != null ||
                                    (operand.eirValue() != null && operand.eirValue().isLocationFixed())) {
                        methodGeneration().splitVariableAtOperand(variable, operand);
                    }
                }
            }
        }
    }

    private void gatherDefinitions() {
        for (EirBlock eirBlock : methodGeneration().eirBlocks()) {
            for (EirInstruction instruction : eirBlock.instructions()) {
                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        operand.recordDefinition();
                    }
                });
            }
        }
    }

    private void gatherUses() {
        for (EirBlock eirBlock : methodGeneration().eirBlocks()) {
            final List<EirInstruction> instructions = eirBlock.instructions();
            for (int i = instructions.size() - 1; i >= 0; i--) {
                instructions.get(i).visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        operand.recordUse();
                    }
                });
            }
        }
    }

    /**
     * Determines the live range for each EIR variable in the EIR control flow graph.
     */
    protected void determineLiveRanges() {
        gatherDefinitions();
        gatherUses();
    }

    private void recordLiveness(Iterable<EirVariable> variables) {
        for (final EirVariable variable : variables) {
            variable.liveRange().forAllLiveInstructions(new EirInstruction.Procedure() {
                public void run(EirInstruction instruction) {
                    instruction.addLiveVariable(variable);
                }
            });
        }
    }

    protected void determineInterferences(Iterable<EirVariable> variables) {
        recordLiveness(variables);
        for (EirVariable variable : variables) {
            variable.determineInterferences();
        }
    }

    protected boolean assertLiveRanges() {
        for (EirVariable variable : methodGeneration().variables()) {
            assert variable.isLiveRangeIntact();
        }
        return true;
    }

    protected boolean assertBookkeeping() {
        assert assertLiveRanges();
        for (EirBlock eirBlock : methodGeneration().eirBlocks()) {
            for (EirInstruction<?, ?> instruction : eirBlock.instructions()) {
                assert instruction.areLiveVariablesIntact(methodGeneration());
            }
        }
        for (EirVariable variable : methodGeneration().variables()) {
            assert variable.areInterferencesIntact(methodGeneration());
        }
        return true;
    }

    protected boolean hasUniqueLocation(EirVariable variable, List<EirVariable> variables) {
        for (EirVariable v : variables) {
            if (v != variable && v.location() == variable.location()) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInterferingStackSlot(EirStackSlot stackSlot, EirVariable variable) {
        for (EirVariable interferingVariable : variable.interferingVariables()) {
            if (stackSlot.equals(interferingVariable.location())) {
                return true;
            }
        }
        return false;
    }

    protected void allocateStackSlot(EirVariable variable) {
        for (EirStackSlot stackSlot : methodGeneration().allocatedStackSlots()) {
            if (!isInterferingStackSlot(stackSlot, variable)) {
                variable.setLocation(stackSlot);
                return;
            }
        }
        variable.setLocation(methodGeneration().allocateSpillStackSlot());
    }

    protected boolean isLocationAvailable(EirVariable variable, EirLocation location) {
        if (location == null) {
            return false;
        }
        for (EirVariable interferingVariable : variable.interferingVariables()) {
            if (interferingVariable.location() == location) {
                return false;
            }
        }
        return true;
    }

    protected EirLocationCategory decideVariableLocationCategory(PoolSet<EirLocationCategory> categories) {
        if (categories.size() == 1) {
            return categories.iterator().next();
        }
        for (EirLocationCategory category : EirLocationCategory.VALUES) {
            if (categories.contains(category)) {
                return category;
            }
        }
        throw ProgramError.unexpected();
    }

    protected boolean assertPlausibleCorrectness() {
        final Pool<EirVariable> variablePool = methodGeneration().variablePool();
        final PoolSet<EirVariable> variables = PoolSet.noneOf(variablePool);
        final PoolSet<EirVariable> emptyVariableSet = variables.clone();
        for (EirBlock block : methodGeneration().eirBlocks()) {
            for (final EirInstruction<?, ?> instruction : block.instructions()) {
                instruction.resetLiveVariables(emptyVariableSet);
                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        if (operand.eirValue() instanceof EirVariable) {
                            variables.add((EirVariable) operand.eirValue());
                        }
                    }
                });
            }
        }
        for (EirVariable variable : methodGeneration().variables()) {
            variable.resetLiveRange();
            variable.resetInterferingVariables(emptyVariableSet);
        }
        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.VERIFY_LIVE_RANGES);
        determineLiveRanges();
        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.VERIFY_LIVE_RANGES);

        methodGeneration().notifyBeforeTransformation(methodGeneration().variables(), Transformation.VERIFY_INTERFERENCE_GRAPH);
        determineInterferences(variables);
        methodGeneration().notifyAfterTransformation(methodGeneration().variables(), Transformation.VERIFY_INTERFERENCE_GRAPH);

        for (final EirBlock block : methodGeneration().eirBlocks()) {
            for (final EirInstruction instruction : block.instructions()) {
                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {
                        assert operand.location() != null;
                        assert operand.locationCategories().contains(operand.location().category());
                        if (operand.eirValue() instanceof EirVariable) {
                            final EirVariable variable = (EirVariable) operand.eirValue();
                            if (operand.requiredLocation() != null) {
                                assert variable.location().equals(operand.requiredLocation());
                            }
                            if (operand.requiredRegister() != null) {
                                assert !EirLocationCategory.R.contains(variable.location().category()) ||
                                       variable.location() == operand.requiredRegister();
                            }
                        }
                    }
                });
            }
        }
        for (EirVariable variable : variables) {
            for (EirVariable v : variable.interferingVariables()) {
                assert v.location() != variable.location() : "Inferfering variables " + v + " and " + variable + " have been assigned the same location";
            }
        }
        return true;
    }
}
