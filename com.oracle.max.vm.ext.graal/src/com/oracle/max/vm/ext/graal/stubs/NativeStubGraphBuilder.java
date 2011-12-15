/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.max.vm.ext.maxri.MaxRuntime.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A utility class for generating a native method stub (as a compiler graph) implementing
 * the transition from Java to native code.
 */
public class NativeStubGraphBuilder extends AbstractGraphBuilder {

    /**
     * Creates a native stub graph builder instance for a given native method.
     */
    public NativeStubGraphBuilder(ClassMethodActor nativeMethod) {
        super(nativeMethod);
        assert nativeMethod.isNative();
        StructuredGraph template = nativeMethod.isCFunction() ? cFunctionTemplate : nativeMethod.isSynchronized() ? synchronizedTemplate : normalTemplate;
        setGraph(template.copy(nativeMethod.name()));
    }

    /**
     * Placeholder for the code allocating and initialize the JNI handles for the object arguments of a native method stub.
     * The call to the method is replaced with a graph produced by {@link InitializeHandlesGraphBuilder}.
     */
    static native Pointer initializeHandles();
    static final MethodActor initializeHandles = findMethod(NativeStubGraphBuilder.class, "initializeHandles");

    /**
     * Placeholder for the real native function call in a template native method stub.
     * The call to the method is replaced with a graph produced by {@link NativeFunctionCallGraphBuilder}.
     */
    static native Object nativeFunctionCall(Address function, Pointer frame, Pointer jniEnv);
    static final MethodActor nativeFunctionCall = findMethod(NativeStubGraphBuilder.class, "nativeFunctionCall", Address.class, Pointer.class, Pointer.class);

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

    static StructuredGraph normalTemplate;
    static StructuredGraph synchronizedTemplate;
    static StructuredGraph cFunctionTemplate;

    /**
     * Builds the graph for the native method stub.
     */
    public StructuredGraph build() {
        SignatureDescriptor sig = nativeMethod.descriptor();

        if (observer != null) {
            observer.compilationStarted(NativeStubGraphBuilder.class.getSimpleName() + ":" + nativeMethod.format("%h.%n(%p)"));
            observer.printGraph("CopyTemplate", graph);
        }

        Iterator<LocalNode> locals = graph.getNodes(LocalNode.class).iterator();
        LocalNode local0 = locals.next();
        LocalNode local1 = locals.next();
        assert !locals.hasNext() : "template should have exactly two arguments";
        assert local0.kind().isObject();
        assert local1.kind().isObject();

        // Replace template parameters with constants
        local0.replaceAtUsages(oconst(nativeMethod.nativeFunction));
        local1.replaceAtUsages(JniFunctions.TraceJNI ? oconst(nativeMethod.format("%H.%n(%P)")) : oconst(null));
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

        if (observer != null) {
            observer.printGraph("SpecializeLocals", graph);
        }

        // Add parameters of native method
        boolean isStatic = nativeMethod.isStatic();
        List<LocalNode> inArgs = createLocals(0, sig, isStatic);

        for (Invoke invoke : graph.getInvokes()) {
            MethodCallTargetNode callTarget = invoke.callTarget();
            RiResolvedMethod method = callTarget.targetMethod();
            if (method == initializeHandles) {
                NodeInputList<ValueNode> arguments = callTarget.arguments();
                assert arguments.isEmpty();
                arguments.addAll(inArgs);
                StructuredGraph initializeHandlesGraph = new InitializeHandlesGraphBuilder(nativeMethod).graph;
                if (observer != null) {
                    observer.printGraph("InitializeHandles", initializeHandlesGraph);
                }
                applyPhasesBeforeInlining(initializeHandlesGraph);
                InliningUtil.inline(invoke, initializeHandlesGraph, false);
            } else if (method == nativeFunctionCall) {
                // replace call with native function sequence
                NodeInputList<ValueNode> arguments = callTarget.arguments();
                arguments.addAll(inArgs);
                StructuredGraph nativeFunctionCallGraph = new NativeFunctionCallGraphBuilder(nativeMethod).graph;
                if (observer != null) {
                    observer.printGraph("NativeFunctionCall", nativeFunctionCallGraph);
                }
                applyPhasesBeforeInlining(nativeFunctionCallGraph);
                InliningUtil.inline(invoke, nativeFunctionCallGraph, false);
            }
        }

        // Fixed the return node to be of the correct kind
        ReturnNode fixedReturnNode = graph.add(new ReturnNode(returnNode.result()));
        returnNode.replaceAndDelete(fixedReturnNode);

        if (observer != null) {
            observer.printGraph("Inlined", graph);
            apply(new DeadCodeEliminationPhase(), graph);
            observer.compilationFinished(null);
        }

        graph.verify();
        return graph;
    }

    /**
     * Applies a number of phases to a graph before it is inlined into another graph.
     */
    protected void applyPhasesBeforeInlining(StructuredGraph graph) {
        apply(new FoldPhase(runtime()), graph);
        apply(new MaxineIntrinsicsPhase(), graph);
        apply(new MustInlinePhase(runtime(), new HashMap<RiMethod, StructuredGraph>(), null, GraphBuilderConfiguration.getDefault()), graph);
        apply(new CanonicalizerPhase(null, runtime(), null), graph);
    }

    @HOSTED_ONLY
    public static void initialize() {
        if (normalTemplate == null) {
            if (GraalOptions.PrintIdealGraphLevel != 0 || GraalOptions.Plot || GraalOptions.PlotOnError) {
                if (GraalOptions.PrintIdealGraphFile) {
                    observer = new IdealGraphPrinterObserver();
                } else {
                    IdealGraphPrinterObserver o = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
                    if (o.networkAvailable()) {
                        observer = o;
                    }
                }
            }

            // Initialize the templates
            normalTemplate = createTemplate("template");
            synchronizedTemplate = createTemplate("syncTemplate");
            cFunctionTemplate = createTemplate("templateC");
        }
    }

    private static void apply(Phase phase, StructuredGraph graph) {
        phase.apply(graph);
        if (observer != null) {
            observer.printGraph(phase.getClass().getSimpleName(), graph);
        }
    }

    @HOSTED_ONLY
    private static StructuredGraph createTemplate(String name) {
        MaxRuntime runtime = runtime();
        StructuredGraph graph = new StructuredGraph();
        MethodActor method = findMethod(NativeStubGraphBuilder.class, name, NativeFunction.class, String.class);

        if (observer != null) {
            observer.compilationStarted(NativeStubGraphBuilder.class.getSimpleName() + ":" + name);
        }

        GraphBuilderConfiguration config = GraphBuilderConfiguration.getDeoptFreeDefault();
        apply(new GraphBuilderPhase(runtime, method, null, config), graph);
        apply(new FoldPhase(runtime), graph);
        apply(new MaxineIntrinsicsPhase(), graph);
        apply(new MustInlinePhase(runtime, new HashMap<RiMethod, StructuredGraph>(), null, config), graph);
        apply(new CanonicalizerPhase(null, runtime, null), graph);
        apply(new DeadCodeEliminationPhase(), graph);

        if (observer != null) {
            observer.compilationFinished(null);
        }

        return graph;
    }

    static IdealGraphPrinterObserver observer;
}
