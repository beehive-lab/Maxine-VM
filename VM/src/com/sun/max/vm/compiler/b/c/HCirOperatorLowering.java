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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.operator.CheckCast;
import com.sun.max.vm.compiler.cir.operator.InstanceOf;
import com.sun.max.vm.compiler.cir.operator.JavaOperator.*;
import com.sun.max.vm.compiler.cir.optimize.SplitTransformation.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.snippet.NonFoldableSnippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * {@code HCirOperatorLowering} defines how each {@link JavaOperator} maps to CIR {@link Builtin} and
 * {@link Snippet} calls.
 *
 * For example:
 *
 * <pre>
 *  New(t, cc, ce) =&gt; resolve(guard, (closure[classActor] . createTuple(classActor, cc, ce)), ce)
 * </pre>
 *
 * avoiding replicating code for ce (by binding it to a temporary variable) as necessary.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public final class HCirOperatorLowering extends HCirOperatorDefaultVisitor {
    private final JavaOperator _operator;
    private final CirCall _call;
    private final CirVariableFactory _variableFactory;
    private final CirValue _originalCC;
    private final CirValue _originalCE;
    private final CirVariable _ce;

    private final CirValue[] _arguments;
    private final CompilerScheme _compilerScheme;

    HCirOperatorLowering(JavaOperator operator, CirCall call, CirVariableFactory variableFactory, CompilerScheme compilerScheme) {
        _operator = operator;
        _call = call;
        _compilerScheme = compilerScheme;
        _variableFactory = variableFactory;
        _arguments = call.arguments();
        _originalCC = _arguments[_arguments.length - 2];
        _originalCE = _arguments[_arguments.length - 1];
        final CirValue ce = _originalCE;
        if (ce instanceof CirVariable) {
            _ce = (CirVariable) ce;
        } else {
            _ce = variableFactory.createFreshExceptionContinuationParameter();
        }
    }

    /**
     * Creates a closure for a block of code with exactly one parameter.
     *
     * @param body the code to be parameterized in a closure
     * @param parameter the single parameter
     * @return a closure that parameterizes {@code body} with the single parameter {@code parameter}
     */
    static CirClosure closure(CirCall body, CirVariable parameter) {
        return new CirClosure(body, parameter);
    }

    /**
     * Creates a continuation for a block of code with no parameters.
     *
     * @param body the code to be wrapped in a continuation
     * @return a continuation that wraps {@code body}
     */
    static CirContinuation cont(CirCall body) {
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
    CirContinuation cont(CirVariable parameter, CirCall body) {
        final CirContinuation k = new CirContinuation(parameter);
        k.setBody(body);
        return k;
    }

    static boolean validateArguments(CirCall call) {
        final CirValue[] arguments = call.arguments();
        if (arguments.length == 0) {
            return true;
        }
        assert arguments.length >= 2;
        for (int i = 0; i < arguments.length - 2; ++i) {
            assert arguments[i] != null;
        }

        final CirValue cc = arguments[arguments.length - 2];
        final CirValue ce = arguments[arguments.length - 1];

        assert cc instanceof CirNormalContinuationParameter || cc instanceof CirContinuation : call.traceToString(false);
        assert ce == CirValue.UNDEFINED || ce instanceof CirExceptionContinuationParameter || ce instanceof CirContinuation : call.traceToString(false);
        return true;
    }

    /**
     * Creates a {@linkplain CirCall CIR call} node.
     *
     * @param procedure the procedure to be called
     * @param arguments the arguments of the call
     * @return a {@link CirCall} node representing a call to {@code procedure} with arguments {@code arguments}
     */
    CirCall call(CirValue procedure, CirValue... arguments) {
        final CirCall call = new CirCall(procedure, arguments.length > 0 ? arguments : CirCall.NO_ARGUMENTS);
        assert validateArguments(call);
        if (procedure instanceof Stoppable) {
            final Stoppable stoppable = (Stoppable) procedure;
            if (canStop(stoppable)) {
                call.setJavaFrameDescriptor(_call.javaFrameDescriptor().copy());
            }
        } else {
            call.setJavaFrameDescriptor(_call.javaFrameDescriptor().copy());
        }
        return call;
    }

    /**
     * Inserts a null check if necessary.
     *
     * @param object the CIR node representing the value to be null checked
     * @param cc where execution continues if the check succeeds
     */
    CirCall nullCheck(CirValue object, CirCall cc) {
        if (!checksNullPointer(_operator)) {
            return cc;
        }
        if (object.kind() == Kind.REFERENCE) {
            final CirSnippet checkNullPointer = CirSnippet.get(Snippet.CheckNullPointer.SNIPPET);
            return call(
                       checkNullPointer,
                       object,
                       cont(cc),
                       ce());
        }
        assert object.kind() == Kind.WORD;
        return cc;
    }

    /**
     * Inserts an array bounds index check if necessary.
     *
     * @param array the CIR node representing the array
     * @param index the CIR node representing the index to be checked
     * @param cc where execution continues if the check succeeds
     * @return
     */
    CirCall indexCheck(CirValue array, CirValue index, CirCall cc) {
        if (!checksArrayIndexAgainstBounds(_operator)) {
            return cc;
        }
        final CirSnippet checkArrayIndex = CirSnippet.get(CheckArrayIndex.SNIPPET);
        return call(
                   checkArrayIndex,
                   array,
                   index,
                   cont(cc),
                   ce());
    }

    void set(CirCall result) {
        CirCall call = result;
        assert _arguments[_arguments.length - 2] == _originalCC : _arguments[_arguments.length - 2] + " != " + _originalCC;
        assert _arguments[_arguments.length - 1] == _originalCE;
        if (ce() != _originalCE) {
            _arguments[_arguments.length - 1] = ce();
            call = new CirCall(closure(call, ce()), _originalCE);
        }
        _call.assign(call);
    }

    abstract class Resolvable {
        final ResolutionSnippet _snippet;

        public Resolvable(ResolutionSnippet resolutionSnippet) {
            _snippet = resolutionSnippet;
        }

        abstract CirCall makeCall(CirValue resolvedAndInitialized);
    }

    CirCall callWithResolutionAndClassInitialization(Resolvable resolvable, JavaResolvableOperator<? extends Actor> operator) {

        final boolean isFieldOrMethod = operator.constant() instanceof MemberRefConstant;

        if (operator.isResolved()) {
            final CirConstant actor = CirConstant.fromObject(operator.actor());

            if (operator.requiresClassInitialization() && !operator.isClassInitialized()) {
                final CirSnippet makeInitialized = CirSnippet.get(isFieldOrMethod ? MakeHolderInitialized.SNIPPET : MakeClassInitialized.SNIPPET);
                return call(
                    makeInitialized,
                    actor,
                    cont(
                        resolvable.makeCall(actor)),
                    ce());
            }

            return resolvable.makeCall(actor);
        }

        final CirVariable actor = variableFactory().createTemporary(resolvable._snippet.resultKind());

        final CirCall innerCall;
        if (operator.requiresClassInitialization() && !operator.isClassInitialized()) {
            final CirSnippet makeInitialized = CirSnippet.get(isFieldOrMethod ? MakeHolderInitialized.SNIPPET : MakeClassInitialized.SNIPPET);
            innerCall = call(
                makeInitialized,
                actor,
                cont(
                    resolvable.makeCall(actor)),
                ce());
        } else {
            innerCall = resolvable.makeCall(actor);
        }

        final CirConstant guard = CirConstant.fromObject(operator.constantPool().makeResolutionGuard(operator.index(), resolvable._snippet));
        final CirSnippet resolve = CirSnippet.get(resolvable._snippet);
        return call(
            resolve,
            guard,
            cont(
                actor,
                innerCall),
            ce());
    }

    @Override
    public void visit(final GetField operator) {
        final Resolvable resolvable = new Resolvable(ResolveInstanceFieldForReading.SNIPPET) {
            @Override
            CirCall makeCall(CirValue fieldActor) {
                final CirValue reference = _arguments[_arguments.length - 3];
                final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operator.fieldKind()));
                return nullCheck(
                    reference,
                    call(
                        fieldRead,
                        reference,
                        fieldActor,
                        cc(),
                        ce()));
            }

        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(final GetStatic operator) {
        final Resolvable resolvable = new Resolvable(ResolveStaticFieldForReading.SNIPPET) {
            @Override
            CirCall makeCall(CirValue fieldActor) {
                final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operator.resultKind()));
                final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
                return call(
                    CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET),
                    fieldActor,
                    cont(
                        staticTuple,
                        call(
                            fieldRead,
                            staticTuple,
                            fieldActor,
                            cc(),
                            ce())),
                    ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(final PutStatic operator) {
        final Resolvable resolvable = new Resolvable(ResolveStaticFieldForWriting.SNIPPET) {
            @Override
            CirCall makeCall(CirValue fieldActor) {
                final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
                final CirSnippet fieldWrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(operator.fieldKind()));
                final CirValue value = _arguments[0];
                final CirSnippet getStaticTuple = CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET);
                return call(
                    getStaticTuple,
                    fieldActor,
                    cont(
                        staticTuple,
                        call(
                            fieldWrite,
                            staticTuple,
                            fieldActor,
                            value,
                            cc(),
                            ce())),
                    ce());

            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(final InvokeVirtual operator) {
        final CirValue receiver = _arguments[0];

        final Resolvable resolvable = new Resolvable(ResolveVirtualMethod.SNIPPET) {
            @Override
            CirCall makeCall(CirValue methodActor) {
                final CirSnippet selectVirtualMethod = CirSnippet.get(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
                final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
                CirCall call =
                    call(
                        selectVirtualMethod,
                        receiver,
                        methodActor,
                        cont(
                            callEntryPoint,
                            call(
                                callEntryPoint,
                                _arguments)),
                        ce());
                final MethodInstrumentation instrumentation = VMConfiguration.target().compilationScheme().getMethodInstrumentation(_call.javaFrameDescriptor().classMethodActor());
                final Hub mostFrequentHub = (instrumentation == null)
                                    ? null
                                    : instrumentation.getMostFrequentlyUsedHub(_call.javaFrameDescriptor().bytecodePosition());

                if (mostFrequentHub != null) {
                    final CirValue cont = _arguments[_arguments.length - 2];
                    final CirContinuationVariable kvar = variableFactory().createFreshNormalContinuationParameter();
                    _arguments[_arguments.length - 2] = kvar;
                    final ClassMethodRefConstant classMethodRef = operator.constantPool().classMethodAt(operator.index());
                    final VirtualMethodActor declaredMethod = classMethodRef.resolveVirtual(operator.constantPool(), operator.index());
                    final Address entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();
                    final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
                    final CirMethod targetCirMethod = operator.methodTranslation().cirGenerator().createIrMethod(targetMethod.classMethodActor());
                    final CirValue cachedHub = new CirConstant(ReferenceValue.from(mostFrequentHub));
                    final CirVariable hub = variableFactory().createTemporary(Kind.REFERENCE);
                    final CirValue[] argumentsCopy = CirCall.newArguments(_arguments.length);
                    System.arraycopy(_arguments, 0, argumentsCopy, 0, _arguments.length);
                    final CirSnippet readHub = CirSnippet.get(MethodSelectionSnippet.ReadHub.SNIPPET);
                    call = call(
                               closure(
                                   call(
                                       readHub,
                                       receiver,
                                       cont(
                                           hub,
                                           call(
                                               CirSwitch.REFERENCE_EQUAL,
                                               hub,
                                               cachedHub,
                                               cont(
                                                   call(
                                                       targetCirMethod,
                                                       argumentsCopy)),
                                               cont(
                                                   call))),
                                       ce()),
                                   kvar),
                               cont,
                               ce());
                }

                call = nullCheck(
                           receiver,
                           call);
                return call;
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(InvokeInterface operator) {
        final Resolvable resolvable = new Resolvable(ResolveInterfaceMethod.SNIPPET) {
            @Override
            CirCall makeCall(CirValue interfaceMethodActor) {
                final CirSnippet selectMethod = CirSnippet.get(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
                final CirValue receiver = _arguments[0];
                final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
                return call(
                    selectMethod,
                    receiver,
                    interfaceMethodActor,
                    cont(
                        callEntryPoint,
                        call(
                            callEntryPoint,
                            _arguments)),
                    ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(InvokeSpecial operator) {
        final Resolvable resolvable = new Resolvable(ResolveSpecialMethod.SNIPPET) {
            @Override
            CirCall makeCall(CirValue virtualMethodActor) {
                final CirSnippet makeEntrypoint = CirSnippet.get(MakeEntrypoint.SNIPPET);
                final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
                return call(
                    makeEntrypoint,
                    virtualMethodActor,
                    cont(
                        callEntryPoint,
                        call(
                            callEntryPoint,
                            _arguments)),
                    ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(InvokeStatic operator) {
        final Resolvable resolvable = new Resolvable(ResolveStaticMethod.SNIPPET) {
            @Override
            CirCall makeCall(CirValue staticMethodActor) {
                final CirSnippet makeEntrypoint = CirSnippet.get(MakeEntrypoint.SNIPPET);
                final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
                return call(
                    makeEntrypoint,
                    staticMethodActor,
                    cont(
                        callEntryPoint,
                        call(
                            callEntryPoint,
                            _arguments)),
                    ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(CheckCast operator) {
        if (!checksTypeCast(operator)) {
            final CirCall call = call(cc(), CirCall.NO_ARGUMENTS);
            set(call);
            return;
        }
        final Resolvable resolvable = new Resolvable(ResolveClass.SNIPPET) {
            @Override
            CirCall makeCall(CirValue classActor) {
                final CirValue object = _arguments[0];
                final CirSnippet checkCast = CirSnippet.get(Snippet.CheckCast.SNIPPET);
                return call(
                                checkCast,
                                classActor,
                                object,
                                cc(),
                                ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(InstanceOf operator) {
        final CirValue objref = _arguments[0];
        final Resolvable resolvable = new Resolvable(ResolveClass.SNIPPET) {
            @Override
            CirCall makeCall(CirValue resolved) {
                return call(
                           CirSnippet.get(Snippet.InstanceOf.SNIPPET),
                           resolved,
                           objref,
                           cc(),
                           ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    /**
     * Gets the original normal continuation for the HCIR call being lowered.
     */
    CirValue cc() {
        return _originalCC;
    }

    /**
     * Gets the exception continuation for the current translation scope.
     */
    CirVariable ce() {
        return _ce;
    }

    CirVariableFactory variableFactory() {
        return _variableFactory;
    }

    @Override
    public void visit(ArrayLoad operator) {
        final CirValue array = _arguments[0];
        final CirValue index = _arguments[1];

        final CirSnippet arrayLoad = CirSnippet.get(ArrayGetSnippet.getSnippet(operator.resultKind()));

        set(nullCheck(
            array,
            indexCheck(
                array,
                index,
                call(
                    arrayLoad,
                    array,
                    index,
                    cc(),
                    ce()))));
    }

    @Override
    public void visit(New operator) {
        final Resolvable resolvable = new Resolvable(ResolveClassForNew.SNIPPET) {
            @Override
            CirCall makeCall(CirValue classActor) {
                final CirSnippet createTupleOrHybrid = CirSnippet.get(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET);
                return call(
                           createTupleOrHybrid,
                           classActor,
                           cc(),
                           ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(ArrayStore operator) {
        final CirValue array = _arguments[0];
        final CirValue index = _arguments[1];
        final CirValue value = _arguments[2];

        final CirSnippet arrayStore = CirSnippet.get(ArraySetSnippet.selectSnippetByKind(operator.elementKind()));

        if (operator.elementKind() == Kind.REFERENCE) {
            final CirSnippet checkReferenceArrayStore = CirSnippet.get(Snippet.CheckReferenceArrayStore.SNIPPET);
            set(nullCheck(
                    array,
                    indexCheck(
                        array,
                        index,
                        call(
                            checkReferenceArrayStore,
                            array,
                            value,
                            cont(
                                call(
                                    arrayStore,
                                    array,
                                    index,
                                    value,
                                    cc(),
                                    ce())),
                            ce()))));
        } else {
            set(nullCheck(
                    array,
                    indexCheck(
                        array,
                        index,
                        call(
                            arrayStore,
                            array,
                            index,
                            value,
                            cc(),
                            ce()))));
        }
    }


    @Override
    public void visit(final PutField operator) {
        final Resolvable resolvable = new Resolvable(ResolveInstanceFieldForWriting.SNIPPET) {
            @Override
            CirCall makeCall(CirValue fieldActor) {
                final CirValue object = _arguments[0];
                final CirValue value = _arguments[1];
                final CirSnippet fieldWrite = CirSnippet.get(FieldWriteSnippet.selectSnippetByKind(operator.fieldKind()));
                return nullCheck(
                           object,
                           call(
                               fieldWrite,
                               object,
                               fieldActor,
                               value,
                               cc(),
                               ce()));
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(NewArray operator) {
        final CirValue count = _arguments[0];
        if (operator.primitiveElementKind() != null) {
            final CirValue kind = CirConstant.fromObject(operator.primitiveElementKind());
            final CirSnippet createPrimitiveArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
            set(call(
                    createPrimitiveArray,
                    kind,
                    count,
                    cc(),
                    ce()));
        } else {
            final Resolvable resolvable = new Resolvable(ResolveArrayClass.SNIPPET) {
                @Override
                CirCall makeCall(CirValue arrayClassActor) {
                    final CirSnippet createReferenceArray = CirSnippet.get(CreateReferenceArray.SNIPPET);
                    return call(
                               createReferenceArray,
                               arrayClassActor,
                               count,
                               cc(),
                               ce());
                }
            };
            set(callWithResolutionAndClassInitialization(resolvable, operator));
        }
    }

    @Override
    public void visit(MultiANewArray operator) {
        final int nDimensions = operator.ndimension();
        final CirSnippet checkArrayDimension = CirSnippet.get(CheckArrayDimension.SNIPPET);
        final CirSnippet createArray = CirSnippet.get(NonFoldableSnippet.CreateMultiReferenceArray.SNIPPET);
        final CirSnippet createDimensionArray = CirSnippet.get(NonFoldableSnippet.CreatePrimitiveArray.SNIPPET);
        final CirSnippet setDimensionInArray = CirSnippet.get(ArraySetSnippet.SetInt.SNIPPET);
        final CirVariable dimensionArray = variableFactory().createTemporary(Kind.REFERENCE);



        final Resolvable resolvable = new Resolvable(ResolveClass.SNIPPET) {
            @Override
            CirCall makeCall(CirValue classActor) {
                return call(
                           createArray,
                           classActor,
                           dimensionArray,
                           cc(),
                           ce());
            }
        };
        CirCall call = callWithResolutionAndClassInitialization(resolvable, operator);

        for (int i = 0; i < nDimensions; i++) {
            call = call(
                       checkArrayDimension, _arguments[i],
                       cont(
                           call(
                               setDimensionInArray,
                               dimensionArray,
                               CirConstant.fromInt(i),
                               _arguments[i],
                               cont(
                                   call),
                               ce())),
                       ce());
        }
        call = call(
                   createDimensionArray,
                   CirConstant.fromObject(Kind.INT),
                   CirConstant.fromInt(nDimensions),
                   cont(
                       dimensionArray,
                       call),
                   ce());

        set(call);
    }

    @Override
    public void visit(ArrayLength operator) {
        final CirValue array = _arguments[0];
        final CirSnippet readLength = CirSnippet.get(ArrayGetSnippet.ReadLength.SNIPPET);
        set(nullCheck(
            array,
            call(
                readLength,
                array,
                cc(),
                ce())));
    }


    @Override
    public void visit(MonitorEnter operator) {
        final CirValue object = _arguments[0];
        final CirSnippet monitorEnter = CirSnippet.get(MonitorSnippet.MonitorEnter.SNIPPET);
        set(nullCheck(
                object,
                call(
                    monitorEnter,
                    object,
                    cc(),
                    ce())));
    }

    @Override
    public void visit(MonitorExit operator) {
        final CirValue object = _arguments[0];
        final CirSnippet monitorExit = CirSnippet.get(MonitorSnippet.MonitorExit.SNIPPET);
        set(nullCheck(
                object,
                call(
                    monitorExit,
                    object,
                    cc(),
                    ce())));
    }

    @Override
    public void visit(Mirror operator) {
        final Resolvable resolvable = new Resolvable(ResolveClass.SNIPPET) {
            @Override
            CirCall makeCall(CirValue classActor) {
                final CirSnippet getMirror = CirSnippet.get(BuiltinsSnippet.GetMirror.SNIPPET);
                return call(
                    getMirror,
                    classActor,
                    cc(),
                    ce());
            }
        };
        set(callWithResolutionAndClassInitialization(resolvable, operator));
    }

    @Override
    public void visit(CallNative operator) {
        final MethodActor classMethodActor = operator.classMethodActor();
        final boolean isCFunction = classMethodActor.isCFunction();

        final CirSnippet linkNativeMethod = CirSnippet.get(LinkNativeMethod.SNIPPET);
        final CirSnippet nativeCallPrologue = CirSnippet.get(NativeCallPrologue.SNIPPET);
        final CirSnippet nativeCallPrologueForC = CirSnippet.get(NativeCallPrologueForC.SNIPPET);
        final CirSnippet nativeCallEpilogue = CirSnippet.get(NativeCallEpilogue.SNIPPET);
        final CirSnippet nativeCallEpilogueForC = CirSnippet.get(NativeCallEpilogueForC.SNIPPET);

        final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);

        final CirVariable localSpace;

        CirCall call;

        if (!isCFunction) {
            localSpace = variableFactory().createTemporary(Kind.WORD);
        } else {
            if (MaxineVM.isPrototyping()) {
                if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                    localSpace = variableFactory().createTemporary(Kind.WORD);
                } else {
                    localSpace = null;
                }
            } else {
                localSpace = variableFactory().createTemporary(Kind.WORD);
            }
        }

        assert cc() instanceof CirContinuation;
        final CirContinuation cc = (CirContinuation) cc();
        final CirCall ccBody = cc.body();

        if (!isCFunction) {
            call = call(nativeCallEpilogue, localSpace, cont(ccBody), ce());
        } else {
            if (localSpace != null) {
                call = call(nativeCallEpilogueForC, localSpace, cont(ccBody), ce());
            } else {
                call = ccBody;
            }
        }

        cc.setBody(call);
        call = call(callEntryPoint, _arguments);

        if (!isCFunction) {
            call = call(nativeCallPrologue, cont(localSpace, call), ce());
        } else {
            if (classMethodActor.isCFunction()) {
                if (MaxineVM.isPrototyping()) {
                    if (!classMethodActor.getAnnotation(C_FUNCTION.class).isInterruptHandler()) {
                        call = call(nativeCallPrologueForC, cont(localSpace, call), ce());
                    }
                } else {
                    call = call(nativeCallPrologueForC, cont(localSpace, call), ce());
                }
            }
        }

        set(call(
                linkNativeMethod,
                CirConstant.fromObject(classMethodActor),
                cont(
                    callEntryPoint,
                    call),
                ce()));
    }


    @Override
    public void visitDefault(JavaOperator operator) {
        if (operator instanceof Lowerable) {
            final Lowerable lowerableOp = (Lowerable) operator;
            lowerableOp.toLCir(lowerableOp, _call, _compilerScheme);
        }
    }

    @Override
    public void visit(Split operator) {
        final CirVariable vOld = (CirVariable) _call.arguments()[0];
        final CirValue cont =  _call.arguments()[1];
        set(call(cont, vOld));
    }
}

