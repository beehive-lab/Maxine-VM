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
 * {@code HCirOperatorLoweringVisitor} defines how each {@link JavaOperator} maps to CIR {@link Builtin} and
 * {@link Snippet} calls.
 *
 * For example:
 *
 * <pre>
 *  New(t, cc, ce) =&gt; resolve(guard, (lambda (classActor) . createTyple(classActor, cc, ce)), ce)
 * </pre>
 *
 * avoiding replicating code for ce (by binding it to a temporary variable) as necessary.
 *
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public final class HCirOperatorLoweringVisitor extends HCirOperatorDefaultVisitor {
    private final CirCall _call;
    private final BytecodeLocation _bytecodeLocation;
    private final CirVariableFactory _variableFactory;

    /**
     * The current normal continuation.
     */
    private final CirValue _cc;

    /**
     * The current exception continuation.
     */
    private final CirValue _ce;

    private final CirValue[] _arguments;
    private final CompilerScheme _compilerScheme;

    HCirOperatorLoweringVisitor(CirCall call, CirVariableFactory variableFactory, CompilerScheme compilerScheme) {
        _call = call;
        _bytecodeLocation = call.bytecodeLocation();
        _compilerScheme = compilerScheme;
        _variableFactory = variableFactory;
        _arguments = call.arguments();
        _cc = arguments()[arguments().length - 2];
        _ce = arguments()[arguments().length - 1];
    }

    /**
     * Creates a closure for a block of code with exactly one parameter.
     *
     * @param parameter the single parameter
     * @param body the code to be parameterized in a closure
     * @return a closure that parameterizes {@code body} with the single parameter {@code parameter}
     */
    private static CirClosure closure(CirVariable parameter, CirCall body) {
        return new CirClosure(body, parameter);
    }

    /**
     * Creates a continuation for a block of code with no parameters.
     *
     * @param body the code to be wrapped in a continuation
     * @return a continuation that wraps {@code body}
     */
    private static CirContinuation cont(CirCall body) {
        final CirContinuation k = new CirContinuation();
        k.setBody(body);
        return k;
    }

    /**
     * Creates a continuation for a block of code with exactly one parameter.
     *
     * @param parameter the single parameter
     * @param body the code to be parameterized in a continuation
     * @return a continuation that parameterizes {@code body} with the single parameter {@code parameter}
     */
    private static CirContinuation cont(CirVariable parameter, CirCall body) {
        final CirContinuation k = new CirContinuation(parameter);
        k.setBody(body);
        return k;
    }

    /**
     * Creates a {@linkplain CirCall CIR call} node.
     *
     * @param procedure the procedure to be called
     * @param arguments the arguments of the call
     * @return a {@link CirCall} node representing a call to {@code procedure} with arguments {@code arguments}
     */
    private CirCall call(CirValue procedure, CirValue ... arguments) {
        final CirCall call =  new CirCall(procedure, arguments);
        call.setBytecodeLocation(_bytecodeLocation);
        return call;
    }

    /**
     * Inserts a null check before a given CIR call if necessary.
     *
     * @param object the CIR node representing the object that must be null checked
     * @param call the CIR call that is to be modified with a prepended null check
     * @param exceptionContinuation
     * @param canRaiseNullPointerException specifies whether the operation in {@code call} can raise a {@link NullPointerException}
     * @return
     */
    private CirCall nullCheck(CirValue object, CirCall call, CirValue exceptionContinuation, boolean canRaiseNullPointerException) {
        if (!canRaiseNullPointerException) {
            return call;
        }
        if (object.kind() == Kind.REFERENCE) {
            return callWithFrameDescriptor(CirSnippet.get(Snippet.CheckNullPointer.SNIPPET), object,
                                           cont(call), exceptionContinuation);
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
        final CirCall call = new CirCall();
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
            _call.assign(call(closure(ce,
                            nullCheck(reference,
                                            callWithFrameDescriptor(fieldRead, reference, fieldActor,
                                                           cc(),
                                                           readExceptionK),
                                           readExceptionK,
                                           operand.canRaiseNullPointerException())),
                            ce()));
        } else {
            final ResolveInstanceFieldForReading resolveSnippet = ResolveInstanceFieldForReading.SNIPPET;
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet));
            final CirSnippet resolutionProcedure = CirSnippet.get(resolveSnippet);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));
            _call.assign(call(closure(ce,
                            callWithFrameDescriptor(resolutionProcedure, guard,
                                  cont(fieldActor,
                                           nullCheck(reference,
                                                          callWithFrameDescriptor(fieldRead, reference, fieldActor, cc(), readExceptionK),
                                                          readExceptionK,
                                                          operand.canRaiseNullPointerException())),
                                 ce)),
                            ce()));
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

            _call.assign(call(closure(ce,
                            callWithFrameDescriptor(readStaticTuple, fieldActor,
                                            cont(staticTuple,
                                                            callWithFrameDescriptor(fieldRead, staticTuple, fieldActor,
                                                                            cc(), ce)), ce)),
                                                                            ce()));
        } else {
            final ResolveStaticFieldForReading resolveSnippet = ResolveStaticFieldForReading.SNIPPET;
            final ResolutionGuard guard = operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet);
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operand.fieldKind()));
            final CirSnippet readStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
            final CirValue guardValue = CirConstant.fromObject(guard);
            final CirVariable fieldActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(closure(ce,
                            callWithFrameDescriptor(resolve, guardValue,
                                            cont(fieldActor,
                                                            callWithFrameDescriptor(readStaticTuple, fieldActor,
                                                                            cont(staticTuple,
                                                                                            callWithFrameDescriptor(fieldRead, staticTuple, fieldActor,
                                                                                                            cc(), ce)), ce)), ce)),
                                                                                                            ce()));
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
        _call.assign(call(closure(ce,
                             callWithFrameDescriptor(resolve, guardValue,
                                       cont(fieldActor,
                                                  callWithFrameDescriptor(readStaticTuple, fieldActor,
                                                              cont(staticTuple,
                                                                     callWithFrameDescriptor(fieldWrite, staticTuple, fieldActor, value,
                                                                                 cc(), ce)), ce)), ce)),
                                                                                                            ce()));
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
                            cont(callEntryPoint,
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
                    call(closure(kvar,
                           call(CirSnippet.get(MethodSelectionSnippet.ReadHub.SNIPPET), receiver,
                             cont(hub,
                                call(CirSwitch.REFERENCE_EQUAL, hub, cachedHub,
                                       cont(callWithFrameDescriptor(targetCirMethod, argumentsCopy)),
                                       cont(lCirCall))),
                             ce)),
                        cont);
            }

            lCirCall = call(closure(ce, nullCheck(receiver, lCirCall, ce, true)), ce());
        } else {
            final ResolutionSnippet resolutionSnippet = ResolveVirtualMethod.SNIPPET;
            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
            _arguments[_arguments.length - 1] = ce;
            lCirCall = call(closure(ce,
                            nullCheck(receiver, callWithFrameDescriptor(resolve,
                            guard,
                            cont(methodActor,
                              callWithFrameDescriptor(selectVirtualMethod,
                                receiver,
                                methodActor,
                                cont(callEntryPoint,
                                   callWithFrameDescriptor(callEntryPoint, _arguments)),
                                   ce)),
                            ce), ce, true)),
                            ce());

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
            lCirCall = call(closure(ce,
                            nullCheck(receiver, callWithFrameDescriptor(selectMethod,
                            receiver,
                            methodActor,
                            cont(callEntryPoint,
                              callWithFrameDescriptor(callEntryPoint, _arguments)),
                            ce), ce, true)),
                            ce());
        } else {
            final ResolutionSnippet resolutionSnippet = ResolveInterfaceMethod.SNIPPET;
            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolutionSnippet));
            final CirSnippet resolve = CirSnippet.get(resolutionSnippet);
            final CirVariable methodActor = variableFactory().createTemporary(Kind.REFERENCE);
            lCirCall = call(closure(ce,
                            nullCheck(receiver, callWithFrameDescriptor(resolve,
                            guard,
                            cont(methodActor,
                              callWithFrameDescriptor(selectMethod,
                                   receiver,
                                   methodActor,
                                   cont(callEntryPoint,
                                      callWithFrameDescriptor(callEntryPoint, _arguments)),
                                   ce)),
                             ce), ce, true)),
                            ce());
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
        lCirCall = call(closure(ce,
                        nullCheck(receiver, callWithFrameDescriptor(resolve,
                        guard,
                        cont(entryPoint,
                               callWithFrameDescriptor(entryPoint, _arguments)),
                        ce), ce, true)),
                        ce());
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
        lCirCall = call(closure(ce,
                          callWithFrameDescriptor(resolve,
                               guard,
                               cont(entryPoint,
                                      callWithFrameDescriptor(entryPoint, _arguments)),
                               ce)),
                         ce());
        _call.assign(lCirCall);
    }

    @Override
    public void visit(CheckCast operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet checkCast = CirSnippet.get(Snippet.CheckCast.SNIPPET);
        if (operand.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(checkCast, classActor, objref, cc(), ce()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(closure(ce,
                            callWithFrameDescriptor(resolve, guard,
                                            cont(classActor,
                                                            callWithFrameDescriptor(checkCast, classActor, objref, cc(), ce)), ce)), ce()));
        }
    }

    @Override
    public void visit(InstanceOf operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];

        final CirSnippet instanceOf = CirSnippet.get(Snippet.InstanceOf.SNIPPET);
        if (operand.isResolved()) {
            final CirConstant classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(instanceOf, classActor, objref, cc(), ce()));
        } else {
            final CirExceptionContinuationParameter ce = variableFactory().createFreshExceptionContinuationParameter();

            final CirConstant guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolveClass.SNIPPET));
            final CirSnippet resolve = CirSnippet.get(ResolveClass.SNIPPET);
            final CirVariable classActor = variableFactory().createTemporary(Kind.REFERENCE);

            _call.assign(call(closure(ce,
                            callWithFrameDescriptor(resolve, guard,
                                            cont(classActor,
                                                            callWithFrameDescriptor(instanceOf, classActor, objref, cc(), ce)), ce)), ce()));
        }
    }

    /**
     * Gets the normal continuation for the current translation scope.
     */
    private CirValue cc() {
        return _cc;
    }

    /**
     * Gets the exception continuation for the current translation scope.
     */
    private CirValue ce() {
        return _ce;
    }

    /**
     * Gets the arguments for the current translation scope.
     */
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
        final CirValue escapek = ce();
        final CirValue k = cc();

        final CirSnippet snippet = CirSnippet.get(ArrayGetSnippet.getSnippet(operator.resultKind()));
        final CirVariable ec = variableFactory().createFreshExceptionContinuationParameter();
        final CirValue readExceptionK = operator.canRaiseNullPointerException() ? ec : CirValue.UNDEFINED;
        _call.assign(call(closure(ec,
                            nullCheck(array,
                              checkArrayIndex(array, index,
                                callWithFrameDescriptor(operator.canRaiseNullPointerException(), snippet, array, index, k, readExceptionK),
                                ec),
                              readExceptionK,
                              operator.canRaiseNullPointerException())),
                         escapek));
    }

    private CirCall checkArrayIndex(CirValue array, CirValue index, CirCall call, CirVariable ec) {
        return callWithFrameDescriptor(CirSnippet.get(CheckArrayIndex.SNIPPET), array, index, cont(call), ec);
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

        _call.assign(call(closure(ce,
                        callWithFrameDescriptor(resolve, guard,
                              cont(classActor,
                                   callWithFrameDescriptor(createTuple, classActor, cc(), ce)), ce)),
                        ce()));

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
            _call.assign(call(closure(ce,
                            nullCheck(arrayRef,
                                            callWithFrameDescriptor(CirSnippet.get(Snippet.CheckArrayIndex.SNIPPET), arrayRef, index,
                                                 cont(null,
                                                    callWithFrameDescriptor(arraysetTypeCheck, arrayRef, value,
                                                          cont(null,
                                                              callWithFrameDescriptor(arraystore, arrayRef, index, value, cc(), npeK)),
                                                          ce)),
                                                 ce),
                                           npeK,
                                           operand.canRaiseNullPointerException())),
                          ce()));

        } else {
            _call.assign(call(closure(ce,
                                 nullCheck(arrayRef,
                                                 callWithFrameDescriptor(CirSnippet.get(Snippet.CheckArrayIndex.SNIPPET), arrayRef, index,
                                                      cont(null,
                                                         callWithFrameDescriptor(arraystore, arrayRef, index, value, cc(), npeK)),
                                                      ce),
                                                npeK,
                                                operand.canRaiseNullPointerException())),
                             ce()));
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
        _call.assign(call(closure(ce,
                              callWithFrameDescriptor(resolve, guard,
                                  cont(fieldActor,
                                       nullCheck(objectref,
                                            callWithFrameDescriptor(fieldwrite, objectref, fieldActor, value, cc(), writeExceptionK),
                                            writeExceptionK,
                                            operand.canRaiseNullPointerException())),
                                   ce)),
                           ce()));
    }

    @Override
    public void visit(NewArray operand) {
        assert _arguments.length == 3;
        final CirValue count = _arguments[0];
        final CirValue kind = CirConstant.fromObject(operand.elementKind());

        if (operand.elementKind() != Kind.REFERENCE) {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
            _call.assign(callWithFrameDescriptor(createArray, kind, count, cc(), ce()));
        } else {
            final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateReferenceArray.SNIPPET);
            if (operand.isResolved()) {
                final CirValue arrayClassActor = CirConstant.fromObject(operand.arrayClassActor());
                _call.assign(callWithFrameDescriptor(createArray, arrayClassActor, count,
                                    cc(), ce()));
            } else {
                final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
                final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
                final CirVariable arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
                final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
                _call.assign(call(closure(ce,
                                    callWithFrameDescriptor(resolve, guard,
                                         cont(arrayClassActor,
                                                 callWithFrameDescriptor(createArray, arrayClassActor, count,
                                                           cc(), ce)), ce)), ce()));
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
            callBuilder = callWithFrameDescriptor(createArray, arrayClassActor, dimensionArray, cc(), ce);
        } else {
            final CirSnippet resolve = CirSnippet.get(ResolutionSnippet.ResolveArrayClass.SNIPPET);
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), ResolutionSnippet.ResolveArrayClass.SNIPPET));
            arrayClassActor = variableFactory().createTemporary(Kind.REFERENCE);
            callBuilder = callWithFrameDescriptor(resolve, guard,
                                  cont((CirVariable) arrayClassActor,
                                                  callWithFrameDescriptor(createArray, arrayClassActor, dimensionArray, cc(), ce)), ce);
        }

        for (int i = 0; i < nDimensions; i++) {
            callBuilder = callWithFrameDescriptor(checkDimension, _arguments[i],
                                cont(null,
                                    callWithFrameDescriptor(setDimensionArray, dimensionArray, CirConstant.fromInt(i), _arguments[i],
                                         cont(null,
                                                 callBuilder), ce)), ce);
        }
        callBuilder = callWithFrameDescriptor(createDimensionArray, CirConstant.fromObject(Kind.INT), CirConstant.fromInt(nDimensions),
                            cont(dimensionArray, callBuilder), ce);

        callBuilder = call(closure(ce, callBuilder), ce());

        _call.assign(callBuilder);
    }


    @Override
    public void visit(ArrayLength operand) {
        assert _arguments.length == 3;
        final CirValue receiver = _arguments[0];
        final CirValue npeK = operand.canRaiseNullPointerException() ? ce() : CirValue.UNDEFINED;
        _call.assign(callWithFrameDescriptor(CirSnippet.get(ArrayGetSnippet.ReadLength.SNIPPET), receiver, cc(), npeK));
    }


    @Override
    public void visit(MonitorEnter operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorEnter = CirSnippet.get(MonitorSnippet.MonitorEnter.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(closure(ce,
                        nullCheck(objref, callWithFrameDescriptor(monitorEnter, objref,
                          cc(), ce), ce, true)), ce()));
    }


    @Override
    public void visit(MonitorExit operand) {
        assert _arguments.length == 3;
        final CirValue objref = _arguments[0];
        final CirSnippet monitorExit = CirSnippet.get(MonitorSnippet.MonitorExit.SNIPPET);
        final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();

        _call.assign(call(closure(ce,
                        nullCheck(objref, callWithFrameDescriptor(monitorExit, objref,
                          cc(), ce), ce, true)), ce()));
    }


    @Override
    public void visit(Mirror operand) {
        assert _arguments.length == 2;

        final CirSnippet mirror = CirSnippet.get(BuiltinsSnippet.GetMirror.SNIPPET);
        if (operand.isResolved()) {
            final CirValue classActor = CirConstant.fromObject(operand.classActor());
            _call.assign(callWithFrameDescriptor(mirror, classActor, cc(), ce()));
        } else {
            final ResolveClass resolveSnippet = ResolveClass.SNIPPET;
            final CirValue guard = CirConstant.fromObject(operand.constantPool().makeResolutionGuard(operand.index(), resolveSnippet));
            final CirSnippet resolve = CirSnippet.get(resolveSnippet);
            final CirVariable classActor = _variableFactory.createTemporary(Kind.REFERENCE);
            final CirExceptionContinuationParameter ce = _variableFactory.createFreshExceptionContinuationParameter();
            _call.assign(call(closure(ce,
                                  callWithFrameDescriptor(resolve, guard,
                                        cont(classActor,
                                            callWithFrameDescriptor(mirror, classActor, cc(), ce)), ce)), ce()));
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

        assert cc() instanceof CirContinuation : cc();
        final CirContinuation cont = (CirContinuation) cc();
        final CirCall body = cont.body();

        if (!callerIsCFunction) {
            callBuilder = callWithFrameDescriptor(nativeCallEpilogue, localSpace, cont(body), ce);
        } else {
            if (localSpace != null) {
                callBuilder = callWithFrameDescriptor(nativeCallEpilogueForC, localSpace, cont(body), ce);
            } else {
                callBuilder = body;
            }
        }

        cont.setBody(callBuilder);

        _arguments[_arguments.length - 1] = ce;
        callBuilder = callWithFrameDescriptor(callEntryPoint, _arguments);

        if (!callerIsCFunction) {
            callBuilder = callWithFrameDescriptor(nativeCallPrologue, cont(localSpace, callBuilder), ce);
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        callBuilder = callWithFrameDescriptor(nativeCallPrologueForC, cont(localSpace, callBuilder), ce);
                    }
                } else {
                    callBuilder = callWithFrameDescriptor(nativeCallPrologueForC, cont(localSpace, callBuilder), ce);
                }
            }
        }

        callBuilder = call(closure(ce,
                               callWithFrameDescriptor(linkNativeMethod, CirConstant.fromObject(classMethodActor),
                                        cont(callEntryPoint,
                                               callBuilder), ce)), ce());
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

