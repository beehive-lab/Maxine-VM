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
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying a range of memory words in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryWordsTable extends InspectorTable {

    private final MemoryWordsTableModel model;
    private final MemoryWordsColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;


    public MemoryWordsTable(Inspection inspection, MemoryWordRegion memoryWordRegion, Address origin, TableColumnVisibilityPreferences<MemoryWordsColumnKind> instanceViewPreferences) {
        super(inspection);
        model = new MemoryWordsTableModel(memoryWordRegion, origin);
        columns = new TableColumn[MemoryWordsColumnKind.VALUES.length()];
        columnModel = new MemoryWordsColumnModel(instanceViewPreferences);

        configureMemoryTable(model, columnModel);
        addMouseListener(new TableCellMouseClickAdapter(inspection, this) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                // By the way we get this event, a left click will have already made a new row selection.
                final int selectedRow = getSelectedRow();
                final int selectedColumn = getSelectedColumn();
                if (selectedRow != -1 && selectedColumn != -1) {
                    // Left button selects a table cell; also cause an address selection at the row.
                    if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                        final MemoryRegion memoryRegion = model.rowToMemoryRegion(selectedRow);
                        inspection().focus().setAddress(memoryRegion.start());
                        if (mouseEvent.getClickCount() > 1) {
                            actions().toggleWatchpointAtLocation(memoryRegion, null).perform();
                        }
                    }
                }
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON3) {
                    final Point p = mouseEvent.getPoint();
                    final int hitRowIndex = rowAtPoint(p);
                    final int columnIndex = getColumnModel().getColumnIndexAtX(p.x);
                    final int modelIndex = getColumnModel().getColumn(columnIndex).getModelIndex();
                    if (modelIndex == ObjectFieldColumnKind.TAG.ordinal() && hitRowIndex >= 0) {
                        final InspectorMenu menu = new InspectorMenu();
                        if (maxVM().watchpointsEnabled()) {
                            final MemoryRegion memoryRegion = model.rowToMemoryRegion(hitRowIndex);
                            menu.add(actions().setRegionWatchpoint(memoryRegion, "Watch this memory word"));
                            menu.add(new WatchpointSettingsMenu(model.rowToWatchpoint(hitRowIndex)));
                            menu.add(actions().removeWatchpoint(memoryRegion, "Remove memory watchpoint"));
                        }
                        menu.add(actions().inspectMemoryBytes(model.rowToAddress(hitRowIndex), "Inspect this memory as bytes"));
                        menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
                super.procedure(mouseEvent);
            }
        });
    }

    /**
     * Changes the area of memory being displayed.
     */
    public void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
        model.setMemoryRegion(memoryWordRegion);
    }

    /**
     * Changes the origin used to computing offsets in the memory being displayed.
     * @param origin
     */
    public void setOrigin(Address origin) {
        model.setOrigin(origin);
    }

    public void scrollToOrigin() {
        final int row = model.findRow(model.getOrigin());
        scrollToRows(row, row);
    }

    public void scrollToRange(Address first, Address last) {
        scrollToRows(model.findRow(first), model.findRow(last));
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            model.refresh();
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
        updateFocusSelection();
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    @Override
    public void updateFocusSelection() {
        final Address address = inspection().focus().address();
        updateFocusSelection(model.findRow(address));
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return MemoryWordsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class MemoryWordsColumnModel extends DefaultTableColumnModel {

        final TableColumnVisibilityPreferences<MemoryWordsColumnKind> instanceViewPreferences;

        private MemoryWordsColumnModel(TableColumnVisibilityPreferences<MemoryWordsColumnKind> instanceViewPreferences) {
            this.instanceViewPreferences = instanceViewPreferences;
            createColumn(MemoryWordsColumnKind.TAG, new TagRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.ADDRESS, new AddressRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.WORD, new WordOffsetRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.OFFSET, new OffsetRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.VALUE, new ValueRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.BYTES, new BytesRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.REGION, new RegionRenderer(inspection()));
        }

        private void createColumn(MemoryWordsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (instanceViewPreferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }

    }

    /**
     * Models a region of memory in the VM as a list of words, one per row.
     * A base address may also be specified, from which offsets are computed.
     * There is no requirement that the base address be in the memory region displayed.
     */
    private final class MemoryWordsTableModel extends AbstractTableModel {

        final Size wordSize;
        MemoryWordRegion memoryWordRegion;

        // Memory location from which to compute offsets
        Address origin;

        // Number of words offset from origin to beginning of region; may be negative.
        int positionBias;

        // Cache of memory descriptors for each row
        private final Map<Long, MemoryRegion> addressToMemoryRegion = new HashMap<Long, MemoryRegion>();

        public MemoryWordsTableModel(MemoryWordRegion memoryRegion, Address origin) {
            this.memoryWordRegion = memoryRegion;
            this.origin = origin;
            wordSize = maxVM().wordSize();
            positionBias = memoryWordRegion.start().minus(origin).dividedBy(wordSize).toInt();
        }

        void setOrigin(Address origin) {
            assert origin.isAligned(wordSize.toInt());
            this.origin = origin;
            update();
        }

        void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
            this.memoryWordRegion = memoryWordRegion;
            update();
        }

        private void update() {
            positionBias = memoryWordRegion.start().minus(origin).dividedBy(wordSize).toInt();
            fireTableDataChanged();
        }

        void refresh() {
            fireTableDataChanged();
        }

        Address getOrigin() {
            return origin;
        }

        @Override
        public int getColumnCount() {
            return MemoryWordsColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return memoryWordRegion == null ? 0 : memoryWordRegion.wordCount;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return Integer.class;
        }

        public int rowToOffset(int row) {
            return (row  + positionBias) * maxVM().wordSize().toInt();
        }

        public Address rowToAddress(int row) {
            return memoryWordRegion.getAddressAt(row);
        }

        public MemoryRegion rowToMemoryRegion(int row) {
            final Address address = memoryWordRegion.getAddressAt(row);
            MemoryRegion rowMemoryRegion = addressToMemoryRegion.get(address.toLong());
            if (rowMemoryRegion == null) {
                rowMemoryRegion = new MemoryWordRegion(address, 1, wordSize);
                addressToMemoryRegion.put(address.toLong(), rowMemoryRegion);
            }
            return rowMemoryRegion;
        }

        /**
         * @return the memory watchpoint, if any, that is active at a row
         */
        public MaxWatchpoint rowToWatchpoint(int row) {
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (watchpoint.overlaps(rowToMemoryRegion(row))) {
                    return watchpoint;
                }
            }
            return null;
        }

        private int findRow(Address address) {
            return memoryWordRegion.indexAt(address);
        }

        public void redisplay() {
        }

    }

    /**
     * @return foreground color for row; color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && model.rowToMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    /**
     * @return background color for row, using alternate color for object origins.
     */
    private Color getRowBackgroundColor(int row) {
        if (maxVM().isValidOrigin(model.rowToMemoryRegion(row).start().asPointer())) {
            return style().defaultCodeAlternateBackgroundColor();
        }
        return style().defaultTextBackgroundColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(model.rowToMemoryRegion(row), focus().thread(), model.rowToWatchpoint(row));
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(getRowBackgroundColor(row));
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
            final Address address = model.rowToMemoryRegion(row).start();
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                final ValueMode labelValueMode = maxVM().isValidOrigin(address.asPointer()) ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection, labelValueMode, address, MemoryWordsTable.this);
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(getRowBackgroundColor(row));
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
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.rowToOffset(row), model.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(getRowBackgroundColor(row));
            return this;
        }
    }

    private final class OffsetRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public OffsetRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.rowToOffset(row), model.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(getRowBackgroundColor(row));
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
            final Address address = model.rowToMemoryRegion(row).start();
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                label = new WordValueLabel(inspection, ValueMode.WORD, MemoryWordsTable.this) {
                    @Override
                    public Value fetchValue() {
                        return new WordValue(maxVM().readWord(address));
                    }
                };
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(getRowBackgroundColor(row));
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
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = model.rowToMemoryRegion(row).start();
            final byte[] bytes = new byte[model.wordSize.toInt()];
            maxVM().readFully(address, bytes);
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            setValue(bytes);
            return this;
        }
    }


    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {
        // Designed so that we only read memory lazily, for words that are visible
        // This label has no state, so we only need one.
        RegionRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            try {
                final Word word = maxVM().readWord(model.rowToMemoryRegion(row).start());
                setValue(WordValue.from(word));
                setBackground(getRowBackgroundColor(row));
                return this;
            } catch (DataIOError dataIOError) {
                return gui().getUnavailableDataTableCellRenderer();
            }
        }
    }
}
