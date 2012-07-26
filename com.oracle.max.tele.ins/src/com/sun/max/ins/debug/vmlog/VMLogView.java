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
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.value.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.reference.*;
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
public class VMLogView extends AbstractView<VMLogView> implements TableColumnViewPreferenceListener {
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

    private void readLogFile(File file) {
        final ArrayList<String> records = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String logClassMarker = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith(VMLog.RawDumpFlusher.LOGCLASS_MARKER)) {
                    if (logClassMarker == null) {
                        logClassMarker = line;
                    }
                    continue;
                }
                records.add(line);
            }
            // Check that logClassName matches that discovered in the image.
            checkLogClassMatch(logClassMarker);
            tableModel.offLineRefresh(records);
            table.refresh(true);
        } catch (IOException ex) {
            System.err.println("failed to read log file: " + file + ": " + ex);
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private void checkLogClassMatch(String logClassMarker) {
        String logClassName = logClassMarker.substring(VMLog.RawDumpFlusher.LOGCLASS_MARKER.length());
        String imageLogClassName = tableModel.getClass().getSimpleName();
        if (!imageLogClassName.startsWith(logClassName)) {
            System.err.println("VMLog class in boot image: " +  imageLogClassName + " does not match log file: " + logClassName);
            System.exit(1);
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
    private static Component emptyStringRenderer;

    private final TeleVMLog vmLog;

    @SuppressWarnings("unchecked")
    VMLogView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        vmLog = inspection.vm().vmLog();
        emptyStringRenderer = new PlainLabel(inspection, "");
        viewPreferences = LogViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
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
        createFrame(true);
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
            validate();
            filterToolBar.getFocus();
        }
    }

    private void closeFilter() {
        if (filterToolBar != null) {
            contentPane.remove(filterToolBar);
            table.setDisplayedRows(null);
            validate();
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

        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu viewMenu = makeMenu(MenuKind.VIEW_MENU);
        viewMenu.add(showFilterCheckboxMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(defaultMenuItems(MenuKind.VIEW_MENU));

        if (inspection().vm().inspectionMode() == MaxInspectionMode.IMAGE) {
            File vmLogFile = inspection().vm().vmLogFile();
            if (vmLogFile != null) {
                readLogFile(vmLogFile);
            }
        }
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

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<VMLogColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
            }
        };
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    /**
     * @return the log being viewed
     */
    public TeleVMLog vmLog() {
        return vmLog;
    }

    private static class VMLogElementsTable extends InspectorTable {
        private VMLogView vmLogView;

        VMLogElementsTable(Inspection inspection, VMLogView vmLogView) {
            super(inspection);
            this.vmLogView = vmLogView;
            String vmLogClassName = vmLogView.vmLog().classActorForObjectType().simpleName();
            try {
                Class<?> klass = Class.forName(VMLogView.class.getPackage().getName() + "." + vmLogClassName + "ElementsTableModel");
                Constructor<?> cons = klass.getDeclaredConstructor(Inspection.class, TeleVMLog.class);
                vmLogView.tableModel = (VMLogElementsTableModel) cons.newInstance(inspection, vmLogView.vmLog);
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
            addColumn(VMLogColumnKind.ID, new IdCellRenderer(inspection, vmLogView), null);
            addColumn(VMLogColumnKind.THREAD, new ThreadCellRenderer(inspection, vmLogView), null);
            addColumn(VMLogColumnKind.OPERATION, new OperationCellRenderer(inspection, vmLogView), null);
            addColumn(VMLogColumnKind.ARG1, new ArgCellRenderer(inspection, vmLogView, 1), null);
            addColumn(VMLogColumnKind.ARG2, new ArgCellRenderer(inspection, vmLogView, 2), null);
            addColumn(VMLogColumnKind.ARG3, new ArgCellRenderer(inspection, vmLogView, 3), null);
            addColumn(VMLogColumnKind.ARG4, new ArgCellRenderer(inspection, vmLogView, 4), null);
            addColumn(VMLogColumnKind.ARG5, new ArgCellRenderer(inspection, vmLogView, 5), null);
            addColumn(VMLogColumnKind.ARG6, new ArgCellRenderer(inspection, vmLogView, 6), null);
            addColumn(VMLogColumnKind.ARG7, new ArgCellRenderer(inspection, vmLogView, 7), null);
            addColumn(VMLogColumnKind.ARG8, new ArgCellRenderer(inspection, vmLogView, 8), null);
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

    private static abstract class CellRendererHelper {
        protected final VMLogView vmLogView;

        CellRendererHelper(VMLogView vmLogView) {
            this.vmLogView = vmLogView;
        }

        public Component getRenderer(Object value, int row, int column) {
            if (value == null) {
                return vmLogView.gui().getUnavailableDataTableCellRenderer();
            }
            return vmLogView.tableModel.getRenderer(row, column);
        }
    }

    private static class IdCellRenderer extends CellRendererHelper implements TableCellRenderer {

        private IdCellRenderer(Inspection inspection, VMLogView vmLogView) {
            super(vmLogView);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = super.getRenderer(value, row, column);
            if (renderer == null) {
                WordValueLabel wvl = new WordValueLabel(vmLogView.inspection(), WordValueLabel.ValueMode.WORD, vmLogView.table);
                int id = (Integer) value;
                wvl.setText(Integer.toString(id));
                renderer = wvl;
                vmLogView.tableModel.setRenderer(row, column, wvl);
            }
            return renderer;
        }
    }

    static class ThreadCellRenderer extends CellRendererHelper implements TableCellRenderer {
        private static Map<Integer, Component> threadRenderers = new HashMap<Integer, Component>();
        private static ThreadCellRenderer singleton;

        private ThreadCellRenderer(Inspection inspection, VMLogView vmLogView) {
            super(vmLogView);
            singleton = this;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = super.getRenderer(value, row, column);
            if (renderer == null) {
                int threadID = (Integer) value;
                renderer = getThreadRenderer(threadID);
                vmLogView.tableModel.setRenderer(row, column, renderer);
            }
            return renderer;
        }

        static Component getThreadRenderer(int threadID) {
            Component renderer = threadRenderers.get(threadID);
            if (renderer == null) {
                String name;
                if (threadID == 0) {
                    name = "primordial";
                } else {
                    if (singleton.vmLogView.vm().inspectionMode() == MaxInspectionMode.IMAGE) {
                        // VM log from a file
                        name = singleton.vmLogView.tableModel.offLineThreadName(threadID);
                        if (name == null) {
                            name = "?id=" + Integer.valueOf(threadID);
                        }
                    } else {
                        MaxThread thread = singleton.vmLogView.vm().threadManager().getThread(threadID);
                        if (thread == null) {
                            name = "?id=" + Integer.valueOf(threadID);
                        } else {
                            name = thread.vmThreadName();
                        }
                    }
                }
                renderer = new PlainLabel(singleton.vmLogView.inspection(), name);
                threadRenderers.put(new Integer(threadID), renderer);
            }
            return renderer;
        }
    }

    private static class OperationCellRenderer extends CellRendererHelper implements TableCellRenderer {

        private Map<Integer, Component> operationRenderers = new HashMap<Integer, Component>();

        private OperationCellRenderer(Inspection inspection, VMLogView vmLogView) {
            super(vmLogView);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = super.getRenderer(value, row, column);
            if (renderer == null) {
                int op = (Integer) value;
                int header = vmLogView.tableModel.getRecord(row).getHeader();
                if (!vmLogView.tableModel.wellFormedHeader(header)) {
                    return vmLogView.gui().getUnavailableDataTableCellRenderer();
                }
                int loggerId = VMLog.Record.getLoggerId(header);
                int key = loggerId << 16 | op;
                renderer = operationRenderers.get(key);
                if (renderer == null) {
                    VMLogger logger = vmLogView.vmLog().getLogger(loggerId);
                    renderer = new PlainLabel(vmLogView.inspection(), logger.name + "." + logger.operationName(op));
                    operationRenderers.put(key, renderer);
                }
                vmLogView.tableModel.setRenderer(row, column, renderer);
            }
            return renderer;
        }
    }

    public static class ArgCellRenderer extends CellRendererHelper implements TableCellRenderer, Prober {
        private int argNum;

        private ArgCellRenderer(Inspection inspection, VMLogView vmLogView, int argNum) {
            super(vmLogView);
            this.argNum = argNum;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // null for an argument means absent
            if (value == null) {
                return emptyStringRenderer;
            }
            Component renderer = super.getRenderer(value, row, column);
            if (renderer == null) {
                int header = vmLogView.tableModel.getRecord(row).getHeader();
                if (!vmLogView.tableModel.wellFormedHeader(header)) {
                    return vmLogView.gui().getUnavailableDataTableCellRenderer();
                }

                long argValue = ((Word) value).value;
                VMLogArgRenderer vmLogArgRenderer = VMLogArgRendererFactory.getArgRenderer(vmLogView.vmLog().getLogger(VMLog.Record.getLoggerId(header)).name, vmLogView);
                renderer = vmLogArgRenderer.getRenderer(header, argNum, argValue);
                vmLogView.tableModel.setRenderer(row, column, renderer);
            }
            return renderer;
        }

        public void refresh(boolean force) {
            vmLogView.tableModel.refreshColumnRenderers(argNum, force);
        }

        public void redisplay() {
            vmLogView.tableModel.redisplayColumnRenderers(argNum);
        }
    }

}
