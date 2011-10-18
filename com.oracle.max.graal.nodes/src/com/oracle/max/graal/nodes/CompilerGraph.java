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

    private final HashMap<CacheEntry, Node> cachedNodes = new HashMap<CacheEntry, Node>();

    private static final class CacheEntry {

        private final Node node;

        public CacheEntry(Node node) {
            this.node = node;
        }

        @Override
        public int hashCode() {
            return node.getNodeClass().valueNumber(node);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Node) {
                Node other = (Node) obj;
                NodeClass nodeClass = node.getNodeClass();
                if (other.getNodeClass() == nodeClass) {
                    return nodeClass.valueNumberable() && nodeClass.valueEqual(node, other) && nodeClass.edgesEqual(node, other);
                }
            }
            return false;
        }
    }

    private boolean checkValueNumberable(Node node) {
        if (!node.getNodeClass().valueNumberable()) {
            throw new VerificationError("node is not valueNumberable").addContext(node);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node & ValueNumberable> T unique(T node) {
        assert checkValueNumberable(node);
        if (!node.getNodeClass().hasOutgoingEdges()) {
            Node cachedNode = cachedNodes.get(new CacheEntry(node));
            if (cachedNode != null && cachedNode.isAlive()) {
                return (T) cachedNode;
            } else {
                Node result = super.unique(node);
                cachedNodes.put(new CacheEntry(result), result);
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
            for (Node input : node.inputs()) {
                if (input != null) {
                    for (Node usage : input.usages()) {
                        if (usage != node && node.getNodeClass().valueEqual(node, usage) && node.getNodeClass().edgesEqual(node, usage)) {
                            return usage;
                        }
                    }
                    return null;
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
