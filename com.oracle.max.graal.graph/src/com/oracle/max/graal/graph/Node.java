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
package com.oracle.max.graal.graph;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import com.oracle.max.graal.graph.NodeClass.Position;

/**
 * This class is the base class for all nodes, it represent a node which can be inserted in a {@link Graph}.<br>
 * Once a node has been added to a graph, it has a graph-unique {@link #id()}.
 * Edges in the subclasses are represented with annotated fields. There are two kind of edges : {@link Input} and {@link Successor}.
 * If a field, of a type compatible with {@link Node}, annotated with either {@link Input} and {@link Successor} is not null, then there is an edge
 * from this node to the node this field points to.<br>
 * Fields in a node subclass can also be annotated with {@link Data} : such fields will be used when comparing 2 nodes with {@link #equals(Object)}, for value numbering and for debugging purposes.<br>
 * Nodes which are be value numberable should implement the {@link ValueNumberable} interface.
 */
public abstract class Node implements Cloneable {

    public static final Node Null = null;
    static final int DELETED_ID = -1;
    static final int INITIAL_ID = -2;
    static final int ALIVE_ID_START = 0;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Input {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Successor {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Data {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public static @interface NodeParameter {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface NodeIntrinsic {}

    public interface ValueNumberable {}

    public interface IterableNodeType {}

    private Graph graph;
    private int id;

    // this next pointer is used in Graph to implement fast iteration over NodeClass types, it therefore points to the next Node of the same type.
    Node typeCacheNext;

    private NodeUsagesList usages;
    private Node predecessor;
    private int modCount;
    private NodeClass nodeClass;

    public Node() {
        this.graph = null;
        this.id = INITIAL_ID;
        nodeClass = NodeClass.get(getClass());
    }

    public int id() {
        return id;
    }

    public Graph graph() {
        return graph;
    }

    /**
     * Returns an {@link NodeInputsIterable iterable} which can be used to traverse all non-null input edges of this node.
     * @return an {@link NodeInputsIterable iterable} for all non-null input edges.
     */
    public NodeInputsIterable inputs() {
        return getNodeClass().getInputIterable(this);
    }

    /**
     * Returns an {@link NodeSuccessorsIterable iterable} which can be used to traverse all non-null successor edges of this node.
     * @return an {@link NodeSuccessorsIterable iterable} for all non-null successor edges.
     */
    public NodeSuccessorsIterable successors() {
        return getNodeClass().getSuccessorIterable(this);
    }

    public final NodeUsagesList usages() {
        return usages;
    }

    public final Node predecessor() {
        return predecessor;
    }

    final int modCount() {
        return modCount;
    }

    final void incModCount() {
        modCount++;
    }

    public String shortName() {
        return getNodeClass().shortName();
    }

    public boolean isDeleted() {
        return id == DELETED_ID;
    }

    public boolean isAlive() {
        return id >= ALIVE_ID_START;
    }

    /**
     * Updates the usages sets of the given nodes after an input slot is changed from oldInput to newInput:
     * removes this node from oldInput's usages and adds this node to newInput's usages.
     */
    protected void updateUsages(Node oldInput, Node newInput) {
        assert usages != null : "adding " + newInput + " to " + this;
        if (usages != null && oldInput != newInput) {
            if (oldInput != null) {
                boolean result = oldInput.usages.remove(this);
                assert result;
            }
            if (newInput != null) {
                newInput.usages.add(this);
            }
        }
    }

    /**
     * Updates the predecessor sets of the given nodes after a successor slot is changed from oldSuccessor to newSuccessor:
     * removes this node from oldSuccessor's predecessors and adds this node to newSuccessor's predecessors.
     */
    protected void updatePredecessors(Node oldSuccessor, Node newSuccessor) {
        assert usages != null : "adding " + newSuccessor + " to " + this;
        if (usages != null && oldSuccessor != newSuccessor) {
            if (oldSuccessor != null) {
                assert oldSuccessor.predecessor == this : "expected pred " + this + " at " + oldSuccessor + ", actual: " + oldSuccessor.predecessor;
                oldSuccessor.predecessor = null;
            }
            if (newSuccessor != null) {
                assert newSuccessor.predecessor == null : "expected pred null at " + newSuccessor + ", actual: " + newSuccessor.predecessor + " while trying to set successor on " + this;
                newSuccessor.predecessor = this;
            }
        }
    }

    void initialize(Graph graph) {
        assert id == INITIAL_ID : "unexpected id: " + id;
        this.graph = graph;
        this.id = graph.register(this);
        usages = new NodeUsagesList();
        for (Node input : inputs()) {
            updateUsages(null, input);
        }
        for (Node successor : successors()) {
            updatePredecessors(null, successor);
        }
    }

    public final NodeClass getNodeClass() {
        return nodeClass;
    }

    public <T extends Op> T lookup(Class<T> clazz) {
        return null;
    }

    public void replaceAtUsages(Node other) {
        assert !isDeleted() && (other == null || !other.isDeleted()) : "id: " + id() + ", other: " + other;
        assert other == null || other.graph == graph;
        for (Node usage : usages) {
            boolean result = usage.getNodeClass().replaceFirstInput(usage, this, other);
            assert result : usage + " doesn't contain input " + this;
            if (other != null) {
                other.usages.add(usage);
            }
        }
        usages.clear();
    }

    public void replaceAtPredecessors(Node other) {
        assert !isDeleted() && (other == null || !other.isDeleted()) : "id: " + id() + ", other: " + other;
        if (predecessor != null) {
            boolean result = predecessor.getNodeClass().replaceFirstSuccessor(predecessor, this, other);
            assert result;
            predecessor.updatePredecessors(this, other);
        }
    }

    public void replaceAndDelete(Node other) {
        assert !isDeleted() && (other == null || !other.isDeleted()) : "id: " + id() + ", other: " + other + (other == null ? "" : " (" + other.hashCode() + ")");
        assert other == null || other.graph == graph;
        clearSuccessors();
        replaceAtUsages(other);
        replaceAtPredecessors(other);
        delete();
    }

    public void replaceFirstSuccessor(Node oldSuccessor, Node newSuccessor) {
        if (getNodeClass().replaceFirstSuccessor(this, oldSuccessor, newSuccessor)) {
            updatePredecessors(oldSuccessor, newSuccessor);
        }
    }

    public void replaceFirstInput(Node oldInput, Node newInput) {
        if (getNodeClass().replaceFirstInput(this, oldInput, newInput)) {
            updateUsages(oldInput, newInput);
        }
    }

    public void clearInputs() {
        assert !isDeleted();

        for (Node input : inputs()) {
            input.usages.remove(this);
        }
        getNodeClass().clearInputs(this);
    }

    public void clearSuccessors() {
        assert !isDeleted();

        for (Node successor : successors()) {
            assert successor.predecessor == this : "expected pred " + this + " at " + successor + ", actual: " + successor.predecessor;
            successor.predecessor = null;
        }
        getNodeClass().clearSuccessors(this);
    }

    public void delete() {
        assert checkDeletion() : "Could not delete " + this + " (usages: " + this.usages() + ", predecessor: " + this.predecessor + ")";

        clearInputs();
        clearSuccessors();
        graph.unregister(this);
        id = DELETED_ID;
        assert isDeleted();
    }

    private boolean checkDeletion() {
        if (usages.size() != 0 || predecessor != null) {
            // TODO(ls) this needs to be refactored into an AssertionError subclass with additional information
            System.out.println(this.shortName() + ", id: " + id + ", usages: " + usages.size());
            System.out.println("usages:");
            for (Node n : usages()) {
                System.out.print(n.id() + " (" + n.shortName() + ") ");
            }
            if (predecessor != null) {
                System.out.println("\npreds:" + predecessor.id() + " (" + predecessor.shortName() + ") ");
            }
            System.out.println();
            return false;
        }
        return true;
    }

    public final Node copyWithInputs() {
        Node newNode = clone(graph);
        NodeClass clazz = getNodeClass();
        clazz.copyInputs(this, newNode);
        for (Node input : inputs()) {
            input.usages.add(newNode);
        }
        return newNode;
    }

    public Node clone(Graph into) {
        Node newNode = null;
        try {
            newNode = (Node) this.clone();
        } catch (CloneNotSupportedException e) {
            assert false : "cannot happen";
        }
        getNodeClass().clearInputs(newNode);
        getNodeClass().clearSuccessors(newNode);
        newNode.graph = into;
        newNode.typeCacheNext = null;
        newNode.id = INITIAL_ID;
        newNode.id = into.register(newNode);
        newNode.usages = new NodeUsagesList();
        newNode.predecessor = null;
        newNode.modCount = 0;
        return newNode;
    }

    /**
     * Checks that this node is equal to another node for the purposes of value numbering.
     *
     * @param i the other node
     * @return {@code true} if this instruction is equivalent to the specified instruction w.r.t. value numbering
     */
    public boolean valueEqual(Node i) {
        return getNodeClass().valueEqual(this, i);
    }

    /**
     * Provides a {@link Map} of properties of this node for use in debugging (e.g., to view in the ideal graph
     * visualizer). Subclasses overriding this method should add to the map returned by their superclass.
     */
    public Map<Object, Object> getDebugProperties() {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("usageCount", usages.size());
        map.put("predecessorCount", predecessor == null ? 0 : 1);
        getNodeClass().getDebugProperties(this, map);
        return map;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-" + this.id();
    }

    public boolean verify() {
        assertTrue(isAlive());
        for (Node input : inputs()) {
            assertTrue(input.usages().contains(this));
            assertTrue(input.graph() == graph());
        }
        for (Node successor : successors()) {
            assertTrue(successor.predecessor() == this, "pred of " + successor + " should be: " + this + ", is: " + successor.predecessor());
            assertTrue(successor.graph() != null, "successor " + successor + " is assigned to no graph!");
            assertTrue(successor.graph() == graph(), "successor " + successor + " is assigned to a different graph!");
        }
        for (Node usage : usages()) {
            assertTrue(usage.inputs().contains(this));
        }
        if (predecessor != null) {
            assertTrue(predecessor.successors().contains(this));
        }
        return true;
    }

    public final void assertTrue(boolean cond) {
        assert cond || assertionFailure("");
    }

    public final void assertTrue(boolean cond, String message) {
        assert cond || assertionFailure(message);
    }

    public final boolean assertionFailure(String message) {
        for (VerificationListener l : Graph.verificationListeners) {
            l.verificationFailed(this, message);
        }
        return true;
    }

    public Iterable<? extends Node> cfgPredecessors() {
        if (predecessor == null) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(predecessor);
        }
    }

    /**
     * Returns an iterator that will provide all control-flow successors of this node. Normally this will be the contents of all fields marked as NodeSuccessor,
     * but some node classes (like EndNode) may return different nodes.
     * Note that the iterator may generate null values if the fields contain them.
     */
    public Iterable<? extends Node> cfgSuccessors() {
        return successors();
    }

    Node get(Position pos) {
        return this.getNodeClass().get(this, pos);
    }

    void set(Position pos, Node value) {
        this.getNodeClass().set(this, pos, value);
    }

    /**
     * Compute the value number of this node. Local and global value numbering optimizations use a hash map, and the
     * value number provides a hash code.
     *
     * @return the hashcode of this instruction
     */
    public int valueNumber() {
        return getNodeClass().valueNumber(this);
    }

    @Override
    public int hashCode() {
        return valueNumber();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Node) {
            Node node = (Node) obj;
            if (node.getNodeClass() == getNodeClass()) {
                if (node.getNodeClass().valueNumberable()) {
                    return valueEqual(node) && getNodeClass().edgesEqual(this, node);
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
