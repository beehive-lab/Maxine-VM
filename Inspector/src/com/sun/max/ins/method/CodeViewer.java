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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.stack.*;

/**
 * Base class for panels that show a row-oriented view of a method in a MethodInspector framework.
 * Not intended for use outside a MethodInspector, so not undockable;
 * Includes machinery for some common operations, based on abstract "rows"
 * - maintaining a cache that maps row->stackFrame for the thread of current focus
 * - tracking which rows are "active", i.e. have some frame at that location for the thread of current focus
 * - an action, attached to a toolbar button, that scrolls to the next active row
 *
 * @author Michael Van De Vanter
 */
public abstract class CodeViewer extends InspectorPanel {

    private static final int TRACE_VALUE = 2;

    private final MethodInspector _parent;

    private JPanel _toolBarPanel;
    private JToolBar _toolBar;
    private RowTextSearchToolBar _searchToolBar;
    private final JButton _searchButton;
    private final JButton _activeRowsButton;
    private JButton _viewCloseButton;

    public MethodInspector parent() {
        return _parent;
    }

    protected JToolBar toolBar() {
        return _toolBar;
    }

    public abstract MethodInspector.CodeKind codeKind();

    public abstract String codeViewerKindName();

    public abstract boolean updateCodeFocus(TeleCodeLocation teleCodeLocation);

    public void updateThreadFocus(TeleNativeThread teleNativeThread) {
        final long epoch = teleVM().teleProcess().epoch();
        updateCaches(epoch, false);
    }

    public CodeViewer(Inspection inspection, MethodInspector parent) {
        super(inspection, new BorderLayout());
        _parent = parent;

        _searchButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                addSearchToolBar();
            }
        });
        _searchButton.setText("Search...");
        _searchButton.setToolTipText("Open toolbar for searching");

        _activeRowsButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                int nextActiveRow = nextActiveRow();
                if (nextActiveRow >= 0) {
                    if (nextActiveRow == getSelectedRow()) {
                        // If already at an active row, go to the next one, if it exists.
                        nextActiveRow = nextActiveRow();
                    }
                    setFocusAtRow(nextActiveRow);
                }
            }
        });
        _activeRowsButton.setIcon(style().debugActiveRowButtonIcon());
        _activeRowsButton.setForeground(style().debugIPTagColor());
        _activeRowsButton.setToolTipText("Scroll to next line with IP or Call Return");
        _activeRowsButton.setEnabled(false);

        _viewCloseButton = new JButton();
        _viewCloseButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                parent().closeCodeViewer(CodeViewer.this);
            }
        });
        _viewCloseButton.setIcon(style().codeViewCloseIcon());
        _viewCloseButton.setToolTipText("Close " + codeViewerKindName());

        //getActionMap().put(SEARCH_ACTION, new SearchAction());

        // TODO (mlvdv)  generalize so that this binding comes from a preference
        //getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('F', CTRL_DOWN_MASK), SEARCH_ACTION);
    }

    protected void createView(long epoch) {
        _toolBarPanel = new JPanel();
        _toolBarPanel.setLayout(new GridLayout(0, 1));
        _toolBar = new JToolBar();
        _toolBar.setFloatable(false);
        _toolBar.setRollover(true);
        _toolBarPanel.add(_toolBar);
        add(_toolBarPanel, BorderLayout.NORTH);
    }

    private IndexedSequence<Integer> _searchMatchingRows = null;

    /**
     * @return the rows that match a current search session; null if no search session active.
     */
    protected final IndexedSequence<Integer> getSearchMatchingRows() {
        return _searchMatchingRows;
    }

    private final RowSearchListener _searchListener = new RowSearchListener() {

        public void searchResult(IndexedSequence<Integer> searchMatchingRows) {
            _searchMatchingRows = searchMatchingRows;
            // go to next matching row from current selection
            if (_searchMatchingRows != null) {
                Trace.line(TRACE_VALUE, "search: matches " + _searchMatchingRows.length() + " = " + _searchMatchingRows);
            }
            repaint();
        }

        public void selectNextResult() {
            setFocusAtNextSearchMatch();
        }

        public void selectPreviousResult() {
            setFocusAtPreviousSearchMatch();
        }

        public void closeSearch() {
            CodeViewer.this.closeSearch();
        }
    };

    private void addSearchToolBar() {
        if (_searchToolBar == null) {
            _searchToolBar = new RowTextSearchToolBar(inspection(), _searchListener, getRowTextSearcher());
            _toolBarPanel.add(_searchToolBar);
            parent().frame().pack();
            _searchToolBar.getFocus();
        }
    }

    private void closeSearch() {
        Trace.line(TRACE_VALUE, "search:  closing");
        _toolBarPanel.remove(_searchToolBar);
        parent().frame().pack();
        _searchToolBar = null;
        _searchMatchingRows = null;
        // do some kind of update in case there were display effects.
    }

    private void setFocusAtNextSearchMatch() {
        Trace.line(TRACE_VALUE, "search:  next match");
        if (_searchMatchingRows.length() > 0) {
            int currentRow = getSelectedRow();
            for (int row : _searchMatchingRows) {
                if (row > currentRow) {
                    setFocusAtRow(row);
                    return;
                }
            }
            // wrap, could be optional, or dialog choice
            currentRow = -1;
            for (int row : _searchMatchingRows) {
                if (row > currentRow) {
                    setFocusAtRow(row);
                    return;
                }
            }
        } else {
            flash();
        }
    }

    private void setFocusAtPreviousSearchMatch() {
        Trace.line(TRACE_VALUE, "search:  previous match");
        if (_searchMatchingRows.length() > 0) {
            int currentRow = getSelectedRow();
            for (int index = _searchMatchingRows.length() - 1; index >= 0; index--) {
                final Integer matchingRow = _searchMatchingRows.get(index);
                if (matchingRow < currentRow) {
                    setFocusAtRow(matchingRow);
                    return;
                }
            }
            // wrap, could be optional, or dialog choice
            currentRow = getRowCount();
            for (int index = _searchMatchingRows.length() - 1; index >= 0; index--) {
                final Integer matchingRow = _searchMatchingRows.get(index);
                if (matchingRow < currentRow) {
                    setFocusAtRow(matchingRow);
                    return;
                }
            }
        } else {
            flash();
        }
    }


    /**
     * @return a searcher for locating rows with a textual regexp.
     */
    protected abstract RowTextSearcher getRowTextSearcher();

    /**
     * @return how man rows are in the view.
     */
    protected abstract int getRowCount();

    /**
     * @return the row in a code display that is currently selected (at code focus); -1 if no selection
     */
    protected abstract int getSelectedRow();

    /**
     * Sets the global focus of code location at the code being displayed in the row.
     */
    protected abstract void setFocusAtRow(int row);

    /**
     * Adds a button to the view's tool bar that enables textual search.
     */
    protected void addSearchButton() {
        toolBar().add(_searchButton);
    }

    /**
     * Adds a button to the view's tool bar that enables navigation among "active" rows, those that correspond to
     * stack locations in the current thread.
     */
    protected void addActiveRowsButton() {
        toolBar().add(_activeRowsButton);
    }

    /**
     * Adds a button to the view's tool bar that closes this view.
     */
    protected void addCodeViewCloseButton() {
        _toolBar.add(_viewCloseButton);
    }

    public final void refresh(long epoch, boolean force) {
        updateCaches(epoch, force);
        updateView(epoch, force);
        updateSize();
        invalidate();
        repaint();
    }

    protected void updateSize() {
        for (int index = 0; index < getComponentCount(); index++) {
            final Component component = getComponent(index);
            if (component instanceof JScrollPane) {
                final JScrollPane scrollPane = (JScrollPane) component;
                final Dimension size = scrollPane.getViewport().getPreferredSize();
                setMaximumSize(new Dimension(size.width + 40, size.height + 40));
            }
        }
    }

    protected void flash() {
        _parent.frame().flash();
    }


    /**
     * Summary information for a frame on the stack.
     */
    protected final class StackFrameInfo {

        private final StackFrame _stackFrame;

        /**
         * @return the {@link StackFrame}
         */
        public StackFrame frame() {
            return _stackFrame;
        }

        private final TeleNativeThread _teleNativeThread;

        /**
         * @return the thread in whose stack the frame resides.
         */
        public TeleNativeThread thread() {
            return _teleNativeThread;
        }

        private final int _stackPosition;

        /**
         * @return the position of the frame on the stack, with 0 at top
         */
        public int position() {
            return _stackPosition;
        }

        public StackFrameInfo(StackFrame stackFrame, TeleNativeThread teleNativeThread, int stackPosition) {
            _stackFrame = stackFrame;
            _teleNativeThread = teleNativeThread;
            _stackPosition = stackPosition;
        }
    }

    // Cached stack information, relative to this method, derived from the thread of current focus.
    // TODO (mlvdv) Generalize to account for the possibility of multiple stack frames associated with a single row.
    protected StackFrameInfo[] _rowToStackFrameInfo;

    /**
     * Rebuild the data in the cached stack information for the code view.
     */
    protected abstract void updateStackCache();

    // The thread from which the stack cache was last built.
    private TeleNativeThread _threadForCache = null;

    // The epoch at which the stack cache was last built.
    private long _processEpochForCache = -1;

    private void updateCaches(long epoch, boolean force) {
        final TeleNativeThread teleNativeThread = inspection().focus().thread();
        if (teleNativeThread != _threadForCache || epoch != _processEpochForCache || force) {
            updateStackCache();
            // Active rows depend on the stack cache.
            updateActiveRows();
            _threadForCache = teleNativeThread;
            _processEpochForCache = epoch;
        }
    }


    /**
     * Updates any label in the view that are based on state in the {@link TeleVM}.
     *
     */
    protected abstract void updateView(long epoch, boolean force);

    /**
     * Returns stack frame information, if any, associated with the row.
     */
    protected StackFrameInfo stackFrameInfo(int row) {
        return _rowToStackFrameInfo[row];
    }

    /**
     * Is the target code address at the row an instruction pointer
     * for a non-top frame of the stack of the thread that is the current focus?
     */
    protected boolean isCallReturn(int row) {
        final StackFrameInfo stackFrameInfo = _rowToStackFrameInfo[row];
        return stackFrameInfo != null && !stackFrameInfo.frame().isTopFrame();
    }

    /**
     * Is the target code address at the row an instruction pointer
     * for the top frame of the stack of the thread that is the current focus?
     */
    protected boolean isInstructionPointer(int row) {
        final StackFrameInfo stackFrameInfo = _rowToStackFrameInfo[row];
        return stackFrameInfo != null && stackFrameInfo.frame().isTopFrame();
    }

    // Active rows are those for which there is an associated stack frame
    private VectorSequence<Integer> _activeRows = new VectorSequence<Integer>(3);
    private int _currentActiveRowIndex = -1;

    private void updateActiveRows() {
        _activeRows.clear();
        for (int row = 0; row < _rowToStackFrameInfo.length; row++) {
            if (_rowToStackFrameInfo[row] != null) {
                _activeRows.append(row);
            }
        }
        _currentActiveRowIndex = -1;
        _activeRowsButton.setEnabled(hasActiveRows());
    }

    /**
     * Does the method have any rows that are either the current instruction pointer or call return lines marked.
     */
    protected boolean hasActiveRows() {
        return _activeRows.length() > 0;
    }

    /**
     * Cycles through the rows in the method that are either the current instruction pointer or call return lines marked.
     * Resets to the first after each refresh.
     */
    protected int nextActiveRow() {
        if (hasActiveRows()) {
            _currentActiveRowIndex = (_currentActiveRowIndex + 1) % _activeRows.length();
            return _activeRows.elementAt(_currentActiveRowIndex);
        }
        return -1;
    }

    // TODO (mlvdv) figure out how to make this a view-specific binding without interference with global menu item accelerators.
//    private final class SearchAction extends InspectorAction {
//
//        SearchAction() {
//            super(inspection(), SEARCH_ACTION);
//        }
//
//        @Override
//        public void procedure() {
//            addSearchToolBar();
//        }
//    }

}
