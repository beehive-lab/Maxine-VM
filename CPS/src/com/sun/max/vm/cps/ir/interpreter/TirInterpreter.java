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

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirInstruction.*;
import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirInterpreter extends IrInterpreter<TirTree> {
    private Mapping<TirInstruction, Value> contextValues = new IdentityHashMapping<TirInstruction, Value>();
    private Mapping<TirInstruction, Value> invariantValues = new IdentityHashMapping<TirInstruction, Value>();
    private Mapping<TirInstruction, Value> variantValues = new IdentityHashMapping<TirInstruction, Value>();
    private Mapping<TirTreeCall, BirState> callResults = new IdentityHashMapping<TirTreeCall, BirState>();

    private static ClassMethodActor createTupleOrHybridMethodActor = CreateTupleOrHybrid.SNIPPET.executable;

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
            execute(Utils.first(tirTree.traces()));
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
        final Mapping<TirInstruction, Value> newContext = new IdentityHashMapping<TirInstruction, Value>();
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
