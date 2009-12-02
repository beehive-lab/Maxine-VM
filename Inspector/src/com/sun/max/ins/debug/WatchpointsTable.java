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
import java.lang.ref.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.runtime.*;
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
        tableModel = new WatchpointsTableModel();
        columnModel = new WatchpointsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled() && col == WatchpointsColumnKind.DESCRIPTION.ordinal()) {
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
        final MaxWatchpoint watchpoint = inspection().focus().watchpoint();
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
            super(WatchpointsColumnKind.VALUES.length(), viewPreferences);
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
            addColumn(WatchpointsColumnKind.EAGER, null, new DefaultCellEditor(new JCheckBox()));
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

        public int getColumnCount() {
            return WatchpointsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return maxVM().watchpoints().length();
        }

        public Object getValueAt(int row, int col) {
            final MaxWatchpoint watchpoint = rowToWatchpoint(row);
            switch (WatchpointsColumnKind.VALUES.get(col)) {
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
                    return watchpoint.isRead();
                case WRITE:
                    return watchpoint.isWrite();
                case EXEC:
                    return watchpoint.isExec();
                case GC:
                    return watchpoint.isEnabledDuringGC();
                case EAGER:
                    return watchpoint.isEagerRelocationUpdateSet();
                default:
                    throw FatalError.unexpected("Unexpected Watchpoint Data column");
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            Boolean newState;
            final MaxWatchpoint watchpoint = rowToWatchpoint(row);

            switch (WatchpointsColumnKind.VALUES.get(column)) {
                case READ:
                    newState = (Boolean) value;
                    if (watchpoint.setRead(newState)) {
                        inspection().settings().save();
                    }
                    break;
                case WRITE:
                    newState = (Boolean) value;
                    if (watchpoint.setWrite(newState)) {
                        inspection().settings().save();
                    }
                    break;
                case EXEC:
                    newState = (Boolean) value;
                    if (watchpoint.setExec(newState)) {
                        inspection().settings().save();
                    }
                    break;
                case GC:
                    newState = (Boolean) value;
                    watchpoint.setEnabledDuringGC(newState);
                    inspection().settings().save();
                    break;
                case EAGER:
                    newState = (Boolean) value;
                    watchpoint.setEagerRelocationUpdate(newState);
                    inspection().settings().save();
                    break;
                default:
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            switch (WatchpointsColumnKind.VALUES.get(column)) {
                case READ:
                case WRITE:
                case EXEC:
                case GC:
                case EAGER:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (WatchpointsColumnKind.VALUES.get(c)) {
                case READ:
                    return Boolean.class;
                case WRITE:
                    return Boolean.class;
                case EXEC:
                    return Boolean.class;
                case GC:
                    return Boolean.class;
                case EAGER:
                    return Boolean.class;
                default:
                    return MaxWatchpoint.class;
            }
        }

        MaxWatchpoint rowToWatchpoint(int row) {
            int count = 0;
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (count == row) {
                    return watchpoint;
                }
                count++;
            }
            throw FatalError.unexpected("WatchpointsInspector.get(" + row + ") failed");
        }

        int findRow(MaxWatchpoint findWatchpoint) {
            int row = 0;
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
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
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && tableModel.rowToWatchpoint(row).contains(watchpointEvent.address())) {
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
            setFont(inspection().style().defaultFont());
            // See if any registers point here
            final MaxThread thread = inspection().focus().thread();
            if (thread != null) {
                final TeleIntegerRegisters teleIntegerRegisters = thread.integerRegisters();
                if (teleIntegerRegisters == null) {
                    // Return a specialized renderer with its own content.
                    label = inspection().gui().getUnavailableDataTableCellRenderer();
                } else {
                    final String registerNameList = teleIntegerRegisters.findAsNameList(watchpoint);
                    if (registerNameList.isEmpty()) {
                        label.setForeground(inspection().style().memoryDefaultTagTextColor());
                    } else {
                        labelText += registerNameList + "-->";
                        toolTipText += "Register(s): " + registerNameList + " in thread " + inspection().nameDisplay().longName(thread) + " point at this location";
                        setForeground(inspection().style().memoryRegisterTagTextColor());
                    }
                }
            }
            // If a watchpoint is currently triggered here, add a pointer icon.
            final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
            if (watchpointEvent != null && tableModel.rowToWatchpoint(row).contains(watchpointEvent.address())) {
                label.setIcon(inspection().style().debugIPTagIcon());
                label.setForeground(inspection().style().debugIPTagColor());
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
                super(inspection, valueMode, watchpoint.start(), WatchpointsTable.this);
                this.watchpoint = watchpoint;
                setOpaque(true);
            }

            @Override
            public Value fetchValue() {
                return watchpoint == null ? null : new WordValue(watchpoint.start());
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
            setValue(watchpoint.size().toInt());
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
                super(inspection, valueMode, watchpoint.end(), WatchpointsTable.this);
                this.watchpoint = watchpoint;
            }

            @Override
            public Value fetchValue() {
                return watchpoint == null ? null : new WordValue(watchpoint.end());
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
            final String description = watchpoint.description();
            setText(description);
            setToolTipText(description);
            // TODO (mlvdv)  Abstract this string, or come up with a method/predicate instead
            if (description.equals("RegionWatchpoint - GC removed corresponding Object")) {
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
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setValue(new WordValue(watchpoint.start()));
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
            final MaxWatchpointEvent watchpointEvent = maxVM().maxVMState().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.maxWatchpoint() == watchpoint) {
                final MaxThread maxThread = watchpointEvent.maxThread();
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
            final MaxWatchpointEvent watchpointEvent = maxVM().maxVMState().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.maxWatchpoint() == watchpoint) {
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
            final MaxWatchpointEvent watchpointEvent = maxVM().maxVMState().watchpointEvent();
            if (watchpointEvent != null && watchpointEvent.maxWatchpoint() == watchpoint) {
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
