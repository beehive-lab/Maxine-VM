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
package com.sun.max.vm.compiler.ir.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * An interpreter for the DIR representation of a method.
 *
 * Instances of this interpreter are configured by the following properties where the
 * value of a property is retrieved by calling {@link System#getProperty(String)} with
 * a key composed of the property name prefixed by {@link IrInterpreter#PROPERTY_PREFIX}.
 * <p>
 * Property: {@code "trace.level"} <br />
 * Default: {@code 3} <br />
 * Description: Specifies the {@linkplain Trace#level() level} at which the interpreter will emit tracing while interpreting.
 * <p>
 * Property: {@code "trace.cpu"} <br />
 * Default: {@code false} <br />
 * Description: Traces variable state after each instruction traced.
 * <p>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class DirInterpreter extends IrInterpreter<DirMethod> {

    private final boolean trace;
    private final boolean traceVariables;

    public DirInterpreter() {
        traceVariables = traceCpuOption.getValue();
        trace = traceVariables || traceOption.getValue();
    }

    private static DirValue[] valuesToDirValues(Value[] values) {
        final DirValue[] dirArguments = new DirValue[values.length];
        for (int i = 0; i < values.length; i++) {
            dirArguments[i] = new DirConstant(values[i]);
        }
        return dirArguments;
    }


    private class Environment {
        private final Map<DirVariable, DirValue> bindings = new HashMap<DirVariable, DirValue>();

        public void bind(DirVariable variable, DirValue value) {
            bindings.put(variable, value);
        }

        public void bind(DirVariable[] variables, DirValue[] values) {
            assert variables.length == values.length;
            for (int i = 0; i < variables.length; i++) {
                bind(variables[i], values[i]);
            }
        }

        public Environment() {
        }

        public Environment(DirVariable[] variables, DirValue[] values) {
            bind(variables, values);
        }

        DirValue lookup(DirValue value) {
            if (value instanceof DirVariable) {
                final DirValue result = bindings.get(value);
                ProgramError.check(result != null, "DIR variable is not defined: " + value);
                return result;
            }
            return value;
        }

        public Value[] lookupValues(DirValue[] dirValues) {
            final Value[] values = new Value[dirValues.length];
            for (int i = 0; i < dirValues.length; i++) {
                values[i] = lookup(dirValues[i]).value();
            }
            return values;
        }

        public void trace() {
            if (trace) {
                for (Map.Entry<DirVariable, DirValue> entry : bindings.entrySet()) {
                    Trace.stream().println("    " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }
    }

    private class Evaluator extends DirAdapter {
        private final DirMethod dirMethod;
        private DirValue result = null;
        private int instructionIndex = 0;
        private DirBlock block;
        private final Environment environment;
        private Throwable throwable;

        Evaluator(DirMethod dirMethod, DirValue[] arguments) {
            this.dirMethod = dirMethod;
            block = dirMethod.blocks().first();
            environment = new Environment(dirMethod.parameters(), arguments);
        }

        private void jump(DirBlock b) {
            this.block = b;
            instructionIndex = 0;
        }

        public Value run() throws InvocationTargetException {
            if (Trace.hasLevel(3)) {
                dirMethod.traceToString();
            }
            while (result == null) {
                final DirInstruction instruction = block.instructions().get(instructionIndex);
                traceRun(instruction);
                instructionIndex++;
                instruction.acceptVisitor(this);
                if (throwable != null) {
                    if (instruction.catchBlock() != null) {
                        ExceptionDispatcher.INTERPRETER_EXCEPTION.set(throwable);
                        jump(instruction.catchBlock());
                        throwable = null;
                    } else {
                        InvocationTargetException invocationTargetException = new InvocationTargetException(throwable);
                        throwable = null;
                        throw invocationTargetException;
                    }
                }
            }
            if (result instanceof DirConstant) {
                final DirConstant dirConstant = (DirConstant) result;
                return dirMethod.classMethodActor().resultKind().convert(dirConstant.value());
            }
            return ReferenceValue.from(result);
        }

        private void traceRun(DirInstruction instruction) {
            if (trace) {
                Trace.stream().println("------------------");
                if (traceVariables) {
                    environment.trace();
                }
                Trace.stream().println(block.toString() + "[" + instructionIndex + "]: " + instruction);
                Trace.stream().flush();
            }
        }

        @Override
        public void visitAssign(DirAssign dirAssign) {
            environment.bind(dirAssign.destination(), environment.lookup(dirAssign.source()));
        }

        @Override
        public void visitGoto(DirGoto dirGoto) {
            jump(dirGoto.targetBlock());
        }

        @Override
        public void visitJump(DirJump dirJump) {
            jump((DirBlock) environment.lookup(dirJump.parameter()));
        }

        @Override
        public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
            final Builtin builtin = dirBuiltinCall.builtin();
            final Value[] arguments = environment.lookupValues(dirBuiltinCall.arguments());
            Value result;
            try {
                if (builtin instanceof PointerBuiltin) {
                    if (builtin instanceof PointerStoreBuiltin) {
                        final PointerStoreBuiltin pointerStoreBuiltin = (PointerStoreBuiltin) builtin;
                        pointerStore(pointerStoreBuiltin.kind(), arguments);
                        return;
                    }
                    assert builtin instanceof PointerLoadBuiltin;
                    final PointerLoadBuiltin pointerLoadBuiltin = (PointerLoadBuiltin) builtin;
                    result = pointerLoad(pointerLoadBuiltin.resultKind(), arguments);
                } else {
                    try {
                        result = builtin.foldingMethodActor().invoke(arguments);
                    } catch (InvocationTargetException invocationTargetException) {
                        throwable = invocationTargetException.getTargetException();
                        return;
                    }
                }
            } catch (Throwable t) {
                throwable = t;
                return;
            }
            environment.bind(dirBuiltinCall.result(), new DirConstant(result));
        }

        private ClassMethodActor unboxClassMethodActor(final DirValue value) {
            ClassMethodActor classMethodActor;
            if (value instanceof DirConstant) {
                final DirConstant constant = (DirConstant) value;
                final MethodID methodID = MethodID.fromWord(constant.value().asWord());
                classMethodActor = (ClassMethodActor) MethodID.toMethodActor(methodID);
            } else {
                final DirMethodValue dirMethodValue = (DirMethodValue) value;
                classMethodActor = dirMethodValue.classMethodActor();
            }
            return classMethodActor;
        }

        @Override
        public void visitMethodCall(DirMethodCall dirMethodCall) {
            final DirValue value = environment.lookup(dirMethodCall.method());
            final MethodActor classMethodActor = unboxClassMethodActor(value);

            if (classMethodActor.isInstanceInitializer()) {
                visitConstructorCall(dirMethodCall, classMethodActor);
            } else {
                try {
                    final Value[] arguments = environment.lookupValues(dirMethodCall.arguments());
                    final Value result = classMethodActor.invoke(arguments);
                    environment.bind(dirMethodCall.result(), new DirConstant(result));
                } catch (InvocationTargetException invocationTargetException) {
                    throwable = invocationTargetException.getTargetException();
                    return;
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected("could not invoke method: " + classMethodActor);
                }
            }
        }

        public void visitConstructorCall(DirMethodCall dirMethodCall, MethodActor classMethodActor) {
            assert classMethodActor.isInstanceInitializer();
            final DirValue[] arguments = dirMethodCall.arguments();
            final DirValue uninitializedObject = environment.lookup(arguments[0]);

            try {
                final Value[] valueArguments = environment.lookupValues(com.sun.max.lang.Arrays.subArray(arguments, 1));
                final Value initializedObject = classMethodActor.invokeConstructor(valueArguments);
                Objects.copy(initializedObject.asObject(), uninitializedObject.value().asObject());
                environment.bind(dirMethodCall.result(), new DirConstant(VoidValue.VOID));
            } catch (InstantiationException e) {
                ProgramError.unexpected("error calling " + classMethodActor, e);
            } catch (IllegalAccessException e) {
                ProgramError.unexpected("could not access " + classMethodActor, e);
            } catch (InvocationTargetException e) {
                ProgramError.unexpected("error calling " + classMethodActor, e.getTargetException());
            }
        }

        @Override
        public void visitReturn(DirReturn dirReturn) {
            result = environment.lookup(dirReturn.returnValue());
        }

        @Override
        public void visitSwitch(DirSwitch dirSwitch) {
            final Value tag = dirSwitch.comparisonKind().convert(environment.lookup(dirSwitch.tag()).value());
            for (int i = 0; i < dirSwitch.matches().length; i++) {
                final Value match = dirSwitch.comparisonKind().convert(environment.lookup(dirSwitch.matches()[i]).value());
                assert match.kind() == dirSwitch.comparisonKind();
                if (dirSwitch.valueComparator().evaluate(tag, match)) {
                    jump(dirSwitch.targetBlocks()[i]);
                    return;
                }
            }
            jump(dirSwitch.defaultTargetBlock());
        }

        @Override
        public void visitThrow(DirThrow dirThrow) {
            throwable = (Throwable) environment.lookup(dirThrow.throwable()).value().asObject();
        }
    }

    @Override
    public Value execute(IrMethod method, Value... arguments) throws InvocationTargetException {
        DirMethod dirMethod = (DirMethod) method;
        if (dirMethod.isNative()) {
            // JNI stubs cannot be interpreted at the DIR level so the DIR interpreter simply
            // invokes the native method via reflection:
            try {
                return dirMethod.classMethodActor().invoke(arguments);
            } catch (IllegalAccessException illegalAccessException) {
                ProgramError.unexpected("could not access method for invocation: " + dirMethod.classMethodActor());
            }
        }
        final DirValue[] executeArguments = valuesToDirValues(arguments);
        final Evaluator evaluator = new Evaluator(dirMethod, executeArguments);
        return evaluator.run();
    }
}
