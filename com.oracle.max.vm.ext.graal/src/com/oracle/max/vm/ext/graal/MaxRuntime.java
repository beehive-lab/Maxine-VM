/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.CodeUtil.RefMapFormatter;
import com.oracle.graal.api.code.RuntimeCallTarget.Descriptor;
import com.oracle.graal.api.code.CompilationResult.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.printer.*;
import com.oracle.graal.snippets.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;



public class MaxRuntime implements GraalCodeCacheProvider {

    private final Map<Class< ? extends Node>, LoweringProvider> lowerings = new HashMap<>();

    MaxTargetDescription maxTargetDescription;

    MaxRuntime(MaxTargetDescription maxTargetDescription) {
        this.maxTargetDescription = maxTargetDescription;
    }

    @Override
    public InstalledCode addMethod(ResolvedJavaMethod method, CompilationResult compResult, CodeInfo[] info) {
        unimplemented("addMethod");
        return null;
    }

    @Override
    public int getSizeOfLockData() {
        unimplemented("getSizeOfLockData");
        return 0;
    }

    @Override
    public String disassemble(CodeInfo info, CompilationResult tm) {
        byte[] code = info.getCode();
        HexCodeFile hcf = new HexCodeFile(code, info.getStart(), maxTargetDescription.arch.getName(), maxTargetDescription.wordSize * 8);
        if (tm != null) {
            HexCodeFile.addAnnotations(hcf, tm.getAnnotations());
            addExceptionHandlersComment(tm, hcf);
            Register fp = RegisterMap.toGraal(vm().registerConfigs.standard.frame);
            RefMapFormatter slotFormatter = new RefMapFormatter(maxTargetDescription.arch, maxTargetDescription.wordSize, fp, 0);
            for (Safepoint safepoint : tm.getSafepoints()) {
                if (safepoint instanceof Call) {
                    Call call = (Call) safepoint;
                    if (call.debugInfo != null) {
                        hcf.addComment(call.pcOffset + call.size, CodeUtil.append(new StringBuilder(100), call.debugInfo, slotFormatter).toString());
                    }
                } else {
                    if (safepoint.debugInfo != null) {
                        hcf.addComment(safepoint.pcOffset, CodeUtil.append(new StringBuilder(100), safepoint.debugInfo, slotFormatter).toString());
                    }
                    addOperandComment(hcf, safepoint.pcOffset, "{safepoint}");
                }
            }
            for (DataPatch site : tm.getDataReferences()) {
                hcf.addOperandComment(site.pcOffset, "{" + site.constant + "}");
            }
        }
        return hcf.toEmbeddedString();
    }

    private static void addExceptionHandlersComment(CompilationResult tm, HexCodeFile hcf) {
        if (!tm.getExceptionHandlers().isEmpty()) {
            String nl = HexCodeFile.NEW_LINE;
            StringBuilder buf = new StringBuilder("------ Exception Handlers ------").append(nl);
            for (CompilationResult.ExceptionHandler e : tm.getExceptionHandlers()) {
                buf.append("    ").append(e.pcOffset).append(" -> ").append(e.handlerPos).append(nl);
                hcf.addComment(e.pcOffset, "[exception -> " + e.handlerPos + "]");
                hcf.addComment(e.handlerPos, "[exception handler for " + e.pcOffset + "]");
            }
            hcf.addComment(0, buf.toString());
        }
    }

    private static void addOperandComment(HexCodeFile hcf, int pos, String comment) {
        String oldValue = hcf.addOperandComment(pos, comment);
        assert oldValue == null : "multiple comments for operand of instruction at " + pos + ": " + comment + ", " + oldValue;
    }

    @Override
    public RegisterConfig lookupRegisterConfig(ResolvedJavaMethod method) {
        return MaxRegisterConfig.get(vm().registerConfigs.getRegisterConfig((ClassMethodActor) MaxJavaMethod.get(method)));
    }

    @Override
    public int getCustomStackAreaSize() {
        return 0;
    }

    @Override
    public int getMinimumOutgoingSize() {
        return 0;
    }

    @Override
    public Object lookupCallTarget(Object callTarget) {
        /*
        if (callTarget instanceof SubstrateRuntimeCall) {
            return metaAccess.lookupJavaMethod(((SubstrateRuntimeCall) callTarget).getDescriptor().getMethod());
        }
        */
        return callTarget;
    }

    private class MaxRuntimeCallTarget implements RuntimeCallTarget {
        private final Descriptor descriptor;
        private final CallingConvention callingConvention;

        MaxRuntimeCallTarget(Descriptor descriptor) {
            this.descriptor = descriptor;
            RegisterConfig registerConfig = MaxRegisterConfig.get(MaxineVM.vm().registerConfigs.standard);
            this.callingConvention = registerConfig.getCallingConvention(CallingConvention.Type.RuntimeCall, descriptor.getResultKind(), descriptor.getArgumentKinds(),
                            maxTargetDescription, false);
        }

        @Override
        public CallingConvention getCallingConvention() {
            return callingConvention;
        }

        @Override
        public long getMaxCallTargetOffset() {
            // TODO Check
            return -1;
        }

        @Override
        public Descriptor getDescriptor() {
            return descriptor;
        }

    }

    @Override
    public RuntimeCallTarget lookupRuntimeCall(Descriptor descriptor) {
        return new MaxRuntimeCallTarget(descriptor);
    }

    @Override
    public int encodeDeoptActionAndReason(DeoptimizationAction action, DeoptimizationReason reason) {
        unimplemented("encodeDeoptActionAndReason");
        return 0;
    }

    @Override
    public boolean needsDataPatch(Constant constant) {
        unimplemented("needsDataPatch");
        return false;
    }

    @Override
    public ResolvedJavaType lookupJavaType(Class< ? > clazz) {
        return MaxResolvedJavaType.get(ClassActor.fromJava(clazz));
    }

    @Override
    public ResolvedJavaMethod lookupJavaMethod(Method reflectionMethod) {
        return MaxResolvedJavaMethod.get(MethodActor.fromJava(reflectionMethod));
    }

    @Override
    public ResolvedJavaField lookupJavaField(Field reflectionField) {
        return MaxResolvedJavaField.get(FieldActor.fromJava(reflectionField));
    }

    @Override
    public ResolvedJavaType lookupJavaType(Constant constant) {
        if (constant.getKind() == Kind.Object) {
            Object o = constant.asObject();
            if (o != null) {
                return MaxResolvedJavaType.get(ClassActor.fromJava(o.getClass()));
            }
        }
        return null;
    }

    @Override
    public boolean constantEquals(Constant x, Constant y) {
        assert x.getKind() == Kind.Object && y.getKind() == Kind.Object;
        return x.asObject() == y.asObject();
    }

    @Override
    public ResolvedJavaMethod lookupJavaConstructor(Constructor reflectionConstructor) {
        unimplemented("lookupJavaConstructor");
        return null;
    }

    @Override
    public int lookupArrayLength(Constant array) {
        return Array.getLength(array.asObject());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void lower(Node n, LoweringTool tool) {
        LoweringProvider lowering = lowerings.get(n.getClass());
        ProgramError.check(lowering != null, "missing lowering for node: " + n.getClass().getSimpleName());
        lowering.lower(n, tool);
    }

    public void init() {
        // Snippets cannot have optimistic assumptions.
        Assumptions assumptions = new Assumptions(false);
        SnippetInstaller installer = new SnippetInstaller(this, assumptions, maxTargetDescription);
        GraalIntrinsics.installIntrinsics(installer);
        installer.install(NewSnippets.class);

        lowerings.put(InvokeNode.class, new InvokeLowering());
        lowerings.put(InvokeWithExceptionNode.class, new InvokeLowering());
        NewSnippets.registerLowerings(VMConfiguration.activeConfig(), maxTargetDescription, this, assumptions, lowerings);
    }

    protected class InvokeLowering implements LoweringProvider<FixedNode> {

        @Override
        public void lower(FixedNode node, LoweringTool tool) {
            StructuredGraph graph = (StructuredGraph) node.graph();
            Invoke invoke = (Invoke) node;
            if (invoke.callTarget() instanceof MethodCallTargetNode) {
                MethodCallTargetNode callTarget = invoke.methodCallTarget();
                NodeInputList<ValueNode> parameters = callTarget.arguments();
                ValueNode receiver = parameters.size() <= 0 ? null : parameters.get(0);
                if (!callTarget.isStatic() && receiver.kind() == Kind.Object && !receiver.objectStamp().nonNull()) {
                    IsNullNode isNull = graph.unique(new IsNullNode(receiver));
                    graph.addBeforeFixed(node, graph.add(new FixedGuardNode(isNull, DeoptimizationReason.NullCheckException, null, true, invoke.leafGraphId())));
                }
                Kind[] signature = MetaUtil.signatureToKinds(callTarget.targetMethod().getSignature(), callTarget.isStatic() ? null : callTarget.targetMethod().getDeclaringClass().getKind());
                CallingConvention.Type callType = CallingConvention.Type.JavaCall;

                CallTargetNode loweredCallTarget = null;
                switch (callTarget.invokeKind()) {
                    case Static:
                    case Special:
                        loweredCallTarget = graph.add(new DirectCallTargetNode(parameters, callTarget.returnStamp(), signature, callTarget.targetMethod(), callType));
                        break;
                    case Virtual:
                        /*
                    case Interface:
                        ClassMethodActor method = (ClassMethodActor) callTarget.targetMethod();
                        if (method.getImplementations().length == 0) {
                            // We are calling an abstract method with no implementation, i.e., the closed-world analysis
                            // showed that there is no concrete receiver ever allocated. This must be dead code.
                            graph.addBeforeFixed(node, graph.add(new FixedGuardNode(ConstantNode.forBoolean(true, graph), DeoptimizationReason.UnreachedCode, null, true, invoke.leafGraphId())));
                            return;
                        }
                        int vtableEntryOffset = NumUtil.safeToInt(hubLayout.getArrayElementOffset(method.getVTableIndex()));

                        ReadNode hub = graph.add(new ReadNode(receiver, LocationNode.create(LocationNode.FINAL_LOCATION, Kind.Object, objectLayout.hubOffset(), graph), StampFactory.objectNonNull()));
                        LocationNode entryLoc = LocationNode.create(LocationNode.FINAL_LOCATION, target.wordKind, vtableEntryOffset, graph);
                        ReadNode entry = graph.add(new ReadNode(hub, entryLoc, StampFactory.forKind(target.wordKind)));
                        loweredCallTarget = graph.add(new IndirectCallTargetNode(entry, parameters, callTarget.returnStamp(), signature, callTarget.targetMethod(), callType));

                        graph.addBeforeFixed(node, hub);
                        graph.addAfterFixed(hub, entry);
                        break;
                        */
                    default:
                        ProgramError.unknownCase();
                }
                callTarget.replaceAndDelete(loweredCallTarget);
            }
        }
    }


}
