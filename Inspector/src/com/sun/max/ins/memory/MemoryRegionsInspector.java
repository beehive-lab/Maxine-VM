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
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
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
 * An inspector that displays a list of {@link MemoryRegion}s that have been allocated in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsInspector extends UniqueInspector<MemoryRegionsInspector> {

    /**
     * @return the singleton instance, if it exists
     */
    private static MemoryRegionsInspector getInspector(Inspection inspection) {
        return UniqueInspector.find(inspection, MemoryRegionsInspector.class);
    }

    /**
     * Display and highlight the (singleton) MemoryRegions inspector.
     *
     * @return  The MemoryRegions inspector, possibly newly created.
     */
    public static MemoryRegionsInspector make(Inspection inspection) {
        MemoryRegionsInspector memoryRegionsInspector = getInspector(inspection);
        if (memoryRegionsInspector == null) {
            Trace.begin(1, "initializing MemoryRegionsInspector");
            memoryRegionsInspector = new MemoryRegionsInspector(inspection, Residence.INTERNAL);
            Trace.end(1, "initializing MemoryRegionsInspector");
        }
        memoryRegionsInspector.highlight();
        return memoryRegionsInspector;
    }

    enum ColumnKind {
        NAME,
        START,
        END,
        SIZE,
        ALLOC;

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }

    private final HeapScheme _heapScheme;
    private final String _heapSchemeName;

    private final JTable _table = new MemoryRegionJTable();

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "MemoryRegionsInspector");

    private final TeleCodeManager _teleCodeManager;

    private final HeapRegionData _bootHeapRegionData;
    private final CodeRegionData _bootCodeRegionData;

    private MemoryRegionsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        _teleCodeManager = teleVM().teleCodeManager();
        _bootHeapRegionData = new HeapRegionData(teleVM().teleHeapManager().teleBootHeapRegion());
        _bootCodeRegionData = new CodeRegionData(_teleCodeManager.teleBootCodeRegion(), -1);
        _heapScheme = teleVM().vmConfiguration().heapScheme();
        _heapSchemeName = _heapScheme.getClass().getSimpleName();
        createFrame(null);
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTitle() {
        return "MemoryRegions";
    }

    @Override
    public void createView(long epoch) {
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.addMouseListener(new MemoryRegionsInspectorMouseClickAdapter(inspection()));
//        final ListSelectionModel listSelectionModel = _table.getSelectionModel();
//        listSelectionModel.addListSelectionListener(new ListSelectionListener() {
//            public void valueChanged(ListSelectionEvent event) {
//                if (event.getValueIsAdjusting()) {
//                    return;
//                }
//                assert event.getSource() == listSelectionModel;
//                // Decide whether to propagate the new table selection.
//                // If the new table selection agrees with the global thread
//                // selection, then this table change is just an initialization or update notification.
//                if (!listSelectionModel.isSelectionEmpty()) {
//                    final TeleNativeThread teleNativeThread = (TeleNativeThread) _table.getValueAt(_table.getSelectedRow(), 0);
//                    // A user action in this inspector has selected a thread different than the global selection; propagate the change.
//                    inspection().focus().setThread(teleNativeThread);
//                }
//            }
//        });
        final JScrollPane scrollPane = new JScrollPane(_table);
        // scrollPane.setPreferredSize(inspection().geometry().threadsFramePrefSize());
        // frame().setLocation(inspection().geometry().threadsFrameDefaultLocation());
        frame().setContentPane(scrollPane);
        refreshView(epoch, true);
    }

    private final class MemoryRegionJTable extends JTable {
        final TableCellRenderer _nameCellRenderer;
        final TableCellRenderer _startAddressCellRenderer;
        final TableCellRenderer _endAddressCellRenderer;
        final TableCellRenderer _sizeCellRenderer;
        final TableCellRenderer _allocCellRenderer;

        MemoryRegionJTable() {
            super(new MemoryRegionTableModel());
            _nameCellRenderer = new NameCellRenderer(inspection());
            _startAddressCellRenderer = new StartAddressCellRenderer(inspection());
            _endAddressCellRenderer = new EndAddressCellRenderer(inspection());
            _sizeCellRenderer = new SizeCellRenderer();
            _allocCellRenderer = new AllocCellRenderer();
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            switch (ColumnKind.VALUES.get(column)) {
                case NAME:
                    return _nameCellRenderer;
                case START:
                    return _startAddressCellRenderer;
                case END:
                    return _endAddressCellRenderer;
                case SIZE:
                    return _sizeCellRenderer;
                case ALLOC:
                    return _allocCellRenderer;
                default:
                    Problem.error("Unexpected MemoryRegions Data column");
            }
            return null;
        }

        public void refreshView() {
        }
    }


    // VM epoch when data last read.
    private long _epoch = -1;



    @Override
    public void refreshView(long epoch, boolean force) {
        if (epoch > _epoch || force) {
            final MemoryRegionTableModel model = (MemoryRegionTableModel) _table.getModel();
            model.refresh();
        }
    }

    public void viewConfigurationChanged(long epoch) {
        refreshView(epoch, true);
    }


    private final class NameCellRenderer extends PlainLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionData memoryRegionData = (MemoryRegionData) value;
            setText(memoryRegionData.description());
            setToolTipText(memoryRegionData.toolTipText());
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class StartAddressCellRenderer extends WordValueLabel implements TableCellRenderer {

        StartAddressCellRenderer(Inspection inspection) {
            super(inspection, ValueMode.WORD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionData memoryRegionData = (MemoryRegionData) value;
            setValue(WordValue.from(memoryRegionData.start().asWord()));
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class EndAddressCellRenderer extends WordValueLabel implements TableCellRenderer {

        EndAddressCellRenderer(Inspection inspection) {
            super(inspection, ValueMode.WORD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionData memoryRegionData = (MemoryRegionData) value;
            setValue(WordValue.from(memoryRegionData.end().asWord()));
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class SizeCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionData memoryRegionData = (MemoryRegionData) value;
            final DataLabel.LongAsHex sizeDataLabel = new DataLabel.LongAsHex(inspection(), memoryRegionData.size().toLong());
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                sizeDataLabel.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                sizeDataLabel.setBackground(style().defaultTextBackgroundColor());
            }
            return sizeDataLabel;
        }
    }

    private final class AllocCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionData memoryRegionData = (MemoryRegionData) value;
            final long allocated = memoryRegionData.allocated().toLong();
            final DataLabel.Percent percentDataLabel = new DataLabel.Percent(inspection(), allocated, memoryRegionData.size().toLong());
            percentDataLabel.setToolTipText("Allocated from region: 0x" + Long.toHexString(allocated) + "(" + allocated + ")");
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                percentDataLabel.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                percentDataLabel.setBackground(style().defaultTextBackgroundColor());
            }
            return  percentDataLabel;
        }
    }



    private final class MemoryRegionTableModel extends AbstractTableModel {

        private SortedMemoryRegionList<MemoryRegionData> _sortedMemoryRegions;

        void refresh() {
            _sortedMemoryRegions = new SortedMemoryRegionList<MemoryRegionData>();

            _sortedMemoryRegions.add(_bootHeapRegionData);
            for (TeleRuntimeMemoryRegion teleRuntimeMemoryRegion : teleVM().teleHeapManager().teleHeapRegions()) {
                _sortedMemoryRegions.add(new HeapRegionData(teleRuntimeMemoryRegion));
            }

            _sortedMemoryRegions.add(_bootCodeRegionData);
            final IndexedSequence<TeleCodeRegion> teleCodeRegions = _teleCodeManager.teleCodeRegions();
            for (int index = 0; index < teleCodeRegions.length(); index++) {
                final TeleCodeRegion teleCodeRegion = teleCodeRegions.get(index);
                // Only display regions that have memory allocated to them, but that could be a view option.
                if (teleCodeRegion.isAllocated()) {
                    _sortedMemoryRegions.add(new CodeRegionData(teleCodeRegion, index));
                }
            }

            for (TeleNativeThread thread : teleProcess().threads()) {
                _sortedMemoryRegions.add(new StackRegionData(thread.stack()));
            }

            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return ColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return _sortedMemoryRegions.length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            int count = 0;
            for (MemoryRegionData memoryRegionData : _sortedMemoryRegions.memoryRegions()) {
                if (count == row) {
                    return memoryRegionData;
                }
                count++;
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return MemoryRegionData.class;
        }

        @Override
        public String getColumnName(int column) {
            switch (ColumnKind.VALUES.get(column)) {
                case NAME:
                    return "Name";
                case START:
                    return "Start";
                case END:
                    return "End";
                case SIZE:
                    return "Size";
                case ALLOC:
                    return "Alloc";
            }
            return "";
        }

        int findRow(MemoryRegion memoryRegion) {
            assert memoryRegion != null;
            int row = 0;
            for (MemoryRegionData memoryRegionData : _sortedMemoryRegions.memoryRegions()) {
                if (memoryRegion.sameAs(memoryRegionData)) {
                    return row;
                }
                row++;
            }
            ProgramError.unexpected("MemoryregionsInspector couldn't find region: " + memoryRegion);
            return -1;
        }

    }

    enum MemoryRegionKind {
        HEAP,
        CODE,
        STACK,
        OTHER;

        public static final IndexedSequence<MemoryRegionKind> VALUES = new ArraySequence<MemoryRegionKind>(values());
    }

    private abstract class MemoryRegionData implements MemoryRegion {

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

    }

    private final class HeapRegionData extends MemoryRegionData {

        private final TeleRuntimeMemoryRegion _teleRuntimeMemoryRegion;

        @Override
        public MemoryRegion memoryRegion() {
            return _teleRuntimeMemoryRegion;
        }

        HeapRegionData(TeleRuntimeMemoryRegion teleRuntimeMemoryRegion) {
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
            if (this == _bootHeapRegionData) {
                return "Boot heap region";
            }
            return "Dynamic region:  " + description() + "{" + _heapSchemeName + "}";
        }
    }

    private final class CodeRegionData extends MemoryRegionData {

        private final TeleCodeRegion _teleCodeRegion;

        /**
         * Position of this region in the {@link CodeManager}'s allocation array, -1 for the boot region.
         */
        private final int _index;

        @Override
        MemoryRegion memoryRegion() {
            return _teleCodeRegion;
        }

        CodeRegionData(TeleCodeRegion teleCodeRegion, int index) {
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

    private final class StackRegionData extends MemoryRegionData {

        private final TeleNativeStack _teleNativeStack;

        @Override
        MemoryRegion memoryRegion() {
            return _teleNativeStack;
        }

        StackRegionData(TeleNativeStack teleNativeStack) {
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
            final int selectedRow = _table.getSelectedRow();
            final int selectedColumn = _table.getSelectedColumn();
            if (selectedRow != -1 && selectedColumn != -1) {
                // Left button selects a table cell; also cause a code selection at the row.
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                    final MemoryRegionData memoryRegionData = (MemoryRegionData) _table.getValueAt(selectedRow, selectedColumn);
                    focus().setMemoryRegion(memoryRegionData.memoryRegion());
                }
            }
            // Locate the renderer under the event location, and pass along the mouse click if appropriate
            final Point p = mouseEvent.getPoint();
            final int hitColumnIndex = _table.columnAtPoint(p);
            final int hitRowIndex = _table.rowAtPoint(p);
            if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
                final TableCellRenderer tableCellRenderer = _table.getCellRenderer(hitRowIndex, hitColumnIndex);
                final Object cellValue = _table.getValueAt(hitRowIndex, hitColumnIndex);
                final Component component = tableCellRenderer.getTableCellRendererComponent(_table, cellValue, false, true, hitRowIndex, hitColumnIndex);
                if (component != null) {
                    component.dispatchEvent(mouseEvent);
                }
            }
        }
    };

    @Override
    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        updateMemoryRegionFocus(memoryRegion);
    }

    /**
     * Changes the Inspector's selected row to agree with the global thread selection.
     */
    private void updateMemoryRegionFocus(MemoryRegion selectedMemoryRegion) {
        final MemoryRegionTableModel model = (MemoryRegionTableModel) _table.getModel();
        final int row = model.findRow(selectedMemoryRegion);
        _table.setRowSelectionInterval(row, row);
    }



}
