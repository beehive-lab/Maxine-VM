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
import javax.swing.event.*;
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
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying the memory regions in the VM.
 *
 * @author Michael Van De Vanter
 */
public class MemoryRegionsTable extends InspectorTable  implements ViewFocusListener {

    private final HeapRegionDisplay _bootHeapRegionDisplay;
    private final CodeRegionDisplay _bootCodeRegionDisplay;

    private final HeapScheme _heapScheme;
    private final String _heapSchemeName;

    private final MemoryRegionsTableModel _model;
    private final MemoryRegionsColumnModel _columnModel;
    private final TableColumn[] _columns;

    MemoryRegionsTable(Inspection inspection, MemoryRegionsViewPreferences viewPreferences) {
        super(inspection);
        _bootHeapRegionDisplay = new HeapRegionDisplay(maxVM().teleBootHeapRegion());
        _bootCodeRegionDisplay = new CodeRegionDisplay(maxVM().teleBootCodeRegion(), -1);
        _heapScheme = inspection.maxVM().vmConfiguration().heapScheme();
        _heapSchemeName = _heapScheme.getClass().getSimpleName();
        _model = new MemoryRegionsTableModel();
        _columns = new TableColumn[MemoryRegionsColumnKind.VALUES.length()];
        _columnModel = new MemoryRegionsColumnModel(viewPreferences);

        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new TableCellMouseClickAdapter(inspection(), this));
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
        updateSelection();
    }

    /**
     * Sets table selection to the memory region, if any, that is the current user focus.
     */
    private void updateSelection() {
        final MemoryRegion memoryRegion = inspection().focus().memoryRegion();
        final int row = _model.findRow(memoryRegion);
        if (row < 0) {
            clearSelection();
        } else  if (row != getSelectedRow()) {
            setRowSelectionInterval(row, row);
        }
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        // Custom table header with tooltips that describe the column data.
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

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // Row selection changed, perhaps by user mouse click or navigation;
        // update user focus to follow the selection.
        super.valueChanged(listSelectionEvent);
        if (!listSelectionEvent.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0) {
                final MemoryRegionDisplay memoryRegionDisplay = (MemoryRegionDisplay) getValueAt(row, 0);
                focus().setMemoryRegion(memoryRegionDisplay.memoryRegion());
            }
        }
    }

    private final class MemoryRegionsColumnModel extends DefaultTableColumnModel {

        private final MemoryRegionsViewPreferences _viewPreferences;

        private MemoryRegionsColumnModel(MemoryRegionsViewPreferences viewPreferences) {
            _viewPreferences = viewPreferences;
            createColumn(MemoryRegionsColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(MemoryRegionsColumnKind.START, new StartAddressCellRenderer());
            createColumn(MemoryRegionsColumnKind.END, new EndAddressCellRenderer());
            createColumn(MemoryRegionsColumnKind.SIZE, new SizeCellRenderer());
            createColumn(MemoryRegionsColumnKind.ALLOC, new AllocCellRenderer());
        }

        private void createColumn(MemoryRegionsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_viewPreferences.isVisible(columnKind)) {
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
            for (TeleRuntimeMemoryRegion teleRuntimeMemoryRegion : maxVM().teleHeapRegions()) {
                _sortedMemoryRegions.add(new HeapRegionDisplay(teleRuntimeMemoryRegion));
            }

            _sortedMemoryRegions.add(_bootCodeRegionDisplay);
            final IndexedSequence<TeleCodeRegion> teleCodeRegions = maxVM().teleCodeRegions();
            for (int index = 0; index < teleCodeRegions.length(); index++) {
                final TeleCodeRegion teleCodeRegion = teleCodeRegions.get(index);
                // Only display regions that have memory allocated to them, but that could be a view option.
                if (teleCodeRegion.isAllocated()) {
                    _sortedMemoryRegions.add(new CodeRegionDisplay(teleCodeRegion, index));
                }
            }

            for (TeleNativeThread thread : maxVM().threads()) {
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
            for (MemoryRegionDisplay memoryRegionDisplay : _sortedMemoryRegions.memoryRegions()) {
                if (count == row) {
                    return memoryRegionDisplay;
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

        public void refresh(boolean force) {
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

        public void refresh(boolean force) {
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

        public void refresh(boolean force) {
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

        public void refresh(boolean force) {
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

    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private MaxVMState _lastRefreshedState = null;

    public void refresh(boolean force) {
        if (maxVMState().newerThan(_lastRefreshedState) || force) {
            _lastRefreshedState = maxVMState();
            _model.refresh();
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
    }

    public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame) {
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        updateSelection();
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
    }

    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
    }
}
