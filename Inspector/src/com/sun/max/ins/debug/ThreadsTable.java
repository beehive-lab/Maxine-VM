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
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;


/**
 * A table specialized for displaying the threads in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsTable extends InspectorTable implements ViewFocusListener {

    private final ThreadsTableModel _model;
    private final ThreadsColumnModel _columnModel;
    private final TableColumn[] _columns;

    ThreadsTable(Inspection inspection, ThreadsViewPreferences viewPreferences) {
        super(inspection);
        _model = new ThreadsTableModel();
        _columns = new TableColumn[ThreadsColumnKind.VALUES.length()];
        _columnModel = new ThreadsColumnModel(viewPreferences);

        setModel(_model);
        setColumnModel(_columnModel);
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
        updateSelection();
    }

    /**
     * Sets table selection to thread, if any, that is the current user focus.
     */
    private void updateSelection() {
        final MaxThread maxThread = inspection().focus().thread();
        final int row = _model.findRow(maxThread);
        if (row < 0) {
            clearSelection();
        } else  if (row != getSelectedRow()) {
            setRowSelectionInterval(row, row);
        }
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        // Custom table header with tooltips that describe the column data.
        return new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return ThreadsColumnKind.VALUES.get(modelIndex).toolTipText();
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
                final MaxThread maxThread = (MaxThread) getValueAt(row, 0);
                focus().setThread(maxThread);
            }
        }
    }

    private final class ThreadsColumnModel extends DefaultTableColumnModel {

        private final ThreadsViewPreferences _viewPreferences;

        private ThreadsColumnModel(ThreadsViewPreferences viewPreferences) {
            _viewPreferences = viewPreferences;
            createColumn(ThreadsColumnKind.ID, new IDCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.HANDLE, new HandleCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.KIND, new KindCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(ThreadsColumnKind.STATUS, new StatusCellRenderer(inspection()));
        }

        private void createColumn(ThreadsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_viewPreferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * A table data model wrapped around the thread list in the
     * current state of the VM. The list goes empty with the process dies.
     */
    private final class ThreadsTableModel extends AbstractTableModel {

        void refresh() {
            fireTableDataChanged();
            updateSelection();
        }

        public int getColumnCount() {
            return ThreadsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return maxVMState().threads().length();
        }

        public Object getValueAt(int row, int col) {
            int count = 0;
            for (MaxThread maxThread : maxVMState().threads()) {
                if (count == row) {
                    return maxThread;
                }
                count++;
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return MaxThread.class;
        }

        public int findRow(MaxThread maxThread) {
            int row = 0;
            for (MaxThread thread : maxVMState().threads()) {
                if (thread == maxThread) {
                    return row;
                }
                row++;
            }
            return -1;
        }

    }

    private final class IDCellRenderer extends PlainLabel implements TableCellRenderer {

        IDCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread maxThread = (MaxThread) value;
            final int id = maxThread.id();
            if (id < 0) {
                setText("");
                setToolTipText("Not a VM thread");
            } else {
                final String threadIdText = Long.toString(id);
                setText(threadIdText);
                setToolTipText("VM thread ID:  " + threadIdText);
            }
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class HandleCellRenderer extends PlainLabel implements TableCellRenderer {

        HandleCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread maxThread = (MaxThread) value;
            final String handleString = Long.toString(maxThread.handle());
            setText(handleString);
            setToolTipText("Native thread handle:  " + handleString);
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread maxThread = (MaxThread) value;
            final MaxVMThread maxVMThread = maxThread.maxVMThread();
            String kind;
            if (maxVMThread != null) {
                kind = "Java";
            } else {
                if (maxThread.isPrimordial()) {
                    kind = "primordial";
                } else {
                    kind = "native";
                }
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread maxThread = (MaxThread) value;
            setValue(inspection().nameDisplay().shortName(maxThread), "Name:  " + inspection().nameDisplay().longName(maxThread));
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread maxThread = (MaxThread) value;
            final String status = maxThread.state().toString();
            setText(status);
            setToolTipText("Status:  " + status);
            if (row == getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private MaxVMState _lastRefreshedState = null;

    public void refresh(boolean force) {
        if (maxVMState().newerThan(_lastRefreshedState) || force) {
            _lastRefreshedState = maxVMState();
            _model.refresh();
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
    }

    public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, MaxThread threadForStackFrame, StackFrame stackFrame) {
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
    }

    public void threadFocusSet(MaxThread oldMaxThread, MaxThread maxThread) {
        updateSelection();
    }
}
