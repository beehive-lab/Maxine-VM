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
import com.sun.max.tele.object.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying memory watchpoints in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class WatchpointsTable extends InspectorTable {

    private final WatchpointsTableModel tableModel;
    private final WatchpointsColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    WatchpointsTable(Inspection inspection, WatchpointsViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new WatchpointsTableModel();
        columns = new TableColumn[WatchpointsColumnKind.VALUES.length()];
        columnModel = new WatchpointsColumnModel(viewPreferences);

        configure(tableModel, columnModel);

        //TODO: generalize this
        addMouseListener(new TableCellMouseClickAdapter(inspection(), this) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON3) {
                    if (maxVM().watchpointsEnabled()) {
                        // So far, only watchpoint-related items on this popup menu.
                        final Point p = mouseEvent.getPoint();
                        final int hitRowIndex = rowAtPoint(p);
                        final int columnIndex = getColumnModel().getColumnIndexAtX(p.x);
                        final int modelIndex = getColumnModel().getColumn(columnIndex).getModelIndex();
                        if (modelIndex == WatchpointsColumnKind.DESCRIPTION.ordinal()) {
                            final InspectorMenu menu = new InspectorMenu();
                            final MaxWatchpoint watchpoint = (MaxWatchpoint) tableModel.getValueAt(hitRowIndex, modelIndex);
                            final TeleObject teleObject = watchpoint.getTeleObject();
                            if (teleObject != null) {
                                menu.add(actions().inspectObject(teleObject, "Inspect Object"));
                                menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                            }
                        }
                    }
                }
                super.procedure(mouseEvent);
            }
        }
        );
    }

    /**
     * Sets table selection to the memory watchpoint, if any, that is the current user focus.
     */
    @Override
    public void updateFocusSelection() {
        final MaxWatchpoint watchpoint = inspection().focus().watchpoint();
        final int row = tableModel.findRow(watchpoint);
        if (row < 0) {
            clearSelection();
        } else  if (row != getSelectedRow()) {
            setRowSelectionInterval(row, row);
        }
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            tableModel.refresh();
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                if (prober != null) {
                    prober.refresh(force);
                }
            }
        }
        invalidate();
        repaint();
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            if (prober != null) {
                prober.redisplay();
            }
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
                return WatchpointsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // Row selection changed, perhaps by user mouse click or navigation;
        // update user focus to follow the selection.
        super.valueChanged(listSelectionEvent);
        if (!listSelectionEvent.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0) {
                final MaxWatchpoint watchpoint = (MaxWatchpoint) getValueAt(row, 0);
                if (watchpoint != null) {
                    focus().setWatchpoint(watchpoint);
                }
            }
        }
    }

    private final class WatchpointsColumnModel extends DefaultTableColumnModel {

        private final WatchpointsViewPreferences viewPreferences;

        private WatchpointsColumnModel(WatchpointsViewPreferences viewPreferences) {
            this.viewPreferences = viewPreferences;
            createColumn(WatchpointsColumnKind.START, new StartAddressCellRenderer(inspection()), null);
            createColumn(WatchpointsColumnKind.SIZE, new SizeCellRenderer(inspection()), null);
            createColumn(WatchpointsColumnKind.END, new EndAddressCellRenderer(inspection()), null);
            createColumn(WatchpointsColumnKind.DESCRIPTION, new DescriptionCellRenderer(inspection()), null);
            createColumn(WatchpointsColumnKind.REGION, new RegionRenderer(inspection()), null);
            createColumn(WatchpointsColumnKind.READ, null, new DefaultCellEditor(new JCheckBox()));
        }

        private void createColumn(WatchpointsColumnKind columnKind, TableCellRenderer renderer, TableCellEditor editor) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (viewPreferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * A table data model built around the list of current watchpoints in the VM.
     *
     * @author Michael Van De Vanter
     */
    private final class WatchpointsTableModel extends DefaultTableModel {

        void refresh() {
            fireTableDataChanged();
            updateFocusSelection();
        }

        @Override
        public int getColumnCount() {
            return WatchpointsColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return maxVM().watchpoints().length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            int count = 0;
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (WatchpointsColumnKind.VALUES.get(col) == WatchpointsColumnKind.READ) {
                    return true;
                }
                if (count == row) {
                    return watchpoint;
                }
                count++;
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (WatchpointsColumnKind.VALUES.get(c)) {
                case READ:
                    return Boolean.class;
                default:
                    return MaxWatchpoint.class;
            }
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

    private final class StartAddressCellRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per watchpoint and keep them around
        private final Map<MaxWatchpoint, WeakReference<WordValueLabel> > watchpointToLabelMap = new HashMap<MaxWatchpoint, WeakReference<WordValueLabel> >();

        public StartAddressCellRenderer(Inspection inspection) {
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
                labelReference = new WeakReference<WordValueLabel>(new WatchpointStartWordValueLabel(inspection(), ValueMode.WORD, watchpoint));
                watchpointToLabelMap.put(watchpoint, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            if (row == getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
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
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setValue(watchpoint.size().toInt());
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
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
                labelReference = new WeakReference<WordValueLabel>(new WatchpointEndWordValueLabel(inspection(), ValueMode.WORD, watchpoint));
                watchpointToLabelMap.put(watchpoint, labelReference);
            }
            final WordValueLabel label = labelReference.get();
            if (row == getSelectionModel().getMinSelectionIndex()) {
                label.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                label.setBackground(style().defaultTextBackgroundColor());
            }
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
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setText(watchpoint.description());
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {

        public RegionRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxWatchpoint watchpoint = (MaxWatchpoint) value;
            setValue(new WordValue(watchpoint.start()));
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

}
