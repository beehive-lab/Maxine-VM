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
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * A singleton inspector that displays thread local storage for the thread the VM that is the current user focus.
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

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "threadlocalsInspector");

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
        refreshView(true);
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().threadLocalsFrameDefaultBounds();
    }

    @Override
    protected void createView() {
        _teleNativeThread = inspection().focus().thread();
        if (_teleNativeThread == null) {
            _tabbedPane = null;
        } else {
            _tabbedPane = new JTabbedPane();
            for (Safepoint.State state : Safepoint.State.CONSTANTS) {
                final TeleThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
                if (values != null) {
                    final ThreadLocalsPanel panel = new ThreadLocalsPanel(inspection(), _teleNativeThread, values, _viewPreferences);
                    _tabbedPane.add(state.toString(), panel);
                }
            }
            _tabbedPane.addChangeListener(new ChangeListener() {
                // Do a refresh whenever there's a tab change, so that the newly exposed pane is sure to be current
                public void stateChanged(ChangeEvent event) {
                    refreshView(true);
                }
            });
        }
        frame().setContentPane(_tabbedPane);
        updateFrameTitle();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        String title = "Thread Locals: ";
        if (_teleNativeThread != null) {
            title += inspection().nameDisplay().longNameWithState(_teleNativeThread);
        }
        return title;
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ThreadLocalsColumnKind>(inspection(), "Thread Locals View Options", _viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) _tabbedPane.getSelectedComponent();
                final String name = getTextForTitle() + " " + threadLocalsPanel.getSafepointState().toString();
                final MessageFormat footer = new MessageFormat("Maxine: " + name + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
                try {
                    final InspectorTable inspectorTable = threadLocalsPanel.getTable();
                    assert inspectorTable != null;
                    inspectorTable.print(JTable.PrintMode.FIT_WIDTH, null, footer);
                } catch (PrinterException printerException) {
                    gui().errorMessage("Print failed: " + printerException.getMessage());
                }
            }
        };
    }

    private ThreadLocalsPanel threadLocalsPanelFor(Safepoint.State state) {
        for (Component component : _tabbedPane.getComponents()) {
            final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) component;
            if (threadLocalsPanel.getSafepointState() == state) {
                return threadLocalsPanel;
            }
        }
        return null;
    }

    @Override
    protected void refreshView(boolean force) {

        boolean panelsAddedOrRemoved = false;
        for (Safepoint.State state : Safepoint.State.CONSTANTS) {
            final TeleThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
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
            threadLocalsPanel.refresh(force);
        }
        super.refreshView(force);
        // The title displays thread state, so must be updated.
        updateFrameTitle();
    }

    public void viewConfigurationChanged() {
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
