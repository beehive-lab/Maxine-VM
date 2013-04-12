/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.Snippet.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.graal.replacements.nodes.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Snippets;

public class NewSnippets extends SnippetLowerings {

    @HOSTED_ONLY
    public NewSnippets(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements,
                    TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(NewInstanceNode.class, new NewInstanceLowering(this));
        lowerings.put(UnresolvedNewInstanceNode.class, new UnresolvedNewInstanceLowering(this));
        lowerings.put(NewArrayNode.class, new NewArrayLowering(this));
        lowerings.put(UnresolvedNewArrayNode.class, new UnresolvedNewArrayLowering(this));
        lowerings.put(NewMultiArrayNode.class, new NewMultiArrayLowering(this));
        lowerings.put(UnresolvedNewMultiArrayNode.class, new UnresolvedNewMultiArrayLowering(this));
    }

    protected class NewInstanceLowering extends Lowering implements LoweringProvider<NewInstanceNode> {

        NewInstanceLowering(NewSnippets newSnippets) {
            super(newSnippets, "newInstanceSnippet");
        }

        @Override
        public void lower(NewInstanceNode node, LoweringTool tool) {
            ClassActor type = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.instanceClass());
            Arguments args = new Arguments(snippet);
            args.add("hub", type.dynamicHub());
            instantiate(node, args);
        }
    }

    protected class UnresolvedNewInstanceLowering extends Lowering implements LoweringProvider<UnresolvedNewInstanceNode> {
        private SnippetInfo initializeClassAndNewSnippet = snippet(NewSnippets.class, "initializeClassAndNewSnippet");

        protected UnresolvedNewInstanceLowering(NewSnippets newSnippets) {
            super(newSnippets, "unresolvedNewInstanceSnippet");
        }

        @Override
        public void lower(UnresolvedNewInstanceNode node, LoweringTool tool) {
            Arguments args;
            // The class may be resolved but just not initialized
            RiType riType = MaxJavaType.getRiType(node.javaType());
            if (riType instanceof ClassActor) {
                args = new Arguments(initializeClassAndNewSnippet);
                args.add("classActor", riType);
            } else {
                UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) riType;
                ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
                args = new Arguments(snippet);
                args.add("guard", guard);
            }
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newInstanceSnippet(DynamicHub hub) {
        return UnsafeCastNode.unsafeCast(Heap.createTuple(hub), StampFactory.forNodeIntrinsic());
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object unresolvedNewInstanceSnippet(ResolutionGuard.InPool guard) {
        return UnsafeCastNode.unsafeCast(resolveClassForNewAndCreate(guard), StampFactory.forNodeIntrinsic());
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object initializeClassAndNewSnippet(ClassActor classActor) {
        return initializeClassAndNew(classActor);
    }

    @RUNTIME_ENTRY
    private static Object initializeClassAndNew(ClassActor classActor) {
        Snippets.makeClassInitialized(classActor);
        final Object tuple = Snippets.createTupleOrHybrid(classActor);
        return tuple;
    }

    @RUNTIME_ENTRY
    private static Object resolveClassForNewAndCreate(ResolutionGuard guard) {
        final ClassActor classActor = Snippets.resolveClassForNew(guard);
        return initializeClassAndNew(classActor);
    }

    protected class NewArrayLowering extends Lowering implements LoweringProvider<NewArrayNode> {

        protected NewArrayLowering(NewSnippets newSnippets) {
            super(newSnippets, "newArraySnippet");
        }

        @Override
        public void lower(NewArrayNode node, LoweringTool tool) {
            ClassActor type = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.elementType().getArrayClass());
            Arguments args = new Arguments(snippet);
            args.add("hub", type.dynamicHub());
            args.add("length", node.length());
            instantiate(node, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newArraySnippet(DynamicHub hub, int length) {
        if (length < 0) {
            Throw.throwNegativeArraySizeException(length);
            throw UnreachableNode.unreachable();
        }
        Object result = Heap.createArray(hub, length);
        BeginNode anchorNode = BeginNode.anchor(StampFactory.forNodeIntrinsic());
        return UnsafeArrayCastNode.unsafeArrayCast(result, length, StampFactory.forNodeIntrinsic(), anchorNode);
    }

    protected class UnresolvedNewArrayLowering extends Lowering implements LoweringProvider<UnresolvedNewArrayNode> {

        public UnresolvedNewArrayLowering(NewSnippets newSnippets) {
            super(newSnippets, "unresolvedNewArraySnippet");
        }

        @Override
        public void lower(UnresolvedNewArrayNode node, LoweringTool tool) {
            UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) MaxJavaType.getRiType(node.elementType());
            ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
            Arguments args = new Arguments(snippet);
            args.add("guard", guard);
            args.add("length", node.length());
            instantiate(node, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object unresolvedNewArraySnippet(ResolutionGuard.InPool guard, int length) {
        return UnsafeCastNode.unsafeCast(resolveClassForNewArrayAndCreate(guard, length), StampFactory.forNodeIntrinsic());
    }

    @RUNTIME_ENTRY
    private static Object resolveClassForNewArrayAndCreate(ResolutionGuard guard, int length) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));
        return Snippets.createArray(arrayClassActor, length);
    }

    protected class NewMultiArrayLowering extends Lowering implements LoweringProvider<NewMultiArrayNode> {

        protected NewMultiArrayLowering(NewSnippets newSnippets) {
            super(newSnippets, "newMultiArraySnippet");
        }

        @Override
        public void lower(NewMultiArrayNode node, LoweringTool tool) {
            ClassActor arrayClassActor = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.type());
            Arguments args = new Arguments(snippet);
            args.add("arrayClassActor", arrayClassActor);
            dimensions(args, node.dimensions());
            instantiate(node, args);
        }

    }

    private static void dimensions(Arguments args, NodeList<ValueNode> dimensions) {
        int rank = dimensions.size();
        ValueNode[] dims = new ValueNode[rank];
        for (int i = 0; i < rank; i++) {
            dims[i] = dimensions.get(i);
        }
        args.addConst("rank", rank);
        args.addVarargs("dimensions", int.class, StampFactory.forKind(Kind.Int), dims);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newMultiArraySnippet(ClassActor arrayClassActor,
                    @ConstantParameter int rank,
                    @VarargsParameter int[] dimensions) {
        int[] dims = new int[rank];
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < rank; i++) {
            dims[i] = dimensions[i];
            if (dimensions[i] < 0) {
                Throw.throwNegativeArraySizeException(dims[i]);
                throw UnreachableNode.unreachable();
            }
        }
        Object result = safeCreateMultiReferenceArray(arrayClassActor, dims);
        BeginNode anchorNode = BeginNode.anchor(StampFactory.forNodeIntrinsic());
        return UnsafeArrayCastNode.unsafeArrayCast(result, dims[0], StampFactory.forNodeIntrinsic(), anchorNode);
    }

    @RUNTIME_ENTRY
    public static Object safeCreateMultiReferenceArray(ClassActor classActor, int[] lengths) {
        return Snippets.createMultiReferenceArrayAtIndex(0, classActor, lengths);
    }


    protected class UnresolvedNewMultiArrayLowering extends Lowering implements LoweringProvider<UnresolvedNewMultiArrayNode> {

        public UnresolvedNewMultiArrayLowering(NewSnippets newSnippets) {
            super(newSnippets, "newMultiArraySnippet");
        }

        @Override
        public void lower(UnresolvedNewMultiArrayNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("arrayClassActor", node.resolvedClassActor());
            dimensions(args, node.dimensions());
            instantiate(node, args);
        }
    }

}

