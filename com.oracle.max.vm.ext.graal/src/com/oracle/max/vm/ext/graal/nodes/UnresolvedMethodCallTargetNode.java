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
import com.oracle.graal.nodes.type.*;

public class UnresolvedMethodCallTargetNode extends CallTargetNode {

    @Input ValueNode methodValue;

    private final JavaType returnType;
    private JavaMethod targetMethod;
    private InvokeKind invokeKind;

    public UnresolvedMethodCallTargetNode(InvokeKind invokeKind, JavaMethod targetMethod,
                    ValueNode[] arguments, JavaType returnType, ValueNode methodValue) {
        super(arguments);
        this.returnType = returnType;
        this.targetMethod = targetMethod;
        this.invokeKind = invokeKind;
        this.methodValue = methodValue;
    }

    /**
     * Gets the target method for this invocation instruction.
     *
     * @return the target method
     */
    public JavaMethod targetMethod() {
        return targetMethod;
    }

    public InvokeKind invokeKind() {
        return invokeKind;
    }

    @Override
    public Stamp returnStamp() {
        Kind returnKind = targetMethod.getSignature().getReturnKind();
        if (returnKind == Kind.Object && returnType instanceof ResolvedJavaType) {
            return StampFactory.declared((ResolvedJavaType) returnType);
        } else {
            return StampFactory.forKind(returnKind);
        }
    }

    @Override
    public String targetName() {
        if (targetMethod() == null) {
            return "??Invalid!";
        }
        return targetMethod().getName();
    }

}
