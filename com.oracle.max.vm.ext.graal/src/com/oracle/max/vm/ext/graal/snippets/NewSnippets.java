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
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.Snippet.Parameter;
import com.oracle.graal.snippets.SnippetTemplate.Arguments;
import com.oracle.graal.snippets.SnippetTemplate.Key;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

public class NewSnippets extends SnippetLowerings implements SnippetsInterface {

    private final VMConfiguration vmConfig;

    public static void registerLowerings(VMConfiguration config, TargetDescription targetDescription, MetaAccessProvider runtime,
                    Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        new NewSnippets(config, targetDescription, runtime, assumptions, lowerings);
    }

    private NewSnippets(VMConfiguration vmConfig, TargetDescription targetDescription, MetaAccessProvider runtime, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, targetDescription);
        this.vmConfig = vmConfig;

        lowerings.put(NewInstanceNode.class, new NewInstanceLowering(this));
        lowerings.put(UnresolvedNewInstanceNode.class, new UnresolvedNewInstanceLowering(this));
        lowerings.put(NewArrayNode.class, new NewArrayLowering(this));
        lowerings.put(UnresolvedNewArrayNode.class, new UnresolvedNewArrayLowering(this));
        /*
        lowerings.put(NewMultiArrayNode.class, new NewMultiArrayLowering());
        */
    }

    protected class NewInstanceLowering extends Lowering implements LoweringProvider<NewInstanceNode> {

        NewInstanceLowering(NewSnippets newSnippets) {
            super(newSnippets, "newInstanceSnippet");
        }

        @Override
        public void lower(NewInstanceNode node, LoweringTool tool) {
            ClassActor type = (ClassActor) MaxResolvedJavaType.getRiResolvedType(node.instanceClass());
            Key key = new Key(snippet);
            Arguments args = new Arguments();
            args.add("hub", type.dynamicHub());
            instantiate(node, key, args);
        }
    }

    protected class UnresolvedNewInstanceLowering extends Lowering implements LoweringProvider<UnresolvedNewInstanceNode> {
        private ResolvedJavaMethod initializeClassAndNewSnippet = findSnippet(NewSnippets.class, "initializeClassAndNewSnippet");

        protected UnresolvedNewInstanceLowering(NewSnippets newSnippets) {
            super(newSnippets, "unresolvedNewInstanceSnippet");
        }

        @Override
        public void lower(UnresolvedNewInstanceNode node, LoweringTool tool) {
            Key key;
            Arguments args = new Arguments();
            // The class may be resolved but just not initialized
            RiType riType = MaxJavaType.getRiType(node.javaType());
            if (riType instanceof ClassActor) {
                key = new Key(initializeClassAndNewSnippet);
                args.add("classActor", riType);
            } else {
                UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) riType;
                ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
                key = new Key(snippet);
                args.add("guard", guard);
            }
            instantiate(node, key, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newInstanceSnippet(@Parameter("hub") DynamicHub hub) {
        return UnsafeCastNode.unsafeCast(Heap.createTuple(hub), StampFactory.forNodeIntrinsic());
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object unresolvedNewInstanceSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return UnsafeCastNode.unsafeCast(resolveClassForNewAndCreate(guard), StampFactory.forNodeIntrinsic());
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object initializeClassAndNewSnippet(@Parameter("classActor") ClassActor classActor) {
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
            Key key = new Key(snippet);
            Arguments args = new Arguments();
            args.add("hub", type.dynamicHub());
            args.add("length", node.length());
            instantiate(node, key, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newArraySnippet(@Parameter("hub") DynamicHub hub, @Parameter("length") int length) {
        if (length < 0) {
            Throw.throwNegativeArraySizeException(length);
        }
        Object result = Heap.createArray(hub, length);
        return UnsafeArrayCastNode.unsafeArrayCast(result, length, StampFactory.forNodeIntrinsic());
    }

    protected class UnresolvedNewArrayLowering extends Lowering implements LoweringProvider<UnresolvedNewArrayNode> {

        public UnresolvedNewArrayLowering(NewSnippets newSnippets) {
            super(newSnippets, "unresolvedNewArraySnippet");
        }

        @Override
        public void lower(UnresolvedNewArrayNode node, LoweringTool tool) {
            UnresolvedType.InPool unresolvedType = (UnresolvedType.InPool) MaxJavaType.getRiType(node.elementType());
            ResolutionGuard.InPool guard = unresolvedType.pool.makeResolutionGuard(unresolvedType.cpi);
            Key key = new Key(snippet);
            Arguments args = new Arguments();
            args.add("guard", guard);
            args.add("length", node.length());
            instantiate(node, key, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object unresolvedNewArraySnippet(@Parameter("guard") ResolutionGuard.InPool guard, @Parameter("length") int length) {
        return UnsafeCastNode.unsafeCast(resolveClassForNewArrayAndCreate(guard, length), StampFactory.forNodeIntrinsic());
    }

    @RUNTIME_ENTRY
    private static Object resolveClassForNewArrayAndCreate(ResolutionGuard guard, int length) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));
        return Snippets.createArray(arrayClassActor, length);
    }

}

