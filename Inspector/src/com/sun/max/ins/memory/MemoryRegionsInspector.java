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

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.InspectorTable.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;


/**
 * A singleton inspector that displays a list of {@link MemoryRegion}s that have been allocated in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsInspector extends Inspector {

    // Set to null when inspector closed.
    private static MemoryRegionsInspector _memoryRegionsInspector;

    /**
     * Display and highlight the (singleton) MemoryRegions inspector.
     *
     * @return  The MemoryRegions inspector, possibly newly created.
     */
    public static MemoryRegionsInspector make(Inspection inspection) {
        if (_memoryRegionsInspector == null) {
            _memoryRegionsInspector = new MemoryRegionsInspector(inspection, Residence.INTERNAL);
        }
        _memoryRegionsInspector.highlight();
        return _memoryRegionsInspector;
    }

    private MemoryRegionsTable _table;

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "MemoryRegionsInspector");

    private MemoryRegionsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        Trace.begin(1, tracePrefix() + "initializing");
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<MemoryRegionsColumnKind>(inspection(), "Memory Regions View Options", _table.preferences());
            }
        });
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "MemoryRegions";
    }

    @Override
    public void createView(long epoch) {
        _table = new MemoryRegionsTable(inspection());
        _table.addColumnChangeListener(new ColumnChangeListener() {
            public void columnPreferenceChanged() {
                reconstructView();
            }
        });
        frame().setContentPane(new InspectorScrollPane(inspection(), _table));
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        _table.refresh(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    @Override
    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        updateMemoryRegionFocus(memoryRegion);
    }

    /**
     * Changes the Inspector's selected row to agree with the global thread selection.
     */
    private void updateMemoryRegionFocus(MemoryRegion selectedMemoryRegion) {
        _table.selectMemoryRegion(selectedMemoryRegion);
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _memoryRegionsInspector = null;
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        _memoryRegionsInspector = null;
        dispose();
    }

}
