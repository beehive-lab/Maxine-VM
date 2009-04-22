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
import java.awt.print.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A singleton inspector that displays the list of threads running in the process of the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static ThreadsInspector _threadsInspector;
    /**
     * Display the (singleton) threads inspector, creating it if needed.
     */
    public static ThreadsInspector make(Inspection inspection) {
        if (_threadsInspector == null) {
            _threadsInspector = new ThreadsInspector(inspection);
        }
        return _threadsInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "threadsInspector");

    private ThreadsTable _table;

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadsViewPreferences _viewPreferences;

    private ThreadsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        _viewPreferences = ThreadsViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().threadsFrameDefaultBounds();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "Threads";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ThreadsColumnKind>(inspection(), "Threads View Options", _viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final MessageFormat footer = new MessageFormat("Maxine: " + getTextForTitle() + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
                try {
                    _table.print(JTable.PrintMode.FIT_WIDTH, null, footer);
                } catch (PrinterException printerException) {
                    inspection().errorMessage("Print failed: " + printerException.getMessage());
                }
            }
        };
    }

    @Override
    public void createView(long epoch) {
        if (_table != null) {
            focus().removeListener(_table);
        }
        _table = new ThreadsTable(inspection(), _viewPreferences);
        focus().addListener(_table);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), _table);
        frame().setContentPane(scrollPane);
    }

    @Override
    protected void refreshView(long epoch, boolean force) {
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
        _threadsInspector = null;
        _viewPreferences.removeListener(this);
        focus().removeListener(_table);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        _threadsInspector = null;
        dispose();
    }

}
