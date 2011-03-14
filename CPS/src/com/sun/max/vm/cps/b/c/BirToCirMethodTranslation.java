/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.ir.*;
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

    private final List<BirBlock> birExceptionDispatchers;
    private final BlockState[] exceptionDispatcherBlockStateMap;

    BirToCirMethodTranslation(BirMethod birMethod, CirVariableFactory variableFactory, CirGenerator cirGenerator) {
        this.birMethod = birMethod;
        this.variableFactory = variableFactory;
        this.cirGenerator = cirGenerator;

        final CirVariable[] parameters = createParameters(birMethod.classMethodActor());

        localVariableFactory = new LocalVariableFactory(variableFactory, birMethod.maxLocals(), parameters);
        stackVariableFactory = new StackVariableFactory(variableFactory, birMethod.maxStack());

        blockStateMap = new BlockState[birMethod.code().length];
        final List<BirBlock> dispatchers = new LinkedList<BirBlock>();
        for (BirBlock birBlock : birMethod.blocks()) {
            if (birBlock.isReachable()) {
                final BlockState blockState = new BlockState(birBlock);
                for (int i = birBlock.bytecodeBlock().start; i <= birBlock.bytecodeBlock().end; i++) {
                    blockStateMap[i] = blockState;
                }

                if (birBlock.role() == IrBlock.Role.EXCEPTION_DISPATCHER) {
                    dispatchers.add(birBlock);
                }
            } else {
                //ProgramWarning.message("unreachable block: " + birBlock + "  " + birMethod);
            }
        }
        this.birExceptionDispatchers = dispatchers;

        if (birMethod.exceptionDispatcherTable().length == 0) {
            exceptionDispatcherBlockStateMap = null;
        } else {
            exceptionDispatcherBlockStateMap = new BlockState[birMethod.code().length];
            for (ExceptionHandlerEntry exceptionDispatcherEntry : birMethod.exceptionDispatcherTable()) {
                final BlockState dispatcherBlockState = blockStateMap[exceptionDispatcherEntry.handlerBCI()];
                assert dispatcherBlockState != null;
                for (int i = exceptionDispatcherEntry.startBCI(); i != exceptionDispatcherEntry.endBCI(); ++i) {
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

    public List<BirBlock> birExceptionDispatchers() {
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
