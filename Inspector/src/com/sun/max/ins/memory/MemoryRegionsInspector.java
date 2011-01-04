/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * A singleton inspector that displays a list of {@linkplain MaxMemoryRegion memory regions} that have been allocated in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 2;

    // Set to null when inspector closed.
    private static MemoryRegionsInspector memoryRegionsInspector;

    /**
     * Displays the (singleton) MemoryRegions inspector.
     * @return  The MemoryRegions inspector, possibly newly created.
     */
    public static MemoryRegionsInspector make(Inspection inspection) {
        if (memoryRegionsInspector == null) {
            memoryRegionsInspector = new MemoryRegionsInspector(inspection);
        }
        return memoryRegionsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "memoryRegionsInspectorGeometry");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MemoryRegionsViewPreferences viewPreferences;
    private InspectorPanel contentPane;

    private MemoryRegionsTable table;

    private TableRowFilterToolBar filterToolBar = null;
    private JCheckBoxMenuItem showFilterCheckboxMenuItem;
    private int[] filterMatchingRows = null;

    private MemoryRegionsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");
        viewPreferences = MemoryRegionsViewPreferences.globalPreferences(inspection());
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
        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedMemoryRegionWords());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));

        final InspectorMenuItems defaultViewMenuItems = defaultMenuItems(MenuKind.VIEW_MENU);
        final InspectorMenu viewMenu = frame.makeMenu(MenuKind.VIEW_MENU);
        viewMenu.add(showFilterCheckboxMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(defaultViewMenuItems);

        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().memoryRegionsFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        table = new MemoryRegionsTable(inspection(), viewPreferences);
        final InspectorScrollPane memoryRegionsScrollPane = new InspectorScrollPane(inspection(), table);
        contentPane = new InspectorPanel(inspection(), new BorderLayout());
        contentPane.add(memoryRegionsScrollPane, BorderLayout.CENTER);
        setContentPane(contentPane);
    }

    private final RowMatchListener rowMatchListener = new RowMatchListener() {

        public void setSearchResult(int[] result) {
            filterMatchingRows = result;
            table.setDisplayedRows(filterMatchingRows);
            System.out.println("Match=" + Arrays.toString(filterMatchingRows));
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
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    public String getTextForTitle() {
        return "MemoryRegions";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MemoryRegionsColumnKind>(inspection(), "Memory Regions View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    @Override
    protected void refreshView(boolean force) {
        table.refresh(force);
        if (filterToolBar != null) {
            filterToolBar.refresh(force);
        }
        super.refreshView(force);
    }

    @Override
    public void memoryRegionFocusChanged(MaxMemoryRegion oldMemoryRegion, MaxMemoryRegion memoryRegion) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() != ProcessState.TERMINATED) {
            refreshView(true);
        }
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        memoryRegionsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        memoryRegionsInspector = null;
        viewPreferences.removeListener(this);
        dispose();
    }


}
