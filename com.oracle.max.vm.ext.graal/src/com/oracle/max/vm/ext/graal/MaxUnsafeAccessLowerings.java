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
package com.oracle.max.vm.ext.graal;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.HeapAccess.BarrierType;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.actor.member.VirtualMethodActor;
import com.sun.max.vm.layout.Layout;

import static com.sun.max.vm.layout.Layout.*;

public class MaxUnsafeAccessLowerings {

    public static void registerLowerings(Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        UnsafeLoadLowering unsafeLoadLowering = new UnsafeLoadLowering();
        lowerings.put(UnsafeLoadNode.class, unsafeLoadLowering);
        UnsafeStoreLowering unsafeStoreLowering = new UnsafeStoreLowering();
        lowerings.put(UnsafeStoreNode.class, unsafeStoreLowering);
        LoadHubNodeLowering loadHubNodeLowering = new LoadHubNodeLowering();
        lowerings.put(LoadHubNode.class, loadHubNodeLowering);
        LoadMethodNodeLowering loadMethodNodeLowering = new LoadMethodNodeLowering();
        lowerings.put(LoadMethodNode.class, loadMethodNodeLowering);

        lowerings.put(MaxCompareAndSwapNode.class, new MaxCompareAndSwapLowering());
    }

    private static class UnsafeLoadLowering implements LoweringProvider<UnsafeLoadNode> {

        @Override
        public void lower(UnsafeLoadNode node, LoweringTool tool) {
            StructuredGraph graph = node.graph();
            assert node.kind() != Kind.Illegal;
            lower(graph, node, node.stamp(), node.object(), node.offset(), node.displacement(), node.accessKind());
        }

        void lower(StructuredGraph graph, FixedWithNextNode node, Stamp stamp, ValueNode object, ValueNode offset, int displacement, Kind accessKind) {
            IndexedLocationNode location = IndexedLocationNode.create(LocationIdentity.ANY_LOCATION, accessKind, displacement, offset, graph, 1);
            ReadNode memoryRead = graph.add(new ReadNode(object, location, stamp, AbstractBeginNode.prevBegin(node), BarrierType.NONE, false));
            graph.replaceFixedWithFixed(node, memoryRead);

        }
    }

    private static class UnsafeStoreLowering implements LoweringProvider<UnsafeStoreNode> {

        @Override
        public void lower(UnsafeStoreNode node, LoweringTool tool) {
            StructuredGraph graph = node.graph();
            lower(graph, node, node.stamp(), node.object(), node.offset(), node.value(), node.displacement(), node.accessKind(), node.stateAfter());
        }

        void lower(StructuredGraph graph, FixedWithNextNode node, Stamp stamp, ValueNode object, ValueNode offset, ValueNode value,
                        int displacement, Kind accessKind, FrameState stateAfter) {
            IndexedLocationNode location = IndexedLocationNode.create(LocationIdentity.ANY_LOCATION, accessKind, displacement, offset, graph, 1);
            WriteNode write = graph.add(new WriteNode(object, value, location, BarrierType.NONE, false));
            write.setStateAfter(stateAfter);
            graph.replaceFixedWithFixed(node, write);
        }

    }

    private static class LoadHubNodeLowering implements LoweringProvider<LoadHubNode> {

        @Override
        public void lower(LoadHubNode node, LoweringTool tool) {
            StructuredGraph graph = node.graph();
            long hubOffset = generalLayout().getOffsetFromOrigin(Layout.HeaderField.HUB).toLong();
            lower(graph, node, Kind.Object, node.object(), node.getGuard(), hubOffset);
        }

        void lower(StructuredGraph graph, LoadHubNode node, Kind kind, ValueNode object, GuardingNode guard, long offset) {
            assert !object.isConstant() || object.asConstant().isNull();
            LocationNode location = ConstantLocationNode.create(LocationIdentity.FINAL_LOCATION, kind, offset, graph);
            FloatingReadNode floatingReadNode = graph.add(new FloatingReadNode(object, location, null, StampFactory.forKind(kind), guard, BarrierType.NONE, false));
            graph.replaceFloating(node, floatingReadNode);
        }

    }

    private static class LoadMethodNodeLowering implements LoweringProvider<LoadMethodNode> {

        @Override
        public void lower(LoadMethodNode node, LoweringTool tool) {
            StructuredGraph graph = node.graph();
            ResolvedJavaMethod method = node.getMethod();
            ClassMethodActor methodActor = (ClassMethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(method);
            assert methodActor instanceof VirtualMethodActor;
            VirtualMethodActor virtualMethodActor = (VirtualMethodActor) methodActor;

            FieldActor classActorField = FieldActor.findInstance(ClassActor.fromJava(Hub.class), "classActor");
            LoadFieldNode loadedClassActor = new LoadFieldNode(node.getHub(), MaxResolvedJavaField.get(classActorField));
            graph.add(loadedClassActor);
            graph.addBeforeFixed(node, loadedClassActor);

            FieldActor allVirtualMethodActorsField = FieldActor.findInstance(ClassActor.fromJava(ClassActor.class), "allVirtualMethodActors");
            LoadFieldNode loadedAllVirtualMethodActors = new LoadFieldNode(loadedClassActor, MaxResolvedJavaField.get(allVirtualMethodActorsField));
            graph.add(loadedAllVirtualMethodActors);
            graph.addBeforeFixed(node, loadedAllVirtualMethodActors);

            final int vTableIndex = virtualMethodActor.vTableIndex() - Hub.vTableStartIndex();
            final long vTableEntryOffset = referenceArrayLayout().getElementOffsetFromOrigin(vTableIndex).toInt();
            lower(graph, node, Kind.Object, loadedAllVirtualMethodActors, vTableEntryOffset);
        }

        void lower(StructuredGraph graph, LoadMethodNode node, Kind kind, ValueNode virtualMethodsArray, long vTableEntryOffset) {
            LocationNode location = ConstantLocationNode.create(LocationIdentity.ANY_LOCATION, kind, vTableEntryOffset, graph);
            ReadNode readNode = graph.add(new ReadNode(virtualMethodsArray, location, StampFactory.forKind(kind), BarrierType.NONE, false));
            graph.replaceFixed(node, readNode);
        }

    }

    protected static class MaxCompareAndSwapLowering implements LoweringProvider<MaxCompareAndSwapNode> {

        @Override
        public void lower(MaxCompareAndSwapNode node, LoweringTool tool) {
            // The MaxCompareAndSwapNode is lowered to LIR instructions.
        }
    }

}
