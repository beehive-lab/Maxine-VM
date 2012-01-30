/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.debug.vmlog;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.annotate.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.AbstractView.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.log.nat.*;
import com.sun.max.vm.log.nat.thread.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A custom singleton viewer for VM log records.
 * Essentially a hybrid array/object viewer.
 *
 * This code supports the several implementations of {@link VMLog}.
 * The main distinctions are:
 * <ul>
 * <ol>Java implementation, Record objects stored in an array.
 * <ol>Native implementation, C-like records stored in a shared (global) native buffer.
 * <ol>Native implementation, C-like records stored in a per-thread native buffer, accessed via {@link VmThreadLocal}.
 * </ul>
 * Variant 3 requires the global view to be reconstituted from the various threads in the target.
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

    private InspectorPanel contentPane;
    private final LogViewPreferences viewPreferences;
    private VMLogElementsTable table;
    private VMLogElementsTableModel tableModel;
    private TableRowFilterToolBar filterToolBar = null;
    private JCheckBoxMenuItem showFilterCheckboxMenuItem;
    private int[] filterMatchingRows = null;



    final Reference vmLogRef;
    private final TeleObject vmLog;
    final TeleInstanceIntFieldAccess nextIdFieldAccess;
    /**
     * Defines the actual {@link VMLog} subclass in ther target VM.
     * Used to select the correct {@link VMLogElementsTableModel}.
     */
    private final ClassActor vmLogClassActor;
    /**
     * Copy of {@link VMLog#logEntries}, which is set at image build time.
     * For implementations with a shared global buffer and fixed size log records
     * this value is the largest number of records that can be in existence.
     * However, for per-thread buffers and/or variable size log records,
     * it may be an underestimate.
     */
    final int logBufferEntries;
    /**
     * The deep-copied set of {@link VMLogger} instances, used for ooperation/argument customization.
     */
    final Map<Integer, VMLogger> loggers;

    @SuppressWarnings("unchecked")
    VMLogView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        TeleVM vm = (TeleVM) vm();
        vmLogRef = vm.fields().VMLog_vmLog.readReference(vm);
        vmLog = VmObjectAccess.make(vm).makeTeleObject(vmLogRef);
        vmLogClassActor = vmLog.classActorForObjectType();
        logBufferEntries = vm.fields().VMLog_logEntries.readInt(vmLogRef);
        nextIdFieldAccess = vm.fields().VMLog_nextId;
        Reference loggersRef = vm.fields().VMLog_loggers.readReference(vm);
        loggers = (Map<Integer, VMLogger>) VmObjectAccess.make(vm).makeTeleObject(loggersRef).deepCopy();

        viewPreferences = LogViewPreferences.globalPreferences(inspection());
//        viewPreferences.addListener(this);
        showFilterCheckboxMenuItem = new InspectorCheckBox(inspection, "Filter view", "Show Filter Field", false);
        showFilterCheckboxMenuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
                if (checkBoxMenuItem.isSelected()) {
                    openFilter();
                } else {
                    closeFilter();
                }
            }
        });
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        final InspectorMenuItems defaultViewMenuItems = defaultMenuItems(MenuKind.VIEW_MENU);
        final InspectorMenu viewMenu = frame.makeMenu(MenuKind.VIEW_MENU);
        viewMenu.add(showFilterCheckboxMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(defaultViewMenuItems);
    }

    private final RowMatchListener rowMatchListener = new RowMatchListener() {

        public void setSearchResult(int[] result) {
            filterMatchingRows = result;
            table.setDisplayedRows(filterMatchingRows);
            //System.out.println("Match=" + Arrays.toString(filterMatchingRows));
        }

        public void closeRequested() {
            closeFilter();
            showFilterCheckboxMenuItem.setState(false);
        }
    };

    private void openFilter() {
        if (filterToolBar == null) {
            filterToolBar = new TableRowFilterToolBar(inspection(), rowMatchListener, table);
            contentPane.add(filterToolBar, BorderLayout.NORTH);
            pack();
            filterToolBar.getFocus();
        }
    }

    private void closeFilter() {
        if (filterToolBar != null) {
            contentPane.remove(filterToolBar);
            table.setDisplayedRows(null);
            pack();
            filterToolBar = null;
            filterMatchingRows = null;
        }
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

    @Override
    protected void createViewContent() {
        table = new VMLogElementsTable(inspection(), this);
        final InspectorScrollPane vmLogViewScrollPane = new InspectorScrollPane(inspection(), table);
        contentPane = new InspectorPanel(inspection(), new BorderLayout());
        contentPane.add(vmLogViewScrollPane, BorderLayout.CENTER);
        setContentPane(contentPane);
    }

    @Override
    protected void refreshState(boolean force) {
        if (inspection().hasProcess()) {
            table.refresh(force);
        }
        if (filterToolBar != null) {
            filterToolBar.refresh(force);
        }
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    private static class VMLogElementsTable extends InspectorTable {
        private VMLogView vmLogView;

        VMLogElementsTable(Inspection inspection, VMLogView vmLogView) {
            super(inspection);
            this.vmLogView = vmLogView;
            String vmLogClassName = vmLogView.vmLogClassActor.simpleName();
            try {
                Class<?> klass = Class.forName(VMLogView.class.getPackage().getName() + "." + vmLogClassName + "ElementsTableModel");
                Constructor<?> cons = klass.getDeclaredConstructor(Inspection.class, VMLogView.class);
                vmLogView.tableModel = (VMLogElementsTableModel) cons.newInstance(inspection, vmLogView);
            } catch (Exception ex) {
                TeleError.unexpected("Exception instantiating VMLog subclass: " + vmLogClassName, ex);
            }

            VMLogColumnModel columnModel = new VMLogColumnModel(vmLogView);
            configureDefaultTable(vmLogView.tableModel, columnModel);
        }

        /**
         * Sets a display filter that will cause only the specified rows
         * to be displayed.
         *
         * @param displayedRows the rows to be displayed, sorted in ascending order, null if all should be displayed.
         */
        public void setDisplayedRows(int[] displayedRows) {
            vmLogView.tableModel.setDisplayedRows(displayedRows);
        }

    }

    private static class VMLogColumnModel extends InspectorTableColumnModel<VMLogColumnKind>  {
        private VMLogColumnModel(VMLogView vmLogView) {
            super(VMLogColumnKind.values().length, vmLogView.viewPreferences);
            Inspection inspection = vmLogView.inspection();
            addColumn(VMLogColumnKind.ID, new IdCellRenderer(inspection), null);
            addColumn(VMLogColumnKind.THREAD, new ThreadCellRenderer(inspection), null);
            addColumn(VMLogColumnKind.OPERATION, new OperationCellRenderer(inspection, vmLogView), null);
            addColumn(VMLogColumnKind.ARG1, new ArgCellRenderer(inspection, vmLogView, 1), null);
            addColumn(VMLogColumnKind.ARG2, new ArgCellRenderer(inspection, vmLogView, 2), null);
            addColumn(VMLogColumnKind.ARG3, new ArgCellRenderer(inspection, vmLogView, 3), null);
            addColumn(VMLogColumnKind.ARG4, new ArgCellRenderer(inspection, vmLogView, 4), null);
            addColumn(VMLogColumnKind.ARG5, new ArgCellRenderer(inspection, vmLogView, 5), null);
            addColumn(VMLogColumnKind.ARG6, new ArgCellRenderer(inspection, vmLogView, 6), null);
            addColumn(VMLogColumnKind.ARG7, new ArgCellRenderer(inspection, vmLogView, 7), null);
        }
    }


    public static class LogViewPreferences extends TableColumnVisibilityPreferences<VMLogColumnKind> {

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
            super(inspection, Log_COLUMN_PREFERENCE, VMLogColumnKind.values());
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }
    }

    private static class IdCellRenderer extends WordValueLabel implements TableCellRenderer {
        private IdCellRenderer(Inspection inspection) {
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

    private static class ThreadCellRenderer extends PlainLabel implements TableCellRenderer {
        private ThreadCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int threadID = (Integer) value;
            if (threadID == 0) {
                setText("primordial");
            } else {
                MaxThread thread = vm().threadManager().getThread(threadID);
                if (thread == null) {
                    return gui().getUnavailableDataTableCellRenderer();
                }
                setText(thread.vmThreadName());
            }
            return this;
        }
    }

    private static class OperationCellRenderer extends PlainLabel implements TableCellRenderer {

        private VMLogView vmLogView;

        private OperationCellRenderer(Inspection inspection, VMLogView vmLogView) {
            super(inspection, null);
            this.vmLogView = vmLogView;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            int op = (Integer) value;
            int header = vmLogView.tableModel.getRecord(row).header;
            if (!vmLogView.tableModel.wellFormedHeader(header)) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            VMLogger logger = vmLogView.loggers.get(VMLog.Record.getLoggerId(header));
            setText(logger.name + "." + logger.operationName(op));
            return this;
        }
    }

    private static class ArgCellRenderer extends WordValueLabel implements TableCellRenderer {
        private VMLogView vmLogView;
        private int argNum;

        private ArgCellRenderer(Inspection inspection, VMLogView vmLogView, int argNum) {
            super(inspection, WordValueLabel.ValueMode.WORD, null);
            this.vmLogView = vmLogView;
            this.argNum = argNum;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                // non-existent argument
                setText("");
                return this;
            }
            int header = vmLogView.tableModel.getRecord(row).header;
            if (!vmLogView.tableModel.wellFormedHeader(header)) {
                return gui().getUnavailableDataTableCellRenderer();
            }

            long argValue = ((Boxed) value).value();
            VMLogArgRenderer vmLogArgRenderer = VMLogArgRendererFactory.getArgRenderer(vmLogView.loggers.get(VMLog.Record.getLoggerId(header)).name);
            setText(vmLogArgRenderer.getText((TeleVM) vm(), header, argNum, argValue));
            return this;
        }

    }

}
