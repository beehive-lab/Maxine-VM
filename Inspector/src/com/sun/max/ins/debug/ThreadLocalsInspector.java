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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * A singleton inspector that displays thread local storage for the thread the VM that is the current user focus.
 *
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsInspector extends Inspector implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static ThreadLocalsInspector threadLocalsInspector;

    /**
     * Displays the (singleton) thread locals inspector, creating it if needed.
     */
    public static ThreadLocalsInspector make(Inspection inspection) {
        if (threadLocalsInspector == null) {
            threadLocalsInspector = new ThreadLocalsInspector(inspection);
        }
        return threadLocalsInspector;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsClient(this, "threadlocalsInspectorGeometry");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final ThreadLocalsViewPreferences viewPreferences;

    private MaxThread thread;
    private JTabbedPane tabbedPane;

    private ThreadLocalsInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        viewPreferences = ThreadLocalsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
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
        thread = inspection().focus().thread();
        tabbedPane = new JTabbedPane();
        if (thread != null) {
            for (Safepoint.State state : Safepoint.State.CONSTANTS) {
                final TeleThreadLocalValues values = thread.threadLocalsFor(state);
                if (values != null) {
                    final ThreadLocalsPanel panel = new ThreadLocalsPanel(inspection(), thread, values, viewPreferences);
                    tabbedPane.add(state.toString(), panel);
                }
            }
            tabbedPane.addChangeListener(new ChangeListener() {
                // Refresh a newly exposed pane to be sure it is current
                public void stateChanged(ChangeEvent event) {
                    final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) tabbedPane.getSelectedComponent();
                    if (threadLocalsPanel != null) {
                        threadLocalsPanel.refresh(true);
                    }
                }
            });
        }
        setContentPane(tabbedPane);
        setTitle();
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        String title = "Thread Locals: ";
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
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<ThreadLocalsColumnKind>(inspection(), "Thread Locals View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) tabbedPane.getSelectedComponent();
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
        for (Component component : tabbedPane.getComponents()) {
            final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) component;
            if (threadLocalsPanel.getSafepointState() == state) {
                return threadLocalsPanel;
            }
        }
        return null;
    }

    @Override
    protected boolean refreshView(boolean force) {

        boolean panelsAddedOrRemoved = false;
        for (Safepoint.State state : Safepoint.State.CONSTANTS) {
            final TeleThreadLocalValues values = thread.threadLocalsFor(state);
            final ThreadLocalsPanel panel = threadLocalsPanelFor(state);
            if (values != null) {
                if (panel == null) {
                    tabbedPane.add(state.toString(), new ThreadLocalsPanel(inspection(), thread, values, viewPreferences));
                    panelsAddedOrRemoved = true;
                }
            } else {
                if (panel != null) {
                    tabbedPane.remove(panel);
                    panelsAddedOrRemoved = true;
                }
            }
        }
        if (panelsAddedOrRemoved) {
            reconstructView();
        }

        // Only need to refresh the panel that's visible, as long as we refresh them when they become visible
        final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) tabbedPane.getSelectedComponent();
        if (threadLocalsPanel != null) {
            threadLocalsPanel.refresh(force);
        }
        super.refreshView(force);
        // The title displays thread state, so must be updated.
        setTitle();

        return true;
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        reconstructView();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        refreshView(true);
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void watchpointSetChanged() {
        refreshView(false);
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        threadLocalsInspector = null;
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

}
