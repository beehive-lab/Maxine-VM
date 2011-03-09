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
package com.sun.max.vm.cps.b.c;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.JniOp.*;
import static com.sun.cri.bytecode.Bytecodes.UnsignedComparisons.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.Stoppable.Static.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.DividedByAddress;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.DividedByInt;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.GreaterEqual;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.GreaterThan;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.LessEqual;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.LessThan;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.RemainderByAddress;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.RemainderByInt;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapInt;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapReference;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapWord;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetByte;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetChar;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetDouble;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetFloat;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetInt;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetLong;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetReference;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetShort;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetWord;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadByte;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadByteAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadChar;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadCharAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadDouble;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadDoubleAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadFloat;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadFloatAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadInt;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadLong;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadLongAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadReference;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadShort;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadShortAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadWord;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetByte;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetDouble;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetFloat;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetInt;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetLong;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetReference;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetShort;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetWord;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteByte;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteByteAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteDouble;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteDoubleAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteFloat;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteFloatAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteInt;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteLong;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteLongAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteReference;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteShort;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteShortAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteWord;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.AboveEqual;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.AboveThan;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.IncrementIntegerRegister;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BarMemory;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BelowEqual;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BelowThan;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.CompareInts;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.CompareWords;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.DoubleToLong;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.FloatToInt;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.FlushRegisterWindows;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.GetIntegerRegister;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.IntToFloat;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.LeastSignificantBit;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.LongToDouble;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.MostSignificantBit;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.Pause;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.SetIntegerRegister;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.ReadHub;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.operator.JavaOperator.JavaBuiltinOperator;
import com.sun.max.vm.cps.cir.operator.Throw;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.Role;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A bytecode visitor that generates a HCIR tree for one BirBlock
 * in the context of a given BirToCirTranslation.
 *
 * A HCIR tree is a CIR tree with some restriction on what can appear as the {@linkplain CirCall#procedure() procedure}
 * of a @{linkplain CirCall call}.  HCIR allows only the following types of operators:
 *   1. {@link JavaBuiltin}
 *   2. {@link CirClosure}
 *   3. {@link CirBlock}
 *   4. {@link CirVariable}
 *   5. {@link CirSwitch}
 *   6. {@link JavaOperator}
 *
 * Basically, only the operators and constructs that can be found in JVM bytecode can appear in
 * HCIR.  Other builtins, snippets, or other CIR values are not allowed to appear here but are
 * introduced in the lowering pass. (see {@link HCirToLCirTranslation})
 *
 * The rationale for this transformation is that there are a certain class of optimizations
 * that can be applied more easily at the JVM operators level and would become harder or more
 * cumbersome if the JVM bytecode is translated to a sequence of one or more snippets.  Examples
 * of these optimizations include devirtualization (which is easier to recognize if expressed
 * as an {@link InvokeVirtual} call instead of a sequence of {@link ReadHub}, {@link
 * ResolutionSnippet}, {@link ReadInt}, and so on.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Yi Guo (HCIR)
 * @author Aziz Ghuloum (HCIR)
 */
public final class BytecodeTranslation extends BytecodeVisitor {

    protected final BlockState blockState;
    protected final JavaFrame frame;
    protected final JavaStack stack;
    protected final BirToCirMethodTranslation methodTranslation;
    protected final ConstantPool constantPool;
    protected CirCall currentCall;

    private boolean isUnsafe;

    public BytecodeTranslation(BlockState blockState, BirToCirMethodTranslation methodTranslation) {
        this.blockState = blockState;
        this.frame = blockState.frame();
        this.stack = blockState.stack();
        this.methodTranslation = methodTranslation;
        this.constantPool = methodTranslation.classMethodActor().codeAttribute().cp;
        final CirCall body = blockState.cirBlock().closure().body();
        this.currentCall = body;
        isUnsafe = methodTranslation.classMethodActor().isUnsafe();
    }

    public String classMethodName() {
        return methodTranslation.classMethodActor().name.toString();
    }

    private CirContinuation makeExceptionContinuation(BlockState dispatcherState) {
        final CirCall call = methodTranslation.newCirCall(dispatcherState.cirBlock());
        final CirVariable throwable = methodTranslation.stackVariableFactory().makeVariable(Kind.REFERENCE, 0);
        final CirContinuation continuation = new CirContinuation(throwable);
        continuation.setBody(call);
        return continuation;
    }

    protected CirValue getCurrentExceptionContinuation() {
        final BlockState dispatcherState = methodTranslation.getExceptionDispatcherState(currentOpcodeBCI());
        if (dispatcherState == null) {
            return methodTranslation.variableFactory().exceptionContinuationParameter();
        }
        if (dispatcherState.frame() == null) {
            dispatcherState.setFrame(frame.copy());
        }
        return makeExceptionContinuation(dispatcherState);
    }

    private CirContinuation getBlockContinuation(int address) {
        final CirCall call = methodTranslation.newCirCall(methodTranslation.getBlockStateAt(address).cirBlock());
        final CirContinuation continuation = new CirContinuation();
        continuation.setBody(call);
        return continuation;
    }

    private CirContinuation getBranchContinuation(int offset) {
        return getBlockContinuation(currentOpcodeBCI() + offset);
    }

    private CirContinuation getAdjacentContinuation() {
        return getBlockContinuation(currentBCI());
    }

    public void terminateBlock() {
        if (currentCall.procedure() != null) {
            // A control flow byte code (e.g. xreturn, athrow, if...)
            // has occurred at the end of this basic block
            assert currentCall.hasArguments();
            return;
        }
        // No control flow byte code has occurred,
        // so fall through to the adjacent basic block:
        currentCall.setProcedure(getAdjacentContinuation());
        currentCall.setArguments(CirCall.NO_ARGUMENTS);
    }

    private void assign(CirVariable variable, CirValue value) {
        assert isUnsafe || variable.kind().stackKind == value.kind().stackKind : incompatibleTypesErrorMessage(variable.kind().stackKind, value.kind().stackKind);
        final CirClosure closure = new CirClosure();
        currentCall.setProcedure(closure);
        currentCall.setArguments(value);

        currentCall = new CirCall();
        closure.setBody(currentCall);
        closure.setParameters(variable);
    }

    protected void push(Kind kind, CirValue argument) {
        assert isUnsafe || argument.kind() == kind : incompatibleTypesErrorMessage(argument.kind(), kind);
        final CirVariable stackVariable = stack.push(kind);
        assign(stackVariable, argument);
    }

    protected <Value_Type extends Value<Value_Type>> void push(Kind<Value_Type> kind, Value<Value_Type> value) {
        push(kind, new CirConstant(value));
    }

    protected void push(Value value) {
        push(value.kind(), new CirConstant(value));
    }

    protected void push(CirVariable variable) {
        push(variable.kind(), variable);
    }

    protected CirVariable pop(Kind kind) {
        final CirVariable stackVariable = stack.pop();
        assert isUnsafe || stackVariable.kind() == kind.stackKind : incompatibleTypesErrorMessage(stackVariable.kind(), kind.stackKind);
        return stackVariable;
    }

    protected CirVariable popReferenceOrWord() {
        final CirVariable stackVariable = stack.pop();
        assert isUnsafe || stackVariable.kind().isReference || stackVariable.kind().isWord : expectedReferenceOrWordErrorMessage(stackVariable.kind());
        return stackVariable;
    }

    protected CirVariable popTemporary() {
        final CirVariable stackVariable = stack.pop();
        final CirVariable temporaryVariable = methodTranslation.variableFactory().createTemporary(stackVariable.kind());
        assign(temporaryVariable, stackVariable);
        return temporaryVariable;
    }

    protected CirVariable getTop(Kind kind) {
        final CirVariable stackVariable = stack.getTop();
        assert isUnsafe || stackVariable.kind() == kind.stackKind : incompatibleTypesErrorMessage(stackVariable.kind(), kind.stackKind);
        return stackVariable;
    }

    protected CirVariable getReferenceOrWordTop() {
        final CirVariable stackVariable = stack.getTop();
        assert isUnsafe || stackVariable.kind().isReference || stackVariable.kind().isWord : expectedReferenceOrWordErrorMessage(stackVariable.kind());
        return stackVariable;
    }

    private void localLoad(Kind kind, int index) {
        final CirVariable localVariable = frame.makeVariable(kind, index);
        push(kind, localVariable);
    }

    private void localLoadReferenceOrWord(int index) {
        final CirVariable localVariable = frame.getVariable(index);
        assert isUnsafe || localVariable.kind().isReference || localVariable.kind().isWord;
        push(localVariable.kind(), localVariable);
    }

    private void localStore(Kind kind, int index) {
        final CirVariable stackVariable = pop(kind);
        final CirVariable localVariable = frame.makeVariable(kind, index);
        assign(localVariable, stackVariable);
    }

    private void localStoreReferenceOrWord(int index) {
        final CirVariable stackVariable = popReferenceOrWord();
        final CirVariable localVariable = frame.makeVariable(stackVariable.kind(), index);
        assign(localVariable, stackVariable);
    }

    private void createJavaFrameDescriptor() {
        currentCall.setJavaFrameDescriptor(new CirJavaFrameDescriptor(methodTranslation.classMethodActor().compilee(), currentOpcodeBCI(), frame.makeDescriptor(), stack.makeDescriptor()));
    }

    protected void call(CirRoutine cirRoutine, CirValue[] regularArguments, CirValue normalContinuation) {
        if (Stoppable.Static.canStop(cirRoutine)) {
            createJavaFrameDescriptor();
        }
        final CirValue exceptionContinuation = Stoppable.Static.canStopWithException(cirRoutine) ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        CirValue[] arguments = Utils.concat(regularArguments, normalContinuation, exceptionContinuation);
        currentCall.setArguments(arguments);
        currentCall.setProcedure((CirProcedure) cirRoutine);
        if (normalContinuation != CirValue.UNDEFINED) {
            currentCall = new CirCall();
            final CirContinuation cc = (CirContinuation) normalContinuation;
            cc.setBody(currentCall);
        } else {
            assert cirRoutine instanceof Throw;
        }
    }

    protected void callAndPush(CirRoutine cirRoutine, CirValue... regularArguments) {
        final Kind resultKind = cirRoutine.resultKind();
        if (resultKind == Kind.VOID) {
            final CirContinuation continuation = new CirContinuation();
            call(cirRoutine, regularArguments, continuation);
        } else {
            final CirVariable result = methodTranslation.variableFactory().createTemporary(resultKind);
            final CirContinuation continuation = new CirContinuation(result);
            call(cirRoutine, regularArguments, continuation);
            push(result);
        }
    }

    protected void callAndPush(Snippet snippet, CirValue... regularArguments) {
        callAndPush(CirSnippet.get(snippet), regularArguments);
    }

    protected void callAndPush(JavaBuiltin builtin, CirValue... regularArguments) {
        callAndPush(CirBuiltin.get(builtin), regularArguments);
    }

    protected void stackCall(CirRoutine cirRoutine) {
        final Kind[] parameterKinds = cirRoutine.parameterKinds();
        final int numberOfArguments = parameterKinds.length;
        final CirValue[] arguments = CirCall.newArguments(numberOfArguments);
        for (int i = numberOfArguments - 1; i >= 0; i--) {
            arguments[i] = pop(parameterKinds[i]);
        }
        callAndPush(cirRoutine, arguments);
    }

    protected void stackCall(Builtin builtin) {
        stackCall(new JavaBuiltinOperator(builtin));
    }

    protected boolean isEndOfBlock() {
        return currentBCI() == methodTranslation.getBlockStateAt(currentOpcodeBCI()).birBlock().bytecodeBlock().end + 1;
    }

    private void conditionalBranch(CirValue value1, CirSwitch cirSwitch, CirValue value2, int offset) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirValue success = getBranchContinuation(offset);
        final CirValue failure = getAdjacentContinuation();
        assert failure != null : "expected fall through basic block";
        currentCall.setArguments(value1, value2, success, failure);
        currentCall.setProcedure(cirSwitch);
    }

    private <Value_Type extends Value<Value_Type>> void conditionalBranch(CirSwitch cirSwitch, Value_Type value2, int offset) {
        final CirVariable value1 = pop(value2.kind());
        conditionalBranch(value1, cirSwitch, new CirConstant(value2), offset);
    }

    private void zeroBranch(CirSwitch cirSwitch, int offset) {
        conditionalBranch(cirSwitch, IntValue.ZERO, offset);
    }

    private void nullBranch(CirSwitch cirSwitch, int offset) {
        conditionalBranch(cirSwitch, ReferenceValue.NULL, offset);
    }

    private void compareBranch(Kind kind, CirSwitch cirSwitch, int offset) {
        final CirVariable value2 = pop(kind);
        final CirVariable value1 = pop(kind);
        conditionalBranch(value1, cirSwitch, value2, offset);
    }

    private void intBranch(CirSwitch cirSwitch, int offset) {
        compareBranch(Kind.INT, cirSwitch, offset);
    }

    private void referenceOrWordBranch(CirSwitch cirSwitch, int offset) {
        final CirVariable value2 = popReferenceOrWord();
        final CirVariable value1 = popReferenceOrWord();
        assert isUnsafe || value1.kind() == value2.kind() : incompatibleTypesErrorMessage(value1.kind(), value2.kind());
        conditionalBranch(value1, cirSwitch, value2, offset);
    }

    private void terminate(Kind kind) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        currentCall.setProcedure(methodTranslation.variableFactory().normalContinuationParameter());
        final CirVariable result = pop(kind);
        currentCall.setArguments(result);
    }

    protected void completeInvocation(CirValue method, Kind returnKind, CirValue[] arguments) {
        createJavaFrameDescriptor();

        CirContinuation continuation;
        if (returnKind == Kind.VOID) {
            continuation = new CirContinuation();
        } else {
            final CirVariable result = stack.push(returnKind);
            continuation = new CirContinuation(result);
        }
        arguments[arguments.length - 2] = continuation;
        arguments[arguments.length - 1] = getCurrentExceptionContinuation();

        currentCall.setArguments(arguments);
        currentCall.setProcedure(method);
        currentCall = new CirCall();
        continuation.setBody(currentCall);
    }

    protected boolean areArgumentsMatchingSignatureDescriptor(CirValue[] arguments, SignatureDescriptor signatureDescriptor, TypeDescriptor receiverDescriptor) {
        final int numberOfParameters = signatureDescriptor.numberOfParameters();
        final int nonContinuationArgumentsLength = arguments.length - 2;
        int argumentIndex = nonContinuationArgumentsLength - numberOfParameters;
        for (int parameterIndex = 0; parameterIndex < numberOfParameters; parameterIndex++) {
            final Kind argumentKind = arguments[argumentIndex].kind();
            final Kind parameterKind = signatureDescriptor.parameterDescriptorAt(parameterIndex).toKind().stackKind;
            assertArgumentMatchesParameter(argumentIndex, argumentKind, parameterKind, signatureDescriptor.parameterDescriptorAt(parameterIndex));
            argumentIndex++;
        }
        if (receiverDescriptor != null) {
            if (methodTranslation.classMethodActor().holder().kind.isWord) {
                // Don't check code in the Word subclasses as it contains casts between WORD and REFERENCE types that only executes in prototype mode.
            } else {
                // Must be a non-static invocation - check the receiver
                assert nonContinuationArgumentsLength == numberOfParameters + 1;
                assertArgumentMatchesParameter(0, arguments[0].kind(), receiverDescriptor.toKind(), receiverDescriptor);
            }
        }
        return true;
    }

    protected boolean areArgumentsMatchingSignatureDescriptor(CirValue[] arguments, SignatureDescriptor signatureDescriptor) {
        return areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, null);
    }

    private void assertArgumentMatchesParameter(int argumentIndex, final Kind argumentKind, final Kind parameterKind, TypeDescriptor parameterDescriptor) {
        if (argumentKind != parameterKind) {
            // This situation only occurs when calling a method that has one or more WORD type parameters (including the receiver)
            // and the parameter in question is of type Accessor (which is implemented by both WORD and REFERENCE types).
            assert argumentKind.isWord;
            assert parameterDescriptor.equals(JavaTypeDescriptor.ACCESSOR) :
                "argument " + argumentIndex + " can only be applied to a WORD parameter type or parameter of type " + Accessor.class.getName() + "(in " + methodTranslation.birMethod() + ")";
            if (argumentKind != Kind.WORD) {
                assert parameterKind.isWord :  "argument " + argumentIndex + " cannot be of kind " + argumentKind + " when parameter is not of kind " + Kind.WORD;
            }
        }
    }

    private String expectedReferenceOrWordErrorMessage(Kind kind) {
        return "Expected REFERENCE or WORD type, got " + kind;
    }

    private static String incompatibleTypesErrorMessage(Kind kind1, Kind kind2) {
        return "Incompatible types: " + kind1 + " and " + kind2;
    }

    protected String expectedEndOfBlockErrorMessage() {
        return "Expected to be at the end of a basic block";
    }

    private String expectedCategory1ErrorMessage() {
        return "Expected category1 type on stack";
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected void nop() {
    }

    @Override
    protected void aconst_null() {
        push(ReferenceValue.NULL);
    }

    @Override
    protected void iconst_m1() {
        push(IntValue.MINUS_ONE);
    }

    @Override
    protected void iconst_0() {
        push(IntValue.ZERO);
    }

    @Override
    protected void iconst_1() {
        push(IntValue.ONE);
    }

    @Override
    protected void iconst_2() {
        push(IntValue.TWO);
    }

    @Override
    protected void iconst_3() {
        push(IntValue.THREE);
    }

    @Override
    protected void iconst_4() {
        push(IntValue.FOUR);
    }

    @Override
    protected void iconst_5() {
        push(IntValue.FIVE);
    }

    @Override
    protected void lconst_0() {
        push(LongValue.ZERO);
    }

    @Override
    protected void lconst_1() {
        push(LongValue.ONE);
    }

    @Override
    protected void fconst_0() {
        push(FloatValue.ZERO);
    }

    @Override
    protected void fconst_1() {
        push(FloatValue.ONE);
    }

    @Override
    protected void fconst_2() {
        push(FloatValue.TWO);
    }

    @Override
    protected void dconst_0() {
        push(DoubleValue.ZERO);
    }

    @Override
    protected void dconst_1() {
        push(DoubleValue.ONE);
    }

    @Override
    protected void bipush(int operand) {
        push(IntValue.from(operand));
    }

    @Override
    protected void sipush(int operand) {
        push(IntValue.from(operand));
    }

    @Override
    protected void ldc(int index) {
        final Tag tag = constantPool.tagAt(index);
        switch (tag) {
            case CLASS: {
                final JavaOperator op = new Mirror(constantPool, index);
                callAndPush(op);
                break;
            }
            case INTEGER: {
                push(IntValue.from(constantPool.intAt(index)));
                break;
            }
            case FLOAT: {
                push(FloatValue.from(constantPool.floatAt(index)));
                break;
            }
            case STRING: {
                push(ReferenceValue.from(constantPool.stringAt(index)));
                break;
            }
            default: {
                throw new VerifyError("invalid index in LDC to " + tag);
            }
        }
    }

    @Override
    protected void ldc_w(int index) {
        ldc(index);
    }

    @Override
    protected void ldc2_w(int index) {
        final Tag tag = constantPool.tagAt(index);
        switch (tag) {
            case LONG: {
                push(LongValue.from(constantPool.longAt(index)));
                break;
            }
            case DOUBLE: {
                push(DoubleValue.from(constantPool.doubleAt(index)));
                break;
            }
            default: {
                throw new VerifyError("invalid index in LDC2_W to " + tag);
            }
        }
    }

    @Override
    protected void iload(int index) {
        localLoad(Kind.INT, index);
    }

    @Override
    protected void lload(int index) {
        localLoad(Kind.LONG, index);
    }

    @Override
    protected void fload(int index) {
        localLoad(Kind.FLOAT, index);
    }

    @Override
    protected void dload(int index) {
        localLoad(Kind.DOUBLE, index);
    }

    @Override
    protected void aload(int index) {
        localLoadReferenceOrWord(index);
    }

    @Override
    protected void iload_0() {
        localLoad(Kind.INT, 0);
    }

    @Override
    protected void iload_1() {
        localLoad(Kind.INT, 1);
    }

    @Override
    protected void iload_2() {
        localLoad(Kind.INT, 2);
    }

    @Override
    protected void iload_3() {
        localLoad(Kind.INT, 3);
    }

    @Override
    protected void lload_0() {
        localLoad(Kind.LONG, 0);
    }

    @Override
    protected void lload_1() {
        localLoad(Kind.LONG, 1);
    }

    @Override
    protected void lload_2() {
        localLoad(Kind.LONG, 2);
    }

    @Override
    protected void lload_3() {
        localLoad(Kind.LONG, 3);
    }

    @Override
    protected void fload_0() {
        localLoad(Kind.FLOAT, 0);
    }

    @Override
    protected void fload_1() {
        localLoad(Kind.FLOAT, 1);
    }

    @Override
    protected void fload_2() {
        localLoad(Kind.FLOAT, 2);
    }

    @Override
    protected void fload_3() {
        localLoad(Kind.FLOAT, 3);
    }

    @Override
    protected void dload_0() {
        localLoad(Kind.DOUBLE, 0);
    }

    @Override
    protected void dload_1() {
        localLoad(Kind.DOUBLE, 1);
    }

    @Override
    protected void dload_2() {
        localLoad(Kind.DOUBLE, 2);
    }

    @Override
    protected void dload_3() {
        localLoad(Kind.DOUBLE, 3);
    }

    @Override
    protected void aload_0() {
        localLoadReferenceOrWord(0);
    }

    @Override
    protected void aload_1() {
        localLoadReferenceOrWord(1);
    }

    @Override
    protected void aload_2() {
        localLoadReferenceOrWord(2);
    }

    @Override
    protected void aload_3() {
        localLoadReferenceOrWord(3);
    }

    @Override
    protected void istore(int index) {
        localStore(Kind.INT, index);
    }

    @Override
    protected void lstore(int index) {
        localStore(Kind.LONG, index);
    }

    @Override
    protected void fstore(int index) {
        localStore(Kind.FLOAT, index);
    }

    @Override
    protected void dstore(int index) {
        localStore(Kind.DOUBLE, index);
    }

    @Override
    protected void astore(int index) {
        localStoreReferenceOrWord(index);
    }

    @Override
    protected void istore_0() {
        localStore(Kind.INT, 0);
    }

    @Override
    protected void istore_1() {
        localStore(Kind.INT, 1);
    }

    @Override
    protected void istore_2() {
        localStore(Kind.INT, 2);
    }

    @Override
    protected void istore_3() {
        localStore(Kind.INT, 3);
    }

    @Override
    protected void lstore_0() {
        localStore(Kind.LONG, 0);
    }

    @Override
    protected void lstore_1() {
        localStore(Kind.LONG, 1);
    }

    @Override
    protected void lstore_2() {
        localStore(Kind.LONG, 2);
    }

    @Override
    protected void lstore_3() {
        localStore(Kind.LONG, 3);
    }

    @Override
    protected void fstore_0() {
        localStore(Kind.FLOAT, 0);
    }

    @Override
    protected void fstore_1() {
        localStore(Kind.FLOAT, 1);
    }

    @Override
    protected void fstore_2() {
        localStore(Kind.FLOAT, 2);
    }

    @Override
    protected void fstore_3() {
        localStore(Kind.FLOAT, 3);
    }

    @Override
    protected void dstore_0() {
        localStore(Kind.DOUBLE, 0);
    }

    @Override
    protected void dstore_1() {
        localStore(Kind.DOUBLE, 1);
    }

    @Override
    protected void dstore_2() {
        localStore(Kind.DOUBLE, 2);
    }

    @Override
    protected void dstore_3() {
        localStore(Kind.DOUBLE, 3);
    }

    @Override
    protected void astore_0() {
        localStoreReferenceOrWord(0);
    }

    @Override
    protected void astore_1() {
        localStoreReferenceOrWord(1);
    }

    @Override
    protected void astore_2() {
        localStoreReferenceOrWord(2);
    }

    @Override
    protected void astore_3() {
        localStoreReferenceOrWord(3);
    }

    @Override
    protected void pop() {
        final CirVariable value = stack.pop();
        assert value.isCategory1() : expectedCategory1ErrorMessage();
    }

    @Override
    protected void pop2() {
        final CirVariable value = stack.pop();
        if (value.isCategory1()) {
            pop();
        }
    }

    @Override
    protected void dup() {
        final CirVariable value = stack.getTop();
        assert value.isCategory1() : expectedCategory1ErrorMessage();
        push(value);
    }

    @Override
    protected void dup_x1() {
        final CirVariable value1 = popTemporary();
        assert value1.isCategory1() : expectedCategory1ErrorMessage();
        final CirVariable value2 = popTemporary();
        assert value2.isCategory1() : expectedCategory1ErrorMessage();
        push(value1);
        push(value2);
        push(value1);
    }

    @Override
    protected void dup_x2() {
        final CirVariable value1 = popTemporary();
        assert value1.isCategory1() : expectedCategory1ErrorMessage();
        final CirVariable value2 = popTemporary();
        if (value2.isCategory1()) {
            final CirVariable value3 = popTemporary();
            assert value3.isCategory1() : expectedCategory1ErrorMessage();
            push(value1);
            push(value3);
            push(value2);
            push(value1);
        } else {
            push(value1);
            push(value2);
            push(value1);
        }
    }

    @Override
    protected void dup2() {
        final CirVariable value1 = popTemporary();
        if (value1.isCategory1()) {
            final CirVariable value2 = popTemporary();
            assert value2.isCategory1() : expectedCategory1ErrorMessage();
            push(value2);
            push(value1);
            push(value2);
            push(value1);
        } else {
            push(value1);
            push(value1);
        }
    }

    @Override
    protected void dup2_x1() {
        final CirVariable value1 = popTemporary();
        final CirVariable value2 = popTemporary();
        assert value2.isCategory1() : expectedCategory1ErrorMessage();
        if (value1.isCategory1()) {
            final CirVariable value3 = popTemporary();
            assert value3.isCategory1() : expectedCategory1ErrorMessage();
            push(value2);
            push(value1);
            push(value3);
            push(value2);
            push(value1);
        } else {
            push(value1);
            push(value2);
            push(value1);
        }
    }

    @Override
    protected void dup2_x2() {
        final CirVariable value1 = popTemporary();
        final CirVariable value2 = popTemporary();
        if (value1.isCategory1()) {
            assert value2.isCategory1() : expectedCategory1ErrorMessage();
            final CirVariable value3 = popTemporary();
            if (value3.isCategory1()) {
                final CirVariable value4 = popTemporary();
                assert value4.isCategory1() : expectedCategory1ErrorMessage();
                push(value2);
                push(value1);
                push(value4);
                push(value3);
                push(value2);
                push(value1);
            } else {
                push(value2);
                push(value1);
                push(value3);
                push(value2);
                push(value1);
            }
        } else {
            if (value2.isCategory1()) {
                final CirVariable value3 = popTemporary();
                assert value3.isCategory1() : expectedCategory1ErrorMessage();
                push(value1);
                push(value3);
                push(value2);
                push(value1);
            } else {
                push(value1);
                push(value2);
                push(value1);
            }
        }
    }

    @Override
    protected void swap() {
        final CirVariable value1 = popTemporary();
        assert value1.isCategory1() : expectedCategory1ErrorMessage();
        final CirVariable value2 = popTemporary();
        assert value2.isCategory1() : expectedCategory1ErrorMessage();
        push(value1);
        push(value2);
    }

    @Override
    protected void ifeq(int offset) {
        zeroBranch(CirSwitch.INT_EQUAL, offset);
    }

    @Override
    protected void ifne(int offset) {
        zeroBranch(CirSwitch.INT_NOT_EQUAL, offset);
    }

    @Override
    protected void iflt(int offset) {
        zeroBranch(CirSwitch.SIGNED_INT_LESS_THAN, offset);
    }

    @Override
    protected void ifge(int offset) {
        zeroBranch(CirSwitch.SIGNED_INT_GREATER_EQUAL, offset);
    }

    @Override
    protected void ifgt(int offset) {
        zeroBranch(CirSwitch.SIGNED_INT_GREATER_THAN, offset);
    }

    @Override
    protected void ifle(int offset) {
        zeroBranch(CirSwitch.SIGNED_INT_LESS_EQUAL, offset);
    }

    @Override
    protected void if_icmpeq(int offset) {
        intBranch(CirSwitch.INT_EQUAL, offset);
    }

    @Override
    protected void if_icmpne(int offset) {
        intBranch(CirSwitch.INT_NOT_EQUAL, offset);
    }

    @Override
    protected void if_icmplt(int offset) {
        intBranch(CirSwitch.SIGNED_INT_LESS_THAN, offset);
    }

    @Override
    protected void if_icmpge(int offset) {
        intBranch(CirSwitch.SIGNED_INT_GREATER_EQUAL, offset);
    }

    @Override
    protected void if_icmpgt(int offset) {
        intBranch(CirSwitch.SIGNED_INT_GREATER_THAN, offset);
    }

    @Override
    protected void if_icmple(int offset) {
        intBranch(CirSwitch.SIGNED_INT_LESS_EQUAL, offset);
    }

    @Override
    protected void if_acmpeq(int offset) {
        referenceOrWordBranch(CirSwitch.REFERENCE_EQUAL, offset);
    }

    @Override
    protected void if_acmpne(int offset) {
        referenceOrWordBranch(CirSwitch.REFERENCE_NOT_EQUAL, offset);
    }

    @Override
    protected void goto_(int offset) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        currentCall.setProcedure(getBranchContinuation(offset));
        currentCall.setArguments(CirCall.NO_ARGUMENTS);
    }

    @Override
    protected void jsr(int offset) {
        ProgramError.unexpected("jsr byte code");
    }

    @Override
    protected void ret(int index) {
        ProgramError.unexpected("ret byte code");
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final int nArguments = (numberOfCases * 2) + 2;
        final CirValue[] arguments = CirCall.newArguments(nArguments);
        arguments[0] = pop(Kind.INT);
        final BytecodeScanner scanner = bytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            final int match = lowMatch + i;
            arguments[1 + i] = CirConstant.fromInt(match);
            arguments[1 + numberOfCases + i] = getBranchContinuation(scanner.readSwitchOffset());
        }
        arguments[1 + (numberOfCases * 2)] = getBranchContinuation(defaultOffset);
        currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        currentCall.setProcedure(switchBuiltin);
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int nArguments = (numberOfCases * 2) + 2;
        final CirValue[] arguments = CirCall.newArguments(nArguments);
        arguments[0] = pop(Kind.INT);
        final BytecodeScanner scanner = bytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            arguments[1 + i] = CirConstant.fromInt(scanner.readSwitchCase());
            arguments[1 + numberOfCases + i] = getBranchContinuation(scanner.readSwitchOffset());
        }
        arguments[1 + (numberOfCases * 2)] = getBranchContinuation(defaultOffset);
        currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        currentCall.setProcedure(switchBuiltin);
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
    }

    @Override
    protected void ireturn() {
        terminate(Kind.INT);
    }

    @Override
    protected void lreturn() {
        terminate(Kind.LONG);
    }

    @Override
    protected void freturn() {
        terminate(Kind.FLOAT);
    }

    @Override
    protected void dreturn() {
        terminate(Kind.DOUBLE);
    }

    @Override
    protected void areturn() {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        currentCall.setProcedure(methodTranslation.variableFactory().normalContinuationParameter());
        final CirVariable result = popReferenceOrWord();
        currentCall.setArguments(result);
    }

    @Override
    protected void vreturn() {
        currentCall.setProcedure(methodTranslation.variableFactory().normalContinuationParameter());
        currentCall.setArguments(CirCall.NO_ARGUMENTS);
    }

    @Override
    protected void wide() {
        // NOTE: we do not need to emit code for WIDE because BytecodeScanner automatically widens opcodes
    }

    @Override
    protected void ifnull(int offset) {
        nullBranch(CirSwitch.REFERENCE_EQUAL, offset);
    }

    @Override
    protected void ifnonnull(int offset) {
        nullBranch(CirSwitch.REFERENCE_NOT_EQUAL, offset);
    }

    @Override
    protected void goto_w(int offset) {
        goto_(offset);
    }

    @Override
    protected void jsr_w(int offset) {
        jsr(offset);
    }

    @Override
    protected void breakpoint() {
        FatalError.unimplemented();
    }

    @Override
    protected void prologue() {
        if (blockState.birBlock().hasSafepoint()) {
            CirValue[] regularArguments = {CirConstant.fromInt(Bytecodes.SAFEPOINT)};
            call(JavaOperator.INFOPOINT_OP, regularArguments, new CirContinuation());
        }
    }

    @Override
    protected void getfield(int index) {
        final CirVariable reference = pop(Kind.REFERENCE);
        final JavaOperator getField = new GetField(constantPool, index);
        callAndPush(getField, reference);
    }

    @Override
    protected void putfield(int index) {
        final Kind kind = constantPool.fieldAt(index).type(constantPool).toKind();
        final CirVariable value = pop(kind);
        final CirVariable reference = pop(Kind.REFERENCE);
        final JavaOperator putfield = new PutField(constantPool, index);
        callAndPush(putfield, reference, value);
    }

    @Override
    protected void putstatic(int index) {
        final Kind kind = constantPool.fieldAt(index).type(constantPool).toKind();
        final CirVariable value = pop(kind);
        final JavaOperator putstatic = new PutStatic(constantPool, index);
        callAndPush(putstatic, value);
    }

    @Override
    protected void getstatic(int index) {
        final FieldRefConstant fieldRef = constantPool.fieldAt(index);
        if (fieldRef.isResolvableWithoutClassLoading(constantPool)) {
            try {
                final FieldActor fieldActor = fieldRef.resolve(constantPool, index);
                if (fieldActor.isFinal() && JavaTypeDescriptor.isPrimitive(fieldActor.descriptor()) && fieldActor.holder().isInitialized()) {
                    // This can be transformed directly into a constant value if the field holder has been initialized
                    final Value fieldValue = fieldActor.getValue(null);
                    push(fieldValue);
                    return;
                }
                // all other cases, fall off to general getstatic, including when failing reflective access.
            } catch (LinkageError e) {
                // do nothing.
            }
        }
        final JavaOperator getstatic = new GetStatic(constantPool, index);
        callAndPush(getstatic);
    }

    static class Invocation {
        final CirValue[] args;
        final SignatureDescriptor sig;
        public Invocation(CirValue[] args, SignatureDescriptor sig) {
            this.args = args;
            this.sig = sig;
        }
    }

    private Invocation invoke(int index, int receiverCount) {
        final MethodRefConstant methodRef = constantPool.methodAt(index);
        final SignatureDescriptor sig = methodRef.signature(constantPool);
        final int numberOfParameters = sig.numberOfParameters();
        final CirValue[] args = CirCall.newArguments(receiverCount + numberOfParameters + 2);
        for (int i = numberOfParameters - 1; i >= 0; i--) {
            args[i + receiverCount] = stack.pop();
        }
        TypeDescriptor receiverDescriptor;
        if (receiverCount == 1) {
            final CirVariable receiver = popReferenceOrWord();
            args[0] = receiver;
            receiverDescriptor = methodRef.holder(constantPool);
        } else {
            assert receiverCount == 0;
            receiverDescriptor = null;
        }
        assert isUnsafe || areArgumentsMatchingSignatureDescriptor(args, sig, receiverDescriptor);
        return new Invocation(args, sig);

    }

    @Override
    protected void invokeinterface(int index, int countUnused) {
        Invocation invocation = invoke(index, 1);
        completeInvocation(new InvokeInterface(constantPool, index), invocation.sig.resultKind(), invocation.args);
    }

    @Override
    protected void invokevirtual(int index) {
        Invocation invocation = invoke(index, 1);
        completeInvocation(new InvokeVirtual(constantPool, index), invocation.sig.resultKind(), invocation.args);
    }

    @Override
    protected void invokespecial(int index) {
        Invocation invocation = invoke(index, 1);
        completeInvocation(new InvokeSpecial(constantPool, index), invocation.sig.resultKind(), invocation.args);
    }

    @Override
    protected void invokestatic(int index) {
        Invocation invocation = invoke(index, 0);
        completeInvocation(new InvokeStatic(constantPool, index), invocation.sig.resultKind(), invocation.args);
    }

    @Override
    protected void checkcast(int index) {
        final CirVariable object = getReferenceOrWordTop();
        final CheckCast checkcast = new CheckCast(constantPool, index);
        if (object.kind().isWord) {
            if (checkcast.actor() == null || !checkcast.actor().kind.isWord) {
                raiseException(CirConstant.fromObject(new ClassCastException("Cannot cast word type to non-word type")));
            } else {
                // A cast from one word type to another word type is a nop.
            }
        } else {
            callAndPush(checkcast, object);
        }
    }

    @Override
    protected void instanceof_(int index) {
        final CirVariable object = popReferenceOrWord();
        final InstanceOf instanceofOp = new InstanceOf(constantPool, index);
        if (object.kind().isWord) {
            push(BooleanValue.from(instanceofOp.actor() != null && instanceofOp.actor().kind.isWord));
        } else {
            callAndPush(instanceofOp, object);
        }
    }

    protected void arrayLoad(Kind kind) {
        final CirVariable index = pop(Kind.INT);
        final CirVariable array = pop(Kind.REFERENCE);
        final JavaOperator op = new ArrayLoad(kind);
        callAndPush(op, array, index);
    }

    @Override
    protected void iaload() {
        arrayLoad(Kind.INT);
    }

    @Override
    protected void laload() {
        arrayLoad(Kind.LONG);
    }

    @Override
    protected void faload() {
        arrayLoad(Kind.FLOAT);
    }

    @Override
    protected void daload() {
        arrayLoad(Kind.DOUBLE);
    }

    @Override
    protected void aaload() {
        arrayLoad(Kind.REFERENCE);
    }

    @Override
    protected void baload() {
        arrayLoad(Kind.BYTE);
    }

    @Override
    protected void caload() {
        arrayLoad(Kind.CHAR);
    }

    @Override
    protected void saload() {
        arrayLoad(Kind.SHORT);
    }

    @Override
    protected void new_(int index) {
        final JavaOperator newOp = new New(constantPool, index);
        callAndPush(newOp);
    }

    private void arrayStore(Kind kind) {
        final JavaOperator arraystore = new ArrayStore(kind);
        final CirVariable value = pop(kind);
        final CirVariable index = pop(Kind.INT);
        final CirVariable array = pop(Kind.REFERENCE);
        callAndPush(arraystore, array, index, value);
    }

    @Override
    protected void aastore() {
        arrayStore(Kind.REFERENCE);
    }

    @Override
    protected void bastore() {
        arrayStore(Kind.BYTE);
    }

    @Override
    protected void castore() {
        arrayStore(Kind.CHAR);
    }

    @Override
    protected void dastore() {
        arrayStore(Kind.DOUBLE);
    }

    @Override
    protected void fastore() {
        arrayStore(Kind.FLOAT);
    }

    @Override
    protected void lastore() {
        arrayStore(Kind.LONG);
    }

    @Override
    protected void iastore() {
        arrayStore(Kind.INT);
    }

    @Override
    protected void sastore() {
        arrayStore(Kind.SHORT);
    }

    @Override
    protected void newarray(int atag) {
        final CirVariable count = pop(Kind.INT);
        final JavaOperator newarray = new NewArray(atag);
        callAndPush(newarray, count);
    }

    @Override
    protected void anewarray(int index) {
        final CirVariable count = pop(Kind.INT);
        final JavaOperator newarray = new NewArray(constantPool, index);
        callAndPush(newarray, count);
    }

    @Override
    protected void multianewarray(int index, int nDimensions) {
        final JavaOperator op = new MultiANewArray(constantPool, index, nDimensions);
        final CirValue[] dimensions = CirCall.newArguments(nDimensions);
        for (int i = 1; i <= nDimensions; i++) {
            dimensions[nDimensions - i] = pop(Kind.INT);
        }
        callAndPush(op, dimensions);
    }

    @Override
    protected void arraylength() {
        final JavaOperator op = new ArrayLength();
        final CirVariable arrayref = pop(Kind.REFERENCE);
        callAndPush(op, arrayref);
    }

    @Override
    protected void monitorenter() {
        final JavaOperator op = new MonitorEnter();
        final CirVariable objectref = pop(Kind.REFERENCE);
        callAndPush(op, objectref);
    }

    @Override
    protected void monitorexit() {
        final JavaOperator op = new MonitorExit();
        final CirVariable objectref = pop(Kind.REFERENCE);
        callAndPush(op, objectref);
    }

    /**
     * @see Bytecodes#JNICALL
     */
    @Override
    protected void jnicall(int nativeFunctionDescriptorIndex) {
        final CallNative op = new CallNative(constantPool, nativeFunctionDescriptorIndex, methodTranslation.classMethodActor());
        final SignatureDescriptor signatureDescriptor = op.signatureDescriptor();
        final int numberOfParameters = signatureDescriptor.numberOfParameters();
        final CirValue[] arguments = CirCall.newArguments(numberOfParameters + 3);
        CirVariable callEntryPoint = pop(Kind.WORD);
        arguments[numberOfParameters] = callEntryPoint;
        for (int i = numberOfParameters - 1; i >= 0; i--) {
            arguments[i] = stack.pop();
        }

        assert isUnsafe || areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);

        currentCall.setIsNative();
        completeInvocation(op, signatureDescriptor.resultKind(), arguments);
    }

    @Override
    protected void f2i() {
        stackCall(JavaOperator.FLOAT_TO_INT);
    }

    @Override
    protected void f2l() {
        stackCall(JavaOperator.FLOAT_TO_LONG);
    }

    @Override
    protected void d2i() {
        stackCall(JavaOperator.DOUBLE_TO_INT);
    }

    @Override
    protected void d2l() {
        stackCall(JavaOperator.DOUBLE_TO_LONG);
    }

    @Override
    protected void fcmpl() {
        stackCall(JavaOperator.FLOAT_COMPARE_L);
    }

    @Override
    protected void fcmpg() {
        stackCall(JavaOperator.FLOAT_COMPARE_G);

    }

    @Override
    protected void dcmpl() {
        stackCall(JavaOperator.DOUBLE_COMPARE_L);
    }

    @Override
    protected void dcmpg() {
        stackCall(JavaOperator.DOUBLE_COMPARE_G);
    }

    @Override
    protected void i2b() {
        stackCall(JavaOperator.INT_TO_BYTE);
    }

    @Override
    protected void i2c() {
        stackCall(JavaOperator.INT_TO_CHAR);
    }

    @Override
    protected void i2s() {
        stackCall(JavaOperator.INT_TO_SHORT);
    }

    @Override
    protected void irem() {
        stackCall(JavaOperator.INT_REMAINDER);
    }

    @Override
    protected void lrem() {
        stackCall(JavaOperator.LONG_REMAINDER);
    }

    @Override
    protected void frem() {
        stackCall(JavaOperator.FLOAT_REMAINDER);
    }

    @Override
    protected void drem() {
        stackCall(JavaOperator.DOUBLE_REMAINDER);
    }

    @Override
    protected void iadd() {
        stackCall(JavaOperator.INT_PLUS);
    }

    @Override
    protected void ladd() {
        stackCall(JavaOperator.LONG_PLUS);
    }

    @Override
    protected void fadd() {
        stackCall(JavaOperator.FLOAT_PLUS);
    }

    @Override
    protected void dadd() {
        stackCall(JavaOperator.DOUBLE_PLUS);
    }

    @Override
    protected void isub() {
        stackCall(JavaOperator.INT_MINUS);
    }

    @Override
    protected void lsub() {
        stackCall(JavaOperator.LONG_MINUS);
    }

    @Override
    protected void fsub() {
        stackCall(JavaOperator.FLOAT_MINUS);
    }

    @Override
    protected void dsub() {
        stackCall(JavaOperator.DOUBLE_MINUS);
    }

    @Override
    protected void imul() {
        stackCall(JavaOperator.INT_TIMES);
    }

    @Override
    protected void lmul() {
        stackCall(JavaOperator.LONG_TIMES);
    }

    @Override
    protected void fmul() {
        stackCall(JavaOperator.FLOAT_TIMES);
    }

    @Override
    protected void dmul() {
        stackCall(JavaOperator.DOUBLE_TIMES);
    }

    @Override
    protected void idiv() {
        stackCall(JavaOperator.INT_DIVIDE);
    }

    @Override
    protected void ldiv() {
        stackCall(JavaOperator.LONG_DIVIDE);
    }

    @Override
    protected void fdiv() {
        stackCall(JavaOperator.FLOAT_DIVIDE);
    }

    @Override
    protected void ddiv() {
        stackCall(JavaOperator.DOUBLE_DIVIDE);
    }

    @Override
    protected void i2l() {
        stackCall(JavaOperator.INT_TO_LONG);
    }

    @Override
    protected void i2f() {
        stackCall(JavaOperator.INT_TO_FLOAT);
    }

    @Override
    protected void i2d() {
        stackCall(JavaOperator.INT_TO_DOUBLE);
    }

    @Override
    protected void l2i() {
        stackCall(JavaOperator.LONG_TO_INT);
    }

    @Override
    protected void l2f() {
        stackCall(JavaOperator.LONG_TO_FLOAT);
    }

    @Override
    protected void l2d() {
        stackCall(JavaOperator.LONG_TO_DOUBLE);
    }

    @Override
    protected void f2d() {
        stackCall(JavaOperator.FLOAT_TO_DOUBLE);
    }

    @Override
    protected void d2f() {
        stackCall(JavaOperator.DOUBLE_TO_FLOAT);
    }

    @Override
    protected void lcmp() {
        stackCall(JavaOperator.LONG_COMPARE);
    }

    @Override
    protected void ishl() {
        stackCall(JavaOperator.INT_SHIFT_LEFT);
    }

    @Override
    protected void lshl() {
        stackCall(JavaOperator.LONG_SHIFT_LEFT);
    }

    @Override
    protected void ishr() {
        stackCall(JavaOperator.INT_SIGNED_SHIFT_RIGHT);
    }

    @Override
    protected void lshr() {
        stackCall(JavaOperator.LONG_SIGNED_SHIFT_RIGHT);
    }

    @Override
    protected void iushr() {
        stackCall(JavaOperator.INT_UNSIGNED_SHIFT_RIGHT);
    }

    @Override
    protected void lushr() {
        stackCall(JavaOperator.LONG_UNSIGNED_SHIFT_RIGHT);
    }

    @Override
    protected void iand() {
        stackCall(JavaOperator.INT_AND);
    }

    @Override
    protected void land() {
        stackCall(JavaOperator.LONG_AND);
    }

    @Override
    protected void ior() {
        stackCall(JavaOperator.INT_OR);
    }

    @Override
    protected void lor() {
        stackCall(JavaOperator.LONG_OR);
    }

    @Override
    protected void ixor() {
        stackCall(JavaOperator.INT_XOR);
    }

    @Override
    protected void lxor() {
        stackCall(JavaOperator.LONG_XOR);
    }

    @Override
    protected void ineg() {
        stackCall(JavaOperator.INT_NEG);
    }

    @Override
    protected void lneg() {
        stackCall(JavaOperator.LONG_NEG);
    }

    @Override
    protected void fneg() {
        stackCall(JavaOperator.FLOAT_NEG);
    }

    @Override
    protected void dneg() {
        stackCall(JavaOperator.DOUBLE_NEG);
    }

    @Override
    protected void iinc(int index, int addend) {
        final CirVariable localVariable = frame.makeVariable(Kind.INT, index);
        final CirContinuation continuation = new CirContinuation(localVariable);
        final JavaOperator op = JavaOperator.INT_PLUS;
        final CirValue exceptionContinuation = canStop(op) ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        currentCall.setArguments(localVariable, CirConstant.fromInt(addend), continuation, exceptionContinuation);
        currentCall.setProcedure(op);
        currentCall = new CirCall();
        continuation.setBody(currentCall);
    }

    @Override
    protected void athrow() {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirVariable throwable = pop(Kind.REFERENCE);
        raiseException(throwable);
    }

    private void raiseException(CirValue throwable) {
        final Throw athrow = new Throw();
        call(athrow, new CirValue[] {throwable}, CirValue.UNDEFINED);
    }

    @Override
    protected boolean extension(int opcode, boolean isWide) {
        int length = Bytecodes.lengthOf(opcode);
        int operand = 0;
        if (length == 2) {
            operand = isWide ? bytecodeScanner().readUnsigned2() : bytecodeScanner().readUnsigned1();
        } else if (length == 3) {
            assert !isWide;
            operand = bytecodeScanner().readUnsigned2();
        } else {
            assert !isWide;
            assert length == 1;
        }
        isUnsafe = true;
        switch (opcode) {
            // Checkstyle: stop
            case UNSAFE_CAST:            break;
            case WLOAD:                  localLoadReferenceOrWord(operand); break;
            case WLOAD_0:                localLoadReferenceOrWord(0); break;
            case WLOAD_1:                localLoadReferenceOrWord(1); break;
            case WLOAD_2:                localLoadReferenceOrWord(2); break;
            case WLOAD_3:                localLoadReferenceOrWord(3); break;
            case WSTORE:                 localStoreReferenceOrWord(operand); break;
            case WSTORE_0:               localStoreReferenceOrWord(0); break;
            case WSTORE_1:               localStoreReferenceOrWord(1); break;
            case WSTORE_2:               localStoreReferenceOrWord(2); break;
            case WSTORE_3:               localStoreReferenceOrWord(3); break;
            case WCONST_0:               push(WordValue.ZERO); break;
            case WDIV:                   stackCall(DividedByAddress.BUILTIN); break;
            case WDIVI:                  stackCall(DividedByInt.BUILTIN); break;
            case WREM:                   stackCall(RemainderByAddress.BUILTIN); break;
            case WREMI:                  stackCall(RemainderByInt.BUILTIN); break;
            case ICMP:                   stackCall(CompareInts.BUILTIN); break;
            case WCMP:                   stackCall(CompareWords.BUILTIN); break;

            case Bytecodes.MEMBAR:       membar(operand); break;

            case Bytecodes.PCMPSWP:
            case Bytecodes.PGET:
            case Bytecodes.PSET:
            case Bytecodes.PREAD:
            case Bytecodes.PWRITE: {
                opcode = opcode | (operand << 8);
                switch (opcode) {
                    case PREAD_BYTE:             stackCall(ReadByte.BUILTIN); break;
                    case PREAD_CHAR:             stackCall(ReadChar.BUILTIN); break;
                    case PREAD_SHORT:            stackCall(ReadShort.BUILTIN); break;
                    case PREAD_INT:              stackCall(ReadInt.BUILTIN); break;
                    case PREAD_FLOAT:            stackCall(ReadFloat.BUILTIN); break;
                    case PREAD_LONG:             stackCall(ReadLong.BUILTIN); break;
                    case PREAD_DOUBLE:           stackCall(ReadDouble.BUILTIN); break;
                    case PREAD_WORD:             stackCall(ReadWord.BUILTIN); break;
                    case PREAD_REFERENCE:        stackCall(ReadReference.BUILTIN); break;
                    case PREAD_BYTE_I:           stackCall(ReadByteAtIntOffset.BUILTIN); break;
                    case PREAD_CHAR_I:           stackCall(ReadCharAtIntOffset.BUILTIN); break;
                    case PREAD_SHORT_I:          stackCall(ReadShortAtIntOffset.BUILTIN); break;
                    case PREAD_INT_I:            stackCall(ReadIntAtIntOffset.BUILTIN); break;
                    case PREAD_FLOAT_I:          stackCall(ReadFloatAtIntOffset.BUILTIN); break;
                    case PREAD_LONG_I:           stackCall(ReadLongAtIntOffset.BUILTIN); break;
                    case PREAD_DOUBLE_I:         stackCall(ReadDoubleAtIntOffset.BUILTIN); break;
                    case PREAD_WORD_I:           stackCall(ReadWordAtIntOffset.BUILTIN); break;
                    case PREAD_REFERENCE_I:      stackCall(ReadReferenceAtIntOffset.BUILTIN); break;
                    case PWRITE_BYTE:            stackCall(WriteByte.BUILTIN); break;
                    case PWRITE_SHORT:           stackCall(WriteShort.BUILTIN); break;
                    case PWRITE_INT:             stackCall(WriteInt.BUILTIN); break;
                    case PWRITE_FLOAT:           stackCall(WriteFloat.BUILTIN); break;
                    case PWRITE_LONG:            stackCall(WriteLong.BUILTIN); break;
                    case PWRITE_DOUBLE:          stackCall(WriteDouble.BUILTIN); break;
                    case PWRITE_WORD:            stackCall(WriteWord.BUILTIN); break;
                    case PWRITE_REFERENCE:       stackCall(WriteReference.BUILTIN); break;
                    case PWRITE_BYTE_I:          stackCall(WriteByteAtIntOffset.BUILTIN); break;
                    case PWRITE_SHORT_I:         stackCall(WriteShortAtIntOffset.BUILTIN); break;
                    case PWRITE_INT_I:           stackCall(WriteIntAtIntOffset.BUILTIN); break;
                    case PWRITE_FLOAT_I:         stackCall(WriteFloatAtIntOffset.BUILTIN); break;
                    case PWRITE_LONG_I:          stackCall(WriteLongAtIntOffset.BUILTIN); break;
                    case PWRITE_DOUBLE_I:        stackCall(WriteDoubleAtIntOffset.BUILTIN); break;
                    case PWRITE_WORD_I:          stackCall(WriteWordAtIntOffset.BUILTIN); break;
                    case PWRITE_REFERENCE_I:     stackCall(WriteReferenceAtIntOffset.BUILTIN); break;
                    case PGET_BYTE:              stackCall(GetByte.BUILTIN); break;
                    case PGET_CHAR:              stackCall(GetChar.BUILTIN); break;
                    case PGET_SHORT:             stackCall(GetShort.BUILTIN); break;
                    case PGET_INT:               stackCall(GetInt.BUILTIN); break;
                    case PGET_FLOAT:             stackCall(GetFloat.BUILTIN); break;
                    case PGET_LONG:              stackCall(GetLong.BUILTIN); break;
                    case PGET_DOUBLE:            stackCall(GetDouble.BUILTIN); break;
                    case PGET_WORD:              stackCall(GetWord.BUILTIN); break;
                    case PGET_REFERENCE:         stackCall(GetReference.BUILTIN); break;
                    case PSET_BYTE:              stackCall(SetByte.BUILTIN); break;
                    case PSET_SHORT:             stackCall(SetShort.BUILTIN); break;
                    case PSET_INT:               stackCall(SetInt.BUILTIN); break;
                    case PSET_FLOAT:             stackCall(SetFloat.BUILTIN); break;
                    case PSET_LONG:              stackCall(SetLong.BUILTIN); break;
                    case PSET_DOUBLE:            stackCall(SetDouble.BUILTIN); break;
                    case PSET_WORD:              stackCall(SetWord.BUILTIN); break;
                    case PSET_REFERENCE:         stackCall(SetReference.BUILTIN); break;
                    case PCMPSWP_INT:            stackCall(CompareAndSwapInt.BUILTIN); break;
                    case PCMPSWP_WORD:           stackCall(CompareAndSwapWord.BUILTIN); break;
                    case PCMPSWP_REFERENCE:      stackCall(CompareAndSwapReference.BUILTIN); break;
                    case PCMPSWP_INT_I:          stackCall(CompareAndSwapIntAtIntOffset.BUILTIN); break;
                    case PCMPSWP_WORD_I:         stackCall(CompareAndSwapWordAtIntOffset.BUILTIN); break;
                    case PCMPSWP_REFERENCE_I:    stackCall(CompareAndSwapReferenceAtIntOffset.BUILTIN); break;
                    default:                     throw verifyError("Unsupported bytecode: " + Bytecodes.nameOf(opcode));
                }
                break;
            }

            case MOV_I2F:                stackCall(IntToFloat.BUILTIN); break;
            case MOV_F2I:                stackCall(FloatToInt.BUILTIN); break;
            case MOV_L2D:                stackCall(LongToDouble.BUILTIN); break;
            case MOV_D2L:                stackCall(DoubleToLong.BUILTIN); break;

            case UWCMP: {
                switch (operand) {
                    case ABOVE_EQUAL: stackCall(GreaterEqual.BUILTIN); break;
                    case ABOVE_THAN:  stackCall(GreaterThan.BUILTIN); break;
                    case BELOW_EQUAL: stackCall(LessEqual.BUILTIN); break;
                    case BELOW_THAN:  stackCall(LessThan.BUILTIN); break;
                    default:          throw verifyError("Unsupported UWCMP operand: " + operand);
                }
                break;
            }
            case UCMP: {
                switch (operand) {
                    case ABOVE_EQUAL: stackCall(AboveEqual.BUILTIN); break;
                    case ABOVE_THAN : stackCall(AboveThan.BUILTIN); break;
                    case BELOW_EQUAL: stackCall(BelowEqual.BUILTIN); break;
                    case BELOW_THAN : stackCall(BelowThan.BUILTIN); break;
                    default:          throw verifyError("Unsupported UCMP operand: " + operand);
                }
                break;
            }
            case JNICALL:                jnicall(operand); break;
            case JNIOP: {
                ClassMethodActor classMethodActor = methodTranslation.classMethodActor();
                if (!classMethodActor.isNative()) {
                    throw verifyError("Cannot use " + Bytecodes.nameOf(JNIOP) + " instruction in non-native method " + classMethodActor);
                }
                switch (operand) {
                    case LINK: callAndPush(JavaOperator.LINK_OP, CirConstant.fromObject(classMethodActor)); break;
                    case J2N:  callAndPush(classMethodActor.isCFunction() ? JavaOperator.J2NC_OP : JavaOperator.J2N_OP); break;
                    case N2J:  callAndPush(classMethodActor.isCFunction() ? JavaOperator.N2JC_OP : JavaOperator.N2J_OP); break;
                    default:          throw verifyError("Unsupported JNIOP operand: " + operand);
                }
                break;
            }

            case INFOPOINT: {
                opcode = INFOPOINT | ((operand & ~0xff) << 8);
                switch (opcode) {
                    case SAFEPOINT:
                    case INFO:
                    case HERE:
                        CirRoutine cirRoutine = JavaOperator.INFOPOINT_OP;
                        CirValue[] regularArguments = { CirConstant.fromInt(opcode)};
                        final Kind resultKind = opcode == HERE ? Kind.LONG : Kind.VOID;
                        if (resultKind == Kind.VOID) {
                            final CirContinuation continuation = new CirContinuation();
                            call(cirRoutine, regularArguments, continuation);
                        } else {
                            final CirVariable result = methodTranslation.variableFactory().createTemporary(resultKind);
                            final CirContinuation continuation = new CirContinuation(result);
                            call(cirRoutine, regularArguments, continuation);
                            push(result);
                        }
                        break;
                    default:          throw verifyError("Unsupported INFOPOINT opcode: " + Bytecodes.nameOf(opcode));

                }
                break;
            }

            case READREG:                readreg(Role.VALUES.get(operand)); break;
            case WRITEREG:               writereg(Role.VALUES.get(operand)); break;
            case INCREG:                 increg(Role.VALUES.get(operand)); break;
            case ALLOCA:                 stackCall(StackAllocate.BUILTIN); break;
            case ALLOCSTKVAR:            stackCall(MakeStackVariable.BUILTIN); break;
            case PAUSE:                  stackCall(Pause.BUILTIN); break;
            case ADD_SP:                 stackCall(IncrementIntegerRegister.BUILTIN); break;
            case FLUSHW:                 stackCall(FlushRegisterWindows.BUILTIN); break;
            case LSB:                    stackCall(LeastSignificantBit.BUILTIN); break;
            case MSB:                    stackCall(MostSignificantBit.BUILTIN); break;

            case TEMPLATE_CALL: {
                Invocation inv = invoke(operand, 0);
                completeInvocation(new Call(inv.sig), inv.sig.resultKind(), inv.args);
                break;
            }
            case WRETURN: {
                assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
                currentCall.setProcedure(methodTranslation.variableFactory().normalContinuationParameter());
                final CirVariable result = stack.pop();
                currentCall.setArguments(result);
                break;
            }
            default: {
                throw verifyError("Unsupported bytecode: " + Bytecodes.nameOf(opcode));
            }

            case BREAKPOINT_TRAP: {
                ProgramWarning.message("ignoring breakpoint_trap in " + this.methodTranslation.classMethodActor());
                break;
            }

            // Checkstyle: resume
        }
        return true;
    }

    private void membar(int barriers) {
        callAndPush(new JavaBuiltinOperator(BarMemory.BUILTIN), CirConstant.fromInt(barriers));
    }

    private void readreg(VMRegister.Role role) {
        callAndPush(new JavaBuiltinOperator(GetIntegerRegister.BUILTIN), CirConstant.fromObject(role));
    }

    private void writereg(VMRegister.Role role) {
        callAndPush(new JavaBuiltinOperator(SetIntegerRegister.BUILTIN), CirConstant.fromObject(role), pop(Kind.WORD));
    }

    private void increg(VMRegister.Role role) {
        callAndPush(new JavaBuiltinOperator(IncrementIntegerRegister.BUILTIN), CirConstant.fromObject(role), pop(Kind.INT));
    }
}
