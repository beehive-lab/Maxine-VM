/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.debug;

import static com.sun.max.tele.MaxMarkBitmap.MarkColor.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMarkBitmap.MarkColor;
import com.sun.max.unsafe.*;

/**
 * A table specialized for displaying a range of memory words in the VM, annotated
 * by data from a {@link MaxMarkBitmap}.
 */
public final class MemoryColoringTable extends InspectorTable {

    private InspectorView view;
    private final MaxMarkBitmap markBitmap;
    private final MarkBitmapTableModel tableModel;
    private final int nBytesInWord;

    public MemoryColoringTable(Inspection inspection,
        InspectorView view,
        MaxMarkBitmap markBitmap,
        MarkBitmapViewPreferences instanceViewPreferences) {
        super(inspection);
        this.view = view;
        this.markBitmap = markBitmap;
        this.nBytesInWord = inspection.vm().platform().nBytesInWord();
        this.tableModel = new MarkBitmapTableModel(inspection, markBitmap);
        MemoryColoringColumnModel columnModel = new MemoryColoringColumnModel(this, this.tableModel, instanceViewPreferences);
        configureMemoryTable(tableModel, columnModel);
    }

//    @Override
//    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
//        if (mouseEvent.getClickCount() == 1 && col == MarkBitmapColumnKind.OFFSET.ordinal()) {
//            setOriginToSelectionAction.perform();
//        } else if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
//            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {
//
//                @Override
//                public MaxWatchpoint setWatchpoint() {
//                    actions().setRegionWatchpoint(tableModel.getMemoryRegion(row), null, null).perform();
//                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
//                    if (!watchpoints.isEmpty()) {
//                        return watchpoints.get(0);
//                    }
//                    return null;
//                }
//            };
//            toggleAction.perform();
//        }
//    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, int col, MouseEvent mouseEvent) {
        final InspectorPopupMenu menu = new InspectorPopupMenu();
        if (vm().watchpointManager() != null) {
            final MaxMemoryRegion memoryRegion = tableModel.getMemoryRegion(row);
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setRegionWatchpoint(tableModel.getMemoryRegion(row), null, null).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            });
            menu.add(actions().setRegionWatchpoint(memoryRegion, "Watch this memory word", null));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
        }
        menu.add(views().memoryBytes().makeViewAction(tableModel.getAddress(row), "View this memory as bytes"));
        return menu;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = focus().address();
        updateSelection(tableModel.findCoveringRow(address));
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
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return preference().style().debugIPTagColor();
        }
        return null;
    }

    /**
     * {@inheritDoc}.
     * <br>
     * Treat a row as a boundary if it appears to be the first word in an object cell.
     */
    @Override
    public boolean isBoundaryRow(int row) {
        // TODO (mlvdv)  this doesn't work when origin != cell, i.e. with layouts other than OHM
        return vm().objects().objectStatusAt(tableModel.getMemoryRegion(row).start()).isLive();
    }

    public InspectorView getView() {
        return view;
    }

    /**
     * Changes the area of memory being displayed.
     */
    void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
        tableModel.setMemoryRegion(memoryWordRegion);
        updateFocusSelection();
    }

    /**
     * Changes the origin used to computing offsets in the memory being displayed.
     * @param origin
     */
    void setOrigin(Address origin) {
        tableModel.setOrigin(origin);
    }

    void scrollToOrigin() {
        final int row = tableModel.findRow(tableModel.getOrigin());
        scrollToRows(row, row);
    }

    void scrollToAddress(Address address) {
        if (address == null || address.isZero()) {
            return;
        }
        final int row = tableModel.findRow(address);
        if (row >= 0) {
            scrollToRows(row, row);
        }
    }

    void scrollToRange(Address first, Address last) {
        scrollToRows(tableModel.findRow(first), tableModel.findRow(last));
    }

    private final class MemoryColoringColumnModel extends InspectorTableColumnModel<MarkBitmapColumnKind> {

        private MemoryColoringColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, TableColumnVisibilityPreferences<MarkBitmapColumnKind> instanceViewPreferences) {
            super(MarkBitmapColumnKind.values().length,  instanceViewPreferences);
            addColumn(MarkBitmapColumnKind.MAP_BIT_INDEX, new MapBitIndexRenderer(inspection()), null);
            addColumn(MarkBitmapColumnKind.WORD_INDEX, new WordIndexRenderer(inspection()), null);
            addColumn(MarkBitmapColumnKind.BITMAP_WORD_ADDRESS, new BitmapWordAddressRenderer(inspection()), null);
            addColumn(MarkBitmapColumnKind.WORD_BIT_INDEX, new WordBitIndexRenderer(inspection()), null);
            addColumn(MarkBitmapColumnKind.MARK_BIT, new MarkBitRenderer(inspection()), null);
            addColumn(MarkBitmapColumnKind.HEAP_ADDRESS, new CoveredAddressRenderer(inspection()), null);
        }
    }

    /**
     * Models a region of memory in the VM as a list of words, one per row.
     * A base address may also be specified, from which offsets are computed.
     * There is no requirement that the base address be in the memory region displayed.
     */
    private final class MarkBitmapTableModel extends InspectorMemoryTableModel {

        private MemoryWordRegion memoryWordRegion;

        // Number of words offset from origin to beginning of region; may be negative.
        int positionBias;

        // Cache of memory descriptors for each row
        private final Map<Long, MaxMemoryRegion> addressToMemoryRegion = new HashMap<Long, MaxMemoryRegion>();

        public MarkBitmapTableModel(Inspection inspection, MaxMarkBitmap markBitmap) {
            super(inspection, markBitmap.coveredMemoryRegion().start());
            setMemoryRegion(new MemoryWordRegion(inspection.vm(), markBitmap.coveredMemoryRegion()));
            //positionBias = memoryWordRegion.start().minus(origin).dividedBy(getWordSize()).toInt();
        }

        @Override
        public void setOrigin(Address origin) {
            assert origin.isAligned(nBytesInWord);
            super.setOrigin(origin);
        }

        void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
            this.memoryWordRegion = memoryWordRegion;
            update();
        }

        @Override
        protected void update() {
            positionBias = memoryWordRegion.start().minus(getOrigin()).dividedBy(nBytesInWord).toInt();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return MarkBitmapColumnKind.values().length;
        }

        public int getRowCount() {
            if (memoryWordRegion == null) {
                return 0;
            }
            long nWords = memoryWordRegion.nWords();
            assert nWords < Integer.MAX_VALUE;
            return (int) nWords;
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return Integer.class;
        }

        @Override
        public Address getAddress(int row) {
            return memoryWordRegion.getAddressAt(row);
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            final Address address = memoryWordRegion.getAddressAt(row);
            MaxMemoryRegion rowMemoryRegion = addressToMemoryRegion.get(address.toLong());
            if (rowMemoryRegion == null) {
                rowMemoryRegion = new MemoryWordRegion(vm(), address, 1);
                addressToMemoryRegion.put(address.toLong(), rowMemoryRegion);
            }
            return rowMemoryRegion;
        }

        @Override
        public int getOffset(int row) {
            return (row  + positionBias) * nBytesInWord;
        }

        @Override
        public int findRow(Address address) {
            return memoryWordRegion.indexAt(address);
        }

        @Override
        public String getRowDescription(int row) {
            // Don't try to cache these at creation, as we sometimes do with other tables, so that
            // we can view arbitrarily large regions.
            return "Mark bit index=" + row + ", covers heap word@" + getAddress(row).to0xHexString();
        }

        /**
         * Finds the row in the table, if any, whose mark bit covers a particular address.
         *
         * @param address a VM memory address, presumed to be in the heap covered by the mark bitmap
         * @return the row (i.e. a bitIndex in the map) that covers the address, {@code -1} if none.
         */
        public int findCoveringRow(Address address) {
            return markBitmap.getBitIndexOf(address);
        }

    }

    private final class MapBitIndexRenderer extends InspectorLabel implements TableCellRenderer {

        private int n;

        public MapBitIndexRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
            redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setText(Integer.toString(row));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipHtmlText("bit index = " + intTo0xHex(row));
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            if (isBoundaryRow(row)) {
                setBorder(preference().style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            return this;
        }

        public void refresh(boolean force) {
        }

        public void redisplay() {
            setFont(preference().style().decimalDataFont());
        }
    }

    private final class WordIndexRenderer extends InspectorLabel implements TableCellRenderer {

        private int n;

        public WordIndexRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
            redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setText(Integer.toString(markBitmap.bitmapWordIndex(row)));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipHtmlText("bit index = " + intTo0xHex(row));
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            if (isBoundaryRow(row)) {
                setBorder(preference().style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            return this;
        }

        public void refresh(boolean force) {
        }

        public void redisplay() {
            setFont(preference().style().decimalDataFont());
        }
    }

    private final class BitmapWordAddressRenderer extends InspectorLabel implements TableCellRenderer {


        public BitmapWordAddressRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
            redisplay();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = markBitmap.bitmapWordAddress(row);
            setText(address.toPaddedHexString('0'));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipHtmlText("Bitmap word@" + address.to0xHexString() + " contains mark bit");
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            if (isBoundaryRow(row)) {
                setBorder(preference().style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            return this;
        }

        public void redisplay() {
            setFont(preference().style().hexDataFont());
        }

        public void refresh(boolean force) {
        }
    }

    private final class WordBitIndexRenderer extends InspectorLabel implements TableCellRenderer {

        private int n;

        public WordBitIndexRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
            redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setText(Integer.toString(markBitmap.getBitIndexInWord(row)));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipHtmlText("bit index = " + intTo0xHex(row));
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            if (isBoundaryRow(row)) {
                setBorder(preference().style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            return this;
        }

        public void refresh(boolean force) {
        }

        public void redisplay() {
            setFont(preference().style().decimalDataFont());
        }
    }


    private final class MarkBitRenderer extends PlainLabel implements TableCellRenderer  {

        private final Inspection inspection;

        MarkBitRenderer(Inspection inspection) {
            super(inspection, "x");
            this.inspection = inspection;
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Address address = tableModel.getAddress(row);
            Color backgroundColor = cellBackgroundColor();
            Color foregroundColor = cellForegroundColor(row, column);
            final String bitValueText = markBitmap.isBitSet(row) ? "1" : "0";
            String labelText = bitValueText;
            MaxObject coveredObject = null;
            final InspectorStyle style = inspection().preference().style();
            // Everything now set to default; i.e. where there is no mark
            try {
                coveredObject = vm().objects().findObjectAt(address);
            } catch (MaxVMBusyException e) {
            }
            // Is this the first bit of a mark?
            MarkColor markColor = markBitmap.getMarkColor(row);
            if (markColor == null && row > 0) {
                // Is this the second bit of a mark?  If so, render the cell with the same style as as the first bit
                markColor = markBitmap.getMarkColor(row - 1);
            }
            if (markColor != null) {
                switch(markColor) {
                    case MARK_WHITE:
                        backgroundColor = style.markedWhiteBackgroundColor();
                        foregroundColor = Color.BLACK;
                        break;
                    case MARK_GRAY:
                        backgroundColor = style.markedGrayBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_BLACK:
                        backgroundColor = style.markedBlackBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_INVALID:
                        backgroundColor = style.markInvalidBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_UNAVAILABLE:
                        labelText = inspection().nameDisplay().unavailableDataShortText();
                        break;
                }
            } else if (markBitmap.isBitSet(row)) {
                // Not a valid location for a mark bit; shouldn't be set
                backgroundColor = style.markInvalidBackgroundColor();
                foregroundColor = Color.WHITE;
                markColor = MARK_INVALID;
            }
            setText(labelText);
            setForeground(foregroundColor);
            setBackground(backgroundColor);

            if (isBoundaryRow(row)) {
                setBorder(preference().style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setFont(preference().style().defaultFont());

            final StringBuilder ttBuilder = new StringBuilder();
            ttBuilder.append("Mark bit value=").append(bitValueText);
            if (markColor != null) {
                ttBuilder.append(", object mark=").append(markColor);
            }
            if (coveredObject != null) {
                ttBuilder.append("<br>Covered object:  ");
                ttBuilder.append(htmlify(inspection().nameDisplay().referenceToolTipText(coveredObject)));
            }
            setWrappedToolTipHtmlText(ttBuilder.toString());
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            return this;
        }
    }

    private final class CoveredAddressRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per memory location and keep them around,
        // even though they may not always appear in the same row.
        private final Map<Long, WordValueLabel> addressToLabelMap = new HashMap<Long, WordValueLabel>();

        public CoveredAddressRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                label = new WordValueLabel(inspection, ValueMode.WORD, address, MemoryColoringTable.this, false);
                label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
                label.setOpaque(true);
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(cellBackgroundColor());
            if (isBoundaryRow(row)) {
                label.setBorder(preference().style().defaultPaneTopBorder());
            } else {
                label.setBorder(null);
            }
            return label;
        }

        public void redisplay() {
            for (WordValueLabel label : addressToLabelMap.values()) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
        }
    }

}
