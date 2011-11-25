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
 */
public final class ThreadsTable extends InspectorTable {

    private final ThreadsTableModel tableModel;

    ThreadsTable(Inspection inspection, ThreadsViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new ThreadsTableModel(inspection);
        ThreadsColumnModel columnModel = new ThreadsColumnModel(viewPreferences);
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

    /**
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where the thread is at a triggered watchpoint or breakpoint
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxThread thread = (MaxThread) tableModel.getValueAt(row, 0);
        final MaxThreadState threadState = thread.state();
        if (threadState == BREAKPOINT || threadState == WATCHPOINT) {
            return preference().style().debugIPTagColor();
        }
        return null;
    }

    private final class ThreadsColumnModel extends InspectorTableColumnModel<ThreadsColumnKind> {

        private ThreadsColumnModel(ThreadsViewPreferences viewPreferences) {
            super(ThreadsColumnKind.values().length, viewPreferences);
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
            return ThreadsColumnKind.values().length;
        }

        public int getRowCount() {
            return vm().state().threads().size();
        }

        public Object getValueAt(int row, int col) {
            return getThreadAt(row);
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return MaxThread.class;
        }

        @Override
        public String getRowDescription(int row) {
            final MaxThread thread = getThreadAt(row);
            if (thread.id() < 0) {
                return "Non-VM thread";
            }
            final String threadIdText = Long.toString(thread.id());
            return "VM thread ID:  " + threadIdText;
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

    private final class IDCellRenderer extends PlainLabel implements TableCellRenderer {

        IDCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxThread thread = (MaxThread) value;
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final int id = thread.id();
            if (id < 0) {
                setText("");
                setToolTipText("Not a VM thread");
            } else {
                final String threadIdText = Long.toString(id);
                setText(threadIdText);
                setToolTipText("VM thread ID:  " + threadIdText);
            }
            setForeground(cellForegroundColor(row, column));
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
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final String handleString = thread.handleString();
            setText(handleString);
            setToolTipText("Thread handle:  " + handleString);
            setForeground(cellForegroundColor(row, column));
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
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final String handleString = Long.toString(thread.localHandle());
            setText(handleString);
            setToolTipText("Local thread handle:  " + handleString);
            setForeground(cellForegroundColor(row, column));
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
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
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
            setToolTipText("Thread kind:  " + kind);
            setForeground(cellForegroundColor(row, column));
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
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            setValue(inspection().nameDisplay().shortName(thread), "Thread name:  " + inspection().nameDisplay().longName(thread));
            setForeground(cellForegroundColor(row, column));
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
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final MaxThreadState threadState = thread.state();
            setText(threadState.toString());
            String toolTipText = "Thread status: " + threadState;
            if (thread.breakpoint() != null) {
                toolTipText = toolTipText + "(" + thread.breakpoint().getDescription() + ")";
            }
            setToolTipText(toolTipText);
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

}
