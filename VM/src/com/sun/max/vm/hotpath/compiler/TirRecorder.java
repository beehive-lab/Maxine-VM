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
package com.sun.max.vm.hotpath.compiler;


import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ClassConstant.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.Console.*;
import com.sun.max.vm.hotpath.compiler.Tracer.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirRecorder {
    public static OptionSet _optionSet = new OptionSet();
    public static Option<Boolean> _printState = _optionSet.newBooleanOption("PRS", true, "(P)rints the Trace (R)ecorders's (S)tate.");

    private final BytecodeRecorder _visitor = new BytecodeRecorder();
    private final BytecodeScanner _scanner = new BytecodeScanner(_visitor);
    private final TirTrace _trace;
    private final TirState _state;
    private final Tracer _tracer;
    private final Scope _scope;

    public TirRecorder(Tracer tracer, Scope scope, TirState state, TirTrace trace) {
        _tracer = tracer;
        _trace = trace;
        _state = state;
        _scope = scope;
    }

    public void record(BytecodeLocation location) {
        _state.last().setPc(location.position());
        _visitor.setMethod(location.classMethodActor());
        final byte[] bytecode = location.classMethodActor().codeAttribute().code();
        _scanner.scanInstruction(bytecode, location.position());
    }

    public void recordNesting(final TreeAnchor anchor, Bailout bailout) {
        final TirState state = takeSnapshot(anchor);
        final TirTreeCall call = new TirTreeCall(anchor.tree(), state);
        append(call);
        if (_printState.getValue()) {
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
                final TirNestedLocal nestedLocal = new TirNestedLocal(call, _index);
                nestedLocal.setKind(exit.kind());
                _state.store(_index, nestedLocal);
                append(nestedLocal);
            }
        });
        _state.append(bailout.guard().state().frames());
    }

    private void push(TirInstruction instruction) {
        _state.push(instruction);
    }

    private void push(Value value) {
        push(new TirConstant(value));
    }

    private TirInstruction pop(Kind kind) {
        return _state.pop(kind);
    }

    private void append(TirInstruction instruction) {
        _trace.append(instruction);
    }

    private void pushAndAppend(TirInstruction instruction) {
        push(instruction);
        append(instruction);
    }

    private void call(ClassMethodActor target, TirState state) {
        final Kind[] parameterKinds = target.getParameterKinds();
        final TirInstruction[] arguments = _state.popMany(parameterKinds);
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
        final TirInstruction[] arguments = _state.popMany(parameterKinds);
        final TirCall builtinCall = new TirBuiltinCall(builtin, arguments);
        append(builtinCall);
        if (builtinCall.kind() != Kind.VOID) {
            push(builtinCall);
        }
    }

    private void peekAndCall(Snippet snippet, int stackDepth) {
        final Kind[] parameterKinds = snippet.parameterKinds();
        final TirInstruction[] arguments = _state.peekMany(stackDepth, parameterKinds);
        call(snippet, arguments);
    }

    private void call(Snippet snippet) {
        final Kind[] parameterKinds = snippet.parameterKinds();
        final TirInstruction[] arguments = _state.popMany(parameterKinds);
        call(snippet, arguments);
    }

    private void call(Snippet snippet, TirInstruction... arguments) {
        final MethodActor method = snippet.foldingMethodActor();
        if (method.isInline()) {
            if (_printState.getValue()) {
                append(new TirInstruction.Placeholder("INLINING SNIPPET: " + snippet.foldingMethodActor().simpleName()));
                Console.printThinDivider("INLINING SNIPPET: " + snippet.foldingMethodActor().simpleName());
                final DirMethod dirMethod = DirTracer.makeDirMethod((ClassMethodActor) snippet.foldingMethodActor());
                Trace.stream().println(dirMethod.traceToString());
                Console.printThinDivider();
            }
            final TirInstruction tirResult = DirTracer.trace(snippet, _trace, arguments, this);
            if (tirResult != null && method.resultKind().toStackKind() == tirResult.kind()) {
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
        assert instruction.kind() == Kind.REFERENCE;
        call(Snippet.CheckNullPointer.SNIPPET, instruction);
    }

    private void callMonitorSnippet(MonitorSnippet snippet) {
        final TirInstruction instruction = pop(Kind.REFERENCE);
        checkNull(instruction);
        call(snippet);
    }


    public class BytecodeRecorder extends BytecodeAggregatingVisitor {
        private ClassMethodActor _method = null;

        public void setMethod(ClassMethodActor method) {
            _method = method;
        }

        @Override
        protected void constant(Value value) {
            push(value);
        }

        @Override
        protected void store(Kind kind, int index) {
            _state.store(kind, index);
        }

        @Override
        protected void load(Kind kind, int slot) {
            _state.load(kind, slot);
        }

        @Override
        protected void getField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final TirConstant fieldActorConstant = TirConstant.fromObject(fieldActor);
            final TirInstruction reference;

            if (fieldActor.isStatic()) {
                reference = TirConstant.fromObject(fieldActor.holder().staticTuple());
            } else {
                reference = _state.pop(Kind.REFERENCE);
            }

            final Snippet snippet = OpMap.operationSnippet(Operation.GETFIELD, fieldActor.kind());
            call(snippet, reference, fieldActorConstant);
        }

        @Override
        protected void putField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final TirConstant fieldActorConstant = TirConstant.fromObject(fieldActor);
            final TirInstruction value = _state.pop(fieldActor.kind());
            final TirInstruction reference;
            if (fieldActor.isStatic()) {
                reference = TirConstant.fromObject(fieldActor.holder().staticTuple());
            } else {
                reference = _state.pop(Kind.REFERENCE);
            }
            final Snippet snippet = OpMap.operationSnippet(Operation.PUTFIELD, fieldActor.kind());
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
            _scope.profileBranch(BranchMetric.IMPLICIT_NULL_CHECK);
            call(HotpathSnippet.CheckNullPointer.SNIPPET, array);
        }

        private void checkType(ClassActor classActor, TirInstruction object) {
            _scope.profileBranch(BranchMetric.IMPLICIT_TYPE_CHECK);
            call(HotpathSnippet.CheckType.SNIPPET, TirConstant.fromObject(classActor), object);
        }

        private void checkArrayIndex(TirInstruction array, TirInstruction index) {
            _scope.profileBranch(BranchMetric.IMPLICIT_BRANCH);
            call(Snippet.CheckArrayIndex.SNIPPET, array, index);
        }

        private void checkArrayStore(TirInstruction array, TirInstruction value) {
            _scope.profileBranch(BranchMetric.IMPLICIT_TYPE_CHECK);
            call(Snippet.CheckReferenceArrayStore.SNIPPET, array, value);
        }

        @Override
        protected void arrayLoad(Kind kind) {
            final TirInstruction[] arguments = _state.peekMany(0, Kind.REFERENCE, Kind.INT);
            final TirInstruction array = arguments[0];
            final TirInstruction index = arguments[1];
            checkNullPointer(array);
            checkArrayIndex(array, index);
            _state.popMany(Kind.REFERENCE, Kind.INT);
            call(OpMap.operationSnippet(Operation.ALOAD, kind), array, index);
        }

        @Override
        protected void arrayStore(Kind kind) {
            final TirInstruction[] arguments = _state.peekMany(0, Kind.REFERENCE, Kind.INT, kind);
            final TirInstruction array = arguments[0];
            final TirInstruction index = arguments[1];
            final TirInstruction value = arguments[2];
            checkNullPointer(array);
            checkArrayIndex(array, index);
            if (kind == Kind.REFERENCE) {
                checkArrayStore(array, value);
            }
            _state.popMany(Kind.REFERENCE, Kind.INT, kind);
            call(OpMap.operationSnippet(Operation.ASTORE, kind), array, index, value);
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index) {
            final Resolved resolvedClass = (Resolved) classConstant;
            final TirConstant constant = TirConstant.fromObject(resolvedClass.classActor());
            final TirInstruction length = TirRecorder.this.pop(Kind.INT);
            call(NonFoldableSnippet.CreateReferenceArray.SNIPPET, constant, length);
        }

        @Override
        protected void allocateArray(Kind kind) {
            final TirInstruction length = TirRecorder.this.pop(Kind.INT);
            final TirConstant kindConstant = TirConstant.fromObject(kind);
            call(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET, kindConstant, length);
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index, int dimensions) {
            assert false : "Not implemented";
        }

        @Override
        protected void allocate(ClassConstant classConstant, int index) {
            final Resolved resolvedClass = (Resolved) classConstant;
            final TirConstant constant = TirConstant.fromObject(resolvedClass.classActor());
            call(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET, constant);
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
            final TirInstruction obj = _state.pop(Kind.REFERENCE);
            call(Snippet.InstanceOf.SNIPPET, TirConstant.fromObject(cls), obj);
        }

        @Override
        protected void execute(Bytecode bytecode) {
            _state.execute(bytecode);
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
            final TirInstruction opearand0 = _state.pop(Kind.INT);
            final TirInstruction opearand1 = new TirConstant(IntValue.ZERO);
            final boolean observedResult = _tracer.evaluateBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, _trace, null);
            append(guard);

            _scope.profileBranch(BranchMetric.EXPLICIT_BRANCH);
        }

        @Override
        protected void icmpBranch(BranchCondition condition, int offset) {
            final TirState state = takeSnapshot();
            final TirInstruction opearand1 = _state.pop(Kind.INT);
            final TirInstruction opearand0 = _state.pop(Kind.INT);
            final boolean observedResult = _tracer.evaluateIcmpBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, _trace, null);
            append(guard);

            _scope.profileBranch(BranchMetric.EXPLICIT_BRANCH);
        }

        @Override
        protected void nullBranch(BranchCondition condition, int offset) {
            final TirState state = takeSnapshot();
            final TirInstruction opearand0 = _state.pop(Kind.REFERENCE);
            final TirInstruction opearand1 = new TirConstant(ReferenceValue.NULL);
            final boolean observedResult = _tracer.evaluateNullBranch(condition);
            BranchCondition observedCondition = condition;

            // Convert branch condition into its taken form.
            if (observedResult == false) {
                observedCondition = observedCondition.opposite();
            }
            final TirGuard guard = new TirGuard(opearand0, opearand1, ValueComparator.fromBranchCondition(observedCondition), state, _trace, null);
            append(guard);

            _scope.profileBranch(BranchMetric.EXPLICIT_NULL_CHECK);
        }

        @Override
        protected void tableSwitch(int defaultOffset, int lowMatch, int highMatch, int[] switchOffsets) {
            final TirState state = takeSnapshot();
            final int observedIndex = _tracer.evaluateInt(0);
            final TirInstruction index = _state.pop(Kind.INT);

            // We need to guard that we're following the observed control flow. For switch cases we guard that
            // the index matches the observed index. For the default case we need to check the index against the
            // low and high boundaries.
            //
            // TODO: We should perform an OR operation between these two conditions and then guard on that result,
            // not guard on each boundary check individually, this could lead to a trace explosion since we don't
            // know which check would fail first.
            if (observedIndex < lowMatch) {
                final TirInstruction low = new TirConstant(IntValue.from(lowMatch));
                final TirGuard lowGuard = new TirGuard(index, low, ValueComparator.LESS_THAN, state, _trace, null);
                append(lowGuard);
            } else if (observedIndex > highMatch) {
                final TirInstruction high = new TirConstant(IntValue.from(highMatch));
                final TirGuard highGuard = new TirGuard(index, high, ValueComparator.GREATER_THAN, state, _trace, null);
                append(highGuard);
            } else {
                final TirInstruction match = new TirConstant(IntValue.from(observedIndex));
                final TirGuard guard = new TirGuard(index, match, ValueComparator.EQUAL, state, _trace, null);
                append(guard);
            }
            _scope.profileBranch(BranchMetric.SWITCH_BRANCH);
        }

        @Override
        protected void invokeStaticMethod(MethodActor method) {
            invokeTarget(InvocationTarget.findInvokeStaticTarget(method));
        }

        @Override
        protected void invokeVirtualMethod(MethodActor method) {
            final TirInstruction receiver = _state.peek(Kind.REFERENCE, method.descriptor().getNumberOfLocals());
            final Object receiverObject = _tracer.evaluateObject(method.descriptor().getNumberOfLocals());
            checkType(ClassActor.fromJava(receiverObject.getClass()), receiver);
            invokeTarget(InvocationTarget.findInvokeVirtualTarget(method, receiverObject));
        }

        @Override
        protected void invokeInterfaceMethod(MethodActor method) {
            final TirInstruction receiver = _state.peek(Kind.REFERENCE, method.descriptor().getNumberOfLocals());
            final Object receiverObject = _tracer.evaluateObject(method.descriptor().getNumberOfLocals());
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
                final int invokeReturnPosition = currentByteAddress();
                _state.enter(target, invokeReturnPosition);
                if (_printState.getValue()) {
                    Console.println(Color.LIGHTRED, "invoked: method: " + method.toString());
                }
            }
        }

        @Override
        protected void methodReturn(Kind kind) {
            if (_state.frames().length() > 1) {
                _state.leave();
            } else {
                _tracer.abort(AbortReason.BREACHED_SCOPE);
            }
        }

        @Override
        protected ConstantPool constantPool() {
            return _method.codeAttribute().constantPool();
        }

        protected ClassActor classActor() {
            return _method.holder();
        }

        @Override
        protected void opcodeDecoded() {
            append(new TirInstruction.Placeholder("RECORDING: " + currentOpcode().toString()));
            if (_printState.getValue()) {
                _state.println(NameMap.COMPACT);
                Console.println();
                Console.println(Color.LIGHTRED, "recording opcode: method: " + _state.last().method() + " pc: " + currentOpcodePosition() + ", op: " + currentOpcode());
            }
        }

    }

    public TirState state() {
        return _state;
    }

    public TirState takeSnapshot() {
        return _state.copy();
    }

    /**
     * Takes a state snapshot using the bytecode position at the specified anchor. This differs from the
     * {@code takeSnapshot()} which assumes the bytecode position is given by the {@code currentOpcodePosition()}.
     * This method is typically used whenever a snapshot is taken without a call to {@link #record(BytecodeLocation)}.
     */
    public TirState takeSnapshot(TreeAnchor anchor) {
        final TirState state = _state.copy();
        state.last().setPc(anchor.location().position());
        return state;
    }
}
