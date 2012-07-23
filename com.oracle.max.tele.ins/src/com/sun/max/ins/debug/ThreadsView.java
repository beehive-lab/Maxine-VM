/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A singleton view that displays the list of threads running in the process of the VM.
 */
public final class ThreadsView extends AbstractView<ThreadsView> implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.THREADS;
    private static final String SHORT_NAME = "Threads";
    private static final String LONG_NAME = "Threads View";
    private static final String GEOMETRY_SETTINGS_KEY = "threadsViewGeometry";

    public static final class ThreadsViewManager extends AbstractSingletonViewManager<ThreadsView> {

        protected ThreadsViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected ThreadsView createView(Inspection inspection) {
            return new ThreadsView(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static ThreadsViewManager viewManager = null;

    public static ThreadsViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new ThreadsViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadsViewPreferences viewPreferences;

    private ThreadsTable table;

    private ThreadsView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        viewPreferences = ThreadsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);

        createFrame(true);

        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        String title = viewManager.shortName();
        if (!inspection().hasProcess()) {
            title += ": " + inspection().nameDisplay().noProcessShortText();
        }
        return title;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    public void createViewContent() {
        if (inspection().hasProcess()) {
            table = new ThreadsTable(inspection(), viewPreferences);
            final JScrollPane scrollPane = new InspectorScrollPane(inspection(), table);
            setContentPane(scrollPane);
        }

        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().viewSelectedThreadLocalsBlockMemory(null));
        memoryMenu.add(actions().viewSelectedThreadStackMemory(null));
        memoryMenu.add(actions().viewSelectedThreadVMLogMemory(null));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
    }

    @Override
    protected void refreshState(boolean force) {
        if (table != null) {
            table.refresh(force);
        }
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
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<ThreadsColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
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
