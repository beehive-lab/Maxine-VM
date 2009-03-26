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

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying the memory regions in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public class MemoryRegionsTable extends InspectorTable {

    private final HeapRegionDisplay _bootHeapRegionDisplay;
    private final CodeRegionDisplay _bootCodeRegionDisplay;

    private final HeapScheme _heapScheme;
    private final String _heapSchemeName;

    private final MemoryRegionsTableModel _model;
    private final MemoryRegionsColumnModel _columnModel;
    private final TableColumn[] _columns;

    MemoryRegionsTable(Inspection inspection) {
        super(inspection);
        _bootHeapRegionDisplay = new HeapRegionDisplay(teleVM().teleBootHeapRegion());
        _bootCodeRegionDisplay = new CodeRegionDisplay(teleVM().teleBootCodeRegion(), -1);
        _heapScheme = teleVM().vmConfiguration().heapScheme();
        _heapSchemeName = _heapScheme.getClass().getSimpleName();
        _model = new MemoryRegionsTableModel();
        _columns = new TableColumn[MemoryRegionsColumnKind.VALUES.length()];
        _columnModel = new MemoryRegionsColumnModel();

        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MemoryRegionsInspectorMouseClickAdapter(inspection()));

        refresh(teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    MemoryRegionsViewPreferences preferences() {
        return _columnModel.localPreferences();
    }

    void selectMemoryRegion(MemoryRegion memoryRegion) {
        if (memoryRegion == null) {
            clearSelection();
        } else {
            final MemoryRegionsTableModel model = (MemoryRegionsTableModel) getModel();
            final int row = model.findRow(memoryRegion);
            setRowSelectionInterval(row, row);
        }
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return MemoryRegionsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class MemoryRegionsColumnModel extends DefaultTableColumnModel {

        private final MemoryRegionsViewPreferences _localPreferences;

        private MemoryRegionsColumnModel() {
            _localPreferences = new MemoryRegionsViewPreferences(MemoryRegionsViewPreferences.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(MemoryRegionsColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(_columns[col]);
                    } else {
                        removeColumn(_columns[col]);
                    }
                    fireColumnPreferenceChanged();
                }
            };
            createColumn(MemoryRegionsColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(MemoryRegionsColumnKind.START, new StartAddressCellRenderer());
            createColumn(MemoryRegionsColumnKind.END, new EndAddressCellRenderer());
            createColumn(MemoryRegionsColumnKind.SIZE, new SizeCellRenderer());
            createColumn(MemoryRegionsColumnKind.ALLOC, new AllocCellRenderer());
        }

        private MemoryRegionsViewPreferences localPreferences() {
            return _localPreferences;
        }

        private void createColumn(MemoryRegionsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_localPreferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    private final class MemoryRegionsTableModel extends AbstractTableModel {

        private SortedMemoryRegionList<MemoryRegionDisplay> _sortedMemoryRegions;

        public MemoryRegionsTableModel() {
            refresh();
        }

        void refresh() {
            _sortedMemoryRegions = new SortedMemoryRegionList<MemoryRegionDisplay>();

            _sortedMemoryRegions.add(_bootHeapRegionDisplay);
            for (TeleRuntimeMemoryRegion teleRuntimeMemoryRegion : teleVM().teleHeapRegions()) {
                _sortedMemoryRegions.add(new HeapRegionDisplay(teleRuntimeMemoryRegion));
            }

            _sortedMemoryRegions.add(_bootCodeRegionDisplay);
            final IndexedSequence<TeleCodeRegion> teleCodeRegions = teleVM().teleCodeRegions();
            for (int index = 0; index < teleCodeRegions.length(); index++) {
                final TeleCodeRegion teleCodeRegion = teleCodeRegions.get(index);
                // Only display regions that have memory allocated to them, but that could be a view option.
                if (teleCodeRegion.isAllocated()) {
                    _sortedMemoryRegions.add(new CodeRegionDisplay(teleCodeRegion, index));
                }
            }

            for (TeleNativeThread thread : teleVM().threads()) {
                final TeleNativeStack stack = thread.stack();
                if (!stack.size().isZero()) {
                    _sortedMemoryRegions.add(new StackRegionDisplay(stack));
                }
            }

            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return MemoryRegionsColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return _sortedMemoryRegions.length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            int count = 0;
            for (MemoryRegionDisplay memoryRegionData : _sortedMemoryRegions.memoryRegions()) {
                if (count == row) {
                    return memoryRegionData;
                }
                count++;
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return MemoryRegionDisplay.class;
        }

        int findRow(MemoryRegion memoryRegion) {
            assert memoryRegion != null;
            int row = 0;
            for (MemoryRegionDisplay memoryRegionData : _sortedMemoryRegions.memoryRegions()) {
                if (memoryRegion.sameAs(memoryRegionData)) {
                    return row;
                }
                row++;
            }
            ProgramError.unexpected("MemoryregionsInspector couldn't find region: " + memoryRegion);
            return -1;
        }

        public void redisplay() {
            for (MemoryRegionDisplay memoryRegionData : _sortedMemoryRegions) {
                memoryRegionData.redisplay();
            }
        }

    }

    private final class NameCellRenderer extends PlainLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            setText(memoryRegionData.description());
            setToolTipText(memoryRegionData.toolTipText());
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }

    }

    private final class StartAddressCellRenderer implements TableCellRenderer, Prober {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final WordValueLabel label = memoryRegionData.startLabel();
            if (row == getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
            return label;
        }

        public void redisplay() {
        }

        public void refresh(long epoch, boolean force) {
        }
    }

    private final class EndAddressCellRenderer implements TableCellRenderer, Prober{

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final WordValueLabel label = memoryRegionData.endLabel();
            if (row == getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
            return label;
        }

        public void redisplay() {
        }

        public void refresh(long epoch, boolean force) {
        }
    }

    private final class SizeCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer, Prober {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final DataLabel.LongAsHex sizeDataLabel = new DataLabel.LongAsHex(inspection(), memoryRegionData.size().toLong());
            if (row == getSelectionModel().getMinSelectionIndex()) {
                sizeDataLabel.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                sizeDataLabel.setBackground(style().defaultTextBackgroundColor());
            }
            return sizeDataLabel;
        }

        public void redisplay() {
        }

        public void refresh(long epoch, boolean force) {
        }
    }

    private final class AllocCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer, Prober {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final long allocated = memoryRegionData.allocated().toLong();
            final DataLabel.Percent percentDataLabel = new DataLabel.Percent(inspection(), allocated, memoryRegionData.size().toLong());
            percentDataLabel.setToolTipText("Allocated from region: 0x" + Long.toHexString(allocated) + "(" + allocated + ")");
            if (row == getSelectionModel().getMinSelectionIndex()) {
                percentDataLabel.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                percentDataLabel.setBackground(style().defaultTextBackgroundColor());
            }
            return  percentDataLabel;
        }

        public void redisplay() {
        }

        public void refresh(long epoch, boolean force) {
        }
    }

    private enum MemoryRegionKind {
        HEAP,
        CODE,
        STACK,
        OTHER;

        public static final IndexedSequence<MemoryRegionKind> VALUES = new ArraySequence<MemoryRegionKind>(values());
    }

    /**
     * Wraps a {@link MemoryRegion} with additional display-related behavior.
     *
     */
    private abstract class MemoryRegionDisplay implements MemoryRegion {

        abstract MemoryRegion memoryRegion();

        public Address start() {
            return memoryRegion().start();
        }

        public Size size() {
            return memoryRegion().size();
        }

        /**
         * @return the amount of memory within the region that has actually been used.
         */
        Size allocated() {
            return size();
        }

        public Address end() {
            return memoryRegion().end();
        }

        public boolean contains(Address address) {
            return memoryRegion().contains(address);
        }

        public boolean overlaps(MemoryRegion memoryRegion) {
            return memoryRegion().overlaps(memoryRegion);
        }

        public boolean sameAs(MemoryRegion otherMemoryRegion) {
            return otherMemoryRegion != null && start().equals(otherMemoryRegion.start()) && size().equals(otherMemoryRegion.size());
        }

        public String description() {
            return memoryRegion().description();
        }

        abstract String toolTipText();

        abstract MemoryRegionKind kind();

        private WordValueLabel _startLabel;

        public WordValueLabel startLabel() {
            if (_startLabel == null) {
                _startLabel = new WordValueLabel(inspection(), ValueMode.WORD) {
                    @Override
                    public Value fetchValue() {
                        return WordValue.from(MemoryRegionDisplay.this.start());
                    }
                };
            }
            return _startLabel;
        }

        private WordValueLabel _endLabel;

        public WordValueLabel endLabel() {
            if (_endLabel == null) {
                _endLabel = new WordValueLabel(inspection(), ValueMode.WORD) {
                    @Override
                    public Value fetchValue() {
                        return WordValue.from(MemoryRegionDisplay.this.end());
                    }
                };
            }
            return _endLabel;
        }

        public void redisplay() {
            if (_startLabel != null) {
                _startLabel.redisplay();
            }
            if (_endLabel != null) {
                _endLabel.redisplay();
            }
        }

    }

    private final class HeapRegionDisplay extends MemoryRegionDisplay {

        private final TeleRuntimeMemoryRegion _teleRuntimeMemoryRegion;

        @Override
        public MemoryRegion memoryRegion() {
            return _teleRuntimeMemoryRegion;
        }

        HeapRegionDisplay(TeleRuntimeMemoryRegion teleRuntimeMemoryRegion) {
            _teleRuntimeMemoryRegion = teleRuntimeMemoryRegion;
        }

        @Override
        Size allocated() {
            return _teleRuntimeMemoryRegion.allocatedSize();
        }

        @Override
        MemoryRegionKind kind() {
            return MemoryRegionKind.HEAP;
        }

        @Override
        String toolTipText() {
            if (this == _bootHeapRegionDisplay) {
                return "Boot heap region";
            }
            return "Dynamic region:  " + description() + "{" + _heapSchemeName + "}";
        }
    }

    private final class CodeRegionDisplay extends MemoryRegionDisplay {

        private final TeleCodeRegion _teleCodeRegion;

        /**
         * Position of this region in the {@link CodeManager}'s allocation array, -1 for the boot region.
         */
        private final int _index;

        @Override
        MemoryRegion memoryRegion() {
            return _teleCodeRegion;
        }

        CodeRegionDisplay(TeleCodeRegion teleCodeRegion, int index) {
            _teleCodeRegion = teleCodeRegion;
            _index = index;
        }

        @Override
        public Size allocated() {
            return _teleCodeRegion.allocatedSize();
        }

        @Override
        MemoryRegionKind kind() {
            return MemoryRegionKind.CODE;
        }

        @Override
        String toolTipText() {
            if (_index < 0) {
                return "Boot code region";
            }
            return "Dynamic region:  " + description();
        }

    }

    private final class StackRegionDisplay extends MemoryRegionDisplay {

        private final TeleNativeStack _teleNativeStack;

        @Override
        MemoryRegion memoryRegion() {
            return _teleNativeStack;
        }

        StackRegionDisplay(TeleNativeStack teleNativeStack) {
            _teleNativeStack = teleNativeStack;
        }

        @Override
        Size allocated() {
            // Stack grows downward from the end of the region;
            // no account taken here for thread locals.
            return end().minus(_teleNativeStack.teleNativeThread().stackPointer()).asSize();
        }
        @Override
        MemoryRegionKind kind() {
            return MemoryRegionKind.STACK;
        }

        @Override
        String toolTipText() {
            final TeleNativeStack teleNativeStack = (TeleNativeStack) memoryRegion();
            return "Thread region: " + inspection().nameDisplay().longName(teleNativeStack.teleNativeThread());
        }

    }

    private final class MemoryRegionsInspectorMouseClickAdapter extends InspectorMouseClickAdapter {

        MemoryRegionsInspectorMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(final MouseEvent mouseEvent) {
            final int selectedRow = getSelectedRow();
            final int selectedColumn = getSelectedColumn();
            if (selectedRow != -1 && selectedColumn != -1) {
                // Left button selects a table cell; also cause a code selection at the row.
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                    final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) getValueAt(selectedRow, selectedColumn);
                    focus().setMemoryRegion(memoryRegionData.memoryRegion());
                }
            }
            // Locate the renderer under the event location, and pass along the mouse click if appropriate
            final Point p = mouseEvent.getPoint();
            final int hitColumnIndex = columnAtPoint(p);
            final int hitRowIndex = rowAtPoint(p);
            if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
                final TableCellRenderer tableCellRenderer = getCellRenderer(hitRowIndex, hitColumnIndex);
                final Object cellValue = getValueAt(hitRowIndex, hitColumnIndex);
                final Component component = tableCellRenderer.getTableCellRendererComponent(MemoryRegionsTable.this, cellValue, false, true, hitRowIndex, hitColumnIndex);
                if (component != null) {
                    component.dispatchEvent(mouseEvent);
                }
            }
        }
    };

    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            _lastRefreshEpoch = epoch;
            _model.refresh();
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
        }
    }
}
