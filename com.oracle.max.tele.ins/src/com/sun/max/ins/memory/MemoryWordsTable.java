/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

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
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying a range of memory words in the VM.
 */
public final class MemoryWordsTable extends InspectorTable {

    private final MemoryWordsTableModel tableModel;
    private final InspectorAction setOriginToSelectionAction;
    private final int nBytesInWord;

    MemoryWordsTable(Inspection inspection,
        MemoryWordRegion memoryWordRegion,
        Address origin,
        TableColumnVisibilityPreferences<MemoryColumnKind> instanceViewPreferences,
        InspectorAction setOriginToSelectionAction) {
        super(inspection);
        this.nBytesInWord = inspection.vm().platform().nBytesInWord();
        this.tableModel = new MemoryWordsTableModel(inspection, memoryWordRegion, origin);
        MemoryWordsColumnModel columnModel = new MemoryWordsColumnModel(this, this.tableModel, instanceViewPreferences);
        this.setOriginToSelectionAction = setOriginToSelectionAction;
        configureMemoryTable(tableModel, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1 && col == MemoryColumnKind.OFFSET.ordinal()) {
            setOriginToSelectionAction.perform();
        } else if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setRegionWatchpoint(tableModel.getMemoryRegion(row), null, null).perform();
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
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
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
        // TODO (mlvdv)  this doesn't work when origin != cell
        return vm().isValidOrigin(tableModel.getMemoryRegion(row).start().asPointer());
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

    private final class MemoryWordsColumnModel extends InspectorTableColumnModel<MemoryColumnKind> {

        private MemoryWordsColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, TableColumnVisibilityPreferences<MemoryColumnKind> instanceViewPreferences) {
            super(MemoryColumnKind.values().length, instanceViewPreferences);
            addColumn(MemoryColumnKind.TAG, new MemoryTagTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(MemoryColumnKind.ADDRESS, new AddressRenderer(inspection()), null);
            addColumn(MemoryColumnKind.WORD, new WordOffsetRenderer(inspection()), null);
            addColumn(MemoryColumnKind.OFFSET, new MemoryOffsetLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(MemoryColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(MemoryColumnKind.BYTES, new MemoryBytesTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(MemoryColumnKind.CHAR, new CharRenderer(inspection()), null);
            addColumn(MemoryColumnKind.UNICODE, new UnicodeRenderer(inspection()), null);
            addColumn(MemoryColumnKind.FLOAT, new FloatRenderer(inspection()), null);
            addColumn(MemoryColumnKind.DOUBLE, new DoubleRenderer(inspection()), null);
            addColumn(MemoryColumnKind.REGION, new MemoryRegionPointerTableCellRenderer(inspection(), table, tableModel), null);
        }
    }

    /**
     * Models a region of memory in the VM as a list of words, one per row.
     * A base address may also be specified, from which offsets are computed.
     * There is no requirement that the base address be in the memory region displayed.
     */
    private final class MemoryWordsTableModel extends InspectorMemoryTableModel {

        MemoryWordRegion memoryWordRegion;

        // Number of words offset from origin to beginning of region; may be negative.
        int positionBias;

        // Cache of memory descriptors for each row
        private final Map<Long, MaxMemoryRegion> addressToMemoryRegion = new HashMap<Long, MaxMemoryRegion>();

        public MemoryWordsTableModel(Inspection inspection, MemoryWordRegion memoryWordRegion, Address origin) {
            super(inspection, origin);
            setMemoryRegion(memoryWordRegion);
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
            return MemoryColumnKind.values().length;
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
            return "Memory word @ " + memoryWordRegion.getAddressAt(row).to0xHexString();
        }

    }

    private final class AddressRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per memory location and keep them around,
        // even though they may not always appear in the same row.
        private final Map<Long, WordValueLabel> addressToLabelMap = new HashMap<Long, WordValueLabel>();

        public AddressRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                final ValueMode labelValueMode = vm().isValidOrigin(address.asPointer()) ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection, labelValueMode, address, MemoryWordsTable.this);
                label.setToolTipPrefix("Memory word location<br>Address=");
                label.setOpaque(true);
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(cellBackgroundColor(isSelected));
            if (isBoundaryRow(row)) {
                label.setBorder(style().defaultPaneTopBorder());
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
            for (WordValueLabel label : addressToLabelMap.values()) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

    private final class WordOffsetRenderer extends LocationLabel.AsWordOffset implements TableCellRenderer {

        public WordOffsetRenderer(Inspection inspection) {
            super(inspection);
            setToolTipPrefix("Memory word location<br>Address= ");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor(isSelected));
            if (isBoundaryRow(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            return this;
        }
    }

    private final class ValueRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per memory location and keep them around,
        // even though they may not always appear in the same row.
        private final Map<Long, WordValueLabel> addressToLabelMap = new HashMap<Long, WordValueLabel>();

        public ValueRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                label = new WordValueLabel(inspection, ValueMode.WORD, MemoryWordsTable.this) {
                    @Override
                    public Value fetchValue() {
                        return vm().readWordValue(address);
                    }
                };
                label.setOpaque(true);
                label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value=");
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(cellBackgroundColor(isSelected));
            if (isBoundaryRow(row)) {
                label.setBorder(style().defaultPaneTopBorder());
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
            for (WordValueLabel label : addressToLabelMap.values()) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

    private final class CharRenderer extends DataLabel.ByteArrayAsChar implements TableCellRenderer {
        CharRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value as chars = ");
            final byte[] bytes = new byte[vm().platform().nBytesInWord()];
            vm().readBytes(address, bytes);
            if (isBoundaryRow(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor(isSelected));
            setValue(bytes);
            return this;
        }
    }

    private final class UnicodeRenderer extends DataLabel.ByteArrayAsUnicode implements TableCellRenderer {
        UnicodeRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value as Unicode = ");
            final byte[] bytes = new byte[vm().platform().nBytesInWord()];
            vm().readBytes(address, bytes);
            if (isBoundaryRow(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor(isSelected));
            setValue(bytes);
            return this;
        }
    }

    private final class FloatRenderer extends DataLabel.FloatAsText implements TableCellRenderer {
        FloatRenderer(Inspection inspection) {
            super(inspection, 0.0f);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value as float = ");
            final Value wordValue = vm().readWordValue(address);
            final float f = Float.intBitsToFloat((int) (wordValue.toLong() & 0xffffffffL));
            setValue(f);
            if (isBoundaryRow(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class DoubleRenderer extends DataLabel.DoubleAsText implements TableCellRenderer {
        DoubleRenderer(Inspection inspection) {
            super(inspection, 0.0d);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value as double = ");
            final Value wordValue = vm().readWordValue(address);
            final double f = Double.longBitsToDouble(wordValue.toLong());
            setValue(f);
            if (isBoundaryRow(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

}
