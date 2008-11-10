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

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * An inspector that displays the list of threads running in the process of the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class ThreadsInspector extends UniqueInspector<ThreadsInspector> {

    /**
     * @return the singleton instance, if it exists
     */
    private static ThreadsInspector getInspector(Inspection inspection) {
        return UniqueInspector.find(inspection, ThreadsInspector.class);
    }

    /**
     * Display and highlight the (singleton) threads inspector.
     *
     * @return  The threads inspector, possibly newly created.
     */
    public static ThreadsInspector make(Inspection inspection) {
        ThreadsInspector threadsInspector = getInspector(inspection);
        if (threadsInspector == null) {
            Trace.begin(1, "initializing ThreadsInspector");
            threadsInspector = new ThreadsInspector(inspection, Residence.INTERNAL);
            Trace.end(1, "initializing ThreadsInspector");
        }
        threadsInspector.highlight();
        return threadsInspector;
    }

    enum ColumnKind {
        ID,
        SERIAL,
        KIND,
        NAME,
        STATUS;

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }

    private final JTable _table = new ThreadJTable();

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "threadsInspector");

    private ThreadsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        createFrame(null);
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTitle() {
        return "Threads";
    }

    @Override
    public void createView(long epoch) {
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.addMouseListener(new ThreadsInspectorMouseClickAdapter(inspection()));
//        final ListSelectionModel listSelectionModel = _table.getSelectionModel();
//        listSelectionModel.addListSelectionListener(new ListSelectionListener() {
//            public void valueChanged(ListSelectionEvent event) {
//                if (event.getValueIsAdjusting()) {
//                    return;
//                }
//                assert event.getSource() == listSelectionModel;
//                // Decide whether to propagate the new table selection.
//                // If the new table selection agrees with the global thread
//                // selection, then this table change is just an initialization or update notification.
//                if (!listSelectionModel.isSelectionEmpty()) {
//                    final TeleNativeThread teleNativeThread = (TeleNativeThread) _table.getValueAt(_table.getSelectedRow(), 0);
//                    // A user action in this inspector has selected a thread different than the global selection; propagate the change.
//                    inspection().focus().setThread(teleNativeThread);
//                }
//            }
//        });
        final JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setPreferredSize(inspection().geometry().threadsFramePrefSize());
        frame().setLocation(inspection().geometry().threadsFrameDefaultLocation());
        frame().setContentPane(scrollPane);
        refreshView(epoch, true);
    }

    private final class ThreadJTable extends JTable {
        final TableCellRenderer _idCellRenderer;
        final TableCellRenderer _serialCellRenderer;
        final TableCellRenderer _kindCellRenderer;
        final TableCellRenderer _nameCellRenderer;
        final TableCellRenderer _statusCellRenderer;

        ThreadJTable() {
            super(new ThreadsTableModel());
            _idCellRenderer = new IDCellRenderer(inspection());
            _serialCellRenderer = new SerialCellRenderer(inspection());
            _kindCellRenderer = new KindCellRenderer(inspection());
            _nameCellRenderer = new NameCellRenderer(inspection());
            _statusCellRenderer = new StatusCellRenderer(inspection());
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            switch (ColumnKind.VALUES.get(column)) {
                case ID:
                    return _idCellRenderer;
                case SERIAL:
                    return _serialCellRenderer;
                case KIND:
                    return _kindCellRenderer;
                case NAME:
                    return _nameCellRenderer;
                case STATUS:
                    return _statusCellRenderer;
                default:
                    Problem.error("Unexpected Thread Data column");
            }
            return null;
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
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
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
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
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
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class NameCellRenderer extends PlainLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) value;
            setText(inspection().nameDisplay().shortName(teleNativeThread));
            setToolTipText("Name:  " + inspection().nameDisplay().longName(teleNativeThread));
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
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
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class ThreadsTableModel extends DefaultTableModel {

        @Override
        public int getColumnCount() {
            return ColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return teleVM().allThreads().length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            int count = 0;
            for (TeleNativeThread teleNativeThread : teleVM().allThreads()) {
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

        @Override
        public String getColumnName(int column) {
            switch (ColumnKind.VALUES.get(column)) {
                case ID:
                    return "ID";
                case SERIAL:
                    return "VM ID";
                case KIND:
                    return "Kind";
                case NAME:
                    return "Name";
                case STATUS:
                    return "Status";
            }
            return "";
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
                    final int row = _table.getSelectedRow();
                    final int column = _table.getSelectedColumn();
                    final TeleNativeThread teleNativeThread = (TeleNativeThread) _table.getValueAt(row, column);
                    focus().setThread(teleNativeThread);
                }
            }
        }
    };

    @Override
    public void refreshView(long epoch, boolean force) {
        final ThreadsTableModel threadsTableModel = (ThreadsTableModel) _table.getModel();
        threadsTableModel.fireTableDataChanged();
        updateThreadFocus(focus().thread());
        super.refreshView(epoch, force);
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        updateThreadFocus(teleNativeThread);
    }

    /**
     * Changes the Inspector's selected row to agree with the global thread selection.
     */
    private void updateThreadFocus(TeleNativeThread selectedThread) {
        int row = 0;
        for (TeleNativeThread teleNativeThread : teleVM().allThreads()) {
            if (teleNativeThread == selectedThread) {
                if (_table.getSelectedRow() != row) {
                    _table.setRowSelectionInterval(row, row);
                }
                break;
            }
            row++;
        }
    }

    public void viewConfigurationChanged(long epoch) {
        //  All view configurations are applied dynamically in this inspector.
        refreshView(epoch, true);
    }

}
