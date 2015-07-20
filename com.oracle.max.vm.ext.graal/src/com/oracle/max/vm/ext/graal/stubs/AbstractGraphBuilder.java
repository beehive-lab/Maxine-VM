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

import static com.oracle.graal.nodes.FrameState.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.Kind;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.type.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Base class for building graphs used in implementing native method stubs.
 */
public abstract class AbstractGraphBuilder {

    private FixedWithNextNode lastFixedNode;
    protected final ClassMethodActor nativeMethodActor;
    protected final ResolvedJavaMethod nativeMethod;
    protected StructuredGraph graph;

    public AbstractGraphBuilder(ClassMethodActor nativeMethodActor) {
        this.nativeMethodActor = nativeMethodActor;
        this.nativeMethod = MaxResolvedJavaMethod.get(nativeMethodActor);
    }

    protected void setGraph(StructuredGraph graph) {
        this.graph = graph;
        lastFixedNode = graph.start();
    }

    /**
     * Creates a constant node for an integer constant.
     */
    public ConstantNode iconst(int value) {
        return ConstantNode.forInt(value, graph);
    }

    /**
     * Creates a constant node for a boolean constant.
     */
    public ConstantNode zconst(boolean value) {
        return ConstantNode.forBoolean(value, graph);
    }

    /**
     * Creates a constant node for an object constant.
     */
    public ConstantNode oconst(Object value) {
        return ConstantNode.forObject(value, MaxGraal.runtime(), graph);
    }

    public List<LocalNode> createLocals(int nextIndex, SignatureDescriptor sig, boolean isStatic) {
        // TODO fix type handling here
        int index = nextIndex;
        List<LocalNode> args = new ArrayList<LocalNode>(sig.argumentCount(!isStatic));
        if (!isStatic) {
            args.add(graph.unique(new LocalNode(index++, StampFactory.forKind(Kind.Object))));
        }
        for (int i = 0; i < sig.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = sig.parameterDescriptorAt(i);
            Kind kind = KindMap.toGraalKind(WordUtil.ciKind(parameterDescriptor.toKind(), true));
            Stamp stamp;
            if (kind == Kind.Object) {
                stamp = StampFactory.forKind(Kind.Object);
            } else {
                stamp = StampFactory.forKind(kind);
            }
            args.add(graph.unique(new LocalNode(index, stamp)));
            index++;
        }
        return args;
    }

    /**
     * Creates a node for the invocation of a method.
     *
     * @param target the method being called
     * @param args the arguments of the call
     */
    public InvokeNode invoke(MethodActor target, ValueNode... args) {
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

        JavaType returnType = MaxJavaType.get(target.descriptor().returnType(nativeMethodActor.holder()));
        MaxResolvedJavaMethod rm = MaxResolvedJavaMethod.get(target);
        MethodCallTargetNode targetNode = graph.add(new MethodCallTargetNode(invokeKind, rm, args, returnType));
        InvokeNode invokeNode = graph.add(new InvokeNode(targetNode, INVALID_FRAMESTATE_BCI));
        FrameState stateAfter = stateAfterCall(invokeNode, KindMap.toGraalKind(target.descriptor().resultKind()));
        invokeNode.setStateAfter(stateAfter);
        return invokeNode;
    }

    public FrameState stateAfterCall(ValueNode callNode, Kind resultKind) {
        ValueNode[] locals = {};
        ArrayList<ValueNode> stack;
        if (resultKind == Kind.Void) {
            stack = new ArrayList<ValueNode>(0);
        } else if (resultKind == Kind.Long || resultKind == Kind.Double) {
            stack = new ArrayList<ValueNode>(2);
            stack.set(0, callNode);
            stack.set(1, null);
        } else {
            stack = new ArrayList<ValueNode>(1);
            stack.add(callNode);
        }

        return graph.add(new FrameState(null, INVALID_FRAMESTATE_BCI, locals, stack, new ValueNode[0], false, false));
    }

    /**
     * Appends a node to the control flow of the graph if it is a fixed node.
     */
    public <T extends FixedNode> T append(T node) {
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

    static final MethodActor Pointer_writeObject = findMethod(Pointer.class, "writeObject", int.class, Object.class);
    static final MethodActor Snippets_nativeCallPrologue = findMethod(Snippets.class, "nativeCallPrologue", NativeFunction.class);
    static final MethodActor Snippets_nativeCallPrologueForC = findMethod(Snippets.class, "nativeCallPrologueForC", NativeFunction.class);
    static final MethodActor Intrinsics_alloca = findMethod(Intrinsics.class, "alloca", int.class, boolean.class);
    static final MethodActor JniHandle_unhand = findMethod(JniHandle.class, "unhand");
    static final MethodActor JniHandles_getHandle = findMethod(JniHandles.class, "getHandle", Pointer.class, int.class, Object.class);
}
