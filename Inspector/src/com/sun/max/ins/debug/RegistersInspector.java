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
package com.sun.max.ins.debug;

import java.awt.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;


/**
 * A singleton inspector that displays register contents for the thread the VM that is the current user focus.
 *
 * @author Michael Van De Vanter
 */
public final class RegistersInspector extends Inspector implements TableColumnViewPreferenceListener {


    // Set to null when inspector closed.
    private static RegistersInspector registersInspector;

    /**
     * Displays the (singleton) registers  inspector, creating it if needed.
     */
    public static RegistersInspector make(Inspection inspection) {
        if (registersInspector == null) {
            registersInspector = new RegistersInspector(inspection);
        }
        return registersInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "registersInspectorGeometry");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final RegistersViewPreferences viewPreferences;

    private MaxThread thread;
    private RegistersTable table;

    private RegistersInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = RegistersViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        createFrame(null);
        refreshView(true);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().registersFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        thread = inspection().focus().thread();
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
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    public String getTextForTitle() {
        String title = "Registers: ";
        if (thread != null) {
            title += inspection().nameDisplay().longNameWithState(thread);
        }
        return title;
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<RegistersColumnKind>(inspection(), "Registers View Options", viewPreferences);
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
        // The title displays thread state, so must be updated.
        setTitle();
        return true;
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        registersInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

}
