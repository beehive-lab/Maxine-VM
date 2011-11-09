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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * Base class for panels that show a row-oriented view of a method in a MethodInspector framework.
 * Not intended for use outside a {@link JavaMethodView}, so not undockable;
 * Includes machinery for some common operations, based on abstract "rows"
 * - maintaining a cache that maps row->stackFrame for the thread of current focus
 * - tracking which rows are "active", i.e. have some frame at that location for the thread of current focus
 * - an action, attached to a toolbar button, that scrolls to the next active row
 * - a "search" function that causes a separate toolbar to appear that permits regexp row-based searching.
 */
public abstract class CodeViewer extends InspectorPanel {

    private static final int TRACE_VALUE = 2;

    private final MethodView parent;

    private JPanel toolBarPanel;
    private JToolBar toolBar;
    private RowTextSearchToolBar searchToolBar;
    private final JButton searchButton;
    private final JButton activeRowsButton;
    private JButton viewCloseButton;

    public MethodView parent() {
        return parent;
    }

    protected JToolBar toolBar() {
        return toolBar;
    }

    public abstract MethodCodeKind codeKind();

    public abstract String codeViewerKindName();

    public abstract void print(String name);

    public abstract boolean updateCodeFocus(MaxCodeLocation codeLocation);

    public void updateThreadFocus(MaxThread thread) {
        updateCaches(false);
    }

    public CodeViewer(Inspection inspection, MethodView parent) {
        super(inspection, new BorderLayout());
        this.parent = parent;

        searchButton = new InspectorButton(inspection, new AbstractAction("Search...") {
            public void actionPerformed(ActionEvent actionEvent) {
                addSearchToolBar();
            }
        });
        searchButton.setText(null);
        final InspectorStyle style = preference().style();
        searchButton.setIcon(style.generalFindIcon());
        searchButton.setToolTipText("Open toolbar for searching");

        activeRowsButton = new InspectorButton(inspection, new AbstractAction(null, style.navigationForwardIcon()) {
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
        activeRowsButton.setText(null);
        activeRowsButton.setForeground(style.debugIPTagColor());
        activeRowsButton.setToolTipText("Scroll to next line with IP or Call Return");
        activeRowsButton.setEnabled(false);

        viewCloseButton =
            new InspectorButton(inspection(), "", "Close " + codeViewerKindName());
        viewCloseButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                parent().closeCodeViewer(CodeViewer.this);
            }
        });
        viewCloseButton.setIcon(style.codeViewCloseIcon());
    }

    protected void createView() {
        toolBarPanel = new InspectorPanel(inspection(), new GridLayout(0, 1));
        toolBar = new InspectorToolBar(inspection());
        toolBar.setBorder(preference().style().defaultPaneBorder());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBarPanel.add(toolBar);
        add(toolBarPanel, BorderLayout.NORTH);
    }

    private int[] searchMatchingRows = null;

    /**
     * @return the rows that match a current search session; null if no search session active.
     */
    protected final int[] getSearchMatchingRows() {
        return searchMatchingRows;
    }

    private final RowMatchNavigationListener rowMatchListener = new RowMatchNavigationListener() {

        public void setSearchResult(int[] result) {
            searchMatchingRows = result;
            // go to next matching row from current selection
            if (searchMatchingRows != null) {
                Trace.line(TRACE_VALUE, "search: matches " + searchMatchingRows.length + " = " + searchMatchingRows);
            }
            repaint();
        }

        public void selectNextResult() {
            setFocusAtNextSearchMatch();
        }

        public void selectPreviousResult() {
            setFocusAtPreviousSearchMatch();
        }

        public void closeRequested() {
            CodeViewer.this.closeSearch();
        }
    };

    private void addSearchToolBar() {
        if (searchToolBar == null) {
            searchToolBar = new RowTextSearchToolBar(inspection(), rowMatchListener, getRowTextSearcher());
            toolBarPanel.add(searchToolBar);
            parent().pack();
            searchToolBar.getFocus();
        }
    }

    private void closeSearch() {
        Trace.line(TRACE_VALUE, "search:  closing");
        toolBarPanel.remove(searchToolBar);
        parent().pack();
        searchToolBar = null;
        searchMatchingRows = null;
    }

    private void setFocusAtNextSearchMatch() {
        Trace.line(TRACE_VALUE, "search:  next match");
        if (searchMatchingRows.length > 0) {
            int currentRow = getSelectedRow();
            for (int row : searchMatchingRows) {
                if (row > currentRow) {
                    setFocusAtRow(row);
                    return;
                }
            }
            // wrap, could be optional, or dialog choice
            currentRow = -1;
            for (int row : searchMatchingRows) {
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
        if (searchMatchingRows.length > 0) {
            int currentRow = getSelectedRow();
            for (int index = searchMatchingRows.length - 1; index >= 0; index--) {
                final Integer matchingRow = searchMatchingRows[index];
                if (matchingRow < currentRow) {
                    setFocusAtRow(matchingRow);
                    return;
                }
            }
            // wrap, could be optional, or dialog choice
            currentRow = getRowCount();
            for (int index = searchMatchingRows.length - 1; index >= 0; index--) {
                final Integer matchingRow = searchMatchingRows[index];
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
    protected abstract RowTextMatcher getRowTextSearcher();

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
        toolBar().add(searchButton);
    }

    /**
     * Adds a button to the view's tool bar that enables navigation among "active" rows, those that correspond to
     * stack locations in the current thread.
     */
    protected void addActiveRowsButton() {
        toolBar().add(activeRowsButton);
    }

    /**
     * Adds a button to the view's tool bar that closes this view.
     */
    protected void addCodeViewCloseButton() {
        toolBar.add(viewCloseButton);
    }

    @Override
    public void refresh(boolean force) {
        updateCaches(force);
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
        parent.flash();
    }

    // Cached stack information, relative to this method, derived from the thread of current focus.
    // TODO (mlvdv) Generalize to account for the possibility of multiple stack frames associated with a single row.
    protected MaxStackFrame[] rowToStackFrame;


    /**
     * Rebuild the data in the cached stack information for the code view.
     */
    protected abstract void updateStackCache();

    // The thread from which the stack cache was last built.
    private MaxThread threadForCache = null;

    private MaxVMState lastRefreshedState = null;

    private void updateCaches(boolean force) {
        final MaxThread thread = focus().thread();
        if (thread != threadForCache || vm().state().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vm().state();
            updateStackCache();
            // Active rows depend on the stack cache.
            updateActiveRows();
            threadForCache = thread;
        }
    }

    /**
     * Returns stack frame, if any, associated with the row.
     */
    protected MaxStackFrame stackFrame(int row) {
        return rowToStackFrame[row];
    }

    /**
     * Is the machine code address at the row an instruction pointer
     * for a non-top frame of the stack of the thread that is the current focus?
     */
    protected boolean isCallReturn(int row) {
        final MaxStackFrame stackFrame = rowToStackFrame[row];
        return stackFrame != null && !stackFrame.isTop();
    }

    /**
     * Is the machine code address at the row an instruction pointer
     * for the top frame of the stack of the thread that is the current focus?
     */
    protected boolean isInstructionPointer(int row) {
        final MaxStackFrame stackFrame = rowToStackFrame[row];
        return stackFrame != null && stackFrame.isTop();
    }

    // Active rows are those for which there is an associated stack frame
    private List<Integer> activeRows = new ArrayList<Integer>(3);
    private int currentActiveRowIndex = -1;

    private void updateActiveRows() {
        activeRows.clear();
        for (int row = 0; row < rowToStackFrame.length; row++) {
            if (rowToStackFrame[row] != null) {
                activeRows.add(row);
            }
        }
        currentActiveRowIndex = -1;
        activeRowsButton.setEnabled(hasActiveRows());
    }

    /**
     * Does the method have any rows that are either the current instruction pointer or call return lines marked.
     */
    protected boolean hasActiveRows() {
        return activeRows.size() > 0;
    }

    /**
     * Cycles through the rows in the method that are either the current instruction pointer or call return lines marked.
     * Resets to the first after each refresh.
     */
    protected int nextActiveRow() {
        if (hasActiveRows()) {
            currentActiveRowIndex = (currentActiveRowIndex + 1) % activeRows.size();
            return activeRows.get(currentActiveRowIndex);
        }
        return -1;
    }

    /**
     * Is there a currently active search that matches the specified row?
     */
    protected boolean isSearchMatchRow(int row) {
        final int[] searchMatchingRows = getSearchMatchingRows();
        if (searchMatchingRows != null) {
            for (int matchingRow : searchMatchingRows) {
                if (row == matchingRow) {
                    return true;
                }
            }
        }
        return false;
    }
}
