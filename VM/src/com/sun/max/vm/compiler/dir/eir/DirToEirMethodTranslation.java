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
package com.sun.max.vm.compiler.dir.eir;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.util.timer.Timer;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirTraceObserver.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Aggregates various state required while translating one method from DIR to EIR form.
 *
 * The bulk of the translation is straight-forward,
 * mapping DIR instructions to sequences of EIR instructions.
 * Most of the complications have to do with ABI specifics and register allocation.
 *
 * @author Bernd Mathiske
 */
public abstract class DirToEirMethodTranslation extends EirMethodGeneration {

    private final EirRegister[] _calleeSavedEirRegisters;

    public final EirRegister[] calleeSavedEirRegisters() {
        return _calleeSavedEirRegisters;
    }

    private final EirVariable[] _calleeSavedEirVariables;

    public final EirVariable[] calleeSavedEirVariables() {
        return _calleeSavedEirVariables;
    }

    private final EirVariable[] _calleeRepositoryEirVariables;

    private final BitSet _isCalleeSavedParameter = new BitSet();

    public final BitSet isCalleeSavedParameter() {
        return _isCalleeSavedParameter;
    }

    private final EirLocation[] _parameterEirLocations;

    public final EirLocation[] parameterEirLocations() {
        return _parameterEirLocations;
    }

    private final EirVariable[] _eirParameters;

    public final EirVariable[] eirParameters() {
        return _eirParameters;
    }

    protected abstract EirPrologue createPrologue(EirBlock block);

    protected abstract EirInstruction createJump(EirBlock eirBlock, EirBlock toBlock);

    public void addJump(EirBlock eirBlock, EirBlock targetBlock) {
        targetBlock.addPredecessor(eirBlock);
        eirBlock.appendInstruction(createJump(eirBlock, targetBlock));
    }

    protected abstract EirInstruction createReturn(EirBlock eirBlock);

    protected EirInstruction createTrampolineExit(EirBlock eirBlock, boolean isStaticTrampoline) {
        return createReturn(eirBlock);
    }

    protected EirInstruction createTrapStubExit(EirBlock eirBlock) {
        return createReturn(eirBlock);
    }

    @Override
    public EirEpilogue createEpilogueAndReturn(EirBlock eirBlock) {
        for (int i = 0; i < _calleeSavedEirRegisters.length; i++) {
            eirBlock.appendInstruction(createAssignment(eirBlock, _calleeSavedEirRegisters[i].kind(), _calleeSavedEirVariables[i], _calleeRepositoryEirVariables[i]));
        }
        final EirEpilogue eirEpilogue = createEpilogue(eirBlock);
        eirBlock.appendInstruction(eirEpilogue);
        if (!isTemplate()) {
            if (_eirMethod.isTrampoline()) {
                final boolean isStaticTrampoline = ((TrampolineMethodActor) _eirMethod.classMethodActor()).invocation() == TRAMPOLINE.Invocation.STATIC;
                eirBlock.appendInstruction(createTrampolineExit(eirBlock, isStaticTrampoline));
            } else if (eirMethod().classMethodActor().isTrapStub()) {
                eirBlock.appendInstruction(createTrapStubExit(eirBlock));
            } else {
                eirBlock.appendInstruction(createReturn(eirBlock));
            }
        }
        return eirEpilogue;
    }

    private final boolean _requiresEpilogue;

    public boolean requiresEpilogue() {
        return _requiresEpilogue;
    }

    protected DirToEirMethodTranslation(EirGenerator eirGenerator, EirMethod eirMethod, DirMethod dirMethod) {
        super(eirGenerator, eirMethod.abi(), eirMethod.isTemplate());
        _requiresEpilogue = eirMethod.isTemplate() || eirMethod.isNative();
        _eirMethod = eirMethod;
        _dirMethod = dirMethod;

        // Location where translated method returns its result.
        _resultEirLocation = abi().getReturnLocation(eirMethod.classMethodActor().resultKind());

        final EirRegister safepointLatchRegister = abi().safepointLatchRegister();
        final EirVariable safepointLatchVariable = makeRegisterVariable(safepointLatchRegister);

        final EirVariable[] sharedEirVariables = new EirVariable[abi().registerPool().length()];
        sharedEirVariables[safepointLatchRegister.serial()] = safepointLatchVariable;


        final Kind[] parameterKinds = IrValue.Static.toKinds(dirMethod.parameters());
        _parameterEirLocations = abi().getParameterLocations(dirMethod.classMethodActor(), EirStackSlot.Purpose.PARAMETER, parameterKinds);

        _eirParameters = new EirVariable[_parameterEirLocations.length];
        for (int i = 0; i < _parameterEirLocations.length; i++) {
            _eirParameters[i] = dirToEirVariable(dirMethod.parameters()[i]);
            if (_parameterEirLocations[i] instanceof EirRegister) {
                final EirRegister eirRegister = (EirRegister) _parameterEirLocations[i];
                sharedEirVariables[eirRegister.serial()] = _eirParameters[i];
            }
        }

        if (eirMethod.isTrampoline()) {
            // Make all potential parameters of the trampoline's compilees callee-saved by the trampoline:
            _calleeSavedEirRegisters = new EirRegister[abi().integerParameterRegisters().length() + abi().floatingPointParameterRegisters().length()];
            int i = 0;
            for (Object register : abi().integerParameterRegisters()) {
                _calleeSavedEirRegisters[i++] = (EirRegister) register;
            }
            for (Object register : abi().floatingPointParameterRegisters()) {
                _calleeSavedEirRegisters[i++] = (EirRegister) register;
            }
        } else {
            final PoolSet<EirRegister> calleeSavedRegisters = StaticLoophole.cast(abi().calleeSavedRegisters());
            _calleeSavedEirRegisters = com.sun.max.lang.Arrays.from(EirRegister.class, calleeSavedRegisters);
        }

        _calleeSavedEirVariables = new EirVariable[_calleeSavedEirRegisters.length];
        _calleeRepositoryEirVariables = new EirVariable[_calleeSavedEirRegisters.length];
        for (int i = 0; i < _calleeSavedEirRegisters.length; i++) {
            final EirVariable sharedEirVariable = sharedEirVariables[_calleeSavedEirRegisters[i].serial()];
            if (sharedEirVariable != null) {
                _calleeSavedEirVariables[i] = sharedEirVariable;
                _isCalleeSavedParameter.set(i);
            } else {
                _calleeSavedEirVariables[i] = createEirVariable(_calleeSavedEirRegisters[i].kind());
            }
            _calleeRepositoryEirVariables[i] = createEirVariable(_calleeSavedEirRegisters[i].kind());
        }

        final EirBlock prologueBlock = createEirBlock(IrBlock.Role.NORMAL);
        final EirPrologue prologue = createPrologue(prologueBlock);
        prologueBlock.appendInstruction(prologue);
        for (int i = 0; i < _calleeSavedEirRegisters.length; i++) {
            prologueBlock.appendInstruction(createAssignment(prologueBlock, _calleeSavedEirRegisters[i].kind(), _calleeRepositoryEirVariables[i], _calleeSavedEirVariables[i]));
        }

        @JdtSyntax("ineffective type checker")
        final Class<PoolSet<EirRegister>> type = null;
        final PoolSet<EirRegister> callerSavedRegisters = StaticLoophole.cast(type, abi().callerSavedRegisters());
        if (callerSavedRegisters.contains(abi().framePointer())) {
            prologue.addDefinition(framePointerVariable());
            addEpilogueUse(framePointerVariable());
        }
        prologue.addDefinition(safepointLatchVariable);

        createBodyEirBlocks();

        addJump(prologueBlock, _dirToEirBlock.get(dirMethod.blocks().first()));
    }

    private final EirMethod _eirMethod;

    @Override
    public EirMethod eirMethod() {
        return _eirMethod;
    }

    @Override
    public ClassMethodActor classMethodActor() {
        return _eirMethod.classMethodActor();
    }

    private final DirMethod _dirMethod;

    private final EirLocation _resultEirLocation;

    public EirLocation resultEirLocation() {
        return _resultEirLocation;
    }

    protected final Map<DirBlock, EirBlock> _dirToEirBlock = new IdentityHashMap<DirBlock, EirBlock>();

    public EirBlock dirToEirBlock(DirBlock dirBlock) {
        return _dirToEirBlock.get(dirBlock);
    }

    protected void createBodyEirBlocks() {
        for (DirBlock dirBlock : _dirMethod.blocks()) {
            final EirBlock eirBlock = createEirBlock(dirBlock.role());
            _dirToEirBlock.put(dirBlock, eirBlock);
        }
    }

    private final Map<DirVariable, EirVariable> _dirToEirVariable = new IdentityHashMap<DirVariable, EirVariable>();

    private EirVariable dirToEirVariable(DirVariable dirVariable) {
        EirVariable eirVariable = _dirToEirVariable.get(dirVariable);
        if (eirVariable == null) {
            eirVariable = createEirVariable(dirVariable.kind());
            _dirToEirVariable.put(dirVariable, eirVariable);
        }
        return eirVariable;
    }

    private final Map<Value, EirConstant> _valueToEirConstant = new HashMap<Value, EirConstant>();

    public EirConstant dirToEirConstant(DirConstant dirConstant) {
        Value value = dirConstant.value();
        value = value.kind().toStackKind().convert(value); // we make no EIR constants smaller than INT

        EirConstant eirConstant = _valueToEirConstant.get(value);
        if (eirConstant == null) {
            eirConstant = createEirConstant(value);
            _valueToEirConstant.put(value, eirConstant);
        }
        return eirConstant;
    }

    private final Map<ClassMethodActor, EirMethodValue> _classMethodActorToEirMethodConstant = new IdentityHashMap<ClassMethodActor, EirMethodValue>();

    public EirValue dirToEirValue(DirValue dirValue) {
        if (dirValue == null) {
            return null;
        } else if (dirValue instanceof DirVariable) {
            return dirToEirVariable((DirVariable) dirValue);
        } else if (dirValue instanceof DirConstant) {
            return dirToEirConstant((DirConstant) dirValue);
        } else if (dirValue == DirValue.UNDEFINED) {
            return EirValue.UNDEFINED;
        } else if (dirValue instanceof DirBlock) {
            return dirToEirBlock((DirBlock) dirValue);
        } else {
            final DirMethodValue dirMethodValue = (DirMethodValue) dirValue;
            final ClassMethodActor classMethodActor = dirMethodValue.classMethodActor();
            return makeEirMethodValue(classMethodActor);
        }
    }

    public EirMethodValue makeEirMethodValue(ClassMethodActor classMethodActor) {
        EirMethodValue eirMethodValue = _classMethodActorToEirMethodConstant.get(classMethodActor);
        if (eirMethodValue == null) {
            eirMethodValue = createEirMethodValue(classMethodActor);
            _classMethodActorToEirMethodConstant.put(classMethodActor, eirMethodValue);
        }
        return eirMethodValue;
    }

    private EirValue[] dirToEirValues(DirValue[] dirValues) {
        return com.sun.max.lang.Arrays.map(dirValues, EirValue.class, new MapFunction<DirValue, EirValue>() {
            public EirValue map(DirValue dirValue) {
                return dirToEirValue(dirValue);
            }
        });
    }

    public EirJavaFrameDescriptor dirToEirJavaFrameDescriptor(DirJavaFrameDescriptor dirJavaFrameDescriptor,
                                                              EirInstruction instruction) {
        if (dirJavaFrameDescriptor == null) {
            return null;
        }
        return new EirJavaFrameDescriptor(instruction,
                                          dirToEirJavaFrameDescriptor(dirJavaFrameDescriptor.parent(), instruction),
                                          dirJavaFrameDescriptor.bytecodeLocation(),
                                          dirToEirValues(dirJavaFrameDescriptor.locals()),
                                          dirToEirValues(dirJavaFrameDescriptor.stackSlots()));
    }

    protected abstract DirToEirInstructionTranslation createInstructionTranslation(EirBlock eirBlock);

    private void translateBlock(DirBlock dirBlock, EirBlock eirBlock) {
        final DirToEirInstructionTranslation instructionTranslation = createInstructionTranslation(eirBlock);
        if (dirBlock instanceof DirCatchBlock) {
            final DirCatchBlock dirCatchBlock = (DirCatchBlock) dirBlock;
            final EirValue eirCatchParameter = dirToEirValue(dirCatchBlock.catchParameter());
            eirBlock.appendInstruction(new EirCatch(eirBlock, eirCatchParameter, eirGenerator().catchParameterLocation()));
        }
        for (DirInstruction dirInstruction : dirBlock.instructions()) {
            dirInstruction.acceptVisitor(instructionTranslation);
        }
    }

    private void translateDirBlocks() {
        for (DirBlock dirBlock : _dirMethod.blocks()) {
            final EirBlock eirBlock = dirToEirBlock(dirBlock);
            translateBlock(dirBlock, eirBlock);
        }
    }

    private static final Timer _registerAllocationTimer = GlobalMetrics.newTimer("RegisterAllocation", Clock.SYSTEM_MILLISECONDS);

    public void translateMethod() {
        // do the translation
        notifyBeforeTransformation(eirBlocks(), Transformation.INITIAL_EIR_CREATION);
        translateDirBlocks();
        notifyAfterTransformation(eirBlocks(), Transformation.INITIAL_EIR_CREATION);

        // perform register allocation
        _registerAllocationTimer.start();
        createAllocator(this).run();
        _registerAllocationTimer.stop();

        notifyBeforeTransformation(eirBlocks(), Transformation.BLOCK_LAYOUT);
        rearrangeBlocks();
        notifyAfterTransformation(eirBlocks(), Transformation.BLOCK_LAYOUT);
    }
}
