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

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * A singleton inspector that displays thread local storage for the thread the {@link TeleVM} that is the current user focus.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static ThreadLocalsInspector _threadLocalsInspector;

    /**
     * Displays the (singleton) thread locals inspector, creating it if needed.
     */
    public static ThreadLocalsInspector make(Inspection inspection) {
        if (_threadLocalsInspector == null) {
            _threadLocalsInspector = new ThreadLocalsInspector(inspection);
        }
        return _threadLocalsInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "threadlocalsInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadLocalsViewPreferences _viewPreferences;

    private TeleNativeThread _teleNativeThread;
    private JTabbedPane _tabbedPane;

    private ThreadLocalsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        _viewPreferences = ThreadLocalsViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ThreadLocalsColumnKind>(inspection(), "Thread Locals View Options", _viewPreferences);
            }
        });
        refreshView(inspection.teleVM().epoch(), true);
        if (!inspection.settings().hasComponentLocation(_saveSettingsListener)) {
            frame().setLocation(inspection().geometry().threadLocalsFrameDefaultLocation());
            frame().getContentPane().setPreferredSize(inspection().geometry().threadLocalsFramePrefSize());
        }
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    protected void createView(long epoch) {
        _teleNativeThread = inspection().focus().thread();
        if (_teleNativeThread == null) {
            _tabbedPane = null;
            frame().setTitle(getTextForTitle());
        } else {
            _tabbedPane = new JTabbedPane();
            for (Safepoint.State state : Safepoint.State.CONSTANTS) {
                final TeleVMThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
                if (values != null) {
                    final ThreadLocalsPanel panel = new ThreadLocalsPanel(inspection(), _teleNativeThread, values, _viewPreferences);
                    _tabbedPane.add(state.toString(), panel);
                }
            }
            _tabbedPane.addChangeListener(new ChangeListener() {
                // Do a refresh whenever there's a tab change, so that the newly exposed pane is sure to be current
                public void stateChanged(ChangeEvent event) {
                    refreshView(teleVM().epoch(), true);
                }
            });
            frame().setTitle(getTextForTitle() + " " + inspection().nameDisplay().longName(_teleNativeThread));
        }
        frame().setContentPane(_tabbedPane);
    }

    @Override
    public String getTextForTitle() {
        return "Thread Locals: ";
    }

    private ThreadLocalsPanel threadLocalsPanelFor(Safepoint.State state) {
        for (Component component : _tabbedPane.getComponents()) {
            final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) component;
            if (threadLocalsPanel._teleVMThreadLocalValues.safepointState() == state) {
                return threadLocalsPanel;
            }
        }
        return null;
    }

    @Override
    public void refreshView(long epoch, boolean force) {

        boolean panelsAddedOrRemoved = false;
        for (Safepoint.State state : Safepoint.State.CONSTANTS) {
            final TeleVMThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
            final ThreadLocalsPanel panel = threadLocalsPanelFor(state);
            if (values != null) {
                if (panel == null) {
                    _tabbedPane.add(state.toString(), new ThreadLocalsPanel(inspection(), _teleNativeThread, values, _viewPreferences));
                    panelsAddedOrRemoved = true;
                }
            } else {
                if (panel != null) {
                    _tabbedPane.remove(panel);
                    panelsAddedOrRemoved = true;
                }
            }
        }
        if (panelsAddedOrRemoved) {
            reconstructView();
        }

        // Only need to refresh the panel that's visible, as long as we refresh them when they become visible
        final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) _tabbedPane.getSelectedComponent();
        if (threadLocalsPanel != null) {
            threadLocalsPanel.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
    }

    @Override
    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativThread) {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _threadLocalsInspector = null;
        _viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
