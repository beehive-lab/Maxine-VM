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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.ResolvedJavaType.Representation;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.max.vm.ext.graal.nodes.*;

/**
 * A customizer to insert nodes that cause unresolved entities to be resolved explicitly at runtime,
 * as opposed to falling back to deoptimization. This may not be the best approach but it is what
 * C1X/T1X do, so we follow that approach for now.
 */
public class MaxUnresolvedCustomizer extends GraphBuilderPhase.UnresolvedCustomizer {

    @Override
    public void unresolvedGetField(GraphBuilderPhase phase, JavaField field, ValueNode receiver) {
        UnresolvedLoadFieldNode node = currentGraph(phase).add(new UnresolvedLoadFieldNode(receiver, field));
        frameState(phase).push(field.getKind().getStackKind(), append(phase, node));
    }

    @Override
    protected void unresolvedPutField(GraphBuilderPhase phase, JavaField field, ValueNode receiver, ValueNode value) {
        UnresolvedStoreFieldNode node = currentGraph(phase).add(new UnresolvedStoreFieldNode(receiver, field, value));
        append(phase, node);
    }

    @Override
    protected void unresolvedNewInstance(GraphBuilderPhase phase, JavaType type) {
        UnresolvedTypeNode node = new UnresolvedNewInstanceNode(type);
        currentGraph(phase).add(node);
        frameState(phase).push(Kind.Object, append(phase, node));
    }

    @Override
    protected void unresolvedNewArray(GraphBuilderPhase phase, JavaType type, ValueNode length) {
        UnresolvedNewArrayNode node = new UnresolvedNewArrayNode(type, length);
        currentGraph(phase).add(node);
        frameState(phase).push(Kind.Object, append(phase, node));
    }

    @Override
    protected void unresolvedNewMultiArrayType(GraphBuilderPhase phase, JavaType type, ValueNode[] dims) {
        assert false;

    }
    @Override
    protected void unresolvedInvoke(GraphBuilderPhase phase, JavaMethod javaMethod, InvokeKind invokeKind) {
        boolean withReceiver = invokeKind != InvokeKind.Static;
        ValueNode[] args = frameState(phase).popArguments(javaMethod.getSignature().getParameterSlots(withReceiver),
                        javaMethod.getSignature().getParameterCount(withReceiver));
        // This node is a placeholder for resolving the method
        ResolveMethodNode resolveMethodNode = new ResolveMethodNode(invokeKind, javaMethod);
        currentGraph(phase).add(resolveMethodNode);
        append(phase, resolveMethodNode);
        UnresolvedMethodCallTargetNode callTarget = new UnresolvedMethodCallTargetNode(invokeKind, javaMethod, args,
                        javaMethod.getSignature().getReturnType(null), resolveMethodNode);
        currentGraph(phase).add(callTarget);
        // TODO InvokeWithExceptionNode
        InvokeNode invokeNode = new InvokeNode(callTarget, phase.bci());
        currentGraph(phase).add(invokeNode);
        Kind returnKind = javaMethod.getSignature().getReturnKind();
        frameState(phase).pushReturn(returnKind, appendWithBCI(phase, invokeNode));
    }

    @Override
    protected void unresolvedCheckCast(GraphBuilderPhase phase, JavaType type, ValueNode object) {
        UnresolvedTypeNode node = new UnresolvedCheckCastNode(type, object);
        currentGraph(phase).add(node);
        frameState(phase).apush(append(phase, node));
    }

    @Override
    protected void unresolvedInstanceOf(GraphBuilderPhase phase, JavaType type, ValueNode object) {
        UnresolvedTypeNode node = new UnresolvedInstanceOfNode(type, object);
        currentGraph(phase).add(node);
        frameState(phase).ipush(append(phase, node));
    }

    @Override
    protected void unresolvedLoadConstant(GraphBuilderPhase phase, JavaType type) {
        UnresolvedLoadConstantNode node = new UnresolvedLoadConstantNode(type);
        currentGraph(phase).add(node);
        frameState(phase).apush(append(phase, node));
    }

    @Override
    protected void unresolvedExceptionCatchType(GraphBuilderPhase phase, JavaType type, Representation representation) {
        assert false;
    }

}
