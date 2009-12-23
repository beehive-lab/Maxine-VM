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
package com.sun.max.vm.compiler.cps.ir.interpreter;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.compiler.cps.tir.*;
import com.sun.max.vm.compiler.cps.tir.TirInstruction.*;
import com.sun.max.vm.compiler.cps.tir.pipeline.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirInterpreter extends IrInterpreter<TirTree> {
    private VariableMapping<TirInstruction, Value> contextValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirInstruction, Value> invariantValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirInstruction, Value> variantValues = new IdentityHashMapping<TirInstruction, Value>();
    private VariableMapping<TirTreeCall, BirState> callResults = new IdentityHashMapping<TirTreeCall, BirState>();

    private static ClassMethodActor createTupleOrHybridMethodActor = ClassMethodActor.findStatic(CreateTupleOrHybrid.class, "createTupleOrHybrid");

    private Evaluator evaluator = new Evaluator();
    private BirState executionState;
    private TirTree tree;
    private TirTrace targetTrace;
    private TirGuard exitGuard;

    private boolean hasBailedOut;
    private boolean isInvariant;
    private boolean needsToJump;

    private boolean needsToJump() {
        return needsToJump;
    }

    private TirTrace resetJump() {
        needsToJump = false;
        return targetTrace;
    }

    private void jump(TirTrace trace) {
        needsToJump = true;
        targetTrace = trace;
    }

    public TirGuard execute(TirTree tirTree, BirState state) {
        assert tirTree.entryState().matchesSliceOf(state) : "Incompatible states.";

        this.tree = tirTree;
        executionState = state;
        isInvariant = true;

        for (TirLocal local : tirTree.locals()) {
            if (local.flags().isRead()) {
                define(local, executionState.getSlot(local.slot()));
            }
        }

        for (TirInstruction instruction : tirTree.prologue()) {
            instruction.accept(evaluator);
        }

        isInvariant = false;
        while (hasBailedOut == false) {
            execute(tirTree.traces().first());
        }
        return exitGuard;
    }

    private Value evaluate(TirInstruction instruction) {
        if (instruction instanceof TirConstant) {
            final TirConstant constant = (TirConstant) instruction;
            return constant.value();
        } else if (instruction instanceof TirLocal) {
            ProgramError.check(contextValues.get(instruction) != null);
            return contextValues.get(instruction);
        } else if (instruction instanceof TirNestedLocal) {
            final TirNestedLocal local = (TirNestedLocal) instruction;
            final BirState state = callResults.get(local.call());
            return state.getOne(local.slot());
        }
        Value value = variantValues.get(instruction);
        if (value == null) {
            value = invariantValues.get(instruction);
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
        callResults.put(call, state);
    }

    private void define(TirLocal context, Value value) {
        contextValues.put(context, value);
    }

    private void define(TirInstruction instruction, Value value) {
        if (isInvariant) {
            invariantValues.put(instruction, value);
        } else {
            variantValues.put(instruction, value);
        }
    }

    private void execute(TirTrace trace) {
        for (TirInstruction instruction : trace.instructions()) {
            try {
                tree.profile().executions++;
                instruction.accept(evaluator);
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
                Console.println(Color.RED, "Exception At: " + NameMap.nameOf(tree, instruction) + " " + e.toString());
                ProgramError.unexpected(e);
                return;
            }
        }
        loop(trace);
    }

    private void loop(TirTrace trace) {
        assert trace.tailState().matches(tree.entryState());
        final VariableMapping<TirInstruction, Value> newContext = new IdentityHashMapping<TirInstruction, Value>();
        trace.tailState().compare(tree.entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction tail, TirInstruction entry) {
                final TirLocal local = (TirLocal) entry;
                if (local.flags().isRead()) {
                    // Console.println(Color.RED, "Copying : " + TirNameMap.nameOf(_tree, local));
                    newContext.put(local, evaluate(tail));
                }
            }
        });
        contextValues = newContext;
        variantValues.clear();
        tree.profile().iterations++;
    }

    private void bailout(TirGuard guard) {
        hasBailedOut = true;
        restore(guard.state(), executionState);
        exitGuard = guard;
    }

    private void restore(TirState state, final BirState exeState) {
        state.compare(tree.entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction exit, TirInstruction entry) {
                if (entry instanceof TirLocal) {
                    final TirLocal local = (TirLocal) entry;
                    if (local.flags().isWritten() == false) {
                        return;
                    }
                }
                if (exit instanceof Placeholder == false) {
                    exeState.store(index, evaluate(exit));
                }
            }
        });
        exeState.append(state.frames());
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
                result = pointerLoad(pointerLoadBuiltin.resultKind, arguments);
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
                if (call.method() == createTupleOrHybridMethodActor) {
                    final ClassActor classActor = (ClassActor) arguments[0].asObject();
                    result = ObjectReferenceValue.from(Objects.allocateInstance(classActor.toJava()));
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
            BirState executionStateCopy = TirInterpreter.this.executionState.copy();
            restore(call.state(), executionStateCopy);
            executionStateCopy = executionStateCopy.slice(1);
            interpreter.execute(call.tree(), executionStateCopy);
            define(call, executionStateCopy);
        }

        @Override
        public void visit(TirNestedLocal local) {
            define(local, executionState.getSlot(local.slot()));
        }
    }

    @Override
    public Value execute(IrMethod method, Value... arguments) throws InvocationTargetException {
        assert false : "Not supported.";
        return null;
    }
}
