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
package com.oracle.max.vm.ext.graal.snippets;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.ConstantParameter;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;


public class MaxMiscLowerings extends SnippetLowerings {

    @HOSTED_ONLY
    public MaxMiscLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription target,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(LoadExceptionObjectNode.class, new LoadExceptionObjectLowering(this));
        lowerings.put(UnwindNode.class, new UnwindLowering(this));
        lowerings.put(DeoptimizeNode.class, new DeoptimizeLowering(this));
        lowerings.put(NegativeArraySizeDeoptimizeNode.class, new MaxNASDeoptimizeLowering(this));
        lowerings.put(IllegalArgumentDeoptimizeNode.class, new MaxIADeoptimizeLowering(this));
    }

    /**
     * {@link DeoptimizeNode} handling.
     * Different behavior when compiling the boot image, when deoptimize isn't (generally) a good idea.
     * This does leave open the possibility of certain (runtime) exceptions not being caught (because Graal does
     * not even create the handlers), but that can be checked for statically.
     *
     * In the boot image, where possible, we throw the same exception that would have been thrown without deopt.
     * However, some relevant state is not available, e.g. for a {@link ClassCastException}.
     */
    protected class AbstractDeoptimizeLowering extends Lowering {

        @HOSTED_ONLY
        private SnippetInfo[] bootDeoptimizeSnippets;
        @HOSTED_ONLY
        private SnippetInfo bootThrowNegativeArraySizeExceptionSnippet;
        @HOSTED_ONLY
        private SnippetInfo bootThrowUnexpectedDeoptReasonExceptionSnippet;
        @HOSTED_ONLY
        private SnippetInfo bootThrowIllegalArgumentExceptionSnippet;

        @HOSTED_ONLY
        AbstractDeoptimizeLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets, "deoptimizeSnippet");
            bootThrowNegativeArraySizeExceptionSnippet = snippet(MaxMiscLowerings.class, "throwNegativeArraySizeExceptionSnippet");
            bootThrowUnexpectedDeoptReasonExceptionSnippet = snippet(MaxMiscLowerings.class, "throwUnexpectedDeoptReasonExceptionSnippet");
            bootThrowIllegalArgumentExceptionSnippet = snippet(MaxMiscLowerings.class, "throwIllegalArgumentExceptionSnippet");
            bootDeoptimizeSnippets = new SnippetInfo[DeoptimizationReason.values().length];
            for (DeoptimizationReason deoptimizationReason : DeoptimizationReason.values()) {
                SnippetInfo snippetInfo = null;
                // Checkstyle: stop
                switch (deoptimizationReason) {
                    case NullCheckException: snippetInfo = snippet(MaxMiscLowerings.class, "throwNullPointerExceptionSnippet"); break;
                    case ClassCastException: snippetInfo = snippet(MaxMiscLowerings.class, "throwClassCastExceptionSnippet"); break;
                    case ArithmeticException: snippetInfo = snippet(MaxMiscLowerings.class, "throwArithmeticExceptionSnippet"); break;
                    // Occurs a lot, probably should suppress whatever creates it in boot image code
                    case LoopLimitCheck: snippetInfo = snippet(MaxMiscLowerings.class, "throwLoopLimitCheckExceptionSnippet"); break;
                    default:
                }
                // Checkstyle: resume
                bootDeoptimizeSnippets[deoptimizationReason.ordinal()] = snippetInfo;
            }
        }

        public void lower(DeoptimizeNode node, LoweringTool tool) {
            DeoptimizationReason deoptimizationReason = node.getDeoptimizationReason();
            ClassMethodActor methodActor = (ClassMethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(node.graph().method());
            SnippetInfo snippetInfo = MaxGraal.bootCompile() ? bootDeoptimizeSnippets[deoptimizationReason.ordinal()] : snippet;
            Arguments args = null;
            if (MaxineVM.isHosted() && MaxGraal.bootCompile()) {
                // throw the (related) exception, with whatever state is known
                if (snippetInfo == null) {
                    if (node instanceof NegativeArraySizeDeoptimizeNode) {
                        NegativeArraySizeDeoptimizeNode maxNASDeoptimizeNode = (NegativeArraySizeDeoptimizeNode) node;
                        args = createAndAdd(bootThrowNegativeArraySizeExceptionSnippet, "length", maxNASDeoptimizeNode.length());
                    } else if (node instanceof IllegalArgumentDeoptimizeNode) {
                        args = new Arguments(bootThrowIllegalArgumentExceptionSnippet);
                    } else {
                        args = createAndAddConst(bootThrowUnexpectedDeoptReasonExceptionSnippet, "message", "unexpected deopt: " + deoptimizationReason.name());
                    }
                } else {
                    // one of the standard reasons (no state available currently)
                    args = new Arguments(snippetInfo);
                }
            } else {
                // In the normal case, there is no state to pass other than (this) methodActor to be deoptimized.
                args = createAndAddConst(snippetInfo, "methodActor", methodActor);
            }
            instantiate(node, args, tool);
        }

        private Arguments createAndAddConst(SnippetInfo snippetInfo, String name, Object value) {
            Arguments args = new Arguments(snippetInfo);
            args.addConst(name,  value);
            return args;
        }

        @HOSTED_ONLY
        private Arguments createAndAdd(SnippetInfo snippetInfo, String name, Object value) {
            Arguments args = new Arguments(snippetInfo);
            args.add(name,  value);
            return args;
        }
    }
    protected class DeoptimizeLowering extends AbstractDeoptimizeLowering implements LoweringProvider<DeoptimizeNode> {

        DeoptimizeLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets);
        }

        @Override
        public void lower(DeoptimizeNode node, LoweringTool tool) {
            super.lower(node, tool);
        }
    }

    protected class MaxNASDeoptimizeLowering extends AbstractDeoptimizeLowering implements LoweringProvider<NegativeArraySizeDeoptimizeNode> {

        MaxNASDeoptimizeLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets);
        }

        @Override
        public void lower(NegativeArraySizeDeoptimizeNode node, LoweringTool tool) {
            super.lower(node, tool);
        }
    }


    protected class MaxIADeoptimizeLowering extends AbstractDeoptimizeLowering implements LoweringProvider<IllegalArgumentDeoptimizeNode> {

        MaxIADeoptimizeLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets);
        }

        @Override
        public void lower(IllegalArgumentDeoptimizeNode node, LoweringTool tool) {
            super.lower(node, tool);
        }
    }


    /**
     * Called to explicitly deoptimize the given method.
     */
    @SNIPPET_SLOWPATH
    private static void deoptimize(ClassMethodActor methodActor) {
        TargetMethod tm = methodActor.currentTargetMethod();
        ArrayList<TargetMethod> tms = new ArrayList<>(1);
        tms.add(tm);
        new Deoptimization(tms).go();
        // on return it will all happen!
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void deoptimizeSnippet(@ConstantParameter ClassMethodActor methodActor) {
        deoptimize(methodActor);
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static NullPointerException throwNullPointerExceptionSnippet() {
        Throw.throwNullPointerException();
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static IllegalArgumentException throwIllegalArgumentExceptionSnippet() {
        Throw.throwIllegalArgumentException();
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void throwArithmeticExceptionSnippet() {
        Throw.throwArithmeticException();
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void throwNegativeArraySizeExceptionSnippet(int length) {
        Throw.throwNegativeArraySizeException(length);
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void throwUnexpectedDeoptReasonExceptionSnippet(@ConstantParameter String message) {
        throwUnexpectedDeoptReasonException(message);
        throw UnreachableNode.unreachable();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void throwLoopLimitCheckExceptionSnippet() {
        throwLoopLimitCheckException();
        throw UnreachableNode.unreachable();
    }

    private static class UnexpectedDeoptReasonException extends RuntimeException {
        UnexpectedDeoptReasonException(String message) {
            super(message);
        }
    }

    private static class LoopLimitCheckException extends RuntimeException {
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static ClassCastException throwClassCastExceptionSnippet() {
        throwClassCastException();
        throw UnreachableNode.unreachable();
    }

    @SNIPPET_SLOWPATH(exactType = true, nonNull = true)
    private static void throwLoopLimitCheckException() {
        throw new LoopLimitCheckException();
    }

    @SNIPPET_SLOWPATH(exactType = true, nonNull = true)
    private static void throwUnexpectedDeoptReasonException(String message) {
        throw new UnexpectedDeoptReasonException(message);
    }

    @SNIPPET_SLOWPATH(exactType = true, nonNull = true)
    private static void throwClassCastException() {
        throw new ClassCastException();
    }

    protected class LoadExceptionObjectLowering extends Lowering implements LoweringProvider<LoadExceptionObjectNode> {

        @HOSTED_ONLY
        LoadExceptionObjectLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets, "loadExceptionObjectSnippet");
        }

        @Override
        public void lower(LoadExceptionObjectNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Throwable loadExceptionObjectSnippet() {
        return loadExceptionForHandler();
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static Throwable loadExceptionForHandler() {
        // this aborts the VM if the stored exception object is null
        return VmThread.current().loadExceptionForHandler();
    }

    protected class UnwindLowering extends Lowering implements LoweringProvider<UnwindNode> {

        @HOSTED_ONLY
        UnwindLowering(MaxMiscLowerings miscSnippets) {
            super(miscSnippets, "throwExceptionSnippet");
        }

        @Override
        public void lower(UnwindNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("throwable", node.exception());
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void throwExceptionSnippet(Throwable throwable) {
        throwException(throwable);
        throw UnreachableNode.unreachable();
    }

    @SNIPPET_SLOWPATH
    private static void throwException(Throwable throwable) {
        Throw.raise(throwable);
    }


}
