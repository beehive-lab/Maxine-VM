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
package com.sun.max.ins;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;

/**
 * A singleton inspector that displays the different aspects of current user focus.
 * Intended for Inspector testing.
 *
 * @author Michael Van De Vanter
 */
public final class FocusInspector extends Inspector {

    // Set to null when inspector closed.
    private static FocusInspector focusInspector;
    /**
     * Display the (singleton) Focus inspector.
     *
     * @return  The Focus inspector, possibly newly created.
     */
    public static FocusInspector make(Inspection inspection) {
        if (focusInspector == null) {
            focusInspector = new FocusInspector(inspection);
        }
        return focusInspector;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final FocusTable.FocusViewPreferences viewPreferences;

    private FocusTable table;

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsListener(this, "focusInspectorGeometry");

    private FocusInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = FocusTable.FocusViewPreferences.globalPreferences(inspection);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        Trace.end(1,  tracePrefix() + " initializing");
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
        return "User Focus";
    }

    @Override
    protected void createView() {
        table = new FocusTable(inspection(), viewPreferences);
        forceRefresh();
        JTableColumnResizer.adjustColumnPreferredWidths(table);
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        setContentPane(panel);
        focus().addListener(table);
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        focusInspector = null;
        focus().removeListener(table);
        super.inspectorClosing();
    }

}
