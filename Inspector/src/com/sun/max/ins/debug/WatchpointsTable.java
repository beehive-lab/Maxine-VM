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
import java.lang.ref.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying memory watchpoints in the VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public final class WatchpointsTable extends InspectorTable {

    private final WatchpointsTableModel tableModel;
    private final WatchpointsColumnModel columnModel;

    WatchpointsTable(Inspection inspection, WatchpointsViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new WatchpointsTableModel(inspection);
        columnModel = new WatchpointsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null && col == WatchpointsColumnKind.DESCRIPTION.ordinal()) {
            final InspectorPopupMenu menu = new InspectorPopupMenu("Watchpoints");
            final MaxWatchpoint watchpoint = (MaxWatchpoint) tableModel.getValueAt(row, col);
            final TeleObject teleObject = watchpoint.getTeleObject();
            if (teleObject != null) {
                menu.add(actions().inspectObject(teleObject, "Inspect Object"));
                return menu;
            }
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory watchpoint, if any, that is the current user focus.
        final MaxWatchpoint watchpoint = focus().watchpoint();
        final int row = tableModel.findRow(watchpoint);
        updateSelection(row);
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // Row selection changed, perhaps by user mouse click or navigation;
        // update user focus to follow the selection.
        super.valueChanged(listSelectionEvent);
        if (!listSelectionEvent.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0) {
                final MaxWatchpoint watchpoint = tableModel.rowToWatchpoint(row);
                focus().setWatchpoint(watchpoint);
            }
        }
    }

    private final class WatchpointsColumnModel extends InspectorTableColumnModel<WatchpointsColumnKind> {

        private WatchpointsColumnModel(WatchpointsViewPreferences viewPreferences) {
            super(WatchpointsColumnKind.values().length, viewPreferences);
            addColumn(WatchpointsColumnKind.TAG, new TagCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.START, new StartAddressCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.SIZE, new SizeCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.END, new EndAddressCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.DESCRIPTION, new DescriptionCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.REGION, new RegionRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.READ, null, new DefaultCellEditor(new JCheckBox()));
            addColumn(WatchpointsColumnKind.WRITE, null, new DefaultCellEditor(new JCheckBox()));
            addColumn(WatchpointsColumnKind.EXEC, null, new DefaultCellEditor(new JCheckBox()));
            addColumn(WatchpointsColumnKind.GC, null, new DefaultCellEditor(new JCheckBox()));
            addColumn(WatchpointsColumnKind.TRIGGERED_THREAD, new TriggerThreadCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.ADDRESS_TRIGGERED, new TriggerAddressCellRenderer(inspection()), null);
            addColumn(WatchpointsColumnKind.CODE_TRIGGERED, new TriggerCodeCellRenderer(inspection()), null);
        }

    }

    /**
     * A table data model built around the list of current watchpoints in the VM.
     *
     * @author Michael Van De Vanter
     */
    private final class WatchpointsTableModel extends InspectorTableModel {

        public WatchpointsTableModel(Inspection inspection) {
            super(inspection);
        }

        public int getColumnCount() {
            return WatchpointsColumnKind.values().length;
        }

        public int getRowCount() {
            return vm().watchpointManager().watchpoints().size();
        }

        public Object getValueAt(int row, int col) {
            final MaxWatchpoint watchpoint = rowToWatchpoint(row);
            switch (WatchpointsColumnKind.values()[col]) {
                case TAG:
                case START:
                case SIZE:
                case END:
                case DESCRIPTION:
                case REGION:
                case TRIGGERED_THREAD:
                case ADDRESS_TRIGGERED:
                case CODE_TRIGGERED:
                    return watchpoint;
                case READ:
                    return watchpoint.getSettings().trapOnRead;
                case WRITE:
                    return watchpoint.getSettings().trapOnWrite;
                case EXEC:
                    return watchpoint.getSettings().trapOnExec;
                case GC:
                    return watchpoint.getSettings().enabledDuringGC;
                default:
                    throw InspectorError.unexpected("Unexpected Watchpoint Data column");
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            final MaxWatchpoint watchpoint = rowToWatchpoint(row);

            switch (WatchpointsColumnKind.values()[column]) {
                case READ: {
                    final Boolean newState = (Boolean) value;
                    try {
                        if (watchpoint.setTrapOnRead(newState)) {
                            inspection().settings().save();
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        final DefaultCellEditor editor = (DefaultCellEditor) columnModel.columnAt(column).getCellEditor();
                        final JCheckBox checkBox = (JCheckBox) editor.getComponent();
                        // System.out.println("Reset READ checkbox at row=" + row + ", col=" + column);
                        checkBox.setSelected(!newState);
                        inspection().announceVMBusyFailure("Watchpoint READ setting");
                    }
                    break;
                }
                case WRITE: {
                    final Boolean newState = (Boolean) value;
                    try {
                        if (watchpoint.setTrapOnWrite(newState)) {
                            inspection().settings().save();
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        final DefaultCellEditor editor = (DefaultCellEditor) columnModel.columnAt(column).getCellEditor();
                        final JCheckBox checkBox = (JCheckBox) editor.getComponent();
                        // System.out.println("Reset WRITE checkbox at row=" + row + ", col=" + column);
                        checkBox.setSelected(!newState);
                        inspection().announceVMBusyFailure("Watchpoint WRITE setting");
                    }
                    break;
                }
                case EXEC: {
                    final Boolean newState = (Boolean) value;
                    try {
                        if (watchpoint.setTrapOnExec(newState)) {
                            inspection().settings().save();
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        final DefaultCellEditor editor = (DefaultCellEditor) columnModel.columnAt(column).getCellEditor();
                        final JCheckBox checkBox = (JCheckBox) editor.getComponent();
                        // System.out.println("Reset EXEC checkbox at row=" + row + ", col=" + column);
                        checkBox.setSelected(!newState);
                        inspection().announceVMBusyFailure("Watchpoint EXEC setting");
                    }
                    break;
                }
                case GC: {
                    final Boolean newState = (Boolean) value;
                    try {
                        watchpoint.setEnabledDuringGC(newState);
                        inspection().settings().save();
                    } catch (MaxVMBusyException maxVMBusyException) {
                        final DefaultCellEditor editor = (DefaultCellEditor) columnModel.columnAt(column).getCellEditor();
                        final JCheckBox checkBox = (JCheckBox) editor.getComponent();
                        // System.out.println("Reset GC checkbox at row=" + row + ", col=" + column);
                        checkBox.setSelected(!newState);
                        inspection().announceVMBusyFailure("Watchpoint GC setting");
                    }
                    break;
                }
                default:
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            switch (WatchpointsColumnKind.values()[column]) {
                case READ:
                    return true;
                case WRITE:
                    return true;
                case EXEC:
                    return true;
                case GC:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (WatchpointsColumnKind.values()[c]) {
                case READ:
                    return Boolean.class;
                case WRITE:
                    return Boolean.class;
                case EXEC:
                    return Boolean.class;
                case GC:
                    return Boolean.class;
                default:
                    return MaxWatchpoint.class;
            }
        }

        MaxWatchpoint rowToWatchpoint(int row) {
            int count = 0;
            for (MaxWatchpoint watchpoint : vm().watchpointManager().watchpoints()) {
                if (count == row) {
                    return watchpoint;
                }
                count++;
            }
            throw InspectorError.unexpected("WatchpointsInspector.get(" + row + ") failed");
        }

        int findRow(MaxWatchpoint findWatchpoint) {
            int row = 0;
            for (MaxWatchpoint watchpoint : vm().watchpointManager().watchpoints()) {
                if (watchpoint.equals(findWatchpoint)) {
                    return row;
                }
                row++;
            }
            return -1;
        }
    }

    /**
     * @return color the text specially in the row where a triggered watchpoint is displayed
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.rowToWatchpoint(row).memoryRegion().contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    private final class TagCellRenderer extends JLabel implements TableCellRenderer {

        TagCellRenderer(Inspection inspection) {
            super("");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            JLabel label = this;
            String labelText = "";
            String toolTipText = "";
            setFont(style().defaultFont());
            // See if any registers point here
            final MaxThread thread = focus().thread();
            if (thread != null) {
                final List<MaxRegister> registers = thread.registers().find(watchpoint.memoryRegion());
                if (registers.isEmpty()) {
                    label.setForeground(style().memoryDefaultTagTextColor());
                } else {
                    final String registerNameList = inspection().nameDisplay().registerNameList(registers);
                    labelText += registerNameList + "-->";
                    toolTipText += "Register(s): " + registerNameList + " in thread " + inspection().nameDisplay().longName(thread) + " point at this location";
                    setForeground(style().memoryRegisterTagTextColor());
                }
            }
            // If a watchpoint is currently triggered here, add a pointer icon.
            final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
            if (watchpointEvent != null && tableModel.rowToWatchpoint(row).memoryRegion().contains(watchpointEvent.address())) {
                label.setIcon(style().debugIPTagIcon());
                label.setForeground(style().debugIPTagColor());
            } else {
                label.setIcon(null);
                label.setForeground(null);
            }
            label.setText(labelText);
            label.setToolTipText(toolTipText);
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }

    private final class StartAddressCellRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per watchpoint and keep them around
        private final Map<MaxWatchpoint, WeakReference<WordValueLabel> > watchpointToLabelMap = new HashMap<MaxWatchpoint, WeakReference<WordValueLabel> >();

        public StartAddressCellRenderer(Inspection inspection) {
            this.inspection = inspection;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            WeakReference<WordValueLabel> labelReference = watchpointToLabelMap.get(watchpoint);
            if (labelReference != null && labelReference.get() == null) {
                // has been collected
                watchpointToLabelMap.remove(labelReference);
                labelReference = null;
            }
            if (labelReference == null) {
                labelReference = new WeakReference<WordValueLabel>(new WatchpointStartWordValueLabel(inspection(), ValueMode.WORD, watchpoint));
                watchpointToLabelMap.put(watchpoint, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (WeakReference<WordValueLabel> labelReference : watchpointToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (WeakReference<WordValueLabel> labelReference : watchpointToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        private final class WatchpointStartWordValueLabel extends WordValueLabel {

            private final MaxWatchpoint watchpoint;

            WatchpointStartWordValueLabel(Inspection inspection, WordValueLabel.ValueMode valueMode, MaxWatchpoint watchpoint) {
                super(inspection, valueMode, watchpoint.memoryRegion().start(), WatchpointsTable.this);
                this.watchpoint = watchpoint;
                setOpaque(true);
            }

            @Override
            public Value fetchValue() {
                return watchpoint == null ? null : new WordValue(watchpoint.memoryRegion().start());
            }
        }
    }

    private final class SizeCellRenderer extends DataLabel.IntAsDecimal implements TableCellRenderer {

        public SizeCellRenderer(Inspection inspection) {
            super(inspection, 0);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setValue(watchpoint.memoryRegion().size().toInt());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class EndAddressCellRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per watchpoint and keep them around
        private final Map<MaxWatchpoint, WeakReference<WordValueLabel> > watchpointToLabelMap = new HashMap<MaxWatchpoint, WeakReference<WordValueLabel> >();

        public EndAddressCellRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            WeakReference<WordValueLabel> labelReference = watchpointToLabelMap.get(watchpoint);
            if (labelReference != null && labelReference.get() == null) {
                // has been collected
                watchpointToLabelMap.remove(labelReference);
                labelReference = null;
            }
            if (labelReference == null) {
                final WatchpointEndWordValueLabel newLabel = new WatchpointEndWordValueLabel(inspection(), ValueMode.WORD, watchpoint);
                newLabel.setOpaque(true);
                labelReference = new WeakReference<WordValueLabel>(newLabel);
                watchpointToLabelMap.put(watchpoint, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (WeakReference<WordValueLabel> labelReference : watchpointToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (WeakReference<WordValueLabel> labelReference : watchpointToLabelMap.values()) {
                final WordValueLabel label = labelReference.get();
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        private final class WatchpointEndWordValueLabel extends WordValueLabel {

            private final MaxWatchpoint watchpoint;

            WatchpointEndWordValueLabel(Inspection inspection, WordValueLabel.ValueMode valueMode, MaxWatchpoint watchpoint) {
                super(inspection, valueMode, watchpoint.memoryRegion().end(), WatchpointsTable.this);
                this.watchpoint = watchpoint;
            }

            @Override
            public Value fetchValue() {
                return watchpoint == null ? null : new WordValue(watchpoint.memoryRegion().end());
            }
        }
    }

    private final class DescriptionCellRenderer extends PlainLabel implements TableCellRenderer {

        public DescriptionCellRenderer(Inspection inspection) {
            super(inspection, "");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            final TeleObject teleObject = watchpoint.getTeleObject();
            final StringBuilder sb = new StringBuilder();
            sb.append(watchpoint.description());
            if (teleObject != null) {
                sb.append(": ").append(inspection().nameDisplay().referenceLabelText(teleObject));
            }
            final String description = sb.toString();
            setText(description);
            setToolTipText(description);
            // TODO (mlvdv)  Abstract this string, or come up with a method/predicate instead
            if (watchpoint.memoryRegion().regionName().equals("RegionWatchpoint - GC removed corresponding Object")) {
                setForeground(Color.RED);
            } else {
                setForeground(getRowTextColor(row));
            }
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {

        public RegionRenderer(Inspection inspection) {
            super(inspection, "Start address");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setValue(new WordValue(watchpoint.memoryRegion().start()));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class TriggerThreadCellRenderer extends PlainLabel implements TableCellRenderer {

        TriggerThreadCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = tableModel.rowToWatchpoint(row);
            final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.watchpoint() == watchpoint) {
                final MaxThread maxThread = watchpointEvent.thread();
                setText(inspection().nameDisplay().longName(maxThread));
                setToolTipText("Thread \"" + inspection().nameDisplay().longName(maxThread) + "\" stopped at this watchpoint");
            } else {
                setText("");
                setToolTipText("No Thread stopped at this watchpoint");
            }
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class TriggerAddressCellRenderer extends PlainLabel implements TableCellRenderer {

        TriggerAddressCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = tableModel.rowToWatchpoint(row);
            final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.watchpoint() == watchpoint) {
                final String addressText = watchpointEvent.address().toHexString();
                setText(addressText);
                setToolTipText("Access of memory location " + addressText + " triggered watchpoint");
            } else {
                setText("");
                setToolTipText("No Thread stopped at this watchpoint");
            }
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class TriggerCodeCellRenderer extends PlainLabel implements TableCellRenderer {

        TriggerCodeCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = tableModel.rowToWatchpoint(row);
            final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.watchpoint() == watchpoint) {
                final int watchpointCode = watchpointEvent.eventCode();
                String codeName;
                switch(watchpointCode) {
                    case 1:
                        codeName = "read";
                        break;
                    case 2:
                        codeName = "write";
                        break;
                    case 3:
                        codeName = "exec";
                        break;
                    default:
                        codeName = "unknown";
                }
                codeName += "(" + String.valueOf(watchpointCode) + ")";
                setText(codeName);
                setToolTipText("Watchpoint trigger code=" + codeName);
            } else {
                setText("");
                setToolTipText("No Thread stopped at this watchpoint");
            }
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }
}
