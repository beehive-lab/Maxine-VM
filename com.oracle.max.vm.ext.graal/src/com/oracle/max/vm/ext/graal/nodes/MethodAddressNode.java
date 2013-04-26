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
package com.oracle.max.vm.ext.graal.nodes;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;

/**
 * Node that is created as part of the {@link InvokeNode} lowering that allows a snippet to be used to return the entry
 * address of the method, for example by accessing the vTable/iTable structure.
 *
 * The {@link #methodActor} input is either a {@link ConstantNode} for a resolved method or the input from a node that
 * handles the resolution. The {@link #receiver} is null for a static method.
 */
public class MethodAddressNode extends FixedWithNextNode implements Lowerable {

    @Input ValueNode receiver;
    @Input ValueNode methodActor;

    private InvokeKind invokeKind;

    public MethodAddressNode(InvokeKind invokeKind, ValueNode methodActor, ValueNode receiver) {
        super(StampFactory.forKind(Kind.Long));
        this.invokeKind = invokeKind;
        this.receiver = receiver;
        this.methodActor = methodActor;
    }

    public InvokeKind invokeKind() {
        return invokeKind;
    }

    public ValueNode methodActor() {
        return methodActor;
    }

    public ValueNode receiver() {
        return receiver;
    }

    @Override
    public void lower(LoweringTool tool, LoweringType loweringType) {
        tool.getRuntime().lower(this, tool);
    }

}
