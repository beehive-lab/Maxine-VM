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
package com.sun.max.vm.cps.hotpath.compiler;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ClassConstant.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.hotpath.compiler.Tracer.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirRecorder {
    public static OptionSet optionSet = new OptionSet();
    public static Option<Boolean> printState = optionSet.newBooleanOption("PRS", true, "(P)rints the Trace (R)ecorders's (S)tate.");

    private final BytecodeRecorder visitor = new BytecodeRecorder();
    private final BytecodeScanner scanner = new BytecodeScanner(visitor);
    private final TirTrace trace;
    private final TirState state;
    private final Tracer tracer;
    private final Scope scope;

    public TirRecorder(Tracer tracer, Scope scope, TirState state, TirTrace trace) {
        this.tracer = tracer;
        this.trace = trace;
        this.state = state;
        this.scope = scope;
    }

    public void record(BytecodeLocation location) {
        state.last().setPc(location.bci);
        visitor.setMethod(location.classMethodActor);
        final byte[] bytecode = location.classMethodActor.codeAttribute().code();
        scanner.scanInstruction(bytecode, location.bci);
    }

    public void recordNesting(final TreeAnchor anchor, Bailout bailout) {
        final TirState state = takeSnapshot(anchor);
        final TirTreeCall call = new TirTreeCall(anchor.tree(), state);
        append(call);
        if (printState.getValue()) {
            Console.printThinDivider("NESTING " + NameMap.nameOf(anchor.tree()));
            Console.print(Color.LIGHTRED, "  ENTRY STATE: ");
            state.println(NameMap.COMPACT);
            Console.print(Color.LIGHTRED, "BAILOUT STATE: ");
            bailout.guard().state().println(NameMap.COMPACT);
            Console.printThinDivider();
        }
        bailout.guard().state().compare(anchor.tree().entryState(), new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction exit, TirInstruction entry) {
                final TirNestedLocal nestedLocal = new TirNestedLocal(call, index);
                nestedLocal.setKind(exit.kind());
                TirRecorder.this.state.store(index, nestedLocal);
                append(nestedLocal);
            }
        });
        this.state.append(bailout.guard().state().frames());
    }

    private void push(TirInstruction instruction) {
        state.push(instruction);
    }

    private void push(Value value) {
        push(new TirConstant(value));
    }

    private TirInstruction pop(Kind kind) {
        return state.pop(kind);
    }

    private void append(TirInstruction instruction) {
        trace.append(instruction);
    }

    private void pushAndAppend(TirInstruction instruction) {
        push(instruction);
        append(instruction);
    }

    private void call(ClassMethodActor target, TirState state) {
        final Kind[] parameterKinds = target.getParameterKinds();
        final TirInstruction[] arguments = this.state.popMany(parameterKinds);
        final TirCall call = new TirMethodCall(target, state, arguments);
        append(call);
        if (call.kind() != Kind.VOID) {
            push(call);
        }
    }

    private void call(ClassMethodActor target, TirState state, TirInstruction... arguments) {
        final TirCall call = new TirMethodCall(target, state, arguments);
        append(call);
        if (call.kind() != Kind.VOID) {
            push(call);
        }
    }

    private void call(Builtin builtin) {
        final Kind[] parameterKinds = builtin.parameterKinds();
        final TirInstruction[] arguments = state.popMany(parameterKinds);
        final TirCall builtinCall = new TirBuiltinCall(builtin, arguments);
        append(builtinCall);
        if (builtinCall.kind() != Kind.VOID) {
            push(builtinCall);
        }
    }

    private void peekAndCall(Snippet snippet, int stackDepth) {
        final Kind[] parameterKinds = snippet.parameterKinds();
        final TirInstruction[] arguments = state.peekMany(stackDepth, parameterKinds);
        call(snippet, arguments);
    }

    private void call(Snippet snippet) {
        final Kind[] parameterKinds = snippet.parameterKinds();
        final TirInstruction[] arguments = state.popMany(parameterKinds);
        call(snippet, arguments);
    }

    private void call(Snippet snippet, TirInstruction... arguments) {
        final MethodActor method = snippet.executable;
        if (method.isInline()) {
            if (printState.getValue()) {
                append(new TirInstruction.Placeholder("INLINING SNIPPET: " + snippet.executable.simpleName()));
                Console.printThinDivider("INLINING SNIPPET: " + snippet.executable.simpleName());
                final DirMethod dirMethod = DirTracer.makeDirMethod((ClassMethodActor) snippet.executable);
                Trace.stream().println(dirMethod.traceToString());
                Console.printThinDivider();
            }
            final TirInstruction tirResult = DirTracer.trace(snippet, trace, arguments, this);
            if (tirResult != null && method.resultKind().stackKind == tirResult.kind()) {
                push(tirResult);
            } else if (tirResult == null && method.resultKind() == Kind.VOID) {
                // Snippet has no result.
            } else {
                ProgramError.unexpected(tirResult.toString());
            }
        } else {
            call((ClassMethodActor) method, null, arguments);
        }
    }

    private void checkNull(TirInstruction instruction) {
        assert instruction.kind().isReference;
        call(Snippet.CheckNullPointer.SNIPPET, instruction);
    }

    private void callMonitorSnippet(MonitorSnippet snippet) {
        final TirInstruction instruction = pop(Kind.REFERENCE);
        checkNull(instruction);
        call(snippet);
    }

    public class BytecodeRecorder extends BytecodeAggregatingVisitor {
        private ClassMethodActor method;

        public void setMethod(ClassMethodActor method) {
            this.method = method;
        }

        @Override
        protected void constant(Value value) {
            push(value);
        }

        @Override
        protected void store(Kind kind, int index) {
            state.store(kind, index);
        }

        @Override
        protected void load(Kind kind, int slot) {
            state.load(kind, slot);
        }

        @Override
        protected void getField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final TirConstant fieldActorConstant = TirConstant.fromObject(fieldActor);
            final TirInstruction reference;

            if (fieldActor.isStatic()) {
                reference = TirConstant.fromObject(fieldActor.holder().staticTuple());
            } else {
                reference = state.pop(Kind.REFERENCE);
            }

            final Snippet snippet = OpMap.operationSnippet(Operation.GETFIELD, fieldActor.kind);
            call(snippet, reference, fieldActorConstant);
        }

        @Override
        protected void putField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final TirConstant fieldActorConstant = TirConstant.fromObject(fieldActor);
            final TirInstruction value = state.pop(fieldActor.kind);
            final TirInstruction reference;
            if (fieldActor.isStatic()) {
                reference = TirConstant.fromObject(fieldActor.holder().staticTuple());
            } else {
                reference = state.pop(Kind.REFERENCE);
            }
            final Snippet snippet = OpMap.operationSnippet(Operation.PUTFIELD, fieldActor.kind);
            call(snippet, reference, fieldActorConstant, value);
        }

        @Override
        protected void arrayLength() {
            call(ArrayGetSnippet.ReadLength.SNIPPET);
        }

        /*
        final TirState state = takeSnapshot();
        final TirInstruction opearand1 = _state.pop(Kind.INT);
        final TirInstruction opearand0 = _state.pop(Kind.INT);
        final boolean observedResult = _tracer.evaluateIcmpBranch(condition);

        // Convert branch condition into its taken form.
        if (observedResult == false) {
            condition = condition.opposite();
        }
        final TirGuard guard = new TirGuard(opearand0, opearand1, condition, state);
        append(guard);
        */

        private void checkNullPointer(TirInstruction array) {
            scope.profileBranch(BranchMetric.IMPLICIT_NULL_CHECK);
            call(HotpathSnippet.CheckNullPointer.SNIPPET, array);
        }

        private void checkType(ClassActor classActor, TirInstruction object) {
            scope.profileBranch(BranchMetric.IMPLICIT_TYPE_CHECK);
            call(HotpathSnippet.CheckType.SNIPPET, TirConstant.fromObject(classActor), object);
        }

        private void checkArrayIndex(TirInstruction array, TirInstruction index) {
            scope.profileBranch(BranchMetric.IMPLICIT_BRANCH);
            call(Snippet.CheckArrayIndex.SNIPPET, array, index);
        }

        private void checkArrayStore(TirInstruction array, TirInstruction value) {
            scope.profileBranch(BranchMetric.IMPLICIT_TYPE_CHECK);
            call(Snippet.CheckReferenceArrayStore.SNIPPET, array, value);
        }

        @Override
        protected void arrayLoad(Kind kind) {
            final TirInstruction[] arguments = state.peekMany(0, Kind.REFERENCE, Kind.INT);
            final TirInstruction array = arguments[0];
            final TirInstruction index = arguments[1];
            checkNullPointer(array);
            checkArrayIndex(array, index);
            state.popMany(Kind.REFERENCE, Kind.INT);
            call(OpMap.operationSnippet(Operation.ALOAD, kind), array, index);
        }

        @Override
        protected void arrayStore(Kind kind) {
            final TirInstruction[] arguments = state.peekMany(0, Kind.REFERENCE, Kind.INT, kind);
            final TirInstruction array = arguments[0];
            final TirInstruction index = arguments[1];
            final TirInstruction value = arguments[2];
            checkNullPointer(array);
            checkArrayIndex(array, index);
            if (kind.isReference) {
                checkArrayStore(array, value);
            }
            state.popMany(Kind.REFERENCE, Kind.INT, kind);
            call(OpMap.operationSnippet(Operation.ASTORE, kind), array, index, value);
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index) {
            final Resolved resolvedClass = (Resolved) classConstant;
            final TirConstant constant = TirConstant.fromObject(resolvedClass.classActor);
            final TirInstruction length = TirRecorder.this.pop(Kind.INT);
            call(CreateReferenceArray.SNIPPET, constant, length);
        }

        @Override
        protected void allocateArray(Kind kind) {
            final TirInstruction length = TirRecorder.this.pop(Kind.INT);
            final TirConstant kindConstant = TirConstant.fromObject(kind);
            call(CreatePrimitiveArray.SNIPPET, kindConstant, length);
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index, int dimensions) {
            assert false : "Not implemented";
        }

        @Override
        protected void allocate(ClassConstant classConstant, int index) {
            final Resolved resolvedClass = (Resolved) classConstant;
            final TirConstant constant = TirConstant.fromObject(resolvedClass.classActor);
            call(CreateTupleOrHybrid.SNIPPET, constant);
        }

        @Override
        protected void enterMonitor() {
            callMonitorSnippet(MonitorSnippet.MonitorEnter.SNIPPET);
        }

        @Override
        protected void exitMonitor() {
            callMonitorSnippet(MonitorSnippet.MonitorExit.SNIPPET);
        }

        @Override
        protected void execute(Operation operation, Kind kind) {
            final Builtin builtin = OpMap.operationBuiltin(operation, kind);
            if (builtin != null) {
                call(OpMap.operationBuiltin(operation, kind));
            } else {
                // TODO: What is going on here?
                // Check if we need to call an additional snippet.
                final Snippet snippet = OpMap.operationSnippet(operation, kind);
                if (snippet != null) {
                    call(snippet);
                } else {
                    ProgramError.unexpected();
                }
            }
        }

        @Override
        protected void instanceOf(ClassConstant classConstant, int index) {
            final ClassActor cls = classConstant.resolve(constantPool(), index);
            final TirInstruction obj = state.pop(Kind.REFERENCE);
            call(Snippet.InstanceOf.SNIPPET, TirConstant.fromObject(cls), obj);
        }

        @Override
        protected void execute(int bytecode) {
            state.execute(bytecode);
        }

        @Override
        protected void convert(Kind fromKind, Kind toKind) {
            final Builtin builtin = OpMap.conversionBuiltin(fromKind, toKind);
            if (builtin != null) {
                call(OpMap.conversionBuiltin(fromKind, toKind));
            }
            // Check if we need to call an additional snippet.
            final Snippet snippet = OpMap.conversionSnippet(fromKind, toKind);
            if (snippet != null) {
                call(snippet);
            }
        }

        @Override
        protected void increment(int slot, int addend) {
            load(Kind.INT, slot);
            constant(IntValue.from(addend));
            call(OpMap.operationBuiltin(Operation.ADD, Kind.INT));
            store(Kind.INT, slot);
        }

        @Override
        protected void jump(int offset) {
            // Nothing to do here.
        }

        @Override
        protected void branch(BranchCondition condition, int offset) {
            final TirState state = takeSnapshot();
            final TirInstruction opearand0 = TirRecorder.this.state.pop(Kind.INT);
            final TirInstruction opearand1 = new TirConstant(IntValue.ZERO);
            final boolean observedResult = tracer.evaluateBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, trace, null);
            append(guard);

            scope.profileBranch(BranchMetric.EXPLICIT_BRANCH);
        }

        @Override
        protected void icmpBranch(BranchCondition condition, int offset) {
            final TirState state = takeSnapshot();
            final TirInstruction opearand1 = TirRecorder.this.state.pop(Kind.INT);
            final TirInstruction opearand0 = TirRecorder.this.state.pop(Kind.INT);
            final boolean observedResult = tracer.evaluateIcmpBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, trace, null);
            append(guard);

            scope.profileBranch(BranchMetric.EXPLICIT_BRANCH);
        }

        @Override
        protected void nullBranch(BranchCondition condition, int offset) {
            final TirState state = takeSnapshot();
            final TirInstruction opearand0 = TirRecorder.this.state.pop(Kind.REFERENCE);
            final TirInstruction opearand1 = new TirConstant(ReferenceValue.NULL);
            final boolean observedResult = tracer.evaluateNullBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, trace, null);
            append(guard);

            scope.profileBranch(BranchMetric.EXPLICIT_NULL_CHECK);
        }

        @Override
        protected void tableSwitch(int defaultOffset, int lowMatch, int highMatch, int[] switchOffsets) {
            final TirState state = takeSnapshot();
            final int observedIndex = tracer.evaluateInt(0);
            final TirInstruction index = TirRecorder.this.state.pop(Kind.INT);

            // We need to guard that we're following the observed control flow. For switch cases we guard that
            // the index matches the observed index. For the default case we need to check the index against the
            // low and high boundaries.
            //
            // TODO: We should perform an OR operation between these two conditions and then guard on that result,
            // not guard on each boundary check individually, this could lead to a trace explosion since we don't
            // know which check would fail first.
            if (observedIndex < lowMatch) {
                final TirInstruction low = new TirConstant(IntValue.from(lowMatch));
                final TirGuard lowGuard = new TirGuard(index, low, ValueComparator.LESS_THAN, state, trace, null);
                append(lowGuard);
            } else if (observedIndex > highMatch) {
                final TirInstruction high = new TirConstant(IntValue.from(highMatch));
                final TirGuard highGuard = new TirGuard(index, high, ValueComparator.GREATER_THAN, state, trace, null);
                append(highGuard);
            } else {
                final TirInstruction match = new TirConstant(IntValue.from(observedIndex));
                final TirGuard guard = new TirGuard(index, match, ValueComparator.EQUAL, state, trace, null);
                append(guard);
            }
            scope.profileBranch(BranchMetric.SWITCH_BRANCH);
        }

        @Override
        protected void invokeStaticMethod(MethodActor method) {
            invokeTarget(InvocationTarget.findInvokeStaticTarget(method));
        }

        @Override
        protected void invokeVirtualMethod(MethodActor method) {
            final TirInstruction receiver = state.peek(Kind.REFERENCE, method.descriptor().computeNumberOfSlots());
            final Object receiverObject = tracer.evaluateObject(method.descriptor().computeNumberOfSlots());
            checkType(ClassActor.fromJava(receiverObject.getClass()), receiver);
            invokeTarget(InvocationTarget.findInvokeVirtualTarget(method, receiverObject));
        }

        @Override
        protected void invokeInterfaceMethod(MethodActor method) {
            final TirInstruction receiver = state.peek(Kind.REFERENCE, method.descriptor().computeNumberOfSlots());
            final Object receiverObject = tracer.evaluateObject(method.descriptor().computeNumberOfSlots());
            checkType(ClassActor.fromJava(receiverObject.getClass()), receiver);
            invokeTarget(InvocationTarget.findInvokeInterfaceTarget(method, receiverObject));
        }

        @Override
        protected void invokeSpecialMethod(MethodActor method) {
            invokeTarget(InvocationTarget.findInvokeSpecialTarget(classActor(), method));
        }

        protected void invokeTarget(ClassMethodActor method) {
            final ClassMethodActor target = method;
            if (target.isNative()) {
                call(target, takeSnapshot());
            } else {
                final int invokeReturnPosition = currentBCI();
                state.enter(target, invokeReturnPosition);
                if (printState.getValue()) {
                    Console.println(Color.LIGHTRED, "invoked: method: " + method.toString());
                }
            }
        }

        @Override
        protected void methodReturn(Kind kind) {
            if (state.frames().size() > 1) {
                state.leave();
            } else {
                tracer.abort(AbortReason.BREACHED_SCOPE);
            }
        }

        @Override
        protected ConstantPool constantPool() {
            return method.codeAttribute().cp;
        }

        protected ClassActor classActor() {
            return method.holder();
        }

        @Override
        protected void opcodeDecoded() {
            append(new TirInstruction.Placeholder("RECORDING: " + Bytecodes.nameOf(currentOpcode())));
            if (printState.getValue()) {
                state.println(NameMap.COMPACT);
                Console.println();
                Console.println(Color.LIGHTRED, "recording opcode: method: " + state.last().method() + " pc: " + currentOpcodeBCI() + ", op: " + currentOpcode());
            }
        }

    }

    public TirState state() {
        return state;
    }

    public TirState takeSnapshot() {
        return state.copy();
    }

    /**
     * Takes a state snapshot using the bytecode position at the specified anchor. This differs from the
     * {@code takeSnapshot()} which assumes the bytecode position is given by the {@code currentOpcodePosition()}.
     * This method is typically used whenever a snapshot is taken without a call to {@link #record(BytecodeLocation)}.
     */
    public TirState takeSnapshot(TreeAnchor anchor) {
        final TirState state = this.state.copy();
        state.last().setPc(anchor.location().bci);
        return state;
    }
}
