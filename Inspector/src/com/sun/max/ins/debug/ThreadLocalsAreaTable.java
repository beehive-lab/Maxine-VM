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
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays VM thread local variable names and values in a thread locals area, to be used within an
 * instance of {@link ThreadLocalsInspector}.
 *
 * @author Michael Van De Vanter
  */
public final class ThreadLocalsAreaTable extends InspectorTable {

    private final ThreadLocalsAreaTableModel tableModel;
    private final ThreadLocalsAreaTableColumnModel columnModel;

    /**
     * A table specialized to display thread local fields.
     */
    public ThreadLocalsAreaTable(Inspection inspection, final MaxThreadLocalsArea tla, ThreadLocalsViewPreferences viewPreferences) {
        super(inspection);
        this.tableModel = new ThreadLocalsAreaTableModel(inspection, tla);
        this.columnModel = new ThreadLocalsAreaTableColumnModel(this, this.tableModel, viewPreferences);
        configureMemoryTable(tableModel, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setThreadLocalWatchpoint(tableModel.getValueAt(row, 0), null).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            };
            toggleAction.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null && col == ThreadLocalVariablesColumnKind.TAG.ordinal()) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setThreadLocalWatchpoint(tableModel.getValueAt(row, 0), null).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            });
            menu.add(actions().setThreadLocalWatchpoint(tableModel.getValueAt(row, 0), "Watch this memory location"));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = focus().address();
        updateSelection(tableModel.findRow(address));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    /**
     * A column model for thread local values.
     * Column selection is driven by choices in the parent.
     * This implementation cannot update column choices dynamically.
     */
    private final class ThreadLocalsAreaTableColumnModel extends InspectorTableColumnModel<ThreadLocalVariablesColumnKind> {

        ThreadLocalsAreaTableColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, ThreadLocalsViewPreferences viewPreferences) {
            super(ThreadLocalVariablesColumnKind.values().length, viewPreferences);
            addColumn(ThreadLocalVariablesColumnKind.TAG, new MemoryTagTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ThreadLocalVariablesColumnKind.ADDRESS, new MemoryAddressLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ThreadLocalVariablesColumnKind.POSITION, new MemoryOffsetLocationTableCellRenderer(inspection(), table, tableModel, inspection().vm().platform().wordSize().toInt()), null);
            addColumn(ThreadLocalVariablesColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ThreadLocalVariablesColumnKind.VALUE, new ValueRenderer(), null);
            addColumn(ThreadLocalVariablesColumnKind.REGION, new MemoryRegionPointerTableCellRenderer(inspection(), table, tableModel), null);
        }
    }

    /**
     * Models the name/value pairs in a VM thread local storage area.
     * Each row displays a variable with index equal to the row number.
     */
    private final class ThreadLocalsAreaTableModel extends InspectorMemoryTableModel {

        private final MaxThreadLocalsArea tla;
        private final String[] threadLocalDescriptions;

        public ThreadLocalsAreaTableModel(Inspection inspection, MaxThreadLocalsArea tla) {
            super(inspection, tla.memoryRegion().start());
            this.tla = tla;
            threadLocalDescriptions = new String[tla.variableCount()];
            for (int row = 0; row < tla.variableCount(); row++) {
                threadLocalDescriptions[row] = "Thread local variable " + tla.getThreadLocalVariable(row).variableName();
            }
        }

        public int getColumnCount() {
            return ThreadLocalVariablesColumnKind.values().length;
        }

        public int getRowCount() {
            return tla.variableCount();
        }

        public MaxThreadLocalVariable getValueAt(int row, int col) {
            return tla.getThreadLocalVariable(row);
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return MaxThreadLocalVariable.class;
        }

        @Override
        public Address getAddress(int row) {
            return tla.getThreadLocalVariable(row).memoryRegion().start();
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            return tla.getThreadLocalVariable(row).memoryRegion();
        }

        @Override
        public Offset getOffset(int row) {
            return Offset.fromInt(tla.getThreadLocalVariable(row).offset());
        }

        @Override
        public int findRow(Address address) {
            final MaxThreadLocalVariable threadLocalVariable = tla.findThreadLocalVariable(address);
            return threadLocalVariable == null ? -1 : threadLocalVariable.index();
        }

        @Override
        public String getRowDescription(int row) {
            return threadLocalDescriptions[row];
        }

        public MaxThread getThread() {
            return tla.thread();
        }

        public MaxThreadLocalsArea getTeleThreadLocalValues() {
            return tla;
        }

        public Value rowToVariableValue(int row) {
            return tla.getThreadLocalVariable(row).value();
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThreadLocalVariable threadLocalVariable = (MaxThreadLocalVariable) value;
            setValue(threadLocalVariable.variableName());
            setWrappedToolTipText(tableModel.getRowDescription(row) + "<br>" +
                            "Description = \"" + threadLocalVariable.variableDocumentation() + "\"<br>" +
                            "Declared in " + threadLocalVariable.declaration());
            if (threadLocalVariable.isReference()) {
                setForeground(style().wordValidObjectReferenceDataColor());
            } else {
                setForeground(cellForegroundColor(row, column));
            }
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[tableModel.getRowCount()];

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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, final int column) {
            final MaxThreadLocalVariable threadLocalVariable = (MaxThreadLocalVariable) value;
            if (labels[row] == null) {
                final ValueMode valueMode = threadLocalVariable.isReference() ? ValueMode.REFERENCE : ValueMode.WORD;
                labels[row] = new WordValueLabel(inspection(), valueMode, ThreadLocalsAreaTable.this) {
                    @Override
                    public Value fetchValue() {
                        return tableModel.rowToVariableValue(row);
                    }
                    @Override
                    public void updateText() {
                        super.updateText();
                        ThreadLocalsAreaTable.this.repaint();
                    }
                };
                labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value = ");
                labels[row].setOpaque(true);
            }
            labels[row].setBackground(cellBackgroundColor(isSelected));
            return labels[row];
        }
    }

}
