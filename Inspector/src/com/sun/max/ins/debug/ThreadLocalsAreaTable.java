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
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays Maxine VM thread local variable names and values in a thread locals area, to be used within an
 * instance of {@link ThreadLocalsInspector}.
 *
 * @author Michael Van De Vanter
  */
public final class ThreadLocalsAreaTable extends InspectorTable {

    private final ThreadLocalsAreaTableModel tableModel;
    private final ThreadLocalsAreaTableColumnModel columnModel;

    /**
     * A table specialized to display Maxine thread local fields.
     */
    public ThreadLocalsAreaTable(Inspection inspection, final MaxThreadLocalsArea threadLocalsArea, ThreadLocalsViewPreferences viewPreferences) {
        super(inspection);
        this.tableModel = new ThreadLocalsAreaTableModel(inspection, threadLocalsArea);
        this.columnModel = new ThreadLocalsAreaTableColumnModel(viewPreferences);
        configureMemoryTable(tableModel, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setThreadLocalWatchpoint(tableModel.getValueAt(row, 0), null).perform();
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
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
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
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
     * A column model for thread local values.
     * Column selection is driven by choices in the parent.
     * This implementation cannot update column choices dynamically.
     */
    private final class ThreadLocalsAreaTableColumnModel extends InspectorTableColumnModel<ThreadLocalVariablesColumnKind> {

        ThreadLocalsAreaTableColumnModel(ThreadLocalsViewPreferences viewPreferences) {
            super(ThreadLocalVariablesColumnKind.VALUES.length(), viewPreferences);
            addColumn(ThreadLocalVariablesColumnKind.TAG, new TagRenderer(inspection()), null);
            addColumn(ThreadLocalVariablesColumnKind.ADDRESS, new AddressRenderer(inspection()), null);
            addColumn(ThreadLocalVariablesColumnKind.POSITION, new PositionRenderer(inspection()), null);
            addColumn(ThreadLocalVariablesColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ThreadLocalVariablesColumnKind.VALUE, new ValueRenderer(), null);
            addColumn(ThreadLocalVariablesColumnKind.REGION, new RegionRenderer(), null);
        }
    }

    /**
     * Models the name/value pairs in a VM thread local storage area.
     * Each row displays a variable with index equal to the row number.
     */
    private final class ThreadLocalsAreaTableModel extends InspectorMemoryTableModel {

        private final MaxThreadLocalsArea threadLocalsArea;

        public ThreadLocalsAreaTableModel(Inspection inspection, MaxThreadLocalsArea threadLocalsArea) {
            super(inspection, threadLocalsArea.memoryRegion().start());
            this.threadLocalsArea = threadLocalsArea;
        }

        public int getColumnCount() {
            return ThreadLocalVariablesColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return threadLocalsArea.variableCount();
        }

        public MaxThreadLocalVariable getValueAt(int row, int col) {
            return threadLocalsArea.getThreadLocalVariable(row);
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return MaxThreadLocalVariable.class;
        }

        public MaxThread getThread() {
            return threadLocalsArea.thread();
        }

        public MaxThreadLocalsArea getTeleThreadLocalValues() {
            return threadLocalsArea;
        }

        @Override
        public Address getAddress(int row) {
            return threadLocalsArea.getThreadLocalVariable(row).memoryRegion().start();
        }

        @Override
        public MemoryRegion getMemoryRegion(int row) {
            return threadLocalsArea.getThreadLocalVariable(row).memoryRegion();
        }

        @Override
        public Offset getOffset(int row) {
            return Offset.fromInt(threadLocalsArea.getThreadLocalVariable(row).offset());
        }

        @Override
        public int findRow(Address address) {
            final MaxThreadLocalVariable threadLocalVariable = threadLocalsArea.findThreadLocalVariable(address);
            return threadLocalVariable == null ? -1 : threadLocalVariable.index();
        }

        public Value rowToVariableValue(int row) {
            return threadLocalsArea.getThreadLocalVariable(row).value();
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(tableModel.getMemoryRegion(row), tableModel.getThread(), tableModel.getWatchpoints(row));
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(cellBackgroundColor(isSelected));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final MaxThreadLocalVariable threadLocalVariable = (MaxThreadLocalVariable) value;
            setValue(Offset.fromInt(threadLocalVariable.offset()), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection, 0, Address.zero(), Word.size());
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final MaxThreadLocalVariable threadLocalVariable = (MaxThreadLocalVariable) value;
            setValue(Offset.fromInt(threadLocalVariable.offset()), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final MaxThreadLocalVariable threadLocalVariable = (MaxThreadLocalVariable) value;
            setValue(threadLocalVariable.name());
            setToolTipText("<html>" + (threadLocalVariable.description().length() > 0 ? threadLocalVariable.description() + "<br>" : "") + "Declaration: " + threadLocalVariable.declaration());
            if (threadLocalVariable.isReference()) {
                setForeground(style().wordValidObjectReferenceDataColor());
            } else {
                setForeground(getRowTextColor(row));
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
            InspectorLabel label = labels[row];
            if (label == null) {
                final ValueMode valueMode = threadLocalVariable.isReference() ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection(), valueMode, ThreadLocalsAreaTable.this) {
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
                label.setOpaque(true);
                labels[row] = label;
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            InspectorLabel label = labels[row];
            if (label == null) {
                label = new MemoryRegionValueLabel(inspection()) {
                    @Override
                    public Value fetchValue() {
                        return tableModel.rowToVariableValue(row);
                    }
                };
                label.setOpaque(true);
                labels[row] = label;
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }

}
