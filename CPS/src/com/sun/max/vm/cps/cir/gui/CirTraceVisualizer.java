/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.gui.CirAnnotatedTrace.*;
import com.sun.max.vm.cps.cir.gui.CirAnnotatedTrace.Element;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.value.*;

/**
 * A GUI for capturing and viewing CIR graph traces.
 *
 * @author Doug Simon
 * @author Sumeet Panchal
 */
final class CirTraceVisualizer extends JPanel {

    /**
     * The name of the file to which the visualizer's settings are persisted.
     */
    private static final String SETTINGS_FILE_NAME = "." + CirTraceVisualizer.class.getSimpleName() + ".properties";

    /**
     * The default size and position of the visualizer.
     */
    private static final Rectangle DEFAULT_FRAME_BOUNDS = new Rectangle(100, 100, 600, 400);

    /**
     * The initial font size of the CIR trace text.
     */
    private static final int DEFAULT_FONT_SIZE = 12;

    private static final String NO_SELECTED_NODE_DETAIL = "<html><table border=\"0\"></table></html>";

    static class HeaderPanel extends JPanel {
        private final JLabel classMethodActorLabel;
        //Saves old signature after we highlight search term in signature.
        private String oldClassMethodActor;

        public HeaderPanel() {

            classMethodActorLabel = new JLabel("");
            classMethodActorLabel.setFont(new Font("Monospace", Font.PLAIN, 26));

            add(classMethodActorLabel);
        }

        void update(CirAnnotatedTrace cirAnnotatedTrace, CirAnnotatedTrace cirAnnotatedTrace2) {
            final MethodActor classMethodActor = cirAnnotatedTrace.classMethodActor();
            if (classMethodActor != null) {
                classMethodActorLabel.setText(classMethodActor.format("%H.%n(%p)"));
                oldClassMethodActor = classMethodActor.format("%H.%n(%p)");
            } else {
                classMethodActorLabel.setText("");
            }
        }
    }

    abstract class NavigationPanel extends JPanel {
        private final JButton previousButton;
        private final JFormattedTextField current;
        private final JTextField total;
        private final JButton nextButton;
        private final String units;

        NavigationPanel(String prevLabel, String nextLabel, String units) {
            super(new FlowLayout(FlowLayout.CENTER));
            this.units = units;
            previousButton = new JButton(new AbstractAction(prevLabel) {
                public void actionPerformed(ActionEvent event) {
                    final int currentIndex = getCurrentIndex();
                    if (currentIndex > 0) {
                        setCurrentIndex(currentIndex - 1);
                        refreshView();
                    }
                }
            });
            nextButton = new JButton(new AbstractAction(nextLabel) {
                public void actionPerformed(ActionEvent event) {
                    final int currentIndex = getCurrentIndex();
                    if (currentIndex < getMaximumIndex() - 1) {
                        setCurrentIndex(currentIndex + 1);
                        refreshView();
                    }
                }
            });

            current = new JFormattedTextField(NumberFormat.getNumberInstance()) {

                @Override
                public void commitEdit() throws ParseException {
                    final Number oldValue = (Number) getValue();
                    try {
                        final int newValue = Integer.parseInt(getText());
                        if (oldValue != null && oldValue.intValue() == newValue) {
                            return;
                        }
                        if (newValue >= 1 && newValue <= getMaximumIndex()) {
                            super.commitEdit();
                            setCurrentIndex(newValue - 1);
                            refreshView();
                            return;
                        }
                    } catch (NumberFormatException e) {
                    }
                    if (oldValue != null) {
                        setText(oldValue.toString());
                    } else {
                        final int currentIndex = getCurrentIndex();
                        setText(currentIndex == -1 ? "--" : "" + currentIndex + 1);
                    }
                }
            };

            current.setColumns(8);
            current.setText("0");
            current.setHorizontalAlignment(JTextField.RIGHT);

            total = new JTextField("0 " + units, 8);
            total.setEditable(false);
            total.setBorder(null);
            total.setHorizontalAlignment(JTextField.LEFT);

            add(previousButton);
            add(current);
            add(new JLabel("of"));
            add(total);
            add(nextButton);
        }

        public void update() {
            final int currentIndex = getCurrentIndex();
            previousButton.setEnabled(currentIndex > 0);
            nextButton.setEnabled(currentIndex < getMaximumIndex() - 1);
            current.setText("" + (currentIndex + 1));
            total.setText(getMaximumIndex() + " " + units);
        }

        protected abstract int getCurrentIndex();
        protected abstract void setCurrentIndex(int newIndex);
        protected abstract int getMaximumIndex();
    }

    protected class TraceNavigationPanel extends NavigationPanel {
        TraceNavigationPanel() {
            super("<", ">", "traces");
        }

        @Override
        protected int getCurrentIndex() {
            return indexWithinTrace;
        }

        @Override
        protected void setCurrentIndex(int newIndex) {
            indexWithinTrace = newIndex;
        }

        @Override
        protected int getMaximumIndex() {
            final List<CirAnnotatedTrace> currentList = currentTraceList();
            if (currentList != null) {
                return currentList.size();
            }
            return 0;
        }
    }

    protected class TraceListNavigationPanel extends NavigationPanel {
        TraceListNavigationPanel() {
            super("<<", ">>", "methods");
        }
        @Override
        protected int getCurrentIndex() {
            return currentTraceListIndex;
        }

        @Override
        protected void setCurrentIndex(int newIndex) {
            currentTraceListIndex = newIndex;
            indexWithinTrace = 0;
        }

        @Override
        protected int getMaximumIndex() {
            return allTraceLists.size();
        }
    }

    class TracePanel extends JPanel {

        private final JLabel description;
        private final JScrollPane traceView;

        public TracePanel(JTextPane tracePane) {
            setLayout(new BorderLayout());

            description = new JLabel("");
            description.setFont(new Font("SansSerif", Font.PLAIN, 24));
            traceView = new JScrollPane(tracePane);

            final JPanel descriptionPanel = new JPanel();
            descriptionPanel.add(description);

            add(descriptionPanel, BorderLayout.NORTH);
            add(traceView, BorderLayout.CENTER);
        }

        void update(CirAnnotatedTrace trace) {
            if (trace != null) {
                description.setText(trace.description());
            } else {
                description.setText("");
            }
        }
    }

    private class TextListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent keyEvent) {
            if (keyEvent.getSource() == searchPanel.actorText) {
                final String text = searchPanel.actorText.getText();
                if (text.length() > 0) {
                    if (!searchPanel.nodeCheck.isSelected()) {
                        searchPanel.prevButton.setEnabled(true);
                        searchPanel.nextButton.setEnabled(true);
                    } else {
                        if (!searchPanel.nodeText.getText().equals("")) {
                            searchPanel.prevButton.setEnabled(true);
                            searchPanel.nextButton.setEnabled(true);
                        }
                    }
                } else {
                    searchPanel.prevButton.setEnabled(false);
                    searchPanel.nextButton.setEnabled(false);
                }
            }
            if (keyEvent.getSource() == searchPanel.nodeText) {
                final String text = searchPanel.nodeText.getText();
                if (text.length() > 0) {
                    if (!searchPanel.actorCheck.isSelected()) {
                        searchPanel.prevButton.setEnabled(true);
                        searchPanel.nextButton.setEnabled(true);
                    } else {
                        if (!searchPanel.actorText.getText().equals("")) {
                            searchPanel.prevButton.setEnabled(true);
                            searchPanel.nextButton.setEnabled(true);
                        }
                    }
                } else {
                    searchPanel.prevButton.setEnabled(false);
                    searchPanel.nextButton.setEnabled(false);
                }
            }
        }
    }

    class SearchPanel extends JPanel {
        private final JCheckBox actorCheck;
        private final JTextField actorText;
        private final JCheckBox nodeCheck;
        private final JTextField nodeText;
        private final JCheckBox regexCheck;
        private final JButton nextButton;
        private final JButton prevButton;
        private final JCheckBox matchCaseCheck;
        private final JCheckBox wrapSearch;

        public SearchPanel() {
            actorCheck = new JCheckBox("Method signature");
            actorCheck.setSelected(false);
            actorText = new JTextField(15);
            actorText.setEnabled(false);
            nodeCheck = new JCheckBox("Node label");
            nodeCheck.setSelected(false);
            nodeText = new JTextField(15);
            nodeText.setEnabled(false);
            regexCheck = new JCheckBox("Regular expression");
            regexCheck.setSelected(false);
            matchCaseCheck = new JCheckBox("Match case");
            matchCaseCheck.setSelected(false);
            wrapSearch = new JCheckBox("Wrap search");
            wrapSearch.setSelected(false);

            actorCheck.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    actorText.setEnabled(actorCheck.isSelected());
                    if (actorCheck.isSelected()) {
                        searchPanel.prevButton.setEnabled(true);
                        searchPanel.nextButton.setEnabled(true);
                    } else {
                        if (searchPanel.nodeCheck.isSelected() && !searchPanel.nodeText.getText().equals("")) {
                            searchPanel.prevButton.setEnabled(true);
                            searchPanel.nextButton.setEnabled(true);
                        } else {
                            searchPanel.prevButton.setEnabled(false);
                            searchPanel.nextButton.setEnabled(false);
                        }
                    }
                    clearSearchHighlights();
                }
            });
            nodeCheck.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    nodeText.setEnabled(nodeCheck.isSelected());
                    if (nodeCheck.isSelected()) {
                        searchPanel.prevButton.setEnabled(false);
                        searchPanel.nextButton.setEnabled(false);
                    } else {
                        if (searchPanel.actorCheck.isSelected() && !searchPanel.actorText.getText().equals("")) {
                            searchPanel.prevButton.setEnabled(true);
                            searchPanel.nextButton.setEnabled(true);
                        } else {
                            searchPanel.prevButton.setEnabled(false);
                            searchPanel.nextButton.setEnabled(false);
                        }
                    }
                    clearSearchHighlights();
                }
            });

            prevButton =  new JButton(new AbstractAction("Previous") {
                public void actionPerformed(ActionEvent event) {
                    final int returnedIndex = getNextSearch(actorText.getText(), nodeText.getText(), SearchDirection.BACKWARD);
                    if (returnedIndex != -1) {
                        if (indexWithinTrace != returnedIndex) {
                            indexWithinTrace = returnedIndex;
                            refreshView();
                        }
                        if (actorCheck.isSelected()) {
                            final List<Range> actorRanges = getSearchStringRanges(currentCir, currentCir.classMethodActor().format("%H.%n(%p)"), actorText.getText(), false);
                            final String newLabelText = getHighlightedLabelText(currentCir.classMethodActor().format("%H.%n(%p)"), actorRanges);
                            headerPanel.classMethodActorLabel.setText(newLabelText);
                        }
                        if (nodeCheck.isSelected()) {
                            final List<Range> leftNodeRanges = getSearchStringRanges(currentCir, currentCir.trace(), nodeText.getText(), true);
                            showSearchResults(leftTracePane, leftNodeRanges);
                            if (currentCir2 != null) {
                                final List<Range> rightNodeRanges = getSearchStringRanges(currentCir2, currentCir2.trace(), nodeText.getText(), true);
                                showSearchResults(rightTracePane, rightNodeRanges);
                            }
                        }
                    }
                }
            });
            prevButton.setEnabled(false);
            nextButton =  new JButton(new AbstractAction("Next") {
                public void actionPerformed(ActionEvent event) {
                    final int returnedIndex = getNextSearch(actorText.getText(), nodeText.getText(), SearchDirection.FORWARD);
                    if (returnedIndex != -1) {
                        if (indexWithinTrace != returnedIndex) {
                            indexWithinTrace = returnedIndex;
                            refreshView();
                        }
                        if (actorCheck.isSelected()) {
                            final List<Range> actorRanges = getSearchStringRanges(currentCir, currentCir.classMethodActor().format("%H.%n(%p)"), actorText.getText(), false);
                            final String newLabelText = getHighlightedLabelText(currentCir.classMethodActor().format("%H.%n(%p)"), actorRanges);
                            headerPanel.classMethodActorLabel.setText(newLabelText);
                        }
                        if (nodeCheck.isSelected()) {
                            final List<Range> leftNodeRanges = getSearchStringRanges(currentCir, currentCir.trace(), nodeText.getText(), true);
                            showSearchResults(leftTracePane, leftNodeRanges);
                            if (currentCir2 != null) {
                                final List<Range> rightNodeRanges = getSearchStringRanges(currentCir2, currentCir2.trace(), nodeText.getText(), true);
                                showSearchResults(rightTracePane, rightNodeRanges);
                            }
                        }
                    }
                }
            });
            nextButton.setEnabled(false);

            add(actorCheck);
            add(actorText);
            add(nodeCheck);
            add(nodeText);
            add(nextButton);
            add(prevButton);
            add(regexCheck);
            add(matchCaseCheck);
            add(wrapSearch);
            actorText.addKeyListener(new TextListener());
            nodeText.addKeyListener(new TextListener());

            setBorder(BorderFactory.createEtchedBorder());
        }
    }

    private static final String NO_TRACE_TITLE = "<html><b>Compiling/interpreting:</b> <i>none</i> <br><b>Description:</b> <i>none</i></html>";

    private CirAnnotatedTrace currentCir;
    private CirAnnotatedTrace currentCir2;
    private final CirTracePane leftTracePane = new CirTracePane(new CirStyledDocument(null));
    private final CirTracePane rightTracePane = new CirTracePane(new CirStyledDocument(null));

    private final Map<ClassMethodActor, List<CirAnnotatedTrace>> traceMap = new HashMap<ClassMethodActor, List<CirAnnotatedTrace>>();

    private List<CirAnnotatedTrace> allTraces = new ArrayList<CirAnnotatedTrace>();

    private List<List<CirAnnotatedTrace>> allTraceLists = new ArrayList<List<CirAnnotatedTrace>>();
    private int indexWithinTrace = -1;
    private int currentTraceListIndex = -1;

    private final JSplitPane splitPane;
    private Highlighter traceHighlighter;

    private final JEditorPane detailPane;
    private final JFrame frame;

    private final JSlider fontSizeSlider;
    private boolean fontChangeFlag;

    private Element selectedElement;
    private Highlighter.HighlightPainter selectedElementPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private Highlighter.HighlightPainter searchPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);

    private final JCheckBox enableFilter;
    private final JTextField filter;

    private final HeaderPanel headerPanel;
    private final TracePanel leftTrace;
    private final TracePanel rightTrace;
    private final NavigationPanel traceNavigationPanel;
    private final NavigationPanel traceListNavigationPanel;
    private SearchPanel searchPanel;
    private CirStyledDocument currentDocument;

    //if true then search from next/previous trace else start from current trace.
    private boolean isCurrentHighlighted;

    private List<CirAnnotatedTrace> currentTraceList() {
        if (currentTraceListIndex >= 0) {
            return allTraceLists.get(currentTraceListIndex);
        }
        return null;
    }

    private CirAnnotatedTrace currentTrace() {
        final List<CirAnnotatedTrace> traceList = currentTraceList();
        if (traceList != null && indexWithinTrace >= 0) {
            return traceList.get(indexWithinTrace);
        }
        return null;
    }

    private CirAnnotatedTrace nextTrace() {
        final List<CirAnnotatedTrace> traceList = currentTraceList();
        if (traceList != null && indexWithinTrace < traceList.size() - 1) {
            return traceList.get(indexWithinTrace + 1);
        }
        return null;
    }

    /**
     * The text pane used to display the CIR trace does not do auto line wrapping.
     */
    static class CirTracePane extends JTextPane {

        public CirTracePane(CirStyledDocument document) {
            super(document);
            setEditable(false);
        }

        @Override
        public void setSize(Dimension d) {
            if (d.width < getParent().getSize().width) {
                d.width = getParent().getSize().width;
            }
            super.setSize(d);
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        public CirStyledDocument cirDocument() {
            return (CirStyledDocument) super.getDocument();
        }
    }

    private CirTraceVisualizer(JFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        final JPanel filterPanel = new JPanel(new SpringLayout());
        final JLabel filterLabel = new JLabel("Filter");
        enableFilter = new JCheckBox();
        filterPanel.setToolTipText("Only show traces whose title contains this string");
        enableFilter.setSelected(false);
        filter = new JTextField("");
        filter.setEnabled(false);
        filterLabel.setEnabled(false);
        filterPanel.add(enableFilter);
        filterPanel.add(filterLabel);
        filterPanel.add(filter);
        enableFilter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                filter.setEnabled(enableFilter.isSelected());
                filterLabel.setEnabled(enableFilter.isSelected());
            }
        });
        SpringUtilities.makeCompactGrid(filterPanel, 3);
        add(filterPanel, BorderLayout.NORTH);

        leftTrace = new TracePanel(leftTracePane);
        rightTrace = new TracePanel(rightTracePane);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTrace, rightTrace);

        // Font size slider
        final JPanel fontSizeSliderPanel = new JPanel();

        fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 40, DEFAULT_FONT_SIZE);
        fontSizeSlider.setMajorTickSpacing(8);
        fontSizeSlider.setMinorTickSpacing(4);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                fontChangeFlag = true;
                refreshView();
                fontChangeFlag = false;
            }
        });

        fontSizeSliderPanel.add(new JLabel("Font Size"));
        fontSizeSliderPanel.add(fontSizeSlider);

        final JPanel tracePanel = new JPanel(new BorderLayout());
        headerPanel = new HeaderPanel();
        tracePanel.add(headerPanel, BorderLayout.NORTH);
        tracePanel.add(splitPane, BorderLayout.CENTER);
        splitPane.setDividerLocation(.5d);

        // Detail panel
        detailPane = new JEditorPane("text/html", null);
        detailPane.setEditable(false);
        final JScrollPane detailView = new JScrollPane(detailPane);

        traceAndDetailView = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tracePanel, detailView);
        add(traceAndDetailView, BorderLayout.CENTER);

        traceNavigationPanel = new TraceNavigationPanel();
        traceListNavigationPanel = new TraceListNavigationPanel();
        searchPanel = new SearchPanel();

        final JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(searchPanel);

        final JCheckBox findCheck = new JCheckBox("Find");
        findCheck.setSelected(false);
        findCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                searchPanel.setVisible(findCheck.isSelected());
                searchPanel.nodeCheck.setSelected(false);
                clearSearchHighlights();
            }
        });

        final JPanel westPanel = new JPanel();
        westPanel.add(findCheck);
        westPanel.add(fontSizeSliderPanel);

        final JPanel p = new JPanel(new BorderLayout());
        p.add(westPanel, BorderLayout.WEST);
        p.add(traceListNavigationPanel, BorderLayout.CENTER);
        p.add(traceNavigationPanel, BorderLayout.EAST);

        southPanel.add(p);

        searchPanel.setVisible(false);
        add(southPanel, BorderLayout.SOUTH);

        traceHighlighter = leftTracePane.getHighlighter();

        final MouseInputAdapter mouseInputAdapter = new MouseInputAdapter() {
            private ParenthesisElement dualRangeElement;
            private String  hiddenTrace = new String();
            private String  hiddenTrace2 = new String();
            private IntHashMap<Element> oldOffsetToElement;
            private IntHashMap<Element> oldOffsetToElement2;
            private IntHashMap<Element> tempOffsetToElement;
            private IntHashMap<Element> newOffsetToElement;
            private int oldNoOffsets;
            private ListBag<CirNode, CirAnnotatedTrace.Element> oldElementsPerNode;
            private ListBag<CirNode, CirAnnotatedTrace.Element> oldElementsPerNode2;
            private ListBag<CirNode, CirAnnotatedTrace.Element> newElementsPerNode;
            private Element currentOccurrenceElement;
            private Highlighter.HighlightPainter secondaryHighlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(leftTracePane.getSelectionColor().darker());
            private final ArrayList<Object> occurrenceHighlights = new ArrayList<Object>();

            /**
             * Clears all highlights marking the occurrence(s) of the CIR node last denoted by a mouse movement.
             */
            private void clearOccurrenceHighlights() {
                for (Object highlight : occurrenceHighlights) {
                    traceHighlighter.removeHighlight(highlight);
                }
                occurrenceHighlights.clear();
            }

            private CirTracePane tracePane(MouseEvent event) {
                if (event.getSource() == leftTracePane) {
                    return leftTracePane;
                } else if (event.getSource() == rightTracePane) {
                    return rightTracePane;
                }
                return null;
            }

            /**
             * Gets the element pointed to by the mouse pointer position in a mouse event.
             */
            private Element element(MouseEvent event) {
                final CirTracePane tracePane = tracePane(event);
                if (tracePane != null) {
                    final int offset = tracePane.viewToModel(event.getPoint());
                    if (offset != -1) {
                        return tracePane.cirDocument().elementAt(offset);
                    }
                }
                return null;
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                clearOccurrenceHighlights();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                final Element element = element(event);
                if (element != currentOccurrenceElement) {
                    CirStyledDocument document = null;
                    if (event.getSource() == leftTracePane) {
                        document = leftTracePane.cirDocument();
                        traceHighlighter = leftTracePane.getHighlighter();
                    } else if (event.getSource() == rightTracePane) {
                        document = rightTracePane.cirDocument();
                        traceHighlighter = rightTracePane.getHighlighter();
                    }
                    clearOccurrenceHighlights();
                    if (element != null) {
                        element.visitAssociatedRanges(new RangeVisitor() {
                            public void visitRange(Range range) {
                                try {
                                    occurrenceHighlights.add(traceHighlighter.addHighlight(range.start(), range.end(), secondaryHighlighterPainter));
                                    //System.err.printAddress("secondary range: " + range);
                                } catch (BadLocationException badLocationException) {
                                    ProgramWarning.message("error highlighting element range " + range);
                                }
                            }
                        });
                        for (Element occurrence : document.occurrences(element)) {
                            occurrence.visitRanges(new RangeVisitor() {
                                public void visitRange(Range range) {
                                    try {
                                        occurrenceHighlights.add(traceHighlighter.addHighlight(range.start(), range.end(), DefaultHighlighter.DefaultPainter));
                                        //System.err.printAddress("primary range: " + range);
                                    } catch (BadLocationException badLocationException) {
                                        ProgramWarning.message("error highlighting element range " + range);
                                    }
                                }
                            });
                        }
                    }
                    currentOccurrenceElement = element;
                    currentDocument = document;
                }
            }
            int foldStart = -1;
            int foldEnd = -1;

            @Override
            public void mouseReleased(MouseEvent event) {
                final Element element = element(event);
                if (event.getSource() == leftTracePane) {
                    traceHighlighter = leftTracePane.getHighlighter();
                } else if (event.getSource() == rightTracePane) {
                    traceHighlighter = rightTracePane.getHighlighter();
                }
                if (element instanceof ParenthesisElement) {
                    dualRangeElement = (ParenthesisElement) element;
                }
                if (element != selectedElement || element instanceof ParenthesisElement) {
                    // Remove all highlights
                    leftTracePane.getHighlighter().removeAllHighlights();
                    rightTracePane.getHighlighter().removeAllHighlights();
                    if (element != null) {
                        final CirNode node = element.node();
                        if (node != null) {
                            element.visitRanges(new RangeVisitor() {
                                public void visitRange(Range range) {
                                    try {
                                        traceHighlighter.addHighlight(range.start(), range.end(), selectedElementPainter);
                                    } catch (BadLocationException badLocationException) {
                                        ProgramWarning.message("error highlighting element range " + range);
                                    }
                                }
                            });
                        } else {
                            if (currentDocument.collapsedOffset == -1) {
                                currentDocument.collapsedDual = dualRangeElement;
                                newOffsetToElement = new IntHashMap<Element>();
                                newElementsPerNode = new ListBag<CirNode, Element>(ListBag.MapType.IDENTITY);
                                foldStart = ((ParenthesisElement) element).firstRange().start();
                                foldEnd = ((ParenthesisElement) element).secondRange().start();
                                oldNoOffsets = currentDocument.getLength();
                                if (event.getSource() == leftTracePane) {
                                    oldOffsetToElement = currentDocument.offsetToElement;
                                    tempOffsetToElement = oldOffsetToElement;
                                    oldElementsPerNode = currentDocument.elementsPerNode;
                                    try {
                                        hiddenTrace = currentDocument.getText(dualRangeElement.firstRange().start() + 1, dualRangeElement.secondRange().start() - dualRangeElement.firstRange().start() - 1);
                                    } catch (BadLocationException ble) {
                                        System.err.println("Bad Location while folding trace in the left pane");
                                    }
                                } else if (event.getSource() == rightTracePane) {
                                    oldOffsetToElement2 = currentDocument.offsetToElement;
                                    tempOffsetToElement = oldOffsetToElement2;
                                    oldElementsPerNode2 = currentDocument.elementsPerNode;
                                    try {
                                        hiddenTrace2 = currentDocument.getText(dualRangeElement.firstRange().start() + 1, dualRangeElement.secondRange().start() - dualRangeElement.firstRange().start() - 1);
                                    } catch (BadLocationException ble) {
                                        System.err.println("Bad Location while folding trace in the right pane");
                                    }
                                }
                                currentDocument.collapsedOffset = foldStart;
                                int newMapIndex = 0;
                                int flag = 0;
                                SimpleElement currSimple = null;
                                ParenthesisElement currDual = null;
                                for (int i = 0; i <= oldNoOffsets; i++) {
                                    if (i > foldStart && i < foldEnd) {
                                        if (flag == 0) {
                                            newMapIndex = newMapIndex + 3;
                                            flag = 1;
                                        }
                                        continue;
                                    }
                                    if (tempOffsetToElement.get(i) == null) {
                                        newMapIndex++;
                                    }

                                    if (i <= foldStart) {
                                        if (tempOffsetToElement.get(i) instanceof SimpleElement) {
                                            final Range simpleRange = new Range(((SimpleElement) tempOffsetToElement.get(i)).range().start(),
                                                                            ((SimpleElement) tempOffsetToElement.get(i)).range().end());
                                            currSimple = new SimpleElement(tempOffsetToElement.get(i).node(), simpleRange);
                                            newOffsetToElement.put(newMapIndex++, currSimple);
                                            newElementsPerNode.add(tempOffsetToElement.get(i).node(), currSimple);
                                        }
                                        if (tempOffsetToElement.get(i) instanceof ParenthesisElement) {
                                            final Range firstRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).firstRange().start());
                                            Range secondRange = null;
                                            if (((ParenthesisElement) tempOffsetToElement.get(i)).secondRange().start() < foldStart) {
                                                secondRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).secondRange().start());
                                            }
                                            if (((ParenthesisElement) tempOffsetToElement.get(i)).secondRange().start() >= foldEnd) {
                                                secondRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).secondRange().start() - (foldEnd - foldStart - 4));
                                            }

                                            currDual = new ParenthesisElement(firstRange, secondRange);
                                            newOffsetToElement.put(newMapIndex++, currDual);
                                        }
                                    }
                                    if (i >= foldEnd) {
                                        if (tempOffsetToElement.get(i) instanceof SimpleElement) {
                                            final Range simpleRange = new Range(((SimpleElement) tempOffsetToElement.get(i)).range().start() - (foldEnd - foldStart - 4),
                                                                            ((SimpleElement) tempOffsetToElement.get(i)).range().end());
                                            currSimple = new SimpleElement(tempOffsetToElement.get(i).node(), simpleRange);

                                            newOffsetToElement.put(newMapIndex++, currSimple);
                                            newElementsPerNode.add(tempOffsetToElement.get(i).node(), currSimple);
                                        }
                                        if (tempOffsetToElement.get(i) instanceof ParenthesisElement) {
                                            Range firstRange = null;
                                            final Range secondRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).secondRange().start() - (foldEnd - foldStart - 4));
                                            if (((ParenthesisElement) tempOffsetToElement.get(i)).firstRange().start() <= foldStart) {
                                                firstRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).firstRange().start());
                                            }
                                            if (((ParenthesisElement) tempOffsetToElement.get(i)).firstRange().start() > foldEnd) {
                                                firstRange = new Range(((ParenthesisElement) tempOffsetToElement.get(i)).firstRange().start() - (foldEnd - foldStart - 4));
                                            }

                                            currDual = new ParenthesisElement(firstRange, secondRange);
                                            newOffsetToElement.put(newMapIndex++, currDual);
                                        }
                                    }
                                }
                                try {
                                    currentDocument.remove(dualRangeElement.firstRange().start() + 1, dualRangeElement.secondRange().start() - dualRangeElement.firstRange().start() - 1);
                                    currentDocument.insertString(dualRangeElement.firstRange().start() + 1, "...", null);
                                } catch (BadLocationException ble) {
                                    System.err.println("Bad Location while folding trace");
                                }

                                final Style style = currentDocument.addStyle(null, NORMAL);
                                StyleConstants.setFontSize(style, fontSizeSlider.getValue());
                                currentDocument.setCharacterAttributes(dualRangeElement.firstRange().start() + 1, 3, style, true);

                                currentDocument.offsetToElement = newOffsetToElement;
                                currentDocument.elementsPerNode = newElementsPerNode;
                            } else if (dualRangeElement.firstRange().start() == currentDocument.collapsedOffset) {
                                try {
                                    currentDocument.remove(dualRangeElement.firstRange().start() + 1, 3);
                                    if (event.getSource() == leftTracePane) {
                                        currentDocument.insertString(dualRangeElement.firstRange().start() + 1, hiddenTrace, null);
                                        currentDocument.offsetToElement = oldOffsetToElement;
                                        currentDocument.elementsPerNode = oldElementsPerNode;
                                    } else if (event.getSource() == rightTracePane) {
                                        currentDocument.insertString(dualRangeElement.firstRange().start() + 1, hiddenTrace2, null);
                                        currentDocument.offsetToElement = oldOffsetToElement2;
                                        currentDocument.elementsPerNode = oldElementsPerNode2;
                                    }
                                } catch (BadLocationException ble) {
                                    System.err.println("Bad Location while expanding trace");
                                }
                                currentDocument.collapsedOffset = -1;
                                currentDocument.collapsedDual = null;

                                final Style style = currentDocument.addStyle(null, NORMAL);
                                StyleConstants.setFontSize(style, fontSizeSlider.getValue());
                                final ParenthesisElement newDual = (ParenthesisElement) currentDocument.offsetToElement.get(dualRangeElement.firstRange().start());

                                currentDocument.setCharacterAttributes(
                                    newDual.firstRange().start() + 1,
                                    newDual.secondRange().start() - newDual.firstRange().start(),
                                    style, true);
                            }
                        }
                    }
                    selectedElement = element;
                    refreshView();
                }
            }
        };
        leftTracePane.addMouseListener(mouseInputAdapter);
        leftTracePane.addMouseMotionListener(mouseInputAdapter);
        rightTracePane.addMouseListener(mouseInputAdapter);
        rightTracePane.addMouseMotionListener(mouseInputAdapter);
    }
    private static final StyleContext STYLE_CONTEXT;
    private static final Style NORMAL;
    private static final Style DIFF_DELETION;
    private static final Style DIFF_INSERTION;
    private JSplitPane traceAndDetailView;

    private List<SimpleElement> leftElements;
    private List<SimpleElement> rightElements;
    private Diff currentDiffs;

    static {
        STYLE_CONTEXT = new StyleContext();
        final Style def = STYLE_CONTEXT.getStyle(StyleContext.DEFAULT_STYLE);

        NORMAL = STYLE_CONTEXT.addStyle("normal", def);
        StyleConstants.setFontFamily(NORMAL, "Monospaced");

        DIFF_DELETION = STYLE_CONTEXT.addStyle("diff_deletion", NORMAL);
        StyleConstants.setForeground(DIFF_DELETION, Color.RED);

        DIFF_INSERTION = STYLE_CONTEXT.addStyle("diff_insertion", NORMAL);
        StyleConstants.setForeground(DIFF_INSERTION, Color.GREEN.darker());
    }

    /**
     * Create the GUI and show it.  For thread safety, this method should be invoked from the event dispatch thread.
     */
    public static CirTraceVisualizer createAndShowGUI() {
        //Create and set up the window.
        final JFrame frame = new JFrame("CIR Visualizer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Add content to the window.
        final CirTraceVisualizer visualizer = new CirTraceVisualizer(frame);
        frame.add(visualizer);

        //Display the window.
        frame.setBounds(DEFAULT_FRAME_BOUNDS);
        frame.pack();
        frame.setVisible(true);

        if (!visualizer.loadSettings()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    visualizer.traceAndDetailView.setDividerLocation(0.8d);
                    visualizer.splitPane.setDividerLocation(0.5d);
                }
            });
        }

        // Show a dialog to allow the user to enter/modify the trace filter before any traces are sent to the visualizer
        final String s = (String) JOptionPane.showInputDialog(frame,
                        "Enter substring for filtering trace of interest to methods whose fully qualified name contains the substring", "Trace Filter Wizard", JOptionPane.QUESTION_MESSAGE, null, null, visualizer.filter.getText());
        if (s != null) {
            visualizer.filter.setText(s);
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                visualizer.saveSettings();
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                visualizer.saveSettings();
            }
        });

        return visualizer;
    }

    public void saveSettings() {
        final Properties settings = new Properties();
        final Rectangle bounds = frame.getBounds();
        settings.setProperty("window.x", String.valueOf(bounds.x));
        settings.setProperty("window.y", String.valueOf(bounds.y));
        settings.setProperty("window.width", String.valueOf(bounds.width));
        settings.setProperty("window.height", String.valueOf(bounds.height));
        settings.setProperty("font.size.slider", String.valueOf(fontSizeSlider.getValue()));
        settings.setProperty("filter.text", filter.getText());
        settings.setProperty("filter.enabled", String.valueOf(filter.isEnabled()));
        settings.setProperty("traceSplitPane.dividerLocation", String.valueOf(traceAndDetailView.getDividerLocation()));
        settings.setProperty("splitPane.dividerLocation", String.valueOf(splitPane.getDividerLocation()));
        final File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE_NAME);

        try {
            final OutputStream settingsStream = new FileOutputStream(settingsFile);
            settings.store(settingsStream, null);
            settingsStream.close();
        } catch (IOException ioException) {
            ProgramWarning.message("could not save CIR visualizer settings to " + settingsFile + ": " + ioException);
        }
    }

    public boolean loadSettings() {
        final Properties settings = new Properties();
        final File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE_NAME);

        if (settingsFile.exists()) {
            try {
                final InputStream settingsStream = new FileInputStream(settingsFile);
                settings.load(settingsStream);
                settingsStream.close();
            } catch (IOException ioException) {
                ProgramWarning.message("could not load CIR visualizer settings from " + settingsFile + ": " + ioException);
                return false;
            }
            final Rectangle bounds = frame.getBounds();
            bounds.x = Integer.parseInt(settings.getProperty("window.x", "100"));
            bounds.y = Integer.parseInt(settings.getProperty("window.y", "100"));
            bounds.width = Integer.parseInt(settings.getProperty("window.width", "400"));
            bounds.height = Integer.parseInt(settings.getProperty("window.height", "200"));
            frame.setBounds(bounds);
            fontSizeSlider.setValue(Integer.parseInt(settings.getProperty("font.size.slider", String.valueOf(DEFAULT_FONT_SIZE))));
            filter.setText(settings.getProperty("filter.text", ""));
            enableFilter.setSelected(Boolean.valueOf(settings.getProperty("filter.enabled", String.valueOf(false))));
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    traceAndDetailView.setDividerLocation(Integer.parseInt(settings.getProperty("traceSplitPane.dividerLocation", "0")));
                    splitPane.setDividerLocation(Integer.parseInt(settings.getProperty("splitPane.dividerLocation", "0")));
                }
            });
            return true;
        }
        return false;
    }

    synchronized void refreshView() {
        final List<CirAnnotatedTrace> currentTraceList = currentTraceList();
        if (currentTraceList == null) {
            return;
        }
        CirStyledDocument document = null;
        CirStyledDocument document2 = null;
        refreshNavigation();
        final CirAnnotatedTrace oldCir = currentCir;
        currentCir = currentTrace();
        currentCir2 = nextTrace();
        document = new CirStyledDocument(currentCir);
        final int numTraces = currentTraceList.size();
        if (currentCir2 != null) {
            document2 = new CirStyledDocument(currentCir2);
        }

        if (numTraces == 2) {
            final Style style = document.addStyle(null, NORMAL);
            StyleConstants.setFontSize(style, fontSizeSlider.getValue());
            try {
                document.insertString(0, currentCir.trace(), style);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }

            final Style style2 = document2.addStyle(null, NORMAL);
            StyleConstants.setFontSize(style2, fontSizeSlider.getValue());
            try {
                document2.insertString(0, currentCir2.trace(), style2);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }
            leftTracePane.setDocument(document);
            rightTracePane.setDocument(document2);
            headerPanel.update(currentCir, currentCir2);
            leftTrace.update(currentCir);
            rightTrace.update(currentCir2);
            findDiffs(document, document2);
        }

        if (oldCir != currentCir) {
            try {
                final Style style = document.addStyle(null, NORMAL);
                StyleConstants.setFontSize(style, fontSizeSlider.getValue());
                document.insertString(0, currentCir.trace(), style);
                leftTracePane.setDocument(document);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }

            if (numTraces >= 2) {
                if (document2 != null) {
                    try {
                        final Style style2 = document2.addStyle(null, NORMAL);
                        StyleConstants.setFontSize(style2, fontSizeSlider.getValue());
                        document2.insertString(0, currentCir2.trace(), style2);
                        rightTracePane.setDocument(document2);
                    } catch (BadLocationException e) {
                        ProgramWarning.message("could not update CIR visualizer: " + e);
                        return;
                    }
                    splitPane.setDividerLocation(.5d);
                    if (numTraces >= 2) {
                        findDiffs(document, document2);
                    }
                } else {
                    //Display only left
                    rightTracePane.setText("");
                    splitPane.setDividerLocation(.99d);
                }
                selectedElement = null;
                traceHighlighter.removeAllHighlights();
                headerPanel.update(currentCir, currentCir2);
                leftTrace.update(currentCir);
                rightTrace.update(currentCir2);
            }
        } else {
            if (fontChangeFlag) {
                document = leftTracePane.cirDocument();
                final Style style = document.addStyle(null, NORMAL);
                StyleConstants.setFontSize(style, fontSizeSlider.getValue());
                document.setCharacterAttributes(0, document.getLength(), style, true);

                if (numTraces >= 2) {
                    document2 = rightTracePane.cirDocument();
                    final Style style2 = document2.addStyle(null, NORMAL);
                    StyleConstants.setFontSize(style2, fontSizeSlider.getValue());
                    document2.setCharacterAttributes(0, document2.getLength(), style2, true);

                    final Diff diff = currentDiffs;
                    showDiffs(document, leftElements, diff.deletions(), DIFF_DELETION);
                    showDiffs(document2, rightElements, diff.insertions(), DIFF_INSERTION);
                }
            }
        }

        refreshNavigation();

        if (selectedElement == null) {
            detailPane.setText(NO_SELECTED_NODE_DETAIL);
        } else if (selectedElement.node() == null) {
            detailPane.setText(NO_SELECTED_NODE_DETAIL);
            if (currentDocument.collapsedOffset == -1) {
                if (numTraces >= 2) {
                    document = leftTracePane.cirDocument();
                    document2 = rightTracePane.cirDocument();
                    final Diff diff = currentDiffs;
                    showDiffs(document, leftElements, diff.deletions(), DIFF_DELETION);
                    showDiffs(document2, rightElements, diff.insertions(), DIFF_INSERTION);
                }
            }
        } else {
            final CirNode node = selectedElement.node();
            final StringBuilder sb = new StringBuilder("<html>").
                append("<table border=\"1\">").
                append("<tr><td>Type</td><td>" + node.getClass().getSimpleName() + "</td></tr>").
                append("<tr><td>ID</td><td>" + node.id() + "</td></tr>");
            node.acceptVisitor(new CirVisitor() {
                @Override
                public void visitConstant(CirConstant constant) {
                    final Value value = constant.value();
                    sb.append("<tr><td>Kind</td><td>" + value.kind() + "</td></tr>");
                    if (value.kind().isReference) {
                        final Object object = value.asObject();
                        sb.append("<tr><td>toString</td><td>" + object + "</td></tr>");
                        if (object != null) {
                            sb.append("<tr><td>Class</td><td>" + object.getClass().getName() + "</td></tr>");
                        }
                    }
                }

                @Override
                public void visitMethod(CirMethod method) {
                    sb.append("<tr><td>Signature</td><td>" + method.classMethodActor().format("%r %H.%n(%p)") + "</td></tr>");
                }
            });
            sb.append("</table></html>");
            detailPane.setText(sb.toString());
        }
    }

    private void refreshNavigation() {
        traceNavigationPanel.update();
        traceListNavigationPanel.update();
    }

    public boolean shouldBeTraced(MethodActor classMethodActor) {
        if (filter.isEnabled()) {
            if (classMethodActor != null) {
                return classMethodActor.format("%H.%n(%p)").contains(filter.getText());

            }
            return false;
        }
        return true;
    }

    public synchronized void add(CirAnnotatedTrace cirAnnotatedTrace) {
        allTraces.add(cirAnnotatedTrace);
        List<CirAnnotatedTrace> traceList = traceMap.get(cirAnnotatedTrace.classMethodActor());
        if (traceList == null) {
            traceList = new ArrayList<CirAnnotatedTrace>();
            traceMap.put(cirAnnotatedTrace.classMethodActor(), traceList);
            allTraceLists.add(traceList);
            if (currentTraceListIndex < 0) {
                currentTraceListIndex = 0;
            }
        }
        traceList.add(cirAnnotatedTrace);
        if (indexWithinTrace < 0 || traceList.size() == 2) {
            indexWithinTrace = 0;
            refreshView();
        } else {
            refreshNavigation();
        }
    }

    private List<SimpleElement> getSimpleElements(CirStyledDocument document) {

        int len = 0;
        final List<SimpleElement> seq = new ArrayList<SimpleElement>();

        while (len <= document.getLength()) {
            if (document.offsetToElement.get(len) instanceof SimpleElement) {
                seq.add((SimpleElement) document.offsetToElement.get(len));
                len += ((SimpleElement) document.offsetToElement.get(len)).range().length();
            } else {
                len++;
            }
        }
        return seq;
    }

    private void findDiffs(CirStyledDocument leftDocument, CirStyledDocument rightDocument) {

        final List<SimpleElement> lElements = getSimpleElements(leftDocument);
        final List<SimpleElement> rElements = getSimpleElements(rightDocument);

        this.leftElements = lElements;
        this.rightElements = rElements;

        final Diff.Equality equality = new Diff.Equality() {
            public boolean test(Object object1, Object object2) {
                final SimpleElement element1 = (SimpleElement) object1;
                final SimpleElement element2 = (SimpleElement) object2;
                return element1.node() == element2.node();
            }

        };
        final Diff diff = new Diff(lElements.toArray(new SimpleElement[lElements.size()]),
                                  rElements.toArray(new SimpleElement[rElements.size()]),
                                  equality);
        currentDiffs = diff;

        showDiffs(leftDocument, lElements, diff.deletions(), DIFF_DELETION);
        showDiffs(rightDocument, rElements, diff.insertions(), DIFF_INSERTION);
    }

    private void showDiffs(CirStyledDocument document, List<SimpleElement> elements, List<Range> changes, Style changeStyle) {
        int shift = 0;
        if (document.collapsedOffset != -1) {
            shift = document.collapsedDual.secondRange().start() - document.collapsedDual.firstRange().start() - 4;
        }
        StyleConstants.setFontSize(changeStyle, fontSizeSlider.getValue());
        for (Range deletion : changes) {
            for (int k = deletion.start(); k < deletion.end(); k++) {
                if (document.collapsedOffset != -1) {
                    if (elements.get(k).range().start() < document.collapsedDual.firstRange().start()) {
                        document.setCharacterAttributes(elements.get(k).range().start(), (int) elements.get(k).range().length(), changeStyle, true);
                    } else if (elements.get(k).range().start() > document.collapsedDual.secondRange().start()) {
                        document.setCharacterAttributes(elements.get(k).range().start() - shift, (int) elements.get(k).range().length(), changeStyle, true);
                    }
                } else {
                    document.setCharacterAttributes(elements.get(k).range().start(), (int) elements.get(k).range().length(), changeStyle, true);
                }
            }
        }
    }

    enum SearchDirection {
        BACKWARD {
            @Override
            int nextIndex(int index, int length, int startIndex, boolean wrap) {
                final int nextIndex = wrap ? (length - 1 + index - 1) % (length - 1) : index - 1;
                if (wrap && nextIndex == startIndex) {
                    return -1;
                }
                return nextIndex;
            }

        },
        FORWARD {
            @Override
            int nextIndex(int index, int length, int startIndex, boolean wrap) {
                final int nextIndex = wrap ? (index + 1) % (length - 1) : index + 1;
                if (wrap && nextIndex == startIndex) {
                    return -1;
                }
                return nextIndex;
            }

        };

        abstract int nextIndex(int index, int length, int startIndex, boolean wrap);
    }

    /**
     * Gets the index of next trace in selected direction which satisfies search criteria.
     *
     * @param actor term to search in method signatures.
     * @param node term to search in traces.
     * @param direction backward or forward.
     * @return index of the first trace that satisfies criteria.
     */
    private int getNextSearch(String actor, String node, SearchDirection direction) {
        final boolean inActors = searchPanel.actorCheck.isSelected();
        final boolean inNodes = searchPanel.nodeCheck.isSelected();
        final boolean currentHighlighted = this.isCurrentHighlighted;
        final boolean isWrapSearch = searchPanel.wrapSearch.isSelected();
        final List<CirAnnotatedTrace> traces = allTraces;
        final int lastIndex = traces.size() - 1;

        //decide from which index to search
        final int startIndex;
        if (!currentHighlighted) {
            startIndex = indexWithinTrace;
        } else {
            if (direction == SearchDirection.FORWARD) {
                startIndex = isWrapSearch ? (indexWithinTrace + 1) % lastIndex : indexWithinTrace + 1;
            } else {
                startIndex = isWrapSearch ? (lastIndex + indexWithinTrace - 1) % lastIndex : indexWithinTrace - 1;
            }
        }

        int index = startIndex;

        while (index > -1 && index <= lastIndex) {
            final CirAnnotatedTrace trace = traces.get(index);
            boolean result = true;
            if (inActors) {
                result = result && !getSearchStringRanges(traces.get(index), trace.classMethodActor().format("%H.%n(%p)"), actor, false).isEmpty();
            }
            if (inNodes) {
                result = result && !getSearchStringRanges(traces.get(index), trace.trace(), node, true).isEmpty();
            }
            if (result) {
                return index;
            }
            index = direction.nextIndex(index, traces.size(), startIndex, isWrapSearch);
            if (index == -1 || index == traces.size() - 1) {
                break;
            }
        }
        JOptionPane.showMessageDialog(this, "Search term not found");
        return -1;
    }

    /**
     * Gets ranges in search-text which match search-term depending upon regular expression and match case options.
     *
     * @param trace current Cir trace.
     * @param searchIn String to search in.
     * @param lookFor String to search for.
     * @param inTrace boolean which tells if current search is in a signature or a trace.
     * @return sequence of matching ranges.
     */
    private List<Range> getSearchStringRanges(CirAnnotatedTrace trace, String searchIn, String lookFor, boolean inTrace) {
        final boolean ignoreCase = !searchPanel.matchCaseCheck.isSelected();
        final boolean regex = searchPanel.regexCheck.isSelected();

        final List<Range> ranges = new ArrayList<Range>();
        int rangeStart = 0;
        if (!regex) {
            while (rangeStart <= (searchIn.length() - lookFor.length())) {
                if (searchIn.regionMatches(ignoreCase, rangeStart, lookFor, 0, lookFor.length())) {
                    final Range range = new Range(rangeStart, rangeStart + lookFor.length());
                    ranges.add(range);
                }
                rangeStart++;
            }
            return ranges;
        }

        //regex is true, so search for regex instead of normal string.
        Pattern pattern;
        if (ignoreCase) {
            pattern = Pattern.compile(".*(" + lookFor + ").*", Pattern.CASE_INSENSITIVE);
        } else {
            pattern = Pattern.compile(".*(" + lookFor + ").*");
        }

        //search for pattern in class method actor
        if (!inTrace) {
            final Matcher matcher = pattern.matcher(searchIn);
            if (matcher.matches()) {
                final int start = matcher.start(1);
                final Range range = new Range(start, start + matcher.group(1).length());
                ranges.add(range);
            }
            return ranges;
        }

        //Search for the regular expression in trace nodes.
        final CirStyledDocument document = new CirStyledDocument(trace);
        try {
            document.insertString(0, trace.trace(), null);
        } catch (BadLocationException ble) {
            ProgramWarning.message("error inserting trace in document");
        }
        final List<SimpleElement> simpleElements = getSimpleElements(document);

        for (int i = 0; i < simpleElements.size(); i++) {
            final String nodeText = searchIn.substring(simpleElements.get(i).range().start(), simpleElements.get(i).range().end());
            final Matcher matcher = pattern.matcher(nodeText);
            if (matcher.matches()) {
                final int start = simpleElements.get(i).range().start() + matcher.start(1);
                final Range range = new Range(start, start + matcher.group(1).length());
                ranges.add(range);
            }
        }
        return ranges;
    }

    /**
     * Highlights search matches in text pane.
     *
     * @param textPane
     * @param ranges
     */
    private void showSearchResults(JTextPane textPane, List<Range> ranges) {
        final Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
        for (Range range : ranges) {
            try {
                highlighter.addHighlight(range.start(), range.end(), searchPainter);
            } catch (BadLocationException ble) {
                ProgramWarning.message("error highlighting search result range " + range);
            }
            isCurrentHighlighted = true;
        }
    }

    /**
     * Highlights search matches in java method signature.
     *
     * @param currentText
     *            java method signature.
     * @param ranges
     *            matched text in signature.
     * @return html text for the signature.
     */
    private String getHighlightedLabelText(String currentText, List<Range> ranges) {
        final StringBuffer newText = new StringBuffer("<html>" + currentText + "</html>");
        final String startTag = "<font bgcolor=\"#00FFFF\">";
        final String endTag = "</font>";
        int count = 0;

        for (Range range : ranges) {
            newText.insert(31 * count + range.start() + 6, startTag);
            newText.insert(31 * count + range.end() + 30, endTag);
            count++;
        }
        isCurrentHighlighted = true;
        return newText.toString();
    }

    /**
     * Clears search highlights when search panel is closed.
     *
     */
    private void clearSearchHighlights() {
        isCurrentHighlighted = false;
        if (!searchPanel.nodeCheck.isSelected()) {
            leftTracePane.getHighlighter().removeAllHighlights();
            rightTracePane.getHighlighter().removeAllHighlights();
        }
        if (!searchPanel.actorCheck.isSelected()) {
            headerPanel.classMethodActorLabel.setText(headerPanel.oldClassMethodActor);
        }
    }
}
