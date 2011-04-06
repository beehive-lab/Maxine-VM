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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
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
    private static final ViewKind VIEW_KIND = ViewKind.WATCHPOINTS;
    private static final String SHORT_NAME = "Watchpoints";
    private static final String LONG_NAME = "Watchpoints Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "watchpointsInspectorGeometry";

    private static final class WatchpointsViewManager extends AbstractSingletonViewManager<WatchpointsInspector> {

        protected WatchpointsViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        public boolean isSupported() {
            return vm().watchpointManager() != null;
        }

        public boolean isEnabled() {
            return vm().watchpointManager() != null;
        }

        public WatchpointsInspector activateView(Inspection inspection) {
            if (inspector == null) {
                inspector = new WatchpointsInspector(inspection);
            }
            return inspector;
        }
    }

    // Will be non-null before any instances created.
    private static WatchpointsViewManager viewManager = null;

    public static ViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new WatchpointsViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final WatchpointsViewPreferences viewPreferences;

    private WatchpointsTable table;

    private WatchpointsInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
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
        memoryMenu.add(actions().activateSingletonView(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    protected void createView() {
        table = new WatchpointsTable(inspection(), viewPreferences);
        setContentPane(new InspectorScrollPane(inspection(), table));
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
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

    public void viewConfigurationChanged() {
        reconstructView();
    }


    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(TRACE_VALUE, tracePrefix() + " closing");
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
