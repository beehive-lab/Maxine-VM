/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.view.*;
import com.sun.max.tele.*;

/**
 * A table specialized for use in the VM Inspector.
 * <p>
 * This table dispatches mouse events to table cell renderers, after first giving
 * implementations an opportunity to respond in the case of left
 * or middle-clicks.  A right click pops up a menu, if provided by
 * an implementation, over the cell where the click took place.
 * <p>
 * After all special handling has completed, the event is passed
 * along to the renderer for the cell where the click took place.
 * <p>
 * This table overrides the Swing display of row selection, which is
 * normally shown with an alternate background shading, using
 * instead a box drawn around the row.
 * <p>
 * Table cells can act as sources for drag and drop operations, but
 * only for <code>Copy</code> (not <code>Move</code>).  The request for something that can
 * be dragged is by default delegated to the specific table renderer,
 * if it is an instance of {@link InspectorLabel}. Subclasses can customize
 * how transferables are created by overriding {@link #getTransferable(int, int)}.
 */
public abstract class InspectorTable extends JTable implements Prober, InspectionHolder {

    public static final int MAXIMUM_ROWS_FOR_COMPUTING_COLUMN_WIDTHS = 100;

    /**
     *   Notification service for table based views that allow columns to be turned on and off.
     */
    public interface ColumnChangeListener {

        /**
         * Notifies that the set of visible columns has been changed.
         */
        void columnPreferenceChanged();
    }

    /**
     * Support for allowing table cells to be sources for Drag and Drop.
     * <br>
     * Only supports Copy from a cell (not Move).
     * <br>
     * Only supports outbound copy, i.e. Drag but not Drop.
     */
    private final class InspectorTableTransferHandler extends TransferHandler {
        private MouseEvent mouseEvent = null;

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        public void exportAsDrag(JComponent comp, InputEvent inputEvent, int action) {
            // This gets called when a drag sequence starts; cache the location.
            if (inputEvent instanceof MouseEvent) {
                mouseEvent = (MouseEvent) inputEvent;
            } else {
                mouseEvent = null;
            }
            super.exportAsDrag(comp, inputEvent, action);
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            // To get something that might be dragged from the
            // table cell where the request originated.
            InspectorTable table = InspectorTable.this;
            if (mouseEvent != null) {
                final Point p = mouseEvent.getPoint();
                final int col = table.columnAtPoint(p);
                final int row = table.rowAtPoint(p);
                return table.getTransferable(row, col);
            }
            return null;
        }
    }

    private final class InspectorTableMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(final MouseEvent mouseEvent) {
            final Point p = mouseEvent.getPoint();
            final int col = columnAtPoint(p);
            final int modelCol = getColumnModel().getColumn(col).getModelIndex();
            final int row = rowAtPoint(p);
            if ((col != -1) && (row != -1)) {
                switch(inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        // Give subclass an opportunity to handle a left-click specially.
                        mouseButton1Clicked(row, modelCol, mouseEvent);
                        break;
                    case MouseEvent.BUTTON2:
                        // Give subclass an opportunity to handle a middle-click specially.
                        mouseButton2Clicked(row, modelCol, mouseEvent);
                        break;
                    case MouseEvent.BUTTON3:
                        // Pop up a menu, if provided by subclass.
                        final InspectorPopupMenu popupMenu = getPopupMenu(row, modelCol, mouseEvent);
                        if (popupMenu != null) {
                            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        }
                        break;
                }
                // Locate the renderer under the event location and pass along the event.
                final TableCellRenderer tableCellRenderer = getCellRenderer(row, col);
                final Object cellValue = getValueAt(row, col);
                final Component component = tableCellRenderer.getTableCellRendererComponent(InspectorTable.this, cellValue, false, true, row, col);
                if (component != null) {
                    component.dispatchEvent(mouseEvent);
                }
            }
        }
    }

    private final Inspection inspection;
    private final String tracePrefix;

    private Set<ColumnChangeListener> columnChangeListeners = CiUtil.newIdentityHashSet();

    private MaxVMState lastRefreshedState;

    /**
     * Creates a new {@link JTable} for use in the {@link Inspection}.
     * <br>
     * Used only at this time by the two code viewers, all of which is
     * subject to further refactoring.
     *
     * @param model a model for the table
     * @param tableColumnModel a column model for the table
     */
    protected InspectorTable(Inspection inspection, InspectorTableModel inspectorTableModel, InspectorTableColumnModel inspectorTableColumnModel) {
        super(inspectorTableModel, inspectorTableColumnModel);
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        getTableHeader().setFont(inspection.preference().style().defaultFont());
        addMouseListener(new InspectorTableMouseListener());
        setDragEnabled(true);
        setTransferHandler(new InspectorTableTransferHandler());
    }

    /**
     * Creates a new {@link JTable} for use in the {@link Inspection}.
     */
    protected InspectorTable(Inspection inspection) {
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        getTableHeader().setFont(preference().style().defaultFont());
        addMouseListener(new InspectorTableMouseListener());
        setDragEnabled(true);
        setTransferHandler(new InspectorTableTransferHandler());
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM vm() {
        return inspection.vm();
    }

    public final InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionViews views() {
        return inspection.views();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public final InspectionPreferences preference() {
        return inspection.preference();
    }

    @Override
    public final void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (getRowSelectionAllowed()) {
            // Draw a box around the selected row in the table
            final int row = getSelectedRow();
            if (row >= 0) {
                g.setColor(preference().style().memorySelectedAddressBorderColor());
                g.drawRect(0, row * getRowHeight(row), getWidth() - 1, getRowHeight(row) - 1);
            }
        }
    }

    public final void refresh(boolean force) {
        MaxVMState maxVMState = vm().state();
        if (maxVMState.newerThan(lastRefreshedState) || force) {
            getInspectorTableModel().refresh();
            getInspectorTableColumnModel().refresh(force);
            lastRefreshedState = maxVMState;
            getTableHeader().setBackground(headerBackgroundColor());
            invalidate();
            repaint();
        }
        updateFocusSelection();
    }

    public final void redisplay() {
        getInspectorTableColumnModel().redisplay();
        invalidate();
        repaint();
    }

    /**
     * Adds a listener for view update when column visibility changes.
     */
    public final void addColumnChangeListener(ColumnChangeListener listener) {
        columnChangeListeners.add(listener);
    }

    /**
     * Remove a listener for view update when column visibility changed.
     */
    public final void removeColumnChangeListener(ColumnChangeListener listener) {
        columnChangeListeners.remove(listener);
    }

    public final InspectorTableColumnModel getInspectorTableColumnModel() {
        return (InspectorTableColumnModel) getColumnModel();
    }

    public final InspectorTableModel getInspectorTableModel() {
        return (InspectorTableModel) getModel();
    }

    /**
     * Scrolls the table to display the first row.
     */
    public final void scrollToBeginning() {
        scrollToRows(0, 0);
    }

    /**
     * Scrolls the table to display the last row.
     */
    public final void scrollToEnd() {
        final int lastRow = getRowCount() - 1;
        scrollToRows(lastRow, lastRow);
    }

    /**
     * Scrolls the table to display the specified range (with a few rows before or after if possible).
     * @param firstRow first row of the range that should be made visible
     * @param lastRow last row of the range that should be made visible
     */
    public final void scrollToRows(int firstRow, int lastRow) {
        assert firstRow <= lastRow;
        final int tableWidth = getWidth() - 2;
        final int rowHeight = getRowHeight() - 2;
        // Create a rectangle in the table view to use as a scroll target; include
        // the row immediately before and the row immediately after so that the row of interest
        // doesn't land at the very beginning or very end of the view, if possible.
        final int rowCount = lastRow - firstRow + 1 + 2;
        final Rectangle rectangle = new Rectangle(0, (firstRow - 1) * getRowHeight(), tableWidth, rowCount * rowHeight);
        // System.out.println("row=" + firstRow + " rect=" + rectangle);
        scrollRectToVisible(rectangle);
    }

    /**
     * Notifies listeners that the column visibility preferences for the table have changed.
     */
    public final void fireColumnPreferenceChanged() {
        ColumnChangeListener[] copy = columnChangeListeners.toArray(new ColumnChangeListener[columnChangeListeners.size()]);
        for (ColumnChangeListener listener : copy) {
            listener.columnPreferenceChanged();
        }
    }

    /**
     * Add tool tip text to the column headers, as specified by the column model.
     */
    @Override
    protected final JTableHeader createDefaultTableHeader() {
        return new JTableHeader(getColumnModel()) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final InspectorTableColumnModel inspectorTableColumnModel = getInspectorTableColumnModel();
                final int index = inspectorTableColumnModel.getColumnIndexAtX(p.x);
                final int modelIndex = inspectorTableColumnModel.getColumn(index).getModelIndex();
                return inspectorTableColumnModel.toolTipTextForColumn(modelIndex);
            }
        };
    }

    /**
     * Sets up default view configuration for tables.
     */
    protected final void configureDefaultTable(InspectorTableModel inspectorTableModel, InspectorTableColumnModel inspectorTableColumnModel) {
        setModel(inspectorTableModel);
        setColumnModel(inspectorTableColumnModel);
        final InspectorStyle style = preference().style();
        setShowHorizontalLines(style.defaultTableShowHorizontalLines());
        setShowVerticalLines(style.defaultTableShowVerticalLines());
        setIntercellSpacing(style.defaultTableIntercellSpacing());
        setRowHeight(style.defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this, MAXIMUM_ROWS_FOR_COMPUTING_COLUMN_WIDTHS);
        updateFocusSelection();
    }

    /**
     * Sets up standard view configuration for tables used to show memory in one way or another.
     */
    protected final void configureMemoryTable(InspectorTableModel inspectorTableModel, InspectorTableColumnModel inspectorTableColumnModel) {
        setModel(inspectorTableModel);
        setColumnModel(inspectorTableColumnModel);
        setFillsViewportHeight(true);
        final InspectorStyle style = preference().style();
        setShowHorizontalLines(style.memoryTableShowHorizontalLines());
        setShowVerticalLines(style.memoryTableShowVerticalLines());
        setIntercellSpacing(style.memoryTableIntercellSpacing());
        setRowHeight(style.memoryTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this, MAXIMUM_ROWS_FOR_COMPUTING_COLUMN_WIDTHS);
        updateFocusSelection();
    }

    /**
     * Updates table state to display a new request for row
     * selection; clears table selection if -1.
     */
    protected final void updateSelection(int row) {
        if (row < 0) {
            clearSelection();
        } else  if (row != getSelectedRow()) {
            setRowSelectionInterval(row, row);
        }
    }

    /**
     * Notification that some focus state of interest has changed, typically
     * causing the table's row selection to follow the new focus.
     */
    public void updateFocusSelection() {
    }


    /**
     * Gets an alternate background color for rendering a table cell, null for the default.
     * <p>
     * Note that this can get called very early in table initialization by superclasses.
     */
    public Color cellBackgroundColor() {
        return null;
    }

    @Override
    public Color getBackground() {
        final Color alternateBackgroundColor = cellBackgroundColor();
        return alternateBackgroundColor == null ? super.getBackground() : alternateBackgroundColor;
    }

    /**
     * Gets the appropriate foreground color for rendering a table cell, depending on the cell.
     * The default is null, which will default to the toolkit' settings
     *
     * @param row
     * @param column
     * @return a color to be used for foreground
     */
    public Color cellForegroundColor(int row, int column) {
        return null;
    }


    /**
     * Gets an alternate background color for rendering the table's header, null for the default.
     * <p>
     * Note that this can get called very early in table initialization by superclasses.
     */
    public Color headerBackgroundColor() {
        return null;
    }

    /**
     * Determines if a row should be treated as a "boundary", and an extra border be
     * drawn at the top of any cell rendered in that row.
     *
     * @param row a row in the table
     * @return whether the ros should be rendered as a boundary.
     */
    public boolean isBoundaryRow(int row) {
        return false;
    }

    /**
     * Gives table implementations an opportunity to respond specially to left-button clicks
     * over the table.  Default implementation of this method in null.
     * <br>
     * After this method is called, the left-button click is passed along to the renderer
     * for the cell under the mouse.
     * <br>
     * <strong>Note:</strong> When this method is called, the tables selection
     * listener has already processed the left-button click: the selection model
     * has been updated and any tables listening to the selection model will have
     * been notified.
     *
     * @param row row in the table model where the click took place
     * @param col column in the column model where the click took place
     * @param mouseEvent the originating event
     */
    protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
    }

    /**
     * Gives table implementations an opportunity to respond specially to middle-button clicks
     * over the table.  Default implementation of this method in null.
     * <br>
     * After this method is called, the middle-button click is passed along to the renderer
     * for the cell under the mouse.
     *
     * @param row row in the table model where the click took place
     * @param col column in the column model where the click took place
     * @param mouseEvent the originating event
     */    protected void mouseButton2Clicked(int row, int col, MouseEvent mouseEvent) {
    }

    /**
     * @param row row in the table model where the click took place
     * @param col column in the column model where the click took place
     * @param mouseEvent the originating event
     * @return a popup menu, null if none relevant to location and circumstances.
     */
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        return null;
    }

    /**
     * Delegates the request for something that can be transferred from a table cell (via drag
     * and drop - copy only) to the renderer for that cell.
     *
     * @param row row in the table where a drag is requested
     * @param col column in the table where a drag is requested
     * @return something that can be transferred (via copying) from
     * the specified cell; null if nothing can be transferred.
     */
    protected Transferable getTransferable(int row, int col) {
        final TableCellRenderer cellRenderer = getColumnModel().getColumn(col).getCellRenderer();
        Object value = getValueAt(row, col);
        if (cellRenderer != null) {
            final Component renderer = cellRenderer.getTableCellRendererComponent(this, value, false, false, row, col);
            if (renderer instanceof InspectorLabel) {
                final InspectorLabel inspectorLabel = (InspectorLabel) renderer;
                return inspectorLabel.getTransferable();
            }
        }
        return null;
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }
}
