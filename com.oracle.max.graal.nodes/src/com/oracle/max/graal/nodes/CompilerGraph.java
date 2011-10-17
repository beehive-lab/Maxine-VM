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
package com.oracle.max.graal.nodes;

import java.util.*;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.Node.ValueNumberable;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class CompilerGraph extends Graph {

    private final RiRuntime runtime;
    private ReturnNode returnSingleton;
    private UnwindNode unwindSingleton;
    private final CiAssumptions assumptions = new CiAssumptions();

    public CompilerGraph(RiRuntime runtime) {
        this(new EntryPointNode(), runtime);
    }

    public CompilerGraph(EntryPointNode entryPoint, RiRuntime runtime) {
        super(entryPoint);
        assert runtime != null;
        this.runtime = runtime;
    }

    public void setReturn(ReturnNode returnNode) {
        assert returnSingleton == null;
        returnSingleton = returnNode;
    }

    public ReturnNode getReturn() {
        return returnSingleton;
    }

    public void setUnwind(UnwindNode unwind) {
        assert unwindSingleton == null;
        unwindSingleton = unwind;
    }

    public UnwindNode getUnwind() {
        return unwindSingleton;
    }

    public RiRuntime runtime() {
        return runtime;
    }

    public CiAssumptions assumptions() {
        return assumptions;
    }

    private final HashMap<Node, Node> cachedNodes = new HashMap<Node, Node>();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node & ValueNumberable> T unique(T node) {
        assert node.getNodeClass().valueNumberable();
        if (!node.getNodeClass().hasOutgoingEdges()) {
            Node cachedNode = cachedNodes.get(node);
            if (cachedNode != null && cachedNode.isAlive()) {
                return (T) cachedNode;
            } else {
                Node result = super.unique(node);
                cachedNodes.put(result, result);
                return (T) result;
            }
        } else {
            Node duplicate = findDuplicate(node);
            if (duplicate != null) {
                return (T) duplicate;
            }
            return super.unique(node);
        }
    }

    public Node findDuplicate(Node node) {
        if (node.getNodeClass().valueNumberable()) {
            Node firstInput = node.inputs().first();
            if (firstInput != null) {
                for (Node usage : firstInput.usages()) {
                    if (usage != node && node.valueEqual(usage) && node.getNodeClass().edgesEqual(node, usage)) {
                        return usage;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean verify() {
        super.verify();
        assert start().next() != null || (getReturn() == null && getUnwind() == null);
        return true;
    }

    @Override
    public EntryPointNode start() {
        return (EntryPointNode) super.start();
    }
}
