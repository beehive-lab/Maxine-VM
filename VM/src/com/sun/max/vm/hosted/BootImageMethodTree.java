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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.util.*;
import com.sun.max.vm.hosted.CompiledPrototype.*;
import com.sun.max.vm.hosted.CompiledPrototype.Link.*;

/**
 * A mechanism for {@linkplain #saveTree(DataOutputStream, IterableWithLength) saving},
 * {@linkplain #loadTree(DataInputStream) loading} and
 * {@linkplain #printTree(Node, boolean, PrintWriter, boolean) printing}
 * the causality spanning-tree of the {@linkplain CompiledPrototype#links() method graph} in an
 * {@linkplain BootImageGenerator image}.
 *
 * @author Doug Simon
 */
public final class BootImageMethodTree {

    private BootImageMethodTree() {
    }

    /**
     * A node in the method tree, which contains links to subnodes. Each link has a specified relationship,
     * which helps to diagnose exactly why a node was included into the graph.
     */
    public static class Node {

        private final String name;
        private List<Node> referents;
        private Relationship relationshipToReferrer;

        /**
         * Creates a new node with the specified name.
         *
         * @param name the name of the node
         */
        public Node(String name) {
            this.name = name;
        }

        /**
         * Adds a link from this node to a child node.
         *
         * @param referent the node that is a new child
         * @param relationshipToReferent the relationship that caused the child to be added
         */
        void addReferent(Node referent, Relationship relationshipToReferent) {
            if (referents == null) {
                referents = new ArrayList<Node>();
            }
            referents.add(referent);
            referent.relationshipToReferrer = relationshipToReferent;
        }

        /**
         * Returns a sequence of all the referents from this node.
         *
         * @return a sequence of all the nodes that are children of this node
         */
        public List<Node> referents() {
            if (referents == null) {
                return Collections.emptyList();
            }
            return referents;
        }

        /**
         * Remove all children nodes of this node that do not match the specified predicate.
         *
         * @param predicate a predicate which decides whether a node should be included
         * @return {@code null} if the predicate is not true for this object or any
         * of its descendants; this node, with the children that do not match the predicate
         * removed otherwise
         */
        public Node prune(Predicate<Node> predicate) {
            final Node pruned = new Node(name);
            pruned.relationshipToReferrer = relationshipToReferrer;
            if (referents != null) {
                for (Node referent : referents) {
                    final Node prunedReferent = referent.prune(predicate);
                    if (prunedReferent != null) {
                        pruned.addReferent(prunedReferent, prunedReferent.relationshipToReferrer);
                    }
                }
            }
            if (pruned.referents != null || predicate.evaluate(this)) {
                return pruned;
            }
            return null;
        }

        private static final String OTHER_SIBLING_PREFIX = "`-- ";
        private static final String LAST_SIBLING_PREFIX = "|-- ";
        private static final String OTHER_CHILD_INDENT = "|   ";
        private static final String LAST_CHILD_INDENT = "    ";

        /**
         * Print the tree out in a textual form.
         *
         * @param node the node to print
         * @param showTreeLines a boolean indicating whether to draw the tree lines
         * @param printWriter the writer to which to print this tree
         * @param prefix a prefix to add to this node
         * @param lastChild a boolean indiciating whether to print the last child
         */
        private static void printTree(Node node, boolean showTreeLines, PrintWriter printWriter, String prefix, boolean lastChild) {
            printWriter.println(prefix + (showTreeLines ? (!lastChild ? LAST_SIBLING_PREFIX : OTHER_SIBLING_PREFIX) : "    ") + node);
            final List<Node> referents = node.referents();
            if (!referents.isEmpty()) {
                final String childPrefix = prefix + (showTreeLines ? (lastChild ? LAST_CHILD_INDENT : OTHER_CHILD_INDENT) : "    ");
                for (final Iterator<Node> iterator = referents.iterator(); iterator.hasNext();) {
                    final Node child = iterator.next();
                    if (child != node) {
                        printTree(child, showTreeLines, printWriter, childPrefix, !iterator.hasNext());
                        printWriter.flush();
                    } else {
                        ProgramWarning.message(node.name + " has itself as a referent");
                    }
                }
            }
        }

        /**
         * Converts this node to a string.
         * @return a string representation of this node
         */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (relationshipToReferrer != null) {
                sb.append(relationshipToReferrer.asReferrer).append(' ');
            }
            sb.append(name);
            return sb.toString();
        }
    }

    /**
     * Save this tree to a file in a compact, binary format.
     *
     * @param dataOutputStream the data output stream to which to write the tree
     * @param links the links to write to the data output stream
     * @throws IOException if there is a problem writing to the output stream
     */
    public static void saveTree(DataOutputStream dataOutputStream, Collection<Link> links) throws IOException {
        final Map<String, Integer> methodPool = new HashMap<String, Integer>();
        final List<String> methodIds = new ArrayList<String>(links.size() * 2);
        int methodId = 0;
        methodPool.put("", methodId++);
        for (Link link : links) {
            final String referentName = link.referentName();
            if (!methodPool.containsKey(referentName)) {
                methodIds.add(referentName);
                methodPool.put(referentName, methodId++);
            }
            final String referrerName = link.referrerName();
            if (referrerName != null && !methodPool.containsKey(referrerName)) {
                if (referentName.equals(referrerName)) {
                    ProgramWarning.message("link with same name for referrer and referent: " + referentName);
                }
                methodIds.add(referentName);
                methodPool.put(referrerName, methodId++);
            }
        }
        assert methodId == methodPool.size();
        assert methodId == methodIds.size() + 1;

        dataOutputStream.writeInt(methodPool.size());
        final Iterator<String> iterator = methodIds.iterator();
        while (iterator.hasNext()) {
            dataOutputStream.writeUTF(iterator.next());
        }

        for (Link link : links) {
            final String referentName = link.referentName();
            dataOutputStream.writeInt(methodPool.get(referentName));
            final String referrerName = link.referrerName();
            if (referrerName != null) {
                dataOutputStream.writeInt(methodPool.get(referrerName));
                dataOutputStream.writeByte(link.relationship().ordinal());
            } else {
                dataOutputStream.writeInt(0);
            }
        }
    }

    /**
     * Loads a tree that was saved by {@linkplain #saveTree(DataOutputStream, IterableWithLength) this method}.
     *
     * @param dataInputStream
     *                a stream containing a saved tree
     * @return the roots of the loaded tree
     */
    public static Set<Node> loadTree(DataInputStream dataInputStream) throws IOException {
        final Set<Node> roots = new HashSet<Node>();
        final int methodPoolSize = dataInputStream.readInt();
        final Node[] methodPool = new Node[methodPoolSize];
        for (int i = 1; i != methodPoolSize; ++i) {
            final String methodName = dataInputStream.readUTF();
            methodPool[i] = new Node(methodName);
        }

        final Relationship[] values = Relationship.values();
        while (dataInputStream.available() != 0) {
            final int referentId = dataInputStream.readInt();
            final Node referent = methodPool[referentId];
            final int referrerId = dataInputStream.readInt();
            if (referrerId != 0) {
                final Node referrer = methodPool[referrerId];
                final Relationship relationship = values[dataInputStream.readByte()];
                referrer.addReferent(referent, relationship);
            } else {
                roots.add(referent);
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
     */
    public static void printTree(Node node, boolean showTreeLines, PrintWriter printWriter, boolean lastChild) {
        Node.printTree(node, showTreeLines, printWriter, "", lastChild);
    }

    private static final OptionSet options = new OptionSet();

    private static final Option<Integer> TRACE = options.newIntegerOption("trace", 1,
            "selects the trace level of the tool");
    private static final Option<File> INPUT_FILE = options.newFileOption("in", BootImageGenerator.getBootImageMethodTreeFile(null),
            "the file from which to load the graph");
    private static final Option<File> OUTPUT_FILE = options.newFileOption("out", getDefaultOutputFile(),
            "the file to which the graph is printed");
    private static final Option<String> FILTER = options.newStringOption("filter", null,
            "filter for pruning the graph before printing");
    private static final Option<Boolean> SHOW_TREE_LINES = options.newBooleanOption("lines", true,
           "show lines (instead of indentation only) to indicate relationships between nodes");
    private static final Option<Boolean> HELP = options.newBooleanOption("help", false,
           "show help message and exits.");

    private static File getDefaultOutputFile() {
        return new File(BootImageGenerator.getBootImageMethodTreeFile(null).getAbsolutePath() + ".txt");
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

        Trace.begin(1, "writing boot image method tree text file: " + outputFile.getAbsolutePath());
        printWriter.println("VM Entry Points");
        for (final Iterator<Node> iterator = roots.iterator(); iterator.hasNext();) {
            final Node root = iterator.next();
            Node.printTree(root, SHOW_TREE_LINES.getValue(), printWriter, "", !iterator.hasNext());
        }
        printWriter.close();
        Trace.end(1, "writing boot image method tree text file: " + outputFile.getAbsolutePath());
    }

    private static FileInputStream openInputFile() {
        try {
            return new FileInputStream(INPUT_FILE.getValue());
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
