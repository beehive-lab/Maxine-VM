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
/*VCSID=5bb1e099-9bab-46eb-9e34-2c1e82e73d82*/
package com.sun.max.vm.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.transform.*;
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

    private final int _traceLevel;
    private final boolean _traceVariables;

    public DirInterpreter() {
        _traceLevel = _traceOption.getValue();
        _traceVariables = _traceCpuOption.getValue();
    }

    private static DirValue[] valuesToDirValues(Value[] values) {
        final DirValue[] dirArguments = new DirValue[values.length];
        for (int i = 0; i < values.length; i++) {
            dirArguments[i] = new DirConstant(values[i]);
        }
        return dirArguments;
    }


    private class Environment {
        private final Map<DirVariable, DirValue> _bindings = new HashMap<DirVariable, DirValue>();

        public void bind(DirVariable variable, DirValue value) {
            _bindings.put(variable, value);
        }

        public void bind(DirVariable[] variables, DirValue[] values) {
            assert variables.length == values.length;
            for (int i = 0; i < variables.length; i++) {
                bind(variables[i], values[i]);
            }
        }

        public int rebind(DirValue oldValue, DirValue newValue) {
            int rebound = 0;
            for (Map.Entry<DirVariable, DirValue> entry : _bindings.entrySet()) {
                if (entry.getValue() == oldValue) {
                    entry.setValue(newValue);
                    ++rebound;
                }
            }
            return rebound;
        }

        public Environment() {
        }

        public Environment(DirVariable[] variables, DirValue[] values) {
            bind(variables, values);
        }

        DirValue lookup(DirValue value) {
            if (value instanceof DirVariable) {
                final DirValue result = _bindings.get(value);
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
            if (Trace.hasLevel(_traceLevel)) {
                for (Map.Entry<DirVariable, DirValue> entry : _bindings.entrySet()) {
                    Trace.line(_traceLevel, "    " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }
    }

    private class Evaluator extends DirAdapter {
        private final DirMethod _dirMethod;
        private DirValue _throwable = null;
        private DirValue _result = null;
        private int _instructionIndex = 0;
        private DirBlock _block;
        private final Environment _environment;
        private DirValue _initReceiver;

        Evaluator(DirMethod dirMethod, DirValue[] arguments) {
            _dirMethod = dirMethod;
            _block = dirMethod.blocks().first();
            _environment = new Environment(dirMethod.parameters(), arguments);
            if (dirMethod.classMethodActor().isInstanceInitializer()) {
                _initReceiver = arguments[0];
            }
        }

        private void jump(DirBlock block) {
            _block = block;
            _instructionIndex = 0;
        }

        public Value run() throws InvocationTargetException {
            if (Trace.hasLevel(3)) {
                _dirMethod.traceToString();
            }
            while (_result == null) {
                final DirInstruction instruction = _block.instructions().get(_instructionIndex);
                traceRun(instruction);
                _instructionIndex++;
                instruction.acceptVisitor(this);
                if (_throwable != null) {
                    _throwable = _environment.lookup(_throwable);
                    if (instruction.catchBlock() != null) {
                        _environment.bind(instruction.catchBlock().catchParameter(), _throwable);
                        _throwable = null;
                        jump(instruction.catchBlock());
                    } else {
                        final Throwable cause = (Throwable) _throwable.value().asObject();
                        _throwable = null;
                        throw new InvocationTargetException(cause);
                    }
                }
            }
            if (_result instanceof DirConstant) {
                final DirConstant dirConstant = (DirConstant) _result;
                return _dirMethod.classMethodActor().resultKind().convert(dirConstant.value());
            }
            return ReferenceValue.from(_result);
        }

        private void traceRun(DirInstruction instruction) {
            if (Trace.hasLevel(_traceLevel)) {
                Trace.line(_traceLevel, "------------------");
                if (_traceVariables) {
                    _environment.trace();
                }
                Trace.line(_traceLevel, _block.toString() + "[" + _instructionIndex + "]: " + instruction);
                Trace.stream().flush();
            }
        }

        @Override
        public void visitAssign(DirAssign dirAssign) {
            _environment.bind(dirAssign.destination(), _environment.lookup(dirAssign.source()));
        }

        @Override
        public void visitGoto(DirGoto dirGoto) {
            jump(dirGoto.targetBlock());
        }

        @Override
        public void visitJump(DirJump dirJump) {
            jump((DirBlock) _environment.lookup(dirJump.parameter()));
        }

        @Override
        public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
            final Builtin builtin = dirBuiltinCall.builtin();
            final Value[] arguments = _environment.lookupValues(dirBuiltinCall.arguments());
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
                        _throwable = new DirConstant(ReferenceValue.from(invocationTargetException.getTargetException()));
                        return;
                    }
                }
            } catch (Throwable throwable) {
                _throwable = new DirConstant(ReferenceValue.from(throwable));
                return;
            }
            _environment.bind(dirBuiltinCall.result(), new DirConstant(result));
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
            final DirValue value = _environment.lookup(dirMethodCall.method());
            final MethodActor classMethodActor = unboxClassMethodActor(value);

            if (classMethodActor.isInstanceInitializer()) {
                visitConstructorCall(dirMethodCall, classMethodActor);
            } else {
                try {
                    final Value[] arguments = _environment.lookupValues(dirMethodCall.arguments());
                    final Value result = classMethodActor.invoke(arguments);
                    _environment.bind(dirMethodCall.result(), new DirConstant(result));
                } catch (InvocationTargetException invocationTargetException) {
                    _throwable = new DirConstant(ReferenceValue.from(invocationTargetException.getTargetException()));
                    return;
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected("could not invoke method: " + classMethodActor);
                }
            }
        }

        public void visitConstructorCall(DirMethodCall dirMethodCall, MethodActor classMethodActor) {
            assert classMethodActor.isInstanceInitializer();
            final DirValue[] arguments = dirMethodCall.arguments();
            final DirValue uninitializedObject = _environment.lookup(arguments[0]);
            assert uninitializedObject.value().asObject() instanceof UninitializedObject;

            try {
                final Value[] valueArguments = _environment.lookupValues(com.sun.max.lang.Arrays.subArray(arguments, 1));
                final Value initializedObject = classMethodActor.invokeConstructor(valueArguments);
                _environment.bind(dirMethodCall.result(), new DirConstant(VoidValue.VOID));
                final int rebound = _environment.rebind(uninitializedObject, new DirConstant(initializedObject));
                if (rebound == 0 || _initReceiver == uninitializedObject) {
                    _initReceiver = new DirConstant(initializedObject);
                }
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
            _result = _environment.lookup(dirReturn.returnValue());
        }

        @Override
        public void visitSwitch(DirSwitch dirSwitch) {
            final Value tag = dirSwitch.comparisonKind().convert(_environment.lookup(dirSwitch.tag()).value());
            for (int i = 0; i < dirSwitch.matches().length; i++) {
                final Value match = dirSwitch.comparisonKind().convert(_environment.lookup(dirSwitch.matches()[i]).value());
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
            _throwable = dirThrow.throwable();
        }
    }

    @Override
    public Value execute(DirMethod dirMethod, Value... arguments) throws InvocationTargetException {
        if (dirMethod.isNative()) {
            // JNI stubs cannot be interpreted at the DIR level so the DIR interpreter simply
            // invokes the native method via reflection:
            try {
                return dirMethod.classMethodActor().invoke(arguments);
            } catch (IllegalAccessException illegalAccessException) {
                ProgramError.unexpected("could not access method for invocation: " + dirMethod.classMethodActor().toJava());
            }
        }
        final DirValue[] executeArguments = valuesToDirValues(arguments);
        final Evaluator evaluator = new Evaluator(dirMethod, executeArguments);
        final Value result = evaluator.run();
        if (dirMethod.classMethodActor().isInstanceInitializer()) {
            // The receiver of <init> has been initialized and is represented by a new object
            arguments[0] = evaluator._initReceiver.value();
        }
        return result;
    }
}
