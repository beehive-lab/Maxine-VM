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
package com.sun.max.ins;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.vm.*;

/**
 * A singleton inspector that displays {@link VMConfiguration}  information in the VM boot image.
 *
 * @author Michael Van De Vanter
 */
public final class BootImageInspector extends Inspector<BootImageInspector>  implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.BOOT_IMAGE;
    private static final String SHORT_NAME = "Boot Image";
    private static final String LONG_NAME = "Boot Image View";
    private static final String GEOMETRY_SETTINGS_KEY = "bootImageViewGeometry";

    public static final class BootImageViewManager extends AbstractSingletonViewManager<BootImageInspector> {

        protected BootImageViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected BootImageInspector createView(Inspection inspection) {
            return new BootImageInspector(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static BootImageViewManager viewManager = null;

    public static BootImageViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new BootImageViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BootImageViewPreferences viewPreferences;

    private BootImageTable table;

    private BootImageInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1, tracePrefix() + "initializing");
        viewPreferences = BootImageViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName() + ": " + vm().bootImageFile().getAbsolutePath();
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    protected void createView() {
        table = new BootImageTable(inspection(), viewPreferences);
        setContentPane(new InspectorScrollPane(inspection(), table));
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<BootImageColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
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
    public void vmProcessTerminated() {
        forceRefresh();
    }

    @Override
    public void inspectorClosing() {
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
