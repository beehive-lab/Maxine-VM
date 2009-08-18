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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A table that displays Maxine array elements; for use in an instance of {@link ObjectInspector}.
 * Null array elements can be hidden from the display.
 * This table is somewhat complex so that it can serve for both ordinary array elements
 * as well as the various array subsets (tables) in hybrid objects (hubs).  They are distinguished
 * by a string prefix appearing before any mention of elements, e.g. [3] for an ordinary
 * array element and M[3] for an element of the MTable in a hub.
 *
 * @author Michael Van De Vanter
 */
public final class ArrayElementsTable extends InspectorTable {

    private final ObjectInspector objectInspector;
    private final Inspection inspection;
    private final TeleObject teleObject;
    private final Reference objectReference;  // Reference to an object is canonical; doesn't change
    private Pointer objectOrigin;  // Origin may change via GC
    private final Kind elementKind;
    private final int elementSize;
    private final Offset startOffset;
    private final int startIndex;
    private final int arrayLength;
    private final String indexPrefix;
    private final WordValueLabel.ValueMode wordValueMode;

    /** Maps display rows to element rows (indexes) in the table. */
    private int[] rowToElementIndex;
    private int visibleElementCount = 0;  // number of array elements being displayed

    private final ArrayElementsTableModel model;
    private final ArrayElementsTableColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    /**
     * A {@link JTable} specialized to display Maxine array elements.
     * This table is somewhat complex so that it can serve for both ordinary array elements
     * as well as the various array subsets (tables) in hybrid objects (hubs).
     *
     * @param objectInspector the parent of this component
     * @param elementKind the Maxine value "kind" of the array elements.
     * @param startOffset memory position relative to the object origin where the displayed array starts
     * @param startIndex index into the array where the display starts
     * @param length number of elements to display
     * @param indexPrefix text to prepend to the displayed name(index) of each element.
     * @param wordValueMode how to display word values, based on their presumed use in the VM.
     */
    ArrayElementsTable(final ObjectInspector objectInspector, final Kind elementKind, final Offset startOffset, int startIndex, int length, final String indexPrefix, WordValueLabel.ValueMode wordValueMode) {
        super(objectInspector.inspection());
        this.objectInspector = objectInspector;
        this.inspection = objectInspector.inspection();
        this.teleObject = objectInspector.teleObject();
        this.objectReference = teleObject.reference();
        this.elementKind = elementKind;
        this.elementSize = elementKind.width.numberOfBytes;
        this.startOffset = startOffset;
        this.startIndex = startIndex;
        this.arrayLength = length;
        this.indexPrefix = indexPrefix;
        this.wordValueMode = wordValueMode;

        // Initialize map so that all elements will display
        this.rowToElementIndex = new int[arrayLength];
        for (int index = 0; index < arrayLength; index++) {
            rowToElementIndex[index] = index;
        }
        this.visibleElementCount = arrayLength;

        this.model = new ArrayElementsTableModel();
        this.columns = new TableColumn[ArrayElementColumnKind.VALUES.length()];
        this.columnModel = new ArrayElementsTableColumnModel(objectInspector);
        configureMemoryTable(model, columnModel);
        setFillsViewportHeight(true);
    }

    @Override
    protected InspectorMenu getDynamicMenu(int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled()) {
            final InspectorMenu menu = new InspectorMenu();
            menu.add(actions().setArrayElementWatchpoint(teleObject, elementKind, startOffset, model.rowToElementIndex(row), indexPrefix, "Watch this array element"));
            menu.add(actions().setObjectWatchpoint(teleObject, "Watch this array's memory"));
            menu.add(new WatchpointSettingsMenu(model.getWatchpoint(row)));
            menu.add(actions().removeWatchpoint(model.getMemoryRegion(row), "Remove memory watchpoint"));
            return menu;
        }
        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < model.getRowCount()) {
                inspection().focus().setAddress(model.getAddress(row));
            }
        }
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            objectOrigin = teleObject.getCurrentOrigin();
            // Update the mapping between array elements and displayed rows.
            if (teleObject.isLive()) {
                if (objectInspector.hideNullArrayElements()) {
                    final int previousVisibleCount = visibleElementCount;
                    visibleElementCount = 0;
                    for (int index = 0; index < arrayLength; index++) {
                        if (!maxVM().getElementValue(elementKind, objectReference, index).isZero()) {
                            rowToElementIndex[visibleElementCount++] = index;
                        }
                    }
                    if (previousVisibleCount != visibleElementCount) {
                        model.fireTableDataChanged();
                    }
                } else {
                    if (visibleElementCount != arrayLength) {
                        // Previously hiding but no longer; reset map
                        for (int index = 0; index < arrayLength; index++) {
                            rowToElementIndex[index] = index;
                        }
                        visibleElementCount = arrayLength;
                    }
                }
                // Update selection, based on global address focus.
                final int oldSelectedRow = getSelectedRow();
                final int newRow = model.findRow(focus().address());
                if (newRow >= 0) {
                    getSelectionModel().setSelectionInterval(newRow, newRow);
                } else {
                    if (oldSelectedRow >= 0) {
                        getSelectionModel().clearSelection();
                    }
                }
                //
                for (TableColumn column : columns) {
                    final Prober prober = (Prober) column.getCellRenderer();
                    prober.refresh(force);
                }
            }
        }
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    /**
     * Add tool tip text to the column headers, as specified by {@link ArrayElementsColumnKind}.
     *
     * @see javax.swing.JTable#createDefaultTableHeader()
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return ArrayElementColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    /**
     * Models (a possible subset of) words/rows in a sequence of array elements;
     * the value of each cell is simply the index into the array
     * elements being displayed.
     */
    private final class ArrayElementsTableModel extends AbstractTableModel implements InspectorMemoryTableModel {

        public int getColumnCount() {
            return ArrayElementColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return visibleElementCount;
        }

        public Object getValueAt(int row, int col) {
            return rowToElementIndex[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return Integer.class;
        }

        /**
         * @param row index of a displayed row in the table
         * @return the memory location of the displayed element in the VM.
         */
        public Address getAddress(int row) {
            return objectOrigin.plus(getOffset(row)).asAddress();
        }

        public MemoryRegion getMemoryRegion(int row) {
            return new FixedMemoryRegion(getAddress(row), Size.fromInt(elementSize), "");
        }

        /**
         * @return the memory watchpoint, if any, that is active at a row
         */
        public MaxWatchpoint getWatchpoint(int row) {
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (watchpoint.contains(getAddress(row))) {
                    return watchpoint;
                }
            }
            return null;
        }

        public Address getOrigin() {
            return objectOrigin;
        }

        /**
         * @param row index of a displayed row in the table
         * @return the offset in memory of the displayed element, relative to the object origin.
         */
        public Offset getOffset(int row) {
            return startOffset.plus(rowToElementIndex[row] * elementSize);
        }

        /**
         * @param address a memory address in the VM.
         * @return the displayed table row that shows the array element at an address;
         * -1 if the address is not in the array, or if that element is currently hidden..
         */
        public int findRow(Address address) {
            if (!address.isZero()) {
                final int offset = address.minus(objectOrigin).minus(startOffset).toInt();
                if (offset >= 0 && offset < arrayLength * elementSize) {
                    final int elementRow = offset / elementSize;
                    for (int row = 0; row < visibleElementCount; row++) {
                        if (rowToElementIndex[row] == elementRow) {
                            return elementRow;
                        }
                    }
                }
            }
            return -1;
        }

        public int rowToElementIndex(int row) {
            return rowToElementIndex[row];
        }
    }

    /**
     * A column model for array elements, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ArrayElementsTableColumnModel extends DefaultTableColumnModel {

        ArrayElementsTableColumnModel(ObjectInspector objectInspector) {
            createColumn(ArrayElementColumnKind.TAG, new TagRenderer(), true);
            createColumn(ArrayElementColumnKind.ADDRESS, new AddressRenderer(), objectInspector.showAddresses());
            createColumn(ArrayElementColumnKind.OFFSET, new PositionRenderer(), objectInspector.showOffsets());
            createColumn(ArrayElementColumnKind.NAME, new NameRenderer(), true);
            createColumn(ArrayElementColumnKind.VALUE, new ValueRenderer(), true);
            createColumn(ArrayElementColumnKind.REGION, new RegionRenderer(), objectInspector.showMemoryRegions());
        }

        private void createColumn(ArrayElementColumnKind columnKind, TableCellRenderer renderer, boolean isVisible) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (isVisible) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && model.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(model.getMemoryRegion(row), focus().thread(), model.getWatchpoint(row));
            renderer.setForeground(getRowTextColor(row));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.getOffset(row), objectOrigin);
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.getOffset(row), objectOrigin);
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class NameRenderer extends LocationLabel.AsIndex implements TableCellRenderer {

        public NameRenderer() {
            super(inspection, indexPrefix, 0, Offset.zero(), Address.zero());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(row, model.getOffset(row), objectOrigin);
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[arrayLength];

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            final int elementIndex = model.rowToElementIndex(row);
            InspectorLabel label = labels[elementIndex];
            if (label == null) {
                if (elementKind == Kind.REFERENCE) {
                    label = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, ArrayElementsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return maxVM().getElementValue(elementKind, objectReference, startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                } else if (elementKind == Kind.WORD) {
                    label = new WordValueLabel(inspection, wordValueMode, ArrayElementsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return maxVM().getElementValue(elementKind, objectReference, startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                } else {
                    label = new PrimitiveValueLabel(inspection, elementKind) {
                        @Override
                        public Value fetchValue() {
                            return maxVM().getElementValue(elementKind, objectReference, startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                }
                labels[elementIndex] = label;
            }
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[arrayLength];

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            final int elementIndex = model.rowToElementIndex(row);
            InspectorLabel label = labels[elementIndex];
            if (label == null) {
                label = new MemoryRegionValueLabel(inspection) {
                    @Override
                    public Value fetchValue() {
                        return maxVM().getElementValue(elementKind, objectReference, startIndex + elementIndex);
                    }
                };
                labels[elementIndex] = label;
            }
            return label;
        }
    }


}
