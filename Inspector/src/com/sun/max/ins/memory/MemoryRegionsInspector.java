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
            memoryRegionsInspector = new MemoryRegionsInspector(inspection, Residence.INTERNAL);
        }
        memoryRegionsInspector.highlight();
        return memoryRegionsInspector;
    }

    /**
     * Defines the columns supported by the inspector; the view includes one of each
     * kind.  The visibility of them, however, may be changed by the user.
     */
    private enum ColumnKind {
        NAME("Name", null, true) {
            @Override
            public boolean canBeMadeInvisible() {
                return false;
            }
        },
        START("Start", "Starting address", true),
        END("End", "Ending address", true),
        SIZE("Size", "Region size allocated from OS", true),
        ALLOC("Alloc", "Memory allocated by VM within region", true);

        private final String _label;
        private final String _toolTipText;
        private final boolean _defaultVisibility;

        private ColumnKind(String label, String toolTipText, boolean defaultVisibility) {
            _label = label;
            _toolTipText = toolTipText;
            _defaultVisibility = defaultVisibility;
            assert defaultVisibility || canBeMadeInvisible();
        }

        public String label() {
            return _label;
        }

        public String toolTipText() {
            return _toolTipText;
        }

        @Override
        public String toString() {
            return _label;
        }

        /**
         * @return whether this column kind can be made invisible; default true.
         */
        public boolean canBeMadeInvisible() {
            return true;
        }

        /**
         * Determines if this column should be visible by default; default true.
         */
        public boolean defaultVisibility() {
            return _defaultVisibility;
        }

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }


    public static class Preferences extends TableColumnVisibilityPreferences<ColumnKind> {

        /**
         * Create a set of preferences specified for use by singleton instances, where local and
         * persistent global choices are identical.
         */
        public Preferences(TableColumnVisibilityPreferences<ColumnKind> otherPreferences) {
            super(otherPreferences, true);
        }

        public Preferences(Inspection inspection) {
            super(inspection, "memoryRegionsInspectorPrefs", ColumnKind.class, ColumnKind.VALUES);
        }

        @Override
        protected boolean canBeMadeInvisible(ColumnKind columnType) {
            return columnType.canBeMadeInvisible();
        }

        @Override
        protected boolean defaultVisibility(ColumnKind columnType) {
            return columnType.defaultVisibility();
        }

        @Override
        protected String label(ColumnKind columnType) {
            return columnType.label();
        }
    }

    private static Preferences _globalPreferences;

    public static synchronized Preferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new Preferences(inspection);
        }
        return _globalPreferences;
    }


    private final HeapScheme _heapScheme;
    private final String _heapSchemeName;

    private final JTable _table;
    private final MemoryRegionTableModel _model;
    private final MemoryRegionColumnModel _columnModel;
    private final TableColumn[] _columns;

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "MemoryRegionsInspector");

    private final HeapRegionDisplay _bootHeapRegionDisplay;
    private final CodeRegionDisplay _bootCodeRegionDisplay;

    private MemoryRegionsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        Trace.begin(1, tracePrefix() + "initializing");
        _bootHeapRegionDisplay = new HeapRegionDisplay(teleVM().teleBootHeapRegion());
        _bootCodeRegionDisplay = new CodeRegionDisplay(teleVM().teleBootCodeRegion(), -1);
        _heapScheme = teleVM().vmConfiguration().heapScheme();
        _heapSchemeName = _heapScheme.getClass().getSimpleName();
        _model = new MemoryRegionTableModel();
        _columns = new TableColumn[ColumnKind.VALUES.length()];
        _columnModel = new MemoryRegionColumnModel();
        _table = new MemoryRegionJTable(_model, _columnModel);
        _model.refresh();
        JTableColumnResizer.adjustColumnPreferredWidths(_table);
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "Preferences") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ColumnKind>(inspection(), "Memory Regions View Options", _columnModel.preferences());
            }
        });
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "MemoryRegions";
    }

    @Override
    public void createView(long epoch) {
        _table.setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        _table.setShowVerticalLines(style().defaultTableShowVerticalLines());
        _table.setIntercellSpacing(style().defaultTableIntercellSpacing());
        _table.setRowHeight(style().defaultTableRowHeight());
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.addMouseListener(new MemoryRegionsInspectorMouseClickAdapter(inspection()));
        final JScrollPane scrollPane = new JScrollPane(_table);
        frame().setContentPane(scrollPane);
        refreshView(epoch, true);
    }


    private final class MemoryRegionJTable extends JTable {

        MemoryRegionJTable(TableModel tableModel, TableColumnModel tableColumnModel) {
            super(tableModel, tableColumnModel);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(_columnModel) {
                @Override
                public String getToolTipText(MouseEvent mouseEvent) {
                    final Point p = mouseEvent.getPoint();
                    final int index = _columnModel.getColumnIndexAtX(p.x);
                    final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                    return ColumnKind.VALUES.get(modelIndex).toolTipText();
                }
            };
        }
    }

    private final class MemoryRegionColumnModel extends DefaultTableColumnModel {

        private final Preferences _preferences;

        private MemoryRegionColumnModel() {
            _preferences = new Preferences(MemoryRegionsInspector.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(ColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(_columns[col]);
                    } else {
                        removeColumn(_columns[col]);
                    }
                    // setColumnSizes();
                    // refresh(teleVM().teleProcess().epoch(), true);
                }
            };

            createColumn(ColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(ColumnKind.START, new StartAddressCellRenderer());
            createColumn(ColumnKind.END, new EndAddressCellRenderer());
            createColumn(ColumnKind.SIZE, new SizeCellRenderer());
            createColumn(ColumnKind.ALLOC, new AllocCellRenderer());
        }

        private Preferences preferences() {
            return _preferences;
        }

        private void createColumn(ColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            if (_preferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }



    private final class MemoryRegionTableModel extends AbstractTableModel {

        private SortedMemoryRegionList<MemoryRegionDisplay> _sortedMemoryRegions;

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
                _sortedMemoryRegions.add(new StackRegionDisplay(thread.stack()));
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

    // VM epoch when data last read.
    private long _epoch = -1;

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            if (epoch > _epoch || force) {
                final MemoryRegionTableModel model = (MemoryRegionTableModel) _table.getModel();
                model.refresh();
                _epoch = epoch;
            }
        }
    }

    public void viewConfigurationChanged(long epoch) {
        _model.redisplay();
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
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }

    }

    private final class StartAddressCellRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final WordValueLabel label = memoryRegionData.startLabel();
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
            return label;
        }

    }

    private final class EndAddressCellRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
            final WordValueLabel label = memoryRegionData.endLabel();
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
            return label;
        }
    }

    private final class SizeCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
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
            final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) value;
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


    enum MemoryRegionKind {
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
            final int selectedRow = _table.getSelectedRow();
            final int selectedColumn = _table.getSelectedColumn();
            if (selectedRow != -1 && selectedColumn != -1) {
                // Left button selects a table cell; also cause a code selection at the row.
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                    final MemoryRegionDisplay memoryRegionData = (MemoryRegionDisplay) _table.getValueAt(selectedRow, selectedColumn);
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

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        dispose();
    }

}
