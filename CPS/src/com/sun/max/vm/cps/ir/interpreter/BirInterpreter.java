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
package com.sun.max.vm.cps.ir.interpreter;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateMultiReferenceArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreatePrimitiveArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateReferenceArray;
import com.sun.max.vm.compiler.snippet.Snippet.MakeHolderInitialized;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.AsynchronousProfiler.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class BirInterpreter extends IrInterpreter<BirMethod> {
    public static OptionSet optionSet = new OptionSet();
    public static Option<Boolean> printState = optionSet.newBooleanOption("PS", false, "(P)rints the Interpreter's Execution (S)tate.");

    public static class BirMethodCache {
        private static Mapping<ClassMethodActor, BirMethod> methods = new IdentityHashMapping<ClassMethodActor, BirMethod>();

        public static BirMethod lookup(ClassMethodActor classMethodActor) {
            BirMethod method = methods.get(classMethodActor);
            if (method == null) {
                method = generateBirMethod(classMethodActor);
                methods.put(classMethodActor, method);
            }
            return method;
        }

        public static BirMethod generateBirMethod(ClassMethodActor classMethodActor) {
            final BirGeneratorScheme birCompilerScheme = (BirGeneratorScheme) CPSCompiler.Static.compiler();
            final BirMethod birMethod = birCompilerScheme.birGenerator().makeIrMethod(classMethodActor, true);
            return birMethod;
        }
    }

    public static interface Profiler {
        void jump(BytecodeLocation fromlocation, BytecodeLocation toLocation, BirState state);
        void invoke(ClassMethodActor target, BirState state);
        void trace(BytecodeLocation location, BirState state);
    }

    static final Value filler = Value.fromBoxedJavaValue("%");
    static final Value undefined = Value.fromBoxedJavaValue("_");

    private final BirState state = new BirState();
    private final Profiler profiler;

    public BirInterpreter(Profiler profiler) {
        this.profiler = profiler;
    }

    private int jumpPosition = -1;

    protected void jumpTo(int position) {
        jumpPosition = position;
    }

    protected int resetJump() {
        final int position = jumpPosition;
        jumpPosition = -1;
        return position;
    }

    protected boolean needsToJump() {
        return jumpPosition >= 0;
    }

    private Value toStackValue(Value value) {
        if (value.kind().stackKind != value.kind()) {
            return value.kind().stackKind.convert(value);
        }
        return value;
    }

    public Value execute(ClassMethodActor method, Value... arguments) {
        return execute(BirMethodCache.lookup(method), arguments);
    }

    @Override
    public Value execute(IrMethod method, Value... arguments) {
        final BytecodeEvaluator evaluator = new BytecodeEvaluator();
        final BytecodeScanner scanner = new BytecodeScanner(evaluator);

        for (Value argument : arguments) {
            state.push(toStackValue(argument));
        }

        state.enter(method.classMethodActor(), 0);

        while (state.hasFrames()) {
            profileTrace(state.last().method(), state.position());
            final int follow = scanner.scanInstruction(state.code(), state.position());
            if (needsToJump()) {
                final int fromPosition = state.position();
                state.setPosition(resetJump());
                profileJump(state.last().method(), fromPosition, state.position());
            } else {
                state.setPosition(follow);
            }
        }

        final Kind resultKind = method.classMethodActor().resultKind();

        if (resultKind != Kind.VOID) {
            return state.pop(resultKind);
        }
        return Kind.VOID.zeroValue();
    }

    private void profileTrace(ClassMethodActor method, int position) {
        if (profiler != null) {
            profiler.trace(new BytecodeLocation(method, position), state);
        }
    }

    private void profileJump(ClassMethodActor method, int fromPosition, int toPosition) {
        if (profiler != null) {
            profiler.jump(new BytecodeLocation(method, fromPosition), new BytecodeLocation(method, toPosition), state);
        }
    }

    private void profileInvoke(ClassMethodActor target) {
        if (profiler != null) {
            profiler.invoke(target, state);
        }
    }

    public class BytecodeEvaluator extends BytecodeAggregatingVisitor {

        @Override
        protected void constant(Value value) {
            state.push(toStackValue(value));
        }

        @Override
        protected void store(Kind kind, int slot) {
            state.store(kind, slot);
        }

        @Override
        protected void load(Kind kind, int slot) {
            state.load(kind, slot);
        }

        @Override
        protected void arrayLoad(Kind kind) {
            final Snippet snippet = OpMap.operationSnippet(Operation.ALOAD, kind);
            execute(snippet.executable);
        }

        @Override
        protected void arrayStore(Kind kind) {
            final Snippet snippet = OpMap.operationSnippet(Operation.ASTORE, kind);
            execute(snippet.executable);
        }

        @Override
        protected void execute(Operation operation, Kind kind) {
            final Builtin builtin = OpMap.operationBuiltin(operation, kind);
            execute(builtin.executable);
        }

        @Override
        protected void convert(Kind fromKind, Kind toKind) {
            final Builtin builtin = OpMap.conversionBuiltin(fromKind, toKind);
            if (builtin != null) {
                execute(builtin.executable);
            }
            final Snippet snippet = OpMap.conversionSnippet(fromKind, toKind);
            if (snippet != null) {
                execute(snippet.executable);
            }
        }

        private void execute(final MethodActor methodActor) {

            try {
                final Value[] arguments =  state.popMany(methodActor.getParameterKinds());
                final Value result = methodActor.invoke(arguments);
                if (methodActor.resultKind() != Kind.VOID) {
                    state.push(toStackValue(result));
                }
            } catch (IllegalAccessException e) {
                ProgramError.unexpected(e);
            } catch (InvocationTargetException e) {
                catchException(e.getTargetException());
            }
        }

        @Override
        protected void arrayLength() {
            execute(ArrayGetSnippet.ReadLength.SNIPPET.executable);
        }

        @Override
        protected void allocate(ClassConstant classConstant, int index) {
            final ClassActor classActor = classConstant.resolve(constantPool(), index);
            Object reference = null;
            try {
                reference = Objects.allocateInstance(classActor.toJava());
            } catch (InstantiationException e) {
                ProgramError.unexpected();
                return;
            }
            state.push(ReferenceValue.from(reference));
        }

        @Override
        protected void allocateArray(Kind kind) {
            final Value length = state.pop(Kind.INT);
            Object array;
            try {
                array = CreatePrimitiveArray.createPrimitiveArray(kind, length.asInt());
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            state.push(ReferenceValue.from(array));
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index) {
            final ClassActor componentClassActor = classConstant.resolve(constantPool(), index);
            final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(componentClassActor);
            final Value length = state.pop(Kind.INT);
            Object array;
            try {
                array = CreateReferenceArray.createReferenceArray(arrayClassActor, length.asInt());
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            state.push(ReferenceValue.from(array));
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index, int dimensions) {
            final ArrayClassActor arrayClassActor = (ArrayClassActor) classConstant.resolve(constantPool(), index);
            final int[] lengths = new int[dimensions];
            for (int i = dimensions - 1; i >= 0; i--) {
                lengths[i] = state.pop(Kind.INT).asInt();
            }
            Object array;
            try {
                array = CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths);
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            state.push(ReferenceValue.from(array));
        }

        @Override
        protected void getField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final Value reference;
            if (fieldActor.isStatic()) {
                reference = ReferenceValue.from(fieldActor.holder().staticTuple());
            } else {
                reference = state.pop(Kind.REFERENCE);
            }
            state.push(toStackValue(fieldActor.getValue(reference.asObject())));
        }

        @Override
        protected void putField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final Value value = fieldActor.kind.convert(state.pop(fieldActor.kind));
            final Value reference;
            if (fieldActor.isStatic()) {
                reference = ReferenceValue.from(fieldActor.holder().staticTuple());
            } else {
                reference = state.pop(Kind.REFERENCE);
            }

            fieldActor.kind.writeErasedValue(reference.asObject(), fieldActor.offset(), value);
        }

        @Override
        protected void invokeStaticMethod(MethodActor method) {
            final StaticMethodActor staticMethodActor = InvocationTarget.findInvokeStaticTarget(method);
            MakeHolderInitialized.makeHolderInitialized(staticMethodActor);
            profileInvoke(staticMethodActor);
            if (staticMethodActor.isNative()) {
                execute(staticMethodActor);
                return;
            }
            state.enter(staticMethodActor, currentBCI());
            jumpTo(0);
        }

        @Override
        protected void invokeVirtualMethod(MethodActor method) {
            final VirtualMethodActor declaredMethod = (VirtualMethodActor) method;
            MakeHolderInitialized.makeHolderInitialized(declaredMethod);
            final Object receiver = state.peek(Kind.REFERENCE, declaredMethod.numberOfParameterSlots() - 1).asObject();
            if (receiver == null) {
                catchException(new NullPointerException());
                return;
            }
            final VirtualMethodActor virtualMethodActor = InvocationTarget.findInvokeVirtualTarget(declaredMethod, receiver);
            profileInvoke(virtualMethodActor);
            MakeHolderInitialized.makeHolderInitialized(virtualMethodActor);
            if (virtualMethodActor.isNative()) {
                execute(virtualMethodActor);
                return;
            }
            state.enter(virtualMethodActor, currentBCI());
            jumpTo(0);
        }

        @Override
        protected void invokeInterfaceMethod(MethodActor method) {
            final InterfaceMethodActor declaredMethod = (InterfaceMethodActor) method;
            MakeHolderInitialized.makeHolderInitialized(declaredMethod);
            final Object receiver = state.peek(Kind.REFERENCE, declaredMethod.descriptor().computeNumberOfSlots()).asObject();
            if (receiver == null) {
                catchException(new NullPointerException());
                return;
            }
            final VirtualMethodActor virtualMethodActor = InvocationTarget.findInvokeInterfaceTarget(declaredMethod, receiver);
            MakeHolderInitialized.makeHolderInitialized(virtualMethodActor);
            profileInvoke(virtualMethodActor);
            if (virtualMethodActor.isNative()) {
                execute(virtualMethodActor);
                return;
            }
            state.enter(virtualMethodActor, currentBCI());
            jumpTo(0);
        }

        @Override
        protected void invokeSpecialMethod(MethodActor method) {
            final VirtualMethodActor virtualMethodActor = InvocationTarget.findInvokeSpecialTarget(classActor(), method);
            profileInvoke(virtualMethodActor);
            if (virtualMethodActor.isNative()) {
                execute(virtualMethodActor);
                return;
            }
            state.enter(virtualMethodActor, currentBCI());
            jumpTo(0);
        }

        @Override
        protected void tableSwitch(int defaultOffset, int lowMatch, int highMatch, int[] caseOffsets) {
            final int index = state.pop(Kind.INT).asInt();
            if (index < lowMatch || index > highMatch) {
                jumpTo(currentOpcodeBCI() + defaultOffset);
            } else {
                jumpTo(currentOpcodeBCI() + caseOffsets[index - lowMatch]);
            }
        }

        @Override
        protected void lookupSwitch(int defaultOffset, int[] switchCases, int[] switchOffsets) {
            final int index = state.pop(Kind.INT).asInt();
            for (int i = 0; i < switchCases.length; i++) {
                if (switchCases[i] == index) {
                    jumpTo(currentOpcodeBCI() + switchOffsets[i]);
                    return;
                }
            }
            jumpTo(currentOpcodeBCI() + defaultOffset);
        }

        @Override
        protected void throwException() {
            final Value exception = state.pop(Kind.REFERENCE);
            final Throwable exceptionObject = exception.asObject() == null ? new NullPointerException() : (Throwable) exception.asObject();
            catchException(exceptionObject);
        }

        private void catchException(final Throwable exceptionObject) throws ProgramError {
            state.last().empty();
            state.push(ReferenceValue.from(exceptionObject));

            while (state.hasFrames()) {

                // Try to catch exception using the current method's exception handler table.
                for (ExceptionHandlerEntry handler : state.last().method().codeAttribute().exceptionHandlerTable()) {
                    if (currentOpcodeBCI() >= handler.startBCI() && currentOpcodeBCI() < handler.endBCI()) {
                        /*
                        final int index = handler.catchTypeIndex();
                        final ClassConstant classConstant = resolveClass(index);
                        final ClassActor classActor = classConstant.resolve(constantPool(), index);
                        boolean caught = Snippet.InstanceOf.instanceOf(classActor, exceptionObject);
                        */

                        jumpTo(handler.handlerBCI());
                        return;
                    }
                }

                state.leaveWithoutReturn();
                if (state.hasFrames()) {
                    jumpTo(state.position());
                }
            }

            throw ProgramError.unexpected(exceptionObject);
        }

        @Override
        protected void methodReturn(Kind kind) {
            state.leave();
            jumpTo(state.position());
        }

        @Override
        protected void jump(int offset) {
            jumpTo(currentOpcodeBCI() + offset);
        }

        @Override
        protected void increment(int slot, int addend) {
            final Value value = state.getSlot(slot);
            if (value instanceof IntValue) {
                state.store(slot, IntValue.from(value.asInt() + addend));
            } else if (value instanceof LongValue) {
                state.store(slot, LongValue.from(value.asLong() + addend));
            } else {
                assert false;
            }
        }

        @Override
        protected void acmpBranch(BranchCondition condition, int offset) {
            final ReferenceValue b = (ReferenceValue) state.pop(Kind.REFERENCE);
            final ReferenceValue a = (ReferenceValue) state.pop(Kind.REFERENCE);
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodeBCI() + offset);
            }
        }

        @Override
        protected void icmpBranch(BranchCondition condition, int offset) {
            final Value b = state.pop(Kind.INT);
            final Value a = state.pop(Kind.INT);
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodeBCI() + offset);
            }
        }

        @Override
        protected void nullBranch(BranchCondition condition, int offset) {
            final ReferenceValue a = (ReferenceValue) state.pop(Kind.REFERENCE);
            final ReferenceValue b = ReferenceValue.NULL;
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodeBCI() + offset);
            }
        }

        @Override
        protected void branch(BranchCondition condition, int offset) {
            final Value a = state.pop(Kind.INT);
            if (condition.evaluate(a, IntValue.ZERO)) {
                jumpTo(currentOpcodeBCI() + offset);
            }
        }

        @Override
        protected void instanceOf(ClassConstant classConstant, int index) {
            final Value reference = state.pop(Kind.REFERENCE);
            final ClassActor classActor = classConstant.resolve(constantPool(), index);
            final boolean result = Snippet.InstanceOf.instanceOf(classActor, reference.asObject());
            state.push(toStackValue(BooleanValue.from(result)));
        }

        @Override
        protected void checkCast(ClassConstant classConstant, int index) {
            final Value reference = state.peek(Kind.REFERENCE, 0);
            final ClassActor classActor = classConstant.resolve(constantPool(), index);
            Snippet.CheckCast.checkCast(classActor, reference.asObject());
            // TODO: Deal with exceptions here.
        }

        @Override
        protected void enterMonitor() {
            final Value reference = state.pop(Kind.REFERENCE);
            Monitor.enter(reference.asObject());
        }

        @Override
        protected void exitMonitor() {
            final Value reference = state.pop(Kind.REFERENCE);
            Monitor.exit(reference.asObject());
        }

        @Override
        protected void execute(int opcode) {
            state.execute(opcode);
        }

        @Override
        protected void opcodeDecoded() {
            AsynchronousProfiler.event(CounterMetric.INTERPRETED_BYTECODES);
            if (printState.getValue()) {
                state.println();
                Console.println(Color.LIGHTMAGENTA, "opcode: method: " + state.last().method() + " pc: " + currentOpcodeBCI() + ", op: " + currentOpcode());
            }
        }

        @Override
        protected ConstantPool constantPool() {
            return state.last().method().compilee().codeAttribute().cp;
        }

        protected ClassActor classActor() {
            return state.last().method().compilee().holder();
        }
    }
}
