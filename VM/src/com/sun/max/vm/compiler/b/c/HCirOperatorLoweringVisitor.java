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


import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.operator.CheckCast;
import com.sun.max.vm.compiler.cir.operator.InstanceOf;
import com.sun.max.vm.compiler.cir.optimize.SplitTransformation.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * {@link HCirOperatorLoweringVisitor} defines how each {@link JavaOperator} maps to Cir
 * {@link Builtin} and {@link Snippet} calls.
 *
 * For example: New(t, k, ek) =>
 *   resolve(guard, (lambda (classActor) . createTyple(classActor, k, ek)), ek)
 *
 * avoiding replicating code for ek (by binding it to a temporary variable) as necessary.
 *
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */

public final class HCirOperatorLoweringVisitor extends HCirOperatorDefaultVisitor {
    private final CirCall _call;
    private final BytecodeLocation _bytecodeLocation;
    private final CirVariableFactory _variableFactory;
    private final CirValue _normalCont;
    private final CirValue _exceptionCont;
    private final CirValue[] _arguments;
    private final CompilerScheme _compilerScheme;

    HCirOperatorLoweringVisitor(CirCall call, CirVariableFactory variableFactory, CompilerScheme compilerScheme) {
        _call = call;
        _bytecodeLocation = call.bytecodeLocation();
        _compilerScheme = compilerScheme;
        _variableFactory = variableFactory;
        _arguments = call.arguments();
        _normalCont = arguments()[arguments().length - 2];
        _exceptionCont = arguments()[arguments().length - 1];
    }

    private static CirClosure lambda(CirVariable parameter, CirCall body) {
        if (parameter == null) {
            return new CirClosure(body);
        }
        return new CirClosure(body, parameter);
    }

    private static CirContinuation lambda_k(CirVariable parameter, CirCall body) {
        CirContinuation k;
        if (parameter == null) {
            k = new CirContinuation();
        } else {
            k = new CirContinuation(parameter);
        }
        k.setBody(body);
        return k;
    }

    private CirCall call(CirValue procedure, CirValue ... arguments) {
        final CirCall call =  new CirCall(procedure, arguments);
        call.setBytecodeLocation(_bytecodeLocation);
        return call;
    }

    private CirCall nullCheckMaybe(CirValue object, CirCall call, CirValue exceptionContinuation, boolean canRaiseNullPointerException) {
        if (!canRaiseNullPointerException) {
            return call;
        }
        if (object.kind() == Kind.REFERENCE) {
            return callWithFrameDescriptor(CirSnippet.get(Snippet.CheckNullPointer.SNIPPET), object,
                                           lambda_k(null, call), exceptionContinuation);
        }
        assert object.kind() == Kind.WORD;
        return call;
    }

    private CirCall callWithFrameDescriptor(boolean canRaiseException, CirValue procedure, CirValue ... arguments) {
        if (canRaiseException) {
            return callWithFrameDescriptor(procedure, arguments);
        }
        return new CirCall(procedure, arguments);
    }

    private CirCall callWithFrameDescriptor(CirValue procedure, CirValue ... arguments) {
        final CirCall call =  new CirCall();
        call.assign(_call);
        call.setProcedure(procedure);
        call.setArguments(arguments);
        final CirJavaFrameDescriptor src = _call.javaFrameDescriptor();
        if (src != null) {
            final CirJavaFrameDescriptor dst = new CirJavaFrameDescriptor(src.parent(), src.bytecodeLocation(), src.locals().clone(), src.stackSlots().clone());
            call.setJavaFrameDescriptor(dst);
        }
        return call;
    }

    private CirExceptionContinuationParameter createFreshExceptionContinuationParameter() {
        return variableFactory().createFreshExceptionContinuationParameter();
    }

    @Override
    public void visit(GetField operand) {
        assert _arguments.length == 3;
        final CirValue reference = _arguments[_arguments.length - 3];

        final CirExceptionContinuationParameter ce = createFreshExceptionContinuationParameter();

        final CirValue readExceptionK = operand.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        if (operand.isResolved()) {
            final CirConstant fieldActor = CirConstant.fromObject(operand.fieldActor());
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));
            _call.assign(call(lambda(ce,
                            nullCheckMaybe(reference,
                                            callWithFrameDescriptor(fieldRead, reference, fieldActor,
                                                           normalCont(),
                                                           readExceptionK),
                                           readExceptionK,
                                           operand.canRaiseNullPointerException())),
                            exceptionCont()));
        } else {
            final ResolveInstanceFieldForReading resolveSnippet = ResolveInstanceFieldForReading.SNIPPET;
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet));
            final CirSnippet resolutionProcedure = CirSnippet.get(resolveSnippet);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));
            _call.assign(call(lambda(ce,
                            callWithFrameDescriptor(resolutionProcedure, guard,
                                  lambda_k(fieldActor,
                                           nullCheckMaybe(reference,
                                                          callWithFrameDescriptor(fieldRead, reference, fieldActor, normalCont(), readExceptionK),
                                                          readExceptionK,
                                                          operand.canRaiseNullPointerException())),
                                 ce)),
                            exceptionCont()));
        }
    }

    @Override
    public void visit(GetStatic operand) {
        assert _arguments.length == 2;

        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

        if (operand.isClassInitialized()) {
            final CirConstant fieldActor = CirConstant.fromObject(operand.fieldActor());
            final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
            final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));

            _call.assign(call(lambda(ce,
                            callWithFrameDescriptor(readStaticTuple, fieldActor,
                                            lambda_k(staticTuple,
                                                            callWithFrameDescriptor(fieldRead, staticTuple, fieldActor,
                                                                            normalCont(), ce)), ce)),
                                                                            exceptionCont()));
        } else {
            final ResolveStaticFieldForReading resolveSnippet = ResolveStaticFieldForReading.SNIPPET;
            final ResolutionGuard guard = operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet);
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));
            final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
            final CirValue guardValue = CirConstant.fromObject(guard);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            callWithFrameDescriptor(resolve, guardValue,
                                            lambda_k(fieldActor,
                                                            callWithFrameDescriptor(readStaticTuple, fieldActor,
                                                                            lambda_k(staticTuple,
                                                                                            callWithFrameDescriptor(fieldRead, staticTuple, fieldActor,
                                                                                                            normalCont(), ce)), ce)), ce)),
                                                                                                            exceptionCont()));
        }
    }

    @Override
    public void visit(PutStatic operand) {
        assert _arguments.length == 3;
        final CirValue value = _arguments[0];

        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

        final ResolveStaticFieldForWriting resolveSnippet = ResolveStaticFieldForWriting.SNIPPET;
        final ResolutionGuard guard = operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet);
        final CirSnippet resolve = CirSnippet.get(resolveSnippet);
        final CirSnippet fieldWrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(operand.fieldKind()));
        final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
        final CirValue guardValue = CirConstant.fromObject(guard);
        final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
        final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
        _call.assign(call(lambda(ce,
                             callWithFrameDescriptor(resolve, guardValue,
                                       lambda_k(fieldActor,
                                                  callWithFrameDescriptor(readStaticTuple, fieldActor,
                                                              lambda_k(staticTuple,
                                                                     callWithFrameDescriptor(fieldWrite, staticTuple, fieldActor, value,
                                                                                 normalCont(), ce)), ce)), ce)),
                                                                                                            exceptionCont()));
    }


    @Override
    public void visit(InvokeVirtual operand) {
        assert _arguments.length == operand.constantPool().methodAt(operand.index()).signature(operand.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];

        CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        if (operand.isResolved()) {
            final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
            final CirConstant methodActor = CirConstant.fromObject(operand.virtualMethodActor());
            final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
            _arguments[_arguments.length - 1] = ce;
            lCirCall = callWithFrameDescriptor(selectVirtualMethod, receiver, methodActor,
                            lambda_k(callEntryPoint,
                                callWithFrameDescriptor(callEntryPoint, _arguments)),
                            ce);
            final MethodInstrumentation instrumentation = VMConfiguration.target().compilationScheme().getMethodInstrumentation(_bytecodeLocation.classMethodActor());
            final Hub mostFrequentHub = (instrumentation == null)
                                ? null
                                : instrumentation.getMostFrequentlyUsedHub(_bytecodeLocation.position());

            if (mostFrequentHub != null) {
                final CirValue cont = _arguments[_arguments.length - 2];
                final CirContinuationVariable kvar = variableFactory().createFreshNormalContinuationParameter();
                _arguments[_arguments.length - 2] = kvar;
                final ClassMethodRefConstant classMethodRef = operand.constantPool().classMethodAt(operand.index());
                final VirtualMethodActor declaredMethod = classMethodRef.resolveVirtual(operand.constantPool(), operand.index());
                final Address entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();
                final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
                final CirMethod targetCirMethod = operand.methodTranslation().cirGenerator().createIrMethod(targetMethod.classMethodActor());
                final CirValue cachedHub = new CirConstant(ReferenceValue.from(mostFrequentHub));
                final CirVariable hub = variableFactory().createTemporary(Kind.REFERENCE);
                final CirValue[] argumentsCopy = new CirValue[_arguments.length];
                System.arraycopy(_arguments, 0, argumentsCopy, 0, _arguments.length);
                lCirCall =
                    call(lambda(kvar,
                           call(CirSnippet.get(MethodSelectionSnippet.ReadHub.SNIPPET), receiver,
                             lambda_k(hub,
                                call(CirSwitch.REFERENCE_EQUAL, hub, cachedHub,
                                       lambda_k(null, callWithFrameDescriptor(targetCirMethod, argumentsCopy)),
                                       lambda_k(null, lCirCall))),
                             ce)),
                        cont);
            }

            lCirCall = call(lambda(ce, nullCheckMaybe(receiver, lCirCall, ce, true)), exceptionCont());
        } else {
            final ResolutionSnippet resolutionSnippet = ResolveVirtualMethod.SNIPPET;
            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
            _arguments[_arguments.length - 1] = ce;
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, callWithFrameDescriptor(resolve,
                            guard,
                            lambda_k(methodActor,
                              callWithFrameDescriptor(selectVirtualMethod,
                                receiver,
                                methodActor,
                                lambda_k(callEntryPoint,
                                   callWithFrameDescriptor(callEntryPoint, _arguments)),
                                   ce)),
                            ce), ce, true)),
                            exceptionCont());

        }
        _call.assign(lCirCall);
    }

    @Override
    public void visit(InvokeInterface operand) {
        assert _arguments.length == operand.constantPool().interfaceMethodAt(operand.index()).signature(operand.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final CirSnippet selectMethod = CirSnippet.get(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
        final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
        if (operand.isResolved()) {
            final CirConstant methodActor = CirConstant.fromObject(operand.interfaceMethodActor());
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, callWithFrameDescriptor(selectMethod,
                            receiver,
                            methodActor,
                            lambda_k(callEntryPoint,
                              callWithFrameDescriptor(callEntryPoint, _arguments)),
                            ce), ce, true)),
                            exceptionCont());
        } else {
            final ResolutionSnippet resolutionSnippet = ResolveInterfaceMethod.SNIPPET;
            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, callWithFrameDescriptor(resolve,
                            guard,
                            lambda_k(methodActor,
                              callWithFrameDescriptor(selectMethod,
                                   receiver,
                                   methodActor,
                                   lambda_k(callEntryPoint,
                                      callWithFrameDescriptor(callEntryPoint, _arguments)),
                                   ce)),
                             ce), ce, true)),
                            exceptionCont());
        }
        _call.assign(lCirCall);
    }


    @Override
    public void visit(InvokeSpecial operand) {
        assert _arguments.length == operand.constantPool().classMethodAt(operand.index()).signature(operand.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final ResolutionSnippet resolutionSnippet = ResolveSpecialMethod.SNIPPET;
        final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
        final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
        final CirVariable entryPoint = variableFactory().createTemporary(Kind.WORD);
        lCirCall = call(lambda(ce,
                        nullCheckMaybe(receiver, callWithFrameDescriptor(resolve,
                        guard,
                        lambda_k(entryPoint,
                               callWithFrameDescriptor(entryPoint, _arguments)),
                        ce), ce, true)),
                        exceptionCont());
        _call.assign(lCirCall);
    }


    @Override
    public void visit(InvokeStatic operand) {
        assert _arguments.length == operand.constantPool().classMethodAt(operand.index()).signature(operand.constantPool()).getNumberOfParameters() + 2;
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final ResolutionSnippet resolutionSnippet = ResolveStaticMethod.SNIPPET;
        final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
        final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
        final CirVariable entryPoint = variableFactory().createTemporary(Kind.WORD);
        lCirCall = call(lambda(ce,
                          callWithFrameDescriptor(resolve,
                               guard,
                               lambda_k(entryPoint,
                                      callWithFrameDescriptor(entryPoint, _arguments)),
                               ce)),
                         exceptionCont());
        _call.assign(lCirCall);
    }

    @Override
    public void visit(CheckCast operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet checkCast = CirSnippet.get(Snippet.CheckCast.SNIPPET);
        if (operand.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(checkCast, classActor, objref, normalCont(), exceptionCont()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            callWithFrameDescriptor(resolve, guard,
                                            lambda_k(classActor,
                                                            callWithFrameDescriptor(checkCast, classActor, objref, normalCont(), ce)), ce)), exceptionCont()));
        }
    }

    @Override
    public void visit(InstanceOf operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet instanceOf = CirSnippet.get(Snippet.InstanceOf.SNIPPET);
        if (operand.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(instanceOf, classActor, objref, normalCont(), exceptionCont()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            callWithFrameDescriptor(resolve, guard,
                                            lambda_k(classActor,
                                                            callWithFrameDescriptor(instanceOf, classActor, objref, normalCont(), ce)), ce)), exceptionCont()));
        }
    }

    private CirValue normalCont() {
        return _normalCont;
    }


    private CirValue exceptionCont() {
        return _exceptionCont;
    }


    private CirValue[] arguments() {
        return _arguments;
    }

    private CirVariableFactory variableFactory() {
        return _variableFactory;
    }


    @Override
    public void visit(ArrayLoad operator) {
        assert _arguments.length == 4;
        final CirValue array = _arguments[0];
        final CirValue index = _arguments[1];
        final CirValue escapek = exceptionCont();
        final CirValue k = normalCont();

        final CirSnippet snippet = CirSnippet.get(ArrayGetSnippet.getSnippet(operator.resultKind()));
        final CirVariable ec = variableFactory().createFreshExceptionContinuationParameter();
        final CirValue readExceptionK = operator.canRaiseNullPointerException() ? ec : CirValue.UNDEFINED;
        _call.assign(call(lambda(ec,
                            nullCheckMaybe(array,
                              checkArrayIndex(array, index,
                                callWithFrameDescriptor(operator.canRaiseNullPointerException(), snippet, array, index, k, readExceptionK),
                                ec),
                              readExceptionK,
                              operator.canRaiseNullPointerException())),
                         escapek));
    }


    private CirCall checkArrayIndex(CirValue array, CirValue index, CirCall call, CirVariable ec) {
        return callWithFrameDescriptor(CirSnippet.get(CheckArrayIndex.SNIPPET), array, index, lambda_k(null, call), ec);
    }



    @Override
    public void visit(New operator) {
        final CirValue[] args = _call.arguments();
        assert args.length == 2;


        final CirConstant guard = CirConstant.fromObject(operator.constantPool().makeResolutionGuard(operator.index(), ResolveClass.SNIPPET));
        final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
        final CirSnippet createTuple = CirSnippet.get(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET);
        final CirVariable classActor = _variableFactory.createTemporary(Kind.REFERENCE);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        callWithFrameDescriptor(resolve, guard,
                              lambda_k(classActor,
                                   callWithFrameDescriptor(createTuple, classActor, normalCont(), ce)), ce)),
                        exceptionCont()));

    }

    @Override
    public void visit(ArrayStore operand) {
        assert _arguments.length == 5;

        final CirValue arrayRef = _arguments[0];
        final CirValue index = _arguments[1];
        final CirValue value = _arguments[2];

        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirSnippet arraystore = CirSnippet.get(ArraySetSnippet.selectSnippetByKind(operand.elementKind()));
        final CirValue npeK = operand.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        if (operand.elementKind() == Kind.REFERENCE) {
            final CirSnippet arraysetTypeCheck = CirSnippet.get(Snippet.CheckReferenceArrayStore.SNIPPET);
            _call.assign(call(lambda(ce,
                            nullCheckMaybe(arrayRef,
                                            callWithFrameDescriptor(CirSnippet.get(Snippet.CheckArrayIndex.SNIPPET), arrayRef, index,
                                                 lambda_k(null,
                                                    callWithFrameDescriptor(arraysetTypeCheck, arrayRef, value,
                                                          lambda_k(null,
                                                              callWithFrameDescriptor(arraystore, arrayRef, index, value, normalCont(), npeK)),
                                                          ce)),
                                                 ce),
                                           npeK,
                                           operand.canRaiseNullPointerException())),
                          exceptionCont()));

        } else {
            _call.assign(call(lambda(ce,
                                 nullCheckMaybe(arrayRef,
                                                 callWithFrameDescriptor(CirSnippet.get(Snippet.CheckArrayIndex.SNIPPET), arrayRef, index,
                                                      lambda_k(null,
                                                         callWithFrameDescriptor(arraystore, arrayRef, index, value, normalCont(), npeK)),
                                                      ce),
                                                npeK,
                                                operand.canRaiseNullPointerException())),
                             exceptionCont()));
        }
    }


    @Override
    public void visit(PutField operand) {
        assert _arguments.length == 4;
        final CirValue objectref = _arguments[0];
        final CirValue value = _arguments[1];
        final CirSnippet fieldwrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(operand.fieldKind()));
        final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolutionSnippet.ResolveInstanceFieldForWriting.SNIPPET));
        final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveInstanceFieldForWriting.SNIPPET);
        final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);

        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirValue writeExceptionK = operand.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        _call.assign(call(lambda(ce,
                              callWithFrameDescriptor(resolve, guard,
                                  lambda_k(fieldActor,
                                       nullCheckMaybe(objectref,
                                            callWithFrameDescriptor(fieldwrite, objectref, fieldActor, value, normalCont(), writeExceptionK),
                                            writeExceptionK,
                                            operand.canRaiseNullPointerException())),
                                   ce)),
                           exceptionCont()));
    }

    @Override
    public void visit(NewArray operand) {
        assert _arguments.length == 3;
        final CirValue count = _arguments[0];
        final CirValue kind = CirConstant.fromObject(operand.elementKind());

        if (operand.elementKind() != Kind.REFERENCE) {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
            _call.assign(callWithFrameDescriptor(createArray, kind, count, normalCont(), exceptionCont()));
        } else {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateReferenceArray.SNIPPET);
            if (operand.isResolved()) {
                final CirValue arrayClassActor = CirConstant.fromObject(operand.arrayClassActor());
                _call.assign(callWithFrameDescriptor(createArray, arrayClassActor, count,
                                    normalCont(), exceptionCont()));
            } else {
                final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
                final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
                final CirVariable arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
                final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
                _call.assign(call(lambda(ce,
                                    callWithFrameDescriptor(resolve, guard,
                                         lambda_k(arrayClassActor,
                                                 callWithFrameDescriptor(createArray, arrayClassActor, count,
                                                           normalCont(), ce)), ce)), exceptionCont()));
            }
        }
    }

    @Override
    public void visit(MultiANewArray operand) {
        assert _arguments.length == operand.ndimension() + 2;

        final int nDimensions = operand.ndimension();
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirSnippet checkDimension = CirSnippet.get(CheckArrayDimension.SNIPPET);
        final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateMultiReferenceArray.SNIPPET);
        final CirSnippet createDimensionArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
        final CirSnippet setDimensionArray = CirSnippet.get(ArraySetSnippet.SetInt.SNIPPET);
        final CirVariable dimensionArray = variableFactory().createTemporary(Kind.REFERENCE);
        final CirValue arrayClassActor;


        CirCall callBuilder;

        if (operand.isResolved()) {
            arrayClassActor = CirConstant.fromObject(operand.arrayClassActor());
            callBuilder = callWithFrameDescriptor(createArray, arrayClassActor, dimensionArray, normalCont(), ce);
        } else {
            final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
            arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
            callBuilder = callWithFrameDescriptor(resolve, guard,
                                  lambda_k((CirVariable) arrayClassActor,
                                                  callWithFrameDescriptor(createArray, arrayClassActor, dimensionArray, normalCont(), ce)), ce);
        }

        for (int i = 0; i < nDimensions; i++) {
            callBuilder = callWithFrameDescriptor(checkDimension, _arguments[i],
                                lambda_k(null,
                                    callWithFrameDescriptor(setDimensionArray, dimensionArray, CirConstant.fromInt(i), _arguments[i],
                                         lambda_k(null,
                                                 callBuilder), ce)), ce);
        }
        callBuilder = callWithFrameDescriptor(createDimensionArray, CirConstant.fromObject(Kind.INT), CirConstant.fromInt(nDimensions),
                            lambda_k(dimensionArray, callBuilder), ce);

        callBuilder = call(lambda(ce, callBuilder), exceptionCont());

        _call.assign(callBuilder);
    }


    @Override
    public void visit(ArrayLength operand) {
        assert _arguments.length == 3;
        final CirValue receiver = _arguments[0];
        final CirValue npeK = operand.canRaiseNullPointerException() ? exceptionCont() : CirValue.UNDEFINED;
        _call.assign(callWithFrameDescriptor(CirSnippet.get(ArrayGetSnippet.ReadLength.SNIPPET), receiver, normalCont(), npeK));
    }


    @Override
    public void visit(MonitorEnter operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorEnter = CirSnippet.get(MonitorSnippet.MonitorEnter.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        nullCheckMaybe(objref, callWithFrameDescriptor(monitorEnter, objref,
                          normalCont(), ce), ce, true)), exceptionCont()));
    }


    @Override
    public void visit(MonitorExit operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorExit = CirSnippet.get(MonitorSnippet.MonitorExit.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        nullCheckMaybe(objref, callWithFrameDescriptor(monitorExit, objref,
                          normalCont(), ce), ce, true)), exceptionCont()));
    }


    @Override
    public void visit(Mirror operand) {
        assert _arguments.length == 2;

        final CirSnippet mirror = CirSnippet.get(BuiltinsSnippet.GetMirror.SNIPPET);
        if (operand.isResolved()) {
            final CirValue classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(mirror, classActor, normalCont(), exceptionCont()));
        } else {
            final ResolveClass resolveSnippet = ResolveClass.SNIPPET;
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet));
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirVariable classActor = _variableFactory.createTemporary(Kind.REFERENCE);
            final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
            _call.assign(call(lambda(ce,
                                  callWithFrameDescriptor(resolve, guard,
                                        lambda_k(classActor,
                                            callWithFrameDescriptor(mirror, classActor, normalCont(), ce)), ce)), exceptionCont()));
        }
    }

    @Override
    public void visit(CallNative operand) {
        assert _arguments.length == operand.signatureDescriptor().getNumberOfParameters() + 2;

        final MethodActor classMethodActor = operand.classMethodActor();
        final boolean callerIsCFunction = classMethodActor.isCFunction();

        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        final CirSnippet linkNativeMethod = CirSnippet.get(LinkNativeMethod.SNIPPET);
        final CirSnippet nativeCallPrologue = CirSnippet.get(NativeCallPrologue.SNIPPET);
        final CirSnippet nativeCallPrologueForC = CirSnippet.get(NativeCallPrologueForC.SNIPPET);
        final CirSnippet nativeCallEpilogue = CirSnippet.get(NativeCallEpilogue.SNIPPET);
        final CirSnippet nativeCallEpilogueForC = CirSnippet.get(NativeCallEpilogueForC.SNIPPET);

        final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);

        CirVariable localSpace = null;

        CirCall callBuilder;

        if (!callerIsCFunction) {
            localSpace = variableFactory().createTemporary(Kind.WORD);
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        localSpace = variableFactory().createTemporary(Kind.WORD);
                    }
                } else {
                    localSpace = variableFactory().createTemporary(Kind.WORD);
                }
            }
        }

        assert normalCont() instanceof CirContinuation : normalCont();
        final CirContinuation cont = (CirContinuation) normalCont();
        final CirCall body = cont.body();

        if (!callerIsCFunction) {
            callBuilder = callWithFrameDescriptor(nativeCallEpilogue, localSpace, lambda_k(null, body), ce);
        } else {
            if (localSpace != null) {
                callBuilder = callWithFrameDescriptor(nativeCallEpilogueForC, localSpace, lambda_k(null, body), ce);
            } else {
                callBuilder = body;
            }
        }

        cont.setBody(callBuilder);

        _arguments[_arguments.length - 1] = ce;
        callBuilder = callWithFrameDescriptor(callEntryPoint, _arguments);

        if (!callerIsCFunction) {
            callBuilder = callWithFrameDescriptor(nativeCallPrologue, lambda_k(localSpace, callBuilder), ce);
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        callBuilder = callWithFrameDescriptor(nativeCallPrologueForC, lambda_k(localSpace, callBuilder), ce);
                    }
                } else {
                    callBuilder = callWithFrameDescriptor(nativeCallPrologueForC, lambda_k(localSpace, callBuilder), ce);
                }
            }
        }

        callBuilder = call(lambda(ce,
                               callWithFrameDescriptor(linkNativeMethod, CirConstant.fromObject(classMethodActor),
                                        lambda_k(callEntryPoint,
                                               callBuilder), ce)), exceptionCont());
        _call.assign(callBuilder);
    }


    @Override
    public void visitDefault(JavaOperator operand) {
        if (operand instanceof Lowerable) {
            final Lowerable lowerableOp = (Lowerable) operand;
            lowerableOp.toLCir(lowerableOp, _call, _compilerScheme);
        }
    }

    @Override
    public void visit(Split operand) {
        final CirVariable vOld = (CirVariable) _call.arguments()[0];
        final CirValue cont =  _call.arguments()[1];
        _call.assign(call(cont, vOld));
    }
}

