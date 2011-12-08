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
import com.sun.max.unsafe.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;


/**
 * A custom singleton viewer for JVMTI log records.
 * Essentially a hyrbid array/object viewer.
 */
public class JVMTILogView extends AbstractView<JVMTILogView> {
    private static final ViewKind VIEW_KIND = ViewKind.JVMTILOG;
    private static final String SHORT_NAME = "JVMTI Log";
    private static final String LONG_NAME = " JVMTI Log View";
    private static final String GEOMETRY_SETTINGS_KEY = "jvmtiLogViewGeometry";

    public static final class JVMTILogViewManager extends AbstractSingletonViewManager<JVMTILogView> {

        protected JVMTILogViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected JVMTILogView createView(Inspection inspection) {
            return new JVMTILogView(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static JVMTILogViewManager viewManager = null;

    public static JVMTILogViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new JVMTILogViewManager(inspection);
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

    JVMTILogView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        TeleVM vm = (TeleVM) vm();
        viewPreferences = LogViewPreferences.globalPreferences(inspection());
        logRef = vm.fields().JVMTILog_singleton.readReference(vm);
        log = VmObjectAccess.make(vm).makeTeleObject(logRef);
        Reference logBufferRef = vm.fields().JVMTILog_buffer.readReference(logRef);
        logBuffer = (TeleArrayObject) VmObjectAccess.make(vm).makeTeleObject(logBufferRef);
        nextIdFieldAccess = vm.fields().JVMTILog_nextId;
        /*final InspectorFrame frame = */ createFrame(true);
//        frame.makeMenu(MenuKind.OBJECT_MENU).add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

    @Override
    protected void createViewContent() {
        elementsPane = createJVMTILogElementsPane(inspection());
        getContentPane().add(elementsPane);
    }

    @Override
    protected void refreshState(boolean force) {
        elementsPane.refresh(force);
    }

    private ObjectScrollPane createJVMTILogElementsPane(Inspection inspection) {
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

        private JVMTILog logCache = new JVMTILog();
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

        public Object getValueAt(int row, int col) {
            // row 0 is interpreted as the start of the circular buffer, not the start of the array.
            //
            int offset;
            if (lastNextId < logCache.buffer.length) {
                offset = 0;
            } else {
                offset = (lastNextId - logCache.buffer.length) % logCache.buffer.length;
            }
            row += offset;
            switch (LogColumnKind.values()[col]) {
                case INDEX:
                    return logCache.buffer[row].id;
                case THREAD:
                    return logCache.buffer[row].threadId;
                case METHOD:
                    return logCache.buffer[row].op;
                case ARG1:
                    return logCache.buffer[row].arg1;
                default:
                    return null;
            }
        }

        @Override
        public void refresh() {
            int nextId = nextIdFieldAccess.readInt(logRef);
            if (nextId <= lastNextId) {
                // no change
                return;
            }
            // get actual array slots
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
            lastNextId = nextId;
            super.refresh();
        }

        private JVMTILog.Record getRecordFromVM(int i) {
            TeleVM vm = (TeleVM) vm();
            Reference recordRef = logBuffer.readElementValue(i).asReference();
            return new JVMTILog.Record(
                            vm.fields().JVMTILog$Record_op.readInt(recordRef),
                            vm.fields().JVMTILog$Record_threadId.readInt(recordRef),
                            vm.fields().JVMTILog$Record_id.readInt(recordRef),
                            vm.fields().JVMTILog$Record_arg1.readWord(recordRef));
        }

    }

    private class LogColumnModel extends InspectorTableColumnModel<LogColumnKind>  {
        private LogColumnModel() {
            super(LogColumnKind.values().length, viewPreferences);
            addColumn(LogColumnKind.INDEX, new IndexCellRenderer(inspection()), null);
            addColumn(LogColumnKind.THREAD, new ThreadCellRenderer(inspection()), null);
            addColumn(LogColumnKind.METHOD, new MethodCellRenderer(inspection()), null);
            addColumn(LogColumnKind.ARG1, new ArgCellRenderer(inspection(), 0), null);
        }
    }

    private enum LogColumnKind implements ColumnKind {
        INDEX("Index", "index in circular buffer", true, 5),
        THREAD("Thread", "thread that created the entry", true, -1),
        METHOD("Method", "method name", true, -1),
        ARG1("Arg1", "argument 1", true, -1);

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
            int threadID = (Integer) value;
            setText(vm().threadManager().getThread(threadID).vmThreadName());
            return this;
        }
    }

    private class MethodCellRenderer extends PlainLabel implements TableCellRenderer {
        private MethodCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int op = (Integer) value;
            setText(JVMTIFunctions.Methods.values()[op].name);
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
            long longValue = ((Boxed) value).value();
            int op = tableModel.logCache.buffer[row].op;
            switch (JVMTIFunctions.Methods.values()[op]) {
                case RawMonitorEnter:
                case RawMonitorExit:
                case RawMonitorWait:
                case RawMonitorNotify:
                case RawMonitorNotifyAll:
                    // value is the address of the RawMonitor struct
                    // from which we can get the name
                    Pointer rawMonitor = Address.fromLong(longValue).asPointer();
                    Value v = vm().memory().readWordValue(rawMonitor.plus(JVMTIRawMonitor.Struct.NAME.offset));
                    Pointer nameCString = Address.fromLong(v.toLong()).asPointer();
                    setText(stringFromCString((TeleVM) vm(), nameCString));
                    break;
                default:
                    setText(Long.toHexString(longValue));
            }
            return this;
        }

        private String stringFromCString(TeleVM vm, Pointer cString) {
            byte[] bytes = new byte[1024];
            int index = 0;
            while (true) {
                byte b = vm.memory().readByte(cString, index);
                if (b == 0) {
                    break;
                }
                bytes[index++] = b;
            }
            return new String(bytes, 0, index);

        }
    }

}
