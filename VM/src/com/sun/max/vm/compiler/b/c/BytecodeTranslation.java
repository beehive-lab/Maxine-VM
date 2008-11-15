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
package com.sun.max.vm.compiler.b.c;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Byte code visitor that generates CIR constructs for one BirBlock
 * in the context of a given BirToCirTranslation.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodeTranslation extends BytecodeVisitor {

    protected final BlockState _blockState;
    protected final JavaFrame _frame;
    protected final JavaStack _stack;
    protected final BirToCirMethodTranslation _methodTranslation;
    protected final ConstantPool _constantPool;
    protected CirCall _currentCall;
    protected BytecodeLocation _lastLocation;

    public BytecodeTranslation(BlockState blockState, BirToCirMethodTranslation methodTranslation) {
        _blockState = blockState;
        _frame = blockState.frame();
        _stack = blockState.stack();
        _methodTranslation = methodTranslation;
        _constantPool = methodTranslation.classMethodActor().codeAttribute().constantPool();
        final CirCall body = blockState.cirBlock().closure().body();
        _currentCall = body;
    }

    protected BytecodeLocation currentLocation() {
        if (_lastLocation == null || _lastLocation.position() != currentOpcodePosition()) {
            _lastLocation = new BytecodeLocation(_methodTranslation.classMethodActor().compilee(), currentOpcodePosition());
        }
        return _lastLocation;
    }

    public String classMethodName() {
        return _methodTranslation.classMethodActor().name().toString();
    }

    private CirContinuation makeExceptionContinuation(BlockState dispatcherState) {
        final CirCall call = new CirCall(dispatcherState.cirBlock());
        _methodTranslation.noteBlockCall(call);
        final CirVariable throwable = _methodTranslation.stackVariableFactory().makeVariable(Kind.REFERENCE, 0, currentLocation());
        final CirContinuation continuation = new CirContinuation(throwable);
        continuation.setBody(call);
        return continuation;
    }

    protected CirValue getCurrentExceptionContinuation() {
        final BlockState dispatcherState = _methodTranslation.getExceptionDispatcherState(currentOpcodePosition());
        if (dispatcherState == null) {
            return _methodTranslation.variableFactory().exceptionContinuationParameter();
        }
        if (dispatcherState.frame() == null) {
            dispatcherState.setFrame(_frame.copy());
        }
        return makeExceptionContinuation(dispatcherState);
    }

    private CirContinuation getBlockContinuation(int address) {
        final CirCall call = new CirCall(_methodTranslation.getBlockStateAt(address).cirBlock());
        _methodTranslation.noteBlockCall(call);
        final CirContinuation continuation = new CirContinuation();
        continuation.setBody(call);
        return continuation;
    }

    private CirContinuation getBranchContinuation(int offset) {
        return getBlockContinuation(currentOpcodePosition() + offset);
    }

    private CirContinuation getAdjacentContinuation() {
        return getBlockContinuation(currentByteAddress());
    }

    public void terminateBlock() {
        if (_currentCall.procedure() != null) {
            // A control flow byte code (e.g. xreturn, athrow, if...)
            // has occurred at the end of this basic block
            assert _currentCall.hasArguments();
            return;
        }
        // No control flow byte code has occurred,
        // so fall through to the adjacent basic block:
        _currentCall.setProcedure(getAdjacentContinuation(), null);
        _currentCall.setArguments();
    }

    private void assign(CirVariable variable, CirValue value) {
        assert variable.kind().toStackKind() == value.kind().toStackKind() : incompatibleTypesErrorMessage(variable.kind().toStackKind(), value.kind().toStackKind());
        final CirClosure closure = new CirClosure(currentLocation());
        _currentCall.setProcedure(closure, null);
        _currentCall.setArguments(value);

        _currentCall = new CirCall();
        closure.setBody(_currentCall);
        closure.setParameters(variable);
    }

    protected void push(Kind kind, CirValue argument) {
        assert argument.kind() == kind : incompatibleTypesErrorMessage(argument.kind(), kind);
        final CirVariable stackVariable = _stack.push(kind, currentLocation());
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
        final CirVariable stackVariable = _stack.pop();
        assert stackVariable.kind() == kind.toStackKind() : incompatibleTypesErrorMessage(stackVariable.kind(), kind.toStackKind());
        return stackVariable;
    }

    protected CirVariable popReferenceOrWord() {
        final CirVariable stackVariable = _stack.pop();
        assert stackVariable.kind() == Kind.REFERENCE || stackVariable.kind() == Kind.WORD : expectedReferenceOrWordErrorMessage(stackVariable.kind());
        return stackVariable;
    }

    protected CirVariable popTemporary() {
        final CirVariable stackVariable = _stack.pop();
        final CirVariable temporaryVariable = _methodTranslation.variableFactory().createTemporary(stackVariable.kind());
        assign(temporaryVariable, stackVariable);
        return temporaryVariable;
    }

    protected CirVariable getTop(Kind kind) {
        final CirVariable stackVariable = _stack.getTop();
        assert stackVariable.kind() == kind.toStackKind() : incompatibleTypesErrorMessage(stackVariable.kind(), kind.toStackKind());
        return stackVariable;
    }

    protected CirVariable getReferenceOrWordTop() {
        final CirVariable stackVariable = _stack.getTop();
        assert stackVariable.kind() == Kind.REFERENCE || stackVariable.kind() == Kind.WORD : expectedReferenceOrWordErrorMessage(stackVariable.kind());
        return stackVariable;
    }

    private void localLoad(Kind kind, int index) {
        final CirVariable localVariable = _frame.makeVariable(kind, index, currentLocation());
        push(kind, localVariable);
    }

    private void localLoadReferenceOrWord(int index) {
        final CirVariable localVariable = _frame.getReferenceOrWordVariable(index);
        push(localVariable.kind(), localVariable);
    }

    private void localStore(Kind kind, int index) {
        final CirVariable stackVariable = pop(kind);
        final CirVariable localVariable = _frame.makeVariable(kind, index, currentLocation());
        assign(localVariable, stackVariable);
    }

    private void localStoreReferenceOrWord(int index) {
        final CirVariable stackVariable = popReferenceOrWord();
        final CirVariable localVariable = _frame.makeVariable(stackVariable.kind(), index, currentLocation());
        assign(localVariable, stackVariable);
    }

    private void createJavaFrameDescriptor() {
        _currentCall.setJavaFrameDescriptor(new CirJavaFrameDescriptor(currentLocation(), _frame.makeDescriptor(), _stack.makeDescriptor()));
    }

    protected void call(CirRoutine cirRoutine, CirValue[] regularArguments, CirContinuation normalContinuation) {
        if (cirRoutine.needsJavaFrameDescriptor()) {
            createJavaFrameDescriptor();
        }
        final CirValue exceptionContinuation = cirRoutine.mayThrowException() ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        _currentCall.setArguments(Arrays.append(CirValue.class, regularArguments, normalContinuation, exceptionContinuation));
        _currentCall.setProcedure((CirProcedure) cirRoutine, currentLocation());
        _currentCall = new CirCall();
        normalContinuation.setBody(_currentCall);
    }


    protected void callAndPush(CirRoutine cirRoutine, CirValue... regularArguments) {
        final Kind resultKind = cirRoutine.resultKind();
        if (resultKind == Kind.VOID) {
            final CirContinuation continuation = new CirContinuation();
            call(cirRoutine, regularArguments, continuation);
        } else {
            final CirVariable result = _methodTranslation.variableFactory().createTemporary(resultKind);
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
        final int nRegularArguments = parameterKinds.length - 2;
        final CirValue[] regularArguments = new CirVariable[nRegularArguments];
        for (int i = nRegularArguments - 1; i >= 0; i--) {
            regularArguments[i] = pop(parameterKinds[i]);
        }
        callAndPush(cirRoutine, regularArguments);
    }

    protected void stackCall(Builtin builtin) {
        stackCall(CirBuiltin.get(builtin));
    }

    private void stackCall(Snippet snippet) {
        stackCall(CirSnippet.get(snippet));
    }

    private void stackCall(Builtin builtin, Snippet snippet) {
        if (_methodTranslation.cirGenerator().compilerScheme().isBuiltinImplemented(builtin)) {
            stackCall(builtin);
        } else {
            stackCall(snippet);
        }
    }

    private CirVariable callSnippet(Kind resultKind, Snippet snippet, CirValue... arguments) {
        final CirVariable result = _methodTranslation.variableFactory().createTemporary(resultKind);
        final CirContinuation continuation = new CirContinuation(result);
        call(CirSnippet.get(snippet), arguments, continuation);
        return result;
    }

    private CirVariable callResolutionSnippet(ResolutionSnippet snippet, int index) {
        final ResolutionGuard guard = _constantPool.makeResolutionGuard(index, snippet);
        return callSnippet(guard.kind(), snippet, CirConstant.fromObject(guard));
    }

    private void callSnippet(Snippet snippet, CirValue... arguments) {
        final CirContinuation continuation = new CirContinuation();
        call(CirSnippet.get(snippet), arguments, continuation);
    }

    protected boolean isEndOfBlock() {
        return currentByteAddress() == _methodTranslation.getBlockStateAt(currentOpcodePosition()).birBlock().bytecodeBlock().end() + 1;
    }

    private void conditionalBranch(CirValue value1, CirSwitch cirSwitch, CirValue value2, int offset) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirValue success = getBranchContinuation(offset);
        final CirValue failure = getAdjacentContinuation();
        assert failure != null : "expected fall through basic block";
        _currentCall.setArguments(value1, value2, success, failure);
        _currentCall.setProcedure(cirSwitch, currentLocation());
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
        assert value1.kind() == value2.kind() : incompatibleTypesErrorMessage(value1.kind(), value2.kind());
        conditionalBranch(value1, cirSwitch, value2, offset);
    }

    private void terminate(Kind kind) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter(), currentLocation());
        final CirVariable result = pop(kind);
        _currentCall.setArguments(result);
    }

    protected void completeInvocation(CirValue method, Kind returnKind, CirValue[] arguments) {
        createJavaFrameDescriptor();

        CirContinuation continuation;
        if (returnKind == Kind.VOID) {
            continuation = new CirContinuation();
        } else {
            final CirVariable result = _stack.push(returnKind, currentLocation());
            continuation = new CirContinuation(result);
        }
        arguments[arguments.length - 2] = continuation;
        arguments[arguments.length - 1] = getCurrentExceptionContinuation();

        _currentCall.setArguments(arguments);
        _currentCall.setProcedure(method, currentLocation());
        _currentCall = new CirCall();
        continuation.setBody(_currentCall);
    }

    /**
     * Only activate this snippet if you are willing to implement explicit null pointer check elimination.
     * @see Snippet.CheckNullPointer
     */
    private void implicitCheckNullPointer(CirVariable object) {
        if (object.kind() == Kind.REFERENCE) {
            callSnippet(Snippet.CheckNullPointer.SNIPPET, object);
        } else {
            // no null pointer check for WORD
            assert object.kind() == Kind.WORD;
        }
    }

    private void checkArrayIndex(CirVariable array, CirVariable index) {
        callSnippet(Snippet.CheckArrayIndex.SNIPPET, array, index);
    }

    private void checkReferenceArrayStore(CirVariable array, CirVariable value) {
        callSnippet(Snippet.CheckReferenceArrayStore.SNIPPET, array, value);
    }

    private void arrayLoad(ArrayGetSnippet snippet) {
        final CirVariable index = pop(Kind.INT);
        final CirVariable array = pop(Kind.REFERENCE);
        implicitCheckNullPointer(array);
        checkArrayIndex(array, index);
        callAndPush(snippet, array, index);
    }

    private void arrayStore(Kind kind, ArraySetSnippet snippet) {
        final CirVariable value = pop(kind);
        final CirVariable index = pop(Kind.INT);
        final CirVariable array = pop(Kind.REFERENCE);
        implicitCheckNullPointer(array);
        checkArrayIndex(array, index);
        if (kind == Kind.REFERENCE) {
            checkReferenceArrayStore(array, value);
        }
        callSnippet(snippet, array, index, value);
    }

    private void callMonitorSnippet(MonitorSnippet snippet) {
        final CirVariable object = pop(Kind.REFERENCE);
        implicitCheckNullPointer(object);
        callSnippet(snippet, object);
    }

    protected boolean areArgumentsMatchingSignatureDescriptor(CirValue[] arguments, SignatureDescriptor signatureDescriptor, TypeDescriptor receiverDescriptor) {
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final TypeDescriptor[] parameterDescriptors = signatureDescriptor.getParameterDescriptors();
        final int nonContinuationArgumentsLength = arguments.length - 2;
        int argumentIndex = nonContinuationArgumentsLength - parameterKinds.length;
        for (int parameterIndex = 0; parameterIndex < parameterKinds.length; parameterIndex++) {
            final Kind argumentKind = arguments[argumentIndex].kind();
            final Kind parameterKind = parameterKinds[parameterIndex].toStackKind();
            assertArgumentMatchesParameter(argumentIndex, argumentKind, parameterKind, parameterDescriptors[parameterIndex]);
            argumentIndex++;
        }
        if (receiverDescriptor != null) {
            if (_methodTranslation.classMethodActor().holder().kind() == Kind.WORD) {
                // Don't check code in the Word subclasses as it contains casts between WORD and REFERENCE types that only executes in prototype mode.
            } else {
                // Must be a non-static invocation - check the receiver
                assert nonContinuationArgumentsLength == parameterKinds.length + 1;
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
            assert argumentKind == Kind.WORD;
            assert parameterDescriptor.equals(JavaTypeDescriptor.ACCESSOR) :
                "argument " + argumentIndex + " can only be applied to a WORD parameter type or parameter of type " + Accessor.class.getName() + "(in " + _methodTranslation.birMethod() + ")";
            if (argumentKind != Kind.WORD) {
                assert parameterKind == Kind.WORD :  "argument " + argumentIndex + " cannot be of kind " + argumentKind + " when parameter is not of kind " + Kind.WORD;
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
    protected void prologue() {
        if (_blockState.birBlock().hasSafepoint()) {
            if (MaxineVM.isPrototyping()) {
                final C_FUNCTION cFunctionAnnotation = _methodTranslation.classMethodActor().getAnnotation(C_FUNCTION.class);
                if (cFunctionAnnotation == null || !cFunctionAnnotation.isInterruptHandler()) {
                    callAndPush(CirBuiltin.get(SafepointBuiltin.SoftSafepoint.BUILTIN));
                }
            } else {
                if (_methodTranslation.classMethodActor().isCFunction()) {
                    callAndPush(CirBuiltin.get(SafepointBuiltin.SoftSafepoint.BUILTIN));
                }
            }
        }
    }

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

    private CirValue getClassActor(int index) {
        final ClassConstant classConstant = _constantPool.classAt(index);
        if (classConstant.isResolvableWithoutClassLoading(_constantPool)) {
            return CirConstant.fromObject(_constantPool.classAt(index).resolve(_constantPool, index));
        }
        return callResolutionSnippet(ResolveClass.SNIPPET, index);
    }

    @Override
    protected void ldc(int index) {
        final Tag tag = _constantPool.tagAt(index);
        switch (tag) {
            case CLASS: {
                final CirVariable mirror = callSnippet(Kind.REFERENCE, BuiltinsSnippet.GetMirror.SNIPPET, getClassActor(index));
                push(mirror);
                break;
            }
            case INTEGER: {
                push(IntValue.from(_constantPool.intAt(index)));
                break;
            }
            case FLOAT: {
                push(FloatValue.from(_constantPool.floatAt(index)));
                break;
            }
            case STRING: {
                push(ReferenceValue.from(_constantPool.stringAt(index)));
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
        final Tag tag = _constantPool.tagAt(index);
        switch (tag) {
            case LONG: {
                push(LongValue.from(_constantPool.longAt(index)));
                break;
            }
            case DOUBLE: {
                push(DoubleValue.from(_constantPool.doubleAt(index)));
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
    protected void iaload() {
        arrayLoad(ArrayGetSnippet.GetInt.SNIPPET);
    }

    @Override
    protected void laload() {
        arrayLoad(ArrayGetSnippet.GetLong.SNIPPET);
    }

    @Override
    protected void faload() {
        arrayLoad(ArrayGetSnippet.GetFloat.SNIPPET);
    }

    @Override
    protected void daload() {
        arrayLoad(ArrayGetSnippet.GetDouble.SNIPPET);
    }

    @Override
    protected void aaload() {
        arrayLoad(ArrayGetSnippet.GetReference.SNIPPET);
    }

    @Override
    protected void baload() {
        arrayLoad(ArrayGetSnippet.GetByte.SNIPPET);
    }

    @Override
    protected void caload() {
        arrayLoad(ArrayGetSnippet.GetChar.SNIPPET);
    }

    @Override
    protected void saload() {
        arrayLoad(ArrayGetSnippet.GetShort.SNIPPET);
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
    protected void iastore() {
        arrayStore(Kind.INT, ArraySetSnippet.SetInt.SNIPPET);
    }

    @Override
    protected void lastore() {
        arrayStore(Kind.LONG, ArraySetSnippet.SetLong.SNIPPET);
    }

    @Override
    protected void fastore() {
        arrayStore(Kind.FLOAT, ArraySetSnippet.SetFloat.SNIPPET);
    }

    @Override
    protected void dastore() {
        arrayStore(Kind.DOUBLE, ArraySetSnippet.SetDouble.SNIPPET);
    }

    @Override
    protected void aastore() {
        arrayStore(Kind.REFERENCE, ArraySetSnippet.SetReference.SNIPPET);
    }

    @Override
    protected void bastore() {
        arrayStore(Kind.INT, ArraySetSnippet.SetByte.SNIPPET);
    }

    @Override
    protected void castore() {
        arrayStore(Kind.CHAR, ArraySetSnippet.SetChar.SNIPPET);
    }

    @Override
    protected void sastore() {
        arrayStore(Kind.SHORT, ArraySetSnippet.SetShort.SNIPPET);
    }

    @Override
    protected void pop() {
        final CirVariable value = _stack.pop();
        assert value.isCategory1() : expectedCategory1ErrorMessage();
    }

    @Override
    protected void pop2() {
        final CirVariable value = _stack.pop();
        if (value.isCategory1()) {
            pop();
        }
    }

    @Override
    protected void dup() {
        final CirVariable value = _stack.getTop();
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
    protected void iadd() {
        stackCall(JavaBuiltin.IntPlus.BUILTIN);
    }

    @Override
    protected void ladd() {
        stackCall(JavaBuiltin.LongPlus.BUILTIN);
    }

    @Override
    protected void fadd() {
        stackCall(JavaBuiltin.FloatPlus.BUILTIN);
    }

    @Override
    protected void dadd() {
        stackCall(JavaBuiltin.DoublePlus.BUILTIN);
    }

    @Override
    protected void isub() {
        stackCall(JavaBuiltin.IntMinus.BUILTIN);
    }

    @Override
    protected void lsub() {
        stackCall(JavaBuiltin.LongMinus.BUILTIN);
    }

    @Override
    protected void fsub() {
        stackCall(JavaBuiltin.FloatMinus.BUILTIN);
    }

    @Override
    protected void dsub() {
        stackCall(JavaBuiltin.DoubleMinus.BUILTIN);
    }

    @Override
    protected void imul() {
        stackCall(JavaBuiltin.IntTimes.BUILTIN);
    }

    @Override
    protected void lmul() {
        stackCall(JavaBuiltin.LongTimes.BUILTIN, Snippet.LongTimes.SNIPPET);
    }

    @Override
    protected void fmul() {
        stackCall(JavaBuiltin.FloatTimes.BUILTIN);
    }

    @Override
    protected void dmul() {
        stackCall(JavaBuiltin.DoubleTimes.BUILTIN);
    }

    @Override
    protected void idiv() {
        stackCall(JavaBuiltin.IntDivided.BUILTIN);
    }

    @Override
    protected void ldiv() {
        stackCall(JavaBuiltin.LongDivided.BUILTIN, Snippet.LongDivided.SNIPPET);
    }

    @Override
    protected void fdiv() {
        stackCall(JavaBuiltin.FloatDivided.BUILTIN);
    }

    @Override
    protected void ddiv() {
        stackCall(JavaBuiltin.DoubleDivided.BUILTIN);
    }

    @Override
    protected void irem() {
        stackCall(JavaBuiltin.IntRemainder.BUILTIN);
    }

    @Override
    protected void lrem() {
        stackCall(JavaBuiltin.LongRemainder.BUILTIN, Snippet.LongRemainder.SNIPPET);
    }

    @Override
    protected void frem() {
        stackCall(JavaBuiltin.FloatRemainder.BUILTIN, Snippet.FloatRemainder.SNIPPET);
    }

    @Override
    protected void drem() {
        stackCall(JavaBuiltin.DoubleRemainder.BUILTIN, Snippet.DoubleRemainder.SNIPPET);
    }

    @Override
    protected void ineg() {
        stackCall(JavaBuiltin.IntNegated.BUILTIN);
    }

    @Override
    protected void lneg() {
        stackCall(JavaBuiltin.LongNegated.BUILTIN);
    }

    @Override
    protected void fneg() {
        stackCall(JavaBuiltin.FloatNegated.BUILTIN);
    }

    @Override
    protected void dneg() {
        stackCall(JavaBuiltin.DoubleNegated.BUILTIN);
    }

    @Override
    protected void ishl() {
        stackCall(JavaBuiltin.IntShiftedLeft.BUILTIN);
    }

    @Override
    protected void lshl() {
        stackCall(JavaBuiltin.LongShiftedLeft.BUILTIN);
    }

    @Override
    protected void ishr() {
        stackCall(JavaBuiltin.IntSignedShiftedRight.BUILTIN);
    }

    @Override
    protected void lshr() {
        stackCall(JavaBuiltin.LongSignedShiftedRight.BUILTIN, Snippet.LongSignedShiftedRight.SNIPPET);
    }

    @Override
    protected void iushr() {
        stackCall(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN);
    }

    @Override
    protected void lushr() {
        stackCall(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN);
    }

    @Override
    protected void iand() {
        stackCall(JavaBuiltin.IntAnd.BUILTIN);
    }

    @Override
    protected void land() {
        stackCall(JavaBuiltin.LongAnd.BUILTIN);
    }

    @Override
    protected void ior() {
        stackCall(JavaBuiltin.IntOr.BUILTIN);
    }

    @Override
    protected void lor() {
        stackCall(JavaBuiltin.LongOr.BUILTIN);
    }

    @Override
    protected void ixor() {
        stackCall(JavaBuiltin.IntXor.BUILTIN);
    }

    @Override
    protected void lxor() {
        stackCall(JavaBuiltin.LongXor.BUILTIN);
    }

    @Override
    protected void iinc(int index, int addend) {
        final CirVariable localVariable = _frame.makeVariable(Kind.INT, index, currentLocation());
        final CirContinuation continuation = new CirContinuation(localVariable);
        _currentCall.setArguments(localVariable, CirConstant.fromInt(addend), continuation, getCurrentExceptionContinuation());
        _currentCall.setProcedure(CirBuiltin.get(JavaBuiltin.IntPlus.BUILTIN), currentLocation());
        _currentCall = new CirCall();
        continuation.setBody(_currentCall);
    }

    @Override
    protected void i2l() {
        stackCall(JavaBuiltin.ConvertIntToLong.BUILTIN);
    }

    @Override
    protected void i2f() {
        stackCall(JavaBuiltin.ConvertIntToFloat.BUILTIN);
    }

    @Override
    protected void i2d() {
        stackCall(JavaBuiltin.ConvertIntToDouble.BUILTIN);
    }

    @Override
    protected void l2i() {
        stackCall(JavaBuiltin.ConvertLongToInt.BUILTIN);
    }

    @Override
    protected void l2f() {
        stackCall(JavaBuiltin.ConvertLongToFloat.BUILTIN);
    }

    @Override
    protected void l2d() {
        stackCall(JavaBuiltin.ConvertLongToDouble.BUILTIN);
    }



    @Override
    protected void f2i() {
        stackCall(Snippet.ConvertFloatToInt.SNIPPET);
    }

    @Override
    protected void f2l() {
        stackCall(Snippet.ConvertFloatToLong.SNIPPET);
    }

    @Override
    protected void d2i() {
        stackCall(Snippet.ConvertDoubleToInt.SNIPPET);
    }

    @Override
    protected void d2l() {
        stackCall(Snippet.ConvertDoubleToLong.SNIPPET);
    }

    @Override
    protected void f2d() {
        stackCall(JavaBuiltin.ConvertFloatToDouble.BUILTIN);
    }

    @Override
    protected void d2f() {
        stackCall(JavaBuiltin.ConvertDoubleToFloat.BUILTIN);
    }

    @Override
    protected void i2b() {
        stackCall(JavaBuiltin.ConvertIntToByte.BUILTIN);
    }

    @Override
    protected void i2c() {
        stackCall(JavaBuiltin.ConvertIntToChar.BUILTIN);
    }

    @Override
    protected void i2s() {
        stackCall(JavaBuiltin.ConvertIntToShort.BUILTIN);
    }

    @Override
    protected void lcmp() {
        stackCall(JavaBuiltin.LongCompare.BUILTIN, Snippet.LongCompare.SNIPPET);
    }

    @Override
    protected void fcmpl() {
        stackCall(JavaBuiltin.FloatCompareL.BUILTIN);
    }

    @Override
    protected void fcmpg() {
        stackCall(JavaBuiltin.FloatCompareG.BUILTIN);
    }

    @Override
    protected void dcmpl() {
        stackCall(JavaBuiltin.DoubleCompareL.BUILTIN);
    }

    @Override
    protected void dcmpg() {
        stackCall(JavaBuiltin.DoubleCompareG.BUILTIN);
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
        _currentCall.setProcedure(getBranchContinuation(offset), currentLocation());
        _currentCall.setArguments();
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
        final CirValue[] arguments = new CirValue[nArguments];
        arguments[0] = pop(Kind.INT);
        final BytecodeScanner scanner = getBytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            final int match = lowMatch + i;
            arguments[1 + i] = CirConstant.fromInt(match);
            arguments[1 + numberOfCases + i] = getBranchContinuation(scanner.readSwitchOffset());
        }
        arguments[1 + (numberOfCases * 2)] = getBranchContinuation(defaultOffset);
        _currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        _currentCall.setProcedure(switchBuiltin, currentLocation());
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int nArguments = (numberOfCases * 2) + 2;
        final CirValue[] arguments = new CirValue[nArguments];
        arguments[0] = pop(Kind.INT);
        final BytecodeScanner scanner = getBytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            arguments[1 + i] = CirConstant.fromInt(scanner.readSwitchCase());
            arguments[1 + numberOfCases + i] = getBranchContinuation(scanner.readSwitchOffset());
        }
        arguments[1 + (numberOfCases * 2)] = getBranchContinuation(defaultOffset);
        _currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        _currentCall.setProcedure(switchBuiltin, currentLocation());
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
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter(), currentLocation());
        final CirVariable result = popReferenceOrWord();
        _currentCall.setArguments(result);
    }

    @Override
    protected void vreturn() {
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter(), currentLocation());
        _currentCall.setArguments();
    }

    private CirVariable getStaticTuple(CirVariable fieldActor) {
        return callSnippet(Kind.REFERENCE, BuiltinsSnippet.GetStaticTuple.SNIPPET, fieldActor);
    }

    private void generateFieldReadSnippetCall(int index, CirVariable fieldActor, CirVariable tuple) {
        final Kind kind = _constantPool.fieldAt(index).type(_constantPool).toKind();
        callAndPush(FieldReadSnippet.selectSnippet(kind), tuple, fieldActor);
    }

    private void generateFieldWriteSnippetCall(Kind kind, CirVariable tuple, CirVariable fieldActor, CirVariable value) {
        switch (kind.asEnum()) {
            case BYTE:
                callAndPush(FieldWriteSnippet.WriteByte.SNIPPET, tuple, fieldActor, value);
                break;
            case BOOLEAN:
                callAndPush(FieldWriteSnippet.WriteBoolean.SNIPPET, tuple, fieldActor, value);
                break;
            case SHORT:
                callAndPush(FieldWriteSnippet.WriteShort.SNIPPET, tuple, fieldActor, value);
                break;
            case CHAR:
                callAndPush(FieldWriteSnippet.WriteChar.SNIPPET, tuple, fieldActor, value);
                break;
            case INT:
                callAndPush(FieldWriteSnippet.WriteInt.SNIPPET, tuple, fieldActor, value);
                break;
            case FLOAT:
                callAndPush(FieldWriteSnippet.WriteFloat.SNIPPET, tuple, fieldActor, value);
                break;
            case LONG:
                callAndPush(FieldWriteSnippet.WriteLong.SNIPPET, tuple, fieldActor, value);
                break;
            case DOUBLE:
                callAndPush(FieldWriteSnippet.WriteDouble.SNIPPET, tuple, fieldActor, value);
                break;
            case WORD:
                callAndPush(FieldWriteSnippet.WriteWord.SNIPPET, tuple, fieldActor, value);
                break;
            case REFERENCE:
                callAndPush(FieldWriteSnippet.WriteReference.SNIPPET, tuple, fieldActor, value);
                break;
            default:
                throw classFormatError("Fields cannot have type void");
        }
    }

    @Override
    protected void getstatic(int index) {
        final FieldRefConstant fieldRef = _constantPool.fieldAt(index);
        if (fieldRef.isResolvableWithoutClassLoading(_constantPool)) {
            try {
                final FieldActor fieldActor = fieldRef.resolve(_constantPool, index);
                if (fieldActor.isFinal() && JavaTypeDescriptor.isPrimitive(fieldActor.descriptor()) && fieldActor.holder().isInitialized()) {
                    // This can be transformed directly into a constant value if the field holder has been initialized
                    Value fieldValue = null;
                    if (MaxineVM.isPrototyping()) {
                        final Field field = fieldActor.toJava();
                        field.setAccessible(true);
                        fieldValue = Value.fromBoxedJavaValue(field.get(null));
                    } else {
                        fieldValue = fieldActor.readValue(Reference.fromJava(fieldActor.holder().staticTuple()));
                    }
                    push(fieldValue);
                    return;
                }
                // all other cases, fall off to general getstatic, including when failing reflective access.
            } catch (LinkageError e) {
            } catch (IllegalAccessException e) {
            }
        }
        final CirVariable fieldActor = callResolutionSnippet(ResolveStaticFieldForReading.SNIPPET, index);
        final CirVariable staticTuple = getStaticTuple(fieldActor);
        generateFieldReadSnippetCall(index, fieldActor, staticTuple);
    }

    @Override
    protected void putstatic(int index) {
        final CirVariable fieldActor = callResolutionSnippet(ResolveStaticFieldForWriting.SNIPPET, index);
        final CirVariable staticTuple = getStaticTuple(fieldActor);
        final Kind kind = _constantPool.fieldAt(index).type(_constantPool).toKind();
        final CirVariable value = pop(kind);
        generateFieldWriteSnippetCall(kind, staticTuple, fieldActor, value);
    }

    @Override
    protected void getfield(int index) {
        final CirVariable fieldActor = callResolutionSnippet(ResolveInstanceFieldForReading.SNIPPET, index);
        final CirVariable reference = pop(Kind.REFERENCE);
        implicitCheckNullPointer(reference);
        generateFieldReadSnippetCall(index, fieldActor, reference);
    }

    @Override
    protected void putfield(int index) {
        final CirVariable fieldActor = callResolutionSnippet(ResolveInstanceFieldForWriting.SNIPPET, index);
        final Kind kind = _constantPool.fieldAt(index).type(_constantPool).toKind();
        final CirVariable value = pop(kind);
        final CirVariable reference = pop(Kind.REFERENCE);
        implicitCheckNullPointer(reference);
        generateFieldWriteSnippetCall(kind, reference, fieldActor, value);
    }

    @Override
    protected void invokevirtual(int index) {
        final ClassMethodRefConstant classMethodRef = _constantPool.classMethodAt(index);
        final SignatureDescriptor signatureDescriptor = classMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 3];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i + 1] = _stack.pop();
        }
        final CirVariable receiver = popReferenceOrWord();
        arguments[0] = receiver;

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, classMethodRef.holder(_constantPool));
        implicitCheckNullPointer(receiver);

        final Hub mostFrequentHub = getMostFrequentHub();

        if (mostFrequentHub != null) {
            invokevirtualWithGuardedInline(index, classMethodRef, signatureDescriptor, arguments, receiver, mostFrequentHub);
        } else {
            final CirVariable methodActor = callResolutionSnippet(ResolveVirtualMethod.SNIPPET, index);
            final CirVariable callEntryPoint = callSnippet(Kind.WORD, MethodSelectionSnippet.SelectVirtualMethod.SNIPPET, receiver, methodActor);
            completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), arguments);
        }
    }

    private Hub getMostFrequentHub() {
        final MethodInstrumentation instrumentation = VMConfiguration.target().compilationScheme().getMethodInstrumentation(currentLocation().classMethodActor());
        final Hub mostFrequentHub = (instrumentation == null)
                                ? null
                                : instrumentation.getMostFrequentlyUsedHub(currentLocation().position());
        return mostFrequentHub;
    }

    private void invokevirtualWithGuardedInline(int index, final ClassMethodRefConstant classMethodRef, final SignatureDescriptor signatureDescriptor, final CirValue[] arguments,
                    final CirVariable receiver, final Hub mostFrequentHub) {
        final VirtualMethodActor declaredMethod = classMethodRef.resolveVirtual(_constantPool, index);
        final CirVariable hub = callSnippet(Kind.REFERENCE, MethodSelectionSnippet.ReadHub.SNIPPET, receiver);
        final CirValue cachedHub = new CirConstant(ReferenceValue.from(mostFrequentHub));

        final Address entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();

        final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
        final CirMethod targetCirMethod = _methodTranslation.cirGenerator().createIrMethod(targetMethod.classMethodActor());
        final CirContinuation successCont = new CirContinuation();
        final CirContinuation failureCont = new CirContinuation();

        _currentCall.setProcedure(CirSwitch.REFERENCE_EQUAL, currentLocation());
        _currentCall.setArguments(hub, cachedHub, successCont, failureCont);

        final CirCall kBlockCall = new CirCall();
        final CirClosure kBlockClosure = new CirClosure(currentLocation());
        kBlockClosure.setBody(kBlockCall);
        final CirBlock kBlock = new CirBlock(kBlockClosure);

        final CirCall successCall = new CirCall();
        successCont.setBody(successCall);
        successCont.setParameters();
        _currentCall = successCall;
        completeInvocation(targetCirMethod, signatureDescriptor.getResultKind(), arguments);
        _currentCall.setProcedure(kBlock, currentLocation());
        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            _currentCall.setArguments(_stack.pop());
        } else {
            _currentCall.setArguments();
        }

        final CirValue[] fkArguments = new CirValue[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            fkArguments[i] = arguments[i];
        }

        final CirCall failureCall = new CirCall();
        failureCont.setBody(failureCall);
        failureCont.setParameters();
        _currentCall = failureCall;
        final CirVariable methodActor = callResolutionSnippet(ResolveVirtualMethod.SNIPPET, index);
        final CirVariable callEntryPoint = callSnippet(Kind.WORD, MethodSelectionSnippet.SelectVirtualMethod.SNIPPET, receiver, methodActor);
        completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), fkArguments);
        _currentCall.setProcedure(kBlock, currentLocation());
        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            _currentCall.setArguments(_stack.pop());
        } else {
            _currentCall.setArguments();
        }

        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            final CirVariable result = _stack.push(signatureDescriptor.getResultKind(), currentLocation());
            kBlockClosure.setParameters(result);
        } else {
            kBlockClosure.setParameters();
        }
        _currentCall = kBlockCall;
    }

    private void invokeinterfaceWithGuardedInline(int index, final InterfaceMethodRefConstant interfaceMethodRef, final SignatureDescriptor signatureDescriptor, final CirValue[] arguments,
                    final CirVariable receiver, final Hub mostFrequentHub) {
        final MethodActor methodActor1 = interfaceMethodRef.resolve(_constantPool, index);
        final CirVariable hub = callSnippet(Kind.REFERENCE, MethodSelectionSnippet.ReadHub.SNIPPET, receiver);
        final CirValue cachedHub = new CirConstant(ReferenceValue.from(mostFrequentHub));

        final Address entryPoint;
        if (methodActor1 instanceof VirtualMethodActor) {
            final VirtualMethodActor declaredMethod = (VirtualMethodActor) methodActor1;
            entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();
        } else {
            assert methodActor1 instanceof InterfaceMethodActor : "methoeActor not interface MethodActor";
            final InterfaceMethodActor declaredMethod = (InterfaceMethodActor) methodActor1;
            final InterfaceActor interfaceActor = (InterfaceActor) declaredMethod.holder();
            final int interfaceIndex = mostFrequentHub.getITableIndex(interfaceActor.id());
            entryPoint =  mostFrequentHub.getWord(interfaceIndex + declaredMethod.iIndexInInterface()).asAddress();
        }

        final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
        final CirMethod targetCirMethod = _methodTranslation.cirGenerator().createIrMethod(targetMethod.classMethodActor());
        final CirContinuation successCont = new CirContinuation();
        final CirContinuation failureCont = new CirContinuation();

        _currentCall.setProcedure(CirSwitch.REFERENCE_EQUAL, currentLocation());
        _currentCall.setArguments(hub, cachedHub, successCont, failureCont);

        final CirCall kBlockCall = new CirCall();
        final CirClosure kBlockClosure = new CirClosure(currentLocation());
        kBlockClosure.setBody(kBlockCall);
        final CirBlock kBlock = new CirBlock(kBlockClosure);
        _blockState.addCirBlock(kBlock);

        final CirCall successCall = new CirCall();
        successCont.setBody(successCall);
        successCont.setParameters();
        _currentCall = successCall;
        completeInvocation(targetCirMethod, signatureDescriptor.getResultKind(), arguments);
        _currentCall.setProcedure(kBlock, currentLocation());
        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            _currentCall.setArguments(_stack.pop());
        } else {
            _currentCall.setArguments();
        }

        final CirValue[] fkArguments = new CirValue[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            fkArguments[i] = arguments[i];
        }

        final CirCall failureCall = new CirCall();
        failureCont.setBody(failureCall);
        failureCont.setParameters();
        _currentCall = failureCall;

        final CirValue methodActor = getInterfaceMethodActor(index, interfaceMethodRef);
        final CirVariable callEntryPoint = callSnippet(Kind.WORD, MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET, receiver, methodActor);
        completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), fkArguments);
        _currentCall.setProcedure(kBlock, currentLocation());
        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            _currentCall.setArguments(_stack.pop());
        } else {
            _currentCall.setArguments();
        }

        if (signatureDescriptor.getResultKind() != Kind.VOID) {
            final CirVariable result = _stack.push(signatureDescriptor.getResultKind(), currentLocation());
            kBlockClosure.setParameters(result);
        } else {
            kBlockClosure.setParameters();
        }
        _currentCall = kBlockCall;
    }
    @Override
    protected void invokespecial(int index) {
        final ClassMethodRefConstant classMethodRef = _constantPool.classMethodAt(index);
        final SignatureDescriptor signatureDescriptor = classMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 3];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i + 1] = _stack.pop();
        }
        final CirVariable receiver = popReferenceOrWord();
        arguments[0] = receiver;

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, classMethodRef.holder(_constantPool));

        final CirVariable callEntryPoint = callResolutionSnippet(ResolveSpecialMethod.SNIPPET, index);
        implicitCheckNullPointer(receiver);
        completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), arguments);
    }

    @Override
    protected void invokestatic(int index) {
        final SignatureDescriptor signatureDescriptor = _constantPool.classMethodAt(index).signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 2];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i] = _stack.pop();
        }

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);

        final CirVariable callEntryPoint = callResolutionSnippet(ResolveStaticMethod.SNIPPET, index);
        completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), arguments);
    }

    /**
     * @see Bytecode#CALLNATIVE
     */
    @Override
    protected void callnative(int nativeFunctionDescriptorIndex) {
        final SignatureDescriptor signatureDescriptor = SignatureDescriptor.create(_constantPool.utf8At(nativeFunctionDescriptorIndex, "native function descriptor"));
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 2];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i] = _stack.pop();
        }

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);

        final MethodActor classMethodActor = _methodTranslation.classMethodActor();
        final boolean callerIsCFunction = classMethodActor.isCFunction();

        final CirVariable callEntryPoint = callSnippet(Kind.WORD, LinkNativeMethod.SNIPPET, CirConstant.fromObject(classMethodActor));
        CirVariable vmThreadLocals = null;
        if (!callerIsCFunction) {
            vmThreadLocals = callSnippet(Kind.WORD, NativeCallPrologue.SNIPPET);
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        vmThreadLocals = callSnippet(Kind.WORD, NativeCallPrologueForC.SNIPPET);
                    }
                } else {
                    vmThreadLocals = callSnippet(Kind.WORD, NativeCallPrologueForC.SNIPPET);
                }
            }
        }
        completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), arguments);
        if (!callerIsCFunction) {
            callSnippet(NativeCallEpilogue.SNIPPET, vmThreadLocals);
        } else {
            if (vmThreadLocals != null) {
                callSnippet(NativeCallEpilogueForC.SNIPPET, vmThreadLocals);
            }
        }
    }

    private CirValue getInterfaceMethodActor(int index, InterfaceMethodRefConstant interfaceMethodRef) {
        if (interfaceMethodRef.isResolvableWithoutClassLoading(_constantPool)) {
            try {
                return CirConstant.fromObject(interfaceMethodRef.resolve(_constantPool, index));
            } catch (NoSuchMethodError noSuchMethodError) {
                // this must be a @PROTOTYPE_ONLY method
                // do nothing: dead code elimination will take care of this
            }
        }
        return callResolutionSnippet(ResolveInterfaceMethod.SNIPPET, index);
    }

    @Override
    protected void invokeinterface(int index, int countUnused) {
        final InterfaceMethodRefConstant interfaceMethodRef = _constantPool.interfaceMethodAt(index);
        final SignatureDescriptor signatureDescriptor = interfaceMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 3];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i + 1] = _stack.pop();
        }
        final CirVariable receiver = popReferenceOrWord();
        arguments[0] = receiver;

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, interfaceMethodRef.holder(_constantPool));

        final Hub mostFrequentHub = getMostFrequentHub();
        if (mostFrequentHub != null) {
            invokeinterfaceWithGuardedInline(index, interfaceMethodRef, signatureDescriptor, arguments, receiver, mostFrequentHub);
        } else {
            final CirValue methodActor = getInterfaceMethodActor(index, interfaceMethodRef);
            implicitCheckNullPointer(receiver);
            final CirVariable callEntryPoint = callSnippet(Kind.WORD, MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET, receiver, methodActor);
            completeInvocation(callEntryPoint, signatureDescriptor.getResultKind(), arguments);
        }
    }

    private CirValue getInitializedClassActor(int index) {
        final ClassConstant classConstant = _constantPool.classAt(index);
        if (classConstant.isResolvableWithoutClassLoading(_constantPool)) {
            final ClassActor classActor = classConstant.resolve(_constantPool, index);
            if (classActor.isInitialized()) {
                return CirConstant.fromObject(classActor);
            }
        }
        return callResolutionSnippet(ResolveTypeAndMakeInitialized.SNIPPET, index);
    }

    @Override
    protected void new_(int index) {
        callAndPush(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET, getInitializedClassActor(index));
    }

    @Override
    protected void newarray(int tag) {
        final CirVariable length = pop(Kind.INT);
        callAndPush(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET, CirConstant.fromObject(Kind.fromNewArrayTag(tag)), length);
    }

    @Override
    protected void anewarray(int index) {
        final ClassConstant classConstant = _constantPool.classAt(index);
        CirValue arrayClassActor;
        if (classConstant.isResolvableWithoutClassLoading(_constantPool)) {
            arrayClassActor = CirConstant.fromObject(ArrayClassActor.forComponentClassActor(_constantPool.classAt(index).resolve(_constantPool, index)));
        } else {
            arrayClassActor = callResolutionSnippet(ResolveArrayClass.SNIPPET, index);
        }
        final CirVariable length = pop(Kind.INT);
        callAndPush(NonFoldableSnippet.CreateReferenceArray.SNIPPET, arrayClassActor, length);
    }

    @Override
    protected void arraylength() {
        stackCall(ArrayGetSnippet.ReadLength.SNIPPET);
    }

    @Override
    protected void athrow() {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirVariable throwable = pop(Kind.REFERENCE);
        _currentCall.setArguments(throwable);
        _currentCall.setProcedure(getCurrentExceptionContinuation(), currentLocation());
    }

    @Override
    protected void checkcast(int index) {
        final CirValue classActor = getClassActor(index);
        final CirVariable object = getReferenceOrWordTop();
        callAndPush(Snippet.CheckCast.SNIPPET, classActor, object);
    }

    @Override
    protected void instanceof_(int index) {
        final CirValue classActor = getClassActor(index);
        final CirVariable object = pop(Kind.REFERENCE);
        callAndPush(Snippet.InstanceOf.SNIPPET, classActor, object);
    }

    @Override
    protected void monitorenter() {
        callMonitorSnippet(MonitorSnippet.MonitorEnter.SNIPPET);
    }

    @Override
    protected void monitorexit() {
        callMonitorSnippet(MonitorSnippet.MonitorExit.SNIPPET);
    }

    @Override
    protected void wide() {
        // NOTE: we do not need to emit code for WIDE because BytecodeScanner automatically widens opcodes
    }

    @Override
    protected void multianewarray(int index, int nDimensions) {
        assert nDimensions >= 1 : "nDimensions < 1";
        final CirValue arrayClassActor = getClassActor(index);

        // At runtime, create an int array of length 'nDimensions':
        final CirVariable lengths = callSnippet(Kind.REFERENCE, NonFoldableSnippet.CreatePrimitiveArray.SNIPPET, CirConstant.fromObject(Kind.INT), CirConstant.fromInt(nDimensions));

        // At runtime, pop array lengths and assign them to array elements,
        // in the ordering in which they were once pushed:
        // and check they are greater than or equal to zero
        for (int i = 1; i <= nDimensions; i++) {
            final CirVariable length = pop(Kind.INT);
            callSnippet(Snippet.CheckArrayDimension.SNIPPET, length);
            callSnippet(ArraySetSnippet.SetInt.SNIPPET, lengths, CirConstant.fromInt(nDimensions - i), length);
        }
        callAndPush(NonFoldableSnippet.CreateMultiReferenceArray.SNIPPET, arrayClassActor, lengths);
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
        Problem.unimplemented();
    }

}
