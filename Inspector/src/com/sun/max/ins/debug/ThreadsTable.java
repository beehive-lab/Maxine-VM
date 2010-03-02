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

import static com.sun.max.tele.MaxThreadState.*;

import java.awt.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
/**
 * A table specialized for displaying the threads in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsTable extends InspectorTable {

    private final ThreadsTableModel tableModel;
    private final ThreadsColumnModel columnModel;

    ThreadsTable(Inspection inspection, ThreadsViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new ThreadsTableModel(inspection);
        columnModel = new ThreadsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to thread, if any, that is the current user focus.
        final MaxThread thread = focus().thread();
        final int row = tableModel.findRow(thread);
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
                final MaxThread thread = (MaxThread) getValueAt(row, 0);
                focus().setThread(thread);
            }
        }
    }

    @Override
    protected Transferable getTransferable(int row, int col) {
        final MaxThread thread = tableModel.getThreadAt(row);
        assert thread != null;
        final TeleObject vmThreadObject  =  thread.teleVmThread();
        if (vmThreadObject != null) {
            return new InspectorTransferable.TeleObjectTransferable(inspection(), vmThreadObject);
        }
        return null;
    }

    private final class ThreadsColumnModel extends InspectorTableColumnModel<ThreadsColumnKind> {

        private ThreadsColumnModel(ThreadsViewPreferences viewPreferences) {
            super(ThreadsColumnKind.VALUES.length(), viewPreferences);
            addColumn(ThreadsColumnKind.ID, new IDCellRenderer(inspection()), null);
            addColumn(ThreadsColumnKind.HANDLE, new HandleCellRenderer(inspection()), null);
            addColumn(ThreadsColumnKind.LOCAL_HANDLE, new LocalHandleCellRenderer(inspection()), null);
            addColumn(ThreadsColumnKind.KIND, new KindCellRenderer(inspection()), null);
            addColumn(ThreadsColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(ThreadsColumnKind.STATUS, new StatusCellRenderer(inspection()), null);
        }
    }

    /**
     * A table data model wrapped around the thread list in the
     * current state of the VM. The list goes empty with the process dies.
     */
    private final class ThreadsTableModel extends InspectorTableModel {

        public ThreadsTableModel(Inspection inspection) {
            super(inspection);
            // TODO Auto-generated constructor stub
        }

        public int getColumnCount() {
            return ThreadsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return vm().state().threads().length();
        }

        public Object getValueAt(int row, int col) {
            return getThreadAt(row);
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return MaxThread.class;
        }

        public int findRow(MaxThread findThread) {
            int row = 0;
            for (MaxThread thread : vm().state().threads()) {
                if (thread.equals(findThread)) {
                    return row;
                }
                row++;
            }
            return -1;
        }

        public MaxThread getThreadAt(int row) {
            int count = 0;
            for (MaxThread thread : vm().state().threads()) {
                if (count == row) {
                    return thread;
                }
                count++;
            }
            return null;
        }
    }

    /**
     * @return color the text specially in the row where the thread is at a triggered watchpoint or breakpoint
     */
    private Color getRowTextColor(int row) {
        final MaxThread thread = (MaxThread) tableModel.getValueAt(row, 0);
        final MaxThreadState threadState = thread.state();
        if (threadState == BREAKPOINT || threadState == WATCHPOINT) {
            return style().debugIPTagColor();
        }
        return null;
    }

    private final class IDCellRenderer extends PlainLabel implements TableCellRenderer {

        IDCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            final int id = thread.id();
            if (id < 0) {
                setText("");
                setToolTipText("Not a VM thread");
            } else {
                final String threadIdText = Long.toString(id);
                setText(threadIdText);
                setToolTipText("VM thread ID:  " + threadIdText);
            }
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class HandleCellRenderer extends PlainLabel implements TableCellRenderer {

        HandleCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            final String handleString = thread.handleString();
            setText(handleString);
            setToolTipText("Thread handle:  " + handleString);
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class LocalHandleCellRenderer extends PlainLabel implements TableCellRenderer {

        LocalHandleCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            final String handleString = Long.toString(thread.localHandle());
            setText(handleString);
            setToolTipText("Local thread handle:  " + handleString);
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class KindCellRenderer extends PlainLabel implements TableCellRenderer {

        KindCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            String kind;
            if (thread.isJava()) {
                kind = "Java";
            } else {
                if (thread.isPrimordial()) {
                    kind = "primordial";
                } else {
                    kind = "native";
                }
            }
            setText(kind);
            setToolTipText("Kind:  " + kind);
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class NameCellRenderer extends JavaNameLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            setValue(inspection().nameDisplay().shortName(thread), "Name:  " + inspection().nameDisplay().longName(thread));
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class StatusCellRenderer extends PlainLabel implements TableCellRenderer {

        StatusCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            final MaxThreadState threadState = thread.state();
            setText(threadState.toString());
            String toolTipText = "Thread status: " + threadState;
            if (thread.breakpoint() != null) {
                toolTipText = toolTipText + "(" + thread.breakpoint().getDescription() + ")";
            }
            setToolTipText(toolTipText);
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

}
