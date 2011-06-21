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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * Singleton view that displays information about all kinds of breakpoints that might be set in the VM.
 * Wrappers with extra information about each breakpoint are kept in a model.
 */
public final class BreakpointsView extends AbstractView<BreakpointsView> implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.BREAKPOINTS;
    private static final String SHORT_NAME = "Breakpoints";
    private static final String LONG_NAME = "Breakpoints View";
    private static final String GEOMETRY_SETTINGS_KEY = "breakpointsViewGeometry";

    public static final class BreakpointsViewManager extends AbstractSingletonViewManager<BreakpointsView> {

        protected BreakpointsViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected BreakpointsView createView(Inspection inspection) {
            return new BreakpointsView(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static BreakpointsViewManager viewManager = null;

    public static BreakpointsViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new BreakpointsViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BreakpointsViewPreferences viewPreferences;

    private BreakpointsTable table;

    private BreakpointsView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = BreakpointsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(actions().removeSelectedBreakpoint());
        editMenu.add(actions().removeAllBreakpoints());

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        final InspectorMenu debugMenu = frame.makeMenu(MenuKind.DEBUG_MENU);
        debugMenu.addSeparator();
        debugMenu.add(actions().genericBreakpointMenuItems());
        if (vm().watchpointManager() != null) {
            debugMenu.add(actions().genericWatchpointMenuItems());
            debugMenu.add(views().activateSingletonViewAction(ViewKind.WATCHPOINTS));
        }

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        Trace.end(1,  tracePrefix() + " initializing");
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
    protected void createViewContent() {
        table = new BreakpointsTable(inspection(), viewPreferences);
        setContentPane(new InspectorScrollPane(inspection(), table));
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    @Override
    public void breakpointStateChanged() {
        forceRefresh();
    }

    @Override
    public  void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<BreakpointsColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void viewClosing() {
        viewPreferences.removeListener(this);
        super.viewClosing();
    }

}
