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
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying memory watchpoints in the VM.
 *
 * @author Michael Van De Vanter
 */
public class WatchpointsTable extends InspectorTable {

    private final WatchpointsTableModel model;
    private final WatchpointsColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    WatchpointsTable(Inspection inspection, WatchpointsViewPreferences viewPreferences) {
        super(inspection);
        model = new WatchpointsTableModel();
        columns = new TableColumn[WatchpointsColumnKind.VALUES.length()];
        columnModel = new WatchpointsColumnModel(viewPreferences);

        setModel(model);
        setColumnModel(columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new TableCellMouseClickAdapter(inspection(), this));
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
        updateFocusSelection();
    }

    /**
     * Sets table selection to the memory watchpoint, if any, that is the current user focus.
     */
    @Override
    public void updateFocusSelection() {
        final MaxWatchpoint watchpoint = inspection().focus().watchpoint();
        final int row = model.findRow(watchpoint);
        if (row < 0) {
            clearSelection();
        } else  if (row != getSelectedRow()) {
            setRowSelectionInterval(row, row);
        }
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            model.refresh();
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
        invalidate();
        repaint();
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
            createColumn(WatchpointsColumnKind.END, new EndAddressCellRenderer(inspection()), null);
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
    private final class WatchpointsTableModel extends AbstractTableModel {

        void refresh() {
            fireTableDataChanged();
            updateFocusSelection();
        }

        public int getColumnCount() {
            return WatchpointsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return maxVM().watchpoints().length();
        }

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

    private final class EndAddressCellRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
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
