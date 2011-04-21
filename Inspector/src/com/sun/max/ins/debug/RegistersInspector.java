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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A singleton inspector that displays register contents for the thread the VM that is the current user focus.
 *
 * @author Michael Van De Vanter
 */
public final class RegistersInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.REGISTERS;
    private static final String SHORT_NAME = "Registers";
    private static final String LONG_NAME = "Registers Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "registersInspectorGeometry";

    public static final class RegistersViewManager extends AbstractSingletonViewManager<RegistersInspector> {

        protected RegistersViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        @Override
        protected RegistersInspector createView(Inspection inspection) {
            return new RegistersInspector(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static RegistersViewManager viewManager = null;

    public static RegistersViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new RegistersViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final RegistersViewPreferences viewPreferences;

    private RegistersTable table;

    /**
     * The thread whose register contents are currently being displayed.  Can be null.
     */
    private MaxThread thread;

    private RegistersInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = RegistersViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(actions().activateSingletonView(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        forceRefresh();
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        String title = viewManager.shortName() + ": ";
        if (!inspection().hasProcess()) {
            title += inspection().nameDisplay().noProcessShortText();
        } else if (thread != null) {
            title += inspection().nameDisplay().longNameWithState(thread);
        }
        return title;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    protected void createView() {
        thread = focus().thread();
        if (thread == null) {
            table = null;
            setContentPane(new InspectorPanel(inspection(), new BorderLayout()));
        } else {
            table = new RegistersTable(inspection(), thread, viewPreferences);
            setContentPane(new InspectorScrollPane(inspection(), table));
        }
        setTitle();
    }

    @Override
    protected void refreshState(boolean force) {
        if (table != null) {
            table.refresh(force);
        }
        // The title displays thread state, so must be updated.
        setTitle();
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<RegistersColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
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
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

}
