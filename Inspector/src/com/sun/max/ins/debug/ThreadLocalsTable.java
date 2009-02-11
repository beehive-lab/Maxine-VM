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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A table that displays Maxine VM thread local names and values, to be used within an
 * instance of {@link ThreadLocalsInspector}.
 *
 * @author Michael Van De Vanter
  */
public final class ThreadLocalsTable extends InspectorTable {

    private final Inspection _inspection;
    private final TeleVMThreadLocalValues _values;
    private final ThreadLocalsViewPreferences _preferences;

    private final ThreadLocalsTableModel _model;
    private final ThreadLocalsTableColumnModel _columnModel;
    private final TableColumn[] _columns;

    /**
     * A {@link JTable} specialized to display Maxine thread local fields.
     */
    public ThreadLocalsTable(ThreadLocalsInspector threadLocalsInspector, TeleVMThreadLocalValues values, ThreadLocalsViewPreferences preferences) {
        super(threadLocalsInspector.inspection());
        _inspection = threadLocalsInspector.inspection();
        _values = values;
        _preferences = preferences;
        _model = new ThreadLocalsTableModel();
        _columns = new TableColumn[ThreadLocalsColumnKind.VALUES.length()];
        _columnModel = new ThreadLocalsTableColumnModel(threadLocalsInspector);
        setModel(_model);
        setColumnModel(_columnModel);
        setFillsViewportHeight(true);
        setShowHorizontalLines(style().threadLocalsTableShowHorizontalLines());
        setShowVerticalLines(style().threadLocalsTableShowVerticalLines());
        setIntercellSpacing(style().threadLocalsTableIntercellSpacing());
        setRowHeight(style().threadLocalsTableRowHeight());
        addMouseListener(new TableCellMouseClickAdapter(_inspection, this));

        refresh(_inspection.teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    /**
     * Add tool tip text to the column headers, as specified by {@link ThreadLocalsColumnKind}.
     *
     * @see javax.swing.JTable#createDefaultTableHeader()
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        final JTableHeader header = new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return ThreadLocalsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
        return header;
    }

/**
     * Models the name/value pairs in a VM thread locals.
     * The value of each cell is the index of the name/value pair
     */
    private final class ThreadLocalsTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return ThreadLocalsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return VmThreadLocal.NAMES.length();
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return Integer.class;
        }
    }

    /**
     * A column model for thread local values, to be displayed in an enclosing {@link ThreadLocalsInspector}.
     * Column selection is driven by choices in the parent {@link ThreadLocalsInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ThreadLocalsTableColumnModel extends DefaultTableColumnModel {

        ThreadLocalsTableColumnModel(ThreadLocalsInspector threadLocalsInspector) {
            createColumn(ThreadLocalsColumnKind.ADDRESS, new AddressRenderer());
            createColumn(ThreadLocalsColumnKind.POSITION, new PositionRenderer());
            createColumn(ThreadLocalsColumnKind.NAME, new NameRenderer());
            createColumn(ThreadLocalsColumnKind.VALUE, new ValueRenderer());
            createColumn(ThreadLocalsColumnKind.REGION, new RegionRenderer());
        }

        private void createColumn(ThreadLocalsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_preferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final int offset = row * Word.size();
            final Address start = _values.start();
            setValue(offset, start);
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final int offset = row * Word.size();
            final Address start = _values.start();
            setValue(offset, start);
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(VmThreadLocal.NAMES.get(row));
            final int offset = row * Word.size();
            setToolTipText("+" + offset + ", 0x" + _values.start().plus(offset).toHexString());
            return this;
        }
    }


    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] _labels = new InspectorLabel[VmThreadLocal.NAMES.length()];

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.refresh(epoch, force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            InspectorLabel label = _labels[row];
            if (label == null) {
                final String name = VmThreadLocal.NAMES.get(row);
                final VmThreadLocal local = row < VmThreadLocal.VALUES.length() ? VmThreadLocal.VALUES.get(row) : null;
                final ValueMode valueMode = local != null && local.kind() == Kind.REFERENCE ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection(), valueMode) {
                    @Override
                    public Value fetchValue() {
                        if (_values.isValid(name)) {
                            return new WordValue(Address.fromLong(_values.get(name)));
                        }
                        return VoidValue.VOID;
                    }
                };
            }
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] _labels = new InspectorLabel[VmThreadLocal.NAMES.length()];

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.refresh(epoch, force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            InspectorLabel label = _labels[row];
            if (label == null) {
                final String name = VmThreadLocal.NAMES.get(row);
                label = new MemoryRegionValueLabel(_inspection) {
                    @Override
                    public Value fetchValue() {
                        if (_values.isValid(name)) {
                            return new WordValue(Address.fromLong(_values.get(name)));
                        }
                        return new WordValue(Address.zero());
                    }
                };
                _labels[row] = label;
            }
            return label;
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

    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
        }
    }

}
