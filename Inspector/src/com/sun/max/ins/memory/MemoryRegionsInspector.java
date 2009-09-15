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
package com.sun.max.ins.memory;

import java.awt.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;


/**
 * A singleton inspector that displays a list of {@link MemoryRegion}s that have been allocated in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsInspector extends Inspector  implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static MemoryRegionsInspector memoryRegionsInspector;

    /**
     * Displays the (singleton) MemoryRegions inspector.
     * @return  The MemoryRegions inspector, possibly newly created.
     */
    public static MemoryRegionsInspector make(Inspection inspection) {
        if (memoryRegionsInspector == null) {
            memoryRegionsInspector = new MemoryRegionsInspector(inspection);
        }
        return memoryRegionsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "memoryRegionsInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MemoryRegionsViewPreferences viewPreferences;

    private MemoryRegionsTable table;

    private MemoryRegionsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");
        viewPreferences = MemoryRegionsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(actions().inspectSelectedMemoryRegionWords());
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().memoryRegionsFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        table = new MemoryRegionsTable(inspection(), viewPreferences);
        frame().setContentPane(new InspectorScrollPane(inspection(), table));
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
        return "MemoryRegions";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MemoryRegionsColumnKind>(inspection(), "Memory Regions View Options", viewPreferences);
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

    @Override
    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        if (table != null) {
            table.updateFocusSelection();
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void watchpointSetChanged() {
        refreshView(true);
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        memoryRegionsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        memoryRegionsInspector = null;
        viewPreferences.removeListener(this);
        dispose();
    }

}
