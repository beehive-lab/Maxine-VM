/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa.callgraph;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.SpringLayout;

import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

/**
 * A tool to display a method call graph, hacked from Swing Tree tutorial, and originally
 * developed to interpret traces from AspectJ. The code to read the latter format of trace file,
 * which can be generated with {@code ConvertLog}, is in {@link AJFileDataParser}, and is the
 * default for stand-alone execution. However, the expected execution path is for {@link #queryMain(TraceRun, String[])}
 * to be invoked from {@link QueryAnalysis}, which parses the in-memory data structures using {@link ARDataParser}.
 *
 */

// Old code, way too many violations to fix.
// Checkstyle: off

public class CallGraphDisplay extends JPanel {

    private static boolean DEBUG = false;
    private static int debugLine = -1;
    private static boolean PROGRESS = false;
    private static boolean GC = false;

    enum TimeFormat {
        Nano, Micro, Milli, Sec
    }

    private static TimeFormat timeFormat = TimeFormat.Micro;

    public enum TimeDisplay {
        WallRel, WallAbs, Duration
    }

    public static TimeDisplay timeDisplay = TimeDisplay.Duration;
    public long traceStartTime; // used for WallRel time display

    // Optionally play with line styles. Possible values are
    // "Angled" (the default), "Horizontal", and "None".
    private static boolean playWithLineStyle = false;
    private static String lineStyle = "Horizontal";

    Map<String, JTree> threadJTrees = new HashMap<String, JTree>(); // thread trees
    Set<JTree> matchJTrees = new HashSet<JTree>();
    JTree currentJTree;

    Map<String,String> threadMap = new HashMap<String,String>();
    Map<String,String> methodMap = new HashMap<String,String>();
    Map<String,String> paramMap = new HashMap<String,String>();

    ArrayList<MethodData> forwardRefs = new ArrayList<MethodData>();
    ArrayList<ArgsMethodData> forwardParamRefs = new ArrayList<ArgsMethodData>();

    /**
     * These are unsorted/sorted arrays of all the {@code TraceType#Entry} and {@code TraceType#Return} instances,
     * used to produce a time-ordered view across all threads.
     */
    ArrayList<MethodData> controlFlowTraces = new ArrayList<MethodData>();
    MethodData[] sortedControlFlowTraces;

    // Optionally set the look and feel.
    private static boolean useSystemLookAndFeel = false;
    JFrame myFrame;
    String traceFilePathName;
    long nodeCount;
    public Map<String, DefaultMutableTreeNode> threadCallGraphs;
    private boolean showGUI = true;

    public static CallGraphDisplay callGraphDisplay;
    private static volatile boolean callGraphDisplayDataValid;
    static TraceRun traceRun;


    public CallGraphDisplay() {
        super(new GridLayout(1, 0));
    }

    private void createGUI() throws Exception {
        if (!showGUI) {
            return;
        }
        // Create and set up the window.
        myFrame = new JFrame("CallGraphAnalyzer " + traceFilePathName);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // JComponent left = null; int count = 0;
        JTabbedPane threadTabPane = new JTabbedPane();
        threadTabPane.addChangeListener(new TabbedPaneChangeListener());
        for (DefaultMutableTreeNode top : threadCallGraphs.values()) {

            // Create a tree that allows one selection at a time.
            JTree tree = new JTree(top);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            ToolTipManager.sharedInstance().registerComponent(tree);
            tree.setCellRenderer(new ToolTipRenderer());

            new APopupMenu(tree);

            tree.setShowsRootHandles(true);

            if (playWithLineStyle) {
                System.out.println("line style = " + lineStyle);
                tree.putClientProperty("JTree.lineStyle", lineStyle);
            }

            // Create the scroll pane and add the tree to it.
            JScrollPane treeView = new JScrollPane(tree);
            // Dimension minimumSize = new Dimension(500, 500);
            // treeView.setMinimumSize(minimumSize);

            MethodData md = (MethodData) top.getUserObject();
            threadJTrees.put(md.thread, tree);
            threadTabPane.add(md.thread, treeView);
        }
        add(threadTabPane);
        this.setOpaque(true); // content panes must be opaque
        myFrame.setContentPane(this);

        myFrame.setJMenuBar(createMenuBar());

        // Display the window.
        myFrame.pack();
        myFrame.setVisible(true);
    }

    private static void usage() {
        System.out.println("usage: -f tracefile [-debug] [-progress] [-time sec | milli | micro | nano]");
    }

    private void processArgsAndData(String[] args) throws Exception {
        boolean showSorted = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-f")) {
                traceFilePathName = args[++i];
            } else if (arg.equals("-debug")) {
                DEBUG = true;
            } else if (arg.equals("-debugline")) {
                debugLine = Integer.parseInt(args[++i]);
            } else if (arg.equals("-progress")) {
                PROGRESS = true;
            } else if (arg.equals("-gc")) {
                GC = true;
            } else if (arg.equals("-sort")) {
                showSorted = true;
            } else if (arg.equals("-time")) {
                i++;
                String tf = args[i];
                if (tf.equals("milli")) {
                    timeFormat = TimeFormat.Milli;
                } else if (tf.equals("nano"))
                    timeFormat = TimeFormat.Nano;
                else if (tf.equals("micro")) {
                    timeFormat = TimeFormat.Micro;
                } else if (tf.equals("sec")) {
                    timeFormat = TimeFormat.Sec;
                } else {
                    System.err.println("unknown time format: " + tf);
                }
            } else if (arg.equals("-nogui")) {
                showGUI = false;
            }
        }

        if (traceRun == null && traceFilePathName == null) {
            usage();
            return;
        }

        // Create the nodes.
        DataParser dataParser = traceRun != null ? new ARDataParser() : new AJFileDataParser();
        threadCallGraphs = dataParser.processData();
        callGraphDisplayDataValid = true;

        if (showSorted) {
            for (MethodData methodData : sortedControlFlowTraces) {
                TraceType tt = methodData.ttype;
                TimeMethodData tmd = timeMethodData(methodData);
                System.out.print(tt + " " + methodData.depth);
                if (tmd != null) {
                    System.out.print(" " + (TraceType.isReturn(tt)  ? tmd.exitTimeInfo : tmd.entryTimeInfo));
                }
                System.out.print(" " +
                                methodData.thread + " " + methodData.methodName);
                if (methodData instanceof ArgsMethodData) {
                    System.out.print("(" + ((ArgsMethodData) methodData).params + ")");
                }
                System.out.println();
            }
        }


    }

    class ToolTipRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            MethodData md = (MethodData) node.getUserObject();
            ArgsMethodData amd = argsMethodData(md);
            if (amd != null && amd.thisArg != null) {
                setToolTipText(amd.thisArg);
            } else
                setToolTipText(null);
            return this;
        }
    }

    class TabbedPaneChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            JTabbedPane tabbedPane = (JTabbedPane) e.getSource();
            JScrollPane scrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
            JViewport viewPort = (JViewport) scrollPane.getComponent(0);
            JTree t = (JTree) viewPort.getComponent(0);
            currentJTree = t;
        }
    }

    enum FindType {
        First, Next, All
    }

    enum FindWhat {
        Method, Arg
    }

    public JTree getCurrentJTree() {
        return currentJTree;
    }

    class APopupMenu implements TreeSelectionListener {

        JTree tree;
        FindHelper findHelper;
        boolean moveByFind = false;

        APopupMenu(JTree tree) {
            this.tree = tree;
            tree.addTreeSelectionListener(this);

            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem(new PropertiesAction());
            popup.add(menuItem);
            menuItem = new JMenuItem(new ExpandAllAction("ExpandAll"));
            popup.add(menuItem);
            menuItem = new JMenuItem(new ExpandAction("Expand"));
            popup.add(menuItem);
            findHelper = new FindHelper(); // singleton
            popup.add(makeFindMenu(FindWhat.Method));
            popup.add(makeFindMenu(FindWhat.Arg));
            JMenuItem myTimeItem = new JMenuItem(new MyTimeAction("My Time"));
            popup.add(myTimeItem);
            popup.add(makeTimeSortMenu());

            MouseListener popupListener = new PopupListener(popup);
            tree.addMouseListener(popupListener);
        }

        /** Required by TreeSelectionListener interface. */
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (node == null)
                return;

            MethodData md = (MethodData) node.getUserObject();
            if (DEBUG) {
                System.out.println(md.toString());
            }
            // reset FindHelper unless this change came from moveByNode
            if (!moveByFind)
                findHelper.startTp = null;
        }

        class PropertiesAction extends AbstractAction {

            JPanel propsPanel;
            Map<String, JTextField> propsMap = new HashMap<String, JTextField>();

            public PropertiesAction() {
                super("Properties", null);
                // putValue(SHORT_DESCRIPTION, desc);
                // putValue(MNEMONIC_KEY, mnemonic);
                String[] labels = { "Name", "Entry time", "Exit time", "Entry cpu", "Exit cpu", "This", "Result", "Parameters"};

                propsPanel = new JPanel(new SpringLayout());
                for (int i = 0; i < labels.length; i++) {
                    JLabel l = new JLabel(labels[i] + ": ", JLabel.TRAILING);
                    propsPanel.add(l);
                    JTextField textField = new JTextField(10);
                    l.setLabelFor(textField);
                    propsPanel.add(textField);
                    propsMap.put(labels[i], textField);
                }
                SpringUtilities.makeCompactGrid(propsPanel, labels.length, 2, 5, 5, 5, 5);

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (DEBUG)
                    System.out.println(e);

                if (node == null)
                    return;

                MethodData md = (MethodData) node.getUserObject();
                ArgsMethodData amd = argsMethodData(md);
                propsMap.get("Name").setText(md.methodName);
                if (amd != null) {
                    propsMap.get("This").setText(amd.thisArg == null ? "" : amd.thisArg);
                    propsMap.get("Result").setText(amd.result == null ? "" : amd.result);
                    propsMap.get("Parameters").setText(amd.params == null ? "" : fixNL(amd.linearizeParams()));
                }
                TimeMethodData tmd = timeMethodData(md);
                if (tmd != null) {
                    long tst = timeDisplay == TimeDisplay.WallRel ? traceStartTime : 0;
                    propsMap.get("Entry time").setText(TimeFunctions.formatTime(tmd.entryTimeInfo.wallTime - tst));
                    propsMap.get("Exit time").setText(tmd.exitTimeInfo == null ? "?" : TimeFunctions.formatTime(tmd.exitTimeInfo.wallTime - tst));
                    propsMap.get("Entry cpu").setText("u: " + TimeFunctions.formatTime(tmd.entryTimeInfo.userUsage) + " s:" + TimeFunctions.formatTime(tmd.entryTimeInfo.sysUsage));
                    propsMap.get("Exit cpu").setText(
                                tmd.exitTimeInfo == null ? "?" : "u:" + TimeFunctions.formatTime(tmd.exitTimeInfo.userUsage) + " s:" + TimeFunctions.formatTime(tmd.exitTimeInfo.sysUsage));
                }
                JFrame propsFrame = new JFrame("Properties for " + md.methodName);
                propsFrame.add(propsPanel);
                propsFrame.pack();
                propsFrame.setVisible(true);
            }

            private String fixNL(String s) {
                String[] sp = s.split("\\\\n");
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < sp.length; i++) {
                    sb.append(sp[i]);
                    if (i != sp.length - 1)
                        sb.append('\n');
                }
                return sb.toString();
            }

        }

        class ExpandAction extends AbstractAction {

            public ExpandAction(String name) {
                super(name, null);
                // putValue(SHORT_DESCRIPTION, desc);
                // putValue(MNEMONIC_KEY, mnemonic);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp = tree.getSelectionPath();
                // System.out.println("Path is " + tp);
                String response = JOptionPane.showInputDialog(myFrame, "Depth:");
                if (response != null && !response.equals("")) {
                    try {
                        int n = Integer.parseInt(response);
                        expandNodes(tp, n);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(myFrame, "Integer required", "", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            protected void expandNodes(TreePath tp, int n) {
                TreeNode tn = (TreeNode) tp.getLastPathComponent();
                tree.makeVisible(tp);
                if (!tn.isLeaf() && n > 0) {
                    Enumeration en = tn.children();
                    while (en.hasMoreElements()) {
                        TreeNode ctn = (TreeNode) en.nextElement();
                        TreePath ctp = tp.pathByAddingChild(ctn);
                        expandNodes(ctp, n - 1);
                    }
                }
            }
        }

        class ExpandAllAction extends ExpandAction {

            public ExpandAllAction(String name) {
                super(name);
                // putValue(SHORT_DESCRIPTION, desc);
                // putValue(MNEMONIC_KEY, mnemonic);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp = tree.getSelectionPath();
                // System.out.println("Path is " + tp);
                expandNodes(tp, Integer.MAX_VALUE);
            }

        }

        class FindHelper {
            static final String DEFAULT_SEARCH_TEXT = "com.sun.max.ve[\\.\\w]*";
            String searchText = DEFAULT_SEARCH_TEXT;
            TreePath startTp = null;
            TreePath[] matches = null;
            int nextIndex = 0;
        }

        private JMenu makeFindMenu(FindWhat findWhat) {
            JMenu result = new JMenu("Find " + findWhat);
            result.add(new JMenuItem(new FindAction(FindType.First, findWhat, "First", findHelper)));
            result.add(new JMenuItem(new FindAction(FindType.Next, findWhat, "Next", findHelper)));
            result.add(new JMenuItem(new FindAction(FindType.All, findWhat, "All", findHelper)));
            return result;
        }

        class FindAction extends AbstractAction {

            FindType type;
            FindWhat findWhat;
            FindHelper actionFindHelper;

            public FindAction(FindType type, FindWhat findWhat, String menuName, FindHelper findHelper) {
                super(menuName, null);
                this.type = type;
                this.findWhat = findWhat;
                this.actionFindHelper = findHelper;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (type == FindType.Next) {
                    if (actionFindHelper.startTp == null) {
                        JOptionPane.showMessageDialog(myFrame, "no active Find from selection", "", JOptionPane.ERROR_MESSAGE);

                    } else if (actionFindHelper.matches == null || actionFindHelper.nextIndex >= actionFindHelper.matches.length) {
                        JOptionPane.showMessageDialog(myFrame, "no more matches", "", JOptionPane.ERROR_MESSAGE);
                    } else {
                        moveToNode(actionFindHelper.matches[actionFindHelper.nextIndex++]);
                    }
                } else {
                    TreePath tp = tree.getSelectionPath();
                    String response = (String) JOptionPane.showInputDialog(myFrame, "Find:", "", JOptionPane.PLAIN_MESSAGE, null, null, actionFindHelper.searchText);
                    if (response == null)
                        return;
                    actionFindHelper.searchText = response;
                    actionFindHelper.startTp = tp;
                    actionFindHelper.nextIndex = 1; // where Find->Next looks
                    TreePath[] tpa = findNodes();
                    if (tpa != null) {
                        actionFindHelper.matches = tpa;
                        if (type == FindType.All) {
                            showAllMatches(tpa);
                        } else {
                            moveToNode(tpa[0]);
                        }
                    } else {
                        JOptionPane.showMessageDialog(myFrame, findWhat + " " + actionFindHelper.searchText + " not found", "", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void moveToNode(TreePath tp) {
                if (DEBUG)
                    System.out.println(tp);
                moveByFind = true;
                tree.setSelectionPath(tp);
                tree.scrollPathToVisible(tp);
                moveByFind = false;
            }

            class MyTreePath extends TreePath {

                TreePath tp;

                MyTreePath(TreePath tp) {
                    this.tp = tp;
                }

                @Override
                public String toString() {
                    return tp.getLastPathComponent().toString();
                }
            }

            protected void showAllMatches(TreePath[] tpa) {
                // We create a depth 1 tree model where the "userObject" is the
                // TreePath to the original node in the threadTree. This allows
                // us to move the selection in the threadTree when the user double-clicks
                // on a node in the depth 1 tree.
                MyTreePath[] mtpa = new MyTreePath[tpa.length];
                for (int i = 0; i < tpa.length; i++) {
                    mtpa[i] = new MyTreePath(tpa[i]);
                }
                final JTree tpaTree = new JTree(mtpa);
                matchJTrees.add(tpaTree);
                MouseListener ml = new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tpaTree.getLastSelectedPathComponent();
                        if (tn == null)
                            return;
                        MyTreePath ttp = (MyTreePath) tn.getUserObject();
                        // ttp is the path to the node in the threadTree
                        if (DEBUG)
                            System.out.println(ttp.tp);
                        tree.setSelectionPath(ttp.tp);
                        tree.scrollPathToVisible(ttp.tp);
                    }
                };
                tpaTree.addMouseListener(ml);
                JScrollPane treeView = new JScrollPane(tpaTree);
                JFrame tpaFrame = new JFrame("Matches for " + actionFindHelper.searchText);
                tpaFrame.add(treeView);
                JMenuBar tpaFrameMenuBar = new JMenuBar();
                JMenu tpaFrameMenu = new JMenu("File");
                tpaFrameMenuBar.add(tpaFrameMenu);
                JMenuItem tpaFrameMenuSaveItem = new JMenuItem(new SaveAction(tpaFrame, new FindSaveActionBody(tpaTree)));
                tpaFrameMenu.add(tpaFrameMenuSaveItem);
                tpaFrameMenu.add(new JMenuItem(new CloseAction(tpaFrame)));
                tpaFrame.setJMenuBar(tpaFrameMenuBar);
                tpaFrame.pack();
                tpaFrame.setVisible(true);
            }

            class FindSaveActionBody implements SaveActionBody {

                JTree myTree;

                FindSaveActionBody(JTree myTree) {
                    this.myTree = myTree;
                }

                @Override
                public void doSave(PrintWriter pw) {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) myTree.getModel().getRoot();
                    for (int i = 0; i < root.getChildCount(); i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                        MyTreePath mtp = (MyTreePath) child.getUserObject();
                        DefaultMutableTreeNode realChild = (DefaultMutableTreeNode) mtp.tp.getLastPathComponent();
                        MethodData md = (MethodData) realChild.getUserObject();
                        pw.println(md);
                    }
                }
            }

            protected TreePath[] findNodes() {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) actionFindHelper.startTp.getLastPathComponent();
                ArrayList<TreePath> result = new ArrayList<TreePath>();
                Pattern pattern;
                try {
                    pattern = Pattern.compile(actionFindHelper.searchText);
                } catch (PatternSyntaxException ex) {
                    JOptionPane.showMessageDialog(myFrame, "Pattern Syntax Error", "", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                matchTree(tn, result, pattern);
                int size = result.size();
                if (size == 0)
                    return null;
                else {
                    return result.toArray(new TreePath[size]);
                }
            }

            protected boolean matchTree(DefaultMutableTreeNode tn, ArrayList<TreePath> result, Pattern pattern) {
                MethodData md = (MethodData) tn.getUserObject();
                ArgsMethodData amd = argsMethodData(md);
                String toMatch = findWhat == FindWhat.Method ? md.methodName : (amd == null ? null : amd.linearizeParams());
                if (toMatch != null && pattern.matcher(toMatch).matches()) {
                    result.add(new TreePath(tn.getPath()));
                    // to make Find->Next easy we find all the matches always
                    // if (type != FindType.All) return true;
                }
                Enumeration en = tn.children();
                while (en.hasMoreElements()) {
                    DefaultMutableTreeNode ctn = (DefaultMutableTreeNode) en.nextElement();
                    if (matchTree(ctn, result, pattern) /* && type != FindType.All */)
                        return true;
                }
                return false;
            }
        }

        class MyTimeAction extends AbstractAction {

            public MyTimeAction(String name) {
                super(name, null);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp = tree.getSelectionPath();
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                TimeInfo ti = timeForNode(tn);
                JOptionPane.showMessageDialog(myFrame, "w=" + TimeFunctions.formatTime(ti.wallTime) + ", u=" + TimeFunctions.formatTime(ti.userUsage) + ", s=" + TimeFunctions.formatTime(ti.sysUsage),
                                "", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private JMenu makeTimeSortMenu() {
            JMenu result = new JMenu("TimeSort");
            result.add(new JMenuItem(new TimeSortAction("Wall")));
            result.add(new JMenuItem(new TimeSortAction("User")));
            result.add(new JMenuItem(new TimeSortAction("Sys")));
            return result;
        }

        class TimeSortAction extends AbstractAction {

            String sortBy;
            Comparator nameAndTimeInfoComparator;
            TimeInfo.Adder timeInfoAdder;
            TimeInfo.Type timeInfoType;

            public TimeSortAction(String name) {
                super(name, null);
                sortBy = name;
                Comparator<TimeInfo> timeInfoComparator = null;
                if (name.equals("Wall")) {
                    timeInfoComparator = new TimeInfo.WallTimeComparator();
                    timeInfoAdder = new TimeInfo.WallTimeAdder();
                    timeInfoType = TimeInfo.Type.WallTime;
                } else if (name.equals("User")) {
                    timeInfoComparator = new TimeInfo.UserUsageComparator();
                    timeInfoAdder = new TimeInfo.UserUsageAdder();
                    timeInfoType = TimeInfo.Type.UserUsage;
                } else if (name.equals("Sys")) {
                    timeInfoComparator = new TimeInfo.SysUsageComparator();
                    timeInfoAdder = new TimeInfo.SysUsageAdder();
                    timeInfoType = TimeInfo.Type.SysUsage;
                }
                nameAndTimeInfoComparator = new NameAndTimeInfoComparator(timeInfoComparator);
            }

            class NameAndTimeInfo {

                String name;
                TimeInfo timeInfo;

                NameAndTimeInfo(String name, TimeInfo timeInfo) {
                    this.name = name;
                    this.timeInfo = timeInfo;
                }
            }

            class NameAndTimeInfoComparator implements Comparator<NameAndTimeInfo> {

                Comparator<TimeInfo> timeInfoComparator;

                NameAndTimeInfoComparator(Comparator<TimeInfo> timeInfoComparator) {
                    this.timeInfoComparator = timeInfoComparator;
                }

                @Override
                public int compare(NameAndTimeInfo n1, NameAndTimeInfo n2) {
                    return timeInfoComparator.compare(n1.timeInfo, n2.timeInfo);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp = tree.getSelectionPath();
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                Map<String, ArrayList<DefaultMutableTreeNode>> map = new HashMap<String, ArrayList<DefaultMutableTreeNode>>();
                visitNodes(map, tn);
                // now we need to sum up the times for the individual methods
                NameAndTimeInfo[] nameAndTimeInfo = new NameAndTimeInfo[map.size()];
                int ix = 0;
                for (Map.Entry<String, ArrayList<DefaultMutableTreeNode>> me : map.entrySet()) {
                    ArrayList<DefaultMutableTreeNode> altn = me.getValue();
                    TimeInfo alti = new TimeInfo();
                    for (DefaultMutableTreeNode xtn : altn) {
                        alti.add(timeForNode(xtn));
                    }
                    nameAndTimeInfo[ix++] = new NameAndTimeInfo(me.getKey(), alti);
                }
                Arrays.sort(nameAndTimeInfo, nameAndTimeInfoComparator);
                long totalTime = 0;
                for (NameAndTimeInfo nti : nameAndTimeInfo) {
                    totalTime = timeInfoAdder.add(totalTime, nti.timeInfo);
                }

                JPanel panel = new JPanel(new SpringLayout());
                panel.add(new JLabel("Percent"));
                panel.add(new JLabel("Time"));
                panel.add(new JLabel("Method"));
                for (int i = nameAndTimeInfo.length - 1; i >= 0; i--) {
                    NameAndTimeInfo nti = nameAndTimeInfo[i];
                    long time = nti.timeInfo.get(timeInfoType);
                    double percent = ((double) time * 100) / totalTime;
                    panel.add(new JTextField(TimeFunctions.ftime(percent, TimeFunctions.format2d)));
                    panel.add(new JTextField(TimeFunctions.formatTime(time)));
                    panel.add(new JTextField(nti.name));
                }
                SpringUtilities.makeCompactGrid(panel, nameAndTimeInfo.length + 1, 3, 3, 3, 3, 3);
                JFrame frame = new JFrame("Sorted Time: " + sortBy);
                JScrollPane scrollPane = new JScrollPane(panel);
                frame.add(scrollPane);
                JMenu frameMenu = new JMenu("File");
                JMenuBar frameMenuBar = new JMenuBar();
                frameMenuBar.add(frameMenu);
                JMenuItem frameMenuSaveItem = new JMenuItem(new SaveAction(frame, new TimeInfoSaveActionBody(nameAndTimeInfo, totalTime)));
                frameMenu.add(frameMenuSaveItem);
                frameMenu.add(new JMenuItem(new CloseAction(frame)));
                frame.setJMenuBar(frameMenuBar);
                frame.pack();
                frame.setVisible(true);
            }

            private void visitNodes(Map<String, ArrayList<DefaultMutableTreeNode>> map, DefaultMutableTreeNode ptn) {
                Enumeration en = ptn.children();
                while (en.hasMoreElements()) {
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode) en.nextElement();
                    MethodData md = (MethodData) tn.getUserObject();
                    ArrayList<DefaultMutableTreeNode> instances = map.get(md.methodName);
                    if (instances == null) {
                        // new
                        instances = new ArrayList<DefaultMutableTreeNode>();
                        map.put(md.methodName, instances);
                    }
                    instances.add(tn);
                    visitNodes(map, tn);
                }
            }

            class TimeInfoSaveActionBody implements SaveActionBody {

                NameAndTimeInfo[] nameAndTimeInfo;
                long totalTime;

                TimeInfoSaveActionBody(NameAndTimeInfo[] nameAndTimeInfo, long totalTime) {
                    this.nameAndTimeInfo = nameAndTimeInfo;
                    this.totalTime = totalTime;
                }

                @Override
                public void doSave(PrintWriter pw) {
                    for (int i = nameAndTimeInfo.length - 1; i >= 0; i--) {
                        NameAndTimeInfo nti = nameAndTimeInfo[i];
                        long time = nti.timeInfo.get(timeInfoType);
                        double percent = ((double) time * 100) / totalTime;
                        pw.print(TimeFunctions.ftime(percent, TimeFunctions.format2d));
                        pw.print("\t");
                        pw.print(TimeFunctions.formatTime(time));
                        pw.print("\t");
                        pw.println(nti.name);
                    }
                }
            }
        }

                private TimeInfo timeForNode(DefaultMutableTreeNode tn) {
                        TimeMethodData mymd = timeMethodData((MethodData) tn
                                        .getUserObject());
                        if (mymd != null) {
                                long childWall = 0;
                                long childUser = 0;
                                long childSys = 0;
                                Enumeration en = tn.children();
                                while (en.hasMoreElements()) {
                                        DefaultMutableTreeNode ctn = (DefaultMutableTreeNode) en
                                                        .nextElement();
                                        TimeMethodData cmd = timeMethodData((MethodData) ctn
                                                        .getUserObject());
                                        childWall += cmd.exitTimeInfo.wallTime
                                                        - cmd.entryTimeInfo.wallTime;
                                        childUser += cmd.exitTimeInfo.userUsage
                                                        - cmd.entryTimeInfo.userUsage;
                                        childSys += cmd.exitTimeInfo.sysUsage
                                                        - cmd.entryTimeInfo.sysUsage;
                                }
                                long myWall = mymd.exitTimeInfo.wallTime
                                                - mymd.entryTimeInfo.wallTime;
                                long myUser = mymd.exitTimeInfo.userUsage
                                                - mymd.entryTimeInfo.userUsage;
                                long mySys = mymd.exitTimeInfo.sysUsage
                                                - mymd.entryTimeInfo.sysUsage;
                                return new TimeInfo(myWall - childWall, myUser - childUser,
                                                mySys - childSys);
                        } else {
                                return new TimeInfo();
                        }
                }
    }

    static class PopupListener extends MouseAdapter {

        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    static class NodeStack {

        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
        int depth = 0;
    }

    private MethodData[] sortMethodData(ArrayList<MethodData> methodDataArray) {
        final MethodData[] result = new MethodData[methodDataArray.size()];
        methodDataArray.toArray(result);
        if (isTimeMethodData(result[0])) {
            Arrays.sort(result, new MethodDataComparator());
        }
        return result;
    }

    enum TraceType {
        Entry, Return, VoidReturn, ReturnByThrow, DefineThread, DefineMethod, DefineParam, StartTime, Call;
        static boolean isReturn(TraceType tt) {
            return tt == Return || tt == VoidReturn || tt == ReturnByThrow;
        }
    }


    public class MethodData {

        public int lineNumber;
        public TraceType ttype;
        public int depth;
        public String thread;
        public String methodName;

        MethodData(TraceType ttype, String thread, int depth, String methodName) {
            this.ttype = ttype;
            this.thread = threadMap.get(thread);
            this.depth = depth;
            this.methodName = methodMap.get(methodName);
            if (this.methodName == null) {
                // forward reference in case where we have per-thread output
                this.methodName = methodName;
                forwardRefs.add(this);
            }

            controlFlowTraces.add(this);
        }

        /**
         * This variant is used for the DefineXXX variants. N.B. definitions do NOT always precede uses!
         *
         * @param ttype
         *            DefineXXX
         * @param realName
         *            the real (full) name of the thread, method, param
         * @param shortForm
         *            the short form that is used in the rest of the trace
         */
        MethodData(TraceType ttype, String realName, String shortForm) {
            this.ttype = ttype;
            this.methodName = shortForm;
            if (ttype == TraceType.DefineThread) {
                if (!isUniqueThreadName(realName)) {
                    realName += "-" + shortForm;
                }
                this.thread = realName;
                threadMap.put(shortForm, realName);
            } else if (ttype == TraceType.DefineMethod) {
                methodMap.put(shortForm, realName);
                for (MethodData m : forwardRefs) {
                    if (shortForm.equals(m.methodName)) {
                        m.methodName = realName;
                    }
                }
            }
        }

        private boolean isUniqueThreadName(String name) {
            for (String userName : threadMap.values()) {
                if (name.equals(userName)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            switch (ttype) {
                case DefineThread:
                    return thread + " stack root";

                case Entry:
                    return methodName;

                default:
                    return "unexpected type " + ttype;
            }
        }

    }

    public class TimeMethodData extends MethodData {

        public TimeInfo entryTimeInfo;
        public TimeInfo exitTimeInfo;

        TimeMethodData(TraceType ttype, String thread, int depth, String methodName, TimeInfo timeInfo) {
            super(ttype, thread, depth, methodName);
            if (ttype == TraceType.Entry || ttype == TraceType.Call) {
                this.entryTimeInfo = timeInfo;
            } else if (TraceType.isReturn(ttype)) {
                this.exitTimeInfo = timeInfo;
            } else {
                throw new IllegalArgumentException("illegal trace type: " + ttype);
            }

        }

        TimeMethodData(TraceType ttype, String realName, String shortForm) {
            super(ttype, realName, shortForm);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (entryTimeInfo.wallTime > 0) {
                sb.append(displayTime(TimeInfo.Type.WallTime));
                sb.append(' ');
                if (entryTimeInfo.userUsage > 0) {
                    sb.append(displayTime(TimeInfo.Type.UserUsage));
                    sb.append(' ');
                }
                if (entryTimeInfo.sysUsage > 0) {
                    sb.append(displayTime(TimeInfo.Type.SysUsage));
                    sb.append(' ');
                }
            }
            sb.append(methodName);
            return sb.toString();
        }

        public String displayTime(TimeInfo.Type tt) {
            String result = null;
            long time = -1;
            // if endInfo == null the method did not return
            switch (tt) {
                case WallTime:
                    if (timeDisplay == TimeDisplay.WallRel) {
                        time = entryTimeInfo.wallTime - traceStartTime;
                    } else if (timeDisplay == TimeDisplay.WallAbs) {
                        time = entryTimeInfo.wallTime;
                    } else if (timeDisplay == TimeDisplay.Duration) {
                        if (exitTimeInfo != null) {
                            time = exitTimeInfo.wallTime - entryTimeInfo.wallTime;
                        }
                    }
                    break;
                case UserUsage:
                    if (timeDisplay == TimeDisplay.Duration) {
                        if (exitTimeInfo != null) {
                            time = exitTimeInfo.userUsage - entryTimeInfo.userUsage;
                        }
                    } else {
                        time = entryTimeInfo.userUsage;
                    }
                    break;
                case SysUsage:
                    if (timeDisplay == TimeDisplay.Duration) {
                        if (exitTimeInfo != null) {
                            time = exitTimeInfo.sysUsage - entryTimeInfo.sysUsage;
                        }
                    } else {
                        time = entryTimeInfo.sysUsage;
                    }
                    break;
            }
            if (time < 0) {
                // no data
                result = "?";
            } else {
                result = TimeFunctions.formatTime(time);
            }
            return result;
        }
    }

    class ArgsMethodData extends TimeMethodData {
        String[] params;
        String thisArg;
        String result;

        ArgsMethodData(TraceType ttype, String thread, int depth, String methodName, String params, TimeInfo timeInfo) {
                super(ttype, thread, depth, methodName, timeInfo);
            /*
             * A method Return trace uses params for the result. For Entry and Call, params includes the this arg,
             * (null for a static method call) We separate out the this arg here and check for short forms (which
             * may be undefined at this stage).
             */
            final String[] splitParams = params.split(",");
            if (ttype == TraceType.Return) {
                this.params = new String[1];
                this.params[0] = checkParamMap(splitParams[0]);
            } else {
                thisArg = checkParamMap(splitParams[0]);
                if (splitParams.length == 1) {
                    // no actual args
                    this.params = new String[0];
                } else {
                    this.params = new String[splitParams.length - 1];
                    for (int i = 1; i < splitParams.length; i++) {
                        this.params[i - 1] = checkParamMap(splitParams[i]);
                    }
                }
            }
        }

        ArgsMethodData(TraceType ttype, String realName, String shortForm) {
                super(ttype, realName, shortForm);
            paramMap.put(shortForm, realName);
            for (ArgsMethodData m : forwardParamRefs) {
                // we don't know exactly which param is involved
                if (m.thisArg != null && m.thisArg.equals(shortForm)) {
                    m.thisArg = realName;
                }
                if (m.result != null && m.result.equals(shortForm)) {
                    m.result = realName;
                }
                for (int i = 0; i < m.params.length; i++) {
                    if (m.params[i].equals(shortForm)) {
                        m.params[i] = realName;
                    }
                }
            }
        }

                /**
                 * Check if arg is a param id, in which case the value is in the map
                 *
                 * @param arg
                 */
                private String checkParamMap(String arg) {
                        if (arg.charAt(0) == 'A') {
                                final String s = paramMap.get(arg);
                                if (s == null) {
                                        forwardParamRefs.add(this);
                                        return arg;
                                } else {
                                        return s;
                                }
                        } else {
                                return arg;
                        }
                }


        private String linearizeParams() {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String part : params) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(part);
            }
            return sb.toString();
        }

                @Override
                public String toString() {
                        String s = super.toString();
                        if (ttype == TraceType.Entry)
                                if (params != null) {
                                        s += "(" + linearizeParams() + ")";
                                }
                        if (result != null) {
                                s += " returned " + result;
                        }
                        return s;
                }

    }

    private static boolean isArgsMethodData(MethodData md) {
        return md instanceof ArgsMethodData;
    }

    private static ArgsMethodData argsMethodData(MethodData md) {
        if (isArgsMethodData(md)) {
                return (ArgsMethodData) md;
        } else {
                return null;
        }
    }

    private static boolean isTimeMethodData(MethodData md) {
        return md instanceof TimeMethodData;
    }

    private static TimeMethodData timeMethodData(MethodData md) {
        if (isTimeMethodData(md)) {
                return (TimeMethodData) md;
        } else {
                return null;
        }
    }

    private MethodData createMethodData(TraceType ttype, String thread, int depth, String methodName, String params, TimeInfo timeInfo) {
        if (params == null) {
                if (timeInfo == null) {
                        return new MethodData(ttype, thread, depth, methodName);
                }
                return new TimeMethodData(ttype, thread, depth, methodName, timeInfo);
        } else {
                return new ArgsMethodData(ttype, thread, depth, methodName, params, timeInfo);
        }
    }

    private MethodData createMethodData(TraceType ttype, String realName, String shortForm) {
        if (ttype == TraceType.DefineParam) {
                return new ArgsMethodData(ttype, realName, shortForm);
        } else {
                return new MethodData(ttype, realName, shortForm);
        }
    }

    static class MethodDataComparator implements Comparator<MethodData> {
        @Override
        public int compare(MethodData xa, MethodData xb) {
                TimeMethodData a = timeMethodData(xa);
                TimeMethodData b = timeMethodData(xb);
                if (a == null || b == null) {
                        return 0;
                }
            if (a.ttype == TraceType.Entry || a.ttype == TraceType.Call) {
                if (b.ttype == TraceType.Entry || b.ttype == TraceType.Call) {
                    return a.entryTimeInfo.wallTime < b.entryTimeInfo.wallTime ? -1 : 1;
                } else {
                    assert TraceType.isReturn(b.ttype);
                    return a.entryTimeInfo.wallTime < b.exitTimeInfo.wallTime ? -1 : 1;
                }
            } else if (TraceType.isReturn(a.ttype)){
                if (TraceType.isReturn(b.ttype)) {
                    return a.exitTimeInfo.wallTime < b.exitTimeInfo.wallTime ? -1 : 1;
                } else {
                    return a.exitTimeInfo.wallTime < b.entryTimeInfo.wallTime ? -1 : 1;
                }
            } else {
                assert false;
                return 0;
            }
        }
    }

    public static class TimeInfo {

        static interface Adder {

            long add(long sum, TimeInfo ti);
        }

        enum Type {
            WallTime, UserUsage, SysUsage
        }

        public long wallTime;
        public long userUsage;
        public long sysUsage;

        TimeInfo() {
        }

        TimeInfo(long wallTime, long userUsage, long sysUsage) {
            this.wallTime = wallTime;
            this.userUsage = userUsage;
            this.sysUsage = sysUsage;
        }

        @Override
        public String toString() {
            return "[t " + wallTime + ", u " + userUsage + ", s " + sysUsage + "]";
        }

        long get(Type t) {
            if (t == Type.WallTime)
                return wallTime;
            else if (t == Type.UserUsage)
                return userUsage;
            else
                return sysUsage;
        }

        void add(TimeInfo ti) {
            this.wallTime += ti.wallTime;
            this.userUsage += ti.userUsage;
            this.sysUsage += ti.sysUsage;
        }

        static class WallTimeAdder implements TimeInfo.Adder {

            // result = sum + ti.wallTime;
            @Override
            public long add(long sum, TimeInfo ti) {
                return sum + ti.wallTime;
            }
        }

        static class UserUsageAdder implements TimeInfo.Adder {

            // result = sum + ti.userUsage;
            @Override
            public long add(long sum, TimeInfo ti) {
                return sum + ti.userUsage;
            }
        }

        static class SysUsageAdder implements TimeInfo.Adder {

            // result = sum + ti.susUsage;
            @Override
            public long add(long sum, TimeInfo ti) {
                return sum + ti.sysUsage;
            }
        }

        static class WallTimeComparator implements Comparator<TimeInfo> {

            @Override
            public int compare(TimeInfo t1, TimeInfo t2) {
                if (t1.wallTime < t2.wallTime)
                    return -1;
                else if (t1.wallTime > t2.wallTime)
                    return +1;
                else
                    return 0;
            }
        }

        static class UserUsageComparator implements Comparator<TimeInfo> {

            @Override
            public int compare(TimeInfo t1, TimeInfo t2) {
                if (t1.userUsage < t2.userUsage)
                    return -1;
                else if (t1.userUsage > t2.userUsage)
                    return +1;
                else
                    return 0;
            }
        }

        static class SysUsageComparator implements Comparator<TimeInfo> {

            @Override
            public int compare(TimeInfo t1, TimeInfo t2) {
                if (t1.sysUsage < t2.sysUsage)
                    return -1;
                else if (t1.sysUsage > t2.sysUsage)
                    return +1;
                else
                    return 0;
            }
        }
    }

    public static class TimeFunctions {

        static DecimalFormat format2d = new DecimalFormat("#.##");
        static DecimalFormat format3d = new DecimalFormat("#.###");
        static DecimalFormat format6d = new DecimalFormat("#.######");
        static DecimalFormat format9d = new DecimalFormat("#.#########");

        public static String formatTime(long time) {
            String ds = null;
            switch (timeFormat) {
                case Milli:
                    ds = ftime(mtime(time), format6d) + "ms";
                    break;
                case Micro:
                    ds = ftime(utime(time), format3d) + "us";
                    break;
                case Sec:
                    ds = ftime(stime(time), format9d) + "s";
                    break;
                case Nano:
                    ds = Long.toString(time) + "ns";
            }
            return ds;
        }

        private static double utime(long time) {
            return (double) time / 1000;
        }

        private static double mtime(long time) {
            return (double) time / (1000 * 1000);
        }

        private static double stime(long time) {
            return (double) time / (1000 * 1000 * 1000);
        }

        static String ftime(double time, DecimalFormat f) {
            return f.format(time);
        }
    }

    abstract class DataParser {
        abstract Map<String, DefaultMutableTreeNode>  processData() throws Exception;

        protected Map<String, NodeStack> forest;
        protected Map<String, DefaultMutableTreeNode> result;
        protected MethodData md;
        protected ArgsMethodData amd;
        protected TimeMethodData tmd;
        protected Map<String, TimeInfo> lastTimeInfoMap = new HashMap<String, TimeInfo>();


        protected DataParser() {
            result = new HashMap<String, DefaultMutableTreeNode>();
            forest = new HashMap<String, NodeStack>();
        }

        protected abstract void error(String msg);

        protected void setMd(MethodData md) {
            this.md = md;
            amd = argsMethodData(md);
            tmd = timeMethodData(md);
        }

        protected void handleMethodData() {
            NodeStack ns = forest.get(md.thread);
            switch (md.ttype) {
                case Entry:
                    DefaultMutableTreeNode nnode = new DefaultMutableTreeNode(md);
                    nodeCount++;
                    ns.nodes.get(md.depth - 1).add(nnode);
                    if (md.depth > ns.depth) {
                        if (md.depth >= ns.nodes.size())
                            ns.nodes.add(null);
                    } else if (md.depth < ns.depth) {
                    } else {
                    }
                    ns.nodes.set(md.depth, nnode);
                    ns.depth = md.depth;
                    break;

                case Return:
                case VoidReturn:
                case ReturnByThrow:
                    // We find the entry that corresponds to this return and copy the exit time
                    // in the Return node into the Entry node and ditto for the result.
                    DefaultMutableTreeNode parent = ns.nodes.get(md.depth - 1);
                    int count = parent.getChildCount();
                    boolean found = false;
                    for (int i = count - 1; i >= 0; i--) {
                        DefaultMutableTreeNode tn = (DefaultMutableTreeNode) parent.getChildAt(i);
                        MethodData cmd = (MethodData) tn.getUserObject();
                        if (cmd.methodName.equals(md.methodName)) {
                            TimeMethodData tcmd = timeMethodData(cmd);
                            if (tcmd != null) {
                                tcmd.exitTimeInfo = tmd.exitTimeInfo;
                                if (md.ttype == TraceType.Return && amd != null) {
                                       ArgsMethodData acmd = argsMethodData(cmd);
                                    assert acmd != null;
                                    acmd.result = acmd.checkParamMap(amd.params[0]);
                                }
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        error("failed to find return for " + md.methodName);
                    }
                    break;

                case DefineThread:
                    ns = new NodeStack();
                    ns.nodes.add(new DefaultMutableTreeNode(md));
                    forest.put(md.thread, ns);
                    break;

                case DefineMethod:
            }

        }

        void finish() {
            for (Map.Entry<String, NodeStack> me : forest.entrySet()) {
                result.put(me.getKey(), me.getValue().nodes.get(0));
            }

            // generate sorted trace
            sortedControlFlowTraces = sortMethodData(controlFlowTraces);
        }
    }

    class ARDataParser extends DataParser {
        @Override
        Map<String, DefaultMutableTreeNode>  processData() throws Exception {
            createNodes();
            return result;
        }

        @Override
        protected void error(String msg) {
            System.err.println(msg);
        }

        class ThreadStack {
            String[] methods = new String[1024];
            int entryIndex = 1;

            void inc() {
                entryIndex++;
                if (entryIndex == methods.length) {
                    String[] newMethods = new String[methods.length * 2];
                    System.arraycopy(methods, 0, newMethods, 0, methods.length);
                    methods = newMethods;
                }
            }

        }

        private void createNodes() {
            Map<String, ThreadStack> threadMethodStackMap = new HashMap<String, ThreadStack>();
            for (ThreadRecord tr : traceRun.threads.values()) {
                md = createMethodData(TraceType.DefineThread, tr.getName(), tr.getName());
                handleMethodData();
            }
            for (int arIndex = 0; arIndex <  traceRun.adviceRecordList.size(); arIndex++) {
                AdviceRecord ar = traceRun.adviceRecordList.get(arIndex);
                RecordType art = ar.getRecordType();
                switch (art) {
                    case MethodEntry: {
                        ObjectMethodAdviceRecord oar = (ObjectMethodAdviceRecord) ar;
                        String threadName = ((ThreadRecord) ar.thread).getName();
                        MethodRecord mr = (MethodRecord) oar.value2;
                        TimeInfo timeInfo = new TimeInfo();
                        timeInfo.wallTime = ar.time;
                        ThreadStack ts = threadMethodStackMap.get(threadName);
                        if (ts == null) {
                            ts = new ThreadStack();
                            threadMethodStackMap.put(threadName, ts);
                        }
                        ts.methods[ts.entryIndex] = mr.getQualName();
                        checkMethodDef(ts.methods[ts.entryIndex]);
                        setMd(createMethodData(TraceType.Entry, threadName, ts.entryIndex, ts.methods[ts.entryIndex], null, timeInfo));
                        handleMethodData();
                        ts.inc();
                        break;
                    }
                    case Return:
                    case ReturnLong:
                    case ReturnFloat:
                    case ReturnDouble:
                    case ReturnObject:
                    case ReturnByThrow: {
                        TimeInfo timeInfo = new TimeInfo();
                        timeInfo.wallTime = ar.time;
                        String threadName = ((ThreadRecord) ar.thread).getName();
                        ThreadStack ts = threadMethodStackMap.get(threadName);
                        assert ts != null;
                        if (art == RecordType.ReturnByThrow) {
                            ObjectLongAdviceRecord oar = (ObjectLongAdviceRecord) ar;
                            ts.entryIndex -= oar.getPackedValue();
                        } else {
                            ts.entryIndex--;
                        }
                        setMd(createMethodData(forReturn(art),
                                        threadName, ts.entryIndex, ts.methods[ts.entryIndex], null, timeInfo));
                        handleMethodData();
                        break;
                    }

                    default:
                }
            }
            finish();
        }

        private TraceType forReturn(RecordType rt) {
            switch (rt) {
                case Return: return TraceType.VoidReturn;
                case ReturnByThrow: return TraceType.ReturnByThrow;
                default: return TraceType.Return;
            }
        }

        private void checkMethodDef(String methodName) {
            if (methodMap.get(methodName) == null) {
                methodMap.put(methodName, methodName);
            }
        }

    }

    class AJFileDataParser extends DataParser {

        int lineCount;

        @Override
        Map<String, DefaultMutableTreeNode>  processData() throws Exception {
            createNodes(traceFilePathName);
            return result;
        }

        @Override
        protected void error(String msg) {
            System.err.println("line " + lineCount + " " + msg);
        }

        void createNodes(String traceFilePathName) throws Exception {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new FileReader(traceFilePathName));

                lineCount = 1;
                long startTime = System.currentTimeMillis();
                long incTime = startTime;
                long lastFreeMemory = 0;
                if (GC) {
                    System.gc();
                    lastFreeMemory = Runtime.getRuntime().freeMemory();
                }
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    if (lineCount == debugLine) {
                        System.console();
                    }
                    setMd(parseLine(line));
                    md.lineNumber = lineCount;
                    lineCount++;
                    if (PROGRESS && (lineCount % 10000 == 0)) {
                        final long now = System.currentTimeMillis();
                        System.out.println("processed " + lineCount + " lines in " + (now - startTime) + " inc " + (now - incTime));
                        incTime = now;
                        if (GC) {
                            System.gc();
                            final long freeMemoryNow = Runtime.getRuntime().freeMemory();
                            System.out.println("memory per line " + ((lastFreeMemory - freeMemoryNow) / 10000));
                            lastFreeMemory = freeMemoryNow;
                        }
                    }
                    if (DEBUG) {
                        System.out.print("line " + lineCount + ", " + md.ttype + " ");
                        switch (md.ttype) {
                            case StartTime:
                                System.out.println(traceStartTime);
                                break;
                            case Entry:
                            case Return:
                            case VoidReturn:
                            case Call:
                                System.out.print("d " + md.depth);
                                if (tmd != null) {
                                    System.out.print(", " + (TraceType.isReturn(md.ttype) ? tmd.exitTimeInfo : tmd.entryTimeInfo));
                                }
                                System.out.print(", " + md.thread + ", " + md.methodName);
                                if (amd != null) {
                                    System.out.print("(" + amd.params + ")");

                                }
                                System.out.println();
                                break;

                            case DefineThread:
                            case DefineMethod:
                                System.out.println(md.methodName + " " + md.thread);
                                break;
                        }
                    }
                    handleMethodData();
                }
                finish();
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }

        private MethodData parseLine(String line) throws Exception {
            // Format, four cases, [] optional
            // 0 S S t start time
            // 0 D TX name define short name TX for thread name
            // 0 M MX name define short name MX for method name
            // 0 P AX name define short name AX for arg/result name
            // d E[t] T M[( ... )] method M entry [at time t,u,s] in thread T, optional args
            // d R[t] M [ (result) ] method M return [at time t,u,s] with optional result
            // d C[t] T M[( ... )] method M call [at time t,u,s] in thread T, optional args
            // wall time is relative to start time

            int s1 = line.indexOf(' '); // before E/R
            int s2 = line.indexOf(' ', s1 + 1); // before T
            int s3 = line.indexOf(' ', s2 + 1); // before M
            if (s1 < 0 || s2 < 0 || s3 < 0)
                throw new Exception("syntax error");

            int depth = Integer.parseInt(line.substring(0, s1));
            String ttype = line.substring(s1 + 1, s2);
            String thread = line.substring(s2 + 1, s3);
            String params = null;
            String methodName = null;

            int s4 = line.indexOf('(', s3 + 1);
            if (s4 > 0) {
                methodName = line.substring(s3 + 1, s4);
                params = line.substring(s4 + 1, line.length() - 1);
            } else {
                methodName = line.substring(s3 + 1);
            }

            if (depth == 0) {
                if (ttype.equals("D")) {
                    lastTimeInfoMap.put(thread, new TimeInfo());
                    return createMethodData(TraceType.DefineThread, methodName, thread);
                } else if (ttype.equals("M")) {
                    return createMethodData(TraceType.DefineMethod, methodName, thread);
                } else if (ttype.equals("P")) {
                    return createMethodData(TraceType.DefineParam, methodName, thread);
                } else if (ttype.equals("S")) {
                    traceStartTime = Long.parseLong(methodName);
                    return createMethodData(TraceType.StartTime, methodName, thread);
                } else {
                    throw new Exception("non-D/M trace at depth 0");
                }
            } else {
                // E, R, V, C
                TraceType traceType = getTraceType(ttype);
                TimeInfo timeInfo = getTimeInfo(ttype, false);
                if (timeInfo != null) {
                    TimeInfo lastTimeInfo = lastTimeInfoMap.get(thread);
                    if (timeInfo.userUsage == 0)
                        timeInfo.userUsage = lastTimeInfo.userUsage;
                    else
                        lastTimeInfo.userUsage = timeInfo.userUsage;
                    if (timeInfo.sysUsage == 0)
                        timeInfo.sysUsage = lastTimeInfo.sysUsage;
                    else
                        lastTimeInfo.sysUsage = timeInfo.sysUsage;
                }
                return createMethodData(traceType, thread, depth, methodName, params, timeInfo);
            }
        }

        private TimeInfo getTimeInfo(String t, boolean absolute) {
            if (t.length() > 1) {
                TimeInfo result = new TimeInfo();
                int t1 = t.indexOf(',');
                int t2 = t.indexOf(',', t1 + 1);
                if (t1 > 0) {
                    result.wallTime = Long.parseLong(t.substring(1, t1));
                    result.userUsage = Long.parseLong(t.substring(t1 + 1, t2));
                    result.sysUsage = Long.parseLong(t.substring(t2 + 1));
                } else {
                    result.wallTime = Long.parseLong(t.substring(1));
                }
                if (!absolute) {
                    result.wallTime += traceStartTime;
                }
                return result;
            } else {
                return null;
            }
        }

        private TraceType getTraceType(String token) {
            switch (token.charAt(0)) {
                case 'R':
                    return TraceType.Return;
                case 'V':
                    return TraceType.VoidReturn;
                case 'E':
                    return TraceType.Entry;
                case 'C':
                    return TraceType.Call;
                default:
                    throw new IllegalArgumentException("unknow trace type " + token);
            }
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI(String[] args) {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }
        createAndShowGUIInternal(args);
    }

    private static void createAndShowGUIInternal(String[] args) {
        try {
            callGraphDisplay = new CallGraphDisplay();
            callGraphDisplay.processArgsAndData(args);
            callGraphDisplay.createGUI();
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        // ------------ File ---------------
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem(new OpenAction());
        fileMenu.add(openItem);
        JMenuItem snapShotItem = new JMenuItem(new SnapShotAction());
        fileMenu.add(snapShotItem);
        fileMenu.addSeparator();
        JMenuItem propsItem = new JMenuItem(new PropsAction());
        fileMenu.add(propsItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem(new ExitAction());
        fileMenu.add(exitItem);
        // ------------ Edit ---------------
        // JMenu edit = new JMenu("Edit");
        // menuBar.add(edit);
        // JMenuItem findItem = new JMenuItem(new FindAction());
        // edit.add(findItem);
        // ------------ Show ---------------
        JMenu showMenu = new JMenu("Show");
        ButtonGroup showGroup = new ButtonGroup();
        JRadioButtonMenuItem etaItem = new JRadioButtonMenuItem(new ElapsedTimeAction());
        JRadioButtonMenuItem absetaItem = new JRadioButtonMenuItem(new AbsElapsedTimeAction());
        JRadioButtonMenuItem durItem = new JRadioButtonMenuItem(new DurationTimeAction());
        showGroup.add(etaItem);
        showGroup.add(absetaItem);
        showGroup.add(durItem);
        durItem.setSelected(true);
        JMenu showTimeSubMenu = new JMenu("Time");
        showTimeSubMenu.add(etaItem);
        showTimeSubMenu.add(absetaItem);
        showTimeSubMenu.add(durItem);
        showMenu.add(showTimeSubMenu);
        showMenu.addSeparator();
        JMenuItem selItem = new JMenuItem(new ScrollSelAction());
        showMenu.add(selItem);
        menuBar.add(showMenu);
        // ------------ Format ---------------
        JMenu formatMenu = new JMenu("Format");
        JMenu timeSubMenu = new JMenu("Time");
        ButtonGroup timeGroup = new ButtonGroup();
        JRadioButtonMenuItem nanoItem = new JRadioButtonMenuItem(new TimeAction(TimeFormat.Nano));
        JRadioButtonMenuItem microItem = new JRadioButtonMenuItem(new TimeAction(TimeFormat.Micro));
        JRadioButtonMenuItem milliItem = new JRadioButtonMenuItem(new TimeAction(TimeFormat.Milli));
        JRadioButtonMenuItem secItem = new JRadioButtonMenuItem(new TimeAction(TimeFormat.Sec));
        switch (timeFormat) {
            case Nano: nanoItem.setSelected(true); break;
            case Micro: microItem.setSelected(true); break;
            case Milli: milliItem.setSelected(true); break;
            case Sec: secItem.setSelected(true); break;
        }

        timeGroup.add(nanoItem); timeGroup.add(microItem);
        timeGroup.add(milliItem); timeGroup.add(secItem);
        timeSubMenu.add(nanoItem); timeSubMenu.add(microItem);
        timeSubMenu.add(milliItem); timeSubMenu.add(secItem);
        formatMenu.add(timeSubMenu);
        menuBar.add(formatMenu);

        return menuBar;
    }

    public static void main(final String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI(args);
            }
        });
    }

    public static CallGraphDisplay queryMain(TraceRun traceRunArg, String[] args) {
        if (!callGraphDisplayDataValid) {
            traceRun = traceRunArg;
            main(args);
            while (!callGraphDisplayDataValid) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
        return callGraphDisplay;
    }

    class OpenAction extends AbstractAction {

        public OpenAction() {
            super("Open", null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(myFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                createAndShowGUIInternal(new String[] { "-f", file.getPath()});
            }
        }
    }

    class PropsAction extends AbstractAction {

        public PropsAction() {
            super("Properties", null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(myFrame, "Trace file path: " + traceFilePathName, "", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    static class ExitAction extends AbstractAction {

        public ExitAction() {
            super("Exit", null);
            // putValue(SHORT_DESCRIPTION, desc);
            // putValue(MNEMONIC_KEY, mnemonic);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    static class CloseAction extends AbstractAction {

        JFrame frame;

        public CloseAction(JFrame frame) {
            super("Close", null);
            this.frame = frame;
            // putValue(SHORT_DESCRIPTION, desc);
            // putValue(MNEMONIC_KEY, mnemonic);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
        }
    }

    class TimeAction extends AbstractAction {

        TimeFormat tf;

        public TimeAction(TimeFormat tf) {
            super(tf.toString());
            this.tf = tf;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            timeFormat = tf;
            repaintTrees();
        }
    }

    class FindAction extends AbstractAction {

        public FindAction() {
            super("Find");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    class SnapShotAction extends AbstractAction {

        public SnapShotAction() {
            super("Snapshot");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    class AbsElapsedTimeAction extends AbstractAction {

        public AbsElapsedTimeAction() {
            super("Wall Clock Absolute");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            timeDisplay = TimeDisplay.WallAbs;
            repaintTrees();
        }
    }

    class ElapsedTimeAction extends AbstractAction {

        public ElapsedTimeAction() {
            super("Wall Clock Relative");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            timeDisplay = TimeDisplay.WallRel;
            repaintTrees();
        }
    }

    class DurationTimeAction extends AbstractAction {

        public DurationTimeAction() {
            super("Duration");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            timeDisplay = TimeDisplay.Duration;
            repaintTrees();
        }
    }

    class ScrollSelAction extends AbstractAction {

        public ScrollSelAction() {
            super("Selection");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TreePath tp = currentJTree.getSelectionPath();
            currentJTree.scrollPathToVisible(tp);
        }
    }

    void repaintTrees() {
        for (JTree tree : threadJTrees.values()) {
            tree.repaint();
        }
        for (JTree tree : matchJTrees) {
            tree.repaint();
        }
    }

    static interface SaveActionBody {

        public void doSave(PrintWriter pw);
    }

    static class SaveAction extends AbstractAction {

        JFrame frame;
        SaveActionBody body;

        public SaveAction(JFrame frame, SaveActionBody body) {
            super("Save", null);
            this.frame = frame;
            this.body = body;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file.exists()) {
                    int r = JOptionPane.showConfirmDialog(frame, "File exists, ok to overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.NO_OPTION)
                        return;
                }
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(new FileWriter(file));
                    body.doSave(pw);
                    JOptionPane.showMessageDialog(frame, "Data saved", "", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "error writing file", "", JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (pw != null)
                        pw.close();
                }
            }
        }
    }

}
