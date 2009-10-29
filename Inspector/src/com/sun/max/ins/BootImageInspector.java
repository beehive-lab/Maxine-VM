/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
        final InspectorFrameInterface frame = createFrame(true);
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
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().bootImageFrameDefaultBounds();
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
        return "Boot Image: " + maxVM().bootImageFile().getAbsolutePath();
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
    protected boolean refreshView(boolean force) {
        table.refresh(force);
        super.refreshView(force);
        return true;
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
