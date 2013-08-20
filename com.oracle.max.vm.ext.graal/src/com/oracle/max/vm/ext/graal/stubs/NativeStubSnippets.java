/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.max.vm.ext.graal.nodes.NativeFunctionCallNode.*;
import static com.oracle.max.vm.ext.graal.nodes.NativeFunctionHandlesNode.*;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.ConstantParameter;
import com.oracle.graal.replacements.Snippet.VarargsParameter;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.graal.replacements.nodes.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jni.JniFunctions.JxxFunctionsLogger;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Snippets;
import com.sun.max.vm.thread.*;

/**
 * We use snippet technology to build the template graphs for the various forms of native stub calls.
 * Instantiation, however, is done partly manually, owing to the varying number of arguments and their types,
 * and the need to pass a {@link JniHandle} for an object type.
 *
 */
public class NativeStubSnippets extends SnippetLowerings {

    static StructuredGraph normalTemplate;
    static StructuredGraph synchronizedTemplate;
    static StructuredGraph cFunctionTemplate;
    static StructuredGraph noPrologueOrEpilogueTemplate;
    static InitializeHandlesLowering initializeHandlesLowering;
    static NativeFunctionCallLowering nativeFunctionCallLowering;

    /**
     * This policy produces exactly the same inlining as the old bytecode based implementation,
     * essentially for comparison purpose. Absent this, inlining proceeds to a significant depth
     * as all calls are inlined by default in snippets. This could only be short circuited by
     * more aggressive use of {@link SNIPPET_SLOWPATH}.
     */
    public static class StubSnippetInliningPolicy extends MaxSnippetInliningPolicy {

        @Override
        public boolean shouldInline(ResolvedJavaMethod method, ResolvedJavaMethod caller) {
            if (!super.shouldInline(method, caller)) {
                return false;
            }
            String name = method.getName();
            return !(name.equals("log") || name.equals("throwJniException") /*|| name.contains("nativeCall")*/);
        }
    }


    @HOSTED_ONLY
    public NativeStubSnippets(MetaAccessProvider runtime, Replacements replacements, TargetDescription target,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);

        for (Method method : getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Snippet.class)) {
                StructuredGraph graph = replacements.getSnippet(runtime.lookupJavaMethod(method));
                switch (method.getName()) {
                    case "template":
                        normalTemplate = graph;
                        break;
                    case "syncTemplate":
                        synchronizedTemplate = graph;
                        break;
                    case "templateC":
                        cFunctionTemplate = graph;
                        break;
                    case "templateNoPrologueOrEpilogue":
                        noPrologueOrEpilogueTemplate = graph;
                        break;
                    case "initializeHandlesSnippet":
                    case "nativeCallPrologueSnippet":
                    case "getJniHandleSnippet":
                    case "jniUnhandSnippet":
                        break;

                    default:
                        FatalError.unexpected("unexpected native stub snippet template: " + method.getName());
                }

            }
        }

    }

    @HOSTED_ONLY
    @Override
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(GetJniHandleNode.class, new GetJniHandleLowering(this));
        lowerings.put(JniUnhandNode.class, new JniUnhandLowering(this));
        lowerings.put(NativeFunctionHandlesNode.class, new InitializeHandlesLowering(this));
        lowerings.put(NativeFunctionCallNode.class, new NativeFunctionCallLowering());
    }

    /**
     * Stub template for a native method.
     */
    @Snippet(inlining = StubSnippetInliningPolicy.class)
    public static Object template(NativeFunction nativeFunction, MethodID methodId) throws Throwable {

        Pointer handles = initializeHandles();

        Address address = nativeFunction.link();
        VmThread thread = VmThread.current();

        if (JniFunctions.logger.enabled()) {
            logNativeCall(JxxFunctionsLogger.DOWNCALL_ENTRY, methodId);
        }

        int jniHandlesTop = thread.jniHandlesTop();

        Snippets.nativeCallPrologue(nativeFunction);
        Object result = nativeFunctionCall(address, handles, VmThread.jniEnv());
        Snippets.nativeCallEpilogue();

        thread.resetJniHandlesTop(jniHandlesTop);

        if (JniFunctions.logger.enabled()) {
            logNativeCall(JxxFunctionsLogger.DOWNCALL_EXIT, methodId);
        }

        thread.throwJniException();

        return result;
    }

    private static final int NativeMethodCallAsInt = JniFunctions.LogOperations.NativeMethodCall.ordinal();

    @SNIPPET_SLOWPATH
    private static void logNativeCall(Word mode, MethodID methodId) {
        JniFunctions.logger.log(NativeMethodCallAsInt, mode, methodId);
    }


    /**
     * Stub template for a synchronized native method.
     */
    @Snippet(inlining = StubSnippetInliningPolicy.class)
    public static synchronized Object syncTemplate(NativeFunction nativeFunction, MethodID methodId) throws Throwable {
        return template(nativeFunction, methodId);
    }

    /**
     * Stub template for a native method annotated with {@link C_FUNCTION}.
     */
    @Snippet(inlining = StubSnippetInliningPolicy.class)
    public static Object templateC(NativeFunction nativeFunction) {
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
    @Snippet(inlining = StubSnippetInliningPolicy.class)
    public static Object templateNoPrologueOrEpilogue(NativeFunction nativeFunction) {
        Pointer frame = VMRegister.getCpuStackPointer();
        Address address = nativeFunction.link();
        Snippets.nativeCallPrologueForC(nativeFunction);
        Object result = nativeFunctionCall(address, frame, VmThread.jniEnv());
        Snippets.nativeCallEpilogueForC();
        return result;
    }

    private static class LastFixedNodeThreadLocal extends ThreadLocal<FixedWithNextNode> {

    }

    private static final LastFixedNodeThreadLocal lastFixedNodeTL = new LastFixedNodeThreadLocal();

    abstract class NativeStubLowering extends Lowering {
        protected NativeStubLowering(NativeStubSnippets nativeStubSnippets, String methodName) {
            super(nativeStubSnippets, methodName);
        }

        protected NativeStubLowering() {
            super();
        }

        protected FixedWithNextNode lastFixedNode() {
            FixedWithNextNode r = lastFixedNodeTL.get();
            assert r != null;
            return r;
        }

        protected void setLastFixedNode(FixedWithNextNode val) {
            lastFixedNodeTL.set(val);
        }


        /**
         * Inserts a node into the control flow of the graph.
         */
        protected <T extends FixedWithNextNode> T insertNode(T node) {
            FixedWithNextNode lastFixedNode = lastFixedNode();
            FixedWithNextNode oldNext = (FixedWithNextNode) lastFixedNode.next();
            oldNext.replaceAtPredecessor(node);
            node.setNext(oldNext);
            lastFixedNode = node;
            return node;
        }
    }

    /**
     * Lowering for an {@link InvokeNode} to the method that sets up and initializes the
     * {@link JniHandle} array. There are always {@code N + 1} handle slots, where
     * {@code N} is the number of {@link Kind.Object} parameters. For a static method
     * the first handle is the {@link java.lang.Class} constant for the class containing
     * the method, otherwise it is the receiver object.
     *
     */
    class InitializeHandlesLowering extends NativeStubLowering implements LoweringProvider<NativeFunctionHandlesNode> {
        InitializeHandlesLowering(NativeStubSnippets nativeStubSnippets) {
            super(nativeStubSnippets, "initializeHandlesSnippet");
        }

        void handles(StructuredGraph graph, Arguments args, MethodActor nativeMethodActor, NodeList<ValueNode> nativeArgs) {
            // N.B. receiver is already in nativeArgs for non-static
            int handleCount = nativeMethodActor.isStatic() ? 1 : 0;
            for (int i = 0; i < nativeArgs.size(); i++) {
                ValueNode nativeArg = nativeArgs.get(i);
                if (nativeArg.kind() == Kind.Object) {
                    handleCount++;
                }
            }
            ValueNode[] nativeObjectArgs = new ValueNode[handleCount];
            int handleIndex = 0;
            if (nativeMethodActor.isStatic()) {
                nativeObjectArgs[0] = ConstantNode.forObject(nativeMethodActor.holder().toJava(), runtime, graph);
                handleIndex = 1;
            }
            for (int i = 0; i < nativeArgs.size(); i++) {
                ValueNode nativeArg = nativeArgs.get(i);
                if (nativeArg.kind() == Kind.Object) {
                    nativeObjectArgs[handleIndex++] = nativeArg;
                }
            }
            args.addConst("handleCount", handleCount);
            args.addVarargs("objectArgs", Object.class, StampFactory.forKind(Kind.Object), nativeObjectArgs);
        }

        @Override
        public void lower(NativeFunctionHandlesNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            handles(node.graph(), args, node.getNativeMethod(), node.arguments());
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = StubSnippetInliningPolicy.class)
    static Pointer initializeHandlesSnippet(@ConstantParameter int handleCount, @VarargsParameter Object[] objectArgs) {
        Pointer handles = Intrinsics.alloca(handleCount * Word.size(), true);
        // Snippet-style loop unrolling
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < handleCount; i++) {
            handles.writeObject(i * Word.size(), objectArgs[i]);
        }
        return handles;
    }

    /**
     * Lowering for an {@link InvokeNode} to the method ({@link #nativeFunctionCall}) that actually makes the native call,
     * creating {@link JNIHandle}s for any object parameters.
     * The {@link InvokeNode#callTarget()} arguments begin with the arguments to {@code nativeFunctionCall}, followed by the actual arguments
     * to the native call.
     *
     */
    class NativeFunctionCallLowering extends NativeStubLowering implements LoweringProvider<NativeFunctionCallNode> {

        NativeFunctionCallLowering() {
            super();
        }

        @Override
        public void lower(NativeFunctionCallNode node, LoweringTool tool) {
            if (node.specialized()) {
                // This is the "normal" lowering call, and there is nothing to do.
                return;
            }
            StructuredGraph graph = node.graph();
            MethodActor nativeMethodActor = node.getNativeMethod();
            boolean isCFunction = nativeMethodActor.isCFunction();
            boolean isStatic = nativeMethodActor.isStatic();

            NodeList<ValueNode> callArgs = node.arguments();

            setLastFixedNode((FixedWithNextNode) node.predecessor());

            // Can't make the actual call via a snippet, so have to do manual graph creation
            List<ValueNode> fcnArgsList = new ArrayList<>(callArgs.size() + 1);

            ValueNode function = callArgs.get(0);
            ValueNode handleBase = callArgs.get(1);
            ValueNode jniEnv = callArgs.get(2);

            int handleOffset = 0;
            int argIndex = 3;

            if (!isCFunction) {
                fcnArgsList.add(jniEnv);

                if (isStatic) {
                    // Load the class for a static method
                    fcnArgsList.add(insertNode(graph.add(new GetJniHandleNode(handleBase, handleOffset,
                                    ConstantNode.forObject(nativeMethodActor.holder().toJava(), runtime, graph)))));
                } else {
                    // Load the receiver for a non-static method
                    fcnArgsList.add(insertNode(graph.add(new GetJniHandleNode(handleBase, handleOffset, callArgs.get(argIndex++)))));
                }
                handleOffset += Word.size();
            }

            // Load remaining arguments, creating handles for objects
            while (argIndex < callArgs.size()) {
                ValueNode arg = callArgs.get(argIndex++);
                if (arg.kind() == Kind.Object) {
                    fcnArgsList.add(insertNode(graph.add(new GetJniHandleNode(handleBase, handleOffset, arg))));
                    handleOffset += Word.size();
                } else {
                    fcnArgsList.add(arg);
                }
            }

            Debug.dump(node.graph(), "After Args");


            ResolvedJavaMethod nativeMethod = MaxResolvedJavaMethod.get(nativeMethodActor);
            ResolvedJavaType returnType = (ResolvedJavaType) nativeMethod.getSignature().getReturnType(
                            MaxResolvedJavaType.get(nativeMethodActor.holder()));
            Kind returnKind = MaxWordType.checkWord(returnType);

            node.updateCall(function, fcnArgsList, returnKind);
            Stamp returnStamp = returnKind == Kind.Object ? StampFactory.declared(returnType) : StampFactory.forKind(returnKind);
            node.setStateAfter(graph.add(new FrameState(FrameState.INVALID_FRAMESTATE_BCI)));
            setLastFixedNode(node);

            // Sign extend or zero the upper bits of a return value smaller than an int to
            // preserve the invariant that all such values are represented by an int
            // in the VM. We cannot rely on the native C compiler doing this for us.
            // Also, an object return value must be un-handlized
            // *before* the JNI frame is restored.
            ValueNode callResult = node;

            switch (returnKind) {
                case Boolean:
                case Byte: {
                    callResult = graph.unique(new ConvertNode(ConvertNode.Op.I2B, callResult));
                    break;
                }
                case Short: {
                    callResult = graph.unique(new ConvertNode(ConvertNode.Op.I2S, callResult));
                    break;
                }
                case Char: {
                    callResult = graph.unique(new ConvertNode(ConvertNode.Op.I2C, callResult));
                    break;
                }
                case Object: {
                    assert !isCFunction;
                    callResult = insertNode(graph.add(new JniUnhandNode(returnStamp, callResult)));
                    break;
                }
                case Void: {
                    break;
                }
            }

            Debug.dump(node.graph(), "After Lowering");

            if (node != callResult) {
                // change return to use modified callResult
                for (Node usage : node.usages()) {
                    if (usage instanceof ReturnNode) {
                        usage.replaceAndDelete(graph.add(new ReturnNode(callResult)));
                        break;
                    }
                }
            }

        }

    }

    private class GetJniHandleLowering extends Lowering implements LoweringProvider<GetJniHandleNode> {

        GetJniHandleLowering(NativeStubSnippets nativeStubSnippets) {
            super(nativeStubSnippets, "getJniHandleSnippet");
        }

        @Override
        public void lower(GetJniHandleNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("handleBase", node.handleBase());
            args.add("offset", node.offset());
            args.add("value", node.value());
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = StubSnippetInliningPolicy.class)
    private static Pointer getJniHandleSnippet(Pointer handleBase, int offset, Object value) {
        return JniHandles.getHandle(handleBase, offset, value);
    }

    private class JniUnhandLowering extends Lowering implements LoweringProvider<JniUnhandNode> {

        JniUnhandLowering(NativeStubSnippets nativeStubSnippets) {
            super(nativeStubSnippets, "jniUnhandSnippet");
        }

        @Override
        public void lower(JniUnhandNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("handle", node.handle());
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = StubSnippetInliningPolicy.class)
    private static Object jniUnhandSnippet(JniHandle handle) {
        return JniHandles.get(handle);
    }


}
