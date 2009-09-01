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

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.EirAssignment.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirMethodGeneration {

    public final EirGenerator eirGenerator;
    public final EirABI abi;
    public final MemoryModel memoryModel;
    public final EirLiteralPool literalPool = new EirLiteralPool();
    /**
     * Specifies if the generated code uses one shared epilogue for all return points.
     */
    public final boolean usesSharedEpilogue;

    protected abstract EirInstruction createJump(EirBlock eirBlock, EirBlock toBlock);

    public void addJump(EirBlock eirBlock, EirBlock targetBlock) {
        targetBlock.addPredecessor(eirBlock);
        eirBlock.appendInstruction(createJump(eirBlock, targetBlock));
    }

    private final EirValue[] integerRegisterRoleValues;

    public EirValue integerRegisterRoleValue(VMRegister.Role registerRole) {
        return integerRegisterRoleValues[registerRole.ordinal()];
    }

    private final EirValue[] floatingPointRegisterRoleValues;

    public EirValue floatingPointRegisterRoleValue(VMRegister.Role registerRole) {
        return floatingPointRegisterRoleValues[registerRole.ordinal()];
    }

    private EirVariable[] registerVariables;

    public EirVariable makeRegisterVariable(EirRegister register) {
        final int index = register.serial();
        if (registerVariables[index] == null) {
            registerVariables[index] = createEirVariable(register.kind());
            registerVariables[index].fixLocation(register);
        }
        return registerVariables[index];
    }

    protected EirMethodGeneration(EirGenerator eirGenerator, EirABI abi, boolean isTemplate, boolean usesSharedEpilogue) {
        this.eirGenerator = eirGenerator;
        this.abi = abi;
        this.memoryModel = eirGenerator.compilerScheme().vmConfiguration().platform().processorKind.processorModel.memoryModel;
        this.isTemplate = isTemplate;
        this.usesSharedEpilogue = usesSharedEpilogue;

        this.integerRegisterRoleValues = new EirValue[VMRegister.Role.VALUES.length()];
        this.floatingPointRegisterRoleValues = new EirValue[VMRegister.Role.VALUES.length()];
        for (VMRegister.Role role : VMRegister.Role.VALUES) {
            integerRegisterRoleValues[role.ordinal()] = preallocate(abi.integerRegisterActingAs(role), role.kind());
            floatingPointRegisterRoleValues[role.ordinal()] = preallocate(abi.floatingPointRegisterActingAs(role), role.kind());
        }

        this.registerVariables = new EirVariable[eirGenerator.eirABIsScheme().registerPool().length()];
    }

    public abstract MethodActor classMethodActor();

    public final void notifyBeforeTransformation(Object context, Object transform) {
        eirGenerator.notifyBeforeTransformation(eirMethod(), context, transform);
    }

    public final void notifyAfterTransformation(Object context, Object transform) {
        eirGenerator.notifyAfterTransformation(eirMethod(), context, transform);
    }

    private VariableSequence<EirBlock> eirBlocks = new ArrayListSequence<EirBlock>();

    private AppendableIndexedSequence<EirBlock> result;

    public IndexedSequence<EirBlock> eirBlocks() {
        if (result != null) {
            return result;
        }
        return eirBlocks;
    }

    private Pool<EirBlock> eirBlockPool;

    /**
     * (tw) Blocks may change during register allocation.
     */
    private void clearBlockPool() {
        eirBlockPool = null;
    }

    public Pool<EirBlock> eirBlockPool() {
        if (eirBlockPool == null) {
            eirBlockPool = new ArrayPool<EirBlock>(Sequence.Static.toArray(eirBlocks(), EirBlock.class));
        }
        return eirBlockPool;
    }

    protected abstract EirMethod eirMethod();

    public EirBlock createEirBlock(IrBlock.Role role) {
        final int serial = eirBlocks.length();
        final EirBlock eirBlock = new EirBlock(eirMethod(), role, serial);
        eirBlocks.append(eirBlock);
        clearBlockPool();
        return eirBlock;
    }

    /**
     * Slots in the frame of the method being generated. That is, the offsets of these slots are relative SP after it
     * has been adjusted upon entry to the method.
     */
    private final AppendableIndexedSequence<EirStackSlot> localStackSlots = new ArrayListSequence<EirStackSlot>();

    /**
     * Slots in the frame of the caller of method being generated. That is, the offsets of these slots are relative to
     * SP before it has been adjusted upon entry to the method.
     */
    private final AppendableIndexedSequence<EirStackSlot> parameterStackSlots = new ArrayListSequence<EirStackSlot>();

    public Sequence<EirStackSlot> allocatedStackSlots() {
        return localStackSlots;
    }

    public EirStackSlot allocateSpillStackSlot() {
        final EirStackSlot stackSlot = new EirStackSlot(EirStackSlot.Purpose.LOCAL, localStackSlots.length() * abi.stackSlotSize());
        localStackSlots.append(stackSlot);
        return stackSlot;
    }

    public int getLocalStackSlotCount() {
        return localStackSlots.length();
    }

    /**
     * Gets the size of the stack frame currently allocated used for local variables.
     */
    public int frameSize() {
        return abi.frameSize(localStackSlots.length());
    }

    public EirStackSlot localStackSlotFromIndex(int index) {
        if (index >= localStackSlots.length()) {
            // Fill in the missing stack slots
            for (int i = localStackSlots.length(); i <= index; i++) {
                localStackSlots.append(new EirStackSlot(EirStackSlot.Purpose.LOCAL, i * abi.stackSlotSize()));
            }
        }
        return localStackSlots.get(index);
    }

    private EirStackSlot canonicalizeStackSlot(EirStackSlot stackSlot, AppendableIndexedSequence<EirStackSlot> slots) {
        final int index = stackSlot.offset / abi.stackSlotSize();
        if (index >= slots.length()) {
            // Fill in the missing stack slots
            for (int i = slots.length(); i < index; i++) {
                slots.append(new EirStackSlot(stackSlot.purpose, i * abi.stackSlotSize()));
            }
            slots.append(stackSlot);
            return stackSlot;
        }
        return slots.get(index);
    }

    /**
     * Gets the canonical instance for a stack slot at a given offset.
     *
     * @param stackSlot specifies a stack slot offset
     * @return the canonical object representing the stack slot at {@code stackSlot.offset()}
     */
    public EirStackSlot canonicalizeStackSlot(EirStackSlot stackSlot) {
        final AppendableIndexedSequence<EirStackSlot> stackSlots = stackSlot.purpose == EirStackSlot.Purpose.PARAMETER ? parameterStackSlots : localStackSlots;
        return canonicalizeStackSlot(stackSlot, stackSlots);
    }

    private final GrowableMapping<EirLocation, EirValue> locationToValue = HashMapping.createEqualityMapping();

    public EirValue preallocate(EirLocation location, Kind kind) {
        if (location == null) {
            return null;
        }
        EirValue value = locationToValue.get(location);
        if (value == null) {
            value = new EirValue.Preallocated(location, kind);
        }
        return value;
    }

    private VariableSequence<EirVariable> variables = new ArrayListSequence<EirVariable>();

    private Pool<EirVariable> variablePool;

    /**
     * Gets the {@linkplain #createEirVariable(Kind) allocated variables} as a pool.
     *
     * <b>This method must not be called before all variable allocation has been performed. That is, there must not be a
     * call to {@link EirMethodGeneration#createEirVariable(Kind)} after a call to this method.</b>
     */
    public Pool<EirVariable> variablePool() {
        if (variablePool == null) {
            variablePool = new ArrayPool<EirVariable>(Sequence.Static.toArray(variables, EirVariable.class));
        }
        return variablePool;
    }

    public void clearVariablePool() {
        variablePool = null;
    }

    /**
     * Gets the set of variables that have been allocated.
     */
    public Sequence<EirVariable> variables() {
        return variables;
    }

    public void setVariables(IterableWithLength<EirVariable> variables) {
        assert variablePool == null : "can't allocate EIR variables once a variable pool exists";
        this.variables = new ArrayListSequence<EirVariable>(variables);

        for (int i = 0; i < variables.length(); i++) {
            this.variables.get(i).setSerial(i);
        }
    }

    public EirVariable createEirVariable(Kind kind) {
        assert variablePool == null : "can't allocate EIR variables once a variable pool exists";
        final int serial = variables.length();
        final EirVariable eirVariable = new EirVariable(eirGenerator.eirKind(kind), serial);
        variables.append(eirVariable);
        return eirVariable;
    }

    private final AppendableSequence<EirConstant> constants = new ArrayListSequence<EirConstant>();

    public Sequence<EirConstant> constants() {
        return constants;
    }

    public EirConstant createEirConstant(Value value) {
        final EirConstant constant = (value.kind() == Kind.REFERENCE) ? new EirConstant.Reference(value, constants.length()) : new EirConstant(value);
        constants.append(constant);
        return constant;
    }

    public EirMethodValue createEirMethodValue(ClassMethodActor classMethodActor) {
        return new EirMethodValue(classMethodActor);
    }

    public EirValue stackPointerVariable() {
        return integerRegisterRoleValue(VMRegister.Role.ABI_STACK_POINTER);
    }

    public EirValue framePointerVariable() {
        return integerRegisterRoleValue(VMRegister.Role.ABI_FRAME_POINTER);
    }

    public EirValue safepointLatchVariable() {
        return integerRegisterRoleValue(VMRegister.Role.SAFEPOINT_LATCH);
    }

    public abstract EirCall createCall(EirBlock eirBlock, EirABI eirAbi, EirValue resultValue, EirLocation resultLocation, EirValue function, EirValue[] arguments, EirLocation[] argumentLocations, boolean isNativeFunctionCall);

    public abstract EirCall createRuntimeCall(EirBlock eirBlock, EirABI eirAbi, EirValue resultValue, EirLocation resultLocation, EirValue function, EirValue[] arguments, EirLocation[] argumentLocations);

    public abstract EirInstruction createAssignment(EirBlock eirBlock, Kind kind, EirValue destination, EirValue source);

    public abstract EirSafepoint createSafepoint(EirBlock eirBlock);

    public EirGuardpoint createGuardpoint(EirBlock eirBlock) {
        return new EirGuardpoint(eirBlock);
    }

    // Used when one wants generated code to perform a jump at the end of
    // the generated code region instead of a return instruction. This is most
    // useful for generating templates of a JIT or an interpreter.
    private EirBlock eirEpilogueBlock;

    public EirBlock eirEpilogueBlock() {
        if (eirEpilogueBlock == null) {
            eirEpilogueBlock = createEirBlock(IrBlock.Role.NORMAL);
        }
        return eirEpilogueBlock;
    }

    public boolean hasEirEpilogueBlock() {
        return eirEpilogueBlock != null;
    }

    private EirEpilogue eirEpilogue;

    public void addResultValue(EirValue resultValue) {
        makeEpilogue();
        eirEpilogue.addResultValue(resultValue);
    }

    public void addEpilogueUse(EirValue useValue) {
        makeEpilogue();
        eirEpilogue.addUse(useValue);
    }

    public void addEpilogueStackSlotUse(EirValue useValue) {
        makeEpilogue();
        eirEpilogue.addStackSlotUse(useValue);
    }

    protected abstract EirEpilogue createEpilogueAndReturn(EirBlock eirBlock);

    public EirBlock makeEpilogue() {
        if (eirEpilogue == null) {
            eirEpilogue = createEpilogueAndReturn(eirEpilogueBlock());
        }
        return eirEpilogueBlock();
    }

    private EirBlock selectSuccessor(EirBlock block, PoolSet<EirBlock> rest) {
        final EirInstruction<?, ?> instruction = block.instructions().last();
        return instruction.selectSuccessorBlock(rest);
    }

    private EirBlock gatherUnconditionalSuccessors(EirBlock eirBlock, PoolSet<EirBlock> rest, AppendableSequence<EirBlock> blocks) {
        EirBlock block = eirBlock;
        while (rest.contains(block)) {
            rest.remove(block);
            blocks.append(block);
            if (block.instructions().last() instanceof EirJump) {
                final EirJump jump = (EirJump) block.instructions().last();
                block = jump.target();
            } else {
                return block;
            }
        }
        return selectSuccessor(block, rest);
    }

    private EirBlock selectUnconditionalPredecessor(EirBlock block, final PoolSet<EirBlock> rest) {
        for (EirBlock predecessor : block.predecessors()) {
            if (rest.contains(predecessor)) {
                if (predecessor.instructions().last() instanceof EirJump) {
                    return predecessor;
                }
            }
        }
        return null;
    }

    private void gatherUnconditionalPredecessors(EirBlock eirBlock, PoolSet<EirBlock> rest, PrependableSequence<EirBlock> blocks) {
        EirBlock block = eirBlock;
        while (rest.contains(eirBlock)) {
            blocks.prepend(block);
            rest.remove(block);
            block = selectUnconditionalPredecessor(block, rest);
        }
    }

    private void gatherSuccessors(EirBlock eirBlock, PoolSet<EirBlock> rest, AppendableSequence<EirBlock> blocks) {
        EirBlock block = eirBlock;
        while (rest.contains(block)) {
            rest.remove(block);
            blocks.append(block);
            block = selectSuccessor(block, rest);
        }
    }

    protected void rearrangeBlocks() {
        final int eirBlocksLength = eirBlocks.length();
        final Pool<EirBlock> blockPool = new IndexedSequencePool<EirBlock>(eirBlocks);
        final PoolSet<EirBlock> rest = PoolSet.noneOf(blockPool);
        result = new ArrayListSequence<EirBlock>();
        rest.addAll();

        final EirBlock head = gatherUnconditionalSuccessors(eirBlocks.first(), rest, result);

        PrependableSequence<EirBlock> tail = null;
        if (eirEpilogueBlock != null) {
            tail = new ArrayListSequence<EirBlock>();
            gatherUnconditionalPredecessors(eirEpilogueBlock, rest, tail);
        }
        gatherSuccessors(head, rest, result);

        for (EirBlock block : eirBlocks) {
            if (block.role() == IrBlock.Role.EXCEPTION_DISPATCHER) {
                final EirBlock last = gatherUnconditionalSuccessors(block, rest, result);
                gatherSuccessors(last, rest, result);
            }
        }
        for (EirBlock block : eirBlocks) {
            if (block.role() != IrBlock.Role.EXCEPTION_DISPATCHER) {
                final EirBlock last = gatherUnconditionalSuccessors(block, rest, result);
                gatherSuccessors(last, rest, result);
            }
        }

        if (tail != null) {
            AppendableSequence.Static.appendAll(result, tail);
        }

        int serial = 0;
        for (EirBlock block : result) {
            block.setSerial(serial);
            serial++;
        }
        assert eirBlocksLength == eirBlocks.length();
        eirBlocks = null;
    }

    private final boolean isTemplate;

    public boolean isTemplate() {
        return isTemplate;
    }

    public EirVariable splitVariableAtDefinition(EirVariable variable, EirOperand operand) {
        final EirVariable source = createEirVariable(variable.kind());
        splitVariableAtDefinition(variable, operand, source);
        return source;
    }

    public void splitVariableAtDefinition(EirVariable variable, EirOperand operand, EirVariable source) {
        assert source != null;
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirInstruction assignment = createAssignment(instruction.block(), variable.kind(), variable, source);
        ((EirAssignment) assignment).setType(Type.FIXED_SPLIT);
        introduceInstructionAfter(instruction, assignment);
        operand.setEirValue(source);
    }

    public EirVariable splitVariableAtUse(EirVariable variable, EirOperand operand) {
        final EirVariable destination = createEirVariable(variable.kind());
        splitVariableAtUse(variable, operand, destination);
        return destination;
    }

    public void splitVariableAtUse(EirVariable variable, EirOperand operand, EirVariable destination) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirInstruction assignment = createAssignment(instruction.block(), variable.kind(), destination, variable);
        ((EirAssignment) assignment).setType(Type.FIXED_SPLIT);
        introduceInstructionBefore(instruction, assignment);
        operand.setEirValue(destination);
    }

    public EirVariable splitVariableAtUpdate(EirVariable variable, EirOperand operand) {
        final EirVariable temporary = createEirVariable(variable.kind());
        splitVariableAtUpdate(variable, operand, temporary);
        return temporary;
    }

    public void splitVariableAtUpdate(EirVariable variable, EirOperand operand, EirVariable temporary) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirInstruction assignment1 = createAssignment(instruction.block(), variable.kind(), temporary, variable);
        ((EirAssignment) assignment1).setType(Type.FIXED_SPLIT);
        introduceInstructionBefore(instruction, assignment1);
        final EirInstruction assignment2 = createAssignment(instruction.block(), variable.kind(), variable, temporary);
        ((EirAssignment) assignment2).setType(Type.FIXED_SPLIT);
        introduceInstructionAfter(instruction, assignment2);
        operand.setEirValue(temporary);
    }

    public void introduceInstructionBefore(EirPosition position, EirInstruction instruction) {
        if (position.index() > 0) {
            final EirInstruction previousInstruction = position.block().instructions().get(position.index() - 1);
            if (previousInstruction.isRedundant()) {
                position.block().setInstruction(previousInstruction.index(), instruction);
                return;
            }
        }
        position.block().insertInstruction(position.index(), instruction);
    }

    public void introduceInstructionAfter(EirInstruction<?, ?> position, EirInstruction instruction) {
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

    public EirVariable splitVariableAtOperand(EirVariable variable, EirOperand operand) {
        final EirVariable newVariable = createEirVariable(variable.kind());
        splitVariableAtOperand(variable, operand, newVariable);
        return newVariable;
    }

    /**
     * Split the variables. The following patterns are generated: - use position: v1 = v; v1; v = v1; - update position:
     * v1 = v; v1 = v1; v = v1; - definition: v1 =; v = v1;
     *
     * @param variable
     * @param operand
     * @param newVariable
     */
    public void splitVariableAtOperandFully(EirVariable variable, EirOperand operand, EirVariable newVariable) {
        switch (operand.instruction().getEffect(variable)) {
            case DEFINITION:
                splitVariableAtDefinition(variable, operand, newVariable);
                break;
            case USE:
                splitVariableAtUpdate(variable, operand, newVariable);
                break;
            case UPDATE:
                splitVariableAtUpdate(variable, operand, newVariable);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void splitVariableAtOperand(EirVariable variable, EirOperand operand, EirVariable newVariable) {
        switch (operand.instruction().getEffect(variable)) {
            case DEFINITION:
                splitVariableAtDefinition(variable, operand, newVariable);
                break;
            case USE:
                splitVariableAtUse(variable, operand, newVariable);
                break;
            case UPDATE:
                splitVariableAtUpdate(variable, operand, newVariable);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    protected EirLocation getConstantLocation(Value value, EirLocationCategory category) {
        switch (category) {
            case INTEGER_REGISTER:
            case FLOATING_POINT_REGISTER:
                break;
            case IMMEDIATE_8:
            case IMMEDIATE_16:
            case IMMEDIATE_32:
            case IMMEDIATE_64:
                return new EirImmediate(category, value);
            case METHOD:
                break;
            case LITERAL:
                return literalPool.makeLiteral(value);
            default:
                break;
        }
        throw ProgramError.unexpected();
    }

    public void allocateConstants() {
        final MutableQueue<EirConstant> constants = new MutableQueue<EirConstant>();

        for (EirConstant constant : constants()) {
            constants.add(constant);
        }
        while (!constants.isEmpty()) {
            allocateConstant(constants.removeFirst(), constants);
        }
        assert assertConstantsAllocated();
    }

    private boolean assertConstantsAllocated() {
        for (EirBlock eirBlock : eirBlocks()) {
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

    private EirOperand splitConstantAtUse(EirConstant constant, EirOperand operand) {
        final EirInstruction<?, ?> instruction = operand.instruction();
        final EirVariable destination = createEirVariable(constant.kind());
        final EirInstruction assignment = createAssignment(instruction.block(), destination.kind(), destination, constant);
        ((EirAssignment) assignment).setType(Type.FIXED_SPLIT);
        introduceInstructionBefore(instruction, assignment);
        operand.setEirValue(destination);
        final EirAssignment a = (EirAssignment) assignment;
        return a.sourceOperand();
    }

    private void allocateConstant(EirConstant constant, MutableQueue<EirConstant> constants) {
        final EirLocationCategory[] categories = new EirLocationCategory[constant.operands().length()];
        final EirOperand[] operands = new EirOperand[constant.operands().length()];
        int i = 0;
        for (EirOperand operand : constant.operands()) {
            operands[i] = operand;
            i++;
        }

        i = 0;
        for (EirOperand operand : operands) {
            categories[i] = decideConstantLocationCategory(constant.value(), operand);
            assert categories[i] == null || operands[i].locationCategories().contains(categories[i]);
            assert operands[i].eirValue() == constant;
            operands[i].clearEirValue();
            i++;
        }

        assert constant.operands().length() == 0;

        for (i = 0; i < operands.length; i++) {
            if (categories[i] == null) {
                operands[i] = splitConstantAtUse(constant, operands[i]);
                categories[i] = decideConstantLocationCategory(constant.value(), operands[i]);
                assert operands[i].locationCategories().contains(categories[i]);
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
                    c = createEirConstant(constant.value());
                    constants.add(c);
                }
                c.setLocation(getConstantLocation(constant.value(), categories[i]));
                categoryToConstant[categoryIndex] = c;
            }
            operands[i].setEirValue(c);
        }
    }

    /**
     * @return location or null iff over-constrained
     */
    protected static EirLocationCategory decideConstantLocationCategory(Value value, EirOperand operand) {
        if (value.kind() != Kind.REFERENCE || value.isZero()) {
            final WordWidth width = value.signedEffectiveWidth();

            EirLocationCategory category = EirLocationCategory.immediateFromWordWidth(width);
            do {
                if (operand.locationCategories().contains(category)) {
                    return category;
                }
                category = category.next();
            } while (EirLocationCategory.I.contains(category));

        }
        if (operand.locationCategories().contains(EirLocationCategory.LITERAL)) {
            return EirLocationCategory.LITERAL;
        }
        return null;
    }

    /**
     * Clears variables without usages (operands).
     */
    public void clearEmptyVariables() {
        boolean variablesToRemove = false;
        for (EirVariable variable : this.variables) {
            if (variable.operands().length() == 0) {
                variablesToRemove = true;
                break;
            }
        }

        if (variablesToRemove) {
            final AppendableSequence<EirVariable> newVariables = new ArrayListSequence<EirVariable>(variables.length());

            for (EirVariable variable : this.variables) {
                if (variable.operands().length() > 0) {
                    newVariables.append(variable);
                }
            }

            this.setVariables(newVariables);
        }
    }
}
