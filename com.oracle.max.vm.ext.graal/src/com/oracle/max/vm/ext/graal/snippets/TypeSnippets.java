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
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.spi.Lowerable.LoweringType;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Snippets;


public class TypeSnippets extends SnippetLowerings {

    @HOSTED_ONLY
    public TypeSnippets(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(MetaAccessProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(CheckCastNode.class, new CheckCastLowering(this));
        lowerings.put(UnresolvedCheckCastNode.class, new UnresolvedCheckCastLowering(this));
        lowerings.put(InstanceOfNode.class, new InstanceOfLowering(runtime, replacements, targetDescription));
        lowerings.put(UnresolvedInstanceOfNode.class, new UnresolvedInstanceOfLowering(this));
        lowerings.put(UnresolvedLoadConstantNode.class, new UnresolvedLoadConstantLowering(this));
        lowerings.put(UnresolvedLoadClassActorNode.class, new UnresolvedLoadClassActorLowering(this));
        lowerings.put(FixedGuardNode.class, new FixedGuardLowering(this));
        lowerings.put(FixedGuardNode.class, new FixedGuardLowering(this));
    }

    protected class FixedGuardLowering extends Lowering implements LoweringProvider<FixedGuardNode> {

        SnippetInfo[] snippets = new SnippetInfo[DeoptimizationReason.values().length];

        @HOSTED_ONLY
        FixedGuardLowering(TypeSnippets typeSnippets) {
            super();
            for (DeoptimizationReason r : DeoptimizationReason.values()) {
                SnippetInfo snippetInfo = null;
                switch (r) {
                    case NullCheckException:
                        snippetInfo = snippet(TypeSnippets.class, "nullCheckSnippet");
                        break;
                    case ClassCastException:
                        snippetInfo = snippet(TypeSnippets.class, "classCastCheckSnippet");
                        break;
                    default:

                }
                snippets[r.ordinal()] = snippetInfo;
            }
        }

        @Override
        public void lower(FixedGuardNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippets[node.getReason().ordinal()]);
            if (node.usages().isNotEmpty()) {
                // TODO Figure this out properly. This was originally put in because PushThroughPiPhase
                // was suppressed, and the instantiate failed without the removal. The phase now runs
                // however, it fails to remove the Pi on some ReadNodes. It appears to be harmless to
                // remove the PiNode, but it shouldn't be necessary. Also FixedGuardNode fails if we
                // default to deoptimization for NullChecks because other node types are in the usages.
                //checkForPiNode(node);
            }
            switch (node.getReason()) {
                case NullCheckException:
                    if (tool.getLoweringType() == LoweringType.BEFORE_GUARDS) {
                        return;
                    }
                    break;
                case ClassCastException:
                    if (tool.getLoweringType() == LoweringType.BEFORE_GUARDS) {
                        return;
                    }
                    break;

                default:
                    FatalError.unexpected("FixedGuardLowering: Unexpected reason: " + node.getReason());
            }
            ValueNode trueValue = ConstantNode.forBoolean(node.isNegated(), node.graph());
            ValueNode falseValue = ConstantNode.forBoolean(!node.isNegated(), node.graph());
            args.add("cond", node.graph().unique(new ConditionalNode(node.condition(), trueValue, falseValue)));
            instantiate(node, args, tool);
        }

        public void checkForPiNode(FixedGuardNode fixedGuardNode) {
            Node usage = getSingleUsage(fixedGuardNode);
            if (usage.getClass() ==  PiNode.class) {
                PiNode pi = (PiNode) usage;
                fixedGuardNode.graph().replaceFloating(pi, pi.object());
            } else {
                assert false;
            }

        }

        private Node getSingleUsage(Node node) {
            List<Node> usages = node.usages().snapshot();
            assert usages.size() == 1;
            return usages.get(0);
        }


    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void nullCheckSnippet(boolean cond) {
        if (cond) {
            Throw.throwNullPointerException();
            throw UnreachableNode.unreachable();
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void classCastCheckSnippet(boolean cond) {
        if (cond) {
            throwClassCastException();
            //Throw.throwClassCastException(classActor, object);
            throw UnreachableNode.unreachable();
        }
    }

    @SNIPPET_SLOWPATH
    private static void throwClassCastException() {
        throw new ClassCastException();
    }

    protected abstract class UnresolvedTypeLowering extends Lowering {

        protected UnresolvedTypeLowering(SnippetLowerings snippets, String string) {
            super(snippets, string);
        }

        protected Arguments createGuard(UnresolvedTypeNode node) {
            UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) MaxJavaType.getRiType(node.javaType());
            ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
            return new Arguments(snippet).add("guard", guard);
        }
    }

    protected class CheckCastLowering extends Lowering implements LoweringProvider<CheckCastNode> {
        CheckCastLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "checkCastSnippet");
        }

        @Override
        public void lower(CheckCastNode node, LoweringTool tool) {
            ClassActor classActor = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.type());
            Arguments args = new Arguments(snippet);
            //boolean checkNull = !node.object().stamp().nonNull(); TODO use this
            args.add("classActor", classActor);
            args.add("object", node.object());
            instantiate(node, args, tool);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Object checkCastSnippet(ClassActor classActor, Object object) {
        //Snippets.checkCast(classActor, object);
        if (!classActor.isNullOrInstance(object)) {
            Throw.throwClassCastException(classActor, object);
            throw UnreachableNode.unreachable();
        }
        BeginNode anchorNode = BeginNode.anchor(StampFactory.forNodeIntrinsic());
        return UnsafeCastNode.unsafeCast(object, StampFactory.forNodeIntrinsic(), anchorNode);
    }

    protected class UnresolvedCheckCastLowering extends UnresolvedTypeLowering implements LoweringProvider<UnresolvedCheckCastNode> {
        UnresolvedCheckCastLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "unresolvedCheckCastSnippet");
        }

        @Override
        public void lower(UnresolvedCheckCastNode node, LoweringTool tool) {
            Arguments args = createGuard(node);
            args.add("object", node.object());
            instantiate(node, args, tool);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Object unresolvedCheckCastSnippet(ResolutionGuard.InPool guard, Object object) {
        resolveAndCheckCast(guard, object);
        return UnsafeCastNode.unsafeCast(object, StampFactory.forNodeIntrinsic());
    }

    @SNIPPET_SLOWPATH
    private static void resolveAndCheckCast(ResolutionGuard.InPool guard, Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (!classActor.isNullOrInstance(object)) {
            Throw.throwClassCastException(classActor, object);
            throw UnreachableNode.unreachable();
        }
    }

    protected class InstanceOfLowering extends InstanceOfSnippetsTemplates implements LoweringProvider<FloatingNode> {
        private final SnippetInfo instanceOf = snippet(TypeSnippets.class, "instanceOfSnippet");

        InstanceOfLowering(MetaAccessProvider runtime, Replacements replacements, TargetDescription target) {
            super(runtime, replacements, target);
        }

        @Override
        protected Arguments makeArguments(InstanceOfUsageReplacer replacer, LoweringTool tool) {
            InstanceOfNode node = (InstanceOfNode) replacer.instanceOf;
            ClassActor classActor = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.type());
            ValueNode object = node.object();
            Arguments args = new Arguments(instanceOf);
            args.add("classActor", classActor);
            args.add("object", object);
            return args;
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static boolean instanceOfSnippet(ClassActor classActor, Object object) {
        return Snippets.instanceOf(classActor, object);
    }

    protected class UnresolvedInstanceOfLowering extends UnresolvedTypeLowering implements LoweringProvider<UnresolvedInstanceOfNode> {
        UnresolvedInstanceOfLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "unresolvedInstanceOfSnippet");
        }

        @Override
        public void lower(UnresolvedInstanceOfNode node, LoweringTool tool) {
            Arguments args = createGuard(node);
            args.add("object", node.object());
            instantiate(node, args, tool);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static boolean unresolvedInstanceOfSnippet(ResolutionGuard.InPool guard, Object object) {
        return resolveAndCheckInstanceOf(guard, object);
    }

    @SNIPPET_SLOWPATH
    private static boolean resolveAndCheckInstanceOf(ResolutionGuard.InPool guard, Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        return Snippets.instanceOf(classActor, object);
    }

    protected class UnresolvedLoadConstantLowering extends UnresolvedTypeLowering implements LoweringProvider<UnresolvedLoadConstantNode> {
        UnresolvedLoadConstantLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "unresolvedLoadConstantSnippet");
        }

        @Override
        public void lower(UnresolvedLoadConstantNode node, LoweringTool tool) {
            Arguments args = createGuard(node);
            instantiate(node, args, tool);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Class<?> unresolvedLoadConstantSnippet(ResolutionGuard.InPool guard) {
        return resolveClass(guard);
    }

    @SNIPPET_SLOWPATH
    private static Class<?> resolveClass(ResolutionGuard.InPool guard) {
        ClassActor classActor = Snippets.resolveClass(guard);
        return classActor.javaClass();
    }

    protected class UnresolvedLoadClassActorLowering extends UnresolvedTypeLowering implements LoweringProvider<UnresolvedLoadClassActorNode> {
        UnresolvedLoadClassActorLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "unresolvedLoadClassActorSnippet");
        }

        @Override
        public void lower(UnresolvedLoadClassActorNode node, LoweringTool tool) {
            Arguments args = createGuard(node);
            instantiate(node, args, tool);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static ClassActor unresolvedLoadClassActorSnippet(ResolutionGuard.InPool guard) {
        return resolveClassActor(guard);
    }

    @SNIPPET_SLOWPATH
    private static ClassActor resolveClassActor(ResolutionGuard.InPool guard) {
        return Snippets.resolveClass(guard);
    }

}
