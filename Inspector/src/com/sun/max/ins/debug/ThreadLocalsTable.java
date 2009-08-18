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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
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

    private final ThreadLocalsViewPreferences preferences;

    private final ThreadLocalsTableModel model;
    private final ThreadLocalsTableColumnModel columnModel;
    private final TableColumn[] columns;



    private MaxVMState lastRefreshedState = null;


    private final class ToggleThreadLocalsWatchpointAction extends InspectorAction {

        private final int row;

        public ToggleThreadLocalsWatchpointAction(Inspection inspection, String name, int row) {
            super(inspection, name);
            this.row = row;
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = model.getWatchpoint(row);
            if (watchpoint == null) {
                actions().setThreadLocalWatchpoint(model.getTeleThreadLocalValues(), row, null).perform();
            } else {
                watchpoint.dispose();
            }
        }
    }

    /**
     * A {@link JTable} specialized to display Maxine thread local fields.
     */
    public ThreadLocalsTable(Inspection inspection, final TeleThreadLocalValues threadLocalValues, ThreadLocalsViewPreferences preferences) {
        super(inspection);
        this.preferences = preferences;
        this.model = new ThreadLocalsTableModel(threadLocalValues);
        this.columns = new TableColumn[ThreadLocalsColumnKind.VALUES.length()];
        this.columnModel = new ThreadLocalsTableColumnModel(inspection);
        configureMemoryTable(model, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && maxVM().watchpointsEnabled()) {
            final InspectorAction action = new ToggleThreadLocalsWatchpointAction(inspection(), null, row);
            action.perform();
        }
    }

    @Override
    protected InspectorMenu getDynamicMenu(int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled() && col == ThreadLocalsColumnKind.TAG.ordinal()) {
            final InspectorMenu menu = new InspectorMenu();
            final MemoryRegion memoryRegion = model.getMemoryRegion(row);
            menu.add(new ToggleThreadLocalsWatchpointAction(inspection(), "Toggle watchpoint (double-click)", row));
            menu.add(actions().setThreadLocalWatchpoint(model.getTeleThreadLocalValues(), row, "Watch this memory location"));
            menu.add(new WatchpointSettingsMenu(model.getWatchpoint(row)));
            menu.add(actions().removeWatchpoint(memoryRegion, "Remove memory watchpoint"));
            return menu;
        }
        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < model.getRowCount()) {
                inspection().focus().setAddress(model.getAddress(row));
            }
        }
    }

    private void setAddressFocus(Address address) {
        inspection().focus().setAddress(address);
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            final int oldSelectedRow = getSelectedRow();
            final int newRow = model.findRow(focus().address());
            if (newRow >= 0) {
                getSelectionModel().setSelectionInterval(newRow, newRow);
            } else {
                if (oldSelectedRow >= 0) {
                    getSelectionModel().clearSelection();
                }
            }
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
    }

     /**
     * Add tool tip text to the column headers, as specified by {@link ThreadLocalsColumnKind}.
     *
     * @see javax.swing.JTable#createDefaultTableHeader()
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        final JTableHeader header = new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return ThreadLocalsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
        return header;
    }

/**
     * Models the name/value pairs in a VM thread local storage area.
     * Each row displays a variable with index equal to the row number.
     */
    private final class ThreadLocalsTableModel extends AbstractTableModel implements InspectorMemoryTableModel {

        private final TeleThreadLocalValues teleThreadLocalValues;

        public ThreadLocalsTableModel(TeleThreadLocalValues teleThreadLocalValues) {
            this.teleThreadLocalValues = teleThreadLocalValues;
        }

        public MaxThread getThread() {
            return teleThreadLocalValues.getMaxThread();
        }

        public TeleThreadLocalValues getTeleThreadLocalValues() {
            return teleThreadLocalValues;
        }

        public int getColumnCount() {
            return ThreadLocalsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return teleThreadLocalValues.valueCount();
        }

        public VmThreadLocal getValueAt(int row, int col) {
            return teleThreadLocalValues.getVmThreadLocal(row);
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return VmThreadLocal.class;
        }

        public Address getAddress(int row) {
            return teleThreadLocalValues.getAddress(row);
        }

        public MemoryRegion getMemoryRegion(int row) {
            return teleThreadLocalValues.getMemoryRegion(row);
        }

        /**
         * @return the memory watchpoint, if any, that is active at a row
         */
        public MaxWatchpoint getWatchpoint(int row) {
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (watchpoint.overlaps(getMemoryRegion(row))) {
                    return watchpoint;
                }
            }
            return null;
        }

        public Address getOrigin() {
            return teleThreadLocalValues.start();
        }

        public Offset getOffset(int row) {
            return Offset.fromInt(teleThreadLocalValues.getVmThreadLocal(row).offset);
        }

        /**
         * @return the row containing a thread local variable stored at the specified address, null if none.
         */
        public int findRow(Address address) {
            final VmThreadLocal vmThreadLocal = teleThreadLocalValues.findVmThreadLocal(address);
            return vmThreadLocal == null ? -1 : vmThreadLocal.index;
        }

        public Value rowToVariableValue(int row) {
            final VmThreadLocal vmThreadLocal = teleThreadLocalValues.getVmThreadLocal(row);
            if (vmThreadLocal != null) {
                final String name = vmThreadLocal.name;
                if (teleThreadLocalValues.isValid(name)) {
                    return new WordValue(Address.fromLong(teleThreadLocalValues.getValue(name)));
                }
            }
            return VoidValue.VOID;
        }
    }

    /**
     * A column model for thread local values.
     * Column selection is driven by choices in the parent.
     * This implementation cannot update column choices dynamically.
     */
    private final class ThreadLocalsTableColumnModel extends DefaultTableColumnModel {

        ThreadLocalsTableColumnModel(Inspection inspection) {
            createColumn(ThreadLocalsColumnKind.TAG, new TagRenderer(inspection));
            createColumn(ThreadLocalsColumnKind.ADDRESS, new AddressRenderer(inspection));
            createColumn(ThreadLocalsColumnKind.POSITION, new PositionRenderer(inspection));
            createColumn(ThreadLocalsColumnKind.NAME, new NameRenderer(inspection));
            createColumn(ThreadLocalsColumnKind.VALUE, new ValueRenderer());
            createColumn(ThreadLocalsColumnKind.REGION, new RegionRenderer());
        }

        private void createColumn(ThreadLocalsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (preferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && model.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(model.getMemoryRegion(row), model.getThread(), model.getWatchpoint(row));
            renderer.setForeground(getRowTextColor(row));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final VmThreadLocal vmThreadLocal = (VmThreadLocal) value;
            setValue(Offset.fromInt(vmThreadLocal.offset), model.getTeleThreadLocalValues().start());
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection, 0, Address.zero(), Word.size());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final VmThreadLocal vmThreadLocal = (VmThreadLocal) value;
            setValue(Offset.fromInt(vmThreadLocal.offset), model.getTeleThreadLocalValues().start());
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final VmThreadLocal vmThreadLocal = (VmThreadLocal) value;
            setValue(vmThreadLocal.name);
            setToolTipText("<html>" + (vmThreadLocal.description.length() > 0 ? vmThreadLocal.description + "<br>" : "") + "Declaration: " + vmThreadLocal.declaration);
            if (vmThreadLocal.kind == Kind.REFERENCE) {
                setForeground(style().wordValidObjectReferenceDataColor());
            } else {
                setForeground(getRowTextColor(row));
            }
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[model.getRowCount()];

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
            final VmThreadLocal vmThreadLocal = (VmThreadLocal) value;
            InspectorLabel label = labels[row];
            if (label == null) {
                final ValueMode valueMode = vmThreadLocal.kind == Kind.REFERENCE ? ValueMode.REFERENCE : ValueMode.WORD;
                label = new WordValueLabel(inspection(), valueMode, ThreadLocalsTable.this) {
                    @Override
                    public Value fetchValue() {
                        return model.rowToVariableValue(row);
                    }
                    @Override
                    public void updateText() {
                        super.updateText();
                        ThreadLocalsTable.this.repaint();
                    }
                };
                labels[row] = label;
            }
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[model.getRowCount()];

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
                        return model.rowToVariableValue(row);
                    }
                };
                labels[row] = label;
            }
            return label;
        }
    }

}
