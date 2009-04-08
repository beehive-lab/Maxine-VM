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

import static com.sun.max.vm.compiler.Stoppable.Static.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.FieldReadSnippet.*;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.reference.*;
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

    protected final BlockState _blockState;
    protected final JavaFrame _frame;
    protected final JavaStack _stack;
    protected final BirToCirMethodTranslation _methodTranslation;
    protected final ConstantPool _constantPool;
    protected CirCall _currentCall;

    public BytecodeTranslation(BlockState blockState, BirToCirMethodTranslation methodTranslation) {
        _blockState = blockState;
        _frame = blockState.frame();
        _stack = blockState.stack();
        _methodTranslation = methodTranslation;
        _constantPool = methodTranslation.classMethodActor().codeAttribute().constantPool();
        final CirCall body = blockState.cirBlock().closure().body();
        _currentCall = body;
    }

    public String classMethodName() {
        return _methodTranslation.classMethodActor().name().toString();
    }

    private CirContinuation makeExceptionContinuation(BlockState dispatcherState) {
        final CirCall call = _methodTranslation.newCirCall(dispatcherState.cirBlock());
        final CirVariable throwable = _methodTranslation.stackVariableFactory().makeVariable(Kind.REFERENCE, 0);
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
        final CirCall call = _methodTranslation.newCirCall(_methodTranslation.getBlockStateAt(address).cirBlock());
        final CirContinuation continuation = new CirContinuation();
        continuation.setBody(call);
        return continuation;
    }

    private CirContinuation getBranchContinuation(int offset) {
        return getBlockContinuation(currentOpcodePosition() + offset);
    }

    private CirContinuation getAdjacentContinuation() {
        return getBlockContinuation(currentBytePosition());
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
        _currentCall.setProcedure(getAdjacentContinuation());
        _currentCall.setArguments(CirCall.NO_ARGUMENTS);
    }

    private void assign(CirVariable variable, CirValue value) {
        assert variable.kind().toStackKind() == value.kind().toStackKind() : incompatibleTypesErrorMessage(variable.kind().toStackKind(), value.kind().toStackKind());
        final CirClosure closure = new CirClosure();
        _currentCall.setProcedure(closure);
        _currentCall.setArguments(value);

        _currentCall = new CirCall();
        closure.setBody(_currentCall);
        closure.setParameters(variable);
    }

    protected void push(Kind kind, CirValue argument) {
        assert argument.kind() == kind : incompatibleTypesErrorMessage(argument.kind(), kind);
        final CirVariable stackVariable = _stack.push(kind);
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
        final CirVariable localVariable = _frame.makeVariable(kind, index);
        push(kind, localVariable);
    }

    private void localLoadReferenceOrWord(int index) {
        final CirVariable localVariable = _frame.getReferenceOrWordVariable(index);
        push(localVariable.kind(), localVariable);
    }

    private void localStore(Kind kind, int index) {
        final CirVariable stackVariable = pop(kind);
        final CirVariable localVariable = _frame.makeVariable(kind, index);
        assign(localVariable, stackVariable);
    }

    private void localStoreReferenceOrWord(int index) {
        final CirVariable stackVariable = popReferenceOrWord();
        final CirVariable localVariable = _frame.makeVariable(stackVariable.kind(), index);
        assign(localVariable, stackVariable);
    }

    private void createJavaFrameDescriptor() {
        _currentCall.setJavaFrameDescriptor(new CirJavaFrameDescriptor(_methodTranslation.classMethodActor().compilee(), currentOpcodePosition(), _frame.makeDescriptor(), _stack.makeDescriptor()));
    }

    protected void call(CirRoutine cirRoutine, CirValue[] regularArguments, CirValue normalContinuation) {
        if (Stoppable.Static.canStop(cirRoutine)) {
            createJavaFrameDescriptor();
        }

        final CirValue exceptionContinuation = Stoppable.Static.canStopWithException(cirRoutine) ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        _currentCall.setArguments(Arrays.append(CirValue.class, regularArguments, normalContinuation, exceptionContinuation));
        _currentCall.setProcedure((CirProcedure) cirRoutine);
        if (normalContinuation != CirValue.UNDEFINED) {
            _currentCall = new CirCall();
            final CirContinuation cc = (CirContinuation) normalContinuation;
            cc.setBody(_currentCall);
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
        final int numberOfArguments = parameterKinds.length;
        final CirValue[] arguments = CirClosure.newParameters(numberOfArguments);
        for (int i = numberOfArguments - 1; i >= 0; i--) {
            arguments[i] = pop(parameterKinds[i]);
        }
        callAndPush(cirRoutine, arguments);
    }

    protected void stackCall(Builtin builtin) {
        stackCall(CirBuiltin.get(builtin));
    }

    protected boolean isEndOfBlock() {
        return currentBytePosition() == _methodTranslation.getBlockStateAt(currentOpcodePosition()).birBlock().bytecodeBlock().end() + 1;
    }

    private void conditionalBranch(CirValue value1, CirSwitch cirSwitch, CirValue value2, int offset) {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirValue success = getBranchContinuation(offset);
        final CirValue failure = getAdjacentContinuation();
        assert failure != null : "expected fall through basic block";
        _currentCall.setArguments(value1, value2, success, failure);
        _currentCall.setProcedure(cirSwitch);
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
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter());
        final CirVariable result = pop(kind);
        _currentCall.setArguments(result);
    }

    protected void completeInvocation(CirValue method, Kind returnKind, CirValue[] arguments) {
        createJavaFrameDescriptor();

        CirContinuation continuation;
        if (returnKind == Kind.VOID) {
            continuation = new CirContinuation();
        } else {
            final CirVariable result = _stack.push(returnKind);
            continuation = new CirContinuation(result);
        }
        arguments[arguments.length - 2] = continuation;
        arguments[arguments.length - 1] = getCurrentExceptionContinuation();

        _currentCall.setArguments(arguments);
        _currentCall.setProcedure(method);
        _currentCall = new CirCall();
        continuation.setBody(_currentCall);
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
        final Tag tag = _constantPool.tagAt(index);
        switch (tag) {
            case CLASS: {
                final JavaOperator op = new Mirror(_constantPool, index);
                callAndPush(op);
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
        _currentCall.setProcedure(getBranchContinuation(offset));
        _currentCall.setArguments(CirCall.NO_ARGUMENTS);
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
        _currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        _currentCall.setProcedure(switchBuiltin);
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
        _currentCall.setArguments(arguments);
        final CirSwitch switchBuiltin = new CirSwitch(Kind.INT, ValueComparator.EQUAL, numberOfCases);
        _currentCall.setProcedure(switchBuiltin);
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
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter());
        final CirVariable result = popReferenceOrWord();
        _currentCall.setArguments(result);
    }

    @Override
    protected void vreturn() {
        _currentCall.setProcedure(_methodTranslation.variableFactory().normalContinuationParameter());
        _currentCall.setArguments(CirCall.NO_ARGUMENTS);
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
        Problem.unimplemented();
    }

    @Override
    protected void prologue() {
        if (_blockState.birBlock().hasSafepoint()) {
            if (MaxineVM.isPrototyping()) {
                final C_FUNCTION cFunctionAnnotation = _methodTranslation.classMethodActor().getAnnotation(C_FUNCTION.class);
                if (cFunctionAnnotation == null || !cFunctionAnnotation.isInterruptHandler()) {
                    callAndPush(JavaOperator.SAFEPOINT_OP);
                }
            } else {
                if (_methodTranslation.classMethodActor().isCFunction()) {
                    callAndPush(JavaOperator.SAFEPOINT_OP);
                }
            }
        }
    }

    @Override
    protected void getfield(int index) {
        final CirVariable reference = pop(Kind.REFERENCE);
        final JavaOperator getField = new GetField(_constantPool, index);
        callAndPush(getField, reference);
    }

    @Override
    protected void putfield(int index) {
        final Kind kind = _constantPool.fieldAt(index).type(_constantPool).toKind();
        final CirVariable value = pop(kind);
        final CirVariable reference = pop(Kind.REFERENCE);
        final JavaOperator putfield = new PutField(_constantPool, index);
        callAndPush(putfield, reference, value);
    }

    @Override
    protected void putstatic(int index) {
        final Kind kind = _constantPool.fieldAt(index).type(_constantPool).toKind();
        final CirVariable value = pop(kind);
        final JavaOperator putstatic = new PutStatic(_constantPool, index);
        callAndPush(putstatic, value);
    }

    @Override
    protected void getstatic(int index) {
        final FieldRefConstant fieldRef = _constantPool.fieldAt(index);
        if (fieldRef.isResolvableWithoutClassLoading(_constantPool)) {
            try {
                final FieldActor fieldActor = fieldRef.resolve(_constantPool, index);
                if (fieldActor.isFinal() && JavaTypeDescriptor.isPrimitive(fieldActor.descriptor()) && fieldActor.holder().isInitialized()) {
                    // This can be transformed directly into a constant value if the field holder has been initialized
                    final Value fieldValue;
                    if (MaxineVM.isPrototyping()) {
                        fieldValue = HostTupleAccess.readValue(null, fieldActor);
                    } else {
                        fieldValue = fieldActor.readValue(Reference.fromJava(fieldActor.holder().staticTuple()));
                    }
                    push(fieldValue);
                    return;
                }
                // all other cases, fall off to general getstatic, including when failing reflective access.
            } catch (LinkageError e) {
                // do nothing.
            }
        }
        final JavaOperator getstatic = new GetStatic(_constantPool, index);
        callAndPush(getstatic);
    }

    @Override
    protected void invokeinterface(int index, int countUnused) {
        final InterfaceMethodRefConstant interfaceMethodRef = _constantPool.interfaceMethodAt(index);
        final SignatureDescriptor signatureDescriptor = interfaceMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = CirCall.newArguments(parameterKinds.length + 3);
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i + 1] = _stack.pop();
        }
        final CirVariable receiver = popReferenceOrWord();
        arguments[0] = receiver;
        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, interfaceMethodRef.holder(_constantPool));
        final JavaOperator invokeinterface = new InvokeInterface(_constantPool, index);
        completeInvocation(invokeinterface, signatureDescriptor.getResultKind(), arguments);
    }

    private void invokeClassMethod(int index, JavaOperator op) {
        final ClassMethodRefConstant classMethodRef = _constantPool.classMethodAt(index);
        final SignatureDescriptor signatureDescriptor = classMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = CirCall.newArguments(parameterKinds.length + 3);
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i + 1] = _stack.pop();
        }
        final CirVariable receiver = popReferenceOrWord();
        arguments[0] = receiver;
        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor, classMethodRef.holder(_constantPool));
        completeInvocation(op, signatureDescriptor.getResultKind(), arguments);
    }

    @Override
    protected void invokevirtual(int index) {
        invokeClassMethod(index, new InvokeVirtual(_constantPool, index, _methodTranslation, _blockState));
    }

    @Override
    protected void invokespecial(int index) {
        invokeClassMethod(index, new InvokeSpecial(_constantPool, index));
    }

    @Override
    protected void invokestatic(int index) {
        final ClassMethodRefConstant classMethodRef = _constantPool.classMethodAt(index);
        final SignatureDescriptor signatureDescriptor = classMethodRef.signature(_constantPool);
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = CirCall.newArguments(parameterKinds.length + 2);
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i] = _stack.pop();
        }
        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);
        final JavaOperator op = new InvokeStatic(_constantPool, index);
        completeInvocation(op, signatureDescriptor.getResultKind(), arguments);
    }

    @Override
    protected void checkcast(int index) {
        final CirVariable object = getReferenceOrWordTop();
        final JavaOperator checkcast = new CheckCast(_constantPool, index);
        callAndPush(checkcast, object);
    }

    @Override
    protected void instanceof_(int index) {
        final CirVariable object = pop(Kind.REFERENCE);
        final JavaOperator instanceofOp = new InstanceOf(_constantPool, index);
        callAndPush(instanceofOp, object);
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
        final JavaOperator newOp = new New(_constantPool, index);
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
        final JavaOperator newarray = new NewArray(_constantPool, index);
        callAndPush(newarray, count);
    }

    @Override
    protected void multianewarray(int index, int nDimensions) {
        final JavaOperator op = new MultiANewArray(_constantPool, index, nDimensions);
        final CirVariable[] dimensions = CirClosure.newParameters(nDimensions);
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
     * @see Bytecode#CALLNATIVE
     */
    @Override
    protected void callnative(int nativeFunctionDescriptorIndex) {
        final CallNative op = new CallNative(_constantPool, nativeFunctionDescriptorIndex, _methodTranslation.classMethodActor());
        final SignatureDescriptor signatureDescriptor = op.signatureDescriptor();
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = CirCall.newArguments(parameterKinds.length + 2);
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i] = _stack.pop();
        }

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);

        completeInvocation(op, signatureDescriptor.getResultKind(), arguments);
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
        final CirVariable localVariable = _frame.makeVariable(Kind.INT, index);
        final CirContinuation continuation = new CirContinuation(localVariable);
        final JavaOperator op = JavaOperator.INT_PLUS;
        final CirValue exceptionContinuation = canStop(op) ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        _currentCall.setArguments(localVariable, CirConstant.fromInt(addend), continuation, exceptionContinuation);
        _currentCall.setProcedure(op);
        _currentCall = new CirCall();
        continuation.setBody(_currentCall);
    }

    @Override
    protected void athrow() {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final Throw athrow = new Throw();
        final CirVariable throwable = pop(Kind.REFERENCE);
        call(athrow, new CirValue[] {throwable}, CirValue.UNDEFINED);
    }
}
