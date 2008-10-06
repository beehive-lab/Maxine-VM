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
/*VCSID=91882a23-80fc-4c2a-ae5d-b45e8093137f*/
package com.sun.max.vm.compiler.b.c;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.FieldReadSnippet.*;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * BytecodeToHCirTranslation is similar to {@link BytecodeTranslation} except that the output is
 * an HCir tree (defined below) instead of an LCir tree that {@link BytecodeTranslation} produces.
 *
 * An HCir tree is a Cir tree with some restriction on what can appear in the operator position
 * of a @{link CirCall}.  HCir allows only the following types of operators:
 *   1. {@link JavaBuiltin}
 *   2. {@link CirClosure}
 *   3. {@link CirBlock}
 *   4. {@link CirVariable}
 *   5. {@link CirSwitch}
 *   6. {@link JavaOperator}
 *
 * Basically, only the operators and constructs that can be found in JVM bytecode can appear in
 * HCir.  Other builtins, snippets, or other Cir values are now allowed to appear here but are
 * introduced in the lowering pass. (see {@link HCirToLCirTranslation})
 *
 * The rationale for this transformation is that there are a certain class of optimizations
 * that can be applied more easily at the JVM operators level and would become harder or more
 * cumbersome if the JVM bytecode is translated to a sequence of one or more snippets.  Examples
 * of these optimizations include devirtualization (which is easier to recognize if expressed
 * as an {@link InvokeVirtual} call instead of a sequence of {@link ReadHub}, {@link
 * ResolutionSnippet}, {@link ReadInt}, and so on.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public class BytecodeToHCirTranslation extends BytecodeTranslation {

    public BytecodeToHCirTranslation(BlockState blockState, BirToCirMethodTranslation methodTranslation) {
        super(blockState, methodTranslation);
    }

    @Override
    protected void prologue() {
        if (_blockState.birBlock().hasSafepoint()) {
            if (MaxineVM.isPrototyping()) {
                final C_FUNCTION cFunctionAnnotation = _methodTranslation.classMethodActor().getAnnotation(C_FUNCTION.class);
                if (cFunctionAnnotation == null || !cFunctionAnnotation.isInterruptHandler()) {
                    callAndPush(JavaOperator.PROLOGUE);
                }
            } else {
                if (_methodTranslation.classMethodActor().isCFunction()) {
                    callAndPush(JavaOperator.PROLOGUE);
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
                    Value fieldValue;
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
                // do nothing.
            } catch (IllegalAccessException e) {
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
        final CirValue[] arguments = new CirValue[parameterKinds.length + 3];
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
        final CirValue[] arguments = new CirValue[parameterKinds.length + 3];
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
        final CirValue[] arguments = new CirValue[parameterKinds.length + 2];
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
        final CirVariable[] dimensions = new CirVariable[nDimensions];
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

    /**
     * @see Bytecode#CALLNATIVE
     */
    @Override
    protected void callnative(int nativeFunctionDescriptorIndex) {
        final CallNative op = new CallNative(_constantPool, nativeFunctionDescriptorIndex, _methodTranslation.classMethodActor());
        final SignatureDescriptor signatureDescriptor = op.signatureDescriptor();
        final Kind[] parameterKinds = signatureDescriptor.getParameterKinds();
        final CirValue[] arguments = new CirValue[parameterKinds.length + 2];
        for (int i = parameterKinds.length - 1; i >= 0; i--) {
            arguments[i] = _stack.pop();
        }

        assert areArgumentsMatchingSignatureDescriptor(arguments, signatureDescriptor);

        completeInvocation(op, signatureDescriptor.getResultKind(), arguments);
    }


    @Override
    protected void f2i() {
        stackCall(JavaOperator.F2I);
    }

    @Override
    protected void f2l() {
        stackCall(JavaOperator.F2L);
    }

    @Override
    protected void d2i() {
        stackCall(JavaOperator.D2I);
    }

    @Override
    protected void d2l() {
        stackCall(JavaOperator.D2L);
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
        stackCall(JavaOperator.I2B);
    }

    @Override
    protected void i2c() {
        stackCall(JavaOperator.I2C);
    }

    @Override
    protected void i2s() {
        stackCall(JavaOperator.I2S);
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
        stackCall(JavaOperator.I2L);
    }

    @Override
    protected void i2f() {
        stackCall(JavaOperator.I2F);
    }

    @Override
    protected void i2d() {
        stackCall(JavaOperator.I2D);
    }

    @Override
    protected void l2i() {
        stackCall(JavaOperator.L2I);
    }

    @Override
    protected void l2f() {
        stackCall(JavaOperator.L2F);
    }

    @Override
    protected void l2d() {
        stackCall(JavaOperator.L2D);
    }

    @Override
    protected void f2d() {
        stackCall(JavaOperator.F2D);
    }

    @Override
    protected void d2f() {
        stackCall(JavaOperator.D2F);
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
        final CirVariable localVariable = _frame.makeVariable(Kind.INT, index, currentLocation());
        final CirContinuation continuation = new CirContinuation(localVariable);
        final JavaOperator op = JavaOperator.INT_PLUS;
        final CirValue exceptionContinuation = op.mayThrowException() ? getCurrentExceptionContinuation() : CirValue.UNDEFINED;
        _currentCall.setArguments(localVariable, CirConstant.fromInt(addend), continuation, exceptionContinuation);
        _currentCall.setProcedure(op, currentLocation());
        _currentCall = new CirCall();
        continuation.setBody(_currentCall);
    }

    @Override
    protected void athrow() {
        assert isEndOfBlock() : expectedEndOfBlockErrorMessage();
        final CirVariable throwable = pop(Kind.REFERENCE);
        _currentCall.setProcedure(new Throw(), currentLocation());
        _currentCall.setArguments(throwable, CirValue.UNDEFINED, getCurrentExceptionContinuation());
    }

}
