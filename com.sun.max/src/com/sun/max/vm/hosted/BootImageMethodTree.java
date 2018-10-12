/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.util.*;
import com.sun.max.vm.hosted.CompiledPrototype.Link;
import com.sun.max.vm.hosted.CompiledPrototype.Link.Relationship;

/**
 * A mechanism for {@linkplain #saveTree(DataOutputStream, Collection) saving},
 * {@linkplain #loadTree(DataInputStream) loading} and
 * {@linkplain #printTree(Node, boolean, PrintWriter, boolean) printing}
 * the causality spanning-tree of the {@linkplain CompiledPrototype#links() method graph} in an
 * {@linkplain BootImageGenerator image}.
 */
public final class BootImageMethodTree {

    private BootImageMethodTree() {
    }

    /**
     * A node in the method tree, which contains links to subnodes. Each link has a specified relationship,
     * which helps to diagnose exactly why a node was included into the graph.
     */
    public static class Node implements Comparable<Node> {

        private final String name;
        private Node parent;
        private TreeSet<Node> children;
        private Relationship relationshipToParent;

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
         * @param child the node that is a new child
         * @param relationshipToParent the relationship that caused the child to be added
         */
        void addChild(Node child, Relationship relationshipToParent) {
            if (children == null) {
                children = new TreeSet<Node>();
            }
            assert child != this : child.name + " == " + name;
            children.add(child);
            child.relationshipToParent = relationshipToParent;
            child.parent = this;
        }

        /**
         * Returns a sequence of all the children from this node.
         *
         * @return a sequence of all the nodes that are children of this node
         */
        public Collection<Node> children() {
            if (children == null) {
                return Collections.emptyList();
            }
            return children;
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
            pruned.relationshipToParent = relationshipToParent;
            if (children != null) {
                for (Node child : children) {
                    final Node prunedChild = child == this ? child : child.prune(predicate);
                    if (prunedChild != null) {
                        pruned.addChild(prunedChild, prunedChild.relationshipToParent);
                    }
                }
            }
            if (pruned.children != null || predicate.evaluate(this)) {
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
            final Collection<Node> children = node.children();
            if (!children.isEmpty()) {
                final String childPrefix = prefix + (showTreeLines ? (lastChild ? LAST_CHILD_INDENT : OTHER_CHILD_INDENT) : "    ");
                for (final Iterator<Node> iterator = children.iterator(); iterator.hasNext();) {
                    final Node child = iterator.next();
                    assert child != node;
                    printTree(child, showTreeLines, printWriter, childPrefix, !iterator.hasNext());
                    printWriter.flush();
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
            if (relationshipToParent != null) {
                sb.append(relationshipToParent.asParent).append(' ');
            }
            sb.append(name);
            return sb.toString();
        }

        /**
         * Create a piece of GUI tree for this node and all of its children.
         * @return a JTree instance representing the object tree starting at this node
         */
        public MethodTreeNode buildTree() {
            if (++btc % 100000 == 0) {
                Trace.line(1, "node: " + btc);
            }
            MethodTreeNode n = new MethodTreeNode(this);
            if (children != null) {
                for (Node child : children) {
                    n.add(child.buildTree());
                }
            }
            return n;
        }

        private static int btc = 0;

        @Override
        public int compareTo(Node o) {
            return name.compareTo(o.name);
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
            final String childId = link.childId();
            if (!methodPool.containsKey(childId)) {
                methodIds.add(childId);
                methodPool.put(childId, methodId++);
            }
            final String parentId = link.parentId();
            if (parentId != null && !methodPool.containsKey(parentId)) {
                if (childId.equals(parentId)) {
                    ProgramWarning.message("link with same name for parent and child: " + childId);
                }
                methodIds.add(parentId);
                methodPool.put(parentId, methodId++);
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
            final String childId = link.childId();
            dataOutputStream.writeInt(methodPool.get(childId));
            final String referrerName = link.parentId();
            if (referrerName != null) {
                dataOutputStream.writeInt(methodPool.get(referrerName));
                dataOutputStream.writeByte(link.relationship.ordinal());
            } else {
                dataOutputStream.writeInt(0);
            }
        }
    }

    /**
     * Loads a tree that was saved by {@linkplain #saveTree(DataOutputStream, Collection) this method}.
     *
     * @param dataInputStream a stream containing a saved tree
     * @return the roots of the loaded tree
     */
    public static Set<Node> loadTree(DataInputStream dataInputStream) throws IOException {
        final TreeSet<Node> roots = new TreeSet<Node>();
        final int methodPoolSize = dataInputStream.readInt();
        final Node[] methodPool = new Node[methodPoolSize];
        for (int i = 1; i != methodPoolSize; ++i) {
            final String methodName = dataInputStream.readUTF();
            methodPool[i] = new Node(methodName);
        }

        final Relationship[] values = Relationship.values();
        while (dataInputStream.available() != 0) {
            final int childId = dataInputStream.readInt();
            final Node child = methodPool[childId];
            final int parentId = dataInputStream.readInt();
            if (parentId != 0) {
                final Node parent = methodPool[parentId];
                final Relationship relationship = values[dataInputStream.readByte()];
                parent.addChild(child, relationship);
            } else {
                roots.add(child);
            }
        }
        return roots;
    }

    /**
     * Prints the tree rooted at a given node to a given print writer. The format of the dump is similar to the output
     * of the tree(1) utility that list contents of directories in a tree-like format.
     *
     * @param node the root of the tree to dump
     * @param printWriter where to print the dump
     * @param lastChild specifies if {@code node} is the last child of its parent that will be dumped
     */
    public static void printTree(Node node, boolean showTreeLines, PrintWriter printWriter, boolean lastChild) {
        Node.printTree(node, showTreeLines, printWriter, "", lastChild);
    }

    /**
     * Create and show a GUI for viewing the object tree.
     */
    private static void viewInGUI(final Set<Node> roots) {
        // build tree and make the root objects shown
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("roots");
        final JTree tree = new JTree(treeRoot);
        for (Node root : roots) {
            treeRoot.add(root.buildTree());
        }
        tree.expandRow(0);
        tree.expandRow(1);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // open GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Method Tree");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MethodTreeView otv = new MethodTreeView(tree);
                frame.add(otv);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    /**
     * GUI class.
     */
    static class MethodTreeView extends JPanel {

        JLabel info;
        JPanel navigation;
        JTextField searchField;
        JTree tree;
        JCheckBox matchCase;

        ArrayList<TreeNode> searchHits = new ArrayList<TreeNode>();
        int searchHit = -1;
        JLabel hitPos;
        JButton prevHit;
        JButton nextHit;

        void redoSearch() {
            ArrayList<TreeNode> searchHits = new ArrayList<TreeNode>();
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                searchHits.clear();
                searchHit = -1;
                searchField.setBackground(Color.WHITE);
            } else {
                if (!matchCase.isSelected()) {
                    searchText = searchText.toLowerCase();
                }
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                Enumeration nodes = root.preorderEnumeration();
                while (nodes.hasMoreElements()) {
                    TreeNode node = (TreeNode) nodes.nextElement();
                    String nodeText = node.toString();
                    if (!matchCase.isSelected()) {
                        nodeText = nodeText.toLowerCase();
                    }
                    if (nodeText.contains(searchText)) {
                        searchHits.add(node);
                    }
                }
                searchField.setBackground(searchHits.isEmpty() ? Color.RED : Color.WHITE);
                this.searchHit = searchHits.size() - 1;
                this.searchHits = searchHits;
            }
            prevHit.setEnabled(!searchHits.isEmpty());
            nextHit.setEnabled(!searchHits.isEmpty());
            hitPos.setText((searchHit + 1) + " of " + searchHits.size());
        }

        MethodTreeView(final JTree t) {
            tree = t;
            setLayout(new BorderLayout());
            // enable info display
            info = new JLabel("Select a tree node to have its information shown here.");
            // handle selection
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (n == null) {
                        return;
                    }
                    String text;
                    if (n == tree.getModel().getRoot()) {
                        text = n.toString();
                    } else {
                        text = ((MethodTreeNode) n).getDetails();
                    }
                    info.setText(text);
                }
            });

            // navigation controls
            // controls for address and node inspection
            navigation = new JPanel();
            navigation.setLayout(new BoxLayout(navigation, BoxLayout.X_AXIS));
            hitPos = new JLabel("0 of 0") {
                @Override
                public void setText(String text) {
                    while (text.length() < 25) {
                        text = ' ' + text;
                    }
                    super.setText(text + ' ');
                }
            };
            navigation.add(hitPos);
            searchField = new JTextField(10);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                }
                public void insertUpdate(DocumentEvent event) {
                    redoSearch();
                }
                public void removeUpdate(DocumentEvent e) {
                    redoSearch();
                }
            });
            navigation.add(searchField);

            prevHit = new JButton(new AbstractAction("^") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int hits = searchHits.size();
                    if (hits != 0) {
                        if (--searchHit < 0) {
                            searchHit = hits - 1;
                        }
                        jumpToNode();
                    }
                }
            });
            nextHit = new JButton(new AbstractAction("v") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int hits = searchHits.size();
                    if (hits != 0) {
                        if (++searchHit >= hits) {
                            searchHit = 0;
                        }
                        jumpToNode();
                    }
                }
            });

            navigation.add(prevHit);
            navigation.add(nextHit);

            matchCase = new JCheckBox("Match case");
            matchCase.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    redoSearch();
                }
            });
            navigation.add(matchCase);

            // control for jumping to a node's parent
            navigation.add(new JSeparator(SwingConstants.VERTICAL));
            navigation.add(new JButton(new AbstractAction("jump to parent") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tree.getSelectionPath() == null) {
                        return;
                    }
                    TreePath path = tree.getSelectionPath().getParentPath();
                    if (path != null) {
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                    }
                }
            }));
            // control for resetting tree (back to start view)
            navigation.add(new JSeparator(SwingConstants.VERTICAL));
            navigation.add(new JButton(new AbstractAction("reset tree view") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = tree.getRowCount() - 1; i >= 0; i--) {
                        tree.collapseRow(i);
                    }
                    tree.expandRow(0);
                    tree.expandRow(1);
                    tree.setSelectionPath(null);
                    info.setText("");
                }
            }));
            // assemble
            add(navigation, BorderLayout.NORTH);
            add(new JScrollPane(tree), BorderLayout.CENTER);
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
            p.add(info);
            add(p, BorderLayout.SOUTH);

            redoSearch();
        }

        void jumpToNode() {
            try {
                TreeNode node = searchHits.get(searchHit);
                Vector<TreeNode> path = new Vector<TreeNode>();
                path.add(node);
                do {
                    node = node.getParent();
                    path.add(0, node);
                } while (node.getParent() != null);
                TreePath treePath = new TreePath(path.toArray());
                tree.setSelectionPath(treePath);
                tree.scrollPathToVisible(treePath);
            } catch (IndexOutOfBoundsException e) {
            }
        }

    }

    /**
     * Class for representing object tree nodes in the GUI.
     */
    static class MethodTreeNode extends DefaultMutableTreeNode {

        Node node;
        String treeStringRep;
        String detailStringRep;
        String shortName;

        MethodTreeNode(Node node) {
            this.node = node;
        }

        @Override
        public String toString() {
            if (treeStringRep == null) {
                treeStringRep = node.name;
            }
            return treeStringRep;
        }

        String shortName() {
            if (shortName == null) {
                String s = toString();
                int openParen = s.indexOf('(');
                if (openParen == -1) {
                    shortName = s;
                } else {
                    int i = s.lastIndexOf('.', openParen);
                    i = s.lastIndexOf('.', i - 1);
                    shortName = s.substring(i + 1, openParen) + "()";
                }
            }
            return shortName;
        }

        String getDetails() {
            if (detailStringRep == null) {
                if (node.relationshipToParent == null) {
                    detailStringRep = "<html>" + shortName() + " is a <font color=green><b>VM entry point</b></font>";
                } else {
                    MethodTreeNode parent = (MethodTreeNode) this.parent;
                    detailStringRep = "<html>" + shortName() + " <font color=green><b>" + node.relationshipToParent.asChild + "</b></font> " + parent.shortName();
                }
            }
            return detailStringRep;
        }
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
    private static final Option<Boolean> GUI = options.newBooleanOption("view", false,
            "view the method tree graphically instead of generating text");

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

        if (GUI.getValue()) {
            viewInGUI(roots);
        } else {
            writeOutputFile(roots);
        }
    }

    static void writeOutputFile(final Set<Node> roots) throws IOException {
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
