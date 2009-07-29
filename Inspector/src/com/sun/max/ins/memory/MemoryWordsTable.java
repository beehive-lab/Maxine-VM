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
import java.lang.ref.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
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


    public MemoryWordsTable(Inspection inspection, MemoryRegion memoryRegion) {
        super(inspection);
        model = new MemoryWordsTableModel(memoryRegion);
        columns = new TableColumn[MemoryWordsColumnKind.VALUES.length()];
        columnModel = new MemoryWordsColumnModel();

        configure(model, columnModel);

        addMouseListener(new TableCellMouseClickAdapter(inspection, this) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                // By the way we get this event, a left click will have already made a new row selection.
                final int selectedRow = getSelectedRow();
                final int selectedColumn = getSelectedColumn();
                if (selectedRow != -1 && selectedColumn != -1) {
                    // Left button selects a table cell; also cause an address selection at the row.
                    if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                        inspection().focus().setAddress(model.rowToMemoryRegion(selectedRow).start());
                    }
                }
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON3) {
                    if (maxVM().watchpointsEnabled()) {
                        // So far, only watchpoint-related items on this popup menu.
                        final Point p = mouseEvent.getPoint();
                        final int hitRowIndex = rowAtPoint(p);
                        final int columnIndex = getColumnModel().getColumnIndexAtX(p.x);
                        final int modelIndex = getColumnModel().getColumn(columnIndex).getModelIndex();
                        if (modelIndex == ObjectFieldColumnKind.TAG.ordinal() && hitRowIndex >= 0) {
                            final InspectorMenu menu = new InspectorMenu();
                            final MemoryRegion wordMemoryRegion = model.rowToMemoryRegion(hitRowIndex);
                            menu.add(actions().setRegionWatchpoint(wordMemoryRegion, "Watch this memory word"));
                            menu.add(new WatchpointSettingsMenu(model.rowToWatchpoint(hitRowIndex)));
                            menu.add(actions().removeWatchpoint(wordMemoryRegion, "Remove memory watchpoint"));
                            menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        }
                    }
                }
                super.procedure(mouseEvent);
            }
        });


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
    public void paintChildren(Graphics g) {
        // Draw a box around the selected row in the table
        super.paintChildren(g);
        final int row = getSelectedRow();
        if (row >= 0) {
            g.setColor(style().memorySelectedAddressBorderColor());
            g.drawRect(0, row * getRowHeight(row), getWidth() - 1, getRowHeight(row) - 1);
        }
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

        private final MemoryWordsViewPreferences localPreferences;

        private MemoryWordsColumnModel() {
            localPreferences = new MemoryWordsViewPreferences(MemoryWordsViewPreferences.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(MemoryWordsColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(columns[col]);
                    } else {
                        removeColumn(columns[col]);
                    }
                    fireColumnPreferenceChanged();
                }
            };
            createColumn(MemoryWordsColumnKind.TAG, new TagRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.ADDRESS, new AddressRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.POSITION, new PositionRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.VALUE, new ValueRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.REGION, new RegionRenderer(inspection()));
        }

        private void createColumn(MemoryWordsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (localPreferences.isVisible(columnKind)) {
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
    private final class MemoryWordsTableModel extends DefaultTableModel {

        final Size wordSize;
        MemoryRegion memoryRegion;

        // Number of words in the region
        int wordCount;

        // Memory location from which to compute offsets
        Address origin;

        // Number of words offset from origin to beginning of region; may be negative.
        int positionBias;

        // A pre-allocated memory region for each word
        MemoryRegion[] wordRegions;

        public MemoryWordsTableModel(MemoryRegion memoryRegion) {
            wordSize = Size.fromInt(maxVM().wordSize());
            setMemoryRegion(memoryRegion, memoryRegion.start());
        }

        void setMemoryRegion(MemoryRegion memoryRegion, Address baseAddress) {
            this.memoryRegion = memoryRegion;
            this.origin = baseAddress;
            ProgramError.check(memoryRegion.start().isWordAligned());
            ProgramError.check(memoryRegion.end().isWordAligned());
            wordCount = memoryRegion.size().dividedBy(maxVM().wordSize()).toInt();
            positionBias = memoryRegion.start().minus(baseAddress).dividedBy(wordSize.toInt()).toInt();
            wordRegions = new MemoryRegion[wordCount];

            Address wordAddress = memoryRegion.start();
            for (int row = 0; row < wordCount; row++) {
                wordRegions[row] = new FixedMemoryRegion(wordAddress, wordSize, "");
                wordAddress = wordAddress.plus(wordSize);
            }
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
            return wordCount;
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
            return row * maxVM().wordSize() - positionBias;
        }

        public MemoryRegion rowToMemoryRegion(int row) {
            return wordRegions[row];
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

        public int findRow(Address address) {
            if (address != null && memoryRegion.contains(address)) {
                return address.minus(memoryRegion.start()).dividedBy(wordSize.toInt()).toInt();
            }
            return -1;
        }

        public void redisplay() {
//            for (MemoryRegionDisplay memoryRegionData : _sortedMemoryWords) {
//                memoryRegionData.redisplay();
//            }
        }

    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && model.rowToMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private Color getRowBackgroundColor(int row) {
        if (row == getSelectionModel().getMinSelectionIndex()) {
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
            return renderer;
        }

    }

    private final class AddressRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per watchpoint and keep them around
        private final Map<Address, WeakReference<WordValueLabel> > addressToLabelMap = new HashMap<Address, WeakReference<WordValueLabel> >();

        public AddressRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = model.rowToMemoryRegion(row).start();
            WeakReference<WordValueLabel> labelReference = addressToLabelMap.get(address);
            if (labelReference != null && labelReference.get() == null) {
                // has been collected
                addressToLabelMap.remove(labelReference);
                labelReference = null;
            }
            if (labelReference == null) {
                labelReference = new WeakReference<WordValueLabel>(new WordValueLabel(inspection, ValueMode.WORD, address, MemoryWordsTable.this));
                addressToLabelMap.put(address, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            label.setBackground(getRowBackgroundColor(row));
            return label;
        }

        public void redisplay() {
            for (WeakReference<WordValueLabel> labelReference : addressToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (WeakReference<WordValueLabel> labelReference : addressToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.rowToOffset(row), model.getOrigin());
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class ValueRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per watchpoint and keep them around
        private final Map<Address, WeakReference<WordValueLabel> > addressToLabelMap = new HashMap<Address, WeakReference<WordValueLabel> >();

        public ValueRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = model.rowToMemoryRegion(row).start();
            WeakReference<WordValueLabel> labelReference = addressToLabelMap.get(address);
            if (labelReference != null && labelReference.get() == null) {
                // has been collected
                addressToLabelMap.remove(labelReference);
                labelReference = null;
            }
            if (labelReference == null) {
                labelReference = new WeakReference<WordValueLabel>(new WordValueLabel(inspection, ValueMode.WORD, MemoryWordsTable.this) {
                    @Override
                    public Value fetchValue() {
                        return new WordValue(maxVM().readWord(address));
                    }
                });
                addressToLabelMap.put(address, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            label.setBackground(getRowBackgroundColor(row));
            return label;
        }

        public void redisplay() {
            for (WeakReference<WordValueLabel> labelReference : addressToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (WeakReference<WordValueLabel> labelReference : addressToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {
        // Designed so that we only read memory lazily, for words that are visible
        // This label has no state, so we only need one.
        RegionRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            setValue(WordValue.from(maxVM().readWord(model.rowToMemoryRegion(row).start())));
            return this;
        }
    }
}
