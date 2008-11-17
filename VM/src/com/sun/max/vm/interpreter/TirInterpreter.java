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
package com.sun.max.vm.interpreter;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.NonFoldableSnippet.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.compiler.tir.TirInstruction.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirInterpreter extends IrInterpreter<TirTree> {
    private VariableMapping<TirInstruction, Value> _contextValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirInstruction, Value> _invariantValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirInstruction, Value> _variantValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirTreeCall, BirState> _callResults = new IdentityHashMapping<TirTreeCall, BirState>();

    private static ClassMethodActor _createTupleOrHybridMethodActor = ClassMethodActor.findStatic(CreateTupleOrHybrid.class, "createTupleOrHybrid");

    private Evaluator _evaluator = new Evaluator();
    private BirState _executionState;
    private TirTree _tree;
    private TirTrace _targetTrace;
    private TirGuard _exitGuard;

    private boolean _hasBailedOut;
    private boolean _isInvariant;
    private boolean _needsToJump;

    private boolean needsToJump() {
        return _needsToJump;
    }

    private TirTrace resetJump() {
        _needsToJump = false;
        return _targetTrace;
    }

    private void jump(TirTrace trace) {
        _needsToJump = true;
        _targetTrace = trace;
    }

    public TirGuard execute(TirTree tree, BirState state) {
        assert tree.entryState().matchesSliceOf(state) : "Incompatible states.";

        _tree = tree;
        _executionState = state;
        _isInvariant = true;

        for (TirLocal local : tree.locals()) {
            if (local.flags().isRead()) {
                define(local, _executionState.getSlot(local.slot()));
            }
        }

        for (TirInstruction instruction : tree.prologue()) {
            instruction.accept(_evaluator);
        }

        _isInvariant = false;
        while (_hasBailedOut == false) {
            execute(tree.traces().first());
        }
        return _exitGuard;
    }


    private Value evaluate(TirInstruction instruction) {
        if (instruction instanceof TirConstant) {
            final TirConstant constant = (TirConstant) instruction;
            return constant.value();
        } else if (instruction instanceof TirLocal) {
            ProgramError.check(_contextValues.get(instruction) != null);
            return _contextValues.get(instruction);
        } else if (instruction instanceof TirNestedLocal) {
            final TirNestedLocal local = (TirNestedLocal) instruction;
            final BirState state = _callResults.get(local.call());
            return state.getOne(local.slot());
        }
        Value value = _variantValues.get(instruction);
        if (value == null) {
            value = _invariantValues.get(instruction);
        }
        return value;
    }

    private Value[] evaluateMany(TirInstruction... instructions) {
        final Value[] values = new Value[instructions.length];
        for (int i = 0; i < instructions.length; i++) {
            values[i] = evaluate(instructions[i]);
        }
        return values;
    }

    private void define(TirTreeCall call, BirState state) {
        _callResults.put(call, state);
    }

    private void define(TirLocal context, Value value) {
        _contextValues.put(context, value);
    }

    private void define(TirInstruction instruction, Value value) {
        if (_isInvariant) {
            _invariantValues.put(instruction, value);
        } else {
            _variantValues.put(instruction, value);
        }
    }

    private void execute(TirTrace trace) {
        for (TirInstruction instruction : trace.instructions()) {
            try {
                _tree.profile()._executions++;
                instruction.accept(_evaluator);
                if (needsToJump()) {
                    final TirTrace target = resetJump();
                    if (target == null) {
                        bailout((TirGuard) instruction);
                    } else {
                        execute(resetJump());
                    }
                    return;
                }
            } catch (Throwable e) {
                Console.println(Color.RED, "Exception At: " + NameMap.nameOf(_tree, instruction) + " " + e.toString());
                ProgramError.unexpected(e);
                return;
            }
        }
        loop(trace);
    }

    private void loop(TirTrace trace) {
        assert trace.tailState().matches(_tree.entryState());
        final VariableMapping<TirInstruction, Value> newContext = new IdentityHashMapping<TirInstruction, Value>();
        trace.tailState().compare(_tree.entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction tail, TirInstruction entry) {
                final TirLocal local = (TirLocal) entry;
                if (local.flags().isRead()) {
                    // Console.println(Color.RED, "Copying : " + TirNameMap.nameOf(_tree, local));
                    newContext.put(local, evaluate(tail));
                }
            }
        });
        _contextValues = newContext;
        _variantValues.clear();
        _tree.profile()._iterations++;
    }

    private void bailout(TirGuard guard) {
        _hasBailedOut = true;
        restore(guard.state(), _executionState);
        _exitGuard = guard;
    }

    private void restore(TirState state, final BirState executionState) {
        state.compare(_tree.entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction exit, TirInstruction entry) {
                if (entry instanceof TirLocal) {
                    final TirLocal local = (TirLocal) entry;
                    if (local.flags().isWritten() == false) {
                        return;
                    }
                }
                if (exit instanceof Placeholder == false) {
                    executionState.store(_index, evaluate(exit));
                }
            }
        });
        executionState.append(state.frames());
    }

    public class Evaluator extends TirInstructionAdapter {

        @Override
        public void visit(TirBuiltinCall call) {
            final Builtin builtin = call.builtin();
            final Value[] arguments = evaluateMany(call.operands());
            final Value result;

            if (builtin instanceof PointerBuiltin) {
                if (builtin instanceof PointerStoreBuiltin) {
                    final PointerStoreBuiltin pointerStoreBuiltin = (PointerStoreBuiltin) builtin;
                    pointerStore(pointerStoreBuiltin.kind(), arguments);
                    return;
                }
                assert builtin instanceof PointerLoadBuiltin;
                final PointerLoadBuiltin pointerLoadBuiltin = (PointerLoadBuiltin) builtin;
                result = pointerLoad(pointerLoadBuiltin.resultKind(), arguments);
                define(call, call.kind().convert(result));
            } else {
                visit((TirCall) call);
            }
        }

        @Override
        public void visit(TirGuard guard) {
            final Value operand0 = evaluate(guard.operand0());
            final Value operand1 = evaluate(guard.operand1());
            final boolean taken = guard.valueComparator().evaluate(operand0, operand1);
            if (taken == false) {
                TirTrace target = null;
                if (guard.anchor() != null) {
                    target = guard.anchor().trace();
                }
                jump(target);
            }
        }

        @Override
        public void visit(TirCall call) {
            final Value[] arguments = evaluateMany(call.operands());
            final Value result;
            try {
                if (call.method() == _createTupleOrHybridMethodActor) {
                    final ClassActor classActor = (ClassActor) arguments[0].asObject();
                    result = ObjectReferenceValue.from(UnsafeAccess.allocateInstance(classActor.toJava()));
                } else {
                    result = call.method().invoke(arguments);
                }
            } catch (Throwable e) {
                ProgramError.unexpected(e);
                return;
            }
            if (call.method().resultKind() != Kind.VOID) {
                define(call, call.kind().convert(result));
            }
        }

        @Override
        public void visit(TirTreeCall call) {
            final TirInterpreter interpreter = new TirInterpreter();
            BirState executionState = _executionState.copy();
            restore(call.state(), executionState);
            executionState = executionState.slice(1);
            interpreter.execute(call.tree(), executionState);
            define(call, executionState);
        }

        @Override
        public void visit(TirNestedLocal local) {
            define(local, _executionState.getSlot(local.slot()));
        }
    }

    @Override
    public Value execute(TirTree method, Value... arguments) throws InvocationTargetException {
        assert false : "Not supported.";
        return null;
    }
}
