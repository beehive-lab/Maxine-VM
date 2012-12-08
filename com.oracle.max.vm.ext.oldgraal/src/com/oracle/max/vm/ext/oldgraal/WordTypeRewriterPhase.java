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
package com.oracle.max.vm.ext.oldgraal;

import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.*;
import com.oracle.max.graal.nodes.type.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.type.*;


public class WordTypeRewriterPhase extends Phase {
    @Override
    protected void run(StructuredGraph graph) {
        for (Node n : graph.getNodes()) {
            if (n instanceof ValueNode) {
                ValueNode valueNode = (ValueNode) n;
                if (isWord(valueNode)) {
                    changeToWord(valueNode);
                }
            }
        }
        for (MethodCallTargetNode callTargetNode : graph.getNodes(MethodCallTargetNode.class)) {
            if (callTargetNode.invokeKind() == InvokeKind.Virtual && callTargetNode.arguments().get(0) != null && callTargetNode.arguments().get(0).kind() != CiKind.Object) {
                callTargetNode.setInvokeKind(InvokeKind.Special);
            }
        }
    }

    public boolean isWord(ValueNode node) {
        if (node.kind() == CiKind.Object) {
            if (node instanceof ConstantNode) {
                ConstantNode c = (ConstantNode) node;
                return c.value.asObject() instanceof WordUtil.WrappedWord;
            }
            return isWord(node.declaredType(), node);
        }
        return false;
    }

    public boolean isWord(RiType type, ValueNode node) {
        if (!(type instanceof ClassActor)) {
            return false;
        }
        ClassActor actor = (ClassActor) type;
        assert actor.kind == Kind.REFERENCE || actor.kind == Kind.WORD : node;
        return actor.kind == Kind.WORD;
    }

    private void changeToWord(ValueNode valueNode) {
        if (valueNode.kind() == WordUtil.archKind()) {
            return;
        }
        assert valueNode.kind() == CiKind.Object;

        if (valueNode instanceof ConstantNode) {
            ConstantNode c = (ConstantNode) valueNode;
            valueNode = ConstantNode.forCiConstant(((WordUtil.WrappedWord) c.value.asObject()).archConstant(), null, valueNode.graph());
            c.replaceAndDelete(valueNode);
        } else if (valueNode instanceof NullCheckNode) {
            assert valueNode.usages().isEmpty();
            valueNode.safeDelete();
        } else {
            valueNode.setStamp(StampFactory.forKind(WordUtil.archKind()));
        }

        // Propagate word kind.
        for (Node n : valueNode.usages()) {
            if (n instanceof PhiNode || n instanceof ReturnNode) {
                changeToWord((ValueNode) n);
            }
        }
    }
}
