/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * A custom singleton viewer for VM log records.
 * Essentially a hybrid array/object viewer.
 */
@SuppressWarnings("unused")
public class VMLogView extends AbstractView<VMLogView> {
    private static final ViewKind VIEW_KIND = ViewKind.VMLOG;
    private static final String SHORT_NAME = "VM Log";
    private static final String LONG_NAME = "VM Log View";
    private static final String GEOMETRY_SETTINGS_KEY = "vmLogViewGeometry";

    public static final class VMLogViewManager extends AbstractSingletonViewManager<VMLogView> {

        protected VMLogViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected VMLogView createView(Inspection inspection) {
            return new VMLogView(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static VMLogViewManager viewManager = null;

    public static VMLogViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new VMLogViewManager(inspection);
        }
        return viewManager;
    }

    private final LogViewPreferences viewPreferences;
    private ObjectScrollPane elementsPane;
    private LogElementsTableModel tableModel;
    private Reference logRef;
    private TeleObject log;
    private TeleArrayObject logBuffer;
    private TeleInstanceIntFieldAccess nextIdFieldAccess;
    private Map<Integer, VMLog.Logger> loggers;

    @SuppressWarnings("unchecked")
    VMLogView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        TeleVM vm = (TeleVM) vm();
        viewPreferences = LogViewPreferences.globalPreferences(inspection());
        logRef = vm.fields().VMLog_vmLog.readReference(vm);
        log = VmObjectAccess.make(vm).makeTeleObject(logRef);
        Reference logBufferRef = vm.fields().VMLog_buffer.readReference(logRef);
        logBuffer = (TeleArrayObject) VmObjectAccess.make(vm).makeTeleObject(logBufferRef);
        nextIdFieldAccess = vm.fields().VMLog_nextId;
        Reference loggersRef = vm.fields().VMLog_loggers.readReference(logRef);
        loggers = (Map<Integer, VMLog.Logger>) VmObjectAccess.make(vm).makeTeleObject(loggersRef).deepCopy();
        /*final InspectorFrame frame = */ createFrame(true);
//        frame.makeMenu(MenuKind.OBJECT_MENU).add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

    @Override
    protected void createViewContent() {
        elementsPane = createVMLogElementsPane(inspection());
        getContentPane().add(elementsPane);
    }

    @Override
    protected void refreshState(boolean force) {
        elementsPane.refresh(force);
    }

    private ObjectScrollPane createVMLogElementsPane(Inspection inspection) {
        final LogElementsTable elementsTable = new LogElementsTable(inspection);
        return new ObjectScrollPane(inspection, elementsTable);
    }

    private class LogElementsTable extends InspectorTable {
        LogElementsTable(Inspection inspection) {
            super(inspection);
            tableModel = new LogElementsTableModel(inspection);
            LogColumnModel columnModel = new LogColumnModel();
            configureDefaultTable(tableModel, columnModel);
        }

    }

    private class LogElementsTableModel extends InspectorTableModel {

        private VMLog logCache = new VMLog();
        /**
         * After {@link #refresh}, this holds a copy of the {@link VMLog#nextId} field,
         * which is the id of the next record that will be written. The actual slot in the
         * log buffer that will be written is {@code nextId % buffer size}.
         */
        private int lastNextId;

        public LogElementsTableModel(Inspection inspection) {
            super(inspection);
        }

        public int getColumnCount() {
            return LogColumnKind.values().length;
        }

        public int getRowCount() {
            int nextId = nextIdFieldAccess.readInt(logRef);
            if (nextId >= logCache.buffer.length) {
                // we have wrapped the circular buffer
                return logCache.buffer.length;
            } else {
                return nextId;
            }
        }

        /**
         * Get the index of the slot in the log buffer at the given logical row and column.
         * N.B. Row 0 is interpreted as the start of the circular buffer, not the start of the array
         * holding the buffer, so we have to translate if we have wrapped.
         */
        private int getBufferRow(int xrow) {
            int row = xrow;
            if (lastNextId >= logCache.buffer.length) {
                int row0Index = lastNextId  % logCache.buffer.length;
                row = (row0Index + xrow)  % logCache.buffer.length;
            }
            return row;
        }

        /**
         * Get the value of the slot in the log buffer at the given logical row and column.
         * N.B. Row 0 is interpreted as the start of the circular buffer, not the start of the array
         * holding the buffer, so we have to translate if we have wrapped.
         */
        public Object getValueAt(int xrow, int col) {
            int row = getBufferRow(xrow);
            if (logCache.buffer[row] == null) {
                TeleError.unexpected("null log record in LogElementsTableModel.getValueAt");
            }
            Object result;
            switch (LogColumnKind.values()[col]) {
                case INDEX:
                    result = logCache.buffer[row].id;
                    break;
                case THREAD:
                    result = logCache.buffer[row].threadId;
                    break;
                case OPERATION:
                    result = logCache.buffer[row].oplc;
                    break;
                case ARG1:
                    result = logCache.buffer[row].arg1;
                    break;
                case ARG2:
                    result = logCache.buffer[row].arg2;
                    break;
                case ARG3:
                    result = logCache.buffer[row].arg3;
                    break;
                case ARG4:
                    result = logCache.buffer[row].arg4;
                    break;
                case ARG5:
                    result = logCache.buffer[row].arg5;
                    break;
                case ARG6:
                    result = logCache.buffer[row].arg6;
                    break;
                default:
                    TeleError.unexpected("illegal column value kind");
                    result = null;
            }
            if (result == null) {
                result = new BoxedWord(0);
            }
            return result;
        }

        @Override
        public void refresh() {
            int nextId = nextIdFieldAccess.readInt(logRef);
            if (nextId != lastNextId) {
                // get actual array slots
                if (nextId >= lastNextId + logCache.buffer.length) {
                    // every slot changed
                    for (int i = 0; i < logCache.buffer.length; i++) {
                        logCache.buffer[i] = getRecordFromVM(i);
                    }
                } else {
                    // subset changed
                    int nextIdIndex = nextId % logCache.buffer.length;
                    int lastNextIdIndex = lastNextId % logCache.buffer.length;
                    if (nextIdIndex < lastNextIdIndex) {
                        // wrapped
                        for (int i = 0; i < logCache.buffer.length; i++) {
                            if (i < nextIdIndex || i >= lastNextIdIndex) {
                                logCache.buffer[i] = getRecordFromVM(i);
                            }
                        }
                    } else {
                        for (int i = lastNextIdIndex; i < nextIdIndex; i++) {
                            logCache.buffer[i] = getRecordFromVM(i);
                        }
                    }
                }
                lastNextId = nextId;
            }
            super.refresh();
        }

        private VMLog.Record getRecordFromVM(int i) {
            TeleVM vm = (TeleVM) vm();
            Reference recordRef = logBuffer.readElementValue(i).asReference();
            return new VMLog.Record(
                            vm.fields().VMLog$Record_oplc.readInt(recordRef),
                            vm.fields().VMLog$Record_threadId.readInt(recordRef),
                            vm.fields().VMLog$Record_id.readInt(recordRef),
                            vm.fields().VMLog$Record_arg1.readWord(recordRef),
                            vm.fields().VMLog$Record_arg2.readWord(recordRef),
                            vm.fields().VMLog$Record_arg3.readWord(recordRef),
                            vm.fields().VMLog$Record_arg4.readWord(recordRef),
                            vm.fields().VMLog$Record_arg5.readWord(recordRef),
                            vm.fields().VMLog$Record_arg6.readWord(recordRef),
                            vm.fields().VMLog$Record_arg7.readWord(recordRef));
        }

    }

    private class LogColumnModel extends InspectorTableColumnModel<LogColumnKind>  {
        private LogColumnModel() {
            super(LogColumnKind.values().length, viewPreferences);
            addColumn(LogColumnKind.INDEX, new IndexCellRenderer(inspection()), null);
            addColumn(LogColumnKind.THREAD, new ThreadCellRenderer(inspection()), null);
            addColumn(LogColumnKind.OPERATION, new OperationCellRenderer(inspection()), null);
            addColumn(LogColumnKind.ARG1, new ArgCellRenderer(inspection(), 1), null);
            addColumn(LogColumnKind.ARG2, new ArgCellRenderer(inspection(), 2), null);
            addColumn(LogColumnKind.ARG3, new ArgCellRenderer(inspection(), 3), null);
            addColumn(LogColumnKind.ARG4, new ArgCellRenderer(inspection(), 4), null);
            addColumn(LogColumnKind.ARG5, new ArgCellRenderer(inspection(), 5), null);
            addColumn(LogColumnKind.ARG6, new ArgCellRenderer(inspection(), 6), null);
            addColumn(LogColumnKind.ARG6, new ArgCellRenderer(inspection(), 7), null);
        }
    }

    private enum LogColumnKind implements ColumnKind {
        INDEX("Index", "index in circular buffer", true, 5),
        THREAD("Thread", "thread that created the entry", true, -1),
        OPERATION("Operation", "operation name", true, -1),
        ARG1("Arg1", "argument 1", true, -1),
        ARG2("Arg2", "argument 2", true, -1),
        ARG3("Arg3", "argument 3", true, -1),
        ARG4("Arg4", "argument 4", true, -1),
        ARG5("Arg5", "argument 5", true, -1),
        ARG6("Arg6", "argument 6", true, -1);

        private final String label;
        private final String toolTipText;
        private final boolean defaultVisibility;
        private final int minWidth;

        private LogColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
            this.label = label;
            this.toolTipText = toolTipText;
            this.defaultVisibility = defaultVisibility;
            this.minWidth = minWidth;
            assert defaultVisibility || canBeMadeInvisible();
        }

        public String label() {
            return label;
        }

        public String toolTipText() {
            return toolTipText;
        }

        public int minWidth() {
            return minWidth;
        }

        @Override
        public String toString() {
            return label;
        }

        public boolean canBeMadeInvisible() {
            return false;
        }

        public boolean defaultVisibility() {
            return defaultVisibility;
        }


    }

    public static class LogViewPreferences extends TableColumnVisibilityPreferences<LogColumnKind> {

        private static LogViewPreferences globalPreferences;

        /**
         * @return the global, persistent set of user preferences for viewing a table of Log.
         */
        static LogViewPreferences globalPreferences(Inspection inspection) {
            if (globalPreferences == null) {
                globalPreferences = new LogViewPreferences(inspection);
            }
            return globalPreferences;
        }

        // Prefix for all persistent column preferences in view
        private static final String Log_COLUMN_PREFERENCE = "LogViewColumn";

        /**
         * @return a GUI panel suitable for setting global preferences for this kind of view.
         */
        public static JPanel globalPreferencesPanel(Inspection inspection) {
            return globalPreferences(inspection).getPanel();
        }

        /**
        * Creates a set of preferences specified for use by singleton instances, where local and
        * persistent global choices are identical.
        */
        private LogViewPreferences(Inspection inspection) {
            super(inspection, Log_COLUMN_PREFERENCE, LogColumnKind.values());
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }
    }

    private class IndexCellRenderer extends WordValueLabel implements TableCellRenderer {
        private IndexCellRenderer(Inspection inspection) {
            super(inspection, WordValueLabel.ValueMode.WORD, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int index = (Integer) value;
            setText(Integer.toString(index));
            return this;
        }
    }

    private class ThreadCellRenderer extends PlainLabel implements TableCellRenderer {
        private ThreadCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int threadID = (Integer) value;
            MaxThread thread = vm().threadManager().getThread(threadID);
            if (thread == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            setText(thread.vmThreadName());
            return this;
        }
    }

    private class OperationCellRenderer extends PlainLabel implements TableCellRenderer {
        private OperationCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int op = (Integer) value;
            if (op == 0) {
                // we have stopped in the Inspector after the log buffer id has been bumped
                // but before the data has been filled in.
                return gui().getUnavailableDataTableCellRenderer();
            }
            VMLog.Logger logger = loggers.get(VMLog.Record.getLoggerId(op));
            setText(logger.name + "." + logger.operationName(VMLog.Record.getOperation(op)));
            return this;
        }
    }

    private class ArgCellRenderer extends WordValueLabel implements TableCellRenderer {
        private int argNum;

        private ArgCellRenderer(Inspection inspection, int argNum) {
            super(inspection, WordValueLabel.ValueMode.WORD, null);
            this.argNum = argNum;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int op = tableModel.logCache.buffer[tableModel.getBufferRow(row)].oplc;
            if (op == 0) {
                // we have stopped in the Inspector after the log buffer id has been bumped
                // but before the data has been filled in.
                return gui().getUnavailableDataTableCellRenderer();
            }

            long argValue = ((Boxed) value).value();
            VMLogArgRenderer vmLogArgRenderer = VmLogArgRendererFactory.getArgRenderer(loggers.get(VMLog.Record.getLoggerId(op)));
            setText(vmLogArgRenderer.getText((TeleVM) vm(), op, argNum, argValue));
            return this;
        }

    }

}
