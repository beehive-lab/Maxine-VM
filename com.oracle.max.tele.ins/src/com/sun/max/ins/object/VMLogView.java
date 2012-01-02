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
import com.sun.max.vm.log.*;
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
    private Reference vmLogRef;
    private TeleObject vmLog;
    private TeleArrayObject teleLogBuffer;
    private int logBufferSize;
    private TeleInstanceIntFieldAccess nextIdFieldAccess;
    private Map<Integer, VMLogger> loggers;

    @SuppressWarnings("unchecked")
    VMLogView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        TeleVM vm = (TeleVM) vm();
        viewPreferences = LogViewPreferences.globalPreferences(inspection());
        vmLogRef = vm.fields().VMLog_vmLog.readReference(vm);
        vmLog = VmObjectAccess.make(vm).makeTeleObject(vmLogRef);
        Reference logBufferRef = vm.fields().VMLogArray_buffer.readReference(vmLogRef);
        teleLogBuffer = (TeleArrayObject) VmObjectAccess.make(vm).makeTeleObject(logBufferRef);
        logBufferSize = vm.fields().VMLog_logSize.readInt(vmLogRef);
        nextIdFieldAccess = vm.fields().VMLog_nextId;
        Reference loggersRef = vm.fields().VMLog_loggers.readReference(vmLogRef);
        loggers = (Map<Integer, VMLogger>) VmObjectAccess.make(vm).makeTeleObject(loggersRef).deepCopy();
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

        private VMLog.HostedRecord[] logRecordCache = new VMLog.HostedRecord[logBufferSize];
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
            if (lastNextId >= logRecordCache.length) {
                // we have wrapped the circular buffer
                return logRecordCache.length;
            } else {
                return lastNextId;
            }
        }

        /**
         * Get the value of the slot in the log buffer at the given logical row and column.
         */
        public Object getValueAt(int row, int col) {
            VMLog.HostedRecord record = logRecordCache[row];
            if (record == null) {
                TeleError.unexpected("null log record in LogElementsTableModel.getValueAt");
            }
            Object result = null;
            int argCount = VMLog.Record.getArgCount(record.header);

            switch (LogColumnKind.values()[col]) {
                case ID:
                    result = record.id;
                    break;
                case THREAD:
                    result = record.getThreadId();
                    break;
                case OPERATION:
                    result = record.getOperation();
                    break;
                case ARG1:
                    if (argCount > 0) {
                        result = record.args[0];
                    }
                    break;
                case ARG2:
                    if (argCount > 1) {
                        result = record.args[1];
                    }
                    break;
                case ARG3:
                    if (argCount > 2) {
                        result = record.args[2];
                    }
                    break;
                case ARG4:
                    if (argCount > 3) {
                        result = record.args[3];
                    }
                    break;
                case ARG5:
                    if (argCount > 4) {
                        result = record.args[4];
                    }
                    break;
                case ARG6:
                    if (argCount > 5) {
                        result = record.args[5];
                    }
                    break;
                case ARG7:
                    if (argCount > 6) {
                        result = record.args[6];
                    }
                    break;
                default:
                    TeleError.unexpected("illegal column value kind");
                    result = null;
            }
            return result;
        }

        @Override
        public void refresh() {
            int nextId = nextIdFieldAccess.readInt(vmLogRef);
            if (nextId != lastNextId) {
                // Some new records; we could try to be clever and just figure out what changed
                // but for now we just read everything into the record cache (underlying page caching helps).
                // This also makes it easy to keep the record cache logical in the sense that index 0 is the first
                // slot in the circular buffer and not index 0 in the VM log array.
                final long firstId = firstId(nextId);
                long id = firstId;
                int cacheIndex = 0;

                while (id < nextId) {
                    int idIndex = (int) (id % logRecordCache.length); // actual slot in target buffer array
                    logRecordCache[cacheIndex++] = getRecordFromVM(idIndex, id);
                    id++;
                }
                lastNextId = nextId;
            } else {
                // one very special case - the last refresh was after nextId was bumped
                // but before the record was filled in.
                if (lastNextId != 0) {
                    int id = lastNextId - 1;
                    // re-read the record
                    int idIndex = id % logRecordCache.length; // actual slot in target buffer array
                    VMLog.HostedRecord record = getRecordFromVM(idIndex, id);
                    logRecordCache[id - firstId(id)] = record;
                }
            }
            super.refresh();
        }

        private int firstId(int id) {
            return id >= logRecordCache.length ? id - logRecordCache.length : 0;
        }

        private VMLog.HostedRecord getRecordFromVM(int index, long id) {
            TeleVM vm = (TeleVM) vm();
            Reference recordRef = teleLogBuffer.readElementValue(index).asReference();
            return new VMLog.HostedRecord(
                            id,
                            vm.fields().VMLog$Record_header.readInt(recordRef),
                            vm.fields().VMLog$Record1_arg1.readWord(recordRef),
                            vm.fields().VMLog$Record2_arg2.readWord(recordRef),
                            vm.fields().VMLog$Record3_arg3.readWord(recordRef),
                            vm.fields().VMLog$Record4_arg4.readWord(recordRef),
                            vm.fields().VMLog$Record5_arg5.readWord(recordRef),
                            vm.fields().VMLog$Record6_arg6.readWord(recordRef),
                            vm.fields().VMLog$Record7_arg7.readWord(recordRef));
        }

        /**
         * Is this header value well-formed?
         * @param header
         * @return
         */
        private boolean wellFormedHeader(int header) {
            // there are brief periods when a record may not be well formed,
            // e.g., we have stopped in the Inspector after the log buffer id has been bumped
            // but before the data has been filled in.
            // specifically, the logger id may be bogus, which will cause a crash.
            if (VMLog.Record.isFree(header)) {
                return false;
            }
            int loggerId = VMLog.Record.getLoggerId(header);
            if (loggers.get(loggerId) == null) {
                return false;
            }
            return true;
        }

    }

    private class LogColumnModel extends InspectorTableColumnModel<LogColumnKind>  {
        private LogColumnModel() {
            super(LogColumnKind.values().length, viewPreferences);
            addColumn(LogColumnKind.ID, new IdCellRenderer(inspection()), null);
            addColumn(LogColumnKind.THREAD, new ThreadCellRenderer(inspection()), null);
            addColumn(LogColumnKind.OPERATION, new OperationCellRenderer(inspection()), null);
            addColumn(LogColumnKind.ARG1, new ArgCellRenderer(inspection(), 1), null);
            addColumn(LogColumnKind.ARG2, new ArgCellRenderer(inspection(), 2), null);
            addColumn(LogColumnKind.ARG3, new ArgCellRenderer(inspection(), 3), null);
            addColumn(LogColumnKind.ARG4, new ArgCellRenderer(inspection(), 4), null);
            addColumn(LogColumnKind.ARG5, new ArgCellRenderer(inspection(), 5), null);
            addColumn(LogColumnKind.ARG6, new ArgCellRenderer(inspection(), 6), null);
            addColumn(LogColumnKind.ARG7, new ArgCellRenderer(inspection(), 7), null);
        }
    }

    private enum LogColumnKind implements ColumnKind {
        ID("Id", "unique id", true, 5),
        THREAD("Thread", "thread that created the entry", true, -1),
        OPERATION("Operation", "operation name", true, -1),
        ARG1("Arg1", "argument 1", true, -1),
        ARG2("Arg2", "argument 2", true, -1),
        ARG3("Arg3", "argument 3", true, -1),
        ARG4("Arg4", "argument 4", true, -1),
        ARG5("Arg5", "argument 5", true, -1),
        ARG6("Arg6", "argument 6", true, -1),
        ARG7("Arg7", "argument 7", true, -1);

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

    private class IdCellRenderer extends WordValueLabel implements TableCellRenderer {
        private IdCellRenderer(Inspection inspection) {
            super(inspection, WordValueLabel.ValueMode.WORD, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            long index = (Long) value;
            setText(Long.toString(index));
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
            int header = tableModel.logRecordCache[row].header;
            if (!tableModel.wellFormedHeader(header)) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            VMLogger logger = loggers.get(VMLog.Record.getLoggerId(header));
            setText(logger.name + "." + logger.operationName(op));
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
                // non-existent argument
                setText("");
                return this;
            }
            int header = tableModel.logRecordCache[row].header;
            if (!tableModel.wellFormedHeader(header)) {
                return gui().getUnavailableDataTableCellRenderer();
            }

            long argValue = ((Boxed) value).value();
            VMLogArgRenderer vmLogArgRenderer = VmLogArgRendererFactory.getArgRenderer(loggers.get(VMLog.Record.getLoggerId(header)).name);
            setText(vmLogArgRenderer.getText((TeleVM) vm(), header, argNum, argValue));
            return this;
        }

    }

}
