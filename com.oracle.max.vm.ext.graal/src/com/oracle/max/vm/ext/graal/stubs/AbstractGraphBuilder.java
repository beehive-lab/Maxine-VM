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

import static com.oracle.max.graal.nodes.FrameState.*;
import static com.oracle.max.vm.ext.maxri.MaxRuntime.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
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
    protected final ClassMethodActor nativeMethod;
    protected StructuredGraph graph;

    public AbstractGraphBuilder(ClassMethodActor nativeMethod) {
        this.nativeMethod = nativeMethod;
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
        return ConstantNode.forObject(value, runtime(), graph);
    }

    public List<LocalNode> createLocals(int nextIndex, SignatureDescriptor sig, boolean isStatic) {
        int index = nextIndex;
        List<LocalNode> args = new ArrayList<LocalNode>(sig.argumentCount(!isStatic));
        if (!isStatic) {
            args.add(graph.unique(new LocalNode(CiKind.Object, index++)));
        }
        for (int i = 0; i < sig.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = sig.parameterDescriptorAt(i);
            CiKind kind = WordUtil.ciKind(parameterDescriptor.toKind(), true).stackKind();
            args.add(graph.unique(new LocalNode(kind, index)));
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

        RiType returnType = target.descriptor().returnType(nativeMethod.holder());
        MethodCallTargetNode targetNode = graph.add(new MethodCallTargetNode(invokeKind, target, args, returnType));
        InvokeNode invokeNode = graph.add(new InvokeNode(targetNode, UNKNOWN_BCI));
        FrameState stateAfter = stateAfterCall(invokeNode, target.descriptor().resultKind());
        invokeNode.setStateAfter(stateAfter);
        return invokeNode;
    }

    public FrameState stateAfterCall(ValueNode callNode, Kind resultKind) {
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
        return graph.add(new FrameState(nativeMethod, UNKNOWN_BCI, locals, stack, stackSize, locks, false));
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
