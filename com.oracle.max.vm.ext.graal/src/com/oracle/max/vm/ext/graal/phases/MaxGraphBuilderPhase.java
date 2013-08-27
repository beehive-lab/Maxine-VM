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
package com.oracle.max.vm.ext.graal.phases;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.ResolvedJavaType.Representation;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.phases.*;
import com.oracle.max.vm.ext.graal.nodes.*;

/**
 * A customized graph builder to insert nodes that cause unresolved entities to be resolved explicitly at runtime,
 * as opposed to falling back to deoptimization. This may not be the best approach but it is what
 * C1X does, so we follow that approach for now.
 */
public class MaxGraphBuilderPhase extends GraphBuilderPhase {

    public MaxGraphBuilderPhase(MetaAccessProvider runtime, GraphBuilderConfiguration graphBuilderConfig, OptimisticOptimizations optimisticOpts) {
        super(runtime, graphBuilderConfig, optimisticOpts);
    }

    @Override
    public void handleUnresolvedLoadField(JavaField field, ValueNode receiver) {
        boolean isStatic = receiver == null;
        ResolveFieldNode resolveFieldNode = new ResolveFieldNode(true, field, isStatic);
        append(resolveFieldNode);
        UnresolvedLoadFieldNode node = new UnresolvedLoadFieldNode(receiver, field, isStatic, resolveFieldNode);
        frameState.push(field.getKind().getStackKind(), append(node));
    }

    @Override
    protected void handleUnresolvedStoreField(JavaField field, ValueNode value, ValueNode receiver) {
        boolean isStatic = receiver == null;
        ResolveFieldNode resolveFieldNode = new ResolveFieldNode(false, field, isStatic);
        append(resolveFieldNode);
        UnresolvedStoreFieldNode node = new UnresolvedStoreFieldNode(receiver, field, isStatic, resolveFieldNode, value);
        append(node);
    }

    @Override
    protected void handleUnresolvedNewInstance(JavaType type) {
        UnresolvedTypeNode node = new UnresolvedNewInstanceNode(type);
        frameState.push(Kind.Object, append(node));
    }

    @Override
    protected void handleUnresolvedNewObjectArray(JavaType type, ValueNode length) {
        UnresolvedNewArrayNode node = new UnresolvedNewArrayNode(type, length);
        frameState.push(Kind.Object, append(node));
    }

    @Override
    protected void handleUnresolvedNewMultiArray(JavaType type, ValueNode[] dims) {
        UnresolvedLoadClassActorNode loadConstantNode = new UnresolvedLoadClassActorNode(type);
        append(loadConstantNode);
        UnresolvedNewMultiArrayNode node = new UnresolvedNewMultiArrayNode(loadConstantNode, dims);
        frameState.push(Kind.Object, append(node));
    }

    @Override
    protected void handleUnresolvedInvoke(JavaMethod javaMethod, InvokeKind invokeKind) {
        boolean withReceiver = invokeKind != InvokeKind.Static;
        ValueNode[] args = frameState.popArguments(javaMethod.getSignature().getParameterSlots(withReceiver),
                        javaMethod.getSignature().getParameterCount(withReceiver));
        // This node is a placeholder for resolving the method
        ResolveMethodNode resolveMethodNode = new ResolveMethodNode(invokeKind, javaMethod);
        append(resolveMethodNode);
        UnresolvedMethodCallTargetNode callTarget = new UnresolvedMethodCallTargetNode(invokeKind, javaMethod, args,
                        javaMethod.getSignature().getReturnType(null), resolveMethodNode);
        currentGraph.add(callTarget);
        createInvokeNode(callTarget, javaMethod.getSignature().getReturnKind());
    }

    @Override
    protected void handleUnresolvedCheckCast(JavaType type, ValueNode object) {
        UnresolvedTypeNode node = new UnresolvedCheckCastNode(type, object);
        frameState.apush(append(node));
    }

    @Override
    protected void handleUnresolvedInstanceOf(JavaType type, ValueNode object) {
        UnresolvedTypeNode node = new UnresolvedInstanceOfNode(type, object);
        frameState.ipush(append(node));
    }

    @Override
    protected void handleUnresolvedLoadConstant(JavaType type) {
        UnresolvedLoadConstantNode node = new UnresolvedLoadConstantNode(type);
        frameState.apush(append(node));
    }

    @Override
    protected void handleUnresolvedExceptionType(Representation representation, JavaType type) {
        assert false;
    }

    @Override
    protected FixedGuardNode createFixedGuardNode(LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, boolean negated) {
        return new FixedGuardNode(condition, deoptReason, action, negated);
    }

    @Override
    protected void emitExplicitExceptions(ValueNode receiver, ValueNode outOfBoundsIndex) {
        // All explicit exceptions are handled by deopt guards
        // Since Maxine currently disables the use of ExceptionProbability, if we don't
        // override this method, we get duplicate checks. The downside is that a method
        // that actually contains a handler for an implicit exception will never have the handler
        // code generated which means it must always deopt. Too bad if it's on the hot path.
    }

}
