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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays VM array elements; for use in an instance of {@link ObjectView}.
 * <p>
 * Null array elements can be hidden from the display.
 * <p>
 * This table is somewhat complex so that it can serve for both ordinary array elements
 * as well as the various array subsets (tables) in hybrid objects (hubs).  They are distinguished
 * by a string prefix appearing before any mention of elements, e.g. [3] for an ordinary
 * array element and M[3] for an element of the MTable in a hub.
 */
public final class ArrayElementsTable extends InspectorTable {

    private static final int TRACE_LEVEL = 1;

    private final ObjectView objectView;
    private final Kind elementKind;
    private final TypeDescriptor elementTypeDescriptor;
    private final int startOffset;
    private final int startIndex;
    private final int arrayLength;
    private final String indexPrefix;
    private final WordValueLabel.ValueMode wordValueMode;

    private final ArrayElementsTableModel tableModel;

    private final List<InspectorAction> extraViewMenuActions = new ArrayList<InspectorAction>();

    /**
     * A table specialized for the display VM array elements.
     * This table is somewhat complex so that it can serve for both ordinary array elements
     * as well as the various array subsets (tables) in hybrid objects (hubs).
     *
     * @param elementKind the VM value "kind" of the array elements.
     * @param startOffset memory position relative to the object origin where the displayed array starts
     * @param startIndex index into the array where the display starts
     * @param length number of elements to display
     * @param indexPrefix text to prepend to the displayed name(index) of each element.
     * @param wordValueMode how to display word values, based on their presumed use in the VM.
     */
    ArrayElementsTable(Inspection inspection,
                    ObjectView objectView, final Kind elementKind, final TypeDescriptor elementTypeDescriptor, final int startOffset,
                    int startIndex, int length, final String indexPrefix, WordValueLabel.ValueMode wordValueMode) {
        super(inspection);
        this.objectView = objectView;
        this.elementKind = elementKind;
        this.elementTypeDescriptor = elementTypeDescriptor;
        this.startOffset = startOffset;
        this.startIndex = startIndex;
        this.arrayLength = length;
        this.indexPrefix = indexPrefix;
        this.wordValueMode = wordValueMode;

        this.tableModel = new ArrayElementsTableModel(inspection, objectView.object().origin());
        ArrayElementsTableColumnModel columnModel = new ArrayElementsTableColumnModel(this, this.tableModel, objectView.viewPreferences());
        configureMemoryTable(tableModel, columnModel);

        this.extraViewMenuActions.add(scrollToBeginningAction());
        this.extraViewMenuActions.add(new ScrollToIndexAction(inspection));
        this.extraViewMenuActions.add(scrollToEndAction());

        setFillsViewportHeight(true);
        updateFocusSelection();
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setArrayElementWatchpoint(objectView.object(), elementKind, startOffset, tableModel.rowToElementIndex(row), indexPrefix, null).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            };
            toggleAction.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null) {
            final MaxObject object = objectView.object();
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setArrayElementWatchpoint(object, elementKind, startOffset, tableModel.rowToElementIndex(row), indexPrefix, null).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            });
            menu.add(actions().setArrayElementWatchpoint(object, elementKind, startOffset, tableModel.rowToElementIndex(row), indexPrefix, "Watch this array element"));
            menu.add(actions().setObjectWatchpoint(object, "Watch this array's memory"));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = focus().address();
        updateSelection(tableModel.findRow(address));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int column) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return preference().style().debugIPTagColor();
        }
        return null;
    }


    @Override
    public Color cellBackgroundColor() {
        return objectView == null ? null : objectView.viewBackgroundColor();
    }

    @Override
    public Color headerBackgroundColor() {
        return cellBackgroundColor();
    }

    @Override
    public List<InspectorAction> extraViewMenuActions() {
        return extraViewMenuActions;
    }

    @Override
    public boolean isElided() {
        return objectView.viewPreferences().elideNullArrayElements();
    }

    public ObjectView getView() {
        return objectView;
    }

    /**
     * A column model for array elements, to be used in an {@link ObjectView}.
     * Column selection is driven by choices in the parent {@link ObjectView}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ArrayElementsTableColumnModel extends InspectorTableColumnModel<ObjectColumnKind> {

        ArrayElementsTableColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, ObjectViewPreferences viewPreferences) {
            super(ObjectColumnKind.values().length, viewPreferences);
            addColumn(ObjectColumnKind.TAG, new MemoryTagTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.ADDRESS, new MemoryAddressLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.OFFSET, new MemoryOffsetLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.TYPE,  new MemoryContentsTypeTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ObjectColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(ObjectColumnKind.BYTES,  new MemoryBytesTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.REGION, new MemoryRegionPointerTableCellRenderer(inspection(), table, tableModel), null);
        }
    }

    /**
     * Models (a possible subset of) words/rows in a sequence of array elements;
     * the value of each cell is simply the index into the array
     * elements being displayed.
     * <br>
     * The origin of the model is the current origin of the object in memory that includes these elements,
     * which may change after GC.
     */
    private final class ArrayElementsTableModel extends InspectorMemoryTableModel {

        private final int nBytesInElement;

        /**
         * Map: display {@code row --> element index}.
         * <p>
         * When null elements are not being hidden then {@code row == elementIndex}
         */
        private final int[] rowToElementIndex;


        private int visibleElementCount = 0;  // number of array elements being displayed

        public ArrayElementsTableModel(Inspection inspection, Address origin) {
            super(inspection, origin);
            this.nBytesInElement = elementKind.width.numberOfBytes;
            this.rowToElementIndex = new int[arrayLength];
            fillRowToElementIndexMap();
            this.visibleElementCount = arrayLength;
        }

        public int getColumnCount() {
            return ObjectColumnKind.values().length;
        }

        public int getRowCount() {
            return visibleElementCount;
        }

        public Object getValueAt(int row, int col) {
            return rowToElementIndex(row);
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return Integer.class;
        }

        @Override
        public Address getAddress(int row) {
            return getOrigin().plus(getOffset(row)).asAddress();
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            return new InspectorMemoryRegion(vm(), "", getAddress(row), nBytesInElement);
        }

        @Override
        public int getOffset(int row) {
            return startOffset + (rowToElementIndex(row) * nBytesInElement);
        }

        /**
         * @param address a memory address in the VM.
         * @return the displayed table row that shows the array element at an address;
         * -1 if the address is not in the array, or if that element is currently hidden..
         */
        @Override
        public int findRow(Address address) {
            if (address.isNotZero()) {
                final int offset = address.minus(getOrigin()).minus(startOffset).toInt();
                if (offset >= 0 && offset < arrayLength * nBytesInElement) {
                    final int elementIndex = offset / nBytesInElement;
                    for (int row = 0; row < visibleElementCount; row++) {
                        if (rowToElementIndex(row) == elementIndex) {
                            return row;
                        }
                    }
                }
            }
            return -1;
        }

        @Override
        public String getRowDescription(int row) {
            return "Array element " + rowToElementIndex(row);
        }

        @Override
        public TypeDescriptor getRowType(int row) {
            return elementTypeDescriptor;
        }

        public int rowToElementIndex(int row) {
            if (objectView.viewPreferences().elideNullArrayElements()) {
                return rowToElementIndex[row];
            }
            return row;
        }

        public int findRow(int elementIndex) {
            if (objectView.viewPreferences().elideNullArrayElements()) {
                return Arrays.binarySearch(rowToElementIndex, elementIndex);
            }
            return elementIndex;
        }

        public int findClosestRow(int elementIndex) {
            if (isElided()) {
                int closestDistance = Integer.MAX_VALUE;
                int closestRow = -1;
                for (int row = 0; row < visibleElementCount; row++) {
                    final int diff = Math.abs(elementIndex - rowToElementIndex[row]);
                    if (diff == 0) {
                        return row;
                    }
                    if (diff < closestDistance) {
                        closestDistance = diff;
                        closestRow = row;
                    }
                }
                return closestRow;
            }
            return elementIndex;
        }

        @Override
        public void refresh() {
            final MaxObject object = objectView.object();
            setOrigin(object.origin());
            // Update the mapping between array elements and displayed rows.
            if (object.status().isNotDead()) {
                fillRowToElementIndexMap();
                super.refresh();
            }
        }

        private void fillRowToElementIndexMap() {
            if (isElided()) {
                visibleElementCount = 0;
                for (int elementIndex = 0; elementIndex < arrayLength; elementIndex++) {
                    if (!vm().memoryIO().readArrayElementValue(elementKind,  objectView.object().reference(), elementIndex).isZero()) {
                        rowToElementIndex[visibleElementCount++] = elementIndex;
                    }
                }
                for (int elementIndex = visibleElementCount; elementIndex < arrayLength; elementIndex++) {
                    rowToElementIndex[elementIndex] = arrayLength;
                }
            } else {
                for (int elementIndex = 0; elementIndex < arrayLength; elementIndex++) {
                    rowToElementIndex[elementIndex] = elementIndex;
                }
                visibleElementCount = arrayLength;
            }
        }
    }

    private final class NameRenderer extends LocationLabel.AsIndex implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection, indexPrefix, 0, 0, Address.zero());
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
           // final int index = tableModel.rowToElementIndex(row);
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>address = ");
            setValue(tableModel.rowToElementIndex(row), tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private final Inspection inspection;

        public ValueRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

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
            final int elementIndex = tableModel.rowToElementIndex(row);
            if (labels[elementIndex] == null) {
                final MaxObject object = objectView.object();
                if (elementKind.isReference) {
                    labels[elementIndex] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, ArrayElementsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return vm().memoryIO().readArrayElementValue(elementKind,  object.reference(), startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                } else if (elementKind.isWord) {
                    labels[elementIndex] = new WordValueLabel(inspection, wordValueMode, ArrayElementsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return vm().memoryIO().readArrayElementValue(elementKind,  object.reference(), startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                } else {
                    labels[elementIndex] = new PrimitiveValueLabel(inspection, elementKind) {
                        @Override
                        public Value fetchValue() {
                            return vm().memoryIO().readArrayElementValue(elementKind,  object.reference(), startIndex + elementIndex);
                        }
                        @Override
                        public void updateText() {
                            super.updateText();
                            ArrayElementsTable.this.repaint();
                        }
                    };
                }
                labels[elementIndex].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>value = ");
                labels[elementIndex].setOpaque(true);
            }
            labels[elementIndex].setBackground(cellBackgroundColor());
            return labels[elementIndex];
        }
    }

    private final class ScrollToIndexAction extends InspectorAction {

        public ScrollToIndexAction(Inspection inspection) {
            super(inspection, "Show array index...");
        }

        @Override
        protected void procedure() {
            int elementIndex = -1;
            final String input = gui().inputDialog("Show index:", "0");
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                elementIndex = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
                return;
            }
            if (elementIndex < startIndex || elementIndex >= startIndex + arrayLength) {
                gui().errorMessage("Array index " + Integer.toString(elementIndex) + " out of range");
                return;
            }
            final int row = tableModel.findClosestRow(elementIndex);
            if (row < 0) {
                gui().errorMessage("Element " + Integer.toString(elementIndex) + " elided, value is null");
                return;
            } else if (row != elementIndex) {
                gui().warningMessage("Element " + Integer.toString(elementIndex) + " elided (null value), scrolling to closest element");
            }
            scrollToRows(row, row);
            final Address rowAddress = tableModel.getAddress(row);
            Trace.line(TRACE_LEVEL, tracePrefix() + "scrolling/selecting row=" + row + ", index=" + elementIndex + ", addr=" + rowAddress.to0xHexString());

            if (rowAddress.isNotZero()) {
                inspection().focus().setAddress(rowAddress);
            }
        }
    }


}
