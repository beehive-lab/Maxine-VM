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

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * Singleton inspector that displays information about memory watchpoints set in the VM.
 * Wrappers with extra information about each breakpoint are kept in a model.
 *
 * @author Michael Van De Vanter
 */
public final class WatchpointsInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;

    // Set to null when inspector closed.
    private static WatchpointsInspector watchpointsInspector;

    /**
     * Displays the (singleton) watchpoints inspector.
     * @return  The watchpoints inspector, possibly newly created.
     */
    public static WatchpointsInspector make(Inspection inspection) {
        if (watchpointsInspector == null) {
            watchpointsInspector = new WatchpointsInspector(inspection);
        }
        return watchpointsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsListener(this, "watchpointsInspectorGeometry");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final WatchpointsViewPreferences viewPreferences;

    private WatchpointsTable table;

    private WatchpointsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        viewPreferences = WatchpointsViewPreferences.globalPreferences(inspection);
        viewPreferences.addListener(this);

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(actions().setWordWatchpoint());
        editMenu.addSeparator();
        editMenu.add(actions().removeSelectedWatchpoint());
        editMenu.add(actions().removeAllWatchpoints());

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedMemoryWatchpointAction());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultGeometry() {
        return inspection().geometry().watchpointsDefaultFrameGeometry();
    }

    @Override
    protected void createView() {
        table = new WatchpointsTable(inspection(), viewPreferences);
        setContentPane(new InspectorScrollPane(inspection(), table));
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
        return "Watchpoints";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<WatchpointsColumnKind>(inspection(), "Watchpoints Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void watchpointSetChanged() {
        forceRefresh();
    }

    @Override
    public void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(TRACE_VALUE, tracePrefix() + " closing");
        watchpointsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
