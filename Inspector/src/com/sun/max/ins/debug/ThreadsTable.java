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
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;


/**
 * A table specialized for displaying the threads in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsTable extends InspectorTable {

    private final ThreadsTableModel _model;
    private final ThreadsColumnModel _columnModel;
    private final TableColumn[] _columns;

    ThreadsTable(Inspection inspection) {
        super(inspection);
        _model = new ThreadsTableModel();
        _columns = new TableColumn[ThreadsColumnKind.VALUES.length()];
        _columnModel = new ThreadsColumnModel();

        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new ThreadsInspectorMouseClickAdapter(inspection()));

        refresh(teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    ThreadsViewPreferences preferences() {
        return _columnModel.localPreferences();
    }

    void selectThread(TeleNativeThread selectedThread) {
        int row = 0;
        for (TeleNativeThread teleNativeThread : teleVM().threads()) {
            if (teleNativeThread == selectedThread) {
                if (getSelectedRow() != row) {
                    setRowSelectionInterval(row, row);
                }
                break;
            }
            row++;
        }
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return MemoryRegionsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class ThreadsColumnModel extends DefaultTableColumnModel {

        private final ThreadsViewPreferences _localPreferences;

        private ThreadsColumnModel() {
            _localPreferences = new ThreadsViewPreferences(ThreadsViewPreferences.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(ThreadsColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(_columns[col]);
                    } else {
                        removeColumn(_columns[col]);
                    }
                    fireColumnPreferenceChanged();
                }
            };
            createColumn(ThreadsColumnKind.ID, new IDCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.SERIAL, new SerialCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.KIND, new KindCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.STATUS, new StatusCellRenderer(inspection()));
        }

        private ThreadsViewPreferences localPreferences() {
            return _localPreferences;
        }

        private void createColumn(ThreadsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_localPreferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    private final class ThreadsTableModel extends AbstractTableModel {

        public ThreadsTableModel() {
        }

        void refresh() {
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return ThreadsColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return teleVM().threads().length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            int count = 0;
            for (TeleNativeThread teleNativeThread : teleVM().threads()) {
                if (count == row) {
                    return teleNativeThread;
                }
                count++;
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return TeleNativeThread.class;
        }
    }

    private final class IDCellRenderer extends PlainLabel implements TableCellRenderer {

        IDCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            final String threadIdText = Long.toString(teleNativeThread.id());
            setText(threadIdText);
            setToolTipText("Native thread ID:  " + threadIdText);
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class SerialCellRenderer extends PlainLabel implements TableCellRenderer {

        SerialCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            final TeleVmThread teleVmThread    = teleNativeThread.teleVmThread();
            if (teleVmThread != null) {
                final String serialString = Long.toString(teleVmThread.serial());
                setText(serialString);
                setToolTipText("VM thread serial ID:  " + serialString);
            } else {
                setText("");
                setToolTipText("Not a VM thread");
            }
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class KindCellRenderer extends PlainLabel implements TableCellRenderer {

        KindCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            final TeleVmThread teleVmThread    = teleNativeThread.teleVmThread();
            String kind;
            if (teleVmThread != null) {
                kind = "Java";
            } else {
                kind = "native";
            }
            setText(kind);
            setToolTipText("Kind:  " + kind);
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class NameCellRenderer extends JavaNameLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            setValue(inspection().nameDisplay().shortName(teleNativeThread), "Name:  " + inspection().nameDisplay().longName(teleNativeThread));
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().javaNameBackgroundColor());
            }
            return this;
        }
    }

    private final class StatusCellRenderer extends PlainLabel implements TableCellRenderer {

        StatusCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            if (teleNativeThread.breakpoint() != null) {
                setText("At Breakpoint");
                setToolTipText("Status: At Breakpoint");
            } else {
                final String status = teleNativeThread.state().toString();
                setText(status);
                setToolTipText("Status:  " + status);
            }
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class ThreadsInspectorMouseClickAdapter extends InspectorMouseClickAdapter {

        ThreadsInspectorMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(final MouseEvent mouseEvent) {
            switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    final int row = getSelectedRow();
                    final int column = getSelectedColumn();
                    final TeleNativeThread teleNativeThread = (TeleNativeThread) getValueAt(row, column);
                    focus().setThread(teleNativeThread);
                }
            }
        }
    };

    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            _lastRefreshEpoch = epoch;
            _model.refresh();
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
        }
    }
}
