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
package com.sun.max.vm.compiler.cir.gui;

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
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.gui.CirAnnotatedTrace.*;
import com.sun.max.vm.compiler.cir.gui.CirAnnotatedTrace.Element;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;
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
        private final JLabel _classMethodActor;
        //Saves old signature after we highlight search term in signature.
        private String _oldClassMethodActor;

        public HeaderPanel() {

            _classMethodActor = new JLabel("");
            _classMethodActor.setFont(new Font("Monospace", Font.PLAIN, 26));

            add(_classMethodActor);
        }

        void update(CirAnnotatedTrace cirAnnotatedTrace, CirAnnotatedTrace cirAnnotatedTrace2) {
            final MethodActor classMethodActor = cirAnnotatedTrace.classMethodActor();
            if (classMethodActor != null) {
                _classMethodActor.setText(classMethodActor.format("%H.%n(%p)"));
                _oldClassMethodActor = classMethodActor.format("%H.%n(%p)");
            } else {
                _classMethodActor.setText("");
            }
        }
    }

    abstract class NavigationPanel extends JPanel {
        private final JButton _previousButton;
        private final JFormattedTextField _current;
        private final JTextField _total;
        private final JButton _nextButton;
        private final String _units;

        NavigationPanel(String prevLabel, String nextLabel, String units) {
            super(new FlowLayout(FlowLayout.CENTER));
            _units = units;
            _previousButton = new JButton(new AbstractAction(prevLabel) {
                public void actionPerformed(ActionEvent event) {
                    final int currentIndex = getCurrentIndex();
                    if (currentIndex > 0) {
                        setCurrentIndex(currentIndex - 1);
                        refreshView();
                    }
                }
            });
            _nextButton = new JButton(new AbstractAction(nextLabel) {
                public void actionPerformed(ActionEvent event) {
                    final int currentIndex = getCurrentIndex();
                    if (currentIndex < getMaximumIndex() - 1) {
                        setCurrentIndex(currentIndex + 1);
                        refreshView();
                    }
                }
            });

            _current = new JFormattedTextField(NumberFormat.getNumberInstance()) {

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

            _current.setColumns(8);
            _current.setText("0");
            _current.setHorizontalAlignment(JTextField.RIGHT);

            _total = new JTextField("0 " + _units, 8);
            _total.setEditable(false);
            _total.setBorder(null);
            _total.setHorizontalAlignment(JTextField.LEFT);

            add(_previousButton);
            add(_current);
            add(new JLabel("of"));
            add(_total);
            add(_nextButton);
        }

        public void update() {
            final int currentIndex = getCurrentIndex();
            _previousButton.setEnabled(currentIndex > 0);
            _nextButton.setEnabled(currentIndex < getMaximumIndex() - 1);
            _current.setText("" + (currentIndex + 1));
            _total.setText(getMaximumIndex() + " " + _units);
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
            return _indexWithinTrace;
        }

        @Override
        protected void setCurrentIndex(int newIndex) {
            _indexWithinTrace = newIndex;
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
            return _currentTraceListIndex;
        }

        @Override
        protected void setCurrentIndex(int newIndex) {
            _currentTraceListIndex = newIndex;
            _indexWithinTrace = 0;
        }

        @Override
        protected int getMaximumIndex() {
            return _allTraceLists.size();
        }
    }

    class TracePanel extends JPanel {

        private final JLabel _description;
        private final JScrollPane _traceView;

        public TracePanel(JTextPane tracePane) {
            setLayout(new BorderLayout());

            _description = new JLabel("");
            _description.setFont(new Font("SansSerif", Font.PLAIN, 24));
            _traceView = new JScrollPane(tracePane);

            final JPanel descriptionPanel = new JPanel();
            descriptionPanel.add(_description);

            add(descriptionPanel, BorderLayout.NORTH);
            add(_traceView, BorderLayout.CENTER);
        }

        void update(CirAnnotatedTrace trace) {
            if (trace != null) {
                _description.setText(trace.description());
            } else {
                _description.setText("");
            }
        }
    }

    private class TextListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent keyEvent) {
            if (keyEvent.getSource() == _searchPanel._actorText) {
                final String text = _searchPanel._actorText.getText();
                if (text.length() > 0) {
                    if (!_searchPanel._nodeCheck.isSelected()) {
                        _searchPanel._prevButton.setEnabled(true);
                        _searchPanel._nextButton.setEnabled(true);
                    } else {
                        if (!_searchPanel._nodeText.getText().equals("")) {
                            _searchPanel._prevButton.setEnabled(true);
                            _searchPanel._nextButton.setEnabled(true);
                        }
                    }
                } else {
                    _searchPanel._prevButton.setEnabled(false);
                    _searchPanel._nextButton.setEnabled(false);
                }
            }
            if (keyEvent.getSource() == _searchPanel._nodeText) {
                final String text = _searchPanel._nodeText.getText();
                if (text.length() > 0) {
                    if (!_searchPanel._actorCheck.isSelected()) {
                        _searchPanel._prevButton.setEnabled(true);
                        _searchPanel._nextButton.setEnabled(true);
                    } else {
                        if (!_searchPanel._actorText.getText().equals("")) {
                            _searchPanel._prevButton.setEnabled(true);
                            _searchPanel._nextButton.setEnabled(true);
                        }
                    }
                } else {
                    _searchPanel._prevButton.setEnabled(false);
                    _searchPanel._nextButton.setEnabled(false);
                }
            }
        }
    }

    class SearchPanel extends JPanel {
        private final JCheckBox _actorCheck;
        private final JTextField _actorText;
        private final JCheckBox _nodeCheck;
        private final JTextField _nodeText;
        private final JCheckBox _regexCheck;
        private final JButton _nextButton;
        private final JButton _prevButton;
        private final JCheckBox _matchCaseCheck;
        private final JCheckBox _wrapSearch;

        public SearchPanel() {
            _actorCheck = new JCheckBox("Method signature");
            _actorCheck.setSelected(false);
            _actorText = new JTextField(15);
            _actorText.setEnabled(false);
            _nodeCheck = new JCheckBox("Node label");
            _nodeCheck.setSelected(false);
            _nodeText = new JTextField(15);
            _nodeText.setEnabled(false);
            _regexCheck = new JCheckBox("Regular expression");
            _regexCheck.setSelected(false);
            _matchCaseCheck = new JCheckBox("Match case");
            _matchCaseCheck.setSelected(false);
            _wrapSearch = new JCheckBox("Wrap search");
            _wrapSearch.setSelected(false);

            _actorCheck.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    _actorText.setEnabled(_actorCheck.isSelected());
                    if (_actorCheck.isSelected()) {
                        _searchPanel._prevButton.setEnabled(true);
                        _searchPanel._nextButton.setEnabled(true);
                    } else {
                        if (_searchPanel._nodeCheck.isSelected() && !_searchPanel._nodeText.getText().equals("")) {
                            _searchPanel._prevButton.setEnabled(true);
                            _searchPanel._nextButton.setEnabled(true);
                        } else {
                            _searchPanel._prevButton.setEnabled(false);
                            _searchPanel._nextButton.setEnabled(false);
                        }
                    }
                    clearSearchHighlights();
                }
            });
            _nodeCheck.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    _nodeText.setEnabled(_nodeCheck.isSelected());
                    if (_nodeCheck.isSelected()) {
                        _searchPanel._prevButton.setEnabled(false);
                        _searchPanel._nextButton.setEnabled(false);
                    } else {
                        if (_searchPanel._actorCheck.isSelected() && !_searchPanel._actorText.getText().equals("")) {
                            _searchPanel._prevButton.setEnabled(true);
                            _searchPanel._nextButton.setEnabled(true);
                        } else {
                            _searchPanel._prevButton.setEnabled(false);
                            _searchPanel._nextButton.setEnabled(false);
                        }
                    }
                    clearSearchHighlights();
                }
            });

            _prevButton =  new JButton(new AbstractAction("Previous") {
                public void actionPerformed(ActionEvent event) {
                    final int returnedIndex = getNextSearch(_actorText.getText(), _nodeText.getText(), SearchDirection.BACKWARD);
                    if (returnedIndex != -1) {
                        if (_indexWithinTrace != returnedIndex) {
                            _indexWithinTrace = returnedIndex;
                            refreshView();
                        }
                        if (_actorCheck.isSelected()) {
                            final VariableSequence<Range> actorRanges = getSearchStringRanges(_currentCir, _currentCir.classMethodActor().format("%H.%n(%p)"), _actorText.getText(), false);
                            final String newLabelText = getHighlightedLabelText(_currentCir.classMethodActor().format("%H.%n(%p)"), actorRanges);
                            _headerPanel._classMethodActor.setText(newLabelText);
                        }
                        if (_nodeCheck.isSelected()) {
                            final VariableSequence<Range> leftNodeRanges = getSearchStringRanges(_currentCir, _currentCir.trace(), _nodeText.getText(), true);
                            showSearchResults(_leftTracePane, leftNodeRanges);
                            if (_currentCir2 != null) {
                                final VariableSequence<Range> rightNodeRanges = getSearchStringRanges(_currentCir2, _currentCir2.trace(), _nodeText.getText(), true);
                                showSearchResults(_rightTracePane, rightNodeRanges);
                            }
                        }
                    }
                }
            });
            _prevButton.setEnabled(false);
            _nextButton =  new JButton(new AbstractAction("Next") {
                public void actionPerformed(ActionEvent event) {
                    final int returnedIndex = getNextSearch(_actorText.getText(), _nodeText.getText(), SearchDirection.FORWARD);
                    if (returnedIndex != -1) {
                        if (_indexWithinTrace != returnedIndex) {
                            _indexWithinTrace = returnedIndex;
                            refreshView();
                        }
                        if (_actorCheck.isSelected()) {
                            final VariableSequence<Range> actorRanges = getSearchStringRanges(_currentCir, _currentCir.classMethodActor().format("%H.%n(%p)"), _actorText.getText(), false);
                            final String newLabelText = getHighlightedLabelText(_currentCir.classMethodActor().format("%H.%n(%p)"), actorRanges);
                            _headerPanel._classMethodActor.setText(newLabelText);
                        }
                        if (_nodeCheck.isSelected()) {
                            final VariableSequence<Range> leftNodeRanges = getSearchStringRanges(_currentCir, _currentCir.trace(), _nodeText.getText(), true);
                            showSearchResults(_leftTracePane, leftNodeRanges);
                            if (_currentCir2 != null) {
                                final VariableSequence<Range> rightNodeRanges = getSearchStringRanges(_currentCir2, _currentCir2.trace(), _nodeText.getText(), true);
                                showSearchResults(_rightTracePane, rightNodeRanges);
                            }
                        }
                    }
                }
            });
            _nextButton.setEnabled(false);

            add(_actorCheck);
            add(_actorText);
            add(_nodeCheck);
            add(_nodeText);
            add(_nextButton);
            add(_prevButton);
            add(_regexCheck);
            add(_matchCaseCheck);
            add(_wrapSearch);
            _actorText.addKeyListener(new TextListener());
            _nodeText.addKeyListener(new TextListener());

            setBorder(BorderFactory.createEtchedBorder());
        }
    }

    private static final String NO_TRACE_TITLE = "<html><b>Compiling/interpreting:</b> <i>none</i> <br><b>Description:</b> <i>none</i></html>";

    private CirAnnotatedTrace _currentCir;
    private CirAnnotatedTrace _currentCir2;
    private final CirTracePane _leftTracePane = new CirTracePane(new CirStyledDocument(null));
    private final CirTracePane _rightTracePane = new CirTracePane(new CirStyledDocument(null));

    private final Map<ClassMethodActor, List<CirAnnotatedTrace>> _traceMap = new HashMap<ClassMethodActor, List<CirAnnotatedTrace>>();

    private VariableSequence<CirAnnotatedTrace> _allTraces = new ArrayListSequence<CirAnnotatedTrace>();

    private List<List<CirAnnotatedTrace>> _allTraceLists = new ArrayList<List<CirAnnotatedTrace>>();
    private int _indexWithinTrace = -1;
    private int _currentTraceListIndex = -1;

    private final JSplitPane _splitPane;
    private Highlighter _traceHighlighter;

    private final JEditorPane _detailPane;
    private final JFrame _frame;

    private final JSlider _fontSizeSlider;
    private boolean _fontChangeFlag;

    private Element _selectedElement;
    private Highlighter.HighlightPainter _selectedElementPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private Highlighter.HighlightPainter _searchPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);

    private final JCheckBox _enableFilter;
    private final JTextField _filter;

    private final HeaderPanel _headerPanel;
    private final TracePanel _leftTrace;
    private final TracePanel _rightTrace;
    private final NavigationPanel _traceNavigationPanel;
    private final NavigationPanel _traceListNavigationPanel;
    private SearchPanel _searchPanel;
    private CirStyledDocument _currentDocument;

    //if true then search from next/previous trace else start from current trace.
    private boolean _isCurrentHighlighted;

    private List<CirAnnotatedTrace> currentTraceList() {
        if (_currentTraceListIndex >= 0) {
            return _allTraceLists.get(_currentTraceListIndex);
        }
        return null;
    }

    private CirAnnotatedTrace currentTrace() {
        final List<CirAnnotatedTrace> traceList = currentTraceList();
        if (traceList != null && _indexWithinTrace >= 0) {
            return traceList.get(_indexWithinTrace);
        }
        return null;
    }

    private CirAnnotatedTrace nextTrace() {
        final List<CirAnnotatedTrace> traceList = currentTraceList();
        if (traceList != null && _indexWithinTrace < traceList.size() - 1) {
            return traceList.get(_indexWithinTrace + 1);
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
        _frame = frame;
        setLayout(new BorderLayout());

        final JPanel filterPanel = new JPanel(new SpringLayout());
        final JLabel filterLabel = new JLabel("Filter");
        _enableFilter = new JCheckBox();
        filterPanel.setToolTipText("Only show traces whose title contains this string");
        _enableFilter.setSelected(false);
        _filter = new JTextField("");
        _filter.setEnabled(false);
        filterLabel.setEnabled(false);
        filterPanel.add(_enableFilter);
        filterPanel.add(filterLabel);
        filterPanel.add(_filter);
        _enableFilter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _filter.setEnabled(_enableFilter.isSelected());
                filterLabel.setEnabled(_enableFilter.isSelected());
            }
        });
        SpringUtilities.makeCompactGrid(filterPanel, 3);
        add(filterPanel, BorderLayout.NORTH);

        _leftTrace = new TracePanel(_leftTracePane);
        _rightTrace = new TracePanel(_rightTracePane);
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _leftTrace, _rightTrace);

        // Font size slider
        final JPanel fontSizeSliderPanel = new JPanel();

        _fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 40, DEFAULT_FONT_SIZE);
        _fontSizeSlider.setMajorTickSpacing(8);
        _fontSizeSlider.setMinorTickSpacing(4);
        _fontSizeSlider.setPaintTicks(true);
        _fontSizeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                _fontChangeFlag = true;
                refreshView();
                _fontChangeFlag = false;
            }
        });

        fontSizeSliderPanel.add(new JLabel("Font Size"));
        fontSizeSliderPanel.add(_fontSizeSlider);

        final JPanel tracePanel = new JPanel(new BorderLayout());
        _headerPanel = new HeaderPanel();
        tracePanel.add(_headerPanel, BorderLayout.NORTH);
        tracePanel.add(_splitPane, BorderLayout.CENTER);
        _splitPane.setDividerLocation(.5d);

        // Detail panel
        _detailPane = new JEditorPane("text/html", null);
        _detailPane.setEditable(false);
        final JScrollPane detailView = new JScrollPane(_detailPane);

        _traceAndDetailView = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tracePanel, detailView);
        add(_traceAndDetailView, BorderLayout.CENTER);

        _traceNavigationPanel = new TraceNavigationPanel();
        _traceListNavigationPanel = new TraceListNavigationPanel();
        _searchPanel = new SearchPanel();

        final JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(_searchPanel);

        final JCheckBox findCheck = new JCheckBox("Find");
        findCheck.setSelected(false);
        findCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _searchPanel.setVisible(findCheck.isSelected());
                _searchPanel._nodeCheck.setSelected(false);
                clearSearchHighlights();
            }
        });

        final JPanel westPanel = new JPanel();
        westPanel.add(findCheck);
        westPanel.add(fontSizeSliderPanel);

        final JPanel p = new JPanel(new BorderLayout());
        p.add(westPanel, BorderLayout.WEST);
        p.add(_traceListNavigationPanel, BorderLayout.CENTER);
        p.add(_traceNavigationPanel, BorderLayout.EAST);

        southPanel.add(p);

        _searchPanel.setVisible(false);
        add(southPanel, BorderLayout.SOUTH);

        _traceHighlighter = _leftTracePane.getHighlighter();

        final MouseInputAdapter mouseInputAdapter = new MouseInputAdapter() {
            private ParenthesisElement _dualRangeElement;
            private String  _hiddenTrace = new String();
            private String  _hiddenTrace2 = new String();
            private IntHashMap<Element> _oldOffsetToElement;
            private IntHashMap<Element> _oldOffsetToElement2;
            private IntHashMap<Element> _tempOffsetToElement;
            private IntHashMap<Element> _newOffsetToElement;
            private int _oldNoOffsets;
            private Bag<CirNode, Element, Sequence<Element>> _oldElementsPerNode;
            private Bag<CirNode, Element, Sequence<Element>> _oldElementsPerNode2;
            private Bag<CirNode, Element, Sequence<Element>> _newElementsPerNode;
            private Element _currentOccurrenceElement;
            private Highlighter.HighlightPainter _secondaryHighlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(_leftTracePane.getSelectionColor().darker());
            private final VariableSequence<Object> _occurrenceHighlights = new ArrayListSequence<Object>();

            /**
             * Clears all highlights marking the occurrence(s) of the CIR node last denoted by a mouse movement.
             */
            private void clearOccurrenceHighlights() {
                for (Object highlight : _occurrenceHighlights) {
                    _traceHighlighter.removeHighlight(highlight);
                }
                _occurrenceHighlights.clear();
            }

            private CirTracePane tracePane(MouseEvent event) {
                if (event.getSource() == _leftTracePane) {
                    return _leftTracePane;
                } else if (event.getSource() == _rightTracePane) {
                    return _rightTracePane;
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
                if (element != _currentOccurrenceElement) {
                    CirStyledDocument document = null;
                    if (event.getSource() == _leftTracePane) {
                        document = _leftTracePane.cirDocument();
                        _traceHighlighter = _leftTracePane.getHighlighter();
                    } else if (event.getSource() == _rightTracePane) {
                        document = _rightTracePane.cirDocument();
                        _traceHighlighter = _rightTracePane.getHighlighter();
                    }
                    clearOccurrenceHighlights();
                    if (element != null) {
                        element.visitAssociatedRanges(new RangeVisitor() {
                            public void visitRange(Range range) {
                                try {
                                    _occurrenceHighlights.append(_traceHighlighter.addHighlight(range.start(), range.end(), _secondaryHighlighterPainter));
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
                                        _occurrenceHighlights.append(_traceHighlighter.addHighlight(range.start(), range.end(), DefaultHighlighter.DefaultPainter));
                                        //System.err.printAddress("primary range: " + range);
                                    } catch (BadLocationException badLocationException) {
                                        ProgramWarning.message("error highlighting element range " + range);
                                    }
                                }
                            });
                        }
                    }
                    _currentOccurrenceElement = element;
                    _currentDocument = document;
                }
            }
            int _foldStart = -1;
            int _foldEnd = -1;

            @Override
            public void mouseReleased(MouseEvent event) {
                final Element element = element(event);
                if (event.getSource() == _leftTracePane) {
                    _traceHighlighter = _leftTracePane.getHighlighter();
                } else if (event.getSource() == _rightTracePane) {
                    _traceHighlighter = _rightTracePane.getHighlighter();
                }
                if (element instanceof ParenthesisElement) {
                    _dualRangeElement = (ParenthesisElement) element;
                }
                if (element != _selectedElement || element instanceof ParenthesisElement) {
                    // Remove all highlights
                    _leftTracePane.getHighlighter().removeAllHighlights();
                    _rightTracePane.getHighlighter().removeAllHighlights();
                    if (element != null) {
                        final CirNode node = element.node();
                        if (node != null) {
                            element.visitRanges(new RangeVisitor() {
                                public void visitRange(Range range) {
                                    try {
                                        _traceHighlighter.addHighlight(range.start(), range.end(), _selectedElementPainter);
                                    } catch (BadLocationException badLocationException) {
                                        ProgramWarning.message("error highlighting element range " + range);
                                    }
                                }
                            });
                        } else {
                            if (_currentDocument._collapsedOffset == -1) {
                                _currentDocument._collapsedDual = _dualRangeElement;
                                _newOffsetToElement = new IntHashMap<Element>();
                                _newElementsPerNode = new SequenceBag<CirNode, Element>(SequenceBag.MapType.IDENTITY);
                                _foldStart = ((ParenthesisElement) element).firstRange().start();
                                _foldEnd = ((ParenthesisElement) element).secondRange().start();
                                _oldNoOffsets = _currentDocument.getLength();
                                if (event.getSource() == _leftTracePane) {
                                    _oldOffsetToElement = _currentDocument._offsetToElement;
                                    _tempOffsetToElement = _oldOffsetToElement;
                                    _oldElementsPerNode = _currentDocument._elementsPerNode;
                                    try {
                                        _hiddenTrace = _currentDocument.getText(_dualRangeElement.firstRange().start() + 1, _dualRangeElement.secondRange().start() - _dualRangeElement.firstRange().start() - 1);
                                    } catch (BadLocationException ble) {
                                        System.err.println("Bad Location while folding trace in the left pane");
                                    }
                                } else if (event.getSource() == _rightTracePane) {
                                    _oldOffsetToElement2 = _currentDocument._offsetToElement;
                                    _tempOffsetToElement = _oldOffsetToElement2;
                                    _oldElementsPerNode2 = _currentDocument._elementsPerNode;
                                    try {
                                        _hiddenTrace2 = _currentDocument.getText(_dualRangeElement.firstRange().start() + 1, _dualRangeElement.secondRange().start() - _dualRangeElement.firstRange().start() - 1);
                                    } catch (BadLocationException ble) {
                                        System.err.println("Bad Location while folding trace in the right pane");
                                    }
                                }
                                _currentDocument._collapsedOffset = _foldStart;
                                int newMapIndex = 0;
                                int flag = 0;
                                SimpleElement currSimple = null;
                                ParenthesisElement currDual = null;
                                for (int i = 0; i <= _oldNoOffsets; i++) {
                                    if (i > _foldStart && i < _foldEnd) {
                                        if (flag == 0) {
                                            newMapIndex = newMapIndex + 3;
                                            flag = 1;
                                        }
                                        continue;
                                    }
                                    if (_tempOffsetToElement.get(i) == null) {
                                        newMapIndex++;
                                    }

                                    if (i <= _foldStart) {
                                        if (_tempOffsetToElement.get(i) instanceof SimpleElement) {
                                            final Range simpleRange = new Range(((SimpleElement) _tempOffsetToElement.get(i)).range().start(),
                                                                            ((SimpleElement) _tempOffsetToElement.get(i)).range().end());
                                            currSimple = new SimpleElement(_tempOffsetToElement.get(i).node(), simpleRange);
                                            _newOffsetToElement.put(newMapIndex++, currSimple);
                                            _newElementsPerNode.add(_tempOffsetToElement.get(i).node(), currSimple);
                                        }
                                        if (_tempOffsetToElement.get(i) instanceof ParenthesisElement) {
                                            final Range firstRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).firstRange().start());
                                            Range secondRange = null;
                                            if (((ParenthesisElement) _tempOffsetToElement.get(i)).secondRange().start() < _foldStart) {
                                                secondRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).secondRange().start());
                                            }
                                            if (((ParenthesisElement) _tempOffsetToElement.get(i)).secondRange().start() >= _foldEnd) {
                                                secondRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).secondRange().start() - (_foldEnd - _foldStart - 4));
                                            }

                                            currDual = new ParenthesisElement(firstRange, secondRange);
                                            _newOffsetToElement.put(newMapIndex++, currDual);
                                        }
                                    }
                                    if (i >= _foldEnd) {
                                        if (_tempOffsetToElement.get(i) instanceof SimpleElement) {
                                            final Range simpleRange = new Range(((SimpleElement) _tempOffsetToElement.get(i)).range().start() - (_foldEnd - _foldStart - 4),
                                                                            ((SimpleElement) _tempOffsetToElement.get(i)).range().end());
                                            currSimple = new SimpleElement(_tempOffsetToElement.get(i).node(), simpleRange);

                                            _newOffsetToElement.put(newMapIndex++, currSimple);
                                            _newElementsPerNode.add(_tempOffsetToElement.get(i).node(), currSimple);
                                        }
                                        if (_tempOffsetToElement.get(i) instanceof ParenthesisElement) {
                                            Range firstRange = null;
                                            final Range secondRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).secondRange().start() - (_foldEnd - _foldStart - 4));
                                            if (((ParenthesisElement) _tempOffsetToElement.get(i)).firstRange().start() <= _foldStart) {
                                                firstRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).firstRange().start());
                                            }
                                            if (((ParenthesisElement) _tempOffsetToElement.get(i)).firstRange().start() > _foldEnd) {
                                                firstRange = new Range(((ParenthesisElement) _tempOffsetToElement.get(i)).firstRange().start() - (_foldEnd - _foldStart - 4));
                                            }

                                            currDual = new ParenthesisElement(firstRange, secondRange);
                                            _newOffsetToElement.put(newMapIndex++, currDual);
                                        }
                                    }
                                }
                                try {
                                    _currentDocument.remove(_dualRangeElement.firstRange().start() + 1, _dualRangeElement.secondRange().start() - _dualRangeElement.firstRange().start() - 1);
                                    _currentDocument.insertString(_dualRangeElement.firstRange().start() + 1, "...", null);
                                } catch (BadLocationException ble) {
                                    System.err.println("Bad Location while folding trace");
                                }

                                final Style style = _currentDocument.addStyle(null, NORMAL);
                                StyleConstants.setFontSize(style, _fontSizeSlider.getValue());
                                _currentDocument.setCharacterAttributes(_dualRangeElement.firstRange().start() + 1, 3, style, true);

                                _currentDocument._offsetToElement = _newOffsetToElement;
                                _currentDocument._elementsPerNode = _newElementsPerNode;
                            } else if (_dualRangeElement.firstRange().start() == _currentDocument._collapsedOffset) {
                                try {
                                    _currentDocument.remove(_dualRangeElement.firstRange().start() + 1, 3);
                                    if (event.getSource() == _leftTracePane) {
                                        _currentDocument.insertString(_dualRangeElement.firstRange().start() + 1, _hiddenTrace, null);
                                        _currentDocument._offsetToElement = _oldOffsetToElement;
                                        _currentDocument._elementsPerNode = _oldElementsPerNode;
                                    } else if (event.getSource() == _rightTracePane) {
                                        _currentDocument.insertString(_dualRangeElement.firstRange().start() + 1, _hiddenTrace2, null);
                                        _currentDocument._offsetToElement = _oldOffsetToElement2;
                                        _currentDocument._elementsPerNode = _oldElementsPerNode2;
                                    }
                                } catch (BadLocationException ble) {
                                    System.err.println("Bad Location while expanding trace");
                                }
                                _currentDocument._collapsedOffset = -1;
                                _currentDocument._collapsedDual = null;

                                final Style style = _currentDocument.addStyle(null, NORMAL);
                                StyleConstants.setFontSize(style, _fontSizeSlider.getValue());
                                final ParenthesisElement newDual = (ParenthesisElement) _currentDocument._offsetToElement.get(_dualRangeElement.firstRange().start());

                                _currentDocument.setCharacterAttributes(
                                    newDual.firstRange().start() + 1,
                                    newDual.secondRange().start() - newDual.firstRange().start(),
                                    style, true);
                            }
                        }
                    }
                    _selectedElement = element;
                    refreshView();
                }
            }
        };
        _leftTracePane.addMouseListener(mouseInputAdapter);
        _leftTracePane.addMouseMotionListener(mouseInputAdapter);
        _rightTracePane.addMouseListener(mouseInputAdapter);
        _rightTracePane.addMouseMotionListener(mouseInputAdapter);
    }
    private static final StyleContext STYLE_CONTEXT;
    private static final Style NORMAL;
    private static final Style DIFF_DELETION;
    private static final Style DIFF_INSERTION;
    private JSplitPane _traceAndDetailView;

    private List<SimpleElement> _leftElements;
    private List<SimpleElement> _rightElements;
    private Diff _currentDiffs;

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
                    visualizer._traceAndDetailView.setDividerLocation(0.8d);
                    visualizer._splitPane.setDividerLocation(0.5d);
                }
            });
        }

        // Show a dialog to allow the user to enter/modify the trace filter before any traces are sent to the visualizer
        final String s = (String) JOptionPane.showInputDialog(frame,
                        "Enter substring for filtering trace of interest to methods whose fully qualified name contains the substring", "Trace Filter Wizard", JOptionPane.QUESTION_MESSAGE, null, null, visualizer._filter.getText());
        if (s != null) {
            visualizer._filter.setText(s);
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
        final Rectangle bounds = _frame.getBounds();
        settings.setProperty("window.x", String.valueOf(bounds.x));
        settings.setProperty("window.y", String.valueOf(bounds.y));
        settings.setProperty("window.width", String.valueOf(bounds.width));
        settings.setProperty("window.height", String.valueOf(bounds.height));
        settings.setProperty("font.size.slider", String.valueOf(_fontSizeSlider.getValue()));
        settings.setProperty("filter.text", _filter.getText());
        settings.setProperty("filter.enabled", String.valueOf(_filter.isEnabled()));
        settings.setProperty("traceSplitPane.dividerLocation", String.valueOf(_traceAndDetailView.getDividerLocation()));
        settings.setProperty("splitPane.dividerLocation", String.valueOf(_splitPane.getDividerLocation()));
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
            final Rectangle bounds = _frame.getBounds();
            bounds.x = Integer.parseInt(settings.getProperty("window.x", "100"));
            bounds.y = Integer.parseInt(settings.getProperty("window.y", "100"));
            bounds.width = Integer.parseInt(settings.getProperty("window.width", "400"));
            bounds.height = Integer.parseInt(settings.getProperty("window.height", "200"));
            _frame.setBounds(bounds);
            _fontSizeSlider.setValue(Integer.parseInt(settings.getProperty("font.size.slider", String.valueOf(DEFAULT_FONT_SIZE))));
            _filter.setText(settings.getProperty("filter.text", ""));
            _enableFilter.setSelected(Boolean.valueOf(settings.getProperty("filter.enabled", String.valueOf(false))));
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    _traceAndDetailView.setDividerLocation(Integer.parseInt(settings.getProperty("traceSplitPane.dividerLocation", "0")));
                    _splitPane.setDividerLocation(Integer.parseInt(settings.getProperty("splitPane.dividerLocation", "0")));
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
        final CirAnnotatedTrace oldCir = _currentCir;
        _currentCir = currentTrace();
        _currentCir2 = nextTrace();
        document = new CirStyledDocument(_currentCir);
        final int numTraces = currentTraceList.size();
        if (_currentCir2 != null) {
            document2 = new CirStyledDocument(_currentCir2);
        }

        if (numTraces == 2) {
            final Style style = document.addStyle(null, NORMAL);
            StyleConstants.setFontSize(style, _fontSizeSlider.getValue());
            try {
                document.insertString(0, _currentCir.trace(), style);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }

            final Style style2 = document2.addStyle(null, NORMAL);
            StyleConstants.setFontSize(style2, _fontSizeSlider.getValue());
            try {
                document2.insertString(0, _currentCir2.trace(), style2);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }
            _leftTracePane.setDocument(document);
            _rightTracePane.setDocument(document2);
            _headerPanel.update(_currentCir, _currentCir2);
            _leftTrace.update(_currentCir);
            _rightTrace.update(_currentCir2);
            findDiffs(document, document2);
        }

        if (oldCir != _currentCir) {
            try {
                final Style style = document.addStyle(null, NORMAL);
                StyleConstants.setFontSize(style, _fontSizeSlider.getValue());
                document.insertString(0, _currentCir.trace(), style);
                _leftTracePane.setDocument(document);
            } catch (BadLocationException e) {
                ProgramWarning.message("could not update CIR visualizer: " + e);
                return;
            }

            if (numTraces >= 2) {
                if (document2 != null) {
                    try {
                        final Style style2 = document2.addStyle(null, NORMAL);
                        StyleConstants.setFontSize(style2, _fontSizeSlider.getValue());
                        document2.insertString(0, _currentCir2.trace(), style2);
                        _rightTracePane.setDocument(document2);
                    } catch (BadLocationException e) {
                        ProgramWarning.message("could not update CIR visualizer: " + e);
                        return;
                    }
                    _splitPane.setDividerLocation(.5d);
                    if (numTraces >= 2) {
                        findDiffs(document, document2);
                    }
                } else {
                    //Display only left
                    _rightTracePane.setText("");
                    _splitPane.setDividerLocation(.99d);
                }
                _selectedElement = null;
                _traceHighlighter.removeAllHighlights();
                _headerPanel.update(_currentCir, _currentCir2);
                _leftTrace.update(_currentCir);
                _rightTrace.update(_currentCir2);
            }
        } else {
            if (_fontChangeFlag) {
                document = _leftTracePane.cirDocument();
                final Style style = document.addStyle(null, NORMAL);
                StyleConstants.setFontSize(style, _fontSizeSlider.getValue());
                document.setCharacterAttributes(0, document.getLength(), style, true);

                if (numTraces >= 2) {
                    document2 = _rightTracePane.cirDocument();
                    final Style style2 = document2.addStyle(null, NORMAL);
                    StyleConstants.setFontSize(style2, _fontSizeSlider.getValue());
                    document2.setCharacterAttributes(0, document2.getLength(), style2, true);

                    final Diff diff = _currentDiffs;
                    showDiffs(document, _leftElements, diff.deletions(), DIFF_DELETION);
                    showDiffs(document2, _rightElements, diff.insertions(), DIFF_INSERTION);
                }
            }
        }

        refreshNavigation();

        if (_selectedElement == null) {
            _detailPane.setText(NO_SELECTED_NODE_DETAIL);
        } else if (_selectedElement.node() == null) {
            _detailPane.setText(NO_SELECTED_NODE_DETAIL);
            if (_currentDocument._collapsedOffset == -1) {
                if (numTraces >= 2) {
                    document = _leftTracePane.cirDocument();
                    document2 = _rightTracePane.cirDocument();
                    final Diff diff = _currentDiffs;
                    showDiffs(document, _leftElements, diff.deletions(), DIFF_DELETION);
                    showDiffs(document2, _rightElements, diff.insertions(), DIFF_INSERTION);
                }
            }
        } else {
            final CirNode node = _selectedElement.node();
            final StringBuilder sb = new StringBuilder("<html>").
                append("<table border=\"1\">").
                append("<tr><td>Type</td><td>" + node.getClass().getSimpleName() + "</td></tr>").
                append("<tr><td>ID</td><td>" + node.id() + "</td></tr>");
            node.acceptVisitor(new CirVisitor() {
                @Override
                public void visitConstant(CirConstant constant) {
                    final Value value = constant.value();
                    sb.append("<tr><td>Kind</td><td>" + value.kind() + "</td></tr>");
                    if (value.kind() == Kind.REFERENCE) {
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
            _detailPane.setText(sb.toString());
        }
    }


    private void refreshNavigation() {
        _traceNavigationPanel.update();
        _traceListNavigationPanel.update();
    }

    public boolean shouldBeTraced(MethodActor classMethodActor) {
        if (_filter.isEnabled()) {
            if (classMethodActor != null) {
                return classMethodActor.format("%H.%n(%p)").contains(_filter.getText());

            }
            return false;
        }
        return true;
    }

    public synchronized void add(CirAnnotatedTrace cirAnnotatedTrace) {
        _allTraces.append(cirAnnotatedTrace);
        List<CirAnnotatedTrace> traceList = _traceMap.get(cirAnnotatedTrace.classMethodActor());
        if (traceList == null) {
            traceList = new ArrayList<CirAnnotatedTrace>();
            _traceMap.put(cirAnnotatedTrace.classMethodActor(), traceList);
            _allTraceLists.add(traceList);
            if (_currentTraceListIndex < 0) {
                _currentTraceListIndex = 0;
            }
        }
        traceList.add(cirAnnotatedTrace);
        if (_indexWithinTrace < 0 || traceList.size() == 2) {
            _indexWithinTrace = 0;
            refreshView();
        } else {
            refreshNavigation();
        }
    }


    private List<SimpleElement> getSimpleElements(CirStyledDocument document) {

        int len = 0;
        final List<SimpleElement> seq = new ArrayList<SimpleElement>();

        while (len <= document.getLength()) {
            if (document._offsetToElement.get(len) instanceof SimpleElement) {
                seq.add((SimpleElement) document._offsetToElement.get(len));
                len += ((SimpleElement) document._offsetToElement.get(len)).range().length();
            } else {
                len++;
            }
        }
        return seq;
    }


    private void findDiffs(CirStyledDocument leftDocument, CirStyledDocument rightDocument) {

        final List<SimpleElement> leftElements = getSimpleElements(leftDocument);
        final List<SimpleElement> rightElements = getSimpleElements(rightDocument);

        _leftElements = leftElements;
        _rightElements = rightElements;

        final Diff.Equality equality = new Diff.Equality() {
            public boolean test(Object object1, Object object2) {
                final SimpleElement element1 = (SimpleElement) object1;
                final SimpleElement element2 = (SimpleElement) object2;
                return element1.node() == element2.node();
            }

        };
        final Diff diff = new Diff(leftElements.toArray(new SimpleElement[leftElements.size()]),
                                  rightElements.toArray(new SimpleElement[rightElements.size()]),
                                  equality);
        _currentDiffs = diff;

        showDiffs(leftDocument, leftElements, diff.deletions(), DIFF_DELETION);
        showDiffs(rightDocument, rightElements, diff.insertions(), DIFF_INSERTION);
    }


    private void showDiffs(CirStyledDocument document, List<SimpleElement> elements, Sequence<Range> changes, Style changeStyle) {
        int shift = 0;
        if (document._collapsedOffset != -1) {
            shift = document._collapsedDual.secondRange().start() - document._collapsedDual.firstRange().start() - 4;
        }
        StyleConstants.setFontSize(changeStyle, _fontSizeSlider.getValue());
        for (Range deletion : changes) {
            for (int k = deletion.start(); k < deletion.end(); k++) {
                if (document._collapsedOffset != -1) {
                    if (elements.get(k).range().start() < document._collapsedDual.firstRange().start()) {
                        document.setCharacterAttributes(elements.get(k).range().start(), (int) elements.get(k).range().length(), changeStyle, true);
                    } else if (elements.get(k).range().start() > document._collapsedDual.secondRange().start()) {
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
        final boolean inActors = _searchPanel._actorCheck.isSelected();
        final boolean inNodes = _searchPanel._nodeCheck.isSelected();
        final boolean isCurrentHighlighted = _isCurrentHighlighted;
        final boolean isWrapSearch = _searchPanel._wrapSearch.isSelected();
        final IndexedSequence<CirAnnotatedTrace> traces = _allTraces;
        final int lastIndex = traces.length() - 1;

        //decide from which index to search
        final int startIndex;
        if (!isCurrentHighlighted) {
            startIndex = _indexWithinTrace;
        } else {
            if (direction == SearchDirection.FORWARD) {
                startIndex = isWrapSearch ? (_indexWithinTrace + 1) % lastIndex : _indexWithinTrace + 1;
            } else {
                startIndex = isWrapSearch ? (lastIndex + _indexWithinTrace - 1) % lastIndex : _indexWithinTrace - 1;
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
            index = direction.nextIndex(index, traces.length(), startIndex, isWrapSearch);
            if (index == -1 || index == traces.length() - 1) {
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
    private VariableSequence<Range> getSearchStringRanges(CirAnnotatedTrace trace, String searchIn, String lookFor, boolean inTrace) {
        final boolean ignoreCase = !_searchPanel._matchCaseCheck.isSelected();
        final boolean regex = _searchPanel._regexCheck.isSelected();

        final VariableSequence<Range> ranges = new ArrayListSequence<Range>();
        int rangeStart = 0;
        if (!regex) {
            while (rangeStart <= (searchIn.length() - lookFor.length())) {
                if (searchIn.regionMatches(ignoreCase, rangeStart, lookFor, 0, lookFor.length())) {
                    final Range range = new Range(rangeStart, rangeStart + lookFor.length());
                    ranges.append(range);
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
                ranges.append(range);
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
                ranges.append(range);
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
    private void showSearchResults(JTextPane textPane, VariableSequence<Range> ranges) {
        final Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
        for (Range range : ranges) {
            try {
                highlighter.addHighlight(range.start(), range.end(), _searchPainter);
            } catch (BadLocationException ble) {
                ProgramWarning.message("error highlighting search result range " + range);
            }
            _isCurrentHighlighted = true;
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
    private String getHighlightedLabelText(String currentText, VariableSequence<Range> ranges) {
        final StringBuffer newText = new StringBuffer("<html>" + currentText + "</html>");
        final String startTag = "<font bgcolor=\"#00FFFF\">";
        final String endTag = "</font>";
        int count = 0;

        for (Range range : ranges) {
            newText.insert(31 * count + range.start() + 6, startTag);
            newText.insert(31 * count + range.end() + 30, endTag);
            count++;
        }
        _isCurrentHighlighted = true;
        return newText.toString();
    }


    /**
     * Clears search highlights when search panel is closed.
     *
     */
    private void clearSearchHighlights() {
        _isCurrentHighlighted = false;
        if (!_searchPanel._nodeCheck.isSelected()) {
            _leftTracePane.getHighlighter().removeAllHighlights();
            _rightTracePane.getHighlighter().removeAllHighlights();
        }
        if (!_searchPanel._actorCheck.isSelected()) {
            _headerPanel._classMethodActor.setText(_headerPanel._oldClassMethodActor);
        }
    }
}
