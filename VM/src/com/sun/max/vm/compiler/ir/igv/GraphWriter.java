/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=f5d6a493-3340-468a-9b3e-ea62b5078990*/
package com.sun.max.vm.compiler.ir.igv;

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;

/**
 * Utility class for writing a graph document as an XML stream.
 *
 * @author Thomas Wuerthinger
 */
public class GraphWriter {

    public static final String TOP_ELEMENT = "graphDocument";
    public static final String GROUP_ELEMENT = "group";
    public static final String GRAPH_ELEMENT = "graph";
    public static final String ROOT_ELEMENT = "graphDocument";
    public static final String PROPERTIES_ELEMENT = "properties";
    public static final String EDGES_ELEMENT = "edges";
    public static final String PROPERTY_ELEMENT = "p";
    public static final String EDGE_ELEMENT = "edge";
    public static final String NODE_ELEMENT = "node";
    public static final String NODES_ELEMENT = "nodes";
    public static final String METHOD_NAME_PROPERTY = "name";
    public static final String GROUP_NAME_PROPERTY = "name";
    public static final String TRUE_VALUE = "true";
    public static final String NODE_NAME_PROPERTY = "name";
    public static final String EDGE_NAME_PROPERTY = "name";
    public static final String NODE_ID_PROPERTY = "id";
    public static final String FROM_PROPERTY = "from";
    public static final String TO_PROPERTY = "to";
    public static final String PROPERTY_NAME_PROPERTY = "name";
    public static final String GRAPH_NAME_PROPERTY = "name";
    public static final String TO_INDEX_PROPERTY = "index";
    public static final String FROM_INDEX_PROPERTY = "fromIndex";
    public static final String METHOD_ELEMENT = "method";
    public static final String INLINE_ELEMENT = "inline";
    public static final String BYTECODES_ELEMENT = "bytecodes";
    public static final String METHOD_BCI_PROPERTY = "bci";
    public static final String METHOD_SHORT_NAME_PROPERTY = "shortName";
    public static final String CONTROL_FLOW_ELEMENT = "controlFlow";
    public static final String BLOCK_NAME_PROPERTY = "name";
    public static final String BLOCK_ELEMENT = "block";
    public static final String SUCCESSORS_ELEMENT = "successors";
    public static final String SUCCESSOR_ELEMENT = "successor";
    public static final String ASSEMBLY_ELEMENT = "assembly";
    public static final String DIFFERENCE_PROPERTY = "difference";
    public static final String REMOVE_EDGE_ELEMENT = "removeEdge";
    public static final String REMOVE_NODE_ELEMENT = "removeNode";

    private static class PropertyObject {

        private Properties _properties;

        public PropertyObject() {
            _properties = new Properties();
        }

        public Properties getProperties() {
            return _properties;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PropertyObject) {
                final PropertyObject other = (PropertyObject) obj;
                return other.getProperties().equals(getProperties());
            }
            return false;
        }
    }

    public static final class Node extends PropertyObject {

        private int _id;
        private Graph _graph;
        private Block _block;

        private Node(Graph graph, int id) {
            _graph = graph;
            _id = id;
        }

        public Graph getGraph() {
            return _graph;
        }

        public int getId() {
            return _id;
        }

        @Override
        public int hashCode() {
            return _id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Node) {
                final Node other = (Node) obj;
                if (other._id != _id) {
                    return false;
                }
                return super.equals(obj);
            }
            return false;
        }
    }

    public static final class Edge {

        private int _from;
        private int _fromIndex;
        private int _to;
        private int _toIndex;
        private Graph _graph;

        private Edge(Graph graph, int from, int fromIndex, int to, int toIndex) {
            _graph = graph;
            _from = from;
            _fromIndex = fromIndex;
            _to = to;
            _toIndex = toIndex;
        }

        public Graph getGraph() {
            return _graph;
        }

        public int getFrom() {
            return _from;
        }

        public int getFromIndex() {
            return _fromIndex;
        }

        public int getTo() {
            return _to;
        }

        public int getToIndex() {
            return _toIndex;
        }

        @Override
        public int hashCode() {
            return _fromIndex << 28 + _toIndex << 24 + _from << 12 + _to;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Edge) {
                final Edge other = (Edge) obj;
                if (other._from == _from && other._fromIndex == _fromIndex && other._to == _to && other._toIndex == _toIndex) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Block {

        private Graph _graph;
        private String _name;
        private AppendableSequence<Block> _successors;
        private AppendableSequence<Block> _predecessors;
        private AppendableSequence<Node> _nodes;

        private Block(Graph graph, String name) {
            _graph = graph;
            _name = name;
            _successors = new LinkSequence<Block>();
            _predecessors = new LinkSequence<Block>();
            _nodes = new LinkSequence<Node>();
        }

        public Sequence<Node> getNodes() {
            return _nodes;
        }

        public Graph getGraph() {
            return _graph;
        }

        public Sequence<Block> getSuccessors() {
            return _successors;
        }

        public Sequence<Block> getPredecessors() {
            return _predecessors;
        }

        public void addNode(Node n) {
            assert n.getGraph() == getGraph();
            assert n._block == null;
            n._block = this;
            _nodes.append(n);
        }

        public void addSuccessor(Block b) {
            assert !Sequence.Static.containsIdentical(_successors, b);
            assert b.getGraph() == _graph;
            _successors.append(b);
        }

        public void addPredecessor(Block b) {
            assert !Sequence.Static.containsIdentical(_predecessors, b);
            assert b.getGraph() == _graph;
            _predecessors.append(b);
        }

        public String getName() {
            return _name;
        }
    }

    public static final class Graph extends PropertyObject {

        private Set<Node> _nodes;
        private Set<Edge> _edges;
        private AppendableSequence<Block> _blocks;
        private GrowableMapping<Integer, Node> _nodesMapping;

        private Graph(String name) {
            this.getProperties().setProperty(GRAPH_NAME_PROPERTY, name);
            _nodes = new HashSet<Node>();
            _edges = new HashSet<Edge>();
            _blocks = new LinkSequence<Block>();
            _nodesMapping = new ChainedHashMapping<Integer, Node>();
        }

        public Collection<Node> getNodes() {
            return _nodes;
        }

        public Node createNode(int id) {
            assert !_nodesMapping.containsKey(id);
            final Node n = new Node(this, id);
            _nodes.add(n);
            _nodesMapping.put(id, n);
            return n;
        }

        public Node getNode(int id) {
            return _nodesMapping.get(id);
        }

        public Collection<Edge> getEdges() {
            return _edges;
        }

        public Edge createEdge(int from, int to) {
            return createEdge(from, 0, to, 0);
        }

        public Edge createEdge(int from, int fromIndex, int to, int toIndex) {
            final Edge e = new Edge(this, from, fromIndex, to, toIndex);
            _edges.add(e);
            return e;
        }

        public Block createBlock(String name) {
            final Block b = new Block(this, name);
            _blocks.append(b);
            return b;
        }

        public Sequence<Block> getBlocks() {
            return _blocks;
        }
    }

    public static class Group extends PropertyObject {

        private AppendableSequence<Graph> _graphs;
        private Method _method;

        public Group(String name) {
            this.getProperties().setProperty(METHOD_NAME_PROPERTY, name);
            _graphs = new LinkSequence<Graph>();
        }

        public Graph createGraph(String name) {
            final Graph g = new Graph(name);
            _graphs.append(g);
            return g;
        }

        public Sequence<Graph> getGraphs() {
            return _graphs;
        }

        public Method getMethod() {
            return _method;
        }

        public Method createMethod(int bci, String name, String shortName, String bytecodes) {
            assert _method == null : "Only one method per group may be created.";
            final Method m = new Method(bci, name, shortName, bytecodes);
            _method = m;
            return m;
        }
    }

    public static class Method {

        private int _bci;
        private String _name;
        private String _shortName;
        private String _bytecodes;
        private AppendableSequence<Method> _inlinedMethods;

        public Method(int bci, String name, String shortName, String bytecodes) {
            _bci = bci;
            _name = name;
            _shortName = shortName;
            _bytecodes = bytecodes;
            _inlinedMethods = new LinkSequence<Method>();
        }

        public Sequence<Method> getInlinedMethods() {
            return _inlinedMethods;
        }

        public Method createInlined(int bci, String name, String shortName, String bytecodes) {
            final Method m = new Method(bci, name, shortName, bytecodes);
            _inlinedMethods.append(m);
            return m;
        }

        public String getBytecodes() {
            return _bytecodes;
        }

        public int getBci() {
            return _bci;
        }

        public String getName() {
            return _name;
        }

        public String getShortName() {
            return _shortName;
        }
    }

    public static final class Document extends PropertyObject {

        private VariableSequence<Group> _groups;

        private Document() {
            _groups = new ArrayListSequence<Group>();
        }

        public Sequence<Group> getGroups() {
            return _groups;
        }

        public void removeGroup(Group g) {
            assert Sequence.Static.containsIdentical(_groups, g);
            _groups.remove(Sequence.Static.indexOfIdentical(_groups, g));
        }

        public Group createGroup(String name) {
            final Group g = new Group(name);
            _groups.append(g);
            return g;
        }
    }

    private XMLWriter _out;

    public GraphWriter(Writer out) {
        this._out = new XMLWriter(out);
    }

    public static Document createDocument() {
        return new Document();
    }

    public void write(Document d) throws IOException {
        _out.begin(TOP_ELEMENT);
        writeProperties(d.getProperties());
        for (Group g : d.getGroups()) {
            writeGroup(g);
        }
        _out.end(TOP_ELEMENT);
    }

    public void close() throws IOException {
        this._out.close();
    }

    private void writeMethod(Method m) throws IOException {
        final Properties p = new Properties();
        p.setProperty(METHOD_BCI_PROPERTY, Integer.toString(m.getBci()));
        p.setProperty(METHOD_NAME_PROPERTY, m.getName());
        p.setProperty(METHOD_SHORT_NAME_PROPERTY, m.getShortName());
        _out.begin(METHOD_ELEMENT, p);

        if (m.getInlinedMethods().length() > 0) {
            _out.begin(INLINE_ELEMENT);
            for (Method im : m.getInlinedMethods()) {
                writeMethod(im);
            }
            _out.end(INLINE_ELEMENT);
        }

        _out.begin(BYTECODES_ELEMENT);
        _out.writeData(m.getBytecodes());
        _out.end(BYTECODES_ELEMENT);

        _out.end(METHOD_ELEMENT);
    }

    private void writeGroup(Group g) throws IOException {
        _out.begin(GROUP_ELEMENT, DIFFERENCE_PROPERTY, TRUE_VALUE);
        writeProperties(g.getProperties());

        Graph previous = null;
        for (Graph graph : g.getGraphs()) {
            writeGraph(graph, previous);
            previous = graph;
        }

        if (g.getMethod() != null) {
            writeMethod(g.getMethod());
        }

        _out.end(GROUP_ELEMENT);
    }

    private void writeNode(Node n) throws IOException {
        _out.begin(NODE_ELEMENT, NODE_ID_PROPERTY, Integer.toString(n.getId()));
        writeProperties(n.getProperties());
        _out.end(NODE_ELEMENT);
    }

    private void writeEdge(Edge e, String elementName) throws IOException {
        final Properties p = new Properties();
        p.setProperty(FROM_PROPERTY, Integer.toString(e.getFrom()));
        p.setProperty(FROM_INDEX_PROPERTY, Integer.toString(e.getFromIndex()));
        p.setProperty(TO_PROPERTY, Integer.toString(e.getTo()));
        p.setProperty(TO_INDEX_PROPERTY, Integer.toString(e.getToIndex()));
        _out.simple(elementName, p);
    }

    private void writeNodeRemoval(Node n) throws IOException {
        _out.simple(REMOVE_NODE_ELEMENT, NODE_ID_PROPERTY, Integer.toString(n.getId()));
    }

    private void writeGraph(Graph g, Graph previous) throws IOException {
        _out.begin(GRAPH_ELEMENT);
        writeProperties(g.getProperties());

        _out.begin(NODES_ELEMENT);
        for (Node n : g.getNodes()) {
            Node prev = null;
            if (previous != null) {
                prev = previous.getNode(n.getId());
            }
            if (prev == null || !prev.equals(n)) {
                writeNode(n);
            }
        }

        if (previous != null) {
            for (Node n : previous.getNodes()) {
                if (g.getNode(n.getId()) == null) {
                    writeNodeRemoval(n);
                }
            }
        }

        _out.end(NODES_ELEMENT);
        _out.begin(EDGES_ELEMENT);

        for (Edge e : g.getEdges()) {
            if (previous == null || !previous.getEdges().contains(e)) {
                writeEdge(e, EDGE_ELEMENT);
            }
        }

        if (previous != null) {
            for (Edge e : previous.getEdges()) {
                if (!g.getEdges().contains(e)) {
                    writeEdge(e, REMOVE_EDGE_ELEMENT);
                }
            }
        }

        _out.end(EDGES_ELEMENT);

        _out.begin(CONTROL_FLOW_ELEMENT);
        for (Block b : g.getBlocks()) {
            writeBlock(b);
        }
        _out.end(CONTROL_FLOW_ELEMENT);

        _out.end(GRAPH_ELEMENT);
    }

    private void writeBlock(Block b) throws IOException {
        _out.begin(BLOCK_ELEMENT, BLOCK_NAME_PROPERTY, b.getName());
        _out.begin(SUCCESSORS_ELEMENT);
        for (Block s : b.getSuccessors()) {
            _out.simple(SUCCESSORS_ELEMENT, BLOCK_NAME_PROPERTY, s.getName());
        }
        _out.end(SUCCESSORS_ELEMENT);
        _out.begin(NODES_ELEMENT);
        for (Node n : b.getNodes()) {
            _out.simple(NODE_ELEMENT, NODE_ID_PROPERTY, Integer.toString(n.getId()));
        }
        _out.end(NODES_ELEMENT);
        _out.end(BLOCK_ELEMENT);
    }

    private void writeProperty(String name, String value) throws IOException {
        _out.begin(PROPERTY_ELEMENT, PROPERTY_NAME_PROPERTY, name);
        _out.write(value);
        _out.end(PROPERTY_ELEMENT);

    }

    private void writeProperties(Properties properties) throws IOException {

        if (properties.size() == 0) {
            return;
        }

        _out.begin(PROPERTIES_ELEMENT);
        final Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            final String propertyName = (String) e.nextElement();
            writeProperty(propertyName, properties.getProperty(propertyName));
        }
        _out.end(PROPERTIES_ELEMENT);
    }
}
