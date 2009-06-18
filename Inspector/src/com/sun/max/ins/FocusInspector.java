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
    private static FocusInspector _focusInspector;
    /**
     * Display the (singleton) Focus inspector.
     *
     * @return  The Focus inspector, possibly newly created.
     */
    public static FocusInspector make(Inspection inspection) {
        if (_focusInspector == null) {
            _focusInspector = new FocusInspector(inspection);
        }
        return _focusInspector;
    }

    private FocusTable _table;

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "focusInspector");

    private FocusInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        createFrame(null);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    protected InspectorTable getTable() {
        return _table;
    }

    @Override
    public String getTextForTitle() {
        return "User Focus";
    }

    @Override
    protected void createView() {
        _table = new FocusTable(inspection());
        refreshView(true);
        JTableColumnResizer.adjustColumnPreferredWidths(_table);
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(_table.getTableHeader(), BorderLayout.NORTH);
        panel.add(_table, BorderLayout.CENTER);
        frame().setContentPane(panel);
        focus().addListener(_table);
    }

    @Override
    protected void refreshView(boolean force) {
        _table.refresh(force);
        super.refreshView(force);
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    @Override
    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _focusInspector = null;
        focus().removeListener(_table);
        super.inspectorClosing();
    }

}
