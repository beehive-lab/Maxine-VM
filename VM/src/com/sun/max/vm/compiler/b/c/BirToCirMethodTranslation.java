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
/*VCSID=2e0c994d-aafa-4210-a555-2e8a2973abca*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;

/**
 * Translation from byte code of a given Java method to CIR.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BirToCirMethodTranslation {

    private final BirMethod _birMethod;
    private final CirGenerator _cirGenerator;

    private final CirVariableFactory _variableFactory;
    private final LocalVariableFactory _localVariableFactory;
    private final StackVariableFactory _stackVariableFactory;

    private final AppendableSequence<CirCall> _blockCalls = new LinkSequence<CirCall>();

    private final CirClosure _rootClosure;

    public CirVariable[] createParameters(Kind[] parameterKinds) {
        final CirVariable[] parameters = new CirVariable[parameterKinds.length + 2];
        int i = 0;
        final BytecodeLocation location = new BytecodeLocation(classMethodActor().compilee(), 0);
        while (i < parameterKinds.length) {
            parameters[i] = _variableFactory.createMethodParameter(parameterKinds[i], i, location);
            i++;
        }
        parameters[i++] = _variableFactory.normalContinuationParameter();
        parameters[i++] = _variableFactory.exceptionContinuationParameter();
        return parameters;
    }

    private static long _numberOfTranslations = 0;

    private final AppendableSequence<BlockState> _blockStates;
    private final BlockState[] _blockStateMap;

    private final Sequence<BirBlock> _birExceptionDispatchers;
    private final BlockState[] _exceptionDispatcherBlockStateMap;

    BirToCirMethodTranslation(BirMethod birMethod, CirVariableFactory variableFactory, CirGenerator cirGenerator) {
        _birMethod = birMethod;
        _variableFactory = variableFactory;
        _cirGenerator = cirGenerator;

        final CirVariable[] parameters = createParameters(birMethod.classMethodActor().getParameterKinds());

        _localVariableFactory = new LocalVariableFactory(_variableFactory, birMethod.maxLocals(), parameters);
        _stackVariableFactory = new StackVariableFactory(_variableFactory, birMethod.maxStack());

        _blockStates = new ArrayListSequence<BlockState>(birMethod.blocks().length());
        _blockStateMap = new BlockState[birMethod.code().length];
        final AppendableSequence<BirBlock> birExceptionDispatchers = new LinkSequence<BirBlock>();
        for (BirBlock birBlock : birMethod.blocks()) {
            if (birBlock.isReachable()) {
                final BlockState blockState = new BlockState(birBlock);
                _blockStates.append(blockState);
                for (int i = birBlock.bytecodeBlock().start(); i <= birBlock.bytecodeBlock().end(); i++) {
                    _blockStateMap[i] = blockState;
                }

                if (birBlock.role() == IrBlock.Role.EXCEPTION_DISPATCHER) {
                    birExceptionDispatchers.append(birBlock);
                }
            } else {
                ProgramWarning.message("unreachable block: " + birBlock + "  " + birMethod);
            }
        }
        _birExceptionDispatchers = birExceptionDispatchers;

        if (birMethod.exceptionDispatcherTable().isEmpty()) {
            _exceptionDispatcherBlockStateMap = null;
        } else {
            _exceptionDispatcherBlockStateMap = new BlockState[birMethod.code().length];
            for (ExceptionHandlerEntry exceptionDispatcherEntry : birMethod.exceptionDispatcherTable()) {
                final BlockState dispatcherBlockState = _blockStateMap[exceptionDispatcherEntry.handlerPosition()];
                assert dispatcherBlockState != null;
                for (int i = exceptionDispatcherEntry.startPosition(); i != exceptionDispatcherEntry.endPosition(); ++i) {
                    // This checks the invariant established by generating exception dispatchers in bytecode
                    ProgramError.check(_exceptionDispatcherBlockStateMap[i] == null, "no more than one exception handler should cover a bytecode address");
                    _exceptionDispatcherBlockStateMap[i] = dispatcherBlockState;
                }
            }
        }

        final CirCall rootCall = new CirCall(_blockStateMap[0].cirBlock());
        noteBlockCall(rootCall);
        _rootClosure = new CirClosure(rootCall, parameters);
    }

    public BirMethod birMethod() {
        return _birMethod;
    }

    public Sequence<BirBlock> birExceptionDispatchers() {
        return _birExceptionDispatchers;
    }

    public CirGenerator cirGenerator() {
        return _cirGenerator;
    }

    public ClassMethodActor classMethodActor() {
        return _birMethod.classMethodActor();
    }

    public CirVariableFactory variableFactory() {
        return _variableFactory;
    }

    public StackVariableFactory stackVariableFactory() {
        return _stackVariableFactory;
    }

    public CirClosure cirClosure() {
        return _rootClosure;
    }

    Iterable<BlockState> blockStates() {
        return _blockStates;
    }

    BlockState getBlockStateAt(int bytecodeAddress) {
        return _blockStateMap[bytecodeAddress];
    }

    public BlockState getExceptionDispatcherState(int opcodeAddress) {
        if (_exceptionDispatcherBlockStateMap == null || _exceptionDispatcherBlockStateMap.length <= opcodeAddress) {
            return null;
        }
        return _exceptionDispatcherBlockStateMap[opcodeAddress];
    }

    public void noteBlockCall(CirCall call) {
        _blockCalls.append(call);
        final CirBlock block = (CirBlock) call.procedure();
        block.addCall(call);
    }

    public JavaFrame createFrame() {
        return new JavaFrame(_localVariableFactory);
    }

    public JavaStack createStack() {
        return new JavaStack(_stackVariableFactory);
    }
}
