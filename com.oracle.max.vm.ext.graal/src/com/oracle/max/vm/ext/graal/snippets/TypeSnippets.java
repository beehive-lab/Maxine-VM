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
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.Parameter;
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
    public TypeSnippets(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(CheckCastNode.class, new CheckCastLowering(this));
        lowerings.put(UnresolvedCheckCastNode.class, new UnresolvedCheckCastLowering(this));
        lowerings.put(InstanceOfNode.class, new InstanceOfLowering(runtime, replacements, targetDescription, TypeSnippets.class));
        lowerings.put(UnresolvedInstanceOfNode.class, new UnresolvedInstanceOfLowering(this));
        lowerings.put(UnresolvedLoadConstantNode.class, new UnresolvedLoadConstantLowering(this));
        lowerings.put(UnresolvedLoadClassActorNode.class, new UnresolvedLoadClassActorLowering(this));
        lowerings.put(FixedGuardNode.class, new FixedGuardLowering(this));
    }

    protected class FixedGuardLowering extends Lowering implements LoweringProvider<FixedGuardNode> {

        FixedGuardLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "nullCheckSnippet");
        }

        @Override
        public void lower(FixedGuardNode node, LoweringTool tool) {
            Key key = null;
            switch (node.getReason()) {
                case NullCheckException: {
                    key = new Key(snippet);
                    break;
                }
                default:
                    FatalError.unexpected("FixedGuardLowering: Unexpected reason: " + node.getReason());
            }
            Arguments args = new Arguments();
            ValueNode trueValue = ConstantNode.forBoolean(node.isNegated(), node.graph());
            ValueNode falseValue = ConstantNode.forBoolean(!node.isNegated(), node.graph());
            args.add("cond", node.graph().unique(new ConditionalNode(node.condition(), trueValue, falseValue)));
            instantiate(node, key, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void nullCheckSnippet(@Parameter("cond") boolean cond) {
        if (cond) {
            Throw.throwNullPointerException();
            throw UnreachableNode.unreachable();
        }
    }

    protected abstract class UnresolvedTypeLowering extends Lowering {

        protected UnresolvedTypeLowering(SnippetLowerings snippets, String string) {
            super(snippets, string);
        }

        protected KeyArgs createGuard(UnresolvedTypeNode node) {
            UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) MaxJavaType.getRiType(node.javaType());
            ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
            return new KeyArgs(new Key(snippet), Arguments.arguments("guard", guard));
         }
    }

    protected class CheckCastLowering extends Lowering implements LoweringProvider<CheckCastNode> {
        CheckCastLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "checkCastSnippet");
        }

        @Override
        public void lower(CheckCastNode node, LoweringTool tool) {
            ClassActor classActor = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.type());
            Key key = new Key(snippet);
            //boolean checkNull = !node.object().stamp().nonNull(); TODO use this
            Arguments args = new Arguments();
            args.add("classActor", classActor);
            args.add("object", node.object());
            instantiate(node, key, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Object checkCastSnippet(@Parameter("classActor") ClassActor classActor, @Parameter("object") Object object) {
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
            KeyArgs keyArgs = createGuard(node);
            keyArgs.args.add("object", node.object());
            instantiate(node, keyArgs.key, keyArgs.args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static void unresolvedCheckCastSnippet(@Parameter("guard") ResolutionGuard.InPool guard, @Parameter("object") Object object) {
        resolveAndCheckCast(guard, object);
    }

    @RUNTIME_ENTRY
    private static Object resolveAndCheckCast(ResolutionGuard.InPool guard, Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (!classActor.isNullOrInstance(object)) {
            Throw.throwClassCastException(classActor, object);
            throw UnreachableNode.unreachable();
        }
        BeginNode anchorNode = BeginNode.anchor(StampFactory.forNodeIntrinsic());
        return UnsafeCastNode.unsafeCast(object, StampFactory.forNodeIntrinsic(), anchorNode);
    }

    protected class InstanceOfLowering extends InstanceOfSnippetsTemplates<TypeSnippets> implements LoweringProvider<FloatingNode> {
        private final ResolvedJavaMethod instanceOf = findSnippet(TypeSnippets.class, "instanceOfSnippet");

        InstanceOfLowering(MetaAccessProvider runtime, Replacements replacements, TargetDescription target, Class<TypeSnippets> snippetsClass) {
            super(runtime, replacements, target, snippetsClass);
        }

        @Override
        protected KeyAndArguments getKeyAndArguments(InstanceOfUsageReplacer replacer, LoweringTool tool) {
            InstanceOfNode node = (InstanceOfNode) replacer.instanceOf;
            ClassActor classActor = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.type());
            ValueNode object = node.object();
            Key key = new Key(instanceOf);
            Arguments args = new Arguments();
            args.add("classActor", classActor);
            args.add("object", object);

            return new KeyAndArguments(key, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static boolean instanceOfSnippet(@Parameter("classActor") ClassActor classActor, @Parameter("object") Object object) {
        return Snippets.instanceOf(classActor, object);
    }

    protected class UnresolvedInstanceOfLowering extends UnresolvedTypeLowering implements LoweringProvider<UnresolvedInstanceOfNode> {
        UnresolvedInstanceOfLowering(TypeSnippets typeSnippets) {
            super(typeSnippets, "unresolvedInstanceOfSnippet");
        }

        @Override
        public void lower(UnresolvedInstanceOfNode node, LoweringTool tool) {
            KeyArgs keyArgs = createGuard(node);
            keyArgs.args.add("object", node.object());
            instantiate(node, keyArgs.key, keyArgs.args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static boolean unresolvedInstanceOfSnippet(@Parameter("guard") ResolutionGuard.InPool guard, @Parameter("object") Object object) {
        return resolveAndCheckInstanceOf(guard, object);
    }

    @RUNTIME_ENTRY
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
            KeyArgs keyArgs = createGuard(node);
            instantiate(node, keyArgs.key, keyArgs.args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static Class<?> unresolvedLoadConstantSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveClass(guard);
    }

    @RUNTIME_ENTRY
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
            KeyArgs keyArgs = createGuard(node);
            instantiate(node, keyArgs.key, keyArgs.args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static ClassActor unresolvedLoadClassActorSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveClassActor(guard);
    }

    @RUNTIME_ENTRY
    private static ClassActor resolveClassActor(ResolutionGuard.InPool guard) {
        return Snippets.resolveClass(guard);
    }

}
