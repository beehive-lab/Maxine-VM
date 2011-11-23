/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying register values for a thread in the VM.
 */
public final class RegistersTable extends InspectorTable {

    private static final Color[] ageColors = {Color.RED, Color.MAGENTA, Color.BLUE};

    private final String isaName;
    private final RegistersTableModel tableModel;

    public RegistersTable(Inspection inspection, MaxThread thread, RegistersViewPreferences viewPreferences) {
        super(inspection);
        this.isaName = inspection.vm().platform().getISA().name();
        tableModel = new RegistersTableModel(inspection, thread);
        RegistersColumnModel columnModel = new RegistersColumnModel(viewPreferences);
        configureMemoryTable(tableModel, columnModel);
        setRowSelectionAllowed(false);
    }

    private final class RegistersColumnModel extends InspectorTableColumnModel<RegistersColumnKind> {

        private RegistersColumnModel(RegistersViewPreferences viewPreferences) {
            super(RegistersColumnKind.values().length, viewPreferences);
            addColumn(RegistersColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(RegistersColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
            addColumn(RegistersColumnKind.REGION, new RegionCellRenderer(), null);
        }
    }

    /**
     * A table data model built around the list of registers in the VM.
     * Displays all three kinds of registers in a single table in the following order:
     * <ol>
     * <li>Integer registers</li>
     * <li>State registers</li>
     * <li>Floating point registers</li>
     * </ol>
     */
    private final class RegistersTableModel extends InspectorTableModel {

        private static final int HISTORY_GENERATIONS = 6;

        private final MaxThread thread;

        private int nRegisters = 0;


        private final RegisterHistory[] registerHistories;
        private final WordValueLabel.ValueMode[] displayModes;
        private final String[] registerDescriptions;

        RegistersTableModel(Inspection inspection, MaxThread thread) {
            super(inspection);
            this.thread = thread;
            final MaxRegisterSet registers = thread.registers();
            nRegisters = registers.allRegisters().size();
            registerHistories = new RegisterHistory[nRegisters];
            displayModes = new WordValueLabel.ValueMode[nRegisters];
            registerDescriptions = new String[nRegisters];
            int row = 0;
            for (MaxRegister register : registers.integerRegisters()) {
                registerHistories[row] = new RegisterHistory(inspection, HISTORY_GENERATIONS, register);
                displayModes[row] = WordValueLabel.ValueMode.INTEGER_REGISTER;
                registerDescriptions[row] = "Integer register (" + isaName + ") \"" + register.name() + "\"";
                row++;
            }
            for (MaxRegister register : registers.floatingPointRegisters()) {
                registerHistories[row] = new RegisterHistory(inspection, HISTORY_GENERATIONS, register);
                displayModes[row] = WordValueLabel.ValueMode.FLOATING_POINT;
                registerDescriptions[row] = "Float register (" + isaName + ") \"" + register.name() + "\"";
                row++;
            }
            for (MaxRegister register : registers.stateRegisters()) {
                registerHistories[row] = new RegisterHistory(inspection, HISTORY_GENERATIONS, register);
                if (register.isFlagsRegister()) {
                    displayModes[row] = WordValueLabel.ValueMode.FLAGS_REGISTER;
                    registerDescriptions[row] = "Flags register (" + isaName + ") \"" + register.name() + "\"";
                } else if (register.isInstructionPointerRegister()) {
                    displayModes[row] = WordValueLabel.ValueMode.CALL_ENTRY_POINT;
                    registerDescriptions[row] = "IP register (" + isaName + ") \"" + register.name() + "\"";
                } else {
                    displayModes[row] = WordValueLabel.ValueMode.INTEGER_REGISTER;
                    registerDescriptions[row] = "Other register (" + isaName + ") \"" + register.name() + "\"";
                }
                row++;
            }
            assert nRegisters == row;
            for (int i = 0; i < nRegisters; i++) {
                registerHistories[i].refresh();
            }
        }

        public int getColumnCount() {
            return RegistersColumnKind.values().length;
        }

        public int getRowCount() {
            return nRegisters;
        }

        public Object getValueAt(int row, int col) {
            return registerHistories[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return RegisterHistory.class;
        }

        @Override
        public void refresh() {
            // Updates the register history from the VM
            for (RegisterHistory registerHistory : registerHistories) {
                registerHistory.refresh();
            }
            super.refresh();
        }

        @Override
        public String getRowDescription(int row) {
            return registerDescriptions[row];
        }

        /**
         * @return the appropriate display mode for the value of the register at this row
         */
        public WordValueLabel.ValueMode getValueMode(int row) {
            return displayModes[row];
        }
    }

    private final class NameCellRenderer extends MachineCodeLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final RegisterHistory registerHistory = (RegisterHistory) value;
            if (registerHistory == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final String name = registerHistory.name();
            setText(name);
            setToolTipText(tableModel.getRowDescription(row));
            final int age = registerHistory.age();
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
                final RegisterHistory registerHistory = (RegisterHistory) tableModel.getValueAt(row, 0);
                labels[row] = new WordValueLabel(inspection, tableModel.getValueMode(row), RegistersTable.this) {

                    @Override
                    protected Value fetchValue() {
                        return registerHistory.value();
                    }
                };
                labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>value = ");
                labels[row].setOpaque(true);
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final RegisterHistory registerHistory = (RegisterHistory) tableModel.getValueAt(row, 0);
            final WordValueLabel label = labels[row];
            label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>age=" + registerHistory.age() + " value = ");
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

        private ValueLabel[] labels = new ValueLabel[tableModel.getRowCount()];

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
            final RegisterHistory registerHistory = (RegisterHistory) value;
            if (labels[row] == null) {
                switch(tableModel.getValueMode(row)) {
                    case WORD:
                    case REFERENCE:
                    case LITERAL_REFERENCE:
                    case INTEGER_REGISTER:
                    case CALL_ENTRY_POINT:
                    case CALL_RETURN_POINT:
                        labels[row] = new MemoryRegionValueLabel(inspection());
                        break;
                    default:
                        // Don't even try to figure what region some register values might
                        // point into, for example floats.  This suppresses the default tooltip,
                        // which announces that the value points into no known region.
                        labels[row] = new NullValueLabel(inspection());
                }
            }
            labels[row].setValue(registerHistory.value());
            labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + " value = " + registerHistory.value().asWord().toHexString() + "<br>");
            labels[row].setBackground(cellBackgroundColor(isSelected));
            return labels[row];
        }
    }
}
