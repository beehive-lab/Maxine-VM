/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes;

import java.util.*;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ri.*;

/**
 * The {@code InvokeNode} represents all kinds of method calls.
 */
public final class InvokeNode extends AbstractMemoryCheckpointNode implements Node.IterableNodeType {

    @Successor private FixedNode exceptionEdge;

    private boolean canInline = true;

    @Input private MethodCallTargetNode callTarget;
    private final int bci; // needed because we can not compute the bci from the sateBefore bci of this Invoke was optimized from INVOKEINTERFACE to INVOKESPECIAL

    /**
     * Constructs a new Invoke instruction.
     *
     * @param bci the bytecode index of the original invoke (used for debug infos)
     * @param opcode the opcode of the invoke
     * @param target the target method being called
     * @param args the list of instructions producing arguments to the invocation, including the receiver object
     */
    public InvokeNode(int bci, MethodCallTargetNode callTarget) {
        super(callTarget.returnKind().stackKind());
        this.callTarget = callTarget;
        this.bci = bci;
    }

    public FixedNode exceptionEdge() {
        return exceptionEdge;
    }

    public void setExceptionEdge(FixedNode x) {
        updatePredecessors(exceptionEdge, x);
        exceptionEdge = x;
    }

    public boolean canInline() {
        return canInline;
    }

    public void setCanInline(boolean b) {
        canInline = b;
    }

    public MethodCallTargetNode callTarget() {
        return callTarget;
    }

    @Override
    public RiResolvedType declaredType() {
        RiType returnType = callTarget().returnType();
        return (returnType instanceof RiResolvedType) ? ((RiResolvedType) returnType) : null;
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitInvoke(this);
    }

    @Override
    public String toString() {
        return super.toString() + callTarget().targetMethod();
    }

    public int bci() {
        return bci;
    }

    @Override
    public Map<Object, Object> getDebugProperties() {
        Map<Object, Object> properties = super.getDebugProperties();
        properties.put("bci", bci);
        return properties;
    }
}
