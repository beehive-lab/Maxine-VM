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
package com.sun.max.vm.cps.ir.interpreter;

import java.lang.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * Interpreter for CIR constructs.
 *
 * Used for unit testing without compiling beyond CIR.
 *
 * @author Bernd Mathiske
 */
public class CirInterpreter extends IrInterpreter<CirMethod> {

    private final CirGenerator cirGenerator;
    private CirOptimizer cirOptimizer;

    public CirInterpreter(CirGenerator cirGenerator) {
        this.cirGenerator = cirGenerator;
    }

    protected CirCall foldBuiltin(CirBuiltin cirBuiltin, CirValue[] cirArguments) {
        final Builtin builtin = cirBuiltin.builtin;
        if (builtin instanceof PointerBuiltin) {
            final Value[] arguments = CirFoldable.Static.cirArgumentsToValues(cirArguments, null);
            final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
            try {
                if (builtin instanceof PointerLoadBuiltin) {
                    final PointerLoadBuiltin pointerLoadBuiltin = (PointerLoadBuiltin) builtin;
                    return new CirCall(normalContinuation, new CirConstant(pointerLoad(pointerLoadBuiltin.resultKind, arguments)));
                }
                final PointerStoreBuiltin pointerStoreBuiltin = (PointerStoreBuiltin) builtin;
                pointerStore(pointerStoreBuiltin.kind(), arguments);
                return new CirCall(normalContinuation, CirCall.NO_ARGUMENTS);
            } catch (Throwable throwable) {
                //TODO: catch only those exceptions that are caused by the interpreted CIR code
                final CirValue exceptionContinuation = cirArguments[cirArguments.length - 1];
                return new CirCall(exceptionContinuation, CirConstant.fromObject(throwable));
            }
        }
        try {
            CirCall result = cirBuiltin.fold(cirOptimizer, cirArguments);
            if (builtin == InfopointBuiltin.BUILTIN) {
                assert result.arguments().length == 1;
                int opcode = cirArguments[0].value().asInt();
                if (opcode != Bytecodes.HERE) {
                    result.setArguments(CirCall.NO_ARGUMENTS);
                }
            }
            return result;
        } catch (CirFoldingException cirFoldingException) {
            return CirFoldable.Static.createExceptionCall(cirFoldingException.getCause(), cirArguments);
        }
    }

    private final CirVariableFactory variableFactory = new CirVariableFactory();

    private CirValue evaluate(CirCall cirCall) throws InvocationTargetException {
        CirCall call = cirCall;
        while (true) {
            Trace.line(3);
            Trace.line(3, "----------------- CIR interpretation:  -----------------");
            call.trace(3);
            CirValue procedure = call.procedure();
            CirValue[] arguments = call.arguments();
            if (procedure instanceof CirConstant) {
                final MethodID methodID = MethodID.fromWord(procedure.value().asWord());
                final ClassMethodActor classMethodActor = (ClassMethodActor) MethodID.toMethodActor(methodID);
                procedure = cirGenerator.createIrMethod(classMethodActor);
            }
            if (procedure instanceof CirMethod) {
                final CirMethod method = (CirMethod) procedure;
                try {
                    call = method.fold(cirOptimizer, arguments);
                } catch (CirFoldingException cirFoldingException) {
                    CPSAbstractCompiler.INTERPRETER_EXCEPTION.set(cirFoldingException.getCause());
                    call = CirFoldable.Static.createExceptionCall(cirFoldingException.getCause(), arguments);
                }
            } else if (procedure instanceof CirBuiltin) {
                call = foldBuiltin((CirBuiltin) procedure, arguments);
            } else if (procedure instanceof CirBlock) {
                final CirBlock block = (CirBlock) procedure;
                call.setProcedure(block.closure());
            } else if (procedure instanceof CirVariable) {
                if (procedure instanceof CirNormalContinuationParameter) {
                    if (arguments.length == 0) {
                        return new CirConstant(VoidValue.VOID);
                    }
                    return arguments[0];
                }
                if (procedure instanceof CirExceptionContinuationParameter) {
                    assert arguments.length == 1;
                    final CirConstant throwable = (CirConstant) arguments[0];
                    CPSAbstractCompiler.INTERPRETER_EXCEPTION.set(null);
                    throw new InvocationTargetException((Throwable) throwable.value().asObject());
                }
                ProgramError.unexpected("call to variable other than continuation parameter: " + procedure);
            } else if (procedure instanceof CirSwitch) {
                final CirSwitch method = (CirSwitch) procedure;
                try {
                    call = method.fold(cirOptimizer, arguments);
                } catch (CirFoldingException cirFoldingException) {
                    // Folding a CirSwitch should never fail
                    throw ProgramError.unexpected(cirFoldingException);
                }
            } else {
                assert procedure instanceof CirClosure;
                final CirClosure closure = CirReplication.apply((CirClosure) procedure);
                final CirVariable[] parameters = closure.parameters();
                assert arguments.length == parameters.length;
                call = CirBetaReduction.applyMultiple(closure, arguments);
            }
        }
    }

    private CirValue[] valuesToCirArguments(Value[] arguments) {
        final CirValue[] cirArguments = new CirValue[arguments.length + 2];
        for (int i = 0; i < arguments.length; i++) {
            cirArguments[i] = new CirConstant(arguments[i]);
        }
        cirArguments[cirArguments.length - 2] = variableFactory.normalContinuationParameter();
        cirArguments[cirArguments.length - 1] = variableFactory.exceptionContinuationParameter();
        return cirArguments;
    }

    @Override
    public Value execute(IrMethod method, Value... arguments) throws InvocationTargetException {
        CirMethod cirMethod = (CirMethod) method;
        CirCall call = new CirCall(cirMethod, arguments.length > 0 ? valuesToCirArguments(arguments) : CirCall.NO_ARGUMENTS);
        cirOptimizer = new CirOptimizer(cirGenerator, cirMethod, call, CirInliningPolicy.NONE);

        if (cirMethod.isNative()) {
            // native stubs cannot be interpreted at the CIR level so the CIR interpreter simply
            // invokes the native method via reflection:
            call = cirMethod.fold(cirOptimizer, valuesToCirArguments(arguments));
        } else {
            call = new CirCall(cirMethod.copyClosure(), valuesToCirArguments(arguments));
        }
        final CirValue result = evaluate(call);
        if (result instanceof CirConstant) {
            final CirConstant cirConstant = (CirConstant) result;
            return cirMethod.resultKind().convert(cirConstant.value());
        }
        return ReferenceValue.from(result);
    }
}
