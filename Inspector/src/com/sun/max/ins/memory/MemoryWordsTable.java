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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying a range of memory words in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryWordsTable extends InspectorTable {

    private final MemoryWordsTableModel tableModel;
    private final MemoryWordsColumnModel columnModel;
    private final InspectorAction setOriginToSelectionAction;

    MemoryWordsTable(Inspection inspection,
        MemoryWordRegion memoryWordRegion,
        Address origin,
        TableColumnVisibilityPreferences<MemoryWordsColumnKind> instanceViewPreferences,
        InspectorAction setOriginToSelectionAction) {
        super(inspection);
        tableModel = new MemoryWordsTableModel(inspection, memoryWordRegion, origin);
        columnModel = new MemoryWordsColumnModel(instanceViewPreferences);
        this.setOriginToSelectionAction = setOriginToSelectionAction;
        configureMemoryTable(tableModel, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1 && col == MemoryWordsColumnKind.OFFSET.ordinal()) {
            setOriginToSelectionAction.perform();
        } else if (mouseEvent.getClickCount() > 1 && maxVM().watchpointsEnabled()) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setRegionWatchpoint(tableModel.getMemoryRegion(row), null, null).perform();
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
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
        if (maxVM().watchpointsEnabled()) {
            final MemoryRegion memoryRegion = tableModel.getMemoryRegion(row);
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setRegionWatchpoint(tableModel.getMemoryRegion(row), null, null).perform();
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
                    }
                    return null;
                }
            });
            menu.add(actions().setRegionWatchpoint(memoryRegion, "Watch this memory word", null));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
        }
        menu.add(actions().inspectMemoryBytes(tableModel.getAddress(row), "Inspect this memory as bytes"));
        return menu;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = inspection().focus().address();
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
                inspection().focus().setAddress(tableModel.getAddress(row));
            }
        }
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

    private final class MemoryWordsColumnModel extends InspectorTableColumnModel<MemoryWordsColumnKind> {

        private MemoryWordsColumnModel(TableColumnVisibilityPreferences<MemoryWordsColumnKind> instanceViewPreferences) {
            super(MemoryWordsColumnKind.VALUES.length(), instanceViewPreferences);
            addColumn(MemoryWordsColumnKind.TAG, new TagRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.ADDRESS, new AddressRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.WORD, new WordOffsetRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.OFFSET, new OffsetRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.BYTES, new BytesRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.CHAR, new CharRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.UNICODE, new UnicodeRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.FLOAT, new FloatRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.DOUBLE, new DoubleRenderer(inspection()), null);
            addColumn(MemoryWordsColumnKind.REGION, new RegionRenderer(inspection()), null);
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
        private final Map<Long, MemoryRegion> addressToMemoryRegion = new HashMap<Long, MemoryRegion>();

        public MemoryWordsTableModel(Inspection inspection, MemoryWordRegion memoryRegion, Address origin) {
            super(inspection, origin);
            this.memoryWordRegion = memoryRegion;

            positionBias = memoryWordRegion.start().minus(origin).dividedBy(getWordSize()).toInt();
        }

        @Override
        public void setOrigin(Address origin) {
            assert origin.isAligned(getWordSize().toInt());
            super.setOrigin(origin);
        }

        void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
            this.memoryWordRegion = memoryWordRegion;
            update();
        }

        @Override
        protected void update() {
            positionBias = memoryWordRegion.start().minus(getOrigin()).dividedBy(getWordSize()).toInt();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return MemoryWordsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return memoryWordRegion == null ? 0 : memoryWordRegion.wordCount;
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
        public MemoryRegion getMemoryRegion(int row) {
            final Address address = memoryWordRegion.getAddressAt(row);
            MemoryRegion rowMemoryRegion = addressToMemoryRegion.get(address.toLong());
            if (rowMemoryRegion == null) {
                rowMemoryRegion = new MemoryWordRegion(address, 1, getWordSize());
                addressToMemoryRegion.put(address.toLong(), rowMemoryRegion);
            }
            return rowMemoryRegion;
        }

        @Override
        public Offset getOffset(int row) {
            return Offset.fromInt((row  + positionBias) * getWordSize().toInt());
        }

        @Override
        public int findRow(Address address) {
            return memoryWordRegion.indexAt(address);
        }

    }

    /**
     * @return foreground color for row; color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    /**
     * @param isSelected TODO
     * @return background color for row, using alternate color for object origins.
     */
    private Color getRowBackgroundColor(int row, boolean isSelected) {
        if (maxVM().isValidOrigin(tableModel.getMemoryRegion(row).start().asPointer())) {
            return style().defaultCodeAlternateBackgroundColor();
        }
        // Otherwise use the default background.
        return cellBackgroundColor(isSelected);
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final JLabel renderer = getRenderer(tableModel.getMemoryRegion(row), focus().thread(), tableModel.getWatchpoints(row));
            renderer.setOpaque(true);
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(getRowBackgroundColor(row, isSelected));
            return renderer;
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
                final ValueMode labelValueMode = maxVM().isValidOrigin(address.asPointer()) ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection, labelValueMode, address, MemoryWordsTable.this);
                label.setOpaque(true);
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(getRowBackgroundColor(row, isSelected));
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
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(getRowBackgroundColor(row, isSelected));
            return this;
        }
    }

    private final class OffsetRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public OffsetRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(getRowBackgroundColor(row, isSelected));
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
                        return new WordValue(maxVM().readWord(address));
                    }
                };
                label.setOpaque(true);
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(getRowBackgroundColor(row, isSelected));
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

    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            final byte[] bytes = new byte[tableModel.getWordSize().toInt()];
            maxVM().readFully(address, bytes);
            setBackground(getRowBackgroundColor(row, isSelected));
            setForeground(getRowTextColor(row));
            setValue(bytes);
            return this;
        }
    }


    private final class CharRenderer extends DataLabel.ByteArrayAsChar implements TableCellRenderer {
        CharRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            final byte[] bytes = new byte[tableModel.getWordSize().toInt()];
            maxVM().readFully(address, bytes);
            setBackground(getRowBackgroundColor(row, isSelected));
            setForeground(getRowTextColor(row));
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
            final byte[] bytes = new byte[tableModel.getWordSize().toInt()];
            maxVM().readFully(address, bytes);
            setBackground(getRowBackgroundColor(row, isSelected));
            setForeground(getRowTextColor(row));
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
            final Word word = maxVM().readWord(address);
            final WordValue wordValue = new WordValue(word);
            final float f = Float.intBitsToFloat((int) (wordValue.toLong() & 0xffffffffL));
            setValue(f);
            setBackground(getRowBackgroundColor(row, isSelected));
            setForeground(getRowTextColor(row));
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
            final Word word = maxVM().readWord(address);
            final WordValue wordValue = new WordValue(word);
            final double f = Double.longBitsToDouble(wordValue.toLong());
            setValue(f);
            setBackground(getRowBackgroundColor(row, isSelected));
            setForeground(getRowTextColor(row));
            return this;
        }
    }


    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {
        // Designed so that we only read memory lazily, for words that are visible
        // This label has no state, so we only need one.
        RegionRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            try {
                final Word word = maxVM().readWord(tableModel.getAddress(row));
                setValue(WordValue.from(word));
                setBackground(getRowBackgroundColor(row, isSelected));
                return this;
            } catch (InvalidReferenceException invalidReferenceException) {
                return gui().getUnavailableDataTableCellRenderer();
            } catch (DataIOError dataIOError) {
                return gui().getUnavailableDataTableCellRenderer();
            }
        }
    }
}
