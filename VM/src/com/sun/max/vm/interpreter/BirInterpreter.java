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
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.hotpath.AsynchronousProfiler;
import com.sun.max.vm.hotpath.AsynchronousProfiler.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class BirInterpreter extends IrInterpreter<BirMethod> {
    public static OptionSet _optionSet = new OptionSet();
    public static Option<Boolean> printState = _optionSet.newBooleanOption("PS", false, "(P)rints the Interpreter's Execution (S)tate.");

    public static class BirMethodCache {
        private static GrowableMapping<ClassMethodActor, BirMethod> methods = new IdentityHashMapping<ClassMethodActor, BirMethod>();

        public static BirMethod lookup(ClassMethodActor classMethodActor) {
            BirMethod method = methods.get(classMethodActor);
            if (method == null) {
                method = generateBirMethod(classMethodActor);
                methods.put(classMethodActor, method);
            }
            return method;
        }

        public static BirMethod generateBirMethod(ClassMethodActor classMethodActor) {
            final BirGeneratorScheme birCompilerScheme = (BirGeneratorScheme) VMConfiguration.target().compilerScheme();
            final BirMethod birMethod = birCompilerScheme.birGenerator().makeIrMethod(classMethodActor);
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

    private final BirState _state = new BirState();
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
        if (value.kind().toStackKind() != value.kind()) {
            return value.kind().toStackKind().convert(value);
        }
        return value;
    }

    public Value execute(ClassMethodActor method, Value... arguments) {
        return execute(BirMethodCache.lookup(method), arguments);
    }

    @Override
    public Value execute(BirMethod method, Value... arguments) {
        final BytecodeEvaluator evaluator = new BytecodeEvaluator();
        final BytecodeScanner scanner = new BytecodeScanner(evaluator);

        for (Value argument : arguments) {
            _state.push(toStackValue(argument));
        }

        _state.enter(method.classMethodActor(), 0);

        while (_state.hasFrames()) {
            profileTrace(_state.last().method(), _state.position());
            final int follow = scanner.scanInstruction(_state.code(), _state.position());
            if (needsToJump()) {
                final int fromPosition = _state.position();
                _state.setPosition(resetJump());
                profileJump(_state.last().method(), fromPosition, _state.position());
            } else {
                _state.setPosition(follow);
            }
        }

        final Kind resultKind = method.classMethodActor().resultKind();

        if (resultKind != Kind.VOID) {
            return _state.pop(resultKind);
        }
        return Kind.VOID.zeroValue();
    }

    private void profileTrace(ClassMethodActor method, int position) {
        if (profiler != null) {
            profiler.trace(new BytecodeLocation(method, position), _state);
        }
    }

    private void profileJump(ClassMethodActor method, int fromPosition, int toPosition) {
        if (profiler != null) {
            profiler.jump(new BytecodeLocation(method, fromPosition), new BytecodeLocation(method, toPosition), _state);
        }
    }

    private void profileInvoke(ClassMethodActor target) {
        if (profiler != null) {
            profiler.invoke(target, _state);
        }
    }

    public class BytecodeEvaluator extends BytecodeAggregatingVisitor {

        @Override
        protected void constant(Value value) {
            _state.push(toStackValue(value));
        }

        @Override
        protected void store(Kind kind, int slot) {
            _state.store(kind, slot);
        }

        @Override
        protected void load(Kind kind, int slot) {
            _state.load(kind, slot);
        }

        @Override
        protected void arrayLoad(Kind kind) {
            final Snippet snippet = OpMap.operationSnippet(Operation.ALOAD, kind);
            execute(snippet.foldingMethodActor());
        }

        @Override
        protected void arrayStore(Kind kind) {
            final Snippet snippet = OpMap.operationSnippet(Operation.ASTORE, kind);
            execute(snippet.foldingMethodActor());
        }

        @Override
        protected void execute(Operation operation, Kind kind) {
            final Builtin builtin = OpMap.operationBuiltin(operation, kind);
            execute(builtin.foldingMethodActor());
        }

        @Override
        protected void convert(Kind fromKind, Kind toKind) {
            final Builtin builtin = OpMap.conversionBuiltin(fromKind, toKind);
            if (builtin != null) {
                execute(builtin.foldingMethodActor());
            }
            final Snippet snippet = OpMap.conversionSnippet(fromKind, toKind);
            if (snippet != null) {
                execute(snippet.foldingMethodActor());
            }
        }

        private void execute(final MethodActor methodActor) {

            try {
                final Value[] arguments =  _state.popMany(methodActor.getParameterKinds());
                final Value result = methodActor.invoke(arguments);
                if (methodActor.resultKind() != Kind.VOID) {
                    _state.push(toStackValue(result));
                }
            } catch (IllegalAccessException e) {
                ProgramError.unexpected(e);
            } catch (InvocationTargetException e) {
                catchException(e.getTargetException());
            }
        }

        @Override
        protected void arrayLength() {
            execute(ArrayGetSnippet.ReadLength.SNIPPET.foldingMethodActor());
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
            _state.push(ReferenceValue.from(reference));
        }

        @Override
        protected void allocateArray(Kind kind) {
            final Value length = _state.pop(Kind.INT);
            Object array;
            try {
                array = NonFoldableSnippet.CreatePrimitiveArray.createPrimitiveArray(kind, length.asInt());
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            _state.push(ReferenceValue.from(array));
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index) {
            final ClassActor componentClassActor = classConstant.resolve(constantPool(), index);
            final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(componentClassActor);
            final Value length = _state.pop(Kind.INT);
            Object array;
            try {
                array = NonFoldableSnippet.CreateReferenceArray.createReferenceArray(arrayClassActor, length.asInt());
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            _state.push(ReferenceValue.from(array));
        }

        @Override
        protected void allocateArray(ClassConstant classConstant, int index, int dimensions) {
            final ArrayClassActor arrayClassActor = (ArrayClassActor) classConstant.resolve(constantPool(), index);
            final int[] lengths = new int[dimensions];
            for (int i = dimensions - 1; i >= 0; i--) {
                lengths[i] = _state.pop(Kind.INT).asInt();
            }
            Object array;
            try {
                array = NonFoldableSnippet.CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths);
            } catch (Throwable e) {
                catchException(e);
                return;
            }
            _state.push(ReferenceValue.from(array));
        }

        @Override
        protected void getField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final Value reference;
            if (fieldActor.isStatic()) {
                reference = ReferenceValue.from(fieldActor.holder().staticTuple());
            } else {
                reference = _state.pop(Kind.REFERENCE);
            }
            _state.push(toStackValue(fieldActor.readValue(reference.asReference())));
        }

        @Override
        protected void putField(FieldRefConstant resolvedField, int index) {
            final FieldActor fieldActor = resolvedField.resolve(constantPool(), index);
            final Value value = fieldActor.kind().convert(_state.pop(fieldActor.kind()));
            final Value reference;
            if (fieldActor.isStatic()) {
                reference = ReferenceValue.from(fieldActor.holder().staticTuple());
            } else {
                reference = _state.pop(Kind.REFERENCE);
            }

            fieldActor.kind().writeErasedValue(reference.asObject(), fieldActor.offset(), value);
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
            _state.enter(staticMethodActor, currentBytePosition());
            jumpTo(0);
        }

        @Override
        protected void invokeVirtualMethod(MethodActor method) {
            final VirtualMethodActor declaredMethod = (VirtualMethodActor) method;
            MakeHolderInitialized.makeHolderInitialized(declaredMethod);
            final Object receiver = _state.peek(Kind.REFERENCE, declaredMethod.numberOfParameterSlots() - 1).asObject();
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
            _state.enter(virtualMethodActor, currentBytePosition());
            jumpTo(0);
        }

        @Override
        protected void invokeInterfaceMethod(MethodActor method) {
            final InterfaceMethodActor declaredMethod = (InterfaceMethodActor) method;
            MakeHolderInitialized.makeHolderInitialized(declaredMethod);
            final Object receiver = _state.peek(Kind.REFERENCE, declaredMethod.descriptor().computeNumberOfSlots()).asObject();
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
            _state.enter(virtualMethodActor, currentBytePosition());
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
            _state.enter(virtualMethodActor, currentBytePosition());
            jumpTo(0);
        }

        @Override
        protected void tableSwitch(int defaultOffset, int lowMatch, int highMatch, int[] caseOffsets) {
            final int index = _state.pop(Kind.INT).asInt();
            if (index < lowMatch || index > highMatch) {
                jumpTo(currentOpcodePosition() + defaultOffset);
            } else {
                jumpTo(currentOpcodePosition() + caseOffsets[index - lowMatch]);
            }
        }

        @Override
        protected void lookupSwitch(int defaultOffset, int[] switchCases, int[] switchOffsets) {
            final int index = _state.pop(Kind.INT).asInt();
            for (int i = 0; i < switchCases.length; i++) {
                if (switchCases[i] == index) {
                    jumpTo(currentOpcodePosition() + switchOffsets[i]);
                    return;
                }
            }
            jumpTo(currentOpcodePosition() + defaultOffset);
        }

        @Override
        protected void throwException() {
            final Value exception = _state.pop(Kind.REFERENCE);
            final Throwable exceptionObject = exception.asObject() == null ? new NullPointerException() : (Throwable) exception.asObject();
            catchException(exceptionObject);
        }

        private void catchException(final Throwable exceptionObject) throws ProgramError {
            _state.last().empty();
            _state.push(ReferenceValue.from(exceptionObject));

            while (_state.hasFrames()) {

                // Try to catch exception using the current method's exception handler table.
                for (ExceptionHandlerEntry handler : _state.last().method().codeAttribute().exceptionHandlerTable()) {
                    if (currentOpcodePosition() >= handler.startPosition() && currentOpcodePosition() < handler.endPosition()) {
                        /*
                        final int index = handler.catchTypeIndex();
                        final ClassConstant classConstant = resolveClass(index);
                        final ClassActor classActor = classConstant.resolve(constantPool(), index);
                        boolean caught = Snippet.InstanceOf.instanceOf(classActor, exceptionObject);
                        */

                        jumpTo(handler.handlerPosition());
                        return;
                    }
                }

                _state.leaveWithoutReturn();
                if (_state.hasFrames()) {
                    jumpTo(_state.position());
                }
            }

            throw ProgramError.unexpected(exceptionObject);
        }

        @Override
        protected void methodReturn(Kind kind) {
            _state.leave();
            jumpTo(_state.position());
        }

        @Override
        protected void jump(int offset) {
            jumpTo(currentOpcodePosition() + offset);
        }

        @Override
        protected void increment(int slot, int addend) {
            final Value value = _state.getSlot(slot);
            if (value instanceof IntValue) {
                _state.store(slot, IntValue.from(value.asInt() + addend));
            } else if (value instanceof LongValue) {
                _state.store(slot, LongValue.from(value.asLong() + addend));
            } else {
                assert false;
            }
        }

        @Override
        protected void acmpBranch(BranchCondition condition, int offset) {
            final ReferenceValue b = (ReferenceValue) _state.pop(Kind.REFERENCE);
            final ReferenceValue a = (ReferenceValue) _state.pop(Kind.REFERENCE);
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodePosition() + offset);
            }
        }

        @Override
        protected void icmpBranch(BranchCondition condition, int offset) {
            final Value b = _state.pop(Kind.INT);
            final Value a = _state.pop(Kind.INT);
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodePosition() + offset);
            }
        }

        @Override
        protected void nullBranch(BranchCondition condition, int offset) {
            final ReferenceValue a = (ReferenceValue) _state.pop(Kind.REFERENCE);
            final ReferenceValue b = ReferenceValue.NULL;
            if (condition.evaluate(a, b)) {
                jumpTo(currentOpcodePosition() + offset);
            }
        }

        @Override
        protected void branch(BranchCondition condition, int offset) {
            final Value a = _state.pop(Kind.INT);
            if (condition.evaluate(a, IntValue.ZERO)) {
                jumpTo(currentOpcodePosition() + offset);
            }
        }

        @Override
        protected void instanceOf(ClassConstant classConstant, int index) {
            final Value reference = _state.pop(Kind.REFERENCE);
            final ClassActor classActor = classConstant.resolve(constantPool(), index);
            final boolean result = Snippet.InstanceOf.instanceOf(classActor, reference.asObject());
            _state.push(toStackValue(BooleanValue.from(result)));
        }

        @Override
        protected void checkCast(ClassConstant classConstant, int index) {
            final Value reference = _state.peek(Kind.REFERENCE, 0);
            final ClassActor classActor = classConstant.resolve(constantPool(), index);
            Snippet.CheckCast.checkCast(classActor, reference.asObject());
            // TODO: Deal with exceptions here.
        }

        @Override
        protected void enterMonitor() {
            final Value reference = _state.pop(Kind.REFERENCE);
            Monitor.enter(reference.asObject());
        }

        @Override
        protected void exitMonitor() {
            final Value reference = _state.pop(Kind.REFERENCE);
            Monitor.exit(reference.asObject());
        }

        @Override
        protected void execute(Bytecode bytecode) {
            _state.execute(bytecode);
        }

        @Override
        protected void opcodeDecoded() {
            AsynchronousProfiler.event(CounterMetric.INTERPRETED_BYTECODES);
            if (printState.getValue()) {
                _state.println();
                Console.println(Color.LIGHTMAGENTA, "opcode: method: " + _state.last().method() + " pc: " + currentOpcodePosition() + ", op: " + currentOpcode());
            }
        }

        @Override
        protected ConstantPool constantPool() {
            return _state.last().method().compilee().codeAttribute().constantPool();
        }

        protected ClassActor classActor() {
            return _state.last().method().compilee().holder();
        }
    }
}
