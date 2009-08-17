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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.util.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying register values for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class RegistersTable extends InspectorTable {

    private static final Color[] ageColors = {Color.RED, Color.MAGENTA, Color.BLUE};

    private final RegistersTableModel model;
    private RegistersColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    public RegistersTable(Inspection inspection, MaxThread thread, RegistersViewPreferences viewPreferences) {
        super(inspection);
        model = new RegistersTableModel(thread);
        columns = new TableColumn[RegistersColumnKind.VALUES.length()];
        columnModel = new RegistersColumnModel(viewPreferences);
        configureMemoryTable(model, columnModel);
        setRowSelectionAllowed(false);
        addMouseListener(new TableCellMouseClickAdapter(inspection, this));
    }


    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            // Read from VM, increment history generation count.
            model.refresh();
            // Refresh the display, might be caused by explicit user request
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                if (prober != null) {
                    prober.refresh(force);
                }
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
    protected JTableHeader createDefaultTableHeader() {
        // Custom table header with tooltips that describe the column data.
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return RegistersColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class RegistersColumnModel extends DefaultTableColumnModel {

        private final RegistersViewPreferences viewPreferences;

        private RegistersColumnModel(RegistersViewPreferences viewPreferences) {
            this.viewPreferences = viewPreferences;
            createColumn(RegistersColumnKind.NAME, new NameCellRenderer(inspection()), null);
            createColumn(RegistersColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
            createColumn(RegistersColumnKind.REGION, new RegionCellRenderer(), null);
        }

        private void createColumn(RegistersColumnKind columnKind, TableCellRenderer renderer, TableCellEditor editor) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, editor);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (viewPreferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * A table data model built around the list of registers in the VM.
     * Displays all three kinds of registers in a single table in the following order:
     * <ol><li>Integer registers</li><li>State registers</li><li>Floating point registers</li></ol>
     *
     * @author Michael Van De Vanter
     */
    private final class RegistersTableModel extends AbstractTableModel {

        private final MaxThread thread;

        private int nRegisters = 0;

        private final RegisterInfo[] registerInfos;

        RegistersTableModel(MaxThread thread) {
            this.thread = thread;
            final TeleIntegerRegisters integerRegisters = thread.integerRegisters();
            final TeleStateRegisters stateRegisters = thread.stateRegisters();
            final TeleFloatingPointRegisters floatingPointRegisters = thread.floatingPointRegisters();
            nRegisters = integerRegisters.symbolizer().numberOfValues()
                 + stateRegisters.symbolizer().numberOfValues()
                 + floatingPointRegisters.symbolizer().numberOfValues();
            registerInfos = new RegisterInfo[nRegisters];
            int row = 0;
            for (Symbol register : integerRegisters.symbolizer()) {
                registerInfos[row] = new IntegerRegisterInfo(integerRegisters, register);
                row++;
            }

            for (Symbol register : stateRegisters.symbolizer()) {
                registerInfos[row] = new StateRegisterInfo(stateRegisters, register);
                row++;
            }

            for (Symbol register : floatingPointRegisters.symbolizer()) {
                registerInfos[row] = new FloatingPointRegisterInfo(floatingPointRegisters, register);
                row++;
            }
            assert nRegisters == row;
            for (int i = 0; i < nRegisters; i++) {
                registerInfos[i].refresh();
            }
        }

        /**
         * Reads from VM and increments the history generation.
         */
        void refresh() {
            for (RegisterInfo registerInfo : registerInfos) {
                registerInfo.refresh();
            }
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return RegistersColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return nRegisters;
        }

        public Object getValueAt(int row, int col) {
            return registerInfos[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return RegisterInfo.class;
        }

        /**
         * @return the appropriate display mode for the value of the register at this row
         */
        public WordValueLabel.ValueMode getValueMode(int row) {
            return registerInfos[row].registerLabelValueMode();
        }

    }

    private final class NameCellRenderer extends TargetCodeLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final RegisterInfo registerInfo = (RegisterInfo) value;
            final String name = registerInfo.name();
            setValue(name, "Register " + name);
            final int age = registerInfo.age();
            if (age < 0 || age >= ageColors.length) {
                setForeground(style().defaultTextColor());
            } else {
                setForeground(ageColors[age]);
            }
            return this;
        }
    }

    private final class ValueCellRenderer implements TableCellRenderer, Prober {

        private final WordValueLabel[] labels;

        ValueCellRenderer(Inspection inspection) {
            labels = new WordValueLabel[model.getRowCount()];
            for (int row = 0; row < model.getRowCount(); row++) {
                final RegisterInfo registerInfo = (RegisterInfo) model.getValueAt(row, 0);
                labels[row] = new WordValueLabel(inspection, model.getValueMode(row), RegistersTable.this) {

                    @Override
                    protected Value fetchValue() {
                        return registerInfo.value();
                    }
                };
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return labels[row];
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

    private final class RegionCellRenderer implements TableCellRenderer, Prober {

        private MemoryRegionValueLabel[] labels = new MemoryRegionValueLabel[model.getRowCount()];

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            final RegisterInfo registerInfo = (RegisterInfo) value;
            MemoryRegionValueLabel label = labels[row];
            if (label == null) {
                label = new MemoryRegionValueLabel(inspection());
                labels[row] = label;
            }
            label.setValue(registerInfo.value());
            return label;
        }
    }
}
