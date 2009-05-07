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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.RegisterInfo.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.debug.*;
import com.sun.max.util.*;

/**
 * A table specialized for displaying register values for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public class RegistersTable extends InspectorTable {

    private static final Color[] _ageColors = {Color.RED, Color.MAGENTA, Color.BLUE};

    private final RegistersTableModel _model;
    private RegistersColumnModel _columnModel;
    private final TableColumn[] _columns;

    public RegistersTable(Inspection inspection, TeleNativeThread teleNativeThread, RegistersViewPreferences viewPreferences) {
        super(inspection);
        _model = new RegistersTableModel(teleNativeThread);
        _columns = new TableColumn[RegistersColumnKind.VALUES.length()];
        _columnModel = new RegistersColumnModel(viewPreferences);
        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().memoryTableShowHorizontalLines());
        setShowVerticalLines(style().memoryTableShowVerticalLines());
        setIntercellSpacing(style().memoryTableIntercellSpacing());
        setRowHeight(style().memoryTableRowHeight());
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new TableCellMouseClickAdapter(inspection, this));
        refresh(vm().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }


    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch) {
            // Read from VM, increment history generation count.
            _model.refresh();
        }
        if (epoch > _lastRefreshEpoch || force) {
            // Refresh the display, might be caused by explicit user request
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                if (prober != null) {
                    prober.refresh(epoch, force);
                }
            }
        }
        _lastRefreshEpoch = epoch;
    }


    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
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
                return RegistersColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class RegistersColumnModel extends DefaultTableColumnModel {

        private final RegistersViewPreferences _viewPreferences;

        private RegistersColumnModel(RegistersViewPreferences viewPreferences) {
            _viewPreferences = viewPreferences;
            createColumn(RegistersColumnKind.NAME, new NameCellRenderer(inspection()), null);
            createColumn(RegistersColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
            createColumn(RegistersColumnKind.REGION, new RegionCellRenderer(), null);
        }

        private void createColumn(RegistersColumnKind columnKind, TableCellRenderer renderer, TableCellEditor editor) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, editor);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_viewPreferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * A table data model built around the list of registers in the VM.
     * Displays all three kinds of registers in a single table in the following order:
     * <ol><li>Integer registers</li><li>State registers</li><li>Floating point registers</li></ol>
     *
     * @author Michael Van De Vanter
     */
    private final class RegistersTableModel extends DefaultTableModel {

        private final TeleNativeThread _teleNativeThread;

        private int _nRegisters = 0;

        private final RegisterInfo[] _registerInfos;

        RegistersTableModel(TeleNativeThread teleNativeThread) {
            _teleNativeThread = teleNativeThread;
            final TeleIntegerRegisters integerRegisters = _teleNativeThread.integerRegisters();
            final TeleStateRegisters stateRegisters = _teleNativeThread.stateRegisters();
            final TeleFloatingPointRegisters floatingPointRegisters = _teleNativeThread.floatingPointRegisters();
            _nRegisters = integerRegisters.symbolizer().numberOfValues()
                 + stateRegisters.symbolizer().numberOfValues()
                 + floatingPointRegisters.symbolizer().numberOfValues();
            _registerInfos = new RegisterInfo[_nRegisters];
            int row = 0;
            for (Symbol register : integerRegisters.symbolizer()) {
                _registerInfos[row] = new IntegerRegisterInfo(integerRegisters, register);
                row++;
            }

            for (Symbol register : stateRegisters.symbolizer()) {
                _registerInfos[row] = new StateRegisterInfo(stateRegisters, register);
                row++;
            }

            for (Symbol register : floatingPointRegisters.symbolizer()) {
                _registerInfos[row] = new FloatingPointRegisterInfo(floatingPointRegisters, register);
                row++;
            }
            assert _nRegisters == row;
        }

        /**
         * Reads from VM and increments the history generation.
         */
        void refresh() {
            for (RegisterInfo registerInfo : _registerInfos) {
                registerInfo.refresh();
            }
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return RegistersColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return _nRegisters;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return _registerInfos[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return RegisterInfo.class;
        }

        /**
         * @return the appropriate display mode for the value of the register at this row
         */
        public WordValueLabel.ValueMode getValueMode(int row) {
            return _registerInfos[row].registerLabelValueMode();
        }

    }

    private final class NameCellRenderer extends TargetCodeLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final RegisterInfo registerInfo = (RegisterInfo) value;
            final String name = registerInfo.name();
            setValue(name, "Register " + name);
            final int age = registerInfo.age();
            if (age < 0 || age >= _ageColors.length) {
                setForeground(style().defaultTextColor());
            } else {
                setForeground(_ageColors[age]);
            }
            return this;
        }
    }

    private final class ValueCellRenderer implements TableCellRenderer, Prober {

        private final WordValueLabel[] _labels;

        ValueCellRenderer(Inspection inspection) {
            _labels = new WordValueLabel[_model.getRowCount()];
            for (int row = 0; row < _model.getRowCount(); row++) {
                _labels[row] = new WordValueLabel(inspection, _model.getValueMode(row));
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final RegisterInfo registerInfo = (RegisterInfo) value;
            _labels[row].setValue(registerInfo.value());
            return _labels[row];
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        @Override
        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.refresh(epoch, force);
                }
            }
        }
    }

    private final class RegionCellRenderer implements TableCellRenderer, Prober {

        private MemoryRegionValueLabel[] _labels = new MemoryRegionValueLabel[_model.getRowCount()];

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
            final RegisterInfo registerInfo = (RegisterInfo) value;
            MemoryRegionValueLabel label = _labels[row];
            if (label == null) {
                label = new MemoryRegionValueLabel(inspection());
                _labels[row] = label;
            }
            label.setValue(registerInfo.value());
            return label;
        }
    }
}
