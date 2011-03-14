/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.compiler.Stoppable.Static.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.operator.CheckCast;
import com.sun.max.vm.cps.cir.operator.InstanceOf;
import com.sun.max.vm.cps.cir.operator.JavaOperator.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.profile.*;
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
public final class HCirOperatorLowering extends HCirOperatorVisitor {
    private final JavaOperator operator;
    private final CirCall call;
    private final BirToCirMethodTranslation methodTranslation;
    private final CirValue originalCC;
    private final CirValue originalCE;
    private final CirVariable ce;

    private final CirValue[] arguments;

    HCirOperatorLowering(JavaOperator operator, CirCall call, BirToCirMethodTranslation methodTranslation) {
        this.operator = operator;
        this.call = call;
        this.methodTranslation = methodTranslation;
        this.arguments = call.arguments();
        this.originalCC = arguments[arguments.length - 2];
        this.originalCE = arguments[arguments.length - 1];
        final CirValue ce = originalCE;
        if (ce instanceof CirVariable) {
            this.ce = (CirVariable) ce;
        } else {
            this.ce = variableFactory().createFreshExceptionContinuationParameter();
        }
    }

    /**
     * Creates a closure for a block of code.
     *
     * @param body the code to be parameterized in a closure
     * @param parameters the parameters of the closure
     * @return a closure that parameterizes {@code body} with the parameter {@code parameters}
     */
    static CirClosure closure(CirCall body, CirVariable... parameters) {
        return new CirClosure(body, parameters);
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
        assert procedure instanceof CirClosure || validateArguments(call);
        if (procedure instanceof Stoppable) {
            final Stoppable stoppable = (Stoppable) procedure;
            if (canStop(stoppable)) {
                call.setJavaFrameDescriptor(this.call.javaFrameDescriptor().copy());
            }
        } else {
            call.setJavaFrameDescriptor(this.call.javaFrameDescriptor().copy());
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
        if (!checksNullPointer(operator)) {
            return cc;
        }
        if (object.kind().isReference) {
            final CirSnippet checkNullPointer = CirSnippet.get(Snippet.CheckNullPointer.SNIPPET);
            return call(
                       checkNullPointer,
                       object,
                       cont(cc),
                       ce());
        }
        assert object.kind().isWord;
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
        if (!checksArrayIndexAgainstBounds(operator)) {
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
        if (ce() != originalCE) {
            if (arguments[arguments.length - 1] == originalCE) {
                arguments[arguments.length - 1] = ce();
            }
            call = new CirCall(closure(call, ce()), originalCE);
        }
        this.call.assign(call);
    }

    abstract class Resolvable {
        final ResolutionSnippet snippet;

        public Resolvable(ResolutionSnippet resolutionSnippet) {
            snippet = resolutionSnippet;
        }

        abstract CirCall makeCall(CirValue resolvedAndInitialized);
    }

    CirCall callWithResolutionAndClassInitialization(Resolvable resolvable, JavaResolvableOperator<? extends Actor> operator) {

        final boolean isFieldOrMethod = operator.isFieldOrMethod();

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

        final CirVariable actor = variableFactory().createTemporary(resolvable.snippet.resultKind);

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

        final CirConstant guard = CirConstant.fromObject(operator.constantPool().makeResolutionGuard(operator.index(), resolvable.snippet));
        final CirSnippet resolve = CirSnippet.get(resolvable.snippet);
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
                final CirValue reference = arguments[arguments.length - 3];
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

    abstract class StaticTupleAccess {
        final CirValue fieldActor;
        StaticTupleAccess(CirValue fieldActor) {
            this.fieldActor = fieldActor;
        }
        abstract CirCall makeCall(CirValue staticTuple);
    }

    CirCall callWithStaticTuple(StaticTupleAccess staticTupleAccess) {
        if (staticTupleAccess.fieldActor.isConstant()) {
            FieldActor fieldActor = (FieldActor) staticTupleAccess.fieldActor.value().asObject();
            CirConstant staticTuple = new CirConstant(ReferenceValue.from(fieldActor.holder().staticTuple()));
            return staticTupleAccess.makeCall(staticTuple);
        }
        final CirVariable staticTuple = variableFactory().createTemporary(Kind.REFERENCE);
        return call(
            CirSnippet.get(BuiltinsSnippet.GetStaticTuple.SNIPPET),
            staticTupleAccess.fieldActor,
            cont(
                staticTuple,
                staticTupleAccess.makeCall(staticTuple)),
            ce());
    }

    @Override
    public void visit(final GetStatic operator) {
        final Resolvable resolvable = new Resolvable(ResolveStaticFieldForReading.SNIPPET) {
            @Override
            CirCall makeCall(CirValue fieldActor) {
                StaticTupleAccess staticTupleAccess = new StaticTupleAccess(fieldActor) {
                    @Override
                    CirCall makeCall(CirValue staticTuple) {
                        final CirSnippet fieldRead = CirSnippet.get(FieldReadSnippet.selectSnippet(operator.resultKind()));
                        return call(
                            fieldRead,
                            staticTuple,
                            fieldActor,
                            cc(),
                            ce());
                    }
                };
                return callWithStaticTuple(staticTupleAccess);
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
                final CirValue value = arguments[0];
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
        final CirValue receiver = arguments[0];

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
                                arguments)),
                        ce());
                CirJavaFrameDescriptor jfd = HCirOperatorLowering.this.call.javaFrameDescriptor();
                final MethodProfile methodProfile = getMethodProfile(jfd.classMethodActor);
                final Hub mostFrequentHub = MethodInstrumentation.computeMostFrequentHub(methodProfile, jfd.bci, 1500, 0.9f);

                if (mostFrequentHub != null) {
                    final CirValue cont = arguments[arguments.length - 2];
                    final CirContinuationVariable cc = variableFactory().createFreshNormalContinuationParameter();
                    final CirContinuationVariable ce = variableFactory().createFreshExceptionContinuationParameter();
                    arguments[arguments.length - 2] = cc;
                    arguments[arguments.length - 1] = ce;

                    final ClassMethodRefConstant classMethodRef = operator.constantPool().classMethodAt(operator.index());
                    final VirtualMethodActor declaredMethod = classMethodRef.resolveVirtual(operator.constantPool(), operator.index());
                    final Address entryPoint = mostFrequentHub.getWord(declaredMethod.vTableIndex()).asAddress();
                    final TargetMethod targetMethod = Code.codePointerToTargetMethod(entryPoint);
                    final ClassMethodActor targetMethodActor = targetMethod.classMethodActor();
                    final CirMethod targetCirMethod = methodTranslation.cirGenerator().createIrMethod(targetMethodActor);
                    final CirValue cachedHub = new CirConstant(ReferenceValue.from(mostFrequentHub));
                    final CirVariable hub = variableFactory().createTemporary(Kind.REFERENCE);
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
                                                       arguments.clone())),
                                               cont(
                                                   call))),
                                       ce()),
                                   cc,
                                   ce),
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

    private MethodProfile getMethodProfile(ClassMethodActor classMethodActor) {
        return null;
    }

    @Override
    public void visit(InvokeInterface operator) {
        final Resolvable resolvable = new Resolvable(ResolveInterfaceMethod.SNIPPET) {
            @Override
            CirCall makeCall(CirValue interfaceMethodActor) {
                final CirSnippet selectMethod = CirSnippet.get(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
                final CirValue receiver = arguments[0];
                final CirVariable callEntryPoint = variableFactory().createTemporary(Kind.WORD);
                return call(
                    selectMethod,
                    receiver,
                    interfaceMethodActor,
                    cont(
                        callEntryPoint,
                        call(
                            callEntryPoint,
                            arguments)),
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
                            arguments)),
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
                            arguments)),
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
                final CirValue object = arguments[0];
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
        final CirValue objref = arguments[0];
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
        return originalCC;
    }

    /**
     * Gets the exception continuation for the current translation scope.
     */
    CirVariable ce() {
        return ce;
    }

    CirVariableFactory variableFactory() {
        return methodTranslation.variableFactory();
    }

    @Override
    public void visit(ArrayLoad operator) {
        final CirValue array = arguments[0];
        final CirValue index = arguments[1];

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
                final CirSnippet createTupleOrHybrid = CirSnippet.get(CreateTupleOrHybrid.SNIPPET);
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
        final CirValue array = arguments[0];
        final CirValue index = arguments[1];
        final CirValue value = arguments[2];

        final CirSnippet arrayStore = CirSnippet.get(ArraySetSnippet.selectSnippetByKind(operator.elementKind()));

        if (operator.elementKind().isReference) {
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
                final CirValue object = arguments[0];
                final CirValue value = arguments[1];
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
        final CirValue length = arguments[0];
        if (operator.primitiveElementKind() != null) {
            final CirValue kind = CirConstant.fromObject(operator.primitiveElementKind());
            final CirSnippet createPrimitiveArray = CirSnippet.get(CreatePrimitiveArray.SNIPPET);
            set(call(
                    createPrimitiveArray,
                    kind,
                    length,
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
                               length,
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
        final CirSnippet createArray = CirSnippet.get(CreateMultiReferenceArray.SNIPPET);
        final CirSnippet createDimensionArray = CirSnippet.get(CreatePrimitiveArray.SNIPPET);
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
                       checkArrayDimension, arguments[i],
                       cont(
                           call(
                               setDimensionInArray,
                               dimensionArray,
                               CirConstant.fromInt(i),
                               arguments[i],
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
        final CirValue array = arguments[0];
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
        final CirValue object = arguments[0];
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
        final CirValue object = arguments[0];
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
        int callEntryPointIndex = arguments.length - 3;
        final CirValue callEntryPoint = arguments[callEntryPointIndex];
        CirValue[] newArguments = CirCall.newArguments(arguments.length - 1);
        System.arraycopy(arguments, 0, newArguments, 0, callEntryPointIndex);
        newArguments[newArguments.length - 1] = arguments[arguments.length - 1];
        newArguments[newArguments.length - 2] = arguments[arguments.length - 2];
        CirCall call = call(callEntryPoint, newArguments);
        call.setIsNative();
        set(call);
    }

    @Override
    public void visit(Call operator) {
        set(call(CirBuiltin.get(SpecialBuiltin.Call.BUILTIN), arguments));
    }

    @Override
    public void visit(JavaOperator operator) {
        if (operator instanceof Lowerable) {
            final Lowerable lowerableOp = (Lowerable) operator;
            lowerableOp.toLCir(lowerableOp, this.call, methodTranslation.cirGenerator().compilerScheme());
        }
    }
}

