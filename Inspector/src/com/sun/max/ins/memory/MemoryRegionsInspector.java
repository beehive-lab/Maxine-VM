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
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;


/**
 * A singleton inspector that displays a list of {@link MemoryRegion}s that have been allocated in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsInspector extends Inspector  implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static MemoryRegionsInspector _memoryRegionsInspector;

    /**
     * Display and highlight the (singleton) MemoryRegions inspector.
     * @return  The MemoryRegions inspector, possibly newly created.
     */
    public static MemoryRegionsInspector make(Inspection inspection) {
        if (_memoryRegionsInspector == null) {
            _memoryRegionsInspector = new MemoryRegionsInspector(inspection, Residence.INTERNAL);
        }
        _memoryRegionsInspector.highlight();
        return _memoryRegionsInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "memoryRegionsInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MemoryRegionsViewPreferences _viewPreferences;

    private MemoryRegionsTable _table;

    private MemoryRegionsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        Trace.begin(1, tracePrefix() + "initializing");
        _viewPreferences = MemoryRegionsViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<MemoryRegionsColumnKind>(inspection(), "Memory Regions View Options", _viewPreferences);
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
        if (_table != null) {
            focus().removeListener(_table);
        }
        _table = new MemoryRegionsTable(inspection(), _viewPreferences);
        focus().addListener(_table);
        frame().setContentPane(new InspectorScrollPane(inspection(), _table));
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        _table.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _memoryRegionsInspector = null;
        _viewPreferences.removeListener(this);
        focus().removeListener(_table);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        _memoryRegionsInspector = null;
        _viewPreferences.removeListener(this);
        dispose();
    }

}
