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

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;

/**
 * A singleton inspector that displays {@link VMConfiguration}  information in the VM boot image.
 *
 * @author Michael Van De Vanter
 */
public final class BootImageInspector extends Inspector  implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static BootImageInspector bootImageInspector;

    /**
     * Displays the (singleton) BootImage inspector.
     * @return  The BootImage inspector, possibly newly created.
     */
    public static BootImageInspector make(Inspection inspection) {
        if (bootImageInspector == null) {
            bootImageInspector = new BootImageInspector(inspection);
        }
        return bootImageInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "bootImageInspectorGeometry");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BootImageViewPreferences viewPreferences;

    private BootImageTable table;

    private BootImageInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");
        viewPreferences = BootImageViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    protected Rectangle defaultGeometry() {
        return inspection().geometry().bootImageDefaultFrameGeometry();
    }

    @Override
    protected void createView() {
        table = new BootImageTable(inspection(), viewPreferences);
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
        return "Boot Image: " + vm().bootImageFile().getAbsolutePath();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<BootImageColumnKind>(inspection(), "Boot Image View Options", viewPreferences);
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

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        bootImageInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        bootImageInspector = null;
        viewPreferences.removeListener(this);
        dispose();
    }

}
