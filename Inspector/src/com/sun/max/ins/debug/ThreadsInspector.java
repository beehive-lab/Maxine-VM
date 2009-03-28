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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * A singleton inspector that displays the list of threads running in the process of the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadsInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static ThreadsInspector _threadsInspector;
    /**
     * Display and highlight the (singleton) threads inspector, creating it if needed.
     */
    public static ThreadsInspector make(Inspection inspection) {
        if (_threadsInspector == null) {
            _threadsInspector = new ThreadsInspector(inspection, Residence.INTERNAL);
        }
        _threadsInspector.highlight();
        return _threadsInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "threadsInspector");

    private ThreadsTable _table;

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadsViewPreferences _viewPreferences;

    private ThreadsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        Trace.begin(1,  tracePrefix() + " initializing");
        _viewPreferences = ThreadsViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ThreadsColumnKind>(inspection(), "Threads View Options", _viewPreferences);
            }
        });
        if (!inspection.settings().hasComponentLocation(_saveSettingsListener)) {
            frame().setLocation(inspection().geometry().threadsFrameDefaultLocation());
            frame().getContentPane().setPreferredSize(inspection().geometry().threadsFramePrefSize());
        }
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "Threads";
    }

    @Override
    public void createView(long epoch) {
        _table = new ThreadsTable(inspection(), _viewPreferences);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), _table);
        frame().setContentPane(scrollPane);
    }


    @Override
    public void refreshView(long epoch, boolean force) {
        _table.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        _table.selectThread(teleNativeThread);
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
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        _threadsInspector = null;
        dispose();
    }

}
