/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.graph;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.max.graal.graph.Graph;
import com.oracle.max.graal.graph.Node;
import com.oracle.max.graal.graph.NodeInputList;
import com.oracle.max.graal.graph.NodeSuccessorList;

public class NodeTest {

    @Test
    public void testBasics() {

        Graph g1 = new Graph(new DummyNode(0, 1));

        DummyNode n2 = g1.add(new DummyNode(1, 1));
        DummyNode n3 = g1.add(new DummyNode(0, 0));
        n2.dummySetInput(0, null);
        n2.dummySetSuccessor(0, n3);

        assertSame(null, n2.inputs().first());
        assertSame(n3, n2.successors().first());
//        assertEquals(n1.inputs().size(), 2);
//        assertEquals(n1.successors().size(), 1);
    }

    @Test
    public void testReplace() {
        Graph g2 = new Graph(new DummyNode(0, 1));

        DummyOp2 o1 = g2.add(new DummyOp2(null, null));
        DummyOp2 o2 = g2.add(new DummyOp2(o1, null));
        DummyOp2 o3 = g2.add(new DummyOp2(o2, null));
        DummyOp2 o4 = g2.add(new DummyOp2(null, null));

        o2.replaceAndDelete(o4);

        assertFalse(o3.inputs().contains(o2));
        assertTrue(o3.inputs().contains(o4));
        assertTrue(o4.usages().contains(o3));
    }

    private static class DummyNode extends Node {

        @Input        private final NodeInputList<Node> inputs;

        @Successor        private final NodeSuccessorList<Node> successors;

        public DummyNode(int inputCount, int successorCount) {
            inputs = new NodeInputList<Node>(this, inputCount);
            successors = new NodeSuccessorList<Node>(this, successorCount);
        }

        public void dummySetInput(int idx, Node n) {
            inputs.set(idx, n);
        }

        public void dummySetSuccessor(int idx, Node n) {
            successors.set(idx, n);
        }
    }

    public static class DummyOp2 extends Node {

        @Input        private Node x;

        @Input        private Node y;

        public Node x() {
            return x;
        }

        public void setX(Node n) {
            updateUsages(x, n);
            x = n;
        }

        public Node y() {
            return y;
        }

        public void setY(Node n) {
            updateUsages(y, n);
            y = n;
        }

        public DummyOp2(Node x, Node y) {
            setX(x);
            setY(y);
        }
    }
}
