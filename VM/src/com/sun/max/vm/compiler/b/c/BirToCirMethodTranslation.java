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
package com.sun.max.vm.compiler.b.c;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
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

    private final BirMethod birMethod;
    private final CirGenerator cirGenerator;

    private final CirVariableFactory variableFactory;
    private final LocalVariableFactory localVariableFactory;
    private final StackVariableFactory stackVariableFactory;

    private final CirClosure rootClosure;

    private CirVariable[] createParameters(ClassMethodActor methodActor) {
        final SignatureDescriptor signature = methodActor.descriptor();
        final int numberOfParameters = signature.numberOfParameters() + (methodActor.isStatic() ? 0 : 1);
        final CirVariable[] parameters = CirClosure.newParameters(numberOfParameters + 2);
        int i = 0;
        if (!methodActor.isStatic()) {
            parameters[i] = variableFactory.createMethodParameter(methodActor.holder().kind, i);
            i++;
        }
        int j = 0;
        while (i < numberOfParameters) {
            parameters[i] = variableFactory.createMethodParameter(signature.parameterDescriptorAt(j).toKind(), i);
            i++;
            j++;
        }
        parameters[i++] = variableFactory.normalContinuationParameter();
        parameters[i++] = variableFactory.exceptionContinuationParameter();
        return parameters;
    }

    private final BlockState[] blockStateMap;

    private final Sequence<BirBlock> birExceptionDispatchers;
    private final BlockState[] exceptionDispatcherBlockStateMap;

    BirToCirMethodTranslation(BirMethod birMethod, CirVariableFactory variableFactory, CirGenerator cirGenerator) {
        this.birMethod = birMethod;
        this.variableFactory = variableFactory;
        this.cirGenerator = cirGenerator;

        final CirVariable[] parameters = createParameters(birMethod.classMethodActor());

        localVariableFactory = new LocalVariableFactory(variableFactory, birMethod.maxLocals(), parameters);
        stackVariableFactory = new StackVariableFactory(variableFactory, birMethod.maxStack());

        blockStateMap = new BlockState[birMethod.code().length];
        final AppendableSequence<BirBlock> dispatchers = new LinkSequence<BirBlock>();
        for (BirBlock birBlock : birMethod.blocks()) {
            if (birBlock.isReachable()) {
                final BlockState blockState = new BlockState(birBlock);
                for (int i = birBlock.bytecodeBlock().start(); i <= birBlock.bytecodeBlock().end(); i++) {
                    blockStateMap[i] = blockState;
                }

                if (birBlock.role() == IrBlock.Role.EXCEPTION_DISPATCHER) {
                    dispatchers.append(birBlock);
                }
            } else {
                ProgramWarning.message("unreachable block: " + birBlock + "  " + birMethod);
            }
        }
        this.birExceptionDispatchers = dispatchers;

        if (birMethod.exceptionDispatcherTable().isEmpty()) {
            exceptionDispatcherBlockStateMap = null;
        } else {
            exceptionDispatcherBlockStateMap = new BlockState[birMethod.code().length];
            for (ExceptionHandlerEntry exceptionDispatcherEntry : birMethod.exceptionDispatcherTable()) {
                final BlockState dispatcherBlockState = blockStateMap[exceptionDispatcherEntry.handlerPosition()];
                assert dispatcherBlockState != null;
                for (int i = exceptionDispatcherEntry.startPosition(); i != exceptionDispatcherEntry.endPosition(); ++i) {
                    // This checks the invariant established by generating exception dispatchers in bytecode
                    ProgramError.check(exceptionDispatcherBlockStateMap[i] == null, "no more than one exception handler should cover a bytecode address");
                    exceptionDispatcherBlockStateMap[i] = dispatcherBlockState;
                }
            }
        }

        final CirCall rootCall = newCirCall(blockStateMap[0].cirBlock());
        rootClosure = new CirClosure(rootCall, parameters);
    }

    public BirMethod birMethod() {
        return birMethod;
    }

    public Sequence<BirBlock> birExceptionDispatchers() {
        return birExceptionDispatchers;
    }

    public CirGenerator cirGenerator() {
        return cirGenerator;
    }

    public ClassMethodActor classMethodActor() {
        return birMethod.classMethodActor();
    }

    public CirVariableFactory variableFactory() {
        return variableFactory;
    }

    public StackVariableFactory stackVariableFactory() {
        return stackVariableFactory;
    }

    public CirClosure cirClosure() {
        return rootClosure;
    }

    BlockState getBlockStateAt(int bytecodePosition) {
        return blockStateMap[bytecodePosition];
    }

    public BlockState getExceptionDispatcherState(int opcodePosition) {
        if (exceptionDispatcherBlockStateMap == null || exceptionDispatcherBlockStateMap.length <= opcodePosition) {
            return null;
        }
        return exceptionDispatcherBlockStateMap[opcodePosition];
    }

    public CirCall newCirCall(CirBlock block) {
        final CirCall call = new CirCall(block, CirCall.NO_ARGUMENTS);
        block.addCall(call);
        return call;
    }

    public JavaFrame createFrame() {
        return new JavaFrame(localVariableFactory);
    }

    public JavaStack createStack() {
        return new JavaStack(stackVariableFactory);
    }
}
