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
package com.oracle.max.vm.ext.graal.stubs;

import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.SignatureDescriptor;

/**
 * A utility class for generating a native method stub (as a compiler graph) implementing
 * the transition from Java to native code. This process is very similar to using snippets
 * to lower high-level nodes. However, since the number and types of the arguments to a native method
 * are variable, we cannot just use the snippet instantiation mechanism directly.
 */
public class NativeStubGraphBuilder extends AbstractGraphBuilder {

    /**
     * Creates a native stub graph builder instance for a given native method.
     */
    public NativeStubGraphBuilder(ClassMethodActor nativeMethod) {
        super(nativeMethod);
        assert nativeMethod.isNative();
        StructuredGraph template;
        if (nativeMethod.isCFunction()) {
            if (NativeInterfaces.needsPrologueAndEpilogue(nativeMethod)) {
                template = cFunctionTemplate;
            } else {
                template = noPrologueOrEpilogueTemplate;
            }
        } else {
            if (nativeMethod.isSynchronized()) {
                template = synchronizedTemplate;
            } else {
                template = normalTemplate;
            }
        }
        setGraph(template.copy(nativeMethod.name()));
    }

    /**
     * Placeholder for the code allocating and initialize the JNI handles for the object arguments of a native method stub.
     * The call to the method is replaced with a graph produced by {@link InitializeHandlesGraphBuilder}.
     */
    static native Pointer initializeHandles();
    static final ResolvedJavaMethod initializeHandles = MaxResolvedJavaMethod.get(findMethod(NativeStubGraphBuilder.class, "initializeHandles"));

    /**
     * Placeholder for the real native function call in a template native method stub.
     * The call to the method is replaced with a graph produced by {@link NativeFunctionCallGraphBuilder}.
     */
    static native Object nativeFunctionCall(Address function, Pointer frame, Pointer jniEnv);
    static final ResolvedJavaMethod nativeFunctionCall = MaxResolvedJavaMethod.get(findMethod(NativeStubGraphBuilder.class, "nativeFunctionCall", Address.class, Pointer.class, Pointer.class));

    /**
     * Stub template for a native method.
     */
    @INLINE
    public static Object template(NativeFunction nativeFunction, String traceName) throws Throwable {

        Pointer handles = initializeHandles();

        Address address = nativeFunction.link();
        VmThread thread = VmThread.current();

        if (traceName != null) {
            Log.print("[Thread \"");
            Log.print(thread.getName());
            Log.print("\" --> JNI: ");
            Log.print(traceName);
            Log.println(']');
        }

        int jniHandlesTop = thread.jniHandlesTop();

        Object result = nativeFunctionCall(address, handles, VmThread.jniEnv());
        Snippets.nativeCallEpilogue();

        thread.resetJniHandlesTop(jniHandlesTop);

        if (traceName != null) {
            Log.print("[Thread \"");
            Log.print(thread.getName());
            Log.print("\" <-- JNI: ");
            Log.print(traceName);
            Log.println(']');
        }

        thread.throwJniException();

        return result;
    }

    /**
     * Stub template for a synchronized native method.
     */
    public static synchronized Object syncTemplate(NativeFunction nativeFunction, String traceName) throws Throwable {
        return template(nativeFunction, traceName);
    }

    /**
     * Stub template for a native method annotated with {@link C_FUNCTION}.
     */
    public static Object templateC(NativeFunction nativeFunction, String ignore) {
        Pointer frame = VMRegister.getCpuStackPointer();
        Address address = nativeFunction.link();
        Snippets.nativeCallPrologueForC(nativeFunction);
        Object result = nativeFunctionCall(address, frame, VmThread.jniEnv());
        Snippets.nativeCallEpilogueForC();
        return result;
    }

    /**
     * Stub template for a native method that doesn't need a prologue and epilogue around the native function call.
     *
     * @see NativeInterfaces#needsPrologueAndEpilogue(MethodActor)
     */
    public static Object templateNoPrologueOrEpilogue(NativeFunction nativeFunction, String ignore) {
        Pointer frame = VMRegister.getCpuStackPointer();
        Address address = nativeFunction.link();
        Snippets.nativeCallPrologueForC(nativeFunction);
        Object result = nativeFunctionCall(address, frame, VmThread.jniEnv());
        Snippets.nativeCallEpilogueForC();
        return result;
    }

    static StructuredGraph normalTemplate;
    static StructuredGraph synchronizedTemplate;
    static StructuredGraph cFunctionTemplate;
    static StructuredGraph noPrologueOrEpilogueTemplate;

    /**
     * Builds the graph for the native method stub.
     */
    public StructuredGraph build() {
        Debug.scope("NativeStubGraphBuilder", new Object[]{nativeMethod}, new Runnable() {
            public void run() {
                buildGraph();
            }
        });
        return graph;
    }

    private StructuredGraph buildGraph() {
        SignatureDescriptor sig = nativeMethodActor.descriptor();

        Debug.dump(graph, NativeStubGraphBuilder.class.getSimpleName() + ":" + nativeMethod.getName());

        Iterator<LocalNode> locals = graph.getNodes(LocalNode.class).iterator();
        LocalNode local0 = locals.next();
        LocalNode local1 = locals.next();
        assert !locals.hasNext() : "template should have exactly two arguments";
        assert local0.kind() == Kind.Object;
        assert local1.kind() == Kind.Object;

        // Replace template parameters with constants
        local0.replaceAtUsages(oconst(nativeMethodActor.nativeFunction));
        local1.replaceAtUsages(JniFunctions.logger.traceEnabled() ? oconst(nativeMethodActor.format("%H.%n(%P)")) : oconst(null));
        local0.safeDelete();
        local1.safeDelete();

        ReturnNode returnNode = null;
        for (Node n : graph.getNodes()) {
            if (n instanceof ReturnNode) {
                returnNode = (ReturnNode) n;
                break;
            }
        }
        assert returnNode != null;

        Debug.dump(graph, "%s: SpecializeLocals", nativeMethod.getName());

        // Add parameters of native method
        boolean isStatic = nativeMethodActor.isStatic();

        for (Invoke invoke : graph.getInvokes()) {
            MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
            ResolvedJavaMethod method = callTarget.targetMethod();
            if (method == initializeHandles) {
                NodeInputList<ValueNode> arguments = callTarget.arguments();
                assert arguments.isEmpty();
                arguments.addAll(createLocals(0, sig, isStatic));
                StructuredGraph initializeHandlesGraph = new InitializeHandlesGraphBuilder(nativeMethodActor).graph;
                Debug.dump(initializeHandlesGraph, "InitializeHandles");
                applyPhasesBeforeInlining(initializeHandlesGraph);
                InliningUtil.inline(invoke, initializeHandlesGraph, false);
            } else if (method == nativeFunctionCall) {
                // replace call with native function sequence
                NodeInputList<ValueNode> arguments = callTarget.arguments();
                arguments.addAll(createLocals(0, sig, isStatic));
                StructuredGraph nativeFunctionCallGraph = new NativeFunctionCallGraphBuilder(nativeMethodActor).graph;
                Debug.dump(nativeFunctionCallGraph, "NativeFunctionCall");
                applyPhasesBeforeInlining(nativeFunctionCallGraph);
                InliningUtil.inline(invoke, nativeFunctionCallGraph, false);
            }
        }

        // Fixed the return node to be of the correct kind
        ReturnNode fixedReturnNode = graph.add(new ReturnNode(returnNode.result()));
        returnNode.replaceAndDelete(fixedReturnNode);

        Debug.dump(graph, "Inlined");
        new DeadCodeEliminationPhase().apply(graph);
        Debug.dump(graph, "%s: Final", nativeMethod.getName());

        graph.verify();
        return graph;
    }

    /**
     * Applies a number of phases to a graph before it is inlined into another graph.
     */
    protected void applyPhasesBeforeInlining(StructuredGraph graph) {
        new MaxFoldPhase(MaxGraal.runtime()).apply(graph);
        new MaxIntrinsicsPhase().apply(graph);
        new CanonicalizerPhase.Instance(MaxGraal.runtime(), new Assumptions(GraalOptions.OptAssumptions.getValue()), true);
    }

    @HOSTED_ONLY
    public static void initialize(OptimisticOptimizations optimisticOptimizations) {
        if (normalTemplate == null) {
            // Initialize the templates
            normalTemplate = createTemplate("template", optimisticOptimizations);
            synchronizedTemplate = createTemplate("syncTemplate", optimisticOptimizations);
            cFunctionTemplate = createTemplate("templateC", optimisticOptimizations);
            noPrologueOrEpilogueTemplate = createTemplate("templateNoPrologueOrEpilogue", optimisticOptimizations);
        }
    }

    @HOSTED_ONLY
    private static StructuredGraph createTemplate(final String name, final OptimisticOptimizations optimisticOptimizations) {
        MethodActor methodActor = findMethod(NativeStubGraphBuilder.class, name, NativeFunction.class, String.class);
        ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);
        final StructuredGraph graph = new StructuredGraph(method);

        Debug.scope("NativeStubCreateTemplate", new Object[]{method}, new Runnable() {

            public void run() {
                new MaxGraphBuilderPhase(MaxGraal.runtime(), GraphBuilderConfiguration.getDefault(), optimisticOptimizations).apply(graph);
                new MaxFoldPhase(MaxGraal.runtime()).apply(graph);
                new MaxIntrinsicsPhase().apply(graph);
                new CanonicalizerPhase.Instance(MaxGraal.runtime(), new Assumptions(GraalOptions.OptAssumptions.getValue()), true).apply(graph);
                new DeadCodeEliminationPhase().apply(graph);

                Debug.dump(graph, "%s: Final", name);
            }

        });

        return graph;
    }

}
