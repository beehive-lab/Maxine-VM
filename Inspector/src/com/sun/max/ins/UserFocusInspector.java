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

import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;

/**
 * A singleton inspector that displays the different aspects of current user focus.
 * Intended for Inspector testing.
 *
 * @author Michael Van De Vanter
 */
public final class UserFocusInspector extends Inspector<UserFocusInspector> {

    private static final ViewKind VIEW_KIND = ViewKind.USER_FOCUS;
    private static final String SHORT_NAME = "User Focus";
    private static final String LONG_NAME = "User Focus Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "userFocusInspectorGeometry";

    public static final class UserFocusViewManager extends AbstractSingletonViewManager<UserFocusInspector> {

        protected UserFocusViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected UserFocusInspector createView(Inspection inspection) {
            return new UserFocusInspector(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static UserFocusViewManager viewManager = null;

    public static UserFocusViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new UserFocusViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final FocusTable.FocusViewPreferences viewPreferences;

    private FocusTable table;

    private UserFocusInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = FocusTable.FocusViewPreferences.globalPreferences(inspection);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

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

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        focus().removeListener(table);
        super.inspectorClosing();
    }

}
