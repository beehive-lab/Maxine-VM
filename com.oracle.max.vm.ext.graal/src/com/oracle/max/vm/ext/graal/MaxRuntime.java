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
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.common.*;
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
    public RegisterConfig lookupRegisterConfig() {
        // TODO this method used to take a ResolvedJavaMethod as an argument
        // Maxine has different register configs for VM_ENTRY_POINT methods and "standard" methods,
        // which will need to be adderess when Graal is nused for the boot image.
        //return MaxRegisterConfig.get(vm().registerConfigs.getRegisterConfig((ClassMethodActor) MaxJavaMethod.getRiMethod(method)));
        return MaxRegisterConfig.get(vm().registerConfigs.standard);
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
    public RuntimeCallTarget lookupRuntimeCall(Descriptor descriptor) {
        return MaxRuntimeCallsMap.get(descriptor);
    }

    @Override
    public int encodeDeoptActionAndReason(DeoptimizationAction action, DeoptimizationReason reason) {
        unimplemented("encodeDeoptActionAndReason");
        return 0;
    }

    @Override
    public boolean needsDataPatch(Constant constant) {
        // TODO when might this be true?
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
                // When running isHosted, StaticTuple is an issue as it is HOSTED_ONLY
                // so shows up as null, which causes an assertion failure later
                if (MaxineVM.isHosted()) {
                    if (o.getClass() == StaticTuple.class) {
                        return MaxResolvedJavaType.get(ClassActor.fromJava(Object.class));
                    }
                }
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

    static class MaxSnippetGraphBuilderConfiguration extends GraphBuilderConfiguration {

        boolean ignoreHostOnlyError;

        public MaxSnippetGraphBuilderConfiguration() {
            super(ResolvePolicy.EagerForSnippets, null);
        }

        @Override
        public boolean unresolvedForSnippetsIsError() {
            // This prevents an assertion error in GraphBuilderPhase when we return an unresolved field
            return false;
        }

    }

    private static class MaxSnippetInstallerCustomizer extends SnippetInstaller.DefaultCustomizer {
        private MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration;

        MaxSnippetInstallerCustomizer(MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration) {
            this.maxSnippetGraphBuilderConfiguration = maxSnippetGraphBuilderConfiguration;
        }

        @Override
        public GraphBuilderConfiguration getConfig() {
            return maxSnippetGraphBuilderConfiguration;
        }

        @Override
        public void afterBuild(SnippetInstaller si, StructuredGraph graph) {
            new MaxIntrinsicsPhase().apply(graph);
            new MaxWordTypeRewriterPhase.MaxInvokeRewriter(si.runtime, si.target.wordKind).apply(graph);
            super.afterBuild(si, graph);
        }

        @Override
        public void afterInline(SnippetInstaller si, StructuredGraph graph) {
            new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(si.runtime, si.target.wordKind).apply(graph);
            new CanonicalizerPhase(si.runtime, si.assumptions).apply(graph);
            new MaxIntrinsicsPhase().apply(graph);
        }

        @Override
        public void beforeFinal(SnippetInstaller si, StructuredGraph graph, final ResolvedJavaMethod method, final boolean isSubstitutionSnippet) {
            new SnippetIntrinsificationPhase(si.runtime, si.pool, SnippetTemplate.hasConstantParameter(method)).apply(graph);

            new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(si.runtime, si.target.wordKind).apply(graph);

            // These only get done once right at the end
            new MaxWordTypeRewriterPhase.MaxUnsafeCastRewriter(si.runtime, si.target.wordKind).apply(graph);
            new MaxSlowpathRewriterPhase(si.runtime).apply(graph);
            new MaxWordTypeRewriterPhase.KindRewriter(si.runtime, si.target.wordKind).apply(graph);

            // safe (I believe and important) to do this for Maxine snippets
            new SnippetFrameStateCleanupPhase().apply(graph);
            new DeadCodeEliminationPhase().apply(graph);

            new InsertStateAfterPlaceholderPhase().apply(graph);

        }

    }

    public void init() {
        // Snippets cannot have optimistic assumptions.
        Assumptions assumptions = new Assumptions(false);
        SnippetInstaller installer = new SnippetInstaller(this, assumptions, maxTargetDescription);
        GraalIntrinsics.installIntrinsics(installer);
        MaxIntrinsics.initialize(this, maxTargetDescription);
        MaxRuntimeCallsMap.initialize(this);
        MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration = new MaxSnippetGraphBuilderConfiguration();
        MaxConstantPool.setGraphBuilderConfig(maxSnippetGraphBuilderConfiguration);
        SnippetInstaller maxInstaller = new SnippetInstaller(this, assumptions, maxTargetDescription, new MaxSnippetInstallerCustomizer(maxSnippetGraphBuilderConfiguration));
        maxInstaller.installSnippets(TestSnippets.class);
        maxInstaller.installSnippets(NewSnippets.class);
        maxInstaller.installSnippets(FieldSnippets.class);
        MaxConstantPool.setGraphBuilderConfig(null);

        lowerings.put(InvokeNode.class, new InvokeLowering());
        lowerings.put(InvokeWithExceptionNode.class, new InvokeLowering());
        lowerings.put(UnsafeLoadNode.class, new UnsafeLoadLowering());
        lowerings.put(UnsafeStoreNode.class, new UnsafeStoreLowering());
        NewSnippets.registerLowerings(VMConfiguration.activeConfig(), maxTargetDescription, this, assumptions, lowerings);
        FieldSnippets.registerLowerings(VMConfiguration.activeConfig(), maxTargetDescription, this, assumptions, lowerings);
    }

    protected class UnsafeLoadLowering implements LoweringProvider<UnsafeLoadNode> {

        @Override
        public void lower(UnsafeLoadNode node, LoweringTool tool) {
            StructuredGraph graph = (StructuredGraph) node.graph();
            assert node.kind() != Kind.Illegal;
            IndexedLocationNode location = IndexedLocationNode.create(LocationNode.ANY_LOCATION, node.accessKind(), node.displacement(), node.offset(), graph, 1);
            ReadNode memoryRead = graph.add(new ReadNode(node.object(), location, node.stamp()));
            // An unsafe read must not floating outside its block as may float above an explicit null check on its object.
            memoryRead.dependencies().add(BeginNode.prevBegin(node));
            graph.replaceFixedWithFixed(node, memoryRead);
        }
    }

    protected class UnsafeStoreLowering implements LoweringProvider<UnsafeStoreNode> {

        @Override
        public void lower(UnsafeStoreNode node, LoweringTool tool) {
            StructuredGraph graph = (StructuredGraph) node.graph();
            IndexedLocationNode location = IndexedLocationNode.create(LocationNode.ANY_LOCATION, node.accessKind(), node.displacement(), node.offset(), graph, 1);
            WriteNode write = graph.add(new WriteNode(node.object(), node.value(), location));
            write.setStateAfter(node.stateAfter());
            graph.replaceFixedWithFixed(node, write);
        }
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
                    graph.addBeforeFixed(node, graph.add(new FixedGuardNode(isNull, DeoptimizationReason.NullCheckException, null, true)));
                }
                JavaType[] signature = MetaUtil.signatureToTypes(callTarget.targetMethod().getSignature(), callTarget.isStatic() ? null : callTarget.targetMethod().getDeclaringClass());
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

    @Override
    public Signature parseMethodDescriptor(String methodDescriptor) {
        MaxGraal.unimplemented("parseMethodDescriptor");
        return null;
    }

    @Override
    public TargetDescription getTarget() {
        return maxTargetDescription;
    }

    @Override
    public Constant readUnsafeConstant(Kind kind, Object base, long displacement) {
        // TODO
        MaxGraal.unimplemented("MaxRuntime.readUnsafeConstant");
        return null;
    }


}
