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

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code InvokeNode} represents all kinds of method calls.
 */
public final class InvokeNode extends AbstractCallNode implements ExceptionExit, Node.IterableNodeType {

    @Successor private FixedNode exceptionEdge;

    private boolean canInline = true;

    public final int opcode;
    public final RiMethod target;
    public final RiType returnType;
    public final int bci; // needed because we can not compute the bci from the sateBefore bci of this Invoke was optimized from INVOKEINTERFACE to INVOKESPECIAL

    /**
     * Constructs a new Invoke instruction.
     *
     * @param bci the bytecode index of the original invoke (used for debug infos)
     * @param opcode the opcode of the invoke
     * @param result the result type
     * @param args the list of instructions producing arguments to the invocation, including the receiver object
     * @param target the target method being called
     * @param returnType the return type of the target method
     */
    public InvokeNode(int bci, int opcode, CiKind result, ValueNode[] args, RiMethod target, RiType returnType) {
        super(result, args);
        this.opcode = opcode;
        this.target = target;
        this.returnType = returnType;
        this.bci = bci;
    }

    @Override
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

    /**
     * Gets the opcode of this invoke instruction.
     * @return the opcode
     */
    public int opcode() {
        return opcode;
    }

    /**
     * Checks whether this is an invocation of a static method.
     * @return {@code true} if the invocation is a static invocation
     */
    public boolean isStatic() {
        assert !target().isResolved() || (opcode == Bytecodes.INVOKESTATIC) == Modifier.isStatic(target().accessFlags());
        return opcode == Bytecodes.INVOKESTATIC;
    }

    @Override
    public RiType declaredType() {
        return returnType;
    }

    /**
     * Gets the instruction that produces the receiver object for this invocation, if any.
     * @return the instruction that produces the receiver object for this invocation if any, {@code null} if this
     *         invocation does not take a receiver object
     */
    public ValueNode receiver() {
        assert !isStatic();
        return arguments().get(0);
    }

    /**
     * Gets the target method for this invocation instruction.
     * @return the target method
     */
    public RiMethod target() {
        return target;
    }

    /**
     * Checks whether this invocation has a receiver object.
     * @return {@code true} if this invocation has a receiver object; {@code false} otherwise, if this is a
     *         static call
     */
    public boolean hasReceiver() {
        return !isStatic();
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitInvoke(this);
    }

    @Override
    public String toString() {
        return super.toString() + target;
    }

    @Override
    public Map<Object, Object> getDebugProperties() {
        Map<Object, Object> properties = super.getDebugProperties();
        properties.put("opcode", Bytecodes.nameOf(opcode));
        properties.put("target", CiUtil.format("%H.%n(%p):%r", target, false));
        properties.put("bci", bci);
        return properties;
    }
}
