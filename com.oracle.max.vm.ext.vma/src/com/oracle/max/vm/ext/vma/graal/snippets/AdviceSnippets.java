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
package com.oracle.max.vm.ext.vma.graal.snippets;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.bytecode.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.ConstantParameter;
import com.oracle.graal.replacements.Snippet.VarargsParameter;
import com.oracle.graal.replacements.SnippetTemplate.Arguments;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.graal.replacements.nodes.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.graal.nodes.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Snippets to handle the lowering of {@link AdviceNode}s. Since the former is generic, there is
 * a separate level of indirection in here to delegate to a class that handles the specific node
 * being advised.
 */
public class AdviceSnippets extends SnippetLowerings {
    private Map<NodeClass, AdviceLowering> adviceLowerings = new HashMap<NodeClass, AdviceLowering>();
    private SnippetInfo noopSnippet;

    @HOSTED_ONLY
    public AdviceSnippets(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
        noopSnippet = snippet(AdviceSnippets.class, "noopSnippet");
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(AdviceNode.class, new GenericAdviceLowering());
        adviceLowerings.put(NodeClass.get(StartNode.class), new StartAdviceLowering());

        LoadFieldAdviceLowering loadFieldAdviceLowering = new LoadFieldAdviceLowering();
        adviceLowerings.put(NodeClass.get(LoadFieldNode.class), loadFieldAdviceLowering);
        StoreFieldAdviceLowering storeFieldAdviceLowering = new StoreFieldAdviceLowering();
        adviceLowerings.put(NodeClass.get(StoreFieldNode.class), storeFieldAdviceLowering);
        adviceLowerings.put(NodeClass.get(UnresolvedLoadFieldNode.class), new UnresolvedLoadFieldAdviceLowering(loadFieldAdviceLowering));
        adviceLowerings.put(NodeClass.get(UnresolvedStoreFieldNode.class), new UnresolvedStoreFieldAdviceLowering(storeFieldAdviceLowering));
        adviceLowerings.put(NodeClass.get(LoadIndexedNode.class), new LoadIndexedAdviceLowering());
        adviceLowerings.put(NodeClass.get(StoreIndexedNode.class), new StoreIndexedAdviceLowering());
        adviceLowerings.put(NodeClass.get(ReturnNode.class), new ReturnAdviceLowering());
        adviceLowerings.put(NodeClass.get(NewInstanceNode.class), new NewInstanceAdviceLowering());
        adviceLowerings.put(NodeClass.get(NewArrayNode.class), new NewArrayAdviceLowering());
        adviceLowerings.put(NodeClass.get(NewMultiArrayNode.class), new NewMultiArrayAdviceLowering());
        adviceLowerings.put(NodeClass.get(ArrayLengthNode.class), new ArrayLengthAdviceLowering());
        adviceLowerings.put(NodeClass.get(InvokeWithExceptionNode.class), new InvokeAdviceLowering());
        adviceLowerings.put(NodeClass.get(IfNode.class), new IfAdviceLowering());
    }

    protected class GenericAdviceLowering extends Lowering implements LoweringProvider<AdviceNode> {

        public void lower(AdviceNode node, LoweringTool tool) {
            AdviceMode adviceMode = node.adviceMode();
            Node advisedNode = adviceMode == AdviceMode.AFTER ? node.predecessor() : node.successors().first();
            AdviceLowering adviceLowering = adviceLowerings.get(advisedNode.getNodeClass());
            Arguments args = adviceLowering.lower(node, advisedNode);
            instantiate(node, args, tool);
        }

    }

    private abstract class AdviceLowering {

        abstract Arguments lower(AdviceNode adviceNode, Node advisedNode);

        protected String kindTag(Kind kind) {
            String s = null;
            switch (kind) {
                case Int: case Boolean: case Byte: case Short: case Char: case Long:
                    s = "Long";
                    break;
                case Float: case Double: case Object:
                    s = kind.name();
            }
            return s;
        }

        protected String adviceModeString(AdviceMode adviceMode) {
            return adviceMode == AdviceMode.BEFORE ? "Before" : "After";
        }

        protected Arguments createArgsAndSetBci(Node advisedNode, SnippetInfo snippetInfo) {
            Arguments args = new Arguments(snippetInfo);
            args.add("arg1", 0); // TODO get this right (as best we can)
            return args;
        }
    }

    private class StartAdviceLowering extends AdviceLowering {
        private SnippetInfo snippet = snippet(AdviceSnippets.class, "adviseAfterMethodEntrySnippet");

        @Override
        Arguments lower(AdviceNode node, Node advisedNode) {
            StructuredGraph graph = node.graph();
            ResolvedJavaMethod method = graph.method();
            LocalNode receiver = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                // find LocalNode 0
                for (Node n : graph.getNodes().filter(LocalNode.class)) {
                    LocalNode ln = (LocalNode) n;
                    if (ln.index() == 0) {
                        receiver = ln;
                        break;
                    }
                }
            }
            Arguments args = createArgsAndSetBci(advisedNode, snippet);
            args.add("arg2", receiver);
            args.add("arg3", ConstantNode.forObject(MaxResolvedJavaMethod.getRiResolvedMethod(method), runtime, graph));
            return args;
        }
    }

    private abstract class NewAdviceLowering extends AdviceLowering {
        SnippetInfo[] snippets = new SnippetInfo[AdviceMode.values().length];

        @HOSTED_ONLY
        NewAdviceLowering() {
            snippets[AdviceMode.BEFORE.ordinal()] = noopSnippet;
            String variant = this instanceof NewInstanceAdviceLowering ? "New" : this instanceof NewArrayAdviceLowering ? "NewArray" : "MultiNewArray";
            snippets[AdviceMode.AFTER.ordinal()] = snippet(AdviceSnippets.class, "adviseAfter" + variant + "Snippet");
        }

        @Override
        Arguments lower(AdviceNode node, Node advisedNode) {
            Arguments args = createArgsAndSetBci(node, snippets[node.adviceMode().ordinal()]);
            args.add("arg2", advisedNode);
            return args;
        }
    }

    private class NewInstanceAdviceLowering extends NewAdviceLowering {
    }

    private class NewArrayAdviceLowering extends NewAdviceLowering {
        @Override
        Arguments lower(AdviceNode node, Node advisedNode) {
            Arguments args = super.lower(node, advisedNode);
            args.add("arg3", ((NewArrayNode) advisedNode).length());
            return args;
        }
    }

    private class NewMultiArrayAdviceLowering extends NewAdviceLowering {
        @Override
        Arguments lower(AdviceNode node, Node advisedNode) {
            Arguments args = super.lower(node, advisedNode);
            // The advice method wants an actual array of int, which has to be created
            NewSnippets.dimensions(args, ((NewMultiArrayNode) advisedNode).dimensions());
            return args;
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterMultiNewArraySnippet(int arg1, Object arg2, @ConstantParameter int rank, @VarargsParameter int[] dimensions) {
        int[] dims = new int[rank];
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < rank; i++) {
            dims[i] = dimensions[i];
        }
        VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(arg1, arg2, dims);
    }

    private class ArrayLengthAdviceLowering extends AdviceLowering {
        private final SnippetInfo[] snippets = new SnippetInfo[AdviceMode.values().length];

        @HOSTED_ONLY
        ArrayLengthAdviceLowering() {
            snippets[AdviceMode.AFTER.ordinal()] = snippet(AdviceSnippets.class, "adviseAfterArrayLengthSnippet");
            snippets[AdviceMode.BEFORE.ordinal()] = noopSnippet;
        }

        @Override
        Arguments lower(AdviceNode adviceNode, Node advisedNode) {
            SnippetInfo snippetInfo = snippets[adviceNode.adviceMode().ordinal()];
            Arguments args = createArgsAndSetBci(advisedNode, snippetInfo);
            if (snippetInfo == noopSnippet) {
                return new Arguments(noopSnippet);
            }
            ArrayLengthNode arrayLengthNode = (ArrayLengthNode) advisedNode;
            args.add("arg2", arrayLengthNode.array());
            args.add("arg3", arrayLengthNode);
            return args;
        }
    }

    private class FieldAdviceLowering extends AdviceLowering {
        protected final SnippetInfo[][][] snippets = new SnippetInfo[2][AdviceMode.values().length][Kind.values().length];

        @HOSTED_ONLY
        FieldAdviceLowering() {
            boolean get = this instanceof LoadFieldAdviceLowering;
            String accessMode = get ? "Get" : "Put";
            boolean isStatic = false;
            while (true) {
                for (AdviceMode adviceMode : AdviceMode.values()) {
                    String adviceModeString = adviceModeString(adviceMode);
                    for (Kind kind : Kind.values()) {
                        if (adviceMode == AdviceMode.AFTER) {
                            continue;
                        }
                        String s = kindTag(kind);
                        if (s != null) {
                            String methodName = "advise" + adviceModeString + accessMode + (isStatic ? "Static" : "Field") +
                                            (get ? "" : s) + "Snippet";
                            setSnippet(adviceMode, isStatic, kind, snippet(AdviceSnippets.class, methodName));
                        }
                    }
                }
                if (isStatic) {
                    break;
                } else {
                    isStatic = true;
                }
            }
        }

        void setSnippet(AdviceMode adviceMode, boolean isStatic, Kind kind, SnippetInfo snippet) {
            snippets[booleanOrd(isStatic)][adviceMode.ordinal()][kind.ordinal()] = snippet;
        }

        private SnippetInfo getSnippet(AdviceMode adviceMode, boolean isStatic, Kind kind) {
            return snippets[booleanOrd(isStatic)][adviceMode.ordinal()][kind.ordinal()];
        }

        private int booleanOrd(boolean b) {
            return b ? 1 : 0;
        }

        @Override
        Arguments lower(AdviceNode adviceNode, Node node) {
            AccessFieldNode fieldNode = (AccessFieldNode) node;
            FieldActor fieldActor = (FieldActor) MaxResolvedJavaField.getRiResolvedField(fieldNode.field());
            SnippetInfo snippetInfo = getSnippet(adviceNode.adviceMode(), fieldNode.isStatic(), fieldNode.field().getKind());
            if (snippetInfo == null) {
                return new Arguments(noopSnippet);
            }
            Arguments args = createArgsAndSetBci(node, snippetInfo);
            args.add("arg2", fieldNode.isStatic() ? fieldActor.holder().staticTuple() : fieldNode.object());
            args.add("arg3", fieldActor);
            storeFieldArg(fieldNode, args);
            return args;
        }

        protected void storeFieldArg(AccessFieldNode fieldNode, Arguments args) {
        }
    }

    private class LoadFieldAdviceLowering extends FieldAdviceLowering {

    }

    private class StoreFieldAdviceLowering extends FieldAdviceLowering {
        @Override
        protected void storeFieldArg(AccessFieldNode fieldNode, Arguments args) {
            args.add("arg4", ((StoreFieldNode) fieldNode).value());
        }

    }

    private class UnresolvedFieldAdviceLowering extends AdviceLowering {
        private final FieldAdviceLowering fieldAdviceLowering;

        UnresolvedFieldAdviceLowering(FieldAdviceLowering fieldAdviceLowering) {
            this.fieldAdviceLowering = fieldAdviceLowering;
        }

        @Override
        Arguments lower(AdviceNode adviceNode, Node node) {
            UnresolvedAccessFieldNode fieldNode = (UnresolvedAccessFieldNode) node;
            SnippetInfo snippetInfo = fieldAdviceLowering.getSnippet(adviceNode.adviceMode(), fieldNode.isStatic(), fieldNode.javaField().getKind());
            debug(snippetInfo);
            if (snippetInfo == null) {
                return new Arguments(noopSnippet);
            }
            Arguments args = createArgsAndSetBci(node, snippetInfo);
            args.add("arg2", fieldNode.isStatic() ? null : fieldNode.object());
            args.add("arg3", fieldNode.resolvedFieldActor());
            storeFieldArg(fieldNode, args);
            return args;
        }

        @NEVER_INLINE
        private void debug(SnippetInfo s) {  }

        protected void storeFieldArg(UnresolvedAccessFieldNode fieldNode, Arguments args) {
        }
    }

    private class UnresolvedLoadFieldAdviceLowering extends UnresolvedFieldAdviceLowering {

        UnresolvedLoadFieldAdviceLowering(FieldAdviceLowering fieldAdviceLowering) {
            super(fieldAdviceLowering);
        }

    }

    private class UnresolvedStoreFieldAdviceLowering extends UnresolvedFieldAdviceLowering {
        UnresolvedStoreFieldAdviceLowering(FieldAdviceLowering fieldAdviceLowering) {
            super(fieldAdviceLowering);
        }

        @Override
        protected void storeFieldArg(UnresolvedAccessFieldNode fieldNode, Arguments args) {
            args.add("arg4", ((UnresolvedStoreFieldNode) fieldNode).value());
        }

    }

    private abstract class IndexedAdviceLowering extends AdviceLowering {
        protected final SnippetInfo[][] snippets = new SnippetInfo[AdviceMode.values().length][Kind.values().length];

        @HOSTED_ONLY
        IndexedAdviceLowering() {
            boolean load = this instanceof LoadIndexedAdviceLowering;
            String accessMode = load ? "Load" : "Store";
            for (AdviceMode adviceMode : AdviceMode.values()) {
                String adviceModeString = adviceModeString(adviceMode);
                for (Kind kind : Kind.values()) {
                    setSnippet(adviceMode, kind, noopSnippet);
                    if (adviceMode == AdviceMode.AFTER) {
                        continue;
                    }
                    String s = kindTag(kind);
                    if (s != null) {
                        String methodName = "advise" + adviceModeString + "Array" + accessMode + (load ? "" : s) + "Snippet";
                        setSnippet(adviceMode, kind, snippet(AdviceSnippets.class, methodName));
                    }
                }
            }
        }

        void setSnippet(AdviceMode adviceMode, Kind kind, SnippetInfo snippet) {
            snippets[adviceMode.ordinal()][kind.ordinal()] = snippet;
        }

        private SnippetInfo getSnippet(AdviceMode adviceMode, Kind kind) {
            return snippets[adviceMode.ordinal()][kind.ordinal()];
        }


        @Override
        Arguments lower(AdviceNode adviceNode, Node advisedNode) {
            AccessIndexedNode indexedNode = (AccessIndexedNode) advisedNode;
            SnippetInfo snippetInfo = getSnippet(adviceNode.adviceMode(), indexedNode.elementKind());
            if (snippetInfo == noopSnippet) {
                return new Arguments(noopSnippet);
            }
            Arguments args = createArgsAndSetBci(adviceNode, snippetInfo);
            args.add("arg2", indexedNode.array());
            args.add("arg3", indexedNode.index());
            storeIndexedArg(indexedNode, args);
            return args;
        }

        protected void storeIndexedArg(AccessIndexedNode indexedNode, Arguments args) {
        }
    }

    private class LoadIndexedAdviceLowering extends IndexedAdviceLowering {

    }

    private class StoreIndexedAdviceLowering extends IndexedAdviceLowering {
        @Override
        protected void storeIndexedArg(AccessIndexedNode indexedNode, Arguments args) {
            args.add("arg4", ((StoreIndexedNode) indexedNode).value());
        }

    }

    private class InvokeAdviceLowering extends AdviceLowering {
        protected final SnippetInfo[][] snippets = new SnippetInfo[AdviceMode.values().length][InvokeKind.values().length];

        @HOSTED_ONLY
        InvokeAdviceLowering() {
            for (AdviceMode adviceMode : AdviceMode.values()) {
                SnippetInfo snippet;
                String adviceModeString = adviceModeString(adviceMode);
                for (InvokeKind invokeKind : InvokeKind.values()) {
                    String name = "advise" + adviceModeString + "Invoke" + invokeKind.name() + "Snippet";
                    if (adviceMode == AdviceMode.AFTER) {
                        snippet = noopSnippet;
                    } else {
                        snippet = snippet(AdviceSnippets.class, name);
                    }
                    snippets[adviceMode.ordinal()][invokeKind.ordinal()] = snippet;
                }
            }
        }

        @Override
        Arguments lower(AdviceNode adviceNode, Node advisedNode) {
            Invoke invoke = (Invoke) advisedNode;
            MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
            SnippetInfo snippetInfo = snippets[adviceNode.adviceMode().ordinal()][callTarget.invokeKind().ordinal()];
            Arguments args = createArgsAndSetBci(advisedNode, snippetInfo);
            args.add("arg2", callTarget.invokeKind() == InvokeKind.Static ? null : callTarget.receiver());
            args.add("arg3", ConstantNode.forObject(MaxResolvedJavaMethod.getRiResolvedMethod(callTarget.targetMethod()), runtime, adviceNode.graph()));
            return args;
        }

    }

    private class ReturnAdviceLowering extends AdviceLowering {
        private SnippetInfo[] snippets = new SnippetInfo[Kind.values().length];

        @HOSTED_ONLY
        ReturnAdviceLowering() {
            for (Kind kind : Kind.values()) {
                String s = kind == Kind.Void ? "" : kindTag(kind);
                if (s != null) {
                    snippets[kind.ordinal()] = snippet(AdviceSnippets.class, "adviseBeforeReturn" + (s == null ? "" : s) + "Snippet");
                }
            }
        }

        @Override
        Arguments lower(AdviceNode node, Node advisedNode) {
            ReturnNode returnNode = (ReturnNode) advisedNode;
            ValueNode result = returnNode.result();
            SnippetInfo snippetInfo;
            if (result == null) {
                snippetInfo = snippets[Kind.Void.ordinal()];
            } else {
                snippetInfo = snippets[result.kind().ordinal()];
            }
            Arguments args = createArgsAndSetBci(returnNode, snippetInfo);
            if (result != null) {
                args.add("arg2", result);
            }
            return args;
        }
    }

    private class IfAdviceLowering extends AdviceLowering {
        private final SnippetInfo intSnippet = snippet(AdviceSnippets.class, "adviseBeforeIfIntSnippet");
        private final SnippetInfo objSnippet = snippet(AdviceSnippets.class, "adviseBeforeIfObjectSnippet");

        @Override
        Arguments lower(AdviceNode adviceNode, Node advisedNode) {
            IfNode ifNode = (IfNode) advisedNode;
            LogicNode condition = ifNode.condition();
            // The condition has already been canonicalized to some extent, so the original bytecode likely is lost to us.
            // In particular the IFxx IF_ICMPxx distinction is lost
            SnippetInfo snippetInfo = (condition instanceof ObjectEqualsNode || condition instanceof IsNullNode) ? objSnippet : intSnippet;
            Arguments args = createArgsAndSetBci(advisedNode, snippetInfo);
            if (condition instanceof IsNullNode) {
                args.add("arg2", Bytecodes.IFNULL);
                args.add("arg3", ((IsNullNode) condition).object());
                args.add("arg4", null);
            } else {
                CompareNode compareNode = (CompareNode) condition;
                args.add("arg2", bytecode(compareNode));
                args.add("arg3", compareNode.x());
                args.add("arg4", compareNode.y());
            }
            // don't have a bci for either successor
            args.add("arg5", -1);
            return args;
        }

        private int bytecode(CompareNode compareNode) {
            if (compareNode instanceof ObjectEqualsNode || compareNode instanceof IntegerEqualsNode) {
                return Bytecodes.IF_ICMPEQ;
            } else if (compareNode instanceof IntegerLessThanNode || compareNode instanceof IntegerBelowThanNode) {
                return Bytecodes.IF_ICMPLT;
            } else {
                assert false;
                return -1;
            }
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void noopSnippet() {
    }


// START GENERATED CODE
// EDIT AND RUN VMASnippetsGenerator.main() TO MODIFY

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeIfObjectSnippet(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        VMAStaticBytecodeAdvice.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeIfIntSnippet(int arg1, int arg2, int arg3, int arg4, int arg5) {
        VMAStaticBytecodeAdvice.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeGetStaticSnippet(int arg1, Object arg2, FieldActor arg3) {
        if (arg2 == null) {
            arg2 = arg3.holder().staticTuple();
        }
        VMAStaticBytecodeAdvice.adviseBeforeGetStatic(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutStaticObjectSnippet(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        if (arg2 == null) {
            arg2 = arg3.holder().staticTuple();
        }
        VMAStaticBytecodeAdvice.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutStaticFloatSnippet(int arg1, Object arg2, FieldActor arg3, float arg4) {
        if (arg2 == null) {
            arg2 = arg3.holder().staticTuple();
        }
        VMAStaticBytecodeAdvice.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutStaticDoubleSnippet(int arg1, Object arg2, FieldActor arg3, double arg4) {
        if (arg2 == null) {
            arg2 = arg3.holder().staticTuple();
        }
        VMAStaticBytecodeAdvice.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutStaticLongSnippet(int arg1, Object arg2, FieldActor arg3, long arg4) {
        if (arg2 == null) {
            arg2 = arg3.holder().staticTuple();
        }
        VMAStaticBytecodeAdvice.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutFieldLongSnippet(int arg1, Object arg2, FieldActor arg3, long arg4) {
        VMAStaticBytecodeAdvice.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutFieldFloatSnippet(int arg1, Object arg2, FieldActor arg3, float arg4) {
        VMAStaticBytecodeAdvice.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutFieldObjectSnippet(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        VMAStaticBytecodeAdvice.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforePutFieldDoubleSnippet(int arg1, Object arg2, FieldActor arg3, double arg4) {
        VMAStaticBytecodeAdvice.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeInvokeVirtualSnippet(int arg1, Object arg2, MethodActor arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeInvokeSpecialSnippet(int arg1, Object arg2, MethodActor arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeInvokeStaticSnippet(int arg1, Object arg2, MethodActor arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeInvokeInterfaceSnippet(int arg1, Object arg2, MethodActor arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeThrowSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeThrow(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeCheckCastSnippet(int arg1, Object arg2, Object arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeCheckCast(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeInstanceOfSnippet(int arg1, Object arg2, Object arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeInstanceOf(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeMonitorEnterSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeMonitorEnter(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeMonitorExitSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterLoadSnippet(int arg1, int arg2, Object arg3) {
        VMAStaticBytecodeAdvice.adviseAfterLoad(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterArrayLoadSnippet(int arg1, Object arg2, int arg3, Object arg4) {
        VMAStaticBytecodeAdvice.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeLoadSnippet(int arg1, int arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeLoad(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeStoreSnippet(int arg1, int arg2, long arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeStoreSnippet(int arg1, int arg2, Object arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeStoreSnippet(int arg1, int arg2, double arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeStoreSnippet(int arg1, int arg2, float arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConstLoadSnippet(int arg1, double arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeConstLoad(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConstLoadSnippet(int arg1, long arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeConstLoad(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConstLoadSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeConstLoad(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConstLoadSnippet(int arg1, float arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeConstLoad(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeArrayLoadSnippet(int arg1, Object arg2, int arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeArrayStoreObjectSnippet(int arg1, Object arg2, int arg3, Object arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeArrayStoreDoubleSnippet(int arg1, Object arg2, int arg3, double arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeArrayStoreFloatSnippet(int arg1, Object arg2, int arg3, float arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeArrayStoreLongSnippet(int arg1, Object arg2, int arg3, long arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeStackAdjustSnippet(int arg1, int arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeOperationSnippet(int arg1, int arg2, double arg3, double arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeOperationSnippet(int arg1, int arg2, float arg3, float arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeOperationSnippet(int arg1, int arg2, long arg3, long arg4) {
        VMAStaticBytecodeAdvice.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConversionSnippet(int arg1, int arg2, double arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConversionSnippet(int arg1, int arg2, float arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeConversionSnippet(int arg1, int arg2, long arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeGotoSnippet(int arg1, int arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeGoto(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeReturnFloatSnippet(int arg1, float arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeReturn(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeReturnDoubleSnippet(int arg1, double arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeReturn(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeReturnObjectSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeReturn(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeReturnSnippet(int arg1) {
        VMAStaticBytecodeAdvice.adviseBeforeReturn(arg1);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeReturnLongSnippet(int arg1, long arg2) {
        VMAStaticBytecodeAdvice.adviseBeforeReturn(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseBeforeGetFieldSnippet(int arg1, Object arg2, FieldActor arg3) {
        VMAStaticBytecodeAdvice.adviseBeforeGetField(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterNewSnippet(int arg1, Object arg2) {
        VMAStaticBytecodeAdvice.adviseAfterNew(arg1, arg2);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterNewArraySnippet(int arg1, Object arg2, int arg3) {
        VMAStaticBytecodeAdvice.adviseAfterNewArray(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterArrayLengthSnippet(int arg1, Object arg2, int arg3) {
        VMAStaticBytecodeAdvice.adviseAfterArrayLength(arg1, arg2, arg3);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void adviseAfterMethodEntrySnippet(int arg1, Object arg2, MethodActor arg3) {
        VMAStaticBytecodeAdvice.adviseAfterMethodEntry(arg1, arg2, arg3);
    }

// END GENERATED CODE


}
