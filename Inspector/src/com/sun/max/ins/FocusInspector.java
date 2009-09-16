/*
 * * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
import com.sun.max.program.*;


/**
 * A singleton inspector that displays the different aspects of current user focus.
 * Intended for Inspector testing.
 *
 * @author Michael Van De Vanter
 */
public final class FocusInspector extends Inspector {

    // Set to null when inspector closed.
    private static FocusInspector focusInspector;
    /**
     * Display the (singleton) Focus inspector.
     *
     * @return  The Focus inspector, possibly newly created.
     */
    public static FocusInspector make(Inspection inspection) {
        if (focusInspector == null) {
            focusInspector = new FocusInspector(inspection);
        }
        return focusInspector;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final FocusTable.FocusViewPreferences viewPreferences;

    private FocusTable table;

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "focusInspectorGeometry");

    private FocusInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = FocusTable.FocusViewPreferences.globalPreferences(inspection);
        createFrame(null);
        Trace.end(1,  tracePrefix() + " initializing");
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
        return "User Focus";
    }

    @Override
    protected void createView() {
        table = new FocusTable(inspection(), viewPreferences);
        refreshView(true);
        JTableColumnResizer.adjustColumnPreferredWidths(table);
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        frame().setContentPane(panel);
        focus().addListener(table);
    }

    @Override
    protected boolean refreshView(boolean force) {
        table.refresh(force);
        super.refreshView(force);
        return true;
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        focusInspector = null;
        focus().removeListener(table);
        super.inspectorClosing();
    }

}
