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
/*VCSID=7a8af93c-bb5f-439c-81fa-f6c74d9c758a*/
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



    private static CirClosure lambda(CirVariable arg, CirCall body) {
        if (arg == null) {
            return new CirClosure(body);
        }
        return new CirClosure(body, arg);
    }

    private static CirContinuation lambda_k(CirVariable arg, CirCall body) {
        CirContinuation k;
        if (arg == null) {
            k = new CirContinuation();
        } else {
            k = new CirContinuation(arg);
        }
        k.setBody(body);
        return k;
    }

    private CirCall call(CirValue proc, CirValue ... args) {
        final CirCall call =  new CirCall(proc, args);
        call.setBytecodeLocation(_bytecodeLocation);
        return call;
    }

    private CirCall nullCheckMaybe(CirValue object, CirCall call, CirValue exceptionContinuation, boolean canRaiseNullPointerException) {
        if (!canRaiseNullPointerException) {
            return call;
        }
        if (object.kind() == Kind.REFERENCE) {
            return call(CirSnippet.get(Snippet.CheckNullPointer.SNIPPET), object,
                            lambda_k(null, call), exceptionContinuation);
        }
        assert object.kind() == Kind.WORD;
        return call;
    }

    private CirCall callWithFrameDescriptor(boolean canRaiseException, CirValue proc, CirValue ... args) {
        if (canRaiseException) {
            return callWithFrameDescriptor(proc, args);
        }
        return new CirCall(proc, args);
    }

    private CirCall callWithFrameDescriptor(CirValue proc, CirValue ... args) {
        final CirCall call =  new CirCall();
        call.assign(_call);
        call.setProcedure(proc);
        call.setArguments(args);
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
    public void visit(GetField op) {
        assert _arguments.length == 3;
        final CirValue reference = _arguments[_arguments.length - 3];

        final CirExceptionContinuationParameter ce = createFreshExceptionContinuationParameter();

        final CirValue readExceptionK = op.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        if (op.isResolved()) {
            final CirConstant fieldActor = CirConstant.fromObject(op.fieldActor());
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(op.fieldKind()));
            _call.assign(call(lambda(ce,
                            nullCheckMaybe(reference,
                                           call(fieldRead, reference, fieldActor,
                                                           normalCont(),
                                                           readExceptionK),
                                           readExceptionK,
                                           op.canRaiseNullPointerException())),
                            exceptionCont()));
        } else {
            final ResolveInstanceFieldForReading resolveSnippet = ResolveInstanceFieldForReading.SNIPPET;
            final CirValue guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolveSnippet));
            final CirSnippet resolutionProcedure = CirSnippet.get(resolveSnippet);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(op.fieldKind()));
            _call.assign(call(lambda(ce,
                            call(resolutionProcedure, guard,
                                  lambda_k(fieldActor,
                                           nullCheckMaybe(reference,
                                                          call(fieldRead, reference, fieldActor, normalCont(), readExceptionK),
                                                          readExceptionK,
                                                          op.canRaiseNullPointerException())),
                                 ce)),
                            exceptionCont()));
        }
    }

    @Override
    public void visit(GetStatic op) {
        assert _arguments.length == 2;

        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

        if (op.isClassInitialized()) {
            final CirConstant fieldActor = CirConstant.fromObject(op.fieldActor());
            final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
            final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(op.fieldKind()));

            _call.assign(call(lambda(ce,
                            call(readStaticTuple, fieldActor,
                                            lambda_k(staticTuple,
                                                            call(fieldRead, staticTuple, fieldActor,
                                                                            normalCont(), ce)), ce)),
                                                                            exceptionCont()));
        } else {
            final ResolveStaticFieldForReading resolveSnippet = ResolveStaticFieldForReading.SNIPPET;
            final ResolutionGuard guard = op.constantPool().makeResolutionGuard(op.index(), resolveSnippet);
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(op.fieldKind()));
            final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
            final CirValue guardValue = CirConstant.fromObject(guard);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            call(resolve, guardValue,
                                            lambda_k(fieldActor,
                                                            call(readStaticTuple, fieldActor,
                                                                            lambda_k(staticTuple,
                                                                                            call(fieldRead, staticTuple, fieldActor,
                                                                                                            normalCont(), ce)), ce)), ce)),
                                                                                                            exceptionCont()));
        }
    }

    @Override
    public void visit(PutStatic op) {
        assert _arguments.length == 3;
        final CirValue value = _arguments[0];

        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

        final ResolveStaticFieldForWriting resolveSnippet = ResolveStaticFieldForWriting.SNIPPET;
        final ResolutionGuard guard = op.constantPool().makeResolutionGuard(op.index(), resolveSnippet);
        final CirSnippet resolve = CirSnippet.get(resolveSnippet);
        final CirSnippet fieldWrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(op.fieldKind()));
        final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
        final CirValue guardValue = CirConstant.fromObject(guard);
        final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
        final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
        _call.assign(call(lambda(ce,
                             call(resolve, guardValue,
                                       lambda_k(fieldActor,
                                                  call(readStaticTuple, fieldActor,
                                                              lambda_k(staticTuple,
                                                                     call(fieldWrite, staticTuple, fieldActor, value,
                                                                                 normalCont(), ce)), ce)), ce)),
                                                                                                            exceptionCont()));
    }


    @Override
    public void visit(InvokeVirtual op) {
        assert _arguments.length == op.constantPool().methodAt(op.index()).signature(op.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];

        CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        if (op.isResolved()) {
            final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
            final CirConstant methodActor = CirConstant.fromObject(op.virtualMethodActor());
            final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
            _arguments[_arguments.length - 1] = ce;
            lCirCall = call(selectVirtualMethod, receiver, methodActor,
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
                final ClassMethodRefConstant classMethodRef = op.constantPool().classMethodAt(op.index());
                final VirtualMethodActor declaredMethod = classMethodRef.resolveVirtual(op.constantPool(), op.index());
                final Address entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();
                final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
                final CirMethod targetCirMethod = op.methodTranslation().cirGenerator().createIrMethod(targetMethod.classMethodActor());
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
            final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
            _arguments[_arguments.length - 1] = ce;
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, call(resolve,
                            guard,
                            lambda_k(methodActor,
                              call(selectVirtualMethod,
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
    public void visit(InvokeInterface op) {
        assert _arguments.length == op.constantPool().interfaceMethodAt(op.index()).signature(op.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final CirSnippet selectMethod = CirSnippet.get(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
        final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
        if (op.isResolved()) {
            final CirConstant methodActor = CirConstant.fromObject(op.interfaceMethodActor());
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, call(selectMethod,
                            receiver,
                            methodActor,
                            lambda_k(callEntryPoint,
                              callWithFrameDescriptor(callEntryPoint, _arguments)),
                            ce), ce, true)),
                            exceptionCont());
        } else {
            final ResolutionSnippet resolutionSnippet = ResolveInterfaceMethod.SNIPPET;
            final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            lCirCall = call(lambda(ce,
                            nullCheckMaybe(receiver, call(resolve,
                            guard,
                            lambda_k(methodActor,
                              call(selectMethod,
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
    public void visit(InvokeSpecial op) {
        assert _arguments.length == op.constantPool().classMethodAt(op.index()).signature(op.constantPool()).getNumberOfParameters() + 3;
        final CirValue receiver = _arguments[0];
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final ResolutionSnippet resolutionSnippet = ResolveSpecialMethod.SNIPPET;
        final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolutionSnippet));
        final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
        final CirVariable entryPoint = variableFactory().createTemporary(Kind.WORD);
        lCirCall = call(lambda(ce,
                        nullCheckMaybe(receiver, call(resolve,
                        guard,
                        lambda_k(entryPoint,
                               callWithFrameDescriptor(entryPoint, _arguments)),
                        ce), ce, true)),
                        exceptionCont());
        _call.assign(lCirCall);
    }


    @Override
    public void visit(InvokeStatic op) {
        assert _arguments.length == op.constantPool().classMethodAt(op.index()).signature(op.constantPool()).getNumberOfParameters() + 2;
        final CirCall lCirCall;
        final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();
        _arguments[_arguments.length - 1] = ce;
        final ResolutionSnippet resolutionSnippet = ResolveStaticMethod.SNIPPET;
        final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolutionSnippet));
        final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
        final CirVariable entryPoint = variableFactory().createTemporary(Kind.WORD);
        lCirCall = call(lambda(ce,
                          call(resolve,
                               guard,
                               lambda_k(entryPoint,
                                      callWithFrameDescriptor(entryPoint, _arguments)),
                               ce)),
                         exceptionCont());
        _call.assign(lCirCall);
    }

    @Override
    public void visit(CheckCast op) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet checkCast = CirSnippet.get(Snippet.CheckCast.SNIPPET);
        if (op.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(op.classActor());
            _call.assign(call(checkCast, classActor, objref, normalCont(), exceptionCont()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            call(resolve, guard,
                                            lambda_k(classActor,
                                                            call(checkCast, classActor, objref, normalCont(), ce)), ce)), exceptionCont()));
        }
    }

    @Override
    public void visit(InstanceOf op) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet instanceOf = CirSnippet.get(Snippet.InstanceOf.SNIPPET);
        if (op.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(op.classActor());
            _call.assign(call(instanceOf, classActor, objref, normalCont(), exceptionCont()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(lambda(ce,
                            call(resolve, guard,
                                            lambda_k(classActor,
                                                            call(instanceOf, classActor, objref, normalCont(), ce)), ce)), exceptionCont()));
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
    public void visit(ArrayLoad op) {
        assert _arguments.length == 4;
        final CirValue array = _arguments[0];
        final CirValue index = _arguments[1];
        final CirValue escapek = exceptionCont();
        final CirValue k = normalCont();

        final CirSnippet snippet = CirSnippet.get(ArrayGetSnippet.getSnippet(op.resultKind()));
        final CirVariable ec = variableFactory().createFreshExceptionContinuationParameter();
        final CirValue readExceptionK = op.canRaiseNullPointerException() ? ec : CirValue.UNDEFINED;
        _call.assign(call(lambda(ec,
                            nullCheckMaybe(array,
                              checkArrayIndex(array, index,
                                callWithFrameDescriptor(op.canRaiseNullPointerException(), snippet, array, index, k, readExceptionK),
                                ec),
                              readExceptionK,
                              op.canRaiseNullPointerException())),
                         escapek));
    }


    private CirCall checkArrayIndex(CirValue array, CirValue index, CirCall call, CirVariable ec) {
        final CirSnippet snippet = CirSnippet.get(CheckArrayIndex.SNIPPET);
        return call(snippet, array, index, lambda_k(null, call), ec);
    }



    @Override
    public void visit(New op) {
        final CirValue[] args = _call.arguments();
        assert args.length == 2;


        final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolveClass.SNIPPET));
        final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
        final CirSnippet createTuple = CirSnippet.get(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET);
        final CirVariable classActor = _variableFactory.createTemporary(Kind.REFERENCE);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        call(resolve, guard,
                              lambda_k(classActor,
                                   call(createTuple, classActor, normalCont(), ce)), ce)),
                        exceptionCont()));

    }

    @Override
    public void visit(ArrayStore op) {
        assert _arguments.length == 5;

        final CirValue arrayRef = _arguments[0];
        final CirValue index = _arguments[1];
        final CirValue value = _arguments[2];

        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirSnippet checkArrayIndex = CirSnippet.get(Snippet.CheckArrayIndex.SNIPPET);
        final CirSnippet arraystore = CirSnippet.get(ArraySetSnippet.selectSnippetByKind(op.elementKind()));
        final CirValue npeK = op.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        if (op.elementKind() == Kind.REFERENCE) {
            final CirSnippet arraysetTypeCheck = CirSnippet.get(Snippet.CheckReferenceArrayStore.SNIPPET);
            _call.assign(call(lambda(ce,
                            nullCheckMaybe(arrayRef,
                                            call(checkArrayIndex, arrayRef, index,
                                                 lambda_k(null,
                                                    call(arraysetTypeCheck, arrayRef, value,
                                                          lambda_k(null,
                                                              call(arraystore, arrayRef, index, value, normalCont(), npeK)),
                                                          ce)),
                                                 ce),
                                           npeK,
                                           op.canRaiseNullPointerException())),
                          exceptionCont()));

        } else {
            _call.assign(call(lambda(ce,
                                 nullCheckMaybe(arrayRef,
                                                 call(checkArrayIndex, arrayRef, index,
                                                      lambda_k(null,
                                                         call(arraystore, arrayRef, index, value, normalCont(), npeK)),
                                                      ce),
                                                npeK,
                                                op.canRaiseNullPointerException())),
                             exceptionCont()));
        }
    }


    @Override
    public void visit(PutField op) {
        assert _arguments.length == 4;
        final CirValue objectref = _arguments[0];
        final CirValue value = _arguments[1];
        final CirSnippet fieldwrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(op.fieldKind()));
        final CirConstant guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolutionSnippet.ResolveInstanceFieldForWriting.SNIPPET));
        final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveInstanceFieldForWriting.SNIPPET);
        final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);

        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirValue writeExceptionK = op.canRaiseNullPointerException() ? ce : CirValue.UNDEFINED;
        _call.assign(call(lambda(ce,
                              call(resolve, guard,
                                  lambda_k(fieldActor,
                                       nullCheckMaybe(objectref,
                                            call(fieldwrite, objectref, fieldActor, value, normalCont(), writeExceptionK),
                                            writeExceptionK,
                                            op.canRaiseNullPointerException())),
                                   ce)),
                           exceptionCont()));
    }

    @Override
    public void visit(NewArray op) {
        assert _arguments.length == 3;
        final CirValue count = _arguments[0];
        final CirValue kind = CirConstant.fromObject(op.elementKind());

        if (op.elementKind() != Kind.REFERENCE) {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
            _call.assign(call(createArray, kind, count, normalCont(), exceptionCont()));
        } else {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateReferenceArray.SNIPPET);
            if (op.isResolved()) {
                final CirValue arrayClassActor = CirConstant.fromObject(op.arrayClassActor());
                _call.assign(call(createArray, arrayClassActor, count,
                                    normalCont(), exceptionCont()));
            } else {
                final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
                final CirValue guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
                final CirVariable arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
                final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
                _call.assign(call(lambda(ce,
                                    call(resolve, guard,
                                         lambda_k(arrayClassActor,
                                                 call(createArray, arrayClassActor, count,
                                                           normalCont(), ce)), ce)), exceptionCont()));
            }
        }
    }

    @Override
    public void visit(MultiANewArray op) {
        assert _arguments.length == op.ndimension() + 2;

        final int nDimensions = op.ndimension();
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
        final CirSnippet checkDimension = CirSnippet.get(CheckArrayDimension.SNIPPET);
        final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateMultiReferenceArray.SNIPPET);
        final CirSnippet createDimensionArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
        final CirSnippet setDimensionArray = CirSnippet.get(ArraySetSnippet.SetInt.SNIPPET);
        final CirVariable dimensionArray = variableFactory().createTemporary(Kind.REFERENCE);
        final CirValue arrayClassActor;


        CirCall callBuilder;

        if (op.isResolved()) {
            arrayClassActor = CirConstant.fromObject(op.arrayClassActor());
            callBuilder = call(createArray, arrayClassActor, dimensionArray, normalCont(), ce);
        } else {
            final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
            final CirValue guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
            arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
            callBuilder = call(resolve, guard,
                                  lambda_k((CirVariable) arrayClassActor,
                                                  call(createArray, arrayClassActor, dimensionArray, normalCont(), ce)), ce);
        }

        for (int i = 0; i < nDimensions; i++) {
            callBuilder = call(checkDimension, _arguments[i],
                                lambda_k(null,
                                    call(setDimensionArray, dimensionArray, CirConstant.fromInt(i), _arguments[i],
                                         lambda_k(null,
                                                 callBuilder), ce)), ce);
        }
        callBuilder = call(createDimensionArray, CirConstant.fromObject(Kind.INT), CirConstant.fromInt(nDimensions),
                            lambda_k(dimensionArray, callBuilder), ce);

        callBuilder = call(lambda(ce, callBuilder), exceptionCont());

        _call.assign(callBuilder);
    }


    @Override
    public void visit(ArrayLength op) {
        assert _arguments.length == 3;
        final CirValue receiver = _arguments[0];
        final CirSnippet getArrayLength = CirSnippet.get(ArrayGetSnippet.ReadLength.SNIPPET);
        final CirValue npeK = op.canRaiseNullPointerException() ? exceptionCont() : CirValue.UNDEFINED;
        _call.assign(call(getArrayLength, receiver, normalCont(), npeK));
    }


    @Override
    public void visit(MonitorEnter op) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorEnter = CirSnippet.get(MonitorSnippet.MonitorEnter.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        nullCheckMaybe(objref, call(monitorEnter, objref,
                          normalCont(), ce), ce, true)), exceptionCont()));
    }


    @Override
    public void visit(MonitorExit op) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorExit = CirSnippet.get(MonitorSnippet.MonitorExit.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(lambda(ce,
                        nullCheckMaybe(objref, call(monitorExit, objref,
                          normalCont(), ce), ce, true)), exceptionCont()));
    }


    @Override
    public void visit(Mirror op) {
        assert _arguments.length == 2;

        final CirSnippet mirror = CirSnippet.get(BuiltinsSnippet.GetMirror.SNIPPET);
        if (op.isResolved()) {
            final CirValue classActor = CirConstant.fromObject(op.classActor());
            _call.assign(call(mirror, classActor, normalCont(), exceptionCont()));
        } else {
            final ResolveClass resolveSnippet = ResolveClass.SNIPPET;
            final CirValue guard = CirConstant.fromObject(op.constantPool().makeResolutionGuard(op.index(), resolveSnippet));
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirVariable classActor = _variableFactory.createTemporary(Kind.REFERENCE);
            final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
            _call.assign(call(lambda(ce,
                                  call(resolve, guard,
                                        lambda_k(classActor,
                                            call(mirror, classActor, normalCont(), ce)), ce)), exceptionCont()));
        }
    }

    @Override
    public void visit(CallNative op) {
        assert _arguments.length == op.signatureDescriptor().getNumberOfParameters() + 2;

        final MethodActor classMethodActor = op.classMethodActor();
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
            callBuilder = call(nativeCallEpilogue, localSpace, lambda_k(null, body), ce);
        } else {
            if (localSpace != null) {
                callBuilder = call(nativeCallEpilogueForC, localSpace, lambda_k(null, body), ce);
            } else {
                callBuilder = body;
            }
        }

        cont.setBody(callBuilder);

        _arguments[_arguments.length - 1] = ce;
        callBuilder = callWithFrameDescriptor(callEntryPoint, _arguments);

        if (!callerIsCFunction) {
            callBuilder = call(nativeCallPrologue, lambda_k(localSpace, callBuilder), ce);
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        callBuilder = call(nativeCallPrologueForC, lambda_k(localSpace, callBuilder), ce);
                    }
                } else {
                    callBuilder = call(nativeCallPrologueForC, lambda_k(localSpace, callBuilder), ce);
                }
            }
        }

        callBuilder = call(lambda(ce,
                               call(linkNativeMethod, CirConstant.fromObject(classMethodActor),
                                        lambda_k(callEntryPoint,
                                               callBuilder), ce)), exceptionCont());
        _call.assign(callBuilder);
    }


    @Override
    public void visitDefault(JavaOperator op) {
        if (op instanceof Lowerable) {
            final Lowerable lowerableOp = (Lowerable) op;
            lowerableOp.toLCir(lowerableOp, _call, _compilerScheme);
        }
    }

    @Override
    public void visit(Split op) {
        final CirVariable vOld = (CirVariable) _call.arguments()[0];
        final CirValue cont =  _call.arguments()[1];
        _call.assign(call(cont, vOld));
    }
}

