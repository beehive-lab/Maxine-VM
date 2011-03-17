/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * A singleton inspector that displays the list of threads running in the process of the VM.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;

    // Set to null when inspector closed.
    private static ThreadsInspector threadsInspector;
    /**
     * Display the (singleton) threads inspector, creating it if needed.
     */
    public static ThreadsInspector make(Inspection inspection) {
        if (threadsInspector == null) {
            threadsInspector = new ThreadsInspector(inspection);
        }
        return threadsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "threadsInspectorGeometry");

    private ThreadsTable table;

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadsViewPreferences viewPreferences;

    private ThreadsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        viewPreferences = ThreadsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedThreadMemoryWords(null));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().threadsDefaultFrameBounds();
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
        return "Threads";
    }

    @Override
    public void createView() {
        table = new ThreadsTable(inspection(), viewPreferences);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), table);
        setContentPane(scrollPane);
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<ThreadsColumnKind>(inspection(), "Threads View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        threadsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
