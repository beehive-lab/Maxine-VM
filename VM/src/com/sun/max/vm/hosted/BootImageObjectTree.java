/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.hosted.GraphPrototype.Link;
import com.sun.max.vm.object.*;

/**
 * A mechanism for saving, loading and printing the
 * causality spanning-tree of the object graph in an {@linkplain BootImageGenerator image}.
 *
 * @author Doug Simon
 * @author Michael Haupt
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

        /**
         * Create a piece of GUI tree for this node and all of its children.
         * @param radix the number radix for representing addresses
         * @param relocation a value to add to addresses for relocation
         * @return a JTree instance representing the object tree starting at this node
         */
        public ObjectTreeNode buildTree(int radix, long relocation) {
            if (++btc % 100000 == 0) {
                Trace.line(1, "node: " + btc);
            }
            ObjectTreeNode n = new ObjectTreeNode(this);
            addressToNodeMap().put(address, n);
            if (children != null) {
                for (Node child : children) {
                    n.add(child.buildTree(radix, relocation));
                }
            }
            return n;
        }

        private static int btc = 0;
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
            final long size = ObjectAccess.size(object).toLong();
            String toString;
            try {
                toString = object.toString();
            } catch (Exception e) {
                toString = "<error calling toString()>: " + e.toString();
            }

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
    private static final Option<Boolean> GUI = options.newBooleanOption("view", false,
            "view the object tree graphically instead of generating text");

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

    /**
     * Create and show a GUI for viewing the object tree.
     */
    private static void viewInGUI(final Set<Node> roots) {
        // build tree and make the root objects shown
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("roots");
        final JTree tree = new JTree(treeRoot);
        for (Node root : roots) {
            treeRoot.add(root.buildTree(RADIX.getValue(), RELOC.getValue()));
        }
        tree.expandRow(0);
        tree.expandRow(1);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // open GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Object Tree");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                ObjectTreeView otv = new ObjectTreeView(tree);
                frame.add(otv);
                frame.setBounds(GraphicsEnvironment.
                                getLocalGraphicsEnvironment().
                                getDefaultScreenDevice().
                                getDefaultConfiguration().
                                getBounds());
                frame.setVisible(true);
                otv.split.setDividerLocation(0.75);
            }
        });
    }

    private static HashMap<Integer, ObjectTreeNode> addressToNode;

    private static HashMap<Integer, ObjectTreeNode> addressToNodeMap() {
        if (addressToNode == null) {
            addressToNode = new HashMap<Integer, ObjectTreeNode>();
        }
        return addressToNode;
    }

    /**
     * GUI class.
     */
    private static class ObjectTreeView extends JPanel {

        JTextArea info;
        JPanel navigation;
        JTextField addressField;
        JSplitPane split;
        JTree tree;

        ObjectTreeView(final JTree t) {
            tree = t;
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            // enable info display
            info = new JTextArea(10, 0);
            info.setText("Select a tree node to have its information shown here.");
            // handle selection
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (n == null) {
                        return;
                    }
                    if (n == tree.getModel().getRoot()) {
                        info.setText(n.toString());
                    } else {
                        info.setText(((ObjectTreeNode) n).getDetails());
                    }
                }
            });
            // navigation controls
            // controls for address and node inspection
            navigation = new JPanel();
            navigation.setLayout(new BoxLayout(navigation, BoxLayout.X_AXIS));
            navigation.add(new JLabel("object address (decimal or 0x... hexadecimal): "));
            addressField = new JTextField(10);
            addressField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // empty
                }
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        jumpToNode();
                    }
                }
                @Override
                public void keyReleased(KeyEvent e) {
                    // empty
                }
            });
            navigation.add(addressField);
            navigation.add(new JButton(new AbstractAction("navigate to object") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jumpToNode();
                }
            }));
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
            c.fill = GridBagConstraints.HORIZONTAL;
            add(navigation, c);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tree), new JScrollPane(info));
            add(split, c);
        }

        void jumpToNode() {
            addressField.setBackground(Color.WHITE);
            int address = 0;
            boolean error = false;
            try {
                if (addressField.getText().startsWith("0x")) {
                    address = Integer.parseInt(addressField.getText().substring(2), 16);
                } else {
                    address = Integer.parseInt(addressField.getText());
                }
            } catch (NumberFormatException nfe) {
                error = true;
            }
            TreeNode node = null;
            if (!error) {
                node = addressToNodeMap().get(address);
                if (node == null) {
                    error = true;
                }
            }
            if (!error) {
                Vector<TreeNode> path = new Vector<TreeNode>();
                path.add(node);
                do {
                    node = node.getParent();
                    path.add(0, node);
                } while (node.getParent() != null);
                TreePath treePath = new TreePath(path.toArray());
                tree.setSelectionPath(treePath);
                tree.scrollPathToVisible(treePath);
            } else {
                addressField.setBackground(Color.RED);
            }
        }

    }

    /**
     * Class for representing object tree nodes in the GUI.
     */
    private static class ObjectTreeNode extends DefaultMutableTreeNode {

        Node node;
        String treeStringRep;
        String detailStringRep;

        ObjectTreeNode(Node node) {
            this.node = node;
        }

        @Override
        public String toString() {
            if (treeStringRep == null) {
                treeStringRep = new StringBuffer(node.className).
                    append(" @ ").
                    append(Long.toString(node.address + RELOC.getValue(), 10)).
                    append(", 0x").
                    append(Long.toString(node.address + RELOC.getValue(), 16)).
                    toString();
            }
            return treeStringRep;
        }

        String getDetails() {
            if (detailStringRep == null) {
                detailStringRep = String.format(
                                "class: %s\naddress (decimal): %s\naddress (hexadecimal): %s\nsize: %d bytes\naggregate size: %d bytes",
                                node.className, Long.toString(node.address + RELOC.getValue(), 10), Long.toString(node.address + RELOC.getValue(), 16),
                                node.size, node.aggregateSize);
            }
            return detailStringRep;
        }

    }

    /**
     * Write the object tree to a file.
     *
     * @param roots
     * @throws IOException
     */
    private static void writeOutputFile(final Set<Node> roots) throws IOException {
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
