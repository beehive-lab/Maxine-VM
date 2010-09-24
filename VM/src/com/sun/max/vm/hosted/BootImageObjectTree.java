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
package com.sun.max.vm.hosted;

import java.io.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.hosted.GraphPrototype.*;
import com.sun.max.vm.object.host.*;

/**
 * A mechanism for saving, loading and printing the
 * causality spanning-tree of the object graph in an {@linkplain BootImageGenerator image}.
 *
 * @author Doug Simon
 */
public final class BootImageObjectTree {

    private BootImageObjectTree() {
    }

    /**
     * The maximum length of an object's {@linkplain Object#toString() string} representation included in the saved
     * tree.
     */
    private static final int MAX_OBJECT_TOSTRING_LENGTH = 200;

    /**
     * The form of an object's {@linkplain Object#toString() string} representation included in the saved tree.
     *
     * @author Doug Simon
     */
    enum TO_STRING_TAG {
        /**
         * An object that does not override {@link Object#toString()}.
         */
        DEFAULT,

        /**
         * An object whose implementation of {@link Object#toString()} returns null.
         */
        NULL,

        /**
         * An object whose implementation of {@link Object#toString()} returns a custom value.
         */
        CUSTOM;

        public static final List<TO_STRING_TAG> VALUES = Arrays.asList(values());
    }

    /**
     * A node in the causality tree of the object graph. An edge from the object graph is a parent-child link in this
     * tree if traversing that edge during {@linkplain GraphPrototype object graph serialization} caused the child to be
     * added to the image.
     */
    public static class Node {

        private final String className;
        private final int address;
        private final long size;
        private long aggregateSize = 0L;
        private final String toString;

        private List<Node> children;
        private String parentLink;

        /**
         * Creates a new node with the specified class name, address, size, and verbose string.
         *
         * @param className the name of the class of this object
         * @param address the address of this object
         * @param size the size of this object in bytes
         * @param toString the verbose string for this object
         */
        public Node(String className, int address, long size, String toString) {
            this.className = className;
            this.address = address;
            this.size = size;
            this.toString = toString;
        }

        /**
         * Adds a child to this node.
         *
         * @param child the child node
         * @param link the name of the link that links this child to its parent, e.g.
         * the name of an object field
         */
        void addChild(Node child, String link) {
            if (children == null) {
                children = new ArrayList<Node>();
            }
            children.add(child);
            child.parentLink = link;
        }

        /**
         * Returns a sequence of the children of this node.
         *
         * @return a sequence of the children
         */
        public List<Node> children() {
            List<Node> empty = Collections.emptyList();
            return children == null ? empty : children;
        }

        /**
         * Prune this graph so that only nodes that match the specified predicate (and their
         * parent nodes) are retained.
         *
         * @param predicate the predicate to apply to each node recursively
         * @return {@code null} if the predicate does not apply to either this
         * node or any child node; this node with the non-matching children removed if
         * the predicate matches this node or a child node
         */
        public Node prune(Predicate<Node> predicate) {
            final Node pruned = new Node(className, address, size, toString);
            pruned.parentLink = parentLink;
            pruned.aggregateSize = aggregateSize();
            if (children != null) {
                for (Node child : children) {
                    final Node prunedChild = child.prune(predicate);
                    if (prunedChild != null) {
                        pruned.addChild(prunedChild, prunedChild.parentLink);
                    }
                }
            }
            if (pruned.children != null || predicate.evaluate(this)) {
                return pruned;
            }
            return null;
        }

        /**
         * Computes the total size, including the size of the children.
         *
         * @return the size of this node plus the sum of the sizes of all its children nodes
         */
        public long aggregateSize() {
            if (aggregateSize == 0) {
                aggregateSize = size;
                for (Node child : children()) {
                    aggregateSize += child.aggregateSize();
                }
            }
            return aggregateSize;
        }

        private static final String OTHER_SIBLING_PREFIX = "`-- ";
        private static final String LAST_SIBLING_PREFIX = "|-- ";
        private static final String OTHER_CHILD_INDENT = "|   ";
        private static final String LAST_CHILD_INDENT = "    ";

        /**
         * Print this thee to the specified print writer.
         *
         * @param node the node to print
         * @param showTreeLines a boolean indicating whether to print the tree lines
         * @param printWriter the writer to which to print the output
         * @param prefix the prefix to this node
         * @param lastChild a boolean indicating whether to print the last child
         * @param addressRadix the numbering radix for printing addresses (e.g. decimal or hexadecimal)
         * @param relocation the address to add to each address for relocation
         */
        private static void printTree(Node node, boolean showTreeLines, PrintWriter printWriter, String prefix, boolean lastChild, int addressRadix, long relocation) {
            printWriter.println(prefix + (showTreeLines ? (!lastChild ? LAST_SIBLING_PREFIX : OTHER_SIBLING_PREFIX) : "    ") + node.toString(addressRadix, relocation));
            final List<Node> children = node.children();
            if (!children.isEmpty()) {
                final String childPrefix = prefix + (showTreeLines ? (lastChild ? LAST_CHILD_INDENT : OTHER_CHILD_INDENT) : "    ");
                for (final Iterator<Node> iterator = children.iterator(); iterator.hasNext();) {
                    final Node child = iterator.next();
                    printTree(child, showTreeLines, printWriter, childPrefix, !iterator.hasNext(), addressRadix, relocation);
                }
            }
        }

        /**
         * Convert this node to a string.
         * @return a string representation of this node
         */
        @Override
        public String toString() {
            return toString(10, 0);
        }

        /**
         * Convert this node to a string with the specified formatting.
         * @param addressRadix the number radix for printing the address (e.g. decimal or hexadecimal)
         * @param relocation the address to add to each address for relocation
         * @return a string representation of this node
         */
        public String toString(int addressRadix, long relocation) {
            final StringBuilder sb = new StringBuilder();
            if (parentLink != null) {
                sb.append(parentLink).append(' ');
            }
            sb.append(className).append('@').append(Long.toString(address + relocation, addressRadix));
            sb.append(" size=").append(size).append(" aggregateSize=").append(aggregateSize());
            if (toString != null) {
                sb.append("   toString=").append(toString.replace('\n', '0'));
            }
            return sb.toString();
        }
    }

    /**
     * Saves the spanning tree of an object graph as represented by the links in the graph that were traversed.
     *
     * @param dataOutputStream
     *                where to save the tree
     * @param links
     *                the traversed links of the graph that describe the tree to be saved
     */
    public static void saveTree(DataOutputStream dataOutputStream, Set<Map.Entry<Object, Link>> links, Map<Object, Address> allocationMap) throws IOException {
        final Map<Class, Integer> classPool = new HashMap<Class, Integer>();
        final Map<Object, Integer> objectPool = new IdentityHashMap<Object, Integer>();
        final Class[] classPoolIndices = new Class[links.size()];
        final Object[] objectPoolIndices = new Object[links.size()];
        int objectId = 0;
        int classId = 0;
        objectPool.put(null, objectId++);
        for (Map.Entry<Object, Link> entry : links) {
            final Object object = entry.getKey();
            if (object != null) {
                final Class< ? > clazz = object.getClass();
                if (!classPool.containsKey(clazz)) {
                    classPoolIndices[classId] = clazz;
                    classPool.put(clazz, classId++);
                }
                objectPoolIndices[objectId] = object;
                objectPool.put(object, objectId++);
            }
        }
        assert objectId == objectPool.size();
        assert classId == classPool.size();
        Trace.line(1, "classes=" + classId + ", objects=" + objectId + ", links=" + links.size());

        dataOutputStream.writeInt(classPool.size());
        for (int i = 0; i != classPool.size(); ++i) {
            if (i % 100 == 0) {
                Trace.line(1, "class: " + i);
            }
            dataOutputStream.writeUTF(classPoolIndices[i].getName());
        }

        dataOutputStream.writeInt(objectId);
        for (int i = 1; i < objectId; ++i) {
            if (i % 100000 == 0) {
                Trace.line(1, "object: " + i);
            }
            final Object object = objectPoolIndices[i];
            final int address = allocationMap.get(object).toInt();
            final long size = HostObjectAccess.getSize(object).toLong();
            final String toString = object.toString();

            dataOutputStream.writeInt(classPool.get(object.getClass()));
            dataOutputStream.writeInt(address);
            dataOutputStream.writeLong(size);
            if (toString == null) {
                dataOutputStream.writeByte(TO_STRING_TAG.NULL.ordinal());
            } else {
                final String defaultToString = object.getClass().getName() + '@' + Integer.toHexString(object.hashCode());
                if (toString.equals(defaultToString)) {
                    dataOutputStream.writeByte(TO_STRING_TAG.DEFAULT.ordinal());
                } else {
                    dataOutputStream.writeByte(TO_STRING_TAG.CUSTOM.ordinal());
                    dataOutputStream.writeUTF(Strings.truncate(toString, MAX_OBJECT_TOSTRING_LENGTH));
                }
            }
        }

        int counter = 0;
        for (Map.Entry<Object, Link> entry : links) {
            final Object object = entry.getKey();
            if (object != null) {
                if (++counter % 100000 == 0) {
                    Trace.line(1, "link: " + counter);
                }
                final Link link = entry.getValue();
                dataOutputStream.writeInt(objectPool.get(object));
                if (link == null) {
                    dataOutputStream.writeInt(0);
                } else {
                    final int parent = objectPool.get(link.parent);
                    final String name = link.name();
                    dataOutputStream.writeInt(parent);
                    dataOutputStream.writeUTF(name);
                }
            }
        }
        assert counter + 1 == objectId;
    }

    /**
     * Loads a tree that was saved by {@linkplain #saveTree(DataOutputStream, Set) this method}.
     *
     * @param dataInputStream
     *                a stream containing a saved tree
     * @return the roots of the loaded tree
     */
    public static Set<Node> loadTree(DataInputStream dataInputStream) throws IOException {
        final Set<Node> roots = new HashSet<Node>();
        final int classPoolSize = dataInputStream.readInt();
        final String[] classNames = new String[classPoolSize];
        for (int i = 0; i != classPoolSize; ++i) {
            if (i % 100 == 0) {
                Trace.line(1, "class: " + i);
            }
            final String className = dataInputStream.readUTF();
            classNames[i] = className;
        }

        final int objectPoolSize = dataInputStream.readInt();
        final Node[] objectPool = new Node[objectPoolSize];
        for (int i = 1; i < objectPoolSize; ++i) {
            if (i % 100000 == 0) {
                Trace.line(1, "object: " + i);
            }
            final int classNameIndex = dataInputStream.readInt();
            final int address = dataInputStream.readInt();
            final long size = dataInputStream.readLong();
            final String className = classNames[classNameIndex];
            final TO_STRING_TAG tag = TO_STRING_TAG.VALUES.get(dataInputStream.readByte());

            final String toString;
            switch (tag) {
                case DEFAULT:
                case NULL:
                    toString = null;
                    break;
                case CUSTOM:
                    toString = dataInputStream.readUTF();
                    break;
                default:
                    throw ProgramError.unexpected();
            }
            objectPool[i] = new Node(className, address, size, toString);
        }

        for (int i = 1; i < objectPoolSize; ++i) {
            if (i % 100000 == 0) {
                Trace.line(1, "link: " + i);
            }
            final int objectIndex = dataInputStream.readInt();
            final int parentIndex = dataInputStream.readInt();
            final Node node = objectPool[objectIndex];
            if (parentIndex != 0) {
                objectPool[parentIndex].addChild(node, dataInputStream.readUTF());
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    /**
     * Prints the tree rooted at a given node to a given print writer. The format of the dump is similar to the output
     * of the tree(1) utility that list contents of directories in a tree-like format.
     *
     * @param node
     *                the root of the tree to dump
     * @param printWriter
     *                where to print the dump
     * @param lastChild
     *                specifies if {@code node} is the last child of its parent that will be dumped
     * @param addressRadix
     *                radix to use for printing addresses
     * @param relocation
     */
    public static void printTree(Node node, boolean showTreeLines, PrintWriter printWriter, boolean lastChild, int addressRadix, long relocation) {
        Node.printTree(node, showTreeLines, printWriter, "", lastChild, addressRadix, relocation);
    }

    private static final OptionSet options = new OptionSet();

    private static final Option<Integer> TRACE = options.newIntegerOption("trace", 1,
            "selects the trace level of the tool");
    private static final Option<File> INPUT_FILE = options.newFileOption("in", BootImageGenerator.getBootImageObjectTreeFile(null),
            "the file from which to load the graph");
    private static final Option<File> OUTPUT_FILE = options.newFileOption("out", getDefaultOutputFile(),
            "the file to which the graph is printed");
    private static final Option<String> FILTER = options.newStringOption("filter", null,
            "filter for pruning the graph before printing");
    private static final Option<Integer> RADIX = options.newIntegerOption("radix", 10,
            "radix used for printing addresses");
    private static final Option<Long> RELOC = options.newLongOption("reloc", 0L,
            "relocation addresses by the specified amount while printing");
    private static final Option<Boolean> SHOW_TREE_LINES = options.newBooleanOption("lines", true,
           "show lines (instead of indentation only) to indicate relationships between nodes");
    private static final Option<Boolean> HELP = options.newBooleanOption("help", false,
           "show help message and exits.");

    /**
     * Gets the default file name to which to output the image object tree.
     * @return a file representing the default location for outputting the tree
     */
    private static File getDefaultOutputFile() {
        return new File(BootImageGenerator.getBootImageObjectTreeFile(null).getAbsolutePath() + ".txt");
    }

    /**
     * Command line interface for loading a saved graph and printing it, optionally pruning it with a provided filter
     * first.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        options.parseArguments(args);
        if (HELP.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }
        Trace.on(TRACE.getValue());

        final InputStream inputStream = openInputFile();
        if (inputStream == null) {
            System.err.println("Cannot find input file " + INPUT_FILE.getValue().getAbsolutePath());
            System.err.println("The input tree file is only created if the -tree option is used when creating the boot image.");
            System.exit(1);
        }

        final DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
        final Set<Node> roots = loadTree(dataInputStream);
        inputStream.close();

        final File outputFile = OUTPUT_FILE.getValue();
        final Writer writer = new FileWriter(outputFile);
        final PrintWriter printWriter = new PrintWriter(new BufferedWriter(writer)) {

            private int counter;

            @Override
            public void println(String s) {
                if (++counter % 100000 == 0) {
                    Trace.line(1, "node: " + counter);
                }
                super.println(s);
            }

        };

        if (FILTER.getValue() != null) {
            final String filter = FILTER.getValue();
            final Set<Node> prunedRoots = new HashSet<Node>(roots.size());
            for (Node root : roots) {
                final Node prunedRoot = root.prune(new Predicate<Node>() {
                    public boolean evaluate(Node object) {
                        return object.toString().contains(filter);
                    }
                });
                if (prunedRoot != null) {
                    prunedRoots.add(prunedRoot);
                }
            }
            roots.clear();
            roots.addAll(prunedRoots);
        }

        Trace.begin(1, "writing boot image object tree text file: " + outputFile.getAbsolutePath());
        for (final Iterator<Node> iterator = roots.iterator(); iterator.hasNext();) {
            final Node root = iterator.next();
            Node.printTree(root, SHOW_TREE_LINES.getValue(), printWriter, "", !iterator.hasNext(), RADIX.getValue(), RELOC.getValue());
        }
        printWriter.close();
        Trace.end(1, "writing boot image object tree text file: " + outputFile.getAbsolutePath());
    }

    private static FileInputStream openInputFile() {
        try {
            return new FileInputStream(INPUT_FILE.getValue());
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
