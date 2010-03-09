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

    private final RegistersTableModel tableModel;
    private RegistersColumnModel columnModel;

    public RegistersTable(Inspection inspection, MaxThread thread, RegistersViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new RegistersTableModel(inspection, thread);
        columnModel = new RegistersColumnModel(viewPreferences);
        configureMemoryTable(tableModel, columnModel);
        setRowSelectionAllowed(false);
    }

    private final class RegistersColumnModel extends InspectorTableColumnModel<RegistersColumnKind> {

        private RegistersColumnModel(RegistersViewPreferences viewPreferences) {
            super(RegistersColumnKind.VALUES.length(), viewPreferences);
            addColumn(RegistersColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(RegistersColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
            addColumn(RegistersColumnKind.REGION, new RegionCellRenderer(), null);
        }
    }

    /**
     * A table data model built around the list of registers in the VM.
     * Displays all three kinds of registers in a single table in the following order:
     * <ol><li>Integer registers</li><li>State registers</li><li>Floating point registers</li></ol>
     *
     * @author Michael Van De Vanter
     */
    private final class RegistersTableModel extends InspectorTableModel {

        private final MaxThread thread;

        private int nRegisters = 0;

        private final RegisterInfo[] registerInfos;

        RegistersTableModel(Inspection inspection, MaxThread thread) {
            super(inspection);
            this.thread = thread;
            final MaxRegisters registers = thread.registers();
            final TeleIntegerRegisterSet integerRegisterSet = registers.integerRegisterSet();
            final TeleStateRegisterSet stateRegisterSet = registers.stateRegisterSet();
            final TeleFloatingPointRegisterSet floatingPointRegisterSet = registers.floatingPointRegisterSet();
            nRegisters = integerRegisterSet.symbolizer().numberOfValues()
                 + stateRegisterSet.symbolizer().numberOfValues()
                 + floatingPointRegisterSet.symbolizer().numberOfValues();
            registerInfos = new RegisterInfo[nRegisters];
            int row = 0;
            for (Symbol register : integerRegisterSet.symbolizer()) {
                registerInfos[row] = new IntegerRegisterInfo(integerRegisterSet, register);
                row++;
            }

            for (Symbol register : stateRegisterSet.symbolizer()) {
                registerInfos[row] = new StateRegisterInfo(stateRegisterSet, register);
                row++;
            }

            for (Symbol register : floatingPointRegisterSet.symbolizer()) {
                registerInfos[row] = new FloatingPointRegisterInfo(floatingPointRegisterSet, register);
                row++;
            }
            assert nRegisters == row;
            for (int i = 0; i < nRegisters; i++) {
                registerInfos[i].refresh();
            }
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

        @Override
        public void refresh() {
            // Reads from VM and increments the history generation.
            for (RegisterInfo registerInfo : registerInfos) {
                registerInfo.refresh();
            }
            super.refresh();
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
                setForeground(null);
            } else {
                setForeground(ageColors[age]);
            }
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ValueCellRenderer implements TableCellRenderer, Prober {

        private final WordValueLabel[] labels;

        ValueCellRenderer(Inspection inspection) {
            labels = new WordValueLabel[tableModel.getRowCount()];
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                final RegisterInfo registerInfo = (RegisterInfo) tableModel.getValueAt(row, 0);
                final WordValueLabel label = new WordValueLabel(inspection, tableModel.getValueMode(row), RegistersTable.this) {

                    @Override
                    protected Value fetchValue() {
                        return registerInfo.value();
                    }
                };
                label.setOpaque(true);
                labels[row] = label;
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final WordValueLabel label = labels[row];
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
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

        private MemoryRegionValueLabel[] labels = new MemoryRegionValueLabel[tableModel.getRowCount()];

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
                label.setOpaque(true);
                labels[row] = label;
            }
            label.setValue(registerInfo.value());
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }
}
