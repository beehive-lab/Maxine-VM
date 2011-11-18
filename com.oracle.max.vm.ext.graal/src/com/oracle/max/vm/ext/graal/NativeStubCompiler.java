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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.graal.nodes.FrameState.*;
import static com.oracle.max.vm.ext.maxri.MaxRuntime.*;
import static com.sun.max.vm.jni.JniHandles.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A utility class for generating a native method stub (as a compiler graph) implementing the transition
 * from Java to native code. Most of these transitions are calls to native functions via JNI.
 * However, faster transitions to Maxine specific native code are also supported.
 */
public class NativeStubCompiler {

    /**
     * Creates a native stub compiler instance for a specific native method.
     */
    public NativeStubCompiler(ClassMethodActor nativeMethod) {
        assert nativeMethod.isNative();
        if (MaxineVM.isHosted() && template == null) {
            // Initalize the templates
            IdealGraphPrinterObserver o = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
            if (o.networkAvailable()) {
                observer = o;
            }
            template = createTemplate("template");
            syncTemplate = createTemplate("syncTemplate");
        }

        StructuredGraph tmpl = nativeMethod.isSynchronized() ? syncTemplate : template;
        this.nativeMethod = nativeMethod;
        this.graph = tmpl.copy();
    }

    final ClassMethodActor nativeMethod;
    final StructuredGraph graph;

    private FixedWithNextNode lastFixedNode;
    private InvokeNode stackHandles;
    private int stackHandleOffset;

    /**
     * Placeholder for the real native function call in a template native method stub.
     */
    private static native Object nativeFunctionCall(Address address);
    private static final MethodActor nativeFunctionCall = findMethod(NativeStubCompiler.class, "nativeFunctionCall", Address.class);

    /**
     * Template for a native method stub.
     *
     * @param nativeMethod
     * @param traceName
     */
    public static Object template(ClassMethodActor nativeMethod, String traceName) throws Throwable {

        // Zero out the slot at STACK_HANDLES_ADDRESS_OFFSET
        // so that the GC doesn't scan the object handles array.
        // There must not be a safepoint in the stub before this point.
        VMRegister.getCpuStackPointer().writeWord(STACK_HANDLES_ADDRESS_OFFSET, Address.zero());

        Address address = nativeMethod.nativeFunction.link();
        VmThread thread = VmThread.current();

//        if (traceName != null) {
//            Log.print("[Thread \"");
//            Log.print(thread.getName());
//            Log.print(" --> JNI: ");
//            Log.print(traceName);
//            Log.println(']');
//        }

        int jniHandlesTop = thread.jniHandlesTop();

        Snippets.nativeCallPrologue(nativeMethod.nativeFunction);
        Object result = nativeFunctionCall(address);
        Snippets.nativeCallEpilogue();

        VMRegister.getCpuStackPointer().writeWord(STACK_HANDLES_ADDRESS_OFFSET, Address.zero());

        thread.resetJniHandlesTop(jniHandlesTop);

//        if (traceName != null) {
//            Log.print("[Thread \"");
//            Log.print(thread.getName());
//            Log.print(" <-- JNI: ");
//            Log.print(traceName);
//            Log.println(']');
//        }

        thread.throwJniException();

        return result;
    }

    /**
     * Template for a synchronized native method stub.
     *
     * @param nativeMethod
     * @param traceName
     */
    public static synchronized Object syncTemplate(ClassMethodActor nativeMethod, String traceName) throws Throwable {
        return template(nativeMethod, traceName);
    }

    static StructuredGraph template;
    static StructuredGraph syncTemplate;
//    static StructuredGraph templateWithTracing;
//    static StructuredGraph syncTemplateWithTracing;

    public CiTargetMethod compile(GraalCompiler graal, PhasePlan plan) {
        Iterator<LocalNode> args = graph.getNodes(LocalNode.class).iterator();

        // Replace parameters of template with constants
        LocalNode arg1 = args.next();
        LocalNode arg2 = args.next();
        assert !args.hasNext();

        arg1.replaceAtUsages(oconst(this.nativeMethod));
        arg2.replaceAtUsages(JniFunctions.TraceJNI ? oconst(nativeMethod.format("%H.%n(%P)")) : oconst(null));

        arg1.delete();
        arg2.delete();

        // Add parameters of native method
        SignatureDescriptor sig = nativeMethod.descriptor();
        List<LocalNode> inArgs = makeArgs(sig);

        ValueNode retValue = null;
        for (Invoke invoke : graph.getInvokes()) {
            MethodCallTargetNode callTarget = invoke.callTarget();
            RiResolvedMethod method = callTarget.targetMethod();
            if (method == nativeFunctionCall) {
                // replace call with native function sequence
                ValueNode function = callTarget.arguments().get(0);
                retValue = intrinsifyNativeFunctionCall(invoke, inArgs, sig, function);
                break;
            }
        }
        assert retValue != null;
        if (observer != null) {
            observer.printSingleGraph(nativeMethod.name(), graph);
            new DeadCodeEliminationPhase().apply(graph);
            observer.printSingleGraph(nativeMethod.name(), graph);
        }

        // Compile and print disassembly.
        graph.verify();
        CiResult result = graal.compileMethod(nativeMethod, graph, plan);
        System.out.println(runtime().disassemble(result.targetMethod()));

        return result.targetMethod();
    }

    private ValueNode intrinsifyNativeFunctionCall(Invoke nativeFunctionCall, List<LocalNode> inArgs, SignatureDescriptor sig, ValueNode function) {

        Iterator<LocalNode> inArg = inArgs.iterator();
        List<ValueNode> outArgs = new ArrayList<ValueNode>(inArgs.size() + 1);
        InvokeNode frame = null;

        assert lastFixedNode == null;
        lastFixedNode = (FixedWithNextNode) nativeFunctionCall.predecessor();

        stackHandles = append(invoke(Intrinsics_stackAllocate, iconst(stackHandlesSize(sig))));
        stackHandleOffset = 0;

        // Load the JNI environment variable
        outArgs.add(append(invoke(VmThread_jniEnv)));

        if (nativeMethod.isStatic()) {
            // Load the class for a static method
            outArgs.add(handlize(oconst(nativeMethod.holder().toJava())));
        } else {
            // Load the receiver for a non-static method
            outArgs.add(handlize(inArg.next()));
        }

        // Load the remaining parameters, handlizing object parameters
        while (inArg.hasNext()) {
            LocalNode arg = inArg.next();
            if (arg.kind().isObject()) {
                outArgs.add(handlize(arg));
                stackHandleOffset += Word.size();
            } else {
                outArgs.add(arg);
            }
        }

        if (stackHandleOffset > 1) {
            // Write the address of the handles array to STACK_HANDLES_ADDRESS_OFFSET
            // to communicate to the GC where the initialized array is.
            append(invoke(Pointer_writeWord, frame, iconst(STACK_HANDLES_ADDRESS_OFFSET), stackHandles));
        }

        // Invoke the native function
        final Kind resultKind = sig.resultDescriptor().toKind();
        ValueNode[] jniArgs = outArgs.toArray(new ValueNode[outArgs.size()]);
        NativeCallNode nativeCall = append(graph.add(new NativeCallNode(function, jniArgs, WordUtil.ciKind(resultKind, true), nativeMethod)));
        FrameState stateAfter = stateAfterCall(nativeCall, resultKind);
        nativeCall.setStateAfter(stateAfter);

        // Sign extend or zero the upper bits of a return value smaller than an int to
        // preserve the invariant that all such values are represented by an int
        // in the VM. We cannot rely on the native C compiler doing this for us.
        // Alternatively, an object return value must be un-handlized
        // *before* the JNI frame is restored.
        ValueNode retValue = nativeCall;
        switch (sig.returnKind(false)) {
            case Boolean:
            case Byte: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2B, retValue));
                break;
            }
            case Short: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2S, retValue));
                break;
            }
            case Char: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2C, retValue));
                break;
            }
            case Object: {
                retValue = append(invoke(JniHandle_unhand, retValue));
            }
        }

        nativeFunctionCall.node().replaceAndDelete(retValue);

        return retValue;
    }

    private List<LocalNode> makeArgs(SignatureDescriptor sig) {
        List<LocalNode> args = new ArrayList<LocalNode>(sig.argumentCount(!nativeMethod.isStatic()));
        int argIndex = 0;
        if (!nativeMethod.isStatic()) {
            args.add(graph.unique(new LocalNode(CiKind.Object, 0)));
        }
        for (int i = 0; i < sig.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = sig.parameterDescriptorAt(i);
            CiKind kind = WordUtil.ciKind(parameterDescriptor.toKind(), true);
            args.add(graph.unique(new LocalNode(kind, argIndex)));
            argIndex++;
        }
        return args;
    }

    @NEVER_INLINE
    private static void traceTransition(String name, boolean entry) {
        Log.print("[Thread \"");
        Log.print(VmThread.current().getName());
        if (entry) {
            Log.print(" --> JNI: ");
        } else {
            Log.print(" <-- JNI: ");
        }
        Log.print(name);
        Log.println(']');
    }

    private <T extends FixedNode> T append(T node) {
        if (lastFixedNode != null) {
            lastFixedNode.setNext(node);
        }
        if (node instanceof FixedWithNextNode) {
            lastFixedNode = (FixedWithNextNode) node;
        } else {
            lastFixedNode = null;
        }
        return node;
    }

    private InvokeNode handlize(ValueNode object) {
        assert object.kind().isObject();
        InvokeNode handle = append(invoke(JniHandles_createStackHandle, stackHandles, iconst(stackHandleOffset), object));
        stackHandleOffset += Word.size();
        return handle;
    }

    /**
     * Creates a node for the invocation of a method.
     *
     * @param target the method being called
     * @param args the arguments of the call
     */
    private InvokeNode invoke(MethodActor target, ValueNode... args) {
        InvokeKind invokeKind;
        if (target.isStatic()) {
            invokeKind = InvokeKind.Static;
        } else if (target.holder().isInterface()) {
            invokeKind = InvokeKind.Interface;
        } else {
            VirtualMethodActor vma = (VirtualMethodActor) target;
            if (vma.vTableIndex() >= 0) {
                invokeKind = InvokeKind.Virtual;
            } else {
                invokeKind = InvokeKind.Special;
            }
        }

        RiType returnType = target.descriptor().returnType(nativeMethod.holder());
        MethodCallTargetNode targetNode = graph.add(new MethodCallTargetNode(invokeKind, target, args, returnType));
        InvokeNode invokeNode = graph.add(new InvokeNode(targetNode, FrameState.BEFORE_BCI));
        FrameState stateAfter = stateAfterCall(invokeNode, target.descriptor().resultKind());
        invokeNode.setStateAfter(stateAfter);
        return invokeNode;
    }

    private FrameState stateAfterCall(ValueNode callNode, Kind resultKind) {
        ValueNode[] locals = {};
        ValueNode[] stack;
        int stackSize;
        if (resultKind == Kind.VOID) {
            stack = new ValueNode[0];
            stackSize = 0;
        } else if (resultKind.isCategory1) {
            stack = new ValueNode[] {callNode};
            stackSize = 1;
        } else {
            stack = new ValueNode[] {callNode, null};
            stackSize = 2;
        }

        List<ValueNode> locks = Collections.emptyList();
        return graph.add(new FrameState(nativeMethod, AFTER_BCI, locals, stack, stackSize, locks, false));
    }

    /**
     * Creates a constant node for an integer constant.
     */
    ConstantNode iconst(int value) {
        return ConstantNode.forInt(value, graph);
    }

    /**
     * Creates a constant node for a boolean constant.
     */
    ConstantNode zconst(boolean value) {
        return ConstantNode.forBoolean(value, graph);
    }

    /**
     * Creates a constant node for an object constant.
     */
    ConstantNode oconst(Object value) {
        return ConstantNode.forObject(value, runtime(), graph);
    }

    static final MethodActor VMRegister_getCpuStackPointer = findMethod(VMRegister.class, "getCpuStackPointer");
    static final MethodActor Pointer_writeWord = findMethod(Pointer.class, "writeWord", int.class, Word.class);
    static final MethodActor VmThread_current = findMethod(VmThread.class, "current");
    static final MethodActor VmThread_jniHandlesTop = findMethod(VmThread.class, "jniHandlesTop");
    static final MethodActor VmThread_resetJniHandlesTop = findMethod(VmThread.class, "resetJniHandlesTop", int.class);
    static final MethodActor VmThread_jniEnv = findMethod(VmThread.class, "jniEnv");
    static final MethodActor VmThread_throwJniException = findMethod(VmThread.class, "throwJniException");
    static final MethodActor Intrinsics_stackAllocate = findMethod(Intrinsics.class, "stackAllocate", int.class);
    static final MethodActor NativeFunction_link = findMethod(NativeFunction.class, "link");
    static final MethodActor Snippets_nativeCallPrologue = findMethod(Snippets.class, "nativeCallPrologue", NativeFunction.class);
    static final MethodActor Snippets_nativeCallPrologueForC = findMethod(Snippets.class, "nativeCallPrologueForC", NativeFunction.class);
    static final MethodActor Snippets_nativeCallEpilogue = findMethod(Snippets.class, "nativeCallEpilogue");
    static final MethodActor Snippets_nativeCallEpilogueForC = findMethod(Snippets.class, "nativeCallEpilogueForC");
    static final MethodActor JniHandle_unhand = findMethod(JniHandle.class, "unhand");
    static final MethodActor JniHandles_createStackHandle = findMethod(JniHandles.class, "createStackHandle", Pointer.class, int.class, Object.class);

    @HOSTED_ONLY
    private static void apply(Phase phase, String name, StructuredGraph graph) {
        phase.apply(graph);
        if (observer != null) {
            observer.printGraph(phase.getClass().getSimpleName(), graph);
        }
    }

    @HOSTED_ONLY
    private static StructuredGraph createTemplate(String name) {
        MaxRuntime runtime = runtime();
        StructuredGraph graph = new StructuredGraph();
        MethodActor template = findMethod(NativeStubCompiler.class, name, ClassMethodActor.class, String.class);

        if (observer != null) {
            observer.compilationStarted(name);
        }

        apply(new GraphBuilderPhase(runtime, template, null, false, true), name, graph);
        apply(new PhiSimplificationPhase(), name, graph);
        apply(new DeadCodeEliminationPhase(), name, graph);
        int nodeCount;
        do {
            nodeCount = graph.getNodeCount();
            apply(new FoldPhase(runtime), name, graph);
            apply(new CanonicalizerPhase(null, runtime, null), name, graph);
            apply(new MaxineIntrinsicsPhase(runtime), name, graph);
            apply(new MustInlinePhase(runtime, new HashMap<RiMethod, StructuredGraph>(), null), name, graph);
            apply(new DeadCodeEliminationPhase(), name, graph);
            apply(new WordTypeRewriterPhase(), name, graph);
            apply(new CanonicalizerPhase(null, runtime, null), name, graph);
            apply(new DeadCodeEliminationPhase(), name, graph);
        } while (graph.getNodeCount() != nodeCount);

        if (observer != null) {
            observer.compilationFinished(null);
        }

        return graph;
    }

    @HOSTED_ONLY
    private static IdealGraphPrinterObserver observer;
}
